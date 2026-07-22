<template>
  <SystemDialog :open="open" :title="title" :subtitle="subtitle" :panel-class="'biz-entity-dialog' + (panelClass ? ' ' + panelClass : '')" @close="emit('close')">
    <div class="biz-dialog-form">
      <component :is="field.type === 'custom' || field.type === 'image-upload' ? 'div' : 'label'" v-for="field in visibleFields" :key="field.key" class="field-stack" :class="field.className">
        <span>{{ field.label }}</span>
        <template v-if="field.type === 'select'">
          <select :value="String(modelValue[field.key] ?? '')" :aria-label="field.label" @change="updateField(field.key, ($event.target as HTMLSelectElement).value)">
            <option v-for="option in field.options || []" :key="String(option.value)" :value="String(option.value)">{{ option.label }}</option>
          </select>
        </template>
        <template v-else-if="field.type === 'textarea'">
          <textarea :value="String(modelValue[field.key] ?? '')" :rows="field.rows || 4" :aria-label="field.label" :placeholder="field.placeholder" @input="updateField(field.key, ($event.target as HTMLTextAreaElement).value)"></textarea>
        </template>
        <template v-else-if="field.type === 'custom'">
          <slot :name="'field-' + field.key" :field="field" :value="modelValue[field.key]" />
        </template>
        <template v-else-if="field.type === 'image-upload'">
          <div class="image-upload-zone">
            <div v-if="modelValue[field.key]" class="image-upload-preview">
              <img :src="toImageUrl(String(modelValue[field.key]))" :alt="field.label" />
              <button type="button" class="image-upload-remove" :aria-label="'清除' + field.label" @click="updateField(field.key, '')">✕</button>
            </div>
            <label v-else class="image-upload-trigger" :class="{ 'is-uploading': uploadingKeys[field.key] }">
              <input type="file" accept="image/*" class="sr-only" :aria-label="field.label" @change="handleImageUpload(field.key, $event)" :disabled="!!uploadingKeys[field.key]" />
              <span v-if="uploadingKeys[field.key]" class="image-upload-status">上传中...</span>
              <span v-else class="image-upload-placeholder">
                <span class="image-upload-icon">+</span>
                <span>点击上传图片</span>
              </span>
            </label>
          </div>
        </template>
        <template v-else>
          <input :value="String(modelValue[field.key] ?? '')" :type="field.type || 'text'" :aria-label="field.label" :placeholder="field.placeholder" @input="updateField(field.key, ($event.target as HTMLInputElement).value)" />
        </template>
        <small v-if="field.hint" class="field-hint">{{ field.hint }}</small>
        <small v-if="errors[field.key]" class="field-error">{{ errors[field.key] }}</small>
      </component>
    </div>
    <template #footer>
      <button type="button" class="action-button action-button--secondary" @click="emit('close')">取消</button>
      <button type="button" class="action-button" @click="emit('save')">保存</button>
    </template>
  </SystemDialog>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import SystemDialog from './SystemDialog.vue'
import { uploadFile, toImageUrl } from '../../api/upload'

interface Option {
  label: string
  value: string | number
}

interface FieldConfig {
  key: string
  label: string
  type?: 'text' | 'number' | 'tel' | 'textarea' | 'select' | 'custom' | 'image-upload'
  placeholder?: string
  hint?: string
  rows?: number
  options?: Option[]
  visible?: boolean
  className?: string
}

const props = defineProps<{
  open: boolean
  title: string
  subtitle?: string
  panelClass?: string
  modelValue: Record<string, string | number | null | undefined>
  fields: FieldConfig[]
  errors: Record<string, string>
}>()

const emit = defineEmits<{
  close: []
  save: []
  'update:modelValue': [value: Record<string, string | number | null | undefined>]
}>()

const uploadingKeys = reactive<Record<string, boolean>>({})

const visibleFields = computed(() => props.fields.filter((field) => field.visible !== false))

function updateField(key: string, value: string) {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value
  })
}

async function handleImageUpload(key: string, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploadingKeys[key] = true
  try {
    const result = await uploadFile(file, 'biz-photo')
    updateField(key, result.objectName)
  } catch (error) {
    const msg = error instanceof Error ? error.message : '上传失败'
    if (props.errors) {
      props.errors[key] = msg
    }
  } finally {
    uploadingKeys[key] = false
    input.value = ''
  }
}
</script>

<style scoped>
@import '../../views/admin-shared.css';

.biz-dialog-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field-hint,
.field-error {
  font-size: 12px;
  line-height: 1.5;
}

.field-hint {
  color: rgba(141, 176, 208, 0.88);
}

.field-error {
  color: #ffb4b4;
}

.field-stack--full {
  grid-column: 1 / -1;
}

/* Image upload styles */
.image-upload-zone {
  width: 100%;
}

.image-upload-preview {
  position: relative;
  display: inline-block;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(141, 176, 208, 0.25);
}

.image-upload-preview img {
  display: block;
  max-width: 100%;
  max-height: 160px;
  object-fit: contain;
  background: rgba(0, 0, 0, 0.2);
}

.image-upload-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  transition: background 0.2s;
}

.image-upload-remove:hover {
  background: rgba(220, 50, 50, 0.85);
}

.image-upload-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 120px;
  border: 2px dashed rgba(141, 176, 208, 0.3);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.image-upload-trigger:hover {
  border-color: rgba(141, 176, 208, 0.6);
  background: rgba(141, 176, 208, 0.05);
}

.image-upload-trigger.is-uploading {
  pointer-events: none;
  opacity: 0.6;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.image-upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: rgba(141, 176, 208, 0.6);
  font-size: 13px;
}

.image-upload-icon {
  font-size: 28px;
  line-height: 1;
  font-weight: 300;
}

.image-upload-status {
  color: rgba(141, 176, 208, 0.8);
  font-size: 13px;
}

@media (max-width: 720px) {
  .biz-dialog-form {
    grid-template-columns: 1fr;
  }
}
</style>
