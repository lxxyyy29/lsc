<template>
  <div class="web-list-page-template" data-testid="web-list-page-template">
    <section v-if="$slots.toolbar" class="panel list-page-header list-section-shell">
      <div class="list-section-shell__actions">
        <slot name="toolbar" />
      </div>
    </section>

    <section v-if="$slots.filters" class="panel list-query-panel list-section-shell">
      <header v-if="filterTitle || filterMeta || $slots['filter-actions']" class="list-query-panel__header">
        <div class="list-section-shell__heading">
          <h3 v-if="filterTitle" class="list-query-panel__title">{{ filterTitle }}</h3>
          <p v-if="filterMeta" class="list-query-panel__meta">{{ filterMeta }}</p>
        </div>
        <div v-if="$slots['filter-actions']" class="list-section-shell__actions">
          <slot name="filter-actions" />
        </div>
      </header>
      <div class="list-query-panel__form">
        <slot name="filters" />
      </div>
    </section>

    <section class="panel list-table-panel list-section-shell">
      <header v-if="tableTitle || $slots['table-actions']" class="list-table-panel__header">
        <div class="list-section-shell__heading">
          <h3 v-if="tableTitle">{{ tableTitle }}</h3>
        </div>
        <div v-if="$slots['table-actions']" class="list-section-shell__actions">
          <slot name="table-actions" />
        </div>
      </header>
      <div class="list-table-panel__body">
        <slot name="table" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  filterTitle?: string
  filterMeta?: string
  tableTitle?: string
  tableMeta?: string
}>()
</script>

<style scoped>
.web-list-page-template {
  display: grid;
  gap: 16px;
}

.list-section-shell {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.list-page-header,
.list-query-panel__header,
.list-table-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.list-page-header__content,
.list-section-shell__heading {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.list-page-header__title,
.list-query-panel__title,
.list-table-panel__header h3 {
  margin: 0;
  color: #eaf5ff;
}

.list-page-header__title {
  font-size: 20px;
}

.list-query-panel__title,
.list-table-panel__header h3 {
  font-size: 18px;
}

.list-query-panel__meta,
.list-table-panel__meta {
  margin: 0;
  color: #8db0d0;
  font-size: 13px;
  line-height: 1.5;
}

.list-section-shell__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  margin-left: auto;
}

.list-query-panel__form,
.list-table-panel__body {
  min-width: 0;
}

.list-table-panel {
  overflow: hidden;
}

@media (max-width: 720px) {
  .list-section-shell {
    padding: 16px;
  }

  .list-page-header,
  .list-query-panel__header,
  .list-table-panel__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .list-section-shell__actions {
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}
</style>
