# 《从零手写一套数智人事系统》系列总纲

> 本文件是这套 28 期系列教程的唯一规范。写每期正文前先对照本纲，防止跨期漂移。

## 一、定位与读者

- **读者**：普通全栈/后端开发者，能看懂 Spring Boot 和 Vue 基础代码，没做过完整中型项目。
- **目标**：让读者读完后知道这个项目是怎么从零做到完成的——不是"看懂了代码"，而是"每一期我也能做出来"。
- **双用途**：公众号文章 + 视频教程基础文档。文章保持"讲解真实项目"的定位；视频的"手把手敲"在拍摄时以文章为大纲现场展开，不要求文章承载完整渐进实现。

## 二、规模与节奏

- **28 期**，按真实开发工序线性排列（环境 → 骨架 → 地基 → 权限 → 业务 → 工作流 → 文件 → AI → 收尾）。
- 单期 **2500–3000 字**（第 1 期例外，3000–3500，要交代项目背景）。
- 一天写一篇正文，不批量生成；本纲一次定死。

## 三、每期固定骨架

```
开篇钩子（2-3 句场景/问题，不讲大道理）
→ 动手前提（一行：想跟着敲需要完成第几期；纯阅读可跳过）
→ 本期目标（一句话可验证产出）
→ 实现过程（为什么这么做 → 关键代码 → 效果验证）
→ 小坑提醒（若有，点到为止，细节统一留给第 26 期）
→ 小结 + 下期预告（一句话勾住下一期）
```

- 正文自解释：该讲的概念当期内讲清，不让读者必须读前文才能懂。
- "动手前提"只标依赖，不重复前文内容。

## 四、内容规则

### 代码三层策略
| 类型 | 处理方式 |
|---|---|
| 配置/脚手架（compose、yml、拦截器） | 给可照抄的完整片段 |
| 核心业务逻辑（OvertimeCalculator、检索管线） | 贴真实源码关键段 + 讲结构，价值在思路不在字符 |
| 坑点（shouldNotFilterAsyncDispatch、NUL 字节） | 当期一句带过，细节归第 26 期 |

### 叙事人称
- 讲决策/取舍："我"（"我当时选了 Flowable，因为……"）
- 带读者操作："你"/祈使句（"把这段配置加进 application.yml"）

### 术语统一
- **首次出现的那期**用一句话给定义，之后各期直接用不重复解释。
- 固定译名：businessKey → "业务键"；side-effect → "副作用"；伪流式就叫"伪流式"不叫"模拟流式"；chunk → "切块"；RRF 保留英文缩写 + 首次出现给中文解释。
- 每期引入新概念 ≤3 个；概念密集的期把伏笔铺到前一期（如"为什么切块要记顺序"在第 23 期埋，第 24 期用）。

### 事实红线
以代码为唯一依据。已确认的三处 README/旧草稿错误**不得再犯**：
1. 加班**不走** Flowable——只有请假走引擎（`processes/` 下仅 `leave.bpmn20.xml`）。
2. 检索是**混合检索**（向量 + 关键词 RRF 融合 + 邻窗扩展 + 证据分级），不是"纯语义向量相似度检索"。
3. 问答是**伪流式**（LLM 同步整段返回后逐字推送），不是"流式/同步反馈"。

## 五、发布链路

- 文章存放：`docs/article/`（已放行 gitignore）；新截图：`img/article/<文章名>/`。
- 编辑器：**doocs/md**（https://md.doocs.org），支持 mermaid；mdnice 不支持，不用。
- 图片：统一 `https://cdn.jsdelivr.net/gh/quuuuj/hrm@<已推送的完整 commit SHA>/<path>`，钉死 SHA 不用分支；写完 `curl -o /dev/null -w "%{http_code}"` 逐个验证。
- 顶部用 HTML 注释块放标题候选/摘要/封面建议，注释不渲染；公众号无 front matter，别写。
- 每篇写完在 doocs/md 实测排版，重点看段落间距、图片、mermaid 渲染。

## 六、28 期期表

> 列：期 / 标题方向 / 本期产出（可验证）/ 核心素材 / 图表预算 / 动手前提 / 引入术语

### 阶段一 · 起手（1–4）

| 期 | 标题方向 | 产出 | 核心素材 | 图表 | 前提 | 新术语 |
|---|---|---|---|---|---|---|
| 1 | 项目介绍与技术选型 | 知道要做一套什么系统 | tmp/wechat-article.md 改造；README 架构图；技术栈表 | 架构图 + 页面截图组 | 无 | 数智人事、RAG |
| 2 | 环境：一键起中间件 | docker/local 起 MySQL/Redis/PG/MinIO + 三库导入 | docker/local/docker-compose.yml；db/ 三个 sql | compose 结构示意 | 1 | 中间件编排 |
| 3 | 后端骨架 | Spring Boot 能启动、swagger-ui 可访问 | pom.xml（parent/BOM/surefire/failsafe 分工）；application.yml 分块；spring.config.import 读 .env | 配置分层图 | 2 | BOM、.env 敏感项 |
| 4 | 前端骨架 | npm run serve 起、代理调通后端 | vue.config.js 代理；src/api 分文件；src/utils/request.js 雏形；Vuex 五模块 | 前后端调用链 | 3 | 代理、Vuex 模块 |

