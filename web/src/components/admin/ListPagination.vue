<template>
  <footer class="list-pagination" aria-label="分页">
    <span class="list-pagination__total">共 {{ total }} 条记录</span>
    <div class="list-pagination__right">
      <button type="button" class="pg-btn" aria-label="上一页" :disabled="currentPage === 1 || disabled" @click="$emit('change', currentPage - 1)">&lt;</button>
      <button
        v-for="p in visiblePages"
        :key="p"
        type="button"
        class="pg-btn"
        :class="{ 'pg-btn--active': p === currentPage }"
        :disabled="disabled"
        @click="$emit('change', p)"
      >{{ p }}</button>
      <button type="button" class="pg-btn" aria-label="下一页" :disabled="currentPage >= totalPages || disabled" @click="$emit('change', currentPage + 1)">&gt;</button>
      <span class="pg-size-label">{{ pageSize }} 条/页</span>
    </div>
  </footer>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ total: number; currentPage: number; pageSize: number; disabled?: boolean }>()
defineEmits<{ change: [page: number] }>()
const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = props.currentPage
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  let start = Math.max(1, current - 2)
  let end = Math.min(total, current + 2)
  if (start <= 2) {
    start = 1
    end = Math.min(5, total)
  }
  if (end >= total - 1) {
    end = total
    start = Math.max(1, total - 4)
  }
  const pages: number[] = []
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})
</script>
<style scoped>
.list-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  color: rgba(205, 222, 248, 0.78);
  font-size: 13px;
}

.list-pagination__total {
  white-space: nowrap;
}

.list-pagination__right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pg-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 6px;
  border: 1px solid rgba(125, 163, 220, 0.18);
  border-radius: 6px;
  background: rgba(10, 26, 45, 0.72);
  color: rgba(238, 245, 255, 0.88);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.pg-btn:hover:not(:disabled) {
  border-color: #409eff;
  color: #fff;
}

.pg-btn--active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
  font-weight: 600;
}

.pg-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.pg-size-label {
  margin-left: 8px;
  padding: 5px 10px;
  border: 1px solid rgba(125, 163, 220, 0.18);
  border-radius: 6px;
  background: rgba(10, 26, 45, 0.72);
  color: rgba(238, 245, 255, 0.88);
  font-size: 13px;
  white-space: nowrap;
}
</style>
