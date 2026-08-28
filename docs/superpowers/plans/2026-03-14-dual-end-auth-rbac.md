# Dual-End Auth and RBAC Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build real authentication for both Web and H5, add unified RBAC with menu/button/API permissions, and migrate current business flows to use authenticated identity instead of foundation-stage headers.

**Architecture:** Add a backend auth/RBAC slice with JWT bearer auth, BCrypt password verification, current-user context, RBAC tables, seed data, and controller/service authorization gates. Then wire Web and H5 to real login/session recovery/token injection, followed by route/menu/button permission enforcement and regression coverage.

**Tech Stack:** Spring Boot 3.3, Spring Security, JdbcTemplate, Flyway, H2 tests, Vue 3, TypeScript, vue-router, Axios, Vitest

---

## File Structure

### Backend auth and RBAC core
- Create: `backend/src/main/resources/db/migration/V5__add_auth_rbac_tables_and_seed_data.sql`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/controller/AuthController.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/controller/H5AuthController.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/service/AuthService.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/service/CurrentUserService.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/vo/LoginResponse.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/vo/CurrentUserVo.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/model/AuthenticatedUser.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/JwtTokenService.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/BearerTokenAuthenticationFilter.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/AuthenticatedUserContextHolder.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/PermissionGuard.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/PermissionCodes.java`
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/test/resources/application-test.yml`
- Modify: `backend/src/main/java/com/changping/platform/common/security/SecurityConfig.java`
- Modify: `backend/src/main/java/com/changping/platform/common/security/FoundationActorResolver.java`

### Backend controller/service authorization migration
- Modify: `backend/src/main/java/com/changping/platform/modules/event/controller/EventController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/audit/controller/AuditController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/process/controller/ProcessInstanceController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/process/controller/ProcessTemplateController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/workorder/controller/WorkOrderController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/workorder/controller/H5WorkOrderController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/process/service/ProcessInstanceService.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/workorder/service/impl/WorkOrderServiceImpl.java`

### Backend tests
- Create: `backend/src/test/java/com/changping/platform/modules/auth/AuthControllerTest.java`
- Create: `backend/src/test/java/com/changping/platform/modules/auth/AuthSecurityIntegrationTest.java`
- Modify: `backend/src/test/java/com/changping/platform/modules/workorder/H5WorkOrderFlowTest.java`
- Modify: `backend/src/test/java/com/changping/platform/modules/workorder/DispatchWorkOrderFlowTest.java`
- Modify: `backend/src/test/java/com/changping/platform/modules/audit/AuditStartFlowTest.java`

### Web frontend auth and RBAC
- Create: `web/src/api/auth.ts`
- Create: `web/src/api/http.ts`
- Create: `web/src/views/auth/LoginView.vue`
- Create: `web/src/auth/session.ts`
- Create: `web/src/auth/permissions.ts`
- Modify: `web/src/router/index.ts`
- Modify: `web/src/main.ts`
- Modify: `web/src/views/audit/AuditDetailView.vue`
- Modify: `web/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `web/src/views/process/ProcessTemplateEditView.vue`
- Modify: `web/src/tests/app-shell.spec.ts`
- Modify: `web/src/tests/core-admin-pages.spec.ts`

### H5 frontend auth and permissions
- Create: `h5/src/api/http.ts`
- Create: `h5/src/auth/permissions.ts`
- Modify: `h5/src/api/auth.ts`
- Modify: `h5/src/router/index.ts`
- Modify: `h5/src/views/login/LoginView.vue`
- Modify: `h5/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `h5/src/views/verify/VerifyView.vue`
- Modify: `h5/src/tests/h5-shell.spec.ts`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`

---

## Chunk 1: Backend auth and RBAC foundation

### Task 1: Add RBAC schema, auth configuration, and seed data

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__add_auth_rbac_tables_and_seed_data.sql`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/resources/application-local.yml`
- Modify: `backend/src/test/resources/application-test.yml`
- Test: `backend/src/test/java/com/changping/platform/schema/Phase1SchemaTest.java`

- [ ] **Step 1: Write the failing schema test assertions for RBAC tables and seed expectations**

Add assertions to `backend/src/test/java/com/changping/platform/schema/Phase1SchemaTest.java` verifying existence of:
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`
- required unique indexes / foreign keys
- required seeded login-entry permissions:
  - `menu:dashboard:view`
  - `menu:event:list`
  - `menu:audit:list`
  - `menu:h5:workbench:view`
  - `menu:h5:workorder:list`
