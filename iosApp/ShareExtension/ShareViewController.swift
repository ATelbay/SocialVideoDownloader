import UIKit
import UniformTypeIdentifiers

class ShareViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        handleSharedItems()
    }

    private func handleSharedItems() {
        guard let extensionItems = extensionContext?.inputItems as? [NSExtensionItem] else {
            extensionContext?.completeRequest(returningItems: nil)
            return
        }

        for item in extensionItems {
            guard let attachments = item.attachments else { continue }
            for provider in attachments {
                if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) {
                    provider.loadItem(forTypeIdentifier: UTType.url.identifier) { [weak self] item, _ in
                        if let url = item as? URL {
                            self?.saveSharedUrl(url.absoluteString)
                        } else if let urlData = item as? Data,
                                  let url = URL(dataRepresentation: urlData, relativeTo: nil) {
                            self?.saveSharedUrl(url.absoluteString)
                        }
                    }
                    return
                }
                if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
                    provider.loadItem(forTypeIdentifier: UTType.plainText.identifier) { [weak self] item, _ in
                        if let text = item as? String, text.hasPrefix("http") {
                            self?.saveSharedUrl(text)
                        }
                    }
                    return
                }
            }
        }
        extensionContext?.completeRequest(returningItems: nil)
    }

    private func saveSharedUrl(_ urlString: String) {
        let userDefaults = UserDefaults(suiteName: "group.com.socialvideodownloader.shared")
        userDefaults?.set(urlString, forKey: "SharedURL")
        userDefaults?.synchronize()

        // The extension cannot open URLs directly.
        // The main app picks up the URL from the shared App Group UserDefaults
        // when it becomes active (scenePhase == .active check in App.swift).
        extensionContext?.completeRequest(returningItems: nil)
    }
}
