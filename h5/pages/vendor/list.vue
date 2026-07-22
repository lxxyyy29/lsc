<template>
  <view class="page">
    <!-- Search bar -->
    <view class="search-bar">
      <AppIcon name="search" size="32rpx" class="search-icon" />
      <input
        class="search-input"
        v-model="keyword"
        placeholder="搜索摊贩名称或法人姓名"
        placeholder-class="search-placeholder"
      />
      <text v-if="keyword" class="search-clear" @click="keyword = ''">✕</text>
    </view>

    <!-- Status filter chips -->
    <view class="filter-row">
      <view
        v-for="opt in statusOptions"
        :key="opt.value"
        class="filter-chip"
        :class="{ 'filter-chip--active': statusFilter === opt.value }"
        @click="statusFilter = opt.value"
      >
        <text>{{ opt.label }}</text>
      </view>
    </view>

    <!-- List -->
    <view v-if="filteredList.length" class="card-list">
      <view
        v-for="item in filteredList"
        :key="item.id"
        class="vendor-card"
        @click="openDetail(item.id)"
      >
        <view class="card-left">
          <view class="photo-thumb">
            <image v-if="item.vendorPhotoUrl" :src="toImageUrl(item.vendorPhotoUrl)" class="thumb-img" mode="aspectFill" />
            <AppIcon v-else name="gavel" size="32rpx" class="thumb-icon" />
          </view>
        </view>
        <view class="card-body">
          <view class="card-head">
            <text class="vendor-name">{{ item.vendorName }}</text>
            <text class="status-badge" :class="item.status === 'ACTIVE' ? 'badge--active' : 'badge--disabled'">
              {{ item.status === 'ACTIVE' ? '启用' : '停用' }}
            </text>
          </view>
          <view v-if="item.legalPersonName" class="info-row">
            <AppIcon name="profile" size="22rpx" class="info-icon" />
            <text class="info-text">{{ item.legalPersonName }}</text>
            <text v-if="item.legalPersonPhone" class="info-phone">{{ item.legalPersonPhone }}</text>
          </view>
          <view v-if="item.remark" class="info-row">
            <text class="info-remark">{{ item.remark }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Empty state -->
    <view v-else-if="!loading" class="empty-state">
      <text class="empty-title">暂无摊贩数据</text>
      <text class="empty-desc">{{ keyword || statusFilter ? '调整筛选条件后重试' : '点击右下角按钮新增摊贩' }}</text>
    </view>

    <!-- Loading -->
    <view v-if="loading" class="loading-state">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- Floating add button -->
    <view v-if="canCreate" class="fab" @click="openCreate">
      <AppIcon name="plus" size="36rpx" class="fab-icon" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import AppIcon from '../../src/components/AppIcon.vue'
import { listVendors, type VendorItem } from '../../src/api/vendor'
import { ensureAuthenticated, navigateToPath } from '../../src/uni/navigation'
import { hasButtonPermission } from '../../src/auth/permissions'
import { toImageUrl, fetchAccessPrefix } from '../../src/api/upload'

const allList = ref<VendorItem[]>([])
const loading = ref(false)
const loadError = ref(false)
const keyword = ref('')
const statusFilter = ref('')

const statusOptions = [
  { label: '全部', value: '' },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' }
]

const canCreate = computed(() => hasButtonPermission('button:h5:vendor:create'))

const filteredList = computed(() => {
  let list = allList.value
  if (statusFilter.value) {
    list = list.filter((item) => item.status === statusFilter.value)
  }
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(
      (item) =>
        item.vendorName.toLowerCase().includes(kw) ||
        (item.legalPersonName?.toLowerCase().includes(kw) ?? false)
    )
  }
  return list
})

async function loadData() {
  loading.value = true
  loadError.value = false
  try {
    allList.value = await listVendors()
  } catch {
    loadError.value = true
    allList.value = []
  } finally {
    loading.value = false
  }
}

