import SwiftUI
import shared_di

// Shared state for the URL received via the Share Extension or URL scheme.
// ContentView injects this as an EnvironmentObject so DownloadView can observe it.
@MainActor
final class SharedURLState: ObservableObject {
    @Published var pendingURL: String? = nil

    private let suiteName = "group.com.socialvideodownloader.shared"
    private let key = "SharedURL"

    /// Reads and clears any URL written by the Share Extension.
    func consumeSharedURL() {
        guard let defaults = UserDefaults(suiteName: suiteName),
              let urlString = defaults.string(forKey: key) else { return }
        defaults.removeObject(forKey: key)
        defaults.synchronize()
        pendingURL = urlString
    }
}

@main
struct SocialVideoDownloaderApp: App {
    @StateObject private var sharedURLState = SharedURLState()
    @Environment(\.scenePhase) private var scenePhase

    init() {
        KoinInitializerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(sharedURLState)
                // Handle socialvideodownloader:// URLs opened by the system
                .onOpenURL { url in
                    if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                       components.scheme == "socialvideodownloader",
                       let queryItems = components.queryItems,
                       let urlParam = queryItems.first(where: { $0.name == "url" })?.value {
                        sharedURLState.pendingURL = urlParam
                    }
                }
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                // Check for URLs deposited by the Share Extension while the app was backgrounded
                sharedURLState.consumeSharedURL()
            }
        }
    }
}
