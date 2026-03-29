import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            DownloadPlaceholderView()
                .tabItem {
                    Label("Download", systemImage: "arrow.down.circle")
                }
            LibraryPlaceholderView()
                .tabItem {
                    Label("Library", systemImage: "square.grid.2x2")
                }
            HistoryPlaceholderView()
                .tabItem {
                    Label("History", systemImage: "clock")
                }
        }
    }
}

// MARK: - Placeholder views (will be replaced in later phases)

struct DownloadPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "arrow.down.circle")
                .font(.system(size: 64))
                .foregroundColor(.accentColor)
            Text("Social Video Downloader")
                .font(.title2)
                .fontWeight(.semibold)
            Text("iOS app shell — KMP framework not yet linked")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
    }
}

struct LibraryPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "square.grid.2x2")
                .font(.system(size: 64))
                .foregroundColor(.accentColor)
            Text("Library")
                .font(.title2)
                .fontWeight(.semibold)
            Text("Coming in Phase 7")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }
}

struct HistoryPlaceholderView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "clock")
                .font(.system(size: 64))
                .foregroundColor(.accentColor)
            Text("History")
                .font(.title2)
                .fontWeight(.semibold)
            Text("Coming in Phase 8")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }
}

#Preview {
    ContentView()
}
