# 家长模式 · API 接口规范

**版本**：v1.0 · 2026-03-21
**项目**：KidsLearn AI · 星芽学堂
**状态**：开发中（Mock 模式）

---

## 一、切换方式

```typescript
// 文件：src/config/api.config.ts

export const API_CONFIG = {
  USE_MOCK: true,           // 开发模式（当前）
  // USE_MOCK: false,       // 生产模式：切换到真实后端

  API_BASE_URL: 'http://localhost:8080',  // 后端地址
  TIMEOUT: 30_000,
  DEBUG: true,
}
```

**切换步骤**：
1. `USE_MOCK = false`
2. `API_BASE_URL` 配置为后端地址
3. 后端按本文档实现所有接口

---

## 二、接口总览

| 分类 | 接口 | 方法 | 路径 | 状态 |
|------|------|------|------|------|
| **密码** | 获取密码状态 | GET | `/api/parent/password/status` | ✅ Mock |
| | 设置密码 | POST | `/api/parent/password/set` | ✅ Mock |
| | 验证密码 | POST | `/api/parent/password/verify` | ✅ Mock |
| **计划** | 获取计划列表 | GET | `/api/parent/plans` | ✅ Mock |
| | 创建计划（AI） | POST | `/api/parent/plan/create` | ✅ Mock |
| | 确认计划 | POST | `/api/parent/plan/confirm` | ✅ Mock |
| | 调整计划（AI） | POST | `/api/parent/plan/adjust` | ✅ Mock |
| | 暂停计划 | POST | `/api/parent/plan/pause` | ✅ Mock |
| | 恢复计划 | POST | `/api/parent/plan/resume` | ✅ Mock |
| | 取消计划 | POST | `/api/parent/plan/cancel` | ✅ Mock |
| **聊天** | 发送消息 | POST | `/api/parent/chat` | ✅ Mock |
| **设置** | 获取设置 | GET | `/api/parent/settings` | ✅ Mock |
| | 更新设置 | PUT | `/api/parent/settings` | ✅ Mock |
| **日历** | 获取月份数据 | GET | `/api/children/{id}/calendar/{year}/{month}` | ✅ Mock |
| | 获取日历统计 | GET | `/api/children/{id}/calendar/stats` | ✅ Mock |
| | 获取孩子档案 | GET | `/api/children/{id}/profile` | ✅ Mock |
| | 获取日详情 | GET | `/api/children/{id}/calendar/daily/{date}` | ✅ Mock |

---

## 三、接口详情

### 3.1 密码相关

#### GET /api/parent/password/status
获取密码状态

**Request**: 无

**Response**:
```json
{
  "hasPassword": true
}
```

---

#### POST /api/parent/password/set
设置家长密码（首次设置）

**Request**:
```json
{
  "password": "123456"
}
```

**Validation**:
- 密码长度：6 位
- 仅支持数字

**Response**:
```json
{
  "success": true
}
```

**Errors**:
- `400`: 密码格式不正确

---

#### POST /api/parent/password/verify
验证家长密码

**Request**:
```json
{
  "password": "123456"
}
```

**Response (成功)**:
```json
{
  "success": true,
  "attemptsLeft": 3
}
```

**Response (失败)**:
```json
{
  "success": false,
  "attemptsLeft": 2
}
```

**Response (锁定中 - HTTP 429)**:
```json
{
  "success": false,
  "attemptsLeft": 0,
  "lockedUntil": 1742544000000
}
```

**安全策略**:
- 连续 3 次错误 → 锁定 30 秒
- `attemptsLeft` 为剩余尝试次数

---

### 3.2 学习计划相关

#### GET /api/parent/plans
获取所有学习计划

**Request**: 无

**Response**:
```json
{
  "plans": [
    {
      "id": "plan-001",
      "subject": "en",
      "duration": 14,
      "dailyGoal": "每天完成 5 道英语题",
      "phases": [
        {
          "dayRange": "1-3",
          "goal": "掌握 10 个动物词汇",
          "topic": "动物",
          "questionsPerDay": 5,
          "difficulty": "easy"
        }
      ],
      "difficulty": "easy",
      "estimatedMinutes": 20,
      "status": "active",
      "createdAt": "2026-03-15",
      "startedAt": "2026-03-15"
    }
  ],
  "activePlan": { ... }
}
```

**Status 枚举**: `pending` | `active` | `paused` | `completed`

---

#### POST /api/parent/plan/create
**【AI Agent 接口】** 创建学习计划

**Request**:
```json
{
  "prompt": "帮我制定一个14天的英语学习计划"
}
```

**AI Agent 需实现**:
1. 解析家长意图（学科、周期、难度等）
2. 查询孩子画像（年龄、历史表现）
3. 生成个性化学习计划
4. 拆解为多个阶段

