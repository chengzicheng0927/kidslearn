<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import EyeCareReminder from '../components/EyeCareReminder.vue'
import RestReminderModal from '../components/RestReminderModal.vue'
import StarFlyAnimation from '../components/StarFlyAnimation.vue'
import MilestoneModal from '../components/MilestoneModal.vue'
import ParentModeView from './ParentModeView.vue'
import {
  getNextQuestion,
  registerDeviceToken,
  type Question,
} from '../services/api'
import { useAppStore } from '../stores/appStore'
import ChatHeader from '../components/ChatHeader.vue'
import ChatTimeline from '../components/ChatTimeline.vue'
import InputBar from '../components/InputBar.vue'
import VoiceOverlay from '../components/VoiceOverlay.vue'
import ComboFireBar from '../components/ComboFireBar.vue'
import DailyTaskCard from '../components/DailyTaskCard.vue'
import CalendarView from './CalendarView.vue'
import PetView from './PetView.vue'
import type { AnsweredQuestion, ChatRecord } from '../types/chat'
import {
  loadSubjectRecord,
  saveSubjectRecord,
  createEmptyRecord,
  appendAnswerToRecord,
  type SubjectQuizRecord,
} from '../types/chat'
import {
  onNativeEvent,
  requestPushPermission,
  startSpeech,
  stopSpeech,
  finishSpeech
} from '../utils/nativeBridge'

const activeSubject = ref<'en' | 'ma'>('en')
const appStore = useAppStore()
const { child, quizSession, achievementState, setPetEmotion, teacherName, dailyTasks, syncCalendar, calendarData } = appStore
const { showPasswordModal, showParentMode } = storeToRefs(appStore)
const { openPasswordModal } = appStore

// 小星老师表情状态
const teacherExpression = ref<'idle' | 'thinking' | 'happy'>('idle')
const teacherEvaluating = ref(false)

// 监听答题状态变化 → 老师表情
watch(() => quizSession.answeredCount, () => {
  teacherExpression.value = 'happy'
  setTimeout(() => { teacherExpression.value = 'idle' }, 2500)
})

function onTeacherEvaluating(isEvaluating: boolean) {
  teacherEvaluating.value = isEvaluating
  if (isEvaluating) {
    teacherExpression.value = 'thinking'
  }
}

// 当前学科的答题记录
const currentRecord = ref<SubjectQuizRecord | null>(null)

// 当前题目
const currentQuestion = ref<Question | null>(null)
const quizComplete = ref(false)
const quizLoading = ref(true)

// quizHistory 来自当前学科记录
const quizHistory = computed(() => currentRecord.value?.questions ?? [])

const messages = ref<ChatRecord[]>([])
const messageDraft = ref('')
const isRecording = ref(false)

// Voice overlay state
const showVoiceOverlay = ref(false)
const voiceStatus = ref<'recording' | 'success' | 'error'>('recording')
const recognizedText = ref('')
let voiceDoneTimer: number | null = null

// 护眼提示：每答完 10 题触发
const showEyeCareReminder = ref(false)
const lastEyeCareTriggeredAt = ref(0)

watch(() => quizSession.answeredCount, (count) => {
  if (count > 0 && count % 10 === 0 && count !== lastEyeCareTriggeredAt.value) {
    lastEyeCareTriggeredAt.value = count
    showEyeCareReminder.value = true
  }
})

// 休息提醒：连续学习 15 分钟无交互触发
const REST_INTERVAL = 15 * 60 * 1000 // 15分钟
const showRestReminder = ref(false)
let lastActivityTime = 0
let restTimer: number | null = null
let restReminderShown = false // 本次学习段是否已显示过

function resetRestTimer() {
  lastActivityTime = Date.now()
  restReminderShown = false
  if (restTimer === null) {
    startRestTimer()
  }
}

function startRestTimer() {
  if (restTimer !== null) return
  restTimer = window.setInterval(() => {
    if (lastActivityTime === 0) return
    const elapsed = Date.now() - lastActivityTime
    if (elapsed >= REST_INTERVAL && !restReminderShown) {
      restReminderShown = true
      showRestReminder.value = true
    }
  }, 1000)
}

function stopRestTimer() {
  if (restTimer !== null) {
    clearInterval(restTimer)
    restTimer = null
  }
}

// 宠物心情系统
watch(() => quizSession.combo, (combo) => {
  if (combo >= 5) {
    setPetEmotion('happy')
  } else if (combo >= 3) {
    setPetEmotion('clingy')
  }
})

