<template>
  <div v-if="open" class="system-dialog" role="dialog" aria-modal="true" :aria-label="title">
    <div class="system-dialog__backdrop"></div>
    <section class="system-dialog__panel" :class="panelClass">
      <header class="system-dialog__header">
        <div>
          <p v-if="subtitle" class="system-dialog__subtitle">{{ subtitle }}</p>
          <h3 class="system-dialog__title">{{ title }}</h3>
        </div>
        <button type="button" class="system-dialog__close" aria-label="关闭弹窗" @click="emit('close')">关闭</button>
      </header>

      <div class="system-dialog__body">
        <slot />
      </div>

      <footer v-if="$slots.footer" class="system-dialog__footer">
        <slot name="footer" />
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  open: boolean
  title: string
  subtitle?: string
  panelClass?: string
}>()

const emit = defineEmits<{
  close: []
}>()
</script>

<style scoped>
.system-dialog {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.system-dialog__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(2, 6, 12, 0.72);
  backdrop-filter: blur(8px);
}

.system-dialog__panel {
  position: relative;
  z-index: 1;
  width: min(640px, 100%);
  max-height: min(88vh, 960px);
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(64, 158, 255, 0.25);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(14, 30, 50, 0.98) 0%, rgba(6, 18, 28, 0.98) 100%);
  box-shadow: 0 0 40px rgba(64, 158, 255, 0.1), 0 24px 48px rgba(0, 0, 0, 0.6);
}

.system-dialog__panel.system-dialog__panel--wide {
  width: min(1200px, 100%);
}

.system-dialog__header,
.system-dialog__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
}

.system-dialog__header {
  border-bottom: 1px solid rgba(64, 158, 255, 0.1);
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.04) 0%, transparent 100%);
}

.system-dialog__footer {
  border-top: 1px solid rgba(64, 158, 255, 0.1);
  background: rgba(0, 0, 0, 0.1);
  justify-content: flex-end;
  gap: 12px;
}

.system-dialog__subtitle {
  margin: 0;
  color: rgba(64, 158, 255, 0.8);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.system-dialog__title {
  margin: 4px 0 0;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.02em;
}

.system-dialog__body {
  display: grid;
  gap: 20px;
  padding: 24px;
  overflow-y: auto;
}

.system-dialog__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  color: rgba(255, 255, 255, 0.6);
  background: transparent;
  font-size: 0; /* Hide text */
  cursor: pointer;
  transition: all 0.2s;
}

.system-dialog__close::before,
.system-dialog__close::after {
  content: '';
  position: absolute;
  width: 14px;
  height: 2px;
  background-color: currentColor;
  border-radius: 1px;
}

.system-dialog__close::before {
  transform: rotate(45deg);
}

.system-dialog__close::after {
  transform: rotate(-45deg);
}

.system-dialog__close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

@media (max-width: 720px) {
  .system-dialog {
    padding: 16px;
    align-items: flex-start;
  }

  .system-dialog__panel {
    width: 100%;
    margin-top: 24px;
  }

  .system-dialog__header,
  .system-dialog__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
