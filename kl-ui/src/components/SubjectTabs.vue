<script setup lang="ts">
import TwemojiText from './TwemojiText.vue'

defineProps<{
  activeSubject: 'en' | 'ma'
}>()

const emit = defineEmits<{
  (e: 'update:activeSubject', value: 'en' | 'ma'): void
}>()
</script>

<template>
  <div class="tabs">
    <div
      class="tab"
      :class="activeSubject === 'en' ? 'en' : 'inactive'"
      @click="emit('update:activeSubject', 'en')"
    >
      <TwemojiText text="📖 英语" />
    </div>
    <div
      class="tab"
      :class="activeSubject === 'ma' ? 'ma' : 'inactive'"
      @click="emit('update:activeSubject', 'ma')"
    >
      <TwemojiText text="🔢 数学" />
    </div>
  </div>
</template>

<style scoped>
.tabs {
  padding: 0 16px 10px;
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.tab {
  padding: 7px 16px;
  border-radius: 22px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.tab.en {
  background: var(--bl-l);
  color: var(--bl-d);
  border-color: var(--bl);
}

.tab.ma {
  background: var(--gn-l);
  color: var(--gn-d);
  border-color: var(--gn);
}

.tab.inactive {
  background: var(--bg);
  color: var(--muted);
  border-color: transparent;
}
</style>
