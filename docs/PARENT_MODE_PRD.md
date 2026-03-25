# 家长模式 · 产品需求文档

**版本**：v1.0 · 2026-03-21
**项目**：KidsLearn AI · 星芽学堂
**文档目标**：定义家长模式的完整功能、数据流、API 接口和前端任务拆分

---

## 一、产品概述

### 1.1 核心价值

家长模式是为家长提供的专属配置和互动空间，区别于儿童的学习界面，家长可以通过聊天的方式与 AI 助手交互，核心功能是**制定和管理孩子的学习计划**。

### 1.2 核心特性

```
家长模式
├── 🔐 密码保护 — 首次设置密码，后续需密码进入
├── 💬 学习计划对话 — 通过聊天创建/调整学习计划
├── 🤖 AI 智能规划 — 后端调用 AI Agent 生成阶段性学习计划
├── ✅ 家长确认 — 所有计划变更需家长明确确认
└── 📊 现有设置 — 继承已有的家长设置功能（勿扰/时长/护眼）
```

---

## 二、用户流程

### 2.1 首次进入家长模式

```
1. 家长长按「设置」按钮（3秒）→ 弹出「设置家长密码」界面
2. 输入 6 位数字密码 → 再次确认密码
3. 密码一致 → 保存成功 → 进入家长模式
4. 密码不一致 → 提示「两次密码不一致，请重新输入」
```

### 2.2 非首次进入家长模式

```
1. 家长长按「设置」按钮（3秒）→ 弹出「输入密码」界面
2. 输入正确密码 → 进入家长模式
3. 密码错误 → 提示「密码错误，请重试」，可重试 3 次
4. 连续 3 次错误 → 锁定 30 秒 → 倒计时后重新可试
```

### 2.3 创建学习计划（核心流程）

```
1. 家长在对话框输入：
   「帮我给孩子制定一个 14 天的英语学习计划」

2. AI 理解意图 → 调用后端学习规划 Agent

3. 后端返回阶段性学习计划（示例）：
   {
     "plan": {
       "id": "plan-xxx",
       "subject": "en",
       "duration": 14,
       "dailyGoal": "完成 5 道英语题",
       "phases": [
         { "day": "1-3", "goal": "掌握 10 个动物词汇", "topic": "动物" },
         { "day": "4-7", "goal": "掌握 10 个水果词汇", "topic": "水果" },
         { "day": "8-11", "goal": "掌握 10 个颜色词汇", "topic": "颜色" },
         { "day": "12-14", "goal": "复习巩固 + 小测验", "topic": "复习" }
       ],
       "estimatedMinutes": 20,
       "difficulty": "easy"
     }
   }

4. 前端展示 AI 生成的计划（卡片形式）

5. 家长可以：
   - ✅「确认计划」→ 计划生效，推送给孩子的学习界面
   - 💬「调整一下」→ 继续对话修改（如：把每天 5 题改成 8 题）
   - ❌「取消」→ 不保存，关闭计划

6. 计划生效后，孩子的学习内容将按计划执行
```

### 2.4 修改/终止学习计划

```
1. 家长进入家长模式 → 查看当前计划
2. 可以对话修改：「把数学改成每天 3 题」
3. 可以终止计划：「取消这个计划」
4. AI 确认变更 → 同步到孩子的学习界面
```

---

## 三、功能详情

### 3.1 密码系统

| 功能 | 说明 |
|------|------|
| 密码长度 | 6 位数字 |
| 存储方式 | 加密后存储在 localStorage（生产环境由后端管理） |
| 重置方式 | 需联系客服或通过设备验证重置 |
| 安全限制 | 连续 3 次错误 → 锁定 30 秒 |

### 3.2 学习计划结构

```typescript
interface LearningPlan {
  id: string
  subject: 'en' | 'ma'
  duration: number           // 周期天数
  dailyGoal: string          // 每日目标描述
  phases: PlanPhase[]         // 阶段拆分
  difficulty: 'easy' | 'medium' | 'hard'
  estimatedMinutes: number    // 预计每日时长（分钟）
  status: 'pending' | 'active' | 'paused' | 'completed'
  createdAt: string
  startedAt?: string
  endedAt?: string
}

interface PlanPhase {
  dayRange: string           // "1-3"
  goal: string                // 阶段目标描述
  topic: string              // 主题（如：动物、水果）
  questionsPerDay: number    // 每日题目数
  difficulty: 'easy' | 'medium' | 'hard'
}
```

