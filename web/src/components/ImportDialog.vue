<template>
  <div v-if="visible" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;">
    <div style="width:600px;max-height:80vh;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="font-size:16px;font-weight:600;">导入 {{ typeLabel }}</h3>
        <button @click="close" style="border:none;background:none;font-size:18px;cursor:pointer;color:#9ca3af;">&times;</button>
      </div>

      <!-- 步骤 1：选择文件 -->
      <div v-if="step === 1">
        <div style="border:2px dashed #d1d5db;border-radius:8px;padding:30px;text-align:center;margin-bottom:16px;cursor:pointer;" @click="triggerUpload">
          <i class="fas fa-cloud-upload-alt" style="font-size:32px;color:#9ca3af;margin-bottom:8px;"></i>
          <p style="font-size:14px;color:#374151;margin-bottom:4px;">点击选择 Excel 文件</p>
          <p style="font-size:12px;color:#9ca3af;">支持 .xlsx 格式</p>
          <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none;" @change="onFileSelected" />
        </div>
        <p v-if="selectedFile" style="font-size:13px;color:#52c41a;margin-bottom:12px;">
          <i class="fas fa-file-excel"></i> {{ selectedFile.name }} ({{ formatSize(selectedFile.size) }})
        </p>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <button @click="downloadTemplate" style="border:none;background:none;font-size:13px;color:#1890ff;cursor:pointer;padding:0;">
            <i class="fas fa-download"></i> 下载导入模板
          </button>
          <div style="display:flex;gap:8px;">
            <button @click="close" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
            <button @click="handlePreview" :disabled="!selectedFile || loading" style="padding:6px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
              {{ loading ? '解析中...' : '预览' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 步骤 2：预览结果 -->
      <div v-if="step === 2">
        <div style="padding:12px;background:#f6ffed;border:1px solid #b7eb8f;border-radius:8px;margin-bottom:16px;font-size:13px;">
          共解析 <strong>{{ previewData?.totalRows || 0 }}</strong> 行数据，
          预览前 <strong>{{ previewData?.previewed || 0 }}</strong> 行，
          <span v-if="previewData?.errorCount" style="color:#ff4d4f;">发现 <strong>{{ previewData.errorCount }}</strong> 个错误</span>
          <span v-else style="color:#52c41a;">格式校验通过</span>
        </div>

        <!-- 预览表格 -->
        <div v-if="previewData?.rows?.length" style="border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;margin-bottom:16px;max-height:300px;overflow-y:auto;">
          <table style="width:100%;border-collapse:collapse;font-size:12px;">
            <thead>
              <tr style="background:#f3f4f6;position:sticky;top:0;">
                <th style="padding:6px 10px;text-align:left;border-bottom:1px solid #e5e7eb;">行号</th>
                <th v-for="col in currentColumns" :key="col" style="padding:6px 10px;text-align:left;border-bottom:1px solid #e5e7eb;">{{ col }}</th>
                <th style="padding:6px 10px;text-align:left;border-bottom:1px solid #e5e7eb;color:#ff4d4f;">错误</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in previewData.rows" :key="row.row" :style="row.error ? 'background:#fff1f0;' : ''">
                <td style="padding:6px 10px;border-bottom:1px solid #f3f4f6;">{{ row.row }}</td>
                <td v-for="col in currentColumns" :key="col" style="padding:6px 10px;border-bottom:1px solid #f3f4f6;">{{ row[col] || '-' }}</td>
                <td style="padding:6px 10px;border-bottom:1px solid #f3f4f6;color:#ff4d4f;">{{ row.error || '' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 错误列表 -->
        <div v-if="previewData?.errors?.length" style="padding:10px;background:#fff1f0;border-radius:6px;margin-bottom:16px;max-height:120px;overflow-y:auto;">
          <p v-for="(err, idx) in previewData.errors" :key="idx" style="font-size:12px;color:#ff4d4f;margin:2px 0;">{{ err }}</p>
        </div>

        <div style="display:flex;justify-content:flex-end;gap:8px;">
          <button @click="step = 1" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">返回</button>
          <button @click="handleExecute" :disabled="loading || previewData?.errorCount > 0" style="padding:6px 16px;border:none;border-radius:6px;background:#52c41a;color:#fff;font-size:13px;cursor:pointer;">
            {{ loading ? '导入中...' : '确认导入' }}
          </button>
        </div>
      </div>

      <!-- 步骤 3：导入结果 -->
      <div v-if="step === 3">
        <div style="text-align:center;padding:30px;">
          <i class="fas fa-check-circle" style="font-size:48px;color:#52c41a;"></i>
          <p style="font-size:16px;font-weight:600;margin-top:12px;">导入完成</p>
          <p style="font-size:14px;color:#374151;margin-top:8px;">
            成功导入 <strong style="color:#52c41a;">{{ result?.success || 0 }}</strong> 条
            <span v-if="result?.fail > 0" style="color:#ff4d4f;">
              ，失败 <strong>{{ result.fail }}</strong> 条
            </span>
          </p>
        </div>

        <div v-if="result?.errors?.length" style="padding:10px;background:#fff1f0;border-radius:6px;margin-bottom:16px;max-height:150px;overflow-y:auto;">
          <p v-for="(err, idx) in result.errors" :key="idx" style="font-size:12px;color:#ff4d4f;margin:2px 0;">{{ err }}</p>
        </div>

        <div style="display:flex;justify-content:flex-end;">
          <button @click="close" style="padding:6px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">完成</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

import { previewImport, executeImport } from '../api'
import { showMessage } from '../utils/message'

const props = defineProps<{
  visible: boolean
  type: string  // population / buildings / places
  columns?: string[]  // 可选：人口库字段配置器动态列
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}>()

const typeLabels: Record<string, string> = {
  population: '实有人口',
  buildings: '房屋',
  places: '场所'
}

const typeLabel = computed(() => typeLabels[props.type] || props.type)

const allColumns: Record<string, string[]> = {
  population: ['name', 'idCard', 'phone', 'householdType', 'specialPopulation', 'specialPopulationType', 'relation', 'address', 'gridName', 'tags'],
  buildings: ['buildingNo', 'address', 'landlordName', 'landlordPhone', 'fireRiskLevel', 'isGroupRental', 'gridName'],
  places: ['placeName', 'contactName', 'contactPhone', 'address', 'remark', 'gridName']
}

const currentColumns = computed(() => props.columns && props.columns.length ? props.columns : (allColumns[props.type] || []))

const fileInput = ref<HTMLInputElement>()
const selectedFile = ref<File | null>(null)
const loading = ref(false)
const step = ref(1)
const previewData = ref<any>(null)
const result = ref<any>(null)

function triggerUpload() {
  fileInput.value?.click()
}

function onFileSelected(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files?.[0]) {
    selectedFile.value = target.files[0]
  }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function downloadTemplate() {
  // 简单的 CSV 模板下载
  const cols = currentColumns.value
  const headers: Record<string, string> = {
    population: '姓名*,身份证号*,手机号,户籍类型,是否特殊人群(是/否),特殊人群类型,与户主关系,地址,网格,标签',
    buildings: '楼栋编号,地址,房东姓名,房东电话,消防风险等级,是否群租,网格',
    places: '场所名称,负责人,负责人电话,地址,备注,网格'
  }
  const csv = headers[props.type] || cols.join(',')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${typeLabel.value}导入模板.csv`
  a.click()
  URL.revokeObjectURL(url)
}

async function handlePreview() {
  if (!selectedFile.value) return
  loading.value = true
  try {
    previewData.value = await previewImport(props.type, selectedFile.value, 10)
    step.value = 2
  } catch (e: any) {
    showMessage(e?.message || '文件解析失败')
  } finally {
    loading.value = false
  }
}

async function handleExecute() {
  if (!selectedFile.value) return
  loading.value = true
  try {
    result.value = await executeImport(props.type, selectedFile.value)
    step.value = 3
    emit('success')
  } catch (e: any) {
    showMessage(e?.message || '导入失败')
  } finally {
    loading.value = false
  }
}

function close() {
  emit('update:visible', false)
  // 重置状态
  step.value = 1
  selectedFile.value = null
  previewData.value = null
  result.value = null
}
</script>