// 积分飞升动画
const showStarFly = ref(false)
const starFlyCount = ref(1)
const lastAnsweredRef = ref(0)

watch(() => quizSession.correctCount, (correct) => {
  if (correct > lastAnsweredRef.value) {
    starFlyCount.value = Math.min(5, correct - lastAnsweredRef.value)
    showStarFly.value = true
  }
  lastAnsweredRef.value = correct
})

// 里程碑动画
const milestoneInfo = ref<{ type: 'achievement' | 'badge' | 'evolution'; icon: string; title: string; desc: string } | null>(null)
const lastTotalRef = ref(0)

watch(() => achievementState.totalQuestions, (total) => {
  if (total > lastTotalRef.value) {
    lastTotalRef.value = total
  }
})
// View state for navigation
const currentView = ref<'chat' | 'calendar' | 'pet'>('chat')

// Combo 连击追踪
const comboBarRef = ref<InstanceType<typeof ComboFireBar> | null>(null)

// 同步 body 背景到场景色（body 背景延伸覆盖刘海区域）
function syncBodyBackground(subject?: 'en' | 'ma') {
  if (subject === 'en') {
    document.body.style.background = 'linear-gradient(160deg, #E3F2FD 0%, #BBDEFB 40%, #FFF8F3 100%)'
  } else if (subject === 'ma') {
    document.body.style.background = 'linear-gradient(160deg, #E8F5E9 0%, #C8E6C9 40%, #FFF8F3 100%)'
  } else {
    // 日历/宠物面板：恢复默认
    document.body.style.background = ''
  }
}

// 监听视图切换，同步 body 背景
watch(currentView, (view) => {
  if (view === 'calendar' || view === 'pet') {
    document.body.style.background = '#FFF8F3'
  } else if (view === 'chat') {
    syncBodyBackground(activeSubject.value)
  }
  // 滑动到日历页时从后端加载日历数据
  if (view === 'calendar') {
    const childId = child?.id
    if (childId) {
      const now = new Date()
      syncCalendar(childId, now.getFullYear(), now.getMonth() + 1)
    }
  }
})

// 监听家长模式开关
watch(() => showParentMode, (show) => {
  if (show) {
    document.body.style.background = 'linear-gradient(135deg, #7c6df8, #9d8df8)'
  } else {
    syncBodyBackground(activeSubject.value)
  }
})

// 监听学科变化：保存当前记录 → 加载新学科记录
watch(activeSubject, (subject) => {
  // 如果记录还没初始化（onMounted 还没执行），忽略
  if (!currentRecord.value) return
  document.documentElement.setAttribute('data-scene', subject === 'en' ? 'en' : 'math')
  syncBodyBackground(subject)
  // 先清掉旧题目，避免闪现旧题
  currentQuestion.value = null
  quizComplete.value = false
  // 保存旧学科记录
  saveSubjectRecord(currentRecord.value)
  // 加载新学科记录
  const record = loadSubjectRecord(subject)
  currentRecord.value = record ?? createEmptyRecord(subject)
  loadNextQuestion()
})

let cleanupSpeechResult: (() => void) | null = null
let cleanupSpeechError: (() => void) | null = null
let cleanupPushPermResult: (() => void) | null = null
let cleanupPushToken: (() => void) | null = null
let cleanupPushOpen: (() => void) | null = null

function goToChat() {
  currentView.value = 'chat'
}

function goToCalendar() {
  currentView.value = 'calendar'
}

function goToPet() {
  currentView.value = 'pet'
}

// Touch handling
let touchStartX = 0

function onTouchStart(e: TouchEvent) {
  // 如果触摸来自走马灯内部，忽略（走马灯有横向滚动）
  const target = e.target as HTMLElement
  if (target.closest('.carousel-wrap, .ci-item')) return
  touchStartX = e.touches[0].clientX
}

function onTouchEnd(e: TouchEvent) {
  // 如果触摸来自走马灯内部，忽略
  const target = e.target as HTMLElement
  if (target.closest('.carousel-wrap, .ci-item')) return

  const touchEndX = e.changedTouches[0].clientX
  const diffX = touchEndX - touchStartX
  const threshold = window.innerWidth * 0.25

  if (Math.abs(diffX) < threshold) return

  if (diffX < 0 && currentView.value === 'chat') {
    currentView.value = 'pet'
  } else if (diffX > 0 && currentView.value === 'chat') {
    currentView.value = 'calendar'
  } else if (diffX > 0 && currentView.value === 'pet') {
    currentView.value = 'chat'
  } else if (diffX < 0 && currentView.value === 'calendar') {
    currentView.value = 'chat'
  }
}

