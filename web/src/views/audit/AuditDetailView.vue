<template>
  <PageContainer title="审核详情">
    <NotFoundState
      v-if="!detail"
      title="未找到审核记录"
      description="当前审核记录不存在或关联事件缺失，请返回审核列表重新选择。"
    />

    <section v-else class="audit-detail-page" data-testid="web-detail-page-template">
      <section class="detail-hero panel">
        <div>
          <p>审核中心 / 详情页</p>
          <h3>{{ detail.eventTitle }}</h3>
          <span>{{ detail.eventCode }}</span>
        </div>
        <StatusTag :status="auditStatusTag" />
      </section>

      <section class="detail-top-grid">
        <section class="detail-card panel">
          <header class="detail-card__header">
            <div>
              <p>审核总览</p>
              <h3>审核摘要</h3>
            </div>
            <span>{{ detail.selectedTemplateVersion }}</span>
          </header>
          <dl class="detail-summary-grid">
            <div v-for="item in summaryItems" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>
                <StatusTag v-if="item.status" :status="item.status" />
                <template v-else>{{ item.value }}</template>
              </dd>
            </div>
          </dl>
        </section>

        <section class="detail-card panel">
          <header class="detail-card__header">
            <div>
              <p>取证附件</p>
              <h3>证据</h3>
            </div>
            <span>{{ detail.eventDetail.evidenceReferences.length }} 项</span>
          </header>
          <ul class="evidence-list">
            <li v-for="evidence in detail.eventDetail.evidenceReferences" :key="evidence">
              <div>
                <strong>{{ evidence }}</strong>
                <p>保留原有证据语义，可直接用于审核结论、复核追溯与后续派单承接。</p>
              </div>
              <small>已归档证据</small>
            </li>
          </ul>
        </section>
      </section>

      <section class="detail-main-grid">
        <section class="detail-card panel">
          <header class="detail-card__header">
            <div>
              <p>模板冻结</p>
              <h3>流程模板配置</h3>
            </div>
          </header>
          <label class="field-stack">
            <span>流程模板</span>
            <select v-model="selectedTemplateId" aria-label="流程模板" :disabled="!canEditTemplateSelection">
              <option v-for="template in detail.templateOptions" :key="template.id" :value="template.id">
                {{ template.templateName }} {{ template.versionLabel }}
              </option>
            </select>
          </label>
          <ul class="note-list">
            <li>提交前模板可编辑；提交后冻结版本只读；驳回重提默认沿用原模板版本。</li>
            <li v-if="detail.canReselectTemplate">当前角色具备重选模板权限，可在提交前重新选择。</li>
            <li v-else>当前角色仅可查看冻结模板版本。</li>
          </ul>
        </section>

        <section class="detail-card panel">
          <header class="detail-card__header">
            <div>
              <p>流程语义</p>
              <h3>流程送审</h3>
            </div>
          </header>
          <RecordTimeline :records="detail.eventDetail.lifecycleRecords" />

          <section class="detail-subsection">
            <header class="detail-card__header detail-card__header--nested">
              <div>
                <p>审核说明</p>
                <h3>送审与复核说明</h3>
              </div>
            </header>
            <ul class="process-list">
              <li>
                <strong>提审语义</strong>
                <span>{{ processSubmissionCopy }}</span>
              </li>
              <li>
                <strong>审核结论</strong>
                <span>{{ reviewOutcomeCopy }}</span>
              </li>
              <li>
                <strong>当前流转</strong>
                <span>{{ currentProgressCopy }}</span>
              </li>
            </ul>
          </section>
        </section>

        <section class="detail-card panel">
          <header class="detail-card__header">
            <div>
              <p>节点进度</p>
              <h3>流程节点轨迹</h3>
            </div>
          </header>
          <ul class="node-list" aria-label="节点进度">
            <li v-for="node in detail.nodeProgress" :key="node.id">
              <div>
                <strong>{{ node.name }}</strong>
                <p>{{ node.assigneeRole }}</p>
              </div>
              <StatusTag :status="node.status === 'PENDING' ? 'PENDING_AUDIT' : node.status" />
            </li>
          </ul>

          <section class="detail-subsection">
            <header class="detail-card__header detail-card__header--nested">
              <div>
                <p>权限动作</p>
                <h3>审核操作</h3>
              </div>
            </header>
            <div class="action-card">
              <p class="action-card__title">审核结论处理</p>
              <p class="action-card__copy">保留当前审核详情动作语义：可执行通过、驳回，或仅查看当前审核状态。</p>
              <div v-if="canApproveAudit || canRejectAudit" class="action-row">
                <button v-if="canApproveAudit" type="button" class="action-button">通过</button>
                <button v-if="canRejectAudit" type="button" class="action-button action-button--secondary">驳回</button>
              </div>
              <p v-else class="detail-empty-state">当前账号暂无可执行审核操作，请联系具备审核权限的人员。</p>
            </div>
          </section>

          <section v-if="detail.eventState === 'WAITING_DISPATCH'" class="detail-subsection">
            <header class="detail-card__header detail-card__header--nested">
              <div>
                <p>派单承接</p>
                <h3>派单录入</h3>
              </div>
            </header>
            <div class="action-card">
              <p class="action-card__title">等待派单</p>
              <p class="action-card__copy">审核通过后进入派单承接阶段，当前页面仅保留展示语义，不新增派单表单流程。</p>
              <div class="dispatch-readonly-card">
                <label>
                  处置人员
                  <input type="text" value="常平镇综合网格员" readonly />
                </label>
              </div>
            </div>
          </section>
        </section>
      </section>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getAuditDetail } from '../../api/audit'
