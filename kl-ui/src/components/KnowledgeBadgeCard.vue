<script setup lang="ts">
import TwemojiText from './TwemojiText.vue'
import type { KnowledgeBadge } from '../mock/data'

const props = defineProps<{
  badges: KnowledgeBadge[]
  earnedIds: string[]
  totalAnswered: number
}>()

const QUESTIONS_PER_BADGE = 10
const MAX_BADGES = 10

// 当前关卡（从 1 开始）
const currentLevel = computed(() => {
  return Math.min(Math.floor(props.totalAnswered / QUESTIONS_PER_BADGE) + 1, MAX_BADGES)
})

// 当前关卡进度（0-100）
const levelProgress = computed(() => {
  return (props.totalAnswered % QUESTIONS_PER_BADGE) * 10
})

// 下一关卡还需多少题
const remainingForNext = computed(() => {
  return QUESTIONS_PER_BADGE - (props.totalAnswered % QUESTIONS_PER_BADGE)
})

// 下一个未解锁的徽章
const nextBadge = computed(() => {
  return props.badges.find(b => !props.earnedIds.includes(b.id))
})

function isEarned(badge: KnowledgeBadge) {
  return props.earnedIds.includes(badge.id)
}
</script>

<script lang="ts">
import { computed } from 'vue'
export default { name: 'KnowledgeBadgeCard' }
</script>

<template>
  <div class="badge-card">
    <!-- 顶部：关卡进度 -->
    <div class="badge-header">
      <div class="badge-title-row">
        <TwemojiText tag="span" text="🌌" />
        <span class="badge-title">知识星球</span>
        <span class="badge-level">第 {{ currentLevel }} 关</span>
      </div>
      <div class="badge-progress-row">
        <div class="badge-progress-bar">
          <div class="badge-progress-fill" :style="{ width: levelProgress + '%' }"></div>
        </div>
        <span class="badge-progress-label">
          <template v-if="nextBadge">
            再答 {{ remainingForNext }} 题解锁
            <TwemojiText :text="nextBadge.icon" />
          </template>
          <template v-else>
            已达成全关卡！🎉
          </template>
        </span>
      </div>
    </div>

    <!-- 徽章网格 -->
    <div class="badge-grid">
      <div
        v-for="badge in badges"
        :key="badge.id"
        class="badge-item"
        :class="{ earned: isEarned(badge), locked: !isEarned(badge) }"
      >
        <div class="badge-icon-wrap">
          <TwemojiText v-if="isEarned(badge)" class="badge-icon" :text="badge.icon" />
          <TwemojiText v-else class="badge-icon" :text="'🔒'" />
        </div>
        <span class="badge-name">{{ badge.title }}</span>
        <span class="badge-req">第 {{ badge.level }} 关</span>
        <!-- 已解锁光环 -->
        <div v-if="isEarned(badge)" class="badge-glow"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.badge-card {
  background: var(--card);
  border-radius: 22px;
  padding: 16px 18px;
  margin: 0 18px 12px;
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 8px rgba(19, 36, 68, 0.07);
}

.badge-header {
  margin-bottom: 14px;
}

.badge-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.badge-title {
  font-size: 15px;
  font-weight: 900;
  color: var(--txt);
  letter-spacing: -0.3px;
}

.badge-level {
  margin-left: auto;
  font-size: 11px;
  font-weight: 700;
  color: var(--or);
  background: var(--or-l);
  padding: 3px 10px;
  border-radius: 999px;
}

.badge-progress-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.badge-progress-bar {
  height: 8px;
  border-radius: 999px;
  background: #F0F0F0;
  overflow: hidden;
}

.badge-progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #FF7043, #FFD54F);
  transition: width 0.5s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.badge-progress-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 徽章网格：5 列 */
.badge-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
}

.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  position: relative;
  padding: 8px 4px 6px;
  border-radius: 14px;
  transition: transform 0.2s;
}

.badge-item.earned {
  background: linear-gradient(135deg, #FFF9E6, #FFFDE7);
}

.badge-item.locked {
  background: #F5F5F5;
  opacity: 0.6;
}

.badge-item.earned:hover {
  transform: scale(1.06);
}

.badge-icon-wrap {
  position: relative;
  z-index: 1;
}

.badge-icon {
  font-size: 26px;
  line-height: 1;
  display: block;
}

.badge-name {
  font-size: 9px;
  font-weight: 700;
  color: var(--txt);
  text-align: center;
  line-height: 1.2;
}

.badge-req {
  font-size: 8.5px;
  font-weight: 600;
  color: var(--muted);
}

/* 已解锁光环动画 */
.badge-glow {
  position: absolute;
  inset: 0;
  border-radius: 14px;
  background: rgba(255, 213, 79, 0.15);
  z-index: 0;
  animation: badgeGlowPulse 2s ease-in-out infinite;
}

@keyframes badgeGlowPulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}
</style>
