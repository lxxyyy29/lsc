<script setup lang="ts">
defineProps<{
  items: Array<{
    label: string
    value: string
    hint: string
  }>
}>()
</script>

<template>
  <div class="system-metric-grid">
    <article v-for="item in items" :key="item.label" class="system-metric-card panel">
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
      <p>{{ item.hint }}</p>
    </article>
  </div>
</template>

<style scoped>
.system-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.system-metric-card {
  display: grid;
  gap: 8px;
  min-height: 132px;
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(17, 44, 74, 0.95) 0%, rgba(9, 24, 40, 0.98) 100%),
    radial-gradient(circle at top right, rgba(94, 162, 255, 0.22) 0%, transparent 40%);
}

.system-metric-card span,
.system-metric-card p {
  margin: 0;
  color: rgba(205, 222, 248, 0.78);
}

.system-metric-card strong {
  color: #eef5ff;
  font-size: 28px;
  line-height: 1.1;
}

.system-metric-card p {
  line-height: 1.6;
}

@media (max-width: 1100px) {
  .system-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .system-metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
