<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">智慧党建</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">党支部管理、党员联户、三会一课</p>

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
      <div class="card card-border-purple">
        <div style="display:flex;align-items:center;gap:12px;">
          <i class="fas fa-landmark" style="font-size:24px;color:#722ED1;"></i>
          <div>
            <p class="stat-label">党支部</p>
            <p class="stat-value">{{ overview.branchCount || 0 }}</p>
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
    <div style="display:flex;gap:8px;margin-bottom:20px;border-bottom:2px solid #e5e7eb;padding-bottom:12px;">
      <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
              :class="['filter-btn', activeTab === tab.key ? 'active' : '']">
        {{ tab.label }}
      </button>
    </div>

    <!-- ===== 党支部管理 ===== -->
    <div v-if="activeTab === 'branch'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;flex-wrap:wrap;gap:12px;">
        <div style="display:flex;gap:8px;align-items:center;">
          <h3 style="font-size:14px;font-weight:600;">党支部列表</h3>
          <input v-model="branchKeyword" class="form-input" placeholder="搜索党支部名称" style="width:240px;" @keyup.enter="loadBranches" />
          <button @click="loadBranches" class="btn btn-default" style="padding:6px 12px;">搜索</button>
        </div>
        <div style="display:flex;gap:8px;">
          <button @click="openImportModal" class="btn btn-default">导入</button>
          <button @click="openCreateBranch" class="btn btn-primary">创建党支部</button>
        </div>
      </div>
      <table class="table">
        <thead><tr>
          <th>党支部名称</th><th>联系网格</th><th>书记</th><th>成员数</th>
          <th>办公地址</th><th>联系电话</th><th>成立日期</th><th>状态</th>
          <th style="width:200px;">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="b in branches" :key="b.id">
            <td style="font-weight:600;">{{ b.branchName || '-' }}</td>
            <td style="font-size:12px;">{{ b.gridName || '-' }}</td>
            <td>{{ b.secretaryName || '<span style="color:#faad14;">未指派</span>' }}</td>
            <td>{{ b.memberCount || 0 }}</td>
            <td style="font-size:12px;">{{ b.address || '-' }}</td>
            <td style="font-size:12px;">{{ b.phone || '-' }}</td>
            <td style="font-size:12px;">{{ b.establishDate || '-' }}</td>
            <td><span class="tag" :class="b.status === 'ACTIVE' ? 'tag-green' : 'tag-gray'">
              {{ b.status === 'ACTIVE' ? '启用' : '停用' }}
            </span></td>
            <td>
              <button @click.stop="viewBranchDetail(b.id)" class="btn btn-primary" style="padding:4px 10px;font-size:12px;margin-right:4px;">查看详情</button>
              <button @click.stop="openEditBranch(b)" class="btn btn-default" style="padding:3px 10px;font-size:12px;border:1px solid #1890ff;color:#1890ff;margin-right:4px;">编辑</button>
              <button @click.stop="deleteBranch(b)" class="btn btn-default" style="padding:3px 10px;font-size:12px;border:1px solid #ffccc7;color:#ff4d4f;">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!branches.length" class="empty-state">暂无党支部，点击右上角「创建党支部」或「导入」快速建立</p>
    </div>

    <!-- 党支部详情抽屉 -->
    <div v-if="showBranchDetail" class="modal-overlay" @click.self="showBranchDetail = false">
      <div class="modal-box" style="width:720px;max-height:86vh;overflow-y:auto;">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
          <h3 style="font-size:16px;font-weight:600;">党支部详情：{{ currentBranch?.branchName }}</h3>
          <button @click="showBranchDetail = false" class="btn btn-default">关闭</button>
        </div>
        <div v-if="currentBranch" style="display:grid;grid-template-columns:1fr 1fr;gap:12px 24px;margin-bottom:20px;padding:12px;border:1px dashed #e5e7eb;border-radius:8px;background:#fafafa;">
          <div><span style="color:#6b7280;">联系网格：</span>{{ currentBranch.gridName || '-' }}</div>
          <div><span style="color:#6b7280;">支部书记：</span><span :style="{color: currentBranch.secretaryName ? '#374151' : '#faad14'}">{{ currentBranch.secretaryName || '未指派' }}</span></div>
          <div><span style="color:#6b7280;">办公地址：</span>{{ currentBranch.address || '-' }}</div>
          <div><span style="color:#6b7280;">联系电话：</span>{{ currentBranch.phone || '-' }}</div>
          <div><span style="color:#6b7280;">成立日期：</span>{{ currentBranch.establishDate || '-' }}</div>
          <div><span style="color:#6b7280;">状态：</span>{{ currentBranch.status === 'ACTIVE' ? '启用' : '停用' }}</div>
          <div style="grid-column:span 2;"><span style="color:#6b7280;">备注：</span>{{ currentBranch.remark || '-' }}</div>
        </div>

        <!-- 人员构成 -->
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
          <h4 style="font-size:14px;font-weight:600;">人员构成（书记 + 成员）</h4>
          <button @click="showAddMember = true" class="btn btn-primary">添加人员</button>
        </div>
        <table class="table">
          <thead><tr><th>角色</th><th>姓名</th><th>账号</th><th>入党日期</th><th>进入支部日期</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="m in currentBranchMembers" :key="m.relId">
              <td>
                <span class="tag" :class="m.role === 'SECRETARY' ? 'tag-red' : 'tag-blue'">
                  {{ m.role === 'SECRETARY' ? '书记' : '成员' }}
                </span>
              </td>
              <td style="font-weight:600;">{{ m.memberName }}</td>
              <td style="font-size:12px;">{{ m.memberAccount }}</td>
              <td style="font-size:12px;">{{ m.partyJoinDate || '-' }}</td>
              <td style="font-size:12px;">{{ m.joinedDate || '-' }}</td>
              <td>
                <button v-if="m.role === 'MEMBER'" @click.stop="setAsSecretary(m.memberId)" class="btn btn-default" style="padding:3px 10px;font-size:12px;border:1px solid #faad14;color:#faad14;margin-right:4px;">任命为书记</button>
                <button @click.stop="removeMember(m.memberId, m.memberName)" class="btn btn-default" style="padding:3px 10px;font-size:12px;border:1px solid #ffccc7;color:#ff4d4f;">移除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!currentBranchMembers.length" class="empty-state">暂无人员，请先添加党员</p>
      </div>
    </div>

    <!-- 创建/编辑党支部弹窗 -->
    <div v-if="showBranchForm" class="modal-overlay">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ branchForm.id ? '编辑党支部' : '创建党支部' }}</h3>
        <div class="form-group">
          <label class="form-label">党支部名称 <span style="color:#ff4d4f;">*</span></label>
          <input v-model="branchForm.branchName" class="form-input" placeholder="例：拔蛟窝社区党支部" />
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <div class="form-group">
            <label class="form-label">联系网格</label>
            <select v-model="branchForm.gridId" class="form-select">
              <option :value="null">未关联</option>
              <option v-for="g in gridOptions" :key="g.id" :value="Number(g.id)">{{ g.label }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">书记</label>
            <select v-model="branchForm.secretaryMemberId" class="form-select">
              <option :value="null">暂不指派</option>
              <option v-for="m in allMemberOptions" :key="m.memberId" :value="Number(m.memberId)">{{ m.memberName }} ({{ m.memberAccount }})</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">联系电话</label>
            <input v-model="branchForm.phone" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">成立日期</label>
            <input v-model="branchForm.establishDate" type="date" class="form-input" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">办公地址</label>
          <input v-model="branchForm.address" class="form-input" />
        </div>
        <div class="form-group">
          <label class="form-label">状态</label>
          <select v-model="branchForm.status" class="form-select">
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">停用</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">备注</label>
          <textarea v-model="branchForm.remark" rows="2" class="form-textarea"></textarea>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showBranchForm = false" class="btn btn-default">取消</button>
          <button @click="submitBranchForm" class="btn btn-primary">提交</button>
        </div>
      </div>
    </div>

    <!-- 导入党支部弹窗 -->
    <div v-if="showImportModal" class="modal-overlay">
      <div class="modal-box" style="width:720px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:8px;">导入党支部</h3>
        <p style="font-size:12px;color:#6b7280;margin-bottom:16px;">按格式粘贴党支部数据（每行一条），系统自动以「党支部名称」创建，已存在的会跳过。可通过 JSON 编辑器批量补充书记、网格、地址、电话、成立日期、备注。</p>
        <div style="margin-bottom:12px;display:flex;gap:8px;align-items:center;">
          <span style="font-size:12px;color:#6b7280;">快捷模板：</span>
          <button @click="appendTplRow" class="btn btn-default" style="padding:3px 10px;font-size:12px;">+ 添加一行</button>
          <button @click="clearImportRows" class="btn btn-default" style="padding:3px 10px;font-size:12px;">清空</button>
        </div>
        <table class="table" style="margin-bottom:12px;">
          <thead><tr>
            <th>党支部名称 *</th>
            <th>书记姓名</th>
            <th>联系网格ID</th>
            <th>电话</th>
            <th>成立日期</th>
            <th>操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="(row, idx) in importRows" :key="idx">
              <td><input v-model="row.branchName" class="form-input" style="padding:4px 6px;font-size:12px;" /></td>
              <td>
                <select v-model="row.secretaryMemberId" class="form-select" style="padding:4px 6px;font-size:12px;">
                  <option :value="null">-</option>
                  <option v-for="m in allMemberOptions" :key="m.memberId" :value="Number(m.memberId)">{{ m.memberName }}</option>
                </select>
              </td>
              <td><input v-model.number="row.gridId" type="number" class="form-input" style="padding:4px 6px;font-size:12px;width:90px;" /></td>
              <td><input v-model="row.phone" class="form-input" style="padding:4px 6px;font-size:12px;width:120px;" /></td>
              <td><input v-model="row.establishDate" type="date" class="form-input" style="padding:4px 6px;font-size:12px;" /></td>
              <td><button @click="importRows.splice(idx,1)" style="padding:3px 10px;font-size:12px;border:1px solid #ffccc7;border-radius:4px;background:#fff;color:#ff4d4f;cursor:pointer;">删除</button></td>
            </tr>
          </tbody>
        </table>
        <div v-if="importResult" style="margin-bottom:12px;padding:10px;border-radius:6px;font-size:12px;background:#f6ffed;border:1px solid #b7eb8f;color:#389e0d;">
          导入结果：共 {{ importResult.total }} 条，新增 {{ importResult.created }} 条，跳过 {{ importResult.skipped }} 条
          <div v-if="importResult.errors?.length" style="margin-top:6px;color:#cf1322;">
            <div v-for="(e, i) in importResult.errors" :key="i">⚠ {{ e }}</div>
          </div>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;">
          <button @click="showImportModal = false" class="btn btn-default">关闭</button>
          <button @click="submitImport" class="btn btn-primary">执行导入</button>
        </div>
      </div>
    </div>

    <!-- 添加支部成员弹窗 -->
    <div v-if="showAddMember" class="modal-overlay">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">添加党员到支部</h3>
        <div class="form-group">
          <label class="form-label">选择党员 <span style="color:#ff4d4f;">*</span></label>
          <select v-model="addMemberForm.memberId" class="form-select">
            <option :value="null">请选择</option>
            <option v-for="m in availableMembersForAdd" :key="m.memberId" :value="Number(m.memberId)">
              {{ m.memberName }} ({{ m.memberAccount }}){{ m.currentBranch ? ` — 原：${m.currentBranch}` : '' }}
            </option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">在支部中的角色</label>
          <select v-model="addMemberForm.role" class="form-select">
            <option value="MEMBER">成员</option>
            <option value="SECRETARY">书记（原书记会自动改为成员）</option>
          </select>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showAddMember = false" class="btn btn-default">取消</button>
          <button @click="submitAddMember" class="btn btn-primary">添加</button>
        </div>
      </div>
    </div>

    <!-- ===== 党员联户 ===== -->
    <div v-if="activeTab === 'household'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">党员联户台账</h3>
        <button @click="showAddHousehold = true" class="btn btn-primary">添加联户</button>
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
            <td><button @click="recordVisit(h.id)" class="btn btn-success" style="padding:4px 10px;font-size:12px;">走访</button></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!households.length" class="empty-state">暂无联户记录</p>
    </div>

    <!-- ===== 三会一课 ===== -->
    <div v-if="activeTab === 'meeting'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">三会一课</h3>
        <button @click="openAddMeeting" class="btn btn-primary">添加记录</button>
      </div>
      <table class="table">
        <thead><tr><th>类型</th><th>主题</th><th>日期</th><th>党支部</th><th>参会人数</th><th>状态</th><th style="width:130px;">操作</th></tr></thead>
        <tbody>
          <tr v-for="m in meetings" :key="m.id">
            <td><span class="tag tag-red">{{ m.meetingType }}</span></td>
            <td>{{ m.title }}</td>
            <td style="font-size:12px;">{{ m.meetingDate }}</td>
            <td style="font-size:12px;">{{ m.partyBranch || '-' }}</td>
            <td>{{ m.participantCount || '-' }}</td>
            <td><span class="tag" :class="m.status === 'COMPLETED' ? 'tag-green' : 'tag-orange'">{{ m.status === 'COMPLETED' ? '已完成' : '计划中' }}</span></td>
            <td>
              <button @click="openEditMeeting(m)" style="padding:3px 10px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;margin-right:4px;">编辑</button>
              <button @click="deleteMeeting(m)" style="padding:3px 10px;border:1px solid #ffccc7;border-radius:4px;background:#fff;color:#ff4d4f;font-size:12px;cursor:pointer;">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!meetings.length" class="empty-state">暂无记录</p>
    </div>

    <!-- 添加联户弹窗 -->
    <div v-if="showAddHousehold" class="modal-overlay">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">添加联户</h3>
        <div class="form-group">
          <label class="form-label">关联党员 <span style="color:#ff4d4f;">*</span></label>
          <select v-model="householdForm.partyMemberId" class="form-select">
            <option :value="null">请选择</option>
            <option v-for="m in allMemberOptions" :key="m.memberId" :value="Number(m.memberId)">{{ m.memberName }} ({{ m.memberAccount }})</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">户主姓名 <span style="color:#ff4d4f;">*</span></label>
          <input v-model="householdForm.householdName" class="form-input" />
        </div>
        <div class="form-group">
          <label class="form-label">地址</label>
          <input v-model="householdForm.householdAddress" class="form-input" />
        </div>
        <div class="form-group">
          <label class="form-label">所属网格 <span style="color:#ff4d4f;">*</span></label>
          <select v-model="householdForm.gridId" class="form-select">
            <option :value="null">请选择网格</option>
            <option v-for="g in gridOptions" :key="g.id" :value="Number(g.id)">{{ g.label }}</option>
          </select>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showAddHousehold = false" class="btn btn-default">取消</button>
          <button @click="submitHousehold" class="btn btn-primary">提交</button>
        </div>
      </div>
    </div>

    <!-- 添加会议弹窗 -->
    <div v-if="showAddMeeting" class="modal-overlay">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">添加会议记录</h3>
        <div class="form-group">
          <label class="form-label">会议类型</label>
          <select v-model="meetingForm.meetingType" class="form-select">
            <option value="支部党员大会">支部党员大会</option>
            <option value="支委会">支委会</option>
            <option value="党小组会">党小组会</option>
            <option value="党课">党课</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">主题</label>
          <input v-model="meetingForm.title" class="form-input" />
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <div class="form-group">
            <label class="form-label">日期</label>
            <input v-model="meetingForm.meetingDate" type="date" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">参会人数</label>
            <input v-model.number="meetingForm.participantCount" type="number" class="form-input" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">党支部</label>
          <select v-model="meetingForm.partyBranch" class="form-select">
            <option value="">请选择</option>
            <option v-for="b in branches" :key="b.id" :value="b.branchName">{{ b.branchName }}</option>
          </select>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showAddMeeting = false" class="btn btn-default">取消</button>
          <button @click="submitMeeting" class="btn btn-primary">提交</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import http from '../api'
import { showMessage } from '../utils/message'
import { confirmDialog } from '../utils/dialog'

// 网格下拉
const gridOptions = ref<any[]>([])
async function loadGridOptions() {
  try {
    const tree: any = await http.get('/community/grids/tree') || []
    gridOptions.value = []
    const flatten = (nodes: any[], prefix = '') => {
      for (const n of nodes) {
        const label = prefix ? `${prefix} > ${n.gridName}` : n.gridName
        gridOptions.value.push({ id: n.id, label })
        if (n.children) flatten(n.children, label)
      }
    }
    flatten(Array.isArray(tree) ? tree : [])
  } catch (e) { /* ignore */ }
}

// 全体在册党员下拉（添加联户/指定书记/支部成员时用）
const allMemberOptions = ref<any[]>([])
async function loadAllMemberOptions() {
  try {
    allMemberOptions.value = await http.get('/party/members/available') || []
  } catch (e) { /* ignore */ }
}

// Tab 配置（已取消：志愿服务、量化考核、党建任务、党群议事、政策推送）
const tabs = [
  { key: 'branch',    label: '党支部管理' },
  { key: 'household', label: '党员联户' },
  { key: 'meeting',   label: '三会一课' },
]
const activeTab = ref('branch')
const tabLoaded = ref<Record<string, boolean>>({ branch: false, household: false, meeting: false })

// 概览
const overview = ref<any>({})

// ============ 党支部管理 ============
const branches = ref<any[]>([])
const branchKeyword = ref('')
const showBranchForm = ref(false)
const showBranchDetail = ref(false)
const showImportModal = ref(false)
const showAddMember = ref(false)

const branchForm = ref<{
  id: number | null; branchName: string; secretaryMemberId: number | null; gridId: number | null;
  address: string; phone: string; establishDate: string; status: string; remark: string;
}>({ id: null, branchName: '', secretaryMemberId: null, gridId: null, address: '', phone: '', establishDate: '', status: 'ACTIVE', remark: '' })

const currentBranch = ref<any>(null)
const currentBranchMembers = ref<any[]>([])
const availableMembersForAdd = ref<any[]>([])

const addMemberForm = ref({ memberId: null as number | null, role: 'MEMBER' })

// 导入
const importRows = ref<any[]>([])
const importResult = ref<any>(null)
function appendTplRow() {
  importRows.value.push({ branchName: '', secretaryMemberId: null, gridId: null, phone: '', establishDate: '', address: '', remark: '' })
}
function clearImportRows() { importRows.value = []; importResult.value = null }

async function loadBranches() {
  try { branches.value = await http.get('/party/branches', { params: { keyword: branchKeyword.value || undefined } }) || [] }
  catch (e) { /* ignore */ }
}
async function viewBranchDetail(id: number) {
  try {
    const d: any = await http.get(`/party/branches/${id}`) || {}
    currentBranch.value = d
    currentBranchMembers.value = d.members || []
    availableMembersForAdd.value = d.availableMembers || []
    showBranchDetail.value = true
  } catch (e: any) { showMessage(e?.message) }
}
function openCreateBranch() {
  branchForm.value = { id: null, branchName: '', secretaryMemberId: null, gridId: null, address: '', phone: '', establishDate: '', status: 'ACTIVE', remark: '' }
  showBranchForm.value = true
}
function openEditBranch(b: any) {
  branchForm.value = {
    id: b.id,
    branchName: b.branchName || '',
    secretaryMemberId: b.secretaryMemberId ?? null,
    gridId: b.gridId ?? null,
    address: b.address || '',
    phone: b.phone || '',
    establishDate: b.establishDate || '',
    status: b.status || 'ACTIVE',
    remark: b.remark || '',
  }
  showBranchForm.value = true
}
async function submitBranchForm() {
  if (!branchForm.value.branchName.trim()) { showMessage('请填写党支部名称'); return }
  try {
    if (branchForm.value.id) {
      await http.put(`/party/branches/${branchForm.value.id}`, branchForm.value)
    } else {
      await http.post('/party/branches', branchForm.value)
    }
    showBranchForm.value = false
    await loadBranches()
    showMessage('保存成功')
  } catch (e: any) { showMessage(e?.message) }
}
async function deleteBranch(b: any) {
  const ok = await confirmDialog({ title: '删除党支部', message: `确认删除党支部「${b.branchName || ''}」？其人员关联也会一并解除。` })
  if (!ok) return
  try { await http.delete(`/party/branches/${b.id}`); await loadBranches(); showMessage('已删除') }
  catch (e: any) { showMessage(e?.message) }
}

// 导入
function openImportModal() {
  clearImportRows()
  if (!importRows.value.length) appendTplRow()
  showImportModal.value = true
}
async function submitImport() {
  const rows = importRows.value.filter((r: any) => r.branchName && r.branchName.trim())
  if (!rows.length) { showMessage('请至少填写一行党支部名称'); return }
  try {
    const r = await http.post('/party/branches/import', { branches: rows }) || {}
    importResult.value = r
    if ((r.created || 0) > 0) { await loadBranches() }
  } catch (e: any) { showMessage(e?.message) }
}

// 支部成员操作
async function submitAddMember() {
  if (!addMemberForm.value.memberId) { showMessage('请选择党员'); return }
  if (!currentBranch.value?.id) return
  try {
    await http.post(`/party/branches/${currentBranch.value.id}/members`, addMemberForm.value)
    showAddMember.value = false
    addMemberForm.value = { memberId: null, role: 'MEMBER' }
    await viewBranchDetail(currentBranch.value.id)
    await loadAllMemberOptions()
    showMessage('添加成功')
  } catch (e: any) { showMessage(e?.message) }
}
async function setAsSecretary(memberId: number) {
  if (!currentBranch.value?.id) return
  const ok = await confirmDialog({ title: '任命书记', message: '确认将该成员任命为支部书记？原书记会自动改为成员。' })
  if (!ok) return
  try {
    await http.post(`/party/branches/${currentBranch.value.id}/members`, { memberId, role: 'SECRETARY' })
    await viewBranchDetail(currentBranch.value.id)
    showMessage('已任命为书记')
  } catch (e: any) { showMessage(e?.message) }
}
async function removeMember(memberId: number, name: string) {
  if (!currentBranch.value?.id) return
  const ok = await confirmDialog({ title: '移除成员', message: `确认将「${name || ''}」从当前支部移除？` })
  if (!ok) return
  try {
    await http.delete(`/party/branches/${currentBranch.value.id}/members/${memberId}`)
    await viewBranchDetail(currentBranch.value.id)
    await loadAllMemberOptions()
    showMessage('已移除')
  } catch (e: any) { showMessage(e?.message) }
}

// ============ 党员联户 + 三会一课 ============
const households = ref<any[]>([])
const meetings = ref<any[]>([])
const showAddHousehold = ref(false)
const showAddMeeting = ref(false)

const householdForm = ref({ partyMemberId: null as number | null, householdName: '', householdAddress: '', gridId: null as number | null })
const meetingForm = ref<{ id: number | null; meetingType: string; title: string; meetingDate: string; partyBranch: string; participantCount: number | null; status?: string }>(
  { id: null, meetingType: '支部党员大会', title: '', meetingDate: '', partyBranch: '', participantCount: null }
)

async function loadOverview() {
  try { overview.value = await http.get('/party/overview') || {} } catch (e) {}
}
async function loadHouseholds() { try { households.value = await http.get('/party/households') || [] } catch (e) {} }
async function loadMeetings()   { try { meetings.value   = await http.get('/party/meetings')   || [] } catch (e) {} }

async function recordVisit(id: number) {
  try { await http.post(`/party/households/${id}/visit`, {}); showMessage('走访记录成功！'); await loadHouseholds() }
  catch (e: any) { showMessage('走访失败：' + (e?.message || '未知错误')) }
}
async function submitHousehold() {
  if (!householdForm.value.partyMemberId) { showMessage('请选择关联党员'); return }
  if (!householdForm.value.gridId) { showMessage('请选择所属网格'); return }
  if (!householdForm.value.householdName?.trim()) { showMessage('请填写户主姓名'); return }
  try { await http.post('/party/households', householdForm.value); showAddHousehold.value = false; loadHouseholds() }
  catch (e: any) { showMessage(e?.message) }
}
async function submitMeeting() {
  try {
    if (meetingForm.value.id) await http.put(`/party/meetings/${meetingForm.value.id}`, meetingForm.value)
    else await http.post('/party/meetings', meetingForm.value)
    showAddMeeting.value = false; loadMeetings()
  } catch (e: any) { showMessage(e?.message) }
}
function openAddMeeting() {
  meetingForm.value = { id: null, meetingType: '支部党员大会', title: '', meetingDate: '', partyBranch: '', participantCount: null }
  showAddMeeting.value = true
}
function openEditMeeting(m: any) {
  meetingForm.value = {
    id: m.id, meetingType: m.meetingType || '支部党员大会', title: m.title || '',
    meetingDate: m.meetingDate || '', partyBranch: m.partyBranch || '',
    participantCount: m.participantCount ?? null, status: m.status || 'COMPLETED',
  }
  showAddMeeting.value = true
}
async function deleteMeeting(m: any) {
  const ok = await confirmDialog({ title: '删除会议', message: `确定删除会议「${m.title || ''}」吗？` })
  if (!ok) return
  try { await http.delete(`/party/meetings/${m.id}`); loadMeetings() }
  catch (e: any) { showMessage(e?.message) }
}

// Tab 懒加载
watch(activeTab, (newTab) => {
  if (!tabLoaded.value[newTab]) {
    tabLoaded.value[newTab] = true
    // 各 tab 首次打开时确保分支列表已加载（会议/联户均可能用到支部作为数据来源）
    if (!branches.value.length) loadBranches()
    switch (newTab) {
      case 'branch':    if (!branches.value.length) loadBranches(); break
      case 'household': loadHouseholds(); break
      case 'meeting':   loadMeetings(); break
    }
  }
})

onMounted(async () => {
  loadGridOptions()
  await loadAllMemberOptions()
  await loadOverview()
  // 默认加载党支部列表（已在 watch 中处理懒加载）
  tabLoaded.value.branch = true
  loadBranches()
})
</script>

<style scoped>
.card-border-purple { border-left: 4px solid #722ED1; }
</style>
