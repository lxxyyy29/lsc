<template>
  <image class="app-icon" :src="iconSrc" :style="iconStyle" mode="aspectFit" />
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    name: string
    size?: string | number
    color?: string
  }>(),
  {
    size: 24,
    color: 'currentColor'
  }
)

const ICONS: Record<string, string> = {
  menu: '<path d="M4 7H20" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M4 12H20" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M4 17H20" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  notifications: '<path d="M8 18H16" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M9 18C9 19.657 10.343 21 12 21C13.657 21 15 19.657 15 18" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M6 16.5H18L16.8 14.9C16.289 14.218 16 13.389 16 12.537V10.5C16 8.291 14.209 6.5 12 6.5C9.791 6.5 8 8.291 8 10.5V12.537C8 13.389 7.711 14.218 7.2 14.9L6 16.5Z" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/><circle cx="17.5" cy="6.5" r="1.5" fill="COLOR"/>',
  search: '<circle cx="11" cy="11" r="5.5" stroke="COLOR" stroke-width="1.8"/><path d="M15.2 15.2L19 19" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  location: '<path d="M12 20C15.5 16.6 18 13.8 18 10.5C18 7.462 15.538 5 12.5 5H11.5C8.462 5 6 7.462 6 10.5C6 13.8 8.5 16.6 12 20Z" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/><circle cx="12" cy="10.5" r="2.2" stroke="COLOR" stroke-width="1.8"/>',
  verified: '<path d="M12 4L18 6.7V11.3C18 15.2 15.4 18.8 12 20C8.6 18.8 6 15.2 6 11.3V6.7L12 4Z" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/><path d="M9.2 12.2L11.1 14.1L15 10.2" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  todo: '<rect x="6" y="5.5" width="12" height="14" rx="2" stroke="COLOR" stroke-width="1.8"/><path d="M9 9H15" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M9 12.5H15" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M9 16H12" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><circle cx="17" cy="7" r="3" fill="COLOR" opacity="0.22"/><path d="M16 7L16.8 7.8L18.5 6.1" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  verify: '<rect x="5" y="5" width="14" height="14" rx="2" stroke="COLOR" stroke-width="1.8"/><path d="M9 12L11 14L15.5 9.5" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M19 8L20.5 9.5L23 7" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  history: '<path d="M6 7V3.5L2.8 6.7L6 10" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M6 6.8C7.4 5.1 9.6 4 12 4C16.418 4 20 7.582 20 12C20 16.418 16.418 20 12 20C7.582 20 4 16.418 4 12" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M12 8V12L15 14" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  user: '<circle cx="12" cy="8.5" r="3.2" stroke="COLOR" stroke-width="1.8"/><path d="M6.5 18C7.4 15.7 9.5 14.5 12 14.5C14.5 14.5 16.6 15.7 17.5 18" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  lock: '<rect x="6.5" y="10.5" width="11" height="8.5" rx="2" stroke="COLOR" stroke-width="1.8"/><path d="M9 10.5V8.5C9 6.843 10.343 5.5 12 5.5C13.657 5.5 15 6.843 15 8.5V10.5" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><circle cx="12" cy="14.5" r="1.2" fill="COLOR"/>',
  eye: '<path d="M3.5 12C5.3 8.9 8.3 7 12 7C15.7 7 18.7 8.9 20.5 12C18.7 15.1 15.7 17 12 17C8.3 17 5.3 15.1 3.5 12Z" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/><circle cx="12" cy="12" r="2.2" stroke="COLOR" stroke-width="1.8"/>',
  'eye-off': '<path d="M4 4L20 20" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M9.7 9.9C9.26 10.43 9 11.11 9 11.85C9 13.51 10.34 14.85 12 14.85C12.74 14.85 13.42 14.59 13.95 14.15" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M6.2 7.1C5.11 7.99 4.2 9.14 3.5 10.5C5.3 13.6 8.3 15.5 12 15.5C13.52 15.5 14.92 15.18 16.15 14.59" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M8.54 5.86C9.61 5.3 10.77 5 12 5C15.7 5 18.7 6.9 20.5 10C20 10.87 19.42 11.66 18.76 12.36" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  security: '<path d="M12 4L18 6.7V11.3C18 15.2 15.4 18.8 12 20C8.6 18.8 6 15.2 6 11.3V6.7L12 4Z" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/><path d="M12 8V16" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M9 11.5H15" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  gavel: '<path d="M10 7L17 14" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M8 9L13 4L20 11L15 16Z" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/><path d="M4 20H14" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M8 14L4.5 17.5" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  info: '<circle cx="12" cy="12" r="8" stroke="COLOR" stroke-width="1.8"/><path d="M12 10V15" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><circle cx="12" cy="7.2" r="1" fill="COLOR"/>',
  settings: '<circle cx="12" cy="12" r="2.7" stroke="COLOR" stroke-width="1.8"/><path d="M12 4.5V6.5" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M12 17.5V19.5" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M4.5 12H6.5" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M17.5 12H19.5" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M6.7 6.7L8.1 8.1" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M15.9 15.9L17.3 17.3" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M17.3 6.7L15.9 8.1" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M8.1 15.9L6.7 17.3" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  locator: '<path d="M12 20C9.791 20 8 18.209 8 16C8 13.791 9.791 12 12 12C14.209 12 16 13.791 16 16C16 18.209 14.209 20 12 20Z" stroke="COLOR" stroke-width="1.8"/><path d="M12 4V7" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M12 21V20" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M4 12H7" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M17 12H20" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  folder: '<path d="M4.5 7.5C4.5 6.948 4.948 6.5 5.5 6.5H9.3L10.9 8H18.5C19.052 8 19.5 8.448 19.5 9V16.5C19.5 17.052 19.052 17.5 18.5 17.5H5.5C4.948 17.5 4.5 17.052 4.5 16.5V7.5Z" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/>',
  briefcase: '<rect x="4.5" y="7.5" width="15" height="10" rx="2" stroke="COLOR" stroke-width="1.8"/><path d="M9 7.5V6.5C9 5.948 9.448 5.5 10 5.5H14C14.552 5.5 15 5.948 15 6.5V7.5" stroke="COLOR" stroke-width="1.8"/><path d="M4.5 11.5H19.5" stroke="COLOR" stroke-width="1.8"/>',
  'upload-records': '<path d="M12 15.5V6.5" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M8.5 10L12 6.5L15.5 10" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M6 17.5H18" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  'recent-update': '<path d="M6 6V18" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M12 10V18" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M18 13V18" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M6 14L12 8L18 11" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  activity: '<path d="M4 13H8L10.5 8L13.5 16L16 11H20" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  plus: '<path d="M12 5V19" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M5 12H19" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  archive: '<rect x="5" y="6.5" width="14" height="4" rx="1" stroke="COLOR" stroke-width="1.8"/><path d="M6.5 10.5V16.5C6.5 17.052 6.948 17.5 7.5 17.5H16.5C17.052 17.5 17.5 17.052 17.5 16.5V10.5" stroke="COLOR" stroke-width="1.8"/><path d="M10 13H14" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  calendar: '<rect x="5" y="6.5" width="14" height="12" rx="2" stroke="COLOR" stroke-width="1.8"/><path d="M8 4.5V8" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M16 4.5V8" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M5 10H19" stroke="COLOR" stroke-width="1.8"/>',
  photo: '<rect x="4.5" y="6.5" width="15" height="11" rx="2" stroke="COLOR" stroke-width="1.8"/><circle cx="9" cy="10" r="1.5" fill="COLOR"/><path d="M7 15L10.2 11.8L12.8 14.4L15 12.2L17 15" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  video: '<rect x="4.5" y="7" width="10.5" height="10" rx="2" stroke="COLOR" stroke-width="1.8"/><path d="M15 10L19 8V16L15 14" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/>',
  upload: '<path d="M12 16V7" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M8.5 10.5L12 7L15.5 10.5" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M6 18H18" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  lens: '<circle cx="12" cy="12" r="4.5" stroke="COLOR" stroke-width="1.8"/><circle cx="12" cy="12" r="1.5" fill="COLOR"/>',
  profile: '<circle cx="12" cy="8.5" r="3.2" stroke="COLOR" stroke-width="1.8"/><path d="M6.5 18C7.4 15.7 9.5 14.5 12 14.5C14.5 14.5 16.6 15.7 17.5 18" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>',
  grid: '<rect x="5" y="5" width="5.5" height="5.5" rx="1" stroke="COLOR" stroke-width="1.8"/><rect x="13.5" y="5" width="5.5" height="5.5" rx="1" stroke="COLOR" stroke-width="1.8"/><rect x="5" y="13.5" width="5.5" height="5.5" rx="1" stroke="COLOR" stroke-width="1.8"/><rect x="13.5" y="13.5" width="5.5" height="5.5" rx="1" stroke="COLOR" stroke-width="1.8"/>',
  swap: '<path d="M7 7H18" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M15 4L18 7L15 10" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M17 17H6" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M9 14L6 17L9 20" stroke="COLOR" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  shield: '<path d="M12 4L18 6.7V11.3C18 15.2 15.4 18.8 12 20C8.6 18.8 6 15.2 6 11.3V6.7L12 4Z" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/>',
  chat: '<path d="M5 5H19V15H8L5 18V5Z" stroke="COLOR" stroke-width="1.8" stroke-linejoin="round"/><path d="M9 10H15" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/><path d="M9 13H12" stroke="COLOR" stroke-width="1.8" stroke-linecap="round"/>'
}

const iconSrc = computed(() => {
  const body = ICONS[props.name]
  if (!body) return ''
  const color = props.color === 'currentColor' || !props.color ? '#eef6ff' : props.color
  const colored = body.replace(/COLOR/g, color)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none">${colored}</svg>`
  return 'data:image/svg+xml,' + encodeURIComponent(svg)
})

const iconStyle = computed(() => ({
  width: typeof props.size === 'number' ? `${props.size}rpx` : props.size,
  height: typeof props.size === 'number' ? `${props.size}rpx` : props.size
}))
</script>

<style scoped>
.app-icon {
  flex-shrink: 0;
}
</style>
