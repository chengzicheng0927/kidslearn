<script setup lang="ts">
import TwemojiText from '../TwemojiText.vue'
import ChatCard from './ChatCard.vue'
import type { ParentMessage } from '../../types/parent'

const props = defineProps<{
  message: ParentMessage
}>()

const emit = defineEmits<{
  (e: 'action', action: string, message: ParentMessage): void
}>()

function handleCardAction(action: string) {
  emit('action', action, props.message)
}
</script>

<template>
  <div class="bubble-wrap" :class="[message.role, message.type]">
    <!-- AI / 系统消息 -->
    <template v-if="message.role === 'ai' || message.role === 'system'">
      <!-- 思考中 -->
      <div v-if="message.type === 'thinking'" class="thinking-bubble">
        <div class="thinking-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>

      <!-- 计划生成中 -->
      <div v-else-if="message.type === 'plan_loading'" class="plan-loading">
        <div class="loading-icon">
          <TwemojiText html="📋" tag="span" />
        </div>
        <div class="loading-text">
          <span class="loading-title">正在生成学习计划</span>
          <span class="loading-desc">正在为孩子量身定制题目，请稍候...</span>
        </div>
        <div class="loading-progress">
          <div class="progress-bar">
            <div class="progress-fill"></div>
          </div>
        </div>
      </div>

      <!-- 卡片消息 -->
      <ChatCard
        v-else-if="message.type === 'card' && message.card"
        :card="message.card"
        @action="handleCardAction"
      />

      <!-- 成功消息 -->
      <div v-else-if="message.type === 'success'" class="text-bubble success">
        <TwemojiText html="✅" tag="span" class="icon" />
        {{ message.content }}
      </div>

      <!-- 错误消息 -->
      <div v-else-if="message.type === 'error'" class="text-bubble error">
        <TwemojiText html="⚠️" tag="span" class="icon" />
        {{ message.content }}
      </div>

      <!-- 文本消息（默认） -->
      <div v-else class="text-bubble ai">
        {{ message.content }}
      </div>
    </template>

    <!-- 家长消息 -->
    <template v-else-if="message.role === 'parent'">
      <div class="text-bubble parent">
        {{ message.content }}
      </div>
    </template>
  </div>
</template>

<style scoped>
.bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  animation: slideIn 0.3s ease;
}

.bubble-wrap.ai,
.bubble-wrap.system {
  align-items: flex-start;
}

.bubble-wrap.parent {
  align-items: flex-end;
}

.text-bubble {
  max-width: 260px;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.55;
  word-break: break-word;
}

.text-bubble.ai {
  background: #F5F7FA;
  border: 1px solid #E8EDF2;
  border-radius: 12px;
  color: #1a1a2e;
}

.text-bubble.parent {
  background: linear-gradient(135deg, #7c6df8, #9d8df8);
  color: white;
  border-radius: 12px;
}

.text-bubble.success {
  background: #e8f5e9;
  color: #2e7d32;
  border-radius: 12px;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.text-bubble.error {
  background: #ffebee;
  color: #c62828;
  border-radius: 12px;
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.icon {
  flex-shrink: 0;
  font-size: 14px;
  margin-top: 1px;
}

/* 思考中动画 */
.thinking-bubble {
  background: #F5F7FA;
  border: 1px solid #E8EDF2;
  border-radius: 12px;
  padding: 14px 20px;
}

.thinking-dots {
  display: flex;
  gap: 5px;
  align-items: center;
}

.thinking-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #bbb;
  animation: bounce 1.4s ease-in-out infinite;
}

.thinking-dots span:nth-child(1) { animation-delay: 0s; }
.thinking-dots span:nth-child(2) { animation-delay: 0.2s; }
.thinking-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

/* 计划生成中 */
.plan-loading {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  background: #F8F7FF;
  border-radius: 12px;
  border: 1px solid #E8E4FF;
  min-width: 200px;
}

.loading-icon {
  font-size: 24px;
  animation: pulse 1.5s ease-in-out infinite;
}

.loading-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.loading-title {
  font-size: 14px;
  font-weight: 700;
  color: #333;
}

.loading-desc {
  font-size: 12px;
  color: #888;
}

.loading-progress {
  width: 100%;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: #E8E4FF;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #7c6df8, #9d8df8);
  border-radius: 2px;
  animation: progress 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(0.95); }
}

@keyframes progress {
  0% { width: 0%; }
  50% { width: 70%; }
  100% { width: 100%; }
}
</style>
