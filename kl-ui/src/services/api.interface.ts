/**
 * API 接口定义
 *
 * 包含前端所有需要的 API 接口。
 * 每个接口包含：endpoint（后端路径）、method、request、response
 * 后端只需按此实现，前端通过 api.ts 调用。
 *
 * 切换模式：修改 src/config/api.config.ts 中的 USE_MOCK 即可
 *
 * @version v1.0 2026-03-21
 */

import type { ParentSettings } from '../mock/data'

// ========== 基础类型 ==========

/** 空请求体（用于 GET 请求或无参数接口） */
export type EmptyRequest = Record<string, never>

/** 通用错误响应 */
export interface ApiError {
  code: number
  message: string
}

// ============================================================
// 家长模式 API
// Base Path: /api/parent
// ============================================================

/**
 * 4.1 密码相关接口
 *
 * 【开发环境】使用 localStorage 模拟
 * 【生产环境】后端实现，密码加密存储
 */

// GET /api/parent/password/status
export interface GetPasswordStatusRequest extends EmptyRequest {}
export interface GetPasswordStatusResponse {
  hasPassword: boolean
}

// POST /api/parent/password/set
export interface SetPasswordRequest {
  password: string  // 4-6位数字
}
export interface SetPasswordResponse {
  success: boolean
}

// POST /api/parent/password/verify
export interface VerifyPasswordRequest {
  password: string
}
export interface VerifyPasswordResponse {
  success: boolean
  attemptsLeft: number  // 剩余尝试次数
  lockedUntil?: number  // 锁定截止时间戳（毫秒），仅在锁定时返回
}

/**
 * 4.2 学习计划相关接口
 *
 * 【开发环境】使用 Mock 数据
 * 【生产环境】后端 AI Agent 生成计划
 */

// GET /api/parent/plan/plans
export interface GetParentPlansRequest extends EmptyRequest {}
export interface GetParentPlansResponse {
  plans: import('../types/parent').LearningPlan[]
  activePlan?: import('../types/parent').LearningPlan
}

// POST /api/parent/plan/create
// 【重要】此接口调用 AI Agent 生成学习计划
export interface CreatePlanRequest {
  prompt: string  // 家长自然语言描述，如「帮我制定一个14天的英语学习计划」
}
export interface CreatePlanResponse {
  plan: import('../types/parent').LearningPlan
  reply: string   // AI 回复文本
}

// POST /api/parent/plan/confirm
export interface ConfirmPlanRequest {
  planId: string
  adjustments?: import('../types/parent').PlanPhase[]  // 可选的阶段调整
}
export interface ConfirmPlanResponse {
  success: boolean
  plan: import('../types/parent').LearningPlan
}

// POST /api/parent/plan/adjust
// 【重要】此接口调用 AI Agent 调整学习计划
export interface AdjustPlanRequest {
  planId: string
  adjustments: string  // 家长自然语言调整描述，如「把每天5题改成8题」
}
export interface AdjustPlanResponse {
  plan: import('../types/parent').LearningPlan
  reply: string
}

// POST /api/parent/plan/pause
export interface PausePlanRequest {
  planId: string
}
export interface PausePlanResponse {
  success: boolean
}

// POST /api/parent/plan/resume
export interface ResumePlanRequest {
  planId: string
}
export interface ResumePlanResponse {
  success: boolean
}

// POST /api/parent/plan/cancel
export interface CancelPlanRequest {
  planId: string
}
export interface CancelPlanResponse {
  success: boolean
}

// GET /api/parent/plan/{planId}
export interface PlanDetailResponse {
  id: string
  subject: 'en' | 'ma'
  status: 'pending' | 'active' | 'paused' | 'completed' | 'cancelled'
  difficulty: 'easy' | 'medium' | 'hard'
  durationDays: number
  dailyGoal: string
  phases: PlanPhaseDetail[]
  estimatedMinutes: number
  startedAt: string | null
  completedAt: string | null
  totalCompleted: number
  totalCorrect: number
  accuracy: number
}

export interface PlanPhaseDetail {
  phaseIndex: number
  dayRange: string
  goal: string
  topic: string
  questionsPerDay: number
  difficulty: 'easy' | 'medium' | 'hard'
  questions: QuestionDetail[]
}

export interface QuestionDetail {
  id: string
  icon: string
  question: string
  options: string[]
  answer: string | null
}

// DELETE /api/parent/plan/{planId}
// 【额外】删除某个计划（PRD 未包含，补充）
export interface DeletePlanRequest {
  planId: string
}
export interface DeletePlanResponse {
  success: boolean
}

