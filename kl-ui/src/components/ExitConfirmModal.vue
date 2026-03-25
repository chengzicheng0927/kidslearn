<script setup lang="ts">
import { ref } from 'vue'
import TwemojiText from './TwemojiText.vue'

const emit = defineEmits<{
  (e: 'confirm'): void   // 用户确认退出
  (e: 'cancel'): void    // 用户取消退出
}>()

const show = ref(false)

function open() {
  show.value = true
}

function confirm() {
  show.value = false
  emit('confirm')
}

function cancel() {
  show.value = false
  emit('cancel')
}

defineExpose({ open })
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="show" class="overlay" @click.self="cancel">
        <div class="sheet">
          <div class="sheet-icon">
            <TwemojiText text="🌟" />
          </div>
          <div class="sheet-title">确定要离开吗？</div>
          <div class="sheet-sub">再坚持一下，学习就是每天进步一点点 💪</div>
          <div class="sheet-actions">
            <button class="btn-cancel" @click="cancel">继续学习</button>
            <button class="btn-confirm" @click="confirm">确定离开</button>
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
  z-index: var(--z-exit-confirm);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(20, 25, 39, 0.5);
  backdrop-filter: blur(4px);
}

.sheet {
  width: min(100%, 320px);
  background: #fff;
  border-radius: 24px;
  padding: 28px 24px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  box-shadow: 0 8px 32px rgba(19, 36, 68, 0.15);
  animation: slideUp 0.35s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.sheet-icon {
  font-size: 52px;
  line-height: 1;
  margin-bottom: 4px;
}

.sheet-title {
  font-size: 20px;
  font-weight: 900;
  color: #273238;
  letter-spacing: -0.4px;
}

.sheet-sub {
  font-size: 14px;
  color: #73808c;
  text-align: center;
  line-height: 1.5;
  padding: 0 8px;
}

.sheet-actions {
  display: flex;
  gap: 10px;
  width: 100%;
  margin-top: 8px;
}

.btn-cancel {
  flex: 1;
  padding: 14px;
  border-radius: 14px;
  border: 2px solid var(--or);
  background: transparent;
  color: var(--or);
  font-size: 15px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:active {
  transform: scale(0.96);
  background: var(--or-l);
}

.btn-confirm {
  flex: 1;
  padding: 14px;
  border-radius: 14px;
  border: none;
  background: var(--or);
  color: white;
  font-size: 15px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 112, 67, 0.3);
  transition: all 0.2s;
}

.btn-confirm:active {
  transform: scale(0.96);
  box-shadow: 0 2px 8px rgba(255, 112, 67, 0.3);
}

/* 动画 */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
