<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAppStore } from '../stores/appStore'

defineProps<{
  activeSubject: 'en' | 'ma'
}>()

const emit = defineEmits<{
  (e: 'switch', subject: 'en' | 'ma'): void
}>()

const store = useAppStore()
const { dailyTasks, todayStars, todayMaxStars } = storeToRefs(store)

// 当天没有任何任务（后端返回 todayMaxStars === 0）
const hasNoTasks = computed(() => todayMaxStars.value === 0)

const R = 20
const CIRC = 2 * Math.PI * R

const enPct = computed(() => {
  const target = dailyTasks.value?.enTarget || 5
  if (target === 0) return 100
  return Math.min(100, ((dailyTasks.value?.enDone || 0) / target) * 100)
})
const maPct = computed(() => {
  const target = dailyTasks.value?.maTarget || 3
  if (target === 0) return 100
  return Math.min(100, ((dailyTasks.value?.maDone || 0) / target) * 100)
})
const enDone = computed(() => {
  const target = dailyTasks.value?.enTarget || 5
  return target === 0 || (dailyTasks.value?.enDone || 0) >= target
})
const maDone = computed(() => {
  const target = dailyTasks.value?.maTarget || 3
  return target === 0 || (dailyTasks.value?.maDone || 0) >= target
})
const allDone = computed(() => enDone.value && maDone.value)

const totalDone = computed(() => (dailyTasks.value?.enDone || 0) + (dailyTasks.value?.maDone || 0))
const totalTarget = computed(() => Math.max(1, (dailyTasks.value?.enTarget || 5) + (dailyTasks.value?.maTarget || 3)))
const stageNum = computed(() => Math.ceil(totalDone.value / 2) || 1)
</script>

<template>
  <div class="dtc">
    <!-- 当天无任务 -->
    <div v-if="hasNoTasks" class="no-task-area">
      <div class="no-task-top">
        <span class="no-task-icon">🌟</span>
        <span class="no-task-title">今日休息</span>
        <span class="no-task-icon">🌟</span>
      </div>
      <div class="no-task-msg">今天没有学习任务</div>
      <div class="no-task-sub">好好休息，养足精神，明天继续加油！</div>
    </div>

    <!-- 全部完成 -->
    <div v-else-if="allDone" class="all-done">
      <span>🎉</span>
      <span>今日任务全部完成！太棒啦！</span>
      <span>🎉</span>
    </div>

    <template v-else>
      <!-- 顶栏 -->
      <div class="top-bar">
        <div class="top-left">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M7 1L8.55 5.09H13L9.64 7.55L10.91 12L7 9.36L3.09 12L4.36 7.55L1 5.09H5.45L7 1Z" fill="#FFD54F"/>
          </svg>
          <span class="top-label">今日任务</span>
        </div>
        <div class="top-right">
          <span class="stage-tag">第{{ stageNum }}阶段</span>
        </div>
      </div>

      <!-- 双圆环 -->
      <div class="rings-area">
        <!-- 英语 -->
        <div
          class="ring-col"
          :class="{ done: enDone, active: activeSubject === 'en' }"
          @click="emit('switch', 'en')"
        >
          <div class="ring-wrap">
            <svg width="56" height="56" viewBox="0 0 48 48">
              <circle cx="24" cy="24" :r="R" fill="none" stroke="#E0F0FF" stroke-width="5"/>
              <circle
                cx="24" cy="24" :r="R"
                fill="none" stroke="#40C4FF"
                stroke-width="5" stroke-linecap="round"
                :stroke-dasharray="CIRC" :stroke-dashoffset="CIRC * (1 - enPct / 100)"
                transform="rotate(-90 24 24)"
                class="arc"
              />
              <text x="24" y="24" text-anchor="middle" dominant-baseline="central" font-size="10" font-weight="900" fill="#29B6F6">
                {{ enDone ? '✓' : dailyTasks.enDone }}
              </text>
            </svg>
          </div>
          <div class="ring-sub">英语练习</div>
        </div>

        <!-- 进度条+星星 -->
        <div class="center-info">
          <div class="total-track">
            <div class="total-fill" :style="{ width: Math.min(100, (totalDone / totalTarget) * 100) + '%' }"></div>
          </div>
          <div class="total-nums">
            <span class="total-done">{{ totalDone }}/{{ totalTarget }}</span>
            <span class="total-stars">
              <svg width="11" height="11" viewBox="0 0 14 14" fill="none">
                <path d="M7 1L8.55 5.09H13L9.64 7.55L10.91 12L7 9.36L3.09 12L4.36 7.55L1 5.09H5.45L7 1Z" fill="#FFD54F"/>
              </svg>
              {{ todayStars }}/{{ todayMaxStars }}
            </span>
          </div>
        </div>

        <!-- 数学 -->
        <div
          class="ring-col"
          :class="{ done: maDone, active: activeSubject === 'ma' }"
          @click="emit('switch', 'ma')"
        >
          <div class="ring-wrap">
            <svg width="56" height="56" viewBox="0 0 48 48">
              <circle cx="24" cy="24" :r="R" fill="none" stroke="#E0F5EA" stroke-width="5"/>
              <circle
                cx="24" cy="24" :r="R"
                fill="none" stroke="#69F0AE"
                stroke-width="5" stroke-linecap="round"
                :stroke-dasharray="CIRC" :stroke-dashoffset="CIRC * (1 - maPct / 100)"
                transform="rotate(-90 24 24)"
                class="arc ma"
              />
              <text x="24" y="24" text-anchor="middle" dominant-baseline="central" font-size="10" font-weight="900" fill="#27AE60">
                {{ maDone ? '✓' : dailyTasks.maDone }}
              </text>
            </svg>
          </div>
          <div class="ring-sub">数学练习</div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.dtc {
  margin: 0 16px 8px;
  background: var(--card);
  border-radius: 16px;
  border: 1.5px solid var(--border);
  padding: 12px 16px;
  box-shadow: var(--shadow-card);
}

