<script setup lang="ts">
import { computed, ref, watch } from 'vue'

interface Person {
  id: number | string
  name?: string
  gender?: string
  phone?: string
  householdType?: string
  address?: string
  gridName?: string
  relation?: string
  specialPopulation?: number
  specialPopulationType?: string
}

// 后端返回树：{ id, label, isHouse, address, head|null, children:[{ id, person, isHead }] }
const props = defineProps<{ households: any[] }>()
const emit = defineEmits<{
  (e: 'edit', p: Person): void
  (e: 'delete', p: Person): void
}>()

// 户头固定蓝；成员按性别蓝/红
const OWNER_COLOR = '#0284c7'
const MALE_COLOR = '#2e6fb0'
const FEMALE_COLOR = '#c2547a'
const genderColor = (p?: Person | null) => (p?.gender === '女' ? FEMALE_COLOR : MALE_COLOR)

const membersOf = (h: any): Person[] =>
  (h.children || []).map((c: any) => c.person || c)

// 户籍类型展示翻译
const HH_TYPE: Record<string, string> = {
  LOCAL: '本地户籍', NON_LOCAL: '外地户籍',
  FLOATING: '流动', MIGRANT: '流动', LOW_INCOME: '低收入',
}
const hhTypeLabel = (t?: string) => (t ? HH_TYPE[t] || t : '-')
const hhTypeClass = (t?: string) => (t === 'LOCAL' ? 'pop-tag-amber' : 'pop-tag-green')

// 直接消费后端树，仅做必要兜底
const view = computed(() =>
  props.households.map((h, i) => {
    const head = h.head || null
    const members = membersOf(h)
    return {
      id: h.id ?? 'house-' + i,
      name: head?.name || members[0]?.name || h.label || '未命名',
      gridName: head?.gridName || members[0]?.gridName || '-',
      address: head?.address || h.address || members[0]?.address || '-',
      count: members.length,
      hasOwner: !!head || members.some((m: Person) => m.relation === '户主'),
      headColor: OWNER_COLOR,
      members,
    }
  })
)

const expanded = ref<Set<string | number>>(new Set())

// 前端分页：每页 20 户
const PAGE_SIZE = 20
const page = ref(1)
const total = computed(() => view.value.length)
const paged = computed(() => view.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))
// 切换页时自动展开当前页所有户
watch(paged, list => {
  for (const h of list) expanded.value.add(h.id)
  expanded.value = new Set(expanded.value)
}, { immediate: true })
// 增删后当前页越界则回第 1 页
watch(total, () => {
  if (total.value > 0 && (page.value - 1) * PAGE_SIZE >= total.value) page.value = 1
})

function toggle(id: string | number) {
  const s = expanded.value
  if (s.has(id)) s.delete(id)
  else s.add(id)
  expanded.value = new Set(s)
}
</script>