// 加载下一题（基于当前记录的 questionIndex）
async function loadNextQuestion() {
  if (!currentRecord.value) return
  quizLoading.value = true
  const idx = currentRecord.value.questionIndex
  const question = await getNextQuestion(activeSubject.value, idx)
  if (question) {
    currentQuestion.value = question
    quizComplete.value = false
  } else {
    currentQuestion.value = null
    quizComplete.value = true
  }
  quizLoading.value = false
}

function handleNext(data: { question: Question; selectedAnswerId: string; isCorrect: boolean; correctAnswerId: string; starsEarned?: number }) {
  if (!currentRecord.value) return
  resetRestTimer() // 重置休息计时器
  const answered: AnsweredQuestion = {
    question: data.question,
    selectedAnswerId: data.selectedAnswerId,
    isCorrect: data.isCorrect,
    correctAnswerId: data.correctAnswerId,
  }
  const starsEarned = data.starsEarned ?? (data.isCorrect ? 2 : 0)
  currentRecord.value = appendAnswerToRecord(currentRecord.value, answered, starsEarned)
  saveSubjectRecord(currentRecord.value)
  // 老师思考弹窗已销毁，答对才弹弹幕
  if (data.isCorrect && quizSession.combo >= 2) {
    comboBarRef.value?.push(quizSession.combo)
  }
  loadNextQuestion()
}

function handleComplete(data?: { question: Question; selectedAnswerId: string; isCorrect: boolean; correctAnswerId: string; starsEarned?: number }) {
  if (data && currentRecord.value) {
    const answered: AnsweredQuestion = {
      question: data.question,
      selectedAnswerId: data.selectedAnswerId,
      isCorrect: data.isCorrect,
      correctAnswerId: data.correctAnswerId,
    }
    const starsEarned = data.starsEarned ?? (data.isCorrect ? 2 : 0)
    currentRecord.value = appendAnswerToRecord(currentRecord.value, answered, starsEarned)
    saveSubjectRecord(currentRecord.value)
  }
  quizComplete.value = true
  currentQuestion.value = null
}

function handleSend(message: string) {
  resetRestTimer() // 重置休息计时器
  messages.value.push({
    id: `msg-${Date.now()}-${messages.value.length}`,
    role: 'user',
    content: message,
    source: isRecording.value ? 'voice' : 'typed',
  })
  isRecording.value = false
}

function handleClearSession() {
  if (!currentRecord.value) return
  // 清空答题记录：从头开始，界面不再渲染历史答题
  const fresh = createEmptyRecord(activeSubject.value)
  currentRecord.value = fresh
  saveSubjectRecord(fresh)
  quizComplete.value = false
  // 重置题目索引（关键！否则清空后无法获取新题目）
  quizSession.value.currentQuestionIndex = 0
  // 重置连击
  quizSession.value.combo = 0
  quizSession.value.correctCount = 0
  quizSession.value.answeredCount = 0
  // 重置每日任务进度（今日答题数、今日星星）
  dailyTasks.enDone = 0
  dailyTasks.maDone = 0
  // 重置孩子的星星和今日分钟数
  if (child) {
    child.stars = 0
    child.todayMinutes = 0
  }
  loadNextQuestion()
}

function handleVoice() {
  if (isRecording.value) {
    stopSpeech()
    isRecording.value = false
    showVoiceOverlay.value = false
    return
  }

  const started = startSpeech('zh-CN')
  if (started) {
    isRecording.value = true
    voiceStatus.value = 'recording'
    recognizedText.value = ''
    showVoiceOverlay.value = true
  }
}

function handleVoiceCancel() {
  if (voiceDoneTimer) {
    clearTimeout(voiceDoneTimer)
    voiceDoneTimer = null
  }
  stopSpeech()
  isRecording.value = false
  showVoiceOverlay.value = false
  recognizedText.value = ''
}

function handleVoiceDone() {
  if (voiceDoneTimer) return // 防止重复调用

  // 如果还没有任何识别内容，立即关闭
  if (!recognizedText.value.trim()) {
    finishSpeech()
    isRecording.value = false
    showVoiceOverlay.value = false
    recognizedText.value = ''
    return
  }

  // 告诉 iOS 完成录音，iOS 会发送最终识别结果
  finishSpeech()

  // 如果 3 秒内没有最终结果，自动关闭遮罩
  voiceDoneTimer = window.setTimeout(() => {
    voiceDoneTimer = null
    isRecording.value = false
    showVoiceOverlay.value = false
    recognizedText.value = ''
  }, 3000)
}

