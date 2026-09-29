<!--
  微信公众号发布参考信息（HTML 注释，不会渲染到正文）

  【标题候选】（公众号标题上限 64 字）
  1. 从零手写数智人事系统（七）：双 Token + httpOnly Cookie 认证实战
  2. 15 分钟过期的 Token 怎么续？双 Cookie 设计一次讲透
  3. 不查库也能认证：JWT claims 直构 SecurityContext 的取舍

  【摘要建议】（上限 120 字）
  表结构理清了，往上走一层——登录。本期拆解双 Token 的寿命差设计
  （15 分钟 + 7 天）、两个 httpOnly Cookie 的 Path 隔离策略、
  JwtAuthenticationFilter 三层降级取 token，以及从 claims 直接构建
  SecurityContext 省下两次数据库查询的认证链路。

  【封面建议】
  系列统一封面风格，沿用第 2/3/4/5/6 期的登录页截图：
  https://cdn.jsdelivr.net/gh/quuuuj/hrm@5f06498f73fd00480b9549d82c358494b97244fe/img/readme/01-login.png
  封面需在公众号后台单独上传，正文中的图片链接仅供编辑器抓取转存。

  【发布链路】
  用 doocs/md（https://md.doocs.org）导入本文件 —— 它支持 mermaid 渲染，
  左侧放入后右侧实时预览，再一键复制到公众号后台。mdnice 不支持 mermaid，请勿使用。

  【发布前待办】
  1. 时序图改为 PNG 引用（微信 SVG 过滤会剥掉箭头的 defs/marker，PNG 不受影响）：
     a. doocs/md 导出「PNG 图片」（注意亮色主题），存为 img/readme/07-auth-sequence.png；
     b. 提交并推送后，把正文图片 URL 里的 <完整commit SHA> 替换为该次提交的完整 SHA；
     c. curl -o /dev/null -w "%{http_code}" 验证 jsDelivr URL 返回 200（钉完整 SHA，勿用 @dev）。
  2. jsDelivr 封面 URL 已在本地 curl 验证 200（SHA=5f06498...，远端 dev）。
-->

# 从零手写数智人事系统（七）：双 Token + httpOnly Cookie 认证实战

上一期把 84 张表的分工理清了，今天往上走一层——登录。这套系统的认证不是简单的"登录发 token、请求带 token"，而是**双 Token + httpOnly Cookie**的组合：一个 15 分钟的 Access Token 负责日常请求，一个 7 天的 Refresh Token 负责续期，两个 token 分别躺在两个不同 Path 的 Cookie 里。

为什么这么麻烦？因为单 Token 有个无解的矛盾：**有效期短了用户体验差（每 15 分钟重新登录），长了安全风险大（泄露即被盗用 7 天）**。双 Token 的设计就是把这个矛盾拆开——短命的负责干活，长命的只在续期时露面，且只出现在一个专门的端点上。

## 动手前提

需完成第 5 期：`hrm-server` 能启动，`ResponseDTO` 统一响应已就位。本期需要前端配合看 Cookie 效果，但验证环节只用 `curl` 就能跑通。

## 本期目标

登录成功后浏览器收到两个 httpOnly Cookie（`token` 和 `refreshToken`），后续请求自动携带 `token`；`token` 过期后能用 `refreshToken` 换新的；`JwtAuthenticationFilter` 能从 Header、Query、Cookie 三层取到 token 并直接构建认证对象。

## 双 Token：把"身份"和"续期资格"拆成两张票

先给本期第一个术语下定义：**双 Token（Access/Refresh Token）**指系统同时签发两个不同寿命、不同用途的 JWT——短命的 Access Token 用于日常 API 调用，长命的 Refresh Token 只用于换取新的 Access Token。

`hrm-server/src/main/java/com/qiujie/util/JwtUtil.java:26` 里两个寿命常数定死了这个分工：

```java
public final static long ACCESS_EXPIRATION = 15 * 60 * 1000;        // 15 分钟
public final static long REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 天
```

为什么是 15 分钟和 7 天？这不是拍脑袋的数字，是两个威胁模型的折中：

