<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import ParentChatTimeline from '../components/ParentMode/ParentChatTimeline.vue'
import ParentInputBar from '../components/ParentMode/ParentInputBar.vue'
import ParentSettingsModal from '../components/ParentMode/ParentSettingsModal.vue'
import { useParentState } from '../composables/useParentState'

const emit = defineEmits<{
  (e: 'close'): void
}>()

const {
  messages,
  isLoading,
  pendingPlan,
  activePlan,
  initParentState,
  sendMessage,
  handleConfirmPlan,
  handleCancelPlan,
  resetParentState,
} = useParentState()

const showSettings = ref(false)

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

// 卡片按钮动作处理
function handleCardAction(action: string, message: any) {
  switch (action) {
    case 'view_plan_detail':
      // TODO: 显示计划详情弹窗
      console.log('查看计划详情', message.plan)
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
        </div>

        <!-- 聊天时间轴 -->
        <ParentChatTimeline
          :messages="messages"
          @suggestion="handleSuggestion"
          @action="handleCardAction"
        />

        <!-- 输入框 -->
        <ParentInputBar
          :disabled="isLoading"
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

</style>
