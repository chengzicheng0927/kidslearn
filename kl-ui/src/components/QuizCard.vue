<script setup lang="ts">
import { ref, watch } from 'vue'
import { submitAnswer, type AnswerResult, type Question } from '../services/api'
import ResultModal from './ResultModal.vue'
import ThinkingModal from './ThinkingModal.vue'

const props = withDefaults(defineProps<{
  quiz: Question
  answeredAnswerId?: string
  correctAnswerId?: string
  activeSubject?: 'en' | 'ma'
}>(), {
  activeSubject: 'en',
})

const isSpeaking = ref(false)

function doSpeak(text: string, lang: string) {
  if (isSpeaking.value) speechSynthesis.cancel()
  const safeText = text.trim().replace(/[。.！!?！?]$/, '') + '。'
  const utterance = new SpeechSynthesisUtterance(safeText)
  utterance.lang = lang
  utterance.rate = 0.7
  utterance.pitch = 1.0
  utterance.volume = 1.0
  const voices = speechSynthesis.getVoices()
  const targetVoice = voices.find(v => v.lang === lang)
    || voices.find(v => v.lang.startsWith(lang.split('-')[0]))
  if (targetVoice) utterance.voice = targetVoice
  utterance.onstart = () => { isSpeaking.value = true }
  utterance.onend = () => { isSpeaking.value = false }
  utterance.onerror = () => { isSpeaking.value = false }
  speechSynthesis.speak(utterance)
}

function speak(text: string, lang = 'zh-CN') {
  if (!('speechSynthesis' in window)) return
  const voices = speechSynthesis.getVoices()
  if (voices.length > 0) {
    doSpeak(text, lang)
    return
  }
  // voices 尚未加载，监听 voiceschanged 事件
  const handler = () => {
    speechSynthesis.removeEventListener('voiceschanged', handler)
    doSpeak(text, lang)
  }
  speechSynthesis.addEventListener('voiceschanged', handler)
  // 同时在 500ms 后重试（兜底，确保不卡死）
  setTimeout(() => {
    speechSynthesis.removeEventListener('voiceschanged', handler)
    const v2 = speechSynthesis.getVoices()
    if (v2.length > 0) doSpeak(text, lang)
  }, 500)
}

function stopSpeaking() {
  if ('speechSynthesis' in window) speechSynthesis.cancel()
  isSpeaking.value = false
}

watch(() => props.quiz.id, () => {
  stopSpeaking()
})

const emit = defineEmits<{
  (e: 'next', data: { question: Question; selectedAnswerId: string; isCorrect: boolean; correctAnswerId: string; starsEarned: number }): void
  (e: 'complete', data?: { question: Question; selectedAnswerId: string; isCorrect: boolean; correctAnswerId: string; starsEarned: number }): void
  (e: 'evaluating', isEvaluating: boolean): void
}>()

const answered = ref(false)
const selectedIndex = ref<number | null>(null)
const showResult = ref(false)
const startTime = ref(Date.now())
const resultData = ref<AnswerResult | null>(null)
const resolvedCorrectAnswerId = ref<string | null>(null)
const lastAnswerData = ref<{ selectedAnswerId: string; isCorrect: boolean; correctAnswerId: string } | null>(null)
const pendingComplete = ref(false)
const isEvaluating = ref(false)
const lastPickTime = ref(0)

// 长按朗读
let longPressTimer: ReturnType<typeof setTimeout> | null = null
const LONG_PRESS_DURATION = 800
const longPressOption = ref<Question['options'][number] | null>(null)

function startLongPress(option: Question['options'][number]) {
  longPressOption.value = option
  longPressTimer = setTimeout(() => {
    speak(option.text, props.activeSubject === 'en' ? 'en-US' : 'zh-CN')
    longPressTimer = null
    longPressOption.value = null
  }, LONG_PRESS_DURATION)
}

function cancelLongPress() {
  if (longPressTimer) {
    clearTimeout(longPressTimer)
    longPressTimer = null
  }
  longPressOption.value = null
}

