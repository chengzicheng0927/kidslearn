# Kids Learning Mock Frontend

这是一个基于 Vue 3 + Vite 的儿童学习产品前端原型仓库，当前重点放在前端交互、统一 Mock 数据和后续后端接口联调准备。

## 目录说明

- `kl-ui/`：主前端项目
- `docs/`：PRD 和架构说明
- `html-sample/`：早期页面原型
- `ui-design/`：设计稿相关素材

## 当前实现

- 邀请码激活与 onboarding 流程
- 聊天答题主链路
- 学习日历与宠物页
- 显式题库与统一 Mock 数据
- 统一状态层，打通 `child / stars / streak / todayMinutes`

## 核心 Mock 文件

- `kl-ui/src/mock/data.ts`
  这里集中维护题库、日历数据、宠物数据、邀请码和基础用户模型。

- `kl-ui/src/services/api.ts`
  这里模拟后端接口行为，后续后端联调时可以直接按这里的返回结构落接口。

- `kl-ui/src/composables/useAppState.ts`
  这里维护前端统一状态，避免聊天页、日历页、宠物页各自维护一套数据。

## 本地开发

在 `kl-ui` 目录下执行：

```bash
npm install
npm run dev
```

## 构建与测试

```bash
cd kl-ui
npm run build
npm run test:run
npm run coverage
```

测试覆盖：

- Mock 数据与年龄计算
- 统一状态层更新逻辑
- Mock API 行为
- 答题卡组件在点击后的样式状态切换

## 后端联调建议

后续开发后端接口时，建议优先对齐以下接口契约：

1. `POST /api/v1/auth/validate-code`
2. `POST /api/v1/auth/activate`
3. `GET /api/v1/quiz/next`
4. `POST /api/v1/quiz/answer`
5. `GET /api/v1/learning/calendar`
6. `POST /api/v1/pet/feed`

这样可以最小成本替换当前 Mock 层，而不需要重写前端页面结构。
