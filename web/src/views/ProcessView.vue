<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">流程中心</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">管理审核流程模板，配置节点和审核方式</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <p class="stat-label">模板总数</p>
        <p class="stat-value">{{ totalTemplates }}</p>
      </div>
      <div class="card card-border-green">
        <p class="stat-label">已启用</p>
        <p class="statusCount('ACTIVE')"></p>
      </div>
      <div class="card card-border-orange">
        <p class="stat-label">已停用</p>
        <p class="statusCount('INACTIVE')"></p>
      </div>
    </div>

    <!-- 流程模板列表 -->
    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">流程模板</h3>
        <button @click="showCreateDialog = true" style="padding:4px 10px;border:1px solid #1890ff;border-radius:4px;background:#1890ff;color:#fff;font-size:12px;cursor:pointer;">
          <i class="fas fa-plus"></i> 新建模板
        </button>
      </div>

      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:8px;font-size:13px;">加载中...</p>
      </div>
      <div v-else>
        <table class="table">
          <thead>
            <tr>
              <th>模板名称</th>
              <th>审核方式</th>
              <th>适用事件类型</th>
              <th>节点数</th>
              <th>状态</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in templates" :key="t.id">
              <td>{{ t.name || t.templateName || '-' }}</td>
              <td><span class="tag tag-blue">{{ auditModeLabel(t.auditMode || t.audit_mode) }}</span></td>
              <td style="font-size:12px;">{{ t.eventTypes || t.event_type || '全部' }}</td>
              <td style="text-align:center;">{{ t.nodeCount || t.node_count || '-' }}</td>
              <td><span :class="['tag', (t.status === 'ACTIVE' || t.status === 1) ? 'tag-green' : 'tag-orange']">{{ (t.status === 'ACTIVE' || t.status === 1) ? '启用' : '停用' }}</span></td>
              <td style="font-size:12px;">{{ t.createdAt || t.created_at || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!templates.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无流程模板</p>
      </div>
    </div>

    <!-- 新建模板对话框 -->
    <div v-if="showCreateDialog" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="width:420px;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">新建流程模板</h3>
        <div style="margin-bottom:12px;">
          <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">模板名称</label>
          <input v-model="newTemplate.name" placeholder="请输入模板名称" style="width:100%;padding:8px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;outline:none;" />
        </div>
        <div style="margin-bottom:12px;">
          <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">审核方式</label>
          <select v-model="newTemplate.auditMode" style="width:100%;padding:8px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
            <option value="SINGLE">单人审核</option>
            <option value="SERIAL">串行审核</option>
            <option value="PARALLEL">并行审核</option>
          </select>
        </div>
        <div style="margin-bottom:16px;">
          <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">适用事件类型</label>
          <input v-model="newTemplate.eventTypes" placeholder="多个类型用逗号分隔，留空表示全部" style="width:100%;padding:8px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;outline:none;" />
        </div>
        <div style="display:flex;justify-content:flex-end;gap:8px;">
          <button @click="showCreateDialog = false" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
          <button @click="createTemplate" :disabled="!newTemplate.name" style="padding:6px 16px;border:1px solid #1890ff;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
            确认创建
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getProcessTemplates, createProcessTemplate } from '../api'

const loading = ref(false)
const templates = ref<any[]>([])
const totalTemplates = ref(0)
const showCreateDialog = ref(false)
const newTemplate = ref<{ name: string; auditMode: string; eventTypes: string }>({ name: '', auditMode: 'SINGLE', eventTypes: '' })

function statusCount(status: string) {
  return templates.value.filter(t => t.status === status).length
}

function auditModeLabel(mode: string) {
  const map: any = { 'SINGLE': '单人审核', 'SERIAL': '串行审核', 'PARALLEL': '并行审核' }
  return map[mode] || mode || '单人审核'
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getProcessTemplates({ page: 1, pageSize: 50 })
    if (res && res.items) {
      templates.value = res.items
      totalTemplates.value = res.total || res.items.length
    } else if (Array.isArray(res)) {
      templates.value = res
      totalTemplates.value = res.length
    }
  } catch (e) {
    console.error('加载流程模板失败:', e)
  } finally {
    loading.value = false
  }
}

async function createTemplate() {
  if (!newTemplate.value.name) return
  try {
    await createProcessTemplate({
      name: newTemplate.value.name,
      auditMode: newTemplate.value.auditMode,
      eventTypes: newTemplate.value.eventTypes,
      status: 'ACTIVE'
    })
    showCreateDialog.value = false
    newTemplate.value = { name: '', auditMode: 'SINGLE', eventTypes: '' }
    loadData()
  } catch (e) {
    console.error('创建模板失败:', e)
  }
}

onMounted(() => {
  loadData()
})
</script>
