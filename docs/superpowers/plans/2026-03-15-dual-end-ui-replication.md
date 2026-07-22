# Dual-End UI 1:1 Replication Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Status:** This plan supersedes `docs/superpowers/plans/2026-03-15-dual-end-figma-replication.md` as the primary frontend implementation plan.

**Goal:** Rebuild the current `web/` and `h5/` frontends so they visually and structurally match `changping-smart-city-ui.html` as closely as possible, while preserving existing route paths, auth guards, permission semantics, redirect behavior, and current API contracts wherever possible.

**Architecture:** Treat `changping-smart-city-ui.html` as the canonical UI baseline. First land a shared dual-end visual system that reproduces the approved deep-blue government-tech style, then map every target HTML page onto the existing Web and H5 route structure. Prefer adapting current routes/components over inventing a new frontend architecture. Frontend completion comes before backend expansion; where the HTML includes unsupported real data or workflow details, use realistic placeholder presentation backed by current APIs and record the gap explicitly.

**Tech Stack:** Vue 3, TypeScript, Vue Router 4, Vite, Vitest, Testing Library, Element Plus (Web), Vant (H5), existing auth/session helpers, existing API modules, static reference UI at `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`

---

## Canonical UI Baseline

Reference source:
- `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`

The HTML currently defines these target pages:

### Big screen / Web pages
1. 综合监管总览大屏
2. Web 登录页
3. Web 首页驾驶舱
4. Web 审核中心 / 事件列表
5. Web 审核详情 / 证据 / 流程送审
6. Web 工单中心
7. Web 工单详情 / 流转记录
8. Web 无人机接入档案
9. Web 飞行 / 巡检任务计划
10. Web AI 模型 / 阈值配置
11. Web 媒体上传与识别结果管理
12. Web 审核中心 / 流程配置
13. Web 系统配置 / 接口预留

### App / H5 pages
14. App 登录
15. App 任务工作台
16. App 任务列表
17. App 任务详情 / 商户信息
18. App 图片 / 视频上传
19. App 我的 / 历史记录

---

## Implementation Principles

- Keep existing route paths unless the current app truly has no matching route.
- Keep current login, logout, redirect, guard, and permission continuity green.
- Do not block frontend work on missing backend capabilities; instead render the correct shell, sections, cards, tables, and action areas using current data sources or realistic placeholders.
- Reuse current page routes when the target HTML page is a visual/structural remap of an existing route.
- Create new routes only for HTML pages that have no current equivalent and are required to complete the canonical UI set.
- Land Web and H5 visual tokens/layout shells before page-by-page rebuilds.
- Every page rebuild must update focused tests and at least one route/integration assertion.
- Record mismatches between HTML target and real current capability in a dedicated deviations log rather than silently dropping sections.

---

## Route Mapping

### Web mapping

| Target HTML page | Current implementation target |
| --- | --- |
| Web 登录页 | `web/src/views/auth/LoginView.vue` |
| Web 首页驾驶舱 | `web/src/views/dashboard/DashboardView.vue` |
| Web 审核中心 / 事件列表 | `web/src/views/audit/AuditListView.vue` and/or `web/src/views/event/EventListView.vue` depending on actual menu wording retained |
| Web 审核详情 / 证据 / 流程送审 | `web/src/views/audit/AuditDetailView.vue` |
| Web 工单中心 | `web/src/views/workorder/WorkOrderListView.vue` |
| Web 工单详情 / 流转记录 | `web/src/views/workorder/WorkOrderDetailView.vue` |
| Web 无人机接入档案 | `web/src/views/drone/DroneListView.vue` |
| Web 飞行 / 巡检任务计划 | `web/src/views/patrol/PatrolTaskListView.vue`, `web/src/views/patrol/PatrolTaskDetailView.vue` if needed |
| Web AI 模型 / 阈值配置 | Prefer remapping existing relevant admin/system page; if no current equivalent, add dedicated view under `web/src/views/system/` |
| Web 媒体上传与识别结果管理 | Prefer remapping existing relevant admin page; if no current equivalent, add dedicated view under `web/src/views/system/` or `web/src/views/media/` |
| Web 审核中心 / 流程配置 | `web/src/views/process/ProcessTemplateListView.vue`, `web/src/views/process/ProcessTemplateEditView.vue` |
| Web 系统配置 / 接口预留 | `web/src/views/system/SystemConfigView.vue` |

