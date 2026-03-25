<script setup lang="ts">
withDefaults(defineProps<{
  role: 'ai' | 'user'
  mood?: 'default' | 'happy' | 'thinking'
}>(), {
  mood: 'default',
})
</script>

<template>
  <div class="avatar-badge" :class="[role, mood]">
    <!-- 用户头像：兔子 -->
    <div v-if="role === 'user'" class="user-face">
      <svg viewBox="0 0 36 36" aria-hidden="true">
        <defs>
          <linearGradient id="userBg" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#B3E5FC" />
            <stop offset="100%" stop-color="#C8E6C9" />
          </linearGradient>
        </defs>
        <circle cx="18" cy="18" r="18" fill="url(#userBg)" />
        <!-- 兔子耳朵 -->
        <ellipse cx="11" cy="8" rx="4" ry="7" fill="#FFCCBC" stroke="#FFAB91" stroke-width="1"/>
        <ellipse cx="25" cy="8" rx="4" ry="7" fill="#FFCCBC" stroke="#FFAB91" stroke-width="1"/>
        <ellipse cx="11" cy="8" rx="2" ry="4" fill="#FFAB91"/>
        <ellipse cx="25" cy="8" rx="2" ry="4" fill="#FFAB91"/>
        <!-- 脸 -->
        <circle cx="18" cy="20" r="10" fill="#FFCCBC"/>
        <!-- 腮红 -->
        <circle cx="11" cy="22" r="3" fill="#FFAB91" opacity="0.5"/>
        <circle cx="25" cy="22" r="3" fill="#FFAB91" opacity="0.5"/>
        <!-- 眼睛 -->
        <circle cx="14" cy="18" r="2" fill="#5D4037"/>
        <circle cx="22" cy="18" r="2" fill="#5D4037"/>
        <circle cx="14.5" cy="17.5" r="0.8" fill="white"/>
        <circle cx="22.5" cy="17.5" r="0.8" fill="white"/>
        <!-- 鼻子 -->
        <ellipse cx="18" cy="21" rx="1.5" ry="1" fill="#FFAB91"/>
        <!-- 嘴巴 -->
        <path v-if="mood === 'happy'" d="M15 23 Q18 26 21 23" stroke="#5D4037" stroke-width="1.2" stroke-linecap="round" fill="none"/>
        <path v-else d="M15 24 Q18 25 21 24" stroke="#5D4037" stroke-width="1.2" stroke-linecap="round" fill="none"/>
      </svg>
    </div>

    <!-- 小星老师：戴帽子的卡通形象 -->
    <svg v-else viewBox="0 0 36 36" aria-hidden="true" class="teacher-svg">
      <defs>
        <linearGradient id="teacherBg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#FFB74D" />
          <stop offset="100%" stop-color="#FF7043" />
        </linearGradient>
      </defs>

      <!-- 背景圆 -->
      <circle cx="18" cy="18" r="18" fill="url(#teacherBg)" />

      <!-- 帽子 -->
      <rect x="6" y="6" width="24" height="8" rx="4" fill="#FF7043"/>
      <path d="M4 14 L18 6 L32 14 Z" fill="#FF8A65"/>
      <!-- 帽子星星 -->
      <path v-if="mood === 'happy'" d="M18 7L19.2 9.8L22 9.8L19.8 11.5L20.8 14.2L18 12.5L15.2 14.2L16.2 11.5L14 9.8L16.8 9.8Z" fill="#FFD54F" class="star-pop"/>
      <path v-else d="M18 8L18.8 10L21 10L19.2 11.2L20 13L18 11.8L16 13L16.8 11.2L15 10L17.2 10Z" fill="#FFD54F" opacity="0.8"/>

      <!-- 脸部 -->
      <circle cx="18" cy="20" r="9" fill="#FFCCBC"/>

      <!-- 眼睛：默认/开心 -->
      <template v-if="mood !== 'thinking'">
        <circle cx="14" cy="19" r="2.2" fill="white"/>
        <circle cx="22" cy="19" r="2.2" fill="white"/>
        <circle cx="14" cy="19" r="1.2" fill="#5D4037"/>
        <circle cx="22" cy="19" r="1.2" fill="#5D4037"/>
        <circle cx="14.4" cy="18.6" r="0.5" fill="white"/>
        <circle cx="22.4" cy="18.6" r="0.5" fill="white"/>
        <!-- 笑眼 -->
        <path v-if="mood === 'happy'" d="M11.5 18 Q14 16 16.5 18" stroke="#FF7043" stroke-width="0.8" fill="none"/>
        <path v-if="mood === 'happy'" d="M19.5 18 Q22 16 24.5 18" stroke="#FF7043" stroke-width="0.8" fill="none"/>
      </template>

      <!-- 眼睛：思考中 -->
      <template v-else>
        <ellipse cx="14" cy="19" rx="2.2" ry="2" fill="white"/>
        <ellipse cx="22" cy="19" rx="2.2" ry="2" fill="white"/>
        <circle cx="14" cy="19" r="1" fill="#5D4037"/>
        <circle cx="22" cy="19" r="1" fill="#5D4037"/>
        <!-- 思考线条 -->
        <circle cx="26" cy="13" r="2" fill="rgba(255,255,255,0.6)" class="thought"/>
        <circle cx="28" cy="10" r="1.5" fill="rgba(255,255,255,0.4)" class="thought"/>
      </template>

      <!-- 嘴巴 -->
      <path v-if="mood === 'happy'" d="M14 24 Q18 28 22 24" stroke="#FF7043" stroke-width="1.5" stroke-linecap="round" fill="none"/>
      <path v-else-if="mood === 'thinking'" d="M15 24 Q18 25 21 24" stroke="#FF7043" stroke-width="1.2" stroke-linecap="round" fill="none"/>
      <path v-else d="M14.5 24 Q18 26 21.5 24" stroke="#FF7043" stroke-width="1.5" stroke-linecap="round" fill="none"/>

      <!-- 腮红 -->
      <circle cx="10" cy="22" r="2" fill="#FFAB91" opacity="0.4"/>
      <circle cx="26" cy="22" r="2" fill="#FFAB91" opacity="0.4"/>
    </svg>
  </div>
</template>

<style scoped>
.avatar-badge {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.14);
  transition: transform 0.3s ease;
}

.avatar-badge svg {
  width: 100%;
  height: 100%;
  display: block;
}

.avatar-badge.user {
  box-shadow: 0 3px 10px rgba(79, 195, 247, 0.28);
}

.avatar-badge.ai {
  box-shadow: 0 3px 10px rgba(255, 112, 67, 0.3);
}

.avatar-badge.happy {
  animation: bounce 0.5s ease;
}

.avatar-badge.thinking {
  animation: wobble 2s ease-in-out infinite;
}

.user-face {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-face svg {
  width: 100%;
  height: 100%;
}

.star-pop {
  animation: starPop 0.6s ease;
}

.thought {
  animation: thoughtPop 1.5s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: scale(1); }
  30% { transform: scale(1.15); }
  60% { transform: scale(0.95); }
}

@keyframes wobble {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-3deg); }
  75% { transform: rotate(3deg); }
}

@keyframes starPop {
  0% { transform: scale(0) rotate(-20deg); opacity: 0; }
  60% { transform: scale(1.3) rotate(5deg); opacity: 1; }
  100% { transform: scale(1) rotate(0deg); opacity: 1; }
}

@keyframes thoughtPop {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.15); opacity: 1; }
}
</style>
