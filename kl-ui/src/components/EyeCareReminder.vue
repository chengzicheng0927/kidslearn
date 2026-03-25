<script setup lang="ts">
import { ref, watch } from 'vue'
import TwemojiText from './TwemojiText.vue'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'dismiss'): void
}>()

const dismissing = ref(false)
let autoTimer: number | null = null

watch(() => props.visible, (val) => {
  if (val) {
    dismissing.value = false
    autoTimer = window.setTimeout(() => {
      dismissing.value = true
      setTimeout(() => emit('dismiss'), 400)
    }, 4000)
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
</script>

<template>
  <Transition name="eye-reminder">
    <div v-if="visible" class="eye-reminder" :class="{ dismissing }">
      <div class="reminder-content">
        <TwemojiText class="eye-icon" text="👀" />
        <div class="reminder-text">
          <div class="reminder-title">护眼提醒 🌿</div>
          <div class="reminder-body">
            看看远处（6米外），<br/>保护眼睛哦！
          </div>
        </div>
        <button class="reminder-close" @click="dismiss">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M4 4L12 12M12 4L4 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
      <div class="tree-scene">
        <div class="tree t1">🌳</div>
        <div class="tree t2">🌲</div>
        <div class="tree t3">🌿</div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.eye-reminder {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 900;
  width: min(320px, 90vw);
  background: linear-gradient(135deg, #E8F5E9, #C8E6C9);
  border-radius: 24px;
  padding: 20px 18px 24px;
  box-shadow: 0 8px 32px rgba(56, 142, 60, 0.25);
  text-align: center;
}

.reminder-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  text-align: left;
}

.eye-icon {
  font-size: 40px;
  line-height: 1;
  flex-shrink: 0;
  animation: blink 1.5s ease-in-out infinite;
}

.reminder-text {
  flex: 1;
}

.reminder-title {
  font-size: 15px;
  font-weight: 900;
  color: #2e7d32;
  margin-bottom: 6px;
}

.reminder-body {
  font-size: 13px;
  font-weight: 600;
  color: #558b2f;
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
  color: #81c784;
  flex-shrink: 0;
}

/* 装饰树木 */
.tree-scene {
  display: flex;
  justify-content: space-around;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1.5px dashed rgba(56, 142, 60, 0.3);
}

.tree {
  font-size: 28px;
  animation: sway 3s ease-in-out infinite;
}

.tree.t1 { animation-delay: 0s; }
.tree.t2 { animation-delay: 0.5s; font-size: 22px; }
.tree.t3 { animation-delay: 1s; font-size: 18px; }

/* 关闭动画 */
.dismissing {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.9);
  transition: all 0.4s ease;
}

/* 过渡动画 */
.eye-reminder-enter-active {
  animation: eyeIn 0.4s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.eye-reminder-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.eye-reminder-leave-to {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.9);
}

@keyframes blink {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes sway {
  0%, 100% { transform: rotate(-3deg); }
  50% { transform: rotate(3deg); }
}

@keyframes eyeIn {
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
