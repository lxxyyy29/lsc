<template>
  <view class="page">
    <view class="page-glow"></view>

    <view class="header-bar">
      <text class="header-title">组长工作台</text>
      <text class="header-sub">审核事件并派发下属网格员</text>
    </view>

    <view v-if="isLoading" class="state-card">
      <text class="state-title">加载中...</text>
    </view>

    <!-- 请求失败：不能与「暂无待办」混为一谈 -->
    <view v-else-if="loadError" class="state-card">
      <view class="state-icon">
        <text class="iconfont">⚠️</text>
      </view>
      <text class="state-title">{{ loadError }}</text>
      <text class="state-sub">请退出重进本页重试；若提示权限问题请联系管理员</text>
    </view>

    <view v-else-if="!events.length" class="state-card">
      <view class="state-icon">
        <text class="iconfont">📋</text>
      </view>
      <text class="state-title">暂无待办事件</text>
      <text class="state-sub">当前网格没有待你审核派发的事件</text>
    </view>

    <view v-else class="event-list">
      <view
        v-for="evt in events"
        :key="evt.id"
        class="event-card"
        :class="`event-card--${evt.urgency === 'RED' ? 'red' : evt.urgency === 'YELLOW' ? 'yellow' : 'green'}`"
        @click="openDispatch(evt)"
      >
        <view class="card-top">
          <view class="urgency-dot" :class="`urgency-dot--${evt.urgency || 'NONE'}`"></view>
          <text class="event-code">{{ evt.eventCode }}</text>
          <view class="status-badge">
            <text class="status-text">{{ evt.statusLabel }}</text>
          </view>
        </view>

        <text class="event-title">{{ evt.title }}</text>

        <view class="meta-row">
          <text class="meta-item">🏷 {{ evt.eventType || '未分类' }}</text>
          <text class="meta-item">⚡ {{ evt.urgencyLabel || '一般' }}</text>
        </view>
        <view class="meta-row">
          <text class="meta-item">📍 {{ evt.location || '无位置信息' }}</text>
        </view>
        <view class="meta-row">
          <text class="meta-item">🗓 {{ formatTime(evt.occurredAt) }}</text>
          <text class="meta-item" v-if="evt.gridName">🏠 {{ evt.gridName }}</text>
        </view>

        <view class="card-actions">
          <text class="action-hint">点击派发 ›</text>
        </view>
        <view class="card-glow" :class="`card-glow--${evt.urgency || 'GREEN'}`"></view>
      </view>
      <view class="list-footer">
        <text class="list-total">共 {{ events.length }} 条待办</text>
      </view>
      <view style="height: 40rpx;"></view>
    </view>

    <!-- 派单弹窗 -->
    <view v-if="showDispatch" class="dispatch-modal" @click="closeDispatch">
      <view class="dispatch-sheet" @click.stop="">
        <view class="sheet-header">
          <text class="sheet-title">组长派单</text>
          <text class="sheet-close" @click="closeDispatch">×</text>
        </view>

        <view v-if="dispatchLoading" class="sheet-loading">
          <text>加载派单信息...</text>
        </view>

        <template v-else-if="dispatchInfo">
          <!-- 事件信息 -->
          <view class="info-section">
            <view class="info-row">
              <text class="info-label">事件</text>
              <text class="info-value info-value--bold">{{ dispatchInfo.event?.title }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">状态</text>
              <text class="urgency-tag">{{ dispatchInfo.event?.statusLabel }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">紧急度</text>
              <text class="urgency-tag" :class="`urgency-tag--${dispatchInfo.event?.urgency}`">
                {{ dispatchInfo.event?.urgencyLabel }}
              </text>
            </view>
          </view>

          <!-- 事件具体信息：组长据此判断派给哪位下属（取不到不影响派单） -->
          <view v-if="eventDetail" class="detail-section">
            <text class="section-label">事件具体信息</text>

            <view class="detail-row">
              <text class="detail-key">事发时间</text>
              <text class="detail-val">{{ formatTime(eventDetail.occurredAt) || '—' }}</text>
            </view>
            <view v-if="eventDetail.reportUserName" class="detail-row">
              <text class="detail-key">上报人</text>
              <text class="detail-val">{{ eventDetail.reportUserName }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-key">事发地点</text>
              <text class="detail-val">{{ eventDetail.location || '—' }}</text>
            </view>

            <view v-if="eventDetail.description" class="detail-block">
              <text class="detail-key">上报描述</text>
              <text class="detail-desc">{{ eventDetail.description }}</text>
            </view>

            <view v-if="eventDetail.evidenceReferences && eventDetail.evidenceReferences.length" class="detail-block">
              <text class="detail-key">现场照片（{{ eventDetail.evidenceReferences.length }} 张，点击可放大）</text>
              <scroll-view class="photo-scroll" scroll-x>
                <view class="photo-list">
                  <image
                    v-for="(url, i) in eventDetail.evidenceReferences"
                    :key="i"
                    class="photo-thumb"
                    :src="url"
                    mode="aspectFill"
                    @click="previewEvidence(i)"
                  />
                </view>
              </scroll-view>
            </view>
          </view>

          <!-- 组长信息 -->
          <view v-if="dispatchInfo.leaderFound" class="leader-section">
            <view class="leader-badge">
              <text class="leader-badge-text">网格组长</text>
            </view>
            <text class="leader-name">{{ dispatchInfo.leader?.name }}</text>
            <text class="leader-position">{{ dispatchInfo.leader?.positionLabel }}</text>
          </view>
          <view v-else class="error-section">
            <text class="error-text">⚠️ {{ dispatchInfo.reason || '未找到网格组长' }}</text>
          </view>

          <!-- 下属选择 -->
          <view v-if="dispatchInfo.leaderFound" class="sub-section">
            <text class="section-label">选择下属网格员</text>
            <view class="sub-list">
              <view
                v-for="sub in dispatchInfo.subordinates"
                :key="sub.userId"
                class="sub-item"
                :class="{ 'sub-item--active': dispatchForm.assigneeUserId === Number(sub.userId) }"
                @click="dispatchForm.assigneeUserId = Number(sub.userId)"
              >
                <view class="sub-avatar">
                  <text class="sub-avatar-text">{{ sub.name.charAt(0) }}</text>
                </view>
                <view class="sub-info">
                  <text class="sub-name">{{ sub.name }}</text>
                  <text class="sub-pending">待办 {{ sub.pendingCount || 0 }} 条</text>
                </view>
                <view v-if="dispatchForm.assigneeUserId === Number(sub.userId)" class="sub-check">
                  <text class="check-icon">✓</text>
                </view>
              </view>
            </view>
            <view v-if="!dispatchInfo.subordinates.length" class="empty-sub">
              <text class="empty-sub-text">该网格暂无下属网格员</text>
            </view>
          </view>

          <!-- 备注 -->
          <view v-if="dispatchInfo.leaderFound" class="remark-section">
            <text class="section-label">派单备注</text>
            <textarea
              v-model="dispatchForm.remark"
              class="remark-input"
              placeholder="选填，如：请尽快处理"
              :maxlength="200"
            />
          </view>

          <!-- 确认按钮 -->
          <view v-if="dispatchInfo.leaderFound" class="action-section">
            <view class="btn btn-primary" :class="{ 'btn--disabled': !dispatchForm.assigneeUserId }" @click="confirmDispatch">
              <text class="btn-text">{{ dispatching ? '派单中...' : '确认派单' }}</text>
            </view>
          </view>
        </template>

        <template v-else>
          <view class="sheet-error">
            <text>无法加载派单信息</text>
          </view>
        </template>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getLeaderPendingEvents, getLeaderDispatchInfo, leaderDispatch, type LeaderPendingEvent, type LeaderDispatchInfo } from '../../src/api/workorder'
import { getEventDetail, type EventDetail } from '../../src/api/event'
import { ensureAuthenticated } from '../../src/uni/navigation'

const events = ref<LeaderPendingEvent[]>([])
const isLoading = ref(true)
const loadError = ref('')
const showDispatch = ref(false)
const dispatchLoading = ref(false)
const dispatching = ref(false)
const dispatchInfo = ref<LeaderDispatchInfo | null>(null)
// 事件具体信息（描述/现场照片/上报人/时间）：组长据此判断派给哪位下属
const eventDetail = ref<EventDetail | null>(null)
const dispatchForm = ref({ assigneeUserId: null as number | null, remark: '' })

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 16)
}

