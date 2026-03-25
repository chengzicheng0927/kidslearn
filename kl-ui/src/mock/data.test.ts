import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  calcAgeFromId,
  cloneChild,
  createInitialPetState,
  createMockChild,
  getAgeGroup,
  verifyInviteCode,
} from './data'

describe('mock data helpers', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-19T09:00:00Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('calculates age from id card correctly', () => {
    expect(calcAgeFromId('110101201903180018')).toBe(7)
    expect(calcAgeFromId('110101202403180018')).toBeNull()
  })

  it('maps age group and creates a child record', () => {
    expect(getAgeGroup(8)).toEqual({ label: '7-8 岁', emoji: '🐰' })
    expect(getAgeGroup(4)).toEqual({ label: '3-4 岁', emoji: '🐣' })
    expect(getAgeGroup(6)).toEqual({ label: '5-6 岁', emoji: '🐥' })
    expect(getAgeGroup(10)).toEqual({ label: '9-10 岁', emoji: '🦊' })
    expect(getAgeGroup(12)).toEqual({ label: '11-12 岁', emoji: '🦁' })

    const child = createMockChild('乐乐', 8, '7-8 岁')
    expect(child.name).toBe('乐乐')
    expect(child.stars).toBe(0)
    expect(child.todayMinutes).toBe(0)

    const cloned = cloneChild(child)
    expect(cloned).toEqual(child)
    expect(cloned).not.toBe(child)
  })

  it('creates initial pet state', () => {
    const petState = createInitialPetState()
    expect(petState.evolutionIndex).toBe(2)
    expect(petState.stats).toHaveLength(3)
  })

  it('validates invite codes explicitly', () => {
    expect(verifyInviteCode('KL-ABCD-1234')).toBe(true)
    expect(verifyInviteCode('kl-abcd-1234')).toBe(true)
    expect(verifyInviteCode('KL-FAIL-0000')).toBe(false)
  })
})
