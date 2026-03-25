<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useAppStore } from '../stores/appStore'
import TwemojiText from '../components/TwemojiText.vue'
import {
  validateInviteCode,
  submitIdCard,
  activateAccountWithCode,
} from '../services/api'
import { getAgeGroup, type Child } from '../mock/data'

const emit = defineEmits<{
  (e: 'complete', child: Child): void
}>()

const appStore = useAppStore()

// State
type Stage = 'greeting' | 'code' | 'id' | 'name' | 'teacherName' | 'done'
const stage = ref<Stage>('greeting')
const progress = ref(0)
const topSubtitle = ref('AI 学习助手 · 正在和你说话')

// User data
const user = ref({
  code: '',
  age: 0,
  ageGroup: '',
  name: '',
  teacherName: '小星老师',
})

// Messages
interface Message {
  id: number
  role: 'ai' | 'usr' | 'sys'
  content: string
}

const messages = ref<Message[]>([])
let messageId = 1

// Refs
const chatContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
const idInputRef = ref<HTMLInputElement | null>(null)
const codeInputRef = ref<HTMLInputElement | null>(null)
const activatedChild = ref<Child | null>(null)

// Helpers
const sleep = (ms: number) => new Promise(r => setTimeout(r, ms))

function scrollBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

function addMessage(role: 'ai' | 'usr' | 'sys', content: string) {
  messages.value.push({ id: messageId++, role, content })
  scrollBottom()
}

function setProgress(pct: number) {
  progress.value = pct
}

// 邀请码输入
const codeInputValue = ref('')
const isCodeComplete = computed(() => {
  const code = codeInputValue.value.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
  const clean = code.length >= 10 ? code.slice(2, 10) : code.slice(0, 8)
  return clean.length === 8
})

function formatCode() {
  // 去除非字母数字，转大写，最多8位
  let v = codeInputValue.value.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
  // 兼容：有无 KL 前缀都行
  if (v.length >= 10) {
    v = v.slice(2)  // 有 KL 前缀，去掉前两位
  }
  v = v.slice(0, 8)
  // 格式化为 XXXX-XXXX（不加 KL）
  let formatted = ''
  for (let i = 0; i < v.length; i++) {
    if (i === 4) formatted += '-'
    formatted += v[i]
  }
  codeInputValue.value = formatted
}

// Code verification
const codeLoading = ref(false)
const codeError = ref('')

async function verifyCode() {
  const code = codeInputValue.value.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
  const clean = code.length >= 10 ? code.slice(2, 10) : code.slice(0, 8)
  if (clean.length < 8) {
    codeError.value = '请输入完整的邀请码'
    return
  }

  codeLoading.value = true
  codeError.value = ''

  const fullCode = 'KL-' + clean.slice(0, 4) + '-' + clean.slice(4)
  const result = await validateInviteCode(fullCode)

  codeLoading.value = false

  if (!result.valid) {
    codeError.value = result.message
    return
  }

  user.value.code = code
  addMessage('usr', `邀请码：KL-${code.slice(0, 4)}-${code.slice(4)}`)
  await aiSay(['✅ 验证成功！', '接下来告诉我小宝贝的<strong>身份证号</strong>，我来帮您制定专属课程吧 😊'], 700)
  stage.value = 'id'
  setProgress(45)
  renderIdWidget()
}

// ID verification
const idLoading = ref(false)
const idError = ref('')
const idCardValue = ref('')

function renderIdWidget() {
  nextTick(() => {
    idInputRef.value?.focus()
  })
}

async function handleIdSubmit() {
  const id = idCardValue.value.trim()
  if (!/^\d{17}[\dXx]$/.test(id)) {
    idError.value = '请输入正确的18位身份证号'
    return
  }

  idLoading.value = true
  idError.value = ''

  const result = await submitIdCard(id)
  idLoading.value = false

  if (!result.valid || !result.age) {
    idError.value = '身份证号格式不正确'
    return
  }

  user.value.age = result.age
  user.value.ageGroup = result.ageGroup || ''

  addMessage('usr', '身份证号 ✓')

  const ageInfo = getAgeGroup(result.age)
  await aiSay([
    `算出来了 ${ageInfo.emoji}`,
    `<span class="age-pill">${ageInfo.emoji} ${result.age} 岁 · ${ageInfo.label}</span><br><span style="font-size:13px;color:var(--sub)">出题难度已自动匹配，随时可调整</span>`,
  ], 600)

  await sleep(300)
  await aiSay(['接下来～<strong>叫什么名字</strong>？我以后就这么叫你 😊'], 900)
  stage.value = 'name'
  setProgress(78)
  showNameChips()
}

