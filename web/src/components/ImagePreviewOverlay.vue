<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="img-preview-overlay"
      @click.self="close"
      @keydown.esc="close"
      tabindex="-1"
    >
      <button type="button" class="img-preview-close" @click="close">&times;</button>

      <!-- Zoom / pan container -->
      <div
        class="img-preview-stage"
        @wheel.prevent="onWheel"
        @mousedown="onMouseDown"
        @mousemove="onMouseMove"
        @mouseup="onMouseUp"
        @mouseleave="onMouseUp"
        @touchstart.prevent="onTouchStart"
        @touchmove.prevent="onTouchMove"
        @touchend="onTouchEnd"
      >
        <img
          v-if="currentSrc"
          :src="currentSrc"
          :alt="`图片 ${internalIndex + 1}`"
          class="img-preview-img"
          :style="imgStyle"
          @load="onImgLoad"
          @error="onImgError"
          draggable="false"
        >
        <span v-if="imgError" class="img-preview-error">图片加载失败</span>
      </div>

      <!-- Controls bar -->
      <div class="img-preview-bar">
        <!-- Nav: prev -->
        <button
          v-if="images.length > 1"
          type="button"
          class="img-preview-nav-btn"
          :disabled="internalIndex === 0"
          @click.stop="go(-1)"
        >&#8249; 上一张</button>

        <!-- Counter -->
        <span v-if="images.length > 1" class="img-preview-counter">
          {{ internalIndex + 1 }} / {{ images.length }}
        </span>

        <!-- Zoom controls -->
        <div class="img-preview-zoom-group">
          <button type="button" class="img-preview-zoom-btn" @click.stop="zoomOut" title="缩小">－</button>
          <span class="img-preview-zoom-label">{{ Math.round(scale * 100) }}%</span>
          <button type="button" class="img-preview-zoom-btn" @click.stop="zoomIn" title="放大">＋</button>
          <button type="button" class="img-preview-zoom-btn img-preview-zoom-reset" @click.stop="resetTransform" title="还原">⊙</button>
        </div>

        <!-- Nav: next -->
        <button
          v-if="images.length > 1"
          type="button"
          class="img-preview-nav-btn"
          :disabled="internalIndex === images.length - 1"
          @click.stop="go(1)"
        >下一张 &#8250;</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: boolean
  images: string[]
  index?: number
}>(), {
  index: 0
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'update:index', val: number): void
}>()

// Internal index so we can navigate without requiring parent to wire index
const internalIndex = ref(props.index)

watch(() => props.index, (v) => { internalIndex.value = v })
watch(() => props.modelValue, (open) => {
  if (open) {
    internalIndex.value = props.index
    resetTransform()
    imgError.value = false
  }
})
watch(internalIndex, () => {
  resetTransform()
  imgError.value = false
})

const currentSrc = computed(() => props.images[internalIndex.value] ?? '')

function go(delta: number) {
  const next = internalIndex.value + delta
  if (next < 0 || next >= props.images.length) return
  internalIndex.value = next
  emit('update:index', next)
}

function close() {
  emit('update:modelValue', false)
}

// ── Transform state ──────────────────────────────────────────────────────────
const scale = ref(1)
const tx = ref(0)
const ty = ref(0)

const MIN_SCALE = 0.2
const MAX_SCALE = 8
const ZOOM_STEP = 0.25

const imgStyle = computed(() => ({
  transform: `translate(${tx.value}px, ${ty.value}px) scale(${scale.value})`,
  cursor: scale.value > 1 ? 'grab' : 'default',
  transition: isDragging.value ? 'none' : 'transform 0.15s ease'
}))

function resetTransform() {
  scale.value = 1
  tx.value = 0
  ty.value = 0
}

function zoomIn() {
  scale.value = Math.min(MAX_SCALE, scale.value + ZOOM_STEP)
}

function zoomOut() {
  scale.value = Math.max(MIN_SCALE, scale.value - ZOOM_STEP)
  // Re-centre if zoomed back to fit
  if (scale.value <= 1) { tx.value = 0; ty.value = 0 }
}

function onImgLoad() { imgError.value = false }
function onImgError() { imgError.value = true }
const imgError = ref(false)

// ── Mouse wheel zoom ─────────────────────────────────────────────────────────
function onWheel(e: WheelEvent) {
  const delta = e.deltaY < 0 ? ZOOM_STEP : -ZOOM_STEP
  scale.value = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale.value + delta))
  if (scale.value <= 1) { tx.value = 0; ty.value = 0 }
}

