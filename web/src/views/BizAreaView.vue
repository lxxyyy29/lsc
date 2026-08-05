<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">辖区管理</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">社区辖区划分、商户/摊贩/违禁区域管理</p>

    <!-- Tab 切换 -->
    <div style="display:flex;gap:8px;margin-bottom:16px;">
      <button v-for="t in tabs" :key="t.key"
        :style="activeTab === t.key ? 'padding:6px 16px;border:none;border-radius:6px;background:#0284c7;color:#fff;font-size:13px;cursor:pointer;' : 'padding:6px 16px;border:1px solid #e5e7eb;border-radius:6px;background:#fff;color:#374151;font-size:13px;cursor:pointer;'"
        @click="activeTab = t.key; fetchData()">
        {{ t.label }}
      </button>
    </div>

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
        <!-- 辖区 -->
        <table v-if="activeTab === 'areas'" class="table">
          <thead><tr><th>辖区名称</th><th>编码</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.areaName || item.name || '-' }}</td>
              <td>{{ item.areaCode || item.code || '-' }}</td>
              <td><span class="tag tag-green">{{ item.status === 'ACTIVE' ? '启用中' : '已停用' }}</span></td>
            </tr>
          </tbody>
        </table>
        <!-- 商户 -->
        <table v-else-if="activeTab === 'merchants'" class="table">
          <thead><tr><th>商户名称</th><th>负责人</th><th>电话</th><th>地址</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.merchantName || item.name || '-' }}</td>
              <td>{{ item.legalPersonName || '-' }}</td>
              <td>{{ item.legalPersonPhone || '-' }}</td>
              <td>{{ item.address || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <!-- 摊贩 -->
        <table v-else-if="activeTab === 'vendors'" class="table">
          <thead><tr><th>摊贩名称</th><th>负责人</th><th>电话</th><th>地址</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.vendorName || item.name || '-' }}</td>
              <td>{{ item.legalPersonName || '-' }}</td>
              <td>{{ item.legalPersonPhone || '-' }}</td>
              <td>{{ item.address || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <!-- 违禁区域 -->
        <table v-else class="table">
          <thead><tr><th>区域名称</th><th>类型</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.areaName || item.name || '-' }}</td>
              <td><span class="tag tag-red">{{ item.violationType || '-' }}</span></td>
              <td><span class="tag tag-green">{{ item.status === 'ACTIVE' ? '启用中' : '已停用' }}</span></td>
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

const tabs = [
  { key: 'areas', label: '辖区' },
  { key: 'merchants', label: '商户' },
  { key: 'vendors', label: '摊贩' },
  { key: 'violations', label: '违禁区域' }
]
const activeTab = ref('areas')
const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')

const urls: Record<string, string> = {
  areas: '/areas',
  merchants: '/merchants',
  vendors: '/mobile-vendors',
  violations: '/violation-areas'
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await http.get(urls[activeTab.value]) || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
