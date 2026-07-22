<template>
  <view class="page">
    <!-- View mode -->
    <view v-if="mode === 'view' && detail" class="content">
      <!-- Photo -->
      <view class="field-section">
        <view class="photo-preview">
          <image v-if="detail.merchantPhotoUrl" :src="toImageUrl(detail.merchantPhotoUrl)" class="preview-img" mode="aspectFill" />
          <view v-else class="preview-placeholder">
            <AppIcon name="briefcase" size="48rpx" class="preview-icon" />
            <text class="preview-hint">暂无照片</text>
          </view>
        </view>
      </view>

      <!-- Fields -->
      <view class="field-section">
        <view class="field-row">
          <text class="field-label">商户名称</text>
          <text class="field-value">{{ detail.merchantName }}</text>
        </view>
        <view v-if="detail.areaName" class="field-row">
          <text class="field-label">所属片区</text>
          <text class="field-value">{{ detail.areaName }}</text>
        </view>
        <view v-if="detail.longitude != null && detail.latitude != null" class="field-row">
          <text class="field-label">经纬度</text>
          <text class="field-value">{{ detail.longitude }}, {{ detail.latitude }}</text>
        </view>
        <view v-if="detail.longitude != null && detail.latitude != null" class="field-map">
          <!-- #ifdef MP-WEIXIN -->
          <map
            class="detail-map"
            :latitude="detail.latitude"
            :longitude="detail.longitude"
            :markers="[{ id: 1, latitude: detail.latitude, longitude: detail.longitude, width: 28, height: 36 }]"
            :scale="16"
          />
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <div class="detail-map" :id="viewMapElId"></div>
          <!-- #endif -->
        </view>
        <view v-if="detail.legalPersonName" class="field-row">
          <text class="field-label">法人姓名</text>
          <text class="field-value">{{ detail.legalPersonName }}</text>
        </view>
        <view v-if="detail.legalPersonPhone" class="field-row">
          <text class="field-label">法人电话</text>
          <text class="field-value field-value--link">{{ detail.legalPersonPhone }}</text>
        </view>
        <view v-if="detail.legalPersonPhotoUrl" class="field-row field-row--column">
          <text class="field-label">法人照片</text>
          <view class="photo-preview" @click="previewLegalPhoto">
            <image class="preview-img" :src="toImageUrl(detail.legalPersonPhotoUrl)" mode="aspectFill" />
          </view>
        </view>
        <view class="field-row">
          <text class="field-label">状态</text>
          <text class="status-badge" :class="detail.status === 'ACTIVE' ? 'badge--active' : 'badge--disabled'">
            {{ detail.status === 'ACTIVE' ? '启用' : '停用' }}
          </text>
        </view>
        <view v-if="detail.remark" class="field-row field-row--column">
          <text class="field-label">备注</text>
          <text class="field-value field-value--remark">{{ detail.remark }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">创建时间</text>
          <text class="field-value field-value--muted">{{ detail.createdAt }}</text>
        </view>
        <view class="field-row">
          <text class="field-label">更新时间</text>
          <text class="field-value field-value--muted">{{ detail.updatedAt }}</text>
        </view>
      </view>

      <!-- View mode bottom bar -->
      <view class="bottom-bar">
        <button v-if="canDelete" class="btn btn--danger" @click="handleDelete">删除</button>
        <button v-if="canEdit" class="btn btn--primary" @click="switchToEdit">编辑</button>
      </view>
    </view>

    <!-- Edit / Create mode -->
    <view v-else-if="mode === 'edit' || mode === 'create'" class="content">
      <!-- Merchant photo -->
      <view class="field-section">
        <text class="section-title">商户照片</text>
        <view class="photo-upload" @click="choosePhoto">
          <image v-if="form.merchantPhotoUrl" :src="toImageUrl(form.merchantPhotoUrl)" class="preview-img" mode="aspectFill" />
          <view v-else class="upload-placeholder">
            <AppIcon name="upload" size="40rpx" class="upload-icon" />
            <text class="upload-hint">点击上传照片</text>
          </view>
        </view>
      </view>

      <!-- Form fields -->
      <view class="field-section">
        <text class="section-title">基本信息</text>

        <view class="form-field">
          <text class="form-label">商户名称 <text class="required">*</text></text>
          <input
            class="form-input"
            v-model="form.merchantName"
            placeholder="请输入商户名称"
            placeholder-class="input-placeholder"
          />
          <text v-if="errors.merchantName" class="field-error">{{ errors.merchantName }}</text>
        </view>

        <view class="form-field">
          <text class="form-label">所属片区</text>
          <view class="form-picker" @click="showAreaPicker">
            <text :class="form.areaId ? 'picker-value' : 'picker-placeholder'">
              {{ selectedAreaName || '请选择片区' }}
            </text>
            <text class="picker-arrow">›</text>
          </view>
        </view>

        <view class="form-field">
          <text class="form-label">商户位置（经纬度）</text>
          <AMapPointPicker
            :longitude="longitudeStr ? Number(longitudeStr) : null"
            :latitude="latitudeStr ? Number(latitudeStr) : null"
            :use-current-location="isCreateMode"
            placeholder="点击选择商户位置"
            @update:longitude="(v) => { longitudeStr = v == null ? '' : String(v) }"
            @update:latitude="(v) => { latitudeStr = v == null ? '' : String(v) }"
          />
          <text v-if="errors.longitude" class="field-error">{{ errors.longitude }}</text>
        </view>
      </view>

      <view class="field-section">
        <text class="section-title">法人信息</text>

        <view class="form-field">
          <text class="form-label">法人姓名</text>
          <input
            class="form-input"
            v-model="form.legalPersonName"
            placeholder="请输入法人姓名"
            placeholder-class="input-placeholder"
          />
        </view>

        <view class="form-field">
          <text class="form-label">法人电话</text>
          <input
            class="form-input"
            v-model="form.legalPersonPhone"
            type="tel"
            placeholder="请输入法人电话"
            placeholder-class="input-placeholder"
          />
          <text v-if="errors.legalPersonPhone" class="field-error">{{ errors.legalPersonPhone }}</text>
        </view>

        <view class="form-field">
          <text class="form-label">法人照片</text>
          <view class="photo-upload photo-upload--sm" @click="chooseLegalPhoto">
            <image v-if="form.legalPersonPhotoUrl" :src="toImageUrl(form.legalPersonPhotoUrl)" class="preview-img" mode="aspectFill" />
            <view v-else class="upload-placeholder">
              <AppIcon name="upload" size="32rpx" class="upload-icon" />
              <text class="upload-hint">上传照片</text>
            </view>
          </view>
        </view>
      </view>

      <view class="field-section">
        <text class="section-title">其他信息</text>

        <view class="form-field">
          <text class="form-label">状态</text>
          <view class="radio-row">
            <view
              class="radio-item"
              :class="{ 'radio-item--active': form.status === 'ACTIVE' }"
              @click="form.status = 'ACTIVE'"
            >
              <text>启用</text>
            </view>
            <view
              class="radio-item"
              :class="{ 'radio-item--active': form.status === 'DISABLED' }"
              @click="form.status = 'DISABLED'"
            >
              <text>停用</text>
            </view>
          </view>
        </view>

        <view class="form-field">
          <text class="form-label">备注</text>
          <textarea
            class="form-textarea"
            v-model="form.remark"
            placeholder="请输入备注信息"
            placeholder-class="input-placeholder"
            :auto-height="true"
          />
        </view>
      </view>

      <!-- Submit bottom bar -->
      <view class="bottom-bar">
        <button class="btn btn--ghost" @click="goBack">取消</button>
        <button class="btn btn--primary" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '保存中...' : '保存' }}
        </button>
      </view>
    </view>

    <!-- Loading state -->
    <view v-if="loading" class="loading-overlay">
      <text class="loading-text">加载中...</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive, nextTick } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AppIcon from '../../src/components/AppIcon.vue'
