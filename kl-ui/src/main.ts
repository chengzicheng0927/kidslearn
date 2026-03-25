import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import './styles/variables.css'
import { setupNativeCallbacks } from './utils/nativeBridge'
import { ApiException } from './services/api'

setupNativeCallbacks()

const pinia = createPinia()
const app = createApp(App)

app.use(pinia)

// 全局 API 异常静默处理（HTTP 错误已在 request 层 toast，业务层无需关心）
app.config.errorHandler = (err, _instance, info) => {
  if (err instanceof ApiException) return
  console.error('[Vue Error]', err, info)
}

app.mount('#app')
