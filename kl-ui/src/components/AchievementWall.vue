<script setup lang="ts">
import TwemojiText from './TwemojiText.vue'
import type { Achievement } from '../mock/data'

const props = defineProps<{
  achievements: Achievement[]
  earnedIds: string[]
  stats: {
    totalQuestions: number
    currentStreak: number
    bestStreak: number
    todayCorrect: number
    todayTotal: number
  }
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const categories = [
  { key: 'streak', label: '连胜成就', color: '#FF7043' },
  { key: 'accuracy', label: '准确成就', color: '#29B6F6' },
  { key: 'volume', label: '题量成就', color: '#66BB6A' },
  { key: 'special', label: '特殊成就', color: '#AB47BC' },
]

function isEarned(ach: Achievement) {
  return props.earnedIds.includes(ach.id)
}

function getProgress(ach: Achievement): number {
  switch (ach.type) {
    case 'total_questions':
      return Math.min(100, (props.stats.totalQuestions / ach.requirement) * 100)
    case 'streak_days':
      return Math.min(100, (props.stats.bestStreak / ach.requirement) * 100)
    case 'accuracy_rate':
      if (props.stats.todayTotal === 0) return 0
      return Math.min(100, ((props.stats.todayCorrect / props.stats.todayTotal) * 100 / ach.requirement) * 100)
    default:
      return 0
  }
}
</script>

<script lang="ts">
export default { name: 'AchievementWall' }
</script>

<template>
  <Teleport to="body">
    <div class="overlay" @click.self="emit('close')">
      <div class="sheet">
        <!-- 头部 -->
        <div class="sheet-header">
          <button class="close-btn" @click="emit('close')">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M5 5L15 15M15 5L5 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <div class="sheet-title">
            <TwemojiText text="🏅" />
            我的成就
          </div>
          <div class="earned-count">
            {{ earnedIds.length }} / {{ achievements.length }}
          </div>
        </div>

        <!-- 统计概览 -->
        <div class="stats-overview">
          <div class="stat-item">
            <TwemojiText text="📝" />
            <span class="stat-num">{{ stats.totalQuestions }}</span>
            <span class="stat-label">累计答题</span>
          </div>
          <div class="stat-item">
            <TwemojiText text="🔥" />
            <span class="stat-num">{{ stats.bestStreak }}</span>
            <span class="stat-label">最佳连胜</span>
          </div>
          <div class="stat-item">
            <TwemojiText text="🎯" />
            <span class="stat-num">{{ stats.todayTotal > 0 ? Math.round(stats.todayCorrect / stats.todayTotal * 100) : 0 }}%</span>
            <span class="stat-label">今日准确率</span>
          </div>
        </div>

        <!-- 成就分类 -->
        <div class="achievements-body">
          <div
            v-for="cat in categories"
            :key="cat.key"
            class="category-section"
          >
            <div class="category-title" :style="{ color: cat.color }">
              <TwemojiText :text="cat.key === 'streak' ? '🔥' : cat.key === 'accuracy' ? '🎯' : cat.key === 'volume' ? '📝' : '⭐'" />
              {{ cat.label }}
            </div>
            <div class="ach-grid">
              <div
                v-for="ach in achievements.filter(a => a.category === cat.key)"
                :key="ach.id"
                class="ach-item"
                :class="{ earned: isEarned(ach), locked: !isEarned(ach) }"
              >
                <div class="ach-icon-wrap">
                  <TwemojiText v-if="isEarned(ach)" class="ach-icon" :text="ach.icon" />
                  <TwemojiText v-else class="ach-icon locked-icon" :text="ach.icon" />
                </div>
                <div class="ach-title">{{ ach.title }}</div>
                <div class="ach-desc">{{ ach.desc }}</div>
                <!-- 未解锁进度条 -->
                <div v-if="!isEarned(ach)" class="ach-progress">
                  <div class="ach-progress-bar">
                    <div class="ach-progress-fill" :style="{ width: getProgress(ach) + '%', background: cat.color }"></div>
                  </div>
                  <span class="ach-progress-text">{{ getProgress(ach).toFixed(0) }}%</span>
                </div>
                <!-- 已解锁光环 -->
                <div v-if="isEarned(ach)" class="ach-glow"></div>
              </div>
            </div>
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
  z-index: var(--z-achievement);
  display: flex;
  align-items: flex-end;
  background: rgba(20, 25, 39, 0.5);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease;
}

.sheet {
  width: 100%;
  max-height: 88vh;
  background: #fff;
  border-radius: 28px 28px 0 0;
  overflow-y: auto;
  animation: slideUp 0.35s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.sheet-header {
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 1;
  display: flex;
  align-items: center;
  padding: 18px 20px 14px;
  gap: 10px;
  border-bottom: 1px solid #F0F0F0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #F5F5F5;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
  flex-shrink: 0;
}

.sheet-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 18px;
  font-weight: 900;
  color: #273238;
  letter-spacing: -0.3px;
  flex: 1;
}

.earned-count {
  font-size: 12px;
  font-weight: 700;
  color: var(--or);
  background: var(--or-l);
  padding: 4px 12px;
  border-radius: 999px;
}

/* 统计概览 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 14px 18px;
  border-bottom: 1px solid #F5F5F5;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  background: #FFF8F5;
  border-radius: 14px;
  padding: 10px 6px;
}

.stat-num {
  font-size: 18px;
  font-weight: 900;
  color: #273238;
}

.stat-label {
  font-size: 10px;
  font-weight: 600;
  color: #9e9e9e;
}

/* 成就列表 */
.achievements-body {
  padding: 8px 18px 32px;
}

.category-section {
  margin-top: 16px;
}

.category-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 800;
  margin-bottom: 10px;
}

.ach-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.ach-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 12px 6px;
  border-radius: 16px;
  position: relative;
  transition: transform 0.2s;
}

.ach-item.earned {
  background: linear-gradient(135deg, #FFF9E6, #FFFDE7);
}

.ach-item.locked {
  background: #F5F5F5;
}

.ach-item.earned:hover {
  transform: scale(1.04);
}

.ach-icon-wrap {
  position: relative;
  z-index: 1;
}

.ach-icon {
  font-size: 30px;
  line-height: 1;
}

.ach-icon.locked-icon {
  filter: grayscale(1);
  opacity: 0.3;
}

.ach-title {
  font-size: 11px;
  font-weight: 800;
  color: #273238;
  text-align: center;
  line-height: 1.2;
}

.ach-desc {
  font-size: 9.5px;
  font-weight: 500;
  color: #9e9e9e;
  text-align: center;
  line-height: 1.2;
}

/* 进度条 */
.ach-progress {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.ach-progress-bar {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: #E0E0E0;
  overflow: hidden;
}

.ach-progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.5s ease;
}

.ach-progress-text {
  font-size: 8px;
  font-weight: 600;
  color: #9e9e9e;
}

/* 已解锁光环 */
.ach-glow {
  position: absolute;
  inset: 0;
  border-radius: 16px;
  background: rgba(255, 213, 79, 0.12);
  z-index: 0;
  animation: achGlow 2s ease-in-out infinite;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

@keyframes achGlow {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}
</style>