**Response**:
```json
{
  "plan": {
    "id": "plan-xxx",
    "subject": "en",
    "duration": 14,
    "dailyGoal": "每天完成 5 道英语题",
    "phases": [...],
    "difficulty": "medium",
    "estimatedMinutes": 20,
    "status": "pending",
    "createdAt": "2026-03-21"
  },
  "reply": "好的，我来为孩子制定一个 14 天的英语学习计划..."
}
```

---

#### POST /api/parent/plan/confirm
确认并激活学习计划

**Request**:
```json
{
  "planId": "plan-xxx",
  "adjustments": [
    { "dayRange": "1-3", "goal": "掌握 10 个动物词汇", "topic": "动物", "questionsPerDay": 8, "difficulty": "easy" }
  ]
}
```

- `adjustments` 为可选，可覆盖特定阶段

**Response**:
```json
{
  "success": true,
  "plan": {
    "id": "plan-xxx",
    "status": "active",
    "startedAt": "2026-03-21"
  }
}
```

---

#### POST /api/parent/plan/adjust
**【AI Agent 接口】** 调整学习计划

**Request**:
```json
{
  "planId": "plan-xxx",
  "adjustments": "把每天5题改成8题"
}
```

**AI Agent 需实现**:
1. 解析调整意图
2. 修改对应阶段
3. 返回调整后的计划

**Response**:
```json
{
  "plan": { ... },
  "reply": "好的，我已经根据您的要求调整了学习计划。"
}
```

---

#### POST /api/parent/plan/pause
暂停学习计划

**Request**:
```json
{
  "planId": "plan-xxx"
}
```

**Response**:
```json
{
  "success": true
}
```

---

#### POST /api/parent/plan/resume
恢复学习计划

**Request**:
```json
{
  "planId": "plan-xxx"
}
```

**Response**:
```json
{
  "success": true
}
```

---

#### POST /api/parent/plan/cancel
取消学习计划

**Request**:
```json
{
  "planId": "plan-xxx"
}
```

**Response**:
```json
{
  "success": true
}
```

---

### 3.3 聊天消息

#### POST /api/parent/chat
**【核心接口 · AI Agent】** 发送家长聊天消息

**Request**:
```json
{
  "message": "帮我制定一个14天的英语学习计划",
  "planId": "plan-001"
}
```

**AI Agent 意图识别流程**:
```
家长消息
    │
    ▼
意图分类
    ├── 创建计划 → 调用 /plan/create → 返回 plan 对象
    ├── 调整计划 → 调用 /plan/adjust → 返回 plan 对象
    ├── 暂停计划 → 调用 /plan/pause → 返回 success
    ├── 恢复计划 → 调用 /plan/resume → 返回 success
    ├── 取消计划 → 调用 /plan/cancel → 返回 success
    ├── 查询状态 → 返回状态描述
    └── 其他 → 返回普通文本回复
```

**Response (生成计划)**:
```json
{
  "reply": "好的，我来为孩子制定一个 14 天的英语学习计划...",
  "plan": { ... },
  "planAction": "confirm"
}
```

**Response (普通回复)**:
```json
{
  "reply": "好的，我会根据您的需求来安排孩子的学习。"
}
```

**planAction 说明**:
- `confirm`: 需要用户确认计划
- `adjust`: 需要用户调整计划
- `cancel`: 计划已取消

---

### 3.4 家长设置

#### GET /api/parent/settings
获取家长设置

**Response**:
```json
{
  "dailyLimitMinutes": 30,
  "dndEnabled": true,
  "dndStart": "21:00",
  "dndEnd": "08:00",
  "eyeCareReminderEnabled": true,
  "breakReminderEnabled": true
}
```

---

#### PUT /api/parent/settings
更新家长设置

**Request** (所有字段可选):
```json
{
  "dailyLimitMinutes": 60,
  "dndEnabled": false,
  "breakReminderEnabled": true
}
```

**Response**:
```json
{
  "success": true,
  "settings": { ... }
}
```

---

### 3.5 日历/学习记录

#### GET /api/children/{childId}/calendar/{year}/{month}
获取指定月份的学习数据（用于渲染日历热力图）

**Request**: 无（路径参数）

**Response**:
```json
{
  "year": 2026,
  "month": 3,
  "days": {
    "1": {
      "heat": 1,
      "minutes": 18,
      "subjects": [
        { "icon": "📖", "name": "英语", "topic": "动物词汇", "stars": 8, "accuracy": 80, "color": "#40C4FF", "bg": "rgba(64,196,255,.12)" }
      ]
    },
    "5": {
      "heat": 3,
      "minutes": 38,
      "subjects": [...]
    }
  }
}
```