function showNameChips() {
  // Chips are rendered in template
}

// Name
const nameInput = ref('')
const nameLoading = ref(false)

async function handleNameSelect(name: string) {
  if (name === 'custom') {
    nextTick(() => {
      inputRef.value?.focus()
    })
    return
  }

  user.value.name = name
  addMessage('usr', name)
  await aiSay([`<strong>${name}</strong>～好的！叫我什么老师呢？😊`], 600)
  stage.value = 'teacherName'
  setProgress(88)
  nextTick(() => {
    const container = chatContainer.value
    if (container) container.scrollTop = container.scrollHeight
  })
}


async function handleTeacherNameSelect(name: string) {
  nameLoading.value = true
  addMessage('usr', name)
  user.value.teacherName = name
  appStore.setTeacherName(name)  // ← 先保存到 store
  await handleNameSubmit()      // ← 再触发跳转
  nameLoading.value = false
}

async function handleSkipTeacher() {
  user.value.teacherName = '小星老师'
  addMessage('usr', '跳过')
  appStore.setTeacherName('小星老师')  // ← 先保存到 store
  await handleNameSubmit()              // ← 再触发跳转
}

async function handleCustomNameInput() {
  const name = nameInput.value.trim()
  if (!name) return
  user.value.name = name
  nameInput.value = ''
  addMessage('usr', name)
  await aiSay([`<strong>${name}</strong>～好的！叫我什么老师呢？😊`], 600)
  stage.value = 'teacherName'
  setProgress(88)
  nextTick(() => {
    const container = chatContainer.value
    if (container) container.scrollTop = container.scrollHeight
  })
}

async function handleNameSubmit(_name?: string) {
  const name = _name ?? user.value.name
  user.value.name = name
  try {
    activatedChild.value = await activateAccountWithCode(
      `KL-${user.value.code.slice(0, 4)}-${user.value.code.slice(4)}`,
      idCardValue.value.trim(),
      name,
      user.value.teacherName
    )
    setProgress(100)
    topSubtitle.value = `欢迎你，${name}！学习冒险开始啦 🚀`
  } catch (e: any) {
    addMessage('ai', `账号激活失败：${e?.message || '请检查网络后重试'}`)
  }

  await aiSay([`<strong>${name}</strong>，准备好了吗？！`], 700)
  await sleep(200)
  stage.value = 'done'
}

function enterApp() {
  if (activatedChild.value) {
    emit('complete', activatedChild.value)
  }
}

// AI typing effect
const typing = ref(false)

async function aiSay(msgs: string[], baseDelay = 850) {
  typing.value = true
  await sleep(baseDelay)
  typing.value = false

  for (let i = 0; i < msgs.length; i++) {
    addMessage('ai', msgs[i])
    if (i < msgs.length - 1) {
      await sleep(350)
    }
  }
}

// Start
onMounted(async () => {
  document.body.style.background = 'var(--bg)'
  await sleep(500)
  await aiSay([
    `你好！👋 我是<strong>${user.value.teacherName}</strong>，你的专属 AI 学习伙伴！`,
    '只需要和我聊几句，就能开通你的专属学堂账号 🎒',
  ], 800)
  await sleep(300)
  await aiSay(['首先，把你的<strong>邀请码</strong>输入下面的格子吧！'], 1000)
  stage.value = 'code'
  setProgress(20)
  nextTick(() => {
    codeInputRef.value?.focus()
  })
})

onUnmounted(() => {
  document.body.style.background = ''
})
</script>

