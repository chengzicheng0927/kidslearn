/**
 * API 服务层
 *
 * 设计原则：
 * 1. 所有接口先定义签名，再实现 Mock
 * 2. 通过 API_CONFIG.USE_MOCK 切换数据来源
 * 3. 生产环境：USE_MOCK = false，自动调用真实后端
 *
 * 切换方式：
 *   修改 src/config/api.config.ts 中的 USE_MOCK = false
 *   并配置 API_BASE_URL 为后端地址
 *
 * 接口文档：src/services/api.interface.ts
 */

import { API_CONFIG } from '../config/api.config'
import type {
  // 密码相关
  GetPasswordStatusResponse,
  SetPasswordResponse,
  VerifyPasswordResponse,
  // 计划相关
  GetParentPlansRequest, GetParentPlansResponse,
  CreatePlanRequest, CreatePlanResponse,
  ConfirmPlanRequest, ConfirmPlanResponse,
  AdjustPlanRequest, AdjustPlanResponse,
  PausePlanRequest, PausePlanResponse,
  ResumePlanRequest, ResumePlanResponse,
  CancelPlanRequest, CancelPlanResponse,
  // 聊天相关
  ParentChatRequest, ParentChatResponse,
  // 设置相关
  UpdateParentSettingsRequest, UpdateParentSettingsResponse,
  GetParentSettingsResponse,
  // 设备相关
  DeviceRegisterRequest, DeviceRegisterResponse,
  PrefetchMessageResponse,
  ValidateInviteCodeResponse,
  ValidateIdCardRequest, ValidateIdCardResponse,
  RegisterChildRequest, RegisterChildResponse,
  ActivateChildRequest, ActivateChildResponse,
  QuizNextRequest, QuizNextResponse,
  QuizSubmitRequest, QuizSubmitResponse,
  QuizResetResponse,
} from './api.interface'
import type { LearningPlan, ParentMessage, PlanPhase } from '../types/parent'
import type { Child, MockUser, ParentSettings, Question } from '../mock/data'

// ========== 工具函数 ==========

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function canUseStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function generateDeviceId(): string {
  return `device-${Math.random().toString(36).slice(2, 12)}`
}

const DEVICE_ID_KEY = 'kidslearn_device_id'
const MOCK_CHILD_KEY = 'kidslearn_mock_child'
const DEVICE_PUSH_TOKEN_KEY = 'kidslearn_device_push_token'

function readStoredChild(): Child | null {
  if (!canUseStorage()) return null
  try {
    const raw = window.localStorage.getItem(MOCK_CHILD_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function persistChild(child: Child) {
  if (!canUseStorage()) return
  window.localStorage.setItem(MOCK_CHILD_KEY, JSON.stringify(child))
}

function getDeviceId(): string {
  if (!canUseStorage()) return generateDeviceId()
  let deviceId = window.localStorage.getItem(DEVICE_ID_KEY)
  if (!deviceId) {
    deviceId = generateDeviceId()
    window.localStorage.setItem(DEVICE_ID_KEY, deviceId)
  }
  return deviceId
}

// ========== 基础请求函数 ==========

async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = `${API_CONFIG.API_BASE_URL}${endpoint}`
  const token = canUseStorage() ? window.localStorage.getItem('kidslearn_token') : null
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    ...(options.headers as Record<string, string>),
  }

  if (API_CONFIG.DEBUG) {
    console.log(`[API] ${options.method ?? 'GET'} ${url}`, options.body)
  }

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), API_CONFIG.TIMEOUT)

  try {
    const response = await fetch(url, {
      ...options,
      headers,
      signal: controller.signal,
    })

    // 读取响应文本，处理空 body 的情况
    const text = await response.text()
    let data: unknown = {}
    if (text) {
      try {
        data = JSON.parse(text)
      } catch {
        // 非 JSON 格式，忽略
      }
    }

    if (!response.ok) {
      const msg = (data as any)?.message || (data as any)?.msg || response.statusText
      if (response.status >= 500) {
        if (API_CONFIG.DEBUG) {
          console.error(`[API Error] ${response.status} ${url}:`, msg)
        }
        window.__API_ERROR_HANDLER__?.(msg)
      }
      // 4xx 业务错误不抛异常，调用方自行处理（避免控制台报错）
      if (response.status < 500) {
        throw new ApiException(response.status, msg, true)
      }
      throw new ApiException(response.status, msg)
    }

    // 解包 data  envelope（后端返回 { data: T, traceId, timestamp }）
    return (data as any)?.data ?? (data as T)
  } catch (err) {
    if (err instanceof ApiException) throw err
    // 网络错误（断网、超时、后端不通等）统一显示友好提示
    const msg = '网络异常，请检查网络或后端服务'
    if (API_CONFIG.DEBUG) {
      console.error(`[API Error] ${url}:`, msg)
    }
    window.__API_ERROR_HANDLER__?.(msg)
    throw new ApiException(0, msg)
  } finally {
    clearTimeout(timeout)
  }
}

