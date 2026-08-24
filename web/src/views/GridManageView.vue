<template>
  <div>
    <!-- 地图中心点配置条（独立于 grid 布局之外，避免挤乱三列结构） -->
    <div style="display:flex;align-items:center;gap:8px;padding:10px 14px;background:#f0f7ff;border:1px solid #bfdbfe;border-radius:8px;margin-bottom:12px;font-size:13px;flex-wrap:wrap;">
      <span style="font-weight:600;color:#075985;"><i class="fas fa-map-pin"></i> 地图中心点</span>
      <span style="color:#6b7280;">地图类页面（看板/GIS/大屏）默认以此坐标为中心</span>
      <input v-model="centerLng" type="number" step="0.000001" placeholder="经度" style="width:120px;padding:5px 10px;border:1px solid #bfdbfe;border-radius:6px;font-size:13px;box-sizing:border-box;" />
      <input v-model="centerLat" type="number" step="0.000001" placeholder="纬度" style="width:120px;padding:5px 10px;border:1px solid #bfdbfe;border-radius:6px;font-size:13px;box-sizing:border-box;" />
      <button @click="saveMapCenter" class="btn-primary" style="padding:5px 16px;font-size:12px;">保存中心点</button>
    </div>
    <div class="grid-manage-page" :class="{ 'no-form': !formVisible }">
    <!-- 左：网格树 -->
    <aside class="tree-panel">
      <div class="panel-header">
        <h2>网格管理</h2>
        <button class="btn-primary" @click="startCreate">＋ 新增网格</button>
      </div>
      <div class="tree-scroll">
        <div v-if="!flatTree.length" class="empty-tip">暂无网格，点击「新增网格」创建</div>
        <div
          v-for="g in flatTree"
          :key="g.id"
          class="tree-node"
          :style="{ paddingLeft: g.depth * 18 + 12 + 'px' }"
          :class="{ active: selectedId === g.id }"
          @click="onTreeNodeClick(g)"
        >
          <span class="tree-toggle" @click.stop="toggleFold(g.id)">
            {{ g.children?.length ? (foldedIds.has(g.id) ? '▸' : '▾') : '·' }}
          </span>
          <span class="tree-name">{{ g.gridName }}</span>
          <span class="tree-level">{{ levelText(g.gridLevel) }}</span>
        </div>
      </div>
    </aside>

    <!-- 中：地图 -->
    <main class="map-panel">
      <div id="gridManageMap" class="map-container"></div>
      <div class="map-toolbar">
        <button class="tool-btn" :disabled="!selectedGrid || drawing || editing" @click="startEditArea">✏ 拖拽顶点</button>
        <button class="tool-btn" :disabled="!selectedGrid || drawing || editing" @click="startRedraw">🔲 重绘边界</button>
        <template v-if="drawing">
          <button class="tool-btn" :disabled="!drawPoints.length" @click="undoDrawPoint">↩ 撤销上一点（{{ drawPoints.length }}）</button>
          <button class="tool-btn primary" @click="finishDraw">✔ 完成绘制</button>
          <button class="tool-btn" @click="cancelDraw">取消</button>
        </template>
        <button v-if="editing" class="tool-btn primary" @click="finishEditArea">✔ 完成拖拽</button>
        <button v-if="editing" class="tool-btn" @click="cancelEditArea">取消拖拽（还原）</button>
        <button class="tool-btn danger" :disabled="!selectedGrid || drawing || editing" @click="removeGrid">🗑 删除网格</button>
      </div>
      <div class="map-tip" :class="{ warn: drawing }">
        <template v-if="drawing">⚠ 逐点点击绘制边界（已点 {{ drawPoints.length }} 个点）：双击或点击第一个红点/「完成绘制」闭合；画错了点「撤销上一点」</template>
        <template v-else-if="editing">正在编辑 {{ selectedGrid?.gridName }} 边界：拖动白色顶点调整，完成后点「完成拖拽」再点右侧「保存网格」</template>
        <template v-else>{{ tipText }}</template>
      </div>
    </main>

    <!-- 右：表单（仅点击「新增网格」或选中网格编辑时显示，默认隐藏避免遮挡地图） -->
    <aside v-if="formVisible" class="form-panel">
      <div class="panel-header">
        <h2>{{ form.id ? '编辑网格' : '新增网格' }}</h2>
        <button class="form-close" title="收起表单" @click="closeForm">✕</button>
      </div>
      <div class="form-body">
        <div class="field">
          <label>网格名称 *</label>
          <input v-model="form.gridName" placeholder="如：第1大网格 / 第1-1小网格" />
        </div>
        <div class="field">
          <label>网格编码</label>
          <input v-model="form.gridCode" placeholder="如：BJW-G01，留空自动编号" />
        </div>
        <div class="field">
          <label>父级网格</label>
          <select v-model="form.parentId" @change="onParentChange">
            <option :value="null">（顶级·社区）</option>
            <option v-for="opt in parentOptions" :key="opt.id" :value="opt.id" :disabled="opt.id === form.id">{{ opt.label }}</option>
          </select>
        </div>
        <div class="field">
          <label>排序</label>
          <input type="number" v-model.number="form.sortOrder" />
        </div>
        <div class="field">
          <label>状态</label>
          <select v-model="form.status">
            <option value="ACTIVE">启用</option>
            <option value="INACTIVE">停用</option>
          </select>
        </div>
        <div class="field">
          <label>备注</label>
          <textarea v-model="form.remark" rows="2" placeholder="选填"></textarea>
        </div>
        <div class="area-box">
          <div>网格层级：<strong>{{ form.gridLevel ? '第 ' + form.gridLevel + ' 级' : '—' }}</strong></div>
          <div>边界点数：<strong>{{ roiPoints }}</strong></div>
          <div>面积：<strong>{{ form.area ? Number(form.area).toFixed(2) + ' km²' : '未计算' }}</strong></div>
        </div>
        <div class="form-actions">
          <button class="btn-primary" :disabled="saving || drawing" @click="saveGrid">保存网格</button>
        </div>
      </div>
    </aside>

    <!-- 全局醒目提示（保存失败等错误/成功反馈） -->
    <transition name="toast">
      <div v-if="toast.visible" class="page-toast" :class="toast.type">
        <span class="toast-icon">{{ toast.type === 'error' ? '⛔' : '✔' }}</span>
        <span class="toast-msg">{{ toast.message }}</span>
        <button class="toast-close" @click="toast.visible = false">✕</button>
      </div>
    </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { getGridTree, createGrid, updateGrid, deleteGrid } from '../api'
