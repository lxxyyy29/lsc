# Changping Smart City UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Dongguan Changping smart city UI set based on the updated Numbers requirements document while preserving the approved deep-blue government-tech visual style, then deliver a fresh final Figma file.

**Architecture:** The implementation keeps a single static design source in `changping-smart-city-ui.html`, but reorganizes its information architecture around the new business chain: UAV onboarding → patrol planning → media upload → AI identification → human review → work order transfer → closed-loop handling → authorization management. The existing visual language stays intact, while page content, page lineup, and section composition are rewritten to match the new requirements extracted from the Numbers document.

**Tech Stack:** Static HTML, CSS, local Python HTTP server, Figma MCP `generate_figma_design`, macOS `open`, Python standard library for Numbers archive inspection.

---

## File Structure

- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`
  - The single source of truth for all Web/App/big-screen mockups.
  - Rework page sequence, labels, data blocks, and layouts to reflect the updated requirements.
- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/docs/superpowers/plans/2026-03-13-changping-smart-city-ui-redesign.md`
  - This implementation plan.
- Reference only: `/Users/tangxinglin/code/javacode/dgcp-oa/东莞常平城管项目功能描述(1).numbers`
  - Updated requirements source.
- Reference only: `/Users/tangxinglin/code/javacode/dgcp-oa/preview-web.jpg`
  - Quick visual confirmation that the Numbers document bundle opened correctly.

## Style Guardrails

The redesign must preserve these approved style elements from the existing HTML:
- Deep navy background with cyan/blue highlights and soft glow accents
- Rounded panel system (`18px`–`28px`) with thin blue borders
- Fixed-width vertical Web page presentation for easy review in Figma
- Mobile pages shown inside device shells, not as flat cards
- Two Web page families only:
  - display pages: dashboard / detail / big screen
  - business pages: sidebar + topbar + query panel + table/config panel
- Darker list-page title bar and darker table header treatment only on list/config pages

Any redesign that removes these visual anchors is out of scope.

## Baseline Page Disposition

The current HTML already contains these baseline pages. During implementation, handle them explicitly:
- Keep and rewrite: `01 指挥大屏`, `02 Web 登录页`, `03 Web 首页驾驶舱`, `04 Web AI 事件审核列表`, `05 Web 事件详情 / 证据回放`, `06 Web 工单处置中心`, `07 Web 工单详情 / 流转记录`
- Repurpose into new capability pages: `08 Web 无人机设备管理` → `08 Web 无人机接入档案`, `09 Web 巡检任务计划` → `09 Web 飞行 / 巡检任务计划`
- Retire and replace with new pages: `10 Web 数据报表中心`, `11 Web 地图标注与权限管理`, `12 Web 支撑状态页`
- Rebuild mobile lineup from the current two grouped mobile sections into six explicit pages: `14–19`

## Chunk 1: Requirements Extraction and Page Mapping

### Task 1: Extract updated feature points from the Numbers document

**Files:**
- Reference: `/Users/tangxinglin/code/javacode/dgcp-oa/东莞常平城管项目功能描述(1).numbers`
- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`

- [ ] **Step 1: Inspect the Numbers archive structure**

Run:
```bash
python3 - <<'PY'
import zipfile
path='/Users/tangxinglin/code/javacode/dgcp-oa/东莞常平城管项目功能描述(1).numbers'
with zipfile.ZipFile(path) as z:
    for name in z.namelist():
        print(name)
PY
```
Expected: A list including `Index/Document.iwa`, `Index/Tables/DataList-*.iwa`, and preview assets.

- [ ] **Step 2: Extract readable strings from the internal iwa payloads**

Run:
```bash
python3 - <<'PY'
import zipfile, re
path='/Users/tangxinglin/code/javacode/dgcp-oa/东莞常平城管项目功能描述(1).numbers'
with zipfile.ZipFile(path) as z:
    for name in z.namelist():
        if name.endswith('.iwa'):
            data = z.read(name)
            texts = re.findall(rb'[\x20-\x7e\xe4-\xef][\x20-\x7e\x80-\xff]{3,}', data)
            out=[]
            for t in texts:
                s=t.decode('utf-8','ignore')
                if any('\u4e00' <= ch <= '\u9fff' for ch in s) or any(ch.isalpha() for ch in s):
                    out.append(s)
            if out:
                print(f'## {name}')
                for s in out[:60]:
                    print(s)
