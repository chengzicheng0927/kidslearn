// ========== 家长模式类型定义 ==========

/**
 * 学习计划阶段
 */
export interface PlanPhase {
  dayRange: string       // "1-3"
  goal: string            // 阶段目标描述
  topic: string           // 主题（如：动物、水果）
  questionsPerDay: number // 每日题目数
  difficulty: 'easy' | 'medium' | 'hard'
  questions?: QuestionDetail[]  // 该阶段的题目
}

/**
 * 学习计划
 */
export interface LearningPlan {
  id: string
  subject: 'en' | 'ma'
  duration: number           // 周期天数
  dailyGoal: string          // 每日目标描述
  phases: PlanPhase[]        // 阶段拆分
  difficulty: 'easy' | 'medium' | 'hard'
  estimatedMinutes: number    // 预计每日时长（分钟）
  status: 'pending' | 'active' | 'paused' | 'completed'
  createdAt: string
  startedAt?: string
  endedAt?: string
  currentPhaseIndex: number  // 当前阶段索引（从0开始）
}

/**
 * 通用聊天卡片配置
 */
export interface ChatCard {
  style: 'plan' | 'stats' | 'schedule' | 'achievement' | 'task' | 'custom'
  title: string
  name?: string
  icon?: string
  rows?: {
    label: string
    value: string
  }[]
  phases?: PhaseDetail[]
  actions: {
    label: string
    action: string
  }[]
  extra?: Record<string, unknown>
}

/**
 * 阶段详情（包含题目列表）
 */
export interface PhaseDetail {
  dayRange?: string
  topic?: string
  label?: string
  value?: string
  questionsPerDay?: number
  difficulty?: 'easy' | 'medium' | 'hard'
  questions?: QuestionDetail[]
}

/**
 * 题目详情
 */
export interface QuestionDetail {
  id: string
  icon: string
  question: string
  options: string[]
  answer: string
}

/**
 * 家长消息类型
 */
export type ParentMessageType = 'text' | 'card' | 'success' | 'error' | 'thinking' | 'plan_loading'

/**
 * 家长聊天消息
 */
export interface ParentMessage {
  id: string
  role: 'parent' | 'ai' | 'system'
  content: string
  type: ParentMessageType
  card?: ChatCard
  plan?: LearningPlan
  planAction?: 'confirm' | 'adjust' | 'cancel'
  timestamp: string
  thinking?: boolean
  conversationId?: string  // 对话ID（用于多轮对话）
  needsQuestionGeneration?: boolean  // 是否需要生成题目（plan阶段）
}

/**
 * 密码状态
 */
export interface PasswordStatus {
  hasPassword: boolean
}

/**
 * 密码验证结果
 */
export interface PasswordVerifyResult {
  success: boolean
  attemptsLeft: number
  lockedUntil?: number  // 锁定截止时间戳（毫秒）
}

/**
 * 家长聊天响应
 */
export interface ParentChatResponse {
  reply: string
  type?: 'text' | 'card' | 'success' | 'error' | 'thinking'
  card?: ChatCard
  plan?: LearningPlan
  planAction?: 'confirm' | 'adjust' | 'cancel'
  thinking?: boolean
  conversationId?: string
}
