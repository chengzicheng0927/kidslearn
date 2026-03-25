export interface Child {
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

export interface MockUser {
  exists: boolean
  child?: Child
}

export interface QuizOption {
  id: string
  text: string
  emoji?: string
}

export interface QuizReward {
  stars: number
  xp: number
  xpNeeded: number
  level: number
  badge?: {
    icon: string
    title: string
    sub: string
  }
}

export interface Question {
  id: string
  subject: 'en' | 'ma'
  successResultMode?: 'correct' | 'reward'
  label: string
  question: string
  options: QuizOption[]
  correctAnswerId: string
  explanation: string
  explanationSteps: string[]
  encouragement: string
  estimatedMinutes: number
  reward: QuizReward
}

export interface StudySubjectRecord {
  icon: string
  name: string
  topic: string
  stars: number
  accuracy: number
  color: string
  bg: string
}

export interface StudyDayData {
  heat: number
  minutes: number
  subjects: StudySubjectRecord[]
}

export interface MonthData {
  [day: number]: StudyDayData
}

export interface PetEvolution {
  emoji: string
  name: string
  label: string
  bg: string
}

export interface PetStat {
  label: string
  value: number
  color: string
}

export interface PetFoodItem {
  emoji: string
  name: string
  cost: number
  statIndex: number
  color: string
  locked: boolean
}

export type PetEmotion = 'idle' | 'happy' | 'clingy' | 'surprised' | 'sleepy' | 'hungry'

export interface PetState {
  evolutionIndex: number
  stats: PetStat[]
  emotion: PetEmotion
}

export const MONTHS = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
export const DAYS = ['日', '一', '二', '三', '四', '五', '六']
export const MONTH_DAYS = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]

export const mockInviteCodes = [
  'KL-ABCD-1234',
  'KL-1234-5678',
  'KL-XYZW-9876',
]

export const mockChildData: Child = {
  id: 'child-001',
  name: '小明',
  age: 8,
  ageGroup: '7-8 岁',
  stars: 286,
  streak: 7,
  todayMinutes: 24,
  dailyTargetEn: 5,
  dailyTargetMa: 3,
}

