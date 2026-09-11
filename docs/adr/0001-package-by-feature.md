# ADR 0001：后端包结构统一为按功能模块（package-by-feature）

- **状态**：已接受（2026-09-11 实施完成）
- **日期**：2026-09-11
- **相关**：GitHub issue（RFC：后端包结构统一为 package-by-feature）

## 背景

后端 `hrm-server` 的包结构长期并存三种组织风格，彼此没有明确边界：

1. **按层扁平**：`controller/`、`service/`、`mapper/`、`entity/`、`dto/`、`vo/`、`enums/`。人事核心域（Staff / Dept / Leave / Salary / Attendance / Menu / Role / Insurance / City / Docs / notification / home）全部堆在这里。
2. **按功能模块**：`chat/`、`knowledge/`，模块内自带 `controller/service/mapper/entity/dto`。
3. **深模块散件**：`overtime/`、`salarycalculation/`、`leaveapproval/`、`filetask/`、`storage/`、`attendance/`（仅 1 个类），是历次深模块重构的产物，却依附于第 1 类的 service。

这种双轨制带来三个具体问题：

- **新人无从判断新代码该放哪**，且从代码本身看不出约定，实际结果是继续加剧分裂。
- **隐蔽耦合**：`com.qiujie.service` 是一个扁平大包，同包内跨域注入**不需要 import**，因此代码评审与静态搜索都看不到。实测存在的跨域注入包括 `HomeService → StaffService/CityService/AttendanceService/DeptService`（4 个域）、`SalaryService → SalaryDeductService`、`AttendanceService → FileTaskCoordinator/FileUploadService`、`StaffOvertimeService → FileTaskCoordinator/FileUploadService/AiHeaderMatcherImpl`。
- **无法用工具约束**：模块边界只存在于命名约定里，ArchUnit 之类的规则没有可断言的切片。

另外，`service/` 包内已经有 `SalaryService` / `AttendanceService` / `StaffOvertimeService` 各自持有一组 `ImportProcessor` / `ExportProcessor` 内部类，并被 `filetask` 引擎按接口反向回调，形成运行期的双向协作；`leave` 域的审批逻辑被切在 `service/`（`StaffLeaveService`）、`leaveapproval/`、`listener/` 三个包里，环由第三方 `listener/` 包兜住。这些都要靠"包结构本身"来表达，而不是靠注释。

## 决策

采用 **package-by-feature**：顶层按业务模块平铺，模块内再分层。

### 1. 顶层模块清单（16 个业务模块，平铺不嵌套）

`staff` / `dept` / `role` / `menu` / `auth` / `salary` / `attendance` / `leave` / `overtime` / `insurance` / `city` / `notification` / `docs` / `filetask` / `chat` / `knowledge`，外加纯聚合模块 `home`。

- **从属表并入主模块**：`SalaryDeduct` 系列 → `salary/`；`StaffRole`、`RoleMenu` 系列 → `role/`。理由：它们没有独立生命周期，拆开会造成两个包永久同步修改。
- **`home/` 独立**，且是**唯一**允许向下依赖多个业务模块的模块（BFF 型聚合点）。
- **不采用两级结构**（如 `hr/staff/`）：16 个包在 IDE 中一屏可见，加一层只让 import 变长而无收益。

### 2. 模块内统一分层子包

每个模块内固定 `<module>/controller|service|mapper|entity|dto|vo/`，模块专属逻辑另开子包：

| 模块专属子包 | 内容 |
| --- | --- |
| `leave/approval/` | `ApprovalCandidateResolver`、`LeaveApprovalSideEffects(+Impl)`、`LeaveNotifier(+Impl)`、两个 Flowable `ExecutionListener` |
| `salary/calculation/` | `SalaryCalculation` |
| `attendance/batch/` | `AttendanceImportBatchProcessor` |
| `filetask/engine/` | `FileTaskEngine`、`FileTaskCoordinator`、导入导出族 |
| `filetask/store/` | `ArtifactStore`、`AsyncFileTasks`、`FileTaskRepositoryAdapter`、`MinioArtifactStore`、`TaskRepository`、`TaskSnapshot` |

命名取舍：`leave/approval/` 用 `approval` 而非 `flow`，避免包名绑死在 Flowable 上；子包名不与曾经的顶层包名（`leaveapproval`）重复，避免迁移期混淆。

`chat/`、`knowledge/` 内部已符合本约定，不返工——它们是参照系而非例外。

### 3. 横切包归位

顶层横切包只保留 `common/`、`config/`、`util/`、`security/`。

- `filter/` + `handler/` + `exception/` 合并为 `security/`。
- `annotation/` + `aspect/`（`RateLimit` + `RateLimitAspect`）合并。
- `spi/` 下沉到 `filetask/`（它本就是 filetask 的扩展点）。
- `MinioStorageService` 移到 `common/storage/`：它有 filetask / docs / knowledge 三方消费者，放任何一个业务模块都会制造反向依赖。
- **枚举全部下沉**：单域枚举进各自模块；共享枚举按主域归位——`AttendanceStatusEnum` → `attendance/enums/`、`AuditStatusEnum` → `leave/enums/`、`BusinessStatusEnum` + `BaseEnum` → `common/enums/`。
- **本轮 `util/` 内部不再细分**（含业务型的 `AiHeaderMatcherImpl`、`ActionRegistry`）。理由：`util/` 内部的分层混乱是另一个维度的坏味道，混入本轮会让验证面失控；且 `AiHeaderMatcherImpl` 被 `overtime/` 直接注入，下沉会制造新的跨模块反向依赖。留作后续独立任务。

