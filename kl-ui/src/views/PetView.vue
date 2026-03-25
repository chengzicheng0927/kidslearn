<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import TwemojiText from '../components/TwemojiText.vue'
import KnowledgeBadgeCard from '../components/KnowledgeBadgeCard.vue'
import AchievementWall from '../components/AchievementWall.vue'
import { storeToRefs } from 'pinia'
import { useAppStore } from '../stores/appStore'
import { PET_ARENA_DECO, mockPetEvolutions, mockPetFoodItems, mockKnowledgeBadges, mockAchievements, type PetFoodItem, type PetEmotion } from '../mock/data'

const appStore = useAppStore()
const { child, petState, petEvolutions, petFoodItems, badgeState, badgeDefinitions, achievementState, achievementDefinitions } = storeToRefs(appStore)
const { feedPet, setPetEmotion, syncAchievements, syncBadges, syncPetState } = appStore

// Defensive: fall back to mock data if store refs not yet initialized
const evolutions = computed(() => petEvolutions.value ?? mockPetEvolutions)
const foodItems = computed(() => petFoodItems.value ?? mockPetFoodItems)
const badges = computed(() => badgeDefinitions.value ?? mockKnowledgeBadges)
const achievements = computed(() => achievementDefinitions.value ?? mockAchievements)

onMounted(async () => {
  const childId = child.value?.id
  if (childId) {
    await Promise.all([syncAchievements(childId), syncBadges(childId), syncPetState(childId)])
  }
})

// 走马灯选中索引（点击哪个哪个到中间）
const selectedPetIndex = ref(petState.value.evolutionIndex - 2)

// 根据选中索引，生成5个槽位的内容
const slots = computed(() => {
  const total = evolutions.value.length
  return [0, 1, 2, 3, 4].map(offset => {
    const petIndex = selectedPetIndex.value + offset
    const hasPet = petIndex >= 0 && petIndex < total
    return {
      petIndex,
      hasPet,
      evo: hasPet ? evolutions.value[petIndex] : null,
      deco: hasPet ? PET_ARENA_DECO[petIndex] : null,
      isCurrent: hasPet && petIndex === petState.evolutionIndex,
    }
  })
})

// 进化进度条
const QUESTIONS_PER_EVO = 50
const evoProgress = computed(() => badgeState.totalAnswered % QUESTIONS_PER_EVO)
const evoNeeded = computed(() => QUESTIONS_PER_EVO - evoProgress.value)
const evoProgressPercent = computed(() => (evoProgress.value / QUESTIONS_PER_EVO) * 100)

// 成就墙 & 菜单
const showAchievementWall = ref(false)
const showWrongSet = ref(false)
const showLeaderboard = ref(false)

const showFabMenu = ref(false)
function toggleFab() { showFabMenu.value = !showFabMenu.value }
function closeFab() { showFabMenu.value = false }

const petMessage = ref('点我玩耍吧！')
let messageTimer: number | null = null
let emotionTimer: number | null = null

// 冒泡提示
const bubbleMsg = ref('')
const bubblePos = ref({ x: 0, y: 0 })
let bubbleTimer: number | null = null

function showBubble(msg: string, x: number, y: number) {
  bubbleMsg.value = msg
  bubblePos.value = { x, y }
  if (bubbleTimer) clearTimeout(bubbleTimer)
  bubbleTimer = window.setTimeout(() => {
    bubbleMsg.value = ''
  }, 2500)
}

function triggerEmotion(emotion: PetEmotion, msgs: string[]) {
  setPetEmotion(emotion)
  petMessage.value = msgs[Math.floor(Math.random() * msgs.length)]
  if (emotionTimer) clearTimeout(emotionTimer)
  if (messageTimer) clearTimeout(messageTimer)
  emotionTimer = window.setTimeout(() => {
    setPetEmotion('idle')
    petMessage.value = '点我玩耍吧！'
  }, 2500)
}

async function feed(item: PetFoodItem, e: MouseEvent | TouchEvent) {
  const pos = 'touches' in e ? e.touches[0] : e
  const success = await feedPet(item)
  if (!success) {
    triggerEmotion('hungry', [])
    showBubble('星星不足！去学习赚取吧', pos.clientX, pos.clientY)
    return
  }
  triggerEmotion('happy', [`${item.emoji} 好吃！`])
  showBubble('喂食成功！', pos.clientX, pos.clientY)
}
</script>

