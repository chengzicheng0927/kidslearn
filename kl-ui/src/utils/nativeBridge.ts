type NativePayload = Record<string, unknown>

declare global {
  interface Window {
    __onNativeEvent__?: (event: string, data: unknown) => void
    webkit?: {
      messageHandlers?: Record<string, { postMessage: (payload: NativePayload) => void }>
    }
  }
}

const nativeActions = new Set([
  'startSpeech',
  'stopSpeech',
  'finishSpeech',
  'playLottie',
  'requestPushPerm',
  'lockScreen',
  'exitApp',
])

function getNativeHandler(action: string) {
  return window.webkit?.messageHandlers?.[action]
}

export function isNativeBridgeAvailable(action?: string) {
  if (!window.webkit?.messageHandlers) return false
  return action ? Boolean(getNativeHandler(action)) : true
}

export function callNative(action: string, payload: NativePayload = {}) {
  const handler = getNativeHandler(action)
  if (handler) {
    handler.postMessage(payload)
    return true
  }

  if (nativeActions.has(action)) {
    console.info(`[NativeBridge Mock] ${action}`, payload)
  } else {
    console.warn(`[NativeBridge Missing] ${action}`, payload)
  }

  return false
}

export function setupNativeCallbacks() {
  window.__onNativeEvent__ = (event: string, data: unknown) => {
    window.dispatchEvent(new CustomEvent(`native:${event}`, { detail: data }))
  }
}

export function onNativeEvent<T>(event: string, handler: (data: T) => void) {
  const listener = (e: Event) => {
    handler((e as CustomEvent<T>).detail)
  }

  window.addEventListener(`native:${event}`, listener)
  return () => window.removeEventListener(`native:${event}`, listener)
}

export function startSpeech(language = 'zh-CN') {
  return callNative('startSpeech', { language })
}

export function stopSpeech() {
  return callNative('stopSpeech')
}

export function finishSpeech() {
  return callNative('finishSpeech')
}

export function playLottie(name: string) {
  return callNative('playLottie', { name })
}

export function requestPushPermission() {
  return callNative('requestPushPerm')
}

export function lockScreen(locked = true) {
  return callNative('lockScreen', { locked })
}

export function exitApp() {
  return callNative('exitApp', {})
}
