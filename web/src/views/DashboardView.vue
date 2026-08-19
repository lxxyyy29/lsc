<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">全域态势看板</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">一屏观全域、以图管格、以格管人（悬停查看网格，点击查看详情）</p>

    <!-- 错误提示 -->
    <div v-if="loadError" style="background:#fff1f0;border:1px solid #ffa39e;border-radius:6px;padding:12px 16px;margin-bottom:16px;display:flex;align-items:flex-start;gap:8px;">
      <i class="fas fa-exclamation-circle" style="color:#ff4d4f;font-size:16px;margin-top:2px;flex-shrink:0;"></i>
      <div>
        <p style="font-size:13px;font-weight:600;color:#cf1322;margin:0 0 4px;">数据加载异常</p>
        <p style="font-size:12px;color:#a8071a;margin:0;line-height:1.6;">{{ loadError }}</p>
      </div>
    </div>

    <!-- KPI 卡片 -->
    <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:14px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">社区面积</p>
            <p class="stat-value" style="font-size:22px;">{{ communityArea }} <span style="font-size:12px;color:#9ca3af;">km²</span></p>
          </div>
          <div style="width:40px;height:40px;background:#e6f4ff;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-map" style="color:#1890FF;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-green">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">大网格</p>
            <p class="stat-value">{{ overview.largeGridCount || 6 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#f6ffed;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-layer-group" style="color:#52C41A;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-purple">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">小网格</p>
            <p class="stat-value">{{ overview.smallGridCount || 12 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#f5f3ff;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-th" style="color:#7c3aed;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-orange">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">事件总数</p>
            <p class="stat-value">{{ overview.eventTotal || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#fff7e6;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-exclamation-triangle" style="color:#FAAD14;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-red">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">紧急事件</p>
            <p class="stat-value">{{ overview.eventRed || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#fff1f0;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-fire" style="color:#FF4D4F;font-size:18px;"></i></div>
        </div>
      </div>
    </div>

    <!-- 地图 + 三色分级 -->
    <div style="display:grid;grid-template-columns:2fr 1fr;gap:16px;margin-bottom:20px;">
      <div class="card" style="position:relative;padding:0;overflow:hidden;">
        <h3 style="font-size:14px;font-weight:600;margin:16px 16px 0;"><i class="fas fa-map-marked-alt" style="color:#1890FF;margin-right:6px;"></i>社区网格GIS</h3>
        <div id="gisMap" style="height:360px;margin:12px;border-radius:8px;overflow:hidden;"></div>

        <!-- 悬停提示框 -->
        <div v-if="hoverInfo.visible" :style="{
          position: 'absolute',
          left: hoverInfo.x + 'px',
          top: hoverInfo.y + 'px',
          transform: 'translate(-50%, calc(-100% - 14px))',
          background: 'rgba(17,24,39,0.92)',
          color: '#fff',
          padding: '6px 12px',
          borderRadius: '6px',
          fontSize: '12px',
          fontWeight: '600',
          pointerEvents: 'none',
          zIndex: 1000,
          whiteSpace: 'nowrap',
          boxShadow: '0 4px 16px rgba(0,0,0,0.3)'
        }">
          {{ hoverInfo.name }}
          <div style="position:absolute;left:50%;bottom:-5px;transform:translateX(-50%);width:0;height:0;border-left:5px solid transparent;border-right:5px solid transparent;border-top:5px solid rgba(17,24,39,0.92);"></div>
        </div>

        <!-- 点击选中信息面板 -->
        <div v-if="selectedGrid" :style="{
          position: 'absolute',
          top: '56px',
          right: '16px',
          background: '#fff',
          borderRadius: '10px',
          padding: '12px 16px',
          minWidth: '180px',
          zIndex: 100,
          boxShadow: '0 6px 24px rgba(0,0,0,0.15)'
        }">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;">
            <span style="font-size:13px;font-weight:700;color:#111827;">{{ selectedGrid.gridName }}</span>
            <button @click="selectedGrid = null" style="border:none;background:none;cursor:pointer;color:#9ca3af;font-size:14px;padding:0 4px;">&times;</button>
          </div>
          <div style="font-size:11px;color:#6b7280;line-height:1.7;">
            <div>层级：<strong style="color:#374151;">{{ selectedGrid.gridLevel === 2 ? '二级网格' : '三级网格' }}</strong></div>
            <div>负责人：<strong style="color:#374151;">{{ selectedGrid.managerName || '-' }}</strong></div>
            <div>状态：<strong :style="selectedGrid.status === 'ACTIVE' ? 'color:#52c41a;' : 'color:#f5222d;'">{{ selectedGrid.status === 'ACTIVE' ? '启用中' : '已停用' }}</strong></div>
          </div>
        </div>
      </div>
      <div class="card">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:16px;"><i class="fas fa-chart-pie" style="color:#1890FF;margin-right:6px;"></i>三色分级</h3>
        <div style="margin-bottom:16px;">
          <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px;"><span>一般（绿）</span><span>{{ overview.eventGreen || 0 }}</span></div>
          <div style="height:8px;background:#f3f4f6;border-radius:4px;"><div :style="{width: greenPct + '%', height:'100%', background:'#52C41A', borderRadius:'4px'}"></div></div>
        </div>
        <div style="margin-bottom:16px;">
          <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px;"><span>重点（黄）</span><span>{{ overview.eventYellow || 0 }}</span></div>
          <div style="height:8px;background:#f3f4f6;border-radius:4px;"><div :style="{width: yellowPct + '%', height:'100%', background:'#FAAD14', borderRadius:'4px'}"></div></div>
        </div>
        <div style="margin-bottom:16px;">
          <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px;"><span>紧急（红）</span><span>{{ overview.eventRed || 0 }}</span></div>
          <div style="height:8px;background:#f3f4f6;border-radius:4px;"><div :style="{width: redPct + '%', height:'100%', background:'#FF4D4F', borderRadius:'4px'}"></div></div>
        </div>
      </div>
    </div>

    <!-- 图表 + 事件列表 -->
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
      <div class="card">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">网格人口排名</h3>
        <div id="chartPopulation" style="height:240px;"></div>
      </div>
      <div class="card">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">最新事件</h3>
        <div style="max-height:240px;overflow-y:auto;">
          <div v-for="evt in events" :key="evt.id" style="display:flex;align-items:center;padding:8px 0;border-bottom:1px solid #f3f4f6;">
            <span :class="['tag', evt.urgencyLevel === 'RED' ? 'tag-red' : evt.urgencyLevel === 'YELLOW' ? 'tag-orange' : 'tag-green']" style="margin-right:8px;">
              {{ evt.urgencyLevel === 'RED' ? '紧急' : evt.urgencyLevel === 'YELLOW' ? '重点' : '一般' }}
            </span>
            <span style="font-size:13px;color:#374151;">{{ evt.title }}</span>
          </div>
          <p v-if="!events.length" style="font-size:12px;color:#9ca3af;text-align:center;padding:20px;">暂无事件</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { getDashboardOverview, getGridStats, getGridTree, getEvents } from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'
import * as echarts from 'echarts'

const overview = ref<any>({})
const events = ref<any[]>([])
const loadError = ref('')
const communityArea = ref('2.50')
const allGrids = ref<any[]>([])

// 保存实例引用以便销毁
let mapInstance: any = null
let chartInstance: any = null
let hoverId = 0

const hoverInfo = reactive({ visible: false, x: 0, y: 0, name: '', id: 0 })
const selectedGrid = ref<any>(null)

const total = computed(() => (overview.value.eventGreen || 0) + (overview.value.eventYellow || 0) + (overview.value.eventRed || 0))
const greenPct = computed(() => total.value ? ((overview.value.eventGreen || 0) / total.value * 100) : 0)
const yellowPct = computed(() => total.value ? ((overview.value.eventYellow || 0) / total.value * 100) : 0)
const redPct = computed(() => total.value ? ((overview.value.eventRed || 0) / total.value * 100) : 0)

onMounted(async () => {
  const errors: string[] = []

  try { overview.value = await getDashboardOverview() } catch (e: any) { errors.push('概览数据加载失败: ' + (e?.message || e)) }
  let stats: any = {}
  try { stats = await getGridStats() } catch (e: any) { errors.push('网格统计加载失败: ' + (e?.message || e)) }
  let tree: any = []
  try {
    tree = await getGridTree()
    // 提取所有网格计算面积
    const flatten = (nodes: any[]) => {
      for (const n of nodes) {
        allGrids.value.push(n)
        if (n.children) flatten(n.children)
      }
    }
    flatten(tree)
    const community = allGrids.value.find((g: any) => g.gridLevel === 1)
    if (community?.area) communityArea.value = community.area.toFixed(2)
  } catch (e: any) { errors.push('网格树加载失败: ' + (e?.message || e)) }
  try {
    const evtResult = await getEvents()
    events.value = evtResult.items || []
  } catch (e: any) {
    errors.push('事件列表加载失败: ' + (e?.message || e))
  }

  if (errors.length) {
    loadError.value = errors.join('；')
  }

  // Init map (only if tree data available and gisMap element exists)
  if (tree && tree.length > 0) {
    try {
      ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
      const AMap = await AMapLoader.load({ key: '5e00e01d2d2b6ca9e1eed533a15572e4', version: '2.0', plugins: ['AMap.Polygon', 'AMap.Marker'] })
      mapInstance = new AMap.Map('gisMap', { zoom: 14, center: [113.939521, 22.971231], mapStyle: 'amap://styles/normal' })
      const map = mapInstance

      // 第一步：绘制社区底图（level 1）
      const drawCommunityOutline = (nodes: any[]) => {
        for (const node of nodes) {
          if (node.gridLevel === 1 && node.roiJson) {
            try {
              const coords = JSON.parse(node.roiJson)
              if (Array.isArray(coords) && coords.length >= 3) {
                const polygon = new AMap.Polygon({
                  path: coords, fillColor: '#0284c7', fillOpacity: 0.08,
                  strokeColor: '#0284c7', strokeWeight: 2, strokeStyle: 'solid', zIndex: 1, map
                })
                polygon.setOptions({ bubble: true })
              }
            } catch (e) {}
          }
          if (node.children) drawCommunityOutline(node.children)
        }
      }
      drawCommunityOutline(tree)

      // 第二步：绘制大网格（level 2）
      const drawLargeGrids = (nodes: any[]) => {
        for (const grid of nodes) {
          if (grid.gridLevel === 2 && grid.roiJson) {
            try {
              const coords = JSON.parse(grid.roiJson)
              if (Array.isArray(coords) && coords.length >= 3) {
                const baseFillColor = '#f59e0b'
                const myId = ++hoverId
                const polygon = new AMap.Polygon({
                  path: coords, fillColor: baseFillColor, fillOpacity: 0.35,
                  strokeColor: '#ffffff', strokeWeight: 2, zIndex: 5, bubble: false, map
                })
                polygon.on('mouseover', (e: any) => {
                  polygon.setOptions({ fillOpacity: 0.6, zIndex: 20 })
                  const px = map.lngLatToContainer(e.lnglat)
                  hoverInfo.visible = true; hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                  hoverInfo.name = `${grid.gridName} · ${grid.area} km²`
                  hoverInfo.id = myId
                })
                polygon.on('mousemove', (e: any) => {
                  const px = map.lngLatToContainer(e.lnglat)
                  hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                })
                polygon.on('mouseout', () => {
                  if (hoverInfo.id !== myId) return
                  polygon.setOptions({ fillOpacity: 0.35, zIndex: 5 })
                  hoverInfo.visible = false
                })
                polygon.on('click', () => { selectedGrid.value = grid })
              }
            } catch (e) {}
          }
          if (grid.children) drawLargeGrids(grid.children)
        }
      }
      drawLargeGrids(tree)

      // 第三步：绘制小网格（level 3）
      const drawSmallGrids = (nodes: any[]) => {
        for (const grid of nodes) {
          if (grid.gridLevel === 3 && grid.roiJson) {
            try {
              const coords = JSON.parse(grid.roiJson)
              if (Array.isArray(coords) && coords.length >= 3) {
                const baseFillColor = '#10b981'
                const myId = ++hoverId
                const polygon = new AMap.Polygon({
                  path: coords, fillColor: baseFillColor, fillOpacity: 0.30,
                  strokeColor: '#ffffff', strokeWeight: 1, zIndex: 5, bubble: false, map
                })
                polygon.on('mouseover', (e: any) => {
                  polygon.setOptions({ fillOpacity: 0.55, zIndex: 20 })
                  const px = map.lngLatToContainer(e.lnglat)
                  hoverInfo.visible = true; hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                  hoverInfo.name = `${grid.gridName} · ${grid.area} km²`
                  hoverInfo.id = myId
                })
                polygon.on('mousemove', (e: any) => {
                  const px = map.lngLatToContainer(e.lnglat)
                  hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                })
                polygon.on('mouseout', () => {
                  if (hoverInfo.id !== myId) return
                  polygon.setOptions({ fillOpacity: 0.30, zIndex: 5 })
                  hoverInfo.visible = false
                })
                polygon.on('click', () => { selectedGrid.value = grid })
              }
            } catch (e) {}
          }
          if (grid.children) drawSmallGrids(grid.children)
        }
      }
      drawSmallGrids(tree)

      // === 事件标记（只展示未归档的活跃事件，避免历史测试/已归档数据污染态势地图） ===
      for (const evt of events.value.filter(e => !e.archived)) {
        if (!evt.longitude || !evt.latitude) continue
        const color = evt.urgencyLevel === 'RED' ? '#FF4D4F' : evt.urgencyLevel === 'YELLOW' ? '#FAAD14' : '#52C41A'
        const marker = new AMap.Marker({
          position: [evt.longitude, evt.latitude],
          zIndex: 10,
          content: `<div style="width:12px;height:12px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 0 6px ${color};transition:all 0.2s;"></div>`,
          offset: new AMap.Pixel(-6, -6),
          map,
          extData: { eventName: evt.title || '事件' }
        })
        marker.on('mouseover', (e: any) => {
          const px = map.lngLatToContainer(e.lnglat)
          hoverInfo.visible = true
          hoverInfo.x = px.getX()
          hoverInfo.y = px.getY()
          hoverInfo.name = (marker.getExtData() as any).eventName
          hoverInfo.id = ++hoverId
          marker.setContent(`<div style="width:16px;height:16px;border-radius:50%;background:${color};border:3px solid #fff;box-shadow:0 0 12px ${color};"></div>`)
          marker.setOffset(new AMap.Pixel(-8, -8))
        })
        marker.on('mousemove', (e: any) => {
          const px = map.lngLatToContainer(e.lnglat)
          hoverInfo.x = px.getX()
          hoverInfo.y = px.getY()
        })
        marker.on('mouseout', () => {
          if (hoverInfo.id !== hoverId) return
          hoverInfo.visible = false
          marker.setContent(`<div style="width:12px;height:12px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 0 6px ${color};"></div>`)
          marker.setOffset(new AMap.Pixel(-6, -6))
        })
      }
    } catch (e: any) {
      if (!errors.length || !errors.some(msg => msg.includes('地图'))) {
        errors.push('地图初始化失败: ' + (e?.message || e))
      }
      loadError.value = errors.join('；')
    }
  }

  // Chart (only if stats available and chart element exists)
  const chartEl = document.getElementById('chartPopulation')
  if (stats && stats.populationRanking && chartEl) {
    try {
      const ranking = stats.populationRanking || []
      chartInstance = echarts.init(chartEl)
      chartInstance.setOption({
        grid: { left: 70, right: 16, top: 8, bottom: 16 },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: ranking.map((r: any) => r.gridName).reverse(), axisLabel: { fontSize: 11 } },
        series: [{ type: 'bar', data: ranking.map((r: any) => r.populationCount).reverse(), itemStyle: { color: '#1890FF', borderRadius: [0, 4, 4, 0] } }]
      })
    } catch (e: any) {
      errors.push('图表初始化失败: ' + (e?.message || e))
      loadError.value = errors.join('；')
    }
  }
})

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>
