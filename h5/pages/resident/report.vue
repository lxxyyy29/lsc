<template>
  <view class="container">
    <view class="hero-card">
      <view class="hero-title">居民上报</view>
      <view class="hero-subtitle">一键反映问题、查询进度</view>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="label">问题类型</text>
        <picker :range="typeNames" @change="onTypeChange">
          <view class="picker-text">{{ selectedTypeName || '请选择类型' }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">标题</text>
        <input v-model="title" class="input" placeholder="简短描述问题" />
      </view>

      <view class="form-item">
        <text class="label">详细描述</text>
        <textarea v-model="content" class="textarea" placeholder="详细描述您的问题..." />
      </view>

      <view class="form-item">
        <text class="label">现场照片</text>
        <view class="photo-grid">
          <view v-for="(photo, idx) in photos" :key="idx" class="photo-item">
            <image :src="photo" mode="aspectFill" />
            <text class="photo-del" @click="removePhoto(idx)">×</text>
          </view>
          <view class="photo-add" @click="takePhoto" v-if="photos.length < 3">
            <text class="photo-add-icon">+</text>
            <text class="photo-add-text">拍照</text>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="label">您的姓名</text>
        <input v-model="residentName" class="input" placeholder="选填" />
      </view>

      <view class="form-item">
        <text class="label">联系电话</text>
        <input v-model="residentPhone" class="input" placeholder="选填，便于回访" />
      </view>
    </view>

    <view class="btn-submit" @click="handleSubmit">提交上报</view>

    <!-- 查询码弹窗 -->
    <view v-if="showCodeDialog" class="mask" @click="showCodeDialog = false">
      <view class="dialog" @click.stop>
        <view class="dialog-title">上报成功！</view>
        <view class="dialog-text">您的查询码为：</view>
        <view class="dialog-code">{{ queryCode }}</view>
        <view class="dialog-text">请保存此码，用于查询处理进度</view>
        <view class="dialog-btn" @click="showCodeDialog = false">确定</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { createResidentReport, ResidentReport } from '../../src/api/community'

const types = [
  { label: '投诉', value: 'COMPLAINT' },
  { label: '报修', value: 'REPAIR' },
  { label: '活动报名', value: 'ACTIVITY' },
  { label: '政策咨询', value: 'POLICY' },
  { label: '隐患上报', value: 'HAZARD' }
]

const typeNames = types.map(t => t.label)
const selectedType = ref('')
const selectedTypeName = ref('')
const title = ref('')
const content = ref('')
const residentName = ref('')
const residentPhone = ref('')
const photos = ref<string[]>([])
const showCodeDialog = ref(false)
const queryCode = ref('')

function onTypeChange(e: any) {
  const idx = e.detail.value
  selectedType.value = types[idx].value
  selectedTypeName.value = types[idx].label
}

function takePhoto() {
  uni.chooseImage({
    count: 3 - photos.value.length,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res) => { res.tempFilePaths.forEach(p => photos.value.push(p)) },
    fail: () => { uni.showToast({ title: '请选择图片', icon: 'none' }) }
  })
}

function removePhoto(idx: number) { photos.value.splice(idx, 1) }

async function handleSubmit() {
  if (!selectedType.value) { uni.showToast({ title: '请选择问题类型', icon: 'none' }); return }
  if (!title.value) { uni.showToast({ title: '请输入标题', icon: 'none' }); return }
  if (!content.value) { uni.showToast({ title: '请填写详细描述', icon: 'none' }); return }

  const report: ResidentReport = {
    reportType: selectedType.value,
    title: title.value,
    content: content.value,
    residentName: residentName.value || undefined,
    residentPhone: residentPhone.value || undefined,
    photoUrls: photos.value
  }

  try {
    await createResidentReport(report)
    queryCode.value = '已生成'
    showCodeDialog.value = true
  } catch (e) {
    uni.showToast({ title: '提交失败', icon: 'none' })
  }
}
</script>

<style scoped>
.container { padding: 20px; background: #030913; min-height: 100vh; }
.hero-card { background: linear-gradient(135deg, #0a2a4a, #0d3866); border-radius: 16px; padding: 20px; margin-bottom: 16px; }
.hero-title { font-size: 22px; font-weight: bold; color: #eaf5ff; }
.hero-subtitle { font-size: 13px; color: #7ea4c8; margin-top: 4px; }
.form-card { background: #0e233a; border-radius: 16px; padding: 16px; margin-bottom: 16px; }
.form-item { margin-bottom: 16px; }
.label { display: block; font-size: 13px; color: #cfe5fb; margin-bottom: 8px; }
.picker-text, .input { background: #0a1d33; border-radius: 8px; padding: 10px; color: #eaf5ff; font-size: 14px; }
.textarea { width: 100%; background: #0a1d33; border-radius: 8px; padding: 10px; color: #eaf5ff; min-height: 80px; }
.photo-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.photo-item { width: 80px; height: 80px; position: relative; border-radius: 8px; overflow: hidden; }
.photo-item image { width: 100%; height: 100%; }
.photo-del { position: absolute; top: 0; right: 0; background: rgba(255,0,0,0.7); color: white; width: 20px; height: 20px; border-radius: 50%; text-align: center; line-height: 20px; font-size: 14px; }
.photo-add { width: 80px; height: 80px; border: 1px dashed #57b9ff; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.photo-add-icon { font-size: 24px; color: #57b9ff; }
.photo-add-text { font-size: 11px; color: #7ea4c8; }
.btn-submit { background: linear-gradient(135deg, #57b9ff, #1e88e5); color: white; text-align: center; padding: 14px; border-radius: 12px; font-weight: bold; font-size: 16px; }
.mask { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 999; }
.dialog { background: #0e233a; border-radius: 16px; padding: 24px; width: 80%; text-align: center; }
.dialog-title { font-size: 18px; font-weight: bold; color: #eaf5ff; margin-bottom: 12px; }
.dialog-text { font-size: 13px; color: #7ea4c8; margin-bottom: 8px; }
.dialog-code { font-size: 28px; font-weight: bold; color: #57b9ff; margin: 16px 0; letter-spacing: 4px; }
.dialog-btn { background: #57b9ff; color: white; padding: 10px; border-radius: 8px; margin-top: 16px; }
</style>