export class ApiException extends Error {
  constructor(
    public code: number,
    message: string,
    public silent = false
  ) {
    super(message)
    this.name = 'ApiException'
  }
}

// ============================================================
// 用户与注册相关 API
// ============================================================

export interface AnswerResult {
  isCorrect: boolean
  resultMode?: 'correct' | 'wrong' | 'reward'
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

/**
 * 检查用户状态（孩子是否已注册）
 * GET /api/children/profile?deviceId=xxx
 */
export async function checkUserStatus(): Promise<MockUser> {
  await sleep(250)
  if (API_CONFIG.USE_MOCK) {
    const { getMockUser } = await import('../mock/data')
    const storedChild = readStoredChild()
    if (storedChild) {
      return { exists: true, child: storedChild }
    }
    return getMockUser(getDeviceId())
  }

  const deviceId = getDeviceId()
  const resp = await request<any>(
    `/api/children/profile?deviceId=${encodeURIComponent(deviceId)}`
  )
  if (resp.token && canUseStorage()) {
    window.localStorage.setItem('kidslearn_token', resp.token)
  }
  if (!resp.child) {
    return { exists: false } as MockUser
  }
  const child: Child = {
    id: resp.child.id,
    name: resp.child.name,
    age: resp.child.age,
    ageGroup: resp.child.ageGroup,
    stars: resp.child.stars,
    streak: resp.child.streak,
    todayMinutes: resp.child.todayMinutes,
    dailyTargetEn: resp.child.dailyTargetEn,
    dailyTargetMa: resp.child.dailyTargetMa,
  }
  return { exists: true, child } as MockUser
}

/**
 * 注册设备推送 Token
 * POST /api/device/register
 */
export async function registerDeviceToken(token: string): Promise<{ success: boolean; deviceId: string; token: string }> {
  await sleep(200)
  const deviceId = getDeviceId()
  if (API_CONFIG.USE_MOCK) {
    if (canUseStorage()) {
      window.localStorage.setItem(DEVICE_PUSH_TOKEN_KEY, token)
    }
    return { success: true, deviceId, token }
  }

  const req: DeviceRegisterRequest = { deviceId, pushToken: token }
  const resp = await request<DeviceRegisterResponse>('/api/device/register', {
    method: 'POST',
    body: JSON.stringify(req),
  })
  return { success: resp.success, deviceId: resp.deviceId, token }
}

/**
 * 获取预取消息（推送点击唤起时）
 * GET /api/message/prefetch?trigger=xxx
 */
export async function fetchPrefetchedMessage(trigger?: string): Promise<{
  content: string
  action?: string
  subject?: 'en' | 'ma'
} | null> {
  await sleep(150)
  if (API_CONFIG.USE_MOCK) {
    if (trigger === 'push_open') {
      return { content: '欢迎回来，我们继续刚才的学习吧！', action: 'resume_study_session' }
    }
    return null
  }

  const resp = await request<PrefetchMessageResponse>(
    `/api/message/prefetch${trigger ? `?trigger=${encodeURIComponent(trigger)}` : ''}`
  )
  return resp.content ? { content: resp.content, action: resp.action, subject: resp.subject } : null
}

/**
 * 验证邀请码
 * GET /api/invite/validate?code=xxx
 */
export async function validateInviteCode(code: string): Promise<{ valid: boolean; message: string }> {
  await sleep(300)
  if (API_CONFIG.USE_MOCK) {
    const { verifyInviteCode } = await import('../mock/data')
    const valid = verifyInviteCode(code)
    return { valid, message: valid ? '邀请码有效' : '邀请码无效或已过期' }
  }

  const resp = await request<ValidateInviteCodeResponse>(
    `/api/invite/validate?code=${encodeURIComponent(code)}`
  )
  return { valid: resp.valid, message: resp.message }
}

/**
 * 验证身份证号并计算年龄
 * POST /api/idCard/validate
 */
export async function submitIdCard(idCard: string): Promise<{ valid: boolean; age?: number; ageGroup?: string }> {
  await sleep(250)
  if (API_CONFIG.USE_MOCK) {
    const { calcAgeFromId, getAgeGroup } = await import('../mock/data')
    const age = calcAgeFromId(idCard)
    if (!age) return { valid: false }
    return { valid: true, age, ageGroup: getAgeGroup(age).label }
  }

  const req: ValidateIdCardRequest = { idCard }
  const resp = await request<ValidateIdCardResponse>('/api/idCard/validate', {
    method: 'POST',
    body: JSON.stringify(req),
  })
  return resp
}

/**
 * 注册孩子账户
 * POST /api/children/register
 */
export async function registerChild(name: string, age: number, ageGroup: string): Promise<Child> {
  await sleep(400)
  if (API_CONFIG.USE_MOCK) {
    const { createMockChild } = await import('../mock/data')
    const appStore = useAppStore()
    const child = createMockChild(name, age, ageGroup)
    persistChild(child)
    appStore.setChild(child)
    return child
  }

  const req: RegisterChildRequest = { name, age, ageGroup, deviceId: getDeviceId() }
  const resp = await request<RegisterChildResponse>('/api/children/register', {
    method: 'POST',
    body: JSON.stringify(req),
  })
  const child: Child = {
    id: resp.child.id,
    name: resp.child.name,
    age: resp.child.age,
    ageGroup: resp.child.ageGroup,
    stars: resp.child.stars,
    streak: resp.child.streak,
    todayMinutes: resp.child.todayMinutes,
  }
  const appStore = useAppStore()
  appStore.setChild(child)
  return child
}

/**
 * 通过邀请码激活孩子账户
 * POST /api/children/activate
 */
export async function activateAccountWithCode(
  code: string,
  idCard: string,
  name: string,
  teacherName?: string
): Promise<Child> {
  await sleep(500)
  if (API_CONFIG.USE_MOCK) {
    const { calcAgeFromId, getAgeGroup, verifyInviteCode, createMockChild } = await import('../mock/data')
    const appStore = useAppStore()
    const age = calcAgeFromId(idCard)
    if (!age) throw new ApiException(400, '身份证号格式不正确')
    if (!verifyInviteCode(code)) throw new ApiException(400, '邀请码无效或已过期')
    const child = createMockChild(name, age, getAgeGroup(age).label)
    persistChild(child)
    appStore.setChild(child)
    return child
  }

  const req: ActivateChildRequest = {
    inviteCode: code,
    idCard,
    name,
    teacherName,
    deviceId: getDeviceId(),
  }
  const resp = await request<ActivateChildResponse>('/api/children/activate', {
    method: 'POST',
    body: JSON.stringify(req),
  })
  if (resp.token && canUseStorage()) {
    window.localStorage.setItem('kidslearn_token', resp.token)
  }
  const child: Child = {
    id: resp.child.id,
    name: resp.child.name,
    age: resp.child.age,
    ageGroup: resp.child.ageGroup,
    stars: resp.child.stars,
    streak: resp.child.streak,
    todayMinutes: resp.child.todayMinutes,
    dailyTargetEn: resp.child.dailyTargetEn,
    dailyTargetMa: resp.child.dailyTargetMa,
  }
  const appStore = useAppStore()
  appStore.setChild(child)
  return child
}

export function getCurrentDeviceId(): string {
  return getDeviceId()
}

export function clearStoredMockUser() {
  if (!canUseStorage()) return
  window.localStorage.removeItem(MOCK_CHILD_KEY)
}

// ============================================================
// 答题相关 API
// ============================================================

import { useAppStore } from '../stores/appStore'

let _mockQuestions: Question[] | null = null

async function getQuestionBank(subject?: 'en' | 'ma'): Promise<Question[]> {
  if (!_mockQuestions) {
    const { mockQuestions } = await import('../mock/data')
    _mockQuestions = mockQuestions
  }
  return subject ? _mockQuestions.filter((q) => q.subject === subject) : _mockQuestions
}

/**
 * 提交答题结果
 * POST /api/quiz/submit
 *
 * 【重要】后端判定答案、计算奖励、更新进度
 */
export async function submitAnswer(
  questionId: string,
  answerId: string,
  timeSpent: number,
  subject?: 'en' | 'ma'
): Promise<AnswerResult> {
  await sleep(300)
  if (API_CONFIG.USE_MOCK) {
    const { quizSession, recordAnswer } = useAppStore()
    const questionBank = await getQuestionBank()
    const question = questionBank.find((item) => item.id === questionId)
    if (!question) throw new ApiException(404, `Question not found: ${questionId}`)

    const isCorrect = answerId === question.correctAnswerId
    recordAnswer(question, isCorrect)

    const remainingQuestions = questionBank.length - quizSession.currentQuestionIndex
    const hasMore = remainingQuestions > 0
    const accuracy = Math.round((quizSession.correctCount / quizSession.answeredCount) * 100)
    const resultMode = !isCorrect ? 'wrong' : question.successResultMode ?? 'correct'
    const showResult = !isCorrect || resultMode === 'reward' || !hasMore

    return {
      isCorrect,
      resultMode,
      correctAnswerId: question.correctAnswerId,
      explanation: question.explanation,
      explanationSteps: question.explanationSteps,
      encouragement: question.encouragement,
      showResult,
      hasMore,
      result: isCorrect
        ? {
            combo: quizSession.combo,
            accuracy,
            timeSpent,
            stars: question.reward.stars,
            xp: question.reward.xp,
            xpNeeded: question.reward.xpNeeded,
            level: question.reward.level,
            reward: question.reward.badge,
          }
        : undefined,
    }
  }

  const req: QuizSubmitRequest = { questionId, answerId, timeSpent }
  const childId = useAppStore().child?.id
  const resp = await request<QuizSubmitResponse>(`/api/quiz/submit?childId=${childId}`, {
    method: 'POST',
    body: JSON.stringify(req),
  })

  const store = useAppStore()
  if (resp.result) {
    store.quizSession.combo = resp.result.combo
    if (resp.isCorrect) store.quizSession.correctCount += 1
    store.quizSession.answeredCount += 1

    if (store.child) {
      // 累加本次获得的星星
      store.child.stars += resp.result.stars
      // 更新整体经验值和等级
      store.child.xp = resp.result.xp
      store.child.level = resp.result.level
    }

    // 同步今日顶栏面板数据
    store.todayStars += resp.result.stars
    if (resp.result.todayMaxStars !== undefined) {
      store.todayMaxStars = resp.result.todayMaxStars
    }
    if (subject === 'en') {
      store.dailyTasks.enDone = Math.min(store.dailyTasks.enTarget, store.dailyTasks.enDone + 1)
    } else if (subject === 'ma') {
      store.dailyTasks.maDone = Math.min(store.dailyTasks.maTarget, store.dailyTasks.maDone + 1)
    }
  }

  return {
    isCorrect: resp.isCorrect,
    resultMode: resp.resultMode,
    correctAnswerId: resp.correctAnswerId,
    explanation: resp.explanation,
    explanationSteps: resp.explanationSteps,
    encouragement: resp.encouragement,
    showResult: resp.showResult,
    hasMore: resp.hasMore,
    result: resp.result ? { ...resp.result } : undefined,
  }
}

/**
 * 获取下一题
 * POST /api/quiz/next
 *
 * 返回 { question, summary }（有题时）或 { question: null, summary }（无题时）
 */
export async function getNextQuestion(
  subject?: 'en' | 'ma',
  _startIndex?: number
): Promise<{ question: Question | null; summary: QuizSummaryDTO }> {
  await sleep(200)
  if (API_CONFIG.USE_MOCK) {
    const store = useAppStore()
    const index = store.consumeNextQuestionIndex()
    const questionBank = await getQuestionBank(subject)
    const question = index < questionBank.length ? questionBank[index] : null
    // Mock summary 与后端逻辑对齐
    const mockSummary: QuizSummaryDTO = {
      hasMore: question !== null,
      todayTotal: questionBank.length,
      todayMaxStars: questionBank.length * 2,
      todayDone: store.quizSession.answeredCount,
      todayStarsEarned: store.todayStars,
      todayTargetEn: store.dailyTasks.enTarget,
      todayTargetMa: store.dailyTasks.maTarget,
    }
    return { question, summary: mockSummary }
  }

  const req: QuizNextRequest = { subject }
  const childId = useAppStore().child?.id
  const resp = await request<QuizNextResponse>(`/api/quiz/next?childId=${childId}`, {
    method: 'POST',
    body: JSON.stringify(req),
  })

  // resp 是 QuizNextResponse: HasQuestion 有 question，NoMoreQuestions 无 question
  const summary = resp.summary
  const question = resp.question ? {
    id: resp.question.id,
    subject: resp.question.subject,
    label: resp.question.label,
    question: resp.question.question,
    options: resp.question.options,
    correctAnswerId: '', // 后端不在此处返回，由 submit 验证
    explanation: '',
    explanationSteps: [],
    encouragement: '',
    estimatedMinutes: resp.question.estimatedMinutes,
    reward: { stars: 2, xp: 20 },
  } : null

  return { question, summary }
}

/**
 * 重置答题进度
 * POST /api/quiz/reset
 */
export async function resetQuestions(): Promise<void> {
  if (API_CONFIG.USE_MOCK) {
    useAppStore().resetQuizSession()
    return
  }

  const childId = useAppStore().child?.id
  await request<QuizResetResponse>(`/api/quiz/reset?childId=${childId}`, { method: 'POST' })
  useAppStore().resetQuizSession()
}

// ============================================================
// 家长模式：密码系统
// ============================================================

const PARENT_PASSWORD_KEY = 'kidslearn_parent_password'
const PARENT_PASSWORD_ATTEMPTS_KEY = 'kidslearn_parent_password_attempts'
const MAX_PASSWORD_ATTEMPTS = 3
const LOCK_DURATION_MS = 30_000

interface PasswordAttempts {
  count: number
  lockedUntil: number | null
}

function readPasswordAttempts(): PasswordAttempts {
  if (!canUseStorage()) return { count: 0, lockedUntil: null }
  try {
    const raw = window.localStorage.getItem(PARENT_PASSWORD_ATTEMPTS_KEY)
    return raw ? JSON.parse(raw) : { count: 0, lockedUntil: null }
  } catch {
    return { count: 0, lockedUntil: null }
  }
}

function persistPasswordAttempts(attempts: PasswordAttempts) {
  if (!canUseStorage()) return
  window.localStorage.setItem(PARENT_PASSWORD_ATTEMPTS_KEY, JSON.stringify(attempts))
}

/**
 * 获取密码状态：是否已设置密码
 * GET /api/parent/password/status
 */
export async function getPasswordStatus(): Promise<GetPasswordStatusResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(100)
    if (!canUseStorage()) return { hasPassword: false }
    const password = window.localStorage.getItem(PARENT_PASSWORD_KEY)
    return { hasPassword: !!password }
  }

  return request<GetPasswordStatusResponse>('/api/parent/password/status')
}

