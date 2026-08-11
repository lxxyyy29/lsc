<template>
  <view class="page">
    <view class="profile-card">
      <view class="profile-main">
        <view class="avatar-box"><text>{{ avatarText }}</text></view>
        <view class="profile-copy">
          <view class="name-row">
            <text class="name-text">{{ displayName }}</text>
            <text class="status-pill">在线</text>
          </view>
          <text class="id-text">ID：{{ accountName }}</text>
        </view>
      </view>
    </view>

    <view class="quick-grid">
      <view v-if="showMerchants" class="quick-card" @click="goMerchants">
        <view class="quick-icon"><AppIcon name="briefcase" size="24rpx" /></view>
        <text class="quick-title">商户管理</text>
        <text class="quick-copy">查看商户信息</text>
      </view>
      <view v-if="showVendors" class="quick-card" @click="goVendors">
        <view class="quick-icon"><AppIcon name="gavel" size="24rpx" /></view>
        <text class="quick-title">摊贩管理</text>
        <text class="quick-copy">查看摊贩信息</text>
      </view>
    </view>

    <button class="logout-btn" @click="handleLogout">退出系统登录</button>

    <GridWorkerTabBar current="/pages/mine/index" />
  </view>
</template>

<script setup lang="ts">
import GridWorkerTabBar from '../../src/components/GridWorkerTabBar.vue'
import { computed, ref } from 'vue'
import AppIcon from '../../src/components/AppIcon.vue'
import { onShow } from '@dcloudio/uni-app'
import { getH5Session, logoutH5, type H5Session } from '../../src/api/auth'
import { ensureAuthenticated, navigateToPath, redirectToPath } from '../../src/uni/navigation'
import { hasMenuPermission } from '../../src/auth/permissions'

const session = ref<H5Session | null>(null)
const showMerchants = computed(() => hasMenuPermission('menu:h5:merchant:view'))
const showVendors = computed(() => hasMenuPermission('menu:h5:vendor:view'))

onShow(() => {
  if (!ensureAuthenticated('/mine')) return
  session.value = getH5Session()
})

const displayName = computed(() => session.value?.userName ?? '未登录')
const accountName = computed(() => session.value?.account ?? '--')
const avatarText = computed(() => displayName.value.trim().slice(-1) || '我')
function goMerchants() { navigateToPath('/merchants') }
function goVendors() { navigateToPath('/vendors') }
async function handleLogout() {
  try { await logoutH5() } finally { uni.reLaunch({ url: '/pages/role-select/index' }) }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 20rpx 208rpx;
  background:
    radial-gradient(circle at top, rgba(25, 77, 124, 0.3) 0, rgba(25, 77, 124, 0) 38%),
    #081421;
  color: #eef6ff;
}

.profile-main,
.name-row,
.quick-grid {
  display: flex;
  align-items: center;
}

.name-text,
.quick-title {
  color: #f3f8ff;
}

.profile-card,
.quick-card {
  border-radius: 16rpx;
  background: linear-gradient(180deg, rgba(18, 32, 49, 0.98) 0%, rgba(13, 25, 38, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.08);
  box-shadow: 0 16rpx 32rpx rgba(3, 11, 20, 0.24);
}

.profile-card {
  position: relative;
  padding: 20rpx;
  margin-bottom: 22rpx;
  overflow: hidden;
}

.profile-card::after {
  content: '';
  position: absolute;
  right: -8rpx;
  top: -18rpx;
  width: 96rpx;
  height: 96rpx;
  background: url('../../src/assets/login-hex.svg') center/contain no-repeat;
  opacity: 0.16;
}

.profile-main {
  gap: 18rpx;
}

.avatar-box {
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  background: linear-gradient(180deg, #1d3853 0%, #112639 100%);
  color: #eef6ff;
  font-size: 38rpx;
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(143, 217, 255, 0.08);
}

.profile-copy {
  display: grid;
  gap: 8rpx;
}

.name-row {
  gap: 12rpx;
}

.name-text {
  font-size: 34rpx;
  font-weight: 700;
}

.status-pill {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: rgba(111, 202, 255, 0.14);
  color: #d7f0ff;
  font-size: 22rpx;
}

.quick-copy,
.id-text {
  color: rgba(214, 225, 239, 0.72);
  font-size: 24rpx;
}

.quick-grid {
  gap: 16rpx;
  margin: 26rpx 0 22rpx;
}

.quick-card {
  flex: 1;
  display: grid;
  gap: 10rpx;
  padding: 18rpx;
}

.quick-icon {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  background: #1b2c40;
  color: #eef6ff;
}

.quick-icon text {
  font-size: 28rpx;
}

.quick-title {
  font-size: 28rpx;
  font-weight: 700;
}

.logout-btn {
  width: 100%;
  height: 82rpx;
  line-height: 82rpx;
  margin-top: 28rpx;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 125, 125, 0.4);
  color: #ffd4d4;
  font-size: 28rpx;
}
</style>

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>