- required first-wave API permissions used by upcoming auth/security tests:
  - `api:audit:start`
  - `api:process-instance:approve`
  - `api:workorder:dispatch`
  - `api:h5:workbench:view`
  - `api:h5:workorder:list`
  - `api:h5:workorder:accept`

- [ ] **Step 2: Run schema test to verify it fails**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=Phase1SchemaTest test`
Expected: FAIL because the new tables and seed data do not exist yet.

- [ ] **Step 3: Write migration `V5__add_auth_rbac_tables_and_seed_data.sql`**

Include:
- `sys_permission` table with `permission_code`, `permission_type`, `client_type`, `parent_id`, `path`, `sort_order`, `status`
- `sys_user_role` table
- `sys_role_permission` table
- compatibility backfill from `sys_user.role_id` into `sys_user_role`
- seed roles if missing: `SUPER_ADMIN`, `EVENT_OPERATOR`, `AUDITOR`, `DISPATCHER`, `H5_WORKER`, `H5_VERIFIER`
- seed the explicit first-wave permissions required by this plan:
  - `menu:dashboard:view`
  - `menu:event:list`
  - `menu:audit:list`
  - `menu:h5:workbench:view`
  - `menu:h5:workorder:list`
  - `api:audit:start`
  - `api:process-instance:approve`
  - `api:workorder:dispatch`
  - `api:h5:workbench:view`
  - `api:h5:workorder:list`
  - `api:h5:workorder:accept`
- seed `SUPER_ADMIN` to all permissions
- seed Web/H5 entry permissions used for login gating

- [ ] **Step 4: Add auth config placeholders to application configs**

Add config keys:
- `security.auth.jwt-secret`
- `security.auth.access-token-expire-minutes`

Use environment-backed values in `application.yml`, safe local defaults in `application-local.yml`, and deterministic test values in `application-test.yml`.

- [ ] **Step 5: Run schema test to verify it passes**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=Phase1SchemaTest test`
Expected: PASS.

- [ ] **Step 6: Run full Flyway-backed backend smoke tests**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=ApplicationStartupTest,Phase1SchemaTest test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/db/migration/V5__add_auth_rbac_tables_and_seed_data.sql src/main/resources/application.yml src/main/resources/application-local.yml src/test/resources/application-test.yml src/test/java/com/changping/platform/schema/Phase1SchemaTest.java
git commit -m "feat: add auth and rbac schema foundation"
```

### Task 2: Add JWT, password encoding, auth models, and login/me/logout endpoints

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/vo/LoginResponse.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/vo/CurrentUserVo.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/model/AuthenticatedUser.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/service/AuthService.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/service/CurrentUserService.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/JwtTokenService.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/controller/AuthController.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/controller/H5AuthController.java`
- Test: `backend/src/test/java/com/changping/platform/modules/auth/AuthControllerTest.java`

- [ ] **Step 1: Write failing auth controller tests**

Create `AuthControllerTest.java` covering:
- Web login success with BCrypt password hash
- H5 login success with BCrypt password hash
- wrong password failure
- disabled user failure
- Web login denied when user lacks Web entry permission
- H5 login denied when user lacks H5 entry permission
- `/api/auth/me` returns wrapped `ApiResponse<CurrentUserVo>`
- `/api/h5/auth/me` returns wrapped `ApiResponse<CurrentUserVo>`
- logout returns wrapped success

