# KidsLearn 原生集成任务清单

基于以下文档整理：

- [docs/KidsLearn_PRD.md](/Users/tianlei/hnyk/vibe-coding/kid-learning/docs/KidsLearn_PRD.md)
- [docs/KidsLearn_ArchSpec_v1.2.md](/Users/tianlei/hnyk/vibe-coding/kid-learning/docs/KidsLearn_ArchSpec_v1.2.md)

当前结论：

- 移动端目标形态不是纯 H5，而是 `iOS 原生 + Vue WebView`
- 语音输入必须走 iOS 原生 `SFSpeechRecognizer`
- 推送必须走 APNs
- 主动学习提醒需要“推送 + App 内预置消息”双链路

进度更新（2026-03-20）：

- [x] Web 侧 `nativeBridge`、语音输入回填、推送事件接收、消息模型扩展已接入
- [x] iOS `WKWebView` / `SpeechManager` / `PushManager` / `LottieOverlay` / `LockScreenOverlay` 源码骨架已创建
- [x] 前端构建通过，关键回归测试通过
- [ ] 仍缺签名配置、真机联调与原生壳实际运行验证
- [ ] 后端真实设备 token 注册接口与 Session 初始化接口尚未接通

---

## 1. 目标范围

本阶段原生集成目标：

- 建立 `Swift Shell + WKWebView + JS Bridge`
- 支持儿童语音输入并转文字发送到聊天框
- 支持 APNs 推送权限、Device Token 注册、通知点击唤醒
- 支持后端预置主动消息注入聊天流
- 为后续 Lottie、锁屏、语音评测预留 Bridge 能力

暂不包含：

- Android 原生接入
- 英语发音评分算法
- 家长端 Web 推送
- 离线资源下载系统

---

## 2. 优先级总览

| 优先级 | 模块 | 目标 |
|---|---|---|
| P0 | iOS 原生壳 | 跑起 Vue WebView，具备原生调用能力 |
| P0 | JS Bridge | 打通 Vue -> Swift -> Vue 通信 |
| P0 | 语音输入 | 原生识别转文字，回填输入框并发送 |
| P1 | 推送通知 | 权限、Token、通知点击回传 |
| P1 | 主动消息 | 冷启动 / 点击通知后注入 AI 第一条消息 |
| P1 | 字体与 WebView 注入 | 必要的运行时样式和环境修复 |
| P2 | Lottie 动画 | 原生庆祝动画桥接 |
| P2 | 勿扰锁屏 | 原生锁屏覆盖层 |
| P3 | 英语发音评测 | 语音识别之外的评分能力 |

---

## 3. 阶段任务清单

## 阶段一：iOS 原生壳搭建

- [x] 创建 iOS 原生工程，采用 `Swift + WKWebView`
- [x] 建立基础目录结构
- [x] 支持开发环境加载 `http://localhost:5173`
- [x] 支持生产环境加载本地打包后的静态资源
- [x] 配置 iOS 16+ 最低版本
- [ ] 配置应用基础权限与签名能力
- [x] 建立 WebView 生命周期管理
- [x] 验证 Xcode 工程可识别 target / scheme 并完成无签名编译
- [ ] 验证现有 Vue 应用可在原生壳内正常启动

建议原生目录：

- `ios/App/WebViewController.swift`
- `ios/App/AppDelegate.swift`
- `ios/App/SceneDelegate.swift`
- `ios/App/Resources/`

验收标准：

- iOS 模拟器和真机都能启动 WebView
- 能区分开发和生产加载路径
- 不改业务逻辑时，现有前端页面可正常运行

---

## 阶段二：Bridge 底座

- [x] 在 `WKUserContentController` 注册消息处理器
- [x] 定义统一原生方法入口
- [x] 定义统一原生回调入口 `window.__onNativeEvent__`
- [x] 实现前端 `callNative`
- [x] 实现前端 `onNativeEvent`
- [x] 在浏览器开发环境提供 mock
- [x] 补充错误处理和不可用环境保护

前端新增文件：

- `kl-ui/src/utils/nativeBridge.ts`

前端改动文件：

- `kl-ui/src/main.ts`

Bridge 首批 action：

- `startSpeech`
- `stopSpeech`
- `requestPushPerm`
- `lockScreen`
- `playLottie`

Bridge 首批 event：

- `onSpeechResult`
- `onSpeechError`
- `onPushPermResult`
- `onPushToken`
- `onPushOpen`

验收标准：

- Vue 能调用原生 action
- 原生能向 Vue 回传事件
- 浏览器环境运行时不会因为缺少 `webkit.messageHandlers` 崩溃

---

## 阶段三：语音输入闭环

### 原生侧

- [x] 新建 `SpeechManager`
- [x] 接入 `SFSpeechRecognizer`
- [x] 接入麦克风录音链路
- [x] 支持 `partial result`
- [x] 支持 `final result`
- [x] 支持手动停止
- [x] 支持权限拒绝处理
- [x] 确保语音数据本地处理，不上传服务端
- [x] 完成后销毁临时识别资源

