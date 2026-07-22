<template>
  <ElDatePicker
    v-model="rangeValue"
    class="zh-date-range-picker"
    type="daterange"
    unlink-panels
    clearable
    value-format="YYYY-MM-DD"
    start-placeholder="开始日期"
    end-placeholder="结束日期"
    range-separator="至"
    :placeholder="placeholder"
    popper-class="zh-date-range-picker-popper"
    @change="emitChange"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  start?: string
  end?: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:start': [value: string]
  'update:end': [value: string]
  change: []
}>()

const rangeValue = computed<[string, string] | []>({
  get() {
    return props.start && props.end ? [props.start, props.end] : []
  },
  set(value) {
    const [start = '', end = ''] = Array.isArray(value) ? value : []
    emit('update:start', start)
    emit('update:end', end)
  }
})

function emitChange() {
  emit('change')
}
</script>

<style scoped>
.zh-date-range-picker {
  width: 100%;
  min-width: 240px;
  height: 40px;
  --el-component-size: 40px;
  --el-input-height: 40px;
  --el-text-color-regular: #fff;
  --el-input-text-color: #fff;
}

:deep(.el-input__wrapper) {
  box-sizing: border-box;
  height: 40px !important;
  min-height: 40px !important;
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 8px;
  background: rgba(2, 8, 16, 0.6);
  box-shadow: none;
  transition: all 0.2s ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  border-color: #409eff;
  background: rgba(2, 8, 16, 0.9);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.15);
}

:deep(.el-range-input) {
  color: #fff !important;
  -webkit-text-fill-color: #fff;
  background: transparent;
  opacity: 1;
}

:deep(.el-range-input::placeholder) {
  color: rgba(255, 255, 255, 0.36);
}

:deep(.el-range__icon),
:deep(.el-range__close-icon) {
  color: rgba(205, 222, 248, 0.78);
}

:deep(.el-range-separator) {
  color: rgba(205, 222, 248, 0.78);
}

:deep(.el-range-separator) {
  flex: 0 0 28px;
}
</style>

<style>
.zh-date-range-picker.el-date-editor .el-range-input,
.zh-date-range-picker.el-date-editor .el-range-input[type="text"] {
  color: #fff !important;
  -webkit-text-fill-color: #fff !important;
  opacity: 1 !important;
}

.zh-date-range-picker.el-date-editor .el-range-input::placeholder {
  color: rgba(255, 255, 255, 0.36) !important;
  -webkit-text-fill-color: rgba(255, 255, 255, 0.36) !important;
}

.zh-date-range-picker-popper {
  --el-bg-color-overlay: #132a45;
  --el-fill-color-blank: #132a45;
  --el-text-color-primary: #eaf5ff;
  --el-text-color-regular: #d6e7f7;
  --el-text-color-placeholder: rgba(255, 255, 255, 0.36);
  --el-border-color-light: rgba(103, 187, 246, 0.22);
  --el-datepicker-off-text-color: rgba(205, 222, 248, 0.34);
  --el-datepicker-header-text-color: #eaf5ff;
  --el-datepicker-icon-color: #d6e7f7;
  --el-datepicker-inrange-bg-color: rgba(64, 158, 255, 0.16);
  --el-datepicker-inrange-hover-bg-color: rgba(64, 158, 255, 0.24);
  --el-datepicker-active-color: #409eff;
}
</style>
