<template>
  <div>
    <div class="page-header" style="display:flex;align-items:center;justify-content:space-between;">
      <div>
        <h1 class="page-title">字典管理</h1>
        <p class="page-desc">维护系统下拉选项等字典数据，业务表单统一按字典编码读取</p>
      </div>
      <button @click="openTypeForm(null)" class="btn btn-primary">
        <i class="fas fa-plus"></i>新增字典
      </button>
    </div>

    <div style="display:grid;grid-template-columns:380px 1fr;gap:16px;align-items:start;">
      <!-- 左侧：字典类型列表 -->
      <div class="card">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">字典类型</h3>
        <table class="table">
          <thead>
            <tr><th>编码 / 名称</th><th style="width:70px;">状态</th><th style="width:110px;">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="t in types" :key="t.id"
                :style="{ cursor: 'pointer', background: selectedCode === t.dictCode ? '#e0f2fe' : '' }"
                @click="selectType(t)">
              <td>
                <div style="font-weight:600;">{{ t.dictName }}</div>
                <div style="font-size:12px;color:#6b7280;">{{ t.dictCode }}（{{ t.itemCount }} 项）</div>
              </td>
              <td>
                <span :class="['tag', t.status === 'ACTIVE' ? 'tag-green' : 'tag-red']">
                  {{ t.status === 'ACTIVE' ? '启用' : '停用' }}
                </span>
              </td>
              <td @click.stop>
                <button class="btn btn-default btn-sm" @click="openTypeForm(t)">编辑</button>
                <button class="btn btn-default btn-sm" style="color:#dc2626;margin-left:4px;" @click="removeType(t)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!types.length" class="empty-state"><p>暂无字典</p></div>
      </div>

      <!-- 右侧：选中字典的字典项 -->
      <div class="card">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
          <h3 style="font-size:14px;font-weight:600;">
            字典项
            <span v-if="selectedType" style="color:#6b7280;font-weight:400;">- {{ selectedType.dictName }}（{{ selectedType.dictCode }}）</span>
          </h3>
          <button v-if="selectedType" @click="openItemForm(null)" class="btn btn-primary btn-sm">
            <i class="fas fa-plus"></i>新增字典项
          </button>
        </div>
        <template v-if="selectedType">
          <table class="table">
            <thead>
              <tr>
                <th style="width:60px;">排序</th>
                <th>值</th>
                <th>显示名</th>
                <th style="width:70px;">状态</th>
                <th>备注</th>
                <th style="width:110px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in items" :key="i.id">
                <td>{{ i.sortOrder }}</td>
                <td style="font-family:monospace;">{{ i.itemValue }}</td>
                <td>{{ i.itemLabel }}</td>
                <td>
                  <span :class="['tag', i.status === 'ACTIVE' ? 'tag-green' : 'tag-red']">
                    {{ i.status === 'ACTIVE' ? '启用' : '停用' }}
                  </span>
                </td>
                <td style="font-size:12px;color:#6b7280;">{{ i.remark || '-' }}</td>
                <td>
                  <button class="btn btn-default btn-sm" @click="openItemForm(i)">编辑</button>
                  <button class="btn btn-default btn-sm" style="color:#dc2626;margin-left:4px;" @click="removeItem(i)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!items.length" class="empty-state"><p>该字典下暂无字典项</p></div>
        </template>
        <div v-else class="empty-state"><p>请在左侧选择字典</p></div>
      </div>
    </div>

    <!-- 字典类型 新增/编辑 弹窗 -->
    <div v-if="showTypeModal" class="modal-overlay" @click.self="showTypeModal = false">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ editingType ? '编辑字典' : '新增字典' }}</h3>
        <div class="form-group">
          <label class="form-label">字典编码 <span class="required">*</span></label>
          <input v-model="typeForm.dictCode" class="form-input" :disabled="!!editingType" placeholder="如 event_report_source" />
          <p v-if="editingType" style="font-size:12px;color:#9ca3af;margin-top:4px;">编码已被业务引用，不可修改</p>
        </div>
        <div class="form-group">
          <label class="form-label">字典名称 <span class="required">*</span></label>
          <input v-model="typeForm.dictName" class="form-input" placeholder="如 事件上报来源" />
        </div>
        <div class="form-group">
          <label class="form-label">状态</label>
          <select v-model="typeForm.status" class="form-select">
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">停用</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">备注</label>
          <input v-model="typeForm.remark" class="form-input" placeholder="选填" />
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:16px;">
          <button @click="showTypeModal = false" class="btn btn-default">取消</button>
          <button @click="saveType" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>

    <!-- 字典项 新增/编辑 弹窗 -->
    <div v-if="showItemModal" class="modal-overlay" @click.self="showItemModal = false">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ editingItem ? '编辑字典项' : '新增字典项' }}</h3>
        <div class="form-group">
          <label class="form-label">值 <span class="required">*</span></label>
          <input v-model="itemForm.itemValue" class="form-input" placeholder="存入业务字段的值，如 GRID_MEMBER" />
        </div>
        <div class="form-group">
          <label class="form-label">显示名 <span class="required">*</span></label>
          <input v-model="itemForm.itemLabel" class="form-input" placeholder="下拉框展示的文字，如 网格员上报" />
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <div class="form-group">
            <label class="form-label">排序</label>
            <input v-model.number="itemForm.sortOrder" type="number" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">状态</label>
            <select v-model="itemForm.status" class="form-select">
              <option value="ACTIVE">启用</option>
              <option value="DISABLED">停用</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">备注</label>
          <input v-model="itemForm.remark" class="form-input" placeholder="选填" />
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:16px;">
          <button @click="showItemModal = false" class="btn btn-default">取消</button>
          <button @click="saveItem" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getDictTypes, getDictItems, createDictType, updateDictType, deleteDictType,
  createDictItem, updateDictItem, deleteDictItem, type DictType, type DictItem
} from '../api'
import { showMessage } from '../utils/message'

