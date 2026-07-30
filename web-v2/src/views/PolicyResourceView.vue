<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:20px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">政策资源库</h2>
        <p style="font-size:13px;color:#6b7280;">低保、养老、救助、医保、惠民政策，支撑政策找人</p>
      </div>
      <button @click="openCreate" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-plus"></i> 新增政策
      </button>
    </div>

    <!-- 类型筛选 -->
    <div style="display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap;">
      <button v-for="t in typeOptions" :key="t.value" @click="filterType = t.value"
              :style="['padding:6px 14px;border-radius:6px;font-size:13px;cursor:pointer;border:1px solid #d1d5db;',
                       filterType === t.value ? 'background:#1890ff;color:#fff;border-color:#1890ff;' : 'background:#fff;color:#374151;']">
        {{ t.label }}
      </button>
    </div>

    <!-- 政策列表 -->
    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:8px;">加载中...</p>
      </div>
      <div v-else-if="!filteredList.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无政策数据</div>
      <table v-else class="table">
        <thead>
          <tr>
            <th>政策编码</th>
            <th>标题</th>
            <th>类型</th>
            <th>状态</th>
            <th>发布日期</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredList" :key="item.id">
            <td style="font-size:12px;">{{ item.policyCode }}</td>
            <td>{{ item.title }}</td>
            <td>
              <span :class="['tag', typeTagClass(item.policyType)]">{{ typeLabel(item.policyType) }}</span>
            </td>
            <td>
              <span :class="['tag', item.status === 'ACTIVE' ? 'tag-green' : 'tag-gray']">
                {{ item.status === 'ACTIVE' ? '启用' : '停用' }}
              </span>
            </td>
            <td style="font-size:12px;">{{ item.publishDate || '-' }}</td>
            <td>
              <button @click="openMatchPeople(item)" style="padding:4px 10px;border:1px solid #52c41a;border-radius:4px;background:#fff;color:#52c41a;font-size:12px;cursor:pointer;margin-right:4px;">政策找人</button>
              <button @click="openEdit(item)" style="padding:4px 10px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;margin-right:4px;">编辑</button>
              <button @click="toggleStatus(item)" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">
                {{ item.status === 'ACTIVE' ? '停用' : '启用' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 创建/编辑弹窗 -->
    <div v-if="showDialog" class="modal-overlay" @click.self="showDialog = false">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ editing ? '编辑政策' : '新增政策' }}</h3>
        <div class="form-group">
          <label class="form-label">标题 <span class="required">*</span></label>
          <input v-model="form.title" class="form-input" placeholder="请输入政策标题" />
        </div>
        <div class="form-group">
          <label class="form-label">政策类型 <span class="required">*</span></label>
          <select v-model="form.policyType" class="form-select">
            <option v-for="t in typeOptions.filter(t => t.value)" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">政策说明</label>
          <textarea v-model="form.description" class="form-textarea" rows="3" placeholder="请输入政策说明..."></textarea>
        </div>
        <div class="form-group">
          <label class="form-label">资格条件</label>
          <textarea v-model="form.eligibility" class="form-textarea" rows="2" placeholder="请输入资格条件..."></textarea>
        </div>
        <div class="form-group">
          <label class="form-label">匹配标签（逗号分隔）</label>
          <input v-model="form.tags" class="form-input" placeholder="如：低保,老年人,残疾人" />
        </div>
        <div class="form-group">
          <label class="form-label">发布日期</label>
          <input v-model="form.publishDate" type="date" class="form-input" />
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:16px;">
          <button @click="showDialog = false" class="btn btn-default">取消</button>
          <button @click="submitForm" class="btn btn-primary">{{ editing ? '保存' : '创建' }}</button>
        </div>
      </div>
    </div>

    <!-- 政策找人结果弹窗 -->
    <div v-if="showMatchDialog" class="modal-overlay" @click.self="showMatchDialog = false">
      <div class="modal-box" style="width:640px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:4px;">政策找人</h3>
        <p style="font-size:13px;color:#6b7280;margin-bottom:16px;">
          政策"{{ matchingPolicy?.title }}"匹配到 {{ matchingPeople.length }} 人
        </p>
        <div v-if="matchingPeople.length" style="max-height:400px;overflow-y:auto;">
          <table class="table">
            <thead>
              <tr>
                <th>姓名</th>
                <th>电话</th>
                <th>户籍类型</th>
                <th>所属网格</th>
                <th>地址</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in matchingPeople" :key="p.id">
                <td>{{ p.name }}</td>
                <td style="font-size:12px;">{{ p.phone || '-' }}</td>
                <td>{{ p.household_type || '-' }}</td>
                <td style="font-size:12px;">{{ p.grid_name || '-' }}</td>
                <td style="font-size:12px;">{{ p.address || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else style="text-align:center;padding:40px;color:#9ca3af;">未匹配到符合条件的人群</p>
        <div style="display:flex;justify-content:flex-end;margin-top:16px;">
          <button @click="showMatchDialog = false" class="btn btn-default">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import http from '../api'

const POLICY_TYPES = [
  { value: '', label: '全部' },
  { value: 'LOW_INCOME', label: '低保' },
  { value: 'ELDERLY', label: '养老' },
  { value: 'RESCUE', label: '救助' },
  { value: 'MEDICAL', label: '医保' },
  { value: 'BENEFIT', label: '惠民' },
  { value: 'OTHER', label: '其他' }
]

const typeOptions = POLICY_TYPES
const list = ref<any[]>([])
const loading = ref(false)
const filterType = ref('')
const showDialog = ref(false)
const showMatchDialog = ref(false)
const editing = ref<any>(null)
const matchingPolicy = ref<any>(null)
const matchingPeople = ref<any[]>([])

const form = ref({
  title: '',
  policyType: 'BENEFIT',
  description: '',
  eligibility: '',
  tags: '',
  publishDate: ''
})

const filteredList = computed(() => {
  if (!filterType.value) return list.value
  return list.value.filter((i: any) => i.policyType === filterType.value)
})

function typeLabel(value: string) {
  return POLICY_TYPES.find((t) => t.value === value)?.label || value
}

function typeTagClass(value: string) {
  const map: any = { LOW_INCOME: 'tag-blue', ELDERLY: 'tag-green', RESCUE: 'tag-red', MEDICAL: 'tag-orange', BENEFIT: 'tag-purple' }
  return map[value] || 'tag-gray'
}

async function loadList() {
  loading.value = true
  try {
    const res: any = await http.get('/community/policy-resources')
    list.value = res || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = { title: '', policyType: 'BENEFIT', description: '', eligibility: '', tags: '', publishDate: '' }
  showDialog.value = true
}

function openEdit(item: any) {
  editing.value = item
  form.value = {
    title: item.title,
    policyType: item.policyType,
    description: item.description || '',
    eligibility: item.eligibility || '',
    tags: item.tags || '',
    publishDate: item.publishDate || ''
  }
  showDialog.value = true
}

async function submitForm() {
  if (!form.value.title.trim()) { alert('请填写标题'); return }
  try {
    if (editing.value) {
      await http.put(`/community/policy-resources/${editing.value.id}`, form.value)
    } else {
      await http.post('/community/policy-resources', form.value)
    }
    showDialog.value = false
    loadList()
  } catch (e: any) {
    alert(e?.message || '操作失败')
  }
}

async function toggleStatus(item: any) {
  try {
    await http.put(`/community/policy-resources/${item.id}`, { ...item, status: item.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE' })
    loadList()
  } catch (e: any) {
    alert(e?.message || '操作失败')
  }
}

async function openMatchPeople(item: any) {
  matchingPolicy.value = item
  try {
    const res: any = await http.get(`/community/policy-resources/${item.id}/matching-people`)
    matchingPeople.value = res || []
  } catch {
    matchingPeople.value = []
  }
  showMatchDialog.value = true
}

onMounted(loadList)
</script>
