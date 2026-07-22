# Dual-End Figma Replication Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the approved first-batch Web and H5 pages to match the Figma baseline 1:1 while preserving current routes, permissions, API contracts, redirect semantics, and closed-loop flows.

**Architecture:** Keep existing route paths and data contracts, but replace the current shell and page structures with Figma-aligned shells, shared page sections, and route-specific page layouts. Execute by frozen page groups: Web shell+login, Web dashboard+events, Web audits+work orders, H5 shell+login, H5 workbench+work orders, and H5 verify+history+mine. Every task must update focused tests, shell/integration tests, and the tracking matrix together.

**Tech Stack:** Vue 3, TypeScript, Vue Router 4, Vite, Vitest, Testing Library, Element Plus (Web), Vant (H5), existing auth/session helpers, existing API modules

---

## File Structure

### Existing files to modify

#### Shared tracking artifacts
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-matrix.md`
  - Re-mark first-batch routes only when they actually satisfy the new 1:1 standard.
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-deviations.md`
  - Record only approved gaps or unsupported Figma elements inside frozen pages.
- Create if needed: `docs/superpowers/tracking/visual-acceptance/web-*.md`
  - One visual acceptance package per frozen Web route.
- Create if needed: `docs/superpowers/tracking/visual-acceptance/h5-*.md`
  - One visual acceptance package per frozen H5 route.

#### Web shell and pages
- Modify: `web/src/layouts/AdminShellLayout.vue`
  - Rebuild to match the approved Web shell baseline, add account area and logout entry, and keep permission-filtered navigation.
- Modify: `web/src/router/index.ts`
  - Preserve current route paths, auth guards, redirect semantics, and permission fallback while keeping the shell route structure stable, and verify them explicitly when the shell/login work lands.
- Modify: `web/src/views/auth/LoginView.vue`
  - Rebuild to match Figma node `1:298` while preserving `loginWeb()` and redirect behavior.
- Modify: `web/src/views/dashboard/DashboardView.vue`
  - Rebuild to match Figma node `1:376`.
- Modify: `web/src/views/event/EventListView.vue`
  - Rebuild to match Figma node `1:621`.
- Modify: `web/src/views/event/EventDetailView.vue`
  - Rebuild to match Figma node `1:773`, keeping evidence and flow-send sections inside the detail route.
- Modify: `web/src/views/audit/AuditListView.vue`
  - Rebuild to match Figma node `1:621`.
- Modify: `web/src/views/audit/AuditDetailView.vue`
  - Rebuild to match Figma node `1:773`, keeping evidence and flow-send sections inside the detail route.
- Modify: `web/src/views/workorder/WorkOrderListView.vue`
  - Rebuild to match Figma node `1:952`.
- Modify: `web/src/views/workorder/WorkOrderDetailView.vue`
  - Rebuild to match Figma node `1:1079`.

#### H5 shell and pages
- Modify: `h5/src/layouts/MobileShellLayout.vue`
  - Rebuild to match the approved H5 shell baseline, keep tabbar permission filtering, and keep shell logout behavior.
- Modify: `h5/src/router/index.ts`
  - Preserve route paths, auth guards, redirect/fallback semantics, and permission fallback behavior, and verify them explicitly when the shell/login and closed-loop work lands.
- Modify: `h5/src/views/login/LoginView.vue`
  - Rebuild to match Figma node `1:2148` while preserving `loginH5()` and redirect behavior.
- Modify: `h5/src/views/workbench/WorkbenchView.vue`
  - Rebuild to match Figma node `1:2194`.
- Modify: `h5/src/views/workorder/WorkOrderListView.vue`
  - Rebuild to match Figma node `1:2272`.
- Modify: `h5/src/views/workorder/WorkOrderDetailView.vue`
  - Rebuild to match Figma node `1:2358`.
- Modify: `h5/src/views/verify/VerifyView.vue`
  - Rebuild to match Figma node `1:2425`, keeping verify permissions and loading/empty/error/read-only states.
- Modify: `h5/src/views/history/HistoryView.vue`
  - Rebuild using the history region of Figma node `1:2477`.