async function loadData() {
  isLoading.value = true
  loadError.value = ''
  try {
    events.value = await getLeaderPendingEvents()
  } catch (e: any) {
    // 请求失败不能静默成「暂无待办事件」：权限失效/网络异常会被误读成"没有待办"
    loadError.value = e?.message || '加载待办失败'
    events.value = []
  } finally {
    isLoading.value = false
  }
}

async function openDispatch(evt: LeaderPendingEvent) {
  showDispatch.value = true
  dispatchInfo.value = null
  eventDetail.value = null
  dispatchForm.value = { assigneeUserId: null, remark: '' }
  dispatchLoading.value = true
  try {
    dispatchInfo.value = await getLeaderDispatchInfo(evt.id)
    const subs = dispatchInfo.value?.subordinates || []
    if (subs.length) {
      dispatchForm.value.assigneeUserId = Number(subs[0].userId)
    }
    // 附加事件具体信息，让组长看清情况再决定派给谁；取不到（无权限/接口异常）不影响派单
    eventDetail.value = (await getEventDetail(evt.id).catch(() => undefined)) ?? null
  } catch (e) {
    console.error('加载派单信息失败:', e)
    showDispatch.value = false
  } finally {
    dispatchLoading.value = false
  }
}

/** 现场照片点击放大（小程序与 H5 均由 uni.previewImage 承担） */
function previewEvidence(index: number) {
  const urls = eventDetail.value?.evidenceReferences || []
  if (!urls.length) return
  uni.previewImage({ urls, current: urls[index] || urls[0] })
}

