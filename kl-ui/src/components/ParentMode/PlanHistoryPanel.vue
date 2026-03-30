<script setup lang="ts">
import { ref, computed } from 'vue'
import ChatCard from './ChatCard.vue'
import TwemojiText from '../TwemojiText.vue'
import PlanDetailModal from './PlanDetailModal.vue'
import type { LearningPlan } from '../../types/parent'

const props = defineProps<{
  planHistory: LearningPlan[]
  initialIndex?: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'removeQuestion', planId: string, phaseIndex: number, questionId: string): void
}>()

const currentIndex = ref(props.initialIndex ?? 0)
const showDetailModal = ref(false)
const detailPlanId = ref<string | null>(null)

const currentPlan = computed(() => {
  if (props.planHistory.length === 0) return null
  return props.planHistory[currentIndex.value] ?? null
})

const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < props.planHistory.length - 1)

function goPrev() {
  if (hasPrev.value) {
    currentIndex.value--
  }
}

function goNext() {
  if (hasNext.value) {
    currentIndex.value++
  }
}

function handleCardAction(action: string) {
  if (action === 'view_plan_detail' && currentPlan.value) {
    detailPlanId.value = currentPlan.value.id
    showDetailModal.value = true
  }
}

function handleRemoveQuestion(phaseIndex: number, questionId: string) {
  if (currentPlan.value) {
    emit('removeQuestion', currentPlan.value.id, phaseIndex, questionId)
  }
  showDetailModal.value = false
}

function isCompleted(plan: LearningPlan): boolean {
  return plan.status === 'completed'
}

// 将 LearningPlan 转换为 ChatCard 格式
function toChatCard(plan: LearningPlan) {
  return {
    style: 'plan' as const,
    title: `${plan.duration}天${plan.subject === 'en' ? '英语' : '数学'}学习计划`,
    icon: plan.subject === 'en' ? '📚' : '🔢',
    rows: [
      { label: '周期', value: `${plan.duration}天` },
      { label: '难度', value: getDifficultyLabel(plan.difficulty) },
      { label: '状态', value: getStatusLabel(plan.status) },
    ],
    phases: plan.phases.map(p => ({
      dayRange: p.dayRange,
      topic: p.topic,
      questionsPerDay: p.questionsPerDay,
      difficulty: p.difficulty,
    })),
    actions: getActions(plan.status),
  }
}

function getDifficultyLabel(difficulty: string): string {
  switch (difficulty) {
    case 'easy': return '⭐ 基础'
    case 'medium': return '⭐⭐ 中等'
    case 'hard': return '⭐⭐⭐ 困难'
    default: return difficulty
  }
}

function getStatusLabel(status: string): string {
  switch (status) {
    case 'pending': return '待确认'
    case 'active': return '进行中'
    case 'paused': return '已暂停'
    case 'completed': return '已完成'
    case 'cancelled': return '已取消'
    default: return status
  }
}

function getActions(status: string) {
  const actions = []
  if (status === 'pending') {
    actions.push({ label: '确认计划', action: 'confirm_plan' })
    actions.push({ label: '调整', action: 'adjust_plan' })
    actions.push({ label: '取消', action: 'cancel_plan' })
  } else if (status === 'active') {
    actions.push({ label: '查看详情', action: 'view_plan_detail' })
    actions.push({ label: '调整', action: 'adjust_plan' })
    actions.push({ label: '暂停', action: 'pause_plan' })
  } else if (status === 'paused') {
    actions.push({ label: '查看详情', action: 'view_plan_detail' })
    actions.push({ label: '调整', action: 'adjust_plan' })
    actions.push({ label: '恢复', action: 'resume_plan' })
  } else if (status === 'completed') {
    actions.push({ label: '查看详情', action: 'view_plan_detail' })
  } else if (status === 'cancelled') {
    actions.push({ label: '查看详情', action: 'view_plan_detail' })
  }
  return actions
}
</script>

