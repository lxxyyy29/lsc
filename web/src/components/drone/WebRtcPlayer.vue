<template>
  <div class="webrtc-player-container">
    <div v-if="error" class="webrtc-player-error">
      视频加载失败: {{ error }}
    </div>
    <video
      ref="videoRef"
      class="webrtc-player-video"
      autoplay
      controls
      muted
      playsinline
      v-show="!error"
    ></video>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  url: string
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const error = ref('')

let reader: any = null
let retryCount = 0
let retryTimer: ReturnType<typeof setTimeout> | null = null

const MAX_RETRIES = 5

function stopPlayer() {
  if (retryTimer) { clearTimeout(retryTimer); retryTimer = null }
  if (videoRef.value) videoRef.value.srcObject = null
  if (reader) {
    try { reader.close() } catch { /* ignore */ }
    reader = null
  }
  // 重置重试计数器，避免旧会话的失败次数影响新会话
  retryCount = 0
}

function startPlayer() {
  if (!props.url) {
    error.value = '视频流地址为空'
    return
  }
  // @ts-ignore
  if (!window.MediaMTXWebRTCReader) {
    error.value = '播放器组件未找到，请检查环境'
    return
  }
  error.value = ''
  try {
    // @ts-ignore
    reader = new window.MediaMTXWebRTCReader({
      url: props.url,
      onTrack: (evt: any) => {
        if (videoRef.value) {
          videoRef.value.srcObject = evt.streams[0]
          retryCount = 0
          error.value = ''
        }
      },
      onError: (_err: any) => {
        if (retryCount < MAX_RETRIES) {
          retryCount++
          error.value = `连接中断，正在重试 (${retryCount}/${MAX_RETRIES})...`
          retryTimer = setTimeout(() => {
            stopPlayer()
            startPlayer()
          }, 3000)
        } else {
          error.value = '视频加载失败，请刷新重试'
        }
      }
    })
  } catch (err) {
    console.error('Video player setup error:', err)
    error.value = '视频播放器初始化失败'
  }
}

// 监听 url 变化：停止旧播放器，启动新播放器
watch(() => props.url, (newUrl, oldUrl) => {
  if (newUrl !== oldUrl) {
    stopPlayer()
    if (newUrl) {
      startPlayer()
    }
  }
})

onMounted(() => startPlayer())
onBeforeUnmount(() => stopPlayer())
</script>

<style scoped>
.webrtc-player-container {
  width: 100%;
  aspect-ratio: 16 / 9;
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.webrtc-player-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.webrtc-player-error {
  color: #ffb4b4;
  padding: 16px;
  text-align: center;
  font-size: 14px;
}
</style>