function openDetail(id: number) {
  navigateToPath(`/vendors/${id}`)
}

function openCreate() {
  navigateToPath('/vendors/create')
}

onShow(async () => {
  if (!ensureAuthenticated('/vendors')) return
  await fetchAccessPrefix().catch(() => {})
  await loadData()
})

onPullDownRefresh(async () => {
  await loadData()
  uni.stopPullDownRefresh()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 0 228rpx;
  background: linear-gradient(180deg, #060f18 0%, #030913 100%);
  color: #eef6ff;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin: 16rpx 24rpx;
  padding: 20rpx 22rpx;
  border-radius: 16rpx;
  background: rgba(13, 30, 50, 0.9);
  border: 1px solid rgba(125, 163, 220, 0.12);
}

.search-icon {
  color: #8ba1b4;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  font-size: 32rpx;
  color: #eef6ff;
  background: transparent;
  border: none;
}

.search-placeholder {
  color: #5e7488;
}

.search-clear {
  color: #5e7488;
  font-size: 32rpx;
  padding: 4rpx 8rpx;
}

.filter-row {
  display: flex;
  gap: 14rpx;
  padding: 0 24rpx 20rpx;
}

.filter-chip {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  background: rgba(13, 30, 50, 0.8);
  border: 1px solid rgba(125, 163, 220, 0.12);
  font-size: 28rpx;
  color: #8ba1b4;
}

.filter-chip--active {
  background: rgba(56, 152, 253, 0.16);
  border-color: rgba(56, 152, 253, 0.4);
  color: #5ea2ff;
}

.card-list {
  display: grid;
  gap: 16rpx;
  padding: 0 24rpx;
}

.vendor-card {
  display: flex;
  gap: 20rpx;
  padding: 22rpx 20rpx;
  border-radius: 18rpx;
  background: rgba(13, 30, 50, 0.9);
  border: 1px solid rgba(125, 163, 220, 0.12);
  box-shadow: 0 8rpx 24rpx rgba(3, 10, 20, 0.2);
}

.card-left {
  flex-shrink: 0;
}

.photo-thumb {
  width: 88rpx;
  height: 88rpx;
  border-radius: 14rpx;
  background: rgba(20, 40, 65, 0.9);
  border: 1px solid rgba(125, 163, 220, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.thumb-icon {
  color: #5e7488;
}

.card-body {
  flex: 1;
  display: grid;
  gap: 10rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.vendor-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #f3f8ff;
  flex: 1;
  margin-right: 12rpx;
}

.status-badge {
  flex-shrink: 0;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.badge--active {
  background: rgba(31, 190, 166, 0.14);
  color: #1fbea6;
  border: 1px solid rgba(31, 190, 166, 0.28);
}

.badge--disabled {
  background: rgba(239, 68, 68, 0.12);
  color: #ef8888;
  border: 1px solid rgba(239, 68, 68, 0.24);
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.info-icon {
  color: #5e7488;
  flex-shrink: 0;
}

.info-text {
  font-size: 26rpx;
  color: #8ba1b4;
}

.info-phone {
  font-size: 26rpx;
  color: #5ea2ff;
  margin-left: 8rpx;
}

.info-remark {
  font-size: 26rpx;
  color: #5e7488;
}

.empty-state,
.loading-state {
  display: grid;
  gap: 12rpx;
  padding: 80rpx 40rpx;
  text-align: center;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #eef6ff;
}

.empty-desc,
.loading-text {
  font-size: 26rpx;
  color: #5e7488;
}

.error-banner {
  display: block;
  margin: 16rpx 24rpx;
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  background: rgba(117, 32, 48, 0.34);
  color: #ffd7de;
  font-size: 26rpx;
}

.fab {
  position: fixed;
  right: 40rpx;
  bottom: 100rpx;
  width: 96rpx;
  height: 96rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #3898fd, #2272d9);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(56, 152, 253, 0.4);
}

.fab-icon {
  color: #fff;
}
</style>