/**
 * 设置家长密码（首次设置）
 * POST /api/parent/password/set
 */
export async function setPassword(password: string): Promise<SetPasswordResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(200)
    if (!canUseStorage()) return { success: false }
    window.localStorage.setItem(PARENT_PASSWORD_KEY, btoa(password))
    persistPasswordAttempts({ count: 0, lockedUntil: null })
    return { success: true }
  }

  return request<SetPasswordResponse>('/api/parent/password/set', {
    method: 'POST',
    body: JSON.stringify({ password }),
  })
}

/**
 * 验证家长密码
 * POST /api/parent/password/verify
 */
export async function verifyPassword(password: string): Promise<VerifyPasswordResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(150)
    const attempts = readPasswordAttempts()
    const now = Date.now()

    if (attempts.lockedUntil && now < attempts.lockedUntil) {
      return { success: false, attemptsLeft: 0, lockedUntil: attempts.lockedUntil }
    }

    if (!canUseStorage()) return { success: false, attemptsLeft: MAX_PASSWORD_ATTEMPTS }
    const stored = window.localStorage.getItem(PARENT_PASSWORD_KEY)
    const success = stored === btoa(password)

    if (success) {
      persistPasswordAttempts({ count: 0, lockedUntil: null })
      return { success: true, attemptsLeft: MAX_PASSWORD_ATTEMPTS }
    }

    const newCount = attempts.count + 1
    let lockedUntil: number | null = null
    if (newCount >= MAX_PASSWORD_ATTEMPTS) {
      lockedUntil = now + LOCK_DURATION_MS
    }

    persistPasswordAttempts({ count: newCount, lockedUntil })
    return {
      success: false,
      attemptsLeft: Math.max(0, MAX_PASSWORD_ATTEMPTS - newCount),
      lockedUntil: lockedUntil ?? undefined,
    }
  }

  return request<VerifyPasswordResponse>('/api/parent/password/verify', {
    method: 'POST',
    body: JSON.stringify({ password }),
  })
}

