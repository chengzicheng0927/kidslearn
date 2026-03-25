<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import ParentMessageBubble from './ParentMessageBubble.vue'
import TwemojiText from '../TwemojiText.vue'
import type { ParentMessage } from '../../types/parent'

defineEmits<{
  (e: 'suggestion', text: string): void
  (e: 'action', action: string, message: ParentMessage): void
}>()

const props = defineProps<{
  messages: ParentMessage[]
}>()

const scrollRef = ref<HTMLElement | null>(null)

// 自动滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (scrollRef.value) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight
    }
  })
}

// 监听消息变化，自动滚动
watch(
  () => props.messages.length,
  () => {
    scrollToBottom()
  },
  { deep: true }
)
</script>

<template>
  <div ref="scrollRef" class="timeline">
    <!-- 欢迎消息 -->
    <div v-if="messages.length === 0" class="welcome">
      <div class="welcome-title">欢迎来到家长模式</div>
      <div class="welcome-desc">
        在这里您可以为孩子制定学习计划。<br/>
        请告诉我您想要什么样的计划？
      </div>
      <div class="suggestion-chips">
        <button class="chip" @click="$emit('suggestion', '帮我制定一个14天的英语学习计划')">
          <TwemojiText html="📚" /> 14天英语计划
        </button>
        <button class="chip" @click="$emit('suggestion', '帮我制定一个7天的数学学习计划')">
          <TwemojiText html="🔢" /> 7天数学计划
        </button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div v-else class="messages">
      <ParentMessageBubble
        v-for="msg in messages"
        :key="msg.id"
        :message="msg"
        @action="(action, msg) => $emit('action', action, msg)"
      />
    </div>
  </div>
</template>

<style scoped>
.timeline {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  scroll-behavior: smooth;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  text-align: center;
  padding: 24px;
}

.welcome-title {
  font-size: 18px;
  font-weight: 800;
  color: #1a1a2e;
}

.welcome-desc {
  font-size: 14px;
  color: #888;
  line-height: 1.6;
}

.suggestion-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 8px;
}

.chip {
  padding: 8px 14px;
  border-radius: 20px;
  border: 1.5px solid #7c6df8;
  background: transparent;
  color: #7c6df8;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.chip:active {
  background: #7c6df8;
  color: white;
  transform: scale(0.96);
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
