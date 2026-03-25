<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useAppStore } from '../stores/appStore'
import TwemojiText from './TwemojiText.vue'

const props = defineProps<{
  isCorrect: boolean
  resultMode?: 'correct' | 'wrong' | 'reward'
  combo: number
  accuracy: number
  timeSpent: number
  xp: number
  xpNeeded: number
  level: number
  stars: number
  rewardIcon?: string
  rewardTitle?: string
  rewardSub?: string
  explanation?: string
  explanationSteps?: string[]
  encouragement?: string
  correctAnswerText?: string
}>()

const emit = defineEmits<{
  (e: 'next'): void
}>()

const store = useAppStore()
const { child, todayStars, todayMaxStars } = storeToRefs(store)

const isExplaining = ref(false)

function stopExplanation() {
  if ('speechSynthesis' in window) speechSynthesis.cancel()
  isExplaining.value = false
}

function doSpeakExplanation() {
  if (isExplaining.value) speechSynthesis.cancel()

  const text = props.explanationSteps?.length
    ? `正确答案：${props.correctAnswerText}。${props.explanationSteps.join('。')}`
    : props.explanation
    ? `正确答案：${props.correctAnswerText}。${props.explanation}`
    : `正确答案：${props.correctAnswerText}。${props.encouragement ?? ''}`

  const safeText = text.trim().replace(/[。.！!?！?]$/, '') + '。'
  const utterance = new SpeechSynthesisUtterance(safeText)
  utterance.lang = 'zh-CN'
  utterance.rate = 0.7
  utterance.pitch = 1.0
  utterance.volume = 1.0

  const voices = speechSynthesis.getVoices()
  const zhVoice = voices.find(v => v.lang === 'zh-CN')
    || voices.find(v => v.lang.startsWith('zh'))
  if (zhVoice) utterance.voice = zhVoice

  utterance.onstart = () => { isExplaining.value = true }
  utterance.onend = () => { isExplaining.value = false }
  utterance.onerror = () => { isExplaining.value = false }
  speechSynthesis.speak(utterance)
}

function speakExplanation() {
  if (!('speechSynthesis' in window)) return

  const voices = speechSynthesis.getVoices()
  if (voices.length > 0) {
    doSpeakExplanation()
    return
  }

  const handler = () => {
    speechSynthesis.removeEventListener('voiceschanged', handler)
    doSpeakExplanation()
  }
  speechSynthesis.addEventListener('voiceschanged', handler)
  setTimeout(() => {
    speechSynthesis.removeEventListener('voiceschanged', handler)
    const v2 = speechSynthesis.getVoices()
    if (v2.length > 0) doSpeakExplanation()
  }, 500)
}

const resolvedMode = computed(() => {
  if (props.resultMode) return props.resultMode
  return props.isCorrect ? 'correct' : 'wrong'
})

const isReward = computed(() => resolvedMode.value === 'reward')
const isWrong = computed(() => resolvedMode.value === 'wrong')
const isCorrect = computed(() => resolvedMode.value === 'correct')
const childName = computed(() => child?.name ?? '小朋友')
const actionLabel = computed(() => (isWrong.value ? '继续' : '继续下一题'))
const titleText = computed(() => {
  if (isReward.value) return `太棒了${childName.value}！`
  return isWrong.value ? '哎呀，答错了' : '太棒了！答对了！'
})
const subtitleText = computed(() => {
  if (isReward.value) return '连续答对触发奖励，快来收下这份鼓励'
  return isWrong.value ? '没关系，我来帮你讲解' : '小星老师给你点个大大的赞'
})

// Confetti colors - fresh and light
const confettiColors = ['#FFD54F', '#FF8A65', '#4FC3F7', '#81C784', '#BA68C8', '#FF7043', '#40C4FF']
</script>

