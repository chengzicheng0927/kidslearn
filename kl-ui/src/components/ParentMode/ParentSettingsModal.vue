<script setup lang="ts">
import { ref, onMounted } from 'vue'
import TwemojiText from '../TwemojiText.vue'
import { getParentSettings, updateParentSettings, type ParentSettings } from '../../services/api'
import { useAppStore } from '../../stores/appStore'

const emit = defineEmits<{
  (e: 'close'): void
}>()

const appStore = useAppStore()

const settings = ref<ParentSettings | null>(null)
const loading = ref(true)
const saving = ref(false)
const saved = ref(false)

onMounted(async () => {
  settings.value = await getParentSettings()
  loading.value = false
})

async function save(key: keyof ParentSettings | 'eyeCareReminder' | 'breakReminder', value: unknown) {
  if (!settings.value || saving.value) return
  saving.value = true
  const updated = await updateParentSettings({ [key]: value })
  settings.value = updated
  saving.value = false
  showSaved()
}

function showSaved() {
  saved.value = true
  setTimeout(() => { saved.value = false }, 2000)
}

function setDailyLimit(minutes: number) {
  save('dailyLimitMinutes', minutes)
}

function toggleDnd(enabled: boolean) {
  save('dndEnabled', enabled)
}

function setDndStart(time: string) {
  save('dndStart', time)
}

function setDndEnd(time: string) {
  save('dndEnd', time)
}

function toggleEyeCare(enabled: boolean) {
  save('eyeCareReminder', enabled)
}

function toggleBreakReminder(enabled: boolean) {
  save('breakReminder', enabled)
}

function updateTeacherName(name: string) {
  save('teacherName', name)
  appStore.setTeacherName(name)
}
</script>

<template>
  <Teleport to="body">
    <div class="overlay" @click.self="emit('close')">
      <div class="sheet">
        <div class="sheet-header">
          <button class="close-btn" @click="emit('close')">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M5 5L15 15M15 5L5 15" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <div class="sheet-title">
            <TwemojiText html="⚙️" />
            家长设置
          </div>
          <div class="saved-badge" :class="{ visible: saved }">
            <TwemojiText html="✅" />
            已保存
          </div>
        </div>

        <div v-if="loading" class="loading">
          <div class="loading-spinner"></div>
        </div>

        <div v-else-if="settings" class="settings-body">
          <!-- 老师名称 -->
          <div class="section">
            <div class="section-title">
              <TwemojiText html="👩‍🏫" />
              老师名称
            </div>
            <div class="section-sub">设置学习界面中显示的老师名称</div>
            <div class="input-row">
              <input
                class="text-input"
                type="text"
                :value="settings.teacherName"
                placeholder="请输入老师名称"
                maxlength="10"
                @change="updateTeacherName(($event.target as HTMLInputElement).value)"
              />
            </div>
          </div>

          <!-- 每日学习时长 -->
          <div class="section">
            <div class="section-title">
              <TwemojiText html="⏰" />
              每日学习时长
            </div>
            <div class="section-sub">超过时长后将暂停学习</div>
            <div class="limit-options">
              <button
                v-for="opt in [
                  { value: 15, label: '15分钟' },
                  { value: 30, label: '30分钟' },
                  { value: 60, label: '60分钟' },
                  { value: 120, label: '120分钟' },
                ]"
                :key="opt.value"
                class="limit-btn"
                :class="{ active: settings.dailyLimitMinutes === opt.value }"
                @click="setDailyLimit(opt.value)"
              >
                {{ opt.label }}
              </button>
            </div>
          </div>

          <!-- 勿扰模式 -->
          <div class="section">
            <div class="section-title">
              <TwemojiText html="🌙" />
              勿扰模式
            </div>
            <div class="section-sub">开启后，在指定时间段内无法使用 App</div>
            <div class="toggle-row">
              <span class="toggle-label">开启勿扰</span>
              <button
                class="toggle-btn"
                :class="{ on: settings.dndEnabled }"
                @click="toggleDnd(!settings.dndEnabled)"
              >
                <span class="toggle-knob"></span>
              </button>
            </div>
            <div v-if="settings.dndEnabled" class="time-range">
              <div class="time-input">
                <span class="time-label">开始</span>
                <input
                  type="time"
                  :value="settings.dndStart"
                  @change="setDndStart(($event.target as HTMLInputElement).value)"
                />
              </div>
              <span class="time-sep">至</span>
              <div class="time-input">
                <span class="time-label">结束</span>
                <input
                  type="time"
                  :value="settings.dndEnd"
                  @change="setDndEnd(($event.target as HTMLInputElement).value)"
                />
              </div>
            </div>
          </div>

          <!-- 健康提醒 -->
          <div class="section">
            <div class="section-title">
              <TwemojiText html="👀" />
              健康学习提醒
            </div>

            <div class="toggle-row">
              <div class="toggle-info">
                <span class="toggle-label">护眼提醒</span>
                <span class="toggle-desc">答完 10 题后提示休息眼睛</span>
              </div>
              <button
                class="toggle-btn"
                :class="{ on: settings.eyeCareReminderEnabled }"
                @click="toggleEyeCare(!settings.eyeCareReminderEnabled)"
              >
                <span class="toggle-knob"></span>
              </button>
            </div>

            <div class="toggle-row">
              <div class="toggle-info">
                <span class="toggle-label">休息提醒</span>
                <span class="toggle-desc">连续学习 15 分钟提醒休息</span>
              </div>
              <button
                class="toggle-btn"
                :class="{ on: settings.breakReminderEnabled }"
                @click="toggleBreakReminder(!settings.breakReminderEnabled)"
              >
                <span class="toggle-knob"></span>
              </button>
            </div>
          </div>

          <!-- 底部提示 -->
          <div class="footer-tip">
            <TwemojiText html="💡" />
            以上设置由家长管理，学习数据将同步到云端
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: var(--z-settings);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(20, 25, 39, 0.5);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease;
}

