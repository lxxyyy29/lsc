# Dongguan Changping Platform Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first deliverable version of the Dongguan Changping low-altitude inspection event governance platform, including a Spring Boot backend, a Vue 3 Web admin app, and a Vue 3 H5 field app, with the phase-1 closed loop: event intake → audit flow → dispatch → H5 handling/verification → closure confirmation.

**Architecture:** Use a single repository containing three independent sibling applications: `backend/`, `web/`, and `h5/`. Implement phase 1 with a modular monolith backend, explicit state transitions, and shared domain vocabulary across all three apps. Keep the dual-track architecture in the structure, but only fully implement the direct-event track in phase 1; patrol-task output stays as a reserved skeleton with minimal domain wiring.

**Tech Stack:** JDK 17, Spring Boot 3.x, Spring Security, MyBatis-Plus, MySQL, Redis, Maven, Vue 3, TypeScript, Vite, Pinia, Vue Router, Element Plus, Vant, Axios.

---

## File Structure

### Repository root
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend`
  - Spring Boot backend service.
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web`
  - Vue 3 Web admin application.
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5`
  - Vue 3 H5 field application.
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/docs/architecture`
  - Optional generated diagrams, sequence notes, API references.

### Backend layout
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/pom.xml`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/Application.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/resources/application.yml`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/resources/application-local.yml`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/resources/db/migration/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/common/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/auth/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/user/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/patrol/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/audit/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/drone/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/map/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/media/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/integration/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/system/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/`

### Web layout
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/package.json`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/vite.config.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/main.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/router/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/stores/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/api/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/layout/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/dashboard/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/event/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/audit/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/process/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/workorder/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/patrol/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/drone/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/map/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/system/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/components/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/tests/`

### H5 layout
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/package.json`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/vite.config.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/main.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/router/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/stores/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/api/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/login/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/workbench/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/workorder/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/verify/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/history/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/mine/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/components/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/tests/`

---

## Chunk 1: Workspace scaffolding and backend foundation

### Task 1: Scaffold the repository workspace

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/`

- [ ] **Step 1: Create the three top-level application directories**

Run: `mkdir -p backend web h5`
Expected: Three sibling directories exist under the repository root.

- [ ] **Step 2: Add a root `.gitignore` if the repository does not already have one**

Create:
```gitignore
# Java
backend/target/
*.log

# Node
web/node_modules/
web/dist/
h5/node_modules/
h5/dist/

# IDE and OS
.idea/
.vscode/
.DS_Store
```

- [ ] **Step 3: Add a root `README.md` with app startup pointers**

Create a minimal workspace guide listing the three apps, their ports, and how they relate.

- [ ] **Step 4: Verify the root structure**

Run: `ls -la`
Expected: `backend`, `web`, and `h5` are visible.

- [ ] **Step 5: Commit the workspace scaffold (only if this directory is already under version control)**

If the directory is already a git repository, run:
```bash
git add .gitignore README.md backend web h5
git commit -m "chore: scaffold changping platform workspace"
```
If the directory is not a git repository yet, skip this step and continue with the implementation plan.

### Task 2: Bootstrap the Spring Boot backend application

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/pom.xml`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/Application.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/resources/application.yml`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/resources/application-local.yml`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/resources/application-test.yml`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/ApplicationStartupTest.java`

- [ ] **Step 1: Create `pom.xml` with the backend dependencies**

Include:
- spring-boot-starter-web
- spring-boot-starter-validation
- spring-boot-starter-security
- mybatis-plus-boot-starter
- mysql-connector-j
- spring-boot-starter-data-redis
- springdoc-openapi-starter-webmvc-ui
- flyway-core
- lombok
- mapstruct
- spring-boot-starter-test

- [ ] **Step 2: Add the Maven wrapper**

Run: `cd backend && mvn -N wrapper:wrapper`
Expected: `mvnw`, `mvnw.cmd`, and `.mvn/wrapper` are created.

- [ ] **Step 3: Write the failing startup test**

