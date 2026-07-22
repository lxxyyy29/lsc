<template>
  <Teleport to="body">
    <TransitionGroup name="toast" tag="div" class="app-toast-container">
      <div
        v-for="item in toasts"
        :key="item.id"
        class="app-toast"
        :class="`app-toast--${item.type}`"
        role="alert"
        @click="dismiss(item.id)"
      >
        <span class="app-toast__icon">{{ icons[item.type] }}</span>
        <span class="app-toast__message">{{ item.message }}</span>
        <button class="app-toast__close" aria-label="关闭">×</button>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup lang="ts">
import { useToast } from '../../composables/useToast'

const { toasts } = useToast()

const icons: Record<string, string> = {
  success: '✓',
  error:   '✕',
  info:    'ℹ',
  warning: '⚠',
}

function dismiss(id: number) {
  const idx = toasts.findIndex((t) => t.id === id)
  if (idx !== -1) toasts.splice(idx, 1)
}
</script>

<style scoped>
.app-toast-container {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 99999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  pointer-events: none;
}

.app-toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  min-width: 220px;
  max-width: 480px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.45);
  pointer-events: all;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}

.app-toast--success {
  background: rgba(15, 64, 32, 0.97);
  color: #6ee6a7;
  border: 1px solid rgba(110, 230, 167, 0.3);
}

.app-toast--error {
  background: rgba(68, 14, 14, 0.97);
  color: #ffb4b4;
  border: 1px solid rgba(255, 120, 117, 0.3);
}

.app-toast--info {
  background: rgba(10, 36, 64, 0.97);
  color: #8dc5ff;
  border: 1px solid rgba(64, 158, 255, 0.3);
}

.app-toast--warning {
  background: rgba(64, 44, 8, 0.97);
  color: #ffd666;
  border: 1px solid rgba(255, 214, 102, 0.3);
}

.app-toast__icon {
  font-size: 15px;
  flex-shrink: 0;
  font-style: normal;
}

.app-toast__message {
  flex: 1;
}

.app-toast__close {
  flex-shrink: 0;
  background: none;
  border: none;
  color: inherit;
  opacity: 0.5;
  font-size: 16px;
  cursor: pointer;
  padding: 0 0 0 6px;
  line-height: 1;
}

.app-toast__close:hover {
  opacity: 1;
}

/* TransitionGroup 动画 */
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.toast-move {
  transition: transform 0.22s ease;
}
</style>
