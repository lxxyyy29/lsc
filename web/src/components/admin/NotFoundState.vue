<template>
  <section class="not-found-state" role="status" aria-live="polite">
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
}>()
</script>

<style scoped>
.not-found-state {
  border: 1px dashed #dcdfe6;
  border-radius: 12px;
  padding: 24px;
  background: #fafafa;
  color: #606266;
}

.not-found-state h3 {
  margin: 0 0 8px;
  color: #303133;
}

.not-found-state p {
  margin: 0;
}
</style>
