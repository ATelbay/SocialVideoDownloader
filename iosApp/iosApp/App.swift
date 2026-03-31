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

    private let appBackground = Color(
        red: 246.0 / 255.0,
        green: 243.0 / 255.0,
        blue: 236.0 / 255.0,
    )

    init() {
        KoinInitializerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                appBackground.ignoresSafeArea()
                ComposeView()
            }
            // Handle socialvideodownloader:// URLs opened by the system
            .onOpenURL { url in
                if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                   components.scheme == "socialvideodownloader",
                   let queryItems = components.queryItems,
                   let urlParam = queryItems.first(where: { $0.name == "url" })?.value {
                    KoinHelper.shared.pushSharedUrl(url: urlParam)
                }
            }
            .background(appBackground)
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                // Check for URLs deposited by the Share Extension while the app was backgrounded
                KoinHelper.shared.consumeSharedUrl()
            }
        }
    }
}
