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
    <div style="display:flex;gap:8px;margin-bottom:20px;border-bottom:2px solid #e5e7eb;padding-bottom:12px;">
      <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
              :class="['filter-btn', activeTab === tab.key ? 'active' : '']">
        {{ tab.label }}
      </button>
    </div>

    <!-- 党员联户 -->
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

    <!-- 志愿服务 -->
    <div v-if="activeTab === 'activity'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">志愿服务活动</h3>
        <button @click="showAddActivity = true" class="btn btn-primary">创建活动</button>
      </div>
      <table class="table">
        <thead><tr><th>活动标题</th><th>日期</th><th>状态</th><th>已报名</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="a in activities" :key="a.id">
            <td>{{ a.title }}</td>
            <td style="font-size:12px;">{{ a.activityDate }}</td>
            <td><span class="tag" :class="a.status === 'COMPLETED' ? 'tag-green' : a.status === 'ONGOING' ? 'tag-blue' : 'tag-orange'">{{ activityStatus(a.status) }}</span></td>
            <td>{{ a.attendedCount || 0 }}/{{ a.maxParticipants || '∞' }}</td>
            <td><button v-if="a.status === 'PLANNED'" @click="signupActivity(a.id)" class="btn btn-success" style="padding:4px 10px;font-size:12px;">报名</button></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!activities.length" class="empty-state">暂无活动</p>
    </div>

    <!-- 三会一课 -->
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

    <!-- 量化考核 -->
    <div v-if="activeTab === 'assessment'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">党员量化考核</h3>
        <div style="display:flex;gap:8px;">
          <input v-model="assessmentMonth" type="month" class="form-input" style="width:auto;" />
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
      <p v-if="!assessments.length" class="empty-state">暂无考核数据</p>
    </div>

    <!-- 党建任务 -->
    <div v-if="activeTab === 'task'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">党建任务下沉</h3>
        <button @click="showAddTask = true" class="btn btn-primary">下发任务</button>
      </div>
      <table class="table">
        <thead><tr><th>任务标题</th><th>类型</th><th>网格</th><th>指派党员</th><th>截止日</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="t in tasks" :key="t.id">
            <td>{{ t.taskTitle }}</td>
            <td><span class="tag tag-red">{{ taskTypeLabel(t.taskType) }}</span></td>
            <td style="font-size:12px;">{{ t.gridName || '-' }}</td>
            <td>{{ t.memberName || '待领办' }}</td>
            <td style="font-size:12px;">{{ t.deadline || '-' }}</td>
            <td><span class="tag" :class="taskStatusClass(t.status)">{{ taskStatusLabel(t.status) }}</span></td>
            <td>
              <button v-if="t.status === 'PENDING'" @click="acceptTask(t.id)" class="btn btn-primary" style="padding:4px 10px;font-size:12px;">领办</button>
              <button v-if="t.status === 'ACCEPTED'" @click="completeTask(t.id)" class="btn btn-success" style="padding:4px 10px;font-size:12px;">完成</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!tasks.length" class="empty-state">暂无任务</p>
    </div>

    <!-- 党群议事 -->
    <div v-if="activeTab === 'deliberation'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">线上党群议事</h3>
        <button @click="showAddDeliberation = true" class="btn btn-primary">发起议事</button>
      </div>
      <div v-for="d in deliberations" :key="d.id" class="card" style="margin-bottom:12px;background:#fafafa;">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;">
          <div style="flex:1;">
            <h4 style="font-size:14px;font-weight:600;margin-bottom:4px;">{{ d.title }}</h4>
            <p style="font-size:12px;color:#6b7280;margin-bottom:8px;">{{ d.content }}</p>
            <div style="display:flex;gap:16px;font-size:12px;color:#374151;">
              <span style="color:#52c41a;">👍 赞成 {{ d.supportCount || 0 }}</span>
              <span style="color:#ff4d4f;">👎 反对 {{ d.opposeCount || 0 }}</span>
              <span style="color:#9ca3af;">➖ 弃权 {{ d.abstainCount || 0 }}</span>
              <span class="tag" :class="d.status === 'OPEN' ? 'tag-blue' : 'tag-gray'">{{ d.status === 'OPEN' ? '征集中' : '已结项' }}</span>
            </div>
          </div>
          <div style="display:flex;gap:6px;margin-left:16px;">
            <button v-if="d.status === 'OPEN'" @click="openVoteModal(d)" class="btn btn-primary" style="padding:4px 10px;font-size:12px;">投票</button>
            <button v-if="d.status === 'OPEN'" @click="closeDeliberation(d.id)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">结项</button>
          </div>
        </div>
      </div>
      <p v-if="!deliberations.length" class="empty-state">暂无议事议题</p>
    </div>

    <!-- 政策推送 -->
    <div v-if="activeTab === 'policyPush'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">政策宣传一键推送</h3>
        <button @click="showPushModal = true" class="btn btn-primary">推送政策</button>
      </div>
      <table class="table">
        <thead><tr><th>政策标题</th><th>类型</th><th>推送目标</th><th>推送人次</th><th>推送时间</th><th>推送人</th></tr></thead>
        <tbody>
          <tr v-for="p in policyPushes" :key="p.id">
            <td>{{ p.policyTitle || '-' }}</td>
            <td><span class="tag tag-blue">{{ p.policyType || '-' }}</span></td>
            <td>{{ pushTargetLabel(p.pushTarget) }}</td>
            <td>{{ p.pushCount || 0 }}</td>
            <td style="font-size:12px;">{{ p.createdAt }}</td>
            <td>{{ p.creatorName || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!policyPushes.length" class="empty-state">暂无推送记录</p>
    </div>

  </div>

  <!-- 弹窗放在组件根级别，避免被 overflow 裁剪 -->
  <!-- 添加联户弹窗 -->
  <div v-if="showAddHousehold" class="modal-overlay">
    <div class="modal-box">
      <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">添加联户</h3>
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
          <option v-for="g in gridOptions" :key="g.id" :value="g.id">{{ g.label }}</option>
        </select>
      </div>
      <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
        <button @click="showAddHousehold = false" class="btn btn-default">取消</button>
        <button @click="submitHousehold" class="btn btn-primary">提交</button>
      </div>
    </div>
  </div>

  <!-- 添加活动弹窗 -->
  <div v-if="showAddActivity" class="modal-overlay">
    <div class="modal-box">
      <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">创建志愿活动</h3>
      <div class="form-group">
        <label class="form-label">活动标题</label>
        <input v-model="activityForm.title" class="form-input" />
      </div>
      <div class="form-group">
        <label class="form-label">描述</label>
        <textarea v-model="activityForm.description" rows="2" class="form-textarea"></textarea>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
        <div class="form-group">
          <label class="form-label">活动日期</label>
          <input v-model="activityForm.activityDate" type="date" class="form-input" />
        </div>
        <div class="form-group">
          <label class="form-label">最大人数</label>
          <input v-model.number="activityForm.maxParticipants" type="number" class="form-input" />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">所属网格 <span style="color:#ff4d4f;">*</span></label>
        <select v-model="activityForm.gridId" class="form-select">
          <option :value="null">请选择网格</option>
          <option v-for="g in gridOptions" :key="g.id" :value="g.id">{{ g.label }}</option>
        </select>
      </div>
      <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
        <button @click="showAddActivity = false" class="btn btn-default">取消</button>
        <button @click="submitActivity" class="btn btn-primary">提交</button>
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
        <input v-model="meetingForm.partyBranch" class="form-input" />
      </div>
      <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
        <button @click="showAddMeeting = false" class="btn btn-default">取消</button>
        <button @click="submitMeeting" class="btn btn-primary">提交</button>
      </div>
    </div>
  </div>

  <!-- 下发任务弹窗 -->
  <div v-if="showAddTask" class="modal-overlay">
    <div class="modal-box">
      <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">下发党建任务</h3>
      <div class="form-group">
        <label class="form-label">任务标题</label>
        <input v-model="taskForm.taskTitle" class="form-input" />
      </div>
      <div class="form-group">
        <label class="form-label">任务类型</label>
        <select v-model="taskForm.taskType" class="form-select">
          <option value="PATROL">巡查</option>
          <option value="MEDIATION">矛盾调解</option>
          <option value="VISIT">走访</option>
          <option value="MEETING">三会一课</option>
          <option value="PUBLICITY">政策宣传</option>
          <option value="OTHER">其他</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">描述</label>
        <textarea v-model="taskForm.description" rows="2" class="form-textarea"></textarea>
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
        <div class="form-group">
          <label class="form-label">所属网格 <span style="color:#ff4d4f;">*</span></label>
          <select v-model="taskForm.gridId" class="form-select">
            <option :value="null">请选择网格</option>
            <option v-for="g in gridOptions" :key="g.id" :value="g.id">{{ g.label }}</option>
          </select>
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">截止日期</label>
        <input v-model="taskForm.deadline" type="date" class="form-input" />
      </div>
      <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
        <button @click="showAddTask = false" class="btn btn-default">取消</button>
        <button @click="submitTask" class="btn btn-primary">提交</button>
      </div>
    </div>
  </div>

  <!-- 发起议事弹窗 -->
  <div v-if="showAddDeliberation" class="modal-overlay">
    <div class="modal-box">
      <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">发起党群议事</h3>
      <div class="form-group">
        <label class="form-label">议事议题</label>
        <input v-model="deliberationForm.title" class="form-input" />
      </div>
      <div class="form-group">
        <label class="form-label">议事内容</label>
        <textarea v-model="deliberationForm.content" rows="3" class="form-textarea"></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">所属网格 <span style="color:#ff4d4f;">*</span></label>
        <select v-model="deliberationForm.gridId" class="form-select">
          <option :value="null">请选择网格</option>
          <option v-for="g in gridOptions" :key="g.id" :value="g.id">{{ g.label }}</option>
        </select>
      </div>
      <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
        <button @click="showAddDeliberation = false" class="btn btn-default">取消</button>
        <button @click="submitDeliberation" class="btn btn-primary">提交</button>
      </div>
    </div>
  </div>

  <!-- 投票弹窗 -->
  <div v-if="showVoteModal" class="modal-overlay">
    <div class="modal-box">
      <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">投票：{{ votingDeliberation?.title }}</h3>
      <div class="form-group">
        <label class="form-label">您的选择</label>
        <div style="display:flex;gap:12px;">
          <button @click="voteForm.voteType = 'SUPPORT'" :class="['btn', voteForm.voteType === 'SUPPORT' ? 'btn-success' : 'btn-default']">👍 赞成</button>
          <button @click="voteForm.voteType = 'OPPOSE'" :class="['btn', voteForm.voteType === 'OPPOSE' ? 'btn-danger' : 'btn-default']">👎 反对</button>
          <button @click="voteForm.voteType = 'ABSTAIN'" :class="['btn', voteForm.voteType === 'ABSTAIN' ? 'btn-warning' : 'btn-default']">➖ 弃权</button>
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">意见（选填）</label>
        <textarea v-model="voteForm.comment" rows="2" class="form-textarea"></textarea>
      </div>
      <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
        <button @click="showVoteModal = false" class="btn btn-default">取消</button>
        <button @click="submitVote" class="btn btn-primary">提交投票</button>
      </div>
    </div>
  </div>

  <!-- 推送政策弹窗 -->
  <div v-if="showPushModal" class="modal-overlay">
    <div class="modal-box">
      <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">推送政策</h3>
      <div class="form-group">
        <label class="form-label">选择政策</label>
        <select v-model.number="pushForm.policyId" class="form-select">
          <option v-for="p in policyOptions" :key="p.id" :value="p.id">{{ p.title }} ({{ p.policyType }})</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">推送目标</label>
        <div style="display:flex;gap:12px;">
          <button @click="pushForm.pushTarget = 'GRID'" :class="['btn', pushForm.pushTarget === 'GRID' ? 'btn-primary' : 'btn-default']">网格</button>
          <button @click="pushForm.pushTarget = 'POPULATION'" :class="['btn', pushForm.pushTarget === 'POPULATION' ? 'btn-primary' : 'btn-default']">全体居民</button>
          <button @click="pushForm.pushTarget = 'ALL'" :class="['btn', pushForm.pushTarget === 'ALL' ? 'btn-primary' : 'btn-default']">全员</button>
        </div>
      </div>
      <div v-if="pushForm.pushTarget === 'GRID'" class="form-group">
        <label class="form-label">目标网格ID</label>
        <input v-model.number="pushForm.gridId" type="number" class="form-input" />
      </div>
      <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
        <button @click="showPushModal = false" class="btn btn-default">取消</button>
        <button @click="submitPush" class="btn btn-primary">确认推送</button>
      </div>
    </div>
  </div>

</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import http from '../api'
import { showMessage } from '../utils/message'

// 网格下拉数据源：三级网格树平铺（名称带层级前缀，便于选择）
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
  } catch (e) { /* 网格加载失败时下拉为空 */ }
}