// ── Mouse drag pan ───────────────────────────────────────────────────────────
const isDragging = ref(false)
let dragStartX = 0
let dragStartY = 0
let dragOriginTx = 0
let dragOriginTy = 0

function onMouseDown(e: MouseEvent) {
  if (scale.value <= 1) return
  isDragging.value = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragOriginTx = tx.value
  dragOriginTy = ty.value
}

function onMouseMove(e: MouseEvent) {
  if (!isDragging.value) return
  tx.value = dragOriginTx + (e.clientX - dragStartX)
  ty.value = dragOriginTy + (e.clientY - dragStartY)
}

function onMouseUp() {
  isDragging.value = false
}

// ── Touch pinch-zoom + pan ───────────────────────────────────────────────────
let lastTouchDist = 0
let lastTouchMidX = 0
let lastTouchMidY = 0
let touchOriginTx = 0
let touchOriginTy = 0
let touchOriginScale = 1

function getTouchDist(touches: TouchList) {
  const dx = touches[0].clientX - touches[1].clientX
  const dy = touches[0].clientY - touches[1].clientY
  return Math.hypot(dx, dy)
}

function onTouchStart(e: TouchEvent) {
  if (e.touches.length === 2) {
    lastTouchDist = getTouchDist(e.touches)
    lastTouchMidX = (e.touches[0].clientX + e.touches[1].clientX) / 2
    lastTouchMidY = (e.touches[0].clientY + e.touches[1].clientY) / 2
    touchOriginTx = tx.value
    touchOriginTy = ty.value
    touchOriginScale = scale.value
  } else if (e.touches.length === 1 && scale.value > 1) {
    isDragging.value = true
    dragStartX = e.touches[0].clientX
    dragStartY = e.touches[0].clientY
    dragOriginTx = tx.value
    dragOriginTy = ty.value
  }
}

function onTouchMove(e: TouchEvent) {
  if (e.touches.length === 2) {
    const dist = getTouchDist(e.touches)
    const ratio = dist / lastTouchDist
    scale.value = Math.min(MAX_SCALE, Math.max(MIN_SCALE, touchOriginScale * ratio))
    if (scale.value <= 1) { tx.value = 0; ty.value = 0 }
  } else if (e.touches.length === 1 && isDragging.value) {
    tx.value = dragOriginTx + (e.touches[0].clientX - dragStartX)
    ty.value = dragOriginTy + (e.touches[0].clientY - dragStartY)
  }
}

function onTouchEnd() {
  isDragging.value = false
  if (scale.value <= 1) { tx.value = 0; ty.value = 0 }
}
</script>

<style scoped>
.img-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.88);
  outline: none;
}

.img-preview-close {
  position: absolute;
  top: 16px;
  right: 24px;
  background: none;
  border: none;
  color: #fff;
  font-size: 36px;
  cursor: pointer;
  line-height: 1;
  opacity: 0.7;
  transition: opacity 0.2s;
  z-index: 1;
}

.img-preview-close:hover {
  opacity: 1;
}

/* Stage fills the available space between header close btn and bottom bar */
.img-preview-stage {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  user-select: none;
}

.img-preview-img {
  max-width: 90vw;
  max-height: 75vh;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  will-change: transform;
}

.img-preview-error {
  color: #ff9d9d;
  font-size: 14px;
}

/* Bottom control bar */
.img-preview-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 20px 20px;
  flex-wrap: wrap;
}

.img-preview-nav-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #eaf5ff;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.2s;
}

.img-preview-nav-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
}

.img-preview-nav-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.img-preview-counter {
  color: #d0dfed;
  font-size: 14px;
  min-width: 60px;
  text-align: center;
}

.img-preview-zoom-group {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(103, 187, 246, 0.18);
  border-radius: 6px;
  padding: 4px 8px;
}

.img-preview-zoom-btn {
  background: none;
  border: none;
  color: #eaf5ff;
  font-size: 16px;
  cursor: pointer;
  line-height: 1;
  padding: 2px 6px;
  border-radius: 3px;
  transition: background 0.15s;
}

.img-preview-zoom-btn:hover {
  background: rgba(103, 187, 246, 0.18);
}

.img-preview-zoom-reset {
  font-size: 14px;
  opacity: 0.8;
}

.img-preview-zoom-label {
  color: #8dc5ff;
  font-size: 12px;
  min-width: 40px;
  text-align: center;
}
</style>
