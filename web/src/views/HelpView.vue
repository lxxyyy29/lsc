<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">帮助中心</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">操作指南与常见问题解答</p>

    <!-- 搜索框 -->
    <div class="card" style="margin-bottom:16px;">
      <div class="filter-bar" style="margin-bottom:0;">
        <input v-model="searchKey" class="filter-input" style="width:320px;" placeholder="🔍 搜索问题关键词..." />
      </div>
    </div>

    <!-- 功能模块导航 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:20px;">
      <div v-for="cat in categories" :key="cat.key" class="card" style="cursor:pointer;text-align:center;padding:16px;" :style="activeCategory === cat.key ? 'border:2px solid #0284c7;' : ''" @click="activeCategory = cat.key">
        <i :class="cat.icon" style="font-size:24px;color:#0284c7;margin-bottom:8px;"></i>
        <p style="font-size:13px;font-weight:500;">{{ cat.name }}</p>
      </div>
    </div>

    <!-- FAQ 列表 -->
    <div class="card">
      <div v-if="filteredFaqs.length === 0" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-search" style="font-size:24px;margin-bottom:8px;"></i>
        <p>没有找到相关问题</p>
      </div>
      <div v-else style="display:flex;flex-direction:column;gap:8px;">
        <div v-for="(faq, idx) in filteredFaqs" :key="idx" style="border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;">
          <div style="padding:12px 16px;cursor:pointer;display:flex;justify-content:space-between;align-items:center;" @click="faq.open = !faq.open">
            <div style="display:flex;align-items:center;gap:10px;">
              <span style="background:#f0f7ff;color:#0284c7;font-size:11px;padding:2px 8px;border-radius:10px;">{{ getCategoryName(faq.category) }}</span>
              <span style="font-size:14px;font-weight:500;">{{ faq.q }}</span>
            </div>
            <i class="fas fa-chevron-down" style="font-size:12px;color:#9ca3af;transition:transform 0.2s;" :style="{ transform: faq.open ? 'rotate(180deg)' : '' }"></i>
          </div>
          <div v-if="faq.open" style="padding:0 16px 16px;font-size:13px;color:#4b5563;line-height:1.8;border-top:1px solid #f3f4f6;">
            <div style="padding-top:12px;" v-html="faq.a"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 流程指南 -->
    <div class="card" style="margin-top:20px;">
      <h3 style="font-size:15px;font-weight:600;margin-bottom:16px;">
        <i class="fas fa-route" style="color:#0284c7;margin-right:6px;"></i>事件闭环处置流程
      </h3>
      <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
        <div v-for="(step, idx) in flowSteps" :key="idx" style="display:flex;align-items:center;gap:8px;">
          <div style="display:flex;align-items:center;gap:6px;padding:8px 14px;background:#f0f7ff;border-radius:8px;">
            <span style="width:20px;height:20px;background:#0284c7;color:#fff;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:11px;">{{ idx + 1 }}</span>
            <span style="font-size:13px;font-weight:500;color:#0284c7;">{{ step }}</span>
          </div>
          <i v-if="idx < flowSteps.length - 1" class="fas fa-arrow-right" style="font-size:12px;color:#d1d5db;"></i>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const searchKey = ref('')
const activeCategory = ref('all')

const categories = [
  { key: 'all', name: '全部', icon: 'fas fa-th-large' },
  { key: 'event', name: '事件管理', icon: 'fas fa-tasks' },
  { key: 'workorder', name: '工单处置', icon: 'fas fa-clipboard-list' },
  { key: 'gis', name: 'GIS 地图', icon: 'fas fa-map-marked-alt' },
  { key: 'data', name: '基础数据', icon: 'fas fa-database' },
  { key: 'system', name: '系统功能', icon: 'fas fa-cog' },
]

const flowSteps = ['发现上报', '智能派单', '现场处置', '复核核查', '督办预警', '归档']