<template>
  <div class="app">
    <!-- Header -->
    <div class="top">
      <div class="mascot">
        <div class="mascot-ring"></div>
        <svg viewBox="0 0 28 28" fill="none">
          <circle cx="14" cy="13" r="9" fill="white" opacity=".92"/>
          <circle cx="11.5" cy="12" r="1.8" fill="var(--or)"/>
          <circle cx="16.5" cy="12" r="1.8" fill="var(--or)"/>
          <path d="M11 16 Q14 19 17 16" stroke="var(--or)" stroke-width="1.6" stroke-linecap="round" fill="none"/>
          <path d="M9 7 Q14 3.5 19 7" stroke="white" stroke-width="1.6" fill="none" stroke-linecap="round"/>
        </svg>
      </div>
      <div class="top-info">
        <h2>{{ user.teacherName }}</h2>
        <TwemojiText tag="p" :text="topSubtitle" />
      </div>
    </div>

    <!-- Progress -->
    <div class="prog-wrap">
      <div class="prog-track">
        <div class="prog-fill" :style="{ width: progress + '%' }"></div>
      </div>
    </div>

    <!-- Chat Messages -->
    <div class="chat" ref="chatContainer">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="greeting-card"
        :class="msg.role === 'usr' ? 'user-card' : ''"
      >
        <span v-if="msg.role === 'ai'" class="teacher-tag">{{ user.teacherName }}</span>
        <TwemojiText
          tag="div"
          :html="msg.content"
        />
      </div>

      <!-- Typing indicator -->
      <div v-if="typing" class="greeting-card">
        <span class="teacher-tag">{{ user.teacherName }}</span>
        <div class="typing">
          <span></span><span></span><span></span>
        </div>
      </div>

      <!-- Invite Code Widget -->
      <div v-if="stage === 'code'" class="widget">
        <div class="code-card">
          <div class="wlbl">输入邀请码</div>
          <div class="slots">
            <input
              ref="codeInputRef"
              class="code-input"
              :class="{ err: codeError, ok: isCodeComplete }"
              type="text"
              maxlength="13"
              placeholder="XXXX-XXXX"
              v-model="codeInputValue"
              @input="formatCode"
              @keydown.enter="verifyCode"
            />
          </div>
          <div v-if="codeError" class="err-msg">{{ codeError }}</div>
          <button class="abtn" @click="verifyCode" :disabled="codeLoading">
            <div class="spn" :class="{ show: codeLoading }"></div>
            <span :class="{ hide: codeLoading }">验证邀请码 →</span>
          </button>
        </div>
      </div>

      <!-- ID Card Widget -->
      <div v-if="stage === 'id'" class="widget">
        <div class="id-card">
          <div class="wlbl">身份证号</div>
          <div class="id-hint">
            <TwemojiText text="🔒" />
            <span>输入身份证号计算小天才年龄，安排学习计划</span>
          </div>
          <div class="id-row">
            <input
              class="id-inp id-card-input"
              :class="{ err: idError }"
              type="text"
              maxlength="18"
              placeholder="输入 18 位身份证号"
              inputmode="numeric"
              autocomplete="off"
              v-model="idCardValue"
              @keydown.enter="handleIdSubmit"
              ref="idInputRef"
            />
            <TwemojiText class="id-ico" text="🪪" />
          </div>
          <div class="id-priv">服务器只用来计算「年龄段」，<b>绝不泄漏</b></div>
          <div v-if="idError" class="err-msg">{{ idError }}</div>
          <button class="abtn" @click="handleIdSubmit" :disabled="idLoading">
            <div class="spn" :class="{ show: idLoading }"></div>
            <span :class="{ hide: idLoading }">确认，自动计算年龄 →</span>
          </button>
        </div>
      </div>

      <!-- Name Chips -->
      <div v-if="stage === 'name'" class="chips">
        <div class="chip" @click="handleNameSelect('小明')">小明</div>
        <div class="chip" @click="handleNameSelect('萌萌')">萌萌</div>
        <div class="chip" @click="handleNameSelect('豆豆')">豆豆</div>
        <div class="chip" @click="handleNameSelect('乐乐')">乐乐</div>
        <div class="chip" @click="handleNameSelect('custom')">自己说</div>
      </div>

      <!-- Teacher Name Chips -->
      <div v-if="stage === 'teacherName'" class="chips">
        <div class="chip" @click="handleTeacherNameSelect('小星老师')">小星老师</div>
        <div class="chip" @click="handleTeacherNameSelect('星星老师')">星星老师</div>
        <div class="chip" @click="handleTeacherNameSelect('小芽老师')">小芽老师</div>
        <div class="chip" @click="handleTeacherNameSelect('贝贝老师')">贝贝老师</div>
        <div class="chip" @click="handleTeacherNameSelect('朵朵老师')">朵朵老师</div>
        <div class="chip skip-chip" @click="handleSkipTeacher">跳过</div>
      </div>

      <!-- Comic Card (Done) -->
      <div v-if="stage === 'done'" class="widget">
        <div class="comic-card">
          <div class="confetti-wrap">
            <div v-for="i in 30" :key="i" class="cp" :class="i % 2 === 0 ? '' : 'b'" :style="{
              left: Math.random() * 100 + '%',
              top: '-15px',
              width: (5 + Math.random() * 9) + 'px',
              height: (5 + Math.random() * 9) + 'px',
              background: ['#FF7043', '#FFD54F', '#29B6F6', '#66BB6A', '#AB47BC', '#F06292', '#FF8A65'][i % 7],
              borderRadius: Math.random() > 0.5 ? '50%' : '2px',
            }"></div>
          </div>

          <div class="comic-band">
            <TwemojiText tag="div" class="zap z1" text="⚡" />
            <TwemojiText tag="div" class="zap z2" text="✨" />
            <TwemojiText tag="div" class="zap z3" text="💥" />
            <div class="orbit-wrap">
              <TwemojiText tag="div" class="orbit-mascot" text="🌟" />
              <TwemojiText tag="div" class="orbit-star" style="animation:starOrbit 1.8s linear infinite" text="⭐" />
              <TwemojiText tag="div" class="orbit-star" style="animation:starOrbit2 1.8s linear infinite" text="🌙" />
              <TwemojiText tag="div" class="orbit-star" style="animation:starOrbit3 1.8s linear infinite" text="💫" />
            </div>
          </div>

          <div class="comic-body">
            <div class="comic-title-wrap">
              <TwemojiText tag="div" class="comic-title" :text="`欢迎加入！${user.name} 🎉`" />
              <div class="comic-sub" style="margin-top:4px">星芽学堂已为你准备好一切，出发吧！</div>
            </div>

            <div class="stat-row" style="margin-top:14px">
              <div class="stat-chip c-or">
                <TwemojiText tag="div" class="sv" :text="`${getAgeGroup(user.age).emoji} ${user.age}岁`" />
                <div class="sl">{{ user.ageGroup }}</div>
              </div>
              <div class="stat-chip c-gn">
                <TwemojiText tag="div" class="sv" text="0 ⭐" />
                <div class="sl">初始积分</div>
              </div>
              <div class="stat-chip c-bl">
                <TwemojiText tag="div" class="sv" text="📖🔢" />
                <div class="sl">英语+数学</div>
              </div>
            </div>

            <button class="enter-btn" @click="enterApp">
              <TwemojiText text="进入学堂，开始冒险！🚀" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Input Bar -->
    <div class="ibar">
      <div class="irow">
        <input
          ref="inputRef"
          type="text"
          :placeholder="stage === 'teacherName' ? '输入老师名称…' : stage === 'name' ? '输入小朋友的名字…' : `和${user.teacherName}说话…`"
          v-model="nameInput"
          @keydown.enter="stage === 'name' ? handleCustomNameInput() : stage === 'teacherName' ? handleTeacherNameSelect(nameInput) : null"
        />
        <button class="send" @click="stage === 'name' ? handleCustomNameInput() : stage === 'teacherName' ? handleTeacherNameSelect(nameInput) : null">
          <svg width="13" height="13" viewBox="0 0 15 15" fill="white">
            <path d="M1 7.5L14 1L9.5 14L7 8.5L1 7.5Z"/>
          </svg>
        </button>
      </div>
      <button class="mic">
        <svg width="14" height="18" viewBox="0 0 14 18" fill="none">
          <rect x="3" y="0" width="8" height="12" rx="4" fill="var(--bl)"/>
          <path d="M1 10c0 3.3 2.7 6 6 6s6-2.7 6-6" stroke="var(--bl)" stroke-width="1.8" stroke-linecap="round" fill="none"/>
          <line x1="7" y1="16" x2="7" y2="18" stroke="var(--bl)" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100vh;
  background: var(--bg);
}