.sheet {
  width: 100%;
  max-height: 90vh;
  background: #fff;
  border-radius: 8px 8px 0 0;
  overflow-y: auto;
  transform: translateY(0);
  will-change: transform;
}

.sheet-header {
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 1;
  display: flex;
  align-items: center;
  padding: 18px 20px 12px;
  gap: 10px;
  border-bottom: 1px solid #F0F0F0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #F5F5F5;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
  flex-shrink: 0;
}

.sheet-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 18px;
  font-weight: 900;
  color: #273238;
  letter-spacing: -0.3px;
  flex: 1;
}

.saved-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  color: #43a047;
  background: #e9f7ea;
  padding: 4px 10px;
  border-radius: 999px;
  opacity: 0;
  transition: opacity 0.3s;
}

.saved-badge.visible {
  opacity: 1;
}

.loading {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #F0F0F0;
  border-top-color: #7c6df8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.settings-body {
  padding: 8px 20px 32px;
}

.section {
  padding: 18px 0;
  border-bottom: 1px solid #F5F5F5;
}

.section:last-of-type {
  border-bottom: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 800;
  color: #273238;
  margin-bottom: 4px;
}

.section-sub {
  font-size: 12px;
  color: #9e9e9e;
  margin-bottom: 12px;
  padding-left: 26px;
}

.input-row {
  margin-top: 12px;
}

.text-input {
  width: 100%;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1.5px solid #EEDFD6;
  background: #fff;
  font-size: 15px;
  font-family: inherit;
  color: #273238;
  font-weight: 600;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.text-input:focus {
  outline: none;
  border-color: #7c6df8;
}

.text-input::placeholder {
  color: #B0B8C4;
  font-weight: 400;
}

.limit-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.limit-btn {
  padding: 10px 6px;
  border-radius: 12px;
  border: 2px solid #EEDFD6;
  background: transparent;
  color: #666;
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.limit-btn.active {
  border-color: #7c6df8;
  background: #7c6df8;
  color: white;
  box-shadow: 0 3px 10px rgba(124, 109, 248, 0.3);
}

.limit-btn:active {
  transform: scale(0.95);
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.toggle-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toggle-label {
  font-size: 14px;
  font-weight: 700;
  color: #273238;
}

.toggle-desc {
  font-size: 11px;
  color: #9e9e9e;
}

.toggle-btn {
  width: 48px;
  height: 28px;
  border-radius: 14px;
  border: none;
  background: #E0E0E0;
  cursor: pointer;
  position: relative;
  transition: background 0.25s;
  flex-shrink: 0;
}

.toggle-btn.on {
  background: #43a047;
}

.toggle-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 4px rgba(0,0,0,0.2);
  transition: transform 0.25s;
}

.toggle-btn.on .toggle-knob {
  transform: translateX(20px);
}

.time-range {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding: 12px;
  background: #FFF8F5;
  border-radius: 14px;
}

.time-input {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.time-label {
  font-size: 11px;
  font-weight: 600;
  color: #9e9e9e;
}

.time-input input {
  padding: 8px 10px;
  border-radius: 10px;
  border: 1.5px solid #EEDFD6;
  background: #fff;
  font-size: 14px;
  font-family: inherit;
  color: #273238;
  font-weight: 600;
  text-align: center;
}

.time-sep {
  font-size: 12px;
  color: #9e9e9e;
  padding-top: 20px;
}

.footer-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9e9e9e;
  padding: 16px 0 0;
  text-align: center;
  justify-content: center;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