const tabs = [
  { key: 'household', label: '党员联户' },
  { key: 'activity', label: '志愿服务' },
  { key: 'meeting', label: '三会一课' },
  { key: 'assessment', label: '量化考核' },
  { key: 'task', label: '党建任务' },
  { key: 'deliberation', label: '党群议事' },
  { key: 'policyPush', label: '政策推送' },
]
const activeTab = ref('household')

const tabLoaded = ref<Record<string, boolean>>({
  household: false,
  activity: false,
  meeting: false,
  assessment: false,
  task: false,
  deliberation: false,
  policyPush: false,
})
const overview = ref<any>({})
const households = ref<any[]>([])
const activities = ref<any[]>([])
const meetings = ref<any[]>([])
const assessments = ref<any[]>([])
const tasks = ref<any[]>([])
const deliberations = ref<any[]>([])
const policyPushes = ref<any[]>([])
const policyOptions = ref<any[]>([])
const assessmentMonth = ref(new Date().toISOString().slice(0, 7))
const showAddHousehold = ref(false)
const showAddActivity = ref(false)
const showAddMeeting = ref(false)
const showAddTask = ref(false)
const showAddDeliberation = ref(false)
const showVoteModal = ref(false)
const showPushModal = ref(false)
const votingDeliberation = ref<any>(null)