PY
```
Expected: Visible feature strings such as `系统登录模块`, `权限验证`, `RBAC/动态菜单`, `无人机接入`, `飞行/巡检任务`, `媒体上传`, `审核中心`, `转工单`.

- [ ] **Step 3: Create a canonical requirement inventory before editing HTML**

Write a concrete requirement inventory into the top of the plan file itself under a temporary heading `## Working Requirement Inventory` with four required sections:
- mandatory modules from the Numbers document
- mandatory UI terms/labels to preserve
- feature-to-page mapping
- page type for each page (`display`, `list/config`, `detail`, `mobile`)

Expected: A stable, reviewable artifact the implementer can follow while rewriting HTML.

- [ ] **Step 4: Verify the updated map differs meaningfully from the current HTML lineup**

Check the current HTML page headings and compare them against the new requirement map.
Expected: A clear list of pages to keep, pages to repurpose, pages to remove, and pages to add.

- [ ] **Step 5: Define a fallback if Numbers string extraction is incomplete**

If the `.iwa` string extraction is fragmented or missing key fields, use both of these fallbacks:
- read the bundle preview image (`preview-web.jpg`) for visible structure clues
- preserve all clearly extracted modules and avoid inventing unsupported new subsystems

If critical modules, page labels, or workflow transitions are still ambiguous after both fallbacks, stop and surface the ambiguity to the user before implementation rather than guessing.
Expected: The redesign stays grounded in recoverable source material instead of guesses.

## Chunk 2: HTML Redesign

### Task 2: Rewrite the high-level page lineup and page titles

**Files:**
- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`

- [ ] **Step 1: Read the existing page sections and heading order**

Use Read on the section block containing the numbered pages.
Expected: Existing pages 01–14 visible in order.

- [ ] **Step 2: Replace the page lineup with the new architecture**

Update the page sequence to this target lineup:
1. 综合监管总览大屏
2. Web 登录页
3. Web 首页驾驶舱
4. Web 审核中心列表
5. Web 审核详情 / 证据 / 派单
6. Web 工单中心
7. Web 工单详情 / 流转记录
8. Web 无人机接入档案
9. Web 飞行 / 巡检任务计划
10. Web AI 模型 / 阈值配置
11. Web 媒体上传与识别结果管理
12. Web 授权管理 / 动态菜单
13. Web 系统配置 / 接口预留
14. App 登录
15. App 巡查工作台
16. App 任务列表
17. App 任务详情 / 现场核查
18. App 图片 / 视频上传
19. App 我的 / 历史记录

Expected: The HTML headings reflect the new information architecture end-to-end.

- [ ] **Step 3: Add page-level acceptance criteria before content rewriting**

For each target page, define the required content blocks:
- 综合监管总览大屏: region heat, model hit ranking, device online, work-order closure, alert ticker
- Web 首页驾驶舱: KPI summary, shortcuts, latest alerts, operational heat map
- 审核中心列表: query area, table, status tags, actions (`查看 / 派单 / 忽略`)
- 审核详情: evidence gallery, confidence/threshold info, review actions, transfer-to-work-order action
- 工单中心: query area, table, owner/area/status/timeout columns
- 工单详情: flow nodes, on-site feedback, close-out actions
- 无人机接入档案: SN, firmware version, area, status, third-party API placeholder
- 飞行 / 巡检任务计划: task name, takeoff time, assignee, route/area, bound device, execution status
- AI 模型 / 阈值配置: model list, version, threshold, labels, enable state
- 媒体上传与识别结果管理: upload records, file type, preview, linked recognition result
- 授权管理 / 动态菜单: roles, menu permissions, operation permissions, data scope
- 系统配置 / 接口预留: API placeholder rows, connection state, parameter descriptions
- App 登录: account entry, password/verification entry, platform description
- App 巡查工作台: pending tasks, urgent alerts, quick actions, current assignment card
- App 任务列表: state tabs, task cards, time/distance/priority information
- App 任务详情 / 现场核查: evidence preview, location info, handling notes, submit action
- App 图片 / 视频上传: upload target, file type, preview area, save/submit actions
- App 我的 / 历史记录: profile summary, historical tasks/uploads, personal settings entry

Expected: No page is implemented from title alone.

- [ ] **Step 4: Keep the overall visual wrapper unchanged unless necessary**

Do not redesign the whole theme system. Preserve the approved deep-blue palette, hero framing, fixed-width vertical Web arrangement, and mobile shell presentation.
Expected: The redesign feels like the same product family, not a different brand.

### Task 3: Rebuild the Web business pages around the updated business chain

**Files:**
- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`

