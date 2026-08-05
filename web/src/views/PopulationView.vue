<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">实有人口库</h2>
        <p style="font-size:13px;color:#6b7280;">常住人口、流动人口、出租屋台账</p>
      </div>
      <button @click="showImport = true" style="padding:6px 14px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-file-import"></i> 导入
      </button>
    </div>
    <div class="card">
      <!-- 加载中 -->
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <!-- 错误提示 -->
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="fetchData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <!-- 数据表格 -->
      <template v-else>
        <table class="table">
          <thead><tr><th>姓名</th><th>电话</th><th>户籍类型</th><th>地址</th><th>网格</th></tr></thead>
          <tbody>
            <tr v-for="p in list" :key="p.id">
              <td>{{ p.name }}</td>
              <td>{{ p.phone || '-' }}</td>
              <td><span class="tag tag-blue">{{ getHouseholdTypeName(p.householdType) }}</span></td>
              <td>{{ p.address || '-' }}</td>
              <td>{{ p.gridName || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!list.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </template>
    </div>

    <!-- 导入对话框 -->
    <ImportDialog v-model:visible="showImport" type="population" @success="fetchData" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'
import { getHouseholdTypeName } from '../utils/eventTypes'
import ImportDialog from '../components/ImportDialog.vue'

const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const showImport = ref(false)

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await http.get('/community/population') || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