<template>
  <div class="app">
    <!-- 弧形菜单 -->
    <div v-if="showFabMenu" class="fab-overlay" @click="closeFab"></div>
    <div class="fab-arc" :class="{ open: showFabMenu }">
      <button class="fab-item" @click="showAchievementWall = true; closeFab()">
        <span class="fab-label">成就墙</span>
        <div class="fab-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
          </svg>
        </div>
      </button>
      <button class="fab-item" @click="showLeaderboard = true; closeFab()">
        <span class="fab-label">积分榜</span>
        <div class="fab-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M8 21V11M12 21V7M16 21V3" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
          </svg>
        </div>
      </button>
      <button class="fab-item" @click="showWrongSet = true; closeFab()">
        <span class="fab-label">错题集</span>
        <div class="fab-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/>
            <path d="M9 9L15 15M15 9L9 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
      </button>
    </div>

    <!-- + 按钮 -->
    <button class="fab-plus" :class="{ open: showFabMenu }" @click="toggleFab">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <line x1="12" y1="5" x2="12" y2="19" stroke="white" stroke-width="2.5" stroke-linecap="round" class="fab-line-v"/>
        <line x1="5" y1="12" x2="19" y2="12" stroke="white" stroke-width="2.5" stroke-linecap="round" class="fab-line-h"/>
      </svg>
    </button>

    <!-- 知识星球徽章卡片 -->
    <KnowledgeBadgeCard
      :badges="badges"
      :earned-ids="badgeState.earnedBadges"
      :total-answered="badgeState.totalAnswered"
    />

    <!-- 宠物头像走马灯 -->
    <div class="carousel-wrap">
      <div class="ci-row">
        <div
          v-for="(slot, i) in slots"
          :key="i"
          class="ci-item"
          :class="{ active: i === 2, empty: !slot.hasPet }"
        >
          <div
            class="ci-ring"
            :style="slot.hasPet ? { background: slot.deco!.bg } : {}"
            @click="slot.hasPet && (selectedPetIndex = slot.petIndex - 2)"
          >
            <TwemojiText
              v-if="slot.hasPet"
              :text="slot.evo!.emoji"
              class="ci-emoji"
            />
            <TwemojiText v-else text="+" class="ci-emoji ci-plus" />
          </div>
          <div class="ci-name">{{ slot.hasPet ? slot.evo!.name : '添加宠物' }}</div>
          <div class="ci-label">{{ slot.hasPet ? slot.deco!.label : '' }}</div>
        </div>
      </div>
    </div>

    <!-- 进化进度条 -->
    <div class="evo-progress">
      <div class="evo-progress-label">
        <span>距离进化还需 {{ evoNeeded }} 题</span>
      </div>
      <div class="evo-progress-bar">
        <div class="evo-progress-fill" :style="{ width: evoProgressPercent + '%' }"></div>
      </div>
    </div>

    <!-- 属性数值（一行三个） -->
    <div class="stats-row">
      <div v-for="(stat, i) in petState.stats" :key="i" class="stat-cell">
        <div class="stat-name">{{ stat.label }}</div>
        <div class="stat-bar">
          <div class="stat-fill" :style="{ '--w': stat.value + '%', background: stat.color }"></div>
        </div>
        <div class="stat-num" :style="{ color: stat.color }">{{ stat.value }}</div>
      </div>
    </div>

    <!-- 冒泡提示 -->
    <Teleport to="body">
      <Transition name="bubble">
        <div v-if="bubbleMsg" class="pet-bubble" :style="{ left: bubblePos.x + 'px', top: bubblePos.y + 'px' }">
          {{ bubbleMsg }}
        </div>
      </Transition>
    </Teleport>

    <!-- 喂食道具 -->
    <div class="feed">
      <div class="feed-title">
        喂食道具 <span>消耗星星解锁属性提升</span>
      </div>
      <div class="feed-grid">
        <div
          v-for="(item, i) in foodItems"
          :key="i"
          class="fi"
          :class="{ locked: item.locked }"
          @click="feed(item, $event)"
        >
          <TwemojiText tag="div" class="fi-em" :text="item.emoji" />
          <div class="fi-name">{{ item.name }}</div>
          <div class="fi-cost">
            <TwemojiText v-if="item.locked" :text="`🔒 ${item.cost}`" />
            <TwemojiText v-else :text="`⭐ ${item.cost}`" />
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 成就墙 -->
  <AchievementWall
    v-if="showAchievementWall"
    :achievements="achievements"
    :earned-ids="achievementState.earnedAchievements"
    :stats="{
      totalQuestions: achievementState.totalQuestions,
      currentStreak: achievementState.currentStreak,
      bestStreak: achievementState.bestStreak,
      todayCorrect: achievementState.todayCorrect,
      todayTotal: achievementState.todayTotal,
    }"
    @close="showAchievementWall = false"
  />

  <!-- 错题集 -->
  <Teleport to="body">
    <Transition name="sheet-up">
      <div v-if="showWrongSet" class="wrong-overlay" @click.self="showWrongSet = false">
        <div class="wrong-sheet">
          <div class="wrong-handle"></div>
          <div class="wrong-head">
            <TwemojiText text="📝" />
            <span>错题集</span>
            <button class="wrong-close" @click="showWrongSet = false">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M4 4L12 12M12 4L4 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <div class="wrong-body">
            <TwemojiText text="🎉" class="wrong-empty-icon" />
            <div class="wrong-empty-text">太棒了！暂无错题</div>
            <div class="wrong-empty-sub">继续保持，正确率100%</div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- 积分榜 -->
  <Teleport to="body">
    <Transition name="sheet-up">
      <div v-if="showLeaderboard" class="wrong-overlay" @click.self="showLeaderboard = false">
        <div class="wrong-sheet">
          <div class="wrong-handle"></div>
          <div class="wrong-head">
            <TwemojiText text="🏆" />
            <span>积分榜</span>
            <button class="wrong-close" @click="showLeaderboard = false">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M4 4L12 12M12 4L4 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
          <div class="wrong-body">
            <TwemojiText text="🌟" class="wrong-empty-icon" />
            <div class="wrong-empty-text">{{ child?.stars ?? 0 }} 星星</div>
            <div class="wrong-empty-sub">继续加油，冲击更高排名</div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