// ============================================================
// 家长模式：学习计划
// ============================================================

/**
 * 获取学习计划列表
 * GET /api/parent/plan/plans
 */
export async function getParentPlans(
  _req: GetParentPlansRequest = {}
): Promise<GetParentPlansResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(200)
    const { mockParentPlans } = await import('../mock/parentData')
    const activePlan = mockParentPlans.find((p: LearningPlan) => p.status === 'active')
    return { plans: mockParentPlans, activePlan }
  }

  return request<GetParentPlansResponse>('/api/parent/plan/plans')
}

/**
 * 创建学习计划（AI 生成）
 * POST /api/parent/plan/create
 *
 * 【重要】此接口调用后端 AI Agent 生成学习计划
 */
export async function createPlan(req: CreatePlanRequest): Promise<CreatePlanResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(1500)
    const { mockGeneratePlan } = await import('../mock/parentData')
    const plan = await mockGeneratePlan(req.prompt)
    return {
      plan,
      reply: `好的，我来为孩子制定一个 ${plan.duration} 天的${plan.subject === 'en' ? '英语' : '数学'}学习计划。\n\n这个计划包含 ${plan.phases.length} 个阶段，每天大约 ${plan.estimatedMinutes} 分钟。`,
    }
  }

  return request<CreatePlanResponse>('/api/parent/plan/create', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

/**
 * 确认学习计划
 * POST /api/parent/plan/confirm
 */
export async function confirmPlan(req: ConfirmPlanRequest): Promise<ConfirmPlanResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(300)
    const { mockParentPlans } = await import('../mock/parentData')
    // 查找已存在的 plan 或使用动态生成的 plan
    const plan = mockParentPlans.find((p: LearningPlan) => p.id === req.planId)
    if (plan) {
      if (req.adjustments) plan.phases = req.adjustments
      plan.status = 'active'
      plan.startedAt = new Date().toISOString().split('T')[0]
      return { success: true, plan }
    }
    // 动态生成的 plan（mockGeneratePlan 创建的）不在 mockParentPlans 中
    // 创建一个模拟的已激活计划返回
    const mockPlan: LearningPlan = {
      id: req.planId,
      subject: 'en',
      duration: 14,
      dailyGoal: '每天完成 5 道英语题',
      phases: [],
      difficulty: 'easy',
      estimatedMinutes: 20,
      status: 'active',
      createdAt: new Date().toISOString(),
      startedAt: new Date().toISOString().split('T')[0],
    }
    return { success: true, plan: mockPlan }
  }

  return request<ConfirmPlanResponse>('/api/parent/plan/confirm', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

/**
 * 调整学习计划（AI 辅助）
 * POST /api/parent/plan/adjust
 *
 * 【重要】此接口调用后端 AI Agent 调整学习计划
 */
export async function adjustPlan(req: AdjustPlanRequest): Promise<AdjustPlanResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(1200)
    const { mockParentPlans } = await import('../mock/parentData')
    const plan = mockParentPlans.find((p: LearningPlan) => p.id === req.planId)
    if (!plan) throw new ApiException(404, `Plan not found: ${req.planId}`)
    const match = req.adjustments.match(/(\d+)\s*题/)
    if (match) {
      const count = parseInt(match[1])
      plan.phases.forEach((p: PlanPhase) => { p.questionsPerDay = count })
    }
    return { plan, reply: '好的，我已经根据您的要求调整了学习计划。' }
  }

  return request<AdjustPlanResponse>('/api/parent/plan/adjust', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

/**
 * 暂停学习计划
 * POST /api/parent/plan/pause
 */
export async function pausePlan(req: PausePlanRequest): Promise<PausePlanResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(200)
    const { mockParentPlans } = await import('../mock/parentData')
    const plan = mockParentPlans.find((p: LearningPlan) => p.id === req.planId)
    if (plan) plan.status = 'paused'
    return { success: true }
  }

  return request<PausePlanResponse>('/api/parent/plan/pause', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

/**
 * 恢复学习计划
 * POST /api/parent/plan/resume
 */
export async function resumePlan(req: ResumePlanRequest): Promise<ResumePlanResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(200)
    const { mockParentPlans } = await import('../mock/parentData')
    const plan = mockParentPlans.find((p: LearningPlan) => p.id === req.planId)
    if (plan) plan.status = 'active'
    return { success: true }
  }

  return request<ResumePlanResponse>('/api/parent/plan/resume', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

/**
 * 取消学习计划
 * POST /api/parent/plan/cancel
 */
export async function cancelPlan(req: CancelPlanRequest): Promise<CancelPlanResponse> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(200)
    const { mockParentPlans } = await import('../mock/parentData')
    const plan = mockParentPlans.find((p: LearningPlan) => p.id === req.planId)
    if (plan) {
      plan.status = 'completed'
      plan.endedAt = new Date().toISOString().split('T')[0]
    }
    return { success: true }
  }

  return request<CancelPlanResponse>('/api/parent/plan/cancel', {
    method: 'POST',
    body: JSON.stringify(req),
  })
}