- [ ] **Step 2: Run auth controller tests to verify they fail**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuthControllerTest test`
Expected: FAIL because auth classes, JWT dependency, and endpoints do not exist yet.

- [ ] **Step 3: Add JWT dependency and password encoder bean support**

Modify `backend/pom.xml` to add a JWT library compatible with Java 17/Spring Boot 3.
Use Spring Security `BCryptPasswordEncoder` for password verification.

- [ ] **Step 4: Implement auth DTOs/VOs/model**

Create focused classes for:
- `LoginRequest`
- `LoginResponse`
- `CurrentUserVo`
- `AuthenticatedUser`

Ensure response payloads fit inside existing `ApiResponse<T>` wrapper.

- [ ] **Step 5: Implement `JwtTokenService` and `AuthService`**

`JwtTokenService` responsibilities:
- create signed JWT from user identity and client type
- parse and validate JWT
- expose expiration handling

`AuthService` responsibilities:
- load user by account
- validate status
- verify BCrypt password
- load role codes and permission codes from RBAC tables (with legacy `role_id` fallback if needed)
- enforce login entry-permission gating for Web/H5
- build login and current-user responses

- [ ] **Step 6: Implement `AuthController` and `H5AuthController`**

Endpoints:
- `POST /api/auth/login`
- `POST /api/h5/auth/login`
- `POST /api/auth/logout`
- `POST /api/h5/auth/logout`
- `GET /api/auth/me`
- `GET /api/h5/auth/me`

All endpoints must return `ApiResponse<T>`.

- [ ] **Step 7: Run auth controller tests to verify they pass**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuthControllerTest test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add pom.xml src/main/java/com/changping/platform/modules/auth src/test/java/com/changping/platform/modules/auth/AuthControllerTest.java
git commit -m "feat: add web and h5 auth endpoints"
```

### Task 3: Add bearer authentication filter, current-user context, and permission guard

**Files:**
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/AuthenticatedUserContextHolder.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/BearerTokenAuthenticationFilter.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/PermissionGuard.java`
- Create: `backend/src/main/java/com/changping/platform/modules/auth/security/PermissionCodes.java`
- Modify: `backend/src/main/java/com/changping/platform/common/security/SecurityConfig.java`
- Modify: `backend/src/main/java/com/changping/platform/common/security/FoundationActorResolver.java`
- Test: `backend/src/test/java/com/changping/platform/modules/auth/AuthSecurityIntegrationTest.java`

- [ ] **Step 1: Write failing security integration tests**

Create `AuthSecurityIntegrationTest.java` covering:
- unauthenticated request to protected API returns 401/403 as designed
- authenticated request with valid token passes authentication
- missing API permission returns 403
- Web token cannot pass H5 me endpoint if client-type rule forbids it
- H5 token cannot pass Web me endpoint if client-type rule forbids it
- `FoundationActorResolver` resolves actor from authenticated context before fallback headers
- `FoundationActorResolver` still falls back to `X-Foundation-*` when no authenticated context exists, so the transition path is pinned down by a failing test

- [ ] **Step 2: Run security integration tests to verify they fail**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuthSecurityIntegrationTest test`
Expected: FAIL because filter, context holder, and permission guard do not exist yet.

- [ ] **Step 3: Implement `AuthenticatedUserContextHolder` and bearer filter**

Filter responsibilities:
- extract `Authorization: Bearer <token>`
- validate token via `JwtTokenService`
- load current roles and permissions
- populate `SecurityContext`
- populate `AuthenticatedUserContextHolder`
- clear context after request

- [ ] **Step 4: Upgrade `FoundationActorResolver` to read auth context first**

Preserve backward compatibility by:
- first reading `AuthenticatedUserContextHolder`
- then falling back to `X-Foundation-*` only where no authenticated principal exists

- [ ] **Step 5: Implement `PermissionGuard` and centralized permission constants**

`PermissionGuard` should expose small methods like:
- `require(String permissionCode)`
- `has(String permissionCode)`
- `requireAny(Set<String> permissionCodes)`

`PermissionCodes` should centralize menu/button/api permission strings to prevent drift.

- [ ] **Step 6: Tighten `SecurityConfig` in phase-1-safe order**

