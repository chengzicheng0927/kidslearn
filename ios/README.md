# KidsLearn iOS Shell

当前目录存放 `Swift + WKWebView` 的原生壳源码和资源骨架。

已包含：

- `KidsLearn.xcodeproj`
- `AppDelegate.swift`
- `SceneDelegate.swift`
- `WebViewController.swift`
- `Bridge/SpeechManager.swift`
- `Bridge/PushManager.swift`
- `Bridge/LottieOverlay.swift`
- `Bridge/LockScreenOverlay.swift`
- `Resources/Info.plist`
- `Resources/App.entitlements`

当前仍需在 Xcode 中完成：

1. 打开 `ios/KidsLearn.xcodeproj`
2. 设置实际 `Bundle ID`
3. 配置 Signing & Capabilities
4. 在 Debug 环境下启动 `kl-ui` 的 `vite dev server`
5. Release / Archive 会自动执行 `ios/scripts/sync_web_assets.sh`，把 `kl-ui/dist` 拷贝到 App bundle 的 `web/` 目录
6. 如果后续改用付费 Apple Developer Team，再把 `Push Notifications` 能力和 `aps-environment` entitlement 打开

推荐 target 结构：

- App target: `KidsLearn`
- Bundle ID: 由实际环境配置
- Deployment Target: `iOS 16.0`

说明：

- Debug 模式下，`WebViewController` 会加载 `http://localhost:5173`
- 这条路径适合 iOS 模拟器联调；如果后续要在真机 Debug，需要改成你电脑的局域网 IP 地址
- Release 模式下，`WebViewController` 会尝试从 bundle 的 `web/index.html` 加载前端资源
- `Sync Web Assets` Build Phase 会在非 Debug 构建时自动运行 `npm run build` 并同步前端资源
- 如果你想在 Debug 也把 bundle 资源打进去，可以在 Xcode Scheme 环境变量里加 `KIDSLEARN_EMBED_WEB_IN_DEBUG=1`
- 目前为了兼容个人免费 Team，`App.entitlements` 里已暂时移除 `aps-environment`；要接入 APNs 时再恢复
- 已验证命令行可完成无签名编译：
  `xcodebuild -project ios/KidsLearn.xcodeproj -scheme KidsLearn -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath /tmp/KidsLearnDerived CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build`