.all-done {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #7A9FD4;
  padding: 4px 0;
}

.no-task-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 0;
}

.no-task-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.no-task-icon {
  font-size: 16px;
}

.no-task-title {
  font-size: 15px;
  font-weight: 700;
  color: #7A9FD4;
}

.no-task-msg {
  font-size: 14px;
  font-weight: 600;
  color: var(--txt);
  margin-top: 4px;
}

.no-task-sub {
  font-size: 12px;
  color: var(--muted);
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.top-left {
  display: flex;
  align-items: center;
  gap: 5px;
}

.top-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--txt);
}

.stage-tag {
  font-size: 10px;
  font-weight: 600;
  color: var(--muted);
  background: rgba(0,0,0,0.04);
  padding: 2px 8px;
  border-radius: 20px;
}

.rings-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ring-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  cursor: pointer;
  transition: opacity 0.2s;
}

.ring-col.done {
  opacity: 0.4;
}

.ring-col.active .ring-wrap {
  filter: drop-shadow(0 0 6px rgba(64, 196, 255, 0.4));
}

.ring-col.active .arc {
  stroke-width: 6;
}

.ring-col:deep(.arc.ma) {
  /* 数学选中效果 */
}

.ring-wrap {
  transition: filter 0.2s;
}

.arc {
  transition: stroke-dashoffset 0.8s ease;
}

.arc.ma {
  stroke: #69F0AE;
}

.ring-sub {
  font-size: 10px;
  font-weight: 600;
  color: var(--txt);
}

.center-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 12px;
}

.total-track {
  height: 6px;
  background: #EDE8E2;
  border-radius: 3px;
  overflow: hidden;
}

.total-fill {
  height: 100%;
  background: linear-gradient(90deg, #40C4FF, #69F0AE);
  border-radius: 3px;
  transition: width 0.6s ease;
}

.total-nums {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.total-done {
  font-size: 12px;
  font-weight: 700;
  color: var(--txt);
}

.total-stars {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 700;
  color: #7A5800;
}
</style>