Configure only the first-stage hardening in Chunk 1:
- permitAll for login/docs routes
- register bearer filter
- require authentication for auth-sensitive and H5 routes covered by Chunk 1 tests
- do **not** flip every `/api/**` route to authenticated-by-default yet
- document or codify any temporary transition exceptions needed until Chunk 2 completes controller migration

Chunk 2 is where the plan finishes the broader controller protection and can safely move toward authenticated-by-default behavior.

- [ ] **Step 7: Run security integration tests to verify they pass**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuthSecurityIntegrationTest test`
Expected: PASS.

- [ ] **Step 8: Run auth controller tests and startup smoke test**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuthControllerTest,AuthSecurityIntegrationTest,ApplicationStartupTest test`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/changping/platform/modules/auth/security src/main/java/com/changping/platform/common/security/SecurityConfig.java src/main/java/com/changping/platform/common/security/FoundationActorResolver.java src/test/java/com/changping/platform/modules/auth/AuthSecurityIntegrationTest.java
git commit -m "feat: add bearer auth and permission guard"
```

---

## Chunk 2: Backend controller and business authorization migration

### Task 4: Protect Web business endpoints with API permissions

**Prerequisite:** Do not start this task until Chunk 1 has created `AuthSecurityIntegrationTest`, `PermissionGuard`, bearer authentication, and the first-wave seeded `api:*` permissions. Chunk 2 assumes those foundations already exist and are green.

**Files:**
- Modify: `backend/src/main/java/com/changping/platform/modules/event/controller/EventController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/audit/controller/AuditController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/process/controller/ProcessInstanceController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/process/controller/ProcessTemplateController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/workorder/controller/WorkOrderController.java`
- Test: `backend/src/test/java/com/changping/platform/modules/auth/AuthSecurityIntegrationTest.java`

- [ ] **Step 1: Add failing authorization tests for protected Web APIs**

Extend `AuthSecurityIntegrationTest.java` with cases for:
- authenticated but missing `api:audit:start` cannot start audit
- authenticated but missing `api:process-instance:approve` cannot approve
- authenticated but missing `api:workorder:dispatch` cannot dispatch
- authenticated with required permission succeeds

- [ ] **Step 2: Run targeted security tests to verify they fail**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuthSecurityIntegrationTest test`
Expected: FAIL because controllers are not guarded yet.

- [ ] **Step 3: Add controller-level permission checks**

Use `PermissionGuard` in each controller method or a small helper pattern. Make the mapping one-to-one and explicit in code for the current controller surface:

- `POST /api/events` -> `api:event:create`
- `GET /api/events/{id}` -> `api:event:detail`
- `GET /api/events` -> `api:event:list`
- `POST /api/audits/{eventId}/start` -> `api:audit:start`
- `GET /api/audits/{eventId}` -> `api:audit:detail`
- `POST /api/processes/instances/{id}/approve` -> `api:process-instance:approve`
- `POST /api/processes/instances/{id}/reject` -> `api:process-instance:reject`
- `POST /api/processes/templates` -> `api:process-template:create`
- `GET /api/processes/templates` -> `api:process-template:list`
- `POST /api/work-orders/{eventId}/dispatch` -> `api:workorder:dispatch`
- `POST /api/work-orders/{id}/confirm-close` -> `api:workorder:confirm-close`

If implementation reveals additional currently-live controller methods, add them to `PermissionCodes` and seed data in the same task rather than leaving them unmapped.

