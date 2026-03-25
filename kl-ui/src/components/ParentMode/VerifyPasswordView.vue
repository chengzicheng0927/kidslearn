<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { verifyPassword } from '../../services/api'
import TwemojiText from '../TwemojiText.vue'

const emit = defineEmits<{
  (e: 'success'): void
  (e: 'close'): void
}>()

const password = ref('')
const error = ref('')
const loading = ref(false)
const locked = ref(false)
const lockedSeconds = ref(0)

let lockTimer: number | null = null

// 是否完全锁定
const isLocked = computed(() => locked.value && lockedSeconds.value > 0)

function handleKeypadInput(digit: string) {
  if (isLocked.value) return

  if (digit === 'del') {
    password.value = password.value.slice(0, -1)
    error.value = ''
    return
  }

  if (digit === 'clear') {
    password.value = ''
    error.value = ''
    return
  }

  if (password.value.length < 6) {
    password.value += digit
    error.value = ''
  }
}

const digits = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'clear', '0', 'del']

async function handleSubmit() {
  // 防御性检查：必须6位
  if (password.value.length !== 6) return
  if (loading.value || isLocked.value) return

  loading.value = true
  error.value = ''

  try {
    const result = await verifyPassword(password.value)

    if (result.success) {
      emit('success')
    } else if (result.lockedUntil) {
      // 被锁定
      startLockdown(result.lockedUntil)
      error.value = `密码错误次数过多，请 ${lockedSeconds.value} 秒后重试`
      password.value = ''
    } else {
      error.value = `密码错误，剩余 ${result.attemptsLeft} 次机会`
      password.value = ''
    }
  } catch {
    error.value = '验证失败，请重试'
  } finally {
    loading.value = false
  }
}

function startLockdown(lockedUntil: number) {
  locked.value = true

  function updateCountdown() {
    const remaining = Math.max(0, Math.ceil((lockedUntil - Date.now()) / 1000))
    lockedSeconds.value = remaining

    if (remaining <= 0) {
      locked.value = false
      lockedSeconds.value = 0
      if (lockTimer) {
        clearInterval(lockTimer)
        lockTimer = null
      }
    }
  }

  updateCountdown()
  lockTimer = window.setInterval(updateCountdown, 1000)
}

onUnmounted(() => {
  if (lockTimer) {
    clearInterval(lockTimer)
    lockTimer = null
  }
})
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
        输入家长密码
      </div>
      <div class="spacer"></div>
    </div>

    <!-- 锁定状态 -->
    <div v-if="isLocked" class="lock-overlay">
      <TwemojiText html="🔒" tag="div" class="lock-icon" />
      <div class="lock-text">请等待</div>
      <div class="lock-countdown">{{ lockedSeconds }} 秒</div>
    </div>

    <template v-else>
      <!-- 密码输入指示 -->
      <div class="pwd-dots">
        <span
          v-for="i in 6"
          :key="i"
          class="dot"
          :class="{ filled: password.length >= i }"
        ></span>
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
          class="confirm-btn"
          :class="{ disabled: password.length < 6 }"
          :disabled="loading || password.length < 6"
          @click="handleSubmit"
        >
          <span v-if="loading" class="btn-spinner"></span>
          <span v-else>{{ password.length < 6 ? `还需 ${6 - password.length} 位` : '确认' }}</span>
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.container {
  padding: 0 0 24px;
}

.header {
  display: flex;
  align-items: center;
  padding: 16px 16px 20px;
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

.pwd-dots {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 8px 24px 16px;
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

.error {
  text-align: center;
  font-size: 13px;
  color: #ff4757;
  padding: 0 24px 8px;
  min-height: 18px;
}

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

/* 锁定状态 */
.lock-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 24px;
}

.lock-icon {
  font-size: 48px;
  animation: pulse 1s ease-in-out infinite;
}

.lock-text {
  font-size: 16px;
  font-weight: 600;
  color: #666;
}

.lock-countdown {
  font-size: 32px;
  font-weight: 800;
  color: #7c6df8;
  font-variant-numeric: tabular-nums;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}
</style>