onMounted(() => {
  // 预热 Web Speech 引擎，避免首次朗读失败
  if ('speechSynthesis' in window) {
    const handler = () => { speechSynthesis.getVoices() }
    speechSynthesis.addEventListener('voiceschanged', handler)
    speechSynthesis.getVoices()
    // 500ms 后移除监听器（voices 应该已缓存）
    setTimeout(() => speechSynthesis.removeEventListener('voiceschanged', handler), 500)
    // 部分浏览器需要延迟再调用一次
    setTimeout(() => speechSynthesis.getVoices(), 200)
  }

  cleanupSpeechResult = onNativeEvent<{ text: string; isFinal: boolean }>('onSpeechResult', ({ text, isFinal }) => {
    recognizedText.value = text
    if (isFinal) {
      voiceStatus.value = 'success'
      if (text.trim()) {
        setTimeout(() => {
          if (voiceDoneTimer) {
            clearTimeout(voiceDoneTimer)
            voiceDoneTimer = null
          }
          handleSend(text)
          messageDraft.value = ''
          isRecording.value = false
          showVoiceOverlay.value = false
          recognizedText.value = ''
        }, 500)
      } else {
        if (voiceDoneTimer) {
          clearTimeout(voiceDoneTimer)
          voiceDoneTimer = null
        }
        isRecording.value = false
        showVoiceOverlay.value = false
        recognizedText.value = ''
      }
    }
  })

  cleanupSpeechError = onNativeEvent<{ message?: string }>('onSpeechError', ({ message }) => {
    if (voiceDoneTimer) {
      clearTimeout(voiceDoneTimer)
      voiceDoneTimer = null
    }
    voiceStatus.value = 'error'
    if (message) {
      messages.value.push({
        id: `msg-${Date.now()}-${messages.value.length}`,
        role: 'ai',
        content: `语音识别暂时不可用：${message}`,
        source: 'system',
      })
    }
    isRecording.value = false
    showVoiceOverlay.value = false
    recognizedText.value = ''
  })

  cleanupPushPermResult = onNativeEvent<{ granted: boolean }>('onPushPermResult', ({ granted }) => {
    if (!granted) {
      messages.value.push({
        id: `msg-${Date.now()}-${messages.value.length}`,
        role: 'ai',
        content: '通知权限未开启，后续可能收不到学习提醒哦。',
        source: 'system',
      })
    }
  })

  cleanupPushToken = onNativeEvent<{ token: string }>('onPushToken', async ({ token }) => {
    const result = await registerDeviceToken(token)
    if (result.success) {
      messages.value.push({
        id: `msg-${Date.now()}-${messages.value.length}`,
        role: 'ai',
        content: '设备提醒已连接成功，学习提醒准备就绪。',
        source: 'system',
      })
    }
  })

  cleanupPushOpen = onNativeEvent<{ subject?: 'en' | 'ma' }>(
    'onPushOpen',
    ({ subject }) => {
      // 只切换到聊天视图，不显示欢迎回来消息
      currentView.value = 'chat'
      if (subject) {
        activeSubject.value = subject
      }
    }
  )

  // 初始化当前学科记录
  const record = loadSubjectRecord(activeSubject.value)
  currentRecord.value = record ?? createEmptyRecord(activeSubject.value)
  quizComplete.value = false
  document.documentElement.setAttribute('data-scene', activeSubject.value === 'en' ? 'en' : 'math')
  syncBodyBackground(activeSubject.value)
  loadNextQuestion()
  requestPushPermission()
  // 启动休息计时器
  resetRestTimer()
})

onUnmounted(() => {
  if (isRecording.value) {
    stopSpeech()
  }
  cleanupSpeechResult?.()
  cleanupSpeechError?.()
  cleanupPushPermResult?.()
  cleanupPushToken?.()
  cleanupPushOpen?.()
  stopRestTimer()
})
</script>