### 3.3 家长对话交互

| 消息类型 | 来源 | 展示形式 |
|---------|------|---------|
| 家长输入 | 用户发送 | 右侧气泡 + 家长头像 |
| AI 响应 | AI 回复 | 左侧气泡 + 小星老师头像 |
| 计划卡片 | AI 回复 | 特殊卡片（阶段性展示 + 操作按钮） |
| 确认结果 | AI 回复 | 成功/失败提示气泡 |

---

## 四、API 接口设计

> **规约**：前端只做展示和用户交互，所有业务逻辑由后端处理。

### 4.1 密码相关

```
POST /api/parent/password/set
  Body: { password: string }
  Response: { success: boolean }

POST /api/parent/password/verify
  Body: { password: string }
  Response: { success: boolean; attemptsLeft: number }
  错误码: 429 (锁定中) 返回 { lockedUntil: number }

GET /api/parent/password/status
  Response: { hasPassword: boolean }
```

### 4.2 学习计划相关

```
GET /api/parent/plans
  Response: { plans: LearningPlan[]; activePlan?: LearningPlan }

POST /api/parent/plan/create
  Body: { prompt: string }  // 家长对话中的自然语言
  Response: { plan: LearningPlan }  // AI 生成的计划，等待确认

POST /api/parent/plan/confirm
  Body: { planId: string; adjustments?: PlanPhase[] }
  Response: { success: boolean; plan: LearningPlan }

POST /api/parent/plan/adjust
  Body: { planId: string; adjustments: string }  // 自然语言调整描述
  Response: { plan: LearningPlan }

POST /api/parent/plan/pause
POST /api/parent/plan/resume
POST /api/parent/plan/cancel
  Response: { success: boolean }
```

### 4.3 家长聊天消息

```
POST /api/parent/chat
  Body: { message: string; planId?: string }
  Response: {
    reply: string;
    plan?: LearningPlan;           // 如果生成了计划
    planAction?: 'confirm' | 'adjust' | 'cancel'; // 需要用户确认的操作
  }
```

### 4.4 Mock 数据（开发阶段）

```typescript
// mock/parentData.ts

export const mockParentPlans: LearningPlan[] = [
  {
    id: 'plan-001',
    subject: 'en',
    duration: 14,
    dailyGoal: '每天完成 5 道英语题',
    phases: [
      { dayRange: '1-3', goal: '掌握 10 个动物词汇', topic: '动物', questionsPerDay: 5, difficulty: 'easy' },
      { dayRange: '4-7', goal: '掌握 10 个水果词汇', topic: '水果', questionsPerDay: 5, difficulty: 'easy' },
      { dayRange: '8-11', goal: '掌握 10 个颜色词汇', topic: '颜色', questionsPerDay: 5, difficulty: 'easy' },
      { dayRange: '12-14', goal: '复习巩固 + 小测验', topic: '复习', questionsPerDay: 6, difficulty: 'medium' },
    ],
    estimatedMinutes: 20,
    difficulty: 'easy',
    status: 'active',
    createdAt: '2026-03-15',
    startedAt: '2026-03-15',
  }
]

export const mockParentPassword = {
  hasPassword: true,
  // 实际不存明文密码，仅标记是否已设置
}
```

---

## 五、页面与组件设计

### 5.1 家长模式入口流程

```
┌─────────────────────────────────────────┐
│  ChatView (主界面)                       │
│  底部导航 → 长按「设置」3秒              │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  PasswordModal (密码弹窗)                │
│  ├─ 未设置密码 → SetPasswordView        │
│  └─ 已设置密码 → VerifyPasswordView      │
└──────────────┬──────────────────────────┘
               │ 验证成功
               ▼
┌─────────────────────────────────────────┐
│  ParentModeView (家长模式主界面)         │
│  ├─ ChatTimeline (对话列表)             │
│  ├─ ParentInputBar (输入框)              │
│  ├─ LearningPlanCard (计划展示卡片)     │
│  └─ ParentSettingsSection (设置入口)   │
└─────────────────────────────────────────┘
```

### 5.2 新增组件清单

