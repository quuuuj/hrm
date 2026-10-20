# 数智人事系统（七）：双 Token 与 httpOnly Cookie 认证

上一期理清了 84 张表的分工，本期进入认证。本系统的认证采用**双 Token + httpOnly Cookie** 组合：一个 15 分钟的 Access Token 用于日常请求，一个 7 天的 Refresh Token 用于续期，两个 token 分别存放在不同 Path 的 Cookie 中。

单 Token 存在一组难以兼顾的矛盾：有效期短则用户体验差（每 15 分钟需重新登录），有效期长则泄露风险大（被盗后可使用 7 天）。双 Token 将该矛盾拆开——短寿命 token 承担日常认证，长寿命 token 仅在续期时使用，且只出现在一个专用端点上。

## 动手前提

需完成第 5 期：`hrm-server` 能启动，`ResponseDTO` 统一响应已就位。本期需要前端配合观察 Cookie 效果，但验证环节仅用 `curl` 即可完成。

## 本期目标

登录成功后浏览器收到两个 httpOnly Cookie（`token` 与 `refreshToken`），后续请求自动携带 `token`；`token` 过期后能用 `refreshToken` 换新的；`JwtAuthenticationFilter` 能从 Header、Query、Cookie 三层取到 token 并直接构建认证对象。

## 双 Token：身份与续期资格分离

本期第一个术语：**双 Token（Access/Refresh Token）** 指系统同时签发两个不同寿命、不同用途的 JWT——短命的 Access Token 用于日常 API 调用，长命的 Refresh Token 只用于换取新的 Access Token。

`hrm-server/src/main/java/com/qiujie/util/JwtUtil.java:26` 中两个寿命常数固定了该分工：

```java
public final static long ACCESS_EXPIRATION = 15 * 60 * 1000;        // 15 分钟
public final static long REFRESH_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 天
```

15 分钟与 7 天对应两个威胁模型的折中：

- **Access Token 15 分钟**：假设其泄露（XSS、中间人、日志泄露），攻击者最多使用 15 分钟。对人事系统而言，15 分钟可查看部分员工信息，但不足以导出全部数据——损失可控。
- **Refresh Token 7 天**：假设用户每天使用一次系统，一周内无需重新登录；超过 7 天不使用则强制重新认证，避免出现“永久在线”的闲置会话。

两个 Token 的载荷（claims）相同——`staffId`、`permissions`、`username`、`type`——区别只在 `type` 字段与签名时间（`JwtUtil.java:92-116`）：

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

`type` 字段是关键防线：**Refresh Token 不能作为 Access Token 使用**。下文可见，`JwtAuthenticationFilter` 明确拒绝 `type != "access"` 的 token，即使其未过期且签名正确。

## httpOnly Cookie：前端代码无法读取 Token

第二个术语：**httpOnly Cookie** 指设置了 `HttpOnly` 标志的浏览器 Cookie，JavaScript 无法通过 `document.cookie` 读取，只能由浏览器在请求时自动携带。

登录成功时，`LoginService` 执行四个步骤（`hrm-server/src/main/java/com/qiujie/auth/service/LoginService.java:66-82`）：

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

**Path 差异是该设计的核心**。浏览器发送 Cookie 时严格匹配 Path：

- `token` 的 `Path=/`：任何请求（`/staff/1`、`/leave/list`、`/refresh`）都会自动携带它；
- `refreshToken` 的 `Path=/refresh`：只有调用 `/refresh` 端点时浏览器才会携带它，日常 `/staff/1` 请求中不会出现 `refreshToken`。

这意味着 **Refresh Token 的网络暴露面被限制在一个端点**。业务接口存在日志漏洞、XSS 注入或中间人嗅探时，`refreshToken` 都不会出现在对应请求的 Cookie 头中。

对比“前端把 token 存 localStorage、每次请求手动加 Authorization 头”的方案：

