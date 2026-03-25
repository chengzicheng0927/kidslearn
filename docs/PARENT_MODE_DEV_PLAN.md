# 家长模式 · 开发计划

**版本**：v1.0 · 2026-03-21
**项目**：KidsLearn AI · 星芽学堂
**基于**：[PARENT_MODE_PRD.md](./PARENT_MODE_PRD.md)

---

## 一、整体周期估算

| 阶段 | 内容 | 优先级 | 工作量 | 建议周期 |
|------|------|--------|--------|----------|
| Phase A | 密码系统 | **P0** | 6 任务 | **第 1–2 天** |
| Phase B | 家长聊天核心 | **P1** | 5 任务 | **第 3–5 天** |
| Phase C | 学习计划卡片 | **P1** | 4 任务 | **第 6–8 天** |
| Phase D | AI 规划 Agent（后端） | P2 | 4 任务 | **第 9–13 天** |
| Phase E | 集成与优化 | P2 | 4 任务 | **第 14–16 天** |

**总工期：约 16 个工作日（3 周）**

> 备注：Phase D 由后端独立完成，可与前端 Phase B-C 并行开发。前端先用 Mock 数据对接，联调时切换真实接口。

---

## 二、详细任务分解

### Phase A：密码系统（P0）

**目标**：完成家长模式入口的密码验证功能

| # | 任务 | 文件 | 依赖 | 工时 |
|---|------|------|------|------|
| A1 | PasswordModal 容器组件 | `components/ParentMode/PasswordModal.vue` | 无 | 0.5d |
| A2 | SetPasswordView 首次设置密码 | `components/ParentMode/SetPasswordView.vue` | A1 | 0.5d |
| A3 | VerifyPasswordView 密码验证 | `components/ParentMode/VerifyPasswordView.vue` | A1 | 0.5d |
| A4 | 密码 API 接口 + Mock | `api.ts` + `mock/parentData.ts` | 无 | 0.5d |
| A5 | ChatView 长按触发集成 | `ChatView.vue` | A1 | 0.5d |
| A6 | ParentModeView 页面骨架 | `views/ParentModeView.vue` | A5 | 0.5d |

**验收标准**：
- [ ] 首次进入显示设置密码界面
- [ ] 两次密码不一致提示错误
- [ ] 密码错误 3 次后锁定 30 秒
- [ ] 验证成功后进入家长模式

**子任务 Checklist**：
- [ ] `PasswordModal.vue` — 根据 `hasPassword` 状态条件渲染
- [ ] `SetPasswordView.vue` — 两次输入 + 确认按钮 + 错误提示
- [ ] `VerifyPasswordView.vue` — 输入框 + 错误提示 + 锁定倒计时
- [ ] `api.ts` — `getPasswordStatus`, `setPassword`, `verifyPassword`
- [ ] `ChatView.vue` — `@longpress(3000)` → `PasswordModal.show()`
- [ ] `ParentModeView.vue` — 基础路由框架 + 退出按钮

---

### Phase B：家长聊天核心（P1）

**目标**：完成家长模式聊天对话的基础功能

| # | 任务 | 文件 | 依赖 | 工时 |
|---|------|------|------|------|
| B1 | ParentChatTimeline 消息列表 | `ParentMode/ParentChatTimeline.vue` | A6 | 1d |
| B2 | ParentInputBar 家长输入框 | `ParentMode/ParentInputBar.vue` | B1 | 0.5d |
| B3 | ParentMessageBubble 消息气泡 | `ParentMode/ParentMessageBubble.vue` | B1 | 0.5d |
| B4 | 家长聊天 API | `api.ts` | A4 | 0.5d |
| B5 | useParentState 状态管理 | `composables/useParentState.ts` | B1-B4 | 0.5d |

**验收标准**：
- [ ] 家长可发送消息并收到 AI 回复
- [ ] 消息自动滚动到最新
- [ ] 区分家长消息（右侧）和 AI 消息（左侧）
- [ ] 支持文本消息展示

**子任务 Checklist**：
- [ ] `ParentChatTimeline.vue` — `v-for` 渲染消息 + scroll to bottom
- [ ] `ParentMessageBubble.vue` — 根据 `role` 切换样式，`type` 区分展示
- [ ] `ParentInputBar.vue` — placeholder「告诉老师您想怎么安排学习...」
- [ ] `api.ts` — `sendParentChat(message, planId?)`
- [ ] `useParentState.ts` — `messages[]`, `sendMessage()`, `isLoading`

---

### Phase C：学习计划卡片（P1）

**目标**：完成学习计划生成、确认、调整的完整流程

| # | 任务 | 文件 | 依赖 | 工时 |
|---|------|------|------|------|
| C1 | LearningPlanCard 计划卡片 | `ParentMode/LearningPlanCard.vue` | B1 | 1d |
| C2 | PhaseList 阶段列表 | `ParentMode/PhaseList.vue` | C1 | 0.5d |
| C3 | PlanConfirmSheet 确认操作 | `ParentMode/PlanConfirmSheet.vue` | C1 | 0.5d |
| C4 | 计划 API | `api.ts` + `mock/parentData.ts` | B4 | 0.5d |
| C5 | MockGeneratePlan | `mock/parentData.ts` | C4 | 0.5d |

