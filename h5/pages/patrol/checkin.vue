<template>
  <view class="container">
    <view class="hero-card">
      <view class="hero-title">巡查打卡</view>
      <view class="hero-subtitle">记录巡查位置与现场情况</view>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="label">所属网格</text>
        <picker :range="gridNameList" range-key="label" @change="onGridChange">
          <view class="picker-text">{{ selectedGridName || '请选择网格' }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">当前位置</text>
        <view class="location-text">{{ locationText }}</view>
        <text class="link" @click="getLocation">重新定位</text>
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
        <text class="label">巡查内容</text>
        <textarea v-model="content" class="textarea" placeholder="描述现场情况..." />
      </view>
    </view>

    <view class="btn-submit" @click="handleSubmit">确认打卡</view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getGridTree, createPatrolRecord, GridTreeVo, PatrolRecord } from '../../src/api/community'

interface GridOption {
  label: string
  value: number
}

const gridTree = ref<GridTreeVo[]>([])
const gridOptions = ref<GridOption[]>([])
const selectedGridId = ref<number | null>(null)
const selectedGridName = ref('')
const locationText = ref('正在定位...')
const longitude = ref<number | null>(null)
const latitude = ref<number | null>(null)
const photos = ref<string[]>([])
const content = ref('')

const gridNameList = computed(() => gridOptions.value.map(g => ({
  label: g.label,
  value: g.value
})))

function flattenGrids(nodes: GridTreeVo[], prefix: string = '') {
  nodes.forEach(n => {
    const name = prefix ? `${prefix} > ${n.gridName}` : n.gridName
    gridOptions.value.push({ label: name, value: n.id })
    if (n.children) flattenGrids(n.children, name)
  })
}

function onGridChange(e: any) {
  const idx = Number(e.detail.value)
  const option = gridOptions.value[idx]
  if (option) {
    selectedGridId.value = option.value
    selectedGridName.value = option.label
  }
}

function getLocation() {
  locationText.value = '定位中...'
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      longitude.value = res.longitude
      latitude.value = res.latitude
      locationText.value = `经度: ${res.longitude.toFixed(6)}, 纬度: ${res.latitude.toFixed(6)}`
    },
    fail: () => {
      locationText.value = '定位失败，请检查GPS权限'
    }
  })
}

function takePhoto() {
  uni.chooseImage({
    count: 3 - photos.value.length,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res: any) => {
      res.tempFilePaths.forEach((p: string) => photos.value.push(p))
    },
    fail: () => {
      uni.showToast({ title: '请选择图片', icon: 'none' })
    }
  })
}

function removePhoto(idx: number) {
  photos.value.splice(idx, 1)
}

async function handleSubmit() {
  if (!selectedGridId.value) {
    uni.showToast({ title: '请选择网格', icon: 'none' })
    return
  }
  if (!content.value) {
    uni.showToast({ title: '请填写巡查内容', icon: 'none' })
    return
  }

  const record: PatrolRecord = {
    gridId: selectedGridId.value!,
    longitude: longitude.value || undefined,
    latitude: latitude.value || undefined,
    address: locationText.value,
    content: content.value,
    photoUrls: photos.value
  }

  try {
    await createPatrolRecord(record)
    uni.showToast({ title: '打卡成功！', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/workbench/index' }), 1500)
  } catch (e) {
    uni.showToast({ title: '打卡失败', icon: 'none' })
  }
}

onMounted(async () => {
  try {
    gridTree.value = await getGridTree()
    flattenGrids(gridTree.value)
  } catch (e) { console.error(e) }
  getLocation()
})
</script>

<style scoped>
.container { padding: 20px; background: #030913; min-height: 100vh; }
.hero-card { background: linear-gradient(135deg, #0a2a4a, #0d3866); border-radius: 16px; padding: 20px; margin-bottom: 16px; }
.hero-title { font-size: 22px; font-weight: bold; color: #eaf5ff; }
.hero-subtitle { font-size: 13px; color: #7ea4c8; margin-top: 4px; }
.form-card { background: #0e233a; border-radius: 16px; padding: 16px; margin-bottom: 16px; }
.form-item { margin-bottom: 16px; }
.label { display: block; font-size: 13px; color: #cfe5fb; margin-bottom: 8px; }
.picker-text { background: #0a1d33; border-radius: 8px; padding: 10px; color: #eaf5ff; font-size: 14px; }
.location-text { font-size: 12px; color: #7ea4c8; margin-bottom: 4px; }
.link { font-size: 12px; color: #57b9ff; }
.photo-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.photo-item { width: 80px; height: 80px; position: relative; border-radius: 8px; overflow: hidden; }
.photo-item image { width: 100%; height: 100%; }
.photo-del { position: absolute; top: 0; right: 0; background: rgba(255,0,0,0.7); color: white; width: 20px; height: 20px; border-radius: 50%; text-align: center; line-height: 20px; font-size: 14px; }
.photo-add { width: 80px; height: 80px; border: 1px dashed #57b9ff; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.photo-add-icon { font-size: 24px; color: #57b9ff; }
.photo-add-text { font-size: 11px; color: #7ea4c8; }
.textarea { width: 100%; background: #0a1d33; border-radius: 8px; padding: 10px; color: #eaf5ff; min-height: 80px; }
.btn-submit { background: linear-gradient(135deg, #57b9ff, #1e88e5); color: white; text-align: center; padding: 14px; border-radius: 12px; font-weight: bold; font-size: 16px; }
</style>
