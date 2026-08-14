<template>
  <view class="page">
    <view class="page-glow"></view>

    <!-- 顶部：全部已读 -->
    <view class="top-bar" v-if="items.length">
      <text class="top-title">消息通知</text>
      <view class="read-all" @click="doReadAll">
        <text class="read-all-text">全部已读</text>
      </view>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="empty-card">
      <text class="empty-title">加载中...</text>
    </view>

    <!-- 空状态 -->
    <view v-else-if="!items.length" class="empty-card">
      <text class="empty-emoji">🔔</text>
      <text class="empty-title">暂无通知</text>
      <text class="empty-sub">应急调度指令、预警提醒会推送到这里</text>
    </view>

    <!-- 通知列表 -->
    <view v-else class="notice-list">
      <view
        v-for="item in items"
        :key="item.id"
        class="notice-card"
        :class="{ 'notice-card--unread': item.isRead === 0 }"
        @click="openNotice(item)"
      >
        <view class="notice-top">
          <view class="notice-title-row">
            <view v-if="item.isRead === 0" class="unread-dot"></view>
            <text class="notice-title">{{ item.title }}</text>
          </view>
          <text class="notice-time">{{ formatTime(item.createdAt) }}</text>
        </view>
        <text class="notice-content">{{ item.content }}</text>
        <view class="notice-bottom">
          <view v-if="item.level === 'URGENT'" class="level-tag level-tag--urgent">
            <text class="level-tag-text">紧急</text>
          </view>
          <view v-if="item.type === 'EMERGENCY'" class="level-tag level-tag--emergency">
            <text class="level-tag-text">应急指令</text>
          </view>
          <text class="notice-go">查看详情 ›</text>
        </view>
      </view>

      <view v-if="hasMore" class="load-more" @click="loadMore">
        <text class="load-more-text">{{ loadingMore ? '加载中…' : '加载更多' }}</text>
      </view>
      <view v-else class="load-more">
        <text class="load-more-text">已到底部</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { getNotices, markAllNoticesRead, markNoticeRead, type NoticeItem } from '../../src/api/notice'
import { navigateToPath } from '../../src/uni/navigation'

const items = ref<NoticeItem[]>([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)

function formatTime(t?: string | null) {
  if (!t) return ''
  const s = String(t).replace('T', ' ').slice(0, 16)
  // 今天只显示时分
  const today = new Date()
  const day = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  if (s.startsWith(day)) return s.slice(11)
  return s.slice(5)
}

async function load(reset = true) {
  if (reset) {
    loading.value = true
    page.value = 1
  } else {
    loadingMore.value = true
  }
  try {
    const res = await getNotices(page.value, 20)
    items.value = reset ? res.items : [...items.value, ...res.items]
    total.value = res.total
    hasMore.value = items.value.length < res.total
    if (!reset) page.value += 1
  } catch (e) {
    console.error('加载通知失败', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  if (!loadingMore.value && hasMore.value) load(false)
}

async function openNotice(item: NoticeItem) {
  try {
    if (item.isRead === 0) await markNoticeRead(item.id)
    item.isRead = 1
  } catch (e) {
    /* 标记失败不阻塞跳转 */
  }
  // 应急指令通知 → 跳转指令详情
  if (item.relatedType === 'EMERGENCY' && item.relatedId) {
    navigateToPath(`/pages/emergency/detail?id=${item.relatedId}`)
  }
}

async function doReadAll() {
  try {
    await markAllNoticesRead()
    items.value.forEach((it) => (it.isRead = 1))
  } catch (e) {
    console.error('全部已读失败', e)
  }
}

onMounted(() => load())
onShow(() => {
  if (items.value.length) load()
})
onReachBottom(loadMore)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4fb 0%, #f6f9fc 100%);
  padding: 20rpx 24rpx 40rpx;
  position: relative;
  overflow: hidden;
}

.page-glow {
  position: absolute;
  top: -160rpx;
  right: -120rpx;
  width: 420rpx;
  height: 420rpx;
  background: radial-gradient(circle, rgba(2, 132, 199, 0.16) 0%, rgba(2, 132, 199, 0) 70%);
  border-radius: 50%;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 8rpx 20rpx;
}

.top-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #0f172a;
}

.read-all {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  background: #fff;
  border: 1rpx solid #e2e8f0;
}

.read-all-text {
  font-size: 24rpx;
  color: #0284c7;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 40rpx;
  gap: 14rpx;
}

.empty-emoji { font-size: 72rpx; }

.empty-title { font-size: 30rpx; font-weight: 600; color: #334155; }

.empty-sub { font-size: 24rpx; color: #94a3b8; }

.notice-list { display: flex; flex-direction: column; gap: 16rpx; }

.notice-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx 26rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.05);
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.notice-card--unread {
  background: #f0f9ff;
  border: 1rpx solid #bae6fd;
}

.notice-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
}

.notice-title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  flex: 1;
}

.unread-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #dc2626;
  flex-shrink: 0;
}

.notice-title {
  font-size: 29rpx;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.4;
}

.notice-time {
  font-size: 22rpx;
  color: #94a3b8;
  flex-shrink: 0;
}

.notice-content {
  font-size: 25rpx;
  color: #475569;
  line-height: 1.6;
}

.notice-bottom {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 4rpx;
}

.level-tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
}

.level-tag--urgent { background: #fee2e2; }
.level-tag--emergency { background: #fef3c7; }

.level-tag-text { font-size: 20rpx; color: #b91c1c; }

.level-tag--emergency .level-tag-text { color: #b45309; }

.notice-go {
  margin-left: auto;
  font-size: 23rpx;
  color: #0284c7;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 24rpx 0;
}

.load-more-text { font-size: 24rpx; color: #94a3b8; }
</style>