</template>

<style scoped>
.app {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  background: var(--bg);
  padding-bottom: 24px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.hdr {
  padding: 8px 22px 0;
  flex-shrink: 0;
  height: 44px;
}

/* 弧形菜单 */
.fab-overlay {
  position: fixed;
  inset: 0;
  z-index: 148;
  background: rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(2px);
  animation: fadeIn 0.2s ease;
}

.fab-arc {
  position: fixed;
  bottom: 140px;
  right: 16px;
  z-index: 151;
  display: flex;
  flex-direction: column-reverse;
  align-items: flex-end;
  gap: 10px;
  pointer-events: none;
}

.fab-arc.open {
  pointer-events: auto;
}

.fab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  cursor: pointer;
  opacity: 0;
  transform: translateX(20px) scale(0.6);
  transition: opacity 0.2s ease, transform 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.fab-arc.open .fab-item {
  opacity: 1;
  transform: translateX(0) scale(1);
}

.fab-arc.open .fab-item:nth-child(1) { transition-delay: 0.05s; }
.fab-arc.open .fab-item:nth-child(2) { transition-delay: 0.1s; }
.fab-arc.open .fab-item:nth-child(3) { transition-delay: 0.15s; }

.fab-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 3px 14px rgba(0, 0, 0, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--or);
  flex-shrink: 0;
  transition: transform 0.15s;
}

.fab-item:active .fab-icon {
  transform: scale(0.88);
}

.fab-label {
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
  white-space: nowrap;
  background: rgba(0, 0, 0, 0.4);
  padding: 4px 10px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

/* + 按钮 */
.fab-plus {
  position: fixed;
  right: 16px;
  bottom: 80px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--or), #FF8A65);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
  box-shadow: 0 4px 16px rgba(255, 112, 67, 0.5);
  z-index: 150;
  animation: plusBounce 3s ease-in-out infinite;
}

.fab-plus:active {
  transform: scale(0.88);
}

.fab-plus.open {
  background: linear-gradient(135deg, #555, #888);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  animation: none;
}

.fab-line-v {
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  transform-origin: center;
}

.fab-line-h {
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  transform-origin: center;
}

.fab-plus.open .fab-line-v {
  transform: rotate(45deg);
}

.fab-plus.open .fab-line-h {
  transform: rotate(-225deg);
}

/* 走马灯 */
.carousel-wrap {
  width: 100%;
  padding: 16px 0 12px;
  flex-shrink: 0;
}

.ci-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

/* 5个圆圈：中间大(110px)，两边小(76px) */
.ci-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.34, 1.1, 0.64, 1), opacity 0.35s ease;
}

.ci-item:not(.active) {
  transform: scale(0.68);
  opacity: 0.45;
}

.ci-item.active {
  transform: scale(1);
  animation: petFloat 2.8s ease-in-out infinite;
}

.ci-ring {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: width 0.35s ease, height 0.35s ease, box-shadow 0.35s ease;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
}

.ci-item.active .ci-ring {
  width: 110px;
  height: 110px;
  box-shadow: 0 8px 28px rgba(255, 143, 0, 0.3);
}

.ci-item.locked .ci-ring {
  filter: grayscale(1);
  opacity: 0.4;
}

.ci-item.empty {
  pointer-events: none;
}

.ci-item.empty .ci-ring {
  border: 2px dashed var(--border);
  background: var(--card) !important;
  box-shadow: none;
}

.ci-item.empty .ci-emoji {
  color: var(--muted);
}

.ci-plus {
  font-size: 28px !important;
  font-weight: 300;
  color: var(--muted);
}

.ci-emoji {
  font-size: 38px;
  line-height: 1;
  transition: font-size 0.35s ease;
}

.ci-item.active .ci-emoji {
  font-size: 66px;
}