- **Access Token 15 分钟**：假设它泄露了（XSS、中间人、日志泄露），攻击者最多能用 15 分钟。对人事系统来说，15 分钟够看几条员工信息，但不够导出全部数据——损失可控。
- **Refresh Token 7 天**：假设用户每天打开一次系统，一周内不用重新登录；超过 7 天不用，强制重新认证一次，防止"永久在线"的僵尸会话。

两个 Token 的载荷（claims）完全一样——`staffId`、`permissions`、`username`、`type`——区别只在 `type` 字段和签名时间（`JwtUtil.java:92-116`）：

```java
public static String generateAccessToken(Integer staffId, String permissions, String username) {
    Map<String, Object> claims = buildClaims(staffId, permissions);
    claims.put("type", TOKEN_TYPE_ACCESS);        // "access"
    return buildToken(claims, createUser(username), ACCESS_EXPIRATION);
}

public static String generateRefreshToken(Integer staffId, String permissions, UserDetails userDetails) {
    Map<String, Object> claims = buildClaims(staffId, permissions);
    claims.put("type", TOKEN_TYPE_REFRESH);       // "refresh"
    return buildToken(claims, userDetails, REFRESH_EXPIRATION);
}
```

`type` 字段是关键防线：**Refresh Token 永远不能当 Access Token 用**。下面会看到，`JwtAuthenticationFilter` 明确拒绝 `type != "access"` 的 token，哪怕它没过期、签名也对。

## httpOnly Cookie：让前端代码碰不到 Token

第二个术语：**httpOnly Cookie**指设置了 `HttpOnly` 标志的浏览器 Cookie，JavaScript 无法通过 `document.cookie` 读取它，只能由浏览器自动在请求时携带。

登录成功的瞬间，`LoginService` 干了四件事（`hrm-server/src/main/java/com/qiujie/auth/service/LoginService.java:66-82`）：

```java
// 1. 生成 Access Token（15 分钟）
String accessToken = JwtUtil.generateAccessToken(staff.getId(), permissions, staff.getCode());

// 2. 写 Cookie：token，Path=/，全站可用
Cookie accessCookie = new Cookie("token", accessToken);
accessCookie.setHttpOnly(true);
accessCookie.setPath("/");              // 所有请求都带
accessCookie.setMaxAge((int) (JwtUtil.ACCESS_EXPIRATION / 1000));  // 900 秒
response.addCookie(accessCookie);

// 3. 生成 Refresh Token（7 天）
String refreshToken = JwtUtil.generateRefreshToken(staff.getId(), permissions, staffDetails);

// 4. 写 Cookie：refreshToken，Path=/refresh，只在续期端点出现
Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
refreshCookie.setHttpOnly(true);
refreshCookie.setPath("/refresh");      // 只有 /refresh 请求才带
refreshCookie.setMaxAge((int) (JwtUtil.REFRESH_EXPIRATION / 1000)); // 604800 秒
response.addCookie(refreshCookie);
```

**Path 差异是这个设计最精妙的一笔**。浏览器发送 Cookie 时严格匹配 Path：

- `token` 的 `Path=/`：任何请求（`/staff/1`、`/leave/list`、甚至 `/refresh`）都会自动带上它；
- `refreshToken` 的 `Path=/refresh`：只有调用 `/refresh` 端点时浏览器才会带上它，日常 `/staff/1` 请求里根本不会出现 `refreshToken` 这个 Cookie。

这意味着**Refresh Token 的网络暴露面被压缩到了一个端点**。哪怕某个业务接口有日志漏洞、被 XSS 注入、被中间人嗅探，`refreshToken` 都不会出现在那个请求的 Cookie 头里——它根本不在那里。

对比传统方案"前端把 token 存 localStorage、每次请求手动塞 Authorization 头"：

| 维度 | httpOnly Cookie | localStorage + Authorization |
|---|---|---|
| XSS 盗取 | 不可能（JS 读不到） | 一行 `localStorage.getItem` 就拿到 |
| CSRF 风险 | 有（浏览器自动带 Cookie） | 无（前端手动加头） |
| 跨域复杂 | 需要 `withCredentials` + CORS 配置 | 不需要 |
| 续期机制 | 浏览器自动带 refreshToken，前端无感 | 前端要存两个 token、手动管理 |

