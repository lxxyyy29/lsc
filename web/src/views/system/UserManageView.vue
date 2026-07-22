<template>
  <PageContainer title="用户管理">
    <WebListPageTemplate
      table-title="用户列表"
      :table-meta="`当前共 ${total} 个用户`"
    >
      <template #table-actions>
        <button class="action-button" @click="handleAdd">新增用户</button>
      </template>
      <template #table>
        <div v-if="loading" class="panel empty-state">加载中...</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>用户编号</th>
              <th>用户账号</th>
              <th>用户姓名</th>
              <th>手机号</th>
              <th>角色</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.username }}</td>
              <td>{{ item.realName }}</td>
              <td>{{ item.phone || '-' }}</td>
              <td>{{ item.roleNames?.join('、') || '-' }}</td>
              <td>
                <button
                  type="button"
                  class="status-toggle-button"
                  :title="item.status === 'ACTIVE' ? '点击停用账号' : '点击启用账号'"
                  @click="handleToggleStatus(item)"
                >
                  <StatusTag :status="item.status" />
                </button>
              </td>
              <td>
                <div class="table-actions">
                  <button class="action-link" @click="handleEdit(item)">编辑</button>
                  <button v-if="canChangePassword" class="action-link" @click="openPasswordDialog(item)">修改密码</button>
                  <button v-if="canDelete" class="action-link action-link--danger" @click="handleDelete(item)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <ListPagination v-if="total > 0" :total="total" :current-page="currentPage" :page-size="pageSize" :disabled="loading" @change="changePage" />
        <div v-if="!loading && !items.length && !errorMessage" class="panel empty-state">暂无用户数据。</div>
      </template>
    </WebListPageTemplate>

    <SystemUserDrawer
      :open="drawerOpen"
      :user="editingUser"
      :role-options="roleOptions"
      @close="drawerOpen = false"
      @save="onSaveUser"
    />

    <!-- Password change dialog -->
    <SystemDialog
      :open="passwordDialogOpen"
      title="修改密码"
      subtitle="账号治理"
      @close="closePasswordDialog"
    >
      <div class="password-dialog__body">
        <label class="field-stack">
          <span>新密码</span>
          <input
            v-model="passwordForm.newPassword"
            type="password"
            aria-label="新密码"
            placeholder="请输入新密码（至少6位）"
            autocomplete="new-password"
          />
          <span v-if="passwordError" class="field-error">{{ passwordError }}</span>
        </label>
        <label class="field-stack">
          <span>确认密码</span>
          <input
            v-model="passwordForm.confirmPassword"
            type="password"
            aria-label="确认密码"
            placeholder="请再次输入新密码"
            autocomplete="new-password"
          />
          <span v-if="confirmError" class="field-error">{{ confirmError }}</span>
        </label>
        <p v-if="passwordSaveError" class="save-error">{{ passwordSaveError }}</p>
      </div>
      <template #footer>
        <button type="button" class="action-button action-button--secondary" @click="closePasswordDialog">取消</button>
        <button type="button" class="action-button" :disabled="passwordSaving" @click="handlePasswordSave">
          {{ passwordSaving ? '保存中...' : '保存' }}
        </button>
      </template>
    </SystemDialog>

    <SystemDialog
      :open="deleteDialogOpen"
      title="确认删除"
      subtitle="账号治理"
      @close="closeDeleteDialog"
    >
      <div class="delete-dialog__body">
        <p>确定删除账号「{{ deleteTargetUser?.username }}」吗？</p>
        <p>删除后该账号将无法登录，历史业务记录会继续保留。</p>
        <p v-if="deleteError" class="save-error">{{ deleteError }}</p>
      </div>
      <template #footer>
        <button type="button" class="action-button action-button--secondary" @click="closeDeleteDialog">取消</button>
        <button type="button" class="action-button action-button--danger" :disabled="deleteSaving" @click="confirmDelete">
          {{ deleteSaving ? '删除中...' : '确认删除' }}
        </button>
      </template>
    </SystemDialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useToast } from '../../composables/useToast'
import PageContainer from '../../components/admin/PageContainer.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import SystemUserDrawer from '../../components/system/SystemUserDrawer.vue'
import SystemDialog from '../../components/system/SystemDialog.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import { usePagination } from '../../composables/usePagination'
import { hasPermission } from '../../auth/permissions'
import { getWebSession } from '../../auth/session'
import {
  listSystemUsersPaged,
  getSystemUserDetail,
  createSystemUser,
  updateSystemUser,
  updateSystemUserStatus,
  deleteSystemUser,
  assignSystemUserRoles,
  changeUserPassword,
  type SystemUser,
  type SystemUserDetail,
  type SystemUserSavePayload
} from '../../api/system-user'
import { listSystemRoles } from '../../api/system-role'

const filters = ref({})