```java
@SpringBootTest
@ActiveProfiles("test")
class ApplicationStartupTest {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 4: Add test-safe backend configuration**

Create `/src/test/resources/application-test.yml` and configure a test-safe profile that avoids external MySQL/Redis requirements during startup. Use either an in-memory datasource or explicitly disable datasource, Flyway, and Redis auto-configuration for this startup smoke test.

- [ ] **Step 5: Run the backend test before implementation**

Run: `cd backend && ./mvnw -q -Dtest=ApplicationStartupTest test`
Expected: FAIL because the application entrypoint and runtime configuration are not complete yet.

- [ ] **Step 6: Add the `Application.java` entrypoint**

```java
@SpringBootApplication
@MapperScan("com.changping.platform.modules.**.mapper")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

- [ ] **Step 7: Add base configuration files**

Configure:
- server port `8080`
- app name `changping-platform`
- MySQL datasource placeholders
- Redis placeholders
- MyBatis-Plus mapper locations
- OpenAPI path

- [ ] **Step 8: Run the startup test again**

Run: `cd backend && ./mvnw -q -Dtest=ApplicationStartupTest test`
Expected: PASS.

- [ ] **Step 9: Commit the backend bootstrap (only if this directory is already under version control)**

If the directory is already a git repository, run:
```bash
git add pom.xml mvnw mvnw.cmd .mvn src/main src/test
git commit -m "feat: bootstrap spring backend"
```
If the directory is not a git repository yet, skip this step and continue.

