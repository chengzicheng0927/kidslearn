<script setup lang="ts">
import AvatarBadge from './AvatarBadge.vue'
import TwemojiText from './TwemojiText.vue'

defineProps<{
  role: 'ai' | 'user'
  content: string
  avatar?: string
}>()
</script>

<template>
  <div class="msg" :class="role">
    <div class="av">
      <AvatarBadge :role="role" />
    </div>
    <TwemojiText tag="div" class="bubble" :class="role" :text="content" />
  </div>
</template>

<style scoped>
.msg {
  display: flex;
  gap: 8px;
  animation: slideIn 0.3s ease;
}

.msg.user {
  flex-direction: row-reverse;
}

.av {
  flex-shrink: 0;
}

.bubble {
  max-width: 240px;
  padding: 10px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.55;
  color: var(--txt);
  font-family: inherit;
}

.bubble.ai {
  background: #F5F7FA;
  border-radius: 4px 18px 18px 18px;
  border: 1px solid #E8EDF2;
}

.bubble.user {
  background: #40C4FF;
  color: #fff;
  border-radius: 18px 4px 18px 18px;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