- Modify: `h5/src/views/mine/MineView.vue`
  - Rebuild using the mine region of Figma node `1:2477`, keeping page-level logout.

#### Web tests to modify
- Modify: `web/src/views/auth/LoginView.test.ts`
- Modify: `web/src/views/dashboard/DashboardView.test.ts`
- Modify: `web/src/views/event/EventListView.test.ts`
- Modify: `web/src/views/event/EventDetailView.test.ts`
- Modify: `web/src/views/audit/AuditListView.test.ts`
- Modify: `web/src/views/audit/AuditDetailView.test.ts`
- Modify: `web/src/views/workorder/WorkOrderListView.test.ts`
- Modify: `web/src/views/workorder/WorkOrderDetailView.test.ts`
- Modify: `web/src/layouts/AdminShellLayout.test.ts`
- Modify: `web/src/tests/app-shell.spec.ts`
- Modify: `web/src/tests/core-admin-pages.spec.ts`

#### H5 tests to modify
- Modify: `h5/src/views/login/LoginView.test.ts`
- Modify: `h5/src/views/workbench/WorkbenchView.test.ts`
- Modify: `h5/src/views/workorder/WorkOrderListView.test.ts`
- Modify: `h5/src/views/workorder/WorkOrderDetailView.test.ts`
- Modify: `h5/src/views/verify/VerifyView.test.ts`
- Modify: `h5/src/views/history/HistoryView.test.ts`
- Modify: `h5/src/views/mine/MineView.test.ts`
- Modify: `h5/src/layouts/MobileShellLayout.test.ts`
- Modify: `h5/src/tests/h5-shell.spec.ts`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`

### New files to create only if existing structure cannot hold the Figma shell cleanly
- Create if needed: `web/src/components/figma/*`
  - Only for shell/header/sidebar/detail-section blocks that are used by 2+ first-batch Web pages.
- Create if needed: `h5/src/components/figma/*`
  - Only for mobile shell/header/bottom-action/section blocks that are used by 2+ first-batch H5 pages.

Rule: do not create a new abstraction for a one-off page section.

---

## Chunk 1: Freeze tracking and acceptance artifacts

### Task 1: Tighten the first-batch matrix to the approved routes only

**Files:**
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-matrix.md`
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-deviations.md`

- [ ] **Step 1: Write the failing tracking assertion by inspection**

Confirm the matrix still includes routes outside the approved first batch (for example `/processes`). That is the failure to remove.

- [ ] **Step 2: Edit the matrix to match the approved scope exactly**

Keep only these Web routes:

```md
/login
/dashboard
/events
/events/:id
/audits
/audits/:id
/work-orders
/work-orders/:id
```

Keep only these H5 routes:

```md
/login
/workbench
/work-orders
/work-orders/:orderNo
/verify
/history
/mine
```

- [ ] **Step 3: Add required page states to every row**

Use only the states required by the spec for each route:

```md
default / loading / empty / error / submitting(if applicable) / read-only or no-permission(if applicable)
```

Write the exact state set into each matrix row so engineers can verify route-by-route coverage instead of inferring it later.

- [ ] **Step 4: Add a blank deviation template**

```md
## Allowed deviations
- none yet

## Deferred gaps
- none yet

## Deviation entry template
- Route:
  Figma node or section:
  Missing dependency type: backend / permission / workflow / data
  Current real landed expression:
  Approved as allowed deviation: yes/no
```

- [ ] **Step 5: Review the docs manually**

Expected: no first-batch row references `/processes`, and every frozen route has a state list.

- [ ] **Step 6: Commit**

```bash
git add docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-matrix.md docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-deviations.md
git commit -m "docs: freeze first-batch figma replication scope"
```

---

## Chunk 2: Web shell and login

### Task 2: Rebuild the Web shell with logout and current permission behavior

**Files:**
- Modify: `web/src/layouts/AdminShellLayout.vue`
- Modify: `web/src/router/index.ts`
- Modify: `web/src/layouts/AdminShellLayout.test.ts`
- Modify: `web/src/tests/app-shell.spec.ts`

- [ ] **Step 1: Write the failing shell tests first**

Add tests that require:

```ts
expect(screen.getByRole('navigation', { name: '主导航' })).toBeTruthy()
expect(screen.getByRole('button', { name: '退出登录' })).toBeTruthy()
expect(screen.getByText('管理员')).toBeTruthy()
```

Also add a shell logout test in `web/src/tests/app-shell.spec.ts`, plus a route-guard continuity assertion that still proves:

```ts
expect(router.currentRoute.value.fullPath).toBe('/login?redirect=/dashboard')
```

- [ ] **Step 2: Run the Web shell tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- AdminShellLayout.test.ts app-shell.spec.ts
```
Expected: FAIL because the current shell has no logout button and the structure does not match the new assertions.

- [ ] **Step 3: Implement the minimal shell rebuild**

Keep these behaviors intact:

```ts
const navItems = computed(() => navigationItems.filter((item) => hasMenuPermission(item.permission)))
```

Add:
- account display from session
- logout button wired to the existing logout API/session clearing path
- Figma-aligned shell structure

If the route guard or fallback logic needs a minimal router edit to keep shell/login continuity green, make that edit in `web/src/router/index.ts` without changing any route path.

- [ ] **Step 4: Run the same tests until green**

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add web/src/layouts/AdminShellLayout.vue web/src/router/index.ts web/src/layouts/AdminShellLayout.test.ts web/src/tests/app-shell.spec.ts
git commit -m "feat: rebuild web shell with figma-aligned logout flow"
```

### Task 3: Rebuild the Web login page without breaking redirect semantics

**Files:**
- Modify: `web/src/views/auth/LoginView.vue`
- Modify: `web/src/views/auth/LoginView.test.ts`
- Modify: `web/src/tests/app-shell.spec.ts`

- [ ] **Step 1: Write the failing Web login test**

Require the approved login surface and submit states:

```ts
expect(await screen.findByRole('heading', { name: '欢迎登录' })).toBeTruthy()
expect(screen.getByLabelText('账号')).toBeTruthy()
expect(screen.getByLabelText('密码')).toBeTruthy()
expect(screen.getByRole('button', { name: '登录' })).toBeTruthy()
```

Add a submitting-state assertion:

```ts
expect(screen.getByRole('button', { name: '登录中...' })).toBeTruthy()
```

- [ ] **Step 2: Run the focused login tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- src/views/auth/LoginView.test.ts src/tests/app-shell.spec.ts
```
Expected: FAIL if the current DOM or submit-state handling does not satisfy the new assertions.

- [ ] **Step 3: Implement the minimal login changes**

Keep this redirect contract unchanged:

```ts
const redirect = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'))
await router.replace(redirect.value)
```

- [ ] **Step 4: Run the same tests until green**

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add web/src/views/auth/LoginView.vue web/src/views/auth/LoginView.test.ts web/src/tests/app-shell.spec.ts
git commit -m "feat: rebuild web login to figma baseline"
```

---

## Chunk 3: Web dashboard and event center

### Task 4: Rebuild the Web dashboard

**Files:**
- Modify: `web/src/views/dashboard/DashboardView.vue`
- Modify: `web/src/views/dashboard/DashboardView.test.ts`
- Modify: `web/src/tests/core-admin-pages.spec.ts`

- [ ] **Step 1: Tighten the failing dashboard tests**

Require the route to expose the approved sections:

```ts
expect(screen.getByText('首页驾驶舱')).toBeTruthy()
expect(screen.getByText('今日闭环率')).toBeTruthy()
expect(screen.getByText('角色待办')).toBeTruthy()
expect(screen.getByText('快捷入口')).toBeTruthy()
expect(screen.getByText('告警与处置提醒')).toBeTruthy()
expect(screen.getByText('地图态势概览')).toBeTruthy()
```

- [ ] **Step 2: Run the focused and integration tests**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- src/views/dashboard/DashboardView.test.ts src/tests/core-admin-pages.spec.ts
```
Expected: FAIL if the dashboard structure differs from the required Figma sections.

- [ ] **Step 3: Implement the minimal dashboard rebuild**

Keep the existing data semantics, but reorganize the DOM/CSS to the approved Figma grouping.
Also cover the spec-required `loading`, `empty`, `error`, and `read-only/no-permission` states in the focused test file and integration assertions.

- [ ] **Step 4: Re-run the dashboard tests until green**

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add web/src/views/dashboard/DashboardView.vue web/src/views/dashboard/DashboardView.test.ts web/src/tests/core-admin-pages.spec.ts
git commit -m "feat: rebuild web dashboard to figma baseline"
```

### Task 5: Rebuild Web events list and detail

**Files:**
- Modify: `web/src/views/event/EventListView.vue`
- Modify: `web/src/views/event/EventListView.test.ts`
- Modify: `web/src/views/event/EventDetailView.vue`
- Modify: `web/src/views/event/EventDetailView.test.ts`
- Modify: `web/src/tests/core-admin-pages.spec.ts`

- [ ] **Step 1: Tighten the failing event list/detail tests**

Require list sections:

```ts
expect(screen.getByText('事件台账')).toBeTruthy()
expect(screen.getByLabelText('来源类型')).toBeTruthy()
expect(screen.getByLabelText('事件类型')).toBeTruthy()
expect(screen.getByLabelText('当前状态')).toBeTruthy()
```

Require detail sections:

```ts
expect(screen.getByText('事件摘要')).toBeTruthy()
expect(screen.getByText('取证附件')).toBeTruthy()
expect(screen.getByText('流转概览')).toBeTruthy()
```

- [ ] **Step 2: Run the event tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- src/views/event/EventListView.test.ts src/views/event/EventDetailView.test.ts src/tests/core-admin-pages.spec.ts
```
Expected: FAIL if the pages still reflect template-oriented rather than route-specific Figma structure.

- [ ] **Step 3: Implement the minimal event list rebuild**

Keep existing filtering and route links working.
Also cover the spec-required list states: `loading`, `empty`, `error`, and `read-only/no-permission`.

- [ ] **Step 4: Re-run only event list tests**

Expected: PASS for list assertions.

- [ ] **Step 5: Implement the minimal event detail rebuild**

Keep same-component route updates and not-found handling.
Also cover the spec-required detail states: `loading`, `empty`, `error`, and `read-only/no-permission`.

- [ ] **Step 6: Re-run all event tests**

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add web/src/views/event/EventListView.vue web/src/views/event/EventListView.test.ts web/src/views/event/EventDetailView.vue web/src/views/event/EventDetailView.test.ts web/src/tests/core-admin-pages.spec.ts
git commit -m "feat: rebuild web event pages to figma baseline"
```

---

## Chunk 4: Web audit center and work orders

### Task 6: Rebuild Web audits list and detail

**Files:**
- Modify: `web/src/views/audit/AuditListView.vue`
- Modify: `web/src/views/audit/AuditListView.test.ts`
- Modify: `web/src/views/audit/AuditDetailView.vue`
- Modify: `web/src/views/audit/AuditDetailView.test.ts`
- Modify: `web/src/tests/core-admin-pages.spec.ts`

- [ ] **Step 1: Write or tighten the failing audit tests**

Require list sections:

```ts
expect(screen.getByText('审核中心 / 事件列表')).toBeTruthy()
expect(screen.getByText('待审核任务')).toBeTruthy()
expect(screen.getAllByText('选流程送审').length).toBeGreaterThan(0)
```

Require detail sections:

```ts
expect(screen.getByText('证据')).toBeTruthy()
expect(screen.getByText('流程送审')).toBeTruthy()
expect(screen.getByText('节点进度')).toBeTruthy()
expect(screen.getByText('审核操作')).toBeTruthy()
```

- [ ] **Step 2: Run the audit tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- src/views/audit/AuditListView.test.ts src/views/audit/AuditDetailView.test.ts src/tests/core-admin-pages.spec.ts
```
Expected: FAIL if the pages still rely on generic template sections not aligned to the approved audit structure.

- [ ] **Step 3: Implement the minimal audit list rebuild**

Keep status filtering and route access intact.
Also cover the spec-required list states: `loading`, `empty`, `error`, and `read-only/no-permission`.

- [ ] **Step 4: Re-run the audit list tests**

Expected: PASS

- [ ] **Step 5: Implement the minimal audit detail rebuild**

Keep template reselection behavior, permission-gated actions, node progress, same-component route updates, and not-found handling.
Also cover the spec-required detail states: `loading`, `empty`, `error`, `submitting`, and `read-only/no-permission`.

- [ ] **Step 6: Re-run all audit tests**

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add web/src/views/audit/AuditListView.vue web/src/views/audit/AuditListView.test.ts web/src/views/audit/AuditDetailView.vue web/src/views/audit/AuditDetailView.test.ts web/src/tests/core-admin-pages.spec.ts
git commit -m "feat: rebuild web audit pages to figma baseline"
```

### Task 7: Rebuild Web work-order list and detail

**Files:**
- Modify: `web/src/views/workorder/WorkOrderListView.vue`
- Modify: `web/src/views/workorder/WorkOrderListView.test.ts`
- Modify: `web/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `web/src/views/workorder/WorkOrderDetailView.test.ts`
- Modify: `web/src/tests/core-admin-pages.spec.ts`

- [ ] **Step 1: Write or tighten the failing work-order tests**

Require list sections:

```ts
expect(screen.getByText('工单中心 / 列表页')).toBeTruthy()
expect(screen.getByText('工单队列总览')).toBeTruthy()
expect(screen.getByLabelText('工单状态')).toBeTruthy()
expect(screen.getByLabelText('处理人')).toBeTruthy()
```

Require detail sections:

```ts
expect(screen.getByText('流程节点')).toBeTruthy()
expect(screen.getByText('关闭确认')).toBeTruthy()
```

- [ ] **Step 2: Run the work-order tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- src/views/workorder/WorkOrderListView.test.ts src/views/workorder/WorkOrderDetailView.test.ts src/tests/core-admin-pages.spec.ts
```
Expected: FAIL if the structure does not match the required Figma sections or action-state layout.

- [ ] **Step 3: Implement the minimal list rebuild**

Keep filters, route links, and current list semantics intact.
Also cover the spec-required list states: `loading`, `empty`, `error`, and `read-only/no-permission`.

- [ ] **Step 4: Re-run list tests**

Expected: PASS

- [ ] **Step 5: Implement the minimal detail rebuild**

Keep action gating, route updates, and not-found handling intact.
Also cover the spec-required detail states: `loading`, `empty`, `error`, `submitting`, and `read-only/no-permission`.

- [ ] **Step 6: Re-run all work-order tests**

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add web/src/views/workorder/WorkOrderListView.vue web/src/views/workorder/WorkOrderListView.test.ts web/src/views/workorder/WorkOrderDetailView.vue web/src/views/workorder/WorkOrderDetailView.test.ts web/src/tests/core-admin-pages.spec.ts
git commit -m "feat: rebuild web work-order pages to figma baseline"
```

---

## Chunk 5: H5 shell and login

### Task 8: Rebuild the H5 shell with current tabbar and logout semantics

**Files:**
- Modify: `h5/src/layouts/MobileShellLayout.vue`
- Modify: `h5/src/router/index.ts`
- Modify: `h5/src/layouts/MobileShellLayout.test.ts`
- Modify: `h5/src/tests/h5-shell.spec.ts`

- [ ] **Step 1: Write the failing H5 shell tests**

Require:

```ts
expect(screen.getByRole('navigation', { name: 'H5主导航' })).toBeTruthy()
expect(screen.getByRole('button', { name: '退出' })).toBeTruthy()
expect(screen.getByText('东莞常平现场处置')).toBeTruthy()
```

- [ ] **Step 2: Run the H5 shell tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- src/layouts/MobileShellLayout.test.ts src/tests/h5-shell.spec.ts
```
Expected: FAIL if the shell does not satisfy the stricter Figma structure assertions.

- [ ] **Step 3: Implement the minimal shell rebuild**

Keep:

```ts
const navItems = computed(() => h5NavigationItems.filter((item) => hasMenuPermission(item.permission)))
```

and the existing shell logout transport behavior.

Also keep explicit guard continuity green in `h5/src/router/index.ts` for:
- unauthenticated redirect to `/login?redirect=...`
- login fallback to the first accessible route
- unauthorized route fallback to the first accessible route

- [ ] **Step 4: Re-run the shell tests until green**

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add h5/src/layouts/MobileShellLayout.vue h5/src/router/index.ts h5/src/layouts/MobileShellLayout.test.ts h5/src/tests/h5-shell.spec.ts
git commit -m "feat: rebuild h5 shell to figma baseline"
```

### Task 9: Rebuild the H5 login page without breaking redirect fallback

**Files:**
- Modify: `h5/src/views/login/LoginView.vue`
- Modify: `h5/src/views/login/LoginView.test.ts`
- Modify: `h5/src/tests/h5-shell.spec.ts`

- [ ] **Step 1: Write the failing H5 login test**

Require:

```ts
expect(await screen.findByRole('heading', { name: '现场处置登录' })).toBeTruthy()
expect(screen.getByLabelText('账号')).toBeTruthy()
expect(screen.getByLabelText('密码')).toBeTruthy()
expect(screen.getByRole('button', { name: '登录' })).toBeTruthy()
```

Add a submitting-state assertion for `登录中...`.

- [ ] **Step 2: Run the H5 login tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- src/views/login/LoginView.test.ts src/tests/h5-shell.spec.ts
```
Expected: FAIL if the current page does not match the approved structure or states.

- [ ] **Step 3: Implement the minimal login rebuild**

Keep this fallback logic unchanged:

```ts
const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : fallbackRedirect.value
await router.replace(redirect || '/login')
```

- [ ] **Step 4: Re-run the same tests until green**

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add h5/src/views/login/LoginView.vue h5/src/views/login/LoginView.test.ts h5/src/tests/h5-shell.spec.ts
git commit -m "feat: rebuild h5 login to figma baseline"
```

---

## Chunk 6: H5 workbench and work orders

### Task 10: Rebuild the H5 workbench

**Files:**
- Modify: `h5/src/views/workbench/WorkbenchView.vue`
- Modify: `h5/src/views/workbench/WorkbenchView.test.ts`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`

- [ ] **Step 1: Tighten the failing workbench tests**

Require:

```ts
expect(screen.getByText('工作台概览')).toBeTruthy()
expect(screen.getByText('快捷入口')).toBeTruthy()
expect(screen.getByText('最新待处理工单')).toBeTruthy()
```

- [ ] **Step 2: Run workbench tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- src/views/workbench/WorkbenchView.test.ts src/tests/h5-closed-loop-pages.spec.ts
```
Expected: FAIL if the page does not satisfy the stricter approved structure.

- [ ] **Step 3: Implement the minimal workbench rebuild**

Keep shortcut permission filtering and current work-order links intact.
Also cover the spec-required `loading`, `empty`, `error`, and `read-only/no-permission` states.

- [ ] **Step 4: Re-run workbench tests until green**

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add h5/src/views/workbench/WorkbenchView.vue h5/src/views/workbench/WorkbenchView.test.ts h5/src/tests/h5-closed-loop-pages.spec.ts
git commit -m "feat: rebuild h5 workbench to figma baseline"
```

### Task 11: Rebuild the H5 work-order list and detail

**Files:**
- Modify: `h5/src/views/workorder/WorkOrderListView.vue`
- Modify: `h5/src/views/workorder/WorkOrderListView.test.ts`
- Modify: `h5/src/views/workorder/WorkOrderDetailView.vue`
- Modify: `h5/src/views/workorder/WorkOrderDetailView.test.ts`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`

- [ ] **Step 1: Tighten the failing H5 work-order tests**

Require list sections:

```ts
expect(screen.getByText('工单分布')).toBeTruthy()
expect(screen.getByRole('heading', { name: '工单列表' })).toBeTruthy()
expect(screen.getByText('待处理总量')).toBeTruthy()
```

Require detail sections:

```ts
expect(screen.getByText('处置进度概览')).toBeTruthy()
expect(screen.getByText('联系信息')).toBeTruthy()
```

- [ ] **Step 2: Run the H5 work-order tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- src/views/workorder/WorkOrderListView.test.ts src/views/workorder/WorkOrderDetailView.test.ts src/tests/h5-closed-loop-pages.spec.ts
```
Expected: FAIL if the pages do not satisfy the approved card layout or action-state structure.

- [ ] **Step 3: Implement the minimal list rebuild**

Keep grouping semantics and detail links intact.
Also cover the spec-required list states: `loading`, `empty`, `error`, and `read-only/no-permission`.

- [ ] **Step 4: Re-run list tests**

Expected: PASS

- [ ] **Step 5: Implement the minimal detail rebuild**

Keep route param refresh, stale-response protection, action visibility, and loading/error/not-found handling intact.
Also cover the spec-required detail states: `loading`, `empty`, `error`, `submitting`, and `read-only/no-permission`.

- [ ] **Step 6: Re-run all H5 work-order tests**

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add h5/src/views/workorder/WorkOrderListView.vue h5/src/views/workorder/WorkOrderListView.test.ts h5/src/views/workorder/WorkOrderDetailView.vue h5/src/views/workorder/WorkOrderDetailView.test.ts h5/src/tests/h5-closed-loop-pages.spec.ts
git commit -m "feat: rebuild h5 work-order pages to figma baseline"
```

---

## Chunk 7: H5 verify, history, and mine

### Task 12: Rebuild H5 verify page with all required states

**Files:**
- Modify: `h5/src/views/verify/VerifyView.vue`
- Modify: `h5/src/views/verify/VerifyView.test.ts`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`

- [ ] **Step 1: Tighten the failing verify tests**

Require:

```ts
expect(await screen.findByRole('heading', { name: '闭环核查' })).toBeTruthy()
expect(screen.getByText('当前待核查')).toBeTruthy()
expect(screen.getByText('提交状态')).toBeTruthy()
expect(screen.getByRole('button', { name: '提交核查结果' })).toBeTruthy()
```

Keep assertions for loading, empty, error, submitting, and read-only states.

- [ ] **Step 2: Run verify tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- src/views/verify/VerifyView.test.ts src/tests/h5-closed-loop-pages.spec.ts
```
Expected: FAIL if the current page structure or states do not satisfy the stricter Figma assertions.

- [ ] **Step 3: Implement the minimal verify rebuild**

Keep permission gating and upload-summary semantics intact.
Also add explicit closed-loop assertions that submit and return actions land back on shell-reachable H5 pages without breaking the frozen route flow.

- [ ] **Step 4: Re-run verify tests until green**

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add h5/src/views/verify/VerifyView.vue h5/src/views/verify/VerifyView.test.ts h5/src/tests/h5-closed-loop-pages.spec.ts
git commit -m "feat: rebuild h5 verify page to figma baseline"
```

### Task 13: Rebuild H5 history and mine pages

**Files:**
- Modify: `h5/src/views/history/HistoryView.vue`
- Modify: `h5/src/views/history/HistoryView.test.ts`
- Modify: `h5/src/views/mine/MineView.vue`
- Modify: `h5/src/views/mine/MineView.test.ts`
- Modify: `h5/src/tests/h5-closed-loop-pages.spec.ts`

- [ ] **Step 1: Write or tighten the failing history/mine tests**

Require history sections:

```ts
expect(screen.getByRole('heading', { name: '历史记录' })).toBeTruthy()
expect(screen.getByText('累计办结')).toBeTruthy()
expect(screen.getByText('上传凭证')).toBeTruthy()
```

Require mine sections:

```ts
expect(screen.getByRole('heading', { name: '我的' })).toBeTruthy()
expect(screen.getByText('个人中心')).toBeTruthy()
expect(screen.getByRole('button', { name: '退出登录' })).toBeTruthy()
```

- [ ] **Step 2: Run the history/mine tests to verify failure**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- src/views/history/HistoryView.test.ts src/views/mine/MineView.test.ts src/tests/h5-closed-loop-pages.spec.ts
```
Expected: FAIL if the pages still reflect a looser template mapping.

- [ ] **Step 3: Implement the minimal history rebuild**

Keep current record grouping semantics intact.
Also add focused and integration assertions for the spec-required history states: `loading`, `empty`, `error`, and `read-only/no-permission`.
Also add a tabbar reachability assertion and a return-flow assertion so `/history` stays inside the current H5 shell semantics after navigation.

- [ ] **Step 4: Re-run history tests**

Expected: PASS

- [ ] **Step 5: Implement the minimal mine rebuild**

Keep account/session display and page-level logout intact.
Also cover the spec-required states for mine: `loading`, `empty` when applicable, `error`, and `read-only/no-permission`.

- [ ] **Step 6: Re-run all history/mine tests**

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add h5/src/views/history/HistoryView.vue h5/src/views/history/HistoryView.test.ts h5/src/views/mine/MineView.vue h5/src/views/mine/MineView.test.ts h5/src/tests/h5-closed-loop-pages.spec.ts
git commit -m "feat: rebuild h5 history and mine pages to figma baseline"
```

---

## Chunk 8: Final matrix reconciliation and verification

### Task 14: Reconcile tracking docs and run end-to-end verification for the frozen routes

**Files:**
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-matrix.md`
- Modify: `docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-deviations.md`
- Modify if needed: any failing test files from earlier chunks

- [ ] **Step 1: Update the matrix route-by-route**

For each frozen route, set implementation status only after the route, required states, tests, and visual acceptance package are actually green.

- [ ] **Step 2: Record any approved gaps exactly once**

Example format:

```md
- Route: /verify
  Figma node or section: 1:2425
  Missing dependency type: data
  Gap: explicit upload widget skin differs because current uploader is placeholder-backed
  Current real landed expression: keep the real placeholder-backed upload summary section and show non-fake evidence counts
  Approved as allowed deviation: yes
  Status: approved deviation pending real uploader integration
```

- [ ] **Step 3: Write the per-route visual acceptance packages**

Create one acceptance note per frozen route under `docs/superpowers/tracking/visual-acceptance/`.
Each file must contain:

```md
# <route> visual acceptance
- Figma node or variant:
- Default-state checklist:
- Required-state checklist:
- Deviations:
- 1:1 completion verdict:
```

Do not mark a route complete in the matrix until its visual acceptance note exists.

- [ ] **Step 4: Run Web verification suite**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/web && npm test -- src/tests/app-shell.spec.ts src/tests/core-admin-pages.spec.ts src/views/auth/LoginView.test.ts src/views/dashboard/DashboardView.test.ts src/views/event/EventListView.test.ts src/views/event/EventDetailView.test.ts src/views/audit/AuditListView.test.ts src/views/audit/AuditDetailView.test.ts src/views/workorder/WorkOrderListView.test.ts src/views/workorder/WorkOrderDetailView.test.ts src/layouts/AdminShellLayout.test.ts
```
Expected: PASS

- [ ] **Step 5: Run H5 verification suite**

Run:
```bash
cd /Users/tangxinglin/code/javacode/dgcp-oa/h5 && npm test -- src/tests/h5-shell.spec.ts src/tests/h5-closed-loop-pages.spec.ts src/views/login/LoginView.test.ts src/views/workbench/WorkbenchView.test.ts src/views/workorder/WorkOrderListView.test.ts src/views/workorder/WorkOrderDetailView.test.ts src/views/verify/VerifyView.test.ts src/views/history/HistoryView.test.ts src/views/mine/MineView.test.ts src/layouts/MobileShellLayout.test.ts
```
Expected: PASS

- [ ] **Step 6: Run spec-to-plan sanity review**

Verify manually that:
- no first-batch route is missing
- `/processes` is not marked first-batch complete
- Web/H5 logout behavior is covered
- loading/empty/error/submitting/read-only states are present in the matrix

- [ ] **Step 7: Commit**

```bash
git add docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-matrix.md docs/superpowers/tracking/2026-03-15-dual-end-figma-replication-deviations.md docs/superpowers/tracking/visual-acceptance web/src h5/src
git commit -m "test: verify first-batch figma replication coverage"
```
