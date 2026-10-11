<template>
  <view class="resident-repair-form-page">
    <view class="form-header">
      <text class="form-title">{{ isEdit ? '✏️ 报修详情' : '🔧 提交报修' }}</text>
      <text class="form-sub">{{ isEdit ? (isReadonly ? '该报修单已受理，仅可查看' : '未受理前可修改报修信息') : '填写报修信息，提交后由工作人员处理' }}</text>
    </view>

    <view class="form-card">
      <view class="form-group">
        <text class="label">报修类型</text>
        <picker :range="repairTypeOptions" range-key="label" :value="repairTypeIndex" @change="onRepairTypeChange" class="picker">
          <view class="picker-input">{{ repairTypeOptions[repairTypeIndex].label }}</view>
        </picker>
      </view>

      <view class="form-group">
        <text class="label">标题</text>
        <input v-model="form.title" class="input" placeholder="简要描述问题" placeholder-style="color:#9ca3af;" />
      </view>

      <view class="form-group">
        <text class="label">详细描述</text>
        <textarea v-model="form.description" class="textarea" placeholder="请详细描述..." placeholder-style="color:#9ca3af;" />
      </view>

      <view class="form-group">
        <text class="label">地址</text>
        <input v-model="form.address" class="input" placeholder="报修地址" placeholder-style="color:#9ca3af;" />
      </view>

      <view class="form-group">
        <text class="label">联系电话</text>
        <input v-model="form.reporterPhone" class="input" placeholder="您的电话" placeholder-style="color:#9ca3af;" />
      </view>

      <view class="form-actions">
        <button @click="cancel" class="btn-default">取消</button>
        <button
          @click="submitRepair"
          :disabled="submitting || isReadonly"
          class="btn-primary"
        >{{ submitting ? '提交中...' : (isEdit ? (isReadonly ? '已受理，不可修改' : '保存修改') : '提交') }}</button>
      </view>
      <text v-if="error" class="error">{{ error }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  submitRepair as submitRepairApi,
  getRepairDetail,
  updateRepair
} from '../../../src/api/resident'

const submitting = ref(false)
const error = ref('')
// 编辑/详情模式（缺陷 BUG-16：报修后不能查看详情、不能修改）：
// 带 id 进入本页即为详情，未受理时可改；已受理只读
const editingId = ref<number | null>(null)
const currentStatus = ref('')
const isEdit = computed(() => editingId.value !== null)
const isReadonly = computed(() => isEdit.value && currentStatus.value !== 'PENDING')
const form = reactive({
  repairType: 'WATER',
  title: '',
  description: '',
  address: '',
  reporterPhone: ''
})

const repairTypeOptions = [
  { value: 'WATER', label: '水电故障' },
  { value: 'ELEVATOR', label: '电梯故障' },
  { value: 'DOOR', label: '门禁问题' },
  { value: 'PIPE', label: '管道问题' },
  { value: 'ROOF', label: '屋面问题' },
  { value: 'OTHER', label: '其他' }
]
const repairTypeIndex = computed(() => {
  const idx = repairTypeOptions.findIndex(o => o.value === form.repairType)
  return idx >= 0 ? idx : 0
})

function onRepairTypeChange(e: any) {
  form.repairType = repairTypeOptions[e.detail.value].value
}

function cancel() {
  uni.navigateBack()
}

async function submitRepair() {
  if (isReadonly.value) {
    uni.showToast({ title: '该报修单已受理，无法修改', icon: 'none' })
    return
  }
  if (!form.title.trim() || !form.description.trim()) {
    error.value = '请填写标题和描述'
    return
  }
  if (!form.address.trim()) {
    error.value = '请填写报修地址'
    return
  }
  submitting.value = true
  error.value = ''
  try {
    if (isEdit.value && editingId.value !== null) {
      await updateRepair(editingId.value, { ...form })
      uni.showToast({ title: '修改成功', icon: 'success' })
    } else {
      await submitRepairApi(form)
      uni.showToast({ title: '提交成功！', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 800)
  } catch (e: any) {
    error.value = e?.message || '提交失败'
  } finally {
    submitting.value = false
  }
}

// 带 ?id=xx 进入 → 详情/修改模式：先拉详情回填，未受理时可保存
onLoad(async (query?: Record<string, string | undefined>) => {
  const id = query?.id ? Number(query.id) : NaN
  if (!Number.isFinite(id)) return
  editingId.value = id
  try {
    const detail: any = await getRepairDetail(id)
    if (detail) {
      form.repairType = detail.repairType || form.repairType
      form.title = detail.title || ''
      form.description = detail.description || ''
      form.address = detail.address || ''
      form.reporterPhone = detail.reporterPhone || ''
      currentStatus.value = detail.status || ''
    }
  } catch (e: any) {
    error.value = e?.message || '加载报修详情失败'
  }
})
</script>

<style>
/* 居民端-提交报修表单页 全局样式 */
.resident-repair-form-page { padding: 32rpx; background: #f5f7fa; min-height: 100vh; }
.resident-repair-form-page .form-header {
  background: linear-gradient(135deg, #fa541c 0%, #d4380d 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.resident-repair-form-page .form-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; color: #fff; }
.resident-repair-form-page .form-sub { font-size: 26rpx; opacity: 0.85; color: #fff; }
.resident-repair-form-page .form-card {
  background: #fff; border-radius: 24rpx; padding: 40rpx 32rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.resident-repair-form-page .form-group { margin-bottom: 28rpx; }
.resident-repair-form-page .label { display: block; font-size: 26rpx; color: #1f2937; margin-bottom: 10rpx; font-weight: 500; }
.resident-repair-form-page .input {
  width: 100%; height: 88rpx; padding: 0 24rpx; border: 1px solid #d1d5db; border-radius: 12rpx;
  font-size: 30rpx; color: #000; background: #fff; box-sizing: border-box;
}
.resident-repair-form-page .textarea {
  width: 100%; min-height: 180rpx; padding: 20rpx 24rpx; border: 1px solid #d1d5db; border-radius: 12rpx;
  font-size: 30rpx; color: #000; background: #fff; box-sizing: border-box; line-height: 1.5;
}
.resident-repair-form-page .picker-input {
  width: 100%; height: 88rpx; line-height: 88rpx; padding: 0 24rpx; border: 1px solid #d1d5db;
  border-radius: 12rpx; font-size: 30rpx; color: #000; background: #fff; box-sizing: border-box;
}
.resident-repair-form-page .form-actions { display: flex; gap: 24rpx; margin-top: 40rpx; }
.resident-repair-form-page .btn-primary {
  flex: 1; height: 88rpx; line-height: 88rpx; padding: 0; background: #fa541c; color: #fff; border: none;
  border-radius: 12rpx; font-size: 30rpx; font-weight: 600;
}
.resident-repair-form-page .btn-primary[disabled] { opacity: 0.6; }
.resident-repair-form-page .btn-default {
  flex: 1; height: 88rpx; line-height: 88rpx; padding: 0; background: #f3f4f6; color: #4b5563; border: none;
  border-radius: 12rpx; font-size: 30rpx;
}
.resident-repair-form-page .error { color: #ff4d4f; font-size: 26rpx; text-align: center; margin-top: 20rpx; display: block; }
</style>
