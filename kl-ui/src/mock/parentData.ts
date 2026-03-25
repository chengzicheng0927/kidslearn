// ========== 家长模式 Mock 数据 ==========

import type { ChatCard, LearningPlan, ParentMessage, PlanPhase, PhaseDetail, QuestionDetail } from '../types/parent'

export const mockParentPlans: LearningPlan[] = [
  {
    id: 'plan-001',
    subject: 'en',
    duration: 14,
    dailyGoal: '每天完成 5 道英语题',
    phases: [
      { dayRange: '1-3', goal: '掌握 10 个动物词汇', topic: '动物', questionsPerDay: 5, difficulty: 'easy' },
      { dayRange: '4-7', goal: '掌握 10 个水果词汇', topic: '水果', questionsPerDay: 5, difficulty: 'easy' },
      { dayRange: '8-11', goal: '掌握 10 个颜色词汇', topic: '颜色', questionsPerDay: 5, difficulty: 'easy' },
      { dayRange: '12-14', goal: '复习巩固 + 小测验', topic: '复习', questionsPerDay: 6, difficulty: 'medium' },
    ],
    estimatedMinutes: 20,
    difficulty: 'easy',
    status: 'active',
    createdAt: '2026-03-15',
    startedAt: '2026-03-15',
  },
]

// 模拟 AI 生成计划
export async function mockGeneratePlan(prompt: string): Promise<LearningPlan> {
  await new Promise((resolve) => setTimeout(resolve, 1500)) // 模拟 AI 思考

  const subject = prompt.includes('数学') ? 'ma' : 'en'
  const subjectLabel = subject === 'en' ? '英语' : '数学'
  const duration = extractDuration(prompt) || 14

  const phases = generatePhases(subject, duration)

  return {
    id: `plan-${Date.now()}`,
    subject,
    duration,
    dailyGoal: `每天完成 5 道${subjectLabel}题`,
    phases,
    estimatedMinutes: 20,
    difficulty: 'medium',
    status: 'pending',
    createdAt: new Date().toISOString(),
  }
}

function extractDuration(prompt: string): number | null {
  const match = prompt.match(/(\d+)\s*[天日]/)
  return match ? parseInt(match[1]) : null
}

function generatePhases(subject: 'en' | 'ma', duration: number): PlanPhase[] {
  const topics = subject === 'en'
    ? [
        { dayRange: '1-3', goal: '掌握 10 个动物词汇', topic: '动物' },
        { dayRange: '4-7', goal: '掌握 10 个水果词汇', topic: '水果' },
        { dayRange: '8-11', goal: '掌握 10 个颜色词汇', topic: '颜色' },
        { dayRange: '12-14', goal: '复习巩固 + 小测验', topic: '复习' },
      ]
    : [
        { dayRange: '1-3', goal: '掌握 10 以内加减法', topic: '基础计算' },
        { dayRange: '4-7', goal: '学习简单的应用题', topic: '应用题' },
        { dayRange: '8-11', goal: '学习形状和空间', topic: '几何入门' },
        { dayRange: '12-14', goal: '复习巩固 + 小测验', topic: '复习' },
      ]

  // 根据 duration 调整阶段
  if (duration <= 7) {
    return [
      { ...topics[0], dayRange: '1-3', questionsPerDay: 6, difficulty: 'easy' },
      { ...topics[topics.length - 1], dayRange: duration <= 3 ? '4-7' : '4-7', questionsPerDay: 6, difficulty: 'medium' },
    ]
  }

  return topics.map((t, i) => ({
    ...t,
    questionsPerDay: i === topics.length - 1 ? 6 : 5,
    difficulty: i === topics.length - 1 ? 'medium' : 'easy',
  }))
}