- [ ] **Step 1: Rewrite the dashboard copy and KPI cards**

Replace old summary content with metrics aligned to the new chain:
- 接入设备
- 待审核识别结果
- 待转工单
- 执行中巡检任务
- 模型在线状态
- 接口预留状态

Expected: The dashboard speaks to updated business operations instead of the old generic layout.

- [ ] **Step 2: Keep list/config pages in the approved query + table layout**

Apply the existing list-page pattern to:
- 审核中心列表
- 工单中心
- 无人机接入档案
- 飞行 / 巡检任务计划
- AI 模型 / 阈值配置
- 媒体上传与识别结果管理
- 授权管理 / 动态菜单
- 系统配置 / 接口预留

Expected: Each of these pages has:
- left navigation
- darkened title bar
- query conditions panel
- table/configuration panel
- darker table headers
- page-specific actions rather than generic placeholders

- [ ] **Step 3: Keep detail pages as richer panel compositions**

Use non-list layouts for:
- 审核详情 / 证据 / 派单
- 工单详情 / 流转记录

Expected: These pages show evidence, flow nodes, actions, and metadata in richer card layouts, not flat tables.

- [ ] **Step 4: Implement the new capability pages with domain-specific blocks**

Create visual sections for:
- AI 模型 / 阈值配置
- 媒体上传与识别结果管理
- 授权管理 / 动态菜单
- 系统配置 / 接口预留

For each page, replace generic placeholders with fields or table columns that come from the requirement inventory.
Expected: The pages visually communicate real product capability instead of placeholder generic admin content.

- [ ] **Step 5: Validate total HTML length and section spacing after adding pages**

Because the redesign expands the page count, verify that the single HTML file still presents cleanly as a vertical stack.
Check specifically:
- no section overlap
- no accidentally collapsed mobile block
- no query/table panel clipping
- enough margin between long Web pages

Expected: The new 19-page lineup remains capturable in one long document.

### Task 4: Rebuild the App pages to match the updated requirement set

**Files:**
- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`

- [ ] **Step 1: Keep the existing mobile shell structure**

Do not change the device framing pattern unless required.
Expected: Mobile pages still present as polished handset mockups.

- [ ] **Step 2: Rewrite mobile page purposes**

Update the app lineup to cover:
- 登录
- 巡查工作台
- 任务列表
- 任务详情 / 现场核查
- 图片 / 视频上传
- 我的 / 历史记录

Expected: The mobile flow mirrors the updated business chain and supports evidence capture.

- [ ] **Step 3: Ensure labels and action verbs match the new document language**

Prefer business text such as:
- 上传图片/视频
- 现场核查
- 转工单
- 动态权限
- 巡检任务
Expected: Mobile copy aligns with the Web/system vocabulary.

## Chunk 3: Verification and Figma Delivery

### Task 5: Verify the rewritten HTML before capture

**Files:**
- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`

