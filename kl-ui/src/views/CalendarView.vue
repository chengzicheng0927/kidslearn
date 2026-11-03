<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import TwemojiText from '../components/TwemojiText.vue'
import { useAppStore, getSelectedStudyDay } from '../stores/appStore'
import { DAYS, MONTHS, MONTH_DAYS } from '../mock/data'

const now = new Date()
const currentYear = ref(now.getFullYear())
const currentMonth = ref(now.getMonth() + 1)
const selectedDay = ref(now.getDate())
const appStore = useAppStore()
const { child, calendarData, calendarStats } = storeToRefs(appStore)
const { syncCalendar, syncCalendarStats } = appStore

// 初始化时从 API 加载当月数据
onMounted(async () => {
  const childId = child.value?.id
  if (childId) {
    await Promise.all([
      syncCalendar(childId, currentYear.value, currentMonth.value),
      syncCalendarStats(childId, currentYear.value, currentMonth.value),
    ])
  }
})

// 补偿：onMounted 执行时 child 可能还未加载完成（checkUserStatus 还在异步中）
// 等 child 加载完成后立即同步日历数据（仅执行一次）
watch(() => child.value?.id, async (childId) => {
  if (childId) {
    await Promise.all([
      syncCalendar(childId, currentYear.value, currentMonth.value),
      syncCalendarStats(childId, currentYear.value, currentMonth.value),
    ])
  }
}, { immediate: false })

// 月份切换时重新获取数据
watch([currentYear, currentMonth], async ([newYear, newMonth]) => {
  const childId = child.value?.id
  if (childId) {
    await Promise.all([
      syncCalendar(childId, newYear, newMonth),
      syncCalendarStats(childId, newYear, newMonth),
    ])
  }
})

const streak = computed(() => calendarStats.value?.currentStreak ?? 0)
const totalStars = computed(() => calendarStats.value?.totalStars ?? 0)
const monthStudyDays = computed(() => calendarStats.value?.totalDays ?? 0)

const cells = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month - 1, 1).getDay()
  const daysInMonth = month === 2 && ((year % 4 === 0 && year % 100 !== 0) || year % 400 === 0) ? 29 : MONTH_DAYS[month - 1]
  const key = `${year}-${month}`
  const monthData = calendarData.value[key] || {}

  const result: { day: number; info: ReturnType<typeof getSelectedStudyDay>; isToday: boolean; isSelected: boolean }[] = []

  for (let i = 0; i < firstDay; i++) {
    result.push({ day: 0, info: null, isToday: false, isSelected: false })
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const isToday = year === now.getFullYear() && month === now.getMonth() + 1 && d === now.getDate()
    result.push({
      day: d,
      info: monthData[d] || null,
      isToday,
      isSelected: d === selectedDay.value
    })
  }

  return result
})

const selectedInfo = computed(() => {
  return getSelectedStudyDay(calendarData.value, currentYear.value, currentMonth.value, selectedDay.value)
})

const selectedDateInfo = computed(() => {
  const dow = new Date(currentYear.value, currentMonth.value - 1, selectedDay.value).getDay()
  const info = selectedInfo.value
  const isToday = currentYear.value === now.getFullYear() && currentMonth.value === now.getMonth() + 1 && selectedDay.value === now.getDate()
  return {
    day: `${selectedDay.value}日`,
    meta: info ? `星期${DAYS[dow]} · 已学习 ${info.minutes} 分钟` : `星期${DAYS[dow]} · 暂无记录`,
    isToday
  }
})

function prevMonth() {
  currentMonth.value--
  if (currentMonth.value < 1) {
    currentMonth.value = 12
    currentYear.value--
  }
}

function nextMonth() {
  currentMonth.value++
  if (currentMonth.value > 12) {
    currentMonth.value = 1
    currentYear.value++
  }
}

function selectDay(day: number) {
  selectedDay.value = day
}
</script>