// ============================================================
// 家长模式：聊天消息
// ============================================================

/**
 * 发送家长聊天消息
 * POST /api/parent/chat
 *
 * 【重要】核心对话接口，后端需调用 AI Agent 识别意图
 */
export async function sendParentChat(
  message: string,
  planId?: string
): Promise<ParentMessage> {
  if (API_CONFIG.USE_MOCK) {
    const { mockSendParentChat } = await import('../mock/parentData')
    return mockSendParentChat(message, planId)
  }

  const req: ParentChatRequest = { message, planId }
  const resp = await request<ParentChatResponse>('/api/parent/chat', {
    method: 'POST',
    body: JSON.stringify(req),
  })

  return {
    id: `msg-${Date.now()}`,
    role: 'ai',
    content: resp.reply,
    type: resp.type ?? (resp.card ? 'card' : resp.plan ? 'card' : 'text'),
    card: resp.card,
    plan: resp.plan,
    planAction: resp.planAction,
    timestamp: new Date().toISOString(),
  }
}

// ============================================================
// 家长设置
// ============================================================

const PARENT_SETTINGS_KEY = 'kidslearn_parent_settings'

function readStoredParentSettings(): ParentSettings | null {
  if (!canUseStorage()) return null
  try {
    const raw = window.localStorage.getItem(PARENT_SETTINGS_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function persistParentSettings(settings: ParentSettings) {
  if (!canUseStorage()) return
  window.localStorage.setItem(PARENT_SETTINGS_KEY, JSON.stringify(settings))
}

/**
 * 获取家长设置
 * GET /api/parent/settings
 */
export async function getParentSettings(): Promise<ParentSettings> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(100)
    const { createInitialParentSettings } = await import('../mock/data')
    const stored = readStoredParentSettings()
    const defaults = createInitialParentSettings()
    // 合并：旧存储 + 新字段默认值
    return stored ? { ...defaults, ...stored } : defaults
  }

  const resp = await request<any>('/api/parent/settings')
  return {
    dailyLimitMinutes: resp.dailyLimitMinutes,
    dndEnabled: resp.dndEnabled,
    dndStart: resp.dndStart,
    dndEnd: resp.dndEnd,
    eyeCareReminderEnabled: resp.eyeCareReminder,
    breakReminderEnabled: resp.breakReminder,
    teacherName: resp.teacherName ?? '小星老师',
  }
}

/**
 * 更新家长设置
 * PUT /api/parent/settings
 */
export async function updateParentSettings(
  settings: Partial<ParentSettings>
): Promise<ParentSettings> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(200)
    const { createInitialParentSettings } = await import('../mock/data')
    const current = readStoredParentSettings() ?? createInitialParentSettings()
    const defaults = createInitialParentSettings()
    const updated = { ...defaults, ...current, ...settings }
    persistParentSettings(updated)
    return updated
  }

  const req: UpdateParentSettingsRequest = { ...settings }
  const resp = await request<any>('/api/parent/settings', {
    method: 'PUT',
    body: JSON.stringify(req),
  })
  // resp 已经是 unwrap 后的 settings 对象，需要映射字段名
  return {
    dailyLimitMinutes: resp.dailyLimitMinutes,
    dndEnabled: resp.dndEnabled,
    dndStart: resp.dndStart,
    dndEnd: resp.dndEnd,
    eyeCareReminderEnabled: resp.eyeCareReminder,
    breakReminderEnabled: resp.breakReminder,
    teacherName: resp.teacherName,
  } as ParentSettings
}