### Task 3: Add backend common infrastructure

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/common/response/ApiResponse.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/common/exception/BusinessException.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/common/exception/GlobalExceptionHandler.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/common/security/SecurityConfig.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/common/TestExceptionController.java`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/common/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write the failing exception-handler test**

Create a test-only controller endpoint that throws `new BusinessException("EVENT_NOT_FOUND", "Event not found")`, then assert the JSON response contains a stable `code` and `message`.

- [ ] **Step 2: Run the new test**

Run: `cd backend && ./mvnw -q -Dtest=GlobalExceptionHandlerTest test`
Expected: FAIL because the response, exception, handler, and test controller do not exist.

- [ ] **Step 3: Create the common API response model**

```java
public record ApiResponse<T>(boolean success, String code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) { ... }
    public static <T> ApiResponse<T> fail(String code, String message) { ... }
}
```

- [ ] **Step 4: Create the domain exception and global exception handler**

Handle at least:
- `BusinessException`
- validation exceptions
- generic unexpected exceptions

- [ ] **Step 5: Create a bootstrap security config that does not block controller test development**

Phase 1 foundation rule:
- allow `/api/auth/login`
- allow `/v3/api-docs/**` and `/swagger-ui/**`
- temporarily allow non-auth business APIs during the foundation stage so controller tests can be written without introducing token infrastructure yet
- disable CSRF for stateless API use

Add a note in the implementation that this foundation rule will be tightened after the auth flow is added.

- [ ] **Step 6: Run the common-layer test again**

Run: `cd backend && ./mvnw -q -Dtest=GlobalExceptionHandlerTest test`
Expected: PASS.

- [ ] **Step 7: Run the full backend test suite**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 8: Commit the common backend layer (only if this directory is already under version control)**

If the directory is already a git repository, run:
```bash
git add src/main/java/com/changping/platform/common src/test/java/com/changping/platform/common
 git commit -m "feat: add backend common infrastructure"
```
If the directory is not a git repository yet, skip this step and continue.

## Chunk 2: Core backend business closed loop

### Task 4: Create the phase-1 database migrations and enums

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/resources/db/migration/V1__init_phase1_schema.sql`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/domain/EventStatus.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/domain/ProcessStatus.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/domain/WorkOrderStatus.java`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/schema/Phase1SchemaTest.java`

- [ ] **Step 1: Write the failing schema smoke test**

Use a lightweight migration/integration test to assert the schema creates the core tables:
- `sys_user`
- `sys_role`
- `biz_event`
- `biz_event_record`
- `biz_process_template`
- `biz_process_template_node`
- `biz_process_instance`
- `biz_process_instance_node`
- `biz_process_action_record`
- `biz_audit_record`
- `biz_work_order`
- `biz_media_file`

- [ ] **Step 2: Run the schema test**

Run: `cd backend && ./mvnw -q -Dtest=Phase1SchemaTest test`
Expected: FAIL because the migration and enums do not exist.

- [ ] **Step 3: Create the phase-1 SQL migration**

Include at minimum:
- auth tables
- event tables
- process tables
- audit table
- work order tables
- media tables
- drone table
- patrol task skeleton table

Important constraints:
- `biz_work_order.source_event_id` unique
- explicit status columns with comments
- create/update timestamps on all business tables

- [ ] **Step 4: Add the domain enums for event, process, and work-order states**

Event states must include:
- `PENDING_AUDIT`
- `IN_AUDIT`
- `AUDIT_APPROVED`
- `AUDIT_REJECTED`
- `WAITING_DISPATCH`
- `DISPATCHED_TO_WORK_ORDER`
- `CLOSED`

Work-order states must include:
- `WAITING_ACCEPT`
- `PROCESSING`
- `WAITING_VERIFY`
- `WAITING_CLOSE_CONFIRM`
- `COMPLETED`
- `CLOSED`
- `TIMEOUT`

Important semantic rule:
- `COMPLETED` means the normal business path has been processed and confirmed as done
- `CLOSED` means the work order is ended without normal completion, such as `NOT_TRUE` or administrative closure

- [ ] **Step 5: Run the schema test again**

Run: `cd backend && ./mvnw -q -Dtest=Phase1SchemaTest test`
Expected: PASS.

- [ ] **Step 6: Commit the schema baseline**

```bash
git add src/main/resources/db/migration src/main/java/com/changping/platform/modules/*/domain
git commit -m "feat: add phase1 schema baseline"
```

### Task 5: Implement event intake and event query APIs

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/controller/EventController.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/service/EventService.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/service/impl/EventServiceImpl.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/mapper/EventMapper.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/entity/EventEntity.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/dto/CreateEventRequest.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/event/vo/EventDetailVo.java`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/event/EventControllerTest.java`

- [ ] **Step 1: Write the failing controller tests**

Cover:
- create event with required fields, `sourceSystem`, and at least one evidence reference returns success
- create event without evidence reference is rejected
- duplicate external event id is rejected
- get event detail returns the stored event

- [ ] **Step 2: Run the event controller tests**

Run: `cd backend && ./mvnw -q -Dtest=EventControllerTest test`
Expected: FAIL because the controller and service do not exist.

- [ ] **Step 3: Implement the event entity, mapper, DTO, and service**

Creation rules:
- set initial state to `PENDING_AUDIT`
- require `externalEventId`, `sourceType`, `sourceSystem`, `eventType`, `title`, `occurredAt`, and location fields
- require at least one evidence/media reference on intake
- enforce source idempotency by unique lookup

- [ ] **Step 4: Implement the event controller**

Endpoints:
- `POST /api/events`
- `GET /api/events/{id}`
- `GET /api/events`

- [ ] **Step 5: Add an event record write on creation**

When an event is created, insert an initial `biz_event_record` row with the status transition reason `EVENT_INTAKE`.

- [ ] **Step 6: Run the controller tests again**

Run: `cd backend && ./mvnw -q -Dtest=EventControllerTest test`
Expected: PASS.

- [ ] **Step 7: Commit the event module baseline**

```bash
git add src/main/java/com/changping/platform/modules/event src/test/java/com/changping/platform/modules/event
git commit -m "feat: add event intake module"
```

### Task 6: Implement process-template and audit-start APIs

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/controller/ProcessTemplateController.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/controller/ProcessInstanceController.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/service/ProcessTemplateService.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/service/ProcessInstanceService.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/entity/ProcessTemplateEntity.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/entity/ProcessTemplateNodeEntity.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/entity/ProcessInstanceEntity.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/entity/ProcessInstanceNodeEntity.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/process/entity/ProcessActionRecordEntity.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/audit/controller/AuditController.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/audit/entity/AuditRecordEntity.java`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/audit/AuditStartFlowTest.java`

- [ ] **Step 1: Write the failing audit-start tests**

Cover:
- create process template with nodes succeeds
- start audit for an event stores a process instance and node-instance rows
- starting audit twice for the same event is rejected
- when exactly one enabled template matches the event type, it is selected by default
- when multiple enabled templates match the event type, manual selection is required
- rejected event re-submission defaults to the original template id and frozen version unless an authorized override is provided

- [ ] **Step 2: Run the audit-start tests**

Run: `cd backend && ./mvnw -q -Dtest=AuditStartFlowTest test`
Expected: FAIL because the process and audit APIs do not exist.

- [ ] **Step 3: Implement process-template CRUD minimums**

Required fields:
- template name
- applicable event type
- enabled flag
- version
- ordered node definitions
- node mode: `SINGLE`, `SEQUENTIAL`, `PARALLEL`

- [ ] **Step 4: Implement the audit-start service**

Rules:
- event must be `PENDING_AUDIT` or `AUDIT_REJECTED`
- first auto-match enabled templates by event type
- if exactly one enabled template matches, use it by default
- if multiple enabled templates match, require explicit template selection from the caller
- freeze template id + version onto the instance
- enforce the invariant that one event binds to at most one process instance across its lifecycle
- on first submission, create the single process instance for the event
- on rejected-event resubmission, reuse or atomically reset the existing process instance instead of creating a second one
- on rejected-event resubmission, default to the original template id + frozen version unless an authorized override is explicitly provided
- move event to `IN_AUDIT`
- create or reset node-instance rows in the correct initial states
- write both process action records and audit records for traceability

- [ ] **Step 5: Expose the minimal process and audit endpoints**

Endpoints:
- `POST /api/processes/templates`
- `GET /api/processes/templates`
- `POST /api/audits/{eventId}/start`
- `GET /api/audits/{eventId}`

- [ ] **Step 6: Run the audit-start tests again**

Run: `cd backend && ./mvnw -q -Dtest=AuditStartFlowTest test`
Expected: PASS.

- [ ] **Step 7: Commit the process-template and audit-start flow**

```bash
git add src/main/java/com/changping/platform/modules/process src/main/java/com/changping/platform/modules/audit src/test/java/com/changping/platform/modules/audit
git commit -m "feat: add process templates and audit start"
```

### Task 7: Implement audit decision, dispatch, and work-order creation

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/controller/WorkOrderController.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/service/WorkOrderService.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/service/impl/WorkOrderServiceImpl.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/entity/WorkOrderEntity.java`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/workorder/DispatchWorkOrderFlowTest.java`

- [ ] **Step 1: Write the failing dispatch-flow tests**

Cover:
- approving a `SINGLE` node completes that node and can move the process forward immediately
- approving a `SEQUENTIAL` node unlocks the next node and does not unlock later nodes early
- approving one `PARALLEL` node does not pass the process until all required parallel nodes are approved
- approve all required nodes moves the event to `WAITING_DISPATCH`
- dispatch creates exactly one work order
- dispatching the same event twice is rejected
- created work order starts in `WAITING_ACCEPT`

- [ ] **Step 2: Run the dispatch-flow tests**

Run: `cd backend && ./mvnw -q -Dtest=DispatchWorkOrderFlowTest test`
Expected: FAIL because approval and dispatch code do not exist.

- [ ] **Step 3: Implement audit approve/reject endpoints**

Endpoints:
- `POST /api/processes/instances/{id}/approve`
- `POST /api/processes/instances/{id}/reject`

Rules:
- update node-instance state
- for `SINGLE` mode, the node completes as soon as its assignee approves
- for `SEQUENTIAL` mode, only the current active node may be approved; approving it unlocks the next node in order
- for `PARALLEL` mode, the process does not pass until all required active parallel nodes are approved
- compute aggregate process state from node-instance states
- on full approval set event state to `AUDIT_APPROVED`, then immediately transition it to `WAITING_DISPATCH` in the same business flow
- on reject set event state to `AUDIT_REJECTED`

- [ ] **Step 4: Implement the dispatch API**

Endpoint:
- `POST /api/work-orders/{eventId}/dispatch`

Rules:
- event must be `WAITING_DISPATCH`
- assign to a concrete user id
- create exactly one work order
- set event state to `DISPATCHED_TO_WORK_ORDER`
- write a work-order record row and an event record row

- [ ] **Step 5: Run the dispatch-flow tests again**

Run: `cd backend && ./mvnw -q -Dtest=DispatchWorkOrderFlowTest test`
Expected: PASS.

- [ ] **Step 6: Run the full backend test suite**

Run: `cd backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 7: Commit the dispatch closed loop**