### H5 / App mapping

| Target HTML page | Current implementation target |
| --- | --- |
| App 登录 | `h5/src/views/login/LoginView.vue` |
| App 任务工作台 | `h5/src/views/workbench/WorkbenchView.vue` |
| App 任务列表 | `h5/src/views/workorder/WorkOrderListView.vue` |
| App 任务详情 / 商户信息 | `h5/src/views/workorder/WorkOrderDetailView.vue` |
| App 图片 / 视频上传 | Prefer extending `h5/src/views/verify/VerifyView.vue`; if structurally too different, add dedicated upload route/view |
| App 我的 / 历史记录 | `h5/src/views/mine/MineView.vue` and `h5/src/views/history/HistoryView.vue` |

### Big screen mapping

| Target HTML page | Current implementation target |
| --- | --- |
| 综合监管总览大屏 | Prefer remapping `web/src/views/map/MapOverviewView.vue`; if no current route exists, add one under Web with existing guard/nav semantics |

---

## Shared Visual System Requirements

The existing frontend does **not** yet fully match the canonical HTML. Before page-by-page work, land these shared style anchors:

### Web visual system
- Deep navy background gradients
- Dark topbar with product title and right-side status/account area
- Dark left sidebar with active-item glow treatment
- Dark business-page content background
- Unified panel style: thin blue borders, soft glow, rounded 18px–28px corners
- Unified query panel style
- Unified dark table panel and dark header row style
- Unified tags, ghost buttons, primary action buttons, stats cards, detail blocks, and flow-step visuals

### H5/App visual system
- Mobile deep-blue gradient app shell
- Shared mobile hero card pattern
- Shared mobile task card/list pattern
- Shared mobile photo/upload block pattern
- Shared bottom action button row pattern
- Shared small status chips/tags and summary cards

### Big screen visual system
- Full-width dark screen container
- Metric cards, map glow/grid treatment, left-right side panels, alert list, and ticker-like info panels consistent with the HTML

---

## File Structure

### Existing Web files to modify
- Modify: `web/src/layouts/AdminShellLayout.vue`
- Modify: `web/src/router/index.ts`
- Modify: `web/src/views/auth/LoginView.vue`
- Modify: `web/src/views/dashboard/DashboardView.vue`
- Modify: `web/src/views/event/EventListView.vue`
- Modify: `web/src/views/event/EventDetailView.vue`
- Modify: `web/src/views/audit/AuditListView.vue`
- Modify: `web/src/views/audit/AuditDetailView.vue`
- Modify: `web/src/views/workorder/WorkOrderListView.vue`
- Modify: `web/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `web/src/views/drone/DroneListView.vue`
- Modify: `web/src/views/patrol/PatrolTaskListView.vue`
- Modify: `web/src/views/patrol/PatrolTaskDetailView.vue`
- Modify: `web/src/views/process/ProcessTemplateListView.vue`
- Modify: `web/src/views/process/ProcessTemplateEditView.vue`
- Modify: `web/src/views/system/SystemConfigView.vue`
- Modify if used: `web/src/views/map/MapOverviewView.vue`
- Modify supporting shared components/templates under `web/src/components/admin/` and `web/src/templates/`

### Existing H5 files to modify
- Modify: `h5/src/layouts/MobileShellLayout.vue`
- Modify: `h5/src/router/index.ts`
- Modify: `h5/src/views/login/LoginView.vue`
- Modify: `h5/src/views/workbench/WorkbenchView.vue`
- Modify: `h5/src/views/workorder/WorkOrderListView.vue`
- Modify: `h5/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `h5/src/views/verify/VerifyView.vue`
- Modify: `h5/src/views/history/HistoryView.vue`
- Modify: `h5/src/views/mine/MineView.vue`
- Modify supporting shared templates/components under `h5/src/templates/` and `h5/src/components/`

