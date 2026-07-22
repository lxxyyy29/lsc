<template>
  <div class="subject-selector">
    <div class="subject-selector__tabs">
      <button
        type="button"
        class="subject-selector__tab"
        :class="{ 'subject-selector__tab--active': activeTab === 'MERCHANT' }"
        @click="switchTab('MERCHANT')"
      >
        商户
      </button>
      <button
        type="button"
        class="subject-selector__tab"
        :class="{ 'subject-selector__tab--active': activeTab === 'VENDOR' }"
        @click="switchTab('VENDOR')"
      >
        摊贩
      </button>
      <button
        v-if="subjectType"
        type="button"
        class="subject-selector__clear"
        @click="clearSelection"
      >
        清除
      </button>
    </div>

    <div v-if="selectedLabel" class="subject-selector__selected">
      已选择：<strong>{{ selectedLabel }}</strong>
    </div>

    <div class="subject-selector__search">
      <input
        v-model="keyword"
        type="text"
        class="subject-selector__input"
        :placeholder="activeTab === 'MERCHANT' ? '搜索商户名称...' : '搜索摊贩名称...'"
        aria-label="搜索关键词"
      />
    </div>

    <div class="subject-selector__list">
      <p v-if="loading" class="subject-selector__hint">加载中...</p>
      <p v-else-if="filteredItems.length === 0" class="subject-selector__hint">暂无数据</p>
      <label
        v-for="item in filteredItems"
        :key="item.id"
        class="subject-selector__item"
        :class="{ 'subject-selector__item--selected': item.id === subjectId && activeTab === subjectType }"
      >
        <input
          type="radio"
          :name="`subject-${activeTab}`"
          :value="item.id"
          :checked="item.id === subjectId && activeTab === subjectType"
          class="subject-selector__radio"
          @change="selectItem(item)"
        />
        <span class="subject-selector__item-name">{{ item.name }}</span>
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { listBizMerchants } from '../../api/biz-merchant'
import { listBizVendors } from '../../api/biz-vendor'

interface SubjectItem {
  id: number
  name: string
}

const props = defineProps<{
  subjectType?: 'MERCHANT' | 'VENDOR' | null
  subjectId?: number | null
  subjectName?: string | null
}>()

const emit = defineEmits<{
  'update:subjectType': ['MERCHANT' | 'VENDOR' | null]
  'update:subjectId': [number | null]
  'update:subjectName': [string | null]
}>()

const activeTab = ref<'MERCHANT' | 'VENDOR'>('MERCHANT')
const keyword = ref('')
const loading = ref(false)
const merchants = ref<SubjectItem[]>([])
const vendors = ref<SubjectItem[]>([])

const allItems = computed<SubjectItem[]>(() =>
  activeTab.value === 'MERCHANT' ? merchants.value : vendors.value
)

const filteredItems = computed<SubjectItem[]>(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return allItems.value
  return allItems.value.filter((item) => item.name.toLowerCase().includes(kw))
})

const selectedLabel = computed(() => {
  if (!props.subjectType || !props.subjectId) return null
  const typeLabel = props.subjectType === 'MERCHANT' ? '商户' : '摊贩'
  return props.subjectName ? `${typeLabel}：${props.subjectName}` : `${typeLabel} #${props.subjectId}`
})

async function loadMerchants() {
  if (merchants.value.length > 0) return
  loading.value = true
  try {
    const data = await listBizMerchants({ status: 'ACTIVE' })
    merchants.value = data.map((m) => ({ id: m.id, name: m.merchantName }))
  } catch {
    merchants.value = []
  } finally {
    loading.value = false
  }
}

async function loadVendors() {
  if (vendors.value.length > 0) return
  loading.value = true
  try {
    const data = await listBizVendors({ status: 'ACTIVE' })
    vendors.value = data.map((v) => ({ id: v.id, name: v.vendorName }))
  } catch {
    vendors.value = []
  } finally {
    loading.value = false
  }
}

async function switchTab(tab: 'MERCHANT' | 'VENDOR') {
  activeTab.value = tab
  keyword.value = ''
  if (tab === 'MERCHANT') {
    await loadMerchants()
  } else {
    await loadVendors()
  }
}

function selectItem(item: SubjectItem) {
  emit('update:subjectType', activeTab.value)
  emit('update:subjectId', item.id)
  emit('update:subjectName', item.name)
}

function clearSelection() {
  emit('update:subjectType', null)
  emit('update:subjectId', null)
  emit('update:subjectName', null)
}

// If current selection is on VENDOR tab, switch to that tab initially
watch(
  () => props.subjectType,
  (type) => {
    if (type === 'VENDOR') {
      activeTab.value = 'VENDOR'
    } else {
      activeTab.value = 'MERCHANT'
    }
  },
  { immediate: true }
)

onMounted(() => {
  void loadMerchants()
})
</script>

<style scoped>
.subject-selector {
  display: grid;
  gap: 10px;
}

.subject-selector__tabs {
  display: flex;
  gap: 8px;
  align-items: center;
}

.subject-selector__tab {
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid rgba(75, 119, 159, 0.6);
  background: #1a344b;
  color: #8db0d0;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  transition: background 0.15s, color 0.15s;
}

.subject-selector__tab--active {
  background: rgba(35, 160, 250, 0.18);
  color: #eaf5ff;
  border-color: rgba(35, 160, 250, 0.5);
}

.subject-selector__clear {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid rgba(200, 100, 80, 0.4);
  background: rgba(200, 100, 80, 0.1);
  color: #f0a0a0;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  margin-left: auto;
}

.subject-selector__selected {
  font-size: 13px;
  color: #67c23a;
  padding: 4px 0;
}

.subject-selector__input {
  width: 100%;
  box-sizing: border-box;
  padding: 7px 12px;
  border: 1px solid rgba(75, 119, 159, 0.9);
  border-radius: 4px;
  background: #1a344b;
  color: #eaf5ff;
  font: inherit;
  font-size: 13px;
}

.subject-selector__input::placeholder {
  color: #5a7a99;
}

.subject-selector__list {
  max-height: 200px;
  overflow-y: auto;
  display: grid;
  gap: 4px;
  border: 1px solid rgba(75, 119, 159, 0.3);
  border-radius: 6px;
  padding: 6px;
  background: rgba(15, 30, 50, 0.5);
}

.subject-selector__hint {
  margin: 0;
  padding: 8px;
  color: #8db0d0;
  font-size: 13px;
  text-align: center;
}

.subject-selector__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #d0dfed;
  transition: background 0.12s;
}

.subject-selector__item:hover {
  background: rgba(35, 160, 250, 0.08);
}

.subject-selector__item--selected {
  background: rgba(35, 160, 250, 0.15);
  color: #eaf5ff;
}

.subject-selector__radio {
  flex-shrink: 0;
  accent-color: #23a0fa;
}

.subject-selector__item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
