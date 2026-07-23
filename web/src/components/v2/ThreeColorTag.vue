<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  level: 'green' | 'yellow' | 'red' | string
  label?: string
}>()

const config = computed(() => {
  const key = String(props.level).toLowerCase()
  switch (key) {
    case 'green':
    case '一般':
    case '低':
    case '低风险':
    case '已完成':
    case '正常':
      return { label: props.label || '一般', class: 'level-green' }
    case 'yellow':
    case '重点':
    case '中':
    case '中风险':
    case '处置中':
      return { label: props.label || '重点', class: 'level-yellow' }
    case 'red':
    case '紧急':
    case '高':
    case '高风险':
    case '待派单':
    case '超期':
      return { label: props.label || '紧急', class: 'level-red' }
    default:
      return { label: props.label || props.level || '未知', class: 'level-default' }
  }
})
</script>

<template>
  <span class="three-color-tag" :class="config.class">{{ config.label }}</span>
</template>

<style scoped>
.three-color-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.level-green {
  background: rgba(103, 194, 58, 0.14);
  border: 1px solid rgba(103, 194, 58, 0.28);
  color: #85ce61;
}

.level-yellow {
  background: rgba(230, 162, 60, 0.14);
  border: 1px solid rgba(230, 162, 60, 0.28);
  color: #f0c78a;
}

.level-red {
  background: rgba(245, 108, 108, 0.14);
  border: 1px solid rgba(245, 108, 108, 0.28);
  color: #f89898;
}

.level-default {
  background: rgba(94, 162, 255, 0.12);
  border: 1px solid rgba(94, 162, 255, 0.22);
  color: var(--v2-primary-light);
}
</style>