| 组件 | 文件 | 说明 |
|------|------|------|
| PasswordModal | `components/ParentMode/` | 密码验证/设置弹窗容器 |
| SetPasswordView | `components/ParentMode/` | 首次设置密码界面 |
| VerifyPasswordView | `components/ParentMode/` | 密码验证界面 |
| ParentModeView | `views/ParentModeView.vue` | 家长模式主页面 |
| ParentChatTimeline | `components/ParentMode/` | 家长聊天时间轴 |
| ParentInputBar | `components/ParentMode/` | 家长输入框 |
| LearningPlanCard | `components/ParentMode/` | 学习计划展示卡片 |
| PhaseList | `components/ParentMode/` | 阶段列表组件 |
| PlanConfirmSheet | `components/ParentMode/` | 计划确认底部弹窗 |

### 5.3 家长聊天消息类型

```typescript
type ParentMessage = {
  id: string
  role: 'parent' | 'ai'
  content: string
  type: 'text' | 'plan_card' | 'plan_confirm' | 'success' | 'error'
  plan?: LearningPlan
  planAction?: 'confirm' | 'adjust' | 'cancel'
  timestamp: string
}
```

---

## 六、技术实现要点

### 6.1 密码存储

- **开发环境**：localStorage（key: `kidslearn_parent_password`）
- **生产环境**：通过后端 API 管理，前端仅存储 token
- **加密**：使用 Base64 简单混淆（开发），生产使用 bcrypt

### 6.2 聊天消息流

```
家长输入文本
    ↓
API: POST /parent/chat
    ↓
后端调用 AI Agent（学习规划）
    ↓
返回计划/回复
    ↓
前端渲染消息 + 卡片
    ↓
家长确认/调整
    ↓
API: POST /parent/plan/confirm
```

### 6.3 聊天输入区设计

- 与儿童端 InputBar 类似，但：
  - Placeholder: 「告诉老师您想怎么安排学习...」
  - 发送按钮样式不同（家长模式用紫色标识）
  - 不提供语音输入（家长打字输入）

### 6.4 计划卡片设计

```
┌─────────────────────────────────┐
│ 📋 学习计划 · 英语               │
│                                 │
│ ⏰ 周期：14 天 · 每天 5 题       │
│ 🎯 难度：基础                     │
│                                 │
│ 📅 阶段安排：                    │
│  ① 1-3天：动物词汇  🐾         │
│  ② 4-7天：水果词汇  🍎          │
│  ③ 8-11天：颜色词汇  🌈         │
│  ④ 12-14天：复习巩固  📝        │
│                                 │
│ [确认计划]  [调整一下]  [取消]  │
└─────────────────────────────────┘
```

---

## 七、任务拆分

### Phase A：密码系统（优先级 P0）

| # | 任务 | 文件 | 说明 |
|---|------|------|------|
| A1 | PasswordModal 容器组件 | `ParentMode/PasswordModal.vue` | 统一管理密码弹窗，根据状态渲染不同子视图 |
| A2 | SetPasswordView | `ParentMode/SetPasswordView.vue` | 首次设置密码，两次确认逻辑 |
| A3 | VerifyPasswordView | `ParentMode/VerifyPasswordView.vue` | 密码验证，3次错误锁定30秒 |
| A4 | API：密码相关接口 | `api.ts` + `mock/parentData.ts` | getPasswordStatus / setPassword / verifyPassword |
| A5 | 集成到 ChatView | `ChatView.vue` | 长按触发 → PasswordModal |
| A6 | ParentModeView 框架 | `views/ParentModeView.vue` | 家长模式主页面骨架 |

### Phase B：家长聊天核心（优先级 P1）

| # | 任务 | 文件 | 说明 |
|---|------|------|------|
| B1 | ParentChatTimeline | `ParentMode/ParentChatTimeline.vue` | 消息列表，支持多种消息类型渲染 |
| B2 | ParentInputBar | `ParentMode/ParentInputBar.vue` | 家长专属输入框 |
| B3 | ParentMessageBubble | `ParentMode/ParentMessageBubble.vue` | 消息气泡（家长/AI/系统） |
| B4 | API：家长聊天接口 | `api.ts` | sendParentChat() |
| B5 | 聊天状态管理 | `composables/useParentState.ts` | 家长模式聊天状态管理 |

