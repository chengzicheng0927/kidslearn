import { ref } from 'vue'

export interface Toast {
  id: number
  message: string
  type: 'error' | 'success' | 'info'
}

const toasts = ref<Toast[]>([])
let nextId = 0

export function useToast() {
  function showToast(message: string, type: Toast['type'] = 'error', duration = 3500) {
    const id = ++nextId
    toasts.value.push({ id, message, type })
    setTimeout(() => {
      const idx = toasts.value.findIndex((t) => t.id === id)
      if (idx !== -1) toasts.value.splice(idx, 1)
    }, duration)
  }

  function error(message: string) {
    showToast(message, 'error')
  }

  function success(message: string) {
    showToast(message, 'success')
  }

  return { toasts, showToast, error, success }
}

// 全局 API 错误处理器注册（供 api.ts 调用，避免循环依赖）
window.__API_ERROR_HANDLER__ = (message: string) => {
  const { error } = useToast()
  error(message)
}

declare global {
  interface Window {
    __API_ERROR_HANDLER__?: (message: string) => void
  }
}
