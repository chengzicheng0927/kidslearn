<script setup lang="ts">
import { ref } from 'vue'

interface DanmakuItem {
  id: number
  text: string
  bg: string
  y: number
}

const items = ref<DanmakuItem[]>([])
let nextId = 0
const MAX_VISIBLE = 3

const THEMES = [
  { bg: 'rgba(255, 228, 160, 0.75)' },
  { bg: 'rgba(255, 212, 196, 0.75)' },
  { bg: 'rgba(179, 228, 255, 0.75)' },
  { bg: 'rgba(200, 240, 208, 0.75)' },
]

function push(combo: number) {
  const labels = ['', '', '', '不错！', '厉害！', '太强了！', '无敌！']
  const text = combo >= 2
    ? `🔥 ${combo}连击 ${labels[Math.min(combo, 6)] || '太棒了！'}`
    : `🔥 ${combo}连击`

  const theme = THEMES[Math.min(combo - 2, THEMES.length - 1)]
  const item: DanmakuItem = {
    id: nextId++,
    text,
    bg: theme.bg,
    y: 15 + Math.random() * 55,
  }

  items.value.push(item)
  if (items.value.length > MAX_VISIBLE) items.value.shift()

  setTimeout(() => {
    const idx = items.value.findIndex(i => i.id === item.id)
    if (idx !== -1) items.value.splice(idx, 1)
  }, 5000)
}

defineExpose({ push })
</script>

<template>
  <div class="dmk-container">
    <TransitionGroup name="dmk">
      <div
        v-for="item in items"
        :key="item.id"
        class="dmk-item"
        :style="{ background: item.bg, top: item.y + '%' }"
      >
        {{ item.text }}
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.dmk-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 250;
  overflow: hidden;
}

.dmk-item {
  position: absolute;
  left: 100%;
  /* 云朵形状 */
  clip-path: polygon(
    6% 50%, 10% 26%, 18% 12%, 32% 6%,
    48% 3%, 62% 7%, 74% 16%, 83% 28%,
    90% 42%, 94% 55%, 92% 70%, 84% 80%,
    72% 88%, 58% 92%, 42% 94%, 28% 90%,
    16% 82%, 8% 70%, 3% 58%
  );
  padding: 12px 24px;
  color: #5D4037;
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-50%);
  letter-spacing: 0.5px;
  /* 动画开始前保持在右侧可见 */
  animation: dmkFly 5s linear forwards;
  animation-delay: 0.3s;
  opacity: 0;
}

.dmk-enter-active {
  animation: dmkFly 5s linear forwards;
}

.dmk-leave-active {
  opacity: 0;
  transition: opacity 0.8s ease;
}

@keyframes dmkFly {
  0% {
    left: 100%;
    opacity: 1;
  }
  8% {
    opacity: 1;
  }
  88% {
    opacity: 1;
  }
  100% {
    left: -280px;
    opacity: 0;
  }
}
</style>
