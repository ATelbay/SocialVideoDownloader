import SwiftUI
import shared_di

/// UIViewControllerRepresentable that hosts the Compose Multiplatform root.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        SharedAppViewControllerKt.SharedAppViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct SocialVideoDownloaderApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        KoinInitializerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
                // Handle socialvideodownloader:// URLs opened by the system
                .onOpenURL { url in
                    if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                       components.scheme == "socialvideodownloader",
                       let queryItems = components.queryItems,
                       let urlParam = queryItems.first(where: { $0.name == "url" })?.value {
                        KoinHelper.shared.pushSharedUrl(url: urlParam)
                    }
                }
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                // Check for URLs deposited by the Share Extension while the app was backgrounded
                KoinHelper.shared.consumeSharedUrl()
            }
        }
    }
}
