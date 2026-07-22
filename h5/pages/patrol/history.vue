<template>
  <view class="container">
    <view class="hero-card">
      <view class="hero-title">巡查记录</view>
      <view class="hero-subtitle">我的巡查打卡历史</view>
    </view>
    <view v-if="records.length" class="record-list">
      <view v-for="record in records" :key="record.id" class="record-card">
        <view class="record-header">
          <text class="record-time">{{ record.createdAt }}</text>
          <text class="record-status">{{ record.status === 'NORMAL' ? '正常' : '异常' }}</text>
        </view>
        <text class="record-content">{{ record.content || '无描述' }}</text>
        <text v-if="record.address" class="record-address">{{ record.address }}</text>
      </view>
    </view>
    <view v-else class="empty-text">暂无巡查记录</view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPatrolRecords, PatrolRecord } from '../../src/api/community'

const records = ref<PatrolRecord[]>([])

onMounted(async () => {
  try { records.value = await getPatrolRecords() } catch (e) { console.error(e) }
})
</script>

<style scoped>
.container { padding: 20px; background: #030913; min-height: 100vh; }
.hero-card { background: linear-gradient(135deg, #0a2a4a, #0d3866); border-radius: 16px; padding: 20px; margin-bottom: 16px; }
.hero-title { font-size: 22px; font-weight: bold; color: #eaf5ff; }
.hero-subtitle { font-size: 13px; color: #7ea4c8; margin-top: 4px; }
.record-list { display: flex; flex-direction: column; gap: 12px; }
.record-card { background: #0e233a; border-radius: 12px; padding: 16px; }
.record-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.record-time { font-size: 12px; color: #7ea4c8; }
.record-status { font-size: 12px; color: #57b9ff; }
.record-content { font-size: 14px; color: #eaf5ff; }
.record-address { font-size: 12px; color: #7ea4c8; margin-top: 4px; display: block; }
.empty-text { text-align: center; color: #7ea4c8; padding: 40px; }
</style>
