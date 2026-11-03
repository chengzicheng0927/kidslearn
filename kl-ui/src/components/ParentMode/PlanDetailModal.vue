<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import TwemojiText from '../TwemojiText.vue'
import { getPlanDetail } from '../../services/api'
import type { PlanDetailResponse } from '../../services/api.interface'

const props = defineProps<{
  planId: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'removeQuestion', phaseIndex: number, questionId: string): void
}>()

const loading = ref(true)
const error = ref<string | null>(null)
const planDetail = ref<PlanDetailResponse | null>(null)
const expandedPhases = ref<Set<number>>(new Set())

onMounted(async () => {
  try {
    planDetail.value = await getPlanDetail(props.planId)
    // 默认展开所有阶段
    planDetail.value?.phases.forEach((_, i) => expandedPhases.value.add(i))
  } catch (e) {
    error.value = '加载计划详情失败'
    console.error(e)
  } finally {
    loading.value = false
  }
})

const isCompleted = computed(() => planDetail.value?.status === 'completed')

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

function togglePhase(index: number) {
  if (expandedPhases.value.has(index)) {
    expandedPhases.value.delete(index)
  } else {
    expandedPhases.value.add(index)
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

function removeQuestion(phaseIndex: number, questionId: string) {
  emit('removeQuestion', phaseIndex, questionId)
}
</script>

<template>
  <Teleport to="body">
    <div class="overlay" @click.self="emit('close')">
      <div class="modal">
        <!-- 头部 -->
        <div class="modal-header">
          <div class="modal-title">
            <TwemojiText html="📋" />
            <span v-if="planDetail">{{ formatDate(planDetail.createdAt) }} {{ planDetail.durationDays }}天{{ planDetail.subject === 'en' ? '英语' : '数学' }}学习计划</span>
            <span v-else>计划详情</span>
          </div>
          <button class="close-btn" @click="emit('close')">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M5 5L15 15M15 5L5 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="loading">
          <div class="loading-spinner"></div>
          <span>加载中...</span>
        </div>

        <!-- 错误 -->
        <div v-else-if="error" class="error">
          {{ error }}
        </div>

        <!-- 详情内容 -->
        <div v-else-if="planDetail" class="modal-content">
          <!-- 基本信息 -->
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">周期</span>
              <span class="info-value">{{ planDetail.durationDays }}天</span>
            </div>
            <div class="info-item">
              <span class="info-label">难度</span>
              <span class="info-value">{{ getDifficultyLabel(planDetail.difficulty) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">状态</span>
              <span class="info-value" :class="`status-${planDetail.status}`">{{ getStatusLabel(planDetail.status) }}</span>
            </div>
          </div>

          <!-- 阶段列表 -->
          <div class="phases-section">
            <div class="phases-header">
              <TwemojiText html="📅" />
              <span>共{{ planDetail.phases.length }}个阶段</span>
            </div>

            <div class="phase-list">
              <div v-for="(phase, index) in planDetail.phases" :key="index" class="phase-item">
                <!-- 阶段概览 -->
                <div class="phase-overview" @click="togglePhase(index)">
                  <div class="phase-main">
                    <span class="phase-index">阶段{{ index + 1 }}</span>
                    <span class="phase-info">
                      <template v-if="phase.dayRange">{{ phase.dayRange }}天</template>
                      <template v-if="phase.topic"> · {{ phase.topic }}</template>
                      <template v-if="phase.questionsPerDay"> · {{ phase.questionsPerDay }}题</template>
                    </span>
                  </div>
                  <div class="phase-right">
                    <span class="phase-difficulty">{{ getDifficultyLabel(phase.difficulty) }}</span>
                    <span class="expand-icon">{{ expandedPhases.has(index) ? '∧' : '∨' }}</span>
                  </div>
                </div>

                <!-- 阶段详情 -->
                <div v-if="expandedPhases.has(index)" class="phase-detail">
                  <!-- 目标 -->
                  <div class="phase-goal">
                    <TwemojiText html="🎯" />
                    <span>{{ phase.goal }}</span>
                  </div>

                  <!-- 题目列表 -->
                  <div v-if="phase.questions.length" class="question-list">
                    <div v-for="q in phase.questions" :key="q.id" class="question-item">
                      <div class="question-main">
                        <span class="question-icon">{{ q.icon }}</span>
                        <span class="question-text">{{ q.question }}</span>
                      </div>
                      <div class="question-options">
                        <span
                          v-for="(opt, optIdx) in q.options"
                          :key="optIdx"
                          class="option-tag"
                          :class="{ correct: q.answer && opt === q.answer }"
                        >
                          {{ opt }}
                        </span>
                      </div>
                      <!-- 删除按钮：只对非已完成计划显示 -->
                      <div v-if="!isCompleted" class="question-actions">
                        <button class="q-action-btn" @click="removeQuestion(index, q.id)">
                          <TwemojiText html="🗑️" tag="span" />
                        </button>
                      </div>
                    </div>
                  </div>
                  <div v-else class="no-questions">暂无题目</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 统计信息：已完成计划显示 -->
          <div v-if="isCompleted" class="stats-section">
            <div class="stats-title">
              <TwemojiText html="📊" />
              <span>学习统计</span>
            </div>
            <div class="stats-grid">
              <div class="stat-item">
                <span class="stat-value">{{ planDetail.totalCompleted }}</span>
                <span class="stat-label">完成题数</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ planDetail.totalCorrect }}</span>
                <span class="stat-label">正确题数</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ planDetail.accuracy }}%</span>
                <span class="stat-label">正确率</span>
              </div>
            </div>
          </div>

          <!-- 按钮 -->
          <div class="modal-footer">
            <button class="confirm-btn" @click="emit('close')">确定</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal, 1000);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 25, 39, 0.5);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease;
}

