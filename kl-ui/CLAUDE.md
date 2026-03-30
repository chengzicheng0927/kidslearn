# Vue 3 前端开发规约

> iOS WebView 嵌套 H5，使用 Vue 3 + TypeScript 构建。

---

## 1. 严谨开发原则

> **前端只做展示，数据由后端返回。禁止在前端实现任何业务逻辑计算。**

### 前端职责边界

| 该做的事 | 禁止做的事 |
|---------|-----------|
| 调用 API 提交用户操作 | 计算答题对错 |
| 渲染后端返回的数据 | 计算星星/XP/连击 |
| 管理 UI 状态（加载中、动画） | 判断宠物进化条件 |
| 处理用户交互事件 | 计算徽章解锁 |
| 请求后端获取业务状态 | 任何业务判断逻辑 |

### 典型错误示例

```typescript
// ❌ 错误：前端自己算奖励
const stars = isCorrect ? 2 : 0
const xp = stars * 10

// ✅ 正确：使用后端返回的数据
const stars = resultData.stars
const xp = resultData.xp
```

```typescript
// ❌ 错误：前端判断是否完成
if (answeredCount >= 10) showComplete()

// ✅ 正确：使用后端返回的完成状态
if (resultData.hasMore === false) showComplete()
```

### 场景职责表

| 场景 | 前端职责 | 后端职责 |
|------|---------|---------|
| 用户答题 | 提交答案，渲染结果 | 判定对错、计算奖励、更新进度 |
| 答题结果弹窗 | 展示后端返回的 stars/xp/combo | 累计积分、触发徽章、计算连击 |
| 宠物进化 | 展示后端返回的进化状态 | 判断进化条件、更新进化阶段 |
| 知识星球徽章 | 展示后端返回的徽章列表和进度 | 计算答题数、判定解锁、解锁时间 |
| 每日任务 | 展示后端返回的目标和进度 | 设置目标、判定完成、发放奖励 |
| 日历打卡 | 展示后端返回的打卡数据 | 记录学习数据、生成热力图 |
| 成就系统 | 展示后端返回的成就列表 | 判定成就条件、记录解锁时间 |
| 家长控制 | 展示后端返回的配置项 | 存储设置、执行限制逻辑 |

**例外**：UI 层面的纯展示状态（如动画触发、加载中）可使用前端 state。

---

## 2. 组合式 API (Composition API)

- **语法糖**：必须使用 `<script setup>`，禁止使用 Options API
- **响应式**：优先使用 `ref()`，仅在深层嵌套对象时使用 `reactive()`
- **逻辑拆分**：超过 30 行的非 UI 逻辑必须抽取到 `composables/` 目录

---

## 3. 类型与通信 (Types & Props)

- **强类型**：尽量避免 `any`，必要时用 `unknown` 替代。所有 API 响应必须定义 `interface`
- **组件通信**：Props 必须使用 TypeScript 接口定义，并配合 `withDefaults`
- **状态管理**：必须使用 Pinia 的 Setup Store 风格，禁止使用 Vuex

---

## 4. UI 与性能 (UI & Performance)

- **样式方案**：使用 `<style scoped>`，避免样式冲突
- **长列表渲染**：数据量超过 1000 行时建议使用虚拟滚动技术

---

## 5. 目录结构

```
kl-ui/
├── src/
│   ├── assets/             # 静态资源（图片、字体）
│   ├── components/         # 公共组件
│   ├── composables/        # 组合式逻辑抽离
│   ├── services/           # API 调用层
│   ├── stores/             # Pinia 状态管理
│   ├── types/              # TypeScript 类型定义
│   ├── utils/              # 工具函数
│   └── views/              # 页面组件
├── public/                 # 公共静态资源（不放 JS/CSS）
└── index.html
```

---

## 6. API 请求规范

- API 响应统一使用 `ApiResponse<T>` 包装，`request()` 函数已自动解包
- 所有 API 定义放在 `services/api.ts`，按模块分组
- Mock 数据通过 `USE_MOCK` 开关控制，路径 `src/services/mock/`
