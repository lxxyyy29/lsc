<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">电子化台账</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">综治平安、安全生产、爱卫、矛盾调解台账一键导出</p>

    <!-- 台账类型选择 -->
    <div style="display:grid;grid-template-columns:repeat(6,1fr);gap:12px;margin-bottom:20px;">
      <div v-for="t in templates" :key="t.id" @click="selectTemplate(t)"
           :class="['card', 'cursor-pointer', selectedTemplate?.id === t.id ? 'card-border-blue' : '']"
           style="text-align:center;padding:16px 8px;cursor:pointer;">
        <i :class="['fas', typeIcon(t.templateType)]" style="font-size:24px;color:#1890ff;margin-bottom:8px;"></i>
        <p style="font-size:13px;font-weight:600;">{{ t.templateName }}</p>
        <p style="font-size:11px;color:#9ca3af;margin-top:4px;">{{ t.description }}</p>
      </div>
    </div>

    <!-- 数据预览 -->
    <div class="card" v-if="selectedTemplate">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">{{ selectedTemplate.templateName }} - 数据预览</h3>
        <div style="display:flex;gap:8px;">
          <button @click="loadData" style="padding:6px 14px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">
            <i class="fas fa-sync"></i> 刷新
          </button>
          <button @click="exportData" style="padding:6px 14px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
            <i class="fas fa-download"></i> 导出Excel
          </button>
        </div>
      </div>

      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;">加载中...</p>
      </div>
      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th v-for="col in columns" :key="col.field" style="min-width:80px;">{{ col.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in data" :key="idx">
              <td v-for="col in columns" :key="col.field" style="font-size:12px;">{{ row[col.field] || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!data.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </template>
    </div>

    <p v-else style="text-align:center;padding:40px;color:#9ca3af;">请选择上方台账类型查看数据</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'

const templates = ref<any[]>([])
const selectedTemplate = ref<any>(null)
const data = ref<any[]>([])
const columns = ref<any[]>([])
const loading = ref(false)

function typeIcon(type: string) {
  const map: Record<string, string> = {
    EVENT: 'fa-clipboard-list', POPULATION: 'fa-users', BUILDING: 'fa-building',
    MERCHANT: 'fa-store', PATROL: 'fa-walking', SAFETY: 'fa-shield-alt'
  }
  return map[type] || 'fa-file'
}

async function loadTemplates() {
  try {
    templates.value = await http.get('/ledger/templates') || []
  } catch (e) {}
}

function selectTemplate(t: any) {
  selectedTemplate.value = t
  try {
    columns.value = JSON.parse(t.columnsJson || '[]')
  } catch {
    columns.value = []
  }
  loadData()
}

async function loadData() {
  if (!selectedTemplate.value) return
  loading.value = true
  try {
    data.value = await http.get(`/ledger/data/${selectedTemplate.value.templateType}`) || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function exportData() {
  if (!selectedTemplate.value) return
  window.open(`/api/ledger/export/${selectedTemplate.value.templateType}`, '_blank')
}

onMounted(loadTemplates)
</script>
