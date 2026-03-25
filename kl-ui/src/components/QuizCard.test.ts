import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import QuizCard from './QuizCard.vue'
import ResultModal from './ResultModal.vue'
import type { Question } from '../mock/data'

const { submitAnswerMock } = vi.hoisted(() => ({
  submitAnswerMock: vi.fn(),
}))

vi.mock('../services/api', () => ({
  submitAnswer: submitAnswerMock,
}))

const quiz: Question = {
  id: 'q-test',
  subject: 'en',
  label: '英语 · 测试题',
  question: 'Pick the correct answer',
  options: [
    { id: 'a', text: 'Alpha' },
    { id: 'b', text: 'Bravo' },
  ],
  correctAnswerId: 'b',
  explanation: 'Bravo is correct.',
  explanationSteps: ['先找关键词。', '再对比选项。', '最后确认 Bravo 符合题意。'],
  encouragement: '你已经很接近啦，再试一次就会更稳。',
  estimatedMinutes: 3,
  reward: {
    stars: 8,
    xp: 12,
    xpNeeded: 100,
    level: 1,
  },
}

describe('QuizCard', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
    submitAnswerMock.mockReset()
    submitAnswerMock.mockImplementation(
      () =>
        new Promise((resolve) => {
          setTimeout(() => {
            resolve({
              isCorrect: true,
              resultMode: 'correct',
              correctAnswerId: 'b',
              explanation: 'Bravo is correct.',
              explanationSteps: ['先找关键词。', '再对比选项。', '最后确认 Bravo 符合题意。'],
              encouragement: '你已经很接近啦，再试一次就会更稳。',
              showResult: false,
              hasMore: true,
              result: {
                combo: 1,
                accuracy: 100,
                timeSpent: 3,
                stars: 8,
                xp: 12,
                xpNeeded: 100,
                level: 1,
              },
            })
          }, 80)
        })
    )
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('shows a neutral judging status before final result colors', async () => {
    submitAnswerMock.mockImplementationOnce(async () => ({
      isCorrect: false,
      resultMode: 'wrong',
      correctAnswerId: 'b',
      explanation: 'Bravo is correct.',
      encouragement: '你已经很接近啦，再试一次就会更稳。',
      showResult: true,
      hasMore: true,
    }))

    const wrapper = mount(QuizCard, {
      props: {
        quiz,
      },
    })

    const options = wrapper.findAll('.qo')
    await options[0].trigger('click')

    expect(document.body.textContent ?? '').toContain('小星老师正在思考中')
    expect(options[0].classes()).not.toContain('selected')
    expect(options[0].classes()).not.toContain('no')
    expect(options[0].classes()).not.toContain('ok')

    await vi.advanceTimersByTimeAsync(80)
    await wrapper.vm.$nextTick()

    expect(document.body.textContent ?? '').toContain('小星老师正在思考中')

    await vi.advanceTimersByTimeAsync(1420)
    await wrapper.vm.$nextTick()

    expect(document.body.textContent ?? '').not.toContain('小星老师正在思考中')
    expect(options[0].classes()).toContain('no')
    expect(options[1].classes()).toContain('ok')
    expect(options[0].classes()).not.toContain('selected')
  })

  it('shows the result modal and emits completion for the last question', async () => {
    submitAnswerMock.mockImplementationOnce(async () => ({
      isCorrect: true,
      resultMode: 'reward',
      correctAnswerId: 'b',
      explanation: 'Bravo is correct.',
      explanationSteps: ['先找关键词。', '再对比选项。', '最后确认 Bravo 符合题意。'],
      encouragement: '你已经很接近啦，再试一次就会更稳。',
      showResult: true,
      hasMore: false,
      result: {
        combo: 2,
        accuracy: 100,
        timeSpent: 4,
        stars: 18,
        xp: 24,
        xpNeeded: 100,
        level: 2,
      },
    }))

    const wrapper = mount(QuizCard, {
      props: {
        quiz,
      },
    })

    await wrapper.findAll('.qo')[1].trigger('click')
    await vi.advanceTimersByTimeAsync(1500)
    await wrapper.vm.$nextTick()

    expect(wrapper.findComponent(ResultModal).exists()).toBe(true)

    await wrapper.findComponent(ResultModal).vm.$emit('next')
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('next')).toBeFalsy()
    expect(wrapper.emitted('complete')).toBeTruthy()
    expect(wrapper.emitted('complete')?.[0]?.[0]).toMatchObject({
      question: expect.objectContaining({ id: 'q-test' }),
      selectedAnswerId: 'b',
      isCorrect: true,
      correctAnswerId: 'b',
    })
  })

  it('renders history answers with final colors only', () => {
    const wrapper = mount(QuizCard, {
      props: {
        quiz,
        answeredAnswerId: 'a',
        correctAnswerId: 'b',
      },
    })

    const options = wrapper.findAll('.qo')
    expect(options[0].classes()).toContain('no')
    expect(options[1].classes()).toContain('ok')
    expect(options[0].classes()).not.toContain('selected')
  })

  it('shows explanation modal when the answer is wrong', async () => {
    submitAnswerMock.mockImplementationOnce(async () => ({
      isCorrect: false,
      resultMode: 'wrong',
      correctAnswerId: 'b',
      explanation: '因为 strawberry 顶部有绿色叶片，看起来像一个小皇冠。',
      encouragement: '你已经抓到一半线索啦，下次会更快找到答案。',
      showResult: true,
      hasMore: true,
    }))

    const wrapper = mount(QuizCard, {
      props: {
        quiz,
      },
    })

    await wrapper.findAll('.qo')[0].trigger('click')
    await vi.advanceTimersByTimeAsync(1500)
    await wrapper.vm.$nextTick()

    expect(wrapper.findComponent(ResultModal).exists()).toBe(true)
    // wrong 模式渲染 explanation 字段（而非 explanationSteps）
    expect(document.body.textContent ?? '').toContain('因为 strawberry 顶部有绿色叶片')
    expect(document.body.textContent ?? '').toContain('你已经抓到一半线索啦')
  })

  it('shows reward page instead of normal correct page when reward logic is hit', async () => {
    submitAnswerMock.mockImplementationOnce(async () => ({
      isCorrect: true,
      resultMode: 'reward',
      correctAnswerId: 'b',
      explanation: 'Bravo is correct.',
      explanationSteps: ['先找关键词。'],
      encouragement: '继续保持。',
      showResult: true,
      hasMore: true,
      result: {
        combo: 3,
        accuracy: 100,
        timeSpent: 3,
        stars: 24,
        xp: 36,
        xpNeeded: 100,
        level: 2,
        reward: {
          icon: '🏅',
          title: '连对奖励已解锁！',
          sub: '连续答对 3 题获得',
        },
      },
    }))

    const wrapper = mount(QuizCard, {
      props: {
        quiz,
      },
    })

    await wrapper.findAll('.qo')[1].trigger('click')
    await vi.advanceTimersByTimeAsync(1500)
    await wrapper.vm.$nextTick()

    expect(document.body.textContent ?? '').toContain('连续答对触发奖励')
    expect(document.body.textContent ?? '').toContain('连对奖励已解锁！')
    expect(document.body.textContent ?? '').not.toContain('太棒了！答对了！')
  })
})