### 4. 边界规则（ArchUnit，迁移完成后一次性引入并硬失败）

1. 分层方向：`controller → service → mapper` 单向；`entity`/`enums`/`dto`/`vo` 为被广泛读取的叶层；禁止 service 反向依赖 controller、mapper 依赖 service。
2. 模块边界：模块 A 不得访问模块 B 的 `controller/` 及模块专属内部子包（`leave/approval/`、`salary/calculation/`、`filetask/engine|store/`）；允许访问 B 的 `entity`/`enums`/`dto`/`vo`/`service`。
3. 模块间零环：`slices("com.qiujie.(*)..").should().beFreeOfCycles()`。
4. 横切包（`util/`、`config/`、`common/`、`security/`）不得依赖任何业务模块的 `controller` 或 `service`。

**关键原则：白名单必须为空。** 如果实现时发现某条规则不得不加例外，说明模块划分有问题，应当回头修改划分，而不是给规则开口子。这是本决策能被长期守住的前提。

`dto`/`vo` 的跨模块使用**不禁止**：`StaffDeptVO` 这类聚合视图本就会被多域读取，禁止会逼出大量无意义的 VO 复制。

### 5. 过渡期配置（新旧并存）

`type-enums-package`（`application.yml`、`application-prod.yml`）与 `@MapperScan`（`HrmApplication`）都写死了包名，必须跟随迁移：

- 在**第一个移动枚举的 commit** 中即把 `type-enums-package` 改为 `com.qiujie.enums,com.qiujie.*.enums`（新旧并存）。
- `@MapperScan` 同理，先加通配、后删旧包名。
- 全部迁移完成后的收尾 commit 再删除旧包名。

这样任何单次 commit 都不会出现"配置漏了某个包"的中间态。注意：枚举扫描失效是**运行期静默**故障——surefire 只跑 `**/*UnitTest.java`（21 个，全为 Mockito 零上下文测试，不经过 MyBatis 的枚举 TypeHandler），因此 `mvn test` 发现不了；必须用 `mvn verify`（failsafe 跑 `**/*IntegrationTest.java`，需本地数据库）并启动应用验证。`service/AttendanceExportIntegrationTest` 读库后断言 `TaskStatusEnum.SUCCESS`，是现成的回归网。

## 后果

**收益**

- 新代码落点由结构本身决定，不再需要口头约定；消除"分层还是模块"的选择题。
- 跨域依赖从"同包隐式"变成"跨包显式 import"，耦合可见、可评审、可约束。
- 具备引入 ArchUnit 硬约束的前提，模块边界从约定升级为机械保证。
- 原有深模块产物（`leaveapproval`、`salarycalculation` 等）归位到业务域，假跨模块抽象被消除——实测 `leaveapproval/` 的引用者只有 `listener/` 与 `StaffLeaveService`，零外部使用者。

**代价与风险**

- 几乎所有 Java 文件被重命名/移动，是最难评审的一类 diff；因此按模块逐个 commit 逐步验证。
- 测试端波及面大：30 个测试文件中，仅 `com.qiujie.service` 的拆分就影响 11 个；若 `entity`/`mapper`/`enums` 一并移动（测试 import 计数：`entity` 31 次、`enums` 22 次、`mapper` 17 次），几乎全部受影响。
- 两个写死包名的配置点必须同步修改，否则运行期静默失效。

## 备选方案（未采用）

- **统一回传统分层**：把 `chat/`、`knowledge/` 拆散回到扁平层。与历次深模块重构方向相反，且放弃了模块化的全部收益。
- **混合但立规矩**：保留分层包，只把散件归位。改动最小，但双轨制仍在，问题不解决。
- **模块内扁平平铺**：小模块清爽，但 `filetask/`（20 个类）、`salary/` 退化成一锅粥，且 `chat/`、`knowledge/` 需反向拆散。
- **模块内"按规模选择分层或扁平"**：把已消灭的双轨制换个尺度重新引入，每个模块都要吵一次阈值，且 ArchUnit 规则必须按模块分支，等于约束失效。

## 迁移顺序（依赖自底向上）

`staff → dept → menu → role → city → insurance → auth → notification → docs → salary → attendance → overtime → leave → filetask → chat → knowledge → home`

每个模块一个 commit；横切包归位、死代码与空包清理、ArchUnit 引入各自独立 commit。

前置步骤（实施时修正）：基线实为 `dev`/`origin/dev`（当前分支落后 0 / 领先 3）。原文所述"与 master 分叉：领先 150 / 落后 43"不成立——本分支与 `master` 根提交不相干（unrelated histories），不存在可合入的公共基线，故跳过合 master 步骤。

顺带清理：删除死代码 `common/llm/LlmProvider`（全仓零引用，javadoc 仍指向早已并入 `chat/` 的 `assistant/`）；清空空目录 `knowledge/controller`、`knowledge/spi`、测试侧 `assistant/store`。

## 明确不做

- 前端（`hrm-admin/`）零改动。
- 无数据库 schema 变更、无 API 路径变更。
- `util/` 内部不细分。
- `CONTEXT.md` 暂不创建（本次决策由本 ADR 承载）。