- [ ] **Step 1: Read back the rewritten section range**

Use Read on the updated page sections.
Expected: All new page headings and structures appear in the correct order.

- [ ] **Step 2: Run a focused review of the modified HTML**

Dispatch a code-review agent to review only `changping-smart-city-ui.html` and check for:
- accidental style scope leaks
- malformed HTML blocks
- inconsistent page numbering
- non-list pages accidentally converted to list pages

Expected: No concrete blocking issues.

- [ ] **Step 3: Manually confirm the intended page-type split**

Checklist:
- Dashboard remains display-oriented
- List/config pages use the query + table structure
- Detail pages remain card/detail oriented
- App pages remain mobile-only
Expected: All four checks pass.

### Task 6: Generate the new Figma deliverable

**Files:**
- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`

- [ ] **Step 1: Confirm the local static server is running**

Run:
```bash
lsof -nP -iTCP:8000 -sTCP:LISTEN
```
Expected: A Python HTTP server is listening on port 8000.

- [ ] **Step 2: Start the local static server if nothing is listening**

Run only if Step 1 returns no listener:
```bash
python3 -m http.server 8000 --directory "/Users/tangxinglin/code/javacode/dgcp-oa"
```
Expected: The local HTML becomes available at `http://localhost:8000/changping-smart-city-ui.html`.

- [ ] **Step 3: Request a fresh Figma capture ID for a new file**

Use `generate_figma_design(outputMode="newFile", planKey="team::1609675192183439675", fileName="东莞常平智慧城管低空巡检新版UI方案")`.
Expected: A new single-use capture ID is returned together with capture instructions. Extract the capture endpoint from that response in the form `https://mcp.figma.com/mcp/capture/<CAPTURE_ID>/submit`, then URL-encode it for the hash URL.

- [ ] **Step 4: Open the HTML page with the capture hash URL**

Run:
```bash
open "http://localhost:8000/changping-smart-city-ui.html#figmacapture=<CAPTURE_ID>&figmaendpoint=https%3A%2F%2Fmcp.figma.com%2Fmcp%2Fcapture%2F<CAPTURE_ID>%2Fsubmit&figmadelay=1000"
```
Expected: The local page opens with the Figma capture script engaged.

- [ ] **Step 5: Poll until the capture completes**

Use `generate_figma_design(captureId="<CAPTURE_ID>")` every 5 seconds.
Expected sequence:
- early polls may return `pending` or `processing`
- continue up to 10 polls before troubleshooting
- final status returns a new Figma file URL

- [ ] **Step 6: Troubleshoot only if polling stalls**

If still pending after 10 polls, verify:
- `capture.js` is still present in the HTML head
- the local server is serving the latest file
- the hash URL contains the correct capture ID and encoded endpoint
- the page still opens successfully with `open`

Expected: Either the original capture completes, or the root cause is fixed before requesting a replacement file.

- [ ] **Step 7: Return the final Figma link to the user**

Provide only the final deliverable URL and a concise note that it is based on the updated Numbers document.
Expected: User receives the fresh final Figma link.

### Task 7: Optional refinement pass if the first redesigned capture reveals layout issues

**Files:**
- Modify: `/Users/tangxinglin/code/javacode/dgcp-oa/changping-smart-city-ui.html`

- [ ] **Step 1: Compare captured result against intended vertical Web browsing flow**

Check whether Web pages remain easy to browse vertically and whether the list-page/table hierarchy remains legible.
Expected: No compression or grouping issues.

- [ ] **Step 2: If problems exist, fix layout in HTML with minimal targeted changes**

Allowed adjustments:
- spacing
- section heights
- table header contrast
- menu spacing
- panel padding
Expected: Issues corrected without changing the approved overall style.

- [ ] **Step 3: Generate one fresh replacement Figma file only if necessary**

Do not create multiple noisy final candidates.
Expected: Either keep the first good redesigned Figma file or replace it with one cleaner final version.
