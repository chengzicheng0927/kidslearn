<script setup lang="ts">
defineProps<{
  batteryLevel?: number
}>()
</script>

<template>
  <div class="status-bar">
    <span class="time">9:41</span>
    <div class="icons">
      <svg width="16" height="11" viewBox="0 0 16 11" fill="none">
        <rect x="0" y="3.5" width="2.5" height="7.5" rx=".8" fill="#2D2D2D" opacity=".3"/>
        <rect x="3.5" y="2" width="2.5" height="9" rx=".8" fill="#2D2D2D" opacity=".55"/>
        <rect x="7" y=".5" width="2.5" height="10.5" rx=".8" fill="#2D2D2D" opacity=".8"/>
        <rect x="10.5" y="0" width="2.5" height="11" rx=".8" fill="#2D2D2D"/>
      </svg>
      <div class="battery">
        <div class="battery-inner" :style="{ width: (batteryLevel ?? 78) + '%' }"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.status-bar {
  height: 50px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 24px 10px;
  flex-shrink: 0;
}

.time {
  font-size: 16px;
  font-weight: 600;
  color: var(--txt);
}

.icons {
  display: flex;
  align-items: center;
  gap: 6px;
}

.battery {
  width: 24px;
  height: 12px;
  border: 1.5px solid rgba(45, 45, 45, 0.4);
  border-radius: 3px;
  position: relative;
  padding: 1.5px;
  display: flex;
  align-items: center;
}

.battery::after {
  content: '';
  position: absolute;
  right: -5px;
  top: 50%;
  transform: translateY(-50%);
  width: 2.5px;
  height: 6px;
  background: rgba(45, 45, 45, 0.4);
  border-radius: 0 2px 2px 0;
}

.battery-inner {
  height: 100%;
  background: var(--gn);
  border-radius: 1.5px;
  transition: width 0.3s ease;
}
</style>
