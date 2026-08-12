<template>
  <div class="grid-manage-page">
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
          @click="selectGrid(g)"
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
        <button v-if="editing" class="tool-btn primary" @click="finishEditArea">✔ 完成编辑</button>
        <button v-if="editing" class="tool-btn" @click="cancelEditArea">取消</button>
        <button class="tool-btn danger" :disabled="!selectedGrid || drawing || editing" @click="removeGrid">🗑 删除网格</button>
      </div>
      <div class="map-tip" :class="{ warn: drawing }">
        <template v-if="drawing">⚠ 请在地图上点击绘制新网格边界（首尾相连闭合），完成后点击「保存网格」</template>
        <template v-else-if="editing">正在编辑 {{ selectedGrid?.gridName }} 边界：拖动白色顶点调整，完成后点击「完成编辑」</template>
        <template v-else>{{ tipText }}</template>
      </div>
    </main>

    <!-- 右：表单 -->
    <aside class="form-panel">
      <div class="panel-header"><h2>{{ form.id ? '编辑网格' : '新增网格' }}</h2></div>
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
          <button v-if="form.id" class="btn-plain" @click="resetForm">取消编辑</button>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { getGridTree, createGrid, updateGrid, deleteGrid } from '../api'

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
const mouseTool = ref<any>(null)
const drawing = ref(false)
const editing = ref(false)
const saving = ref(false)
const tipText = ref('点击左侧网格查看/调整区域；选中后可拖拽顶点或重绘边界')

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
  const m = map.value
  if (!m) return
  const draw = (nodes: GridNode[]) => {
    nodes.forEach(n => {
      if (n.roiJson) {
        const coords = safeParse(n.roiJson)
        if (coords?.length >= 3) {
          const style = n.gridLevel === 1
            ? { fillColor: '#0284c7', strokeColor: '#0284c7', strokeWeight: 3, fillOpacity: 0.08 }
            : n.gridLevel === 2
              ? { fillColor: '#f59e0b', strokeColor: '#f59e0b', strokeWeight: 2, fillOpacity: 0.1 }
              : { fillColor: '#10b981', strokeColor: '#10b981', strokeWeight: 1, fillOpacity: 0.08 }
          const poly = new AMapLib.value.Polygon({ path: coords, zIndex: 5, bubble: true, map: m, ...style })
          poly.on('click', () => selectGrid(n))
          polygons.value.set(n.id, poly)
          defaultStyles.set(n.id, style)
        }
      }
      if (n.children?.length) draw(n.children)
    })
  }
  draw(gridTree.value)
}

function safeParse(json?: string): any[] | null {
  try {
    const v = JSON.parse(json || '')
    return Array.isArray(v) && v.length >= 3 ? v : null
  } catch {
    return null
  }
}

function highlight(id: number) {
  polygons.value.forEach((poly, pid) => {
    const active = pid === id
    if (active) {
      const base = defaultStyles.get(pid) || {}
      // 选中：保持层级色，仅加深
      poly.setOptions({ fillColor: base.fillColor, strokeColor: base.strokeColor, fillOpacity: 0.25, strokeWeight: 3, zIndex: 10 })
    } else {
      // 非选中：恢复各层级默认样式，不统一覆盖
      poly.setOptions(defaultStyles.get(pid) || { fillOpacity: 0.06, strokeWeight: 1, zIndex: 5 })
    }
  })
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

function selectGrid(g: GridNode, skipHighlight = false) {
  if (editing.value) finishEditArea()
  if (drawing.value) cancelDraw()
  selectedId.value = g.id
  selectedGrid.value = g
  fillForm(g)
  if (!skipHighlight) highlight(g.id)
  const poly = polygons.value.get(g.id)
  if (poly) {
    map.value.setFitView([poly], false, [80, 80, 80, 80])
    setCurrentPolygon(poly)
  } else {
    setCurrentPolygon(null)
  }
  tipText.value = `已选中「${g.gridName}」，可拖拽顶点或重绘边界调整区域`
}

function setCurrentPolygon(poly: any | null) {
  if (currentPolygon.value && currentPolygon.value !== poly) {
    currentPolygon.value.setMap(null)
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

function startCreate() {
  if (editing.value) finishEditArea()
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
  mouseTool.value = new AMapLib.value.MouseTool(m)
  mouseTool.value.polygon({
    strokeColor: '#0284c7', fillColor: '#0284c7', fillOpacity: 0.2, strokeWeight: 2
  })
  mouseTool.value.on('draw-complete', (e: any) => {
    const poly = e.obj
    mouseTool.value?.close()
    drawing.value = false
    setCurrentPolygon(poly)
    tipText.value = '边界绘制完成，可填写右侧信息后保存'
  })
}

function cancelDraw() {
  drawing.value = false
  mouseTool.value?.close()
}

function startEditArea() {
  if (!currentPolygon.value || !selectedGrid.value) return
  if (!currentPolygon.value.getPath?.().length) {
    tipText.value = '该网格还没有边界，请使用「重绘边界」绘制'
    return
  }
  editing.value = true
  polyEditor.value = new AMapLib.value.PolyEditor(map.value, currentPolygon.value)
  polyEditor.value.open()
}

function finishEditArea() {
  if (!editing.value) return
  polyEditor.value?.close()
  polyEditor.value = null
  editing.value = false
  updateFormFromPolygon(currentPolygon.value)
  tipText.value = '边界已更新，点击「保存网格」生效'
}

function cancelEditArea() {
  polyEditor.value?.close()
  polyEditor.value = null
  editing.value = false
  // 重新加载原边界
  if (selectedGrid.value?.roiJson) {
    const coords = safeParse(selectedGrid.value.roiJson)
    if (coords && currentPolygon.value) {
      currentPolygon.value.setPath(coords)
      updateFormFromPolygon(currentPolygon.value)
    }
  }
  tipText.value = '已取消编辑'
}

function startRedraw() {
  if (!selectedGrid.value) return
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
  if (!form.value.gridName?.trim()) {
    alert('请填写网格名称')
    return
  }
  if (drawing.value) {
    alert('请先完成边界绘制（闭合多边形）')
    return
  }
  const roiJson = collectRoiJson()
  if (!roiJson) {
    alert('请先在右侧地图上绘制/确认网格边界（至少 3 个点）')
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
      alert('网格已更新')
    } else {
      await createGrid(payload)
      alert('网格已创建')
    }
    await reloadAll()
    // 定位到新/更新的网格（视野+表单），不改变其样式，保持与同级网格一致
    const target = flatTree.value.find(n => n.gridName === payload.gridName)
    if (target) selectGrid(target, true)
    else resetForm()
  } catch (e: any) {
    alert(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeGrid() {
  if (!selectedGrid.value) return
  const g = selectedGrid.value
  if (!window.confirm(`确定删除网格「${g.gridName}」？\n（该网格下有子网格时将无法删除）`)) return
  try {
    await deleteGrid(g.id)
    alert('网格已删除')
    selectedId.value = null
    selectedGrid.value = null
    resetForm()
    await reloadAll()
  } catch (e: any) {
    alert(e?.message || '删除失败')
  }
}

/* ---------- 初始化 ---------- */

onMounted(async () => {
  ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
  AMapLib.value = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.Polygon', 'AMap.PolyEditor', 'AMap.MouseTool']
  })
  map.value = new AMapLib.value.Map('gridManageMap', {
    zoom: 13,
    center: [113.939521, 22.971231],
    mapStyle: 'amap://styles/normal'
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
</style>