<template>
  <div class="hh-list">
    <section v-for="hh in paged" :key="hh.id" class="household" :class="{ expanded: expanded.has(hh.id) }">
      <div class="hh-head" @click="toggle(hh.id)">
        <div class="avatar" :style="{ background: hh.headColor }">{{ hh.name.charAt(0) }}</div>
        <div class="hh-main">
          <div class="hh-name-row">
            <span class="hh-name">{{ hh.name }}</span>
            <span v-if="hh.hasOwner" class="tag tag-owner">户主</span>
            <span v-if="hh.gridName && hh.gridName !== '-'" class="tag tag-grid">{{ hh.gridName }}</span>
          </div>
          <div class="hh-meta">
            <span>住址 <b>{{ hh.address }}</b></span>
            <span class="dot">·</span>
            <span>在册 <b>{{ hh.count }}</b> 人</span>
          </div>
        </div>
        <button class="hh-toggle" :aria-expanded="expanded.has(hh.id)" :aria-controls="hh.id + '-detail'"
                :aria-label="expanded.has(hh.id) ? '收起' : '展开'" @click.stop="toggle(hh.id)">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 6l5 5 5-5"></path></svg>
        </button>
      </div>

      <div v-show="expanded.has(hh.id)" :id="hh.id + '-detail'" class="members">
        <table class="hh-table">
          <thead>
            <tr>
              <th class="col-name">姓名</th>
              <th>性别</th>
              <th>电话</th>
              <th>户籍类型</th>
              <th>特殊人群</th>
              <th>网格</th>
              <th>备注</th>
              <th class="col-act">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in hh.members" :key="m.id">
              <td>
                <div class="m-name">
                  <span class="m-avatar" :style="{ background: genderColor(m) }">{{ (m.name || '-').charAt(0) }}</span>
                  <span class="who">{{ m.name || '-' }}</span>
                  <span v-if="m.relation === '户主'" class="rel-chip owner">户主</span>
                  <span v-else-if="m.relation" class="rel-chip">{{ m.relation }}</span>
                </div>
              </td>
              <td>{{ m.gender || '-' }}</td>
              <td class="masked">{{ m.phone || '-' }}</td>
              <td>
                <span v-if="m.householdType" class="pop-tag" :class="hhTypeClass(m.householdType)">{{ hhTypeLabel(m.householdType) }}</span>
                <span v-else class="muted">-</span>
              </td>
              <td>
                <span v-if="m.specialPopulation === 1 && m.specialPopulationType" class="pop-tag pop-tag-special">{{ m.specialPopulationType }}</span>
                <span v-else class="muted">-</span>
              </td>
              <td class="muted">{{ m.gridName || '-' }}</td>
              <td class="muted">{{ m.remark || '-' }}</td>
              <td class="col-act">
                <el-button size="small" link type="primary" @click.stop="emit('edit', m)">编辑</el-button>
                <el-button size="small" link type="danger" @click.stop="emit('delete', m)">删除</el-button>
              </td>
            </tr>
            <tr v-if="!hh.members.length">
              <td colspan="7" class="empty">该户暂无成员</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <p v-if="!households.length" class="pop-empty">暂无数据</p>

    <el-pagination
      v-if="total > PAGE_SIZE"
      class="hh-pager"
      layout="prev, pager, next, total"
      :total="total"
      :page-size="PAGE_SIZE"
      :current-page="page"
      @current-change="page = $event"
      background
    />
  </div>
</template>

<style scoped>
.hh-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.household {
  background: #fff;
  border: 0.5px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  overflow: hidden;
  transition: box-shadow 0.15s ease;
}
.household:hover {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}
.hh-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  cursor: pointer;
  user-select: none;
}
.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  font-size: 18px;
  flex-shrink: 0;
}
.hh-main {
  flex: 1;
  min-width: 0;
}
.hh-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.hh-name {
  font-size: 15px;
  font-weight: 500;
  color: #26221d;
}
.tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.5;
}
.tag-owner {
  background: #fde8e8;
  color: #a32d2d;
  border: 0.5px solid #f09595;
}
.tag-grid {
  background: #f1efe8;
  color: #854f0b;
}
.hh-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
  flex-wrap: wrap;
}
.hh-meta b {
  color: #444441;
  font-weight: 500;
}
.dot {
  color: #c4c1b8;
}
.hh-toggle {
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  padding: 6px;
  display: flex;
  transition: transform 0.18s ease;
}
.household.expanded .hh-toggle {
  transform: rotate(180deg);
}
.members {
  border-top: 0.5px solid rgba(0, 0, 0, 0.06);
  padding: 4px 18px 12px;
}
.hh-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
  font-size: 13px;
}
.hh-table th,
.hh-table td {
  width: calc(100% / 7);
  padding: 10px 8px;
}
.hh-table th {
  text-align: left;
  font-weight: 500;
  color: #5f5e5a;
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.06);
  font-size: 12px;
  white-space: nowrap;
}
.hh-table td {
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.04);
  color: #26221d;
  vertical-align: middle;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hh-table tr:last-child td {
  border-bottom: none;
}
.col-name { width: calc(100% / 7); }
.col-act { width: calc(100% / 7); }
.m-name {
  display: flex;
  align-items: center;
  gap: 10px;
}
.m-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  font-size: 13px;
  flex-shrink: 0;
}
.who {
  font-weight: 500;
}
.rel-chip {
  display: inline-block;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 11px;
  background: #f1efe8;
  color: #854f0b;
}
.rel-chip.owner {
  background: #fde8e8;
  color: #a32d2d;
  border: 0.5px solid #f09595;
}
.masked {
  color: #6b7280;
}
.muted {
  color: #6b7280;
}
.ellipsis {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pop-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.4;
}
.pop-tag-amber { background: #f1efe8; color: #854f0b; }
.pop-tag-green { background: #e1f5ee; color: #0f6e56; }
.pop-tag-special { background: #fdeede; color: #b4540a; border: 0.5px solid #f0b27a; }
.col-act :deep(.el-button) {
  padding: 0 6px;
}
.empty, .pop-empty {
  text-align: center;
  padding: 28px;
  color: #9ca3af;
}
.hh-pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
