<template>
  <view class="page">
    <view class="header">
      <text class="page-title">商户管理</text>
      <text class="page-sub">共 {{ filtered.length }} 家 · 点击可查看详情</text>
    </view>

    <view class="search-row">
      <input
        class="search-input"
        v-model.trim="keyword"
        placeholder="搜索商户名称 / 负责人"
        placeholder-class="search-ph"
      />
      <text v-if="keyword" class="search-clear" @click="keyword = ''">×</text>
    </view>

    <view v-if="isLoading" class="tip">加载中...</view>
    <view v-else-if="loadError" class="tip tip--error" @click="load">加载失败，点击重试</view>
    <view v-else-if="!filtered.length" class="tip">暂无商户数据</view>

    <view v-else class="list">
      <view v-for="m in filtered" :key="m.id" class="card" @click="openDetail(m)">
        <view class="card-head">
          <text class="card-title">{{ m.merchantName }}</text>
          <text class="card-status">{{ m.status === 'ACTIVE' ? '营业中' : '已停用' }}</text>
        </view>
        <text class="card-meta">
          负责人：{{ m.legalPersonName || '-' }}<text v-if="m.legalPersonPhone"> · {{ m.legalPersonPhone }}</text>
        </text>
        <text class="card-meta">所属辖区：{{ m.areaName || '未匹配' }}</text>
      </view>
    </view>

    <!-- 详情弹层：原占位页点击无任何反馈，现直接展示商户详情字段 -->
    <view v-if="detail" class="sheet-mask" @click.self="detail = null">
      <view class="sheet">
        <view class="sheet-head">
          <text class="sheet-title">{{ detail.merchantName }}</text>
          <text class="sheet-close" @click="detail = null">×</text>
        </view>
        <view class="sheet-body">
          <view class="row"><text class="row-label">负责人</text><text class="row-value">{{ detail.legalPersonName || '-' }}</text></view>
          <view class="row"><text class="row-label">联系电话</text><text class="row-value">{{ detail.legalPersonPhone || '-' }}</text></view>
          <view class="row"><text class="row-label">所属辖区</text><text class="row-value">{{ detail.areaName || '未匹配' }}</text></view>
          <view class="row"><text class="row-label">状态</text><text class="row-value">{{ detail.status === 'ACTIVE' ? '营业中' : '已停用' }}</text></view>
          <view class="row">
            <text class="row-label">经纬度</text>
            <text class="row-value">{{ detail.longitude && detail.latitude ? detail.longitude + ', ' + detail.latitude : '未采集' }}</text>
          </view>
          <view class="row"><text class="row-label">备注</text><text class="row-value">{{ detail.remark || '-' }}</text></view>
          <view class="row"><text class="row-label">更新时间</text><text class="row-value">{{ detail.updatedAt || '-' }}</text></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listMerchants, type MerchantItem } from '../../src/api/merchant'
import { ensureAuthenticated } from '../../src/uni/navigation'

const merchants = ref<MerchantItem[]>([])
const keyword = ref('')
const isLoading = ref(true)
const loadError = ref(false)
const detail = ref<MerchantItem | null>(null)

const filtered = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return merchants.value
  return merchants.value.filter(
    (m) => (m.merchantName || '').includes(kw) || (m.legalPersonName || '').includes(kw)
  )
})

async function load() {
  isLoading.value = true
  loadError.value = false
  try {
    merchants.value = await listMerchants()
  } catch (e) {
    loadError.value = true
    merchants.value = []
  } finally {
    isLoading.value = false
  }
}

function openDetail(item: MerchantItem) {
  detail.value = item
}

onShow(() => {
  if (!ensureAuthenticated('/merchants')) return
  void load()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 208rpx;
  background: #081421;
  color: #eef6ff;
}

.header {
  margin-bottom: 18rpx;
}

.page-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #f3f8ff;
}

.page-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.7);
}

.search-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.search-input {
  flex: 1;
  height: 72rpx;
  padding: 0 22rpx;
  border-radius: 14rpx;
  background: rgba(20, 40, 65, 0.8);
  color: #eef6ff;
  font-size: 28rpx;
}

.search-ph {
  color: #5e7488;
}

.search-clear {
  width: 56rpx;
  text-align: center;
  font-size: 36rpx;
  color: rgba(214, 225, 239, 0.6);
}

.tip {
  padding: 80rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: rgba(214, 225, 239, 0.7);
}

.tip--error {
  color: #fca5a5;
}

.list {
  display: grid;
  gap: 14rpx;
}

.card {
  display: grid;
  gap: 8rpx;
  padding: 22rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, rgba(18, 32, 49, 0.98), rgba(13, 25, 38, 0.98));
  border: 1px solid rgba(118, 189, 255, 0.08);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.card-title {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #f3f8ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-status {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #8ddcff;
}

.card-meta {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.7);
}

.sheet-mask {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: rgba(2, 8, 16, 0.6);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.sheet {
  width: 100%;
  max-height: 76vh;
  overflow-y: auto;
  padding: 26rpx 26rpx 40rpx;
  border-radius: 24rpx 24rpx 0 0;
  background: #0d1a28;
  border-top: 1px solid rgba(118, 189, 255, 0.12);
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.sheet-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #f3f8ff;
}

.sheet-close {
  font-size: 40rpx;
  line-height: 1;
  color: rgba(214, 225, 239, 0.6);
  padding: 0 8rpx;
}

.sheet-body {
  display: grid;
  gap: 14rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  font-size: 26rpx;
}

.row-label {
  color: rgba(214, 225, 239, 0.6);
  flex-shrink: 0;
}

.row-value {
  color: #eef6ff;
  text-align: right;
  word-break: break-all;
}
</style>

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>
