<template>
  <view class="page">
    <view class="section-block section-block--tight">
      <view class="section-head">
        <view class="section-title-wrap">
          <text class="section-mark"></text>
          <text class="section-title">待复核工单</text>
        </view>
      </view>
      <view v-if="isLoading" class="empty-card" style="margin-top:16rpx;">
        <text class="empty-title">加载中...</text>
      </view>
      <view v-else-if="loadError" class="empty-card" style="margin-top:16rpx;">
        <text class="empty-title">加载失败</text>
      </view>
      <view v-else-if="canView" class="task-card">
        <text class="task-label">工单编号</text>
        <view class="task-row">
          <text class="task-value">{{ currentTask?.workOrderNo || '--' }}</text>
          <text class="status-chip">{{ currentTask?.statusText || '待复核' }}</text>
        </view>
        <view style="margin-top:16rpx;">
          <text class="task-label">事件标题</text>
          <text class="meta-value" style="margin-top:6rpx;">{{ currentTask?.eventTitle || '--' }}</text>
        </view>
      </view>
      <view v-else class="empty-card" style="margin-top:16rpx;">
        <text class="empty-title">暂无待复核工单</text>
        <text class="empty-description">复核核查由管理端负责，此处仅展示进度</text>
      </view>
    </view>

    <view class="section-block">
      <view class="section-head">
        <view class="section-title-wrap">
          <text class="section-mark"></text>
          <text class="section-title">说明</text>
        </view>
      </view>
      <view class="note-block">
        <text class="note-textarea" style="background:transparent;padding:0;">
          现场处置完成后，工单将提交至管理端进行复核核查。复核通过则办结归档，不通过则退回重新处理。
        </text>
      </view>
    </view>

    <view class="bottom-bar">
      <button class="bottom-btn bottom-btn--ghost" @click="goBack">返回</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getWorkOrders, type WorkOrderItem } from '../../src/api/workorder'
import { ensureAuthenticated } from '../../src/uni/navigation'

// 复核核查已统一由 Web 管理端负责。H5 端仅展示待复核工单信息（只读）。
const isLoading = ref(true)
const loadError = ref(false)
const currentTask = ref<WorkOrderItem | null>(null)

const canView = computed(() => currentTask.value != null)

function goBack() {
  uni.navigateBack()
}

onShow(async () => {
  if (!ensureAuthenticated('/verify')) return
  isLoading.value = true
  loadError.value = false
  try {
    const orders = await getWorkOrders()
    // 展示待复核（待关闭确认）的工单，供网格员了解进度
    currentTask.value = orders.find((item) => item.status === 'WAITING_CLOSE_CONFIRM') ?? null
  } catch {
    loadError.value = true
    currentTask.value = null
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 18rpx 244rpx;
  background:
    radial-gradient(circle at top, rgba(29, 69, 108, 0.34) 0, rgba(29, 69, 108, 0) 42%),
    #081421;
  color: #eef6ff;
}

.section-title-wrap,
.section-head,
.task-row,
.bottom-bar {
  display: flex;
  align-items: center;
}

.task-row,
.between,
.section-head {
  justify-content: space-between;
}

.section-block {
  display: grid;
  gap: 16rpx;
  margin-bottom: 22rpx;
}

.section-block--tight {
  margin-bottom: 26rpx;
}

.section-mark {
  width: 6rpx;
  height: 24rpx;
  border-radius: 999rpx;
  background: linear-gradient(180deg, #d7f7ff 0%, #52d4ff 100%);
  box-shadow: 0 0 12rpx rgba(82, 212, 255, 0.35);
  margin-right: 12rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
}

.task-card {
  border-radius: 18rpx;
  background: linear-gradient(180deg, rgba(18, 32, 49, 0.98) 0%, rgba(14, 26, 40, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.08);
  padding: 24rpx 20rpx 22rpx;
}

.task-label {
  display: block;
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.72);
  margin-bottom: 10rpx;
}

.task-value {
  font-size: 38rpx;
  font-weight: 700;
  color: #f3f8ff;
}

.meta-value {
  display: block;
  font-size: 28rpx;
  color: #f3f8ff;
  font-weight: 600;
}

.status-chip {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(250, 173, 20, 0.16);
  color: #fde68a;
  font-size: 24rpx;
  border: 1px solid rgba(250, 173, 20, 0.28);
}

.task-top-right {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.empty-card {
  display: grid;
  gap: 10rpx;
  padding: 40rpx 24rpx;
  text-align: center;
  border-radius: 18rpx;
  background: linear-gradient(180deg, rgba(18, 31, 46, 0.98) 0%, rgba(11, 24, 38, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.08);
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #f3f8ff;
}

.empty-description {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.65);
}

.note-block {
  padding: 18rpx;
  border-radius: 18rpx;
  background: linear-gradient(180deg, rgba(18, 32, 49, 0.98) 0%, rgba(14, 26, 40, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.08);
}

.note-textarea {
  font-size: 26rpx;
  color: rgba(214, 225, 239, 0.75);
  line-height: 1.6;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx;
  gap: 16rpx;
  background: linear-gradient(180deg, rgba(8, 20, 33, 0.02) 0%, #081421 22%);
  backdrop-filter: blur(18rpx);
}

.bottom-btn {
  flex: 1;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
}

.bottom-btn--ghost {
  background: #122031;
  color: #f3f8ff;
  border: 1px solid rgba(118, 189, 255, 0.1);
}
</style>
