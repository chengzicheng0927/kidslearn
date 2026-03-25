<script setup lang="ts">
import { ref } from 'vue'
import { setPassword } from '../../services/api'
import TwemojiText from '../TwemojiText.vue'

const emit = defineEmits<{
  (e: 'success'): void
  (e: 'close'): void
}>()

const password = ref('')
const confirmPassword = ref('')
const error = ref('')
const loading = ref(false)

// 当前步骤: 1 = 输入密码, 2 = 确认密码
const step = ref<1 | 2>(1)

async function handleSubmit() {
  // 防御性检查：必须6位
  if (password.value.length !== 6 || confirmPassword.value.length !== 6) {
    error.value = '请输入完整的6位密码'
    return
  }
  if (loading.value) return

  error.value = ''

  // 校验两次输入一致
  if (password.value !== confirmPassword.value) {
    error.value = '两次密码不一致，请重新输入'
    confirmPassword.value = ''
    return
  }

  loading.value = true

  try {
    const result = await setPassword(password.value)
    if (result.success) {
      emit('success')
    } else {
      error.value = '设置失败，请重试'
    }
  } catch {
    error.value = '设置失败，请重试'
  } finally {
    loading.value = false
  }
}

function handleKeypadInput(digit: string) {
  error.value = ''

  if (digit === 'del') {
    if (step.value === 2 && confirmPassword.value) {
      confirmPassword.value = confirmPassword.value.slice(0, -1)
    } else if (password.value) {
      password.value = password.value.slice(0, -1)
    }
    return
  }

  if (digit === 'clear') {
    password.value = ''
    confirmPassword.value = ''
    step.value = 1
    return
  }

  // 数字输入
  if (step.value === 1) {
    // 第一步：输入密码（固定6位）
    if (password.value.length < 6) {
      password.value += digit
      // 输入满6位后，自动进入第二步
      if (password.value.length === 6) {
        step.value = 2
      }
    }
  } else {
    // 第二步：确认密码
    if (confirmPassword.value.length < 6) {
      confirmPassword.value += digit
    }
  }
}

const digits = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'clear', '0', 'del']
</script>

<template>
  <div class="container">
    <!-- 头部 -->
    <div class="header">
      <button class="close-btn" @click="emit('close')">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <path d="M5 5L15 15M15 5L5 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
      <div class="title">
        <TwemojiText html="🔒" tag="span" class="icon" />
        设置家长密码
      </div>
      <div class="spacer"></div>
    </div>

    <!-- 说明文字 -->
    <div class="desc">
      请输入 6 位数字密码
    </div>

    <!-- 步骤指示 -->
    <div class="steps">
      <div class="step-item" :class="{ active: step === 1, done: step === 2 }">
        <div class="step-num">{{ step > 1 ? '✓' : '1' }}</div>
        <div class="step-label">输入密码</div>
      </div>
      <div class="step-line" :class="{ active: step === 2 }"></div>
      <div class="step-item" :class="{ active: step === 2 }">
        <div class="step-num">2</div>
        <div class="step-label">确认密码</div>
      </div>
    </div>

    <!-- 密码输入指示 -->
    <div class="pwd-indicator">
      <!-- 第一步：输入密码 -->
      <div v-if="step === 1" class="pwd-dots">
        <span
          v-for="i in 6"
          :key="i"
          class="dot"
          :class="{ filled: password.length >= i }"
        ></span>
      </div>
      <!-- 第二步：确认密码 -->
      <div v-else class="pwd-dots">
        <span
          v-for="i in 6"
          :key="i"
          class="dot"
          :class="{ filled: confirmPassword.length >= i }"
        ></span>
      </div>
      <div class="pwd-hint">
        {{ step === 1 ? `请输入 6 位密码（${password.length}/6）` : `请再次输入确认（${confirmPassword.length}/6）` }}
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error">
      {{ error }}
    </div>

    <!-- 数字键盘 -->
    <div class="keypad">
      <button
        v-for="digit in digits"
        :key="digit"
        class="key"
        :class="{ action: digit === 'clear' || digit === 'del' }"
        @click="handleKeypadInput(digit)"
        :disabled="loading"
      >
        <template v-if="digit === 'del'">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M5 5L15 15M15 5L5 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </template>
        <template v-else-if="digit === 'clear'">
          清空
        </template>
        <template v-else>
          {{ digit }}
        </template>
      </button>
    </div>

    <!-- 确认按钮 -->
    <div class="actions">
      <button
        v-if="step === 2"
        class="confirm-btn"
        :class="{ disabled: confirmPassword.length < 6 }"
        :disabled="loading || confirmPassword.length < 6"
        @click="handleSubmit"
      >
        <span v-if="loading" class="btn-spinner"></span>
        <span v-else>{{ confirmPassword.length < 6 ? `还需 ${6 - confirmPassword.length} 位` : '确认设置' }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.container {
  padding: 0 0 24px;
}

.header {
  display: flex;
  align-items: center;
  padding: 16px 16px 12px;
  gap: 12px;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f5f5f5;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
  flex-shrink: 0;
}

.close-btn:active {
  background: #eee;
}

.title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 17px;
  font-weight: 800;
  color: #1a1a2e;
}

.icon {
  font-size: 18px;
}

.spacer {
  width: 32px;
}

.desc {
  text-align: center;
  font-size: 13px;
  color: #888;
  padding: 0 24px 16px;
}

/* 步骤指示器 */
.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 0 24px 16px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  opacity: 0.4;
  transition: opacity 0.3s;
}

.step-item.active,
.step-item.done {
  opacity: 1;
}

.step-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ddd;
  color: white;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-item.active .step-num {
  background: #7c6df8;
}

.step-item.done .step-num {
  background: #43a047;
}

.step-label {
  font-size: 13px;
  font-weight: 600;
  color: #666;
}

.step-item.active .step-label {
  color: #7c6df8;
}

.step-item.done .step-label {
  color: #43a047;
}

.step-line {
  width: 40px;
  height: 2px;
  background: #ddd;
  margin: 0 8px;
  transition: background 0.3s;
}

.step-line.active {
  background: #7c6df8;
}

/* 密码指示器 */
.pwd-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 0 24px 12px;
}

.pwd-dots {
  display: flex;
  gap: 12px;
}

.dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #ddd;
  transition: all 0.2s;
}

.dot.filled {
  background: #7c6df8;
  border-color: #7c6df8;
}

.pwd-hint {
  font-size: 12px;
  color: #999;
}

/* 错误提示 */
.error {
  text-align: center;
  font-size: 13px;
  color: #ff4757;
  padding: 0 24px 8px;
}

/* 数字键盘 */
.keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 8px 24px;
}

.key {
  height: 52px;
  border-radius: 14px;
  border: none;
  background: #f5f5f5;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  font-family: inherit;
}

.key:active:not(:disabled) {
  background: #eee;
  transform: scale(0.95);
}

.key.action {
  font-size: 14px;
  font-weight: 600;
  color: #666;
  background: #f0f0f0;
}

.key:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 操作区域 */
.actions {
  padding: 16px 24px 0;
}

.confirm-btn {
  width: 100%;
  height: 50px;
  border-radius: 25px;
  border: none;
  background: linear-gradient(135deg, #7c6df8, #9d8df8);
  color: white;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
  font-family: inherit;
}

.confirm-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.confirm-btn.disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
