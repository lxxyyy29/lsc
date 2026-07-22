<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { SystemUserDetail, SystemUserSavePayload, SystemUserStatus } from '../../api/system-user'
import SystemDialog from './SystemDialog.vue'

const props = defineProps<{
  open: boolean
  user?: SystemUserDetail | null
  roleOptions: Array<{ id: number; roleName: string }>
}>()

const emit = defineEmits<{
  close: []
  save: [payload: SystemUserSavePayload]
}>()

const form = reactive<SystemUserSavePayload>({
  username: '',
  realName: '',
  phone: '',
  status: 'ACTIVE'
})

const phoneError = ref('')
const usernameError = ref('')
const phonePattern = /^1[3-9]\d{9}$/
const usernamePattern = /^[A-Za-z][A-Za-z0-9_-]{3,31}$/
const title = computed(() => (props.user ? '编辑用户' : '新增用户'))
const isCreateMode = computed(() => !props.user)

watch(
  () => [props.open, props.user],
  () => {
    if (!props.open) return
    form.id = props.user?.id
    form.username = props.user?.username ?? ''
    form.realName = props.user?.realName ?? ''
    form.phone = props.user?.phone ?? ''
    form.status = props.user?.status ?? 'ACTIVE'
    form.roleIds = props.user?.roleIds ? [...props.user.roleIds] : []
    phoneError.value = ''
    usernameError.value = ''
  },
  { immediate: true }
)

function toggleRole(id: number) {
  const ids = form.roleIds ?? []
  if (ids.includes(id)) {
    form.roleIds = ids.filter((item) => item !== id)
    return
  }
  form.roleIds = [...ids, id]
}

function handleSave() {
  const username = form.username.trim()
  const phone = form.phone.trim()
  if (!usernamePattern.test(username)) {
    usernameError.value = '账号需以字母开头，支持字母、数字、下划线和短横线，4-32位'
    return
  }
  usernameError.value = ''
  if (isCreateMode.value && !phone) {
    phoneError.value = '新增用户时手机号不能为空'
    return
  }
  if (phone && !phonePattern.test(phone)) {
    phoneError.value = '请输入正确的11位手机号'
    return
  }
  phoneError.value = ''
  emit('save', {
    id: form.id,
    username,
    realName: form.realName.trim(),
    phone,
    status: form.status as SystemUserStatus,
    roleIds: form.roleIds ? [...form.roleIds] : []
  })
}
</script>

<template>
  <SystemDialog :open="open" :title="title" subtitle="账号治理" panel-class="system-user-dialog" @close="emit('close')">
    <div class="system-user-dialog__body">
      <label class="field-stack">
        <span>姓名</span>
        <input v-model="form.realName" aria-label="用户姓名" />
      </label>
      <label class="field-stack">
        <span>账号</span>
        <input
          v-model="form.username"
          aria-label="用户账号"
          maxlength="32"
          pattern="^[A-Za-z][A-Za-z0-9_-]{3,31}$"
          @input="usernameError = ''"
        />
        <small v-if="usernameError" class="field-error">{{ usernameError }}</small>
      </label>
      <label class="field-stack">
        <span>手机号</span>
        <input
          v-model="form.phone"
          aria-label="手机号"
          inputmode="numeric"
          maxlength="11"
          pattern="^1[3-9]\d{9}$"
          @input="phoneError = ''"
        />
        <small v-if="phoneError" class="field-error">{{ phoneError }}</small>
      </label>
      <label class="field-stack">
        <span>账号状态</span>
        <select v-model="form.status" aria-label="账号状态">
          <option value="ACTIVE">启用</option>
          <option value="DISABLED">停用</option>
        </select>
      </label>
      <fieldset class="field-stack system-user-dialog__fieldset">
        <legend>角色绑定</legend>
        <div class="system-chip-list">
          <button
            v-for="role in roleOptions"
            :key="role.id"
            type="button"
            class="system-chip"
            :class="{ 'system-chip--active': (form.roleIds ?? []).includes(role.id) }"
            @click="toggleRole(role.id)"
          >
            {{ role.roleName }}
          </button>
        </div>
      </fieldset>
    </div>

    <template #footer>
      <button type="button" class="action-button action-button--secondary" @click="emit('close')">取消</button>
      <button type="button" class="action-button" @click="handleSave">保存</button>
    </template>
  </SystemDialog>
</template>

<style scoped>
@import '../../views/admin-shared.css';

.system-user-dialog__body {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.system-user-dialog__fieldset {
  grid-column: 1 / -1;
  border: 1px solid rgba(125, 163, 220, 0.18);
  border-radius: 14px;
  padding: 12px;
}

.system-user-dialog__fieldset legend {
  color: rgba(205, 222, 248, 0.78);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.system-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.system-chip {
  border: 1px solid rgba(115, 235, 255, 0.14);
  border-radius: 999px;
  padding: 8px 14px;
  background: rgba(12, 29, 48, 0.92);
  color: #d8edff;
  cursor: pointer;
}

.system-chip--active {
  background: rgba(35, 160, 250, 0.22);
  border-color: rgba(115, 235, 255, 0.34);
  color: #ffffff;
}

.field-error {
  color: #ff8f8f;
  font-size: 12px;
  line-height: 1.4;
}

@media (max-width: 720px) {
  .system-user-dialog__body {
    grid-template-columns: 1fr;
  }
}
</style>
