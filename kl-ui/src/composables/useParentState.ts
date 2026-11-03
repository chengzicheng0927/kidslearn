// ========== 家长模式状态管理 ==========

import { ref, computed } from 'vue'
import type { ParentMessage, LearningPlan } from '../types/parent'
import { sendParentChat, getParentPlans, confirmPlan, cancelPlan, generateQuestionsFromBank } from '../services/api'

// 消息列表
const messages = ref<ParentMessage[]>([])

// 加载状态
const isLoading = ref(false)

// 题目生成中状态
const isGeneratingQuestions = ref(false)

// 当前待确认的计划
const pendingPlan = ref<LearningPlan | null>(null)

// 活跃计划
const activePlan = ref<LearningPlan | null>(null)

// 历史计划
const planHistory = ref<LearningPlan[]>([])

// 当前对话ID（用于多轮对话）
const conversationId = ref<string | null>(null)

// 检测是否需要生成学习计划
function needsPlanGeneration(content: string): boolean {
  // 仅匹配明确的计划创建意图，排除"生成题目"等场景
  const planKeywords = ['计划', '安排', '制定']
  const subjectKeywords = ['英语', '数学']
  const hasSubject = subjectKeywords.some(k => content.includes(k))
  const hasPlanIntent = planKeywords.some(k => content.includes(k))
  // 有科目关键词时，需要同时有计划意图词
  if (hasSubject) return hasPlanIntent
  // 无科目时，仅计划意图词触发
  return hasPlanIntent
}

// 初始化：加载计划列表
async function initParentState() {
  const { plans, activePlan: active } = await getParentPlans()
  planHistory.value = plans
  activePlan.value = active ?? null
}

// 发送消息
async function sendMessage(content: string, skipPushUser = false, context?: { subject?: 'en' | 'ma' }) {
  if (!content.trim() || isLoading.value) return

  const userMessage: ParentMessage = {
    id: `msg-${Date.now()}-user`,
    role: 'parent',
    content: content.trim(),
    type: 'text',
    timestamp: new Date().toISOString(),
  }

  if (!skipPushUser) {
    messages.value.push(userMessage)
  }
  isLoading.value = true

  // 检查是否已有 plan_loading 消息（handleGenerateQuestions 会提前推送）
  const existingLoading = messages.value.find(m => m.type === 'plan_loading')
  const loadingMsgId = existingLoading?.id ?? (needsPlanGeneration(content.trim())
    ? `msg-${Date.now()}-loading`
    : null)

  if (loadingMsgId && !existingLoading) {
    messages.value.push({
      id: loadingMsgId,
      role: 'ai',
      content: '正在生成学习计划',
      type: 'plan_loading',
      timestamp: new Date().toISOString(),
    })
  }

  try {
    const response = await sendParentChat(content, activePlan.value?.id, conversationId.value || undefined, context)

    // 移除 loading 消息
    if (loadingMsgId) {
      const loadingIndex = messages.value.findIndex(m => m.id === loadingMsgId)
      if (loadingIndex !== -1) {
        messages.value.splice(loadingIndex, 1)
      }
    }

    // 保存对话ID（如果有）
    if (response.conversationId) {
      conversationId.value = response.conversationId
    }

    if (response.plan) {
      pendingPlan.value = response.plan
      messages.value.push(response)
    } else {
      messages.value.push(response)
    }
  } catch (error) {
    // 移除 loading 消息
    if (loadingMsgId) {
      const loadingIndex = messages.value.findIndex(m => m.id === loadingMsgId)
      if (loadingIndex !== -1) {
        messages.value.splice(loadingIndex, 1)
      }
    }

    messages.value.push({
      id: `msg-${Date.now()}-error`,
      role: 'ai',
      content: '抱歉，发生了错误，请稍后重试。',
      type: 'error',
      timestamp: new Date().toISOString(),
    })
  } finally {
    isLoading.value = false
  }
}

