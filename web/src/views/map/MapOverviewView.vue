<template>
  <PageContainer title="地图总览">
    <section class="overview-page">
      <section class="panel overview-hero" aria-label="地图总览概览">
        <article v-for="item in summaryCards" :key="item.label" class="overview-hero__card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.hint }}</p>
        </article>
      </section>

      <section class="overview-main-grid">
        <section class="panel overview-map-panel">
          <header class="overview-section-header">
            <div>
              <p>区域热力</p>
              <h3>综合态势地图</h3>
            </div>
            <span>重点区域 / 设备布点 / 工单闭环联动</span>
          </header>
          <QueryPanel>
            <label class="field-stack">
              <ElSelect v-model="selectedRegion" clearable placeholder="请选择监管区域" aria-label="监管区域">
                <ElOption label="全镇域" value="全镇域" />
                <ElOption label="桥沥片区" value="桥沥片区" />
                <ElOption label="常平站周边" value="常平站周边" />
                <ElOption label="朗洲片区" value="朗洲片区" />
              </ElSelect>
            </label>
            <label class="field-stack">
              <ElSelect v-model="selectedTopic" clearable placeholder="请选择监管主题" aria-label="监管主题">
                <ElOption label="事件热力" value="事件热力" />
                <ElOption label="设备在线" value="设备在线" />
                <ElOption label="工单闭环" value="工单闭环" />
              </ElSelect>
            </label>
            <label class="field-stack">
              <ElSelect v-model="selectedLevel" clearable placeholder="请选择告警等级" aria-label="告警等级">
                <ElOption label="紧急" value="紧急" />
                <ElOption label="预警" value="预警" />
                <ElOption label="关注" value="关注" />
              </ElSelect>
            </label>
          </QueryPanel>

          <div class="overview-map-canvas" aria-label="综合态势地图">
            <div class="overview-map-canvas__glow"></div>
            <div class="overview-map-canvas__grid"></div>
            <article v-for="marker in markers" :key="marker.name" class="overview-map-canvas__marker" :style="marker.style">
              <strong>{{ marker.name }}</strong>
              <span>{{ marker.meta }}</span>
            </article>
          </div>

          <div class="overview-summary-grid">
            <article v-for="item in heatZones" :key="item.label" class="overview-summary-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <p>{{ item.hint }}</p>
            </article>
          </div>
        </section>

        <section class="overview-side-stack">
          <section class="panel">
            <header class="overview-section-header">
              <div>
                <p>模型命中排行</p>
                <h3>热点算法</h3>
              </div>
              <span>近 24 小时识别结果</span>
            </header>
            <div class="overview-info-list">
              <div v-for="item in modelRanks" :key="item.label" class="overview-info-list__item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="panel">
            <header class="overview-section-header">
              <div>
                <p>告警播报</p>
                <h3>最新告警</h3>
              </div>
              <span>值班调度视角</span>
            </header>
            <div class="overview-alert-list">
              <article v-for="item in alerts" :key="item.title" class="overview-alert-item">
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.detail }}</p>
                </div>
                <span :class="['overview-alert-tag', `overview-alert-tag--${item.tone}`]">{{ item.level }}</span>
              </article>
            </div>
          </section>
        </section>
      </section>

      <section class="overview-bottom-grid">
        <section class="panel">
          <header class="overview-section-header">
            <div>
              <p>设备在线</p>
              <h3>接入设备状态</h3>
            </div>
            <span>无人机 / 固定点位 / 算法服务</span>
          </header>
          <table class="data-table">
            <thead>
              <tr>
                <th>设备类型</th>
                <th>在线数量</th>
                <th>离线数量</th>
                <th>重点区域</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in deviceStats" :key="item.type">
                <td>{{ item.type }}</td>
                <td>{{ item.online }}</td>
                <td>{{ item.offline }}</td>
                <td>{{ item.focusArea }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section class="panel">
          <header class="overview-section-header">
            <div>
              <p>工单闭环</p>
              <h3>处置进度</h3>
            </div>
            <span>调度闭环效率</span>
          </header>
          <div class="overview-info-list">
            <div v-for="item in workorderStats" :key="item.label" class="overview-info-list__item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>
      </section>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'

const selectedTopic = ref('')
const selectedLevel = ref('')
const selectedRegion = ref('')

const summaryCards = [
  { label: '重点监管区域', value: '6', hint: '高热片区实时监测' },
  { label: '在线模型', value: '9 / 10', hint: '命中统计持续回传' },
  { label: '在线设备', value: '31', hint: '含无人机与固定点位' },
  { label: '待闭环工单', value: '18', hint: '重点跟踪超时任务' }
]

const markers = [
  { name: '常平站周边', meta: '事件 42 / 工单 12', style: { top: '18%', right: '12%' } },
  { name: '桥沥片区', meta: '事件 37 / 工单 8', style: { left: '16%', bottom: '18%' } },
  { name: '常黄路沿线', meta: '设备 14 / 巡查 5', style: { right: '28%', bottom: '24%' } }
]

const heatZones = [
  { label: '最高热度区域', value: '常平站周边', hint: '车辆违停与占道经营持续活跃' },
  { label: '重点巡检片区', value: '桥沥片区', hint: '垃圾堆放与施工类事件高发' },
  { label: '闭环压力区域', value: '朗洲片区', hint: '工单待确认数量居前' }
]

const modelRanks = [
  { label: '占道经营识别', value: '56 次命中' },
  { label: '垃圾堆放识别', value: '41 次命中' },
  { label: '违法施工识别', value: '28 次命中' },
  { label: '广告牌异常识别', value: '16 次命中' }
]

const alerts = [
  { title: '常平站周边占道经营持续聚集', detail: '连续 30 分钟命中阈值，建议优先派单处置。', level: '紧急', tone: 'red' },
  { title: '桥沥片区垃圾堆放识别攀升', detail: '近 2 小时新增 12 条识别记录。', level: '预警', tone: 'orange' },
  { title: '朗洲片区闭环时效下降', detail: '3 条工单等待现场补充照片。', level: '关注', tone: 'blue' }
]

const deviceStats = [
  { type: '无人机', online: '12', offline: '2', focusArea: '桥沥片区' },
  { type: '固定摄像头', online: '15', offline: '1', focusArea: '常平站周边' },
  { type: '算法服务', online: '4', offline: '0', focusArea: '镇级中心机房' }
]

const workorderStats = [
  { label: '24 小时闭环率', value: '91%' },
  { label: '平均派单时长', value: '8 分钟' },
  { label: '待现场补充', value: '6 条' },
  { label: '超时预警工单', value: '3 条' }
]
</script>

<style scoped>
@import '../admin-shared.css';

.overview-page {
  display: grid;
  gap: 18px;
}

.overview-page :deep(.query-panel) {
  border-color: rgba(103, 187, 246, 0.18);
  background:
    linear-gradient(180deg, rgba(11, 33, 56, 0.94) 0%, rgba(7, 22, 39, 0.98) 100%);
  box-shadow: inset 0 1px 0 rgba(133, 194, 255, 0.08);
}

.overview-page :deep(.query-panel__actions) {
  align-items: center;
}

.overview-page :deep(.el-select__wrapper) {
  border-color: rgba(102, 171, 255, 0.24);
  background: rgba(8, 24, 42, 0.92);
  box-shadow: none;
}

.overview-page :deep(.el-select__selected-item) {
  color: #eff6ff;
}

.overview-page :deep(.el-select__placeholder) {
  color: rgba(239, 246, 255, 0.36);
}

.overview-main-grid,
.overview-bottom-grid,
.overview-side-stack,
.overview-info-list,
.overview-alert-list {
  display: grid;
  gap: 18px;
}

.overview-main-grid {
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
}

.overview-bottom-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.overview-hero,
.overview-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 18px;
}

