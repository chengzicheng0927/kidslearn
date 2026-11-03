import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { API_CONFIG } from '../config/api.config'
import {
  createInitialCalendarData,
  createInitialPetState,
  createInitialBadgeState,
  createInitialAchievementState,
  mockAchievements,
  mockKnowledgeBadges,
  mockPetEvolutions,
  mockPetFoodItems,
  mockQuestions,
  type Achievement,
  type AchievementState,
  type BadgeState,
  type Child,
  type KnowledgeBadge,
  type MonthData,
  type PetEmotion,
  type PetEvolution,
  type PetFoodItem,
  type PetState,
  type Question,
  type StudyDayData,
  type StudySubjectRecord,
} from '../mock/data'
import { getAchievements, getBadges, getPetState, getMonthCalendar, getCalendarStats, feedPet as apiFeedPet } from '../services/api'

const TEACHER_NAME_KEY = 'kidslearn_teacher_name'
const MOCK_CHILD_KEY = 'kidslearn_mock_child'
const TODAY_STARS_KEY = 'kidslearn_today_stars'
const TODAY_DATE_KEY = 'kidslearn_today_date'

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function persistChild(child: Child) {
  if (!canUseStorage()) return
  window.localStorage.setItem(MOCK_CHILD_KEY, JSON.stringify(child))
}

function getTodayDate() {
  const d = new Date()
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}

function loadTodayStars(): number {
  if (!canUseStorage()) return 0
  const savedDate = window.localStorage.getItem(TODAY_DATE_KEY)
  const today = getTodayDate()
  if (savedDate !== today) {
    window.localStorage.setItem(TODAY_DATE_KEY, today)
    window.localStorage.setItem(TODAY_STARS_KEY, '0')
    return 0
  }
  return parseInt(window.localStorage.getItem(TODAY_STARS_KEY) ?? '0', 10)
}

function saveTodayStars(stars: number) {
  if (!canUseStorage()) return
  window.localStorage.setItem(TODAY_DATE_KEY, getTodayDate())
  window.localStorage.setItem(TODAY_STARS_KEY, String(stars))
}

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

