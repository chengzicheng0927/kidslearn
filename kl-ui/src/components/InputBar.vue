<script setup lang="ts">
import { ref, computed } from 'vue'
import { API_CONFIG } from '../config/api.config'

const props = withDefaults(defineProps<{
  modelValue?: string
  isRecording?: boolean
  placeholder?: string
}>(), {
  modelValue: '',
  isRecording: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'send', message: string): void
  (e: 'voice'): void
  (e: 'openParent'): void
  (e: 'openPet'): void
  (e: 'openCalendar'): void
  (e: 'clearSession'): void
}>()

const showFabMenu = ref(false)
const isMock = computed(() => API_CONFIG.USE_MOCK)

function updateValue(value: string) {
  emit('update:modelValue', value)
}

function send() {
  const value = props.modelValue.trim()
  if (!value) return
  emit('send', value)
  emit('update:modelValue', '')
}

function toggleFab() {
  showFabMenu.value = !showFabMenu.value
}

function closeFab() {
  showFabMenu.value = false
}

function handleClearClick() {
  try {
    emit('clearSession')
  } finally {
    closeFab()
  }
}
</script>

<template>
  <div class="ibar-wrap">
    <!-- 弧形菜单 -->
    <div v-if="showFabMenu" class="fab-overlay" @click="closeFab"></div>

    <!-- 左半个圆弧菜单，+ 按钮在右侧圆心 -->
    <div class="fab-arc" :class="{ open: showFabMenu }">
      <!-- Dev 模式：清空会话 -->
      <button v-if="isMock" class="fab-item fab-dev" @click="handleClearClick">
        <span class="fab-label">🧹 清空</span>
        <div class="fab-icon fab-icon-dev">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <polyline points="3,6 5,6 21,6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M19,6L18.2,20H5.8L5,6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M10,11V17M14,11V17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M9,6V4H15V6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
      </button>

      <button class="fab-item" @click="emit('openCalendar'); closeFab()">
        <span class="fab-label">学习日历</span>
        <div class="fab-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
            <line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="2"/>
            <line x1="9" y1="2" x2="9" y2="6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <line x1="15" y1="2" x2="15" y2="6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
      </button>

      <button class="fab-item" @click="emit('openParent'); closeFab()">
        <span class="fab-label">家长端</span>
        <div class="fab-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="2"/>
            <path d="M4 20C4 16.686 7.582 14 12 14C16.418 14 20 16.686 20 20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
      </button>

      <button class="fab-item" @click="emit('openPet'); closeFab()">
        <span class="fab-label">我的宝贝</span>
        <div class="fab-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="2"/>
            <path d="M8 11C8 11 9.5 13 12 13C14.5 13 16 11 16 11" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <circle cx="9.5" cy="10.5" r="1" fill="currentColor"/>
            <circle cx="14.5" cy="10.5" r="1" fill="currentColor"/>
          </svg>
        </div>
      </button>
    </div>

    <!-- 输入栏 -->
    <div class="ibar">
      <!-- 照片按钮 -->
      <button class="icon-btn" title="相册/拍照">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M3 7V17C3 18.1 3.9 19 5 19H19C20.1 19 21 18.1 21 17V9C21 7.9 20.1 7 19 7H15L13 5H5C3.9 5 3 5.9 3 7Z" stroke="var(--or)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="12" cy="13" r="4" stroke="var(--or)" stroke-width="2"/>
        </svg>
      </button>

      <!-- 输入框区域 -->
      <div class="irow">
        <input
          :value="modelValue"
          type="text"
          :placeholder="placeholder"
          @input="updateValue(($event.target as HTMLInputElement).value)"
          @keydown.enter="send"
        />
        <!-- 发送箭头 -->
        <button class="send-btn" :disabled="!modelValue.trim()" @click="send">
          <svg width="13" height="13" viewBox="0 0 15 15" fill="white">
            <path d="M1 7.5L14 1L9.5 14L7 8.5L1 7.5Z"/>
          </svg>
        </button>
      </div>

      <!-- 麦克风 -->
      <button class="mic-btn" :class="{ recording: isRecording }" @click="emit('voice')">
        <svg width="14" height="18" viewBox="0 0 14 18" fill="none">
          <rect x="3" y="0" width="8" height="12" rx="4" :fill="isRecording ? 'var(--card)' : 'var(--bl)'"/>
          <path d="M1 10c0 3.3 2.7 6 6 6s6-2.7 6-6" :stroke="isRecording ? 'var(--card)' : 'var(--bl)'" stroke-width="1.8" stroke-linecap="round" fill="none"/>
          <line x1="7" y1="16" x2="7" y2="18" :stroke="isRecording ? 'var(--card)' : 'var(--bl)'" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>

      <!-- + 菜单按钮 -->
      <button class="fab-plus" :class="{ open: showFabMenu }" @click="toggleFab">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
          <line x1="12" y1="5" x2="12" y2="19" stroke="white" stroke-width="2.5" stroke-linecap="round" class="fab-line-v"/>
          <line x1="5" y1="12" x2="19" y2="12" stroke="white" stroke-width="2.5" stroke-linecap="round" class="fab-line-h"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.ibar-wrap {
  position: relative;
  flex-shrink: 0;
}

