<template>
  <view class="page" :style="{ paddingTop: statusBarPadding }">
    <ResidentBackBar />
    <view class="header">
      <text class="header-title">📋 我的上报</text>
      <text class="header-sub">查看上报记录和处理进度</text>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in reports" :key="item.id" class="card" @click="openDetail(item)">
        <view class="card-top">
          <text class="title">{{ item.title }}</text>
          <text :class="['status', statusClass(item.status)]">{{ statusLabel(item.status) }}</text>
        </view>
        <text class="desc">{{ item.description }}</text>
        <view class="card-bottom">
          <text class="time">{{ item.createdAt }}</text>
          <text class="code">查询码：{{ item.eventCode || item.id }}</text>
        </view>
        <!-- 办结后评价 -->
        <view v-if="item.status === 'CLOSED'" class="rating-section">
          <view v-if="item.rating" class="rating-display">
            <view class="stars">
              <text v-for="n in 5" :key="n" :class="['star', n <= item.rating ? 'star-active' : '']">★</text>
            </view>
            <text class="rating-text">{{ item.ratingText || '已评价' }}</text>
          </view>
          <view v-else class="rating-action">
            <text class="rating-label">请评价：</text>
            <view class="stars-input">
              <text v-for="n in 5" :key="n" class="star-input" @click.stop="rateItem(item, n)">★</text>
            </view>
          </view>
        </view>
      </view>
      <text v-if="!reports.length" class="empty">暂无上报记录</text>
    </view>

    <!-- 上报详情 / 修改（缺陷 BUG-12：上报后看不到更详细信息、也无法修改） -->
    <view v-if="detailItem" class="sheet-mask" @click.self="closeDetail">
      <view class="sheet">
        <view class="sheet-head">
          <text class="sheet-title">上报详情</text>
          <text class="sheet-close" @click="closeDetail">×</text>
        </view>

        <view class="sheet-body">
          <view class="row"><text class="row-label">当前状态</text><text class="row-value">{{ statusLabel(detailItem.status) }}</text></view>
          <view class="row"><text class="row-label">查询码</text><text class="row-value">{{ detailItem.eventCode || detailItem.id }}</text></view>
          <view class="row"><text class="row-label">上报时间</text><text class="row-value">{{ detailItem.createdAt }}</text></view>
          <view v-if="detailItem.rating" class="row"><text class="row-label">我的评价</text><text class="row-value">{{ detailItem.rating }} 分</text></view>

          <view class="field">
            <text class="row-label">问题标题</text>
            <input v-if="canEdit" v-model="editForm.title" class="edit-input" placeholder="请输入标题" />
            <text v-else class="field-text">{{ detailItem.title || '-' }}</text>
          </view>
          <view class="field">
            <text class="row-label">问题描述</text>
            <textarea v-if="canEdit" v-model="editForm.description" class="edit-textarea" placeholder="请输入问题描述" />
            <text v-else class="field-text">{{ detailItem.description || '-' }}</text>
          </view>
          <view class="field">
            <text class="row-label">详细地址</text>
            <input v-if="canEdit" v-model="editForm.address" class="edit-input" placeholder="请输入详细地址" />
            <text v-else class="field-text">{{ detailItem.address || '-' }}</text>
          </view>
          <text v-if="!canEdit" class="sheet-tip">事件已进入处置流程，如需补充信息请联系工作人员</text>
        </view>

        <view class="sheet-actions">
          <button v-if="canEdit" class="btn-primary" :disabled="saving" @click="saveEdit">{{ saving ? '保存中...' : '保存修改' }}</button>
          <button v-else class="btn-default" @click="closeDetail">关闭</button>
        </view>
      </view>
    </view>

    <ResidentTabBar current="/pages/resident/history/index" />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useStatusBar } from '../../../src/utils/useStatusBar'
import ResidentBackBar from '../../../src/components/ResidentBackBar.vue'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getMyReports, rateEvent, updateMyReport } from '../../../src/api/resident'

// ===== 上报详情 / 修改（缺陷 BUG-12）=====
const detailItem = ref<any>(null)
const saving = ref(false)
const editForm = reactive({ title: '', description: '', address: '' })

// 与后端 PUT /events/my-reports/{id} 的允许范围保持一致：进入处置流程后仅可查看
const EDITABLE_STATUSES = ['PENDING', 'PENDING_AUDIT', 'IN_AUDIT', 'WAITING_DISPATCH']
const canEdit = computed(() => !!detailItem.value && EDITABLE_STATUSES.includes(String(detailItem.value.status)))

function openDetail(item: any) {
  detailItem.value = item
  editForm.title = item.title || ''
  editForm.description = item.description || ''
  editForm.address = item.address || ''
}

function closeDetail() {
  detailItem.value = null
}