.top {
  padding: 10px 20px 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.mascot {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--or), var(--yw));
  display: flex;
  align-items: center;
  justify-content: center;
  animation: float 3s ease-in-out infinite;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(255, 112, 67, 0.35);
  position: relative;
}

.mascot-ring {
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 2px dashed rgba(255, 112, 67, 0.25);
  animation: ringRotate 10s linear infinite;
}

.mascot svg {
  width: 28px;
  height: 28px;
}

.top-info h2 {
  font-size: 17px;
  font-weight: bold;
  color: var(--txt);
}

.top-info p {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}

.prog-wrap {
  padding: 0 20px 10px;
  flex-shrink: 0;
}

.prog-track {
  height: 4px;
  border-radius: 2px;
  background: var(--border);
  overflow: hidden;
}

.prog-fill {
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--or), var(--yw));
  transition: width 0.7s cubic-bezier(0.4, 0, 0.2, 1);
}

.chat {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  scrollbar-width: none;
}

.chat::-webkit-scrollbar {
  display: none;
}

/* 老师引导卡片 */
.greeting-card {
  background: var(--card);
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--txt);
  line-height: 1.55;
  align-self: flex-start;
  display: flex;
  flex-direction: column;
  gap: 3px;
  animation: msgIn 0.3s ease;
  max-width: 280px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.teacher-tag {
  font-size: 11px;
  font-weight: 700;
  color: var(--or);
}