这套系统选了前者，代价是要处理 CSRF——缓解方案藏在 `SecurityConfig` 的 CORS 配置里（`hrm-server/src/main/java/com/qiujie/config/SecurityConfig.java:74`）：`allowCredentials(true)` + 明确的 `allowedOrigins`，只允许同源或显式声明的域名带 Cookie，配合 Spring Security 默认的 CSRF 防护（对非 GET 请求检查 Origin/Referer）。

## JwtAuthenticationFilter：三层降级取 Token

登录成功后，后续每个请求都要经过 `JwtAuthenticationFilter`（`hrm-server/src/main/java/com/qiujie/security/JwtAuthenticationFilter.java:47`）。它不按"Header 优先、Cookie 兜底"这种常见套路，而是**三层并行降级**：

```java
// 第一层：Authorization: Bearer <token>
String authorization = request.getHeader("Authorization");
if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
    token = authorization.substring(7);
}

// 第二层：?token=<token> 查询参数（给 SSE/下载场景用）
if (token == null) {
    String tokenParam = request.getParameter("token");
    if (StringUtils.hasText(tokenParam)) {
        token = tokenParam;
    }
}

// 第三层：Cookie 里的 token 字段
if (token == null) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }
    }
}
```

为什么要三层？因为**不是每个客户端都能自由设置 Header**：

- **Header 层**：标准的 axios/fetch 请求，前端可以在拦截器里统一加 `Authorization: Bearer xxx`；
- **Query 层**：`<a href="/api/file/download?token=xxx">` 这种浏览器直接下载的场景，没法加 Header；还有第 25 期会讲的 SSE（Server-Sent Events），`EventSource` API 也不支持自定义 Header；
- **Cookie 层**：兜底的——浏览器自动带，前端什么都不用做。

拿到 token 后的处理才是关键差异点（`JwtAuthenticationFilter.java:80-96`）：

```java
// 拒绝非 Access Token（refresh token 在这被拦下）
if (!JwtUtil.TOKEN_TYPE_ACCESS.equals(JwtUtil.extractTokenType(token))) {
    filterChain.doFilter(request, response);
    return;  // 不设置认证信息，让后面的 Security 拦截器返回 1200
}

// 从 claims 直接构建认证对象，不查数据库
Integer staffId = JwtUtil.extractStaffId(token);
List<String> permissions = JwtUtil.extractPermissions(token);
String username = JwtUtil.extractUsername(token);

StaffDetails staffDetails = new StaffDetails(staffId, username, permissions);
UsernamePasswordAuthenticationToken authToken = 
    new UsernamePasswordAuthenticationToken(staffDetails, null, staffDetails.getAuthorities());
SecurityContextHolder.getContext().setAuthentication(authToken);
```

**这里没有调用 `StaffDetailsService.loadUserByUsername()` 查库**——这是和传统 Spring Security 认证最大的区别。传统的做法是 token 解析出 username → `loadUserByUsername` 查 `sys_staff` + `per_staff_role` + `per_menu` 三表联查 → 构建 `UserDetails`。这套系统把权限列表直接塞进了 JWT claims（登录时一次性查好），过滤器里只需要解析 claims 就能构建完整的认证对象，**省下 2-3 次数据库查询**。

代价当然有：**权限变更不实时生效**。管理员在后台改了某个员工的角色，那个员工手里的 Access Token 还是旧权限，直到 15 分钟后过期刷新。这个延迟在 `/refresh` 端点被显式接受了——下面会看到 `/refresh` 重新查库拿最新权限，等于给"权限变更"设了个 15 分钟的生效窗口。

还有一个容易忽略的细节（`JwtAuthenticationFilter.java:39`）：

```java
@Override
protected boolean shouldNotFilterAsyncDispatch() {
    return false;  // SSE 的异步 dispatch 也要过这个过滤器
}
```

默认情况下 `OncePerRequestFilter` 会跳过 ASYNC dispatch 类型，但 SSE（Server-Sent Events）在长连接期间会触发多次 ASYNC dispatch，如果跳过过滤器，`SecurityContext` 是空的，后续的 `@PreAuthorize` 会全部失败。这行 `return false` 是让 SSE 请求也能带上认证信息的关键，第 26 期故障复盘会专门展开。

## /refresh 端点：换 Token 时顺便查一次"这员工还能用吗"

Access Token 过期后（15 分钟），前端会调用 `/refresh` 换新的。这个端点不只是"验证 refresh token 然后发新 token"，它还干了两件关键的事（`hrm-server/src/main/java/com/qiujie/auth/controller/LoginController.java:80-98`）：

