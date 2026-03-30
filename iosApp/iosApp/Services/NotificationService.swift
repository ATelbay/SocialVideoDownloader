import Foundation
import UserNotifications

/// Manages local notification permission and posting.
///
/// Used by the Download screen to notify the user when a download completes
/// while the app is in the background.
final class NotificationService {

    static let shared = NotificationService()
    private init() {}

    // MARK: - Permission

    /// Request notification authorization if not already granted.
    /// - Parameter completion: Called with `true` if permission was granted.
    func requestPermission(completion: @escaping (Bool) -> Void) {
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .authorized, .provisional:
                DispatchQueue.main.async { completion(true) }
            case .notDetermined:
                center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
                    DispatchQueue.main.async { completion(granted) }
                }
            default:
                DispatchQueue.main.async { completion(false) }
            }
        }
    }

    // MARK: - Post notifications

    /// Post a "Download complete" local notification.
    func postDownloadComplete(videoTitle: String) {
        let content = UNMutableNotificationContent()
        content.title = NSLocalizedString("notification_download_complete_title", comment: "")
        content.body = videoTitle
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "download-complete-\(UUID().uuidString)",
            content: content,
            trigger: nil // deliver immediately
        )

        UNUserNotificationCenter.current().add(request) { error in
            if let error {
                print("[NotificationService] Failed to post notification: \(error)")
            }
        }
    }

    /// Post a "Download failed" local notification.
    func postDownloadFailed(videoTitle: String) {
        let content = UNMutableNotificationContent()
        content.title = NSLocalizedString("notification_download_failed_title", comment: "Download failed")
        content.body = videoTitle
        content.sound = UNNotificationSound(named: UNNotificationSoundName("basso"))

        let request = UNNotificationRequest(
            identifier: "download-failed-\(UUID().uuidString)",
            content: content,
            trigger: nil
        )

        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
}
