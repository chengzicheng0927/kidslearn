import type { Question } from '../services/api'

export interface ChatRecord {
  id: string
  role: 'ai' | 'user'
  content: string
  avatar?: string
  source?: 'typed' | 'voice' | 'push' | 'system'
  action?: string
  subject?: 'en' | 'ma'
}

export interface AnsweredQuestion {
  question: Question
  selectedAnswerId: string
  isCorrect: boolean
  correctAnswerId: string
}

/** 单个学科的单日答题记录 */
export interface SubjectQuizRecord {
  date: string          // YYYY-MM-DD
  subject: 'en' | 'ma'
  questions: AnsweredQuestion[]
  questionIndex: number // 当前做到第几题（下一次取题的起始位置）
  stars: number         // 当日累计星星
  lastUpdated: number   // 时间戳
}

/** 存储 key */
const STORAGE_KEY = 'kidslearn_quiz_records'

function todayKey(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function storageKey(subject: 'en' | 'ma'): string {
  return `${STORAGE_KEY}_${todayKey()}_${subject}`
}

/** 加载某学科今日记录（没有则返回 null） */
export function loadSubjectRecord(subject: 'en' | 'ma'): SubjectQuizRecord | null {
  try {
    const raw = localStorage.getItem(storageKey(subject))
    if (!raw) return null
    const record: SubjectQuizRecord = JSON.parse(raw)
    // 检查是否是今天
    if (record.date !== todayKey()) return null
    return record
  } catch {
    return null
  }
}

/** 保存某学科今日记录 */
export function saveSubjectRecord(record: SubjectQuizRecord): void {
  try {
    localStorage.setItem(storageKey(record.subject), JSON.stringify(record))
  } catch {
    // storage full, ignore
  }
}

/** 新建空的单科记录 */
export function createEmptyRecord(subject: 'en' | 'ma'): SubjectQuizRecord {
  return {
    date: todayKey(),
    subject,
    questions: [],
    questionIndex: 0,
    stars: 0,
    lastUpdated: Date.now(),
  }
}

/** 向记录追加答题结果 */
export function appendAnswerToRecord(
  record: SubjectQuizRecord,
  answered: AnsweredQuestion,
  starsEarned: number
): SubjectQuizRecord {
  return {
    ...record,
    questions: [...record.questions, answered],
    questionIndex: record.questionIndex + 1,
    stars: record.stars + starsEarned,
    lastUpdated: Date.now(),
  }
}