function getCorrectAnswerText(correctAnswerId: string) {
  return props.quiz.options.find((option) => option.id === correctAnswerId)?.text
}

async function pick(option: Question['options'][number], index: number) {
  if (answered.value || isEvaluating.value) return
  const now = Date.now()
  if (now - lastPickTime.value < 300) return
  lastPickTime.value = now
  answered.value = true
  selectedIndex.value = index
  isEvaluating.value = true
  emit('evaluating', true)

  const judgeStartedAt = Date.now()
  const timeSpent = Math.round((Date.now() - startTime.value) / 1000)
  const response = await submitAnswer(props.quiz.id, option.id, timeSpent, props.activeSubject)
  const minJudgeDuration = 1500
  const elapsed = Date.now() - judgeStartedAt
  const remainingJudgeDuration = Math.max(0, minJudgeDuration - elapsed)
  if (remainingJudgeDuration > 0) {
    await new Promise((resolve) => setTimeout(resolve, remainingJudgeDuration))
  }

  resolvedCorrectAnswerId.value = response.correctAnswerId
  isEvaluating.value = false
  emit('evaluating', false)

  lastAnswerData.value = {
    selectedAnswerId: option.id,
    isCorrect: response.isCorrect,
    correctAnswerId: response.correctAnswerId
  }
  const starsEarned = response.isCorrect ? (props.quiz.reward?.stars ?? 2) : 0

  if (response.showResult) {
    resultData.value = response
    showResult.value = true
    if (!response.hasMore) {
      pendingComplete.value = true
    }
  } else if (response.hasMore) {
    resetState()
    emit('next', { question: props.quiz, ...lastAnswerData.value, starsEarned })
    lastAnswerData.value = null
  } else {
    resetState()
    emit('complete', { question: props.quiz, ...lastAnswerData.value, starsEarned })
    lastAnswerData.value = null
  }
}

function resetState() {
  answered.value = false
  selectedIndex.value = null
  showResult.value = false
  resultData.value = null
  resolvedCorrectAnswerId.value = null
  isEvaluating.value = false
  startTime.value = Date.now()
}

function resetAllState() {
  resetState()
  lastAnswerData.value = null
  pendingComplete.value = false
}

watch(() => props.quiz.id, () => {
  resetAllState()
})

function handleNext() {
  const isLastQuestion = pendingComplete.value
  pendingComplete.value = false
  showResult.value = false
  resultData.value = null
  resolvedCorrectAnswerId.value = null
  answered.value = false
  selectedIndex.value = null
  isEvaluating.value = false
  startTime.value = Date.now()
  const starsEarned = (lastAnswerData.value?.isCorrect ? (props.quiz.reward?.stars ?? 2) : 0)
  if (!isLastQuestion && lastAnswerData.value) {
    emit('next', { question: props.quiz, ...lastAnswerData.value, starsEarned })
  }
  if (isLastQuestion) {
    emit('complete', lastAnswerData.value ? { question: props.quiz, ...lastAnswerData.value, starsEarned } : undefined)
  }
  lastAnswerData.value = null
}
</script>

