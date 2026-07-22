<template>
  <span class="status-tag" :class="`status-tag--${tone}`">{{ text }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
}>()

const labelMap: Record<string, string> = {
  PENDING_AUDIT: '待审核',
  AUDITING: '审核中',
  WAITING_DISPATCH: '待派单',
  PROCESSING: '处理中',
  WAITING_CLOSE_CONFIRM: '待关单确认',
  CLOSED: '已关闭',
  REJECTED: '已驳回',
  PENDING: '待处理',
  ACTIVE: '启用',
  APPROVED: '已通过',
  DISPATCHED: '已派单',
  ENABLED: '启用',
  DISABLED: '停用',
  COMPLETED: '已完成',
  IN_PROGRESS: '进行中'
}

const toneMap: Record<string, string> = {
  PENDING_AUDIT: 'warning',
  WAITING_DISPATCH: 'warning',
  PENDING: 'warning',
  REJECTED: 'danger',
  DISABLED: 'danger',
  APPROVED: 'success',
  CLOSED: 'success',
  ENABLED: 'success',
  COMPLETED: 'success',
  DISPATCHED: 'primary',
  PROCESSING: 'primary',
  ACTIVE: 'primary',
  IN_PROGRESS: 'primary',
  WAITING_CLOSE_CONFIRM: 'info',
  AUDITING: 'info'
}

const text = computed(() => labelMap[props.status] ?? props.status)
const tone = computed(() => toneMap[props.status] ?? 'default')
</script>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-tag--default {
  background: #f4f4f5;
  color: #606266;
}

.status-tag--warning {
  background: #fdf6ec;
  color: #e6a23c;
}

.status-tag--danger {
  background: #fef0f0;
  color: #f56c6c;
}

.status-tag--success {
  background: #f0f9eb;
  color: #67c23a;
}

.status-tag--primary {
  background: #ecf5ff;
  color: #409eff;
}

.status-tag--info {
  background: #f4f4f5;
  color: #909399;
}
</style>