### 阶段二 · 地基（5–7）

| 期 | 标题方向 | 产出 | 核心素材 | 图表 | 前提 | 新术语 |
|---|---|---|---|---|---|---|
| 5 | 公共层 | ResponseDTO/Response、BaseExceptionHandler、BaseEnum | common/dto；security/BaseExceptionHandler（@ControllerAdvice）；@EnumValue+@JsonValue 落库/序列化 | 统一响应结构 | 3 | 统一响应、BaseEnum |
| 6 | 表结构设计 | 理解 25+55+4 张三库表怎么分、为什么这么分 | hrm.sql 前缀分组（att_/chat_/file_/kb_/per_/sal_/soc_/sys_）；无物理外键靠逻辑约束；联合唯一索引；PG 侧 vector(1024)+hnsw；无 Flyway/Liquibase 直接改全量 sql | 三库分工图 + 表分组树 | 3 | 逻辑外键、种子数据 |
| 7 | 认证：双 Token | 能登录、后续请求自动带身份 | JwtUtil（15min/7d）；LoginService 写两个 Cookie（Path=/、Path=/refresh）；JwtAuthenticationFilter 三层取 token + claims 直构 SecurityContext | 双 Token 时序图 | 5,6 | Access/Refresh Token、httpOnly Cookie |

### 阶段三 · 权限（8–10）

| 期 | 标题方向 | 产出 | 核心素材 | 图表 | 前提 | 新术语 |
|---|---|---|---|---|---|---|
| 8 | 前端无感续期 | 401 自动刷新、并发请求不重复刷新 | request.js isRefreshing + pendingRequests 并发去重；/refresh 端点重查员工状态+最新权限 | 并发去重刷新时序 | 7 | 并发去重刷新 |
| 9 | RBAC 表与后端校验 | 接口能按权限点拦住 | per_menu/per_role/per_role_menu/per_staff_role；StaffDetailsService 装载权限进 JWT claim；@PreAuthorize 校验链 | RBAC 四表关系 | 7 | RBAC、权限点 |
| 10 | 动态路由与 v-permission | 菜单按权限生成、按钮按权限显隐 | router/index.js 动态加载；directive/permission；permission store | 登录→菜单→路由链路 | 9 | 动态路由 |

### 阶段四 · 基础模块（11–13）

| 期 | 标题方向 | 产出 | 核心素材 | 图表 | 前提 | 新术语 |
|---|---|---|---|---|---|---|
| 11 | CRUD 范式（员工为例） | 一套可复用的增删改查模板 | StaffController/Service/Mapper；ServiceImpl 无接口；QueryWrapper 动态条件；@Select 注解多表关联；逻辑删除 | CRUD 分层图 | 9 | 逻辑删除 |
| 12 | 组织与权限维护 | 部门树、角色分配、菜单配置可维护 | dept 树形查询；role-menu 关联；菜单管理 | 部门树 | 11 | — |
| 13 | 考勤与打卡 | 日历/统计/图表能看 | att_attendance；AttendanceStatusEnum；ECharts 仪表盘；节假日 HolidayConfig | 考勤日历 | 11 | — |

> **可砍预案**：12、13 素材不足时并进 11（"CRUD 范式"一期带三个模块的共性）；收尾三期（26–28）与文件异步（19–21）是差异化内容，**不砍**。

### 阶段五 · 工作流与业务计算（14–18）

| 期 | 标题方向 | 产出 | 核心素材 | 图表 | 前提 | 新术语 |
|---|---|---|---|---|---|---|
| 14 | 请假单据建模 | 单据能提交、余额/类型/日期区间建模清楚 | att_staff_leave/att_leave；LeaveEnum/AuditStatusEnum；余额校验 | 请假单状态机 | 11 | — |
| 15 | Flowable 落地 | 流程能流转 | leave.bpmn20.xml 全量；DataSourceConfig 三数据源；businessKey=staff_leave.id；ExecutionListener on sequenceFlow | BPMN 流程图 + 双数据源 | 14 | BPMN、业务键 |
| 16 | 监听器分层与副作用端口 | 审批通过自动改考勤/调休 | LeaveApprovalSideEffects 端口+Impl；HrApproveListener/ManagerApproveListener 只做透传；ApprovalCandidateResolver；逐日遍历跳过周末节假日；LeaveNotifierImpl→SSE | 审批副作用链 | 15 | 副作用端口 |
| 17 | 加班核算 | 加班费算得对 | OvertimeCalculator.computeFromReferences 纯静态；调用方预取 staffCache/configCache/salaryCache；零 mock 单测 | 计算输入输出 | 13 | 纯计算深模块 |
| 18 | 薪资与五险一金 | 薪资金额算得对 | SalaryCalculation final+私有构造+static compute；DeductEnum；soc_city/soc_insurance | 薪资构成拆解 | 17 | — |

