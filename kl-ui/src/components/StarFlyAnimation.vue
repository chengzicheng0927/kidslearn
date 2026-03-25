<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  count: number
}>()

const emit = defineEmits<{
  (e: 'done'): void
}>()

const flyingStars = ref<Array<{ id: number; x: number; delay: number; endX: number }>>([])

watch(() => props.visible, (val) => {
  if (val) {
    // 生成飞星
    flyingStars.value = Array.from({ length: props.count }, (_, i) => ({
      id: i,
      x: 30 + Math.random() * 60,
      delay: i * 80,
      endX: 20 + Math.random() * 40,
    }))
    // 动画结束后通知
    setTimeout(() => {
      flyingStars.value = []
      emit('done')
    }, 1800)
  }
})
</script>

<template>
  <Teleport to="body">
    <div class="star-fly-container">
      <TransitionGroup name="star-fly">
        <div
          v-for="star in flyingStars"
          :key="star.id"
          class="flying-star"
          :style="{
            left: star.x + '%',
            animationDelay: star.delay + 'ms',
            '--end-x': star.endX + '%',
          }"
        >
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M10 1L12.39 6.26L18 7.27L14 11.14L14.93 17L10 14.27L5.07 17L6 11.14L2 7.27L7.61 6.26L10 1Z" fill="#FFD54F" stroke="#FF8F00" stroke-width="1"/>
          </svg>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.star-fly-container {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: var(--z-modal-close);
  overflow: hidden;
}

.flying-star {
  position: absolute;
  bottom: 30%;
  font-size: 20px;
  animation: flyUp 1.4s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}

@keyframes flyUp {
  0% {
    transform: translate(0, 0) scale(1) rotate(0deg);
    opacity: 1;
  }
  50% {
    opacity: 1;
    transform: translate(calc(var(--end-x) - 50%), -200px) scale(1.2) rotate(180deg);
  }
  100% {
    transform: translate(calc(var(--end-x) - 50%), -400px) scale(0.5) rotate(360deg);
    opacity: 0;
  }
}
</style>