/**
 * 4.3 家长聊天消息接口
 *
 * 【重要】核心对话接口，后端需调用 AI Agent
 *
 * AI Agent 需要理解家长意图：
 * 1. 创建学习计划 → 调用 POST /api/parent/plan/create
 * 2. 调整学习计划 → 调用 POST /api/parent/plan/adjust
 * 3. 取消/暂停/恢复计划 → 调用对应接口
 * 4. 其他对话 → 返回普通文本回复
 */

// POST /api/parent/chat
export interface ParentChatRequest {
  message: string     // 家长输入的自然语言
  planId?: string     // 当前关联的计划ID（如果有）
  conversationId?: string  // 对话ID（用于多轮对话，支持生成计划和生成题目分离）
  context?: {
    childId?: string  // 孩子ID（用于 AI 生成个性化计划）
    childAge?: number // 孩子年龄
    subject?: 'en' | 'ma'  // 偏好学科
  }
}

/**
 * 题目生成请求
 * 用于用户确认计划后，生成题目的接口
 */
export interface GenerateQuestionsRequest {
  planId: string       // 学习计划ID
  conversationId?: string  // 对话ID
}

/**
 * 通用聊天卡片配置
 * 后端返回此配置，前端统一渲染
 */
export interface ChatCard {
  style: 'plan' | 'stats' | 'schedule' | 'achievement' | 'task' | 'custom'
  title: string
  icon?: string
  rows?: {
    label: string
    value: string
  }[]
  phases?: {
    dayRange?: string
    topic?: string
    label?: string
    value?: string
    questionsPerDay?: number
    difficulty?: 'easy' | 'medium' | 'hard'
  }[]
  actions: {
    label: string
    action: string
  }[]
  extra?: Record<string, unknown>
}

export interface ParentChatResponse {
  reply: string
  type?: 'text' | 'card' | 'success' | 'error' | 'thinking'
  card?: ChatCard
  plan?: import('../types/parent').LearningPlan
  planAction?: 'confirm' | 'adjust' | 'cancel'
  thinking?: boolean
  conversationId?: string  // 对话ID（用于后续生成题目）
  needsQuestionGeneration?: boolean  // 是否需要生成题目（plan阶段）
}

// POST /api/parent/chat/generate-questions
// 用户确认计划后，生成题目的接口
export interface GenerateQuestionsResponse {
  plan: import('../types/parent').LearningPlan
  card: ChatCard
  reply: string
}

// POST /api/parent/chat/stream
// 【额外】流式聊天接口，适合长回复场景
// SSE 格式返回，后端 AI 生成计划时使用
export interface ParentChatStreamRequest {
  message: string
  planId?: string
  conversationId?: string
  context?: {
    childId?: string
    childAge?: number
    subject?: 'en' | 'ma'
  }
}

// ============================================================
// 家长设置相关 API（现有功能）
// Base Path: /api/parent/settings
// ============================================================

// GET /api/parent/settings
export interface GetParentSettingsRequest extends EmptyRequest {}
export interface GetParentSettingsResponse {
  dailyLimitMinutes: number     // 每日学习时长上限（分钟）
  dndEnabled: boolean           // 勿扰模式是否开启
  dndStart: string              // 勿扰开始时间 HH:mm
  dndEnd: string                // 勿扰结束时间 HH:mm
  eyeCareReminderEnabled: boolean
  breakReminderEnabled: boolean
  teacherName: string           // 老师名称
}

// PUT /api/parent/settings
export interface UpdateParentSettingsRequest {
  dailyLimitMinutes?: number
  dndEnabled?: boolean
  dndStart?: string
  dndEnd?: string
  eyeCareReminderEnabled?: boolean
  breakReminderEnabled?: boolean
  teacherName?: string         // 老师名称
}
export interface UpdateParentSettingsResponse {
  success: boolean
  settings: ParentSettings
}

// ============================================================
// 孩子信息相关 API（供家长查看）
// Base Path: /api/children
// ============================================================

// GET /api/children/{childId}/profile
export interface GetChildProfileRequest {
  childId: string
}
export interface GetChildProfileResponse {
  id: string
  name: string
  age: number
  ageGroup: string
  stars: number
  streak: number
  todayMinutes: number
  level: number
  xp: number
  xpNeeded: number
  dailyTargetEn?: number
  dailyTargetMa?: number
}

// GET /api/children/{childId}/stats/daily
export interface GetChildDailyStatsRequest {
  childId: string
  date?: string  // YYYY-MM-DD，默认今天
}
export interface GetChildDailyStatsResponse {
  date: string
  totalQuestions: number
  correctCount: number
  accuracy: number
  minutes: number
  subjects: {
    subject: 'en' | 'ma'
    questions: number
    correct: number
  }[]
}

