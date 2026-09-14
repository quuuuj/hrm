# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HRM (Human Resource Management) system - a full-stack application with separate backend (Java/Spring Boot) and frontend (Vue.js) modules, integrating RAG knowledge base and AI assistant.

## Build & Run Commands

### Backend (hrm-server/)
```bash
cd hrm-server
mvn test                           # Run all tests (unit + integration)
mvn test -Dtest=*UnitTest          # Run only unit tests
mvn test -Dtest=*IntegrationTest   # Run only integration tests
mvn clean package -DskipTests      # Build JAR without tests
mvn spring-boot:run                # Run dev server (port 8888)
```

### Frontend (hrm-admin/)
```bash
cd hrm-admin
npm ci                             # Install dependencies
npm run serve                      # Dev server (proxies /api to backend)
npm run build                      # Production build
npm run lint                       # ESLint check
```

### Infrastructure
```bash
docker compose -f docker/local/docker-compose.yml up -d   # Start local middleware
```

Required services: MySQL (3306), Redis (6379), PostgreSQL/pgvector (5432). Import schema from `db/mysql/hrm.sql`, `db/mysql/hrm_flowable.sql` and `db/postgresql/knowledge_base.sql`.

## Architecture

### Backend (Spring Boot 3.4.4, Java 17)
- **Package**: `com.qiujie`
- **ORM**: MyBatis-Plus 3.5.10 (`mybatis-plus-spring-boot3-starter`) with 3 datasources:
  - `master` — MySQL, main app data (primary)
  - `flowable` — MySQL, Flowable workflow engine (separate DB)
  - `kb` — PostgreSQL/pgvector, knowledge base vectors
- **Auth**: Spring Security + JWT (jjwt 0.11.5) with httpOnly Cookie dual-token refresh (Access + Refresh Token)
- **API Docs**: SpringDoc OpenAPI (replaces Swagger), available at `/swagger-ui.html`
- **Workflow**: Flowable 8.0 (Activiti fork, native Spring Boot 3 support) for leave/overtime approval
- **AI**: Spring AI 1.0.0-M6 — Ollama for chat/embedding, pgvector for vector store
- **Storage**: MinIO S3-compatible object storage (`io.minio:minio:8.5.7`)
- **Layers**: Controller → Service → Mapper (extends BaseMapper)
- **Response format**: `ResponseDTO` via `Response.success()`/`Response.error()` helper
- **Config**: `application.yml` (common) + env-variable-driven profile switching
- **Key packages**:
  - `assistant/` — OpenAI-compatible LLM adapter for employee self-service Q&A; read-only, current-user scoped, backed by allowlisted service/mapper tools
  - `knowledge/` — RAG knowledge base module (documents, chunks, embeddings, retrieval)
  - `storage/` — MinIO file storage service
  - `entity/` — Domain objects (Staff, Dept, Role, Menu, Salary, Attendance, Leave, Overtime, etc.)
  - `enums/` — MyBatis-Plus enums implementing `BaseEnum`, auto-registered via `type-enums-package`
  - `filter/` — `JwtAuthenticationFilter` runs before Spring Security's auth filter
  - `listener/` — Flowable task listeners for approval workflows
  - `config/` — Security, Redis, MyBatis-Plus, DataSource, Holiday configs
  - `spi/` — Service Provider Interface extensions

### Frontend (Vue 2.6, Element UI 2.15)
- **Build**: Vue CLI 4.5
- **State**: Vuex with modules (staff, menu, permission, token, tag)
- **Routing**: Dynamic routes loaded from backend menu data; static route only for `/login`
- **API layer**: `/src/api/` — one file per resource, uses axios instance from `/src/utils/request.js`
- **Auth flow**: httpOnly Cookie-based dual-token mechanism; Access Token in Cookie with Path=/; Refresh Token in Cookie with Path=/refresh; automatic silent refresh on 401/1200; concurrent request queueing during refresh
- **Proxy**: `/api` prefix → backend (configured in `vue.config.js` via env vars)
- **Permissions**: Custom directive `v-permission` checks against permission store
- **New views**: Knowledge base (`views/knowledge/`), AI assistant chat

### Key Domain Entities
- **Staff** — employee with role assignments (StaffRole) and menu permissions (via RoleMenu)
- **Leave/Overtime** — approval workflows processed through Flowable
- **Salary/SalaryDeduct** — payroll with deduction types
- **Attendance** — attendance tracking with status enums
- **Menu/Role** — RBAC permission system, menus define both navigation and route components
- **Assistant** — employee self-service Q&A backed by LLM with allowlisted tools
- **Knowledge** — RAG document ingestion, chunking, embedding, and vector search

