<script setup lang="ts">
import { computed } from 'vue'
import mdit from 'markdown-it'
import AvatarBadge from './AvatarBadge.vue'
import TwemojiText from './TwemojiText.vue'

const props = defineProps<{
  role: 'ai' | 'user'
  content: string
  avatar?: string
}>()

const md = new mdit({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true // 将 \n 解析为 <br>，对非标准 MD 换行更友好
})

const renderedHtml = computed(() => {
  if (props.role === 'user') return props.content
  // 预处理：确保表格前后有空行，防止合并
  const processed = props.content.replace(/([^\n])\n\|/g, '$1\n\n|')
  return md.render(processed)
})
</script>

<template>
  <div class="msg" :class="role">
    <div class="av">
      <AvatarBadge :role="role" />
    </div>
    <TwemojiText
      tag="div"
      class="bubble"
      :class="role"
      :html="role === 'ai' ? renderedHtml : undefined"
      :text="role === 'user' ? content : undefined"
    />
  </div>
</template>

<style scoped>
.msg {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  animation: slideIn 0.3s ease-out;
}

.msg.user {
  flex-direction: row-reverse;
}

.av {
  flex-shrink: 0;
  margin-top: 2px;
}

.bubble {
  max-width: 85%;
  padding: 12px 16px;
  border-radius: 18px;
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
}

.bubble.ai {
  background: #FFFFFF;
  color: #333;
  border-radius: 4px 20px 20px 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  border: 1px solid #F0F2F5;
}

.bubble.user {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
  border-radius: 20px 4px 20px 20px;
  box-shadow: 0 2px 10px rgba(79, 172, 254, 0.3);
}

/* Markdown Styles */
:deep(.bubble.ai table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 13px;
  background: #fdfdfd;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
}

:deep(.bubble.ai th) {
  background: #F8FAFC;
  padding: 8px 10px;
  border: 1px solid #eee;
  font-weight: 600;
  text-align: left;
}

:deep(.bubble.ai td) {
  padding: 8px 10px;
  border: 1px solid #eee;
}

:deep(.bubble.ai p) {
  margin: 8px 0;
}

:deep(.bubble.ai p:first-child) {
  margin-top: 0;
}

:deep(.bubble.ai p:last-child) {
  margin-bottom: 0;
}

:deep(.bubble.ai ul, .bubble.ai ol) {
  padding-left: 20px;
  margin: 8px 0;
}

:deep(.bubble.ai strong) {
  color: #1a1a1a;
  font-weight: 700;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
