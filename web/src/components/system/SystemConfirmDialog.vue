<template>
  <SystemDialog :open="open" :title="title" :subtitle="subtitle" @close="emit('close')">
    <div class="system-confirm-dialog__body">
      <p>{{ message }}</p>
      <p v-if="description">{{ description }}</p>
      <p v-if="error" class="system-confirm-dialog__error">{{ error }}</p>
    </div>
    <template #footer>
      <button type="button" class="action-button action-button--secondary" :disabled="loading" @click="emit('close')">取消</button>
      <button type="button" class="action-button action-button--danger" :disabled="loading" @click="emit('confirm')">
        {{ loading ? loadingText : confirmText }}
      </button>
    </template>
  </SystemDialog>
</template>

<script setup lang="ts">
import SystemDialog from './SystemDialog.vue'

withDefaults(defineProps<{
  open: boolean
  title?: string
  subtitle?: string
  message: string
  description?: string
  error?: string
  loading?: boolean
  confirmText?: string
  loadingText?: string
}>(), {
  title: '确认删除',
  subtitle: '操作确认',
  description: '',
  error: '',
  loading: false,
  confirmText: '确认删除',
  loadingText: '删除中...'
})

const emit = defineEmits<{
  close: []
  confirm: []
}>()
</script>

<style scoped>
@import '../../views/admin-shared.css';

.system-confirm-dialog__body {
  display: grid;
  gap: 12px;
}

.system-confirm-dialog__body p {
  margin: 0;
  color: rgba(221, 235, 255, 0.86);
  line-height: 1.7;
}

.system-confirm-dialog__error {
  color: #ffb4b4 !important;
}
</style>
