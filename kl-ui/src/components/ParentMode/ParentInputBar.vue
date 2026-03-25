<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(defineProps<{
  disabled?: boolean
}>(), {
  disabled: false,
})

const emit = defineEmits<{
  (e: 'send', message: string): void
}>()

const inputValue = ref('')

function handleSend() {
  const value = inputValue.value.trim()
  if (!value || props.disabled) return

  emit('send', value)
  inputValue.value = ''
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handlePickImage() {
  // TODO: 接入 iOS 相册/拍照
}
</script>

<template>
  <div class="input-bar" :class="{ disabled }">
    <!-- 照片按钮 -->
    <button class="icon-btn" :disabled="disabled" @click="handlePickImage" title="相册/拍照">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
        <path d="M3 7V17C3 18.1 3.9 19 5 19H19C20.1 19 21 18.1 21 17V9C21 7.9 20.1 7 19 7H15L13 5H5C3.9 5 3 5.9 3 7Z" stroke="#7c6df8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <circle cx="12" cy="13" r="4" stroke="#7c6df8" stroke-width="2"/>
      </svg>
    </button>

    <!-- 输入区域 -->
    <div class="input-field">
      <input
        v-model="inputValue"
        type="text"
        placeholder="告诉老师您想怎么安排学习..."
        :disabled="disabled"
        @keydown="handleKeydown"
      />
      <!-- 发送箭头（嵌入输入框右侧） -->
      <button
        class="send-btn"
        :disabled="disabled || !inputValue.trim()"
        @click="handleSend"
      >
        <svg width="14" height="14" viewBox="0 0 15 15" fill="white">
          <path d="M1 7.5L14 1L9.5 14L7 8.5L1 7.5Z"/>
        </svg>
      </button>
    </div>

    <!-- 语音按钮（竖麦） -->
    <button class="icon-btn mic" :disabled="disabled" title="语音输入">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
        <rect x="9" y="2" width="6" height="11" rx="3" stroke="#7c6df8" stroke-width="2" stroke-linecap="round"/>
        <path d="M5 12C5 15.866 7.686 19 12 19C16.314 19 19 15.866 19 12" stroke="#7c6df8" stroke-width="2" stroke-linecap="round"/>
        <line x1="12" y1="19" x2="12" y2="22" stroke="#7c6df8" stroke-width="2" stroke-linecap="round"/>
        <line x1="9" y1="22" x2="15" y2="22" stroke="#7c6df8" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </button>
  </div>
</template>

<style scoped>
.input-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px 28px;
  flex-shrink: 0;
  background: #fff;
}

.input-bar.disabled {
  opacity: 0.6;
  pointer-events: none;
}

/* 图标按钮（照片 / 语音） */
.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0eeff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.icon-btn:active:not(:disabled) {
  background: #e0ddff;
  transform: scale(0.92);
}

.icon-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.icon-btn.mic {
  background: linear-gradient(135deg, #7c6df8, #9d8df8);
  box-shadow: 0 2px 8px rgba(124, 109, 248, 0.3);
}

.icon-btn.mic:active:not(:disabled) {
  background: linear-gradient(135deg, #6b5ce7, #8d7df7);
  transform: scale(0.92);
}

/* 输入框区域 */
.input-field {
  flex: 1;
  position: relative;
  background: #fff;
  border-radius: 30px;
  border: 1.5px solid #e8e4ff;
  box-shadow: 0 2px 8px rgba(124, 109, 248, 0.08);
  display: flex;
  align-items: center;
  transition: all 0.2s;
}

.input-field:focus-within {
  border-color: #7c6df8;
  box-shadow: 0 2px 12px rgba(124, 109, 248, 0.15);
}

.input-field input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 15px;
  color: #1a1a2e;
  outline: none;
  font-family: inherit;
  padding: 12px 44px 12px 18px;
}

.input-field input::placeholder {
  color: #bbb;
}

/* 发送按钮（嵌入输入框内） */
.send-btn {
  position: absolute;
  right: 6px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7c6df8, #9d8df8);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(124, 109, 248, 0.35);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.9);
}

.send-btn:disabled {
  background: #d8d5d5;
  box-shadow: none;
  cursor: not-allowed;
}
</style>
