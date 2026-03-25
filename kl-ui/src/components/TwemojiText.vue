<script setup lang="ts">
import { nextTick, onMounted, ref, useAttrs, watch } from 'vue'
import twemoji from 'twemoji'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(defineProps<{
  tag?: string
  text?: string
  html?: string
}>(), {
  tag: 'span',
  text: '',
})

const attrs = useAttrs()
const root = ref<HTMLElement | null>(null)

function renderTwemoji() {
  if (!root.value) return

  if (props.html !== undefined) {
    root.value.innerHTML = props.html
  } else {
    root.value.textContent = props.text
  }

  twemoji.parse(root.value, {
    base: '/emoji/',
    folder: '',
    ext: '.svg',
    className: 'twemoji',
    attributes: () => ({
      draggable: 'false',
      loading: 'lazy',
      decoding: 'async',
    }),
  })
}

watch(
  () => [props.text, props.html],
  async () => {
    await nextTick()
    renderTwemoji()
  },
  { immediate: true }
)

onMounted(renderTwemoji)
</script>

<template>
  <component :is="tag" ref="root" v-bind="attrs"></component>
</template>

<style scoped>
:deep(img.twemoji) {
  width: 1em;
  height: 1em;
  margin: 0 0.08em;
  vertical-align: -0.14em;
  display: inline-block;
}
</style>