- [ ] **Step 4: Run security tests to verify they pass**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuthSecurityIntegrationTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/changping/platform/modules/event/controller/EventController.java src/main/java/com/changping/platform/modules/audit/controller/AuditController.java src/main/java/com/changping/platform/modules/process/controller/ProcessInstanceController.java src/main/java/com/changping/platform/modules/process/controller/ProcessTemplateController.java src/main/java/com/changping/platform/modules/workorder/controller/WorkOrderController.java src/test/java/com/changping/platform/modules/auth/AuthSecurityIntegrationTest.java
git commit -m "feat: enforce api permissions on web endpoints"
```

### Task 5: Migrate H5 business endpoints to real auth identity and API permissions

**Files:**
- Modify: `backend/src/main/java/com/changping/platform/modules/workorder/controller/H5WorkOrderController.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/workorder/service/impl/WorkOrderServiceImpl.java`
- Modify: `backend/src/test/java/com/changping/platform/modules/workorder/H5WorkOrderFlowTest.java`
- Test: `backend/src/test/java/com/changping/platform/modules/auth/AuthSecurityIntegrationTest.java`

- [ ] **Step 1: Add failing tests for H5 auth migration**

Cover:
- H5 endpoints reject unauthenticated requests
- H5 endpoints reject requests lacking `api:h5:*` permission
- H5 endpoints succeed with H5 token and permission
- ownership still rejects non-assignee even with permission
- legacy `X-Foundation-*` headers are no longer required when authenticated

Be explicit about migration intent in the tests:
- rewrite normal H5 business-flow tests to use authenticated bearer tokens as the primary path
- retain only one narrow compatibility test proving header fallback still works when no authenticated context exists, because that fallback belongs to transition safety rather than steady-state business flow

- [ ] **Step 2: Run H5 flow and security tests to verify they fail**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=H5WorkOrderFlowTest,AuthSecurityIntegrationTest test`
Expected: FAIL because H5 flow still assumes header-driven identity.

- [ ] **Step 3: Add H5 controller permission checks**

Map each H5 endpoint to:
- `api:h5:workbench:view`
- `api:h5:workorder:list`
- `api:h5:workorder:detail`
- `api:h5:workorder:accept`
- `api:h5:workorder:arrive`
- `api:h5:workorder:handle`
- `api:h5:workorder:verify`

- [ ] **Step 4: Switch service identity resolution to authenticated actor**

Because `FoundationActorResolver` now resolves from auth context first, update service/tests to stop relying on request headers for normal authenticated execution.
Retain ownership checks exactly as they are conceptually.

- [ ] **Step 5: Run H5 flow and security tests to verify they pass**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=H5WorkOrderFlowTest,AuthSecurityIntegrationTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/changping/platform/modules/workorder/controller/H5WorkOrderController.java src/main/java/com/changping/platform/modules/workorder/service/impl/WorkOrderServiceImpl.java src/test/java/com/changping/platform/modules/workorder/H5WorkOrderFlowTest.java src/test/java/com/changping/platform/modules/auth/AuthSecurityIntegrationTest.java
git commit -m "feat: migrate h5 flow to bearer auth"
```

### Task 6: Regress audit and dispatch flows under authenticated context

**Files:**
- Modify: `backend/src/main/java/com/changping/platform/modules/process/service/ProcessInstanceService.java`
- Modify: `backend/src/main/java/com/changping/platform/modules/workorder/service/impl/WorkOrderServiceImpl.java`
- Modify: `backend/src/test/java/com/changping/platform/modules/audit/AuditStartFlowTest.java`
- Modify: `backend/src/test/java/com/changping/platform/modules/workorder/DispatchWorkOrderFlowTest.java`

- [ ] **Step 1: Add failing regression tests for authenticated audit/dispatch actors**

Ensure tests prove:
- approve/reject action records use authenticated actor identity
- dispatch action records use authenticated actor identity
- no controller/service path trusts request JSON for operator identity

- [ ] **Step 2: Run targeted regression tests to verify they fail**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuditStartFlowTest,DispatchWorkOrderFlowTest test`
Expected: FAIL if current tests or services still assume pre-auth identity behavior.

- [ ] **Step 3: Implement minimal migration fixes in process/workorder services**

Keep changes narrow:
- continue using resolver abstraction
- ensure authenticated actor is recorded consistently in `ProcessInstanceService` approve/reject paths
- ensure authenticated actor is recorded consistently in `WorkOrderServiceImpl.dispatch(...)`
- do not broaden scope into unrelated refactors

