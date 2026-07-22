<template>
  <PageContainer title="巡查任务详情">
    <WebDetailPageTemplate
      :title="task.title"
      :code="task.code"
      :overview-items="overviewItems"
    >
      <template #hero-meta>
        <span class="detail-state">{{ task.status }}</span>
      </template>

      <template #primary>
        <section class="panel detail-block">
          <header class="detail-block__header">
            <div>
              <p class="detail-block__eyebrow">任务概览</p>
              <h3>执行安排</h3>
            </div>
          </header>
          <dl class="detail-list detail-list--summary">
            <div v-for="item in scheduleItems" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>
        </section>
      </template>

      <template #aside>
        <section class="panel detail-block">
          <header class="detail-block__header">
            <div>
              <p class="detail-block__eyebrow">关联事件</p>
              <h3>关联链路</h3>
            </div>
          </header>
          <ul class="detail-listing">
            <li>
              <strong>来源事件</strong>
              <span>{{ task.eventCode }} / {{ task.eventTitle }}</span>
            </li>
            <li>
              <strong>当前工单</strong>
              <span>CP-20260314-003 / {{ task.status }}</span>
            </li>
            <li>
              <strong>资料回传</strong>
              <span>已上传图片 1 项、视频 1 项</span>
            </li>
          </ul>
        </section>
      </template>

      <template #secondary-primary>
        <section class="panel detail-block">
          <header class="detail-block__header">
            <div>
              <p class="detail-block__eyebrow">现场记录</p>
              <h3>巡查重点</h3>
            </div>
          </header>
          <ul class="detail-listing detail-listing--cards">
            <li>
              <strong>巡查范围</strong>
              <span>桥沥村主街、工地围挡外缘及垃圾临时堆放点。</span>
            </li>
            <li>
              <strong>核查要求</strong>
              <span>比对整改照片、定位信息与现场现状，确认是否满足闭环条件。</span>
            </li>
            <li>
              <strong>协同岗位</strong>
              <span>{{ task.owner }}、审核岗王审核、调度岗李调度。</span>
            </li>
          </ul>
        </section>
      </template>

      <template #secondary-aside>
        <section class="panel detail-block">
          <header class="detail-block__header">
            <div>
              <p class="detail-block__eyebrow">核查结论</p>
              <h3>核查提示</h3>
            </div>
          </header>
          <ul class="detail-listing">
            <li>
              <strong>状态说明</strong>
              <span>任务已进入核查阶段，等待移动端提交最终结论。</span>
            </li>
            <li>
              <strong>时限要求</strong>
              <span>需在 2026-03-14 16:00 前完成回传与复核。</span>
            </li>
          </ul>
        </section>
      </template>
    </WebDetailPageTemplate>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PageContainer from '../../components/admin/PageContainer.vue'
import WebDetailPageTemplate from '../../templates/WebDetailPageTemplate.vue'

const route = useRoute()
const taskId = Number(route.params.id)

const allTasks = [
  { id: 1, code: 'PT-20260314-001', title: '常黄路主干道日常巡查', area: '常黄路沿线', owner: '巡查员张三', eventCode: 'EV-20260314-001', eventTitle: '桥沥村疑似违法施工', status: '执行中' },
  { id: 2, code: 'PT-20260314-002', title: '桥沥片区重点复核', area: '桥沥村', owner: '巡查员王五', eventCode: 'EV-20260314-003', eventTitle: '桥沥片区垃圾堆放', status: '待核查' },
  { id: 3, code: 'PT-20260314-003', title: '常平站周边秩序巡查', area: '常平站周边', owner: '巡查员李四', eventCode: 'EV-20260314-004', eventTitle: '站前广场占道经营', status: '已完成' }
]

const task = computed(() => allTasks.find((t) => t.id === taskId) || allTasks[0])

const overviewItems = computed(() => [
  { label: '执行区域', value: task.value.area },
  { label: '责任人', value: task.value.owner },
  { label: '关联事件', value: '1 个' }
])

const scheduleItems = computed(() => [
  { label: '任务编号', value: task.value.code },
  { label: '巡查主题', value: task.value.title },
  { label: '执行区域', value: task.value.area },
  { label: '执行时间', value: '2026-03-14 09:00 - 16:00' },
  { label: '责任人', value: task.value.owner },
  { label: '任务状态', value: task.value.status }
])
</script>

<style scoped>
@import '../admin-shared.css';

.detail-state {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(94, 162, 255, 0.12);
  color: var(--fg-primary, #5ea2ff);
  font-size: 12px;
  font-weight: 600;
}

.detail-block {
  display: grid;
  gap: 16px;
}

.detail-block__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.detail-block__eyebrow {
  margin: 0;
  color: var(--fg-primary, #5ea2ff);
  font-size: 13px;
  letter-spacing: 0.08em;
}

.detail-block__header h3 {
  margin: 8px 0 0;
  color: var(--fg-text-primary, #eef5ff);
}

.detail-list--summary {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.detail-list--summary div,
.detail-listing li {
  padding: 16px;
  border: 1px solid rgba(103, 187, 246, 0.14);
  border-radius: 12px;
  background: rgba(8, 30, 50, 0.5);
}

.detail-listing {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.detail-listing--cards {
  grid-template-columns: 1fr;
}

.detail-listing strong,
.detail-listing span {
  display: block;
}

.detail-listing strong {
  color: var(--fg-text-primary, #eef5ff);
}

.detail-listing span {
  margin-top: 8px;
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
  line-height: 1.6;
}
</style>