```bash
git add src/main/java/com/changping/platform/modules/process src/main/java/com/changping/platform/modules/workorder src/test/java/com/changping/platform/modules/workorder
git commit -m "feat: add audit approval and work order dispatch"
```

### Task 8: Implement H5 handling, verification, and closure confirmation APIs

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/controller/H5WorkOrderController.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/dto/AcceptWorkOrderRequest.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/dto/ArriveWorkOrderRequest.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/dto/HandleWorkOrderRequest.java`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/main/java/com/changping/platform/modules/workorder/dto/VerifyWorkOrderRequest.java`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/backend/src/test/java/com/changping/platform/modules/workorder/H5WorkOrderFlowTest.java`

- [ ] **Step 1: Write the failing H5 closed-loop tests**

Cover:
- accept assigned work order moves it to `PROCESSING`
- submit verification result `NEEDS_MORE_EVIDENCE` moves it to `WAITING_VERIFY`
- submit verification result `CONTINUE_PROCESSING` keeps it in `PROCESSING`
- submit handled result `RESOLVED` moves it to `WAITING_CLOSE_CONFIRM`
- submit arrive action records on-site confirmation successfully
- confirm close moves event to `CLOSED` and work order to `COMPLETED`
- submit result `NOT_TRUE` closes both event and work order

- [ ] **Step 2: Run the H5 flow tests**

Run: `cd backend && ./mvnw -q -Dtest=H5WorkOrderFlowTest test`
Expected: FAIL because the H5 APIs do not exist.

- [ ] **Step 3: Implement the H5 work-order endpoints**

Endpoints:
- `GET /api/h5/workbench`
- `GET /api/h5/work-orders`
- `GET /api/h5/work-orders/{id}`
- `POST /api/h5/work-orders/{id}/accept`
- `POST /api/h5/work-orders/{id}/arrive`
- `POST /api/h5/work-orders/{id}/handle`
- `POST /api/h5/work-orders/{id}/verify`

- [ ] **Step 4: Implement status transitions exactly as defined in the spec**

Rules:
- accept → `WAITING_ACCEPT` to `PROCESSING`
- arrive → keep work order in `PROCESSING` and write an on-site confirmation record
- resolved and handled → `WAITING_CLOSE_CONFIRM`
- continue processing → keep work order in `PROCESSING`
- needs more evidence → `WAITING_VERIFY`
- not true → work order `CLOSED`, event `CLOSED`
- close confirm approved → work order `COMPLETED`, event `CLOSED`
- close confirm rejected → work order `PROCESSING`, event remains `DISPATCHED_TO_WORK_ORDER`

- [ ] **Step 5: Implement the Web close-confirm API**

Endpoint:
- `POST /api/work-orders/{id}/confirm-close`

- [ ] **Step 6: Run the H5 flow tests again**

Run: `cd backend && ./mvnw -q -Dtest=H5WorkOrderFlowTest test`
Expected: PASS.

- [ ] **Step 7: Commit the H5 backend closed loop**

```bash
git add src/main/java/com/changping/platform/modules/workorder src/test/java/com/changping/platform/modules/workorder
git commit -m "feat: add h5 handling and closure APIs"
```

## Chunk 3: Web admin application

### Task 9: Bootstrap the Web admin application shell

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/package.json`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/tsconfig.json`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/vite.config.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/main.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/App.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/router/index.ts`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/tests/app-shell.spec.ts`

- [ ] **Step 1: Write the failing app-shell test**

Verify that the app renders a login route and a protected layout route.

- [ ] **Step 2: Run the Web test before implementation**

Run: `pnpm test -- --runInBand app-shell.spec.ts`
Expected: FAIL because the web app does not exist yet.

- [ ] **Step 3: Initialize the Web app**

Run: `pnpm create vite web --template vue-ts`
Expected: A Vue 3 TypeScript app is created in `web/`.

- [ ] **Step 4: Add dependencies**

Run: `pnpm add vue-router pinia axios element-plus`
Run: `pnpm add -D vitest @testing-library/vue jsdom`

- [ ] **Step 5: Build the app shell and router**

Routes must include:
- `/login`
- `/dashboard`
- `/events`
- `/events/:id`
- `/audits`
- `/audits/:id`
- `/processes`
- `/processes/:id/edit`
- `/work-orders`
- `/work-orders/:id`
- `/patrol-tasks`
- `/patrol-tasks/:id`
- `/drones`
- `/maps`
- `/system`
- `/system/config`

- [ ] **Step 6: Run the Web test again**

Run: `pnpm test -- --runInBand app-shell.spec.ts`
Expected: PASS.

- [ ] **Step 7: Run the Web build**

Run: `pnpm build`
Expected: PASS.

- [ ] **Step 8: Commit the Web shell**

```bash
git add .
git commit -m "feat: scaffold web admin app"
```

### Task 10: Implement the Web event, audit, process, and work-order pages

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/api/event.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/api/audit.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/api/process.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/api/workorder.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/event/EventListView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/event/EventDetailView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/audit/AuditListView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/audit/AuditDetailView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/process/ProcessTemplateListView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/process/ProcessTemplateEditView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/workorder/WorkOrderListView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/workorder/WorkOrderDetailView.vue`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/tests/core-admin-pages.spec.ts`

- [ ] **Step 1: Write the failing page-render tests**

Cover:
- event list renders query filters + table
- event detail renders metadata, evidence region, and lifecycle records
- audit list renders pending audit rows and status filters
- audit detail renders process template selector and node progress
- process template list renders templates and status columns
- process template edit renders node order + node mode editing controls
- work-order list renders assignee, state, and action columns
- work-order detail renders dispatch or close-confirm actions depending on state

- [ ] **Step 2: Run the page tests**

Run: `pnpm test -- --runInBand core-admin-pages.spec.ts`
Expected: FAIL because the pages do not exist.

- [ ] **Step 3: Create shared admin building blocks**

Create reusable pieces:
- `PageContainer`
- `QueryPanel`
- `StatusTag`
- `RecordTimeline`

- [ ] **Step 4: Implement the event pages**

The list page must include:
- source type
- event type
- current status
- occurrence time
- area
- actions

The detail page must include:
- core metadata
- media evidence region
- lifecycle record region

- [ ] **Step 5: Implement the audit pages**

The detail page must include:
- event summary
- process template selector
- node progress display
- approve / reject action region
- dispatch entry when event state is `WAITING_DISPATCH`

Template-selection rules in the page must match the approved spec:
- before audit submission, the selector is editable
- after submission, the frozen template version is read-only
- on rejected-event resubmission, default to the original template/version
- reselect controls are shown only to authorized roles

- [ ] **Step 6: Implement the process-template pages**

The edit page must support:
- template metadata
- applicable event type
- enabled toggle
- ordered node editing
- node mode selection

- [ ] **Step 7: Implement the work-order pages**

The detail page must show:
- source event summary
- assignee
- current state
- flow records
- close confirmation action when applicable

- [ ] **Step 8: Run the page tests again**

Run: `pnpm test -- --runInBand core-admin-pages.spec.ts`
Expected: PASS.

- [ ] **Step 9: Run the Web build again**

Run: `pnpm build`
Expected: PASS.

- [ ] **Step 10: Commit the core Web admin pages**

```bash
git add src
 git commit -m "feat: add core web admin pages"
