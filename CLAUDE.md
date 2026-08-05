# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

This workspace contains four sibling applications that implement one event-governance platform:

- `backend/` — Spring Boot 3.3 / Java 17 API service on port `8080`, served under `/api`
- `web-v2/` — Vue 3 + TypeScript + Vite admin application on port `5175`（管理人员使用）
- `h5/` — Vue 3 + uni-app mobile field application on port `5174`（网格员使用）
- `mp_mysql_test/` — Vue 3 + TypeScript + Vite mini-program on port `5176`（居民随手拍）
- `docs/architecture/` — delivery contract and manual verification docs for the current phase

Start with `docs/系统说明文档.md` for the current API reference (315 endpoints as of 2026-08-05, Flyway V78). `docs/architecture/phase1-endpoints.md` is the historical Phase1 contract, not the current state.

## Common commands

Run commands from each app directory.

### Backend (`backend/`)

- Start dev server: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw -Dtest=PermissionGuardTest test`
- Package app: `./mvnw package`

### Web admin (`web-v2/`)

- Start dev server: `npx pnpm dev --host 0.0.0.0 --port 5175`
- Build production bundle: `npx pnpm build`

### H5 (`h5/`)

- Start dev server: `npx pnpm dev --host 0.0.0.0 --port 5174`
- Run all tests: `npx pnpm test`（`src/tests/http.spec.ts`、`src/tests/navigation.spec.ts`）
- Build production bundle: `npx pnpm build`

### Mini-program (`mp_mysql_test/`)

- Start dev server: `npx pnpm dev --host 0.0.0.0 --port 5176`
- Build production bundle: `npx pnpm build`

Notes:

- H5 tests run through `node ./scripts/run-vitest.mjs`, which forwards extra CLI args to `vitest run`.
- This environment may not expose a global `pnpm` binary, so prefer `npx pnpm ...`.
- The backend currently has a single test class: `backend/src/test/java/com/changping/platform/modules/auth/security/PermissionGuardTest.java`.

## High-level architecture

### Core business flow

The main product flow spans all four apps:

1. event intake
2. audit start and process-node approval
3. dispatch to a work order
4. H5 assignee accepts / arrives / handles / verifies
5. Web confirms close or rejects back to processing

To understand a feature, read across backend controllers/services plus the corresponding `web/` or `h5/` API layer and views. This is a cross-app workflow, not three isolated codebases.

### Backend structure

Backend entry point:

- `backend/src/main/java/com/changping/platform/Application.java`

Important backend modules:

- `modules/auth` — login, current-user lookup, JWT, permission checks, client-type separation for Web vs H5
- `modules/event` — event intake and event detail
- `modules/audit` — starting audit against an event
- `modules/process` — process templates, process instances, approve/reject node transitions
- `modules/workorder` — dispatch, H5 assignee actions, close confirmation
- `common/response` — unified API response envelope
- `common/exception` — global exception normalization
- `common/security` — security config and actor resolution

Important implementation detail: although MyBatis-Plus is present in `pom.xml`, much of the business logic is implemented with explicit SQL and state updates in service classes, especially work-order flow logic. Do not assume a JPA-style repository architecture.

Files to inspect first for backend behavior:

- `backend/src/main/java/com/changping/platform/common/security/SecurityConfig.java`
- `backend/src/main/java/com/changping/platform/modules/auth/security/BearerTokenAuthenticationFilter.java`
- `backend/src/main/java/com/changping/platform/modules/auth/security/PermissionGuard.java`
- `backend/src/main/java/com/changping/platform/modules/workorder/service/impl/WorkOrderServiceImpl.java`
- `backend/src/main/resources/db/migration/`

### Web admin structure

Web app bootstrap:

- `web-v2/src/main.ts`

Routing and access control:

- `web-v2/src/router/index.ts`

Key patterns:

- session is recovered before app mount
- router guards enforce auth and menu-permission access
- Element Plus is the UI library
- `AdminShellLayout` hosts route-driven admin pages

Main Web domains include dashboard, big screen, events, audits, process templates, work orders, patrol tasks, drones, map oversight, org members, biz areas, resident reports, policy resources, and system configuration.

### H5 structure

H5 app bootstrap:

- `h5/main.ts`

Pages and bottom tab bar:

- `h5/pages.json` (uni-app page registry)
- `h5/src/navigation.ts` (navigation entries gated by `menu:h5:*` permissions)

Key patterns:

- session is recovered before app mount
- router guards enforce auth and menu-permission access
- Vant is the UI library
- `MobileShellLayout` hosts the main field workflow

Main H5 domains include workbench, work-order list/detail, verification, history, patrol checkin, merchant/vendor management, map, message (chat entry hidden for now), and mine/profile.

### Mini-program (居民小程序) structure

Mini-program app bootstrap:

- `mp_mysql_test/src/main.ts`

Routing and access control:

- `mp_mysql_test/src/router/index.ts`

Key patterns:

- Bottom tab bar navigation (report/history/mine)
- Session stored in localStorage as `grid-mp-session`
- Uses same JWT bearer token flow as Web
- Proxy `/api` → `http://localhost:8080` via Vite config

Main mini-program features: resident report (随手拍), report history with status tracking, event rating (1-5 stars), community services (activities/repairs/policies/points), user profile.

## Auth and permission model

The current steady-state auth model is bearer-token based:

- Web login: `POST /api/auth/login`
- H5 login: `POST /api/h5/auth/login`
- Mini-program login: `POST /api/auth/login` (with clientType: web)
- Public report (免认证): `POST /api/events/public-report`
- protected requests send `Authorization: Bearer <token>`

Important conventions:

- backend responses use a unified envelope: `success`, `code`, `message`, `data`
- Web and H5 are separate client types with separate permission surfaces
- frontend navigation is gated by `menu:*` permissions
- backend API access is gated by explicit permission checks
- H5 work-order actions also enforce assignee ownership in the service layer

The legacy `X-Foundation-*` headers still exist only as a fallback/testing path; use the bearer-token flow for real integration work.

## Data and schema notes

- Backend config lives in `backend/src/main/resources/application.yml`
- Flyway migrations in `backend/src/main/resources/db/migration/` are the source of truth for schema evolution
- Treat `application.yml` as sensitive: it contains concrete fallback connection settings and secrets that should not be copied into docs, summaries, or commits

## Testing and verification entry points

Useful references:

- `docs/系统说明文档.md` — current full API reference (updated 2026-08-05)
- `docs/architecture/phase1-endpoints.md` — historical Phase1 API contract and workflow mapping
- `docs/architecture/phase1-verification-checklist.md` — manual verification checklist and known gaps

Existing backend test:

- `backend/src/test/java/com/changping/platform/modules/auth/security/PermissionGuardTest.java`

H5 page tests:

- `h5/src/tests/` (run via `npx pnpm test`)

## UI and styling conventions

All frontend apps import shared Figma-oriented tokens:

- `web-v2/src/styles/figma-tokens.css`
- `h5/src/styles/figma-tokens.css`

There are also Figma/design-derived templates and planning artifacts under `docs/superpowers/`, but those are supporting design/history materials rather than the runtime source of truth.
