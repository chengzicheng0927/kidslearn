// ========== 家长模式状态管理 ==========

import { ref, computed } from 'vue'
import type { ParentMessage, LearningPlan } from '../types/parent'
import { sendParentChat, getParentPlans, confirmPlan, cancelPlan, generateQuestions } from '../services/api'

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
  const keywords = ['计划', '安排', '制定', '学习', '英语', '数学']
  return keywords.some(k => content.includes(k))
}

// 初始化：加载计划列表
async function initParentState() {
  const { plans, activePlan: active } = await getParentPlans()
  planHistory.value = plans
  activePlan.value = active ?? null
}

// 发送消息
async function sendMessage(content: string) {
  if (!content.trim() || isLoading.value) return

  const userMessage: ParentMessage = {
    id: `msg-${Date.now()}-user`,
    role: 'parent',
    content: content.trim(),
    type: 'text',
    timestamp: new Date().toISOString(),
  }

  messages.value.push(userMessage)
  isLoading.value = true

  // 如果需要生成学习计划，先显示 loading
  const loadingMsgId = needsPlanGeneration(content.trim())
    ? `msg-${Date.now()}-loading`
    : null

  if (loadingMsgId) {
    messages.value.push({
      id: loadingMsgId,
      role: 'ai',
      content: '',
      type: 'plan_loading',
      timestamp: new Date().toISOString(),
    })
  }

  try {
    const response = await sendParentChat(content, activePlan.value?.id, conversationId.value || undefined)

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

// 生成题目（用户确认计划后调用）
async function handleGenerateQuestions(plan: LearningPlan) {
  if (!plan.id || isGeneratingQuestions.value) return

  isGeneratingQuestions.value = true

  try {
    const response = await generateQuestions(plan.id, conversationId.value || undefined)

    // 更新 pendingPlan
    pendingPlan.value = response.plan

    // 找到包含该计划的消息，替换为带题目的完整卡片
    const planMsgIndex = messages.value.findIndex(m => m.plan?.id === plan.id)
    if (planMsgIndex !== -1) {
      messages.value[planMsgIndex] = {
        ...messages.value[planMsgIndex],
        content: response.reply,
        type: 'card',
        card: response.card,
        plan: response.plan,
      }
    }
  } catch (error) {
    messages.value.push({
      id: `msg-${Date.now()}-error`,
      role: 'ai',
      content: '抱歉，生成题目时发生了错误，请稍后重试。',
      type: 'error',
      timestamp: new Date().toISOString(),
    })
  } finally {
    isGeneratingQuestions.value = false
  }
}

// 确认计划
async function handleConfirmPlan(plan: LearningPlan) {
  try {
    await confirmPlan({ planId: plan.id })
    activePlan.value = plan
    pendingPlan.value = null

    messages.value.push({
      id: `msg-${Date.now()}-success`,
      role: 'ai',
      content: `太好了！${plan.duration} 天的${plan.subject === 'en' ? '英语' : '数学'}学习计划已经生效了。每天学习约 ${plan.estimatedMinutes} 分钟，加油！`,
      type: 'success',
      timestamp: new Date().toISOString(),
    })
  } catch (error) {
    messages.value.push({
      id: `msg-${Date.now()}-error`,
      role: 'ai',
      content: '确认计划时出错了，请稍后重试。',
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