```

### Task 11: Implement the Web dashboard, drone, map, system, and patrol skeleton pages

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/dashboard/DashboardView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/drone/DroneListView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/map/MapOverviewView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/system/UserRoleView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/system/SystemConfigView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/patrol/PatrolTaskListView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/views/patrol/PatrolTaskDetailView.vue`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/web/src/tests/skeleton-pages.spec.ts`

- [ ] **Step 1: Write the failing skeleton-page tests**

Verify these routes render the expected phase-1 placeholders and page headings.

- [ ] **Step 2: Run the skeleton-page tests**

Run: `pnpm test -- --runInBand skeleton-pages.spec.ts`
Expected: FAIL because the pages do not exist.

- [ ] **Step 3: Build the dashboard page**

Include:
- KPI cards
- latest events table
- pending work-order panel
- map placeholder region

- [ ] **Step 4: Build the drone and map pages**

Drone page must include:
- device list
- online status
- platform source column

Map page must include:
- filter bar
- event/work-order/device layers placeholder

- [ ] **Step 5: Build the system and patrol skeleton pages**

The system pages must include:
- user and role management skeleton
- a dedicated interface/parameter configuration page at `/system/config`

The patrol pages must clearly state:
- phase 1 keeps skeleton support
- task-to-event relationship is reserved

- [ ] **Step 6: Run the skeleton-page tests again**

Run: `pnpm test -- --runInBand skeleton-pages.spec.ts`
Expected: PASS.

- [ ] **Step 7: Run the Web build**

Run: `pnpm build`
Expected: PASS.

- [ ] **Step 8: Commit the supporting Web pages**

```bash
git add src
 git commit -m "feat: add supporting web pages"