/* 用户消息卡片 */
.user-card {
  background: #FFF0EB;
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--txt);
  line-height: 1.55;
  align-self: flex-end;
  animation: msgIn 0.3s ease;
  max-width: 280px;
}

/* 打字指示器 */
.typing {
  display: flex;
  align-items: center;
  gap: 5px;
}

.typing span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--muted);
  animation: dotBounce 1.1s infinite ease-in-out;
}

.typing span:nth-child(2) { animation-delay: 0.15s; }
.typing span:nth-child(3) { animation-delay: 0.3s; }

.widget {
  animation: msgIn 0.32s ease;
  width: 100%;
  align-self: flex-start;
}

.code-card {
  background: var(--card);
  border-radius: 20px;
  padding: 16px 18px;
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.wlbl {
  font-size: 11px;
  font-weight: bold;
  color: var(--muted);
  letter-spacing: 0.6px;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.slots {
  margin-top: 4px;
}

.code-input {
  width: 100%;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1.5px solid var(--border);
  background: var(--bg);
  font-size: 16px;
  font-weight: 600;
  font-family: inherit;
  color: var(--txt);
  text-align: center;
  letter-spacing: 3px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.code-input::placeholder {
  color: var(--muted);
  letter-spacing: 1px;
}

.code-input:focus {
  border-color: var(--or);
  background: var(--or-l);
  box-shadow: 0 0 0 3px rgba(255, 112, 67, 0.12);
}

.code-input.ok {
  border-color: rgba(255, 112, 67, 0.4);
  background: var(--or-l);
  color: var(--or-d);
}

.code-input.err {
  border-color: #ef5350;
  background: #ffebee;
}

.err-msg {
  font-size: 12px;
  color: #EF5350;
  text-align: center;
  margin-top: 8px;
}

.abtn {
  width: 100%;
  height: 46px;
  border-radius: 28px;
  background: var(--card);
  border: 1.5px solid var(--or);
  color: var(--or);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s;
  margin-top: 12px;
}

.abtn:active {
  background: var(--or-l);
}

.abtn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spn {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid rgba(255, 112, 67, 0.3);
  border-top-color: var(--or);
  animation: spin 0.7s linear infinite;
  display: none;
}

.spn.show {
  display: block;
}

.abtn .hide {
  display: none;
}

.age-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--gn-l);
  border: 2px solid var(--gn);
  border-radius: 12px;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: bold;
  color: var(--gn-d);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  padding: 2px 0;
}

.chip {
  padding: 9px 16px;
  border-radius: 22px;
  background: var(--card);
  border: 2px solid var(--border);
  font-size: 14px;
  font-weight: 600;
  color: var(--txt);
  cursor: pointer;
  transition: all 0.18s;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  animation: slideChip 0.25s ease both;
}

.chip:nth-child(1) { animation-delay: 0s; }
.chip:nth-child(2) { animation-delay: 0.06s; }
.chip:nth-child(3) { animation-delay: 0.12s; }
.chip:nth-child(4) { animation-delay: 0.18s; }
.chip:nth-child(5) { animation-delay: 0.24s; }
.chip:nth-child(6) { animation-delay: 0.30s; }

.chip:active {
  border-color: var(--or);
  background: var(--or-l);
  transform: scale(0.95);
}

.skip-chip {
  background: transparent;
  border-color: var(--muted);
  color: var(--muted);
}

.id-card {
  background: var(--card);
  border-radius: 20px;
  padding: 16px 18px;
  border: 1.5px solid var(--border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.id-hint {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 10px;
  display: flex;
  align-items: flex-start;
  gap: 5px;
  line-height: 1.5;
}

.id-row {
  position: relative;
}

.id-inp {
  width: 100%;
  height: 52px;
  border-radius: 14px;
  border: 2px solid var(--border);
  background: var(--bg);
  padding: 0 46px 0 16px;
  font-size: 17px;
  font-weight: 600;
  color: var(--txt);
  outline: none;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
  transition: all 0.2s;
}

.id-inp::placeholder {
  font-family: inherit;
  letter-spacing: 0;
  font-size: 14px;
  font-weight: 400;
  color: var(--muted);
}

.id-inp:focus {
  border-color: var(--or);
  background: var(--or-l);
  box-shadow: 0 0 0 3px rgba(255, 112, 67, 0.12);
}

.id-inp.err {
  border-color: #EF5350;
  background: var(--or-l);
}

.id-ico {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  pointer-events: none;
}

.id-priv {
  font-size: 11px;
  color: var(--muted);
  margin-top: 8px;
  line-height: 1.5;
  text-align: center;
}

.id-priv b {
  color: var(--or);
}

/* Comic Card */
.comic-card {
  background: var(--card);
  border-radius: 24px;
  border: 3px solid var(--yw);
  box-shadow: 0 0 0 6px rgba(255, 213, 79, 0.15), 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  animation: comicPop 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  position: relative;
}

.comic-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255, 213, 79, 0.18) 1.5px, transparent 1.5px);
  background-size: 12px 12px;
  pointer-events: none;
}