// 生成样题
function generateSampleQuestions(subject: 'en' | 'ma', topic?: string, phaseIndex?: number): QuestionDetail[] {
  if (subject === 'en') {
    const questionBanks: Record<string, QuestionDetail[]> = {
      '动物': [
        { id: 'q1', icon: '🐱', question: 'cat', options: ['猫', '狗', '兔子', '鸟'], answer: '猫' },
        { id: 'q2', icon: '🐕', question: 'dog', options: ['猫', '狗', '鱼', '鸭子'], answer: '狗' },
        { id: 'q3', icon: '🐘', question: 'elephant', options: ['大象', '狮子', '老虎', '熊猫'], answer: '大象' },
        { id: 'q4', icon: '🦁', question: 'lion', options: ['熊', '狮子', '狼', '狐狸'], answer: '狮子' },
        { id: 'q5', icon: '🐰', question: 'rabbit', options: ['猫', '狗', '兔子', '仓鼠'], answer: '兔子' },
      ],
      '水果': [
        { id: 'q1', icon: '🍎', question: 'apple', options: ['苹果', '香蕉', '橙子', '葡萄'], answer: '苹果' },
        { id: 'q2', icon: '🍌', question: 'banana', options: ['苹果', '香蕉', '草莓', '西瓜'], answer: '香蕉' },
        { id: 'q3', icon: '🍊', question: 'orange', options: ['柠檬', '橙子', '柚子', '橘子'], answer: '橙子' },
        { id: 'q4', icon: '🍇', question: 'grape', options: ['葡萄', '蓝莓', '樱桃', '草莓'], answer: '葡萄' },
        { id: 'q5', icon: '🍓', question: 'strawberry', options: ['苹果', '香蕉', '草莓', '桃子'], answer: '草莓' },
      ],
      '颜色': [
        { id: 'q1', icon: '🔴', question: 'red', options: ['红色', '蓝色', '绿色', '黄色'], answer: '红色' },
        { id: 'q2', icon: '🔵', question: 'blue', options: ['红色', '蓝色', '紫色', '橙色'], answer: '蓝色' },
        { id: 'q3', icon: '🟢', question: 'green', options: ['黄色', '蓝色', '绿色', '紫色'], answer: '绿色' },
        { id: 'q4', icon: '🟡', question: 'yellow', options: ['红色', '蓝色', '黄色', '黑色'], answer: '黄色' },
        { id: 'q5', icon: '🟣', question: 'purple', options: ['粉色', '蓝色', '紫色', '棕色'], answer: '紫色' },
      ],
      '复习': [
        { id: 'q1', icon: '📝', question: 'apple', options: ['苹果', '香蕉', '橙子', '葡萄'], answer: '苹果' },
        { id: 'q2', icon: '📝', question: 'dog', options: ['猫', '狗', '鱼', '鸭子'], answer: '狗' },
        { id: 'q3', icon: '📝', question: 'red', options: ['红色', '蓝色', '绿色', '黄色'], answer: '红色' },
        { id: 'q4', icon: '📝', question: 'cat', options: ['猫', '狗', '兔子', '鸟'], answer: '猫' },
        { id: 'q5', icon: '📝', question: 'blue', options: ['红色', '蓝色', '紫色', '橙色'], answer: '蓝色' },
      ],
    }
    const bank = questionBanks[topic || '复习'] || questionBanks['复习']
    return bank
  } else {
    // 数学题
    const mathBanks: Record<string, QuestionDetail[]> = {
      '基础计算': [
        { id: 'q1', icon: '1️⃣', question: '1 + 1 = ?', options: ['1', '2', '3', '4'], answer: '2' },
        { id: 'q2', icon: '2️⃣', question: '2 + 3 = ?', options: ['4', '5', '6', '7'], answer: '5' },
        { id: 'q3', icon: '3️⃣', question: '5 - 2 = ?', options: ['2', '3', '4', '5'], answer: '3' },
        { id: 'q4', icon: '4️⃣', question: '4 + 1 = ?', options: ['4', '5', '6', '3'], answer: '5' },
        { id: 'q5', icon: '5️⃣', question: '3 + 2 = ?', options: ['4', '6', '5', '3'], answer: '5' },
      ],
      '应用题': [
        { id: 'q1', icon: '🍎', question: '小明有3个苹果，又买了2个，一共几个？', options: ['3个', '4个', '5个', '6个'], answer: '5个' },
        { id: 'q2', icon: '📦', question: '盒子里有4个球，拿走1个，还剩几个？', options: ['2个', '3个', '4个', '5个'], answer: '3个' },
        { id: 'q3', icon: '🌸', question: '花园里有5朵花，开了2朵，开了几朵？', options: ['2朵', '3朵', '4朵', '5朵'], answer: '2朵' },
      ],
      '几何入门': [
        { id: 'q1', icon: '⬜', question: '这个形状有几个角？', options: ['3个', '4个', '5个', '没有角'], answer: '4个' },
        { id: 'q2', icon: '🔺', question: '这个形状叫什么？', options: ['圆形', '三角形', '正方形', '长方形'], answer: '三角形' },
        { id: 'q3', icon: '⭕', question: '这个形状有什么特点？', options: ['有角', '没有角', '有4个角', '有3个角'], answer: '没有角' },
      ],
      '复习': [
        { id: 'q1', icon: '📝', question: '2 + 3 = ?', options: ['4', '5', '6', '7'], answer: '5' },
        { id: 'q2', icon: '📝', question: '5 - 1 = ?', options: ['3', '4', '5', '6'], answer: '4' },
        { id: 'q3', icon: '📝', question: '1 + 4 = ?', options: ['4', '5', '6', '3'], answer: '5' },
      ],
    }
    const bank = mathBanks[topic || '复习'] || mathBanks['复习']
    return bank
  }
}

