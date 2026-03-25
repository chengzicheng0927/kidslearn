<script setup lang="ts">
import { ref, onMounted } from 'vue'
import SetPasswordView from './SetPasswordView.vue'
import VerifyPasswordView from './VerifyPasswordView.vue'
import { getPasswordStatus } from '../../services/api'

const emit = defineEmits<{
  (e: 'success'): void
  (e: 'close'): void
}>()

// 状态: 'loading' | 'set' | 'verify'
const status = ref<'loading' | 'set' | 'verify'>('loading')
const loading = ref(true)

onMounted(async () => {
  try {
    const { hasPassword } = await getPasswordStatus()
    status.value = hasPassword ? 'verify' : 'set'
  } catch {
    status.value = 'set'
  } finally {
    loading.value = false
  }
})

function handlePasswordSet() {
  emit('success')
}

function handleVerifySuccess() {
  emit('success')
}

function handleClose() {
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <div class="overlay" @click.self="handleClose">
      <div class="modal">
        <!-- 加载态 -->
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
          <span>加载中...</span>
        </div>

        <!-- 设置密码 -->
        <SetPasswordView
          v-else-if="status === 'set'"
          @success="handlePasswordSet"
          @close="handleClose"
        />

        <!-- 验证密码 -->
        <VerifyPasswordView
          v-else-if="status === 'verify'"
          @success="handleVerifySuccess"
          @close="handleClose"
        />
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-parent);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 25, 39, 0.6);
  backdrop-filter: blur(8px);
  animation: fadeIn 0.2s ease;
}

.modal {
  width: 90%;
  max-width: 360px;
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 24px;
  color: #666;
  font-size: 14px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #f0f0f0;
  border-top-color: #7c6df8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(40px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
