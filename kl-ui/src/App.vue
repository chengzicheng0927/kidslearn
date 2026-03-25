<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import OnboardingView from './views/OnboardingView.vue'
import ChatView from './views/ChatView.vue'
import SkeletonLoader from './components/SkeletonLoader.vue'
import ExitConfirmModal from './components/ExitConfirmModal.vue'
import PasswordModal from './components/ParentMode/PasswordModal.vue'
import { checkUserStatus } from './services/api'
import { useAppStore } from './stores/appStore'
import { useToast } from './composables/useToast'
import { callNative, onNativeEvent } from './utils/nativeBridge'
import type { Child } from './mock/data'

type View = 'loading' | 'onboarding' | 'chat'

const currentView = ref<View>('loading')
const appStore = useAppStore()
const { child, showPasswordModal } = storeToRefs(appStore)
const { setChild, openParentMode, closePasswordModal } = appStore
const exitModal = ref<InstanceType<typeof ExitConfirmModal> | null>(null)
const { toasts } = useToast()

// 退出确认标记，防止重复弹窗
let exitConfirmed = false

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (currentView.value !== 'chat' || exitConfirmed) return
  e.preventDefault()
  e.returnValue = ''
  exitModal.value?.open()
}

function confirmExit() {
  exitConfirmed = true
  callNative('exitApp', {})
}

let cleanupExitRequest: (() => void) | null = null

onMounted(async () => {
  try {
    const result = await checkUserStatus()
    if (result.exists && result.child) {
      setChild(result.child)
      currentView.value = 'chat'
    } else {
      currentView.value = 'onboarding'
    }
  } catch {
    currentView.value = 'onboarding'
  }
  window.addEventListener('beforeunload', handleBeforeUnload)
  cleanupExitRequest = onNativeEvent('requestExitApp', () => {
    exitModal.value?.open()
  })
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  cleanupExitRequest?.()
})

function handleOnboardingComplete(nextChild: Child) {
  setChild(nextChild)
  currentView.value = 'chat'
}
</script>

<template>
  <div class="root">
    <div v-if="currentView === 'loading'" class="loading">
      <SkeletonLoader type="app" />
    </div>
    <OnboardingView
      v-else-if="currentView === 'onboarding'"
      @complete="handleOnboardingComplete"
    />
    <ChatView
      v-else-if="currentView === 'chat'"
    />
  </div>

  <!-- 家长模式：密码验证 -->
  <PasswordModal
    v-if="showPasswordModal"
    @success="openParentMode"
    @close="closePasswordModal"
  />

  <!-- 退出确认弹窗 -->
  <ExitConfirmModal
    ref="exitModal"
    @confirm="confirmExit"
  />

  <!-- 全局 Toast -->
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="toast"
          :class="toast.type"
        >
          {{ toast.message }}
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  -webkit-tap-highlight-color: transparent;
  font-family: var(--font-family);
  font-synthesis: weight style;
}

html, body {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

#app {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
}

/* 全局 Toast */
.toast-container {
  position: fixed;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  pointer-events: none;
}

.toast {
  padding: 10px 20px;
  border-radius: 22px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18);
  white-space: nowrap;
}

.toast.error {
  background: #FF5252;
  color: #fff;
}

.toast.success {
  background: #4CAF50;
  color: #fff;
}

.toast.info {
  background: #2196F3;
  color: #fff;
}

.toast-enter-active {
  animation: toastIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.toast-leave-active {
  animation: toastOut 0.2s ease forwards;
}

@keyframes toastIn {
  from { opacity: 0; transform: translateY(-16px) scale(0.8); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes toastOut {
  from { opacity: 1; transform: translateY(0) scale(1); }
  to { opacity: 0; transform: translateY(-8px) scale(0.9); }
}
</style>

<style scoped>
.root {
  width: 100%;
  height: 100%;
}

.loading {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: linear-gradient(180deg, #E3F2FD 0%, #F1F8E9 100%);
}

.loading-mascot {
  font-size: 64px;
  animation: float 2s ease-in-out infinite;
}

.loading-text {
  font-size: 24px;
  font-weight: bold;
  color: var(--txt);
  letter-spacing: -0.5px;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
</style>
