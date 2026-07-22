<template>
  <PageContainer title="角色管理">
    <WebListPageTemplate
      table-title="角色列表"
      :table-meta="`当前共 ${total} 个角色`"
    >
      <template #table-actions>
        <button class="action-button" @click="handleAdd">新增角色</button>
      </template>

      <template #table>
        <div v-if="loading" class="panel empty-state">加载中...</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>角色编码</th>
              <th>角色名称</th>
              <th>状态</th>
              <th>备注</th>
              <th>用户数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.roleCode }}</td>
              <td>{{ item.roleName }}</td>
              <td><StatusTag :status="item.status" /></td>
              <td>{{ item.remark || '-' }}</td>
              <td>{{ item.userCount ?? 0 }}</td>
              <td>
                <div class="table-actions">
                  <button class="action-link" @click="handleEdit(item)">编辑</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <ListPagination v-if="total > 0" :total="total" :current-page="currentPage" :page-size="pageSize" :disabled="loading" @change="changePage" />
        <div v-if="!loading && !items.length && !errorMessage" class="panel empty-state">暂无角色数据。</div>
      </template>
    </WebListPageTemplate>

    <SystemRoleDrawer
      :open="roleDrawerOpen"
      :role="editingRole"
      @close="roleDrawerOpen = false"
      @save="onSaveRole"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useToast } from '../../composables/useToast'
import PageContainer from '../../components/admin/PageContainer.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import SystemRoleDrawer from '../../components/system/SystemRoleDrawer.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import { usePagination } from '../../composables/usePagination'
import {
  listSystemRolesPaged,
  saveSystemRole,
  saveSystemRolePermissions,
  type SystemRole,
  type SystemRoleSavePayload
} from '../../api/system-role'

const filters = ref({})

const { items, total, loading, errorMessage, currentPage, pageSize, changePage, resetAndReload } = usePagination<SystemRole>({
  fetcher: (page, ps) => listSystemRolesPaged(page, ps),
  filters
})

const toast = useToast()
watch(errorMessage, (msg) => { if (msg) toast.error(msg) })

const roleDrawerOpen = ref(false)
const editingRole = ref<SystemRole | null>(null)

onMounted(() => resetAndReload())

function handleAdd() {
  editingRole.value = null
  roleDrawerOpen.value = true
}

function handleEdit(role: SystemRole) {
  editingRole.value = role
  roleDrawerOpen.value = true
}

async function onSaveRole(payload: SystemRoleSavePayload, permissionIds: number[]) {
  const detail = await saveSystemRole(payload)
  await saveSystemRolePermissions(detail.id, { permissionIds })
  roleDrawerOpen.value = false
  await resetAndReload()
}
</script>

<style scoped>
@import '../admin-shared.css';

.error-text {
  color: #ffb4b4;
  margin-top: 16px;
}
</style>