// ============================================================
// 日历/学习记录 API
// ============================================================

import type { MonthData, StudyDayData, Achievement, KnowledgeBadge, PetEvolution, PetFoodItem } from '../mock/data'

/**
 * 获取指定月份的学习数据
 * GET /api/children/{childId}/calendar/{year}/{month}
 *
 * 用于渲染日历热力图
 */
export async function getMonthCalendar(
  childId: string,
  year: number,
  month: number
): Promise<Record<string, MonthData>> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(200)
    const { createInitialCalendarData } = await import('../mock/data')
    const allData = createInitialCalendarData()
    const key = `${year}-${month}`
    // 返回包含该月数据的完整结构
    const result: Record<string, MonthData> = {}
    if (allData[key]) {
      result[key] = allData[key]
    }
    return result
  }

  // 后端返回 ApiResponse<{ year, month, days: { day: { heat, minutes, subjects } } }>
  // 前端期望 { "2026-3": { 1: {...}, 19: {...} } }
  const resp = await request<{ year: number; month: number; days: Record<number, { heat: number; minutes: number; subjects: import('../mock/data').StudySubjectRecord[] }> }>(
    `/api/children/${childId}/calendar/${year}/${month}`
  )
  if (!resp) {
    throw new Error(`日历接口返回空数据: childId=${childId}, year=${year}, month=${month}`)
  }
  const { year: y, month: m, days } = resp
  const key = `${y}-${m}`
  return { [key]: days } as Record<string, MonthData>
}


