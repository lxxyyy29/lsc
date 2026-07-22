<template>
  <PageContainer title="事件列表">
    <section class="audit-list-page" data-testid="web-list-page-template">
      <section class="query-panel panel">
        <header class="query-panel-header">
          <h3 class="query-panel-header__title">查询条件</h3>
        </header>
        <div class="query-panel__filters">
          <label class="field-stack">
            <ElSelect v-model="selectedStatus" clearable placeholder="请选择审核状态" aria-label="审核状态">
              <ElOption v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </ElSelect>
          </label>
        </div>
      </section>



      <section class="audit-content-grid">
        <section class="audit-card-grid" aria-label="审核任务卡片">
          <article v-for="item in audits" :key="item.id" class="audit-task-card panel">
            <div class="audit-task-card__media" aria-hidden="true">
              <div class="audit-task-card__media-overlay"></div>
              <div class="audit-task-card__detect-box"></div>
              <div class="audit-task-card__detect-label">车辆 0.78</div>
            </div>
            <div class="audit-task-card__body">
              <div class="audit-task-card__headline">
                <div>
                  <div class="audit-task-card__type-row">
                    <span class="audit-task-card__type">AI图片</span>
                    <strong>{{ item.eventTitle }}</strong>
                  </div>
                  <p>{{ item.eventCode }}</p>
                </div>
                <StatusTag :status="item.status === 'PENDING' ? 'PENDING_AUDIT' : item.status" />
              </div>

              <div class="audit-task-card__meta-grid">
                <div>
                  <span>审核状态</span>
                  <strong>{{ item.status === 'PENDING' ? '待审核' : item.currentNodeStatus }}</strong>
                </div>
                <div>
                  <span>审核节点</span>
                  <strong>{{ item.currentNodeName }}</strong>
                </div>
              </div>

              <div class="audit-task-card__flow-list">
                <div>
                  <span>流程模板</span>
                  <strong>{{ item.templateName }}</strong>
                </div>
                <div>
                  <span>当前节点</span>
                  <strong>{{ item.currentNodeName }} / {{ item.currentNodeStatus }}</strong>
                </div>
              </div>

              <div class="audit-task-card__actions">
                <button type="button" class="ghost-btn">忽略</button>
                <RouterLink :to="`/audits/${item.id}`" class="action-link action-link--primary">
                  {{ item.status === 'PENDING' ? '选流程送审' : '进入审核' }}
                </RouterLink>
                <RouterLink :to="`/audits/${item.id}`" class="ghost-link">查看</RouterLink>
              </div>
            </div>
          </article>
        </section>

        <section class="audit-table-panel panel">
          <header class="audit-table-panel__header">
            <div>
              <p>审核台账</p>
              <h3>列表内容</h3>
            </div>
            <span>当前共 {{ audits.length }} 条</span>
          </header>
          <table class="data-table">
            <thead>
              <tr>
                <th>事件编号 / 事件标题</th>
                <th>审核状态</th>
                <th>审核节点</th>
                <th>节点状态</th>
                <th>流程模板</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in audits" :key="`row-${item.id}`">
                <td>
                  <div class="audit-primary-cell">
                    <strong>{{ item.eventCode }}</strong>
                    <p>{{ item.eventTitle }}</p>
                  </div>
                </td>
                <td><StatusTag :status="item.status === 'PENDING' ? 'PENDING_AUDIT' : item.status" /></td>
                <td>{{ item.currentNodeName }}</td>
                <td>{{ item.currentNodeStatus }}</td>
                <td>{{ item.templateName }}</td>
                <td>
                  <RouterLink :to="`/audits/${item.id}`" class="audit-row-link">{{ item.status === 'PENDING' ? '选流程送审' : '进入审核' }}</RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </section>
      </section>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listAudits } from '../../api/audit'
import PageContainer from '../../components/admin/PageContainer.vue'
import StatusTag from '../../components/admin/StatusTag.vue'

const allAudits = listAudits()
const selectedStatus = ref('')

const statusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
] as const

const audits = computed(() =>
  allAudits.filter((item) => !selectedStatus.value || item.status === selectedStatus.value)
)


</script>

<style scoped>
@import '../admin-shared.css';

.audit-list-page {
  display: grid;
  gap: 18px;
}

