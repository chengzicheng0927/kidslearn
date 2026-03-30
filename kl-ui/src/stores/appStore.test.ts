import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { getSelectedStudyDay, useAppStore } from './appStore'
import { mockChildData, mockPetFoodItems, mockQuestions } from '../mock/data'

vi.mock('../config/api.config', () => ({
  API_CONFIG: { USE_MOCK: true, API_BASE_URL: 'http://localhost:8080', TIMEOUT: 30000, DEBUG: false },
  getApiMode: () => 'mock' as const,
}))

vi.mock('../services/api', () => ({
  feedPet: vi.fn().mockResolvedValue({ success: true, newStars: 276, newStats: [
    { label: '饱食度', value: 80, color: '#4CAF50' },
    { label: '心情值', value: 80, color: '#FF9800' },
    { label: '经验值', value: 72, color: '#2196F3' },
  ]}),
}))

describe('useAppStore', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-19T09:00:00Z'))
    setActivePinia(createPinia())
    const store = useAppStore()
    store.resetMockState()
    store.setChild(mockChildData)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('records a correct answer into shared child and calendar state', () => {
    const store = useAppStore()
    const question = mockQuestions[0]

    store.recordAnswer(question, true)

    expect(store.child?.stars).toBe(mockChildData.stars + question.reward.stars)
    expect(store.child?.todayMinutes).toBe(mockChildData.todayMinutes + question.estimatedMinutes)
    expect(store.quizSession.combo).toBe(1)
    expect(store.calendarData['2026-3']?.[19]?.subjects[0]?.topic).toBe('动物词汇')
  })

  it('resets combo on wrong answer', () => {
    const store = useAppStore()
    store.recordAnswer(mockQuestions[0], true)
    store.recordAnswer(mockQuestions[1], false)

    expect(store.quizSession.correctCount).toBe(1)
    expect(store.quizSession.combo).toBe(0)
  })

  it('feeds pet by consuming shared stars', async () => {
    const store = useAppStore()
    const before = store.child?.stars ?? 0
    const success = await store.feedPet(mockPetFoodItems[0])

    expect(success).toBe(true)
    expect(store.child?.stars).toBe(before - mockPetFoodItems[0].cost)
    expect(store.petState.stats[0].value).toBe(80)
  })

  it('handles evolution bounds and selected day lookup', () => {
    const store = useAppStore()

    // 初始为 2；setPetEvolution(1) < 2，无效，保持 2
    store.setPetEvolution(1)
    expect(store.petState.evolutionIndex).toBe(2)

    // setPetEvolution(4) >= 2，有效，设为 4
    store.setPetEvolution(4)
    expect(store.petState.evolutionIndex).toBe(4)

    expect(store.childStars).toBe(mockChildData.stars)
    expect(store.childStreak).toBe(mockChildData.streak)
    expect(store.childTodayMinutes).toBe(mockChildData.todayMinutes)

    expect(getSelectedStudyDay(store.calendarData, 2026, 3, 19)?.minutes).toBe(42)
    expect(getSelectedStudyDay(store.calendarData, 2026, 3, 30)).toBeNull()
  })

  it('covers math subject records and failed pet feeding', async () => {
    const store = useAppStore()
    store.recordAnswer(mockQuestions[3], true)

    const mathRecord = store.calendarData['2026-3']?.[19]?.subjects.find((item) => item.name === '数学')
    expect(mathRecord?.topic).toBe('10 以内加法')

    store.setChild(null)
    const success = await store.feedPet(mockPetFoodItems[0])
    expect(success).toBe(false)
  })
})
