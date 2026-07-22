<template>
  <img v-if="blobSrc" :src="blobSrc" v-bind="$attrs" />
  <span v-else-if="loading" class="auth-image-loading">...</span>
  <span v-else class="auth-image-error">—</span>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { buildDownloadUrl } from '../../api/upload'

const props = defineProps<{
  src: string
}>()

const blobSrc = ref('')
const loading = ref(false)

function revokePrevious() {
  if (blobSrc.value) {
    URL.revokeObjectURL(blobSrc.value)
    blobSrc.value = ''
  }
}

async function loadImage(url: string) {
  revokePrevious()
  if (!url) return

  loading.value = true
  try {
    const token = JSON.parse(localStorage.getItem('dgcp-oa-web-session') || '{}').token || ''
    const downloadUrl = buildDownloadUrl(url)
    const response = await fetch(downloadUrl, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!response.ok) throw new Error('load failed')
    const blob = await response.blob()
    blobSrc.value = URL.createObjectURL(blob)
  } catch {
    blobSrc.value = ''
  } finally {
    loading.value = false
  }
}

watch(() => props.src, (newUrl) => void loadImage(newUrl), { immediate: true })
onBeforeUnmount(revokePrevious)
</script>

<style scoped>
.auth-image-loading,
.auth-image-error {
  color: rgba(141, 176, 208, 0.5);
  font-size: 12px;
}
</style>