export const useAppStore = defineStore('app', () => {
  const child = ref<Child | null>(null)
  // 非 mock 模式下，初始化为空数据，禁止使用 mock 填充日历
  const calendarData = ref<Record<string, MonthData>>(
    API_CONFIG.USE_MOCK ? createInitialCalendarData() : {}
  )
  const calendarStats = ref<{
    totalDays: number; totalMinutes: number; totalQuestions: number;
    totalStars: number; currentStreak: number; longestStreak: number;
  } | null>(null)
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

  // 静态元数据（客户端常量，API 不提供）
  const achievementDefinitions = ref<Achievement[]>(mockAchievements)
  const badgeDefinitions = ref<KnowledgeBadge[]>(mockKnowledgeBadges)
  const petEvolutions = ref<PetEvolution[]>(mockPetEvolutions)
  const petFoodItems = ref<PetFoodItem[]>(mockPetFoodItems)

  // 今日星星（按天重置）
  const todayStars = ref<number>(loadTodayStars())

  // 最大星星：
  // - Mock 模式：从题库计算
  // - Real 模式：初始化为 0，由 getNextQuestion 的后端 summary 实时更新
  function calcTodayMaxStars(): number {
    if (!API_CONFIG.USE_MOCK) return 0
    const enQuestions = mockQuestions.filter((q) => q.subject === 'en')
    const maQuestions = mockQuestions.filter((q) => q.subject === 'ma')
    const enTarget = dailyTasks.value?.enTarget || 5
    const maTarget = dailyTasks.value?.maTarget || 3
    const enReward = enQuestions.slice(0, enTarget).reduce((sum, q) => sum + (q.reward?.stars || 0), 0)
    const maReward = maQuestions.slice(0, maTarget).reduce((sum, q) => sum + (q.reward?.stars || 0), 0)
    return enReward + maReward
  }
  const todayMaxStars = ref<number>(calcTodayMaxStars())

  // 家长模式
  const showPasswordModal = ref(false)
  const showParentMode = ref(false)

  function openPasswordModal() {
    showPasswordModal.value = true
  }

  function closePasswordModal() {
    showPasswordModal.value = false
  }

  function openParentMode() {
    showPasswordModal.value = false
    showParentMode.value = true
  }

  function closeParentMode() {
    showParentMode.value = false
  }

  function setChild(nextChild: Child | null) {
    child.value = nextChild ? { ...nextChild } : null
    if (nextChild) {
      if (nextChild.dailyTargetEn !== undefined) dailyTasks.value.enTarget = nextChild.dailyTargetEn
      if (nextChild.dailyTargetMa !== undefined) dailyTasks.value.maTarget = nextChild.dailyTargetMa
    }
  }

  function setTeacherName(name: string) {
    teacherName.value = name
    localStorage.setItem(TEACHER_NAME_KEY, name)
  }

  function resetMockState() {
    child.value = null
    // 非 mock 模式下不清空日历数据，只清除会话状态
    calendarData.value = API_CONFIG.USE_MOCK ? createInitialCalendarData() : {}
    petState.value = API_CONFIG.USE_MOCK ? createInitialPetState() : petState.value
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
    todayStars.value = loadTodayStars()
    todayMaxStars.value = calcTodayMaxStars()
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
      achievementState.value.bestStreak = Math.max(
        achievementState.value.bestStreak,
        achievementState.value.currentStreak
      )
    } else {
      achievementState.value.currentStreak = 0
    }
    achievementState.value.todayTotal += 1

    // Real 模式下 dailyTasks 由后端 summary 同步，不在此增量更新
    if (API_CONFIG.USE_MOCK) {
      if (question.subject === 'en') {
        dailyTasks.value.enDone = Math.min(
          dailyTasks.value.enTarget,
          dailyTasks.value.enDone + 1
        )
      } else {
        dailyTasks.value.maDone = Math.min(
          dailyTasks.value.maTarget,
          dailyTasks.value.maDone + 1
        )
      }
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

    const earnedStars = question.reward.stars
    child.value = {
      ...child.value,
      stars: child.value.stars + earnedStars,
      todayMinutes: child.value.todayMinutes + question.estimatedMinutes,
    }
    persistChild(child.value)

    // 今日星星累加
    todayStars.value += earnedStars
    saveTodayStars(todayStars.value)

    const subjectName = question.subject === 'ma' ? '数学' : '英语'
    const todayRecord = ensureTodayRecord(subjectName)
    todayRecord.minutes += question.estimatedMinutes
    todayRecord.heat = Math.min(
      4,
      Math.max(1, Math.ceil(todayRecord.minutes / 15))
    )

    const subjectRecord = todayRecord.subjects.find(
      (item) => item.name === subjectName
    )
    if (subjectRecord) {
      subjectRecord.topic = question.label.split(' · ')[1] ?? question.label
      subjectRecord.stars += question.reward.stars
      subjectRecord.accuracy = Math.round(
        (quizSession.value.correctCount / quizSession.value.answeredCount) * 100
      )
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
          return (
            achievementState.value.currentStreak >= a.requirement ||
            achievementState.value.bestStreak >= a.requirement
          )
        case 'accuracy_rate':
          if (previousTotal === 0) return false
          return (
            (achievementState.value.todayCorrect /
              Math.max(1, achievementState.value.todayTotal)) *
              100 >=
            a.requirement
          )
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

  async function syncAchievements(childId: string) {
    const resp = await getAchievements(childId, {
      earnedIds: achievementState.value.earnedAchievements,
      stats: {
        totalQuestions: achievementState.value.totalQuestions,
        currentStreak: achievementState.value.currentStreak,
        bestStreak: achievementState.value.bestStreak,
        todayCorrect: achievementState.value.todayCorrect,
        todayTotal: achievementState.value.todayTotal,
      },
    })
    achievementDefinitions.value = resp.achievements
    achievementState.value.earnedAchievements = resp.earnedIds
    achievementState.value.totalQuestions = resp.stats.totalQuestions
    achievementState.value.currentStreak = resp.stats.currentStreak
    achievementState.value.bestStreak = resp.stats.bestStreak
    achievementState.value.todayCorrect = resp.stats.todayCorrect
    achievementState.value.todayTotal = resp.stats.todayTotal
  }

  async function syncBadges(childId: string) {
    const resp = await getBadges(childId, {
      earnedIds: badgeState.value.earnedBadges,
      totalAnswered: badgeState.value.totalAnswered,
    })
    badgeDefinitions.value = resp.badges
    badgeState.value.earnedBadges = resp.earnedIds
    badgeState.value.totalAnswered = resp.totalAnswered
  }

  async function syncPetState(childId: string) {
    const resp = await getPetState(childId, {
      evolutionIndex: petState.value.evolutionIndex,
      stats: petState.value.stats,
      emotion: petState.value.emotion,
    })

    // 后端 PetStateDTO 字段映射：单值 happiness/hunger → 前端 stats 数组
    const stats: { label: string; value: number; color: string }[] = [
      { label: '饱食', value: (resp as any).hunger ?? 50, color: '#66BB6A' },
      { label: '快乐', value: (resp as any).happiness ?? 80, color: '#29B6F6' },
      { label: '聪明', value: (resp as any).xp ?? 0, color: '#CE93D8' },
    ]

    petState.value = {
      evolutionIndex: ((resp as any).evolutionId ?? 1) - 1, // 后端 evolutionId 是 1-based，转为 0-based
      stats,
      emotion: (resp as any).emotion ?? 'idle',
    }

    // evolutions 和 foodItems 是客户端常量，无需从后端同步
  }

  async function syncCalendar(childId: string, year: number, month: number) {
    const resp = await getMonthCalendar(childId, year, month)
    const key = `${year}-${month}`
    if (resp[key]) {
      calendarData.value[key] = resp[key]
    }
  }

  async function syncCalendarStats(childId: string, year: number, month: number) {
    try {
      calendarStats.value = await getCalendarStats(childId, year, month)
    } catch (e) {
      console.error('CALENDAR_STATS_FAIL', e)
    }
  }

  /**
   * 喂食宠物
   * 业务逻辑全部由后端处理，前端仅同步状态
   */
  async function feedPet(item: PetFoodItem): Promise<boolean> {
    const childId = child.value?.id
    if (!childId) return false

    try {
      const resp = await apiFeedPet(childId, item.name)
      if (!resp.success) return false

      // 用后端返回值同步星星（后端已扣减）
      if (child.value) {
        child.value.stars = resp.newStars
      }

      // 同步宠物属性（后端已计算）
      petState.value.stats[0].value = resp.newStats[0].value
      petState.value.stats[1].value = resp.newStats[1].value
      petState.value.stats[2].value = resp.newStats[2].value

      return true
    } catch {
      return false
    }
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

  return {
    // State
    child,
    childStars,
    childStreak,
    childTodayMinutes,
    calendarData,
    calendarStats,
    syncCalendarStats,
    petState,
    petEvolutions,
    petFoodItems,
    teacherName,
    quizSession,
    dailyTasks,
    todayStars,
    todayMaxStars,
    badgeState,
    badgeDefinitions,
    achievementState,
    achievementDefinitions,
    showPasswordModal,
    showParentMode,
    // Actions
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
    syncAchievements,
    syncBadges,
    syncPetState,
    syncCalendar,
    openPasswordModal,
    closePasswordModal,
    openParentMode,
    closeParentMode,
  }
})

export function getSelectedStudyDay(
  data: Record<string, MonthData>,
  year: number,
  month: number,
  day: number
): StudyDayData | null {
  return data[`${year}-${month}`]?.[day] ?? null
}