```

## Chunk 4: H5 application and integration verification

### Task 12: Bootstrap the H5 application shell

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/package.json`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/tsconfig.json`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/vite.config.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/main.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/App.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/router/index.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/login/LoginView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/api/auth.ts`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/tests/h5-shell.spec.ts`

- [ ] **Step 1: Write the failing H5 shell test**

Verify that the app renders:
- `/login`
- `/workbench`
- `/work-orders`
- `/verify`
- `/history`
- `/mine`

Also verify that protected routes require a logged-in session and redirect to `/login` when no H5 auth state exists.

- [ ] **Step 2: Run the H5 test before implementation**

Run: `pnpm test -- --runInBand h5-shell.spec.ts`
Expected: FAIL because the h5 app does not exist yet.

- [ ] **Step 3: Initialize the H5 app**

Run: `pnpm create vite h5 --template vue-ts`
Expected: A Vue 3 TypeScript app is created in `h5/`.

- [ ] **Step 4: Add H5 dependencies**

Run: `pnpm add vue-router pinia axios vant`
Run: `pnpm add -D vitest @testing-library/vue jsdom`

- [ ] **Step 5: Build the router and app shell**

Include routes for login, workbench, work-orders, verify, history, and mine.

Also implement:
- a real H5 login page with account/password form
- a reserved captcha input or placeholder slot
- auth API integration and local session persistence
- route guards for protected pages

- [ ] **Step 6: Run the H5 shell test again**

Run: `pnpm test -- --runInBand h5-shell.spec.ts`
Expected: PASS.

- [ ] **Step 7: Run the H5 build**

Run: `pnpm build`
Expected: PASS.

- [ ] **Step 8: Commit the H5 shell**

```bash
git add .
git commit -m "feat: scaffold h5 app"
```

### Task 13: Implement the H5 workbench, work-order, verification, and history pages

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/api/workorder.ts`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/workbench/WorkbenchView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/workorder/WorkOrderListView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/workorder/WorkOrderDetailView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/verify/VerifyView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/history/HistoryView.vue`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/views/mine/MineView.vue`
- Test: `/Users/tangxinglin/code/javacode/dgcp-oa/h5/src/tests/h5-closed-loop-pages.spec.ts`

