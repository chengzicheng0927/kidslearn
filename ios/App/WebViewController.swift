import UIKit
import WebKit

final class WebViewController: UIViewController, WKScriptMessageHandler, WKNavigationDelegate {
    private(set) var webView: WKWebView!
    private let webDirectory = "web"

    override func viewDidLoad() {
        super.viewDidLoad()
        setupWebView()
        setupPushCallbacks()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        loadApp()
        updateSafeArea()
    }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        updateSafeArea()
    }

    private func updateSafeArea() {
        let top = view.safeAreaInsets.top
        let bottom = view.safeAreaInsets.bottom
        print("📐 updateSafeArea - top:\(top) bottom:\(bottom)")
        let script = """
            document.documentElement.style.setProperty('--safe-top', '\(top)px');
            document.documentElement.style.setProperty('--safe-bottom', '\(bottom)px');
            var app = document.getElementById('app');
            if (app) {
                app.style.paddingTop = '\(top)px';
                console.log('app.style.paddingTop set to: \(top)px');
            } else {
                console.log('app not found');
            }
        """
        webView.evaluateJavaScript(script) { _, error in
            if let error = error {
                print("📐 JS error:", error)
            }
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
    }

    private func setupWebView() {
        let config = WKWebViewConfiguration()
        let controller = config.userContentController

        [
            "startSpeech",
            "stopSpeech",
            "finishSpeech",
            "playLottie",
            "requestPushPerm",
            "lockScreen",
            "exitApp"
        ].forEach {
            controller.add(self, name: $0)
        }

        let fontScript = WKUserScript(
            source: """
                var s = document.createElement('style');
                s.textContent = 'html, body { font-family: "PingFang SC", "Hiragino Sans GB", sans-serif !important; }';
                document.documentElement.appendChild(s);
            """,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true
        )
        controller.addUserScript(fontScript)

        webView = WKWebView(frame: .zero, configuration: config)
        webView.translatesAutoresizingMaskIntoConstraints = false

        // Hide keyboard accessory toolbar (∧ ∨ ✓)
        webView.inputAssistantItem.leadingBarButtonGroups = []
        webView.inputAssistantItem.trailingBarButtonGroups = []

        if #available(iOS 16.4, *) {
            webView.isInspectable = true
        }
        webView.backgroundColor = UIColor(named: "AppBackground") ?? .systemBackground
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.scrollView.bounces = false
        webView.navigationDelegate = self
        view.addSubview(webView)

        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.topAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
        ])
    }

    private func setupPushCallbacks() {
        PushManager.shared.onPermissionResult = { [weak self] granted in
            self?.sendToVue("onPushPermResult", data: ["granted": granted])
        }

        PushManager.shared.onTokenReceived = { [weak self] token in
            self?.sendToVue("onPushToken", data: ["token": token])
        }

        PushManager.shared.onOpenPayload = { [weak self] payload in
            self?.sendToVue("onPushOpen", data: payload)
        }
    }

    private func loadApp() {
        print("🚀 loadApp 开始")

        // 直接走 dev server（假设 Mac IP 固定）
        let devServerIP = "192.168.31.214"
        let devServerPort = 5173
        print("🌐 加载 dev server: http://\(devServerIP):\(devServerPort)")
        if let url = URL(string: "http://\(devServerIP):\(devServerPort)") {
            webView.load(URLRequest(url: url))
            return
        }

        print("📦 使用本地 bundle 模式")

        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let webURL = documentsURL.appendingPathComponent(webDirectory)
        let indexURL = webURL.appendingPathComponent("index.html")

        print("  Documents: \(documentsURL.path)")
        print("  web目录存在: \(FileManager.default.fileExists(atPath: webURL.path))")
        print("  index.html存在: \(FileManager.default.fileExists(atPath: indexURL.path))")

        // 确保 web 目录存在
        try? FileManager.default.createDirectory(at: webURL, withIntermediateDirectories: true)

        if !FileManager.default.fileExists(atPath: indexURL.path) {
            // 从 bundle 读取并写入 Documents
            if let bundleIndexURL = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "web") {
                print("  从bundle复制: \(bundleIndexURL)")
                try? FileManager.default.copyItem(at: bundleIndexURL, to: indexURL)
            } else {
                print("  ⚠️ bundle中无index.html")
            }
        }

        if FileManager.default.fileExists(atPath: indexURL.path) {
            print("  ✅ loadFileURL: \(indexURL)")
            webView.loadFileURL(indexURL, allowingReadAccessTo: webURL)
        } else {
            print("  ❌ index.html 仍不存在")
        }
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let body = message.body as? [String: Any] else { return }

        switch message.name {
        case "startSpeech":
            let language = body["language"] as? String ?? "zh-CN"
            SpeechManager.shared.start(language: language) { [weak self] text, isFinal in
                self?.sendToVue("onSpeechResult", data: [
                    "text": text,
                    "isFinal": isFinal
                ])
            } onError: { [weak self] error in
                self?.sendToVue("onSpeechError", data: ["message": error.localizedDescription])
            }

        case "stopSpeech":
            SpeechManager.shared.stop()

        case "finishSpeech":
            SpeechManager.shared.finish()

        case "playLottie":
            let name = body["name"] as? String ?? "celebrate"
            LottieOverlay.show(name: name, in: view)

        case "requestPushPerm":
            PushManager.shared.requestPermission()

        case "lockScreen":
            let locked = body["locked"] as? Bool ?? true
            LockScreenOverlay.setLocked(locked, in: view)

        case "exitApp":
            view.window?.isHidden = true

        default:
            break
        }
    }

    func sendToVue(_ event: String, data: [String: Any]) {
        guard let json = try? JSONSerialization.data(withJSONObject: data),
              let jsonString = String(data: json, encoding: .utf8) else { return }

        let script = "window.__onNativeEvent__('\(event)', \(jsonString))"
        DispatchQueue.main.async { [weak self] in
            self?.webView.evaluateJavaScript(script, completionHandler: nil)
        }
    }

    // MARK: - WKNavigationDelegate
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        print("📐 webView didFinish - calling updateSafeArea")
        updateSafeArea()
    }
}