```java
// 1. 重新查库：员工还在吗？没离职没禁用？
Staff staff = staffMapper.selectOne(new QueryWrapper<Staff>()
        .eq("code", username).eq("is_deleted", 0));
if (staff == null || staff.getStatus() != 1) {
    return Response.error("员工状态异常");  // 离职/禁用，拒绝续期
}

// 2. 重新查权限：拿到最新的权限列表
List<Menu> menus = menuMapper.queryPermission(staffId);
String permissions = menus.stream().map(Menu::getPermission).collect(Collectors.joining(","));

// 3. 生成新的 Access Token（携带最新权限）
String newAccessToken = JwtUtil.generateAccessToken(staffId, permissions, username);
Cookie accessCookie = new Cookie("token", newAccessToken);
accessCookie.setHttpOnly(true);
accessCookie.setPath("/");
accessCookie.setMaxAge((int) (JwtUtil.ACCESS_EXPIRATION / 1000));
response.addCookie(accessCookie);
```

**`/refresh` 是系统里唯一会"重新评估身份"的地方**。员工今天被离职了（`status=0` 或 `is_deleted=1`），他手里的 Access Token 还能用最多 15 分钟，但一到续期时刻就会被拒——这比"Access Token 里塞个版本号、每次请求都校验"简单得多，15 分钟的延迟窗口对这个系统是可接受的。

权限列表的重查更重要：管理员早上 9 点把张三从"普通员工"改成"人事专员"，张三 9:05 的 Access Token 里还是旧权限，9:20 过期续期后新权限才生效。**这个 15 分钟窗口是双 Token 设计的固有特性**，不是 bug——如果你的系统要求权限秒级生效，要么把 Access Token 缩到 1 分钟（续期请求暴增），要么放弃 claims 直构改回每次查库。

## 登出：两个 Cookie 一起清

`/logout` 的实现极简（`LoginController.java:107-120`）：

```java
Cookie accessTokenCookie = new Cookie("token", "");
accessTokenCookie.setHttpOnly(true);
accessTokenCookie.setPath("/");
accessTokenCookie.setMaxAge(0);        // 立即过期

Cookie refreshTokenCookie = new Cookie("refreshToken", "");
refreshTokenCookie.setHttpOnly(true);
refreshTokenCookie.setPath("/refresh");
refreshTokenCookie.setMaxAge(0);
```

`maxAge=0` 让浏览器立刻删掉 Cookie。注意 Path 必须和写入时完全一致——`token` 是 `/`，`refreshToken` 是 `/refresh`，Path 不匹配的话浏览器会认为这是两个不同的 Cookie，删不掉。

## 整条链路连起来

<!--
  时序图源码（mermaid sequenceDiagram，mermaid v12 parse 通过）。
  发布不直接用它：微信公众号的 SVG 白名单会剥掉 <defs>/<marker>，箭头会消失，
  所以导出为 PNG 引用（见下方图片）。改图时改这里的源码，重新导出即可。

sequenceDiagram
    participant U as 用户
    participant F as 前端 (hrm-admin)
    participant B as 后端 (hrm-server)
    participant DB as MySQL hrm

    U->>F: 输入账号密码 + 验证码
    F->>B: POST /login/（validateCode 路径参数）
    B->>DB: 校验验证码 (Redis)
    B->>DB: 查员工 + 权限 (staffMapper + menuMapper)
    B->>B: 生成 Access Token (15min) + Refresh Token (7d)
    B-->>F: Set-Cookie: token=xxx（Path=/，HttpOnly）<br/>Set-Cookie: refreshToken=yyy（Path=/refresh，HttpOnly）
    F-->>U: 登录成功，跳首页

    Note over F,B: 后续 15 分钟内的所有请求
    F->>B: GET /staff/1<br/>Cookie: token=xxx
    B->>B: JwtAuthenticationFilter 解析 token<br/>从 claims 直构 SecurityContext (不查库)
    B-->>F: 返回数据

    Note over F,B: 15 分钟后 Access Token 过期
    F->>B: GET /staff/1<br/>Cookie: token=xxx (已过期)
    B-->>F: Response 响应码 1200
    F->>B: POST /refresh<br/>Cookie: refreshToken=yyy (浏览器自动带)
    B->>DB: 重新查员工状态 + 最新权限
    B->>B: 生成新 Access Token
    B-->>F: Set-Cookie: token=zzz（Path=/，HttpOnly）
    F->>B: 重试 GET /staff/1<br/>Cookie: token=zzz
    B-->>F: 返回数据