import AMapPointPicker from '../../src/components/AMapPointPicker.vue'
import {
  getMerchant,
  createMerchant,
  updateMerchant,
  deleteMerchant,
  listAreaOptions,
  type MerchantItem,
  type MerchantUpsert,
  type AreaOption
} from '../../src/api/merchant'
import { uploadFile, toImageUrl, fetchAccessPrefix, toFullImageUrl } from '../../src/api/upload'
import { ensureAuthenticated } from '../../src/uni/navigation'
import { hasButtonPermission } from '../../src/auth/permissions'

const viewMapElId = 'merchant-map-' + Math.random().toString(36).slice(2, 8)

const mode = ref<'view' | 'edit' | 'create'>('view')
const merchantId = ref<number | null>(null)
const detail = ref<MerchantItem | null>(null)
const loading = ref(false)
const submitting = ref(false)
const areaOptions = ref<AreaOption[]>([])

const form = reactive<MerchantUpsert & { merchantName: string; status: 'ACTIVE' | 'DISABLED' }>({
  merchantName: '',
  merchantPhotoUrl: null,
  longitude: null,
  latitude: null,
  legalPersonName: null,
  legalPersonPhotoUrl: null,
  legalPersonPhone: null,
  areaId: null,
  areaMatchMode: 'MANUAL',
  remark: null,
  status: 'ACTIVE'
})

