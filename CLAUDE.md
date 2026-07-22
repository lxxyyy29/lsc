# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

This workspace contains three sibling applications that implement one event-governance platform:

- `backend/` — Spring Boot 3.3 / Java 17 API service on port `8080`, served under `/api`
- `web/` — Vue 3 + TypeScript + Vite admin application on port `5173`
- `h5/` — Vue 3 + TypeScript + Vite mobile field application on port `5174`
- `docs/architecture/` — delivery contract and manual verification docs for the current phase

Start with `docs/architecture/phase1-endpoints.md` when you need the current backend/frontend contract.

## Common commands

Run commands from each app directory.

### Backend (`backend/`)

- Start dev server: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw -Dtest=H5WorkOrderFlowTest test`
- Run a single test method: `./mvnw -Dtest=H5WorkOrderFlowTest#shouldAcceptWorkOrder test`
- Package app: `./mvnw package`

### Web admin (`web/`)

- Start dev server: `npx pnpm dev --host 0.0.0.0 --port 5173`
- Run all tests: `npx pnpm test`
- Run a single test file: `npx pnpm test -- src/tests/auth-session.spec.ts`
- Build production bundle: `npx pnpm build`

### H5 (`h5/`)

- Start dev server: `npx pnpm dev --host 0.0.0.0 --port 5174`
- Run all tests: `npx pnpm test`
- Run a single test file: `npx pnpm test -- src/tests/h5-closed-loop-pages.spec.ts`
- Build production bundle: `npx pnpm build`

Notes:

- Both frontend apps run tests through `node ./scripts/run-vitest.mjs`, which forwards extra CLI args to `vitest run`.
- This environment may not expose a global `pnpm` binary, so prefer `npx pnpm ...`.

## High-level architecture

### Core business flow

The main product flow spans all three apps:

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

- `web/src/main.ts`

Routing and access control:

- `web/src/router/index.ts`

Key patterns:

- session is recovered before app mount
- router guards enforce auth and menu-permission access
- Element Plus is the UI library
- `AdminShellLayout` hosts route-driven admin pages

Main Web domains include dashboard, events, audits, process templates, work orders, patrol tasks, drones, map oversight, and system configuration.

### H5 structure

H5 app bootstrap:

- `h5/src/main.ts`

Routing and access control:

- `h5/src/router/index.ts`

Key patterns:

- session is recovered before app mount
- router guards enforce auth and menu-permission access
- Vant is the UI library
- `MobileShellLayout` hosts the main field workflow

Main H5 domains include workbench, work-order list/detail, verification, history, and mine/profile.

## Auth and permission model

The current steady-state auth model is bearer-token based:

- Web login: `POST /api/auth/login`
- H5 login: `POST /api/h5/auth/login`
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

- `docs/architecture/phase1-endpoints.md` — current API contract and workflow mapping
- `docs/architecture/phase1-verification-checklist.md` — manual verification checklist and known gaps

Important existing tests called out by the docs:

- `backend/src/test/java/com/changping/platform/modules/workorder/DispatchWorkOrderFlowTest.java`
- `backend/src/test/java/com/changping/platform/modules/workorder/H5WorkOrderFlowTest.java`
- `h5/src/tests/h5-closed-loop-pages.spec.ts`

## UI and styling conventions

Both frontend apps import shared Figma-oriented tokens:

- `web/src/styles/figma-tokens.css`
- `h5/src/styles/figma-tokens.css`

There are also Figma/design-derived templates and planning artifacts under `docs/superpowers/`, but those are supporting design/history materials rather than the runtime source of truth.