const { items, total, loading, errorMessage, currentPage, pageSize, changePage, resetAndReload } = usePagination<SystemUser>({
  fetcher: (page, ps) => listSystemUsersPaged(page, ps),
  filters
})

const toast = useToast()
watch(errorMessage, (msg) => { if (msg) toast.error(msg) })

const roleOptions = ref<Array<{ id: number; roleName: string }>>([])
const drawerOpen = ref(false)
const editingUser = ref<SystemUserDetail | null>(null)

// Password dialog state
const passwordDialogOpen = ref(false)
const passwordTargetUser = ref<SystemUser | null>(null)
const passwordForm = reactive({ newPassword: '', confirmPassword: '' })
const passwordError = ref('')
const confirmError = ref('')
const passwordSaveError = ref('')
const passwordSaving = ref(false)
const deleteDialogOpen = ref(false)
const deleteTargetUser = ref<SystemUser | null>(null)
const deleteError = ref('')
const deleteSaving = ref(false)

const canChangePassword = computed(() =>
  hasPermission('button:system:user:change-password') ||
  hasPermission('api:system:user:change-password') ||
  (getWebSession()?.roleCodes.includes('SUPER_ADMIN') ?? false)
)
const canDelete = computed(() => hasPermission('button:system:user:delete') || hasPermission('api:system:user:delete'))

async function loadRoles() {
  const roles = await listSystemRoles()
  roleOptions.value = roles.map((r) => ({ id: r.id, roleName: r.roleName }))
}

onMounted(async () => {
  await Promise.all([resetAndReload(), loadRoles()])
})

function handleAdd() {
  editingUser.value = null
  drawerOpen.value = true
}

async function handleEdit(user: SystemUser) {
  editingUser.value = await getSystemUserDetail(user.id)
  drawerOpen.value = true
}

async function handleToggleStatus(user: SystemUser) {
  const newStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateSystemUserStatus(user.id, newStatus)
  await resetAndReload()
}

function handleDelete(user: SystemUser) {
  deleteTargetUser.value = user
  deleteError.value = ''
  deleteDialogOpen.value = true
}

function closeDeleteDialog() {
  if (deleteSaving.value) return
  deleteDialogOpen.value = false
  deleteTargetUser.value = null
  deleteError.value = ''
}

async function confirmDelete() {
  if (!deleteTargetUser.value) return
  deleteSaving.value = true
  deleteError.value = ''
  try {
    await deleteSystemUser(deleteTargetUser.value.id)
    toast.success('账号已删除')
    deleteDialogOpen.value = false
    deleteTargetUser.value = null
    deleteError.value = ''
    await resetAndReload()
  } catch (error) {
    deleteError.value = error instanceof Error ? error.message : '账号删除失败'
  } finally {
    deleteSaving.value = false
  }
}

async function onSaveUser(payload: SystemUserSavePayload) {
  if (payload.id) {
    await updateSystemUser(payload.id, payload)
    await assignSystemUserRoles(payload.id, payload.roleIds ?? [])
  } else {
    await createSystemUser(payload)
  }
  drawerOpen.value = false
  editingUser.value = null
  await resetAndReload()
}

function openPasswordDialog(user: SystemUser) {
  passwordTargetUser.value = user
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordError.value = ''
  confirmError.value = ''
  passwordSaveError.value = ''
  passwordDialogOpen.value = true
}

function closePasswordDialog() {
  passwordDialogOpen.value = false
  passwordTargetUser.value = null
}

async function handlePasswordSave() {
  passwordError.value = ''
  confirmError.value = ''
  passwordSaveError.value = ''

  let valid = true
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    passwordError.value = '密码不能为空且至少6位'
    valid = false
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    confirmError.value = '两次密码输入不一致'
    valid = false
  }
  if (!valid || !passwordTargetUser.value) return

  passwordSaving.value = true
  try {
    await changeUserPassword(passwordTargetUser.value.id, passwordForm.newPassword)
    closePasswordDialog()
  } catch (error) {
    passwordSaveError.value = error instanceof Error ? error.message : '密码修改失败，请稍后重试'
  } finally {
    passwordSaving.value = false
  }
}
</script>

<style scoped>
@import '../admin-shared.css';

.error-text {
  color: #ffb4b4;
  margin-top: 16px;
}

.password-dialog__body,
.delete-dialog__body {
  display: grid;
  gap: 16px;
}

.delete-dialog__body p {
  margin: 0;
  color: rgba(221, 235, 255, 0.86);
  line-height: 1.7;
}

.field-error {
  color: #ffb4b4;
  font-size: 12px;
  margin-top: 2px;
}

.save-error {
  color: #ffb4b4;
  font-size: 13px;
  margin: 0;
}

.status-toggle-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.status-toggle-button:hover {
  filter: brightness(1.08);
}
</style>
