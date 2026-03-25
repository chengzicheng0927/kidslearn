<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { Question } from '../services/api'
import type { AnsweredQuestion, ChatRecord } from '../types/chat'
import QuizCard from './QuizCard.vue'

const props = defineProps<{
  childName: string
  teacherName: string
  activeSubject: 'en' | 'ma'
  quizLoading: boolean
  quizHistory: AnsweredQuestion[]
  quizComplete: boolean
  currentQuestion: Question | null
  messages: ChatRecord[]
}>()

const emit = defineEmits<{
  (e: 'next', data: { question: Question; selectedAnswerId: string; isCorrect: boolean; correctAnswerId: string }): void
  (e: 'complete', data?: { question: Question; selectedAnswerId: string; isCorrect: boolean; correctAnswerId: string }): void
  (e: 'evaluating', isEvaluating: boolean): void
}>()

const scrollContainer = ref<HTMLElement | null>(null)

function scrollToBottom() {
  nextTick(() => {
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
    }
  })
}

watch(
  () => [
    props.quizLoading,
    props.quizHistory.length,
    props.quizComplete,
    props.currentQuestion?.id ?? '',
    props.messages.length,
  ],
  () => {
    scrollToBottom()
  },
  { immediate: true }
)
</script>

<template>
  <div ref="scrollContainer" class="chat-scroll">
    <div class="chat">

      <!-- 加载中 -->
      <div v-if="quizLoading" class="loading-card">
        <div class="loading-dot"></div>
        <div class="loading-dot"></div>
        <div class="loading-dot"></div>
      </div>

      <!-- 历史题目 -->
      <template v-for="(item, idx) in quizHistory" :key="`history-${item.question.id}`">
        <div v-if="idx === 0" class="greeting-card" :class="'greeting-' + activeSubject">
          <span class="teacher-tag">{{ teacherName }}</span>
          <span>Hello! 今天状态不错哦，我们继续练习吧～</span>
        </div>
        <QuizCard
          :quiz="item.question"
          :active-subject="activeSubject"
          :answered-answer-id="item.selectedAnswerId"
          :correct-answer-id="item.correctAnswerId"
          @next="() => {}"
          @complete="() => {}"
        />
      </template>

      <!-- 今日完成（只有真正答完题后才显示） -->
      <div v-if="quizComplete && quizHistory.length > 0" class="greeting-card" :class="'greeting-' + activeSubject">
        <span class="teacher-tag">{{ teacherName }}</span>
        <span>太棒了！你已经完成了今天的全部答题！继续保持哦～</span>
      </div>

      <!-- 当前题目 -->
      <template v-else-if="currentQuestion">
        <!-- 老师引导语 -->
        <div class="greeting-card" :class="'greeting-' + activeSubject">
          <span class="teacher-tag">{{ teacherName }}</span>
          <span v-if="quizHistory.length === 0">Hello! 今天状态不错哦，我们开始练习吧～</span>
          <span v-else>第{{ quizHistory.length + 1 }}题，继续加油！</span>
        </div>
        <Transition name="quiz" mode="out-in">
          <QuizCard
            v-if="currentQuestion"
            :key="`current-${currentQuestion.id}`"
            :quiz="currentQuestion"
            :active-subject="activeSubject"
            @next="emit('next', $event)"
            @complete="emit('complete', $event)"
            @evaluating="emit('evaluating', $event)"
          />
        </Transition>
      </template>

      <!-- 用户文字消息（如果有） -->
      <div
        v-for="message in messages"
        :key="message.id"
        class="user-card"
      >
        {{ message.content }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}

.chat {
  padding: 4px 16px 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 加载中 */
.loading-card {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 18px;
  background: var(--card);
  border-radius: 14px;
  align-self: flex-start;
}

.loading-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #B0B8C4;
  animation: dotBounce 1.2s ease-in-out infinite;
}

.loading-dot:nth-child(2) { animation-delay: 0.2s; }
.loading-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* 老师引导卡片 */
.greeting-card {
  background: #F5F7FA;
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--txt);
  line-height: 1.55;
  align-self: flex-start;
  display: flex;
  flex-direction: column;
  gap: 3px;
  animation: slideIn 0.3s ease;
}

.greeting-card.greeting-en .teacher-tag {
  color: #29B6F6;
}

.greeting-card.greeting-ma .teacher-tag {
  color: #27AE60;
}

.teacher-tag {
  font-size: 11px;
  font-weight: 700;
}

/* 用户消息卡片 */
.user-card {
  background: #E8F4FF;
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--txt);
  line-height: 1.55;
  align-self: flex-end;
  animation: slideIn 0.3s ease;
}

/* 题目卡片动画 */
.quiz-enter-active {
  animation: quizSlideIn 0.35s cubic-bezier(0.34, 1.2, 0.64, 1);
}
.quiz-leave-active {
  animation: quizSlideOut 0.25s ease-in forwards;
}

@keyframes quizSlideIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes quizSlideOut {
  from { opacity: 1; transform: translateY(0); }
  to { opacity: 0; transform: translateY(-8px); }
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