- [ ] **Step 1: Write the failing H5 page tests**

Cover:
- workbench shows pending counts
- work-order detail shows accept button for waiting-accept work orders
- verify page shows all four result options: `属实并已处理`, `属实但需继续处理`, `不属实`, `需补充`
- history page renders completed items and upload records
- mine page renders message-center and settings entries

- [ ] **Step 2: Run the H5 page tests**

Run: `pnpm test -- --runInBand h5-closed-loop-pages.spec.ts`
Expected: FAIL because the pages do not exist.

- [ ] **Step 3: Implement the workbench page**

Include:
- pending counts
- shortcut cards
- latest pending work orders

- [ ] **Step 4: Implement the work-order list and detail pages**

The detail page must support:
- accept action
- arrive marker
- evidence upload section
- image/video upload integration
- handling notes
- submit action

- [ ] **Step 5: Implement the verification page**

The page must include:
- result options `属实并已处理`, `属实但需继续处理`, `不属实`, `需补充`
- evidence upload area
- note input

- [ ] **Step 6: Implement the history and mine pages**

History must show:
- completed work orders
- verify submissions
- uploaded image/video records

Mine must show:
- user summary
- message-center entry
- settings entry
- logout entry

- [ ] **Step 7: Run the H5 page tests again**

Run: `pnpm test -- --runInBand h5-closed-loop-pages.spec.ts`
Expected: PASS.

