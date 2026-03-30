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
    <!-- 粒子背景 -->
    <div class="particles">
      <div class="p p1"></div>
      <div class="p p2"></div>
      <div class="p p3"></div>
      <div class="p p4"></div>
      <div class="p p5"></div>
      <div class="p p6"></div>
      <div class="p p7"></div>
      <div class="p p8"></div>
      <div class="p p9"></div>
      <div class="p p10"></div>
      <div class="p p11"></div>
      <div class="p p12"></div>
    </div>

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
          :readonly="true"
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
  position: relative;
  z-index: 0;
}

/* ========== 粒子背景 ========== */
.particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.p {
  position: absolute;
  border-radius: 50%;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  opacity: 0;
  animation: float linear infinite;
}

@keyframes float {
  0% {
    transform: translateY(0) rotate(0deg) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 0.7;
  }
  80% {
    opacity: 0.5;
  }
  100% {
    transform: translateY(-100vh) rotate(360deg) scale(0.6);
    opacity: 0;
  }
}

.p1 { width: 8px;  height: 8px;  left: 8%;  bottom: -10px; background: rgba(255, 228, 160, 0.6); animation-duration: 8s;  animation-delay: 0s;    }
.p2 { width: 6px;  height: 6px;  left: 20%; bottom: -10px; background: rgba(255, 183, 178, 0.6); animation-duration: 10s; animation-delay: 1s;    }
.p3 { width: 10px; height: 10px; left: 35%; bottom: -10px; background: rgba(179, 229, 252, 0.6); animation-duration: 9s;  animation-delay: 0.5s;  }
.p4 { width: 5px;  height: 5px;  left: 50%; bottom: -10px; background: rgba(200, 240, 208, 0.6); animation-duration: 12s; animation-delay: 2s;    }
.p5 { width: 7px;  height: 7px;  left: 65%; bottom: -10px; background: rgba(255, 224, 178, 0.6); animation-duration: 8s;  animation-delay: 1.5s;  }
.p6 { width: 9px;  height: 9px;  left: 78%; bottom: -10px; background: rgba(206, 147, 216, 0.5); animation-duration: 11s; animation-delay: 0.8s;  }
.p7 { width: 6px;  height: 6px;  left: 15%; bottom: -10px; background: rgba(128, 203, 196, 0.5); animation-duration: 9s;  animation-delay: 3s;    }
.p8 { width: 8px;  height: 8px;  left: 42%; bottom: -10px; background: rgba(255, 202, 40,  0.5); animation-duration: 10s; animation-delay: 0.3s;  }
.p9 { width: 5px;  height: 5px;  left: 88%; bottom: -10px; background: rgba(255, 138, 101, 0.5); animation-duration: 7s;  animation-delay: 2.5s;  }
.p10 { width: 7px; height: 7px; left: 28%; bottom: -10px; background: rgba(144, 202, 249, 0.5); animation-duration: 9s;  animation-delay: 1.2s;  }
.p11 { width: 6px; height: 6px; left: 58%; bottom: -10px; background: rgba(220, 231, 117, 0.5); animation-duration: 11s; animation-delay: 0.7s;  }
.p12 { width: 10px;height: 10px;left: 72%; bottom: -10px; background: rgba(186, 104, 200, 0.4); animation-duration: 8s;  animation-delay: 3.5s;  }

.chat {
  padding: 4px 16px 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
  z-index: 1;
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