.overview-hero__card,
.overview-summary-card,
.overview-info-list__item,
.overview-alert-item {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(118, 182, 255, 0.18);
  box-shadow:
    inset 0 1px 0 rgba(153, 208, 255, 0.08),
    0 18px 36px rgba(2, 8, 18, 0.24);
}

.overview-hero__card,
.overview-summary-card {
  display: grid;
  gap: 10px;
  min-height: 148px;
  padding: 20px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(15, 39, 69, 0.96) 0%, rgba(9, 24, 42, 0.98) 100%);
}

.overview-hero__card::before,
.overview-summary-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(82, 150, 255, 0.16), transparent 56%);
  pointer-events: none;
}

.overview-hero__card span,
.overview-hero__card p,
.overview-summary-card span,
.overview-summary-card p,
.overview-section-header p,
.overview-section-header span,
.overview-info-list__item span,
.overview-alert-item p {
  margin: 0;
  color: rgba(205, 222, 248, 0.78);
}

.overview-hero__card span,
.overview-summary-card span {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.overview-hero__card strong,
.overview-summary-card strong,
.overview-section-header h3,
.overview-info-list__item strong,
.overview-alert-item strong,
.overview-map-canvas__marker strong {
  color: #eef5ff;
}

.overview-hero__card strong,
.overview-summary-card strong {
  font-size: 30px;
  line-height: 1.1;
}

.overview-hero__card p,
.overview-summary-card p,
.overview-alert-item p {
  line-height: 1.6;
}

.overview-section-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.overview-section-header p {
  font-size: 12px;
  letter-spacing: 0.12em;
  color: #6bb5ff;
}

.overview-section-header h3 {
  margin: 8px 0 0;
}

.overview-section-header span {
  max-width: 260px;
  text-align: right;
}

.overview-map-panel {
  display: grid;
  gap: 18px;
}

.overview-map-canvas {
  position: relative;
  min-height: 360px;
  overflow: hidden;
  border-radius: 28px;
  border: 1px solid rgba(118, 182, 255, 0.18);
  background:
    radial-gradient(circle at top left, rgba(85, 168, 255, 0.2), transparent 26%),
    linear-gradient(180deg, #0f2745 0%, #071526 100%);
  box-shadow: inset 0 1px 0 rgba(153, 208, 255, 0.08);
}

.overview-map-canvas__glow {
  position: absolute;
  top: 24px;
  left: 32px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: rgba(77, 157, 255, 0.2);
  filter: blur(24px);
}

.overview-map-canvas__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(140, 186, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(140, 186, 255, 0.08) 1px, transparent 1px);
  background-size: 38px 38px;
}

.overview-map-canvas__marker {
  position: absolute;
  display: grid;
  gap: 4px;
  min-width: 148px;
  padding: 14px 16px;
  border: 1px solid rgba(137, 178, 255, 0.24);
  border-radius: 18px;
  background: rgba(9, 30, 52, 0.86);
  box-shadow: 0 18px 36px rgba(2, 8, 18, 0.3);
}

.overview-map-canvas__marker span {
  color: #cfe2ff;
}

.overview-info-list__item,
.overview-alert-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(15, 39, 69, 0.94) 0%, rgba(10, 26, 45, 0.98) 100%);
}

.overview-info-list__item {
  align-items: center;
}

.overview-alert-item {
  align-items: flex-start;
}

.overview-info-list__item strong {
  white-space: nowrap;
}

.overview-alert-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  padding: 6px 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 12px;
}

.overview-alert-tag--red {
  color: #ff9cb0;
  border-color: rgba(255, 122, 148, 0.18);
  background: rgba(201, 60, 86, 0.14);
}

.overview-alert-tag--orange {
  color: #ffd08a;
  border-color: rgba(255, 202, 118, 0.18);
  background: rgba(185, 109, 16, 0.16);
}

.overview-alert-tag--blue {
  color: #94c7ff;
  border-color: rgba(118, 182, 255, 0.18);
  background: rgba(36, 104, 201, 0.16);
}

@media (max-width: 1080px) {
  .overview-main-grid,
  .overview-bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .overview-section-header {
    flex-direction: column;
  }

  .overview-section-header span {
    text-align: left;
  }
}
</style>
