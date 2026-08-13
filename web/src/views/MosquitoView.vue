<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">爱卫蚊媒管控</h2>
        <p style="font-size:13px;color:#6b7280;">蚊媒孳生地三色分级（红=紧急险情需立即消杀 / 黄=重点 / 绿=一般）、消杀记录、重点场所卫生监测、检测设备监测</p>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div style="display:flex;gap:8px;margin-bottom:16px;border-bottom:2px solid #f3f4f6;padding-bottom:8px;">
      <button v-for="t in tabs" :key="t.key" @click="switchTab(t.key)"
              :style="{ padding: '8px 18px', borderRadius: '6px', cursor: 'pointer', fontSize: '14px', fontWeight: 500,
                        background: tab === t.key ? '#1890ff' : '#fff', color: tab === t.key ? '#fff' : '#6b7280', border: tab === t.key ? 'none' : '1px solid #d1d5db' }">
        {{ t.label }}
      </button>
    </div>

    <!-- ============ Tab1 孳生地 ============ -->
    <div v-if="tab === 'sites'">
      <div style="display:grid;grid-template-columns:repeat(6,1fr);gap:12px;margin-bottom:16px;">
        <div class="card card-border-red" style="padding:14px;">
          <p class="stat-label">红色（紧急）</p>
          <p class="stat-value" style="color:#dc2626;">{{ siteStats.red || 0 }}</p>
        </div>
        <div class="card card-border-orange" style="padding:14px;">
          <p class="stat-label">黄色（重点）</p>
          <p class="stat-value" style="color:#d97706;">{{ siteStats.yellow || 0 }}</p>
        </div>
        <div class="card card-border-green" style="padding:14px;">
          <p class="stat-label">绿色（一般）</p>
          <p class="stat-value" style="color:#16a34a;">{{ siteStats.green || 0 }}</p>
        </div>
        <div class="card card-border-blue" style="padding:14px;">
          <p class="stat-label">在管总数</p>
          <p class="stat-value" style="color:#1890ff;">{{ siteStats.active || 0 }}</p>
        </div>
        <div class="card" style="padding:14px;">
          <p class="stat-label">已消除</p>
          <p class="stat-value">{{ siteStats.eliminated || 0 }}</p>
        </div>
        <div class="card card-border-purple" style="padding:14px;">
          <p class="stat-label">本月消杀</p>
          <p class="stat-value" style="color:#722ed1;">{{ siteStats.monthDisinfection || 0 }}</p>
        </div>
      </div>

      <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center;justify-content:space-between;">
        <div style="display:flex;gap:12px;">
          <select v-model="siteFilterStatus" @change="loadSites" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
            <option value="">全部状态</option>
            <option value="ACTIVE">在管</option>
            <option value="ELIMINATED">已消除</option>
          </select>
          <select v-model="siteFilterLevel" @change="loadSites" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
            <option value="">全部等级</option>
            <option value="RED">红（紧急）</option>
            <option value="YELLOW">黄（重点）</option>
            <option value="GREEN">绿（一般）</option>
          </select>
          <span style="font-size:12px;color:#6b7280;align-self:center;">共 {{ siteTotal }} 处</span>
        </div>
        <button @click="openSiteForm(null)" style="padding:8px 16px;border:none;border-radius:6px;background:#16a34a;color:#fff;font-size:13px;cursor:pointer;">
          <i class="fas fa-plus"></i> 新增孳生地
        </button>
      </div>

      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>等级</th>
              <th>名称 / 编号</th>
              <th>类型</th>
              <th>地址 / 网格</th>
              <th>责任人</th>
              <th>最近检查</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in sites" :key="s.id">
              <td><span :class="['tag', levelTagClass(s.riskLevel)]">{{ levelLabel(s.riskLevel) }}</span></td>
              <td>
                <div style="font-weight:500;font-size:13px;">{{ s.site_name }}</div>
                <div style="font-size:11px;color:#9ca3af;">{{ s.site_no }}</div>
              </td>
              <td style="font-size:12px;">{{ siteTypeLabel(s.site_type) }}</td>
              <td style="font-size:12px;color:#6b7280;">
                <div>{{ s.address || '-' }}</div>
                <div v-if="s.grid_name" style="color:#1890ff;">{{ s.grid_name }}</div>
              </td>
              <td style="font-size:12px;">{{ s.owner_name || '-' }}<div style="font-size:11px;color:#9ca3af;">{{ s.owner_phone || '' }}</div></td>
              <td style="font-size:12px;color:#6b7280;">{{ formatTime(s.last_check_at) }}</td>
              <td><span :class="['tag', s.status === 'ACTIVE' ? 'tag-blue' : 'tag-gray']">{{ s.status === 'ACTIVE' ? '在管' : '已消除' }}</span></td>
              <td>
                <button @click="openSiteForm(s)" style="padding:4px 8px;font-size:12px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;cursor:pointer;margin-right:4px;">编辑</button>
                <button v-if="s.status === 'ACTIVE'" @click="handleEliminate(s)" style="padding:4px 8px;font-size:12px;border:1px solid #16a34a;border-radius:4px;background:#fff;color:#16a34a;cursor:pointer;margin-right:4px;">消除</button>
                <button @click="handleDeleteSite(s)" style="padding:4px 8px;font-size:12px;border:1px solid #ff4d4f;border-radius:4px;background:#fff;color:#ff4d4f;cursor:pointer;">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!sites.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无孳生地记录</p>
      </div>
    </div>

    <!-- ============ Tab2 消杀记录 ============ -->
    <div v-if="tab === 'disinfections'">
      <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center;justify-content:space-between;">
        <div style="display:flex;gap:12px;align-items:center;">
          <select v-model="disFilterSite" @change="loadDisinfections" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;max-width:260px;">
            <option value="">全部孳生地</option>
            <option v-for="s in sites" :key="s.id" :value="s.id">{{ s.site_name }}</option>
          </select>
          <span style="font-size:12px;color:#6b7280;">共 {{ disTotal }} 条</span>
        </div>
        <button @click="openDisForm()" style="padding:8px 16px;border:none;border-radius:6px;background:#16a34a;color:#fff;font-size:13px;cursor:pointer;">
          <i class="fas fa-plus"></i> 新增消杀记录
        </button>
      </div>

      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>编号</th>
              <th>消杀对象</th>
              <th>方式 / 药物</th>
              <th>作业人员</th>
              <th>作业日期</th>
              <th>面积</th>
              <th>效果</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in disinfections" :key="d.id">
              <td style="font-size:12px;color:#9ca3af;">{{ d.record_no }}</td>
              <td style="font-size:13px;font-weight:500;">{{ d.site_name || '-' }}</td>
              <td style="font-size:12px;">
                <div>{{ disTypeLabel(d.disinfection_type) }}</div>
                <div style="color:#9ca3af;font-size:11px;">{{ d.disinfectant || '' }}</div>
              </td>
              <td style="font-size:12px;">{{ d.operator_name || '-' }}</td>
              <td style="font-size:12px;">{{ d.operator_date }}</td>
              <td style="font-size:12px;">{{ d.area_sqm ? d.area_sqm + ' ㎡' : '-' }}</td>
              <td><span :class="['tag', d.result === 'GOOD' ? 'tag-green' : d.result === 'FAIR' ? 'tag-orange' : 'tag-red']">{{ disResultLabel(d.result) }}</span></td>
              <td><button @click="handleDeleteDis(d)" style="padding:4px 8px;font-size:12px;border:1px solid #ff4d4f;border-radius:4px;background:#fff;color:#ff4d4f;cursor:pointer;">删除</button></td>
            </tr>
          </tbody>
        </table>
        <p v-if="!disinfections.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无消杀记录</p>
      </div>
    </div>

    <!-- ============ Tab3 卫生监测 ============ -->
    <div v-if="tab === 'monitors'">
      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:16px;">
        <div class="card card-border-red" style="padding:14px;"><p class="stat-label">红色（不达标）</p><p class="stat-value" style="color:#dc2626;">{{ monStats.red || 0 }}</p></div>
        <div class="card card-border-orange" style="padding:14px;"><p class="stat-label">黄色（需整改）</p><p class="stat-value" style="color:#d97706;">{{ monStats.yellow || 0 }}</p></div>
        <div class="card card-border-green" style="padding:14px;"><p class="stat-label">绿色（达标）</p><p class="stat-value" style="color:#16a34a;">{{ monStats.green || 0 }}</p></div>
        <div class="card card-border-blue" style="padding:14px;"><p class="stat-label">平均评分</p><p class="stat-value" style="color:#1890ff;">{{ monStats.avgScore || 0 }}</p></div>
      </div>

      <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center;justify-content:space-between;">
        <span style="font-size:12px;color:#6b7280;">共 {{ monTotal }} 条监测记录</span>
        <button @click="openMonForm(null)" style="padding:8px 16px;border:none;border-radius:6px;background:#16a34a;color:#fff;font-size:13px;cursor:pointer;">
          <i class="fas fa-plus"></i> 新增监测记录
        </button>
      </div>

      <div class="card">
        <table class="table">
          <thead>
            <tr>
              <th>等级</th>
              <th>场所</th>
              <th>类型</th>
              <th>监测项目</th>
              <th>评分</th>
              <th>监测日期</th>
              <th>监测机构</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in monitors" :key="m.id">
              <td><span :class="['tag', levelTagClass(m.riskLevel)]">{{ levelLabel(m.riskLevel) }}</span></td>
              <td>
                <div style="font-weight:500;font-size:13px;">{{ m.place_name }}</div>
                <div style="font-size:11px;color:#9ca3af;">{{ m.address || '' }}</div>
              </td>
              <td style="font-size:12px;">{{ monTypeLabel(m.place_type) }}</td>
              <td style="font-size:12px;">{{ m.monitor_item || '-' }}</td>
              <td style="text-align:center;">
                <span :style="{ fontSize: '15px', fontWeight: 600, color: m.score >= 85 ? '#16a34a' : m.score >= 70 ? '#d97706' : '#dc2626' }">{{ m.score }}</span>
              </td>
              <td style="font-size:12px;">{{ m.monitor_date }}</td>
              <td style="font-size:12px;">{{ m.monitor_org || '-' }}</td>
              <td>
                <button @click="openMonForm(m)" style="padding:4px 8px;font-size:12px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;cursor:pointer;margin-right:4px;">编辑</button>
                <button @click="handleDeleteMon(m)" style="padding:4px 8px;font-size:12px;border:1px solid #ff4d4f;border-radius:4px;background:#fff;color:#ff4d4f;cursor:pointer;">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!monitors.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无监测记录</p>
      </div>
    </div>

    <!-- ============ 孳生地新增/编辑弹窗 ============ -->
    <div v-if="showSiteForm" class="modal-overlay" @click.self="showSiteForm = false">
      <div class="modal-box" style="width:520px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ siteForm.id ? '编辑孳生地' : '新增孳生地' }}</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <div class="form-group">
            <label class="form-label">名称 <span style="color:#ff4d4f;">*</span></label>
            <input v-model="siteForm.siteName" class="form-input" placeholder="如：XX小区2栋后沟渠" />
          </div>
          <div class="form-group">
            <label class="form-label">类型</label>
            <select v-model="siteForm.siteType" class="form-select">
              <option value="CATCH_BASIN">积水容器</option>
              <option value="DITCH">沟渠</option>
              <option value="SEWER">下水道</option>
              <option value="GREEN">绿化带</option>
              <option value="GARBAGE">垃圾点</option>
              <option value="WATER">水塘</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">三色分级</label>
            <select v-model="siteForm.riskLevel" class="form-select">
              <option value="RED">红（紧急，需立即消杀）</option>
              <option value="YELLOW">黄（重点）</option>
              <option value="GREEN">绿（一般）</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">所属网格</label>
            <select v-model="siteForm.gridId" class="form-select">
              <option :value="null">— 未指定 —</option>
              <option v-for="g in gridOptions" :key="g.id" :value="g.id">{{ g.name }}</option>
            </select>
          </div>
          <div class="form-group" style="grid-column:1/3;">
            <label class="form-label">详细地址</label>
            <input v-model="siteForm.address" class="form-input" placeholder="详细地址..." />
          </div>
          <div class="form-group">
            <label class="form-label">经度</label>
            <input v-model="siteForm.longitude" class="form-input" placeholder="113.xxxx" />
          </div>
          <div class="form-group">
            <label class="form-label">纬度</label>
            <input v-model="siteForm.latitude" class="form-input" placeholder="23.xxxx" />
          </div>
          <div class="form-group">
            <label class="form-label">责任人</label>
            <input v-model="siteForm.ownerName" class="form-input" placeholder="责任人姓名" />
          </div>
          <div class="form-group">
            <label class="form-label">责任人电话</label>
            <input v-model="siteForm.ownerPhone" class="form-input" placeholder="联系电话" />
          </div>
          <div class="form-group" style="grid-column:1/3;">
            <label class="form-label">备注</label>
            <input v-model="siteForm.remark" class="form-input" placeholder="备注..." />
          </div>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showSiteForm = false" class="btn btn-default">取消</button>
          <button @click="saveSite" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>

    <!-- ============ 消杀记录新增弹窗 ============ -->
    <div v-if="showDisForm" class="modal-overlay" @click.self="showDisForm = false">
      <div class="modal-box" style="width:520px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">新增消杀记录</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <div class="form-group" style="grid-column:1/3;">
            <label class="form-label">关联孳生地</label>
            <select v-model="disForm.siteId" class="form-select" @change="onDisSiteChange">
              <option :value="null">— 直接消杀（不关联）—</option>
              <option v-for="s in sites" :key="s.id" :value="s.id">{{ s.site_name }}</option>
            </select>
          </div>
          <div class="form-group" style="grid-column:1/3;">
            <label class="form-label">消杀对象名称</label>
            <input v-model="disForm.siteName" class="form-input" placeholder="消杀对象..." />
          </div>
          <div class="form-group">
            <label class="form-label">消杀方式</label>
            <select v-model="disForm.disinfectionType" class="form-select">
              <option value="CHEMICAL">药物消杀</option>
              <option value="CLEANING">清理积水</option>
              <option value="BIOLOGICAL">生物防治</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">使用药物/手段</label>
            <input v-model="disForm.disinfectant" class="form-input" placeholder="如：菊酯类药剂" />
          </div>
          <div class="form-group">
            <label class="form-label">作业人员</label>
            <input v-model="disForm.operatorName" class="form-input" placeholder="作业人员" />
          </div>
          <div class="form-group">
            <label class="form-label">作业日期</label>
            <input v-model="disForm.operatorDate" type="date" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">消杀面积(㎡)</label>
            <input v-model="disForm.areaSqm" type="number" class="form-input" placeholder="如：50" />
          </div>
          <div class="form-group">
            <label class="form-label">效果</label>
            <select v-model="disForm.result" class="form-select">
              <option value="GOOD">良好</option>
              <option value="FAIR">一般</option>
              <option value="POOR">差（需复查）</option>
            </select>
          </div>
          <div class="form-group" style="grid-column:1/3;">
            <label class="form-label">备注</label>
            <input v-model="disForm.remark" class="form-input" placeholder="备注..." />
          </div>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showDisForm = false" class="btn btn-default">取消</button>
          <button @click="saveDis" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>

    <!-- ============ 卫生监测新增/编辑弹窗 ============ -->
    <div v-if="showMonForm" class="modal-overlay" @click.self="showMonForm = false">
      <div class="modal-box" style="width:520px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ monForm.id ? '编辑监测记录' : '新增监测记录' }}</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <div class="form-group">
            <label class="form-label">场所名称 <span style="color:#ff4d4f;">*</span></label>
            <input v-model="monForm.placeName" class="form-input" placeholder="如：XX农贸市场" />
          </div>
          <div class="form-group">
            <label class="form-label">场所类型</label>
            <select v-model="monForm.placeType" class="form-select">
              <option value="SCHOOL">学校</option>
              <option value="MARKET">农贸市场</option>
              <option value="RESTAURANT">餐饮</option>
              <option value="CLINIC">诊所</option>
              <option value="COMMUNITY">小区</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <div class="form-group" style="grid-column:1/3;">
            <label class="form-label">详细地址</label>
            <input v-model="monForm.address" class="form-input" placeholder="详细地址..." />
          </div>
          <div class="form-group">
            <label class="form-label">所属网格</label>
            <select v-model="monForm.gridId" class="form-select">
              <option :value="null">— 未指定 —</option>
              <option v-for="g in gridOptions" :key="g.id" :value="g.id">{{ g.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">监测项目</label>
            <input v-model="monForm.monitorItem" class="form-input" placeholder="如：蚊媒密度" />
          </div>
          <div class="form-group">
            <label class="form-label">评分（0-100）</label>
            <input v-model.number="monForm.score" type="number" min="0" max="100" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">三色分级</label>
            <select v-model="monForm.riskLevel" class="form-select">
              <option value="GREEN">绿（达标）</option>
              <option value="YELLOW">黄（需整改）</option>
              <option value="RED">红（不达标）</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">监测日期</label>
            <input v-model="monForm.monitorDate" type="date" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">监测机构</label>
            <input v-model="monForm.monitorOrg" class="form-input" placeholder="如：社区爱卫办" />
          </div>
          <div class="form-group" style="grid-column:1/3;">
            <label class="form-label">备注</label>
            <input v-model="monForm.remark" class="form-input" placeholder="备注..." />
          </div>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showMonForm = false" class="btn btn-default">取消</button>
          <button @click="saveMon" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>
  </div>

  <!-- ============ Tab4 检测设备监测 ============ -->
  <MosquitoDevicePanel v-if="tab === 'devices'" />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import MosquitoDevicePanel from '../components/MosquitoDevicePanel.vue'
import {
  getMosquitoSites, getMosquitoSiteStatistics,
  createMosquitoSite, updateMosquitoSite, eliminateMosquitoSite, deleteMosquitoSite,
  getMosquitoDisinfections, createMosquitoDisinfection, deleteMosquitoDisinfection,
  getMosquitoMonitors, getMosquitoMonitorStatistics,
  createMosquitoMonitor, updateMosquitoMonitor, deleteMosquitoMonitor,
  getGridTree
} from '../api'

const tabs = [
  { key: 'sites', label: '蚊媒孳生地（红黄绿）' },
  { key: 'disinfections', label: '消杀记录' },
  { key: 'monitors', label: '重点场所卫生监测' },
  { key: 'devices', label: '检测设备监测' }
]
const tab = ref('sites')

// 网格下拉（树扁平化）
const gridOptions = ref<{ id: number; name: string }[]>([])
async function loadGrids() {
  try {
    const tree: any = await getGridTree()
    const flat: { id: number; name: string }[] = []
    const walk = (nodes: any[]) => {
      for (const n of nodes || []) {
        if (n.id && n.name) flat.push({ id: n.id, name: n.name })
        if (n.children?.length) walk(n.children)
      }
    }
    walk(Array.isArray(tree) ? tree : tree?.children || [])
    gridOptions.value = flat
  } catch { /* 网格加载失败不影响主功能 */ }
}

// ============ 孳生地 ============
const sites = ref<any[]>([])
const siteTotal = ref(0)
const siteStats = ref<any>({})
const siteFilterStatus = ref('')
const siteFilterLevel = ref('')
const showSiteForm = ref(false)
const siteForm = ref<any>({})

async function loadSites() {
  try {
    const res = await getMosquitoSites({ status: siteFilterStatus.value, level: siteFilterLevel.value, page: 1, size: 100 })
    sites.value = res?.items || []
    siteTotal.value = res?.total || 0
    siteStats.value = await getMosquitoSiteStatistics() || {}
  } catch (e: any) { alert(e?.message || '加载失败') }
}

function openSiteForm(s: any) {
  siteForm.value = s ? {
    id: s.id, siteName: s.site_name, siteType: s.site_type, riskLevel: s.risk_level,
    gridId: s.grid_id, address: s.address, longitude: s.longitude, latitude: s.latitude,
    ownerName: s.owner_name, ownerPhone: s.owner_phone, remark: s.remark
  } : { id: null, siteName: '', siteType: 'OTHER', riskLevel: 'GREEN', gridId: null, address: '', longitude: '', latitude: '', ownerName: '', ownerPhone: '', remark: '' }
  showSiteForm.value = true
}

async function saveSite() {
  if (!siteForm.value.siteName?.trim()) { alert('请填写孳生地名称'); return }
  try {
    const payload = { ...siteForm.value }
    const grid = gridOptions.value.find(g => g.id === payload.gridId)
    payload.gridName = grid?.name || ''
    if (payload.id) await updateMosquitoSite(payload.id, payload)
    else await createMosquitoSite(payload)
    showSiteForm.value = false
    await loadSites()
  } catch (e: any) { alert(e?.message || '保存失败') }
}

async function handleEliminate(s: any) {
  if (!confirm(`确认「${s.site_name}」已消除？`)) return
  try { await eliminateMosquitoSite(s.id); await loadSites() } catch (e: any) { alert(e?.message || '操作失败') }
}

async function handleDeleteSite(s: any) {
  if (!confirm(`确认删除孳生地「${s.site_name}」？`)) return
  try { await deleteMosquitoSite(s.id); await loadSites() } catch (e: any) { alert(e?.message || '删除失败') }
}

// ============ 消杀记录 ============
const disinfections = ref<any[]>([])
const disTotal = ref(0)
const disFilterSite = ref<number | string>('')
const showDisForm = ref(false)
const disForm = ref<any>({})

async function loadDisinfections() {
  try {
    const res = await getMosquitoDisinfections({ siteId: disFilterSite.value ? Number(disFilterSite.value) : undefined, page: 1, size: 100 })
    disinfections.value = res?.items || []
    disTotal.value = res?.total || 0
  } catch (e: any) { alert(e?.message || '加载失败') }
}

function openDisForm() {
  disForm.value = { siteId: null, siteName: '', disinfectionType: 'CHEMICAL', disinfectant: '', operatorName: '', operatorDate: new Date().toISOString().substring(0, 10), areaSqm: '', result: 'GOOD', remark: '' }
  showDisForm.value = true
}

function onDisSiteChange() {
  const s = sites.value.find(x => x.id === disForm.value.siteId)
  if (s) disForm.value.siteName = s.site_name
}

async function saveDis() {
  if (!disForm.value.siteName?.trim()) { alert('请填写消杀对象名称'); return }
  try {
    await createMosquitoDisinfection({ ...disForm.value, siteId: disForm.value.siteId || null })
    showDisForm.value = false
    await loadDisinfections()
    await loadSites()
  } catch (e: any) { alert(e?.message || '保存失败') }
}

async function handleDeleteDis(d: any) {
  if (!confirm(`确认删除消杀记录「${d.record_no}」？`)) return
  try { await deleteMosquitoDisinfection(d.id); await loadDisinfections() } catch (e: any) { alert(e?.message || '删除失败') }
}

// ============ 卫生监测 ============
const monitors = ref<any[]>([])
const monTotal = ref(0)
const monStats = ref<any>({})
const showMonForm = ref(false)
const monForm = ref<any>({})

async function loadMonitors() {
  try {
    const res = await getMosquitoMonitors({ page: 1, size: 100 })
    monitors.value = res?.items || []
    monTotal.value = res?.total || 0
    monStats.value = await getMosquitoMonitorStatistics() || {}
  } catch (e: any) { alert(e?.message || '加载失败') }
}

function openMonForm(m: any) {
  monForm.value = m ? {
    id: m.id, placeName: m.place_name, placeType: m.place_type, address: m.address,
    gridId: m.grid_id, monitorItem: m.monitor_item, score: m.score, riskLevel: m.risk_level,
    monitorDate: m.monitor_date, monitorOrg: m.monitor_org, remark: m.remark
  } : { id: null, placeName: '', placeType: 'OTHER', address: '', gridId: null, monitorItem: '', score: 100, riskLevel: 'GREEN', monitorDate: new Date().toISOString().substring(0, 10), monitorOrg: '', remark: '' }
  showMonForm.value = true
}

async function saveMon() {
  if (!monForm.value.placeName?.trim()) { alert('请填写场所名称'); return }
  try {
    const payload = { ...monForm.value }
    const grid = gridOptions.value.find(g => g.id === payload.gridId)
    payload.gridName = grid?.name || ''
    if (payload.id) await updateMosquitoMonitor(payload.id, payload)
    else await createMosquitoMonitor(payload)
    showMonForm.value = false
    await loadMonitors()
  } catch (e: any) { alert(e?.message || '保存失败') }
}

async function handleDeleteMon(m: any) {
  if (!confirm(`确认删除监测记录「${m.placeName}」？`)) return
  try { await deleteMosquitoMonitor(m.id); await loadMonitors() } catch (e: any) { alert(e?.message || '删除失败') }
}

// ============ 通用 ============
function switchTab(key: string) {
  tab.value = key
  if (key === 'disinfections' && !disinfections.value.length) loadDisinfections()
  if (key === 'monitors' && !monitors.value.length) loadMonitors()
}

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

function levelLabel(l: string) {
  return { RED: '红（紧急）', YELLOW: '黄（重点）', GREEN: '绿（一般）' }[l] || l
}
function levelTagClass(l: string) {
  return { RED: 'tag-red', YELLOW: 'tag-orange', GREEN: 'tag-green' }[l] || 'tag-gray'
}
function siteTypeLabel(t: string) {
  return { CATCH_BASIN: '积水容器', DITCH: '沟渠', SEWER: '下水道', GREEN: '绿化带', GARBAGE: '垃圾点', WATER: '水塘', OTHER: '其他' }[t] || t
}
function disTypeLabel(t: string) {
  return { CHEMICAL: '药物消杀', CLEANING: '清理积水', BIOLOGICAL: '生物防治', OTHER: '其他' }[t] || t
}
function disResultLabel(r: string) {
  return { GOOD: '良好', FAIR: '一般', POOR: '差（需复查）' }[r] || r
}
function monTypeLabel(t: string) {
  return { SCHOOL: '学校', MARKET: '农贸市场', RESTAURANT: '餐饮', CLINIC: '诊所', COMMUNITY: '小区', OTHER: '其他' }[t] || t
}

onMounted(() => { loadGrids(); loadSites() })
</script>
