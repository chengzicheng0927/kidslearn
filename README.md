# KidsLearn - 儿童 AI 学习平台

通过 AI 聊天问答驱动儿童英语和数学学习的移动优先产品。

## 技术栈

| 模块 | 技术 | 说明 |
|------|------|------|
| **kl-server** | Spring Boot 3.4 / JDK 21 | REST API 后端，PostgreSQL 数据库 |
| **kl-ui** | Vue 3 / TypeScript / Tailwind | 移动端 H5 前端，Vite 构建 |
| **ios** | Swift / WKWebView | iOS 包装 App |

## 目录结构

```
kidslearn/
├── kl-server/          # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/kidslearn/
│   │       ├── content/    # 答题、宠物、成就、徽章
│   │       ├── chat/       # AI 聊天、安全过滤
│   │       ├── user/       # 用户、家长控制
│   │       ├── report/     # 学习报表
│   │       └── common/     # 公共组件（事件、异常、日志）
│   └── src/test/java/      # 单元测试（47 个测试）
├── kl-ui/              # Vue 3 前端
│   ├── src/
│   │   ├── views/          # 页面组件
│   │   ├── components/     # 可复用组件
│   │   ├── stores/         # Pinia 状态管理
│   │   ├── services/       # API 调用层
│   │   ├── mock/           # Mock 数据
│   │   └── config/         # 环境配置
│   └── package.json
├── ios/                # iOS 包装 App
│   └── App/
└── docs/              # 产品文档
```

## 核心功能

### 已上线

- **邀请码激活与 Onboarding** — 家长绑定孩子账号
- **AI 聊天答题** — 英语/数学自适应题目流，连击奖励机制
- **宠物养成系统** — 喂食、互动、进化（真实后端数据）
- **知识星球徽章** — 答题解锁等级徽章（真实后端数据）
- **成就墙** — 连续天数、正确率、题量成就
- **学习日历热力图** — 每日学习打卡统计
- **家长控制模式** — 家长密码验证进入，设置学习目标

### 开发进度

| 阶段 | 状态 |
|------|------|
| 学习目标动态化（后端配置每日目标） | ✅ 完成 |
| 宠物系统（喂食、进化、情感） | ✅ 完成 |
| 成就与徽章体系 | ✅ 完成 |
| 日历热力图 | ✅ 完成 |

详见 [API 完善与去 Mock 化开发计划](docs/API完善与去Mock化开发计划.md)

## 本地开发

### 前端

```bash
cd kl-ui
npm install
npm run dev
```

> 默认使用 Mock 数据，启动后访问 http://localhost:5173
> 切换到真实后端：在 `src/config/api.config.ts` 中设置 `USE_MOCK = false`，并确保后端运行在 http://localhost:8080

```typescript
// src/config/api.config.ts
export const API_CONFIG = {
  USE_MOCK: false,           // 改为 false 使用真实后端
  BASE_URL: 'http://localhost:8080',
}
```

### 后端

```bash
cd kl-server
./mvnw spring-boot:run
```

> 需要 PostgreSQL 数据库，配置见 `src/main/resources/application.yml`

### 测试

```bash
# 后端测试（47 个单元测试）
cd kl-server && mvn test

# 前端测试
cd kl-ui && npm run test:run

# 前端覆盖率
cd kl-ui && npm run coverage
```

## API 概览

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/auth/validate-code` | 验证邀请码 |
| POST | `/api/auth/activate` | 激活账号 |
| GET | `/api/children/{id}` | 获取孩子档案 |
| POST | `/api/quiz/next` | 获取下一道题 |
| POST | `/api/quiz/submit` | 提交答案 |
| GET | `/api/children/{id}/calendar/{year}/{month}` | 月度学习日历 |
| GET | `/api/children/{id}/pet` | 获取宠物状态 |
| POST | `/api/children/{id}/pet/feed` | 喂食宠物 |
| POST | `/api/children/{id}/pet/interact` | 互动宠物 |
| GET | `/api/children/{id}/achievements` | 获取成就列表 |
| GET | `/api/children/{id}/badges` | 获取知识星球徽章 |

## 技术亮点

- **Spring Boot 3.4 + JDK 21** — 虚拟线程、模式匹配、record DTO
- **Vue 3 Composition API + Pinia** — Setup Store 风格，响应式状态管理
- **前后端分离 Mock 切换** — `USE_MOCK` 一键切换，方便开发调试
- **领域事件驱动** — `AnswerSubmittedEvent` 触发宠物/成就/徽章联动更新
- **PostgreSQL GENERATED 列** — `accuracy` 等聚合值由数据库自动计算
- **47 个后端单元测试** — 覆盖 Service 和 Controller 层

## 部署

### 后端容器化

```bash
cd kl-server
docker build -t kidslearn-backend .
docker run -p 8080:8080 kidslearn-backend
```

### 前端构建

```bash
cd kl-ui
npm run build
# 输出到 dist/ 目录
```

> 前端构建产物可部署至任意静态托管服务（Vercel、Netlify、OSS 等）