| 维度 | httpOnly Cookie | localStorage + Authorization |
|---|---|---|
| XSS 盗取 | 不可能（JS 读不到） | 一行 `localStorage.getItem` 即可获取 |
| CSRF 风险 | 有（浏览器自动带 Cookie） | 无（前端手动加头） |
| 跨域复杂 | 需要 `withCredentials` + CORS 配置 | 不需要 |
| 续期机制 | 浏览器自动带 refreshToken，前端无感 | 前端要存两个 token、手动管理 |

本系统选择前者，代价是需要处理 CSRF——缓解方案在 `SecurityConfig` 的 CORS 配置中（`hrm-server/src/main/java/com/qiujie/config/SecurityConfig.java:74`）：`allowCredentials(true)` + 明确的 `allowedOrigins`，只允许同源或显式声明的域名携带 Cookie，配合 Spring Security 默认的 CSRF 防护（对非 GET 请求检查 Origin/Referer）。

## JwtAuthenticationFilter：三层降级取 Token

登录成功后，每个请求都经过 `JwtAuthenticationFilter`（`hrm-server/src/main/java/com/qiujie/security/JwtAuthenticationFilter.java:47`）。该过滤器按三层并行降级取 token：

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

三层的原因是不同客户端设置 Header 的能力不同：

- **Header 层**：标准 axios/fetch 请求，前端可在拦截器中统一加 `Authorization: Bearer xxx`；
- **Query 层**：`<a href="/api/file/download?token=xxx">` 这类浏览器直接下载的场景无法加 Header；第 24 期的 SSE（Server-Sent Events）中，`EventSource` API 也不支持自定义 Header；
- **Cookie 层**：兜底——浏览器自动携带，前端无需处理。

拿到 token 后的处理是关键差异点（`JwtAuthenticationFilter.java:80-96`）：

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

**该处没有调用 `StaffDetailsService.loadUserByUsername()` 查库**——这是与传统 Spring Security 认证的主要区别。传统做法是 token 解析出 username → `loadUserByUsername` 联查 `sys_staff` + `per_staff_role` + `per_menu` 三表 → 构建 `UserDetails`。本系统在登录时将权限列表一次性写入 JWT claims，过滤器只需解析 claims 即可构建完整认证对象，每次请求省去 2-3 次数据库查询。

代价是**权限变更不实时生效**。管理员修改某员工的角色后，该员工手中的 Access Token 仍持旧权限，直到 15 分钟后过期刷新。该延迟在 `/refresh` 端点被显式接受——`/refresh` 重新查库获取最新权限，权限变更的生效窗口为 15 分钟。

另有一个细节（`JwtAuthenticationFilter.java:39`）：

```java
@Override
protected boolean shouldNotFilterAsyncDispatch() {
    return false;  // SSE 的异步 dispatch 也要过这个过滤器
}
```

默认情况下 `OncePerRequestFilter` 会跳过 ASYNC dispatch 类型，而 SSE 在长连接期间会触发多次 ASYNC dispatch——若跳过过滤器，`SecurityContext` 为空，后续 `@PreAuthorize` 全部失败。`return false` 使 SSE 请求也能携带认证信息，第 25 期故障复盘会展开。

## /refresh 端点：换 Token 时重新校验员工状态

Access Token 过期后（15 分钟），前端调用 `/refresh` 换新的。该端点除“验证 refresh token 并签发新 token”外还执行两项校验（`hrm-server/src/main/java/com/qiujie/auth/controller/LoginController.java:80-98`）：

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

**`/refresh` 是系统中唯一重新评估身份的位置**。员工离职后（`status=0` 或 `is_deleted=1`），其 Access Token 仍可使用最多 15 分钟，但续期时刻即被拒绝——相比“Access Token 里存版本号、每次请求都校验”，15 分钟的延迟窗口更简单，对该系统也足够。

权限列表的重查同理：管理员将张三的角色从“普通员工”改为“人事专员”后，张三当前的 Access Token 仍持旧权限，续期后新权限才生效。**15 分钟窗口是双 Token 设计的固有特性**。若系统要求权限立即生效，方案是把 Access Token 缩短至 1 分钟（续期请求增多），或放弃 claims 直构、改回每次查库。

## 登出：清空两个 Cookie

