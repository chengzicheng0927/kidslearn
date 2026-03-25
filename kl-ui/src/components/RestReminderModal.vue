<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'dismiss'): void
  (e: 'rest'): void
}>()

const dismissing = ref(false)
let autoTimer: number | null = null

watch(() => props.visible, (val) => {
  if (val) {
    dismissing.value = false
    autoTimer = window.setTimeout(() => {
      dismissing.value = true
      setTimeout(() => emit('dismiss'), 400)
    }, 5000)
  } else {
    if (autoTimer) {
      clearTimeout(autoTimer)
      autoTimer = null
    }
  }
})

function dismiss() {
  if (autoTimer) {
    clearTimeout(autoTimer)
    autoTimer = null
  }
  dismissing.value = true
  setTimeout(() => emit('dismiss'), 400)
}

function takeRest() {
  if (autoTimer) {
    clearTimeout(autoTimer)
    autoTimer = null
  }
  dismissing.value = true
  setTimeout(() => emit('rest'), 400)
}
</script>

<template>
  <Transition name="rest-reminder">
    <div v-if="visible" class="rest-reminder" :class="{ dismissing }">
      <div class="reminder-content">
        <div class="mascot">🧘</div>
        <div class="reminder-text">
          <div class="reminder-title">休息一下 🌿</div>
          <div class="reminder-body">
            学习{{ 15 }}分钟啦！<br/>
            起来活动活动，伸展一下身体吧～
          </div>
        </div>
        <button class="reminder-close" @click="dismiss">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M4 4L12 12M12 4L4 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
      <div class="actions">
        <button class="btn-rest" @click="takeRest">
          <span class="btn-icon">🫣</span>
          <span>休息一下</span>
        </button>
        <button class="btn-continue" @click="dismiss">
          继续学习
        </button>
      </div>
      <div class="scene">
        <div class="cloud c1">☁️</div>
        <div class="cloud c2">☁️</div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.rest-reminder {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 950;
  width: min(320px, 90vw);
  background: linear-gradient(135deg, #FFF8E1, #FFECB3);
  border-radius: 24px;
  padding: 20px 18px 20px;
  box-shadow: 0 8px 32px rgba(255, 152, 0, 0.25);
  text-align: center;
}

.reminder-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  text-align: left;
}

.mascot {
  font-size: 44px;
  line-height: 1;
  flex-shrink: 0;
  animation: breathe 2s ease-in-out infinite;
}

.reminder-text {
  flex: 1;
}

.reminder-title {
  font-size: 17px;
  font-weight: 900;
  color: #e65100;
  margin-bottom: 6px;
}

.reminder-body {
  font-size: 13px;
  font-weight: 600;
  color: #bf360c;
  line-height: 1.5;
}

.reminder-close {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255,255,255,0.6);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffb74d;
  flex-shrink: 0;
}

.actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1.5px dashed rgba(230, 81, 0, 0.2);
}

.btn-rest {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 14px;
  background: #ff9800;
  color: white;
  border: none;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s, background 0.15s;
}

.btn-rest:active {
  transform: scale(0.96);
  background: #f57c00;
}

.btn-icon {
  font-size: 16px;
}

.btn-continue {
  flex: 1;
  padding: 10px 14px;
  background: rgba(255,255,255,0.7);
  color: #e65100;
  border: none;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, background 0.15s;
}

.btn-continue:active {
  transform: scale(0.96);
  background: rgba(255,255,255,0.9);
}

.scene {
  display: flex;
  justify-content: space-around;
  margin-top: 12px;
  padding-top: 10px;
}

.cloud {
  font-size: 24px;
  opacity: 0.7;
  animation: float 4s ease-in-out infinite;
}

.cloud.c1 { animation-delay: 0s; }
.cloud.c2 { animation-delay: 1.5s; font-size: 18px; }

.dismissing {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.9);
  transition: all 0.4s ease;
}

.rest-reminder-enter-active {
  animation: restIn 0.4s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.rest-reminder-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.rest-reminder-leave-to {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.9);
}

@keyframes breathe {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

@keyframes restIn {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.7);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}
</style>
