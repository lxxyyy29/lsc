<template>
  <div class="page">
    <div class="header">
      <h2>📋 政策查询</h2>
      <p>惠民政策，一查便知</p>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else>
      <div v-for="p in policies" :key="p.id" class="card" @click="showDetail(p)">
        <div class="card-title">{{ p.title }}</div>
        <div class="card-meta">
          <span class="tag tag-blue">{{ policyTypeLabel(p.policyType) }}</span>
          <span class="time">{{ p.publishDate || p.createdAt }}</span>
        </div>
        <p class="desc">{{ p.description }}</p>
      </div>
      <p v-if="!policies.length" class="empty">暂无政策</p>
    </div>

    <div v-if="selected" class="modal-overlay" @click.self="selected = null">
      <div class="modal-box">
        <h3>{{ selected.title }}</h3>
        <div class="detail-tags">
          <span class="tag tag-blue">{{ policyTypeLabel(selected.policyType) }}</span>
          <span v-if="selected.policyCode" class="code">编号：{{ selected.policyCode }}</span>
        </div>
        <div v-if="selected.description" class="section">
          <div class="section-label">政策简介</div>
          <p>{{ selected.description }}</p>
        </div>
        <div v-if="selected.eligibility" class="section">
          <div class="section-label">申请条件</div>
          <p>{{ selected.eligibility }}</p>
        </div>
        <div v-if="selected.tags" class="section">
          <div class="section-label">标签</div>
          <p>{{ selected.tags }}</p>
        </div>
        <div class="modal-actions">
          <button @click="selected = null" class="btn-primary">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getResidentPolicies } from '../api'

const loading = ref(false)
const policies = ref<any[]>([])
const selected = ref<any>(null)

function policyTypeLabel(t: string) {
  const map: any = {
    LOW_INCOME: '低保救助', ELDERLY: '养老服务', RESCUE: '临时救助',
    MEDICAL: '医疗救助', BENEFIT: '福利政策', EDUCATION: '教育资助',
    EMPLOYMENT: '就业扶持', HOUSING: '住房保障', OTHER: '其他'
  }
  return map[t] || t || '政策'
}

async function loadData() {
  loading.value = true
  try {
    policies.value = await getResidentPolicies() || []
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

function showDetail(p: any) {
  selected.value = p
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.header {
  background: linear-gradient(135deg, #13c2c2 0%, #08979c 100%);
  border-radius: 12px; padding: 20px; color: #fff; margin-bottom: 16px;
}
.header h2 { font-size: 20px; margin-bottom: 4px; }
.header p { font-size: 13px; opacity: 0.8; }
.card {
  background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04); cursor: pointer;
}
.card-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.card-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.tag-blue { background: #e6fffb; color: #13c2c2; }
.time { font-size: 11px; color: #9ca3af; }
.desc { font-size: 13px; color: #6b7280; }
.loading { text-align: center; padding: 40px; color: #9ca3af; }
.empty { text-align: center; padding: 40px; color: #9ca3af; font-size: 13px; }
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100;
}
.modal-box {
  background: #fff; border-radius: 12px; padding: 20px; width: 90%; max-width: 400px; max-height: 80vh; overflow-y: auto;
}
.modal-box h3 { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.detail-tags { display: flex; gap: 8px; align-items: center; margin-bottom: 16px; }
.code { font-size: 11px; color: #9ca3af; }
.section { margin-bottom: 14px; }
.section-label { font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 4px; }
.section p { font-size: 13px; color: #6b7280; line-height: 1.6; }
.modal-actions { display: flex; justify-content: flex-end; margin-top: 16px; }
.btn-primary {
  padding: 10px 20px; background: #13c2c2; color: #fff; border: none;
  border-radius: 6px; font-size: 14px; cursor: pointer;
}
</style>