<template>
  <div class="app">
    <div class="month-hero">
      <div class="month-nav">
        <button class="mnav" @click="prevMonth">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M10 3L5 8L10 13" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" opacity=".6"/>
          </svg>
        </button>
        <div class="month-center">
          <div class="month-big">{{ MONTHS[currentMonth - 1] }}</div>
          <div class="year-sm">{{ currentYear }}年</div>
        </div>
        <button class="mnav" @click="nextMonth">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M6 3L11 8L6 13" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" opacity=".6"/>
          </svg>
        </button>
      </div>
    </div>

    <div class="stats">
      <div class="sc hot" :class="{ 'streak-active': streak > 0 }">
        <TwemojiText tag="div" class="v streak-display" :text="`${streak}`" />
        <span class="streak-fire" v-if="streak > 0">🔥</span>
        <div class="l">连续天数</div>
      </div>
      <div class="sc">
        <div class="v">{{ monthStudyDays }}</div>
        <div class="l">本月学习</div>
      </div>
      <div class="sc">
        <TwemojiText tag="div" class="v" :text="`${totalStars}⭐`" />
        <div class="l">总积分</div>
      </div>
    </div>

    <div class="legend">
      <span class="leg-l">少</span>
      <div class="leg-c" style="background: var(--t4); border: 1px solid rgba(255,255,255,.1)"></div>
      <div class="leg-c" style="background: rgba(255,112,67,.16)"></div>
      <div class="leg-c" style="background: rgba(255,112,67,.36)"></div>
      <div class="leg-c" style="background: rgba(255,112,67,.62)"></div>
      <div class="leg-c" style="background: rgba(255,112,67,.84)"></div>
      <span class="leg-l">多</span>
    </div>

    <div class="dow">
      <div v-for="(d, i) in DAYS" :key="i" class="dw" :class="{ we: i === 0 || i === 6 }">{{ d }}</div>
    </div>

    <div class="grid">
      <div
        v-for="(cell, i) in cells"
        :key="i"
        class="cell"
        :class="{
          emp: cell.day === 0,
          [`h${cell.info?.heat || 0}`]: cell.info,
          today: cell.isToday,
          sel: cell.isSelected
        }"
        @click="cell.day && selectDay(cell.day)"
      >
        <template v-if="cell.day">
          <span class="dn">{{ cell.day }}</span>
          <div v-if="cell.info?.subjects" class="sdots">
            <div v-for="(subject, j) in cell.info.subjects" :key="j" class="sdot" :style="{ background: subject.color }"></div>
          </div>
        </template>
      </div>
    </div>

    <div class="detail">
      <div class="handle"></div>
      <div class="det-head">
        <div>
          <div class="det-day">{{ selectedDateInfo.day }}</div>
          <div class="det-meta">{{ selectedDateInfo.meta }}</div>
        </div>
        <div v-if="selectedDateInfo.isToday" class="today-badge">今天</div>
      </div>
      <div class="det-body">
        <template v-if="selectedInfo?.subjects">
          <div
            v-for="(subject, i) in selectedInfo.subjects"
            :key="i"
            class="subj-row"
            :style="{ animationDelay: `${i * 0.07}s` }"
          >
            <div class="sr-top">
              <TwemojiText tag="div" class="sr-icon" :style="{ background: subject.bg }" :text="subject.icon" />
              <div class="sr-info">
                <div class="sr-name">{{ subject.name }} · {{ subject.topic }}</div>
                <div class="sr-topic">已完成全部任务</div>
              </div>
              <TwemojiText tag="div" class="sr-stars" :text="`⭐ +${subject.stars}`" />
            </div>
            <div class="bar-row">
              <div class="bar-lbl">正确</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ '--w': subject.accuracy + '%', background: subject.color, animationDelay: `${i * 0.1 + 0.15}s` }"></div>
              </div>
              <div class="bar-val">{{ subject.accuracy }}%</div>
            </div>
            <div class="bar-row">
              <div class="bar-lbl">星星</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ '--w': Math.round(subject.stars / 20 * 100) + '%', background: 'var(--yw)', animationDelay: `${i * 0.1 + 0.25}s` }"></div>
              </div>
              <div class="bar-val">{{ subject.stars }}</div>
            </div>
          </div>
        </template>
        <div v-else class="empty">
          <TwemojiText tag="div" class="empty-i" text="📭" />
          <p>这天没有学习记录<br>快去学习打卡吧！</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100vh;
  background: var(--bg);
  color: var(--txt);
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.month-hero {
  padding: 6px 26px 0;
  flex-shrink: 0;
}

.month-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mnav {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--s2);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.mnav:active {
  background: rgba(255, 255, 255, 0.2);
}

.month-center {
  text-align: center;
}

.month-big {
  font-size: 40px;
  font-weight: 900;
  color: var(--txt);
  letter-spacing: -1.5px;
  line-height: 1;
}

.year-sm {
  font-size: 13px;
  color: var(--t3);
  font-weight: 600;
  margin-top: 3px;
  letter-spacing: 0.6px;
}

.stats {
  display: flex;
  gap: 8px;
  padding: 12px 26px 0;
  flex-shrink: 0;
}

.sc {
  flex: 1;
  background: var(--s3);
  border-radius: 16px;
  padding: 11px 10px;
  border: 1px solid var(--border);
  text-align: center;
}

.sc .v {
  font-size: 21px;
  font-weight: 900;
  color: var(--txt);
  line-height: 1;
}

.sc .l {
  font-size: 9.5px;
  color: var(--t3);
  margin-top: 4px;
  font-weight: 600;
  letter-spacing: 0.4px;
  text-transform: uppercase;
}

.sc.hot {
  background: linear-gradient(135deg, rgba(255, 112, 67, 0.18), rgba(255, 213, 79, 0.08));
  border-color: rgba(255, 112, 67, 0.28);
}

.sc.hot .v {
  color: var(--or);
}