// 模拟发送家长聊天消息
export async function mockSendParentChat(
  message: string,
  _currentPlanId?: string
): Promise<ParentMessage> {
  await new Promise((resolve) => setTimeout(resolve, 800 + Math.random() * 700))

  // 检测是否要生成计划
  const shouldGeneratePlan =
    message.includes('计划') ||
    message.includes('安排') ||
    message.includes('制定') ||
    message.includes('学习') ||
    message.includes('英语') ||
    message.includes('数学')

  if (shouldGeneratePlan) {
    // 等待 AI 思考后返回计划
    await new Promise((resolve) => setTimeout(resolve, 1500))

    const plan = await mockGeneratePlan(message)

    const reply = `好的，我来为孩子制定一个 ${plan.duration} 天的${plan.subject === 'en' ? '英语' : '数学'}学习计划。\n\n这个计划包含 ${plan.phases.length} 个阶段，每天大约 ${plan.estimatedMinutes} 分钟，适合当前年龄段的孩子。`

    // 构建卡片配置
    const card: ChatCard = {
      style: 'plan',
      title: '学习计划',
      name: `${plan.duration}天${plan.subject === 'en' ? '英语' : '数学'}学习计划`,
      icon: '📋',
      rows: [
        { label: '科目', value: plan.subject === 'en' ? '英语' : '数学' },
        { label: '周期', value: `${plan.duration}天` },
        { label: '难度', value: plan.difficulty === 'easy' ? '基础' : plan.difficulty === 'medium' ? '中等' : '困难' },
        { label: '每日时长', value: `约${plan.estimatedMinutes}分钟` },
      ],
      phases: plan.phases.map((p, idx) => ({
        dayRange: p.dayRange,
        topic: p.topic,
        questionsPerDay: p.questionsPerDay,
        difficulty: p.difficulty,
        questions: generateSampleQuestions(plan.subject, p.topic, idx),
      })),
      actions: [
        { label: '查看详情', action: 'view_plan_detail' },
        { label: '确认计划', action: 'confirm_plan' },
      ],
    }

    return {
      id: `msg-${Date.now()}`,
      role: 'ai',
      content: reply,
      type: 'card',
      card,
      plan,
      timestamp: new Date().toISOString(),
    }
  }

  // 检测取消计划
  if (message.includes('取消') && message.includes('计划')) {
    return {
      id: `msg-${Date.now()}`,
      role: 'ai',
      content: '好的，我已经取消了当前的学习计划。如果您有其他需求，请随时告诉我。',
      type: 'success',
      timestamp: new Date().toISOString(),
    }
  }

  // 默认回复
  const defaultReplies = [
    '好的，我会根据您的需求来安排孩子的学习计划。请告诉我您想要什么样的计划？比如：「制定一个14天的英语学习计划」',
    '明白了，我会帮您安排孩子的学习。您想要侧重哪个学科呢？英语还是数学？',
    '好的，您可以通过对话告诉我您的需求。比如设置学习周期、每日题量、学科偏好等。',
  ]

  return {
    id: `msg-${Date.now()}`,
    role: 'ai',
    content: defaultReplies[Math.floor(Math.random() * defaultReplies.length)],
    type: 'text',
    timestamp: new Date().toISOString(),
  }
}
