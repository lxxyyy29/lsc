<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">派单规则</h2>
        <p style="font-size:13px;color:#6b7280;">配置事件类型 → 受理角色映射，派单时按规则自动路由（未配置的事件类型默认派给网格员）</p>
      </div>
      <button @click="openCreate" style="padding:8px 16px;border:none;border-radius:6px;background:#722ed1;color:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-plus"></i> 新增规则
      </button>
    </div>

    <div class="card">
      <table class="table" style="table-layout:fixed;">
        <colgroup>
          <col style="width:150px;" />
          <col style="width:120px;" />
          <col style="width:80px;" />
          <col style="width:80px;" />
          <col />
          <col style="width:140px;" />
        </colgroup>
        <thead>
          <tr>
            <th>事件类型</th>
            <th>目标角色</th>
            <th style="text-align:center;">优先级</th>
            <th>状态</th>
            <th>备注</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in rules" :key="r.id">
            <td style="font-weight:500;word-break:break-all;">{{ getEventTypeName(r.eventType) }}</td>
            <td><span class="tag tag-purple">{{ roleName(r.targetRoleCode) }}</span></td>
            <td style="text-align:center;">{{ r.priority }}</td>
            <td><span :class="['tag', r.enabled === 1 ? 'tag-green' : 'tag-orange']">{{ r.enabled === 1 ? '启用' : '停用' }}</span></td>
            <td style="font-size:12px;color:#6b7280;word-break:break-all;">{{ r.remark || '-' }}</td>
            <td>
              <button @click="openEdit(r)" style="padding:4px 10px;font-size:12px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;cursor:pointer;margin-right:6px;">编辑</button>
              <button @click="handleDelete(r)" style="padding:4px 10px;font-size:12px;border:1px solid #ff4d4f;border-radius:4px;background:#fff;color:#ff4d4f;cursor:pointer;">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!rules.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无派单规则</p>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-box" style="width:440px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ form.id ? '编辑规则' : '新增规则' }}</h3>
        <div class="form-group">
          <label class="form-label">事件类型 <span style="color:#ff4d4f;">*</span></label>
          <input v-model="form.eventType" class="form-input" placeholder="如：消防安全 / FIRE / 矛盾纠纷" />
        </div>
        <div class="form-group">
          <label class="form-label">目标角色 <span style="color:#ff4d4f;">*</span></label>
          <select v-model="form.targetRoleCode" class="form-select">
            <option v-for="r in roleOptions" :key="r.roleCode" :value="r.roleCode">{{ r.roleName }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">优先级</label>
          <input v-model.number="form.priority" type="number" class="form-input" placeholder="数值小者优先（默认 0）" />
        </div>
        <div class="form-group">
          <label class="form-label">状态</label>
          <select v-model.number="form.enabled" class="form-select">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">备注</label>
          <input v-model="form.remark" class="form-input" placeholder="规则说明..." />
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showForm = false" class="btn btn-default">取消</button>
          <button @click="handleSave" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDispatchRules, createDispatchRule, updateDispatchRule, deleteDispatchRule, getSystemRoles } from '../api'
import { showMessage } from '../utils/message'
import { confirmDialog } from '../utils/dialog'
import { getEventTypeName } from '../utils/eventTypes'

const rules = ref<any[]>([])

// 目标角色：动态读取系统角色列表展示中文名称（居民角色不受理事件，不作为派单目标）
const roleOptions = ref<{ roleCode: string; roleName: string }[]>([])
const FALLBACK_ROLE_NAMES: Record<string, string> = {
  SUPER_ADMIN: '超级管理员', EVENT_OPERATOR: '管理员', GRID_WORKER: '网格员', PUBLIC: '居民',
}
function roleName(code: string): string {
  return roleOptions.value.find(r => r.roleCode === code)?.roleName || FALLBACK_ROLE_NAMES[code] || code
}
async function loadRoles() {
  try {
    const data: any = await getSystemRoles()
    const list = Array.isArray(data) ? data : []
    roleOptions.value = list
      .filter((r: any) => r.status === 'ACTIVE' && r.roleCode !== 'PUBLIC')
      .map((r: any) => ({ roleCode: r.roleCode, roleName: r.roleName }))
  } catch (e: any) {
    showMessage(e?.message || '角色列表加载失败')
  }
}
const showForm = ref(false)
const form = ref<any>({ id: null, eventType: '', targetRoleCode: 'GRID_WORKER', priority: 0, enabled: 1, remark: '' })

async function loadRules() {
  try {
    rules.value = await getDispatchRules() || []
  } catch (e: any) {
    showMessage(e?.message || '加载规则失败')
  }
}

function openCreate() {
  form.value = { id: null, eventType: '', targetRoleCode: 'GRID_WORKER', priority: 0, enabled: 1, remark: '' }
  showForm.value = true
}

function openEdit(r: any) {
  form.value = { id: r.id, eventType: r.eventType, targetRoleCode: r.targetRoleCode, priority: r.priority, enabled: r.enabled, remark: r.remark || '' }
  showForm.value = true
}

async function handleSave() {
  if (!form.value.eventType?.trim()) { showMessage('请填写事件类型'); return }
  if (!form.value.targetRoleCode) { showMessage('请选择目标角色'); return }
  try {
    const payload = {
      eventType: form.value.eventType.trim(),
      targetRoleCode: form.value.targetRoleCode,
      priority: form.value.priority ?? 0,
      enabled: form.value.enabled ?? 1,
      remark: form.value.remark || ''
    }
    if (form.value.id) {
      await updateDispatchRule(form.value.id, payload)
    } else {
      await createDispatchRule(payload)
    }
    showForm.value = false
    await loadRules()
  } catch (e: any) {
    showMessage(e?.message || '保存失败')
  }
}

async function handleDelete(r: any) {
  if (!await confirmDialog({ message: `确定删除规则「${r.eventType} → ${r.targetRoleCode}」？`, danger: true, okText: '删除' })) return
  try {
    await deleteDispatchRule(r.id)
    await loadRules()
  } catch (e: any) {
    showMessage(e?.message || '删除失败')
  }
}

onMounted(() => {
  loadRules()
  loadRoles()
})
</script>