export const mockQuestions: Question[] = [
  {
    id: 'q1',
    subject: 'en',
    successResultMode: 'correct',
    label: '英语 · 动物词汇 · ⭐⭐',
    question: 'Which animal lives in the ocean and has eight arms?',
    options: [
      { id: 'q1-a', text: 'Shark', emoji: '🦈' },
      { id: 'q1-b', text: 'Octopus', emoji: '🐙' },
      { id: 'q1-c', text: 'Dolphin', emoji: '🐬' },
      { id: 'q1-d', text: 'Crab', emoji: '🦀' },
    ],
    correctAnswerId: 'q1-b',
    explanation: 'Octopus means 章鱼. It lives in the ocean and has eight arms.',
    explanationSteps: [
      '先看关键词 ocean，它说明动物生活在海里。',
      '再看 eight arms，表示它有八条手臂。',
      '四个选项里只有 Octopus 同时满足这两个条件，所以答案是 Octopus。',
    ],
    encouragement: '你已经抓到关键词啦，下次会更快答出来。',
    estimatedMinutes: 4,
    reward: {
      stars: 8,
      xp: 18,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q2',
    subject: 'en',
    successResultMode: 'reward',
    label: '英语 · 水果词汇 · ⭐⭐',
    question: 'Which fruit is red and wears a leafy crown?',
    options: [
      { id: 'q2-a', text: 'Apple', emoji: '🍎' },
      { id: 'q2-b', text: 'Banana', emoji: '🍌' },
      { id: 'q2-c', text: 'Orange', emoji: '🍊' },
      { id: 'q2-d', text: 'Strawberry', emoji: '🍓' },
    ],
    correctAnswerId: 'q2-d',
    explanation: 'Strawberries are red and their green leafy top looks like a tiny crown.',
    explanationSteps: [
      '题目先告诉我们 fruit，而且颜色是 red，所以先排除香蕉。',
      'wears a leafy crown 的意思是头上有绿色叶片，像戴着小皇冠。',
      'Strawberry 顶部正好有一圈绿色叶片，所以正确答案是 Strawberry。',
    ],
    encouragement: '你已经注意到颜色线索了，再把外形线索一起看就更棒了。',
    estimatedMinutes: 5,
    reward: {
      stars: 10,
      xp: 20,
      xpNeeded: 100,
      level: 4,
      badge: {
        icon: '🏅',
        title: '词汇小达人 徽章解锁！',
        sub: '累计完成 2 道英语词汇题',
      },
    },
  },
  {
    id: 'q3',
    subject: 'en',
    successResultMode: 'correct',
    label: '英语 · 颜色词汇 · ⭐',
    question: 'What color is the sky on a sunny day?',
    options: [
      { id: 'q3-a', text: 'Red', emoji: '🔴' },
      { id: 'q3-b', text: 'Blue', emoji: '🔵' },
      { id: 'q3-c', text: 'Green', emoji: '🟢' },
      { id: 'q3-d', text: 'Yellow', emoji: '🟡' },
    ],
    correctAnswerId: 'q3-b',
    explanation: 'On a clear sunny day, the sky looks blue.',
    explanationSteps: [
      '题目问的是 sunny day，也就是晴天。',
      '晴天时我们看到的天空通常是蓝色的。',
      '所以正确答案是 Blue。',
    ],
    encouragement: '这种生活里的题目很适合你，多观察就会越来越准。',
    estimatedMinutes: 3,
    reward: {
      stars: 6,
      xp: 14,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q4',
    subject: 'ma',
    successResultMode: 'correct',
    label: '数学 · 10 以内加法 · ⭐',
    question: '3 + 4 = ?',
    options: [
      { id: 'q4-a', text: '5' },
      { id: 'q4-b', text: '6' },
      { id: 'q4-c', text: '7' },
      { id: 'q4-d', text: '8' },
    ],
    correctAnswerId: 'q4-c',
    explanation: '3 plus 4 equals 7.',
    explanationSteps: [
      '从 3 开始往后数 4 个数：4、5、6、7。',
      '或者把 3 和 4 两部分合在一起，一共是 7。',
      '所以 3 + 4 = 7。',
    ],
    encouragement: '你已经很接近了，再多练几道加法题会更熟练。',
    estimatedMinutes: 3,
    reward: {
      stars: 7,
      xp: 15,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q5',
    subject: 'en',
    successResultMode: 'correct',
    label: '英语 · 食物词汇 · ⭐',
    question: 'What do you call a baby dog?',
    options: [
      { id: 'q5-a', text: 'Kitten', emoji: '🐱' },
      { id: 'q5-b', text: 'Calf', emoji: '🐮' },
      { id: 'q5-c', text: 'Puppy', emoji: '🐶' },
      { id: 'q5-d', text: 'Cub', emoji: '🐻' },
    ],
    correctAnswerId: 'q5-c',
    explanation: 'A baby dog is called a Puppy.',
    explanationSteps: [
      '题目问的是 baby dog，小狗的宝宝。',
      'Kitten 是小猫，Calf 是小牛，Cub 一般指熊宝宝。',
      'Puppy 正好是小狗的意思，所以答案是 Puppy。',
    ],
    encouragement: '动物宝宝的词汇很实用，多积累一些吧！',
    estimatedMinutes: 4,
    reward: {
      stars: 7,
      xp: 14,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q6',
    subject: 'en',
    successResultMode: 'correct',
    label: '英语 · 天气词汇 · ⭐',
    question: 'Which word describes the weather when the sun is shining and it is hot?',
    options: [
      { id: 'q6-a', text: 'Rainy', emoji: '🌧️' },
      { id: 'q6-b', text: 'Windy', emoji: '🌬️' },
      { id: 'q6-c', text: 'Sunny', emoji: '☀️' },
      { id: 'q6-d', text: 'Snowy', emoji: '❄️' },
    ],
    correctAnswerId: 'q6-c',
    explanation: 'Sunny describes hot weather with the sun shining.',
    explanationSteps: [
      '题目描述了两个线索：sun is shining 和 it is hot。',
      'Rainy 是下雨，Windy 是刮风，Snowy 是下雪。',
      'Sunny 正好描述晴天、太阳很晒的天气，所以答案是 Sunny。',
    ],
    encouragement: '天气词汇很实用，出门时记得用一用！',
    estimatedMinutes: 4,
    reward: {
      stars: 7,
      xp: 14,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q7',
    subject: 'en',
    successResultMode: 'reward',
    label: '英语 · 数字词汇 · ⭐⭐',
    question: 'How many sides does a triangle have?',
    options: [
      { id: 'q7-a', text: 'Two', emoji: '2️⃣' },
      { id: 'q7-b', text: 'Three', emoji: '3️⃣' },
      { id: 'q7-c', text: 'Four', emoji: '4️⃣' },
      { id: 'q7-d', text: 'Five', emoji: '5️⃣' },
    ],
    correctAnswerId: 'q7-b',
    explanation: 'Triangle means three angles, so it has three sides.',
    explanationSteps: [
      'Triangle 前缀 tri- 表示"三"。',
      '它代表有三个角的形状，自然就有三条边。',
      '所以正确答案是 Three。',
    ],
    encouragement: '你把形状和英语前缀结合起来了，真棒！',
    estimatedMinutes: 5,
    reward: {
      stars: 10,
      xp: 20,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q8',
    subject: 'en',
    successResultMode: 'correct',
    label: '英语 · 身体部位 · ⭐',
    question: 'Which body part do you use to hear sounds?',
    options: [
      { id: 'q8-a', text: 'Eyes', emoji: '👀' },
      { id: 'q8-b', text: 'Ears', emoji: '👂' },
      { id: 'q8-c', text: 'Nose', emoji: '👃' },
      { id: 'q8-d', text: 'Mouth', emoji: '👄' },
    ],
    correctAnswerId: 'q8-b',
    explanation: 'Ears are the body parts we use to hear sounds.',
    explanationSteps: [
      '题目问的是我们用什么来听声音。',
      'Eyes 是眼睛，用来看；Ears 是耳朵，用来听。',
      '所以正确答案是 Ears。',
    ],
    encouragement: '身体部位的词汇在日常交流中很有用！',
    estimatedMinutes: 3,
    reward: {
      stars: 6,
      xp: 12,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q15',
    subject: 'en',
    successResultMode: 'correct',
    label: '英语 · 衣服词汇 · ⭐',
    question: 'What do you wear on your feet when you go to school?',
    options: [
      { id: 'q15-a', text: 'Gloves', emoji: '🧤' },
      { id: 'q15-b', text: 'Socks', emoji: '🧦' },
      { id: 'q15-c', text: 'Shoes', emoji: '👟' },
      { id: 'q15-d', text: 'Hat', emoji: '🧢' },
    ],
    correctAnswerId: 'q15-c',
    explanation: 'Shoes are what we wear on our feet to go to school.',
    explanationSteps: [
      '题目问的是去学校时穿在脚上的东西。',
      'Gloves 是手套，Socks 是袜子，Shoes 是鞋子，Hat 是帽子。',
      '去学校需要穿鞋，正确答案是 Shoes。',
    ],
    encouragement: '衣物词汇也很有用，出门时可以认一认！',
    estimatedMinutes: 3,
    reward: {
      stars: 6,
      xp: 12,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q16',
    subject: 'ma',
    successResultMode: 'correct',
    label: '数学 · 简单应用题 · ⭐',
    question: '小明有 3 个苹果，小红又给了他 2 个，现在小明有多少个苹果？',
    options: [
      { id: 'q16-a', text: '4' },
      { id: 'q16-b', text: '5' },
      { id: 'q16-c', text: '6' },
      { id: 'q16-d', text: '7' },
    ],
    correctAnswerId: 'q16-b',
    explanation: '3 + 2 = 5，小明现在有 5 个苹果。',
    explanationSteps: [
      '小明原来有 3 个苹果。',
      '小红又给了他 2 个，所以用加法：3 + 2。',
      '3 + 2 = 5，答案是 5 个苹果。',
    ],
    encouragement: '应用题就是把生活变成数学题，你做得很棒！',
    estimatedMinutes: 4,
    reward: {
      stars: 8,
      xp: 16,
      xpNeeded: 100,
      level: 4,
    },
  },

  {
    id: 'q9',
    subject: 'ma',
    successResultMode: 'correct',
    label: '数学 · 10 以内减法 · ⭐',
    question: '9 - 3 = ?',
    options: [
      { id: 'q9-a', text: '4' },
      { id: 'q9-b', text: '5' },
      { id: 'q9-c', text: '6' },
      { id: 'q9-d', text: '7' },
    ],
    correctAnswerId: 'q9-c',
    explanation: '9 minus 3 equals 6.',
    explanationSteps: [
      '从 9 往前数 3 个数：8、7、6。',
      '所以 9 - 3 = 6。',
    ],
    encouragement: '减法学得真快！减法就是加法的逆运算。',
    estimatedMinutes: 3,
    reward: {
      stars: 6,
      xp: 14,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q10',
    subject: 'ma',
    successResultMode: 'reward',
    label: '数学 · 形状认知 · ⭐',
    question: 'How many corners does a square have?',
    options: [
      { id: 'q10-a', text: '3' },
      { id: 'q10-b', text: '4' },
      { id: 'q10-c', text: '5' },
      { id: 'q10-d', text: '6' },
    ],
    correctAnswerId: 'q10-b',
    explanation: 'A square has four equal sides and four corners.',
    explanationSteps: [
      '正方形有四条边，每两条边相交的地方就是一个角。',
      '正方形四条边，所以有四个角。',
      '答案是 4。',
    ],
    encouragement: '形状和数数的结合真棒！',
    estimatedMinutes: 3,
    reward: {
      stars: 8,
      xp: 16,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q11',
    subject: 'ma',
    successResultMode: 'correct',
    label: '数学 · 10 以内加法 · ⭐',
    question: '5 + 5 = ?',
    options: [
      { id: 'q11-a', text: '8' },
      { id: 'q11-b', text: '9' },
      { id: 'q11-c', text: '10' },
      { id: 'q11-d', text: '11' },
    ],
    correctAnswerId: 'q11-c',
    explanation: '5 plus 5 equals 10.',
    explanationSteps: [
      '两个 5 加在一起。',
      '5 + 5 = 10。',
    ],
    encouragement: '配对加法很方便，比如两只手各有5根手指！',
    estimatedMinutes: 2,
    reward: {
      stars: 5,
      xp: 12,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q12',
    subject: 'ma',
    successResultMode: 'correct',
    label: '数学 · 10 以内减法 · ⭐',
    question: '10 - 6 = ?',
    options: [
      { id: 'q12-a', text: '2' },
      { id: 'q12-b', text: '3' },
      { id: 'q12-c', text: '4' },
      { id: 'q12-d', text: '5' },
    ],
    correctAnswerId: 'q12-c',
    explanation: '10 minus 6 equals 4.',
    explanationSteps: [
      '10 减 6，从 10 往前数 6 个数：9、8、7、6、5、4。',
      '所以答案是 4。',
    ],
    encouragement: '大数减法也没难倒你！',
    estimatedMinutes: 4,
    reward: {
      stars: 7,
      xp: 15,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q13',
    subject: 'ma',
    successResultMode: 'correct',
    label: '数学 · 找规律 · ⭐⭐',
    question: '2, 4, 6, 8, ? — What comes next?',
    options: [
      { id: 'q13-a', text: '9' },
      { id: 'q13-b', text: '10' },
      { id: 'q13-c', text: '11' },
      { id: 'q13-d', text: '12' },
    ],
    correctAnswerId: 'q13-b',
    explanation: 'The pattern is adding 2 each time: 2, 4, 6, 8, 10.',
    explanationSteps: [
      '观察规律：2+2=4，4+2=6，6+2=8。',
      '每次加 2，所以下一个是 8+2=10。',
      '答案是 10。',
    ],
    encouragement: '找规律需要仔细观察，你做到了！',
    estimatedMinutes: 5,
    reward: {
      stars: 10,
      xp: 20,
      xpNeeded: 100,
      level: 4,
    },
  },
  {
    id: 'q14',
    subject: 'ma',
    successResultMode: 'correct',
    label: '数学 · 10 以内加法 · ⭐',
    question: '7 + 2 = ?',
    options: [
      { id: 'q14-a', text: '8' },
      { id: 'q14-b', text: '9' },
      { id: 'q14-c', text: '10' },
      { id: 'q14-d', text: '11' },
    ],
    correctAnswerId: 'q14-b',
    explanation: '7 plus 2 equals 9.',
    explanationSteps: [
      '从 7 往后数 2 个数：8、9。',
      '所以 7 + 2 = 9。',
    ],
    encouragement: '又答对了，继续加油！',
    estimatedMinutes: 2,
    reward: {
      stars: 5,
      xp: 12,
      xpNeeded: 100,
      level: 4,
    },
  },
]

export const mockCalendarData: Record<string, MonthData> = {
  '2026-3': {
    1: {
      heat: 1,
      minutes: 18,
      subjects: [
        { icon: '📖', name: '英语', topic: '动物词汇', stars: 8, accuracy: 80, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
        { icon: '🔢', name: '数学', topic: '10以内加法', stars: 6, accuracy: 90, color: '#69F0AE', bg: 'rgba(105,240,174,.12)' },
      ],
    },
    3: {
      heat: 2,
      minutes: 25,
      subjects: [
        { icon: '📖', name: '英语', topic: '颜色单词', stars: 12, accuracy: 88, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
      ],
    },
    5: {
      heat: 3,
      minutes: 38,
      subjects: [
        { icon: '📖', name: '英语', topic: '家庭成员', stars: 15, accuracy: 95, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
        { icon: '🔢', name: '数学', topic: '图形认知', stars: 10, accuracy: 83, color: '#69F0AE', bg: 'rgba(105,240,174,.12)' },
      ],
    },
    8: {
      heat: 2,
      minutes: 22,
      subjects: [
        { icon: '📖', name: '英语', topic: '食物词汇', stars: 11, accuracy: 86, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
      ],
    },
    10: {
      heat: 4,
      minutes: 52,
      subjects: [
        { icon: '📖', name: '英语', topic: '身体部位', stars: 14, accuracy: 92, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
        { icon: '🔢', name: '数学', topic: '20以内加法', stars: 12, accuracy: 88, color: '#69F0AE', bg: 'rgba(105,240,174,.12)' },
      ],
    },
    16: {
      heat: 4,
      minutes: 48,
      subjects: [
        { icon: '📖', name: '英语', topic: '职业词汇', stars: 16, accuracy: 94, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
        { icon: '🔢', name: '数学', topic: '乘法入门', stars: 14, accuracy: 82, color: '#69F0AE', bg: 'rgba(105,240,174,.12)' },
      ],
    },
    19: {
      heat: 4,
      minutes: 42,
      subjects: [
        { icon: '📖', name: '英语', topic: '动物词汇', stars: 12, accuracy: 92, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
        { icon: '🔢', name: '数学', topic: '应用题', stars: 15, accuracy: 88, color: '#69F0AE', bg: 'rgba(105,240,174,.12)' },
      ],
    },
  },
  '2025-3': {
    1: {
      heat: 1,
      minutes: 18,
      subjects: [
        { icon: '📖', name: '英语', topic: '动物词汇', stars: 8, accuracy: 80, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
        { icon: '🔢', name: '数学', topic: '10以内加法', stars: 6, accuracy: 90, color: '#69F0AE', bg: 'rgba(105,240,174,.12)' },
      ],
    },
    3: {
      heat: 2,
      minutes: 25,
      subjects: [
        { icon: '📖', name: '英语', topic: '颜色单词', stars: 12, accuracy: 88, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
      ],
    },
    5: {
      heat: 3,
      minutes: 38,
      subjects: [
        { icon: '📖', name: '英语', topic: '家庭成员', stars: 15, accuracy: 95, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
        { icon: '🔢', name: '数学', topic: '图形认知', stars: 10, accuracy: 83, color: '#69F0AE', bg: 'rgba(105,240,174,.12)' },
      ],
    },
    19: {
      heat: 4,
      minutes: 42,
      subjects: [
        { icon: '📖', name: '英语', topic: '动物词汇', stars: 12, accuracy: 92, color: '#40C4FF', bg: 'rgba(64,196,255,.12)' },
        { icon: '🔢', name: '数学', topic: '应用题', stars: 15, accuracy: 88, color: '#69F0AE', bg: 'rgba(105,240,174,.12)' },
      ],
    },
  },
}

export const mockPetEvolutions: PetEvolution[] = [
  { emoji: '🥚', name: '星蛋', label: '蛋 · 第 1 阶 · 即将孵化', bg: 'linear-gradient(135deg, #E8EAF6, #C5CAE9)' },
  { emoji: '🐣', name: '破壳橙', label: '幼崽 · 第 2 阶 · 成长中', bg: 'linear-gradient(135deg, #FFF9C4, #FFE082)' },
  { emoji: '🐱', name: '小橙子', label: '少年 · 第 3 阶 · 再答 28 题进化', bg: 'linear-gradient(135deg, #B3E5FC, #C8E6C9)' },
  { emoji: '🐯', name: '橙虎王', label: '成年 · 第 4 阶 · 威猛无比', bg: 'linear-gradient(135deg, #FFE0B2, #FFCC02)' },
  { emoji: '🦁', name: '星际狮王', label: '传说 · 第 5 阶 · 最终形态', bg: 'linear-gradient(135deg, #F3E5F5, #E1BEE7)' },
]

/** 宠物动态背景装饰配置 */
export const PET_ARENA_DECO: Record<number, { type: 'stars' | 'clouds' | 'grass' | 'forest' | 'galaxy'; emoji: string[]; label: string; bg: string }> = {
  0: { type: 'stars', emoji: ['✨', '⭐', '🌟', '💫'], label: '星云空间', bg: 'linear-gradient(135deg, #1a1a3e, #2d2d7a)' },
  1: { type: 'clouds', emoji: ['☁️', '🌤️', '🌈'], label: '晴朗天空', bg: 'linear-gradient(135deg, #B3E5FC, #E1F5FE)' },
  2: { type: 'grass', emoji: ['🌱', '🌿', '🌸'], label: '青青草地', bg: 'linear-gradient(135deg, #A5D6A7, #C8E6C9)' },
  3: { type: 'forest', emoji: ['🌲', '🍂', '🦋'], label: '金色森林', bg: 'linear-gradient(135deg, #FFE082, #FFD54F)' },
  4: { type: 'galaxy', emoji: ['🌌', '🪐', '✨', '💜'], label: '银河星际', bg: 'linear-gradient(135deg, #CE93D8, #BA68C8)' },
}

export const mockPetFoodItems: PetFoodItem[] = [
  { emoji: '🍎', name: '苹果', cost: 10, statIndex: 0, color: '#66BB6A', locked: false },
  { emoji: '🥛', name: '牛奶', cost: 15, statIndex: 1, color: '#29B6F6', locked: false },
  { emoji: '📚', name: '书本', cost: 20, statIndex: 2, color: '#CE93D8', locked: false },
  { emoji: '🍰', name: '蛋糕', cost: 30, statIndex: 0, color: '#F48FB1', locked: false },
  // 学习主题道具
  { emoji: '✏️', name: '铅笔', cost: 25, statIndex: 2, color: '#FF8A65', locked: true },
  { emoji: '🎒', name: '书包', cost: 50, statIndex: 0, color: '#40C4FF', locked: true },
  { emoji: '🏆', name: '奖杯', cost: 80, statIndex: 1, color: '#FFD54F', locked: true },
  { emoji: '🌟', name: '星星糖', cost: 40, statIndex: 1, color: '#FFCA28', locked: true },
]

export function getMockUser(deviceId: string): MockUser {
  if (!deviceId) {
    return { exists: false }
  }

  return { exists: false }
}

export function verifyInviteCode(code: string): boolean {
  return mockInviteCodes.includes(code.toUpperCase())
}

export function calcAgeFromId(id: string): number | null {
  if (!/^\d{17}[\dXx]$/.test(id)) return null
  const year = Number(id.slice(6, 10))
  const month = Number(id.slice(10, 12))
  const day = Number(id.slice(12, 14))
  const now = new Date()
  let age = now.getFullYear() - year

  if (now.getMonth() + 1 < month || (now.getMonth() + 1 === month && now.getDate() < day)) {
    age -= 1
  }

  return age >= 3 && age <= 14 ? age : null
}

export function getAgeGroup(age: number): { label: string; emoji: string } {
  if (age <= 4) return { label: '3-4 岁', emoji: '🐣' }
  if (age <= 6) return { label: '5-6 岁', emoji: '🐥' }
  if (age <= 8) return { label: '7-8 岁', emoji: '🐰' }
  if (age <= 10) return { label: '9-10 岁', emoji: '🦊' }
  return { label: '11-12 岁', emoji: '🦁' }
}

export function createMockChild(name: string, age: number, ageGroup: string): Child {
  return {
    id: `child-${Date.now()}`,
    name,
    age,
    ageGroup,
    stars: 0,
    streak: 1,
    todayMinutes: 0,
    dailyTargetEn: 5,
    dailyTargetMa: 3,
  }
}

export function createInitialCalendarData(): Record<string, MonthData> {
  return JSON.parse(JSON.stringify(mockCalendarData)) as Record<string, MonthData>
}

export function createInitialPetState(): PetState {
  return {
    evolutionIndex: 2,
    stats: [
      { label: '饱食', value: 72, color: '#66BB6A' },
      { label: '快乐', value: 85, color: '#29B6F6' },
      { label: '聪明', value: 60, color: '#CE93D8' },
    ],
    emotion: 'idle',
  }
}

export function cloneChild(child: Child): Child {
  return { ...child }
}

// ========== 知识星球徽章系统 ==========

/** 知识星球徽章：每完成 10 题解锁一个 */
export interface KnowledgeBadge {
  id: string
  level: number         // 第几级（1-10）
  icon: string          // Twemoji 图标
  title: string         // 徽章名称
  desc: string          // 描述
  requirement: number   // 解锁所需答题数
  earnedAt?: string     // 获得时间（mock 数据中已解锁的才有）
}

// 10 个知识星球徽章（每 10 题解锁一个）
export const mockKnowledgeBadges: KnowledgeBadge[] = [
  { id: 'badge-1',  level: 1,  icon: '🌱', title: '萌新之星',    desc: '完成第 1 关卡',      requirement: 10 },
  { id: 'badge-2',  level: 2,  icon: '📖', title: '学习小将',    desc: '完成第 2 关卡',      requirement: 20 },
  { id: 'badge-3',  level: 3,  icon: '🔭', title: '探索新星',    desc: '完成第 3 关卡',      requirement: 30 },
  { id: 'badge-4',  level: 4,  icon: '⚡', title: '知识闪电',    desc: '完成第 4 关卡',      requirement: 40 },
  { id: 'badge-5',  level: 5,  icon: '🌟', title: '智慧之星',    desc: '完成第 5 关卡',      requirement: 50 },
  { id: 'badge-6',  level: 6,  icon: '🚀', title: '成长火箭',    desc: '完成第 6 关卡',      requirement: 60 },
  { id: 'badge-7',  level: 7,  icon: '🏆', title: '学霸奖章',    desc: '完成第 7 关卡',      requirement: 70 },
  { id: 'badge-8',  level: 8,  icon: '💎', title: '钻石智慧',    desc: '完成第 8 关卡',      requirement: 80 },
  { id: 'badge-9',  level: 9,  icon: '👑', title: '知识之王',    desc: '完成第 9 关卡',      requirement: 90 },
  { id: 'badge-10', level: 10, icon: '🌌', title: '星际学者',    desc: '完成全部关卡',        requirement: 100 },
]

/** 徽章状态（模拟用户当前进度） */
export interface BadgeState {
  earnedBadges: string[]   // 已获得的徽章 ID 列表
  totalAnswered: number     // 当前累计答题数
}

export const mockBadgeState: BadgeState = {
  earnedBadges: ['badge-1', 'badge-2', 'badge-3'],
  totalAnswered: 27,        // 已答 27 题，处于第 4 关的进度中
}

export function createInitialBadgeState(): BadgeState {
  return { ...mockBadgeState, earnedBadges: [...mockBadgeState.earnedBadges] }
}

// ========== 家长模式设置 ==========

export interface ParentSettings {
  dailyLimitMinutes: number        // 每日学习时长限制（分钟）
  dndEnabled: boolean              // 勿扰模式是否开启
  dndStart: string                // 勿扰开始时间 HH:mm
  dndEnd: string                  // 勿扰结束时间 HH:mm
  eyeCareReminderEnabled: boolean // 护眼提醒是否开启
  breakReminderEnabled: boolean    // 休息提醒是否开启
  teacherName: string             // 老师名称
}

// ========== 成就系统 ==========

export interface Achievement {
  id: string
  category: 'streak' | 'accuracy' | 'volume' | 'special'  // 连击/准确率/题量/特殊
  icon: string
  title: string
  desc: string
  requirement: number  // 达成条件（如连续天数、正确率阈值等）
  type: 'streak_days' | 'accuracy_rate' | 'total_questions' | 'special'
  earnedAt?: string
}

// 成就列表
export const mockAchievements: Achievement[] = [
  // 连击成就
  { id: 'ach-streak-1', category: 'streak', icon: '🔥', title: '初露锋芒', desc: '连续答对 3 题', requirement: 3, type: 'streak_days' },
  { id: 'ach-streak-2', category: 'streak', icon: '🔥', title: '小宇宙爆发', desc: '连续答对 5 题', requirement: 5, type: 'streak_days' },
  { id: 'ach-streak-3', category: 'streak', icon: '🔥', title: '连胜达人', desc: '连续答对 10 题', requirement: 10, type: 'streak_days' },
  // 准确率成就
  { id: 'ach-acc-1', category: 'accuracy', icon: '🎯', title: '瞄靶精准', desc: '单次准确率 ≥ 80%', requirement: 80, type: 'accuracy_rate' },
  { id: 'ach-acc-2', category: 'accuracy', icon: '💯', title: '满分答卷', desc: '单次准确率 = 100%', requirement: 100, type: 'accuracy_rate' },
  // 题量成就
  { id: 'ach-vol-1', category: 'volume', icon: '📝', title: '学习新手', desc: '累计完成 10 题', requirement: 10, type: 'total_questions' },
  { id: 'ach-vol-2', category: 'volume', icon: '📚', title: '学习小将', desc: '累计完成 50 题', requirement: 50, type: 'total_questions' },
  { id: 'ach-vol-3', category: 'volume', icon: '🏆', title: '学习达人', desc: '累计完成 100 题', requirement: 100, type: 'total_questions' },
  // 特殊成就
  { id: 'ach-special-1', category: 'special', icon: '⭐', title: '首发命中', desc: '完成第一道题', requirement: 1, type: 'total_questions' },
  { id: 'ach-special-2', category: 'special', icon: '🌟', title: '连续 7 天学习', desc: '坚持学习 7 天', requirement: 7, type: 'streak_days' },
]

export interface AchievementState {
  earnedAchievements: string[]  // 已获得的成就 ID
  // 其他统计
  currentStreak: number         // 当前连续答对
  bestStreak: number            // 最佳连续答对
  totalQuestions: number        // 累计答题数
  todayCorrect: number          // 今日答对数
  todayTotal: number            // 今日答题数
}

export const mockAchievementState: AchievementState = {
  earnedAchievements: ['ach-special-1', 'ach-vol-1', 'ach-streak-1'],
  currentStreak: 2,
  bestStreak: 5,
  totalQuestions: 12,
  todayCorrect: 8,
  todayTotal: 10,
}

export function createInitialAchievementState(): AchievementState {
  return { ...mockAchievementState, earnedAchievements: [...mockAchievementState.earnedAchievements] }
}

export const mockParentSettings: ParentSettings = {
  dailyLimitMinutes: 30,
  dndEnabled: false,
  dndStart: '21:00',
  dndEnd: '08:00',
  eyeCareReminderEnabled: true,
  breakReminderEnabled: true,
  teacherName: '小星老师',
}

export const DAILY_LIMIT_OPTIONS = [
  { label: '15 分钟', value: 15 },
  { label: '30 分钟', value: 30 },
  { label: '45 分钟', value: 45 },
  { label: '60 分钟', value: 60 },
]

export function createInitialParentSettings(): ParentSettings {
  return { ...mockParentSettings }
}