import http from '../api'
import { confirmDialog } from '../utils/dialog'

// ==================== 地图中心点配置 ====================
const centerLng = ref('113.939521')
const centerLat = ref('22.971231')

async function loadMapCenter() {
  try {
    const [lng, lat] = await Promise.all([
      http.get('/system/config/map.center.lng'),
      http.get('/system/config/map.center.lat'),
    ])
    if (lng) centerLng.value = String(lng)
    if (lat) centerLat.value = String(lat)
  } catch (e) { /* 接口失败保持默认值 */ }
}

async function saveMapCenter() {
  const lng = Number(centerLng.value)
  const lat = Number(centerLat.value)
  if (isNaN(lng) || isNaN(lat) || lng < 73 || lng > 135 || lat < 18 || lat > 54) {
    confirmDialog({ message: '请输入有效的经纬度（经度 73~135，纬度 18~54）' })
    return
  }
  try {
    await http.put('/system/config/map.center.lng', { value: String(lng) })
    await http.put('/system/config/map.center.lat', { value: String(lat) })
    confirmDialog({ message: '地图中心点已保存，地图页面刷新后生效' })
  } catch (e: any) {
    confirmDialog({ message: '保存失败：' + (e?.message || '服务器异常') })
  }
}

interface GridNode {
  id: number
  gridName: string
  gridCode?: string
  gridLevel: number
  parentId?: number | null
  roiJson?: string
  area?: number | null
  sortOrder?: number
  status?: string
  remark?: string
  children?: GridNode[]
}

interface FlatNode extends GridNode {
  depth: number
}

