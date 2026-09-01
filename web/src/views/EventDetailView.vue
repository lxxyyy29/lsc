<template>
  <div>
    <div v-if="loading" style="text-align:center;padding:60px;color:#9ca3af;">
      <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
      <p style="margin-top:12px;">加载中...</p>
    </div>

    <div v-else-if="event">
      <!-- 顶部：只读信息，无任何功能按钮 -->
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
        <div>
          <h2 style="font-size:20px;font-weight:600;">{{ event.title }}</h2>
          <div style="display:flex;align-items:center;gap:8px;margin-top:6px;flex-wrap:wrap;">
            <span :class="['tag', event.urgencyLevel === 'RED' ? 'tag-red' : event.urgencyLevel === 'YELLOW' ? 'tag-orange' : 'tag-green']">
              {{ event.urgencyLevel === 'RED' ? '紧急' : event.urgencyLevel === 'YELLOW' ? '重点' : '一般' }}
            </span>
            <span class="tag tag-blue">{{ statusLabel(event.currentStatus) }}</span>
            <span v-if="event.archived" class="tag" style="background:#f3f4f6;color:#6b7280;border:1px solid #d1d5db;">已归档</span>
            <span v-if="event.deleted === 1" class="tag" style="background:#f3f4f6;color:#dc2626;border:1px solid #fca5a5;">已删除</span>
            <span style="font-size:12px;color:#9ca3af;">{{ event.eventCode }}</span>
          </div>
        </div>
        <button @click="handleBack" :title="embedded ? '关闭' : '返回'" style="width:32px;height:32px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:18px;line-height:1;color:#6b7280;cursor:pointer;display:flex;align-items:center;justify-content:center;">×</button>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
        <!-- 左侧：基本信息 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">基本信息</h3>
          <div style="font-size:13px;color:#374151;line-height:2;">
            <div><span style="color:#9ca3af;">事件类型：</span>{{ getEventTypeName(event.eventType) }}</div>
            <div><span style="color:#9ca3af;">发生时间：</span>{{ event.occurredAt || '-' }}</div>
            <div><span style="color:#9ca3af;">事发地点：</span>{{ event.location || '-' }}</div>
            <div><span style="color:#9ca3af;">所属网格：</span>{{ event.gridName || '-' }}</div>
            <div><span style="color:#9ca3af;">上报人：</span>{{ event.reportUserName || '-' }}<span v-if="event.reportSource" style="color:#9ca3af;margin-left:4px;">（{{ getReportSourceName(event.reportSource) }}）</span></div>
            <div v-if="event.reportPhone"><span style="color:#9ca3af;">联系电话：</span>{{ event.reportPhone }}</div>
            <div><span style="color:#9ca3af;">来源系统：</span>{{ getSourceSystemName(event.sourceSystem) }}</div>
            <div v-if="event.deleted === 1" style="margin-top:8px;padding-top:8px;border-top:1px solid #f3f4f6;">
              <span style="color:#9ca3af;">删除原因：</span>{{ event.deletedReason || '-' }}
            </div>
            <div v-if="event.description" style="margin-top:8px;padding-top:8px;border-top:1px solid #f3f4f6;"><span style="color:#9ca3af;">详细描述：</span><br/>{{ event.description }}</div>
            <!-- 现场照片（image 预览模式：点击打开全屏预览浮层，不跳转链接） -->
            <div v-if="event.evidenceReferences && event.evidenceReferences.length" style="margin-top:8px;padding-top:8px;border-top:1px solid #f3f4f6;">
              <span style="color:#9ca3af;">现场照片（{{ event.evidenceReferences.length }}）：</span>
              <div style="display:flex;flex-wrap:wrap;gap:8px;margin-top:8px;">
                <img v-for="(url, idx) in event.evidenceReferences" :key="idx" :src="url" @click="previewIdx = Number(idx)" title="点击预览大图" style="width:72px;height:72px;object-fit:cover;border-radius:6px;border:1px solid #e5e7eb;cursor:zoom-in;" />
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：生命周期时间轴 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">处置时间轴</h3>
          <div v-if="timeline.length" style="position:relative;padding-left:20px;">
            <div style="position:absolute;left:7px;top:0;bottom:0;width:2px;background:#e5e7eb;"></div>
            <div v-for="(item, idx) in timeline" :key="idx" style="position:relative;margin-bottom:16px;">
              <div style="position:absolute;left:-17px;top:4px;width:10px;height:10px;border-radius:50%;background:#1890ff;border:2px solid #fff;"></div>
              <div style="font-size:13px;font-weight:600;color:#374151;">{{ item.action }}</div>
              <div style="font-size:12px;color:#6b7280;margin-top:2px;">{{ item.remark }}</div>
              <div style="font-size:11px;color:#9ca3af;margin-top:2px;">{{ item.occurredAt }}</div>
            </div>
          </div>
          <p v-else style="font-size:12px;color:#9ca3af;text-align:center;padding:20px;">暂无操作记录</p>
        </div>
      </div>

      <!-- 现场照片预览浮层 -->
      <div v-if="previewIdx !== null && event.evidenceReferences?.length" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.85);display:flex;align-items:center;justify-content:center;z-index:10001;" @click.self="previewIdx = null">
        <button @click="previewIdx = null" title="关闭" style="position:absolute;top:20px;right:24px;width:36px;height:36px;border-radius:50%;border:none;background:rgba(255,255,255,0.2);color:#fff;font-size:20px;cursor:pointer;">×</button>
        <button v-if="event.evidenceReferences.length > 1" @click="previewIdx = (previewIdx - 1 + event.evidenceReferences.length) % event.evidenceReferences.length" title="上一张" style="position:absolute;left:24px;top:50%;transform:translateY(-50%);width:40px;height:40px;border-radius:50%;border:none;background:rgba(255,255,255,0.2);color:#fff;font-size:20px;cursor:pointer;">‹</button>
        <img :src="event.evidenceReferences[previewIdx]" style="max-width:80vw;max-height:85vh;object-fit:contain;border-radius:8px;" />
        <button v-if="event.evidenceReferences.length > 1" @click="previewIdx = (previewIdx + 1) % event.evidenceReferences.length" title="下一张" style="position:absolute;right:24px;top:50%;transform:translateY(-50%);width:40px;height:40px;border-radius:50%;border:none;background:rgba(255,255,255,0.2);color:#fff;font-size:20px;cursor:pointer;">›</button>
        <span style="position:absolute;bottom:24px;left:50%;transform:translateX(-50%);color:#fff;font-size:13px;">{{ previewIdx + 1 }} / {{ event.evidenceReferences.length }}</span>
      </div>
    </div>

    <div v-else style="text-align:center;padding:60px;color:#9ca3af;">事件不存在</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEventDetail, getEventTimeline } from '../api'