const longitudeStr = ref('')
const latitudeStr = ref('')
const errors = reactive<Record<string, string>>({})

const canEdit = computed(() => hasButtonPermission('button:h5:merchant:update'))
const canDelete = computed(() => hasButtonPermission('button:h5:merchant:delete'))
const isCreateMode = computed(() => mode.value === 'create')

const pageTitle = computed(() => {
  if (mode.value === 'create') return '新增商户'
  if (mode.value === 'edit') return '编辑商户'
  return '商户详情'
})

const selectedAreaName = computed(() => {
  if (!form.areaId) return ''
  return areaOptions.value.find((a) => a.id === form.areaId)?.areaName ?? ''
})

function resetForm() {
  form.merchantName = ''
  form.merchantPhotoUrl = null
  form.longitude = null
  form.latitude = null
  form.legalPersonName = null
  form.legalPersonPhotoUrl = null
  form.legalPersonPhone = null
  form.areaId = null
  form.areaMatchMode = 'MANUAL'
  form.remark = null
  form.status = 'ACTIVE'
  longitudeStr.value = ''
  latitudeStr.value = ''
  Object.keys(errors).forEach((k) => delete errors[k])
}

function fillFormFromDetail(d: MerchantItem) {
  form.merchantName = d.merchantName
  form.merchantPhotoUrl = d.merchantPhotoUrl
  form.longitude = d.longitude
  form.latitude = d.latitude
  form.legalPersonName = d.legalPersonName
  form.legalPersonPhotoUrl = d.legalPersonPhotoUrl
  form.legalPersonPhone = d.legalPersonPhone
  form.areaId = d.areaId
  form.areaMatchMode = d.areaMatchMode
  form.remark = d.remark
  form.status = d.status
  longitudeStr.value = d.longitude != null ? String(d.longitude) : ''
  latitudeStr.value = d.latitude != null ? String(d.latitude) : ''
}

function validate() {
  Object.keys(errors).forEach((k) => delete errors[k])
  let valid = true
  if (!form.merchantName.trim()) {
    errors.merchantName = '商户名称不能为空'
    valid = false
  }
  if (form.legalPersonPhone && !/^1[3-9]\d{9}$/.test(form.legalPersonPhone.trim())) {
    errors.legalPersonPhone = '请输入正确的手机号'
    valid = false
  }
  const hasLon = longitudeStr.value.trim().length > 0
  const hasLat = latitudeStr.value.trim().length > 0
  if (hasLon !== hasLat) {
    errors.longitude = '经度和纬度必须同时填写'
    errors.latitude = '经度和纬度必须同时填写'
    valid = false
  }
  return valid
}

function showAreaPicker() {
  if (!areaOptions.value.length) return
  const items = areaOptions.value.map((a) => a.areaName)
  uni.showActionSheet({
    itemList: items,
    success: (res) => {
      form.areaId = areaOptions.value[res.tapIndex].id
    }
  })
}

async function choosePhoto() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      try {
        const result = await uploadFile(res.tempFilePaths[0], 'merchant')
        form.merchantPhotoUrl = toFullImageUrl(result.objectName)
      } catch {
        uni.showToast({ title: '照片上传失败', icon: 'none' })
      }
    }
  })
}

function previewLegalPhoto() {
  if (!detail.value?.legalPersonPhotoUrl) return
  uni.previewImage({ urls: [toImageUrl(detail.value.legalPersonPhotoUrl)] })
}

async function chooseLegalPhoto() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      try {
        const result = await uploadFile(res.tempFilePaths[0], 'merchant')
        form.legalPersonPhotoUrl = toFullImageUrl(result.objectName)
      } catch {
        uni.showToast({ title: '照片上传失败', icon: 'none' })
      }
    }
  })
}

