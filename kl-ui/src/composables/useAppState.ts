import { computed, ref } from 'vue'
import {
  createInitialCalendarData,
  createInitialPetState,
  createInitialBadgeState,
  createInitialAchievementState,
  mockAchievements,
  type Achievement,
  type AchievementState,
  type BadgeState,
  type Child,
  type MonthData,
  type PetEmotion,
  type PetFoodItem,
  type PetState,
  type Question,
  type StudyDayData,
  type StudySubjectRecord,
} from '../mock/data'

const TEACHER_NAME_KEY = 'kidslearn_teacher_name'

interface QuizSessionState {
  currentQuestionIndex: number
  answeredCount: number
  correctCount: number
  combo: number
}

interface DailyTaskState {
  enTarget: number
  maTarget: number
  enDone: number
  maDone: number
}

const child = ref<Child | null>(null)
const calendarData = ref<Record<string, MonthData>>(createInitialCalendarData())
const petState = ref<PetState>(createInitialPetState())
const teacherName = ref<string>(localStorage.getItem(TEACHER_NAME_KEY) ?? '小星老师')
const quizSession = ref<QuizSessionState>({
  currentQuestionIndex: 0,
  answeredCount: 0,
  correctCount: 0,
  combo: 0,
})

const dailyTasks = ref<DailyTaskState>({
  enTarget: 5,
  maTarget: 3,
  enDone: 0,
  maDone: 0,
})

const badgeState = ref<BadgeState>(createInitialBadgeState())
const achievementState = ref<AchievementState>(createInitialAchievementState())

// 家长模式
const showPasswordModal = ref(false)
const showParentMode = ref(false)
function openParentMode() {
  showPasswordModal.value = false
  showParentMode.value = true
}
function closeParentMode() {
  showParentMode.value = false
}

function setChild(nextChild: Child | null) {
  child.value = nextChild ? { ...nextChild } : null
}

export function setTeacherName(name: string) {
  teacherName.value = name
  localStorage.setItem(TEACHER_NAME_KEY, name)
}

function resetMockState() {
  child.value = null
  calendarData.value = createInitialCalendarData()
  petState.value = createInitialPetState()
  badgeState.value = createInitialBadgeState()
  achievementState.value = createInitialAchievementState()
  resetQuizSession()
}

function resetQuizSession() {
  quizSession.value = {
    currentQuestionIndex: 0,
    answeredCount: 0,
    correctCount: 0,
    combo: 0,
  }
  dailyTasks.value = {
    enTarget: 5,
    maTarget: 3,
    enDone: 0,
    maDone: 0,
  }
}

function consumeNextQuestionIndex() {
  const index = quizSession.value.currentQuestionIndex
  quizSession.value.currentQuestionIndex += 1
  return index
}

function getTodayKey(date = new Date()) {
  return `${date.getFullYear()}-${date.getMonth() + 1}`
}

function ensureTodayRecord(subjectName: string, date = new Date()) {
  const key = getTodayKey(date)
  const day = date.getDate()
  const month = calendarData.value[key] ?? {}

  if (!month[day]) {
    month[day] = {
      heat: 0,
      minutes: 0,
      subjects: [],
    }
  }

  const subject = month[day].subjects.find((item) => item.name === subjectName)
  if (!subject) {
    month[day].subjects.push(createSubjectRecord(subjectName))
  }

  calendarData.value[key] = month
  return month[day]
}

function createSubjectRecord(subjectName: string): StudySubjectRecord {
  if (subjectName === '数学') {
    return {
      icon: '🔢',
      name: '数学',
      topic: '今日练习',
      stars: 0,
      accuracy: 0,
      color: '#69F0AE',
      bg: 'rgba(105,240,174,.12)',
    }
  }

  return {
    icon: '📖',
    name: '英语',
    topic: '今日练习',
    stars: 0,
    accuracy: 0,
    color: '#40C4FF',
    bg: 'rgba(64,196,255,.12)',
  }
}

