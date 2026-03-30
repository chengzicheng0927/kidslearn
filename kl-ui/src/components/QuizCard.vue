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
  readonly?: boolean   // 历史记录模式，不播放答对/答错动画
}>(), {
  activeSubject: 'en',
  readonly: false,
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
  const handler = () => {
    speechSynthesis.removeEventListener('voiceschanged', handler)
    doSpeak(text, lang)
  }
  speechSynthesis.addEventListener('voiceschanged', handler)
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

// 选项颜色配置（依次循环）
const optionColors = [
  { bg: 'linear-gradient(135deg, #dcedc8, #f1f8e9)', shadow: '#c5e1a5', label: 'A' },
  { bg: 'linear-gradient(135deg, #b3e5fc, #e1f5fe)', shadow: '#81d4fa', label: 'B' },
  { bg: 'linear-gradient(135deg, #ffe0b2, #fff3e0)', shadow: '#ffcc80', label: 'C' },
  { bg: 'linear-gradient(135deg, #f8bbd0, #fce4ec)', shadow: '#f48fb1', label: 'D' },
]

function getOptionColor(index: number) {
  return optionColors[index % optionColors.length]
}
</script>

<template>
  <div class="qcard subject-en" :class="['subject-' + activeSubject, { readonly: props.readonly }]">
    <!-- 听题按钮 -->
    <button class="speak-btn" @click.stop="speak(quiz.question, activeSubject === 'en' ? 'en-US' : 'zh-CN')">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
        <path d="M5 9H8L13 5V19L8 15H5V9Z" stroke="white" stroke-width="2" stroke-linejoin="round"/>
        <path d="M16 8.5C17.8 9.9 19 11.9 19 14C19 16.1 17.8 18.1 16 19.5" stroke="white" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </button>

    <!-- 标签行 -->
    <div class="qlbl">
      <span>{{ quiz.label }}</span>
      <span class="quiz-stars">+{{ quiz.reward?.stars ?? 2 }}⭐</span>
    </div>

    <!-- 撕纸卡片：狐狸 + 气泡 + 选项全部在里面 -->
    <div class="torn-card">
      <!-- 狐狸 + 气泡同排 -->
      <div class="fox-bubble-row">
        <div class="fox-area">
          <img class="fox-img" src="/fox.png" alt="小狐狸" />
        </div>
        <div class="speech-area">
          <div class="speech-bubble">
            <div class="fox-label">小狐狸说：</div>
            <div class="question-text">{{ quiz.question }}</div>
          </div>
        </div>
      </div>

      <!-- 选项 -->
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
          :style="!readonly && !answered && !isEvaluating && answeredAnswerId === undefined ? {
            background: getOptionColor(idx).bg,
            boxShadow: `2px 5px 10px rgba(0,0,0,0.08), -1px 2px 4px rgba(0,0,0,0.03), inset 0 1px 0 rgba(255,255,255,0.6)`,
          } : {}"
          @click="answeredAnswerId !== undefined ? () => {} : pick(option, idx)"
          @touchstart="startLongPress(option)"
          @touchend="cancelLongPress"
          @touchcancel="cancelLongPress"
        >
          <span class="opt-main">
            <span class="opt-label">{{ getOptionColor(idx).label }}.</span>
            <span class="opt-text">{{ option.text }}</span>
          </span>
          <span v-if="option.emoji" class="opt-em">{{ option.emoji }}</span>
        </div>
      </div>
    </div>

    <!-- 评判中动画 -->
    <ThinkingModal v-if="isEvaluating" />

    <!-- 结果弹窗 -->
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
/* 整体卡片 */
.qcard {
  margin: 0 0 6px;
  position: relative;
  padding-top: 2px;
}

/* 标签行 */
.qlbl {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
  font-weight: 600;
  color: #B0B8C4;
  margin-bottom: 8px;
}

.quiz-stars {
  font-size: 11px;
  font-weight: 700;
  color: #FFB300;
}

