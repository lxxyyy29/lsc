<template>
  <section class="grid-view">
    <header class="grid-view__header">
      <h2>GIS 网格可视化</h2>
      <p class="grid-view__subtitle">拔蛟窝社区 — 6大网格 → 12小网格</p>
    </header>

    <div class="grid-view__body">
      <aside class="grid-view__sidebar">
        <div class="panel">
          <h3 class="panel__title">网格列表</h3>
          <el-tree
            v-if="tree.length"
            :data="treeProps"
            :props="{ label: 'gridName', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="grid-tree__node">
                <el-tag size="small" :type="levelTagType(data.gridLevel)">
                  {{ levelLabel(data.gridLevel) }}
                </el-tag>
                <span class="grid-tree__name">{{ node.label }}</span>
                <span class="grid-tree__meta">人口 {{ data.population || 0 }}</span>
              </span>
            </template>
          </el-tree>
          <el-empty v-else description="暂无网格数据" :image-size="80" />
        </div>

        <div v-if="selectedGrid" class="panel panel--detail">
          <h3 class="panel__title">网格详情</h3>
          <dl class="detail-list">
            <dt>名称</dt>
            <dd>{{ selectedGrid.gridName }}</dd>
            <dt>编码</dt>
            <dd>{{ selectedGrid.gridCode }}</dd>
            <dt>层级</dt>
            <dd>{{ levelLabel(selectedGrid.gridLevel) }}</dd>
            <dt>面积</dt>
            <dd>{{ selectedGrid.area || '-' }} km²</dd>
            <dt>人口</dt>
            <dd>{{ selectedGrid.population || 0 }} 人</dd>
            <dt>楼栋</dt>
            <dd>{{ selectedGrid.buildingCount || 0 }} 栋</dd>
          </dl>
        </div>
      </aside>

      <main class="grid-view__map-container">
        <div ref="mapRef" class="grid-view__map"></div>
      </main>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { getGridTree, GridTreeVo } from '../../api/community'
import AMapLoader from '@amap/amap-jsapi-loader'

const tree = ref<GridTreeVo[]>([])
const selectedGrid = ref<GridTreeVo | null>(null)
const mapRef = ref<HTMLDivElement | null>(null)

let mapInstance: any = null
let mouseTool: any = null

const treeProps = computed(() => tree.value)

function levelLabel(level?: number) {
  return level === 1 ? '社区' : level === 2 ? '大网格' : level === 3 ? '小网格' : '-'
}

function levelTagType(level?: number) {
  return level === 1 ? 'primary' : level === 2 ? 'success' : 'warning'
}

function handleNodeClick(data: GridTreeVo) {
  selectedGrid.value = data
  if (data.roiJson && mapInstance) {
    drawGridPolygon(data)
  }
}

function drawGridPolygon(grid: GridTreeVo) {
  try {
    const coords = JSON.parse(grid.roiJson)
    if (!Array.isArray(coords) || coords.length < 3) return

    mapInstance.clearMap()
    const polygon = new window.AMap.Polygon({
      path: coords,
      fillColor: '#1e88e5',
      fillOpacity: 0.25,
      strokeColor: '#57b9ff',
      strokeWeight: 2,
    })
    mapInstance.add(polygon)
    mapInstance.setFitView([polygon])
  } catch (e) {
    console.warn('绘制网格边界失败', e)
  }
}

async function loadTree() {
  try {
    tree.value = await getGridTree()
  } catch (e) {
    console.error('加载网格树失败', e)
  }
}

onMounted(async () => {
  await loadTree()

  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    const AMap = await AMapLoader.load({
      key: '5e00e01d2d2b6ca9e1eed533a15572e4',
      version: '2.0',
      plugins: ['AMap.MouseTool', 'AMap.Polygon'],
    })

    if (mapRef.value) {
      mapInstance = new AMap.Map(mapRef.value, {
        zoom: 14,
        center: [113.939521, 22.971231],
        mapStyle: 'amap://styles/dark',
      })
    }
  } catch (e) {
    console.error('地图加载失败', e)
  }
})

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.destroy()
  }
})
</script>

<style scoped>
.grid-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 16px;
}

.grid-view__header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--fg-text-primary);
}

.grid-view__subtitle {
  margin: 4px 0 0;
  color: var(--fg-text-secondary);
  font-size: 13px;
}

.grid-view__body {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.grid-view__sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.panel {
  background: var(--fg-bg-card);
  border: 1px solid var(--fg-border);
  border-radius: var(--fg-radius-lg);
  padding: 16px;
}

.panel__title {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--fg-text-primary);
}

/* 覆盖 el-tree 默认白色背景，统一深色主题 */
:deep(.el-tree) {
  background: transparent;
  color: var(--fg-text-primary);
}

:deep(.el-tree-node__content) {
  background: transparent;
}

:deep(.el-tree-node__content:hover) {
  background: rgba(94, 162, 255, 0.12);
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(94, 162, 255, 0.2);
}

:deep(.el-tree-node__label) {
  color: var(--fg-text-primary);
}

:deep(.el-tree-node__expand-icon) {
  color: var(--fg-text-secondary);
}

.grid-tree__node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.grid-tree__name {
  flex: 1;
  color: var(--fg-text-primary);
}

.grid-tree__meta {
  color: var(--fg-text-secondary);
  font-size: 12px;
}

:deep(.el-tag) {
  border: none;
}

.detail-list {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px 16px;
  margin: 0;
}

.detail-list dt {
  color: #7ea4c8;
  font-size: 13px;
}

.detail-list dd {
  margin: 0;
  color: #eaf5ff;
  font-size: 13px;
}

.grid-view__map-container {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(110, 194, 255, 0.14);
}

.grid-view__map {
  width: 100%;
  height: 100%;
  min-height: 500px;
}
</style>