const householdForm = ref({ partyMemberId: null as number | null, householdName: '', householdAddress: '', gridId: null as number | null })
const activityForm = ref({ title: '', description: '', activityDate: '', gridId: null as number | null, maxParticipants: null as number | null, createdBy: 1 })
const meetingForm = ref<{ id: number | null; meetingType: string; title: string; meetingDate: string; partyBranch: string; participantCount: number | null; status?: string }>({ id: null, meetingType: '支部党员大会', title: '', meetingDate: '', partyBranch: '', participantCount: null })
const taskForm = ref({ taskTitle: '', taskType: 'PATROL', description: '', gridId: null as number | null, assignedMemberId: null as number | null, deadline: '', createdBy: 1 })
const deliberationForm = ref({ title: '', content: '', gridId: null as number | null, createdBy: 1 })
const voteForm = ref({ voteType: 'SUPPORT', comment: '' })
const pushForm = ref({ policyId: null as number | null, pushTarget: 'GRID', gridId: null as number | null })

function activityStatus(status: string) {
  const map: Record<string, string> = { PLANNED: '计划中', ONGOING: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[status] || status
}
function taskTypeLabel(t: string) {
  const map: Record<string, string> = { PATROL: '巡查', MEDIATION: '矛盾调解', VISIT: '走访', MEETING: '三会一课', PUBLICITY: '政策宣传', OTHER: '其他' }
  return map[t] || t
}
function taskStatusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待领办', ACCEPTED: '已领办', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[s] || s
}
function taskStatusClass(s: string) {
  const map: Record<string, string> = { PENDING: 'tag-orange', ACCEPTED: 'tag-blue', COMPLETED: 'tag-green', CANCELLED: 'tag-gray' }
  return map[s] || ''
}
function pushTargetLabel(t: string) {
  const map: Record<string, string> = { GRID: '网格', POPULATION: '全体居民', ALL: '全员' }
  return map[t] || t
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
async function loadTasks() {
  try { tasks.value = await http.get('/party/tasks') || [] } catch (e) {}
}
async function loadDeliberations() {
  try { deliberations.value = await http.get('/party/deliberations') || [] } catch (e) {}
}
async function loadPolicyPushes() {
  try { policyPushes.value = await http.get('/party/policy-push') || [] } catch (e) {}
}
async function loadPolicyOptions() {
  try { policyOptions.value = await http.get('/party/policy-resources') || [] } catch (e) {}
}

async function recordVisit(id: number) {
  try {
    await http.post(`/party/households/${id}/visit`, {})
    showMessage('走访记录成功！')
    await loadHouseholds()
  } catch (e: any) {
    showMessage('走访失败：' + (e?.message || '未知错误'))
  }
}
async function signupActivity(id: number) {
  try {
    await http.post(`/party/activities/${id}/signup`, { userId: 1 })
    showMessage('报名成功！')
    await loadActivities()
  } catch (e: any) {
    showMessage('报名失败：' + (e?.message || '未知错误'))
  }
}
async function submitHousehold() {
  if (!householdForm.value.partyMemberId) { showMessage('请选择关联党员'); return }
  if (!householdForm.value.gridId) { showMessage('请选择所属网格'); return }
  if (!householdForm.value.householdName?.trim()) { showMessage('请填写户主姓名'); return }
  try { await http.post('/party/households', householdForm.value); showAddHousehold.value = false; loadHouseholds() } catch (e: any) { showMessage(e?.message) }
}
async function submitActivity() {
  if (!activityForm.value.gridId) { showMessage('请选择所属网格'); return }
  try { await http.post('/party/activities', activityForm.value); showAddActivity.value = false; loadActivities() } catch (e: any) { showMessage(e?.message) }
}
async function submitMeeting() {
  try {
    if (meetingForm.value.id) {
      await http.put(`/party/meetings/${meetingForm.value.id}`, meetingForm.value)
    } else {
      await http.post('/party/meetings', meetingForm.value)
    }
    showAddMeeting.value = false
    loadMeetings()
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
  if (!confirm(`确定删除会议「${m.title || ''}」吗？`)) return
  try { await http.delete(`/party/meetings/${m.id}`); loadMeetings() } catch (e: any) { showMessage(e?.message) }
}
async function submitTask() {
  if (!taskForm.value.gridId) { showMessage('请选择所属网格'); return }
  try { await http.post('/party/tasks', taskForm.value); showAddTask.value = false; loadTasks() } catch (e: any) { showMessage(e?.message) }
}
async function acceptTask(id: number) {
  try { await http.post(`/party/tasks/${id}/accept`, { memberId: 1 }); loadTasks() } catch (e: any) { showMessage(e?.message) }
}
async function completeTask(id: number) {
  try { await http.post(`/party/tasks/${id}/complete`, {}); loadTasks() } catch (e: any) { showMessage(e?.message) }
}
async function submitDeliberation() {
  if (!deliberationForm.value.gridId) { showMessage('请选择所属网格'); return }
  try { await http.post('/party/deliberations', deliberationForm.value); showAddDeliberation.value = false; loadDeliberations() } catch (e: any) { showMessage(e?.message) }
}
function openVoteModal(d: any) {
  votingDeliberation.value = d
  voteForm.value = { voteType: 'SUPPORT', comment: '' }
  showVoteModal.value = true
}
async function submitVote() {
  try {
    await http.post(`/party/deliberations/${votingDeliberation.value.id}/vote`, voteForm.value)
    showVoteModal.value = false
    showMessage('投票成功！')
    loadDeliberations()
  } catch (e: any) { showMessage(e?.message) }
}
async function closeDeliberation(id: number) {
  try { await http.post(`/party/deliberations/${id}/close`, {}); loadDeliberations() } catch (e: any) { showMessage(e?.message) }
}
async function submitPush() {
  try {
    const res = await http.post('/party/policy-push', pushForm.value)
    showPushModal.value = false
    showMessage(`推送成功！覆盖 ${res?.pushCount || 0} 人次`)
    loadPolicyPushes()
  } catch (e: any) { showMessage(e?.message) }
}
async function generateAssessment() {
  try {
    const count = await http.post('/party/assessments/generate', { month: assessmentMonth.value })
    showMessage(`已生成 ${count} 条考核记录`)
    loadAssessments()
  } catch (e: any) { showMessage(e?.message) }
}

watch(activeTab, (newTab) => {
  if (!tabLoaded.value[newTab]) {
    tabLoaded.value[newTab] = true
    switch (newTab) {
      case 'household':
        loadHouseholds()
        break
      case 'activity':
        loadActivities()
        break
      case 'meeting':
        loadMeetings()
        break
      case 'assessment':
        loadAssessments()
        break
      case 'task':
        loadTasks()
        break
      case 'deliberation':
        loadDeliberations()
        break
      case 'policyPush':
        loadPolicyPushes()
        loadPolicyOptions()
        break
    }
  }
})

onMounted(() => {
  loadGridOptions()
  loadOverview()
  tabLoaded.value.household = true
  loadHouseholds()
})
</script>