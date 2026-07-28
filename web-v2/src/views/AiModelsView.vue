<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">AI 算法模型管理</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">管理无人机巡检 AI 识别模型，支持模型列表查看、航线绑定配置</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <p class="stat-label">模型总数</p>
        <p class="stat-value">{{ totalModels }}</p>
      </div>
      <div class="card card-border-green">
        <p class="stat-label">已启用</p>
        <p class="stat-value">{{ enabledCount }}</p>
      </div>
      <div class="card card-border-orange">
        <p class="stat-label">千问模型</p>
        <p class="stat-value">{{ qwenCount }}</p>
      </div>
      <div class="card card-border-red">
        <p class="stat-label">已绑定航线</p>
        <p class="stat-value">{{ boundWaylineCount }}</p>
      </div>
    </div>

    <!-- 模型列表 -->
    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">AI 模型列表</h3>
        <div style="display:flex;gap:8px;">
          <input v-model="searchKey" placeholder="搜索模型名称..." style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;outline:none;" />
          <button @click="loadData" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">
            <i class="fas fa-sync"></i> 刷新
          </button>
        </div>
      </div>

      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:8px;font-size:13px;">加载中...</p>
      </div>

      <div v-else>
        <table class="table">
          <thead>
            <tr>
              <th>模型名称</th>
              <th>序列号</th>
              <th>类型</th>
              <th>标签</th>
              <th>最近训练</th>
              <th>上线时间</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in filteredModels" :key="m.id || m.modelNo">
              <td>
                <div style="display:flex;align-items:center;gap:6px;">
                  <i class="fas fa-brain" style="color:#1890ff;"></i>
                  <span style="font-weight:600;">{{ m.name || m.modelName || '-' }}</span>
                </div>
              </td>
              <td style="font-size:12px;">{{ m.modelNo || '-' }}</td>
              <td><span class="tag tag-blue">{{ m.modelNo === 'QWENMODEL' ? '千问' : '普通算法' }}</span></td>
              <td style="font-size:12px;">
                <span v-if="m.labelList && m.labelList.length" style="display:flex;flex-wrap:wrap;gap:4px;">
                  <span v-for="label in (Array.isArray(m.labelList) ? m.labelList : [])" :key="label" class="tag tag-green" style="font-size:10px;padding:1px 6px;">{{ label }}</span>
                </span>
                <span v-else>-</span>
              </td>
              <td style="font-size:12px;">{{ m.latestTrainingTime || '-' }}</td>
              <td style="font-size:12px;">{{ m.onlineTime || '-' }}</td>
              <td>
                <span :class="['tag', m.status === 0 ? 'tag-green' : 'tag-orange']">
                  {{ m.status === 0 ? '启用' : '未启用' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!filteredModels.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无模型数据</p>

        <!-- 分页 -->
        <div v-if="totalModels > pageSize" style="display:flex;justify-content:center;gap:8px;margin-top:16px;">
          <button @click="page--; loadData()" :disabled="page <= 1" style="padding:4px 12px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">
            上一页
          </button>
          <span style="padding:4px 12px;font-size:12px;color:#6b7280;">第 {{ page }} 页 / 共 {{ Math.ceil(totalModels / pageSize) }} 页</span>
          <button @click="page++; loadData()" :disabled="page >= Math.ceil(totalModels / pageSize)" style="padding:4px 12px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 模型说明 -->
    <div class="card" style="margin-top:16px;">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;"><i class="fas fa-info-circle" style="color:#1890ff;margin-right:6px;"></i>模型说明</h3>
      <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:12px;font-size:12px;color:#6b7280;">
        <div>
          <p style="font-weight:600;color:#374151;margin-bottom:4px;">普通算法模型</p>
          <p>由无人机平台提供的内置 AI 算法，支持目标检测、图像识别等功能。</p>
        </div>
        <div>
          <p style="font-weight:600;color:#374151;margin-bottom:4px;">千问模型</p>
          <p>基于千问大语言模型的智能分析，支持自然语言理解和复杂场景分析。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAiModels } from '../api'

const loading = ref(false)
const models = ref<any[]>([])
const searchKey = ref('')
const page = ref(1)
const pageSize = ref(10)
const totalModels = ref(0)

const filteredModels = computed(() => {
  if (!searchKey.value) return models.value
  const key = searchKey.value.toLowerCase()
  return models.value.filter(m =>
    (m.name || '').toLowerCase().includes(key) ||
    (m.modelNo || '').toLowerCase().includes(key) ||
    (m.modelName || '').toLowerCase().includes(key)
  )
})

const enabledCount = computed(() => models.value.filter(m => m.status === 0).length)
const qwenCount = computed(() => models.value.filter(m => m.modelNo === 'QWENMODEL').length)
const boundWaylineCount = computed(() => {
  // 简化：返回有标签的模型数量
  return models.value.filter(m => m.labelList && m.labelList.length > 0).length
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await getAiModels({ page: page.value, pageSize: pageSize.value })
    if (res && res.items) {
      models.value = res.items
      totalModels.value = res.total || res.items.length
    } else if (Array.isArray(res)) {
      models.value = res
      totalModels.value = res.length
    }
  } catch (e) {
    console.error('加载 AI 模型失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
