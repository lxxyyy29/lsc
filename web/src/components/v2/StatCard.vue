<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  value: string | number
  icon?: string
  trend?: number
  suffix?: string
  color?: 'blue' | 'green' | 'yellow' | 'red'
}>()

const colorClass = computed(() => {
  switch (props.color) {
    case 'green':
      return 'stat-card--green'
    case 'yellow':
      return 'stat-card--yellow'
    case 'red':
      return 'stat-card--red'
    default:
      return 'stat-card--blue'
  }
})

const displayValue = computed(() => {
  const num = typeof props.value === 'number' ? props.value : Number(props.value)
  if (!Number.isNaN(num)) {
    return num.toLocaleString('zh-CN')
  }
  return props.value
})
</script>

<template>
  <div class="stat-card" :class="colorClass">
    <div class="stat-card__icon" v-if="icon">{{ icon }}</div>
    <div class="stat-card__content">
      <div class="stat-card__label">{{ label }}</div>
      <div class="stat-card__value">
        {{ displayValue }}
        <span v-if="suffix" class="stat-card__suffix">{{ suffix }}</span>
      </div>
      <div v-if="trend !== undefined" class="stat-card__trend" :class="trend >= 0 ? 'stat-card__trend--up' : 'stat-card__trend--down'">
        {{ trend >= 0 ? '↑' : '↓' }} {{ Math.abs(trend) }}%
      </div>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: var(--v2-bg-card);
  border: 1px solid var(--v2-border);
  border-radius: var(--v2-radius-lg);
  box-shadow: var(--v2-shadow-card);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.stat-card:hover {
  box-shadow: var(--v2-shadow-card-hover);
  transform: translateY(-2px);
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 22px;
  background: rgba(94, 162, 255, 0.12);
  border: 1px solid rgba(94, 162, 255, 0.22);
}

.stat-card--green .stat-card__icon {
  background: rgba(103, 194, 58, 0.12);
  border-color: rgba(103, 194, 58, 0.22);
}

.stat-card--yellow .stat-card__icon {
  background: rgba(230, 162, 60, 0.12);
  border-color: rgba(230, 162, 60, 0.22);
}

.stat-card--red .stat-card__icon {
  background: rgba(245, 108, 108, 0.12);
  border-color: rgba(245, 108, 108, 0.22);
}

.stat-card__content {
  flex: 1;
  min-width: 0;
}

.stat-card__label {
  font-size: 13px;
  color: var(--v2-text-secondary);
  margin-bottom: 6px;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
  color: var(--v2-text-primary);
  background: linear-gradient(180deg, #fff 0%, #a8d4ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-card__suffix {
  font-size: 14px;
  font-weight: 500;
  margin-left: 4px;
  -webkit-text-fill-color: var(--v2-text-secondary);
}

.stat-card__trend {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 600;
}

.stat-card__trend--up {
  color: var(--v2-success);
}

.stat-card__trend--down {
  color: var(--v2-danger);
}
</style>