### Phase C：学习计划卡片（优先级 P1）

| # | 任务 | 文件 | 说明 |
|---|------|------|------|
| C1 | LearningPlanCard | `ParentMode/LearningPlanCard.vue` | 计划展示卡片，含阶段列表 |
| C2 | PhaseList | `ParentMode/PhaseList.vue` | 阶段列表组件 |
| C3 | PlanConfirmSheet | `ParentMode/PlanConfirmSheet.vue` | 确认/调整/取消操作 |
| C4 | 计划相关 API | `api.ts` + `mock/parentData.ts` | getPlans / confirmPlan / adjustPlan / cancelPlan |

### Phase D：AI 规划 Agent（优先级 P2 - 后端）

| # | 任务 | 说明 |
|---|------|------|
| D1 | 学习规划 Agent | 接收家长自然语言 → 生成学习计划 |
| D2 | 计划拆解逻辑 | 将长周期计划拆分为每日/每周阶段 |
| D3 | 难度评估 | 根据孩子历史表现评估题目难度 |
| D4 | 计划调整逻辑 | 接收调整指令 → 修改计划 |

### Phase E：集成与优化（优先级 P2）

| # | 任务 | 说明 |
|---|------|------|
| E1 | ParentModeView 完整集成 | 将所有组件集成到家长模式页面 |
| E2 | 计划生效后的孩子端同步 | 计划确认后孩子的学习内容按计划执行 |
| E3 | 家长端查看/修改现有计划 | 支持查看、调整、暂停、取消计划 |
| E4 | UI 优化与动画 | 消息出现动画、计划卡片展开动画 |

---

## 八、Mock 开发阶段接口

```typescript
// 模拟 AI 生成计划（开发阶段）
export async function mockGeneratePlan(prompt: string): Promise<{
  reply: string
  plan?: LearningPlan
}> {
  await sleep(1500) // 模拟 AI 思考
  return {
    reply: '好的，我来为孩子制定一个学习计划...',
    plan: {
      id: `plan-${Date.now()}`,
      subject: prompt.includes('英语') ? 'en' : 'ma',
      duration: 14,
      dailyGoal: '每天完成 5 道题',
      phases: [
        { dayRange: '1-3', goal: '掌握基础词汇', topic: '基础', questionsPerDay: 5, difficulty: 'easy' },
        { dayRange: '4-7', goal: '进阶词汇学习', topic: '进阶', questionsPerDay: 5, difficulty: 'medium' },
        { dayRange: '8-11', goal: '综合练习', topic: '练习', questionsPerDay: 6, difficulty: 'medium' },
        { dayRange: '12-14', goal: '复习巩固', topic: '复习', questionsPerDay: 5, difficulty: 'easy' },
      ],
      estimatedMinutes: 20,
      difficulty: 'medium',
      status: 'pending',
      createdAt: new Date().toISOString(),
    }
  }
}

export async function mockSendParentChat(
  message: string,
  planId?: string
): Promise<ParentMessage> {
  await sleep(1000)
  if (message.includes('计划') || message.includes('安排')) {
    return mockGeneratePlan(message)
  }
  return {
    id: `msg-${Date.now()}`,
    role: 'ai',
    content: '好的，我会帮您安排孩子的学习计划。请告诉我您想要什么样的计划？比如：「制定一个14天的英语学习计划」',
    type: 'text',
    timestamp: new Date().toISOString(),
  }
}
```

---

## 九、验收标准

### 密码系统
- [ ] 首次进入可设置 6 位数字密码
- [ ] 两次密码不一致时提示错误
- [ ] 非首次进入需验证密码
- [ ] 密码错误 3 次后锁定 30 秒

### 家长聊天
- [ ] 家长可发送消息，AI 正常回复
- [ ] 消息自动滚动到最新
- [ ] 支持文本消息展示

### 学习计划
- [ ] AI 生成的计划以卡片形式展示
- [ ] 家长可确认、调整、取消计划
- [ ] 调整内容通过对话完成
- [ ] 计划生效后可在列表中查看

### 集成
- [ ] 家长模式与儿童学习界面独立
- [ ] 切换流畅，无明显卡顿

---

*文档由 Claude Code 生成 · KidsLearn AI · 2026-03-21 · v1.0*
