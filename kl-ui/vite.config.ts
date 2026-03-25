import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',  // 监听所有网络接口，允许真机访问
    port: 5173,
  },
  test: {
    environment: 'jsdom',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: [
        'src/mock/data.ts',
        'src/composables/useAppState.ts',
        'src/services/api.ts',
        'src/components/QuizCard.vue',
      ],
      exclude: [
        'src/main.ts',
        'src/vite-env.d.ts',
      ],
      thresholds: {
        lines: 80,
        functions: 80,
        statements: 80,
        branches: 75,
      },
    },
  },
})
