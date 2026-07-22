import { ref, computed, watch, type Ref } from 'vue'
import type { PagedResult } from '../api/types'

export interface UsePaginationOptions<T, F = Record<string, unknown>> {
  pageSize?: number
  fetcher: (page: number, pageSize: number, filters: F) => Promise<PagedResult<T>>
  filters: Ref<F> | (() => F)
}

export function usePagination<T, F = Record<string, unknown>>(options: UsePaginationOptions<T, F>) {
  const pageSize = options.pageSize ?? 10
  const currentPage = ref(1)
  const items = ref<T[]>([]) as Ref<T[]>
  const total = ref(0)
  const loading = ref(false)
  const errorMessage = ref('')
  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

  function getFilters(): F {
    return typeof options.filters === 'function' ? options.filters() : options.filters.value
  }

  async function loadPage() {
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await options.fetcher(currentPage.value, pageSize, getFilters())
      items.value = result.items
      total.value = result.total
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '数据加载失败'
      items.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  function changePage(page: number) {
    if (page < 1 || page > totalPages.value || page === currentPage.value) return
    currentPage.value = page
    void loadPage()
  }

  function resetAndReload() {
    currentPage.value = 1
    void loadPage()
  }

  watch(totalPages, (value) => {
    if (currentPage.value > value) currentPage.value = value
  })

  return { items, total, loading, errorMessage, currentPage, pageSize, totalPages, loadPage, changePage, resetAndReload }
}