**说明**:
- `heat`: 学习热度 0-4（0=未学习）
- `days`: 仅返回有学习记录的天，无记录的天不返回

---

#### GET /api/children/{childId}/calendar/stats?year=2026&month=3
获取日历统计数据（用于日历页顶部统计）

**Query Parameters**:
- `year`: 年份
- `month`: 月份

**Response**:
```json
{
  "totalDays": 12,
  "totalMinutes": 286,
  "totalQuestions": 48,
  "totalStars": 156,
  "currentStreak": 5,
  "longestStreak": 12
}
```

---

#### GET /api/children/{childId}/profile
获取孩子档案（用于日历页/家长模式）

**Response**:
```json
{
  "id": "child-001",
  "name": "小明",
  "age": 8,
  "ageGroup": "7-8 岁",
  "stars": 286,
  "streak": 7,
  "todayMinutes": 24,
  "level": 4,
  "xp": 78,
  "xpNeeded": 100,
  "totalAnswered": 120,
  "totalDays": 45
}
```

---

#### GET /api/children/{childId}/calendar/daily/{date}
获取指定日期的学习详情

**Path Parameters**:
- `date`: YYYY-MM-DD 格式

**Response**:
```json
{
  "date": "2026-03-15",
  "totalMinutes": 38,
  "totalQuestions": 12,
  "correctCount": 10,
  "accuracy": 83,
  "streak": 5,
  "subjects": [
    { "icon": "📖", "name": "英语", "topic": "家庭成员", "stars": 15, "accuracy": 95, "color": "#40C4FF", "bg": "rgba(64,196,255,.12)" }
  ]
}
```

---

## 四、类型定义

### LearningPlan（学习计划）

```typescript
interface LearningPlan {
  id: string
  subject: 'en' | 'ma'
  duration: number           // 周期天数
  dailyGoal: string          // 每日目标描述
  phases: PlanPhase[]        // 阶段拆分
  difficulty: 'easy' | 'medium' | 'hard'
  estimatedMinutes: number    // 预计每日时长
  status: 'pending' | 'active' | 'paused' | 'completed'
  createdAt: string           // ISO 日期
  startedAt?: string          // 开始日期
  endedAt?: string            // 结束日期
}
```

### PlanPhase（计划阶段）

```typescript
interface PlanPhase {
  dayRange: string           // "1-3"
  goal: string                // 阶段目标
  topic: string               // 主题
  questionsPerDay: number      // 每日题目数
  difficulty: 'easy' | 'medium' | 'hard'
}
```

### ParentMessage（聊天消息）

```typescript
interface ParentMessage {
  id: string
  role: 'parent' | 'ai' | 'system'
  content: string
  type: 'text' | 'plan_card' | 'plan_confirm' | 'success' | 'error' | 'thinking'
  plan?: LearningPlan
  planAction?: 'confirm' | 'adjust' | 'cancel'
  timestamp: string
  thinking?: boolean
}
```

### ParentSettings（家长设置）

```typescript
interface ParentSettings {
  dailyLimitMinutes: number     // 每日时长上限（分钟）
  dndEnabled: boolean           // 勿扰模式
  dndStart: string             // HH:mm
  dndEnd: string               // HH:mm
  eyeCareReminderEnabled: boolean
  breakReminderEnabled: boolean
}
```

### StudyDayRecord（日学习记录）

```typescript
interface StudyDayRecord {
  heat: number          // 学习热度 0-4
  minutes: number       // 学习时长（分钟）
  subjects: StudySubjectRecord[]
}
```

### StudySubjectRecord（学科记录）

```typescript
interface StudySubjectRecord {
  icon: string    // 图标，如 '📖'
  name: string    // 学科名，如 '英语'
  topic: string   // 主题，如 '动物词汇'
  stars: number   // 获得星星数
  accuracy: number // 正确率 0-100
  color: string    // 主题色，如 '#40C4FF'
  bg: string       // 背景色，如 'rgba(64,196,255,.12)'
}
```

---

## 五、错误处理

所有接口统一错误格式：

```json
{
  "code": 400,
  "message": "密码格式不正确"
}
```

**错误码**:
- `400`: 请求参数错误
- `401`: 未认证
- `403`: 无权限
- `404`: 资源不存在
- `429`: 请求过于频繁（锁定中）
- `500`: 服务器内部错误

---

## 六、Mock 数据

开发阶段使用 `src/mock/parentData.ts` 中的 Mock 数据。

切换到生产后端后，所有 Mock 数据将被忽略。

---

*接口规范由 Claude Code 生成 · KidsLearn AI · 2026-03-21 · v1.0*
