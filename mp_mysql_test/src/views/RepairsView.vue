<template>
  <div class="page">
    <div class="header">
      <h2>🔧 便民报修</h2>
      <p>提交报修申请，查看处理进度</p>
    </div>

    <!-- 提交报修按钮 -->
    <button @click="showForm = true" class="btn-submit">+ 提交报修</button>

    <!-- 我的报修列表 -->
    <div class="section-title">我的报修</div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else>
      <div v-for="r in repairs" :key="r.id" class="card">
        <div class="card-top">
          <span class="title">{{ r.title }}</span>
          <span :class="['status', statusClass(r.status)]">{{ statusLabel(r.status) }}</span>
        </div>
        <p class="desc">{{ r.description }}</p>
        <div class="card-bottom">
          <span class="type">{{ repairTypeLabel(r.repairType) }}</span>
          <span class="time">{{ r.createdAt }}</span>
        </div>
        <div v-if="r.handleResult" class="result">
          <strong>处理结果：</strong>{{ r.handleResult }}
        </div>
      </div>
      <p v-if="!repairs.length" class="empty">暂无报修记录</p>
    </div>

    <!-- 提交报修弹窗 -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-box">
        <h3>提交报修</h3>
        <div class="form-group">
          <label>报修类型</label>
          <select v-model="form.repairType" class="input">
            <option value="WATER">水电故障</option>
            <option value="ELEVATOR">电梯故障</option>
            <option value="DOOR">门禁问题</option>
            <option value="PIPE">管道问题</option>
            <option value="ROOF">屋面问题</option>
            <option value="OTHER">其他</option>
          </select>
        </div>
        <div class="form-group">
          <label>标题</label>
          <input v-model="form.title" class="input" placeholder="简要描述问题" />
        </div>
        <div class="form-group">
          <label>详细描述</label>
          <textarea v-model="form.description" class="textarea" rows="3" placeholder="请详细描述..." />
        </div>
        <div class="form-group">
          <label>地址</label>
          <input v-model="form.address" class="input" placeholder="报修地址" />
        </div>
        <div class="form-group">
          <label>联系电话</label>
          <input v-model="form.reporterPhone" class="input" placeholder="您的电话" />
        </div>
        <div class="modal-actions">
          <button @click="showForm = false" class="btn-default">取消</button>
          <button @click="submitRepair" class="btn-primary">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getMyRepairs, submitRepair } from '../api'

const loading = ref(false)
const showForm = ref(false)
const repairs = ref<any[]>([])
const form = reactive({
  repairType: 'WATER',
  title: '',
  description: '',
  address: '',
  reporterPhone: ''
})

function statusLabel(status: string) {
  const map: any = { PENDING: '待处理', ASSIGNED: '已派单', PROCESSING: '处理中', COMPLETED: '已完成', REJECTED: '已驳回' }
  return map[status] || status
}
function statusClass(status: string) {
  if (status === 'COMPLETED') return 'status-green'
  if (status === 'ASSIGNED' || status === 'PROCESSING') return 'status-blue'
  if (status === 'REJECTED') return 'status-red'
  return 'status-orange'
}
function repairTypeLabel(t: string) {
  const map: any = { WATER: '水电', ELEVATOR: '电梯', DOOR: '门禁', PIPE: '管道', ROOF: '屋面', OTHER: '其他' }
  return map[t] || t
}

async function loadData() {
  loading.value = true
  try {
    repairs.value = await getMyRepairs() || []
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

async function submitRepair() {
  if (!form.title.trim() || !form.description.trim()) {
    alert('请填写标题和描述')
    return
  }
  try {
    await submitRepair(form)
    alert('提交成功！')
    showForm.value = false
    Object.assign(form, { repairType: 'WATER', title: '', description: '', address: '', reporterPhone: '' })
    loadData()
  } catch (e: any) {
    alert('提交失败：' + (e?.message || '未知错误'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.header {
  background: linear-gradient(135deg, #fa541c 0%, #d4380d 100%);
  border-radius: 12px; padding: 20px; color: #fff; margin-bottom: 16px;
}
.header h2 { font-size: 20px; margin-bottom: 4px; }
.header p { font-size: 13px; opacity: 0.8; }

.btn-submit {
  width: 100%; padding: 12px; background: #fa541c; color: #fff; border: none;
  border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; margin-bottom: 16px;
}

.section-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #374151; }

.card {
  background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.title { font-size: 14px; font-weight: 600; }
.status { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.status-green { background: #f6ffed; color: #52c41a; }
.status-blue { background: #e6f7ff; color: #1890ff; }
.status-orange { background: #fff7e6; color: #fa8c1c; }
.status-red { background: #fff1f0; color: #ff4d4f; }

.desc { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.card-bottom { display: flex; justify-content: space-between; font-size: 11px; color: #9ca3af; }
.result { margin-top: 10px; padding: 8px; background: #f6ffed; border-radius: 6px; font-size: 12px; color: #389e0d; }

.loading { text-align: center; padding: 20px; color: #9ca3af; }
.empty { text-align: center; padding: 20px; color: #9ca3af; font-size: 13px; }

.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 100;
}
.modal-box {
  background: #fff; border-radius: 12px; padding: 20px; width: 90%; max-width: 400px;
}
.modal-box h3 { font-size: 16px; font-weight: 600; margin-bottom: 16px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; color: #374151; margin-bottom: 4px; }
.input, .textarea {
  width: 100%; padding: 10px 12px; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 14px; outline: none;
}
.input:focus, .textarea:focus { border-color: #fa541c; }
.textarea { resize: none; }
.modal-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px; }
.btn-primary {
  padding: 10px 20px; background: #fa541c; color: #fff; border: none;
  border-radius: 6px; font-size: 14px; cursor: pointer;
}
.btn-default {
  padding: 10px 20px; background: #f3f4f6; color: #6b7280; border: none;
  border-radius: 6px; font-size: 14px; cursor: pointer;
}
</style>