import { reactive } from 'vue'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface ToastItem {
  id: number
  message: string
  type: ToastType
  duration: number
}

// 模块级单例 — 所有组件共享同一份状态，无需 provide/inject
const toasts = reactive<ToastItem[]>([])
let nextId = 1

function show(message: string, type: ToastType = 'info', duration = 3000) {
  const id = nextId++
  toasts.push({ id, message, type, duration })
  setTimeout(() => {
    const idx = toasts.findIndex((t) => t.id === id)
    if (idx !== -1) toasts.splice(idx, 1)
  }, duration)
}

export function useToast() {
  return {
    toasts,
    show,
    success: (message: string, duration?: number) => show(message, 'success', duration),
    error:   (message: string, duration?: number) => show(message, 'error',   duration ?? 4000),
    info:    (message: string, duration?: number) => show(message, 'info',    duration),
    warning: (message: string, duration?: number) => show(message, 'warning', duration),
  }
}
