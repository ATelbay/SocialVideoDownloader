import SwiftUI
import shared_data

@main
struct SocialVideoDownloaderApp: App {
    init() {
        KoinInitializerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