const types = ref<DictType[]>([])
const items = ref<DictItem[]>([])
const selectedCode = ref('')
const selectedType = computed(() => types.value.find(t => t.dictCode === selectedCode.value) || null)

// ---------- 字典类型 ----------
const showTypeModal = ref(false)
const editingType = ref<DictType | null>(null)
const typeForm = reactive({ dictCode: '', dictName: '', status: 'ACTIVE', remark: '' })

function openTypeForm(t: DictType | null) {
  editingType.value = t
  Object.assign(typeForm, t
    ? { dictCode: t.dictCode, dictName: t.dictName, status: t.status, remark: t.remark || '' }
    : { dictCode: '', dictName: '', status: 'ACTIVE', remark: '' })
  showTypeModal.value = true
}

async function saveType() {
  if (!typeForm.dictName.trim()) { showMessage('请填写字典名称', 'warning'); return }
  try {
    if (editingType.value) {
      await updateDictType(editingType.value.id, { dictName: typeForm.dictName, status: typeForm.status, remark: typeForm.remark })
      showMessage('保存成功', 'success')
    } else {
      if (!typeForm.dictCode.trim()) { showMessage('请填写字典编码', 'warning'); return }
      if (!/^[a-zA-Z][a-zA-Z0-9_-]*$/.test(typeForm.dictCode.trim())) { showMessage('字典编码仅支持字母开头，由字母/数字/下划线/中划线组成', 'warning'); return }
      await createDictType({ dictCode: typeForm.dictCode.trim(), dictName: typeForm.dictName, status: typeForm.status, remark: typeForm.remark })
      showMessage('新增成功', 'success')
    }
    showTypeModal.value = false
    await loadTypes()
  } catch (e: any) {
    showMessage(e?.message || '保存失败', 'error')
  }
}

async function removeType(t: DictType) {
  if (!confirm(`确认删除字典「${t.dictName}」及其 ${t.itemCount} 个字典项吗？`)) return
  try {
    await deleteDictType(t.id)
    showMessage('删除成功', 'success')
    if (selectedCode.value === t.dictCode) { selectedCode.value = ''; items.value = [] }
    await loadTypes()
  } catch (e: any) {
    showMessage(e?.message || '删除失败', 'error')
  }
}

// ---------- 字典项 ----------
const showItemModal = ref(false)
const editingItem = ref<DictItem | null>(null)
const itemForm = reactive({ itemValue: '', itemLabel: '', sortOrder: 0, status: 'ACTIVE', remark: '' })

function openItemForm(i: DictItem | null) {
  editingItem.value = i
  Object.assign(itemForm, i
    ? { itemValue: i.itemValue, itemLabel: i.itemLabel, sortOrder: i.sortOrder, status: i.status, remark: i.remark || '' }
    : { itemValue: '', itemLabel: '', sortOrder: (items.value.length + 1) * 10, status: 'ACTIVE', remark: '' })
  showItemModal.value = true
}

async function saveItem() {
  if (!selectedType.value) return
  if (!itemForm.itemValue.trim() || !itemForm.itemLabel.trim()) { showMessage('请填写值和显示名', 'warning'); return }
  try {
    if (editingItem.value) {
      await updateDictItem(editingItem.value.id, { ...itemForm })
      showMessage('保存成功', 'success')
    } else {
      await createDictItem(selectedType.value.dictCode, { ...itemForm })
      showMessage('新增成功', 'success')
    }
    showItemModal.value = false
    await Promise.all([loadTypes(), loadItems()])
  } catch (e: any) {
    showMessage(e?.message || '保存失败', 'error')
  }
}

async function removeItem(i: DictItem) {
  if (!confirm(`确认删除字典项「${i.itemLabel}」吗？`)) return
  try {
    await deleteDictItem(i.id)
    showMessage('删除成功', 'success')
    await Promise.all([loadTypes(), loadItems()])
  } catch (e: any) {
    showMessage(e?.message || '删除失败', 'error')
  }
}

// ---------- 加载 ----------
async function loadTypes() {
  try {
    const res: any = await getDictTypes()
    types.value = Array.isArray(res) ? res : []
  } catch (e: any) {
    showMessage(e?.message || '加载字典失败', 'error')
  }
}

async function loadItems() {
  if (!selectedCode.value) return
  try {
    const res: any = await getDictItems(selectedCode.value)
    items.value = Array.isArray(res) ? res : []
  } catch (e: any) {
    showMessage(e?.message || '加载字典项失败', 'error')
  }
}

function selectType(t: DictType) {
  selectedCode.value = t.dictCode
  loadItems()
}

onMounted(loadTypes)
</script>