const AMapLib: any = ref(null)
const map: any = ref(null)
const gridTree = ref<GridNode[]>([])
const flatTree = ref<FlatNode[]>([])
const foldedIds = ref<Set<number>>(new Set())
const polygons = ref<Map<number, any>>(new Map())
const defaultStyles = new Map<number, any>()
const selectedId = ref<number | null>(null)
const selectedGrid = ref<GridNode | null>(null)
const currentPolygon = ref<any>(null)
const polyEditor = ref<any>(null)
const drawing = ref(false)
// 自定义绘制状态：顶点数组 + 预览面 + 顶点标记（替代 MouseTool，支持逐点撤销）
const drawPoints = ref<[number, number][]>([])
let drawPreviewPoly: any = null
let drawVertexMarkers: any[] = []
let mapClickHandler: ((e: any) => void) | null = null
let mapDblHandler: (() => void) | null = null
const editing = ref(false)
const saving = ref(false)
// 右侧表单默认隐藏：仅点击「新增网格」或选中网格进入编辑时才弹出，避免空表单遮挡地图
const formVisible = ref(false)
watch(formVisible, () => {
  // 布局变化后地图容器宽度变了，需要 resize 才能铺满
  nextTick(() => setTimeout(() => map.value?.resize(), 60))
})
function closeForm() {
  if (drawing.value) cancelDraw()
  formVisible.value = false
}
const tipText = ref('点击左侧网格查看/调整区域；选中后可拖拽顶点或重绘边界')

// 醒目悬浮提示：替代 alert()，错误红色长驻 8 秒、成功绿色 3 秒，均可手动关闭
const toast = ref({ visible: false, type: 'error' as 'error' | 'success', message: '' })
let toastTimer: ReturnType<typeof setTimeout> | null = null
function notify(message: string, type: 'error' | 'success' = 'error') {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { visible: true, type, message }
  toastTimer = setTimeout(() => { toast.value.visible = false }, type === 'error' ? 8000 : 3000)
}

const form = ref<any>({
  id: null,
  gridName: '',
  gridCode: '',
  parentId: null,
  gridLevel: null,
  sortOrder: 0,
  status: 'ACTIVE',
  remark: '',
  area: null
})

const roiPoints = computed(() => {
  if (!currentPolygon.value) return 0
  const path = currentPolygon.value.getPath?.() || []
  return path.length
})

const parentOptions = computed(() =>
  flatTree.value
    .filter(n => n.id !== form.value.id)
    .map(n => ({ id: n.id, label: `${'　'.repeat(n.depth)}${n.gridName}（第${n.gridLevel}级）` }))
)

function levelText(level: number) {
  return level === 1 ? '社区' : level === 2 ? '大网格' : level === 3 ? '小网格' : `L${level}`
}

function flatten(nodes: GridNode[], depth: number): FlatNode[] {
  const result: FlatNode[] = []
  nodes.forEach(n => {
    result.push({ ...n, depth })
    if (n.children?.length && !foldedIds.value.has(n.id)) {
      result.push(...flatten(n.children, depth + 1))
    }
  })
  return result
}