.comic-band {
  background: linear-gradient(135deg, var(--yw), var(--or));
  padding: 18px 20px 16px;
  position: relative;
  overflow: hidden;
}

.comic-band::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 8px;
  background: var(--card);
  border-radius: 100% 100% 0 0;
}

.orbit-wrap {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 6px;
  flex-shrink: 0;
}

.orbit-mascot {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--or), var(--yw));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  box-shadow: 0 4px 20px rgba(255, 112, 67, 0.45);
  animation: bounce 1.4s ease-in-out infinite;
}

.orbit-star {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 18px;
  height: 18px;
  margin: -9px;
  font-size: 16px;
  animation: starOrbit 2s linear infinite;
  transform-origin: 0 0;
}

.orbit-star:nth-child(2) { animation-name: starOrbit2; }
.orbit-star:nth-child(3) { animation-name: starOrbit3; }

.zap {
  position: absolute;
  font-size: 22px;
  animation: zap 1s ease-in-out infinite;
}

.zap.z1 { top: 8px; right: 24px; }
.zap.z2 { top: 18px; left: 20px; animation-delay: 0.35s; font-size: 16px; }
.zap.z3 { bottom: 22px; right: 16px; animation-delay: 0.7s; font-size: 14px; }

.comic-title-wrap {
  text-align: center;
  margin: 6px 0 4px;
}

.comic-title {
  font-size: 26px;
  font-weight: 900;
  color: var(--txt);
  letter-spacing: -1px;
  line-height: 1.1;
  text-shadow: 3px 3px 0 rgba(255, 213, 79, 0.6);
}

.comic-sub {
  font-size: 13px;
  color: var(--sub);
  font-weight: 500;
  text-align: center;
  margin-bottom: 0;
}

.comic-body {
  padding: 14px 18px 18px;
}

