<template>
  <PageContainer title="综合监管总览">
    <section class="oversight-board">
      <section class="oversight-board__hero panel" aria-label="综合监管总览概览">
        <article v-for="item in summaryCards" :key="item.label" class="oversight-board__metric">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.hint }}</p>
        </article>
      </section>

      <section class="oversight-board__main-grid">
        <section class="panel oversight-board__map-panel">
          <header class="oversight-board__header">
            <div>
              <p>热点区域</p>
              <h3>重点片区热力图</h3>
            </div>
            <span>联动展示重点片区事件热度、设备在线与巡检任务压力</span>
          </header>
          <div class="oversight-board__canvas" aria-label="重点片区热力图">
            <div class="oversight-board__canvas-glow"></div>
            <div class="oversight-board__canvas-grid"></div>
            <article v-for="item in hotspots" :key="item.name" class="oversight-board__hotspot" :style="item.style">
              <strong>{{ item.name }}</strong>
              <span>{{ item.meta }}</span>
            </article>
          </div>
        </section>

        <section class="oversight-board__side-stack">
          <section class="panel">
            <header class="oversight-board__header">
              <h3>实时播报</h3>
            </header>
            <div class="oversight-board__list">
              <article v-for="item in alerts" :key="item.title" class="oversight-board__list-item">
                <strong>{{ item.title }}</strong>
                <span>{{ item.meta }}</span>
              </article>
            </div>
          </section>

          <section class="panel">
            <header class="oversight-board__header">
              <h3>今日闭环进展</h3>
            </header>
            <div class="oversight-board__digest-grid">
              <article v-for="item in digests" :key="item.label" class="oversight-board__digest-card">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
          </section>
        </section>
      </section>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import PageContainer from '../../components/admin/PageContainer.vue'

const summaryCards = [
  { label: '待审核识别结果', value: '148 条', hint: '来自视频算法与无人机识别的待确认结果' },
  { label: '处理中工单', value: '36 条', hint: '现场处置与核查任务持续推进' },
  { label: '执行中巡查任务', value: '12 个', hint: '重点片区与主干道路口优先覆盖' },
  { label: '在线设备', value: '31 台', hint: '无人机与视频设备统一纳管' }
]

const hotspots = [
  { name: '常平站周边', meta: '事件 42 / 工单 12', style: { top: '16%', right: '12%' } },
  { name: '桥沥片区', meta: '巡查任务 5 / 待核查 3', style: { left: '14%', bottom: '18%' } },
  { name: '常黄路沿线', meta: '在线设备 14 台', style: { right: '30%', bottom: '24%' } }
]

const alerts = [
  { title: '桥沥村垃圾堆放新增 12 起', description: '建议优先安排复核航线并同步工单处置。', meta: '10:32 更新' },
  { title: '常平大道占道经营持续活跃', description: '现场处置已回传图片，等待核查结果确认。', meta: '待核查 6 条' },
  { title: '东兴路设备在线率下降', description: '视频设备离线 2 台，建议联动无人机补盲。', meta: '设备告警' }
]

const digests = [
  { label: '已闭环事件', value: '87%', hint: '今日闭环率持续提升' },
  { label: '平均到场时长', value: '12 分钟', hint: '现场处置响应保持稳定' },
  { label: '补充核查任务', value: '8 条', hint: '等待移动端提交最终结论' }
]
</script>

<style scoped>
@import '../admin-shared.css';

.oversight-board,
.oversight-board__main-grid,
.oversight-board__side-stack,
.oversight-board__list,
.oversight-board__digest-grid {
  display: grid;
  gap: 16px;
}

.oversight-board__hero {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.oversight-board__metric,
.oversight-board__list-item,
.oversight-board__digest-card {
  padding: 18px;
  border: 1px solid rgba(103, 187, 246, 0.18);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(14, 45, 70, 0.96), rgba(10, 33, 53, 0.98));
  color: #eaf5ff;
}

.oversight-board__metric span,
.oversight-board__metric p,
.oversight-board__header p,
.oversight-board__header span,
.oversight-board__list-item p,
.oversight-board__list-item span,
.oversight-board__digest-card span,
.oversight-board__digest-card p {
  margin: 0;
  color: #8db0d0;
}

.oversight-board__metric strong,
.oversight-board__header h3,
.oversight-board__list-item strong,
.oversight-board__digest-card strong,
.oversight-board__hotspot strong,
.oversight-board__hotspot span {
  color: #eaf5ff;
}

.oversight-board__metric strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
}

.oversight-board__metric p,
.oversight-board__digest-card p,
.oversight-board__list-item p {
  margin-top: 10px;
  line-height: 1.6;
}

.oversight-board__main-grid {
  grid-template-columns: minmax(0, 1.3fr) minmax(340px, 0.7fr);
}

.oversight-board__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.oversight-board__header p {
  font-size: 12px;
  letter-spacing: 0.08em;
}

.oversight-board__header h3 {
  margin: 8px 0 0;
}

.oversight-board__header span {
  max-width: 240px;
  text-align: right;
}

.oversight-board__canvas {
  position: relative;
  min-height: 360px;
  margin-top: 16px;
  overflow: hidden;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(7, 27, 46, 0.98), rgba(4, 14, 24, 0.99));
}

.oversight-board__canvas-glow {
  position: absolute;
  top: 32px;
  left: 32px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: rgba(87, 185, 255, 0.2);
  filter: blur(24px);
}

.oversight-board__canvas-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(97, 188, 245, 0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(97, 188, 245, 0.08) 1px, transparent 1px);
  background-size: 36px 36px;
}

.oversight-board__hotspot {
  position: absolute;
  display: grid;
  gap: 4px;
  min-width: 150px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(137, 178, 255, 0.22);
  background: rgba(9, 30, 52, 0.86);
}

.oversight-board__digest-grid {
  grid-template-columns: 1fr;
}

@media (max-width: 1100px) {
  .oversight-board__hero,
  .oversight-board__main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .oversight-board__header {
    flex-direction: column;
  }

  .oversight-board__header span {
    text-align: left;
  }
}
</style>