<template>
  <Teleport to="body">
    <!-- Backdrop -->
    <div class="result-backdrop" @click="emit('next')"></div>

    <!-- Confetti (only for correct) -->
    <div v-if="isCorrect || isReward" class="confetti-container">
      <div v-for="i in 20" :key="i" class="confetti" :style="{
        left: `${Math.random() * 100}%`,
        backgroundColor: confettiColors[i % confettiColors.length],
        animationDelay: `${Math.random() * 0.5}s`,
        animationDuration: `${1.5 + Math.random() * 1}s`
      }"></div>
    </div>

    <!-- Bottom Sheet -->
    <div class="result-sheet">
      <!-- Handle -->
      <div class="result-handle"></div>

      <!-- Header -->
      <div class="result-header" :class="{ 'header-wrong': isWrong }">
        <div class="status-icon-wrapper">
          <svg v-if="isCorrect || isReward" viewBox="0 0 44 44" fill="none" width="44" height="44">
            <path
              d="M8 22L18 32L36 12"
              stroke="white"
              stroke-width="4.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <svg v-else viewBox="0 0 36 36" fill="none" width="36" height="36">
            <path
              d="M10 10L26 26M26 10L10 26"
              stroke="white"
              stroke-width="4"
              stroke-linecap="round"
            />
          </svg>
        </div>
        <h1 class="result-title">{{ titleText }}</h1>
        <p class="result-subtitle">{{ subtitleText }}</p>
      </div>

      <!-- Correct State -->
      <div v-if="isCorrect || isReward" class="result-content">
        <!-- Stats Pills -->
        <div class="result-stats-row">
          <div class="stat-pill star-pill">
            <span class="stat-icon">⭐</span>
            <span class="stat-value">+2</span>
          </div>
          <div class="stat-pill xp-pill">
            <span class="stat-icon">⚡</span>
            <span class="stat-value">+{{ xp }}</span>
          </div>
          <div v-if="combo > 1" class="stat-pill combo-pill">
            <span class="stat-icon">🔥</span>
            <span class="stat-value">{{ combo }}连击</span>
          </div>
        </div>

        <!-- XP Progress -->
        <div class="result-xp-block">
          <div class="xp-header">
            <span class="xp-label">等级 {{ level }}</span>
            <span class="xp-value">{{ xp }} / {{ xpNeeded }} XP</span>
          </div>
          <div class="xp-track">
            <div class="xp-fill" :style="{ width: `${Math.min(100, (xp / Math.max(1, xpNeeded)) * 100)}%` }"></div>
          </div>
        </div>
      </div>

      <!-- Wrong State -->
      <div v-else class="result-content">
        <!-- Answer Bubble -->
        <div class="result-answer-bubble">
          <div class="bubble-label">正确答案是</div>
          <div class="bubble-answer">{{ correctAnswerText }}</div>
        </div>

        <!-- Explanation Card -->
        <div class="result-explain-card">
          <div class="explain-header">
            <span class="explain-icon">💡</span>
            <span class="explain-label">解析</span>
          </div>
          <div class="explain-text">{{ explanation || explanationSteps?.[0] || '' }}</div>
          <button class="explain-speak-btn" @click.stop="speakExplanation">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M5 9H8L13 5V19L8 15H5V9Z" stroke="#FF7043" stroke-width="2" stroke-linejoin="round"/>
              <path d="M16 8.5C17.8 9.9 19 11.9 19 14C19 16.1 17.8 18.1 16 19.5" stroke="#FF7043" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <span>听老师讲解</span>
          </button>
        </div>
      </div>

      <!-- Continue Button -->
      <div class="result-action-wrapper">
        <button class="result-action-btn" :class="{ 'is-wrong': isWrong }" @click="stopExplanation(); emit('next')">
          {{ actionLabel }}
        </button>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
/* Animations */
@keyframes sheetIn {
  from { transform: translateY(100%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

@keyframes backdropIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes confettiFall {
  0% {
    transform: translateY(-20px) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(400px) rotate(720deg);
    opacity: 0;
  }
}

/* Backdrop */
.result-backdrop {
  position: fixed;
  inset: 0;
  z-index: var(--z-overlay, 1000);
  background: rgba(0, 0, 0, .35);
  animation: backdropIn .25s ease;
}

/* Confetti */
.confetti-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  pointer-events: none;
  z-index: calc(var(--z-overlay, 1000) + 1);
  overflow: hidden;
}

.confetti {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  animation: confettiFall 2s ease-out forwards;
}

/* Bottom Sheet */
.result-sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: calc(var(--z-overlay, 1000) + 2);
  width: 100%;
  max-width: 430px;
  margin: 0 auto;
  background: #fff;
  border-radius: 20px 20px 0 0;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  animation: sheetIn .38s cubic-bezier(.32, 1.2, .64, 1);
  overflow: hidden;
}

/* Handle */
.result-handle {
  display: none;
}

/* Header */
.result-header {
  padding: 12px 20px 16px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  background: #F0FAF0;
}

.result-header.header-wrong {
  background: #FFF5F0;
}

.status-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}

