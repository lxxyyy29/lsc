<template>
  <div>
    <div class="page-header" style="display:flex;align-items:center;justify-content:space-between;">
      <div>
        <h1 class="page-title">注册审批</h1>
        <p class="page-desc">网格员注册申请审批管理</p>
      </div>
    </div>

    <!-- 标签页 -->
    <div style="display:flex;gap:8px;margin-bottom:20px;border-bottom:2px solid #e5e7eb;padding-bottom:12px;">
      <button @click="activeTab = 'pending'" :class="['filter-btn', activeTab === 'pending' ? 'active' : '']">
        待审批 ({{ pendingList.length }})
      </button>
      <button @click="activeTab = 'all'" :class="['filter-btn', activeTab === 'all' ? 'active' : '']">
        全部记录
      </button>
    </div>

    <!-- 待审批列表 -->
    <div v-if="activeTab === 'pending'" class="card">
      <div v-if="loading" class="empty-state">
        <i class="fas fa-spinner fa-spin"></i>
        <p>加载中...</p>
      </div>
      <template v-else>
        <table class="table" v-if="pendingList.length">
          <thead><tr><th>账号</th><th>姓名</th><th>电话</th><th>申请时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in pendingList" :key="item.id">
              <td>{{ item.account }}</td>
              <td>{{ item.real_name }}</td>
              <td>{{ item.phone || '-' }}</td>
              <td style="font-size:12px;">{{ item.created_at }}</td>
              <td>
                <div style="display:flex;gap:6px;">
                  <button @click="handleApprove(item)" class="btn btn-success" style="padding:4px 10px;font-size:12px;">通过</button>
                  <button @click="handleReject(item)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;">驳回</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">
          <i class="fas fa-clipboard-check"></i>
          <p>暂无待审批申请</p>
        </div>
      </template>
    </div>

    <!-- 全部记录 -->
    <div v-else class="card">
      <div v-if="loading" class="empty-state">
        <i class="fas fa-spinner fa-spin"></i>
        <p>加载中...</p>
      </div>
      <template v-else>
        <table class="table" v-if="allList.length">
          <thead><tr><th>账号</th><th>姓名</th><th>电话</th><th>状态</th><th>申请时间</th><th>审批时间</th></tr></thead>
          <tbody>
            <tr v-for="item in allList" :key="item.id">
              <td>{{ item.account }}</td>
              <td>{{ item.real_name }}</td>
              <td>{{ item.phone || '-' }}</td>
              <td>
                <span :class="['tag', item.status === 'APPROVED' ? 'tag-green' : item.status === 'REJECTED' ? 'tag-red' : 'tag-orange']">
                  {{ statusLabel(item.status) }}
                </span>
              </td>
              <td style="font-size:12px;">{{ item.created_at }}</td>
              <td style="font-size:12px;">{{ item.reviewed_at || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state">
          <i class="fas fa-inbox"></i>
          <p>暂无记录</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPendingRegistrations, getAllRegistrations, approveRegistration, rejectRegistration } from '../api'

const activeTab = ref('pending')
const pendingList = ref<any[]>([])
const allList = ref<any[]>([])
const loading = ref(true)

function statusLabel(s: string) {
  return { PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回' }[s] || s
}

async function fetchData() {
  loading.value = true
  try {
    const [pending, all] = await Promise.all([
      getPendingRegistrations().catch(() => []),
      getAllRegistrations().catch(() => [])
    ])
    pendingList.value = Array.isArray(pending) ? pending : []
    allList.value = Array.isArray(all) ? all : []
  } catch(e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleApprove(item: any) {
  const remark = prompt('审批备注（可选）：', '审批通过')
  if (remark === null) return
  try {
    await approveRegistration(item.id, remark || '审批通过')
    alert('审批通过！网格员已创建并同步到组织人员。')
    fetchData()
  } catch(e: any) {
    alert(e?.message || '操作失败')
  }
}

async function handleReject(item: any) {
  const remark = prompt('驳回原因：')
  if (!remark) return
  try {
    await rejectRegistration(item.id, remark)
    alert('已驳回')
    fetchData()
  } catch(e: any) {
    alert(e?.message || '操作失败')
  }
}

onMounted(fetchData)
</script>