- [ ] **Step 8: Run the H5 build**

Run: `pnpm build`
Expected: PASS.

- [ ] **Step 9: Commit the H5 phase-1 pages**

```bash
git add src
 git commit -m "feat: add h5 closed-loop pages"
```

### Task 14: Add integration verification and delivery checklist

**Files:**
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/docs/architecture/phase1-endpoints.md`
- Create: `/Users/tangxinglin/code/javacode/dgcp-oa/docs/architecture/phase1-verification-checklist.md`
- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/README.md`

- [ ] **Step 1: Write the backend-to-frontend contract document**

Document at least:
- event intake payload
- audit start payload
- approve/reject payloads
- dispatch payload
- H5 login payload
- H5 accept/arrive/handle/verify payloads
- media upload request/response payloads
- close-confirm payload

- [ ] **Step 2: Write the manual verification checklist**

Include this exact business path:
1. create event
2. view event in Web
3. start audit with a process template
4. approve nodes until the event is waiting for dispatch
5. dispatch to a concrete user
6. log into H5 as that concrete dispatched user
7. verify only that user sees the assigned work order in the workbench
8. accept work order
9. record arrive/on-site confirmation
10. upload image/video evidence successfully
11. submit `属实并已处理` and verify the work order enters waiting-close-confirm
12. confirm close in Web and verify event is closed and work order is completed
13. run branch case: submit `需补充` and verify work order enters waiting-verify
14. run branch case: submit `属实但需继续处理` and verify work order stays processing
15. run branch case: submit `不属实` and verify both event and work order are closed
16. run branch case: reject close confirmation and verify work order returns to processing while the event remains dispatched
17. verify uploaded evidence is visible in H5 history and the corresponding Web event/work-order detail views

- [ ] **Step 3: Update the root README with run instructions**

Include startup commands for backend, web, and h5.

- [ ] **Step 4: Run final automated verification**

Run backend:
```bash
cd backend && ./mvnw test
```
Expected: PASS.

Run web:
```bash
cd web && pnpm test && pnpm build
```
Expected: PASS.

Run h5:
```bash
cd h5 && pnpm test && pnpm build
```
Expected: PASS.

- [ ] **Step 5: Perform the manual closed-loop verification**

Execute the checklist in `docs/architecture/phase1-verification-checklist.md`.
Expected: The full phase-1 closed loop works end-to-end.

- [ ] **Step 6: Commit the verification and docs pass**

```bash
git add README.md docs/architecture
git commit -m "docs: add phase1 verification artifacts"
```