.result-header:not(.header-wrong) .status-icon-wrapper {
  background: #B8E6C1;
  box-shadow: 0 6px 20px rgba(64, 196, 255, 0.2);
}

.result-header.header-wrong .status-icon-wrapper {
  background: #FFCCBC;
  box-shadow: 0 6px 20px rgba(255, 112, 67, 0.2);
}

.result-title {
  font-size: 22px;
  font-weight: 800;
  color: #1A1A1A;
  line-height: 1.2;
  margin-bottom: 4px;
}

.result-subtitle {
  font-size: 14px;
  color: #888;
  font-weight: 400;
}

/* Content */
.result-content {
  padding: 0 20px 20px;
  flex: 1;
}

/* Stats Pills */
.result-stats-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  justify-content: center;
}

.stat-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 700;
}

.star-pill {
  background: #FFF5C0;
  color: #8B6914;
  box-shadow: 0 4px 12px rgba(255, 179, 0, 0.15);
}

.xp-pill {
  background: #B3E5FC;
  color: #0277BD;
  box-shadow: 0 4px 12px rgba(79, 195, 247, 0.2);
}

.combo-pill {
  background: #FFE0B2;
  color: #BF360C;
  box-shadow: 0 4px 12px rgba(255, 112, 67, 0.15);
}

.stat-icon {
  font-size: 16px;
}

/* XP Block */
.result-xp-block {
  background: #F8F8F8;
  border-radius: 14px;
  padding: 14px;
}

.xp-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.xp-label {
  font-size: 13px;
  font-weight: 600;
  color: #1A1A1A;
}

.xp-value {
  font-size: 12px;
  color: #666;
}

.xp-track {
  height: 6px;
  background: #E8E8E8;
  border-radius: 3px;
  overflow: hidden;
}

.xp-fill {
  height: 100%;
  background: #81D4FA;
  border-radius: 3px;
  transition: width .35s ease;
}

/* Answer Bubble - Child Friendly */
.result-answer-bubble {
  background: linear-gradient(135deg, #E3F2FD, #BBDEFB);
  border-radius: 16px;
  padding: 14px 18px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 2px solid #90CAF9;
}

.bubble-label {
  font-size: 14px;
  color: #1565C0;
  font-weight: 500;
}

.bubble-answer {
  font-size: 17px;
  font-weight: 700;
  color: #1565C0;
}

/* Explanation Card */
.result-explain-card {
  background: #FFF8F5;
  border-radius: 14px;
  padding: 14px;
  border: 1.5px solid #F5E0D6;
}

.explain-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.explain-icon {
  font-size: 18px;
}

.explain-label {
  font-size: 14px;
  font-weight: 600;
  color: #FF7043;
}

.explain-text {
  font-size: 14px;
  color: #5D4037;
  line-height: 1.6;
  margin-bottom: 12px;
}

.explain-speak-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 20px;
  background: #FFF3E0;
  border: 1.5px solid #FFE0B2;
  font-size: 14px;
  font-weight: 600;
  color: #E65100;
  cursor: pointer;
  transition: all .15s;
}

.explain-speak-btn:active {
  transform: scale(.97);
  background: #FFE0B2;
}

/* Action Button */
.result-action-wrapper {
  padding: 0 20px 40px;
}

.result-action-btn {
  width: 100%;
  height: 50px;
  border-radius: 25px;
  border: none;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all .18s;
}

.result-action-btn:not(.is-wrong) {
  background: #90EE90;
  color: #2E7D32;
  box-shadow: 0 6px 20px rgba(64, 196, 255, 0.2);
}

.result-action-btn:not(.is-wrong):active {
  transform: scale(.97);
  background: #7FCC7F;
}

.result-action-btn.is-wrong {
  background: #FFCCBC;
  color: #BF360C;
  box-shadow: 0 6px 20px rgba(255, 112, 67, 0.2);
}

.result-action-btn.is-wrong:active {
  transform: scale(.97);
  background: #FFBB99;
}
</style>