/* 遮罩 */
.fab-overlay {
  position: fixed;
  inset: 0;
  z-index: 148;
  background: rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(2px);
  animation: fadeIn 0.2s ease;
}

/* 弧形菜单：固定在屏幕右下角，竖向排列像 [ 括号 */
.fab-arc {
  position: fixed;
  bottom: 80px;
  right: 16px;
  z-index: 149;
  display: flex;
  flex-direction: column-reverse;
  align-items: flex-end;
  gap: 10px;
  pointer-events: none;
}

.fab-arc.open {
  pointer-events: auto;
}

/* 菜单项：竖向排列，图标在右，文字在左 */
.fab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  cursor: pointer;
  opacity: 0;
  transform: translateX(20px) scale(0.6);
  transition: opacity 0.2s ease, transform 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.fab-arc.open .fab-item {
  opacity: 1;
  transform: translateX(0) scale(1);
}

.fab-arc.open .fab-item:nth-child(1) { transition-delay: 0.05s; }
.fab-arc.open .fab-item:nth-child(2) { transition-delay: 0.1s; }
.fab-arc.open .fab-item:nth-child(3) { transition-delay: 0.15s; }

/* 图标圆按钮 */
.fab-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 3px 14px rgba(0, 0, 0, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--or);
  flex-shrink: 0;
  transition: transform 0.15s;
}

.fab-item:active .fab-icon {
  transform: scale(0.88);
}

/* 文字标签 */
.fab-label {
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
  white-space: nowrap;
  background: rgba(0, 0, 0, 0.4);
  padding: 4px 10px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

/* Dev 清空按钮样式 */
.fab-icon-dev {
  color: #e53935;
}

/* 输入栏 */
.ibar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px 24px;
}

/* 照片按钮 */
.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--or-l);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.icon-btn:active {
  transform: scale(0.92);
  background: #ffe0d6;
}

/* 输入框 */
.irow {
  flex: 1;
  position: relative;
  background: var(--card);
  border-radius: 28px;
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  transition: all 0.2s;
}

.irow:focus-within {
  border-color: var(--or);
  box-shadow: 0 2px 12px rgba(255, 112, 67, 0.15);
}

.irow input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 15px;
  color: var(--txt);
  outline: none;
  font-family: inherit;
  padding: 11px 44px 11px 18px;
}

.irow input::placeholder {
  color: var(--muted);
}

/* 发送按钮 */
.send-btn {
  position: absolute;
  right: 5px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--or), #FF8A65);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(255, 112, 67, 0.35);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.9);
}

.send-btn:disabled {
  background: #d8d5d5;
  box-shadow: none;
  cursor: not-allowed;
}

/* 麦克风 */
.mic-btn {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--bl-l);
  border: 2px solid rgba(41, 182, 246, 0.2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.mic-btn:active {
  transform: scale(0.92);
}

.mic-btn.recording {
  background: linear-gradient(135deg, var(--or), var(--yw));
  border-color: rgba(255, 112, 67, 0.3);
  box-shadow: 0 0 0 6px rgba(255, 112, 67, 0.12);
}

/* + 按钮 */
.fab-plus {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--or), #FF8A65);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(255, 112, 67, 0.35);
}

.fab-plus:active {
  transform: scale(0.92);
}

.fab-plus.open {
  background: linear-gradient(135deg, #555, #888);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.fab-line-v {
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  transform-origin: center;
}

.fab-line-h {
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  transform-origin: center;
}

.fab-plus.open .fab-line-v {
  transform: rotate(45deg);
}

.fab-plus.open .fab-line-h {
  transform: rotate(-225deg);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
