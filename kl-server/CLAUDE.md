# Spring Boot 3.x 后端开发规约

> **核心目标**：代码具备极致的性能、可观测性和一致性。

---

## 1. 严谨开发原则

> **后端负责所有业务逻辑和数据计算。前端只是展示层，禁止在后端返回前端可直接使用的数据结构之外的数据。**

### 后端职责边界

| 该做的事 | 禁止做的事 |
|---------|-----------|
| 判定答题对错 | 返回原始题目让前端判断 |
| 计算星星/XP/连击/徽章 | 让前端自己算奖励 |
| 判断宠物进化阶段 | 让前端判断是否进化 |
| 生成热力图数据 | 返回原始打卡记录让前端渲染 |
| 存储所有业务数据 | 前端存储业务相关数据 |
| 返回完成/失败状态 | 让前端判断是否完成 |

### API 设计原则

每个 API 必须返回完整的业务状态，前端**无需任何计算**：

```json
// ✅ 正确：返回完整业务状态
{
  "isCorrect": true,
  "starsEarned": 2,
  "xpEarned": 20,
  "combo": 3,
  "hasMore": true,
  "level": 5,
  "progress": 0.65
}

// ❌ 错误：只返回原始数据
{
  "correctAnswerId": "A",
  "userAnswerId": "A"
}
```

---

## 2. JDK 21 现代化语法

### 虚拟线程 (Virtual Threads)
- 必须在 `application.yml` 开启 `spring.threads.virtual.enabled: true`
- 禁止手动创建 `FixedThreadPool`
- 禁止在虚拟线程中长时间持有 `synchronized` 锁，优先使用 `ReentrantLock`

### 模式匹配 (Pattern Matching)
- 禁止显式强转，必须使用 `if (obj instanceof String s)`
- 复杂分支必须使用 `switch` 表达式并具备 `yield` 返回值

### 数据载体
- 所有的 DTO、VO、Event 必须声明为 `record`
- 状态机必须使用 `sealed interface` 配合 `permits`

---

## 3. Spring Boot 3.x 工程治理

### 声明式客户端
- 新功能研发必须使用 `RestClient`
- 存量代码维护允许沿用旧有模式以降低迁移成本

### 异常处理 (RFC 7807)
- 必须开启 `spring.mvc.problemdetails.enabled: true`
- 禁止返回自定义 `Result<T>` 类，统一使用 `ProblemDetail`

### 可观测性
- 关键步骤必须注入 `ObservationRegistry` 进行埋点
- 日志必须包含 `trace_id` 和 `span_id`

---

## 4. 架构约束与防御性编程

- **函数式编程**：集合处理必须使用 `Stream API`，禁止传统 `for` 循环处理大规模数据转换
- **校验与配置**：
  - Controller 入参必须包含 `jakarta.validation` 注解
  - 简单配置项（如超时、开关）允许使用 `@Value`
  - 复杂对象或具备逻辑关联的配置必须使用 `@ConfigurationProperties`

---

## 5. 性能与健壮性

- **异步转换**：必须使用 `CompletableFuture` 或 JDK 21 结构化并发
- **日志规范**：
  - 严禁 `e.printStackTrace()`
  - 日志必须包含业务上下文（如：`log.error("QUIZ_SUBMIT_FAIL | ChildId: {} | Reason: {}", childId, e.getMessage())`）

---

## 6. 目录结构

```
kl-server/
├── src/main/java/com/kidslearn/
│   ├── chat/              # 聊天服务（题目会话）
│   ├── content/           # 内容服务（题目、知识点）
│   ├── user/             # 用户/儿童账户
│   ├── llm/              # LLM 调用和工具
│   ├── schedule/         # 定时任务
│   ├── config/           # 配置类
│   └── common/           # 公共模块（异常、日志、事件）
└── src/main/resources/
    ├── application.yml
    └── db/migration/      # Flyway 迁移脚本
```