function closeDispatch() {
  if (dispatching.value) return
  showDispatch.value = false
  dispatchInfo.value = null
}

async function confirmDispatch() {
  if (!dispatchForm.value.assigneeUserId) {
    // 原先这里静默 return，用户只看到"点了没反应"
    uni.showToast?.({ title: '请先选择下属网格员', icon: 'none' })
    return
  }
  const evtId = dispatchInfo.value?.event?.id
  if (!evtId) {
    uni.showToast?.({ title: '未获取到事件信息，请重新进入', icon: 'none' })
    return
  }
  dispatching.value = true
  try {
    await leaderDispatch(evtId, dispatchForm.value.assigneeUserId, dispatchForm.value.remark)
    uni.showToast?.({ title: '派单成功', icon: 'success' })
    showDispatch.value = false
    await loadData()
  } catch (e) {
    // 失败原因（如「事件已派发为工单」）由 http 层统一弹出提示，这里仅保留日志便于排查
    console.error('组长派单失败:', e)
  } finally {
    dispatching.value = false
  }
}

// 每次进入页面都重新拉取：Web 端管理员可代为「组长派单」，若沿用上次的旧列表，
// 组长会看到已被派发的事件，点「确认派单」只会得到「事件已派发为工单」
onShow(() => {
  if (!ensureAuthenticated('/pages/leader/pending')) return
  loadData()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #030913;
  padding: 24rpx 28rpx;
  box-sizing: border-box;
}

.page-glow {
  position: fixed;
  top: -200rpx;
  right: -100rpx;
  width: 600rpx;
  height: 600rpx;
  background: radial-gradient(circle, rgba(24,144,255,0.15) 0%, transparent 70%);
  pointer-events: none;
}

.header-bar {
  margin-bottom: 28rpx;
}

.header-title {
  font-size: 44rpx;
  font-weight: 700;
  color: #fff;
  display: block;
}

.header-sub {
  font-size: 26rpx;
  color: #8db0d0;
  margin-top: 8rpx;
  display: block;
}

.state-card {
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 20rpx;
  padding: 60rpx 40rpx;
  text-align: center;
}

.state-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.state-title {
  font-size: 30rpx;
  color: #fff;
  display: block;
}

.state-sub {
  font-size: 24rpx;
  color: #8db0d0;
  margin-top: 10rpx;
  display: block;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.event-card {
  position: relative;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 20rpx;
  padding: 28rpx;
  overflow: hidden;
}

.event-card--red {
  border-color: rgba(255,77,79,0.4);
}
.event-card--yellow {
  border-color: rgba(250,173,20,0.4);
}
.event-card--green {
  border-color: rgba(82,196,26,0.4);
}

.card-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4rpx;
}
.card-glow--RED { background: linear-gradient(90deg, #ff4d4f, transparent); }
.card-glow--YELLOW { background: linear-gradient(90deg, #faad14, transparent); }
.card-glow--GREEN { background: linear-gradient(90deg, #52c41a, transparent); }

.card-top {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.urgency-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}
.urgency-dot--RED { background: #ff4d4f; box-shadow: 0 0 12rpx #ff4d4f; }
.urgency-dot--YELLOW { background: #faad14; box-shadow: 0 0 12rpx #faad14; }
.urgency-dot--GREEN, .urgency-dot--NONE { background: #52c41a; box-shadow: 0 0 12rpx #52c41a; }

.event-code {
  font-size: 24rpx;
  color: #8db0d0;
  flex: 1;
}

.status-badge {
  background: rgba(24,144,255,0.2);
  border: 1px solid rgba(24,144,255,0.4);
  border-radius: 8rpx;
  padding: 6rpx 16rpx;
}
.status-text {
  font-size: 22rpx;
  color: #57b9ff;
}

.event-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
  display: block;
  margin-bottom: 16rpx;
  line-height: 1.5;
}

.meta-row {
  display: flex;
  gap: 20rpx;
  margin-bottom: 8rpx;
}

.meta-item {
  font-size: 24rpx;
  color: #8db0d0;
}

.card-actions {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1px solid rgba(255,255,255,0.08);
}

.action-hint {
  font-size: 24rpx;
  color: #57b9ff;
}

.list-footer {
  padding: 24rpx 0;
  text-align: center;
}

.list-total {
  font-size: 24rpx;
  color: #8db0d0;
}

/* 派单弹窗 */
.dispatch-modal {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.6);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.dispatch-sheet {
  width: 100%;
  background: #0d1b2a;
  border-radius: 32rpx 32rpx 0 0;
  padding: 32rpx 28rpx 60rpx;
  max-height: 85vh;
  overflow-y: auto;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
}

.sheet-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
}

.sheet-close {
  font-size: 48rpx;
  color: #8db0d0;
  padding: 12rpx;
}

.sheet-loading, .sheet-error {
  text-align: center;
  padding: 60rpx;
  color: #8db0d0;
  font-size: 28rpx;
}

.info-section {
  background: rgba(255,255,255,0.06);
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.info-row:last-child { margin-bottom: 0; }

.info-label {
  font-size: 26rpx;
  color: #8db0d0;
}

.info-value {
  font-size: 26rpx;
  color: #fff;
}
.info-value--bold {
  font-weight: 600;
  max-width: 60%;
  text-align: right;
}

/* 事件具体信息（描述 / 现场照片 / 上报人 / 时间） */
.detail-section {
  background: rgba(255,255,255,0.06);
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.detail-key {
  font-size: 26rpx;
  color: #8db0d0;
  flex-shrink: 0;
}

.detail-val {
  font-size: 26rpx;
  color: #fff;
  flex: 1;
  text-align: right;
  word-break: break-all;
}

.detail-block {
  margin-top: 16rpx;
}

.detail-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #cfe3f5;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
}

.photo-scroll {
  margin-top: 12rpx;
  white-space: nowrap;
}

.photo-list {
  display: flex;
  gap: 12rpx;
}

.photo-thumb {
  width: 160rpx;
  height: 160rpx;
  border-radius: 12rpx;
  border: 1px solid rgba(255,255,255,0.12);
  flex-shrink: 0;
}

.urgency-tag {
  font-size: 24rpx;
  color: #fff;
  background: rgba(24,144,255,0.3);
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}
.urgency-tag--RED { background: rgba(255,77,79,0.3); }
.urgency-tag--YELLOW { background: rgba(250,173,20,0.3); }
.urgency-tag--GREEN { background: rgba(82,196,26,0.3); }

.leader-section {
  background: rgba(24,144,255,0.15);
  border: 1px solid rgba(24,144,255,0.3);
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.leader-badge {
  background: #1890ff;
  border-radius: 8rpx;
  padding: 6rpx 16rpx;
}
.leader-badge-text {
  font-size: 22rpx;
  color: #fff;
}

.leader-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
}

.leader-position {
  font-size: 24rpx;
  color: #57b9ff;
}

.error-section {
  background: rgba(255,77,79,0.15);
  border: 1px solid rgba(255,77,79,0.3);
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}
.error-text {
  font-size: 26rpx;
  color: #ff7875;
}

.sub-section {
  margin-bottom: 20rpx;
}

.section-label {
  font-size: 26rpx;
  color: #8db0d0;
  margin-bottom: 16rpx;
  display: block;
}

.sub-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.sub-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 16rpx;
  padding: 20rpx;
}

.sub-item--active {
  border-color: #1890ff;
  background: rgba(24,144,255,0.1);
}

.sub-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #1890ff, #096dd9);
  display: flex;
  align-items: center;
  justify-content: center;
}
.sub-avatar-text {
  font-size: 32rpx;
  color: #fff;
  font-weight: 600;
}

.sub-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.sub-name {
  font-size: 28rpx;
  color: #fff;
  font-weight: 500;
}

.sub-pending {
  font-size: 24rpx;
  color: #8db0d0;
}

.sub-check {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.check-icon {
  font-size: 28rpx;
  color: #fff;
}

.empty-sub {
  padding: 40rpx;
  text-align: center;
}
.empty-sub-text {
  font-size: 26rpx;
  color: #8db0d0;
}

.remark-section {
  margin-bottom: 20rpx;
}

.remark-input {
  width: 100%;
  min-height: 120rpx;
  background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  color: #fff;
  box-sizing: border-box;
}

.action-section {
  margin-top: 32rpx;
}

.btn {
  border-radius: 12rpx;
  padding: 24rpx;
  text-align: center;
}

.btn-primary {
  background: #1890ff;
}

.btn--disabled {
  background: rgba(24,144,255,0.4);
}

.btn-text {
  font-size: 30rpx;
  color: #fff;
  font-weight: 500;
}
</style>