-->

![双 Token 认证时序图](https://cdn.jsdelivr.net/gh/quuuuj/hrm@<完整commit SHA>/img/readme/07-auth-sequence.png)

## 动手验证：curl 看两个 Cookie

后端启动后，按顺序打三条：

```bash
# 1. 登录拿验证码（开发环境固定 1234，生产从 Redis 读）
curl -s -c cookies.txt -X POST http://localhost:8888/login/1234 \
  -H "Content-Type: application/json" \
  -d '{"code":"admin","pwd":"123456"}'

# 预期响应：{"code":200,"message":"成功","data":{...}}
# 看 cookies.txt 里应该有两行：
#   token     xxx...    Path=/
#   refreshToken  yyy...    Path=/refresh
```

打开 `cookies.txt`（Netscape 格式），确认 `refreshToken` 那行的 Path 是 `/refresh` 而不是 `/`——这是双层防护的关键。

```bash
# 2. 用 token 访问受保护接口
curl -s -b cookies.txt http://localhost:8888/staff/1
# 预期：正常返回员工数据（如果 1200 说明 token 没带上）
```

```bash
# 3. 等 15 分钟 token 过期后，测试 /refresh
# 或者手动改一个假 token 让它失败：
curl -s -b cookies.txt -X POST http://localhost:8888/refresh
# 如果 refreshToken 有效：返回 200，cookies.txt 里 token 被更新
# 如果 refreshToken 过期/无效：返回 1200
```

## 小坑提醒

- **Cookie 的 Path 必须严格匹配**：`refreshToken` 写入时 `Path=/refresh`，删除时也必须 `Path=/refresh`，否则浏览器认为是两个 Cookie 删不掉。`/logout` 里两个 `setMaxAge(0)` 的 Path 不能写反。
- **`?token=` 参数会进日志**：`JwtAuthenticationFilter` 的第二层 Query 降级是为了 SSE/下载场景，但 URL 会进 Nginx access log、浏览器历史、代理日志——这些地方都可能泄露 token。生产环境建议把这个降级关掉，或者在日志里脱敏。
- **`type` 字段不能省**：如果生成 Refresh Token 时忘了 `claims.put("type", "refresh")`，它就能通过 `JwtAuthenticationFilter` 的 type 检查，被当 Access Token 用——长寿命 token 日常请求满天飞，安全风险直接翻倍。
- **Cookie 里的 token 字段名固定是 "token"**：前端改不了，后端过滤器写死的（`JwtAuthenticationFilter.java:64`）。想改成 `access_token` 要同步改 LoginService、LoginController、JwtAuthenticationFilter 三处。

## 小结

到这一步，登录态的建立和维护链路应该清楚了：

1. **双 Token 拆开"身份"和"续期"**：Access Token 15 分钟干日常活，Refresh Token 7 天只在 `/refresh` 露面，两个 token 的 `type` claim 互相不能冒充。
2. **httpOnly Cookie + Path 隔离**：两个 token 躺在不同 Path 的 Cookie 里，浏览器自动携带但 JavaScript 读不到，`refreshToken` 的网络暴露面被压缩到一个端点。
3. **claims 直构 SecurityContext**：`JwtAuthenticationFilter` 从 token 里解析出 staffId + permissions 直接构建认证对象，不查数据库，每次请求省 2-3 次 DB 查询。
4. **`/refresh` 是唯一的重新评估点**：续期时重新查员工状态（离职/禁用拒绝）和最新权限（权限变更 15 分钟内生效），登出时两个 Cookie 一起 `maxAge=0` 清掉。

登录搞定了，下一期该看前端怎么接住这套机制——401 自动续期、并发请求不重复刷新、刷新失败跳登录页。`request.js` 里的 `isRefreshing` 标志和 `pendingRequests` 队列是下一期的主角。

**GitHub**：https://github.com/quuuuj/hrm

如果这个项目对你有帮助，欢迎 Star。
