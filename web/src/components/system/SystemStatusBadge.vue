<script setup lang="ts">
const props = defineProps<{
  status: string
}>()

const statusToneMap: Record<string, string> = {
  ENABLED: 'success',
  ACTIVE: 'success',
  DISABLED: 'muted',
  LOCKED: 'danger',
  CATALOG: 'primary',
  MENU: 'info',
  BUTTON: 'warning',
  MENU_GROUP: 'primary',
  ACTION: 'warning',
  CONFIG: 'info',
  MODEL: 'success',
  API: 'info'
}

const statusLabelMap: Record<string, string> = {
  ENABLED: '启用',
  ACTIVE: '启用',
  DISABLED: '停用',
  LOCKED: '锁定',
  CATALOG: '目录',
  MENU: '菜单',
  BUTTON: '按钮',
  ACTION: '操作权限',
  CONFIG: '配置权限',
  MODEL: '模型权限',
  MENU_GROUP: '菜单权限',
  API: '接口权限'
}
</script>

<template>
  <span class="system-status-badge" :class="`system-status-badge--${statusToneMap[props.status] ?? 'info'}`">
    {{ statusLabelMap[props.status] ?? props.status }}
  </span>
</template>

<style scoped>
.system-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  line-height: 1;
}

.system-status-badge--success {
  color: #9af4c8;
  background: rgba(49, 186, 120, 0.16);
  border-color: rgba(49, 186, 120, 0.26);
}

.system-status-badge--muted {
  color: #c7d7ed;
  background: rgba(145, 163, 184, 0.14);
  border-color: rgba(145, 163, 184, 0.22);
}

.system-status-badge--danger {
  color: #ffb0b0;
  background: rgba(211, 75, 75, 0.16);
  border-color: rgba(211, 75, 75, 0.28);
}

.system-status-badge--primary {
  color: #88dcff;
  background: rgba(47, 143, 255, 0.16);
  border-color: rgba(47, 143, 255, 0.28);
}

.system-status-badge--info {
  color: #c7d7ed;
  background: rgba(94, 162, 255, 0.14);
  border-color: rgba(94, 162, 255, 0.24);
}

.system-status-badge--warning {
  color: #ffd28c;
  background: rgba(229, 160, 33, 0.16);
  border-color: rgba(229, 160, 33, 0.28);
}
</style>
