import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import {
  buildDroneWebSocketUrl,
  type DroneAlgorithmTaskStatus,
  type DroneDeviceOsd,
  type DroneDockOsd,
  type DroneOfflineNotice,
  type DroneSocketEnvelope
} from '../api/drone'

interface UseDroneWebSocketOptions {
  enabled?: Ref<boolean>
}

// DJI Cloud API nests OSD data under data.host; fall back to data itself for compatibility
function extractHost(data: unknown): unknown {
  if (data && typeof data === 'object' && 'host' in data) {
    return (data as Record<string, unknown>).host
  }
  return data
}

export function useDroneWebSocket(deviceSn: Ref<string>, options: UseDroneWebSocketOptions = {}) {
  const socket = ref<WebSocket | null>(null)
  const connected = ref(false)
  const connecting = ref(false)
  const lastMessageAt = ref<string | null>(null)
  const errorMessage = ref('')
  const dockState = ref<DroneDockOsd | null>(null)
  const deviceState = ref<DroneDeviceOsd | null>(null)
  const algorithmState = ref<DroneAlgorithmTaskStatus | null>(null)
  const offlineNotice = ref<DroneOfflineNotice | null>(null)

  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let shouldReconnect = true

  // Throttle OSD updates to avoid UI flashing (upstream pushes every ~0.5s)
  // Upstream sends dock_osd in SPLIT batches — each message has a subset of fields.
  // We must MERGE incoming data into the existing state, not replace it.
  let pendingDock: Record<string, unknown> | null = null
  let pendingDevice: Record<string, unknown> | null = null
  let osdFlushTimer: ReturnType<typeof setTimeout> | null = null
  const OSD_THROTTLE_MS = 1000

  function flushOsd() {
    if (pendingDock !== null) {
      // Merge into existing state to accumulate split batches
      dockState.value = { ...(dockState.value ?? {}), ...pendingDock } as DroneDockOsd
      pendingDock = null
    }
    if (pendingDevice !== null) {
      deviceState.value = { ...(deviceState.value ?? {}), ...pendingDevice } as DroneDeviceOsd
      pendingDevice = null
    }
    osdFlushTimer = null
  }

  function scheduleOsdFlush() {
    if (osdFlushTimer == null) {
      osdFlushTimer = setTimeout(flushOsd, OSD_THROTTLE_MS)
    }
  }

  const enabled = computed(() => options.enabled?.value ?? true)

  function clearReconnectTimer() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  function resetSocket() {
    clearReconnectTimer()
    if (osdFlushTimer) {
      clearTimeout(osdFlushTimer)
      osdFlushTimer = null
    }
    // 清空残留的 pending 数据，防止旧连接数据污染新连接
    pendingDock = null
    pendingDevice = null
    if (socket.value) {
      socket.value.close()
      socket.value = null
    }
    connected.value = false
    connecting.value = false
  }

  /** 切换设备或离线时清空所有实时状态 */
  function clearOsdState() {
    dockState.value = null
    deviceState.value = null
    algorithmState.value = null
    offlineNotice.value = null
  }

  function scheduleReconnect() {
    if (!shouldReconnect || !enabled.value || !deviceSn.value || typeof WebSocket === 'undefined') {
      return
    }

    clearReconnectTimer()
    reconnectTimer = setTimeout(() => {
      void connect()
    }, 3000)
  }

  async function connect() {
    if (!enabled.value || !deviceSn.value || connecting.value || typeof WebSocket === 'undefined') {
      return
    }

    const url = buildDroneWebSocketUrl(deviceSn.value)
    if (!url) {
      return
    }

    resetSocket()
    connecting.value = true
    errorMessage.value = ''

    const ws = new WebSocket(url)
    socket.value = ws

    ws.onopen = () => {
      connected.value = true
      connecting.value = false
      errorMessage.value = ''
    }

    ws.onmessage = (event) => {
      lastMessageAt.value = new Date().toISOString()

      try {
        const payload = JSON.parse(String(event.data)) as DroneSocketEnvelope
        switch (payload.biz_code) {
          case 'dock_osd':
            pendingDock = { ...(pendingDock ?? {}), ...(extractHost(payload.data) as Record<string, unknown> ?? {}) }
            scheduleOsdFlush()
            break
          case 'device_osd':
            pendingDevice = { ...(pendingDevice ?? {}), ...(extractHost(payload.data) as Record<string, unknown> ?? {}) }
            scheduleOsdFlush()
            break
          case 'device_offline':
            offlineNotice.value = (payload.data ?? null) as DroneOfflineNotice | null
            // 离线时清空设备实时状态，让 UI 立即反映离线（对齐上游行为）
            deviceState.value = null
            break
          case 'Flyline_Alg_Task_Staus':
            algorithmState.value = (payload.data ?? null) as DroneAlgorithmTaskStatus | null
            break
          default:
            break
        }
      } catch {
        errorMessage.value = '实时数据解析失败'
      }
    }

    ws.onerror = () => {
      errorMessage.value = '实时连接异常'
    }

    ws.onclose = () => {
      connected.value = false
      connecting.value = false
      socket.value = null
      scheduleReconnect()
    }
  }

  function disconnect() {
    shouldReconnect = false
    resetSocket()
  }

  function reconnect() {
    shouldReconnect = true
    void connect()
  }

  watch(
    [deviceSn, enabled],
    ([nextDeviceSn, nextEnabled], [prevDeviceSn]) => {
      if (!nextEnabled || !nextDeviceSn) {
        resetSocket()
        clearOsdState()
        return
      }

      if (nextDeviceSn !== prevDeviceSn || !socket.value) {
        shouldReconnect = true
        // 切换设备时清空旧设备的状态，避免显示残留数据
        if (nextDeviceSn !== prevDeviceSn) {
          clearOsdState()
        }
        void connect()
      }
    },
    { immediate: true }
  )

  onBeforeUnmount(() => {
    disconnect()
  })

  return {
    connected,
    connecting,
    lastMessageAt,
    errorMessage,
    dockState,
    deviceState,
    algorithmState,
    offlineNotice,
    connect,
    disconnect,
    reconnect
  }
}
