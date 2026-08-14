/**
 * 离线采集队列(R07 工作人员移动端「离线采集」)
 *
 * 在网络信号差的区域,数据采集(事件上报/巡查打卡等)先写入本地存储队列,
 * 网络恢复后自动同步到服务端;同步失败保留在队列,不丢数据。
 *
 * - H5 环境用 localStorage,微信小程序用 uni storage(双端兼容)
 * - 网络恢复监听(uni.onNetworkStatusChange)+ 30 秒定时重试 + 手动重试入口
 * - 幂等:事件上报 fixed externalEventId(后端唯一键),巡查打卡 clientRequestId(后端去重)
 */
import { createEventForH5, EventCreatePayload } from '../api/event'
import { createPatrolRecord, PatrolRecord } from '../api/community'
import { HttpResponseError } from '../api/http'

export interface OfflineTask {
  /** 任务唯一ID(幂等键) */
  id: string
  /** 任务类型 */
  type: 'EVENT' | 'CHECKIN'
  /** 展示标题 */
  label: string
  /** 入队时间 */
  queuedAt: number
  /** 失败次数(超过阈值标记 failed) */
  errorCount: number
  /** 状态:pending 待同步 / failed 同步失败(需人工处理) */
  status: 'pending' | 'failed'
  /** 提交载荷 */
  payload: any
}

const STORAGE_KEY = 'zhsq-offline-queue'
const MAX_RETRY = 5
const RETRY_INTERVAL = 30_000

/** 获取 uni 全局对象(H5 运行时 window.uni 可能是空占位对象,需检查方法再调用) */
function getUni() {
  // #ifdef MP-WEIXIN
  const wxInstance = (globalThis as { wx?: { getStorageSync?: unknown; setStorageSync?: unknown; onNetworkStatusChange?: unknown } }).wx
  if (wxInstance && typeof wxInstance.getStorageSync === 'function') return wxInstance
  return undefined
  // #endif
  // #ifndef MP-WEIXIN
  const globalUni = (globalThis as { uni?: { getStorageSync?: unknown; setStorageSync?: unknown; onNetworkStatusChange?: unknown } }).uni
  if (globalUni && typeof globalUni.getStorageSync === 'function') return globalUni
  return undefined
  // #endif
}

function readQueue(): OfflineTask[] {
  const uni = getUni()
  try {
    const raw = uni ? String(uni.getStorageSync(STORAGE_KEY) || '') : localStorage.getItem(STORAGE_KEY) || ''
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function writeQueue(tasks: OfflineTask[]) {
  const uni = getUni()
  try {
    if (uni) {
      uni.setStorageSync(STORAGE_KEY, JSON.stringify(tasks))
    } else {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks))
    }
  } catch (e) {
    console.warn('[offlineQueue] 队列写入失败(容量超限?)', e)
  }
}

/** 判断是否为网络层错误(业务错误带 status,网络错误不带) */
export function isNetworkError(e: unknown): boolean {
  return !(e instanceof HttpResponseError && e.status !== undefined)
}

/** 待同步任务数 */
export function getPendingCount(): number {
  return readQueue().filter((t) => t.status === 'pending').length
}

/** 全部任务(含失败,供角标/入口展示) */
export function getOfflineTasks(): OfflineTask[] {
  return readQueue()
}

/** 任务入队(仅在网络错误时调用) */
export function enqueueOfflineTask(type: OfflineTask['type'], payload: any, label: string): number {
  const tasks = readQueue()
  tasks.unshift({
    id: (payload.externalEventId || payload.clientRequestId || '') + '-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8),
    type,
    label,
    queuedAt: Date.now(),
    errorCount: 0,
    status: 'pending',
    payload
  })
  writeQueue(tasks)
  return tasks.length
}

async function executeTask(task: OfflineTask): Promise<void> {
  if (task.type === 'EVENT') {
    await createEventForH5(task.payload as EventCreatePayload)
  } else {
    await createPatrolRecord(task.payload as PatrolRecord)
  }
}

let flushing = false

/** 同步队列:逐个提交,成功移除,失败保留(网络错误重试,业务错误标记 failed) */
export async function flushOfflineQueue(): Promise<{ success: number; failed: number }> {
  if (flushing) return { success: 0, failed: 0 }
  flushing = true
  try {
    const tasks = readQueue()
    if (!tasks.length) return { success: 0, failed: 0 }
    let success = 0
    let failed = 0
    const remaining: OfflineTask[] = []
    for (const task of tasks) {
      if (task.status === 'failed') { remaining.push(task); continue }
      try {
        await executeTask(task)
        success += 1
      } catch (e) {
        if (isNetworkError(e)) {
          // 网络错误:保留重试
          task.errorCount += 1
          if (task.errorCount > MAX_RETRY) task.status = 'failed'
          remaining.push(task)
        } else {
          // 业务错误:不再自动重试,标记失败待人工处理
          task.status = 'failed'
          remaining.push(task)
        }
        failed += 1
      }
    }
    writeQueue(remaining)
    return { success, failed }
  } finally {
    flushing = false
  }
}

/** 手动重试:重置失败任务为待同步后立即同步 */
export async function retryOfflineQueue(): Promise<{ success: number; failed: number }> {
  const tasks = readQueue().map((t) => (t.status === 'failed' ? { ...t, status: 'pending', errorCount: 0 } : t))
  writeQueue(tasks)
  return flushOfflineQueue()
}

/** 清空已失败任务 */
export function clearFailedTasks() {
  writeQueue(readQueue().filter((t) => t.status !== 'failed'))
}

let timer: ReturnType<typeof setInterval> | null = null
let initialized = false

/**
 * 初始化:网络恢复监听 + 定时重试 + 立即同步一次。
 * 在 App 启动与关键页面 onShow 调用(幂等,只初始化一次)。
 */
export function initOfflineQueue() {
  if (initialized) return
  initialized = true
  // 网络恢复自动同步
  const uni = getUni()
  try {
    if (uni && typeof uni.onNetworkStatusChange === 'function') {
      uni.onNetworkStatusChange((res: { isConnected: boolean }) => {
        if (res.isConnected) flushOfflineQueue()
      })
    } else if (typeof window !== 'undefined' && window.addEventListener) {
      window.addEventListener('online', () => { flushOfflineQueue() })
    }
  } catch (e) {
    console.warn('[offlineQueue] 网络监听注册失败', e)
  }
  // 定时重试(仅在有待同步任务时执行)
  timer = setInterval(() => {
    if (getPendingCount() > 0) flushOfflineQueue()
  }, RETRY_INTERVAL)
  // 启动时立即同步一次
  if (getPendingCount() > 0) flushOfflineQueue()
}
