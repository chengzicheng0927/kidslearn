<script setup lang="ts">
import { ref, watch } from 'vue'
import TwemojiText from './TwemojiText.vue'

const props = defineProps<{
  visible: boolean
  type: 'achievement' | 'badge' | 'evolution'
  icon: string
  title: string
  desc: string
}>()

const emit = defineEmits<{
  (e: 'done'): void
}>()

const phase = ref<'enter' | 'hold' | 'leave'>('enter')
let timer1: number | null = null
let timer2: number | null = null

watch(() => props.visible, (val) => {
  if (val) {
    phase.value = 'enter'
    timer1 = window.setTimeout(() => { phase.value = 'hold' }, 600)
    timer2 = window.setTimeout(() => {
      phase.value = 'leave'
      setTimeout(() => emit('done'), 400)
    }, 2500)
  } else {
    if (timer1) clearTimeout(timer1)
    if (timer2) clearTimeout(timer2)
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition name="milestone">
      <div v-if="visible" class="milestone-overlay" :class="phase">
        <!-- 背景粒子 -->
        <div class="particles">
          <div v-for="i in 20" :key="i" class="particle" :style="{
            '--x': Math.random() * 100 + '%',
            '--y': Math.random() * 100 + '%',
            '--d': (Math.random() * 2 + 1) + 's',
            '--delay': (Math.random() * 1) + 's',
            '--size': (Math.random() * 8 + 8) + 'px',
          }">
            <TwemojiText :text="['⭐', '🌟', '✨', '💫', '🎉', '🏅', '🌈'][i % 7]" />
          </div>
        </div>

        <!-- 中央卡片 -->
        <div class="milestone-card" :class="phase">
          <div class="card-glow"></div>
          <div class="card-icon">
            <TwemojiText :text="icon" />
          </div>
          <div class="card-title">{{ title }}</div>
          <div class="card-desc">{{ desc }}</div>
          <div class="card-badge-type">
            <TwemojiText :text="type === 'achievement' ? '🏅' : type === 'badge' ? '🌌' : '🐾'" />
            {{ type === 'achievement' ? '成就解锁' : type === 'badge' ? '星球徽章' : '宠物进化' }}
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.milestone-overlay {
  position: fixed;
  inset: 0;
  z-index: 1800;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(10, 10, 30, 0.75);
  backdrop-filter: blur(8px);
  overflow: hidden;
}

/* 粒子 */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  font-size: var(--size);
  left: var(--x);
  top: var(--y);
  animation: particleFloat var(--d) ease-in-out var(--delay) infinite;
  opacity: 0;
}

.milestone-overlay.enter .particle,
.milestone-overlay.hold .particle {
  opacity: 1;
}

/* 中央卡片 */
.milestone-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 36px 40px;
  background: linear-gradient(135deg, #FFF9E6, #FFFFFF);
  border-radius: 28px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  z-index: 1;
  transition: all 0.4s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.milestone-card.enter {
  transform: scale(0.3) rotate(-10deg);
  opacity: 0;
}

.milestone-card.hold {
  transform: scale(1) rotate(0);
  opacity: 1;
}

.milestone-card.leave {
  transform: scale(0.8);
  opacity: 0;
}

.card-glow {
  position: absolute;
  inset: -4px;
  border-radius: 32px;
  background: linear-gradient(135deg, #FFD54F, #FF7043, #FF9800);
  z-index: -1;
  animation: glowRotate 2s linear infinite;
}

.card-icon {
  font-size: 72px;
  line-height: 1;
  animation: iconBounce 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275) 0.4s both;
}

.card-title {
  font-size: 22px;
  font-weight: 900;
  color: #273238;
  letter-spacing: -0.4px;
  animation: fadeUp 0.4s ease 0.5s both;
}

.card-desc {
  font-size: 14px;
  color: #73808c;
  text-align: center;
  line-height: 1.5;
  animation: fadeUp 0.4s ease 0.6s both;
}

.card-badge-type {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 700;
  color: #FF7043;
  background: #FFF3EF;
  padding: 4px 14px;
  border-radius: 999px;
  margin-top: 4px;
  animation: fadeUp 0.4s ease 0.7s both;
}

/* 过渡动画 */
.milestone-enter-active {
  animation: milestoneIn 0.5s ease;
}

.milestone-leave-active {
  transition: opacity 0.4s ease;
}

.milestone-leave-to {
  opacity: 0;
}

@keyframes particleFloat {
  0%, 100% {
    transform: translateY(0) rotate(0deg) scale(0.5);
    opacity: 0;
  }
  20% {
    opacity: 1;
    transform: translateY(-30px) rotate(90deg) scale(1);
  }
  80% {
    opacity: 0.8;
  }
  100% {
    transform: translateY(-80px) rotate(180deg) scale(0.3);
    opacity: 0;
  }
}

@keyframes glowRotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes iconBounce {
  0% { transform: scale(0) rotate(-20deg); }
  60% { transform: scale(1.3) rotate(5deg); }
  100% { transform: scale(1) rotate(0); }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes milestoneIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
