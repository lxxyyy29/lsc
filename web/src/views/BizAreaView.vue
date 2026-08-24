<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">辖区管理</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">社区辖区划分、商户/摊贩/违禁区域管理</p>

    <!-- Tab 切换 + 新增按钮 -->
    <div style="display:flex;gap:8px;margin-bottom:16px;align-items:center;">
      <button v-for="t in tabs" :key="t.key"
        :style="activeTab === t.key ? 'padding:6px 16px;border:none;border-radius:6px;background:#0284c7;color:#fff;font-size:13px;cursor:pointer;' : 'padding:6px 16px;border:1px solid #e5e7eb;border-radius:6px;background:#fff;color:#374151;font-size:13px;cursor:pointer;'"
        @click="activeTab = t.key; fetchData()">
        {{ t.label }}
      </button>
      <button @click="openCreate()" style="margin-left:auto;padding:6px 16px;border:none;border-radius:6px;background:#52c41a;color:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-plus"></i> 新增{{ currentTabLabel }}
      </button>
    </div>

    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="fetchData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <template v-else>
        <!-- 辖区 -->
        <table v-if="activeTab === 'areas'" class="table">
          <thead><tr><th>辖区名称</th><th>负责人</th><th>电话</th><th>状态</th><th style="width:130px;">操作</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.areaName || item.name || '-' }}</td>
              <td>{{ item.principalName || '-' }}</td>
              <td>{{ item.principalPhone || '-' }}</td>
              <td><span :class="['tag', item.status === 'ACTIVE' ? 'tag-green' : 'tag-orange']">{{ item.status === 'ACTIVE' ? '启用中' : '已停用' }}</span></td>
              <td>
                <button @click="openEdit(item)" style="padding:3px 10px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;margin-right:4px;">编辑</button>
                <button @click="handleDelete(item)" style="padding:3px 10px;border:1px solid #ffccc7;border-radius:4px;background:#fff;color:#ff4d4f;font-size:12px;cursor:pointer;">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <!-- 商户 -->
        <table v-else-if="activeTab === 'merchants'" class="table">
          <thead><tr><th>商户名称</th><th>负责人</th><th>电话</th><th>状态</th><th style="width:130px;">操作</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.merchantName || item.name || '-' }}</td>
              <td>{{ item.legalPersonName || '-' }}</td>
              <td>{{ item.legalPersonPhone || '-' }}</td>
              <td><span :class="['tag', item.status === 'ACTIVE' ? 'tag-green' : 'tag-orange']">{{ item.status === 'ACTIVE' ? '启用中' : '已停用' }}</span></td>
              <td>
                <button @click="openEdit(item)" style="padding:3px 10px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;margin-right:4px;">编辑</button>
                <button @click="handleDelete(item)" style="padding:3px 10px;border:1px solid #ffccc7;border-radius:4px;background:#fff;color:#ff4d4f;font-size:12px;cursor:pointer;">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <!-- 摊贩 -->
        <table v-else-if="activeTab === 'vendors'" class="table">
          <thead><tr><th>摊贩名称</th><th>负责人</th><th>电话</th><th>状态</th><th style="width:130px;">操作</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.vendorName || item.name || '-' }}</td>
              <td>{{ item.legalPersonName || '-' }}</td>
              <td>{{ item.legalPersonPhone || '-' }}</td>
              <td><span :class="['tag', item.status === 'ACTIVE' ? 'tag-green' : 'tag-orange']">{{ item.status === 'ACTIVE' ? '启用中' : '已停用' }}</span></td>
              <td>
                <button @click="openEdit(item)" style="padding:3px 10px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;margin-right:4px;">编辑</button>
                <button @click="handleDelete(item)" style="padding:3px 10px;border:1px solid #ffccc7;border-radius:4px;background:#fff;color:#ff4d4f;font-size:12px;cursor:pointer;">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <!-- 违禁区域 -->
        <table v-else class="table">
          <thead><tr><th>区域名称</th><th>类型</th><th>状态</th><th style="width:130px;">操作</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.areaName || item.name || '-' }}</td>
              <td><span class="tag tag-red">{{ item.violationType || '-' }}</span></td>
              <td><span :class="['tag', item.status === 'ACTIVE' ? 'tag-green' : 'tag-orange']">{{ item.status === 'ACTIVE' ? '启用中' : '已停用' }}</span></td>
              <td>
                <button @click="openEdit(item)" style="padding:3px 10px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;margin-right:4px;">编辑</button>
                <button @click="handleDelete(item)" style="padding:3px 10px;border:1px solid #ffccc7;border-radius:4px;background:#fff;color:#ff4d4f;font-size:12px;cursor:pointer;">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!list.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </template>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="formVisible" class="mask" @click="formVisible = false">
      <div class="dialog" @click.stop>
        <h3 style="margin:0 0 16px;font-size:16px;">{{ form.id ? '编辑' : '新增' }}{{ currentTabLabel }}</h3>
        <div v-for="f in formFields" :key="f.key" style="margin-bottom:14px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">{{ f.label }}<span v-if="f.required" style="color:#ff4d4f;"> *</span></label>
          <select v-if="f.type === 'select'" v-model="form[f.key]" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;box-sizing:border-box;">
            <option v-for="o in f.options" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
          <input v-else v-model="form[f.key]" :placeholder="f.placeholder || '请输入' + f.label" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;box-sizing:border-box;" />
        </div>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:20px;">
          <button @click="formVisible = false" style="padding:8px 20px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
          <button @click="handleSave" :disabled="saving" style="padding:8px 20px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import http from '../api'