## Database
- Two MySQL databases: `hrm` (app data) and `hrm_activiti` (Flowable engine)
- One PostgreSQL database for knowledge base vectors (pgvector extension required)
- Schema files: `db/mysql/hrm.sql` (all app tables, including assistant + file_task + knowledge), `db/mysql/hrm_flowable.sql` (workflow engine), `db/postgresql/knowledge_base.sql` (KB vectors)
- MyBatis-Plus mappers use annotation-based SQL (no XML mapper files)

## Testing
- Unit tests: `*UnitTest.java` (maven-surefire-plugin 3.5.3)
- Integration tests: `*IntegrationTest.java` (maven-failsafe-plugin 3.5.3)
- CI requires secrets: `CI_DB_PASSWORD`, `CI_JWT_SECRET`

## Development Notes
- SpringDoc OpenAPI UI available at `/swagger-ui.html` when running
- Backend uses `@EnableScheduling` for scheduled tasks
- File upload max: 20MB per file, 30MB per request
- Frontend `npm run serve` requires `NODE_OPTIONS=--openssl-legacy-provider` (already configured in scripts)
- LLM/Assistant requires `ASSISTANT_PROVIDER_BASE_URL`, `ASSISTANT_PROVIDER_API_KEY`, `ASSISTANT_PROVIDER_MODEL`
- Knowledge base requires PostgreSQL with pgvector extension; set `KNOWLEDGE_ENABLED=true`
- Ollama embedding defaults to `nomic-embed-text`, chat to `minimax-m3:cloud`
- MinIO storage: configure `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`
- Production deployment via Docker Compose (`deploy/docker-compose.server.yml`); uses external Docker network `my_network`

## Deep-Module Refactor Playbook

When running `/improve-codebase-architecture` (or any refactor request), follow this established pattern — it is the project's proven approach (OvertimeCalculator, LeaveApprovalSideEffects, SalaryCalculation, ChatSessionStore, AssistantLlm):

### 1. Detect the friction
- Logic duplicated 2+ times (inline in services/listeners/controllers) → extract a deep module
- God methods mixing data-fetch with computation → split: caller pre-fetches, pure function computes
- Business logic inside Flowable listeners / filters / config → extract behind a port, listener stays transport glue
- Shallow pass-through services (Controller → Service → Mapper with no real logic) → usually fine, don't over-engineer

### 2. Choose the shape (by dependency category)
- **In-process** (pure computation, no I/O) → `static` pure function + caller assembles reference data
- **Local-substitutable / mock** (mappers, utils like DatetimeUtil) → constructor-injected domain service; `@Component`, mappers injected at boundary
- **External/remote** → port (interface) + production adapter + test adapter (e.g. `ChatSessionStore`, `AssistantLlm`, `LeaveApprovalSideEffects`)

### 3. Package & naming convention
- New packages under `com.qiujie.<feature>` — one package per deepened module:
  - `overtime/` → `OvertimeCalculator` + `OvertimeResult` (record)
  - `leaveapproval/` → `LeaveApprovalSideEffects` (port) + `...Impl`
  - `salarycalculation/` → `SalaryCalculation` (final class, private ctor, pure static)
- Pure computation must NOT touch Spring context, mappers, or `ResponseDTO` — tests must be zero-context JUnit
- Entity setters may not be `@Accessors(chain)` — test fixtures must use non-chained setters (verify before writing tests)

### 4. Testing (boundary, replace don't layer)
- New boundary unit tests: `*UnitTest.java` in `src/test/java/com/qiujie/<feature>/`, zero Spring context
- Pure static functions → zero-mock tests; port impls → Mockito mappers, verify cache-miss fallback queries
- Assert `BigDecimal` with `compareTo` (scale differs), not `equals`
- Delete now-redundant shallow tests; delete dead code left behind by the refactor (unused imports/fields/methods) in the same change

### 5. RFC + closure
- Create GitHub issue RFC (template in `~/.claude/skills/improve-codebase-architecture/REFERENCE.md`) before/with implementation
- After implementation verified (full `mvn test` green, all new tests pass): close the issue with the commit hash in the comment
- Commit per module; message prefix `refactor(<area>):`
- Verify no leftovers: grep for dead methods/imports after each refactor (e.g. the four `queryXxxDeduct` methods were dead after SalaryCalculation landed)

## Agent skills

### Issue tracker

Issues 和 spec 以 GitHub Issues 形式存在于 quuuuj/hrm，经 `gh` CLI 操作。见 `docs/agents/issue-tracker.md`。

### Domain docs

单上下文布局：根目录 `CONTEXT.md` + `docs/adr/`。见 `docs/agents/domain.md`。
