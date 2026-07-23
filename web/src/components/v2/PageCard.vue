<script setup lang="ts">
defineProps<{
  title?: string
  subtitle?: string
  padding?: boolean
}>()
</script>

<template>
  <section class="v2-page-card">
    <header v-if="title || subtitle || $slots.header" class="v2-page-card__header">
      <div>
        <h3 v-if="title" class="v2-page-card__title">{{ title }}</h3>
        <p v-if="subtitle" class="v2-page-card__subtitle">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.header" class="v2-page-card__extra">
        <slot name="header" />
      </div>
    </header>
    <div class="v2-page-card__body" :class="{ 'v2-page-card__body--no-padding': padding === false }">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.v2-page-card {
  background: var(--v2-bg-card);
  border: 1px solid var(--v2-border);
  border-radius: var(--v2-radius-lg);
  box-shadow: var(--v2-shadow-card);
  display: flex;
  flex-direction: column;
}

.v2-page-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--v2-border);
}

.v2-page-card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--v2-text-primary);
}

.v2-page-card__subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--v2-text-tertiary);
}

.v2-page-card__extra {
  display: flex;
  align-items: center;
  gap: 8px;
}

.v2-page-card__body {
  flex: 1;
  padding: 20px;
  min-height: 0;
}

.v2-page-card__body--no-padding {
  padding: 0;
}
</style>