### Existing tests to modify
- Modify the focused view tests already present under `web/src/views/**/**/*.test.ts`
- Modify: `web/src/layouts/AdminShellLayout.test.ts`
- Modify: `web/src/tests/app-shell.spec.ts`
- Modify: `web/src/tests/core-admin-pages.spec.ts`
- Modify the focused view tests already present under `h5/src/views/**/**/*.test.ts`
- Modify: `h5/src/layouts/MobileShellLayout.test.ts`
- Modify: `h5/src/tests/h5-shell.spec.ts`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`

### Tracking docs to modify
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-matrix.md`
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-deviations.md`
- Create if needed: `docs/superpowers/tracking/visual-acceptance/*.md`

### New files to create only when current structure cannot hold the target UI cleanly
- Create if needed: `web/src/components/ui-shell/*`
- Create if needed: `web/src/components/ui-blocks/*`
- Create if needed: `h5/src/components/ui-shell/*`
- Create if needed: `h5/src/components/ui-blocks/*`
- Create if needed: new Web view files for target pages that currently have no equivalent
- Create if needed: new H5 upload-focused route/view if `VerifyView` cannot cleanly host the App 上传 page

Rule: prefer extracting only repeated visual blocks used by 2+ pages. Do not create one-off abstractions.

---

## Phase 1: Freeze the new scope and target mapping

### Task 1: Replace the old first-batch-only scope with the full frontend target set

**Files:**
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-matrix.md`
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-deviations.md`

- [ ] **Step 1: Remove first-batch-only framing**

Rewrite the matrix intro so it tracks the full frontend UI replication effort, not only the previous frozen subset.

- [ ] **Step 2: Rewrite the route/page inventory to match the canonical UI baseline**

The matrix must explicitly track:
- big screen page
- all target Web pages
- all target H5/App pages

- [ ] **Step 3: Mark which pages are route remaps vs true new pages**

Add a column or note indicating:
- existing route remap
- existing route expanded
- new route required

- [ ] **Step 4: Reset implementation status to reflect real current state**

Do not keep earlier `已完成` labels if the page is only partially aligned to the canonical HTML.

- [ ] **Step 5: Create/update deviation categories**

Use at least:
- visual mismatch
- missing backend data
- route mismatch
- permission mismatch
- component gap

---

## Phase 2: Land the shared Web and H5 visual systems

### Task 2: Rebuild the Web shell and global business-page style system

**Files:**
- Modify: `web/src/layouts/AdminShellLayout.vue`
- Modify supporting shared Web components/templates/styles
- Modify: `web/src/layouts/AdminShellLayout.test.ts`
- Modify: `web/src/tests/app-shell.spec.ts`

- [ ] **Step 1: Tighten shell tests before implementation**

Require the shell to expose:
- dark topbar
- dark left navigation
- account area
- logout action
- main content region

- [ ] **Step 2: Rebuild the Web shell to match the canonical HTML frame**

Keep:
- current route paths
- menu permission filtering
- logout behavior
- login redirect continuity

Add:
- topbar title/status/account layout
- dark left menu presentation
- dark content background
- page container treatment matching the canonical HTML

- [ ] **Step 3: Extract only the repeated visual primitives**

Examples:
- stat card
- query panel
- table panel
- dark tag
- ghost action button
- flow step card
- detail block

- [ ] **Step 4: Re-run shell and route continuity tests until green**

### Task 3: Rebuild the H5 shell and global mobile style system

**Files:**
- Modify: `h5/src/layouts/MobileShellLayout.vue`
- Modify supporting H5 shared templates/components/styles
- Modify: `h5/src/layouts/MobileShellLayout.test.ts`
- Modify: `h5/src/tests/h5-shell.spec.ts`

- [ ] **Step 1: Tighten H5 shell tests first**

Require:
- deep-blue mobile shell
- top header/user area
- tabbar continuity
- logout continuity

- [ ] **Step 2: Rebuild the H5 shell to match the canonical mobile frame**

Keep:
- current tab filtering
- current logout flow
- route guard behavior
- redirect fallback behavior

Add:
- mobile deep-blue app frame
- shared hero/header presentation
- shared bottom action area spacing and shell padding

- [ ] **Step 3: Extract repeated mobile visual primitives**

Examples:
- hero card
- summary card
- task card
- upload/photo block
- bottom action row

- [ ] **Step 4: Re-run shell tests until green**

---

## Phase 3: Rebuild core Web pages to the canonical HTML

### Task 4: Rebuild Web login and dashboard

**Files:**
- Modify: `web/src/views/auth/LoginView.vue`
- Modify: `web/src/views/auth/LoginView.test.ts`
- Modify: `web/src/views/dashboard/DashboardView.vue`
- Modify: `web/src/views/dashboard/DashboardView.test.ts`
- Modify: `web/src/tests/app-shell.spec.ts`
- Modify: `web/src/tests/core-admin-pages.spec.ts`

- [ ] **Step 1: Rebuild Web login to the canonical dark login page**

Keep current login API and redirect semantics.

- [ ] **Step 2: Rebuild dashboard to the canonical driving-cockpit structure**

Require these sections:
- KPI summary
- shortcuts
- latest alerts
- operational heat/map panel
- process/flow summary

- [ ] **Step 3: Cover default/loading/empty/error states where applicable**

- [ ] **Step 4: Re-run focused tests and integration tests until green**

### Task 5: Rebuild Web audit and work-order chains

**Files:**
- Modify: `web/src/views/audit/AuditListView.vue`
- Modify: `web/src/views/audit/AuditListView.test.ts`
- Modify: `web/src/views/audit/AuditDetailView.vue`
- Modify: `web/src/views/audit/AuditDetailView.test.ts`
- Modify: `web/src/views/workorder/WorkOrderListView.vue`
- Modify: `web/src/views/workorder/WorkOrderListView.test.ts`
- Modify: `web/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `web/src/views/workorder/WorkOrderDetailView.test.ts`
- Modify: `web/src/tests/core-admin-pages.spec.ts`

- [ ] **Step 1: Rebuild the audit list into the canonical query + dark table page**

Require:
- query panel
- action toolbar
- table with statuses/actions
- sections/labels aligned to the HTML target

- [ ] **Step 2: Rebuild the audit detail into the canonical evidence + flow + action layout**

Require:
- evidence gallery
- review info
- map/location block
- flow nodes
- review action area

- [ ] **Step 3: Rebuild the work-order list into the canonical list page**

Require:
- query panel
- work-order table
- current owner/area/status/timeout columns or closest equivalent from current data

- [ ] **Step 4: Rebuild the work-order detail into the canonical flow/detail page**

Require:
- basic info
- flow timeline
- on-site feedback
- close-out action area

- [ ] **Step 5: Re-run focused tests and integration tests until green**

### Task 6: Rebuild additional Web admin pages required by the canonical HTML

**Files:**
- Modify: `web/src/views/drone/DroneListView.vue`
- Modify: `web/src/views/patrol/PatrolTaskListView.vue`
- Modify if needed: `web/src/views/patrol/PatrolTaskDetailView.vue`
- Modify: `web/src/views/process/ProcessTemplateListView.vue`
- Modify: `web/src/views/process/ProcessTemplateEditView.vue`
- Modify: `web/src/views/system/SystemConfigView.vue`
- Modify if needed: `web/src/views/map/MapOverviewView.vue`
- Create only if needed: missing Web views for AI model/media/auth pages
- Add/update focused tests for each landed page

- [ ] **Step 1: Rebuild 无人机接入档案 page**

- [ ] **Step 2: Rebuild 飞行 / 巡检任务计划 page**

- [ ] **Step 3: Rebuild 审核中心 / 流程配置 page**

- [ ] **Step 4: Rebuild 系统配置 / 接口预留 page**

- [ ] **Step 5: Land missing canonical pages not covered by current routes**

At minimum evaluate and land:
- AI 模型 / 阈值配置
- 媒体上传与识别结果管理
- 授权管理 / 动态菜单
- 综合监管总览大屏

- [ ] **Step 6: Add route entries, nav items, and permissions only as minimally required**

Do not break current auth/permission patterns while adding missing canonical pages.

- [ ] **Step 7: Re-run affected tests and route checks until green**

---

## Phase 4: Rebuild H5/App pages to the canonical HTML

### Task 7: Rebuild H5 login, workbench, list, and detail pages

**Files:**
- Modify: `h5/src/views/login/LoginView.vue`
- Modify: `h5/src/views/login/LoginView.test.ts`
- Modify: `h5/src/views/workbench/WorkbenchView.vue`
- Modify: `h5/src/views/workbench/WorkbenchView.test.ts`
- Modify: `h5/src/views/workorder/WorkOrderListView.vue`
- Modify: `h5/src/views/workorder/WorkOrderListView.test.ts`
- Modify: `h5/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `h5/src/views/workorder/WorkOrderDetailView.test.ts`
- Modify: `h5/src/tests/h5-shell.spec.ts`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`

- [ ] **Step 1: Rebuild App 登录 page**

Keep current login and redirect continuity.

- [ ] **Step 2: Rebuild App 任务工作台 page**

Require:
- summary hero
- pending tasks
- urgent alerts
- quick actions
- assignment/task cards

- [ ] **Step 3: Rebuild App 任务列表 page**

Require:
- state tabs or equivalent segmented filtering
- task cards
- time/distance/priority info

- [ ] **Step 4: Rebuild App 任务详情 / 商户信息 page**

Require:
- task detail
- merchant/contact/location info
- evidence/notes area
- submit or next-step actions

- [ ] **Step 5: Re-run focused tests and H5 integration tests until green**

### Task 8: Rebuild App 上传 and 我的 / 历史 pages

**Files:**
- Modify: `h5/src/views/verify/VerifyView.vue`
- Modify: `h5/src/views/verify/VerifyView.test.ts`
- Modify: `h5/src/views/history/HistoryView.vue`
- Modify: `h5/src/views/history/HistoryView.test.ts`
- Modify: `h5/src/views/mine/MineView.vue`
- Modify: `h5/src/views/mine/MineView.test.ts`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`
- Create only if needed: dedicated upload view and its test

- [ ] **Step 1: Decide whether VerifyView can host the canonical 上传 page**

If yes, remap it visually and structurally.
If no, add a dedicated upload route/view and keep verify flow continuity elsewhere.

- [ ] **Step 2: Rebuild App 图片 / 视频上传 page**

Require:
- upload target context
- preview area
- file/media block
- save/submit actions

- [ ] **Step 3: Rebuild 我的 / 历史记录 experience**

Require:
- personal summary
- history entry or separate history page continuity
- settings/service blocks
- logout continuity

- [ ] **Step 4: Re-run focused tests and H5 integration tests until green**

---

## Phase 5: Final reconciliation, visual acceptance, and verification

### Task 9: Reconcile the tracking docs against real landed pages

**Files:**
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-matrix.md`
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-deviations.md`
- Create: `docs/superpowers/tracking/visual-acceptance/*.md` as needed

- [ ] **Step 1: Update every tracked page with real implementation status**

- [ ] **Step 2: Record every approved mismatch exactly once in deviations**

Examples:
- missing backend field
- permission label mismatch
- route mapping compromise
- upload widget placeholder
- big-screen route newly added but still demo-data-backed

- [ ] **Step 3: Create one visual acceptance note per landed canonical page**

Each note must contain:
```md
# <page or route> visual acceptance
- Canonical HTML section:
- Implemented route/view:
- Default-state checklist:
- Required-state checklist:
- Remaining deviations:
- 1:1 completion verdict:
```

### Task 10: Run final verification suites

- [ ] **Step 1: Run Web build**

```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm run build
```

- [ ] **Step 2: Run H5 build**

```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm run build
```

- [ ] **Step 3: Run Web verification tests**

Run focused route/layout/view suites covering the rebuilt Web pages.

- [ ] **Step 4: Run H5 verification tests**

Run focused route/layout/view suites covering the rebuilt H5 pages.

- [ ] **Step 5: Run diagnostics on affected directories/files**

Use LSP diagnostics to ensure zero errors on affected code.

- [ ] **Step 6: Manual sanity review**

Verify:
- canonical HTML page inventory is represented in the app
- Web shell matches the dark deep-blue target family
- H5 shell matches the mobile target family
- login/logout/redirect continuity still works
- route/permission semantics are not accidentally broken

---

## Definition of Done

The frontend replication is only complete when all of the following are true:

- [ ] Canonical HTML pages are all mapped to landed Web/H5 routes or explicitly recorded as approved deviations
- [ ] Shared Web shell and H5 shell visually match the canonical UI family
- [ ] Core Web pages are rebuilt to the canonical structures
- [ ] Additional Web admin pages required by the HTML are landed or explicitly deferred with approval
- [ ] Core H5/App pages are rebuilt to the canonical structures
- [ ] Upload / history / mine experience is landed consistently with the canonical mobile UI
- [ ] Matrix reflects real status rather than earlier partial-completion labels
- [ ] Visual acceptance notes exist for landed canonical pages
- [ ] Deviations log records every remaining mismatch exactly once
- [ ] Web build passes
- [ ] H5 build passes
- [ ] Relevant Web tests pass
- [ ] Relevant H5 tests pass
- [ ] LSP diagnostics show 0 errors on affected files
- [ ] Final reviewer verification confirms the frontend now follows `changping-smart-city-ui.html` rather than the older first-batch-only target