<template>
  <div class="qcard" :class="'subject-' + activeSubject">
    <div class="qlbl">
      <span>{{ quiz.label }}</span>
      <span class="quiz-stars">+{{ quiz.reward?.stars ?? 2 }}⭐</span>
    </div>

    <div class="qq-wrap">
      <div class="qq">{{ quiz.question }}</div>
      <button class="speak-btn" @click.stop="speak(quiz.question, activeSubject === 'en' ? 'en-US' : 'zh-CN')">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
          <path d="M5 9H8L13 5V19L8 15H5V9Z" stroke="white" stroke-width="2" stroke-linejoin="round"/>
          <path d="M16 8.5C17.8 9.9 19 11.9 19 14C19 16.1 17.8 18.1 16 19.5" stroke="white" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
    </div>

    <ThinkingModal v-if="isEvaluating" />

    <div class="qopts">
      <div
        v-for="(option, idx) in quiz.options"
        :key="idx"
        class="qo"
        :class="{
          'is-disabled': isEvaluating,
          'ok': answeredAnswerId !== undefined
            ? props.correctAnswerId === option.id
            : (!isEvaluating && answered && resolvedCorrectAnswerId !== null && resolvedCorrectAnswerId === option.id),
          'no': answeredAnswerId !== undefined
            ? answeredAnswerId === option.id && props.correctAnswerId !== undefined && answeredAnswerId !== props.correctAnswerId
            : (!isEvaluating && answered && selectedIndex === idx && resolvedCorrectAnswerId !== null && resolvedCorrectAnswerId !== option.id)
        }"
        @click="answeredAnswerId !== undefined ? () => {} : pick(option, idx)"
        @touchstart="startLongPress(option)"
        @touchend="cancelLongPress"
        @touchcancel="cancelLongPress"
      >
        <div v-if="option.emoji" class="opt-em">{{ option.emoji }}</div>
        <span class="opt-text">{{ option.text }}</span>
      </div>
    </div>

    <ResultModal
      v-if="showResult && resultData"
      :is-correct="resultData.isCorrect"
      :result-mode="resultData.resultMode"
      :combo="resultData.result?.combo ?? 0"
      :accuracy="resultData.result?.accuracy ?? 0"
      :time-spent="resultData.result?.timeSpent ?? 0"
      :xp="resultData.result?.xp ?? 0"
      :xp-needed="resultData.result?.xpNeeded ?? 100"
      :level="resultData.result?.level ?? 1"
      :stars="lastAnswerData?.isCorrect ? (props.quiz.reward?.stars ?? 2) : 0"
      :reward-icon="resultData.result?.reward?.icon"
      :reward-title="resultData.result?.reward?.title"
      :reward-sub="resultData.result?.reward?.sub"
      :explanation="resultData.explanation"
      :explanation-steps="resultData.explanationSteps"
      :encouragement="resultData.encouragement"
      :correct-answer-text="getCorrectAnswerText(resultData.correctAnswerId)"
      @next="handleNext"
    />
  </div>
</template>

<style scoped>
.qcard {
  background: var(--card);
  border-radius: 16px;
  padding: 14px 16px 12px;
  margin: 0 0 6px;
}

.qlbl {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
  font-weight: 600;
  color: #B0B8C4;
  margin-bottom: 6px;
}

.quiz-stars {
  font-size: 11px;
  font-weight: 700;
  color: #FFB300;
}

.qq-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.qq {
  font-size: 16px;
  font-weight: 600;
  color: var(--txt);
  line-height: 1.5;
  flex: 1;
  min-width: 0;
}

/* 听题按钮 */
.speak-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #E8F4FF;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s, transform 0.15s;
}

.speak-btn:active {
  transform: scale(0.92);
  background: #D0E8FF;
}

.qcard .speak-btn svg path {
  stroke: #40C4FF;
}

/* 选项 */
.qopts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.qo {
  border-radius: 10px;
  background: #FAFAFA;
  font-size: 14px;
  font-weight: 500;
  color: var(--txt);
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 10px 8px;
  text-align: center;
}

.qo:active:not(.ok):not(.no):not(.is-disabled) {
  transform: scale(0.97);
  background: #F0F0F0;
}

@media (hover: hover) and (pointer: fine) {
  .qo:hover:not(.ok):not(.no):not(.is-disabled) {
    background: #F0F0F0;
  }
}

.qo.is-disabled {
  cursor: wait;
}

.opt-em {
  font-size: 18px;
  line-height: 1;
  flex-shrink: 0;
}

.opt-text {
  flex: 1;
  text-align: center;
  min-width: 0;
  word-break: break-word;
}

/* 答对 */
.qo.ok {
  background: #E8F5E9;
  color: #2E7D32;
  animation: popIn 0.3s ease;
}

/* 答错 */
.qo.no {
  background: #FFEBEE;
  color: #C62828;
  animation: shakeIn 0.3s ease;
}

@keyframes popIn {
  0% { transform: scale(1); }
  50% { transform: scale(1.04); }
  100% { transform: scale(1); }
}

@keyframes shakeIn {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-3px); }
  75% { transform: translateX(3px); }
}
</style>