// GET /api/children/{childId}/stats/weekly
export interface GetChildWeeklyStatsRequest {
  childId: string
}
export interface GetChildWeeklyStatsResponse {
  weekStart: string
  weekEnd: string
  totalQuestions: number
  totalMinutes: number
  avgAccuracy: number
  dailyData: {
    date: string
    questions: number
    minutes: number
    accuracy: number
  }[]
}

// ============================================================
// 日历/学习记录 API
// Base Path: /api/children/{childId}/calendar
// ============================================================

/**
 * 日历某一天的学习记录
 * 对应前端 MonthData[day] 结构
 */
export interface StudyDayRecord {
  heat: number         // 学习热度 0-4（0=未学习）
  minutes: number      // 学习时长（分钟）
  subjects: StudySubjectRecord[]
}

/**
 * 学科学习记录
 */
export interface StudySubjectRecord {
  icon: string        // 学科图标，如 '📖'
  name: string        // 学科名称，如 '英语'
  topic: string       // 学习主题，如 '动物词汇'
  stars: number       // 获得星星数
  accuracy: number     // 正确率（0-100）
  color: string        // 主题色，如 '#40C4FF'
  bg: string           // 背景色，如 'rgba(64,196,255,.12)'
}

/**
 * 获取指定月份的学习数据
 * GET /api/children/{childId}/calendar/{year}/{month}
 *
 * 返回该月每天的学习记录，用于渲染日历热力图
 */
export interface GetMonthCalendarRequest {
  childId: string
  year: number
  month: number
}
export interface GetMonthCalendarResponse {
  year: number
  month: number
  days: Record<number, StudyDayRecord>  // key 为日期（1-31），空则表示未学习
}

// GET /api/children/{childId}/calendar/daily/{date}
// 获取指定日期的学习详情
export interface GetDayStudyDetailRequest {
  childId: string
  date: string  // YYYY-MM-DD
}
export interface GetDayStudyDetailResponse {
  date: string
  totalMinutes: number
  totalQuestions: number
  correctCount: number
  accuracy: number
  streak: number              // 连续学习天数
  subjects: StudySubjectRecord[]
}

// GET /api/children/{childId}/calendar/stats
// 获取累计统计数据（用于日历页顶部统计）
export interface GetCalendarStatsRequest {
  childId: string
  year: number
  month: number
}
export interface GetCalendarStatsResponse {
  totalDays: number           // 本月学习天数
  totalMinutes: number        // 本月累计时长
  totalQuestions: number      // 本月累计答题
  totalStars: number         // 本月累计星星
  currentStreak: number       // 当前连续天数
  longestStreak: number       // 最长连续天数
}

// GET /api/children/{childId}/profile
// 获取孩子完整档案（用于日历页/家长模式）
export interface GetChildProfileRequest {
  childId: string
}
export interface GetChildProfileResponse {
  id: string
  name: string
  age: number
  ageGroup: string
  stars: number
  streak: number
  todayMinutes: number
  level: number
  xp: number
  xpNeeded: number
  totalAnswered: number        // 累计答题数
  totalDays: number            // 累计学习天数
  dailyTargetEn?: number
  dailyTargetMa?: number
}

// ============================================================
// 设备注册 API
// Base Path: /api/device
// ============================================================

// POST /api/device/register
export interface DeviceRegisterRequest {
  deviceId: string
  pushToken?: string
  platform?: string  // 'ios' | 'android' | 'h5'
}
export interface DeviceRegisterResponse {
  success: boolean
  deviceId: string
}

// ============================================================
// 预取消息 API
// Base Path: /api/message
// ============================================================

// GET /api/message/prefetch?trigger=xxx
export interface PrefetchMessageRequest {
  trigger?: string  // 'push_open' | 'app_launch' | ...
}
export interface PrefetchMessageResponse {
  content?: string
  action?: string
  subject?: 'en' | 'ma'
}

// ============================================================
// 邀请码验证 API
// Base Path: /api/invite
// ============================================================

// GET /api/invite/validate?code=xxx
export interface ValidateInviteCodeRequest {
  code: string
}
export interface ValidateInviteCodeResponse {
  valid: boolean
  message: string
}

// ============================================================
// 身份证验证 API
// Base Path: /api/idCard
// ============================================================

// POST /api/idCard/validate
export interface ValidateIdCardRequest {
  idCard: string
}
export interface ValidateIdCardResponse {
  valid: boolean
  age?: number
  ageGroup?: string
  message?: string
}

