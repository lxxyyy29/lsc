<template>
  <PageContainer title="授权管理">
    <section class="system-page">
      <WebListPageTemplate
        title="授权管理"
        badge="账号台账"
        summary-aria-label="授权管理概览"
        filter-title="查询条件"
        table-title="用户与角色列表"
        :table-meta="`当前共 ${filteredUsers.length} 个账号`"
      >
        <template #filters>
          <QueryPanel>
            <label class="field-stack">
              <ElSelect v-model="selectedRole" clearable placeholder="请选择角色类型" aria-label="角色类型">
                <ElOption v-for="item in roleOptions" :key="item" :label="item" :value="item" />
              </ElSelect>
            </label>
            <label class="field-stack">
              <ElSelect v-model="selectedStatus" clearable placeholder="请选择账号状态" aria-label="账号状态">
                <ElOption v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </ElSelect>
            </label>
          </QueryPanel>
        </template>

        <template #table>
          <table class="data-table">
            <thead>
              <tr>
                <th>姓名</th>
                <th>账号</th>
                <th>手机号</th>
                <th>角色</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredUsers" :key="item.id">
                <td>{{ item.realName }}</td>
                <td>{{ item.username }}</td>
                <td>{{ item.phone || '-' }}</td>
                <td>{{ item.roleNames?.join('、') || '-' }}</td>
                <td><StatusTag :status="item.status" /></td>
              </tr>
            </tbody>
          </table>
        </template>
      </WebListPageTemplate>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import { listSystemUsers, type SystemUser } from '../../api/system-user'
import { listSystemRoles } from '../../api/system-role'

const users = ref<SystemUser[]>([])
const selectedRole = ref('')
const selectedStatus = ref('')

const roleOptions = computed(() => {
  const names = new Set(users.value.flatMap((u) => u.roleNames ?? []))
  return [...names]
})

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' }
]

const filteredUsers = computed(() =>
  users.value.filter((item) => {
    const matchesRole = !selectedRole.value || item.roleNames?.includes(selectedRole.value)
    const matchesStatus = !selectedStatus.value || item.status === selectedStatus.value
    return matchesRole && matchesStatus
  })
)

onMounted(async () => {
  const [userList] = await Promise.all([listSystemUsers(), listSystemRoles()])
  users.value = userList
})
</script>

<style scoped>
@import '../admin-shared.css';

.system-page {
  display: grid;
  gap: 16px;
}

:deep(.data-table) {
  border: 0;
  border-radius: 18px;
  overflow: hidden;
}

:deep(.data-table th) {
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
  font-size: 12px;
  font-weight: 600;
  background: rgba(94, 162, 255, 0.08);
}

@media (max-width: 720px) {
  .system-page {
    gap: 8px;
  }
}
</style>