### 阶段六 · 文件与异步（19–21）

| 期 | 标题方向 | 产出 | 核心素材 | 图表 | 前提 | 新术语 |
|---|---|---|---|---|---|---|
| 19 | MinIO 对象存储 | 附件能上传下载 | common/storage/MinioStorageService；composeObject 零拷贝合并；MinioArtifactStore | 存储链路 | 5 | 对象存储 |
| 20 | 分片上传 | 大文件能传、秒传、断点续传 | kb_upload_session/kb_upload_chunk；256MB/10MB/24h TTL；分片三阶段 | 分片上传时序 | 19 | 秒传、断点续传 |
| 21 | FileTaskEngine 异步任务 | 批量数据能异步进出 | fileTaskExecutor（core4/max8/queue50/CallerRunsPolicy/@Primary 三处复用）；CAS claim；SPI processor；file_task_error 错误产出 | 任务引擎状态机 | 19 | CAS 抢占 |

### 阶段七 · AI（22–25）

| 期 | 标题方向 | 产出 | 核心素材 | 图表 | 前提 | 新术语 |
|---|---|---|---|---|---|---|
| 22 | 知识库建模与文档生命周期 | 文档有状态可管理 | kb_document/document_chunk/kb_qa_record（MySQL+PG 镜像表）；DocumentLifecycleService | 文档生命周期状态机 | 6 | 镜像表 |
| 23 | 摄入流水线 | 文档能变向量 | IngestionPipeline 三阶段（Claim/Etl/Settle）；StartupRecovery 崩溃恢复；chunk max-size=1000/overlap=100/len/2.5 token 估算；ChunkService 段落边界切分；DashScope batch=10 | 三阶段流水线 | 22 | ETL、切块、overlap（埋"为什么记顺序"伏笔） |
| 24 | pgvector 与混合检索 | 能召回准确片段 | vector(1024)+hnsw+vector_cosine_ops；HybridRetrievalService 向量+关键词 RRF；top-k/rrf-k=60/window-size=1 邻窗扩展；EvidenceAssessmentService 四级分级 | 混合检索流程 | 23 | RRF、邻窗、证据分级 |
| 25 | 智能问答工程化 | 答疑闭环 | ChatController SseEmitter；ChatQaService 伪流式逐字推+SecurityContext 手动透传+员工上下文注入；IntentGate 单 prompt 分类+改写；JdbcChatSessionStore 会话存 MySQL 游标分页 | 问答链路图 | 24 | 伪流式、意图分流 |

### 阶段八 · 收尾（26–28）

| 期 | 标题方向 | 产出 | 核心素材 | 图表 | 前提 | 新术语 |
|---|---|---|---|---|---|---|
| 26 | 三个真实故障复盘 | 知道这套系统的代价在哪 | ①shouldNotFilterAsyncDispatch()=false 修 SSE ASYNC dispatch SecurityContext 为空；②PG 侧 autocommit 不可回滚；③NUL 字节致 PG 落库失败 TextCleanupService 剥离 | 三个故障时间线 | 16,23 | — |
| 27 | 架构复盘 | 知道为什么这么分层 | ADR 0001 package-by-feature；深模块重构模式（OvertimeCalculator/LeaveApprovalSideEffects/SalaryCalculation/ChatSessionStore/AssistantLlm）；ArchitectureUnitTest 架构守护 | 包结构图 | 全文 | package-by-feature |
| 28 | 部署上线与总结 | 知道怎么发到服务器 | docker 网络、生产 compose 思路（不泄露 deploy/ 细节与公网信息）；全系列回顾 | 部署拓扑 | 27 | — |

## 七、素材分配备忘

- **截图缺口**：第 1 期智能问答截图（`14-smart-qa.png`）是旧 `views/chat/` 界面，现为 `views/qa/chat/`——**发布前需重截一张新界面**，替换同名文件或放 `img/article/01-项目介绍/`。
- **每期代码引用**统一带 `file:line` 可点击路径（如 `hrm-server/.../OvertimeCalculator.java:117`），方便读者对照仓库。
- **mermaid 风格**：以第 2 期 docker-compose 拓扑图为视觉基准——`flowchart TD`、扁平分层不用 subgraph、圆角框节点 `["..."]`、存储卷圆柱 `[(...)]`、标签短句 + `<br/>` 换行、一行一条连线、同层扇出；架构图可用 `graph TB`，同一画风；doocs/md 实测渲染。
- **图表预算**：每期 mermaid ≤2 张、截图 ≤4 张，避免移动端刷屏。

## 八、变更记录

| 日期 | 内容 |
|---|---|
| 2026-09-25 | 初版：28 期线性结构定稿（曾走过"深挖 15 + 骨架 12"双线方案，已废弃） |
