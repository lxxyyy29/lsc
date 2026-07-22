<template>
  <section class="page-container">
    <header v-if="description || $slots.header || $slots.actions" class="page-container__header">
      <slot name="header">
        <div v-if="description" class="page-container__heading">
          <p class="page-container__description">{{ description }}</p>
        </div>
      </slot>
      <div v-if="$slots.actions" class="page-container__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="page-container__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
}>()
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-container__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.page-container__heading {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.page-container__title {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
  color: var(--fg-text-primary, #eaf5ff);
}

.page-container__description {
  margin: 0;
  max-width: 720px;
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
  line-height: 1.6;
}

.page-container__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  margin-left: auto;
}

.page-container__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 720px) {
  .page-container__header {
    flex-direction: column;
  }

  .page-container__actions {
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}
</style>
