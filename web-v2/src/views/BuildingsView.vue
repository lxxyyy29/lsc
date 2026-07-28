<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">房屋/出租屋库</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">楼栋、分户信息、房东租客、消防隐患</p>
    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="fetchData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <template v-else>
        <table class="table">
          <thead><tr><th>楼栋编号</th><th>地址</th><th>房东</th><th>消防风险</th><th>群租房</th></tr></thead>
          <tbody>
            <tr v-for="b in list" :key="b.id">
              <td>{{ b.buildingNo }}</td>
              <td>{{ b.address || '-' }}</td>
              <td>{{ b.landlordName || '-' }}</td>
              <td><span :class="['tag', b.fireRiskLevel === 'HIGH' ? 'tag-red' : b.fireRiskLevel === 'MEDIUM' ? 'tag-orange' : 'tag-green']">{{ b.fireRiskLevel || '-' }}</span></td>
              <td>{{ b.isGroupRental ? '是' : '否' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!list.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'
const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await http.get('/community/buildings') || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