import { hasPermission } from '../../auth/permissions'
import NotFoundState from '../../components/admin/NotFoundState.vue'
import PageContainer from '../../components/admin/PageContainer.vue'
import RecordTimeline from '../../components/admin/RecordTimeline.vue'
import StatusTag from '../../components/admin/StatusTag.vue'

const route = useRoute()
const auditId = computed(() => Number(route.params.id))
const detail = ref<Awaited<ReturnType<typeof getAuditDetail>>>()
const selectedTemplateId = ref<number>()

watch(
  auditId,
  async (id) => {
    if (!Number.isFinite(id)) {
      detail.value = undefined
      selectedTemplateId.value = undefined
      return
    }

    detail.value = await getAuditDetail(id)
    selectedTemplateId.value = detail.value?.selectedTemplateId
  },
  { immediate: true }
)

const eventStateLabelMap: Record<string, string> = {
  WAITING_DISPATCH: '待派单',
  PENDING_AUDIT: '待审核',
  REJECTED: '已驳回',
  APPROVED: '已通过',
  IN_PROGRESS: '进行中'
}

const canEditTemplateSelection = computed(() => Boolean(detail.value?.templateEditable && detail.value?.canReselectTemplate))
const canApproveAudit = computed(() => hasPermission('button:audit:approve'))
const canRejectAudit = computed(() => hasPermission('button:audit:reject'))
const auditStatusTag = computed(() => (detail.value?.currentAuditStatus === 'PENDING' ? 'PENDING_AUDIT' : detail.value?.currentAuditStatus ?? ''))
const auditStatusText = computed(() => {
  if (!detail.value) return ''
  if (detail.value.currentAuditStatus === 'PENDING') return '待审核'
  if (detail.value.currentAuditStatus === 'APPROVED') return '已通过'
  if (detail.value.currentAuditStatus === 'REJECTED') return '已驳回'
  return '进行中'
})
const eventStateText = computed(() => (detail.value ? eventStateLabelMap[detail.value.eventState] ?? detail.value.eventState : ''))
const processSubmissionCopy = computed(() => {
  if (!detail.value) return ''
  return detail.value.isResubmission
    ? '当前记录属于驳回后重提，沿用原冻结模板版本并补充送审说明后再次进入审核链路。'
    : '当前记录为首次提审，按选定流程模板送审，并保留原始事件摘要与证据材料作为审核依据。'
})
const reviewOutcomeCopy = computed(() => {
  if (!detail.value) return ''
  if (detail.value.currentAuditStatus === 'APPROVED') return '审核已通过，页面继续保留审核结论、证据与流程记录，供后续派单与复核追踪使用。'
  if (detail.value.currentAuditStatus === 'REJECTED') return '审核已驳回，需根据驳回意见补充材料后重新提交，保持当前审核详情与模板冻结语义。'
  return '审核尚未完成，当前页面继续展示证据、节点进度与操作入口，供审核人员做出结论。'
})
const currentProgressCopy = computed(() => {
  if (!detail.value) return ''
  return `当前审核状态为${auditStatusText.value}，事件状态为${eventStateText.value}，共保留 ${detail.value.nodeProgress.length} 个审核节点进度。`
})

