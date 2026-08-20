<template>
  <view class="page" :style="{ paddingTop: statusBarPadding }">
    <ResidentBackBar />
    <view class="header">
      <text class="header-title">📋 政策查询</text>
      <text class="header-sub">惠民政策，一查便知</text>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="p in policies" :key="p.id" class="card" @click="showDetail(p)">
        <view class="card-title">{{ p.title }}</view>
        <view class="card-meta">
          <text class="tag tag-blue">{{ policyTypeLabel(p.policyType) }}</text>
          <text class="time">{{ p.publishDate || p.createdAt }}</text>
        </view>
        <text class="desc">{{ p.description }}</text>
      </view>
      <text v-if="!policies.length" class="empty">暂无政策</text>
    </view>

    <!-- 政策详情弹窗 -->
    <view v-if="selected" class="modal-overlay" @click.self="selected = null">
      <view class="modal-box">
        <text class="modal-title">{{ selected.title }}</text>
        <view class="detail-tags">
          <text class="tag tag-blue">{{ policyTypeLabel(selected.policyType) }}</text>
          <text v-if="selected.policyCode" class="code">编号：{{ selected.policyCode }}</text>
        </view>
        <view v-if="selected.description" class="section">
          <text class="section-label">政策简介</text>
          <text class="section-text">{{ selected.description }}</text>
        </view>
        <view v-if="selected.eligibility" class="section">
          <text class="section-label">申请条件</text>
          <text class="section-text">{{ selected.eligibility }}</text>
        </view>
        <view v-if="selected.tags" class="section">
          <text class="section-label">标签</text>
          <text class="section-text">{{ selected.tags }}</text>
        </view>
        <view class="modal-actions">
          <button @click="selected = null" class="btn-primary">关闭</button>
        </view>
      </view>
    </view>

    <ResidentTabBar current="/pages/resident/policies/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getResidentPolicies } from '../../../src/api/resident'
import { useStatusBar } from '../../../src/utils/useStatusBar'
import ResidentBackBar from '../../../src/components/ResidentBackBar.vue'

const { statusBarPadding } = useStatusBar()

const loading = ref(false)
const policies = ref<any[]>([])
const selected = ref<any>(null)

function policyTypeLabel(t: string) {
  const map: any = {
    LOW_INCOME: '低保救助', ELDERLY: '养老服务', RESCUE: '临时救助',
    MEDICAL: '医疗救助', BENEFIT: '福利政策', EDUCATION: '教育资助',
    EMPLOYMENT: '就业扶持', HOUSING: '住房保障', OTHER: '其他'
  }
  return map[t] || t || '政策'
}

async function loadData(query?: Record<string, string | undefined>) {
  loading.value = true
  try {
    policies.value = await getResidentPolicies() || []
    // 从通知跳转进入时携带政策 ID，自动打开对应详情
    const targetId = query?.id ? Number(query.id) : null
    if (targetId) {
      const target = policies.value.find((p: any) => Number(p.id) === targetId)
      if (target) selected.value = target
    }
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

function showDetail(p: any) {
  selected.value = p
}

onLoad((query) => loadData(query))
</script>

<style scoped>
.page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.header {
  background: linear-gradient(135deg, #13c2c2 0%, #08979c 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.header-sub { font-size: 26rpx; opacity: 0.8; }
.card {
  background: #fff; border-radius: 24rpx; padding: 32rpx; margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.card-title { font-size: 28rpx; font-weight: 600; margin-bottom: 16rpx; }
.card-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; }
.tag-blue { background: #e6fffb; color: #13c2c2; }
.time { font-size: 22rpx; color: #9ca3af; }
.desc { font-size: 26rpx; color: #6b7280; display: block; line-height: 1.5; }
.loading { text-align: center; padding: 80rpx; color: #9ca3af; }
.empty { text-align: center; padding: 80rpx; color: #9ca3af; font-size: 26rpx; display: block; }
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-box {
  background: #fff; border-radius: 24rpx; padding: 40rpx; width: 90%; max-width: 800rpx;
  max-height: 80vh; overflow-y: auto;
}
.modal-title { font-size: 32rpx; font-weight: 600; display: block; margin-bottom: 16rpx; }
.detail-tags { display: flex; gap: 16rpx; align-items: center; margin-bottom: 32rpx; }
.code { font-size: 22rpx; color: #9ca3af; }
.section { margin-bottom: 28rpx; }
.section-label { font-size: 26rpx; font-weight: 600; color: #374151; display: block; margin-bottom: 8rpx; }
.section-text { font-size: 26rpx; color: #6b7280; line-height: 1.6; display: block; }
.modal-actions { display: flex; justify-content: flex-end; margin-top: 32rpx; }
.btn-primary {
  padding: 20rpx 40rpx; background: #13c2c2; color: #fff; border: none;
  border-radius: 12rpx; font-size: 28rpx; line-height: 1.5;
}
</style>
