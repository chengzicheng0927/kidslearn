# iOS 开发规范

> iOS 原生开发，使用 Swift + SwiftUI 构建儿童学习 App。

---

## 1. 安全区域与刘海适配

iOS 通过 Swift 直接设置 `document.getElementById('app').style.paddingTop` 适配刘海高度，**前端不需要额外处理 safe-area padding**。

刘海区域颜色由各页面在 `WKWebView` 的 JavaScript 中控制：

```javascript
document.body.style.background = '<页面背景色>'
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

## 2. WebView 与 H5 通信

- 使用 `WKScriptMessageHandler` 接收 H5 传来的消息
- 使用 `WKWebView.evaluateJavaScript()` 向 H5 发送消息
- 页面切换时必须重置 `document.body.style.background`

---

## 3. Swift 代码规范

- **SwiftUI 组件**：使用 `@StateObject` / `@ObservedObject` / `@EnvironmentObject` 管理状态
- **网络请求**：统一使用 `URLSession`，封装到 `NetworkService`
- **API 通信格式**：
  ```json
  // 请求
  { "action": "submitAnswer", "childId": 1, "questionId": "q1", "answerId": "a1" }
  // 响应
  { "isCorrect": true, "starsEarned": 2, "hasMore": true }
  ```
- **本地存储**：`UserDefaults` 存轻量数据，业务数据放 Keychain 或存 H5 localStorage

---

## 4. iOS 原生组件使用

- 优先使用 iOS 原生组件，H5 仅负责展示和交互
- 语音朗读使用 `AVSpeechSynthesizer`
- 相机/相册使用 `PHPickerViewController`

---

## 5. 目录结构

```
ios/
├── App/                      # App 入口和生命周期
├── KidsLearn/                # 主业务代码
│   ├── Views/                # SwiftUI View
│   ├── ViewModels/           # ViewModel（ObservableObject）
│   ├── Models/               # 数据模型（Codable）
│   ├── Services/             # 服务层（NetworkService、WebViewBridge）
│   └── Utils/                # 工具类（Extensions）
├── KidsLearn.xcodeproj/      # Xcode 项目
└── scripts/                  # 打包脚本
```
