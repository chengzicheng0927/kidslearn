<script setup lang="ts">
import { ref } from 'vue'
import TwemojiText from '../TwemojiText.vue'
import type { ChatCard, PhaseDetail, QuestionDetail } from '../../types/parent'

const props = defineProps<{
  card: ChatCard
}>()

const emit = defineEmits<{
  (e: 'action', action: string): void
}>()

const isExpanded = ref(false)

function handleAction(action: string) {
  if (action === 'view_plan_detail') {
    isExpanded.value = !isExpanded.value
  }
  emit('action', action)
}

function getDifficultyLabel(difficulty?: string) {
  switch (difficulty) {
    case 'easy': return '⭐ 基础'
    case 'medium': return '⭐⭐ 中等'
    case 'hard': return '⭐⭐⭐ 困难'
    default: return ''
  }
}

function removeQuestion(phaseIndex: number, questionId: string) {
  if (props.card.phases?.[phaseIndex]) {
    props.card.phases[phaseIndex].questions = props.card.phases[phaseIndex].questions?.filter(q => q.id !== questionId)
  }
}
</script>

<template>
  <div class="chat-card" :class="card.style">
    <!-- 卡片头部 -->
    <div class="card-header">
      <span v-if="card.icon" class="card-icon">
        <TwemojiText :html="card.icon" tag="span" />
      </span>
      <div class="card-title-wrap">
        <span class="card-title">{{ card.title }}</span>
        <span v-if="card.name" class="card-name">{{ card.name }}</span>
      </div>
      <button v-if="card.style === 'plan'" class="expand-btn" @click="handleAction('view_plan_detail')">
        {{ isExpanded ? '∧' : '∨' }}
      </button>
    </div>

    <!-- 键值对信息 -->
    <div v-if="card.rows?.length" class="card-rows">
      <div v-for="(row, index) in card.rows" :key="index" class="info-row">
        <span class="row-label">{{ row.label }}</span>
        <span class="row-value">{{ row.value }}</span>
      </div>
    </div>

    <!-- 阶段列表（plan 类型）- 收起状态 -->
    <div v-if="card.phases?.length && card.style === 'plan' && !isExpanded" class="card-phases">
      <div class="phases-header">
        <TwemojiText html="📅" tag="span" />
        <span>共{{ card.phases.length }}个阶段</span>
      </div>
      <div class="phase-list">
        <div v-for="(phase, index) in card.phases" :key="index" class="phase-item">
          <div class="phase-main">
            <span class="phase-index">阶段{{ index + 1 }}</span>
            <span class="phase-info">
              <template v-if="phase.dayRange">{{ phase.dayRange }}天</template>
              <template v-if="phase.topic"> · {{ phase.topic }}</template>
              <template v-if="phase.questionsPerDay"> · {{ phase.questionsPerDay }}题</template>
            </span>
          </div>
          <div class="phase-difficulty">
            {{ getDifficultyLabel(phase.difficulty) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 阶段详情（展开状态） -->
    <div v-if="card.phases?.length && card.style === 'plan' && isExpanded" class="card-detail">
      <div v-for="(phase, phaseIndex) in card.phases" :key="phaseIndex" class="detail-phase">
        <div class="detail-phase-header">
          <span class="phase-index">阶段{{ phaseIndex + 1 }}</span>
          <span class="phase-info">
            <template v-if="phase.dayRange">{{ phase.dayRange }}天</template>
            <template v-if="phase.topic"> · {{ phase.topic }}</template>
          </span>
          <span class="phase-difficulty">{{ getDifficultyLabel(phase.difficulty) }}</span>
        </div>
        <!-- 题目列表 -->
        <div v-if="phase.questions?.length" class="question-list">
          <div v-for="(q, qIndex) in phase.questions" :key="q.id" class="question-item">
            <div class="question-main">
              <span class="question-icon">{{ q.icon }}</span>
              <span class="question-text">{{ q.question }}</span>
            </div>
            <div class="question-options">
              <span
                v-for="(opt, optIdx) in q.options"
                :key="optIdx"
                class="option-tag"
                :class="{ correct: opt === q.answer }"
              >
                {{ opt }}
              </span>
            </div>
            <div class="question-actions">
              <button class="q-action-btn" @click="removeQuestion(phaseIndex, q.id)">
                <TwemojiText html="🗑️" tag="span" />
              </button>
            </div>
          </div>
        </div>
        <div v-else class="no-questions">暂无题目</div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div v-if="card.actions.length" class="card-actions">
      <button
        v-for="(action, index) in card.actions"
        :key="index"
        class="action-btn"
        :class="{
          primary: index === 0 && (action.action === 'confirm_plan' || action.action === 'confirm_generate_questions')
        }"
        @click="handleAction(action.action)"
      >
        {{ action.label }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.chat-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  width: 85%;
}

/* plan 风格 - 紫色主题 */
.chat-card.plan {
  border: 1.5px solid #E8E4FF;
}

.chat-card.plan .card-header {
  color: #7c6df8;
}

/* stats 风格 - 蓝色主题 */
.chat-card.stats {
  border: 1.5px solid #E0F0FF;
}

.chat-card.stats .card-header {
  color: #40C4FF;
}

/* schedule 风格 - 绿色主题 */
.chat-card.schedule {
  border: 1.5px solid #E0F5E0;
}

.chat-card.schedule .card-header {
  color: #43a047;
}

/* achievement 风格 - 金色主题 */
.chat-card.achievement {
  border: 1.5px solid #FFE0B2;
}

.chat-card.achievement .card-header {
  color: #FF9800;
}

/* task 风格 - 青色主题 */
.chat-card.task {
  border: 1.5px solid #E0F7FA;
}

.chat-card.task .card-header {
  color: #00BCD4;
}

/* custom 风格 - 灰色主题 */
.chat-card.custom {
  border: 1.5px solid #F0F0F0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 800;
  margin-bottom: 12px;
  color: #333;
}

.card-icon {
  font-size: 18px;
}

.card-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.card-title {
  font-size: 15px;
  font-weight: 800;
}

.card-name {
  font-size: 12px;
  color: #888;
  font-weight: 400;
}

/* 键值对 */
.card-rows {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.row-label {
  color: #888;
}

.row-value {
  color: #333;
  font-weight: 600;
}

/* 阶段列表 */
.card-phases {
  margin-bottom: 12px;
}

.phases-header {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.phase-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.phase-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  background: #F8F7FF;
  border-radius: 8px;
}

.phase-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.phase-index {
  font-size: 12px;
  font-weight: 700;
  color: #7c6df8;
}

.phase-info {
  font-size: 12px;
  color: #666;
}

.phase-difficulty {
  font-size: 11px;
  color: #888;
}

/* 通用列表 */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 6px 0;
  border-bottom: 1px solid #F5F5F5;
}

.list-item:last-child {
  border-bottom: none;
}

.item-label {
  color: #666;
}

.item-value {
  color: #333;
  font-weight: 600;
}

/* 操作按钮 */
.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #F0F0F0;
}

