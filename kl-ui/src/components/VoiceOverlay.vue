<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  visible: boolean
  status?: 'recording' | 'success' | 'error'
  recognizedText?: string
}>(), {
  visible: false,
  status: 'recording',
  recognizedText: '',
})

const emit = defineEmits<{
  (e: 'cancel'): void
  (e: 'done'): void
}>()

const waveformBars = ref<number[]>(Array(20).fill(0.1))
let animInterval: number | null = null
let textStableTimer: number | null = null
let lastRecognizedText = ''

function startTextStableTimer() {
  if (textStableTimer) clearTimeout(textStableTimer)
  textStableTimer = window.setTimeout(() => {
    if (props.recognizedText && props.recognizedText === lastRecognizedText) {
      emit('done')
    }
  }, 5000)
}

function resetTextStableTimer() {
  lastRecognizedText = props.recognizedText || ''
  startTextStableTimer()
}

function generateWaveform() {
  return Array(20).fill(0).map(() => Math.random() * 0.8 + 0.2)
}

function startWaveformAnimation() {
  if (animInterval) return
  animInterval = window.setInterval(() => {
    waveformBars.value = generateWaveform()
  }, 150)
}

function stopWaveformAnimation() {
  if (animInterval) {
    clearInterval(animInterval)
    animInterval = null
  }
  waveformBars.value = Array(20).fill(0.1)
}

watch(() => props.visible, (newVal) => {
  if (newVal && props.status === 'recording') {
    startWaveformAnimation()
    lastRecognizedText = ''
    startTextStableTimer()
  } else {
    stopWaveformAnimation()
    if (textStableTimer) {
      clearTimeout(textStableTimer)
      textStableTimer = null
    }
  }
})

watch(() => props.recognizedText, (newVal) => {
  if (newVal && newVal !== lastRecognizedText) {
    resetTextStableTimer()
  }
})

function handleCancel() {
  if (textStableTimer) {
    clearTimeout(textStableTimer)
    textStableTimer = null
  }
  emit('cancel')
}

function handleDone() {
  emit('done')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="voice-overlay">
      <div v-if="visible" class="overlay" @click.self="handleCancel">
        <div class="sheet">
          <div class="handle"></div>

          <!-- 波浪 + 状态文字 -->
          <div class="waveform-wrap">
            <div class="waveform" :class="{ recording: !recognizedText, success: recognizedText }">
              <div
                v-for="(height, i) in waveformBars"
                :key="i"
                class="bar"
                :style="{ height: height * 56 + 'px' }"
              ></div>
            </div>
            <div class="status-label">
              <span class="dot-pulse"></span>
              <span v-if="status === 'recording' && !recognizedText">正在聆听…</span>
              <span v-else-if="status === 'success' || recognizedText">已识别</span>
              <span v-else-if="status === 'error'">识别失败</span>
            </div>
          </div>

          <!-- 识别文字 -->
          <div v-if="recognizedText" class="recognized-text">
            {{ recognizedText }}
          </div>

          <!-- 底部按钮 -->
          <div class="btn-group">
            <button class="cancel-btn" @click="handleCancel">取消</button>
            <button class="done-btn" @click="handleDone">完成</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(10, 5, 30, 0.65);
  backdrop-filter: blur(3px);
  animation: backdropIn 0.25s ease;
}

.sheet {
  width: 100%;
  background: #fff;
  border-radius: 28px 28px 0 0;
  padding: 0 0 40px;
  animation: modalSlide 0.35s cubic-bezier(0.34, 1.2, 0.64, 1);
  position: relative;
  overflow: hidden;
}

.handle {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: #E0D8D0;
  margin: 12px auto 0;
}

/* 波浪 + 状态 */
.waveform-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px 28px 0;
}

.waveform {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 60px;
  width: 100%;
}

.waveform.recording .bar {
  background: linear-gradient(180deg, var(--or), var(--yw));
  animation: wave 0.6s ease-in-out infinite;
}

.waveform.success .bar {
  background: var(--gn);
  animation: wave 0.6s ease-in-out infinite;
}

.waveform.error .bar {
  background: #EF5350;
  animation: none;
}

.bar {
  width: 5px;
  border-radius: 3px;
  min-height: 4px;
  transition: height 0.15s ease;
}

.bar:nth-child(odd) {
  animation-delay: 0.1s;
}

.bar:nth-child(3n) {
  animation-delay: 0.2s;
}

.status-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #273238;
}

.dot-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--or);
  animation: pulse 1s ease-in-out infinite;
}

.recognized-text {
  margin: 16px 24px 0;
  padding: 14px 18px;
  background: #F5F5F5;
  border-radius: 14px;
  font-size: 15px;
  color: #273238;
  line-height: 1.5;
  text-align: center;
}

/* 底部按钮 */
.btn-group {
  display: flex;
  gap: 12px;
  margin: 24px 24px 0;
}

.cancel-btn {
  flex: 1;
  height: 48px;
  border-radius: 16px;
  background: #F5F5F5;
  border: none;
  font-size: 16px;
  font-weight: 700;
  color: #73808c;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:active {
  transform: scale(0.97);
}

.done-btn {
  flex: 2;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--or), #FF7043);
  border: none;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 16px rgba(255, 112, 67, 0.35);
}

.done-btn:active {
  transform: scale(0.97);
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.7); }
}

@keyframes wave {
  0%, 100% { transform: scaleY(0.4); }
  50% { transform: scaleY(1); }
}

@keyframes backdropIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modalSlide {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.voice-overlay-enter-active,
.voice-overlay-leave-active {
  transition: opacity 0.25s ease;
}

.voice-overlay-enter-from,
.voice-overlay-leave-to {
  opacity: 0;
}

.voice-overlay-enter-from .sheet,
.voice-overlay-leave-to .sheet {
  transform: translateY(30px) scale(0.96);
}
</style>
