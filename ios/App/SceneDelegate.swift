import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        print("📐 SceneDelegate - windowScene.screen:", windowScene.screen)
        print("📐 SceneDelegate - windowScene.screen.bounds:", windowScene.screen.bounds)

        let window = UIWindow(windowScene: windowScene)
        print("📐 SceneDelegate - window.bounds before:", window.bounds)

        let rootViewController = WebViewController()
        window.rootViewController = rootViewController
        window.makeKeyAndVisible()
        print("📐 SceneDelegate - window.bounds after makeKeyAndVisible:", window.bounds)

        self.window = window

        if let response = connectionOptions.notificationResponse {
            PushManager.shared.handleNotificationResponse(response)
        }
    }

    func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
        PushManager.shared.handleUserActivity(userActivity)
    }
}
