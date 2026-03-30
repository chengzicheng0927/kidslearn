import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getSelectedStudyDay, useAppState } from './useAppState'
import { mockChildData, mockPetFoodItems, mockQuestions } from '../mock/data'

describe('useAppState', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-19T09:00:00Z'))
    const state = useAppState()
    state.resetMockState()
    state.setChild(mockChildData)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('records a correct answer into shared child and calendar state', () => {
    const state = useAppState()
    const question = mockQuestions[0]

    state.recordAnswer(question, true)

    expect(state.child.value?.stars).toBe(mockChildData.stars + question.reward.stars)
    expect(state.child.value?.todayMinutes).toBe(mockChildData.todayMinutes + question.estimatedMinutes)
    expect(state.quizSession.value.combo).toBe(1)
    expect(state.calendarData.value['2026-3']?.[19]?.subjects[0]?.topic).toBe('动物词汇')
  })

  it('resets combo on wrong answer', () => {
    const state = useAppState()
    state.recordAnswer(mockQuestions[0], true)
    state.recordAnswer(mockQuestions[1], false)

    expect(state.quizSession.value.correctCount).toBe(1)
    expect(state.quizSession.value.combo).toBe(0)
  })

  it('feeds pet by consuming shared stars', () => {
    const state = useAppState()
    const before = state.child.value?.stars ?? 0
    const success = state.feedPet(mockPetFoodItems[0])

    expect(success).toBe(true)
    expect(state.child.value?.stars).toBe(before - mockPetFoodItems[0].cost)
    expect(state.petState.value.stats[0].value).toBe(80)
  })

  it('handles evolution bounds and selected day lookup', () => {
    const state = useAppState()

    // 初始为 2；setPetEvolution(1) < 2，无效，保持 2
    state.setPetEvolution(1)
    expect(state.petState.value.evolutionIndex).toBe(2)

    // setPetEvolution(4) >= 2，有效，设为 4
    state.setPetEvolution(4)
    expect(state.petState.value.evolutionIndex).toBe(4)

    expect(state.childStars.value).toBe(mockChildData.stars)
    expect(state.childStreak.value).toBe(mockChildData.streak)
    expect(state.childTodayMinutes.value).toBe(mockChildData.todayMinutes)

    expect(getSelectedStudyDay(state.calendarData.value, 2026, 3, 19)?.minutes).toBe(42)
    expect(getSelectedStudyDay(state.calendarData.value, 2026, 3, 30)).toBeNull()
  })

  it('covers math subject records and failed pet feeding', () => {
    const state = useAppState()
    state.recordAnswer(mockQuestions[3], true)

    const mathRecord = state.calendarData.value['2026-3']?.[19]?.subjects.find((item) => item.name === '数学')
    expect(mathRecord?.topic).toBe('10 以内加法')

    state.setChild(null)
    expect(state.feedPet(mockPetFoodItems[0])).toBe(false)
  })
})