- [ ] **Step 4: Run targeted regression tests to verify they pass**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuditStartFlowTest,DispatchWorkOrderFlowTest test`
Expected: PASS.

- [ ] **Step 5: Run combined backend auth + flow regression suite**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw -Dtest=AuthControllerTest,AuthSecurityIntegrationTest,AuditStartFlowTest,DispatchWorkOrderFlowTest,H5WorkOrderFlowTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/changping/platform/modules/process/service/ProcessInstanceService.java src/main/java/com/changping/platform/modules/workorder/service/impl/WorkOrderServiceImpl.java src/test/java/com/changping/platform/modules/audit/AuditStartFlowTest.java src/test/java/com/changping/platform/modules/workorder/DispatchWorkOrderFlowTest.java
git commit -m "fix: align business flows with authenticated actors"
```

---

## Chunk 3: Web frontend login, session, and RBAC UI

### Task 7: Add Web auth API, session store, and real login page

**Files:**
- Create: `web/src/api/http.ts`
- Create: `web/src/api/auth.ts`
- Create: `web/src/auth/session.ts`
- Create: `web/src/views/auth/LoginView.vue`
- Modify: `web/src/router/index.ts`
- Modify: `web/src/main.ts`
- Modify: `web/src/tests/app-shell.spec.ts`
- Modify: `web/src/tests/skeleton-pages.spec.ts`

- [ ] **Step 1: Write failing Web auth shell tests**

Extend `web/src/tests/app-shell.spec.ts` to cover:
- real login view rendering fields and submit button
- stored structured session allows protected navigation
- invalid stored session is rejected and cleared
- login redirect handling still works with structured session
- app bootstrap in `web/src/main.ts` (or extracted bootstrap helper) attempts `/api/auth/me` recovery when a token exists and clears session on recovery failure

Also update `web/src/tests/skeleton-pages.spec.ts` so it no longer depends on `setAuthenticated(...)` from the old boolean-only auth model.

- [ ] **Step 2: Run Web shell tests to verify they fail**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- --run src/tests/app-shell.spec.ts`
Expected: FAIL because Web only has placeholder login and boolean auth state.

- [ ] **Step 3: Implement shared HTTP client and auth API**

Before implementation, add a failing test around the HTTP layer covering:
- `ApiResponse<T>` unwrapping
- `Authorization: Bearer <token>` injection
- clearing session on 401/403 auth failure

`http.ts` responsibilities:
- create axios client
- inject bearer token from Web session
- unwrap `ApiResponse<T>`
- clear session on auth failure if needed

`auth.ts` responsibilities:
- `loginWeb`
- `logoutWeb`
- `fetchCurrentWebUser`

`session.ts` responsibilities:
- persist structured session
- validate stored session shape
- expose permission helpers

- [ ] **Step 4: Replace placeholder login view with real component**

`LoginView.vue` should:
- collect account/password
- submit to `/api/auth/login`
- show API error message
- redirect on success

- [ ] **Step 5: Update router and bootstrap to use structured session and recovery flow**

Stop using the boolean-only local storage key. Use the shared session helper instead.

Concretely:
- `web/src/router/index.ts` handles protected-route checks from structured session state
- `web/src/main.ts` (or a tiny extracted bootstrap helper it imports) runs one startup recovery pass via `/api/auth/me` when a token exists
- recovery failure must clear local session before rendering protected navigation assumptions

- [ ] **Step 6: Run Web shell tests to verify they pass**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- --run src/tests/app-shell.spec.ts`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/api/http.ts src/api/auth.ts src/auth/session.ts src/views/auth/LoginView.vue src/router/index.ts src/tests/app-shell.spec.ts
git commit -m "feat: add web real login and session handling"
```

### Task 8: Add Web route/menu/button permission enforcement

**Files:**
- Create: `web/src/auth/permissions.ts`
- Modify: `web/src/router/index.ts`
- Modify: `web/src/views/audit/AuditDetailView.vue`
- Modify: `web/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `web/src/tests/core-admin-pages.spec.ts`

- [ ] **Step 1: Write failing Web RBAC UI tests**