.modal {
  width: 90%;
  max-width: 400px;
  max-height: 85vh;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: scaleIn 0.25s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { transform: scale(0.95); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.modal-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #F0F0F0;
  gap: 8px;
  flex-shrink: 0;
}

.modal-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 17px;
  font-weight: 800;
  color: #333;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: #F5F5F5;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
  color: #888;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #F0F0F0;
  border-top-color: #7c6df8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error {
  padding: 40px;
  text-align: center;
  color: #c62828;
}

.modal-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

/* 基本信息 */
.info-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.info-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 12px;
  color: #888;
}

.info-value {
  font-size: 14px;
  font-weight: 700;
  color: #333;
}

.status-completed { color: #43a047; }
.status-active { color: #7c6df8; }
.status-paused { color: #ff9800; }
.status-pending { color: #888; }
.status-cancelled { color: #c62828; }

/* 阶段列表 */
.phases-section {
  margin-bottom: 16px;
}

.phases-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #666;
  margin-bottom: 12px;
}

.phase-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.phase-item {
  border: 1px solid #E8E4FF;
  border-radius: 10px;
  overflow: hidden;
}

.phase-overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: #F8F7FF;
  cursor: pointer;
}

.phase-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.phase-index {
  font-size: 13px;
  font-weight: 700;
  color: #7c6df8;
}

.phase-info {
  font-size: 12px;
  color: #666;
}

.phase-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.phase-difficulty {
  font-size: 11px;
  color: #888;
}

.expand-icon {
  font-size: 14px;
  color: #7c6df8;
  font-weight: 700;
}

/* 阶段详情 */
.phase-detail {
  padding: 12px 14px;
  background: #FAFAFA;
  border-top: 1px solid #E8E4FF;
}

.phase-goal {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #555;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #E0E0E0;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.question-item {
  background: #fff;
  border: 1px solid #F0F0F0;
  border-radius: 8px;
  padding: 10px;
}

.question-main {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.question-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.question-text {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.question-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-left: 24px;
}

.option-tag {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  background: #F5F5F5;
  color: #666;
  border: 1px solid #E0E0E0;
}

.option-tag.correct {
  background: #E8F5E9;
  color: #2E7D32;
  border-color: #A5D6A7;
  font-weight: 600;
}

.question-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.q-action-btn {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.q-action-btn:active {
  background: #FFE0E0;
}

.no-questions {
  text-align: center;
  padding: 12px;
  color: #999;
  font-size: 12px;
}

/* 统计信息 */
.stats-section {
  background: #F8F7FF;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 16px;
}

.stats-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 700;
  color: #7c6df8;
  margin-bottom: 12px;
}

.stats-grid {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-value {
  font-size: 20px;
  font-weight: 800;
  color: #333;
}

.stat-label {
  font-size: 11px;
  color: #888;
}

/* 底部按钮 */
.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #F0F0F0;
  flex-shrink: 0;
}

.confirm-btn {
  width: 100%;
  padding: 12px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #7c6df8, #9d8df8);
  color: white;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.confirm-btn:active {
  transform: scale(0.98);
}
</style>
