<template>
  <div class="zh-date-picker" ref="pickerRef">
    <div class="zh-date-picker__input" @click="toggle">
      <span :class="{ 'zh-date-picker__placeholder': !modelValue }">
        {{ modelValue || placeholder }}
      </span>
      <span class="zh-date-picker__icon">&#128197;</span>
    </div>
    <Teleport to="body">
      <div v-if="open" class="zh-date-picker__dropdown" :style="dropdownStyle" ref="dropdownRef">
        <div class="zh-cal__header">
          <button type="button" @click="changeMonth(-1)">&lt;</button>
          <span>{{ currentYear }}年 {{ currentMonth + 1 }}月</span>
          <button type="button" @click="changeMonth(1)">&gt;</button>
        </div>
        <div class="zh-cal__weekdays">
          <span v-for="d in weekdays" :key="d">{{ d }}</span>
        </div>
        <div class="zh-cal__days">
          <button
            v-for="(day, i) in calendarDays"
            :key="i"
            type="button"
            class="zh-cal__day"
            :class="{
              'zh-cal__day--other': day.other,
              'zh-cal__day--today': day.today,
              'zh-cal__day--selected': day.selected
            }"
            @click="selectDay(day)"
          >{{ day.num }}</button>
        </div>
        <div class="zh-cal__footer">
          <button type="button" class="zh-cal__footer-btn" @click="clearDate">清除</button>
          <button type="button" class="zh-cal__footer-btn" @click="selectToday">今天</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'

const props = defineProps<{
  modelValue?: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const open = ref(false)
const pickerRef = ref<HTMLElement>()
const dropdownRef = ref<HTMLElement>()
const dropdownStyle = ref<Record<string, string>>({})

const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth())

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

// Parse the model value to set initial view
watch(() => props.modelValue, (val) => {
  if (val) {
    const d = new Date(val)
    if (!isNaN(d.getTime())) {
      currentYear.value = d.getFullYear()
      currentMonth.value = d.getMonth()
    }
  }
}, { immediate: true })

interface CalDay {
  num: number
  year: number
  month: number
  other: boolean
  today: boolean
  selected: boolean
}

const calendarDays = computed<CalDay[]>(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const daysInPrevMonth = new Date(year, month, 0).getDate()

  const now = new Date()
  const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

  const days: CalDay[] = []

  // Previous month trailing days
  for (let i = firstDay - 1; i >= 0; i--) {
    const num = daysInPrevMonth - i
    const m = month === 0 ? 11 : month - 1
    const y = month === 0 ? year - 1 : year
    const dateStr = `${y}-${String(m + 1).padStart(2, '0')}-${String(num).padStart(2, '0')}`
    days.push({ num, year: y, month: m, other: true, today: dateStr === todayStr, selected: dateStr === props.modelValue })
  }

  // Current month
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    days.push({ num: d, year, month, other: false, today: dateStr === todayStr, selected: dateStr === props.modelValue })
  }

  // Next month leading days
  const remaining = 42 - days.length
  for (let d = 1; d <= remaining; d++) {
    const m = month === 11 ? 0 : month + 1
    const y = month === 11 ? year + 1 : year
    const dateStr = `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    days.push({ num: d, year: y, month: m, other: true, today: dateStr === todayStr, selected: dateStr === props.modelValue })
  }

  return days
})

function formatDate(year: number, month: number, day: number) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function positionDropdown() {
  if (!pickerRef.value) return
  const rect = pickerRef.value.getBoundingClientRect()
  dropdownStyle.value = {
    position: 'fixed',
    top: `${rect.bottom + 4}px`,
    left: `${rect.left}px`,
    zIndex: '9999'
  }
}

async function toggle() {
  open.value = !open.value
  if (open.value) {
    await nextTick()
    positionDropdown()
  }
}

function selectDay(day: CalDay) {
  emit('update:modelValue', formatDate(day.year, day.month, day.num))
  open.value = false
}

function selectToday() {
  const now = new Date()
  emit('update:modelValue', formatDate(now.getFullYear(), now.getMonth(), now.getDate()))
  open.value = false
}

function clearDate() {
  emit('update:modelValue', '')
  open.value = false
}

function changeMonth(delta: number) {
  let m = currentMonth.value + delta
  let y = currentYear.value
  if (m < 0) { m = 11; y-- }
  if (m > 11) { m = 0; y++ }
  currentMonth.value = m
  currentYear.value = y
}

function onClickOutside(e: MouseEvent) {
  if (!open.value) return
  const target = e.target as Node
  if (pickerRef.value?.contains(target)) return
  if (dropdownRef.value?.contains(target)) return
  open.value = false
}

onMounted(() => document.addEventListener('mousedown', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('mousedown', onClickOutside))
</script>

<style scoped>
.zh-date-picker {
  position: relative;
  min-width: 0;
  width: 100%;
}

.zh-date-picker__input {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(75, 119, 159, 0.9);
  border-radius: 3px;
  background: #1a344b;
  color: #eaf5ff;
  font-size: 14px;
  cursor: pointer;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.zh-date-picker__input:hover {
  border-color: rgba(64, 158, 255, 0.6);
}

.zh-date-picker__placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.zh-date-picker__icon {
  font-size: 14px;
  opacity: 0.5;
}

.zh-date-picker__dropdown {
  width: 280px;
  padding: 12px;
  background: #132a45;
  border: 1px solid rgba(103, 187, 246, 0.25);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  color: #eaf5ff;
}

.zh-cal__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
}

.zh-cal__header button {
  width: 28px;
  height: 28px;
  border: 1px solid rgba(125, 163, 220, 0.18);
  border-radius: 6px;
  background: rgba(10, 26, 45, 0.72);
  color: #eaf5ff;
  cursor: pointer;
  font-size: 13px;
}

.zh-cal__header button:hover {
  border-color: #409eff;
}

.zh-cal__weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 12px;
  color: rgba(205, 222, 248, 0.6);
  margin-bottom: 4px;
}

.zh-cal__days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.zh-cal__day {
  width: 100%;
  aspect-ratio: 1;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #eaf5ff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.1s;
}

.zh-cal__day:hover {
  background: rgba(64, 158, 255, 0.2);
}

.zh-cal__day--other {
  color: rgba(205, 222, 248, 0.3);
}

.zh-cal__day--today {
  border: 1px solid #409eff;
}

.zh-cal__day--selected {
  background: #409eff;
  color: #fff;
  font-weight: 600;
}

.zh-cal__footer {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(125, 163, 220, 0.12);
}

.zh-cal__footer-btn {
  border: none;
  background: transparent;
  color: #5ea2ff;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
}

.zh-cal__footer-btn:hover {
  color: #fff;
}
</style>
