<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import ParentChatTimeline from '../components/ParentMode/ParentChatTimeline.vue'
import ParentInputBar from '../components/ParentMode/ParentInputBar.vue'
import ParentSettingsModal from '../components/ParentMode/ParentSettingsModal.vue'
import PlanHistoryPanel from '../components/ParentMode/PlanHistoryPanel.vue'
import { useParentState } from '../composables/useParentState'
import { removeQuestionFromPlan } from '../services/api'

const emit = defineEmits<{
  (e: 'close'): void
}>()

const {
  messages,
  isLoading,
  isGeneratingQuestions,
  pendingPlan,
  activePlan,
  planHistory,
  initParentState,
  sendMessage,
  handleGenerateQuestions,
  handleConfirmPlan,
  handleCancelPlan,
  resetParentState,
} = useParentState()

const showSettings = ref(false)
const showPlanHistory = ref(false)

// 过滤掉当前活跃计划，显示历史计划
const historyPlans = computed(() => {
  return planHistory.value.filter(p => p.id !== activePlan.value?.id)
})

onMounted(async () => {
  await initParentState()
})

onUnmounted(() => {
  resetParentState()
})

function handleSend(message: string) {
  sendMessage(message)
}

function handleSuggestion(text: string) {
  sendMessage(text)
}

function handleConfirmPlanClick() {
  if (pendingPlan.value) {
    handleConfirmPlan(pendingPlan.value)
  }
}

function handleCancelPlanClick() {
  handleCancelPlan()
}

function handleExit() {
  emit('close')
}

// 处理历史计划中删除题目
async function handleRemoveQuestion(planId: string, phaseIndex: number, questionId: string) {
  try {
    await removeQuestionFromPlan(planId, questionId)
    console.log('题目已删除', { planId, phaseIndex, questionId })
    // 删除成功后，可以刷新计划详情
  } catch (error) {
    console.error('删除题目失败', error)
  }
}

// 卡片按钮动作处理
function handleCardAction(action: string, message: any) {
  switch (action) {
    case 'view_plan_detail':
      // TODO: 显示计划详情弹窗
      console.log('查看计划详情', message.plan)
      break
    case 'confirm_generate_questions':
      // 用户确认计划，开始生成题目
      if (message.plan) {
        handleGenerateQuestions(message.plan)
      }
      break
    case 'confirm_plan':
      if (message.plan) {
        handleConfirmPlan(message.plan)
      }
      break
    case 'adjust_plan':
      // TODO: 调整计划
      console.log('调整计划')
      break
    case 'cancel_plan':
      handleCancelPlan()
      break
    default:
      console.log('未知动作', action)
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="parent-mode">
      <!-- 顶部栏 -->
      <div class="header">
        <button class="exit-btn" @click="handleExit">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M12 5L7 10L12 15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="title">
          <span class="icon">🔒</span>
          家长模式
        </div>
        <button class="settings-btn" @click="showSettings = true">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M10 13a3 3 0 100-6 3 3 0 000 6z" stroke="currentColor" stroke-width="1.5"/>
            <path d="M17 10a7 7 0 01-.7 2.8l.8.8 1.2-1.7-.7-.6A7 7 0 0117 10zM3 10a7 7 0 00.7 2.8l-.8.8-1.2-1.7.7-.6A7 7 0 003 10z" stroke="currentColor" stroke-width="1.5"/>
          </svg>
        </button>
      </div>

      <!-- 内容区域：白色背景 -->
      <div class="content">
        <!-- 活跃计划提示 -->
        <div v-if="activePlan" class="active-plan-banner">
          <span class="banner-icon">📋</span>
          <span class="banner-text">
            当前计划：{{ activePlan.duration }}天{{ activePlan.subject === 'en' ? '英语' : '数学' }}学习 · 第{{ activePlan.phases.length }}阶段
          </span>
          <button
            class="expand-btn"
            @click="showPlanHistory = !showPlanHistory"
            :title="showPlanHistory ? '收起历史计划' : '查看历史计划'"
          >
            {{ showPlanHistory ? '∧' : '»' }}
          </button>
        </div>

        <!-- 历史计划展开面板 -->
        <Transition name="slide-down">
          <PlanHistoryPanel
            v-if="showPlanHistory && historyPlans.length > 0"
            :plan-history="historyPlans"
            @close="showPlanHistory = false"
            @remove-question="handleRemoveQuestion"
          />
        </Transition>

        <!-- 聊天时间轴 -->
        <ParentChatTimeline
          :messages="messages"
          @suggestion="handleSuggestion"
          @action="handleCardAction"
        />

        <!-- 题目生成中遮罩 -->
        <Transition name="fade">
          <div v-if="isGeneratingQuestions" class="generating-overlay">
            <div class="generating-content">
              <div class="generating-icon">📝</div>
              <div class="generating-title">正在生成题目</div>
              <div class="generating-desc">正在根据计划生成个性化题目...</div>
              <div class="generating-progress">
                <div class="progress-bar">
                  <div class="progress-fill"></div>
                </div>
              </div>
            </div>
          </div>
        </Transition>

        <!-- 输入框 -->
        <ParentInputBar
          :disabled="isLoading || isGeneratingQuestions"
          @send="handleSend"
        />
      </div>

      <!-- 设置面板 -->
      <ParentSettingsModal
        v-if="showSettings"
        @close="showSettings = false"
      />
    </div>
  </Teleport>
</template>

<style scoped>
.parent-mode {
  position: fixed;
  inset: 0;
  z-index: var(--z-parent);
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #7c6df8, #9d8df8);
  padding-top: env(safe-area-inset-top);
  overflow: hidden;
}

.header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  gap: 12px;
  flex-shrink: 0;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  background: #fff;
}

.exit-btn,
.settings-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.2s;
  flex-shrink: 0;
}

.exit-btn:active,
.settings-btn:active {
  background: rgba(255,255,255,0.3);
  transform: scale(0.92);
}

.title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 17px;
  font-weight: 800;
  color: white;
}

.icon {
  font-size: 18px;
}

/* 活跃计划提示 */
.active-plan-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #f0f5ff;
  border-bottom: 1px solid #e8e4ff;
  flex-shrink: 0;
}

.banner-icon {
  font-size: 16px;
}

.banner-text {
  flex: 1;
  font-size: 13px;
  color: #555;
}

/* 展开按钮 */
.expand-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: #E8E4FF;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #7c6df8;
  font-weight: 700;
  transition: all 0.2s;
  flex-shrink: 0;
}

.expand-btn:active {
  background: #D0C8FF;
  transform: scale(0.92);
}

/* 瀑布展开动画 */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease-out;
  overflow: hidden;
}

.slide-down-enter-from,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
}

.slide-down-enter-to,
.slide-down-leave-from {
  max-height: 500px;
  opacity: 1;
}

/* 题目生成中遮罩 */
.generating-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.generating-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
}

.generating-icon {
  font-size: 48px;
  animation: pulse 1.5s ease-in-out infinite;
}

.generating-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.generating-desc {
  font-size: 14px;
  color: #666;
}

.generating-progress {
  width: 200px;
  margin-top: 8px;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background: #E8E4FF;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #7c6df8, #9d8df8);
  border-radius: 3px;
  animation: progress 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(0.95); }
}

@keyframes progress {
  0% { width: 0%; }
  50% { width: 70%; }
  100% { width: 100%; }
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

</style>