function toggleFold(id: number) {
  const next = new Set(foldedIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  foldedIds.value = next
  flatTree.value = flatten(gridTree.value, 0)
}

/** 树节点点击：绘制/编辑中拦截切换，防止打断当前绘制导致已画内容丢失 */
function onTreeNodeClick(g: FlatNode) {
  if (drawing.value) {
    notify('正在绘制边界，请先完成绘制或点「取消」后再切换网格', 'error')
    return
  }
  selectGrid(g)
}

function loadTree() {
  flatTree.value = flatten(gridTree.value, 0)
}

async function reloadAll() {
  const tree = await getGridTree()
  gridTree.value = tree || []
  loadTree()
  clearPolylines()
  drawAllGrids()
}

/* ---------- 地图初始化与网格绘制 ---------- */

/** 层级配色：绘制与增量更新共用，保证样式一致 */
function gridStyleFor(level: number) {
  return level === 1
    ? { fillColor: '#0284c7', strokeColor: '#0284c7', strokeWeight: 3, fillOpacity: 0.08 }
    : level === 2
      ? { fillColor: '#f59e0b', strokeColor: '#f59e0b', strokeWeight: 2, fillOpacity: 0.1 }
      : { fillColor: '#10b981', strokeColor: '#10b981', strokeWeight: 1, fillOpacity: 0.08 }
}

function clearPolylines() {
  polygons.value.forEach(p => p.setMap(null))
  polygons.value = new Map()
  defaultStyles.clear()
  if (currentPolygon.value) {
    currentPolygon.value.setMap(null)
    currentPolygon.value = null
  }
}

function drawAllGrids() {
  const draw = (nodes: GridNode[]) => {
    nodes.forEach(n => {
      styleGridPoly(n)
      if (n.children?.length) draw(n.children)
    })
  }
  draw(gridTree.value)
}

/** 为单个网格创建/复用多边形（创建时绑定点击选中，绘制/编辑中不响应） */
function styleGridPoly(n: GridNode) {
  const m = map.value
  if (!m) return
  const coords = n.roiJson ? safeParse(n.roiJson) : null
  const existing = polygons.value.get(n.id)
  if (!coords || coords.length < 3) {
    existing?.setMap(null)
    polygons.value.delete(n.id)
    defaultStyles.delete(n.id)
    return
  }
  const style = gridStyleFor(n.gridLevel)
  if (existing) {
    existing.setPath(coords)
    existing.setOptions(style)
  } else {
    const poly = new AMapLib.value.Polygon({ path: coords, zIndex: 5, bubble: true, map: m, ...style })
    // 绘制/编辑中不响应选中，避免新增网格时误点已有网格导致绘制被打断、已画的点丢失
    poly.on('click', () => {
      if (drawing.value || editing.value) return
      selectGrid(n)
    })
    polygons.value.set(n.id, poly)
  }
  defaultStyles.set(n.id, style)
}

/** 轻量同步：保存/删除后只拉新树 + 增量更新多边形（setPath），不销毁重建全部覆盖物，交互反馈更快 */
async function syncTreeLight() {
  const tree = await getGridTree()
  gridTree.value = tree || []
  loadTree()
  const seen = new Set<number>()
  const walk = (nodes: GridNode[]) => {
    nodes.forEach(n => {
      seen.add(n.id)
      styleGridPoly(n)
      if (n.children?.length) walk(n.children)
    })
  }
  walk(gridTree.value)
  // 已删除网格的多边形从地图上移除
  for (const [id, poly] of [...polygons.value.entries()]) {
    if (!seen.has(id)) {
      poly.setMap(null)
      polygons.value.delete(id)
      defaultStyles.delete(id)
    }
  }
}

function safeParse(json?: string): any[] | null {
  try {
    const v = JSON.parse(json || '')
    return Array.isArray(v) && v.length >= 3 ? v : null
  } catch {
    return null
  }
}

// 记录上一次高亮的网格，切换时只刷新新旧两个多边形，避免每次选中全量 setOptions
let lastHighlightId: number | null = null

function highlight(id: number) {
  if (lastHighlightId !== null && lastHighlightId !== id) {
    const prev = polygons.value.get(lastHighlightId)
    if (prev) prev.setOptions(defaultStyles.get(lastHighlightId) || { fillOpacity: 0.06, strokeWeight: 1, zIndex: 5 })
  }
  const poly = polygons.value.get(id)
  if (poly) {
    const base = defaultStyles.get(id) || {}
    // 选中：保持层级色，仅加深
    poly.setOptions({ fillColor: base.fillColor, strokeColor: base.strokeColor, fillOpacity: 0.25, strokeWeight: 3, zIndex: 10 })
  }
  lastHighlightId = id || null
}

/* ---------- 网格选择 / 表单 ---------- */

function fillForm(g: GridNode) {
  form.value = {
    id: g.id,
    gridName: g.gridName || '',
    gridCode: g.gridCode || '',
    parentId: g.parentId ?? null,
    gridLevel: g.gridLevel,
    sortOrder: g.sortOrder ?? 0,
    status: g.status || 'ACTIVE',
    remark: g.remark || '',
    area: g.area ?? null
  }
}

async function selectGrid(g: GridNode, skipHighlight = false, skipUnsavedCheck = false) {
  if (editing.value) finishEditArea()
  if (drawing.value) cancelDraw()
  // 有未保存的新绘边界时先确认，避免点一下已有网格就把刚画的图静默清掉
  // （保存成功后的自动定位跳过该检查，避免误弹"边界还未保存"提示）
  if (!skipUnsavedCheck && !form.value.id && roiPoints.value >= 3 && !await confirmLeaveUnsaved()) return
  selectedId.value = g.id
  selectedGrid.value = g
  fillForm(g)
  formVisible.value = true
  if (!skipHighlight) highlight(g.id)
  const poly = polygons.value.get(g.id)
  if (poly) {
    // immediately=true：视野跳转无动画，选中响应更干脆
    map.value.setFitView([poly], true, [80, 80, 80, 80])
    setCurrentPolygon(poly)
  } else {
    setCurrentPolygon(null)
  }
  tipText.value = `已选中「${g.gridName}」，可拖拽顶点或重绘边界调整区域`
}

/** 未保存的新绘边界拦截：确认放弃才切换，取消则留在原地继续保存 */
function confirmLeaveUnsaved(): Promise<boolean> {
  return confirmDialog({ message: '当前新勾画的网格边界还未保存，切换后将被丢弃。\n确定放弃吗？（建议先点右侧「保存网格」）', danger: true, okText: '放弃' })
}

function setCurrentPolygon(poly: any | null) {
  const prev = currentPolygon.value
  if (prev && prev !== poly) {
    // 已保存网格的多边形由 polygons 集合管理，不能移除，否则切换选中后该网格就从地图上消失；
    // 只有临时多边形（新勾画未保存的）才需要从地图上清掉
    const isSavedGridPoly = [...polygons.value.values()].includes(prev)
    if (!isSavedGridPoly) prev.setMap(null)
  }
  currentPolygon.value = poly
  if (poly) {
    updateFormFromPolygon(poly)
  } else {
    form.value.roiJson = ''
    form.value.area = null
  }
}

function updateFormFromPolygon(poly: any) {
  try {
    const path: any[] = poly.getPath() || []
    const coords = path.map((p: any) => [Number(p.lng.toFixed(6)), Number(p.lat.toFixed(6))])
    form.value.area = Number(AMapLib.value.GeometryUtil.ringArea(coords) / 1_000_000)
  } catch (e) {
    console.error('计算面积失败', e)
  }
}

async function startCreate() {
  if (editing.value) finishEditArea()
  if (!form.value.id && roiPoints.value >= 3 && !await confirmLeaveUnsaved()) return
  formVisible.value = true
  selectedId.value = null
  selectedGrid.value = null
  highlight(0)
  setCurrentPolygon(null)
  form.value = {
    id: null, gridName: '', gridCode: '', parentId: null, gridLevel: null,
    sortOrder: flatTree.value.length + 1, status: 'ACTIVE', remark: '', area: null
  }
  tipText.value = '新增网格：请先在地图上绘制边界，再填写信息保存'
  startDraw()
}

function onParentChange() {
  const parent = flatTree.value.find(n => n.id === form.value.parentId)
  form.value.gridLevel = parent ? parent.gridLevel + 1 : 1
  if (parent && !form.value.gridCode) {
    form.value.gridCode = `${parent.gridCode || 'GRID'}-${String(flatTree.value.filter(n => n.parentId === parent.id).length + 1).padStart(2, '0')}`
  }
}

function resetForm() {
  setCurrentPolygon(null)
  if (selectedGrid.value) {
    selectGrid(selectedGrid.value)
  } else {
    form.value = { id: null, gridName: '', gridCode: '', parentId: null, gridLevel: null, sortOrder: 0, status: 'ACTIVE', remark: '', area: null }
  }
}

/* ---------- 地图绘制 / 编辑 ---------- */

function startDraw() {
  const m = map.value
  if (!m || drawing.value) return
  drawing.value = true
  drawPoints.value = []
  mapClickHandler = (e: any) => onDrawMapClick(e)
  mapDblHandler = () => finishDraw()
  m.on('click', mapClickHandler)
  // dblclick 在 click 之后触发，此时已多加一个重复点，finishDraw 内会自动去重
  m.on('dblclick', mapDblHandler)
  tipText.value = '绘制中：逐点点击地图添加顶点，画错可撤销，双击或点「完成绘制」闭合'
}

/** 绘制中点击：近首点则闭合，否则追加顶点 */
function onDrawMapClick(e: any) {
  const lng = e.lnglat.getLng()
  const lat = e.lnglat.getLat()
  const first = drawPoints.value[0]
  if (first && drawPoints.value.length >= 3) {
    const px = map.value.lngLatToContainer(new AMapLib.value.LngLat(lng, lat))
    const p0 = map.value.lngLatToContainer(new AMapLib.value.LngLat(first[0], first[1]))
    if (Math.hypot(px.x - p0.x, px.y - p0.y) < 16) {
      finishDraw()
      return
    }
  }
  drawPoints.value.push([lng, lat])
  refreshDrawPreview()
}

/** 根据顶点数组重绘预览：≥3 点显示半透明预览面，顶点用标记高亮（首点红色提示可点击闭合，均可拖动调整） */
function refreshDrawPreview() {
  const pts = drawPoints.value
  drawVertexMarkers.forEach(mk => mk.setMap(null))
  drawVertexMarkers = pts.map((p, i) => {
    const mk = new AMapLib.value.Marker({
      position: p,
      content: `<div style="width:${i === 0 ? '14px' : '10px'};height:${i === 0 ? '14px' : '10px'};border-radius:50%;background:${i === 0 ? '#ef4444' : '#0284c7'};border:2px solid #fff;box-shadow:0 0 4px rgba(0,0,0,0.4);cursor:move;"></div>`,
      offset: new AMapLib.value.Pixel(i === 0 ? -8 : -6, i === 0 ? -8 : -6),
      zIndex: 20,
      draggable: true,
      map: map.value
    })
    // 拖动顶点：更新对应坐标并重绘预览，绘制中即可微调区域
    mk.on('dragend', (e: any) => {
      const pos = e.target.getPosition()
      drawPoints.value[i] = [pos.getLng(), pos.getLat()]
      refreshDrawPreview()
    })
    return mk
  })
  if (pts.length >= 3) {
    if (!drawPreviewPoly) {
      drawPreviewPoly = new AMapLib.value.Polygon({
        path: pts, map: map.value,
        strokeColor: '#0284c7', fillColor: '#0284c7', fillOpacity: 0.2, strokeWeight: 2, zIndex: 11
      })
    } else {
      drawPreviewPoly.setPath(pts)
    }
  } else if (drawPreviewPoly) {
    drawPreviewPoly.setMap(null)
    drawPreviewPoly = null
  }
}

/** 撤销上一个顶点 */
function undoDrawPoint() {
  if (!drawing.value || !drawPoints.value.length) return
  drawPoints.value.pop()
  refreshDrawPreview()
}

/** 完成绘制：顶点转正式多边形（双击闭合时自动去除重复尾点） */
function finishDraw() {
  const pts = drawPoints.value.slice()
  // 双击闭合会在同一位置连加两个点，先去掉相邻重复点；再去掉与首点重合的尾点
  while (pts.length >= 2 && pts[pts.length - 1][0] === pts[pts.length - 2][0] && pts[pts.length - 1][1] === pts[pts.length - 2][1]) {
    pts.pop()
  }
  if (pts.length >= 4 && pts[0][0] === pts[pts.length - 1][0] && pts[0][1] === pts[pts.length - 1][1]) {
    pts.pop()
  }
  if (pts.length < 3) {
    tipText.value = '至少需要 3 个点才能闭合为网格边界，请继续点击地图添加顶点'
    return
  }
  teardownDrawOverlays()
  drawing.value = false
  const poly = new AMapLib.value.Polygon({
    path: pts, map: map.value,
    strokeColor: '#0284c7', fillColor: '#0284c7', fillOpacity: 0.2, strokeWeight: 2, zIndex: 11
  })
  setCurrentPolygon(poly)
  // 绘制完成后自动进入顶点编辑模式，可直接拖动白色顶点微调区域（新增网格无 selectedGrid，不走 startEditArea）
  editing.value = true
  editBackupPath = pts.map(p => [p[0], p[1]])
  polyEditor.value = new AMapLib.value.PolyEditor(map.value, poly)
  polyEditor.value.open()
  tipText.value = '边界绘制完成，可拖动白色顶点微调区域；满意后填写右侧信息保存，「完成拖拽」退出拖动模式'
}
function cancelDraw() {
  const prev = selectedGrid.value?.roiJson ? safeParse(selectedGrid.value.roiJson) : null
  teardownDrawOverlays()
  drawing.value = false
  if (prev && currentPolygon.value) {
    currentPolygon.value.setPath(prev)
    // 重绘前隐藏过旧边界，取消时需恢复显示
    currentPolygon.value.setMap(map.value)
  }
  tipText.value = '已取消绘制'
}

/** 清理绘制态的全部临时图层与事件监听 */
function teardownDrawOverlays() {
  const m = map.value
  if (m) {
    if (mapClickHandler) m.off('click', mapClickHandler)
    if (mapDblHandler) m.off('dblclick', mapDblHandler)
  }
  mapClickHandler = mapDblHandler = null
  drawVertexMarkers.forEach(mk => mk.setMap(null))
  drawVertexMarkers = []
  drawPreviewPoly?.setMap(null)
  drawPreviewPoly = null
  drawPoints.value = []
}

// 进入顶点编辑前的原始顶点备份：取消编辑时用它还原（PolyEditor 会实时改写 polygon 路径，不能靠拖动前的引用）
let editBackupPath: [number, number][] | null = null

function startEditArea() {
  if (!currentPolygon.value || !selectedGrid.value) return
  const path = currentPolygon.value.getPath?.() || []
  if (!path.length) {
    tipText.value = '该网格还没有边界，请使用「重绘边界」绘制'
    return
  }
  // 备份进入编辑时的顶点，取消时还原
  editBackupPath = path.map((p: any) => [Number(p.lng), Number(p.lat)])
  editing.value = true
  polyEditor.value = new AMapLib.value.PolyEditor(map.value, currentPolygon.value)
  polyEditor.value.open()
}

function finishEditArea() {
  if (!editing.value) return
  polyEditor.value?.close()
  polyEditor.value = null
  editing.value = false
  editBackupPath = null
  updateFormFromPolygon(currentPolygon.value)
  tipText.value = '拖拽完成，点击右侧「保存网格」后生效'
}

function cancelEditArea() {
  polyEditor.value?.close()
  polyEditor.value = null
  editing.value = false
  // 优先用进入编辑时的备份，其次用已保存的 roiJson
  const coords = editBackupPath || (selectedGrid.value?.roiJson ? safeParse(selectedGrid.value.roiJson) : null)
  editBackupPath = null
  const poly = currentPolygon.value
  if (coords && poly) {
    // PolyEditor.close() 内部对路径的最后同步可能晚于当前时序，延迟一帧还原，避免被拖拽后的路径覆盖回去
    setTimeout(() => {
      poly.setPath(coords)
      updateFormFromPolygon(poly)
    }, 0)
  }
  tipText.value = '已取消拖拽，边界已还原'
}

function startRedraw() {
  if (!selectedGrid.value) return
  // 重绘前先隐藏旧边界，避免绘制时新旧两层重叠干扰；
  // 取消绘制/保存后 reloadAll 会恢复
  if (selectedGrid.value.id != null) {
    polygons.value.get(selectedGrid.value.id)?.setMap(null)
  }
  startDraw()
}

/* ---------- 保存 / 删除 ---------- */

function collectRoiJson(): string | null {
  if (!currentPolygon.value) return null
  const path: any[] = currentPolygon.value.getPath?.() || []
  if (path.length < 3) return null
  return JSON.stringify(path.map((p: any) => [Number(p.lng.toFixed(6)), Number(p.lat.toFixed(6))]))
}

async function saveGrid() {
  if (editing.value) finishEditArea()
  if (!form.value.gridName?.trim()) {
    notify('请填写网格名称')
    return
  }
  if (drawing.value) {
    notify('请先完成边界绘制（闭合多边形）')
    return
  }
  const roiJson = collectRoiJson()
  if (!roiJson) {
    notify('请先在右侧地图上绘制/确认网格边界（至少 3 个点）')
    return
  }
  saving.value = true
  try {
    const payload = {
      gridName: form.value.gridName.trim(),
      gridCode: form.value.gridCode?.trim() || null,
      parentId: form.value.parentId,
      gridLevel: form.value.gridLevel,
      sortOrder: form.value.sortOrder ?? 0,
      status: form.value.status,
      remark: form.value.remark || null,
      roiJson,
      area: form.value.area
    }
    if (form.value.id) {
      await updateGrid(form.value.id, payload)
    } else {
      const created = await createGrid(payload)
      if (created?.id) form.value.id = created.id
    }
    // 轻量同步：增量更新多边形而非销毁重建，保存后立即看到最新边界
    await syncTreeLight()
    // 保存成功后完全回到初始状态（无选中、无表单、按钮禁用）
    selectedId.value = null
    selectedGrid.value = null
    highlight(0)
    formVisible.value = false
    setCurrentPolygon(null)
    tipText.value = '点击左侧网格查看/调整区域；选中后可拖拽顶点或重绘边界'
    notify(`网格${form.value.id ? '已更新' : '已创建'}：${payload.gridName}`, 'success')
  } catch (e: any) {
    notify(`保存失败：${e?.message || '服务器内部异常，请稍后重试'}`)
  } finally {
    saving.value = false
  }
}

async function removeGrid() {
  if (!selectedGrid.value) return
  if (editing.value) finishEditArea()
  const g = selectedGrid.value
  if (!await confirmDialog({ message: `确定删除网格「${g.gridName}」？\n（该网格下有子网格时将无法删除）`, danger: true, okText: '删除' })) return
  try {
    await deleteGrid(g.id)
    notify('网格已删除', 'success')
    selectedId.value = null
    selectedGrid.value = null
    // 隐藏右侧表单并清理状态，完全回到初始状态（包括提示文字）
    formVisible.value = false
    setCurrentPolygon(null)
    tipText.value = '点击左侧网格查看/调整区域；选中后可拖拽顶点或重绘边界'
    resetForm()
    await syncTreeLight()
  } catch (e: any) {
    notify(`删除失败：${e?.message || '服务器内部异常，请稍后重试'}`)
  }
}

/* ---------- 初始化 ---------- */

onMounted(async () => {
  loadMapCenter()
  ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
  AMapLib.value = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.Polygon', 'AMap.Polyline', 'AMap.Marker', 'AMap.PolyEditor']
  })
  map.value = new AMapLib.value.Map('gridManageMap', {
    zoom: 13,
    center: [113.939521, 22.971231],
    mapStyle: 'amap://styles/normal',
    // 性能优化：纯平面绘制场景用 2D 渲染（避开 WebGL 开销），关闭室内图/楼块降低渲染负担
    viewMode: '2D',
    showIndoorMap: false,
    showBuildingBlock: false,
    showLabel: true,
    // 关闭视野变换/缩放动画：拖拽、点击选中等交互响应更直接
    animateEnable: false,
    zoomAnimation: false
  })
  await reloadAll()
})
</script>