.query-panel,
.summary-card,
.audit-task-card,
.audit-table-panel {
  border: 1px solid rgba(103, 187, 246, 0.18);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(14, 45, 70, 0.96), rgba(10, 33, 53, 0.98));
  color: #eaf5ff;
  box-shadow: 0 20px 40px rgba(3, 10, 20, 0.18);
}

.query-panel {
  padding: 20px;
}


.query-panel-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.query-panel-header__kicker {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #9cc6e8;
}

.query-panel-header__dot {
  font-size: 11px;
}

.query-panel-header__title {
  margin: 0;
  font-size: 18px;
  color: #eaf5ff;
}

.audit-table-panel__header h3 {
  margin: 0;
  color: #eaf5ff;
}

.query-panel__filters {
  margin-top: 18px;
  display: grid;
  grid-template-columns: minmax(220px, 320px);
}

.field-stack {
  display: grid;
  gap: 8px;
}

.field-stack select {
  min-height: 44px;
  border: 1px solid rgba(103, 187, 246, 0.18);
  border-radius: 12px;
  padding: 0 12px;
  font: inherit;
  color: #eaf5ff;
  background: rgba(7, 27, 46, 0.96);
}

.audit-content-grid,
.audit-card-grid {
  display: grid;
  gap: 18px;
}



.audit-content-grid {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.audit-card-grid {
  align-content: start;
}

.audit-task-card {
  overflow: hidden;
}

.audit-task-card__media {
  position: relative;
  height: 120px;
  background: linear-gradient(135deg, rgba(20, 20, 30, 0.92), rgba(9, 28, 46, 0.96));
}

.audit-task-card__media-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 72% 28%, rgba(122, 170, 255, 0.24), transparent 12%), linear-gradient(90deg, rgba(0, 0, 0, 0.3), rgba(0, 0, 0, 0.08));
}

.audit-task-card__detect-box {
  position: absolute;
  left: 190px;
  top: 34px;
  width: 20px;
  height: 24px;
  border: 2px solid rgba(89, 144, 255, 0.92);
  box-shadow: 0 0 0 1px rgba(130, 189, 255, 0.18), 0 0 12px rgba(89, 144, 255, 0.45);
}

.audit-task-card__detect-label {
  position: absolute;
  left: 183px;
  top: 28px;
  padding: 2px 4px;
  border-radius: 4px;
  background: rgba(46, 97, 220, 0.88);
  color: #fff;
  font-size: 9px;
}

.audit-task-card__body {
  display: grid;
  gap: 14px;
  padding: 14px;
}

.audit-task-card__headline,
.audit-task-card__meta-grid,
.audit-task-card__actions,
.audit-table-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.audit-task-card__type-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.audit-task-card__type {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  color: #95b9d8;
  background: rgba(255, 255, 255, 0.06);
}

.audit-task-card__headline p {
  margin: 8px 0 0;
}

.audit-task-card__meta-grid,
.audit-task-card__flow-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.audit-task-card__meta-grid div,
.audit-task-card__flow-list div {
  display: grid;
  gap: 6px;
}

.audit-task-card__actions {
  align-items: center;
}

.audit-table-panel {
  padding: 20px;
}

.audit-table-panel__header {
  align-items: flex-end;
  margin-bottom: 16px;
}

.audit-table-panel__header p,
.audit-primary-cell p {
  margin: 0;
}

:deep(.data-table) {
  border: 0;
  border-radius: 18px;
  overflow: hidden;
  background: rgba(7, 27, 46, 0.72);
}

:deep(.data-table th) {
  color: #9fc2df;
  font-size: 12px;
  font-weight: 600;
  background: rgba(7, 27, 46, 0.96);
}

:deep(.data-table td) {
  color: #eaf5ff;
  vertical-align: middle;
  border-bottom-color: rgba(103, 187, 246, 0.08);
}

.audit-primary-cell {
  display: grid;
  gap: 4px;
}

.audit-row-link {
  text-decoration: none;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .audit-content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .audit-task-card__headline,
  .audit-task-card__actions,
  .audit-table-panel__header {
    flex-direction: column;
    align-items: flex-start;
  }



  .audit-task-card__meta-grid,
  .audit-task-card__flow-list {
    grid-template-columns: 1fr;
  }

  :deep(.data-table) {
    display: block;
    overflow-x: auto;
  }
}
</style>