建议原生文件：

- `ios/App/Bridge/SpeechManager.swift`

系统权限：

- [x] `NSSpeechRecognitionUsageDescription`
- [x] `NSMicrophoneUsageDescription`

### 前端侧

- [x] `InputBar.vue` 改为支持受控输入值
- [x] 增加录音状态 `isRecording`
- [x] 点击麦克风开始/停止识别
- [x] `partial result` 实时回填输入框
- [x] `final result` 自动走现有发送逻辑
- [x] 语音失败时给出 UI 提示

建议改动文件：

- `kl-ui/src/components/InputBar.vue`
- `kl-ui/src/views/ChatView.vue`

验收标准：

- 说中文后，输入框能实时看到识别结果
- 识别完成后，文本自动发送到聊天消息流
- 权限被拒绝时，界面能恢复到可再次操作状态

---

## 阶段四：消息模型与聊天流适配

- [x] 扩展聊天消息模型，区分 typed / voice / push / system
- [x] 语音发送消息进入现有消息列表
- [x] 系统注入消息和用户主动发送消息可并存
- [x] 保留现有文字输入发送逻辑

建议改动文件：

- `kl-ui/src/types/chat.ts`
- `kl-ui/src/views/ChatView.vue`
- `kl-ui/src/components/ChatTimeline.vue`

建议新增字段：

- `source?: 'typed' | 'voice' | 'push' | 'system'`
- `action?: string`
- `subject?: 'en' | 'ma'`

验收标准：

- 手输消息和语音消息渲染一致
- 后续推送消息可作为系统消息插入列表

---

## 阶段五：APNs 推送接入

### 原生侧

- [x] 新建 `PushManager`
- [x] 申请通知权限
- [x] 注册 APNs
- [x] 获取 Device Token
- [x] 将 token 通过 Bridge 回传给 Vue
- [x] 处理前台通知展示策略
- [x] 处理通知点击事件

建议原生文件：

- `ios/App/Bridge/PushManager.swift`

### 前端侧

- [x] App 启动时触发推送权限流程
- [x] 接收 `onPushPermResult`
- [x] 接收 `onPushToken`
- [ ] 将 token 发送给后端绑定设备（当前仅完成 mock 上报）
- [x] 接收 `onPushOpen`
- [x] 根据通知 payload 打开学习会话

建议改动文件：

- `kl-ui/src/utils/nativeBridge.ts`
- `kl-ui/src/views/ChatView.vue`
- `kl-ui/src/services/api.ts`

后端对接要求：

- [ ] User Service 提供设备 token 注册接口
- [ ] 设备 token 绑定 parent / child
- [ ] 支持 token 更新和失效清理

验收标准：

- 权限申请成功后可拿到 APNs token
- token 能成功上报服务端
- 点击通知可带着 payload 进入 App

---

## 阶段六：主动学习消息注入

依据架构文档，主动消息不是前端本地生成，而是后端预置：

- Scheduler Service 在提醒时间写入 Redis
- Chat Service 初始化 Session 时检查 Redis
- 若命中则返回预置 AI 问候

任务清单：

- [ ] 前端新增 Session 初始化流程
- [ ] 冷启动时请求一次预置消息
- [x] 点击推送进入时请求一次预置消息
- [ ] 前台唤醒时在符合条件下请求预置消息
- [ ] 若存在预置消息，则作为 AI 第一条消息插入聊天流
- [ ] 若无预置消息，则走默认欢迎语

建议改动文件：

- `kl-ui/src/views/ChatView.vue`
- `kl-ui/src/services/api.ts`
- `kl-ui/src/types/chat.ts`

后端依赖：

- [ ] Chat Service 提供 Session 初始化接口
- [ ] 返回 `prefetchedMessage` / `action` / `subject`

验收标准：

- 冷启动超过阈值可看到主动开场消息
- 点推送进入可显示与 payload 对应的开场消息
- 学习中不再额外弹推送，改为 App 内消息

---

## 阶段七：字体注入与 WebView 运行时修复

- [x] 在原生 WebView 注入最小必要样式修复
- [ ] 避免使用全局 `* { ... !important }` 长期覆盖业务样式
- [ ] 明确只修复 WebView 字体与 viewport 相关问题
- [ ] 验证和现有 Twemoji 方案不冲突

建议原则：

- 文本字体由前端样式控制
- Emoji 继续优先走 Twemoji/SVG
- 原生注入仅作为兜底，不作为长期业务样式来源

验收标准：

- 不引入新的字体覆盖问题
- iOS WebView 中文和 emoji 都稳定

---

## 阶段八：Lottie 动画与勿扰锁屏

### Lottie

- [x] 原生侧增加 `playLottie`
- [x] 支持传入动画名和展示时机
- [ ] 在答题正确 / 奖励页场景接入