const summaryItems = computed(() =>
  detail.value
    ? [
        { label: '事件编号', value: detail.value.eventCode },
        { label: '事件标题', value: detail.value.eventTitle },
        { label: '审核状态', value: '', status: auditStatusTag.value },
        { label: '事件状态', value: '', status: detail.value.eventState },
        { label: '来源系统', value: detail.value.eventDetail.sourceSystem },
        { label: '处置区域', value: detail.value.eventDetail.area },
        { label: '发生时间', value: detail.value.eventDetail.occurredAt },
        { label: '证据数量', value: `${detail.value.eventDetail.evidenceReferences.length} 项` }
      ]
    : []
)
</script>

<style scoped>
@import '../admin-shared.css';

.audit-detail-page {
  display: grid;
  gap: 18px;
}

.detail-hero,
.detail-card {
  border: 1px solid rgba(103, 187, 246, 0.18);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(14, 45, 70, 0.96), rgba(10, 33, 53, 0.98));
  color: #eaf5ff;
  box-shadow: 0 20px 40px rgba(3, 10, 20, 0.18);
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 20px;
}

.detail-hero p,
.detail-hero span,
.detail-card__header p,
.detail-card__header span,
.evidence-list p,
.evidence-list small,
.note-list,
.process-list span,
.node-list p,
.action-card__copy,
.detail-empty-state {
  color: #8db0d0;
}

.detail-hero p,
.detail-card__header p,
.action-card__title,
.action-card__copy,
.detail-empty-state,
.note-list {
  margin: 0;
}

.detail-hero h3,
.detail-card__header h3,
.evidence-list strong,
.process-list strong,
.node-list strong,
.detail-summary-grid dd,
.action-card__title {
  color: #eaf5ff;
}

.detail-hero h3,
.detail-card__header h3 {
  margin: 8px 0 0;
}

.detail-top-grid,
.detail-main-grid {
  display: grid;
  gap: 18px;
}

.detail-top-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-main-grid {
  grid-template-columns: 320px minmax(0, 1.1fr) minmax(320px, 0.9fr);
}

.detail-card {
  padding: 20px;
}

.detail-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.detail-card__header--nested {
  margin-top: 18px;
}

.detail-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0 0;
}

.detail-summary-grid div,
.evidence-list li,
.note-list li,
.process-list li,
.node-list li,
.action-card,
.dispatch-readonly-card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid rgba(103, 187, 246, 0.14);
  background: rgba(8, 30, 50, 0.78);
}

.detail-summary-grid dt {
  font-size: 12px;
  color: #8db0d0;
}

.detail-summary-grid dd {
  margin: 8px 0 0;
}

.evidence-list,
.note-list,
.process-list,
.node-list {
  display: grid;
  gap: 12px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.evidence-list li,
.node-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.evidence-list p,
.node-list p,
.process-list span,
.action-card__copy,
.detail-empty-state {
  line-height: 1.6;
}

.evidence-list p,
.node-list p {
  margin: 8px 0 0;
}

.field-stack {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.field-stack select,
.dispatch-readonly-card input {
  min-height: 44px;
  border: 1px solid rgba(103, 187, 246, 0.18);
  border-radius: 12px;
  padding: 0 12px;
  font: inherit;
  color: #eaf5ff;
  background: rgba(7, 27, 46, 0.96);
}

.dispatch-readonly-card {
  padding: 0;
  border: none;
  background: transparent;
}

.dispatch-readonly-card label {
  display: grid;
  gap: 8px;
  color: #8db0d0;
}

.action-card {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

@media (max-width: 1200px) {
  .detail-top-grid,
  .detail-main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .detail-hero,
  .detail-card__header,
  .evidence-list li,
  .node-list li {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
