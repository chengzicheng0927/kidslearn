<script setup lang="ts">
/**
 * 骨架屏组件 - 消除 loading 白屏感
 * 用于 App 初始化、页面切换等场景
 */

defineProps<{
  type?: 'app' | 'chat' | 'quiz' | 'onboarding'
}>()
</script>

<template>
  <div class="skeleton-wrap">
    <!-- App 初始化骨架屏 -->
    <template v-if="type === 'app' || !type">
      <div class="sk-hero">
        <div class="sk-avatar"></div>
        <div class="sk-lines">
          <div class="sk-line sk-line--lg"></div>
          <div class="sk-line sk-line--sm"></div>
        </div>
      </div>
      <div class="sk-tabs">
        <div class="sk-tab sk-tab--active"></div>
        <div class="sk-tab"></div>
        <div class="sk-tab"></div>
      </div>
      <div class="sk-content">
        <div class="sk-msg">
          <div class="sk-msg-av"></div>
          <div class="sk-msg-bubble"></div>
        </div>
        <div class="sk-msg sk-msg--right">
          <div class="sk-msg-bubble sk-msg-bubble--sm"></div>
        </div>
        <div class="sk-msg">
          <div class="sk-msg-av"></div>
          <div class="sk-msg-bubble"></div>
        </div>
      </div>
    </template>

    <!-- Chat 骨架屏 -->
    <template v-else-if="type === 'chat'">
      <div class="sk-chat-header">
        <div class="sk-avatar sk-avatar--sm"></div>
        <div class="sk-lines">
          <div class="sk-line sk-line--md"></div>
          <div class="sk-line sk-line--xs"></div>
        </div>
      </div>
      <div class="sk-content">
        <div class="sk-card"></div>
        <div class="sk-card sk-card--sm"></div>
      </div>
    </template>

    <!-- Quiz 骨架屏 -->
    <template v-else-if="type === 'quiz'">
      <div class="sk-quiz-label"></div>
      <div class="sk-quiz-question"></div>
      <div class="sk-quiz-options">
        <div class="sk-quiz-opt"></div>
        <div class="sk-quiz-opt"></div>
        <div class="sk-quiz-opt"></div>
        <div class="sk-quiz-opt"></div>
      </div>
    </template>

    <!-- Onboarding 骨架屏 -->
    <template v-else-if="type === 'onboarding'">
      <div class="sk-hero">
        <div class="sk-avatar"></div>
      </div>
      <div class="sk-bar"></div>
      <div class="sk-content">
        <div class="sk-msg">
          <div class="sk-msg-av"></div>
          <div class="sk-msg-bubble"></div>
        </div>
        <div class="sk-card sk-card--form"></div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.skeleton-wrap {
  width: 100%;
  height: 100%;
  background: var(--bg);
  padding: 16px;
  overflow: hidden;
}

.sk-hero {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.sk-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}

.sk-avatar--sm {
  width: 40px;
  height: 40px;
}

.sk-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sk-line {
  height: 14px;
  border-radius: 7px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-line--xs { width: 60px; height: 10px; }
.sk-line--sm { width: 120px; }
.sk-line--md { width: 160px; }
.sk-line--lg { width: 200px; height: 20px; }

.sk-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.sk-tab {
  width: 80px;
  height: 36px;
  border-radius: 18px;
  background: #f0f0f0;
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-tab--active {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
}

.sk-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sk-msg {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.sk-msg--right {
  flex-direction: row-reverse;
}

.sk-msg-av {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}

.sk-msg-bubble {
  height: 44px;
  width: 220px;
  border-radius: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-msg-bubble--sm {
  width: 140px;
}

.sk-card {
  height: 120px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.sk-card--sm {
  height: 80px;
}

.sk-card--form {
  height: 200px;
  margin-top: 16px;
}

.sk-bar {
  height: 4px;
  border-radius: 2px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-bottom: 24px;
}

.sk-chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 4px;
  margin-bottom: 12px;
}

.sk-quiz-label {
  height: 12px;
  width: 80px;
  border-radius: 6px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-bottom: 12px;
}

.sk-quiz-question {
  height: 24px;
  width: 280px;
  border-radius: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-bottom: 20px;
}

.sk-quiz-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.sk-quiz-opt {
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