### 勿扰锁屏

- [x] 原生侧增加锁屏覆盖层
- [x] 支持前端传入锁定状态
- [ ] 支持晚间勿扰和学习超时锁定

建议原生文件：

- `ios/App/Bridge/LottieOverlay.swift`
- `ios/App/Bridge/LockScreenOverlay.swift`

建议前端改动文件：

- `kl-ui/src/components/ResultModal.vue`
- `kl-ui/src/views/ChatView.vue`

验收标准：

- 可以通过 Bridge 调用原生动画
- 锁屏态可以阻止继续学习交互

---

## 阶段九：英语语音评测预留

此项不与“语音转文字发送”同阶段推进，建议在语音输入稳定后单独做。

- [ ] 明确评测引擎选型
- [ ] 设计 A/B/C 三档评级映射
- [ ] 区分“语音作答”与“语音评测”两种模式
- [ ] 评测结果接入英语练习题型

备注：

- 文档要求不给数字分，避免挫败感
- 此功能建议作为后续独立里程碑

---

## 4. 文件改动建议清单

### 前端新增

- [x] `kl-ui/src/utils/nativeBridge.ts`
- [ ] `kl-ui/src/services/device.ts`
- [ ] `kl-ui/src/services/push.ts`

### 前端修改

- [x] `kl-ui/src/main.ts`
- [x] `kl-ui/src/components/InputBar.vue`
- [x] `kl-ui/src/views/ChatView.vue`
- [x] `kl-ui/src/types/chat.ts`
- [x] `kl-ui/src/services/api.ts`

### iOS 新增

- [x] `ios/KidsLearn.xcodeproj`
- [x] `ios/scripts/sync_web_assets.sh`
- [x] `ios/App/WebViewController.swift`
- [x] `ios/App/Bridge/SpeechManager.swift`
- [x] `ios/App/Bridge/PushManager.swift`
- [x] `ios/App/Bridge/LottieOverlay.swift`
- [x] `ios/App/Bridge/LockScreenOverlay.swift`
- [x] `ios/App/Resources/Info.plist`
- [x] `ios/App/Resources/App.entitlements`
- [x] `ios/README.md`

---

## 5. 测试清单

### Bridge

- [x] 浏览器环境 mock 可运行
- [ ] iOS 真机 / 模拟器 action 与 event 正常

### Web 验证

- [x] `npm run build`
- [x] `npm run test:run -- src/components/QuizCard.test.ts src/services/api.test.ts src/composables/useAppState.test.ts`

### 语音

- [ ] 首次权限弹窗
- [ ] 拒绝权限后的恢复流程
- [ ] partial result 实时更新
- [ ] final result 自动发送
- [ ] 中途停止识别

### 推送

- [ ] APNs token 获取
- [ ] token 上报成功
- [ ] 通知点击回传 payload
- [ ] 前台、后台、冷启动三种进入路径

### 主动消息

- [ ] 冷启动主动问候
- [ ] 点击推送进入主动问候
- [ ] 学习中改为 App 内消息

### 运行时兼容

- [ ] iOS 16+
- [ ] 模拟器与真机行为对比
- [ ] WebView 字体与 Twemoji 不冲突

### 原生工程验证

- [x] `xcodebuild -list -project ios/KidsLearn.xcodeproj`
- [x] `xcodebuild -project ios/KidsLearn.xcodeproj -scheme KidsLearn -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath /tmp/KidsLearnDerived CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build`
- [x] `xcodebuild -project ios/KidsLearn.xcodeproj -scheme KidsLearn -configuration Release -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath /tmp/KidsLearnReleaseDerived CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build`

---

## 6. 推荐执行顺序

### Sprint 1

- [x] iOS 原生壳
- [x] JS Bridge
- [x] 语音输入 MVP

### Sprint 2

- [x] APNs 权限
- [x] Device Token 注册
- [x] 推送点击打开 App

### Sprint 3

- [ ] 主动消息注入
- [ ] 学习时间提醒闭环
- [ ] 推送限流与勿扰逻辑联调

### Sprint 4

- [ ] Lottie 动画
- [ ] 锁屏覆盖层
- [ ] 语音评测方案预研

---

## 7. 当前阻塞项

- [ ] 尚未配置开发 Team / Bundle ID / 自动签名
- [ ] 设备 token 注册接口当前仍为 mock
- [ ] 尚无 Session 初始化返回预置消息接口
- [ ] APNs / 语音能力尚未完成真机或模拟器联调
- [ ] 还未在 Xcode 中实际启动并验证 `WKWebView` 页面加载

---

## 8. 第一批建议立即开工的任务

- [x] 创建 iOS 原生壳工程并接入 `WKWebView`
- [x] 新建 `nativeBridge.ts`
- [x] 改造 `InputBar.vue` 为受控输入
- [x] 接入 `SFSpeechRecognizer`
- [x] 打通“语音识别完成 -> 自动发送消息”闭环
