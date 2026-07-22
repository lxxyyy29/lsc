<script setup lang="ts">
import { computed } from 'vue'
import type { SystemMenu } from '../../api/system-menu'
import SystemStatusBadge from './SystemStatusBadge.vue'

const props = defineProps<{
  items: SystemMenu[]
}>()

const emit = defineEmits<{
  createChild: [item: SystemMenu]
  edit: [item: SystemMenu]
  toggleStatus: [item: SystemMenu]
  remove: [item: SystemMenu]
}>()

interface FlatSystemMenu extends SystemMenu {
  _level: number
}

function flatten(items: SystemMenu[], level = 0): FlatSystemMenu[] {
  return items.flatMap((item) => [{ ...item, _level: level }, ...flatten(item.children ?? [], level + 1)])
}

const rows = computed(() => flatten(props.items))
</script>

<template>
  <div class="system-tree-table">
    <table class="data-table">
      <thead>
        <tr>
          <th>菜单名称</th>
          <th>权限标识</th>
          <th>路由路径</th>
          <th>组件路径</th>
          <th>类型</th>
          <th>终端</th>
          <th>排序</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.id">
          <td>
            <div class="system-tree-table__name" :style="{ paddingLeft: `${item._level * 20}px` }">
              <span class="system-tree-table__indent" :class="{ 'system-tree-table__indent--child': item._level > 0 }"></span>
              <div class="system-tree-table__name-text">
                <strong>{{ item.permissionName }}</strong>
                <span v-if="item.remark" class="system-tree-table__remark">{{ item.remark }}</span>
              </div>
            </div>
          </td>
          <td>{{ item.permissionCode }}</td>
          <td>{{ item.path || '-' }}</td>
          <td>{{ item.component || '-' }}</td>
          <td><SystemStatusBadge :status="item.permissionType" /></td>
          <td>{{ item.clientType }}</td>
          <td>{{ item.sortOrder }}</td>
          <td>{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</td>
          <td>
            <div class="table-actions">
              <button type="button" class="action-link" @click="emit('createChild', item)">新增下级</button>
              <button type="button" class="action-link" @click="emit('edit', item)">编辑</button>
              <button type="button" class="action-link" @click="emit('toggleStatus', item)">
                {{ item.status === 'ACTIVE' ? '停用' : '启用' }}
              </button>
              <button type="button" class="action-link action-link--danger" @click="emit('remove', item)">删除</button>
            </div>
          </td>
        </tr>
        <tr v-if="rows.length === 0">
          <td colspan="9" class="system-tree-table__empty">暂无菜单数据</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
@import '../../views/admin-shared.css';

.system-tree-table__name {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.system-tree-table__name-text {
  display: grid;
  gap: 4px;
}

.system-tree-table__remark {
  color: rgba(205, 222, 248, 0.78);
  font-size: 12px;
}

.system-tree-table__indent {
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 999px;
  background: rgba(115, 235, 255, 0.54);
  box-shadow: 0 0 10px rgba(115, 235, 255, 0.3);
}

.system-tree-table__indent--child {
  width: 22px;
  height: 1px;
  margin-top: 10px;
  border-radius: 0;
  background: rgba(115, 235, 255, 0.38);
  box-shadow: none;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.system-tree-table__empty {
  text-align: center;
  color: rgba(205, 222, 248, 0.78);
}
</style>