// 生成题目（用户确认计划后调用，从题库匹配）
async function handleGenerateQuestions(plan: LearningPlan) {
  if (!plan.id) return

  // 显示 loading 消息
  const loadingMsgId = `msg-${Date.now()}-loading`
  messages.value.push({
    id: loadingMsgId,
    role: 'ai',
    content: '正在为萌萌匹配练习题...',
    type: 'plan_loading',
    timestamp: new Date().toISOString(),
  })

  try {
    // 直接调 API 从题库获取题目（不走 LLM）
    const result = await generateQuestionsFromBank(plan.id)

    // 移除 loading 消息
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)

    if (result.success) {
      pendingPlan.value = result.plan

      const card: import('../types/parent').ChatCard = {
        style: 'plan',
        title: '题目已准备好啦！',
        name: `${result.plan.duration}天${result.plan.subject === 'en' ? '英语' : '数学'}学习计划`,
        icon: '🎉',
        rows: [
          { label: '科目', value: result.plan.subject === 'en' ? '英语' : '数学' },
          { label: '周期', value: `${result.plan.duration}天` },
          { label: '已匹配', value: `${result.totalQuestions}道练习题` },
        ],
        phases: result.plan.phases?.map((p: any) => ({
          ...p,
          questions: [],
        })) || [],
        actions: [
          { label: '确认计划', action: 'confirm_plan' },
          { label: '查看详情', action: 'view_plan_detail' },
        ],
      }

      messages.value.push({
        id: `msg-${Date.now()}-card`,
        role: 'ai',
        content: `太棒了！已为萌萌准备好了 ${result.totalQuestions} 道练习题，可以开始学习啦！`,
        type: 'card',
        card,
        plan: result.plan,
        timestamp: new Date().toISOString(),
      })
    } else {
      // 题库题目不足，显示提示
      const topicNames = result.insufficientTopicKeys.map(k => k.replace('MA_', '').replace('EN_', '')).join('、')
      messages.value.push({
        id: `msg-${Date.now()}-warn`,
        role: 'ai',
        content: `哎呀，${result.insufficientTopicKeys.length} 个学习主题的题目还没准备好呢（${topicNames}）。别担心，这些内容很快就会上线！您可以先试试其他主题吗？`,
        type: 'text',
        timestamp: new Date().toISOString(),
      })
    }
  } catch (error) {
    // 移除 loading 消息
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)

    messages.value.push({
      id: `msg-${Date.now()}-error`,
      role: 'ai',
      content: '匹配题目时出了点小问题，请稍后再试一次~',
      type: 'text',
      timestamp: new Date().toISOString(),
    })
  }
}

// 确认计划
async function handleConfirmPlan(plan: LearningPlan) {
  try {
    const resp = await confirmPlan({ planId: plan.id })
    activePlan.value = resp.plan
    pendingPlan.value = null

    messages.value.push({
      id: `msg-${Date.now()}-success`,
      role: 'ai',
      content: `太好了！${resp.plan.duration} 天的${resp.plan.subject === 'en' ? '英语' : '数学'}学习计划已经生效了。每天学习约 ${resp.plan.estimatedMinutes} 分钟，加油！`,
      type: 'success',
      timestamp: new Date().toISOString(),
    })
  } catch (error) {
    messages.value.push({
      id: `msg-${Date.now()}-error`,
      role: 'ai',
      content: '哎呀，确认计划时出了点小问题，请稍后再试一次~',
      type: 'error',
      timestamp: new Date().toISOString(),
    })
  }
}

// 取消计划
async function handleCancelPlan() {
  if (pendingPlan.value) {
    try {
      await cancelPlan({ planId: pendingPlan.value.id })
      // 刷新计划列表
      const { plans, activePlan: active } = await getParentPlans()
      planHistory.value = plans
      activePlan.value = active ?? null
    } catch {
      // 忽略错误
    }
  }
  pendingPlan.value = null

  messages.value.push({
    id: `msg-${Date.now()}-cancel`,
    role: 'ai',
    content: '好的，计划已取消。如果您有其他需求，请随时告诉我。',
    type: 'text',
    timestamp: new Date().toISOString(),
  })
}

// 重置状态（退出家长模式时调用）
function resetParentState() {
  messages.value = []
  pendingPlan.value = null
  isLoading.value = false
  isGeneratingQuestions.value = false
  conversationId.value = null
}

export function useParentState() {
  return {
    // 状态
    messages,
    isLoading,
    isGeneratingQuestions,
    pendingPlan,
    activePlan,
    planHistory,
    conversationId,

    // 计算属性
    hasPendingPlan: computed(() => pendingPlan.value !== null),

    // 方法
    initParentState,
    sendMessage,
    handleGenerateQuestions,
    handleConfirmPlan,
    handleCancelPlan,
    resetParentState,
  }
}
