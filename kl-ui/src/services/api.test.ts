import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAppStore } from '../stores/appStore'
import {
  activateAccountWithCode,
  checkUserStatus,
  clearStoredMockUser,
  getCurrentDeviceId,
  getNextQuestion,
  registerChild,
  resetQuestions,
  submitAnswer,
  submitIdCard,
  validateInviteCode,
} from './api'

describe('mock api service', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-19T09:00:00Z'))
    localStorage.clear()
    // 强制启用 mock 模式，测试不依赖真实后端
    // 必须在 Pinia 创建前设置，否则 calendarData 在 store 创建时就已为空
    import('../config/api.config').then(({ API_CONFIG }) => {
      ;(API_CONFIG as any).USE_MOCK = true
    })
    setActivePinia(createPinia())
    clearStoredMockUser()
    useAppStore().resetMockState()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('activates a child and persists the mock user', async () => {
    const childPromise = activateAccountWithCode('KL-ABCD-1234', '110101201803180018', '豆豆')
    await vi.runAllTimersAsync()
    const child = await childPromise

    expect(child.name).toBe('豆豆')
    expect(useAppStore().child?.name).toBe('豆豆')

    const statusPromise = checkUserStatus()
    await vi.runAllTimersAsync()
    await expect(statusPromise).resolves.toMatchObject({
      exists: true,
      child: expect.objectContaining({ name: '豆豆' }),
    })
  })

  it('returns onboarding state when no stored child exists', async () => {
    const statusPromise = checkUserStatus()
    await vi.runAllTimersAsync()
    await expect(statusPromise).resolves.toEqual({ exists: false })
  })

  it('returns explicit correct answers instead of assuming the second option', async () => {
    useAppStore().setChild({
      id: 'child-100',
      name: '小明',
      age: 8,
      ageGroup: '7-8 岁',
      stars: 0,
      streak: 3,
      todayMinutes: 0,
    })

    resetQuestions()
    const questionPromise = getNextQuestion()
    await vi.runAllTimersAsync()
    const firstQuestion = await questionPromise

    expect(firstQuestion.question?.correctAnswerId).toBe('q1-b')

    const firstResultPromise = submitAnswer('q1', 'q1-b', 4)
    await vi.runAllTimersAsync()
    await expect(firstResultPromise).resolves.toMatchObject({
      isCorrect: true,
      resultMode: 'correct',
    })

    const secondQuestionPromise = getNextQuestion()
    await vi.runAllTimersAsync()
    await expect(secondQuestionPromise).resolves.toMatchObject({ question: { id: 'q2' } })

    const resultPromise = submitAnswer('q2', 'q2-d', 5)
    await vi.runAllTimersAsync()
    const result = await resultPromise

    expect(result.isCorrect).toBe(true)
    expect(result.resultMode).toBe('reward')
    expect(result.correctAnswerId).toBe('q2-d')
    // 本题奖励 = q2.reward.stars = 10；累计今日星星由 store 的 todayStars 管理
    expect(result.result?.stars).toBe(10)
    // 验证累计今日星星正确累加（q1 8 + q2 10 = 18）
    const store = useAppStore()
    expect(store.todayStars).toBe(18)
  })

  it('keeps correct answers on non-reward questions from opening the normal modal mid-session', async () => {
    useAppStore().setChild({
      id: 'child-101',
      name: '可可',
      age: 8,
      ageGroup: '7-8 岁',
      stars: 0,
      streak: 1,
      todayMinutes: 0,
    })

    resetQuestions()
    const questionPromise = getNextQuestion()
    await vi.runAllTimersAsync()
    await questionPromise

    const resultPromise = submitAnswer('q1', 'q1-b', 4)
    await vi.runAllTimersAsync()
    const result = await resultPromise

    expect(result.isCorrect).toBe(true)
    expect(result.resultMode).toBe('correct')
    expect(result.showResult).toBe(false)
  })

  it('covers validation and edge branches for the mock api', async () => {
    const validCodePromise = validateInviteCode('KL-ABCD-1234')
    const invalidCodePromise = validateInviteCode('KL-FAIL-0000')
    const validIdPromise = submitIdCard('110101201803180018')
    const invalidIdPromise = submitIdCard('bad-id')
    await vi.runAllTimersAsync()

    await expect(validCodePromise).resolves.toEqual({ valid: true, message: '邀请码有效' })
    await expect(invalidCodePromise).resolves.toEqual({ valid: false, message: '邀请码无效或已过期' })
    await expect(validIdPromise).resolves.toMatchObject({ valid: true, age: 8, ageGroup: '7-8 岁' })
    await expect(invalidIdPromise).resolves.toEqual({ valid: false })

    const registeredPromise = registerChild('萌萌', 7, '7-8 岁')
    await vi.runAllTimersAsync()
    await expect(registeredPromise).resolves.toMatchObject({ name: '萌萌', age: 7 })

    expect(getCurrentDeviceId()).toMatch(/^device-/)

    const badActivatePromise = activateAccountWithCode('KL-FAIL-0000', '110101201803180018', '乐乐')
    badActivatePromise.catch(() => undefined)
    await vi.runAllTimersAsync()
    await expect(badActivatePromise).rejects.toThrow('邀请码无效或已过期')

    const invalidIdActivatePromise = activateAccountWithCode('KL-ABCD-1234', 'bad-id', '乐乐')
    invalidIdActivatePromise.catch(() => undefined)
    await vi.runAllTimersAsync()
    await expect(invalidIdActivatePromise).rejects.toThrow('身份证号格式不正确')

    const badQuestionPromise = submitAnswer('missing-question', 'x', 1)
    badQuestionPromise.catch(() => undefined)
    await vi.runAllTimersAsync()
    await expect(badQuestionPromise).rejects.toThrow('Question not found')

    resetQuestions()
    const mathQuestionPromise = getNextQuestion('ma')
    await vi.runAllTimersAsync()
    await expect(mathQuestionPromise).resolves.toMatchObject({ question: { id: 'q4' } })

    // 新的实现使用全局索引，每次调用递增，
    // 第二次调用返回 questionBank['ma'][1]，即 q16
    const secondMathQuestionPromise = getNextQuestion('ma')
    await vi.runAllTimersAsync()
    await expect(secondMathQuestionPromise).resolves.toMatchObject({ question: { id: 'q16' } })
  })
})
