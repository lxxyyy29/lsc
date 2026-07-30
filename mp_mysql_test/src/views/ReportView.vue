<template>
  <div class="page">
    <div class="header">
      <h2>📸 随手拍</h2>
      <p>发现身边问题，一键上报</p>
    </div>

    <div class="card">
      <h3>问题类型</h3>
      <div class="type-grid">
        <div v-for="t in types" :key="t.value" class="type-item"
             :class="{ active: form.type === t.value }" @click="form.type = t.value">
          <span class="type-icon">{{ t.icon }}</span>
          <span>{{ t.label }}</span>
        </div>
      </div>
    </div>

    <div class="card">
      <h3>问题描述</h3>
      <p class="selected-type">已选择：{{ selectedType.label }}</p>
      <textarea v-model="form.description" placeholder="请详细描述您发现的问题..." class="textarea" rows="3" />
    </div>

    <div class="card">
      <h3>现场照片</h3>
      <div class="photo-list">
        <div v-for="(p, idx) in photos" :key="idx" class="photo-item">
          <div class="photo-placeholder">📷 {{ idx + 1 }}</div>
          <span class="photo-del" @click="photos.splice(idx, 1)">×</span>
        </div>
        <div v-if="photos.length < 3" class="photo-add" @click="addPhoto">
          <span>+</span>
          <p>添加照片</p>
        </div>
      </div>
    </div>

    <div class="card">
      <h3>位置信息</h3>
      <p class="location">📍 拔蛟窝社区（自动定位）</p>
    </div>

    <div class="card">
      <h3>联系方式（选填）</h3>
      <input v-model="form.contactName" placeholder="您的姓名" class="input" />
      <input v-model="form.contactPhone" placeholder="联系电话" class="input" style="margin-top:8px;" />
    </div>

    <button @click="handleSubmit" :disabled="loading" class="btn-submit">
      {{ loading ? '提交中...' : '提交上报' }}
    </button>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive } from 'vue'
import { reportEvent } from '../api'

const loading = ref(false)
const error = ref('')
const success = ref('')
const photos = ref<any[]>([])

const types = [
  { value: 'ROAD', label: '道路损坏', icon: '🛣️' },
  { value: 'LIGHT', label: '路灯故障', icon: '💡' },
  { value: 'PIPE', label: '管道破损', icon: '🔧' },
  { value: 'ENV', label: '环境卫生', icon: '🗑️' },
  { value: 'SAFE', label: '安全隐患', icon: '⚠️' },
  { value: 'NOISE', label: '噪音扰民', icon: '🔊' },
  { value: 'OTHER', label: '其他问题', icon: '📝' },
]

const form = reactive({
  type: 'ROAD',
  description: '',
  contactName: '',
  contactPhone: ''
})

const selectedType = computed(() => types.find(t => t.value === form.type) || types[0])

function addPhoto() {
  if (photos.value.length < 3) {
    photos.value.push({})
  }
}

async function handleSubmit() {
  if (!form.description.trim()) { error.value = '请填写问题描述'; return }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const result: any = await reportEvent({
      title: selectedType.value.label,
      description: form.description,
      type: form.type,
      contactName: form.contactName,
      contactPhone: form.contactPhone,
      photos: photos.value
    })
    success.value = '上报成功！查询码：' + (result?.eventCode || result?.id || '')
    // 重置表单
    form.description = ''
    form.contactName = ''
    form.contactPhone = ''
    photos.value = []
  } catch (e: any) {
    error.value = e || '上报失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border-radius: 12px;
  padding: 20px;
  color: #fff;
  margin-bottom: 16px;
}
.header h2 { font-size: 20px; margin-bottom: 4px; }
.header p { font-size: 13px; opacity: 0.8; }
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card h3 { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #374151; }
.selected-type { margin-bottom: 8px; font-size: 13px; color: #1890ff; }
.type-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.type-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 10px 4px; border: 1px solid #e5e7eb; border-radius: 8px; cursor: pointer; font-size: 11px;
}
.type-item.active { border-color: #1890ff; background: #e6f4ff; }
.type-icon { font-size: 20px; }
.input, .textarea {
  width: 100%; padding: 10px 12px; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 14px; outline: none;
}
.input:focus, .textarea:focus { border-color: #1890ff; }
.textarea { resize: none; }
.photo-list { display: flex; gap: 8px; flex-wrap: wrap; }
.photo-item { position: relative; width: 80px; height: 80px; }
.photo-placeholder {
  width: 80px; height: 80px; background: #f3f4f6; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; font-size: 24px;
}
.photo-del {
  position: absolute; top: -6px; right: -6px; width: 20px; height: 20px;
  background: #ff4d4f; color: #fff; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; font-size: 12px; cursor: pointer;
}
.photo-add {
  width: 80px; height: 80px; border: 1px dashed #d1d5db; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; color: #9ca3af; font-size: 12px;
}
.photo-add span { font-size: 24px; }
.location { font-size: 13px; color: #6b7280; padding: 8px; background: #f9fafb; border-radius: 6px; }
.btn-submit {
  width: 100%; padding: 14px; background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  color: #fff; border: none; border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; margin-top: 8px;
}
.btn-submit[disabled] { opacity: 0.6; }
.error { color: #ff4d4f; font-size: 13px; text-align: center; margin-top: 12px; }
.success { color: #52c41a; font-size: 13px; text-align: center; margin-top: 12px; }
</style>
