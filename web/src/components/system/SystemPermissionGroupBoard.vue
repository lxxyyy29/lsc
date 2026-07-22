<script setup lang="ts">
import { computed } from 'vue'
import { getSystemPermissionTypeLabel, type SystemPermission, type SystemPermissionType } from '../../api/system-permission'
import SystemStatusBadge from './SystemStatusBadge.vue'

const props = defineProps<{
  groups: Record<string, SystemPermission[]>
}>()

const orderedGroups = computed(() =>
  Object.entries(props.groups)
    .filter(([, items]) => items.length > 0)
    .sort(([left], [right]) => left.localeCompare(right))
)
</script>

<template>
  <div class="permission-group-board">
    <section v-for="[key, items] in orderedGroups" :key="key" class="panel permission-group-board__panel">
      <header class="permission-group-board__header">
        <div>
          <p>权限分组</p>
          <h3>{{ getSystemPermissionTypeLabel(key as SystemPermissionType) }}</h3>
        </div>
        <div class="permission-group-board__header-side">
          <SystemStatusBadge :status="key" />
          <span>{{ items.length }} 项</span>
        </div>
      </header>
      <table class="data-table permission-group-board__table">
        <thead>
          <tr>
            <th>权限名称</th>
            <th>权限编码</th>
            <th>状态</th>
            <th>说明</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>{{ item.permissionName }}</td>
            <td><code>{{ item.permissionCode }}</code></td>
            <td><SystemStatusBadge :status="item.status" /></td>
            <td>{{ item.remark || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped>
.permission-group-board {
  display: grid;
  gap: 16px;
}

.permission-group-board__panel {
  display: grid;
  gap: 14px;
}

.permission-group-board__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.permission-group-board__header-side {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.permission-group-board__header p,
.permission-group-board__header-side span {
  margin: 0;
  color: rgba(205, 222, 248, 0.78);
}

.permission-group-board__header h3 {
  margin: 6px 0 0;
  color: #eef5ff;
}

.permission-group-board__table code {
  font-size: 12px;
  color: #eef5ff;
}

@media (max-width: 720px) {
  .permission-group-board__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