/**
 * 获取孩子档案（包含累计数据）
 * GET /api/children/{childId}/profile
 */
export async function getChildProfile(childId: string): Promise<{
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
}> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(150)
    const { mockChildData } = await import('../mock/data')
    return {
      id: mockChildData.id,
      name: mockChildData.name,
      age: mockChildData.age,
      ageGroup: mockChildData.ageGroup,
      stars: mockChildData.stars,
      streak: mockChildData.streak,
      todayMinutes: mockChildData.todayMinutes,
      level: 4,
      xp: 78,
      xpNeeded: 100,
    }
  }

  return request(`/api/children/${childId}/profile`)
}

/**
 * 获取指定日期的学习详情
 * GET /api/children/{childId}/calendar/daily/{date}
 */
export async function getDayStudyDetail(
  childId: string,
  date: string
): Promise<StudyDayData | null> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(100)
    const { createInitialCalendarData } = await import('../mock/data')
    const allData = createInitialCalendarData()
    const [year, month, day] = date.split('-').map(Number)
    const key = `${year}-${month}`
    return allData[key]?.[day] ?? null
  }

  const resp = await request<{ day: StudyDayData } | null>(
    `/api/children/${childId}/calendar/daily/${date}`
  )
  return resp?.day ?? null
}

// ============================================================
// 成就系统 API
// ============================================================

