<template>
  <section class="page-container">
    <header class="page-header">
      <h2>场所台账</h2>
      <div class="page-header__actions">
        <el-button @click="handleExport">导出Excel</el-button>
      </div>
    </header>
    <div class="page-filters">
      <el-form :inline="true" :model="filters">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="场所名称/地址" clearable @change="onFilterChange" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="filters.category" placeholder="全部类别" clearable @change="onFilterChange">
            <el-option label="三小场所" value="SMALL_SHOP" />
            <el-option label="小作坊" value="SMALL_WORKSHOP" />
            <el-option label="出租屋" value="RENTAL_HOUSE" />
            <el-option label="工业园" value="INDUSTRIAL_PARK" />
            <el-option label="住宅小区" value="RESIDENTIAL" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onFilterChange">查询</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="displayItems" v-loading="loading" stripe>
      <el-table-column prop="merchantName" label="名称" width="180" />
      <el-table-column label="类别" width="100">
        <template #default="{ row }"><el-tag size="small">{{ categoryLabel(row) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="legalPersonName" label="负责人" width="100" />
      <el-table-column prop="legalPersonPhone" label="电话" width="130" />
      <el-table-column label="地址" show-overflow-tooltip>
        <template #default="{ row }">{{ getRemarkValue(row, '地址') || '-' }}</template>
      </el-table-column>
      <el-table-column label="面积" width="80">
        <template #default="{ row }">{{ getRemarkValue(row, '面积') ? getRemarkValue(row, '面积') + '㎡' : '-' }}</template>
      </el-table-column>
      <el-table-column label="两委干部" width="100">
        <template #default="{ row }">{{ getRemarkValue(row, '两委') || '-' }}</template>
      </el-table-column>
      <el-table-column label="消防巡查" width="100">
        <template #default="{ row }">{{ getRemarkValue(row, '消防') || '-' }}</template>
      </el-table-column>
    </el-table>
    <div class="pagination-container">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalCount"
        v-model:page-size="pageSize"
        v-model:current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElInput, ElSelect, ElOption, ElButton, ElTable, ElTableColumn, ElTag, ElPagination, ElForm, ElFormItem } from 'element-plus'
import { http } from '../../api/http'

interface LedgerItem {
  id: number
  merchantName: string
  legalPersonName: string
  legalPersonPhone: string
  remark: string
  status: string
}

const loading = ref(false)
const allItems = ref<LedgerItem[]>([])
const totalCount = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filters = ref({ keyword: '', category: '' })

const categoryMap: Record<string, string> = {
  SMALL_SHOP: '三小场所',
  SMALL_WORKSHOP: '小作坊',
  RENTAL_HOUSE: '出租屋',
  INDUSTRIAL_PARK: '工业园',
  RESIDENTIAL: '住宅小区',
  OTHER: '其他'
}

function parseRemark(remark: string): Record<string, string> {
  const result: Record<string, string> = {}
  if (!remark) return result
  remark.split(' | ').forEach(part => {
    const idx = part.indexOf(': ')
    if (idx > 0) {
      result[part.substring(0, idx).trim()] = part.substring(idx + 2).trim()
    }
  })
  return result
}

function getRemarkValue(item: LedgerItem, key: string): string {
  return parseRemark(item.remark)[key] || ''
}

function categoryLabel(item: LedgerItem): string {
  return categoryMap[getRemarkValue(item, '类别')] || '其他'
}

const filteredItems = computed(() => {
  let result = allItems.value
  if (filters.value.keyword) {
    const kw = filters.value.keyword.toLowerCase()
    result = result.filter(i =>
      (i.merchantName && i.merchantName.toLowerCase().includes(kw)) ||
      (getRemarkValue(i, '地址') && getRemarkValue(i, '地址').toLowerCase().includes(kw))
    )
  }
  if (filters.value.category) {
    result = result.filter(i => getRemarkValue(i, '类别') === filters.value.category)
  }
  return result
})

const displayItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

async function loadData() {
  loading.value = true
  try {
    const result = await http.get<{ items: LedgerItem[]; total: number }, { items: LedgerItem[]; total: number }>(
      '/merchants/paged', { params: { page: 1, size: 1000 } }
    )
    allItems.value = result.items
    totalCount.value = result.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  currentPage.value = 1
}

function handleExport() {
  window.open('/api/community/export/merchants', '_blank')
}

onMounted(loadData)

watch(pageSize, () => { currentPage.value = 1 })
</script>

<style scoped>
.page-container { padding: 20px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; color: #eef5ff; }
.page-header__actions { display: flex; gap: 8px; }
.page-filters { margin-bottom: 16px; padding: 16px; background: #132a45; border: 1px solid rgba(125, 163, 220, 0.18); border-radius: 12px; }
.pagination-container { margin-top: 16px; display: flex; justify-content: flex-end; }
:deep(.el-form-item__label) { color: rgba(205, 222, 248, 0.78) !important; }
:deep(.el-input__wrapper), :deep(.el-select__wrapper) { background: #0e233a !important; border: 1px solid rgba(125, 163, 220, 0.18) !important; box-shadow: none !important; }
:deep(.el-input__inner), :deep(.el-select__placeholder) { color: #eef5ff !important; }
:deep(.el-button) { background: #0e233a !important; border-color: rgba(125, 163, 220, 0.18) !important; color: #eef5ff !important; }
:deep(.el-button--primary) { background: #23a0fa !important; border-color: #23a0fa !important; color: #fff !important; }
:deep(.el-button:hover) { background: rgba(94, 162, 255, 0.15) !important; }
:deep(.el-table) { background: #132a45 !important; color: #eef5ff !important; --el-table-bg-color: #132a45 !important; --el-table-tr-bg-color: #132a45 !important; --el-table-header-bg-color: #0e233a !important; --el-table-text-color: #eef5ff !important; --el-table-header-text-color: rgba(205, 222, 248, 0.78) !important; --el-table-row-hover-bg-color: rgba(94, 162, 255, 0.08) !important; --el-table-border-color: rgba(125, 163, 220, 0.18) !important; }
:deep(.el-table th.el-table__cell) { background: #0e233a !important; color: rgba(205, 222, 248, 0.78) !important; border-bottom: 1px solid rgba(125, 163, 220, 0.18) !important; }
:deep(.el-table td.el-table__cell) { background: #132a45 !important; color: #eef5ff !important; border-bottom: 1px solid rgba(125, 163, 220, 0.18) !important; }
:deep(.el-table tr:hover > td.el-table__cell) { background: rgba(94, 162, 255, 0.08) !important; }
:deep(.el-pager li) { background: #0e233a !important; color: #eef5ff !important; border: 1px solid rgba(125, 163, 220, 0.18) !important; }
:deep(.el-pager li.is-active) { background: #23a0fa !important; color: #fff !important; }
:deep(.el-pagination .btn-prev), :deep(.el-pagination .btn-next) { background: #0e233a !important; color: #eef5ff !important; }
:deep(.el-pagination__total), :deep(.el-pagination__jump) { color: rgba(205, 222, 248, 0.78) !important; }
:deep(.el-tag) { background: rgba(94, 162, 255, 0.15) !important; border-color: rgba(94, 162, 255, 0.3) !important; color: #5ea2ff !important; }
:deep(.el-select__popper) { background: #132a45 !important; border: 1px solid rgba(125, 163, 220, 0.18) !important; }
:deep(.el-select__popper .el-select-dropdown__item) { color: #eef5ff !important; }
:deep(.el-select__popper .el-select-dropdown__item:hover) { background: rgba(94, 162, 255, 0.15) !important; }
:deep(.el-select__popper .el-select-dropdown__item.selected) { background: #23a0fa !important; color: #fff !important; }
</style>