const faqs = ref([
  {
    category: 'event',
    q: '如何创建事件？',
    a: '进入「事件中心」→「创建事件」，填写事件标题、类型、事发地点、描述等信息后提交。事件创建后状态为「待审核」。',
    open: false
  },
  {
    category: 'event',
    q: '事件的状态有哪些？',
    a: '事件共有以下状态：<br>• <strong>待审核</strong>（PENDING_AUDIT）：新上报事件等待审核<br>• <strong>审核中</strong>（IN_AUDIT）：正在审核流程中<br>• <strong>待派单</strong>（WAITING_DISPATCH）：审核通过等待派发<br>• <strong>组长审核</strong>（WAITING_LEADER_REVIEW）：事件已推送网格组长，等待组长派单给下属网格员<br>• <strong>已派单</strong>（DISPATCHED_TO_WORK_ORDER）：已生成工单<br>• <strong>已关闭</strong>（CLOSED）：事件已处置完毕',
    open: false
  },
  {
    category: 'event',
    q: '如何给事件派单？',
    a: '在事件详情页点击「派单」，选择网格员（系统会根据角色自动推荐），填写备注后确认。派单后自动生成工单。',
    open: false
  },
  {
    category: 'workorder',
    q: '工单的处置流程是什么？',
    a: '工单状态流转：<br>1. <strong>待接单</strong>（WAITING_ACCEPT）→ 网格员接单<br>2. <strong>处理中</strong>（PROCESSING）→ 现场处置<br>3. <strong>待核实</strong>（WAITING_VERIFY）→ 提交处置结果<br>4. <strong>待确认关闭</strong>（WAITING_CLOSE_CONFIRM）→ 管理员确认<br>5. <strong>已完成</strong>（COMPLETED）→ 归档',
    open: false
  },
  {
    category: 'workorder',
    q: '什么是紧急程度自动升级？',
    a: '系统每 30 分钟自动检查：<br>• GREEN（绿色）超过 24 小时 → 自动升级为 YELLOW（黄色）<br>• YELLOW（黄色）超过 48 小时 → 自动升级为 RED（红色）<br>升级后会自动生成督办记录并发送通知。',
    open: false
  },
  {
    category: 'gis',
    q: 'GIS 地图如何查看热力图和巡查轨迹？',
    a: '进入「GIS 网格可视化」页面，顶部有「事件热力图」和「巡查轨迹」两个图层开关。<br>• 热力图：颜色越深表示事件越密集<br>• 轨迹：不同颜色线代表不同网格员，标签显示姓名',
    open: false
  },
  {
    category: 'gis',
    q: '地图不显示怎么办？',
    a: '地图依赖高德地图 API，请确认：<br>1. 网络连接正常<br>2. 浏览器允许加载外部脚本<br>3. 如仍不显示，尝试刷新页面',
    open: false
  },
  {
    category: 'data',
    q: '如何批量导入数据？',
    a: '在「实有人口库」「房屋库」「场所库」页面点击「导入」按钮：<br>1. 先下载导入模板<br>2. 按模板格式填写数据<br>3. 上传文件预览校验<br>4. 确认无误后执行导入<br>导入失败的行会生成错误报告供下载。',
    open: false
  },
  {
    category: 'data',
    q: '如何导出数据？',
    a: '在「数据报表」或各基础数据页面，点击「导出」按钮即可下载 Excel 文件。导出文件包含当前筛选条件下的所有数据。',
    open: false
  },
  {
    category: 'system',
    q: '审计日志记录了什么？',
    a: '审计日志自动记录所有数据变更操作，包括：<br>• 操作类型（新增/修改/删除/审批/回滚）<br>• 变更的表和记录 ID<br>• 变更前后的字段值对比<br>• 操作人和时间<br>支持查看字段级变更详情和回滚到历史版本。',
    open: false
  },
  {
    category: 'system',
    q: '通知铃铛的角标是什么意思？',
    a: '右上角铃铛图标显示未读消息数量，包括：<br>• 工单超期提醒<br>• 待处置通知<br>• 系统公告<br>系统每分钟自动刷新未读数量。',
    open: false
  },
])

const filteredFaqs = computed(() => {
  let list = faqs.value
  if (activeCategory.value !== 'all') {
    list = list.filter(f => f.category === activeCategory.value)
  }
  if (searchKey.value) {
    const key = searchKey.value.toLowerCase()
    list = list.filter(f => f.q.toLowerCase().includes(key) || f.a.toLowerCase().includes(key))
  }
  return list
})

function getCategoryName(key: string) {
  return categories.find(c => c.key === key)?.name || key
}
</script>