.stat-row {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.stat-chip {
  flex: 1;
  border-radius: 14px;
  padding: 10px 8px;
  text-align: center;
  border: 2px solid transparent;
}

.stat-chip.c-or {
  background: var(--or-l);
  border-color: rgba(255, 112, 67, 0.3);
}

.stat-chip.c-gn {
  background: var(--gn-l);
  border-color: rgba(102, 187, 106, 0.3);
}

.stat-chip.c-bl {
  background: var(--bl-l);
  border-color: rgba(41, 182, 246, 0.3);
}

.stat-chip .sv {
  font-size: 18px;
  font-weight: 900;
  color: var(--txt);
  line-height: 1;
}

.stat-chip .sl {
  font-size: 10px;
  color: var(--muted);
  margin-top: 3px;
  font-weight: 600;
}

.confetti-wrap {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  border-radius: 22px;
}

.cp {
  position: absolute;
  animation: confettiA 1.5s ease-in infinite;
}

.cp.b {
  animation-name: confettiB;
}

.enter-btn {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--or), var(--yw));
  border: none;
  color: var(--card);
  font-size: 17px;
  font-weight: 900;
  cursor: pointer;
  letter-spacing: -0.2px;
  box-shadow: 0 5px 20px rgba(255, 112, 67, 0.5);
  animation: enterBtn 0.5s 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: transform 0.15s;
}

.enter-btn:active {
  transform: scale(0.97);
}

.ibar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px 24px;
  flex-shrink: 0;
}

.irow {
  flex: 1;
  position: relative;
  background: var(--card);
  border-radius: 28px;
  border: 1.5px solid var(--border);
  display: flex;
  align-items: center;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.irow:focus-within {
  border-color: var(--or);
  box-shadow: 0 2px 12px rgba(255, 112, 67, 0.15);
}

.irow input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 15px;
  color: var(--txt);
  outline: none;
  font-family: inherit;
  padding: 11px 44px 11px 18px;
}

.irow input::placeholder {
  color: var(--muted);
}

.send {
  position: absolute;
  right: 5px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--or), #FF8A65);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(255, 112, 67, 0.35);
  transition: all 0.2s;
}

.send:active {
  transform: scale(0.9);
}

.mic {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--bl-l);
  border: 2px solid rgba(41, 182, 246, 0.2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.mic:active {
  transform: scale(0.92);
}

/* Animations */
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-7px); }
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(12px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes dotBounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-6px); }
}

@keyframes ringRotate {
  to { transform: rotate(360deg); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes slideChip {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes comicPop {
  0% { transform: scale(0) rotate(-10deg); opacity: 0; }
  55% { transform: scale(1.18) rotate(3deg); opacity: 1; }
  80% { transform: scale(0.96) rotate(-1deg); }
  100% { transform: scale(1) rotate(0); opacity: 1; }
}

@keyframes starOrbit {
  0% { transform: rotate(0deg) translateX(52px) rotate(0deg); }
  100% { transform: rotate(360deg) translateX(52px) rotate(-360deg); }
}

@keyframes starOrbit2 {
  0% { transform: rotate(120deg) translateX(52px) rotate(-120deg); }
  100% { transform: rotate(480deg) translateX(52px) rotate(-480deg); }
}

@keyframes starOrbit3 {
  0% { transform: rotate(240deg) translateX(52px) rotate(-240deg); }
  100% { transform: rotate(600deg) translateX(52px) rotate(-600deg); }
}

@keyframes confettiA {
  0% { transform: translateY(0) rotate(0deg); opacity: 1; }
  100% { transform: translateY(120px) rotate(540deg); opacity: 0; }
}

@keyframes confettiB {
  0% { transform: translateY(0) rotate(0deg) scaleX(1); opacity: 1; }
  100% { transform: translateY(100px) rotate(-420deg) scaleX(-1); opacity: 0; }
}

@keyframes zap {
  0%, 100% { opacity: 0; transform: scale(0.5); }
  50% { opacity: 1; transform: scale(1); }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0) scale(1); }
  30% { transform: translateY(-10px) scale(1.08); }
  60% { transform: translateY(-4px) scale(0.97); }
}

@keyframes enterBtn {
  from { opacity: 0; transform: scale(0.8) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
</style>
