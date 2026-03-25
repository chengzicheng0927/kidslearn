# 🚀 Spring Boot 3.x + JDK 21 开发规约

> **核心目标**：确保在日均 1 亿条短信的高并发场景下，代码具备极致的性能、可观测性和一致性。

---

## 1. JDK 21 现代化语法重塑 (Modernity Score: 30%)

* **虚拟线程 (Virtual Threads)**：
    * 必须在 `application.yml` 开启 `spring.threads.virtual.enabled: true`。
    * 禁止手动创建 `FixedThreadPool`。
    * 禁止在虚拟线程中长时间持有 `synchronized` 锁，优先使用 `ReentrantLock`。
* **模式匹配 (Pattern Matching)**：
    * 禁止显式强转，必须使用 `if (obj instanceof String s)`。
    * 复杂分支必须使用 `switch` 表达式并具备 `yield` 返回值。
* **数据载体**：
    * 所有的 DTO、VO、Event 必须声明为 `record`。
    * 状态机必须使用 `sealed interface` 配合 `permits`。

## 2. Spring Boot 3.x 工程治理 (Framework Score: 25%)

* **声明式客户端**：
    * 新功能研发必须使用 `RestClient`。
    * 存量代码维护允许沿用旧有模式以降低迁移成本。
* **异常处理 (RFC 7807)**：
    * 必须开启 `spring.mvc.problemdetails.enabled: true`。
    * 禁止返回自定义 `Result<T>` 类，统一使用 `ProblemDetail`。
* **可观测性**：
    * 关键步骤必须注入 `ObservationRegistry` 进行埋点。
    * 日志必须包含 `trace_id` 和 `span_id`。

## 3. 架构约束与防御性编程 (Architecture Score: 25%)

* **函数式编程**：集合处理必须使用 `Stream API`，禁止传统 `for` 循环处理大规模数据转换。
* **校验与配置**：
    * Controller 入参必须包含 `jakarta.validation` 注解。
    * 简单配置项（如超时、开关）允许使用 `@Value`。复杂对象或具备逻辑关联的配置必须使用 `@ConfigurationProperties`。

## 4. 性能与健壮性 (Efficiency Score: 20%)

* **非阻塞边界**：Netty EventLoop 中严禁任何阻塞操作。
* **异步转换**：必须使用 `CompletableFuture` 或 JDK 21 结构化并发。
* **日志规范**：
    * 严禁 `e.printStackTrace()`。
    * 日志必须包含业务上下文（如：`log.error("SMS_SEND_FAIL | Mobile: {} | Reason: {}", mobile, e.getMessage())`）。


## 🔵 前端规范：Vue 3 + TS + Tailwind

### 1. 组合式 API (Composition API)
* **语法糖**：必须使用 `<script setup>`，禁止使用 Options API。
* **响应式**：优先使用 `ref()`，仅在深层嵌套对象时使用 `reactive()`。
* **逻辑拆分**：超过 30 行的非 UI 逻辑必须抽取到 `composables/` 目录。

### 2. 类型与通信 (Types & Props)
* **强类型**：尽量避免 `any`，必要时用 `unknown` 替代。所有 API 响应必须定义 `interface`。
* **组件通信**：Props 必须使用 TypeScript 接口定义，并配合 `withDefaults`。
* **状态管理**：必须使用 Pinia 的 Setup Store 风格，禁止使用 Vuex。

### 3. UI 与性能 (UI & Performance)
* **样式方案**：必须优先使用 Tailwind CSS 类名，禁止写冗余的 `<style scoped>`。
* **长列表渲染**：处理海量短信流水统计时（数据量超过 1000 行），建议使用虚拟滚动技术。

### 4. 安全区域 (Safe Area) 与刘海适配
iOS 原生通过 Swift 直接设置 `document.getElementById('app').style.paddingTop` 来适配刘海高度，前端**不需要额外处理 safe-area padding**。

**刘海区域颜色**：各页面通过 JS `document.body.style.background` 控制，需要在 `onMounted` 设置、`onUnmounted` 重置。

```javascript
onMounted(() => {
  document.body.style.background = '<页面背景色>'
})

onUnmounted(() => {
  document.body.style.background = ''
})
```

| 页面 | 刘海/安全区背景色 |
|------|------------------|
| 聊天页（英语） | `linear-gradient(160deg, #E3F2FD 0%, #BBDEFB 40%, #FFF8F3 100%)` |
| 聊天页（数学） | `linear-gradient(160deg, #E8F5E9 0%, #C8E6C9 40%, #FFF8F3 100%)` |
| 日历页 | `#FFF8F3` |
| 宠物页 | `#FFF8F3` |
| 引导页 | `var(--bg)` |
| 家长页 | `linear-gradient(135deg, #7c6df8, #9d8df8)` |

---

## 🤖 AI 自动化审计指令

1. **质量评分**：完成代码后，请根据上述规约进行 0-100 评分。
2. **压力测试**：涉及高并发逻辑时，AI 必须生成基于虚拟线程的压测类，监控 GC 频率。
3. **一致性检查**：确保前端 API 定义与后端 Record 结构完全对齐。

---

## 📋 前端开发核心规约

### 前端只做展示，数据由后端返回

> **所有业务数据和状态必须从后端接口获取，前端仅负责展示和用户交互。任何计算逻辑（如统计、进度、奖励判定）一律由后端处理。**

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

**为什么这样设计？**
1. **防篡改**：前端数据可以被修改（用户可改 JS），业务逻辑必须放后端
2. **一致性**：多端（iOS/Android/H5）保持数据同步
3. **性能**：复杂计算在服务端完成，减轻客户端负担

**例外情况**：
- UI 层面的纯展示状态（如动画触发、加载中）可以使用前端 state
- Mock 数据仅用于开发调试，产品上线前必须对接真实接口

---

## gstack

使用 gstack 的 /browse skill 进行所有网页浏览。禁止使用 `mcp__claude-in-chrome__*` 工具。

**可用技能**：
- `/office-hours` - YC Office Hours，产品构思头脑风暴
- `/plan-ceo-review` - CEO/创始人视角审视计划
- `/plan-eng-review` - 工程技术评审，架构和数据流
- `/plan-design-review` - 设计评审，评分每个设计维度
- `/design-consultation` - 构建设计系统
- `/review` - 代码审查，发现生产环境 bug
- `/investigate` - 系统性根因调试
- `/design-review` - 设计+代码修复
- `/qa` - QA 测试，真实浏览器点击测试
- `/qa-only` - QA 报告（仅报告，不修改代码）
- `/ship` - 发布工程师，同步测试推送 PR
- `/document-release` - 技术文档更新
- `/retro` - 团队周回顾
- `/browse` - QA 工程师，真实 Chromium 浏览器
- `/setup-browser-cookies` - 导入浏览器 Cookie 测试认证页面
- `/codex` - OpenAI Codex 第二意见
- `/careful` - 安全护栏，警告破坏性命令
- `/freeze` - 编辑锁，限制文件修改范围
- `/guard` - 完全安全模式（/careful + /freeze）
- `/unfreeze` - 解除编辑锁
- `/gstack-upgrade` - 自升级

如果 gstack 技能不工作，运行 `cd .claude/skills/gstack && ./setup` 构建二进制文件并注册技能。