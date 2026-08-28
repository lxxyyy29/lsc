<template>
  <view class="subject-selector">
    <!-- Selected display -->
    <view v-if="subjectName" class="selected-bar">
      <view class="selected-info">
        <text class="selected-label">{{ subjectType === 'MERCHANT' ? '商户' : '摊贩' }}</text>
        <text class="selected-name">{{ subjectName }}</text>
      </view>
      <view class="selected-clear" @click="clearSelection">
        <text class="clear-icon">×</text>
      </view>
    </view>

    <!-- Tab switcher -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="activeTab === 'MERCHANT' ? 'tab-item--active' : ''"
        @click="switchTab('MERCHANT')"
      >
        <text class="tab-text">商户</text>
      </view>
      <view
        class="tab-item"
        :class="activeTab === 'VENDOR' ? 'tab-item--active' : ''"
        @click="switchTab('VENDOR')"
      >
        <text class="tab-text">摊贩</text>
      </view>
    </view>

    <!-- Search box -->
    <view class="search-box">
      <text class="search-icon-text">🔍</text>
      <input
        v-model.trim="keyword"
        class="search-input"
        :placeholder="activeTab === 'MERCHANT' ? '搜索商户名称...' : '搜索摊贩名称...'"
        placeholder-class="search-placeholder"
      />
      <view v-if="keyword" class="search-clear" @click="keyword = ''">
        <text class="search-clear-text">×</text>
      </view>
    </view>

    <!-- Loading state -->
    <view v-if="isLoading" class="list-state">
      <text class="list-state-text">加载中...</text>
    </view>

    <!-- Empty state -->
    <view v-else-if="filteredList.length === 0" class="list-state">
      <text class="list-state-text">{{ keyword ? '无匹配结果' : '暂无数据' }}</text>
    </view>

    <!-- List -->
    <scroll-view v-else scroll-y class="list-scroll">
      <view
        v-for="item in filteredList"
        :key="item.id"
        class="list-item"
        :class="isSelected(item.id) ? 'list-item--selected' : ''"
        @click="selectItem(item)"
      >
        <view class="list-item-body">
          <text class="list-item-name">{{ item.name }}</text>
          <text v-if="item.extra" class="list-item-extra">{{ item.extra }}</text>
        </view>
        <view v-if="isSelected(item.id)" class="list-item-check">
          <text class="check-icon">✓</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listMerchants } from '../api/merchant'
import { listVendors } from '../api/vendor'

interface SelectableItem {
  id: number
  name: string
  extra?: string
}

const props = defineProps<{
  subjectType?: 'MERCHANT' | 'VENDOR' | null
  subjectId?: number | null
  subjectName?: string | null
}>()

const emit = defineEmits<{
  'update:subjectType': [value: 'MERCHANT' | 'VENDOR' | null]
  'update:subjectId': [value: number | null]
  'update:subjectName': [value: string | null]
}>()

const activeTab = ref<'MERCHANT' | 'VENDOR'>('MERCHANT')
const keyword = ref('')
const isLoading = ref(false)
const loadError = ref(false)

const merchants = ref<SelectableItem[]>([])
const vendors = ref<SelectableItem[]>([])

const currentList = computed<SelectableItem[]>(() =>
  activeTab.value === 'MERCHANT' ? merchants.value : vendors.value
)

const filteredList = computed<SelectableItem[]>(() => {
  const kw = keyword.value.toLowerCase()
  if (!kw) return currentList.value
  return currentList.value.filter((item) => item.name.toLowerCase().includes(kw))
})

function isSelected(id: number): boolean {
  return props.subjectId === id && props.subjectType === activeTab.value
}

function switchTab(tab: 'MERCHANT' | 'VENDOR') {
  activeTab.value = tab
  keyword.value = ''
}

function selectItem(item: SelectableItem) {
  if (isSelected(item.id)) {
    // Deselect on second click
    clearSelection()
    return
  }
  emit('update:subjectType', activeTab.value)
  emit('update:subjectId', item.id)
  emit('update:subjectName', item.name)
}