/* ========== 撕纸卡片（狐狸 + 气泡 + 选项全部在里面） ========== */
.torn-card {
  background: #FFF;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  border: 2px solid #f0f0f0;
  box-shadow: 0 8px 30px rgba(0,0,0,0.08);
  padding: 14px 16px 16px;
  margin-bottom: 0;
  overflow: visible;
  position: relative;
}

/* 狐狸 + 气泡同排 */
.fox-bubble-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}

/* 狐狸 */
.fox-area {
  width: 36%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.fox-img {
  width: 100%;
  max-width: 100px;
  object-fit: contain;
  filter: drop-shadow(2px 4px 6px rgba(0,0,0,0.1));
}

/* ========== 气泡 ========== */
.speech-area {
  flex: 1;
  min-width: 0;
  position: relative;
}

/* 气泡主体 */
.speech-bubble {
  background: #FFF;
  border: 1.5px solid #E0D9C8;
  border-radius: 16px 6px 16px 6px;
  padding: 10px 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  position: relative;
}

/* 气泡尖角（朝左指向狐狸） */
.speech-bubble::before {
  content: '';
  position: absolute;
  left: -9px;
  top: 14px;
  width: 0;
  height: 0;
  border-top: 5px solid transparent;
  border-right: 8px solid #E0D9C8;
  border-bottom: 0px solid transparent;
}

.speech-bubble::after {
  content: '';
  position: absolute;
  left: -7px;
  top: 15px;
  width: 0;
  height: 0;
  border-top: 4px solid transparent;
  border-right: 6px solid #FFF;
  border-bottom: 0px solid transparent;
}

.fox-label {
  font-size: 11px;
  font-weight: 700;
  color: #F57C00;
  margin-bottom: 4px;
}

.question-text {
  font-size: 15px;
  font-weight: 600;
  color: #3A3A3A;
  line-height: 1.5;
}

/* ========== 听题按钮 ========== */
.speak-btn {
  position: absolute;
  top: 36px;
  right: 14px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #FFF;
  border: 1.5px solid #E0D9C8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s, background 0.2s;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.speak-btn:active {
  transform: scale(0.9);
  background: #F5F0E0;
}

.speak-btn svg path {
  stroke: #B0A890;
}

/* ========== 选项区 ========== */
.qopts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.qo {
  border-radius: 8px;
  background: #FAFAFA;
  font-size: 15px;
  font-weight: 700;
  color: #3A3A3A;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.2s, background 0.2s;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border: 2px solid rgba(0,0,0,0.05);
}

.qo:active:not(.ok):not(.no):not(.is-disabled) {
  transform: scale(0.96);
}

@media (hover: hover) and (pointer: fine) {
  .qo:hover:not(.ok):not(.no):not(.is-disabled) {
    transform: scale(1.02);
  }
}

.qo.is-disabled {
  cursor: wait;
  opacity: 0.7;
}

.opt-main {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.opt-label {
  font-weight: 700;
  color: #5D4037;
}

.opt-text {
  color: #3A3A3A;
  word-break: break-word;
}

.opt-em {
  font-size: 16px;
  line-height: 1;
  flex-shrink: 0;
  color: #5D4037;
}

/* 答对 */
.qo.ok {
  background: linear-gradient(135deg, #c8e6c9, #e8f5e9) !important;
  color: #2E7D32;
  border-color: #81C784;
  animation: popIn 0.3s ease;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.25) !important;
}

/* 答错 */
.qo.no {
  background: linear-gradient(135deg, #ffcdd2, #ffebee) !important;
  color: #C62828;
  border-color: #EF9A9A;
  animation: shakeIn 0.3s ease;
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.2) !important;
}

/* readonly 模式下：保留颜色，去掉动画 */
.qcard.readonly .qo.ok,
.qcard.readonly .qo.no {
  animation: none;
}

@keyframes popIn {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

@keyframes shakeIn {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-4px); }
  40% { transform: translateX(4px); }
  60% { transform: translateX(-3px); }
  80% { transform: translateX(3px); }
}
</style>