<style scoped>
.grid-manage-page {
  display: grid;
  grid-template-columns: 280px 1fr 340px;
  gap: 14px;
  height: calc(100vh - 140px);
  min-height: 520px;
}
/* 表单隐藏时地图占据右侧全部空间 */
.grid-manage-page.no-form { grid-template-columns: 280px 1fr; }
.form-close {
  border: none;
  background: #f3f4f6;
  color: #6b7280;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
}
.form-close:hover { background: #fee2e2; color: #dc2626; }
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #e5e7eb;
}
.panel-header h2 { font-size: 15px; font-weight: 700; color: #111827; margin: 0; }
.btn-primary {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: #0284c7;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-plain {
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  cursor: pointer;
}

/* 左：树 */
.tree-panel, .form-panel, .map-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.tree-scroll { flex: 1; overflow-y: auto; padding: 6px 0; }
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px 8px 12px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  border-left: 3px solid transparent;
}
.tree-node:hover { background: #f3f7fb; }
.tree-node.active { background: #e6f2fa; border-left-color: #0284c7; font-weight: 600; color: #075985; }
.tree-toggle { width: 14px; font-size: 11px; color: #6b7280; flex-shrink: 0; }
.tree-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-level { font-size: 11px; color: #9ca3af; background: #f3f4f6; border-radius: 4px; padding: 1px 6px; }
.empty-tip { padding: 30px 14px; text-align: center; color: #9ca3af; font-size: 13px; }

/* 中：地图 */
.map-panel { position: relative; }
.map-container { flex: 1; min-height: 0; }
.map-toolbar {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tool-btn {
  padding: 7px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.tool-btn.primary { background: #0284c7; color: #fff; border-color: #0284c7; }
.tool-btn.danger { color: #dc2626; border-color: #fca5a5; }
.tool-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.map-tip {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  z-index: 10;
  background: rgba(17, 24, 39, 0.82);
  color: #e5e7eb;
  font-size: 12px;
  border-radius: 8px;
  padding: 9px 12px;
}
.map-tip.warn { background: rgba(217, 119, 6, 0.92); color: #fff; font-weight: 600; }

/* 右：表单 */
.form-body { flex: 1; overflow-y: auto; padding: 14px; }
.field { margin-bottom: 12px; }
.field label { display: block; font-size: 12px; color: #6b7280; margin-bottom: 5px; }
.field input, .field select, .field textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  color: #111827;
  box-sizing: border-box;
  background: #fff;
}
.area-box {
  background: #f8fafc;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.9;
}
.area-box strong { color: #0f766e; }
.form-actions { display: flex; gap: 10px; margin-top: 14px; }

/* 全局醒目提示 */
.page-toast {
  position: fixed;
  top: 70px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3000;
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 620px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.25);
}
.page-toast.error {
  background: #dc2626;
  color: #fff;
  border: 1px solid #b91c1c;
}
.page-toast.success {
  background: #059669;
  color: #fff;
  border: 1px solid #047857;
}
.toast-icon { font-size: 16px; flex-shrink: 0; }
.toast-msg { flex: 1; word-break: break-all; }
.toast-close {
  flex-shrink: 0;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border-radius: 4px;
  width: 22px;
  height: 22px;
  cursor: pointer;
  font-size: 12px;
}
.toast-enter-active, .toast-leave-active { transition: all 0.25s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(-16px); }
</style>