function recordAnswer(question: Question, isCorrect: boolean) {
  quizSession.value.answeredCount += 1

  // 更新成就统计
  achievementState.value.totalQuestions += 1
  if (isCorrect) {
    achievementState.value.currentStreak += 1
    achievementState.value.todayCorrect += 1
    achievementState.value.bestStreak = Math.max(achievementState.value.bestStreak, achievementState.value.currentStreak)
  } else {
    achievementState.value.currentStreak = 0
  }
  achievementState.value.todayTotal += 1

  if (question.subject === 'en') {
    dailyTasks.value.enDone = Math.min(dailyTasks.value.enTarget, dailyTasks.value.enDone + 1)
  } else {
    dailyTasks.value.maDone = Math.min(dailyTasks.value.maTarget, dailyTasks.value.maDone + 1)
  }

  if (isCorrect) {
    quizSession.value.correctCount += 1
    quizSession.value.combo += 1
  } else {
    quizSession.value.combo = 0
  }

  if (!child.value || !isCorrect) {
    return
  }

  child.value = {
    ...child.value,
    stars: child.value.stars + question.reward.stars,
    todayMinutes: child.value.todayMinutes + question.estimatedMinutes,
  }

  const subjectName = question.subject === 'ma' ? '数学' : '英语'
  const todayRecord = ensureTodayRecord(subjectName)
  todayRecord.minutes += question.estimatedMinutes
  todayRecord.heat = Math.min(4, Math.max(1, Math.ceil(todayRecord.minutes / 15)))

  const subjectRecord = todayRecord.subjects.find((item) => item.name === subjectName)
  if (subjectRecord) {
    subjectRecord.topic = question.label.split(' · ')[1] ?? question.label
    subjectRecord.stars += question.reward.stars
    subjectRecord.accuracy = Math.round((quizSession.value.correctCount / quizSession.value.answeredCount) * 100)
  }
}

// 获取新解锁的成就（与上一次状态对比）
function getNewlyEarnedAchievements(previousTotal: number): Achievement[] {
  return mockAchievements.filter((a) => {
    if (achievementState.value.earnedAchievements.includes(a.id)) return false
    switch (a.type) {
      case 'total_questions':
        return achievementState.value.totalQuestions >= a.requirement
      case 'streak_days':
        return achievementState.value.currentStreak >= a.requirement || achievementState.value.bestStreak >= a.requirement
      case 'accuracy_rate':
        if (previousTotal === 0) return false
        return (achievementState.value.todayCorrect / Math.max(1, achievementState.value.todayTotal)) * 100 >= a.requirement
      default:
        return false
    }
  })
}

function earnAchievement(achievementId: string) {
  if (!achievementState.value.earnedAchievements.includes(achievementId)) {
    achievementState.value.earnedAchievements.push(achievementId)
  }
}

function feedPet(item: PetFoodItem): boolean {
  if (!child.value || item.locked || child.value.stars < item.cost) {
    return false
  }

  child.value = {
    ...child.value,
    stars: child.value.stars - item.cost,
  }

  const targetStat = petState.value.stats[item.statIndex]
  if (targetStat) {
    targetStat.value = Math.min(100, targetStat.value + 8)
  }

  return true
}

function setPetEvolution(index: number) {
  if (index >= petState.value.evolutionIndex) {
    petState.value.evolutionIndex = index
  }
}

function setPetEmotion(emotion: PetEmotion) {
  petState.value.emotion = emotion
}

const childStars = computed(() => child.value?.stars ?? 0)
const childStreak = computed(() => child.value?.streak ?? 0)
const childTodayMinutes = computed(() => child.value?.todayMinutes ?? 0)

export function useAppState() {
  return {
    child,
    childStars,
    childStreak,
    childTodayMinutes,
    calendarData,
    petState,
    teacherName,
    quizSession,
    dailyTasks,
    badgeState,
    achievementState,
    showPasswordModal,
    showParentMode,
    setChild,
    setTeacherName,
    resetMockState,
    resetQuizSession,
    consumeNextQuestionIndex,
    recordAnswer,
    feedPet,
    setPetEvolution,
    setPetEmotion,
    getNewlyEarnedAchievements,
    earnAchievement,
    openParentMode,
    closeParentMode,
  }
}

export function getSelectedStudyDay(
  data: Record<string, MonthData>,
  year: number,
  month: number,
  day: number
): StudyDayData | null {
  return data[`${year}-${month}`]?.[day] ?? null
}