Cover:
- menu items hidden when corresponding `menu:*` permission is absent
- route guard blocks navigation without route permission
- audit approve/reject buttons hidden without `button:audit:*`
- work-order close-confirm actions hidden without matching `button:workorder:*` permissions
- sidebar items for current routes are filtered by `menu:*` permissions

- [ ] **Step 2: Run Web admin page tests to verify they fail**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- --run src/tests/core-admin-pages.spec.ts`
Expected: FAIL because menu and buttons are not permission-aware yet.

- [ ] **Step 3: Implement permission helpers and route metadata**

Add helpers like:
- `hasPermission(code)`
- `hasAnyPermission(codes)`

Add `meta.permission` to protected routes using `menu:*` codes.
Filter sidebar items from the same source of truth.

- [ ] **Step 4: Gate page buttons with permission helpers**

Use the smallest possible change in each page, and only target actions that actually exist in the current UI:
- `AuditDetailView.vue` -> approve / reject buttons
- `WorkOrderDetailView.vue` -> confirm-close / return buttons

Do not invent new process-template save/edit buttons in this chunk unless the page is first given a real action in a separate, explicit task.

- [ ] **Step 5: Run Web admin page tests to verify they pass**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- --run src/tests/core-admin-pages.spec.ts`
Expected: PASS.

- [ ] **Step 6: Run full Web test suite**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- --run`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/auth/permissions.ts src/router/index.ts src/views/audit/AuditDetailView.vue src/views/workorder/WorkOrderDetailView.vue src/views/process/ProcessTemplateEditView.vue src/tests/core-admin-pages.spec.ts
git commit -m "feat: add web menu and button permissions"
```

---

## Chunk 4: H5 frontend real login and permission-gated actions

### Task 9: Upgrade H5 auth client, session recovery, and bearer injection

**Files:**
- Create: `h5/src/api/http.ts`
- Modify: `h5/src/api/auth.ts`
- Modify: `h5/src/router/index.ts`
- Modify: `h5/src/main.ts`
- Modify: `h5/src/views/login/LoginView.vue`
- Modify: `h5/src/tests/h5-shell.spec.ts`

- [ ] **Step 1: Write failing H5 auth shell tests**

Cover:
- stored structured session with `userId/permissionCodes` survives refresh
- `/api/h5/auth/me` is invoked on startup when a token exists
- route guard uses recovered session state
- logout clears structured session correctly
- H5 route entry permissions are enforced for page navigation and tabbar visibility

Add separate failing client-layer tests (in this task or a nearby focused test file) for:
- `ApiResponse<T>` unwrapping
- `Authorization: Bearer <token>` injection
- auth-failure cleanup on 401/403

- [ ] **Step 2: Run H5 shell tests to verify they fail**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- --run src/tests/h5-shell.spec.ts`
Expected: FAIL because H5 auth client does not yet unwrap `ApiResponse<T>` or inject bearer token.

- [ ] **Step 3: Implement shared H5 HTTP client and upgrade auth client**

`h5/src/api/http.ts` responsibilities:
- create axios client
- inject bearer token from H5 session
- unwrap `ApiResponse<T>`
- centralize auth failure handling

Upgrade `h5/src/api/auth.ts` to:
- include `userId`, `roleCodes`, `permissionCodes`
- call `/api/h5/auth/me` for session recovery
- stop assuming raw axios `data` is the final login payload

- [ ] **Step 4: Update router, tabbar, bootstrap, and login view to use recovered structured session**

Concretely:
- `h5/src/main.ts` (or a tiny extracted bootstrap helper it imports) runs one startup recovery pass via `/api/h5/auth/me` when a token exists
- `h5/src/router/index.ts` enforces route entry permissions like `menu:h5:workbench:view`, `menu:h5:workorder:list`, `menu:h5:history:view`, `menu:h5:mine:view`
- H5 shell tabbar only renders links the current user can access
- keep redirect handling, but rely on structured session helpers and real me recovery path

- [ ] **Step 5: Run H5 shell tests to verify they pass**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- --run src/tests/h5-shell.spec.ts`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/api/http.ts src/api/auth.ts src/router/index.ts src/views/login/LoginView.vue src/tests/h5-shell.spec.ts
git commit -m "feat: add h5 real login and bearer session"
```

### Task 10: Add H5 permission-gated actions and regression coverage

**Files:**
- Create: `h5/src/auth/permissions.ts`
- Modify: `h5/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `h5/src/views/verify/VerifyView.vue`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`

- [ ] **Step 1: Write failing H5 permission UI tests**

Cover:
- accept button hidden without `button:h5:workorder:accept`
- arrive button hidden without `button:h5:workorder:arrive`
- handle submit hidden/disabled without `button:h5:workorder:handle`
- verify page gets an explicit submit action in this task, and that submit button is hidden/disabled without `button:h5:workorder:verify`
- tabbar/page entry links disappear when corresponding `menu:h5:*` permission is absent

- [ ] **Step 2: Run H5 page tests to verify they fail**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- --run src/tests/h5-closed-loop-pages.spec.ts`
Expected: FAIL because H5 pages are not permission-aware yet.