// ============================================================
// 孩子注册/激活 API
// Base Path: /api/children
// ============================================================

// POST /api/children/register
export interface RegisterChildRequest {
  name: string
  age: number
  ageGroup: string
  deviceId: string
}
export interface RegisterChildResponse {
  child: {
    id: string
    name: string
    age: number
    ageGroup: string
    stars: number
    streak: number
    todayMinutes: number
    dailyTargetEn?: number
    dailyTargetMa?: number
  }
}

// POST /api/children/activate
export interface ActivateChildRequest {
  inviteCode: string
  idCard: string
  name: string
  teacherName?: string
  deviceId: string
}
export interface ActivateChildResponse {
  token?: string
  child: {
    id: string
    name: string
    age: number
    ageGroup: string
    stars: number
    xp?: number
    level?: number
    streak: number
    todayMinutes: number
    dailyTargetEn?: number
    dailyTargetMa?: number
  }
}

// ============================================================
// 答题流程 API
// Base Path: /api/quiz
// ============================================================

// POST /api/quiz/next
export interface QuizNextRequest {
  subject?: 'en' | 'ma'  // 学科筛选，不传则返回全部
  planId?: string        // 学习计划ID（计划模式必传）
}

/** 今日答题总结 */
export interface QuizSummaryDTO {
  hasMore: boolean       // 今日是否还有更多题目
  todayTotal: number     // 今日总题目数（计划目标）
  todayMaxStars: number  // 今日最高可获得星星数（计划目标）
  todayDone: number      // 今日已答题数
  todayStarsEarned: number // 今日已获得星星
  todayTargetEn: number  // 英语今日目标题数
  todayTargetMa: number  // 数学今日目标题数
}

/**
 * 获取下一题的响应。
 * - HasQuestion: 有题目时返回（question + summary）
 * - NoMoreQuestions: 无更多题目时返回（summary）
 */
export interface QuizNextResponse {
  question?: {
    id: string
    subject: 'en' | 'ma'
    label: string
    question: string
    options: { id: string; text: string }[]
    estimatedMinutes: number
  }
  summary: QuizSummaryDTO
}

// POST /api/quiz/submit
export interface QuizSubmitRequest {
  questionId: string
  planQuestionId?: string  // 关联的计划题目ID（学习计划场景可选）
  answerId: string
  timeSpent: number  // 秒
}
export interface QuizSubmitResponse {
  isCorrect: boolean
  resultMode: 'correct' | 'wrong' | 'reward'
  correctAnswerId: string
  explanation: string
  explanationSteps: string[]
  encouragement: string
  showResult: boolean
  hasMore: boolean
  result?: {
    combo: number
    accuracy: number
    timeSpent: number
    stars: number
    todayMaxStars: number  // 今日目标星星总数（由后端计算）
    xp: number
    xpNeeded: number
    level: number
    reward?: {
      icon: string
      title: string
      sub: string
    }
  }
}

// POST /api/quiz/reset
export interface QuizResetRequest {}
export interface QuizResetResponse {
  success: boolean
}

// ============================================================
// 成就系统 API
// Base Path: /api/children/{childId}/achievements
// ============================================================

export interface AchievementStats {
  totalQuestions: number
  currentStreak: number
  bestStreak: number
  todayCorrect: number
  todayTotal: number
}

// GET /api/children/{childId}/achievements
export interface GetAchievementsRequest {
  childId: string
}
export interface GetAchievementsResponse {
  achievements: import('../mock/data').Achievement[]
  earnedIds: string[]
  stats: AchievementStats
}

// ============================================================
// 知识星球徽章 API
// Base Path: /api/children/{childId}/badges
// ============================================================

// GET /api/children/{childId}/badges
export interface GetBadgesRequest {
  childId: string
}
export interface GetBadgesResponse {
  badges: import('../mock/data').KnowledgeBadge[]
  earnedIds: string[]
  totalAnswered: number
  currentLevel: number
}

// ============================================================
// 宠物系统 API
// Base Path: /api/children/{childId}/pet
// ============================================================

// GET /api/children/{childId}/pet
export interface GetPetStateRequest {
  childId: string
}
export interface GetPetStateResponse {
  evolutionIndex: number
  stats: import('../mock/data').PetStat[]
  emotion: import('../mock/data').PetEmotion
  evolutions: import('../mock/data').PetEvolution[]
  foodItems: import('../mock/data').PetFoodItem[]
}

// POST /api/children/{childId}/pet/feed
export interface FeedPetRequest {
  childId: string
  itemId: string
}
export interface FeedPetResponse {
  success: boolean
  newStars: number
  newStats: import('../mock/data').PetStat[]
}
