<template>
  <section class="query-panel" aria-label="查询条件">
    <div class="query-panel__form">
      <slot />
    </div>
    <div v-if="$slots.actions" class="query-panel__actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<style scoped>
.query-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.query-panel__form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px 16px;
  align-items: center;
}

.query-panel__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 720px) {
  .query-panel__actions {
    justify-content: flex-start;
  }
}
</style>