const tabs = [
  { key: 'areas', label: '辖区' },
  { key: 'merchants', label: '商户' },
  { key: 'vendors', label: '摊贩' },
  { key: 'violations', label: '违禁区域' }
]
const activeTab = ref('areas')
const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const formVisible = ref(false)
const saving = ref(false)
const form = ref<any>({})

const urls: Record<string, string> = {
  areas: '/areas',
  merchants: '/merchants',
  vendors: '/mobile-vendors',
  violations: '/violation-areas'
}

const currentTabLabel = computed(() => tabs.find(t => t.key === activeTab.value)?.label || '')

// 各 Tab 表单字段配置（key 与后端实体字段一致）
const fieldConfigs: Record<string, any[]> = {
  areas: [
    { key: 'areaName', label: '辖区名称', required: true, placeholder: '如：第一社区' },
    { key: 'principalName', label: '负责人', placeholder: '负责人姓名' },
    { key: 'principalPhone', label: '联系电话', placeholder: '负责人电话' },
    { key: 'status', label: '状态', type: 'select', options: [{ value: 'ACTIVE', label: '启用' }, { value: 'DISABLED', label: '停用' }] },
    { key: 'remark', label: '备注', placeholder: '备注说明' },
  ],
  merchants: [
    { key: 'merchantName', label: '商户名称', required: true, placeholder: '商户名称' },
    { key: 'legalPersonName', label: '负责人', placeholder: '负责人姓名' },
    { key: 'legalPersonPhone', label: '联系电话', placeholder: '负责人电话' },
    { key: 'status', label: '状态', type: 'select', options: [{ value: 'ACTIVE', label: '启用' }, { value: 'DISABLED', label: '停用' }] },
    { key: 'remark', label: '备注', placeholder: '备注说明' },
  ],
  vendors: [
    { key: 'vendorName', label: '摊贩名称', required: true, placeholder: '摊贩名称' },
    { key: 'legalPersonName', label: '负责人', placeholder: '负责人姓名' },
    { key: 'legalPersonPhone', label: '联系电话', placeholder: '负责人电话' },
    { key: 'status', label: '状态', type: 'select', options: [{ value: 'ACTIVE', label: '启用' }, { value: 'DISABLED', label: '停用' }] },
    { key: 'remark', label: '备注', placeholder: '备注说明' },
  ],
  violations: [
    { key: 'areaName', label: '区域名称', required: true, placeholder: '如：学校门口' },
    { key: 'violationType', label: '类型', placeholder: '如：占道经营' },
    { key: 'status', label: '状态', type: 'select', options: [{ value: 'ACTIVE', label: '启用' }, { value: 'DISABLED', label: '停用' }] },
    { key: 'remark', label: '备注', placeholder: '备注说明' },
  ],
}

const formFields = computed(() => fieldConfigs[activeTab.value] || [])

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await http.get(urls[activeTab.value]) || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  const f: any = { status: 'ACTIVE' }
  formFields.value.forEach((x: any) => { if (x.type !== 'select') f[x.key] = '' })
  form.value = f
  formVisible.value = true
}

function openEdit(item: any) {
  const f: any = { id: item.id, status: item.status || 'ACTIVE' }
  formFields.value.forEach((x: any) => { f[x.key] = item[x.key] ?? '' })
  form.value = f
  formVisible.value = true
}

async function handleSave() {
  const required = formFields.value.find((x: any) => x.required && !String(form.value[x.key] || '').trim())
  if (required) { alert(`请填写${required.label}`); return }
  saving.value = true
  try {
    const payload: any = { ...form.value }
    delete payload.id
    if (form.value.id) {
      await http.put(`${urls[activeTab.value]}/${form.value.id}`, payload)
    } else {
      await http.post(urls[activeTab.value], payload)
    }
    formVisible.value = false
    await fetchData()
  } catch (e: any) {
    alert(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(item: any) {
  if (!confirm(`确定删除该${currentTabLabel.value}吗？删除后不可恢复。`)) return
  try {
    await http.delete(`${urls[activeTab.value]}/${item.id}`)
    await fetchData()
  } catch (e: any) {
    alert(e?.message || '删除失败')
  }
}

onMounted(fetchData)
</script>

<style scoped>
.mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog {
  background: #fff; border-radius: 10px; padding: 24px; width: 420px; max-width: 92vw;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
</style>