**验收标准**：
- [ ] AI 生成的计划以卡片形式展示
- [ ] 阶段列表清晰展示每个阶段的目标
- [ ] 家长可确认、调整、取消计划
- [ ] 调整通过对话完成

**子任务 Checklist**：
- [ ] `LearningPlanCard.vue` — 标题、周期、难度、阶段列表、操作按钮
- [ ] `PhaseList.vue` — `v-for` 渲染阶段，序号 + 图标 + 目标
- [ ] `PlanConfirmSheet.vue` — 确认 / 调整 / 取消按钮
- [ ] `api.ts` — `getPlans()`, `confirmPlan()`, `adjustPlan()`, `cancelPlan()`
- [ ] `mockGeneratePlan()` — 模拟 AI 生成计划，1.5s 延迟

---

### Phase D：AI 规划 Agent（后端 · P2）

**目标**：后端实现学习计划生成和拆解的 AI 逻辑

| # | 任务 | 说明 | 依赖 | 工时 |
|---|------|------|------|------|
| D1 | 学习规划 Agent 核心 | 接收自然语言 → 调用 LLM 生成计划 | 无 | 3d |
| D2 | 计划拆解逻辑 | 长周期拆分为日/周阶段 | D1 | 1d |
| D3 | 难度评估 | 根据孩子历史表现确定难度 | D1 | 1d |
| D4 | 计划调整逻辑 | 接收调整指令 → 修改计划 | D1 | 1d |

> **并行开发**：Phase D 与 Phase B-C 可并行，前端先用 Mock 对接。

**子任务 Checklist**：
- [ ] `POST /parent/chat` — 识别意图，调用规划 Agent
- [ ] 规划 Agent Prompt — 包含孩子画像、学科知识库、拆解规则
- [ ] 计划结构化输出 — JSON 格式符合 `LearningPlan` 接口
- [ ] 调整逻辑 — 支持增减题目数、调整周期、修改主题

---

### Phase E：集成与优化（P2）

**目标**：前后端联调，完成端到端功能

| # | 任务 | 说明 | 依赖 | 工时 |
|---|------|------|------|------|
| E1 | ParentModeView 完整集成 | 所有组件集成到页面 | C1-C5 | 1d |
| E2 | 孩子端计划同步 | 确认后推送到孩子学习界面 | D1 | 1d |
| E3 | 计划管理功能 | 查看/修改/暂停/取消 | E1 | 1d |
| E4 | UI 优化与动画 | 消息出现动画、卡片展开动画 | E1 | 1d |

**验收标准**：
- [ ] 家长模式与儿童学习界面独立
- [ ] 切换流畅，无明显卡顿
- [ ] 计划确认后孩子在学习界面看到对应内容

---

## 三、里程碑

```
Week 1 (Day 1-5)
├── Day 1-2: Phase A 密码系统 ✅
└── Day 3-5: Phase B 聊天核心 + Mock ✅

Week 2 (Day 6-10)
├── Day 6-8: Phase C 学习计划卡片 + Mock ✅
└── Day 9-10: Phase D AI Agent 开发中...

Week 3 (Day 11-16)
├── Day 11-13: Phase D AI Agent 交付 + 联调
└── Day 14-16: Phase E 集成 + 优化 + UAT ✅
```

---

## 四、技术债务与风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| AI Agent Prompt 调优耗时 | 可能导致联调延期 | 提前准备 Prompt 测试用例，并行 Mock 开发 |
| 密码存储安全性 | localStorage 仅适合开发 | 生产环境必须迁移到后端 API |
| LLM 调用延迟 | 家长等待时间过长 | 添加骨架屏 + 「AI 正在思考...」提示 |
| 计划结构变更 | 前后端接口不一致 | 先定义 TypeScript Interface，后端按此实现 |

---

## 五、优先级汇总

```
P0（阻塞主线）
├── A1–A6  密码系统
└── B1–B5  家长聊天核心

P1（核心功能）
├── C1–C5  学习计划卡片
└── D1–D4  AI 规划 Agent

P2（完善功能）
├── E1–E4  集成与优化
└── 现有设置迁移（日/时长/护眼）
```

---

## 六、快速启动 Checklist

开发前请确认以下环境已就绪：

- [ ] 前端项目 (`kl-ui`) 可运行 `npm run dev`
- [ ] 后端 API 服务可启动（可先用 Mock）
- [ ] Git 分支命名：`feat/parent-mode`
- [ ] TypeScript 类型 `LearningPlan` 已定义
- [ ] Mock 数据文件 `mock/parentData.ts` 已创建

---

*计划由 Claude Code 生成 · KidsLearn AI · 2026-03-21 · v1.0*
