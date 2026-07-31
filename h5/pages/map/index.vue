<template>
  <view class="map-container">
    <!-- 地图 -->
    <map
      class="map"
      :latitude="centerLat"
      :longitude="centerLng"
      :markers="markers"
      :polylines="polylines"
      :show-location="true"
      :scale="scale"
      @markertap="onMarkerTap"
      @regionchange="onRegionChange"
    ></map>

    <!-- 顶部信息栏 -->
    <view class="map-header">
      <text class="header-title">移动 GIS</text>
      <text class="header-sub">{{ markers.length }} 个事件</text>
    </view>

    <!-- 定位按钮 -->
    <view class="locate-btn" @click="locateMe">
      <text class="locate-icon">📍</text>
    </view>

    <!-- 缩放控制 -->
    <view class="zoom-controls">
      <view class="zoom-btn" @click="scale = Math.min(18, scale + 1)">+</view>
      <view class="zoom-btn" @click="scale = Math.max(5, scale - 1)">-</view>
    </view>

    <!-- 底部事件详情 -->
    <view v-if="selectedEvent" class="event-detail">
      <view class="event-header">
        <text class="event-title">{{ selectedEvent.title }}</text>
        <text class="event-close" @click="selectedEvent = null">×</text>
      </view>
      <view class="event-info">
        <text class="event-type">{{ selectedEvent.eventType || '未分类' }}</text>
        <text class="event-status" :class="'status-' + (selectedEvent.status || '').toLowerCase()">
          {{ statusLabel(selectedEvent.status) }}
        </text>
      </view>
      <text class="event-address">{{ selectedEvent.address || '未知位置' }}</text>
      <text class="event-time">{{ formatTime(selectedEvent.createdAt) }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const centerLat = ref(22.9712)
const centerLng = ref(113.9395)
const scale = ref(13)
const markers = ref<any[]>([])
const polylines = ref<any[]>([])
const selectedEvent = ref<any>(null)

// 加载周边事件
async function loadEvents() {
  try {
    const res: any = await uni.request({
      url: '/api/events/map-points',
      method: 'GET',
      header: {
        'Authorization': `Bearer ${uni.getStorageSync('h5-token') || ''}`
      }
    })

    if (res.data && res.data.code === 'OK') {
      const points = res.data.data || []
      markers.value = points.map((p: any, idx: number) => ({
        id: idx,
        latitude: p.lat,
        longitude: p.lng,
        title: p.title || '',
        iconPath: getMarkerIcon(p.status),
        width: 30,
        height: 30,
        callout: {
          content: p.title || '',
          display: 'BYCLICK'
        },
        // 自定义数据
        _data: p
      }))
    }
  } catch (e) {
    console.error('加载事件失败:', e)
  }
}

function getMarkerIcon(status: string) {
  if (status === 'CLOSED') return '/static/marker-green.png'
  if (status === 'PENDING_AUDIT') return '/static/marker-red.png'
  return '/static/marker-blue.png'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    'PENDING_AUDIT': '待审核',
    'IN_AUDIT': '审核中',
    'WAITING_DISPATCH': '待派单',
    'DISPATCHED_TO_WORK_ORDER': '已派单',
    'CLOSED': '已关闭'
  }
  return map[status] || status || '未知'
}

function formatTime(time: any) {
  if (!time) return ''
  try {
    const d = new Date(time)
    return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch { return time }
}

// 点击标记
function onMarkerTap(e: any) {
  const markerId = e.detail?.markerId ?? e.markerId
  const marker = markers.value.find(m => m.id === markerId)
  if (marker && marker._data) {
    selectedEvent.value = marker._data
  }
}

// 地图区域变化
function onRegionChange(e: any) {
  // 可以在这里实现按需加载可见区域内的事件
}

// 定位到当前位置
function locateMe》 {
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      centerLat.value = res.latitude
      centerLng.value = res.longitude
      scale.value = 15
    },
    fail: () => {
      uni.showToast({ title: '定位失败', icon: 'none' })
    }
  })
}

onMounted(() => {
  loadEvents()
})
</script>

<style lang="scss" scoped>
.map-container {
  position: relative;
  width: 100%;
  height: 100vh;
}

.map {
  width: 100%;
  height: 100%;
}

.map-header {
  position: absolute;
  top: 80rpx;
  left: 30rpx;
  right: 30rpx;
  background: rgba(6, 18, 31, 0.85);
  border-radius: 16rpx;
  padding: 20rpx 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  backdrop-filter: blur(10rpx);
}

.header-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #ffffff;
}

.header-sub {
  font-size: 24rpx;
  color: #8db0d0;
}

.locate-btn {
  position: absolute;
  bottom: 200rpx;
  right: 30rpx;
  width: 80rpx;
  height: 80rpx;
  background: rgba(6, 18, 31, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.3);
}

.locate-icon {
  font-size: 36rpx;
}

.zoom-controls {
  position: absolute;
  bottom: 300rpx;
  right: 30rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.zoom-btn {
  width: 64rpx;
  height: 64rpx;
  background: rgba(6, 18, 31, 0.9);
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 36rpx;
  font-weight: 600;
}

.event-detail {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(6, 18, 31, 0.95);
  border-radius: 24rpx 24rpx 0 0;
  padding: 30rpx;
  backdrop-filter: blur(10rpx);
}

.event-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.event-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
  flex: 1;
}

.event-close {
  font-size: 40rpx;
  color: #8db0d0;
  padding: 0 10rpx;
}

.event-info {
  display: flex;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.event-type {
  font-size: 22rpx;
  color: #57b9ff;
  background: rgba(87, 185, 255, 0.15);
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.event-status {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.status-pending_audit { color: #ff5252; background: rgba(255, 82, 82, 0.15); }
.status-in_audit { color: #ff9800; background: rgba(255, 152, 0, 0.15); }
.status-waiting_dispatch { color: #ffc107; background: rgba(255, 193, 7, 0.15); }
.status-dispatched_to_work_order { color: #007bff; background: rgba(0, 123, 255, 0.15); }
.status-closed { color: #28a745; background: rgba(40, 167, 69, 0.15); }

.event-address {
  display: block;
  font-size: 24rpx;
  color: #8db0d0;
  margin-bottom: 8rpx;
}

.event-time {
  display: block;
  font-size: 22rpx;
  color: #6c757d;
}
</style>
