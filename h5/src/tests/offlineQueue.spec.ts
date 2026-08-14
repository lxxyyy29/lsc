import { beforeEach, describe, expect, it, vi } from 'vitest'
import { HttpResponseError } from '../api/http'

// mock API 层:验证队列调用行为与 payload 传递
const createEventMock = vi.fn()
const createPatrolMock = vi.fn()

vi.mock('../api/event', () => ({
  createEventForH5: (payload: unknown) => createEventMock(payload)
}))
vi.mock('../api/community', () => ({
  createPatrolRecord: (payload: unknown) => createPatrolMock(payload)
}))

import {
  enqueueOfflineTask,
  flushOfflineQueue,
  retryOfflineQueue,
  getPendingCount,
  getOfflineTasks,
  isNetworkError,
  clearFailedTasks
} from '../utils/offlineQueue'

const storageState = new Map<string, string>()

Object.defineProperty(window, 'localStorage', {
  value: {
    getItem(key: string) {
      return storageState.has(key) ? storageState.get(key)! : null
    },
    setItem(key: string, value: string) {
      storageState.set(key, value)
    },
    removeItem(key: string) {
      storageState.delete(key)
    }
  },
  configurable: true
})

beforeEach(() => {
  storageState.clear()
  createEventMock.mockReset()
  createPatrolMock.mockReset()
})

describe('offlineQueue 离线采集队列', () => {
  it('入队后待同步计数与任务列表正确', () => {
    enqueueOfflineTask('EVENT', { externalEventId: 'EVT-1', title: '测试' }, '事件上报:测试')
    enqueueOfflineTask('CHECKIN', { clientRequestId: 'CKI-1' }, '巡查打卡:点位A')
    expect(getPendingCount()).toBe(2)
    expect(getOfflineTasks().map((t) => t.type)).toEqual(['CHECKIN', 'EVENT'])
  })

  it('flush 成功:执行器被调用且队列清空,payload 原样传递(幂等键不变)', () => {
    createEventMock.mockResolvedValue({ eventCode: 'EVT-100' })
    const payload = { externalEventId: 'EVT-FIXED-1', title: '固定幂等键' }
    enqueueOfflineTask('EVENT', payload, '事件上报:测试')
    return flushOfflineQueue().then((r) => {
      expect(r.success).toBe(1)
      expect(createEventMock).toHaveBeenCalledTimes(1)
      expect(createEventMock).toHaveBeenCalledWith(payload)
      expect(getPendingCount()).toBe(0)
      expect(getOfflineTasks()).toHaveLength(0)
    })
  })

  it('flush 网络错误:任务保留待重试,errorCount 递增', () => {
    createEventMock.mockRejectedValue(new TypeError('Network request failed'))
    enqueueOfflineTask('EVENT', { externalEventId: 'EVT-2' }, '事件上报:测试')
    return flushOfflineQueue().then((r) => {
      expect(r.failed).toBe(1)
      const tasks = getOfflineTasks()
      expect(tasks).toHaveLength(1)
      expect(tasks[0].status).toBe('pending')
      expect(tasks[0].errorCount).toBe(1)
    })
  })

  it('flush 业务错误:任务标记 failed 不再自动重试', () => {
    createEventMock.mockRejectedValue(new HttpResponseError('参数校验失败', 400))
    enqueueOfflineTask('EVENT', { externalEventId: 'EVT-3' }, '事件上报:测试')
    return flushOfflineQueue().then((r) => {
      expect(r.failed).toBe(1)
      expect(getOfflineTasks()[0].status).toBe('failed')
      // 再次 flush:failed 任务跳过,执行器不重复调用
      return flushOfflineQueue().then((r2) => {
        expect(createEventMock).toHaveBeenCalledTimes(1)
        expect(r2.success).toBe(0)
      })
    })
  })

  it('retryOfflineQueue:失败任务重置为待同步并重试成功', () => {
    createEventMock.mockRejectedValueOnce(new HttpResponseError('业务失败', 400))
    enqueueOfflineTask('EVENT', { externalEventId: 'EVT-4' }, '事件上报:测试')
    return flushOfflineQueue().then(() => {
      expect(getOfflineTasks()[0].status).toBe('failed')
      createEventMock.mockResolvedValue({ eventCode: 'EVT-200' })
      return retryOfflineQueue().then((r) => {
        expect(r.success).toBe(1)
        expect(getOfflineTasks()).toHaveLength(0)
      })
    })
  })

  it('isNetworkError:网络错误与业务错误区分正确', () => {
    expect(isNetworkError(new TypeError('Network request failed'))).toBe(true)
    expect(isNetworkError(new Error('timeout'))).toBe(true)
    expect(isNetworkError(new HttpResponseError('业务失败', 400))).toBe(false)
  })

  it('clearFailedTasks:只清理失败任务,保留待同步', () => {
    // flush 顺序:后入队的在前;EVT-6 网络错误保留待同步、EVT-5 业务失败
    createEventMock.mockRejectedValueOnce(new TypeError('Network request failed'))
    createEventMock.mockRejectedValueOnce(new HttpResponseError('业务失败', 400))
    enqueueOfflineTask('EVENT', { externalEventId: 'EVT-5' }, '失败的')
    enqueueOfflineTask('EVENT', { externalEventId: 'EVT-6' }, '待同步的')
    return flushOfflineQueue().then(() => {
      clearFailedTasks()
      const tasks = getOfflineTasks()
      expect(tasks).toHaveLength(1)
      expect(tasks[0].payload.externalEventId).toBe('EVT-6')
    })
  })
})