/* 连续打卡动画 */
.streak-active {
  position: relative;
  animation: streakPulse 1.5s ease-in-out infinite;
}

.streak-fire {
  font-size: 18px;
  line-height: 1;
  display: inline-block;
  animation: fireBounce 0.6s ease-in-out infinite;
  margin-left: 2px;
}

.streak-display {
  display: inline;
}

@keyframes streakPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(255, 112, 67, 0.3);
  }
  60% {
    box-shadow: 0 0 0 6px rgba(255, 112, 67, 0);
  }
}

@keyframes fireBounce {
  0%, 100% {
    transform: scale(1) rotate(-5deg);
  }
  50% {
    transform: scale(1.2) rotate(5deg);
  }
}

.legend {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 26px 6px;
  flex-shrink: 0;
}

.leg-l {
  font-size: 10px;
  color: var(--t3);
  font-weight: 600;
}

.leg-c {
  width: 14px;
  height: 14px;
  border-radius: 4px;
}

.dow {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 2px 20px 5px;
  flex-shrink: 0;
}

.dw {
  text-align: center;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--t3);
  letter-spacing: 0.3px;
}

.dw.we {
  color: rgba(255, 80, 100, 0.7);
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 3.5px;
  padding: 0 20px;
  flex-shrink: 0;
}

.cell {
  position: relative;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  aspect-ratio: 1;
  transition: transform 0.12s;
}

.cell:active {
  transform: scale(0.84);
}

.cell.emp {
  pointer-events: none;
  opacity: 0;
}

.cell { background: #F0EBE7; }
.cell.h0 { background: #F0EBE7; }
.cell.h1 { background: rgba(255, 112, 67, 0.16); }
.cell.h2 { background: rgba(255, 112, 67, 0.36); }
.cell.h3 { background: rgba(255, 112, 67, 0.62); }
.cell.h4 { background: rgba(255, 112, 67, 0.84); }
.cell.today { background: var(--or); animation: todayPulse 2.5s ease-in-out infinite; }
.cell.sel::after {
  content: '';
  position: absolute;
  inset: -2.5px;
  border: 2.5px solid #fff;
  border-radius: 13px;
  pointer-events: none;
}

.dn {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--txt);
  line-height: 1;
  margin-bottom: 2px;
}

.cell.h1 .dn,
.cell.h2 .dn,
.cell.h3 .dn,
.cell.h4 .dn {
  color: #fff;
}

.cell.today .dn {
  color: #fff;
  font-weight: 900;
}

.sdots {
  display: flex;
  gap: 2px;
}

.sdot {
  width: 3.5px;
  height: 3.5px;
  border-radius: 50%;
}

.detail {
  flex: 1;
  background: var(--s2);
  border-radius: 22px 22px 0 0;
  margin-top: 9px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-top: 1px solid var(--border);
}

.handle {
  width: 38px;
  height: 4px;
  border-radius: 2px;
  background: var(--t4);
  margin: 10px auto 0;
  flex-shrink: 0;
}

.det-head {
  padding: 12px 24px 12px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.det-day {
  font-size: 30px;
  font-weight: 900;
  color: var(--txt);
  line-height: 1;
}

.det-meta {
  font-size: 13px;
  color: var(--t2);
  margin-top: 3px;
  font-weight: 500;
}

.today-badge {
  background: linear-gradient(135deg, var(--or), #FF8A65);
  border-radius: 10px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: bold;
  color: #fff;
}

.det-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  scrollbar-width: none;
}

.det-body::-webkit-scrollbar {
  display: none;
}

.subj-row {
  background: var(--s3);
  border-radius: 18px;
  padding: 14px 16px;
  animation: slideUp 0.3s ease both;
}

.sr-top {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 11px;
}

.sr-icon {
  width: 40px;
  height: 40px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.sr-info {
  flex: 1;
}

.sr-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--txt);
}

.sr-topic {
  font-size: 12px;
  color: var(--t2);
  margin-top: 2px;
}

.sr-stars {
  font-size: 14px;
  font-weight: bold;
  color: var(--yw);
  white-space: nowrap;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.bar-row:last-child {
  margin-bottom: 0;
}

.bar-lbl {
  font-size: 10px;
  color: var(--t3);
  width: 28px;
  text-align: right;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.bar-track {
  flex: 1;
  height: 5px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.07);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 3px;
  animation: barIn 0.55s ease forwards;
  width: 0;
}

.bar-val {
  font-size: 10.5px;
  color: var(--t2);
  font-weight: 600;
  width: 30px;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 8px;
  animation: fadeIn 0.3s ease;
}

.empty-i {
  font-size: 48px;
  opacity: 0.5;
}

.empty p {
  font-size: 13px;
  color: var(--t2);
  text-align: center;
  line-height: 1.6;
}

@keyframes barIn {
  from { width: 0; }
  to { width: var(--w); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes todayPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 112, 67, 0.45); }
  60% { box-shadow: 0 0 0 7px rgba(255, 112, 67, 0); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