- [ ] **Step 3: Implement lightweight H5 permission helpers**

Add helper functions reading session permission codes. Keep H5 simple; do not invent a large menu framework.

- [ ] **Step 4: Gate H5 action buttons with permission helpers**

Apply minimal, explicit checks in:
- `WorkOrderDetailView.vue`
- `VerifyView.vue`

Because `VerifyView.vue` currently has no submit button, first add a minimal submit action in this task, then gate it with `button:h5:workorder:verify`. Do not leave the test targeting a non-existent control.

- [ ] **Step 5: Run H5 page tests to verify they pass**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- --run src/tests/h5-closed-loop-pages.spec.ts`
Expected: PASS.

- [ ] **Step 6: Run full H5 test suite**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- --run`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/auth/permissions.ts src/views/workorder/WorkOrderDetailView.vue src/views/verify/VerifyView.vue src/tests/h5-closed-loop-pages.spec.ts
git commit -m "feat: add h5 action permissions"
```

---

## Chunk 5: Final verification and docs touch-up

### Task 11: Run end-to-end verification across backend, Web, and H5 auth/RBAC flows

**Files:**
- Modify only if contracts or usage actually changed: `docs/architecture/phase1-endpoints.md`
- Modify only if startup/usage instructions actually changed: `README.md`

- [ ] **Step 1: Run backend auth and flow verification suite**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/backend && ./mvnw test`
Expected: PASS.

- [ ] **Step 2: Run Web verification suite**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- --run`
Expected: PASS.

- [ ] **Step 3: Run H5 verification suite**

Run: `cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- --run`
Expected: PASS.

- [ ] **Step 4: Validate acceptance criteria explicitly, not just by test-suite green state**

Before claiming completion, verify the shipped behavior matches the spec acceptance points:
- Web can log in via real backend auth
- H5 can log in via real backend auth
- Web/H5 requests carry Bearer token
- backend recognizes current user and permission set from token-derived auth context
- menu/button/API permissions actually take effect
- H5 still rejects non-assignee work-order operations
- unauthenticated and unauthorized access are rejected correctly
- existing phase-1 business main flow still works

Write down any failures and fix them before moving on.

- [ ] **Step 5: Update docs only if behavior/contracts changed materially**

If endpoint contracts, login payloads, startup steps, or auth assumptions differ from existing docs, update only the affected files:
- `docs/architecture/phase1-endpoints.md`
- `README.md`

Keep doc edits strictly scoped to auth/RBAC behavior introduced by this plan.

- [ ] **Step 6: Run targeted doc-adjacent sanity checks**

Run whichever suite corresponds to any changed code/docs paths.
Expected: PASS.

- [ ] **Step 7: Commit only if docs changed**

If and only if Step 5 changed documentation files:

```bash
git add README.md docs/architecture/phase1-endpoints.md
git commit -m "docs: document auth and rbac flows"
```

---

Plan complete and saved to `docs/superpowers/plans/2026-03-14-dual-end-auth-rbac.md`. Ready to execute?