/**
 * 获取成就数据
 * GET /api/children/{childId}/achievements
 */
export async function getAchievements(
  childId: string,
  _req: { earnedIds: string[]; stats: { totalQuestions: number; currentStreak: number; bestStreak: number; todayCorrect: number; todayTotal: number } }
): Promise<{
  achievements: Achievement[]
  earnedIds: string[]
  stats: { totalQuestions: number; currentStreak: number; bestStreak: number; todayCorrect: number; todayTotal: number }
}> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(150)
    const { mockAchievements } = await import('../mock/data')
    return {
      achievements: mockAchievements,
      earnedIds: _req.earnedIds,
      stats: _req.stats,
    }
  }

  return request(`/api/children/${childId}/achievements`)
}

// ============================================================
// 知识星球徽章 API
// ============================================================

/**
 * 获取知识星球徽章数据
 * GET /api/children/{childId}/badges
 */
export async function getBadges(
  childId: string,
  _req: { earnedIds: string[]; totalAnswered: number }
): Promise<{
  badges: KnowledgeBadge[]
  earnedIds: string[]
  totalAnswered: number
  currentLevel: number
}> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(150)
    const { mockKnowledgeBadges } = await import('../mock/data')
    const level = Math.floor(_req.totalAnswered / 10) + 1
    return {
      badges: mockKnowledgeBadges,
      earnedIds: _req.earnedIds,
      totalAnswered: _req.totalAnswered,
      currentLevel: Math.min(level, 10),
    }
  }

  return request(`/api/children/${childId}/badges`)
}

// ============================================================
// 宠物系统 API
// ============================================================

/**
 * 获取宠物状态
 * GET /api/children/{childId}/pet
 */
export async function getPetState(
  childId: string,
  _req: { evolutionIndex: number; stats: import('../mock/data').PetStat[]; emotion: import('../mock/data').PetEmotion }
): Promise<{
  evolutionIndex: number
  stats: import('../mock/data').PetStat[]
  emotion: import('../mock/data').PetEmotion
  evolutions: PetEvolution[]
  foodItems: PetFoodItem[]
}> {
  if (API_CONFIG.USE_MOCK) {
    await sleep(150)
    const { mockPetEvolutions, mockPetFoodItems } = await import('../mock/data')
    return {
      evolutionIndex: _req.evolutionIndex,
      stats: _req.stats,
      emotion: _req.emotion,
      evolutions: mockPetEvolutions,
      foodItems: mockPetFoodItems,
    }
  }

  return request(`/api/children/${childId}/pet`)
}

/**
 * 喂食宠物
 * POST /api/children/{childId}/pet/feed
 */
export async function feedPet(
  childId: string,
  itemId: string
): Promise<{
  success: boolean
  newStars: number
  newStats: import('../mock/data').PetStat[]
}> {
  const { mockPetFoodItems } = await import('../mock/data')
  const store = useAppStore()
  const item = mockPetFoodItems.find((i) => i.name === itemId)
  const cost = item ? item.cost : 0

  if (API_CONFIG.USE_MOCK) {
    await sleep(200)
    if (!item || item.locked || (store.child?.stars ?? 0) < cost) {
      return { success: false, newStars: store.child?.stars ?? 0, newStats: store.petState.stats }
    }
    const newStars = (store.child?.stars ?? 0) - cost
    const newStats = store.petState.stats.map((s, i) =>
      i === item.statIndex ? { ...s, value: Math.min(100, s.value + 8) } : s
    )
    return { success: true, newStars, newStats }
  }

  try {
    const resp = await request<{ name: string; evolutionId: number; happiness: number; hunger: number; xp: number; xpToNext: number; childStars: number }>(`/api/children/${childId}/pet/feed`, {
      method: 'POST',
      body: JSON.stringify({ foodItemId: itemId }),
    })

    // 星星扣减和属性更新全部由后端处理，前端仅接收结果
    const xpPercent = resp.xpToNext > 0 ? Math.round((resp.xp / resp.xpToNext) * 100) : 100
    const newStats: import('../mock/data').PetStat[] = [
      { label: '饱食度', value: resp.hunger, color: '#4CAF50' },
      { label: '心情值', value: resp.happiness, color: '#FF9800' },
      { label: '经验值', value: xpPercent, color: '#2196F3' }
    ]
    return { success: true, newStars: resp.childStars, newStats }
  } catch (e) {
    return { success: false, newStars: store.child?.stars ?? 0, newStats: store.petState.stats }
  }
}

// ============================================================
// 导出类型
// ============================================================

export type { LearningPlan, ParentMessage, PlanPhase } from '../types/parent'
export type { Question, ParentSettings, MonthData, StudyDayData }