.action-btn {
  flex: 1;
  min-width: 100px;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1.5px solid #DDD;
  background: transparent;
  color: #666;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn.primary {
  background: linear-gradient(135deg, #7c6df8, #9d8df8);
  border-color: transparent;
  color: white;
}

.action-btn:active {
  transform: scale(0.96);
}

/* 展开按钮 */
.expand-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background: #F5F5F5;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #666;
  font-weight: 600;
  transition: all 0.2s;
  flex-shrink: 0;
}

.expand-btn:active {
  background: #E8E4FF;
  color: #7c6df8;
  transform: scale(0.92);
}

/* 阶段详情（展开状态） */
.card-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background: #FAFAFA;
  border-radius: 10px;
  margin-bottom: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.detail-phase {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-phase-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #E8E4FF;
}

.detail-phase-header .phase-index {
  font-size: 13px;
  font-weight: 700;
  color: #7c6df8;
}

.detail-phase-header .phase-info {
  flex: 1;
  font-size: 12px;
  color: #666;
}

.detail-phase-header .phase-difficulty {
  font-size: 11px;
  color: #888;
}

/* 题目列表 */
.question-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.question-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #F0F0F0;
}

.question-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.question-text {
  flex: 1;
  font-size: 13px;
  color: #333;
  font-weight: 600;
  word-break: break-word;
  line-height: 1.4;
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
  gap: 4px;
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
  transition: all 0.2s;
}

.q-action-btn:active {
  background: #FFE0E0;
  transform: scale(0.92);
}

.no-questions {
  text-align: center;
  padding: 12px;
  color: #999;
  font-size: 12px;
}
</style>
