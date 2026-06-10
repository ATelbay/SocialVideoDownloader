import UIKit
import UniformTypeIdentifiers

class ShareViewController: UIViewController {
    private static let appGroup = "group.com.socialvideodownloader.shared"
    private static let sharedURLKey = "SharedURL"

    override func viewDidLoad() {
        super.viewDidLoad()
        handleSharedItems()
    }

    private func handleSharedItems() {
        let providers = (extensionContext?.inputItems as? [NSExtensionItem])?
            .compactMap { $0.attachments }
            .flatMap { $0 } ?? []

        // Find the first attachment we can extract a URL from. A share carries a single link, so
        // we resolve the first match and ignore the rest.
        if let urlProvider = providers.first(where: {
            $0.hasItemConformingToTypeIdentifier(UTType.url.identifier)
        }) {
            urlProvider.loadItem(forTypeIdentifier: UTType.url.identifier) { [weak self] item, _ in
                let urlString = Self.urlString(fromURLItem: item)
                self?.finish(with: urlString)
            }
            return
        }

        if let textProvider = providers.first(where: {
            $0.hasItemConformingToTypeIdentifier(UTType.plainText.identifier)
        }) {
            textProvider.loadItem(forTypeIdentifier: UTType.plainText.identifier) { [weak self] item, _ in
                let urlString = Self.sanitizedURLString(from: item as? String)
                self?.finish(with: urlString)
            }
            return
        }

        // No URL/text attachment present — nothing to do.
        completeRequest()
    }

    /// Persists [urlString] (when non-nil) for the main app and always completes the request,
    /// so the extension never hangs even when extraction fails.
    private func finish(with urlString: String?) {
        if let urlString {
            saveSharedUrl(urlString)
        }
        completeRequest()
    }

    private func saveSharedUrl(_ urlString: String) {
        // The extension cannot open URLs directly. The main app picks the URL up from the shared
        // App Group on its next foreground (scenePhase == .active check in App.swift).
        let userDefaults = UserDefaults(suiteName: Self.appGroup)
        userDefaults?.set(urlString, forKey: Self.sharedURLKey)
    }

    private func completeRequest() {
        extensionContext?.completeRequest(returningItems: nil)
    }

    // MARK: - Pure helpers (no UIKit / extension state — unit-testable)

    /// Extracts an absolute URL string from a loaded `UTType.url` item, accepting both `URL` and the
    /// data-representation form some apps provide.
    static func urlString(fromURLItem item: Any?) -> String? {
        if let url = item as? URL {
            return url.absoluteString
        }
        if let urlData = item as? Data,
           let url = URL(dataRepresentation: urlData, relativeTo: nil) {
            return url.absoluteString
        }
        return nil
    }

    /// Validates that shared plain text is an http(s) link and returns it, or `nil` otherwise.
    static func sanitizedURLString(from text: String?) -> String? {
        guard let text = text?.trimmingCharacters(in: .whitespacesAndNewlines),
              text.hasPrefix("http"),
              let url = URL(string: text),
              url.scheme == "http" || url.scheme == "https" else {
            return nil
        }
        return text
    }
}