async function handleSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    const payload: MerchantUpsert = {
      merchantName: form.merchantName.trim(),
      merchantPhotoUrl: form.merchantPhotoUrl || null,
      longitude: longitudeStr.value ? Number(longitudeStr.value) : null,
      latitude: latitudeStr.value ? Number(latitudeStr.value) : null,
      legalPersonName: form.legalPersonName?.trim() || null,
      legalPersonPhotoUrl: form.legalPersonPhotoUrl || null,
      legalPersonPhone: form.legalPersonPhone?.trim() || null,
      areaId: form.areaId,
      areaMatchMode: form.areaMatchMode,
      remark: form.remark?.trim() || null,
      status: form.status
    }
    if (mode.value === 'create') {
      await createMerchant(payload)
      uni.showToast({ title: '新增成功', icon: 'success' })
    } else {
      await updateMerchant(merchantId.value!, payload)
      uni.showToast({ title: '保存成功', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e: any) {
    uni.showToast({ title: (e?.message && /[\u4e00-\u9fa5]/.test(e.message) ? e.message : '操作失败'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function switchToEdit() {
  if (detail.value) fillFormFromDetail(detail.value)
  mode.value = 'edit'
}

async function handleDelete() {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除商户「${detail.value?.merchantName}」吗？此操作不可撤销。`,
    confirmColor: '#ef4444',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteMerchant(merchantId.value!)
        uni.showToast({ title: '删除成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 800)
      } catch (e: any) {
        uni.showToast({ title: (e?.message && /[\u4e00-\u9fa5]/.test(e.message) ? e.message : '删除失败'), icon: 'none' })
      }
    }
  })
}

async function initViewMap() {
  // #ifdef MP-WEIXIN
  return
  // #endif
  if (!detail.value?.longitude || !detail.value?.latitude) return
  const container = document.getElementById(viewMapElId)
  if (!container) return

  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: '0a57a5453a660300283bebf7323d8bce'
  }

  const AMapLoader = (await import('@amap/amap-jsapi-loader')).default
  const AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: []
  })

  const lng = detail.value.longitude
  const lat = detail.value.latitude

  const satelliteLayer = new AMapLib.TileLayer.Satellite()
  const roadNetLayer = new AMapLib.TileLayer.RoadNet()

  const map = new AMapLib.Map(container, {
    zoom: 16,
    center: [lng, lat],
    viewMode: '2D',
    layers: [satelliteLayer, roadNetLayer],
    resizeEnable: false
  })

  new AMapLib.Marker({
    position: new AMapLib.LngLat(lng, lat),
    anchor: 'bottom-center',
    map
  })
}

function goBack() {
  uni.navigateBack()
}

onLoad((options) => {
  const m = options?.mode as string
  if (m === 'create' || m === 'edit' || m === 'view') {
    mode.value = m
  }
  if (options?.id) {
    merchantId.value = Number(options.id)
  }
})

onShow(async () => {
  if (!ensureAuthenticated('/merchants')) return

  // Ensure access prefix is loaded for image display
  await fetchAccessPrefix().catch(() => {})

  // Load area options
  try {
    areaOptions.value = await listAreaOptions()
  } catch {
    areaOptions.value = []
  }

  if (mode.value === 'create') {
    resetForm()
    return
  }
  if (merchantId.value) {
    loading.value = true
    try {
      detail.value = await getMerchant(merchantId.value)
      if (mode.value === 'edit') fillFormFromDetail(detail.value)
      if (mode.value === 'view' && detail.value?.longitude != null) {
        void nextTick(() => { setTimeout(() => initViewMap(), 200) })
      }
    } catch {
      uni.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 24rpx 160rpx;
  background: linear-gradient(180deg, #060f18 0%, #030913 100%);
  color: #eef6ff;
}

.content {
  padding: 20rpx 24rpx;
  display: grid;
  gap: 20rpx;
}

.field-section {
  border-radius: 18rpx;
  background: rgba(13, 30, 50, 0.9);
  border: 1px solid rgba(125, 163, 220, 0.12);
  padding: 20rpx;
  display: grid;
  gap: 16rpx;
}

.section-title {
  font-size: 26rpx;
  color: #5e7488;
  font-weight: 600;
  letter-spacing: 1rpx;
  text-transform: uppercase;
}

.field-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.field-row:last-child {
  border-bottom: none;
}

.field-map {
  padding: 0 20rpx 16rpx;
}

.detail-map {
  width: 100%;
  height: 400rpx;
  border-radius: 12rpx;
  border: 1px solid rgba(125, 163, 220, 0.15);
}

.field-row--column {
  flex-direction: column;
  align-items: flex-start;
  gap: 8rpx;
}

.field-label {
  font-size: 28rpx;
  color: #8ba1b4;
  flex-shrink: 0;
}

.field-value {
  font-size: 28rpx;
  color: #eef6ff;
  text-align: right;
}

.field-value--link {
  color: #5ea2ff;
}

.field-value--muted {
  color: #5e7488;
  font-size: 26rpx;
}

.field-value--remark {
  color: #8ba1b4;
  text-align: left;
  line-height: 1.6;
}

.status-badge {
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}

.badge--active {
  background: rgba(31, 190, 166, 0.14);
  color: #1fbea6;
  border: 1px solid rgba(31, 190, 166, 0.28);
}

.badge--disabled {
  background: rgba(239, 68, 68, 0.12);
  color: #ef8888;
  border: 1px solid rgba(239, 68, 68, 0.24);
}

.photo-preview,
.photo-upload {
  width: 100%;
  height: 280rpx;
  border-radius: 14rpx;
  overflow: hidden;
  background: rgba(20, 40, 65, 0.9);
  border: 1px solid rgba(125, 163, 220, 0.1);
}

.photo-upload--sm {
  height: 180rpx;
}

.photo-preview--sm {
  height: 180rpx;
}

.preview-img {
  width: 100%;
  height: 100%;
}

.preview-placeholder,
.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}

.preview-icon,
.upload-icon {
  color: #5e7488;
}

.preview-hint,
.upload-hint {
  font-size: 26rpx;
  color: #5e7488;
}

.form-field {
  display: grid;
  gap: 10rpx;
}

.form-label {
  font-size: 28rpx;
  color: #8ba1b4;
}

.required {
  color: #ef4444;
}

.form-input {
  padding: 18rpx 20rpx;
  border-radius: 12rpx;
  background: rgba(20, 40, 65, 0.8);
  border: 1px solid rgba(125, 163, 220, 0.14);
  font-size: 30rpx;
  color: #eef6ff;
  height: auto;
  min-height: 80rpx;
  box-sizing: border-box;
}

/* Ensure native input fills the full clickable area */
:deep(.uni-input-wrapper) {
  width: 100%;
  height: 100%;
}

:deep(.uni-input-input) {
  width: 100%;
  height: 100% !important;
}

:deep(.uni-input-placeholder) {
  pointer-events: none;
}

:deep(.uni-textarea-placeholder) {
  pointer-events: none;
}

.form-textarea {
  padding: 18rpx 20rpx;
  border-radius: 12rpx;
  background: rgba(20, 40, 65, 0.8);
  border: 1px solid rgba(125, 163, 220, 0.14);
  font-size: 30rpx;
  color: #eef6ff;
  min-height: 120rpx;
  width: 100%;
}

.input-placeholder {
  color: #5e7488;
}

.form-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 20rpx;
  border-radius: 12rpx;
  background: rgba(20, 40, 65, 0.8);
  border: 1px solid rgba(125, 163, 220, 0.14);
}

.picker-value {
  font-size: 30rpx;
  color: #eef6ff;
}

.picker-placeholder {
  font-size: 30rpx;
  color: #5e7488;
}

.picker-arrow {
  color: #5e7488;
  font-size: 34rpx;
}

.field-error {
  font-size: 26rpx;
  color: #ef4444;
}

.radio-row {
  display: flex;
  gap: 16rpx;
}

.radio-item {
  flex: 1;
  padding: 16rpx;
  border-radius: 12rpx;
  background: rgba(20, 40, 65, 0.8);
  border: 1px solid rgba(125, 163, 220, 0.14);
  text-align: center;
  font-size: 30rpx;
  color: #8ba1b4;
}

.radio-item--active {
  background: rgba(56, 152, 253, 0.16);
  border-color: rgba(56, 152, 253, 0.4);
  color: #5ea2ff;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 16rpx;
  padding: 20rpx 24rpx 40rpx;
  background: rgba(6, 15, 24, 0.96);
  border-top: 1px solid rgba(125, 163, 220, 0.08);
}

.btn {
  flex: 1;
  height: 88rpx;
  border-radius: 14rpx;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
}

.btn--primary {
  background: linear-gradient(135deg, #3898fd, #2272d9);
  color: #fff;
}

.btn--danger {
  background: rgba(239, 68, 68, 0.14);
  border: 1px solid rgba(239, 68, 68, 0.36);
  color: #ef8888;
}

.btn--ghost {
  background: rgba(13, 30, 50, 0.9);
  border: 1px solid rgba(125, 163, 220, 0.16);
  color: #8ba1b4;
}

.loading-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(3, 9, 19, 0.6);
}

.loading-text {
  font-size: 32rpx;
  color: #eef6ff;
}
</style>