<template>
  <div class="plan-history-panel">
    <!-- 面板头部 -->
    <div class="panel-header">
      <span class="panel-title">历史计划</span>
      <span class="panel-counter" v-if="planHistory.length > 0">
        {{ currentIndex + 1 }} / {{ planHistory.length }}
      </span>
      <button class="close-btn" @click="emit('close')">
        <TwemojiText html="✕" tag="span" />
      </button>
    </div>

    <!-- 计划卡片区域 -->
    <div class="panel-content">
      <!-- 计划卡片 -->
      <div class="card-container">
        <div v-if="planHistory.length === 0" class="empty-state">
          <TwemojiText html="📋" tag="span" class="empty-icon" />
          <span class="empty-text">暂无历史计划</span>
        </div>

        <div v-else-if="currentPlan" class="card-wrapper">
          <!-- 左箭头 -->
          <button
            class="nav-arrow left"
            :class="{ disabled: !hasPrev }"
            :disabled="!hasPrev"
            @click="goPrev"
          >
            ‹
          </button>

          <!-- 已完成印章 -->
          <div v-if="isCompleted(currentPlan)" class="completed-stamp">
            <span>已完成</span>
          </div>

          <div class="card-inner">
            <ChatCard
              :card="toChatCard(currentPlan)"
              @action="handleCardAction"
            />
          </div>

          <!-- 右箭头 -->
          <button
            class="nav-arrow right"
            :class="{ disabled: !hasNext }"
            :disabled="!hasNext"
            @click="goNext"
          >
            ›
          </button>
        </div>
      </div>
    </div>

    <!-- 切换指示器 -->
    <div v-if="planHistory.length > 1" class="page-dots">
      <span
        v-for="(_, index) in planHistory"
        :key="index"
        class="dot"
        :class="{ active: index === currentIndex }"
        @click="currentIndex = index"
      />
    </div>

    <!-- 详情弹窗 -->
    <PlanDetailModal
      v-if="showDetailModal && detailPlanId"
      :plan-id="detailPlanId"
      @close="showDetailModal = false"
      @remove-question="handleRemoveQuestion"
    />
  </div>
</template>

<style scoped>
.plan-history-panel {
  background: #fff;
  border-bottom: 1px solid #E8E4FF;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  gap: 8px;
  border-bottom: 1px solid #F0F0F0;
  background: #FAFAFA;
}

.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #333;
  flex: 1;
}

.panel-counter {
  font-size: 13px;
  color: #888;
  font-weight: 500;
}

.close-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: #F5F5F5;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  transition: all 0.2s;
}

.close-btn:active {
  background: #FFE0E0;
  transform: scale(0.92);
}

.panel-content {
  display: flex;
  align-items: center;
  padding: 16px;
  gap: 12px;
  min-height: 200px;
}

.card-container {
  flex: 1;
  display: flex;
  align-items: center;
  min-height: 180px;
  padding: 0 8px;
}

.card-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  max-width: 100%;
}

.card-inner {
  flex: 1;
  max-width: 100%;
}

.card-inner :deep(.chat-card) {
  width: 100%;
}

/* 左右箭头 - 覆盖在卡片左右边缘 */
.nav-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #666;
  transition: all 0.2s;
}

.nav-arrow.left {
  left: -16px;
}

.nav-arrow.right {
  right: -16px;
}

.nav-arrow:active:not(.disabled) {
  background: #E8E4FF;
  color: #7c6df8;
  transform: translateY(-50%) scale(0.92);
}

.nav-arrow.disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
}

.empty-icon {
  font-size: 32px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
}

/* 已完成印章 */
.completed-stamp {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-15deg);
  z-index: 10;
  padding: 8px 16px;
  border: 3px solid #c62828;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  opacity: 0.85;
}

.completed-stamp span {
  font-size: 18px;
  font-weight: 800;
  color: #c62828;
  letter-spacing: 2px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 切换指示器 */
.page-dots {
  display: flex;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px 16px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #DDD;
  cursor: pointer;
  transition: all 0.2s;
}

.dot.active {
  background: #7c6df8;
  transform: scale(1.2);
}

.dot:hover:not(.active) {
  background: #BBB;
}
</style>
