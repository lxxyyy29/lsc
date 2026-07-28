<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">考核研判</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">网格员效能考核、数据分析、处置时效</p>

    <!-- 加载状态 -->
    <div v-if="loading" style="text-align:center;padding:60px;color:#9ca3af;">
      <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
      <p style="margin-top:12px;">加载中...</p>
    </div>

    <template v-else>
      <!-- 事件总览 -->
      <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:12px;margin-bottom:20px;">
        <div class="card card-border-blue">
          <p class="stat-label">事件总数</p>
          <p class="stat-value">{{ overview.events?.total || 0 }}</p>
        </div>
        <div class="card card-border-orange">
          <p class="stat-label">待派单</p>
          <p class="stat-value">{{ overview.events?.waitingDispatch || 0 }}</p>
        </div>
        <div class="card card-border-blue">
          <p class="stat-label">已派单</p>
          <p class="stat-value">{{ overview.events?.dispatched || 0 }}</p>
        </div>
        <div class="card card-border-green">
          <p class="stat-label">已关闭</p>
          <p class="stat-value">{{ overview.events?.closed || 0 }}</p>
        </div>
        <div class="card card-border-red">
          <p class="stat-label">待审核</p>
          <p class="stat-value">{{ overview.events?.pendingAudit || 0 }}</p>
        </div>
      </div>

      <!-- 工单总览 -->
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:20px;">
        <div class="card card-border-blue">
          <p class="stat-label">工单总数</p>
          <p class="stat-value">{{ overview.orders?.total || 0 }}</p>
        </div>
        <div class="card card-border-orange">
          <p class="stat-label">处理中</p>
          <p class="stat-value">{{ overview.orders?.processing || 0 }}</p>
        </div>
        <div class="card card-border-green">
          <p class="stat-label">已完成</p>
          <p class="stat-value">{{ overview.orders?.completed || 0 }}</p>
        </div>
      </div>

      <!-- 处置时效 -->
      <div class="card" style="margin-bottom:20px;">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">处置时效</h3>
        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;">
          <div style="text-align:center;">
            <p style="font-size:24px;font-weight:800;color:#1890ff;">{{ responseTime.avgResolutionHours || 0 }}</p>
            <p style="font-size:12px;color:#6b7280;">平均处置时长（小时）</p>
          </div>
          <div style="text-align:center;">
            <p style="font-size:24px;font-weight:800;color:#ff4d4f;">{{ responseTime.overdueDispatch || 0 }}</p>
            <p style="font-size:12px;color:#6b7280;">超期未派单（>24h）</p>
          </div>
          <div style="text-align:center;">
            <p style="font-size:24px;font-weight:800;color:#ff4d4f;">{{ responseTime.overdueClose || 0 }}</p>
            <p style="font-size:12px;color:#6b7280;">超期未关闭（>48h）</p>
          </div>
        </div>
      </div>

      <!-- 群众评价统计 -->
      <div class="card" style="margin-bottom:20px;">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">群众评价</h3>
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;">
          <div style="text-align:center;">
            <p style="font-size:24px;font-weight:800;color:#faad14;">{{ ratingStats.avgRating || '0' }}</p>
            <p style="font-size:12px;color:#6b7280;">平均评分</p>
          </div>
          <div style="text-align:center;">
            <p style="font-size:24px;font-weight:800;color:#52c41a;">{{ ratingStats.totalRated || 0 }}</p>
            <p style="font-size:12px;color:#6b7280;">已评价数</p>
          </div>
          <div style="text-align:center;">
            <p style="font-size:24px;font-weight:800;color:#1890ff;">{{ ratingStats.satisfactionRate || '0%' }}</p>
            <p style="font-size:12px;color:#6b7280;">满意率（4-5星）</p>
          </div>
          <div style="text-align:center;">
            <div style="display:flex;justify-content:center;gap:2px;margin-bottom:4px;">
              <span v-for="n in 5" :key="n" :style="{fontSize:'20px',color: n <= Math.round(ratingStats.avgRating || 0) ? '#faad14' : '#d1d5db'}">★</span>
            </div>
            <p style="font-size:12px;color:#6b7280;">综合评分</p>
          </div>
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px;">
        <!-- 紧急程度分布 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">紧急程度分布</h3>
          <div v-for="item in overview.urgencyDistribution" :key="item.level" style="margin-bottom:8px;">
            <div style="display:flex;justify-content:space-between;font-size:12px;margin-bottom:4px;">
              <span>{{ item.level === 'RED' ? '紧急' : item.level === 'YELLOW' ? '重点' : item.level === 'GREEN' ? '一般' : item.level }}</span>
              <span>{{ item.count }}</span>
            </div>
            <div style="height:6px;background:#f3f4f6;border-radius:3px;overflow:hidden;">
              <div :style="{width: getPct(item.count, overview.events?.total) + '%', height:'100%', background: item.level === 'RED' ? '#ff4d4f' : item.level === 'YELLOW' ? '#faad14' : '#52c41a', borderRadius:'3px'}"></div>
            </div>
          </div>
          <p v-if="!overview.urgencyDistribution?.length" style="font-size:12px;color:#9ca3af;text-align:center;">暂无数据</p>
        </div>

        <!-- 事件类型分布 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">事件类型TOP10</h3>
          <div v-for="(item, idx) in overview.eventTypeDistribution" :key="idx" style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
            <span style="font-size:11px;color:#9ca3af;width:20px;">{{ idx + 1 }}</span>
            <span style="flex:1;font-size:12px;">{{ item.type }}</span>
            <span style="font-size:12px;font-weight:600;color:#1890ff;">{{ item.count }}</span>
          </div>
          <p v-if="!overview.eventTypeDistribution?.length" style="font-size:12px;color:#9ca3af;text-align:center;">暂无数据</p>
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px;">
        <!-- 网格事件排名 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">网格事件排名</h3>
          <div v-for="(item, idx) in overview.gridRanking" :key="idx" style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
            <span :style="{width:'20px',fontSize:'11px',fontWeight:'700',color: idx < 3 ? '#ff4d4f' : '#9ca3af'}">{{ idx + 1 }}</span>
            <span style="flex:1;font-size:12px;">{{ item.gridName || '未分配' }}</span>
            <span style="font-size:12px;font-weight:600;color:#1890ff;">{{ item.eventCount }}</span>
          </div>
          <p v-if="!overview.gridRanking?.length" style="font-size:12px;color:#9ca3af;text-align:center;">暂无数据</p>
        </div>

        <!-- 月度统计 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">月度事件统计</h3>
          <div v-for="(item, idx) in overview.monthlyStats" :key="idx" style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
            <span style="font-size:12px;color:#6b7280;width:60px;">{{ item.month }}</span>
            <div style="flex:1;height:16px;background:#e6f4ff;border-radius:3px;overflow:hidden;">
              <div :style="{width: getMonthPct(item.count) + '%', height:'100%', background:'#1890ff', borderRadius:'3px'}"></div>
            </div>
            <span style="font-size:12px;font-weight:600;width:30px;text-align:right;">{{ item.count }}</span>
          </div>
          <p v-if="!overview.monthlyStats?.length" style="font-size:12px;color:#9ca3af;text-align:center;">暂无数据</p>
        </div>
      </div>

      <!-- 网格员效能考核表 -->
      <div class="card">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">网格员效能考核</h3>
        <table class="table">
          <thead>
            <tr>
              <th>排名</th>
              <th>姓名</th>
              <th>账号</th>
              <th>总工单</th>
              <th>已完成</th>
              <th>处理中</th>
              <th>办结率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(w, idx) in workers" :key="w.userId">
              <td><span :style="{fontWeight:'700',color: idx < 3 ? '#ff4d4f' : '#9ca3af'}">{{ idx + 1 }}</span></td>
              <td>{{ w.realName || '-' }}</td>
              <td style="font-size:12px;color:#6b7280;">{{ w.username }}</td>
              <td>{{ w.totalOrders || 0 }}</td>
              <td>{{ w.completedOrders || 0 }}</td>
              <td>{{ w.processingOrders || 0 }}</td>
              <td>
                <span :style="{fontWeight:'600',color: (w.completionRate || 0) >= 80 ? '#52c41a' : (w.completionRate || 0) >= 50 ? '#faad14' : '#ff4d4f'}">
                  {{ w.completionRate || 0 }}%
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!workers.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'

const loading = ref(true)
const overview = ref<any>({})
const responseTime = ref<any>({})
const workers = ref<any[]>([])
const ratingStats = ref<any>({})

function getPct(val: number, total: number) {
  if (!total) return 0
  return Math.round((val / total) * 100)
}

function getMonthPct(val: number) {
  const max = Math.max(...((overview.value.monthlyStats || []).map((m: any) => m.count)), 1)
  return Math.round((val / max) * 100)
}

onMounted(async () => {
  try {
    const [overviewRes, timeRes, workersRes, ratingRes] = await Promise.all([
      http.get('/assessment/overview'),
      http.get('/assessment/response-time'),
      http.get('/assessment/worker-performance'),
      http.get('/assessment/rating-stats').catch(() => ({}))
    ])
    overview.value = overviewRes || {}
    responseTime.value = timeRes || {}
    workers.value = workersRes || []
    ratingStats.value = ratingRes || {}
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>