import { getEventTypeName, getReportSourceName, getSourceSystemName } from '../utils/eventTypes'

const route = useRoute()
const router = useRouter()
// embedded=true 时作为弹窗内嵌组件：事件 id 由 props 传入，纯只读展示
const props = withDefaults(defineProps<{ embedded?: boolean; eventId?: string | number | null }>(), { embedded: false, eventId: null })
const emit = defineEmits<{ (e: 'close'): void }>()
// 弹窗模式优先用 props 传入的 id，页面模式回退到路由参数
const eventId = computed(() => props.eventId ?? route.params.id)

const event = ref<any>(null)
const timeline = ref<any[]>([])
const loading = ref(true)
// 现场照片预览：非 null 时打开全屏预览浮层，值为当前预览下标
const previewIdx = ref<number | null>(null)

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核',
    IN_AUDIT: '审核中',
    AUDIT_APPROVED: '已通过',
    AUDIT_REJECTED: '已驳回',
    WAITING_DISPATCH: '待派单',
    WAITING_LEADER_REVIEW: '组长审核',
    DISPATCHED_TO_WORK_ORDER: '已派单',
    CLOSED: '已关闭',
    IGNORED: '已忽略'
  }
  return map[status] || status
}

async function loadData() {
  loading.value = true
  try {
    // 支持数字 id 与字符串 externalEventId 两种参数
    const idParam = String(eventId.value || '')
    event.value = await getEventDetail(idParam)
    try {
      timeline.value = await getEventTimeline(idParam) || []
    } catch (e) {
      timeline.value = []
    }
  } catch (e) {
    event.value = null
  } finally {
    loading.value = false
  }
}

function handleBack() {
  if (props.embedded) {
    emit('close')
  } else {
    router.back()
  }
}

onMounted(loadData)
// 事件 id 变化时重新加载
watch(eventId, loadData)
</script>