function clearSelection() {
  emit('update:subjectType', null)
  emit('update:subjectId', null)
  emit('update:subjectName', null)
}

onMounted(async () => {
  isLoading.value = true
  loadError.value = false
  try {
    const [merchantList, vendorList] = await Promise.all([listMerchants(), listVendors()])
    merchants.value = merchantList.map((m) => ({
      id: m.id,
      name: m.merchantName,
      extra: m.areaName ?? undefined
    }))
    vendors.value = vendorList.map((v) => ({
      id: v.id,
      name: v.vendorName,
      extra: v.legalPersonName ?? undefined
    }))
  } catch {
    loadError.value = true
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.subject-selector {
  display: grid;
  gap: 12rpx;
}

/* Selected bar */
.selected-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 18rpx;
  border-radius: 12rpx;
  background: rgba(94, 162, 255, 0.12);
  border: 1px solid rgba(94, 162, 255, 0.35);
}

.selected-info {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex: 1;
  min-width: 0;
}

.selected-label {
  font-size: 22rpx;
  padding: 3rpx 10rpx;
  border-radius: 6rpx;
  background: rgba(94, 162, 255, 0.2);
  color: #a8d0ff;
  flex-shrink: 0;
}

.selected-name {
  font-size: 26rpx;
  color: #eef6ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-clear {
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.clear-icon {
  font-size: 34rpx;
  color: rgba(214, 225, 239, 0.6);
  line-height: 1;
}

/* Tab bar */
.tab-bar {
  display: flex;
  gap: 0;
  border-radius: 12rpx;
  overflow: hidden;
  background: rgba(7, 18, 31, 0.78);
  border: 1px solid rgba(118, 189, 255, 0.1);
}

.tab-item {
  flex: 1;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-item--active {
  background: rgba(94, 162, 255, 0.18);
  border-bottom: 2px solid #5ea2ff;
}

.tab-text {
  font-size: 28rpx;
  font-weight: 600;
  color: rgba(214, 225, 239, 0.6);
}

.tab-item--active .tab-text {
  color: #a8d0ff;
}

/* Search box */
.search-box {
  height: 64rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 0 16rpx;
  border-radius: 10rpx;
  background: rgba(7, 18, 31, 0.78);
  border: 1px solid rgba(118, 189, 255, 0.1);
}

.search-icon-text {
  font-size: 26rpx;
  opacity: 0.5;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #eaf3fd;
}

.search-placeholder {
  color: rgba(214, 225, 239, 0.4);
  font-size: 26rpx;
}

.search-clear {
  width: 32rpx;
  height: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-clear-text {
  font-size: 32rpx;
  color: rgba(214, 225, 239, 0.5);
  line-height: 1;
}

/* List states */
.list-state {
  padding: 24rpx;
  text-align: center;
}

.list-state-text {
  font-size: 26rpx;
  color: rgba(214, 225, 239, 0.5);
}

.list-state--error .list-state-text {
  color: rgba(255, 160, 160, 0.7);
}

/* Scroll list */
.list-scroll {
  max-height: 400rpx;
  border-radius: 12rpx;
  overflow: hidden;
  border: 1px solid rgba(118, 189, 255, 0.08);
}

/* List items */
.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 18rpx;
  border-bottom: 1px solid rgba(118, 189, 255, 0.06);
  background: rgba(10, 22, 36, 0.9);
}

.list-item:last-child {
  border-bottom: none;
}

.list-item--selected {
  background: rgba(94, 162, 255, 0.1);
}

.list-item-body {
  display: grid;
  gap: 4rpx;
  flex: 1;
  min-width: 0;
}

.list-item-name {
  font-size: 28rpx;
  color: #eef6ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-item--selected .list-item-name {
  color: #a8d0ff;
}

.list-item-extra {
  font-size: 22rpx;
  color: rgba(214, 225, 239, 0.5);
}

.list-item-check {
  width: 36rpx;
  height: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.check-icon {
  font-size: 28rpx;
  color: #5ea2ff;
  font-weight: 700;
}
</style>