`/logout` 的实现（`LoginController.java:107-120`）：

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

`maxAge=0` 使浏览器立即删除 Cookie。Path 必须与写入时完全一致——`token` 是 `/`，`refreshToken` 是 `/refresh`，Path 不匹配时浏览器会视为两个不同 Cookie，导致删除失败。

## 整条链路

![双 Token 认证时序图](https://cdn.jsdelivr.net/gh/quuuuj/hrm@e5a65d28c2c36336a1eaf3de3339b430cbff3597/img/article/07-auth-sequence.png)

## 动手验证：curl 观察两个 Cookie

后端启动后，依次执行三条命令：

```bash
# 1. 登录拿验证码（开发环境固定 1234，生产从 Redis 读）
curl -s -c cookies.txt -X POST http://localhost:8888/login/1234 \
  -H "Content-Type: application/json" \
  -d '{"code":"admin","pwd":"123"}'

# 预期响应：{"code":200,"message":"成功","data":{...}}
# 看 cookies.txt 里应该有两行：
#   token     xxx...    Path=/
#   refreshToken  yyy...    Path=/refresh
```

打开 `cookies.txt`（Netscape 格式），确认 `refreshToken` 行的 Path 是 `/refresh` 而非 `/`。

```bash
# 2. 用 token 访问受保护接口
curl -s -b cookies.txt http://localhost:8888/staff/1
# 预期：正常返回员工数据（1200 说明 token 未携带）
```

```bash
# 3. 等 15 分钟 token 过期后，测试 /refresh
# 或者手动改一个假 token 让它失败：
curl -s -b cookies.txt -X POST http://localhost:8888/refresh
# refreshToken 有效：返回 200，cookies.txt 里 token 被更新
# refreshToken 过期/无效：返回 1200
```

## 小坑提醒

- **Cookie 的 Path 必须严格匹配**：`refreshToken` 写入时 `Path=/refresh`，删除时也必须 `Path=/refresh`，否则浏览器视为两个 Cookie 删不掉。`/logout` 中两个 `setMaxAge(0)` 的 Path 不能写反。
- **`?token=` 参数会进日志**：`JwtAuthenticationFilter` 第二层 Query 降级是为 SSE/下载场景设计，但 URL 会进 Nginx access log、浏览器历史、代理日志——这些位置都可能泄露 token。生产环境建议关闭该降级，或在日志中脱敏。
- **`type` 字段不能省**：生成 Refresh Token 时若遗漏 `claims.put("type", "refresh")`，它就能通过 `JwtAuthenticationFilter` 的 type 检查被当 Access Token 使用——长寿命 token 出现在日常请求中，安全风险显著增加。
- **Cookie 里的 token 字段名固定是 “token”**：前端改不了，后端过滤器写死（`JwtAuthenticationFilter.java:64`）。改成 `access_token` 需同步修改 LoginService、LoginController、JwtAuthenticationFilter 三处。

## 小结

登录态的建立与维护链路如下：

- **双 Token 分离身份与续期**：Access Token 15 分钟承担日常认证，Refresh Token 7 天仅在 `/refresh` 使用，两个 token 的 `type` claim 互相不能冒充；
- **httpOnly Cookie + Path 隔离**：两个 token 位于不同 Path 的 Cookie 中，浏览器自动携带但 JavaScript 读不到，`refreshToken` 的网络暴露面被限制在一个端点；
- **claims 直构 SecurityContext**：`JwtAuthenticationFilter` 从 token 解析 staffId 与 permissions 直接构建认证对象，不查数据库，每次请求省 2-3 次 DB 查询；
- **`/refresh` 是唯一的重新评估点**：续期时重新查员工状态（离职/禁用拒绝）与最新权限（权限变更 15 分钟内生效），登出时两个 Cookie 一起 `maxAge=0` 清除。

登录完成后，下一期进入前端对该机制的对接——401 自动续期、并发请求不重复刷新、刷新失败跳登录页。`request.js` 中的 `isRefreshing` 标志与 `pendingRequests` 队列是下一期的重点。

**GitHub**：https://github.com/quuuuj/hrm