<template>
  <div
    class="app"
    @touchstart="onTouchStart"
    @touchend="onTouchEnd"
  >
    <!-- Calendar Panel - positioned to the left, slides in when active -->
    <div
      class="panel calendar-panel"
      :class="{ active: currentView === 'calendar' }"
    >
      <CalendarView @go-chat="goToChat" />
    </div>

    <!-- Pet Panel - positioned to the right, slides in when active -->
    <div
      class="panel pet-panel"
      :class="{ active: currentView === 'pet' }"
    >
      <PetView @go-chat="goToChat" />
    </div>

    <!-- Chat Panel - always visible, slides to reveal other panels -->
    <div
      class="panel chat-panel"
      :class="{
        'slide-left': currentView === 'pet',
        'slide-right': currentView === 'calendar'
      }"
    >
      <ChatHeader />

      <!-- Combo 火焰条 -->
      <ComboFireBar ref="comboBarRef" />

      <!-- 场景背景 -->

      <!-- 每日任务卡片 -->
      <DailyTaskCard
        :active-subject="activeSubject"
        @switch="activeSubject = $event"
      />

      <ChatTimeline
        :child-name="child?.name ?? '小朋友'"
        :teacher-name="teacherName"
        :active-subject="activeSubject"
        :quiz-loading="quizLoading"
        :quiz-history="quizHistory"
        :quiz-complete="quizComplete"
        :current-question="currentQuestion"
        :messages="messages"
        @next="handleNext"
        @complete="handleComplete"
        @evaluating="onTeacherEvaluating"
      />

      <div class="dots">
        <div class="dot"></div>
        <div class="dot active"></div>
        <div class="dot"></div>
      </div>

      <InputBar
        v-model:model-value="messageDraft"
        :is-recording="isRecording"
        :placeholder="`和${teacherName}说话…`"
        @send="handleSend"
        @voice="handleVoice"
        @open-calendar="goToCalendar"
        @open-parent="openPasswordModal"
        @open-pet="goToPet"
        @clear-session="handleClearSession"
      />
    </div>

  </div>

  <VoiceOverlay
    :visible="showVoiceOverlay"
    :status="voiceStatus"
    :recognized-text="recognizedText"
    @cancel="handleVoiceCancel"
    @done="handleVoiceDone"
  />

  <!-- 护眼提示 -->
  <EyeCareReminder
    :visible="showEyeCareReminder"
    @dismiss="showEyeCareReminder = false"
  />

  <!-- 休息提醒：连续学习15分钟 -->
  <RestReminderModal
    :visible="showRestReminder"
    @dismiss="showRestReminder = false"
    @rest="showRestReminder = false"
  />

  <!-- 积分飞升动画 -->
  <StarFlyAnimation
    :visible="showStarFly"
    :count="starFlyCount"
    @done="showStarFly = false"
  />

  <!-- 里程碑弹窗 -->
  <MilestoneModal
    v-if="milestoneInfo"
    :visible="!!milestoneInfo"
    :type="milestoneInfo.type"
    :icon="milestoneInfo.icon"
    :title="milestoneInfo.title"
    :desc="milestoneInfo.desc"
    @done="milestoneInfo = null"
  />

  <!-- 家长模式：固定覆盖 -->
  <ParentModeView
    v-if="showParentMode"
    @close="showParentMode = false"
  />

</template>

<style scoped>
.app {
  width: 100%;
  height: 100%;
  min-height: 100%;
  position: relative;
  overflow: hidden;
  z-index: 0;
}

.panel {
  position: absolute;
  inset: 0;
  transition: transform 0.4s cubic-bezier(0.34, 1.1, 0.64, 1);
}

/* Calendar panel - hidden to the left by default */
.calendar-panel {
  transform: translateX(-100%);
  z-index: 10;
  background: var(--bg);
}

.calendar-panel.active {
  transform: translateX(0);
  z-index: 25;
}

/* Pet panel - hidden to the right by default */
.pet-panel {
  transform: translateX(100%);
  z-index: 10;
  background: var(--bg);
}

.pet-panel.active {
  transform: translateX(0);
  z-index: 25;
}

/* Chat panel - center, slides to reveal other panels */
.chat-panel {
  z-index: 20;
  background: var(--scene-bg);
  display: flex;
  flex-direction: column;
  transition: transform 0.4s cubic-bezier(0.34, 1.1, 0.64, 1);
}

.chat-panel.slide-left {
  transform: translateX(-75%);
}

.chat-panel.slide-right {
  transform: translateX(75%);
}

/* 场景头部布局 */
.scene-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px 10px;
  flex-shrink: 0;
}

.dots {
  display: flex;
  justify-content: center;
  gap: 5px;
  padding: 6px 0 2px;
  flex-shrink: 0;
}

.dot {
  height: 6px;
  border-radius: 3px;
  transition: all 0.3s;
  background: var(--border);
}

.dot.active {
  width: 20px;
  background: var(--or);
}
</style>