async function saveEdit() {
  if (!detailItem.value) return
  if (!editForm.title.trim()) {
    uni.showToast({ title: '请填写问题标题', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await updateMyReport(Number(detailItem.value.id), {
      title: editForm.title.trim(),
      description: editForm.description.trim(),
      address: editForm.address.trim()
    })
    // 就地回写列表，避免整页重载
    detailItem.value.title = editForm.title.trim()
    detailItem.value.description = editForm.description.trim()
    detailItem.value.address = editForm.address.trim()
    uni.showToast({ title: '修改成功', icon: 'success' })
    closeDetail()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '修改失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

const { statusBarPadding } = useStatusBar()

const loading = ref(false)
const reports = ref<any[]>([])

function statusLabel(status: string) {
  const map: any = {
    PENDING: '待处理',
    WAITING_DISPATCH: '待派单',
    WAITING_LEADER_REVIEW: '组长审核',
    DISPATCHED_TO_WORK_ORDER: '处理中',
    PROCESSING: '处理中',
    WAITING_CLOSE_CONFIRM: '待确认',
    CLOSED: '已办结',
    IGNORED: '已忽略',
    REJECTED: '已驳回'
  }
  return map[status] || status || '未知'
}
function statusClass(status: string) {
  if (status === 'CLOSED') return 'status-green'
  if (status === 'DISPATCHED_TO_WORK_ORDER' || status === 'PROCESSING' || status === 'WAITING_CLOSE_CONFIRM') return 'status-blue'
  if (status === 'REJECTED' || status === 'IGNORED') return 'status-red'
  return 'status-orange'
}

async function rateItem(item: any, rating: number) {
  try {
    await rateEvent(item.id, { rating, comment: '' })
    item.rating = rating
    item.ratingText = ['', '很不满意', '不满意', '一般', '满意', '非常满意'][rating]
  } catch (e: any) {
    uni.showToast({ title: e?.message || '评价失败', icon: 'none' })
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getMyReports()
    reports.value = res?.items || res?.data?.items || []
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

onLoad(() => { loadData() })
</script>

<style scoped>
.page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
/* 上报详情 / 修改弹层（BUG-12） */
.sheet-mask { position: fixed; top: 0; right: 0; bottom: 0; left: 0; background: rgba(15,23,42,0.5); display: flex; align-items: flex-end; z-index: 999; }
.sheet { width: 100%; max-height: 80vh; overflow-y: auto; padding: 28rpx; border-radius: 24rpx 24rpx 0 0; background: #fff; }
.sheet-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.sheet-title { font-size: 32rpx; font-weight: 700; color: #1f2937; }
.sheet-close { font-size: 40rpx; line-height: 1; color: #9ca3af; padding: 0 8rpx; }
.sheet-body { display: grid; gap: 16rpx; }
.row { display: flex; justify-content: space-between; gap: 16rpx; font-size: 26rpx; }
.row-label { color: #6b7280; flex-shrink: 0; }
.row-value { color: #1f2937; text-align: right; word-break: break-all; }
.field { display: grid; gap: 8rpx; }
.field-text { font-size: 26rpx; color: #374151; line-height: 1.7; }
.edit-input { width: 100%; height: 76rpx; padding: 0 20rpx; border: 1px solid #d1d5db; border-radius: 12rpx; font-size: 28rpx; box-sizing: border-box; }
.edit-textarea { width: 100%; min-height: 150rpx; padding: 16rpx 20rpx; border: 1px solid #d1d5db; border-radius: 12rpx; font-size: 28rpx; box-sizing: border-box; }
.sheet-tip { font-size: 24rpx; color: #9ca3af; }
.sheet-actions { margin-top: 24rpx; }
.sheet-actions .btn-primary { height: 84rpx; line-height: 84rpx; border-radius: 14rpx; font-size: 30rpx; background: linear-gradient(135deg, #52c41a, #389e0d); color: #fff; }
.sheet-actions .btn-default { height: 84rpx; line-height: 84rpx; border-radius: 14rpx; font-size: 30rpx; background: #f3f4f6; color: #4b5563; }
.header {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.header-sub { font-size: 26rpx; opacity: 0.8; }
.loading { text-align: center; padding: 80rpx; color: #9ca3af; }
.card {
  background: #fff; border-radius: 24rpx; padding: 32rpx; margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.title { font-size: 28rpx; font-weight: 600; flex: 1; margin-right: 16rpx; }
.status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; flex-shrink: 0; }
.status-green { background: #f6ffed; color: #52c41a; }
.status-blue { background: #e6f7ff; color: #1890ff; }
.status-orange { background: #fff7e6; color: #fa8c16; }
.status-red { background: #fff1f0; color: #ff4d4f; }
.desc { font-size: 26rpx; color: #6b7280; margin-bottom: 16rpx; display: block; line-height: 1.5; }
.card-bottom { display: flex; justify-content: space-between; font-size: 22rpx; color: #9ca3af; }
.empty { text-align: center; padding: 80rpx; color: #9ca3af; display: block; }
.rating-section { margin-top: 24rpx; padding-top: 24rpx; border-top: 1px solid #f3f4f6; }
.rating-display { display: flex; align-items: center; gap: 16rpx; }
.stars { display: flex; gap: 4rpx; }
.star { font-size: 32rpx; color: #d1d5db; }
.star-active { color: #faad14; }
.rating-text { font-size: 24rpx; color: #6b7280; }
.rating-action { display: flex; align-items: center; gap: 16rpx; }
.rating-label { font-size: 24rpx; color: #6b7280; }
.stars-input { display: flex; gap: 8rpx; }
.star-input { font-size: 48rpx; color: #d1d5db; padding: 0 4rpx; }
.star-input:active { color: #faad14; }
</style>