.ci-name {
  font-size: 10px;
  font-weight: 800;
  color: var(--txt);
  text-align: center;
  transition: font-size 0.35s ease;
}

.ci-item.active .ci-name {
  font-size: 13px;
  font-weight: 900;
}

.ci-item.locked .ci-name {
  color: var(--muted);
}

.ci-label {
  font-size: 9px;
  font-weight: 500;
  color: var(--muted);
  text-align: center;
}

.ci-item.active .ci-label {
  color: var(--or);
  font-weight: 700;
}

@keyframes petFloat {
  0%, 100% { transform: scale(1) translateY(0); }
  50% { transform: scale(1) translateY(-6px); }
}

/* 冒泡提示 */
.pet-bubble {
  position: fixed;
  background: #fff;
  border-radius: 16px;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 700;
  color: #273238;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  transform: translate(-50%, -120%);
  z-index: 9999;
  pointer-events: none;
  white-space: nowrap;
  max-width: 200px;
}

.pet-bubble::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  border-left: 7px solid transparent;
  border-right: 7px solid transparent;
  border-top: 7px solid #fff;
}

.bubble-enter-active { animation: bubblePop 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.bubble-leave-active { animation: bubblePop 0.2s ease reverse; }

@keyframes bubblePop {
  from { opacity: 0; transform: translate(-50%, -100%) scale(0.6); }
  to { opacity: 1; transform: translate(-50%, -120%) scale(1); }
}

/* 进化进度条 */
.evo-progress {
  padding: 0 22px 12px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex-shrink: 0;
}

.evo-progress-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted);
  text-align: center;
}

.evo-progress-bar {
  height: 8px;
  border-radius: 4px;
  background: var(--border);
  overflow: hidden;
}

.evo-progress-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #FFD54F, #FF7043);
  transition: width 0.5s cubic-bezier(0.34, 1.2, 0.64, 1);
}

/* 三个属性一行 */
.stats-row {
  width: calc(100% - 44px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  flex-shrink: 0;
}

.stat-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.stat-name {
  font-size: 10px;
  font-weight: 700;
  color: var(--muted);
  text-align: center;
}

.stat-bar {
  height: 6px;
  border-radius: 3px;
  background: var(--border);
  overflow: hidden;
}

.stat-fill {
  height: 100%;
  border-radius: 3px;
  animation: statIn 0.6s ease forwards;
}

.stat-num {
  font-size: 14px;
  font-weight: 900;
  text-align: center;
}

.feed {
  padding: 12px 20px 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 80px;
}

.feed-title {
  font-size: 13px;
  font-weight: bold;
  color: var(--txt);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.feed-title span {
  font-size: 11px;
  color: var(--muted);
  font-weight: 500;
}

.feed-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 9px;
  flex: 1;
  align-content: start;
}

.fi {
  background: var(--card);
  border-radius: 18px;
  padding: 12px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.07);
}

.fi:hover {
  border-color: var(--or);
  background: var(--or-l);
  transform: translateY(-2px);
}

.fi:active {
  transform: scale(0.92);
}

.fi.locked {
  opacity: 0.4;
  cursor: not-allowed;
  filter: grayscale(0.4);
}

.fi-em {
  font-size: 26px;
  line-height: 1;
}

.fi-name {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--txt);
}

.fi-cost {
  font-size: 10px;
  font-weight: bold;
  color: var(--yw-d);
  display: flex;
  align-items: center;
  gap: 2px;
}

@keyframes statIn {
  from { width: 0; }
  to { width: var(--w); }
}

/* 错题集/积分榜 底部面板 */
.wrong-overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-overlay);
  background: rgba(20, 25, 39, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.wrong-sheet {
  width: 100%;
  background: #fff;
  border-radius: 24px 24px 0 0;
  padding: 0 0 28px;
  animation: sheetSlideUp 0.35s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.wrong-handle {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: var(--border);
  margin: 12px auto 0;
}

.wrong-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px 10px;
  border-bottom: 1px solid var(--border);
}

.wrong-head span {
  font-size: 16px;
  font-weight: 900;
  color: var(--txt);
  flex: 1;
}

.wrong-close {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--bg);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
}

.wrong-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  gap: 8px;
}

.wrong-empty-icon {
  font-size: 64px;
  line-height: 1;
}

.wrong-empty-text {
  font-size: 20px;
  font-weight: 900;
  color: var(--txt);
}

.wrong-empty-sub {
  font-size: 13px;
  color: var(--muted);
}

@keyframes plusBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes sheetSlideUp {
  from { transform: translateY(100%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.sheet-up-enter-active {
  animation: sheetSlideUp 0.35s cubic-bezier(0.34, 1.2, 0.64, 1);
}
.sheet-up-leave-active {
  animation: sheetSlideUp 0.25s ease reverse;
}

</style>
