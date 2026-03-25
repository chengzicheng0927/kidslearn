import Foundation
import UIKit
import UserNotifications

final class PushManager: NSObject, UNUserNotificationCenterDelegate {
    static let shared = PushManager()

    var onPermissionResult: ((Bool) -> Void)?
    var onTokenReceived: ((String) -> Void)?
    var onOpenPayload: (([String: Any]) -> Void)?

    private override init() {
        super.init()
    }

    func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            DispatchQueue.main.async {
                self.onPermissionResult?(granted)
                if granted {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
        }
    }

    func didRegister(deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        onTokenReceived?(token)
    }

    func didFailToRegister(error: Error) {
        onOpenPayload?([
            "action": "push_registration_failed",
            "message": error.localizedDescription
        ])
    }

    func handleNotificationResponse(_ response: UNNotificationResponse) {
        var payload = response.notification.request.content.userInfo
        payload["actionIdentifier"] = response.actionIdentifier
        onOpenPayload?(payload as? [String: Any] ?? [:])
    }

    func handleUserActivity(_ activity: NSUserActivity) {
        guard let webpageURL = activity.webpageURL else { return }
        onOpenPayload?(["action": "universal_link", "url": webpageURL.absoluteString])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        handleNotificationResponse(response)
        completionHandler()
    }
}
