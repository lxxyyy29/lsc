<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">智慧党建</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">党员网格联户、志愿服务、三会一课、量化考核</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px;">
      <div class="card card-border-red">
        <div style="display:flex;align-items:center;gap:12px;">
          <i class="fas fa-users" style="font-size:24px;color:#FF4D4F;"></i>
          <div>
            <p class="stat-label">党员数</p>
            <p class="stat-value">{{ overview.memberCount || 0 }}</p>
          </div>
        </div>
      </div>
      <div class="card card-border-blue">
        <div style="display:flex;align-items:center;gap:12px;">
          <i class="fas fa-home" style="font-size:24px;color:#1890FF;"></i>
          <div>
            <p class="stat-label">联户总数</p>
            <p class="stat-value">{{ overview.householdCount || 0 }}</p>
          </div>
        </div>
      </div>
      <div class="card card-border-green">
        <div style="display:flex;align-items:center;gap:12px;">
          <i class="fas fa-hand-holding-heart" style="font-size:24px;color:#52C41A;"></i>
          <div>
            <p class="stat-label">志愿活动</p>
            <p class="stat-value">{{ overview.activityCount || 0 }}</p>
          </div>
        </div>
      </div>
      <div class="card card-border-orange">
        <div style="display:flex;align-items:center;gap:12px;">
          <i class="fas fa-book" style="font-size:24px;color:#FAAD14;"></i>
          <div>
            <p class="stat-label">三会一课</p>
            <p class="stat-value">{{ overview.meetingCount || 0 }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 标签页 -->
    <div style="display:flex;gap:8px;margin-bottom:16px;border-bottom:1px solid #e5e7eb;padding-bottom:8px;">
      <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
              :style="{padding:'6px 16px',border:'none',borderRadius:'6px',fontSize:'13px',cursor:'pointer',background: activeTab === tab.key ? '#1890ff' : '#f3f4f6',color: activeTab === tab.key ? '#fff' : '#374151'}">
        {{ tab.label }}
      </button>
    </div>

    <!-- 党员联户 -->
    <div v-if="activeTab === 'household'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">党员联户台账</h3>
        <button @click="showAddHousehold = true" style="padding:6px 14px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">添加联户</button>
      </div>
      <table class="table">
        <thead><tr><th>党员</th><th>户主</th><th>地址</th><th>网格</th><th>走访次数</th><th>最近走访</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="h in households" :key="h.id">
            <td>{{ h.memberName || '-' }}</td>
            <td>{{ h.householdName }}</td>
            <td style="font-size:12px;">{{ h.householdAddress || '-' }}</td>
            <td style="font-size:12px;">{{ h.gridName || '-' }}</td>
            <td>{{ h.visitCount }}</td>
            <td style="font-size:12px;">{{ h.lastVisitDate || '-' }}</td>
            <td><button @click="recordVisit(h.id)" style="padding:4px 10px;border:none;border-radius:4px;background:#52c41a;color:#fff;font-size:12px;cursor:pointer;">走访</button></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!households.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无联户记录</p>
    </div>

    <!-- 志愿服务 -->
    <div v-if="activeTab === 'activity'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">志愿服务活动</h3>
        <button @click="showAddActivity = true" style="padding:6px 14px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">创建活动</button>
      </div>
      <table class="table">
        <thead><tr><th>活动标题</th><th>日期</th><th>状态</th><th>已报名</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="a in activities" :key="a.id">
            <td>{{ a.title }}</td>
            <td style="font-size:12px;">{{ a.activityDate }}</td>
            <td><span class="tag" :class="a.status === 'COMPLETED' ? 'tag-green' : a.status === 'ONGOING' ? 'tag-blue' : 'tag-orange'">{{ activityStatus(a.status) }}</span></td>
            <td>{{ a.attendedCount || 0 }}/{{ a.maxParticipants || '∞' }}</td>
            <td><button v-if="a.status === 'PLANNED'" @click="signupActivity(a.id)" style="padding:4px 10px;border:none;border-radius:4px;background:#52c41a;color:#fff;font-size:12px;cursor:pointer;">报名</button></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!activities.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无活动</p>
    </div>

    <!-- 三会一课 -->
    <div v-if="activeTab === 'meeting'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">三会一课</h3>
        <button @click="showAddMeeting = true" style="padding:6px 14px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">添加记录</button>
      </div>
      <table class="table">
        <thead><tr><th>类型</th><th>主题</th><th>日期</th><th>党支部</th><th>参会人数</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="m in meetings" :key="m.id">
            <td><span class="tag tag-red">{{ m.meetingType }}</span></td>
            <td>{{ m.title }}</td>
            <td style="font-size:12px;">{{ m.meetingDate }}</td>
            <td style="font-size:12px;">{{ m.partyBranch || '-' }}</td>
            <td>{{ m.participantCount || '-' }}</td>
            <td><span class="tag" :class="m.status === 'COMPLETED' ? 'tag-green' : 'tag-orange'">{{ m.status === 'COMPLETED' ? '已完成' : '计划中' }}</span></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!meetings.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无记录</p>
    </div>

    <!-- 量化考核 -->
    <div v-if="activeTab === 'assessment'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">党员量化考核</h3>
        <div style="display:flex;gap:8px;">
          <input v-model="assessmentMonth" type="month" style="padding:4px 8px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;" />
          <button @click="generateAssessment" style="padding:6px 14px;border:none;border-radius:6px;background:#52c41a;color:#fff;font-size:13px;cursor:pointer;">生成考核</button>
        </div>
      </div>
      <table class="table">
        <thead><tr><th>排名</th><th>姓名</th><th>巡查</th><th>调解</th><th>志愿时长</th><th>出勤</th><th>综合得分</th></tr></thead>
        <tbody>
          <tr v-for="(a, idx) in assessments" :key="a.id">
            <td><span :style="{fontWeight:'700',color: idx < 3 ? '#ff4d4f' : '#9ca3af'}">{{ idx + 1 }}</span></td>
            <td>{{ a.memberName || '-' }}</td>
            <td>{{ a.patrolCount || 0 }}</td>
            <td>{{ a.mediationCount || 0 }}</td>
            <td>{{ a.volunteerHours || 0 }}</td>
            <td>{{ a.meetingAttendance || 0 }}</td>
            <td><strong :style="{color: (a.totalScore || 0) >= 80 ? '#52c41a' : (a.totalScore || 0) >= 50 ? '#faad14' : '#ff4d4f'}">{{ a.totalScore || 0 }}</strong></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!assessments.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无考核数据</p>
    </div>

    <!-- 添加联户弹窗 -->
    <div v-if="showAddHousehold" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="background:#fff;border-radius:12px;padding:24px;width:420px;max-width:90vw;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">添加联户</h3>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">党员ID</label>
          <input v-model.number="householdForm.partyMemberId" type="number" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">户主姓名</label>
          <input v-model="householdForm.householdName" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">地址</label>
          <input v-model="householdForm.householdAddress" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="margin-bottom:16px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">网格ID</label>
          <input v-model.number="householdForm.gridId" type="number" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;">
          <button @click="showAddHousehold = false" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
          <button @click="submitHousehold" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">提交</button>
        </div>
      </div>
    </div>

    <!-- 添加活动弹窗 -->
    <div v-if="showAddActivity" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="background:#fff;border-radius:12px;padding:24px;width:420px;max-width:90vw;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">创建志愿活动</h3>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">活动标题</label>
          <input v-model="activityForm.title" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">描述</label>
          <textarea v-model="activityForm.description" rows="2" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;"></textarea>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:12px;">
          <div>
            <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">活动日期</label>
            <input v-model="activityForm.activityDate" type="date" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
          </div>
          <div>
            <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">最大人数</label>
            <input v-model.number="activityForm.maxParticipants" type="number" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
          </div>
        </div>
        <div style="margin-bottom:16px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">网格ID</label>
          <input v-model.number="activityForm.gridId" type="number" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;">
          <button @click="showAddActivity = false" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
          <button @click="submitActivity" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">提交</button>
        </div>
      </div>
    </div>

    <!-- 添加会议弹窗 -->
    <div v-if="showAddMeeting" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="background:#fff;border-radius:12px;padding:24px;width:420px;max-width:90vw;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">添加会议记录</h3>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">会议类型</label>
          <select v-model="meetingForm.meetingType" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
            <option value="支部党员大会">支部党员大会</option>
            <option value="支委会">支委会</option>
            <option value="党小组会">党小组会</option>
            <option value="党课">党课</option>
          </select>
        </div>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">主题</label>
          <input v-model="meetingForm.title" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:12px;">
          <div>
            <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">日期</label>
            <input v-model="meetingForm.meetingDate" type="date" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
          </div>
          <div>
            <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">参会人数</label>
            <input v-model.number="meetingForm.participantCount" type="number" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
          </div>
        </div>
        <div style="margin-bottom:16px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">党支部</label>
          <input v-model="meetingForm.partyBranch" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;">
          <button @click="showAddMeeting = false" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
          <button @click="submitMeeting" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'

const tabs = [
  { key: 'household', label: '党员联户' },
  { key: 'activity', label: '志愿服务' },
  { key: 'meeting', label: '三会一课' },
  { key: 'assessment', label: '量化考核' },
]
const activeTab = ref('household')
const overview = ref<any>({})
const households = ref<any[]>([])
const activities = ref<any[]>([])
const meetings = ref<any[]>([])
const assessments = ref<any[]>([])
const assessmentMonth = ref(new Date().toISOString().slice(0, 7))
const showAddHousehold = ref(false)
const showAddActivity = ref(false)
const showAddMeeting = ref(false)

const householdForm = ref({ partyMemberId: null as number | null, householdName: '', householdAddress: '', gridId: null as number | null })
const activityForm = ref({ title: '', description: '', activityDate: '', gridId: null as number | null, maxParticipants: null as number | null, createdBy: 1 })
const meetingForm = ref({ meetingType: '支部党员大会', title: '', meetingDate: '', partyBranch: '', participantCount: null as number | null })

function activityStatus(status: string) {
  const map: Record<string, string> = { PLANNED: '计划中', ONGOING: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[status] || status
}

async function loadOverview() {
  try { overview.value = await http.get('/party/overview') || {} } catch (e) {}
}
async function loadHouseholds() {
  try { households.value = await http.get('/party/households') || [] } catch (e) {}
}
async function loadActivities() {
  try { activities.value = await http.get('/party/activities') || [] } catch (e) {}
}
async function loadMeetings() {
  try { meetings.value = await http.get('/party/meetings') || [] } catch (e) {}
}
async function loadAssessments() {
  try { assessments.value = await http.get('/party/assessments', { params: { month: assessmentMonth.value } }) || [] } catch (e) {}
}

async function recordVisit(id: number) {
  try { await http.post(`/party/households/${id}/visit`, {}); loadHouseholds() } catch (e: any) { alert(e?.message) }
}
async function signupActivity(id: number) {
  try { await http.post(`/party/activities/${id}/signup`, { userId: 1 }); loadActivities() } catch (e: any) { alert(e?.message) }
}
async function submitHousehold() {
  try { await http.post('/party/households', householdForm.value); showAddHousehold.value = false; loadHouseholds() } catch (e: any) { alert(e?.message) }
}
async function submitActivity() {
  try { await http.post('/party/activities', activityForm.value); showAddActivity.value = false; loadActivities() } catch (e: any) { alert(e?.message) }
}
async function submitMeeting() {
  try { await http.post('/party/meetings', meetingForm.value); showAddMeeting.value = false; loadMeetings() } catch (e: any) { alert(e?.message) }
}
async function generateAssessment() {
  try {
    const count = await http.post('/party/assessments/generate', { month: assessmentMonth.value })
    alert(`已生成 ${count} 条考核记录`)
    loadAssessments()
  } catch (e: any) { alert(e?.message) }
}

onMounted(() => {
  loadOverview()
  loadHouseholds()
  loadActivities()
  loadMeetings()
  loadAssessments()
})
</script>
