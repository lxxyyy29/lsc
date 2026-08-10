<template>
  <view class="page">
    <view class="header">
      <text class="header-title">🔧 便民报修</text>
      <text class="header-sub">提交报修申请，查看处理进度</text>
    </view>

    <!-- 提交报修按钮 -->
    <button @click="showForm = true" class="btn-submit">+ 提交报修</button>

    <!-- 我的报修列表 -->
    <view class="section-title">我的报修</view>
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="r in repairs" :key="r.id" class="card">
        <view class="card-top">
          <text class="title">{{ r.title }}</text>
          <text :class="['status', statusClass(r.status)]">{{ statusLabel(r.status) }}</text>
        </view>
        <text class="desc">{{ r.description }}</text>
        <view class="card-bottom">
          <text class="type">{{ repairTypeLabel(r.repairType) }}</text>
          <text class="time">{{ r.createdAt }}</text>
        </view>
        <view v-if="r.handleResult" class="result">
          <text class="result-label">处理结果：</text>{{ r.handleResult }}
        </view>
      </view>
      <text v-if="!repairs.length" class="empty">暂无报修记录</text>
    </view>

    <!-- 提交报修弹窗 -->
    <view v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <view class="modal-box">
        <text class="modal-title">提交报修</text>
        <view class="form-group">
          <text class="label">报修类型</text>
          <picker :range="repairTypeOptions" range-key="label" :value="repairTypeIndex" @change="onRepairTypeChange">
            <view class="picker-input">{{ repairTypeOptions[repairTypeIndex].label }}</view>
          </picker>
        </view>
        <view class="form-group">
          <text class="label">标题</text>
          <input v-model="form.title" class="input" placeholder="简要描述问题" />
        </view>
        <view class="form-group">
          <text class="label">详细描述</text>
          <textarea v-model="form.description" class="textarea" placeholder="请详细描述..." />
        </view>
        <view class="form-group">
          <text class="label">地址</text>
          <input v-model="form.address" class="input" placeholder="报修地址" />
        </view>
        <view class="form-group">
          <text class="label">联系电话</text>
          <input v-model="form.reporterPhone" class="input" placeholder="您的电话" />
        </view>
        <view class="modal-actions">
          <button @click="showForm = false" class="btn-default">取消</button>
          <button @click="submitRepair" class="btn-primary">提交</button>
        </view>
      </view>
    </view>

    <ResidentTabBar current="/pages/resident/repairs/index" />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getMyRepairs, submitRepair as submitRepairApi } from '../../../src/api/resident'

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

const repairTypeOptions = [
  { value: 'WATER', label: '水电故障' },
  { value: 'ELEVATOR', label: '电梯故障' },
  { value: 'DOOR', label: '门禁问题' },
  { value: 'PIPE', label: '管道问题' },
  { value: 'ROOF', label: '屋面问题' },
  { value: 'OTHER', label: '其他' }
]
const repairTypeIndex = computed(() => {
  const idx = repairTypeOptions.findIndex(o => o.value === form.repairType)
  return idx >= 0 ? idx : 0
})

function onRepairTypeChange(e: any) {
  form.repairType = repairTypeOptions[e.detail.value].value
}

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
    uni.showToast({ title: '请填写标题和描述', icon: 'none' })
    return
  }
  try {
    await submitRepairApi(form)
    uni.showToast({ title: '提交成功！', icon: 'success' })
    showForm.value = false
    Object.assign(form, { repairType: 'WATER', title: '', description: '', address: '', reporterPhone: '' })
    loadData()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '提交失败', icon: 'none' })
  }
}

onLoad(loadData)
</script>

<style scoped>
.page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.header {
  background: linear-gradient(135deg, #fa541c 0%, #d4380d 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.header-sub { font-size: 26rpx; opacity: 0.8; }
.btn-submit {
  width: 100%; padding: 24rpx; background: #fa541c; color: #fff; border: none;
  border-radius: 16rpx; font-size: 30rpx; font-weight: 600; margin-bottom: 32rpx; line-height: 1.5;
}
.section-title { font-size: 28rpx; font-weight: 600; margin-bottom: 24rpx; color: #374151; }
.card {
  background: #fff; border-radius: 24rpx; padding: 32rpx; margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.title { font-size: 28rpx; font-weight: 600; flex: 1; margin-right: 16rpx; }
.status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; flex-shrink: 0; }
.status-green { background: #f6ffed; color: #52c41a; }
.status-blue { background: #e6f7ff; color: #1890ff; }
.status-orange { background: #fff7e6; color: #fa8c1c; }
.status-red { background: #fff1f0; color: #ff4d4f; }
.desc { font-size: 26rpx; color: #6b7280; margin-bottom: 16rpx; display: block; line-height: 1.5; }
.card-bottom { display: flex; justify-content: space-between; font-size: 22rpx; color: #9ca3af; }
.result { margin-top: 20rpx; padding: 16rpx; background: #f6ffed; border-radius: 12rpx; font-size: 24rpx; color: #389e0d; }
.result-label { font-weight: 600; }
.loading { text-align: center; padding: 40rpx; color: #9ca3af; }
.empty { text-align: center; padding: 40rpx; color: #9ca3af; font-size: 26rpx; display: block; }
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-box {
  background: #fff; border-radius: 24rpx; padding: 40rpx; width: 90%; max-width: 800rpx;
  max-height: 80vh; overflow-y: auto;
}
.modal-title { font-size: 32rpx; font-weight: 600; display: block; margin-bottom: 32rpx; }
.form-group { margin-bottom: 24rpx; }
.form-group .label { display: block; font-size: 26rpx; color: #374151; margin-bottom: 8rpx; }
.input, .textarea, .picker-input {
  width: 100%; padding: 20rpx 24rpx; border: 1px solid #e5e7eb; border-radius: 16rpx;
  font-size: 28rpx; outline: none; box-sizing: border-box; background: #fff;
}
.textarea { min-height: 120rpx; }
.picker-input { color: #333; }
.modal-actions { display: flex; gap: 24rpx; justify-content: flex-end; margin-top: 32rpx; }
.btn-primary {
  padding: 20rpx 40rpx; background: #fa541c; color: #fff; border: none;
  border-radius: 12rpx; font-size: 28rpx; line-height: 1.5;
}
.btn-default {
  padding: 20rpx 40rpx; background: #f3f4f6; color: #6b7280; border: none;
  border-radius: 12rpx; font-size: 28rpx; line-height: 1.5;
}
</style>
