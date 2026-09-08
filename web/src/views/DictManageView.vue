<template>
  <div class="dict-root">
    <div class="page-header dict-page-header-row">
      <div>
        <h1 class="page-title">字典管理</h1>
        <p class="page-desc">维护系统下拉选项等字典数据，业务表单统一按字典编码读取</p>
      </div>
      <button @click="openTypeForm(null)" class="btn btn-primary">
        <i class="fas fa-plus"></i>新增字典
      </button>
    </div>

    <div class="dict-grid">
      <!-- 左侧：字典类型列表 -->
      <div class="card dict-pane">
        <h3 class="dict-card-title">字典类型</h3>
        <table class="table">
          <thead>
            <tr><th>名称 / 编码</th><th style="width:70px;">状态</th><th style="width:150px;">操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="t in types" :key="t.id"
                :style="{ background: selectedCode === t.dictCode ? '#e0f2fe' : '' }"
                class="dict-row-clickable"
                @click="selectType(t)">
              <td>
                <div class="dict-name">
                  {{ t.dictName }}
                  <span class="dict-name-count">（{{ t.itemCount }} 项）</span>
                </div>
                <div class="dict-code">{{ t.dictCode }}</div>
              </td>
              <td>
                <span :class="['tag', t.status === 'ACTIVE' ? 'tag-green' : 'tag-red']">
                  {{ t.status === 'ACTIVE' ? '启用' : '停用' }}
                </span>
              </td>
              <td @click.stop>
                <button class="btn btn-default btn-sm" @click="openTypeForm(t)">编辑</button>
                <button class="btn btn-default btn-sm dict-btn-danger" @click="removeType(t)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!types.length" class="empty-state"><p>暂无字典</p></div>
      </div>

      <!-- 右侧：选中字典的字典项 -->
      <div class="card dict-pane">
        <div class="dict-card-header">
          <h3 class="dict-card-title" style="margin-bottom:0;">
            字典项
            <span v-if="selectedType" class="dict-card-title-sub">- {{ selectedType.dictName }}（{{ selectedType.dictCode }}）</span>
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
                <th>显示名</th>
                <th>值</th>
                <th style="width:70px;">状态</th>
                <th>备注</th>
                <th style="width:180px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in items" :key="i.id">
                <td>{{ i.sortOrder }}</td>
                <td>{{ i.itemLabel }}</td>
                <td class="dict-item-value">{{ i.itemValue }}</td>
                <td>
                  <span :class="['tag', i.status === 'ACTIVE' ? 'tag-green' : 'tag-red']">
                    {{ i.status === 'ACTIVE' ? '启用' : '停用' }}
                  </span>
                </td>
                <td class="dict-item-remark">{{ i.remark || '-' }}</td>
                <td>
                  <button class="btn btn-default btn-sm" @click="openItemForm(i)">编辑</button>
                  <button class="btn btn-default btn-sm dict-btn-danger" @click="removeItem(i)">删除</button>
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
    <div v-if="showTypeModal" class="modal-overlay">
      <div class="modal-box">
        <h3 class="dict-modal-title">{{ editingType ? '编辑字典' : '新增字典' }}</h3>
        <div v-if="editingType" class="form-group">
          <label class="form-label">字典编码</label>
          <div class="form-input dict-readonly">{{ typeForm.dictCode }}</div>
          <p class="dict-readonly-hint">编码已被业务引用，不可修改；新增时系统自动生成</p>
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
        <div class="dict-modal-actions">
          <button @click="showTypeModal = false" class="btn btn-default">取消</button>
          <button @click="saveType" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>

    <!-- 字典项 新增/编辑 弹窗 -->
    <div v-if="showItemModal" class="modal-overlay">
      <div class="modal-box">
        <h3 class="dict-modal-title">{{ editingItem ? '编辑字典项' : '新增字典项' }}</h3>
        <div v-if="editingItem" class="form-group">
          <label class="form-label">值</label>
          <div class="form-input dict-readonly">{{ itemForm.itemValue }}</div>
        </div>
        <div class="form-group">
          <label class="form-label">显示名 <span class="required">*</span></label>
          <input v-model="itemForm.itemLabel" class="form-input" placeholder="下拉框展示的文字，如 网格员上报" />
        </div>
        <div class="dict-form-row">
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
        <div class="dict-modal-actions">
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
import { alertDialog } from '../utils/dialog'

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
      // 新增字典：编码由系统自动生成（DICT_ + 时间戳），无需填写
      const payload: any = { dictName: typeForm.dictName, status: typeForm.status, remark: typeForm.remark }
      if (typeForm.dictCode.trim()) {
        if (!/^[a-zA-Z][a-zA-Z0-9_-]*$/.test(typeForm.dictCode.trim())) { showMessage('字典编码仅支持字母开头，由字母/数字/下划线/中划线组成', 'warning'); return }
        payload.dictCode = typeForm.dictCode.trim()
      }
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
  // TODO: 后端字典列表加引用来源字段后，按字段判断是否禁用；当前默认全部提示
  await alertDialog({
    title: '无法删除',
    message: `字典「${t.dictName}」（${t.dictCode}）已被业务模块关联引用，删除或编辑后将影响关联业务正常运行。\n如需调整，请联系系统管理员处理。`,
  })
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
  if (!itemForm.itemLabel.trim()) { showMessage('请填写显示名', 'warning'); return }
  try {
    if (editingItem.value) {
      await updateDictItem(editingItem.value.id, { ...itemForm })
      showMessage('保存成功', 'success')
    } else {
      // 新增字典项：值由系统自动生成（ITEM_ + 时间戳），仅传显示名等
      const payload: any = { ...itemForm }
      delete payload.itemValue
      await createDictItem(selectedType.value.dictCode, payload)
      showMessage('新增成功', 'success')
    }
    showItemModal.value = false
    await Promise.all([loadTypes(), loadItems()])
  } catch (e: any) {
    showMessage(e?.message || '保存失败', 'error')
  }
}

async function removeItem(i: DictItem) {
  // TODO: 后端字典列表加引用来源字段后，按字段判断是否禁用；当前默认全部提示
  await alertDialog({
    title: '无法删除',
    message: `字典项「${i.itemLabel}」（${i.itemValue}）已被业务模块关联引用，删除或编辑后将影响关联业务正常运行。\n如需调整，请联系系统管理员处理。`,
  })
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

<style scoped>
.dict-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.dict-page-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dict-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
  align-items: stretch;
  overflow: hidden;
}

.dict-pane {
  overflow: auto;
  min-height: 0;
  margin: 0;
}

.dict-card-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.dict-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.dict-card-title-sub {
  color: #6b7280;
  font-weight: 400;
}

.dict-row-clickable {
  cursor: pointer;
}

.dict-name {
  font-weight: 600;
}

.dict-name-count {
  font-weight: 400;
  color: #9ca3af;
  font-size: 12px;
  margin-left: 6px;
}

.dict-code {
  font-size: 12px;
  color: #6b7280;
}

.dict-item-value {
  font-family: monospace;
}

.dict-item-remark {
  font-size: 12px;
  color: #6b7280;
}

.dict-btn-danger {
  color: #dc2626;
  margin-left: 4px;
}

.dict-modal-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.dict-modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
}

.dict-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.dict-readonly {
  font-family: monospace;
  background: #f3f4f6;
  color: #374151;
  cursor: not-allowed;
}

.dict-readonly-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}
</style>