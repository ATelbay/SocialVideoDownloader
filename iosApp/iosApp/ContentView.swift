import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            DownloadView()
                .tabItem {
                    Label("Download", systemImage: "arrow.down.circle")
                }
            LibraryView()
                .tabItem {
                    Label("Library", systemImage: "square.grid.2x2")
                }
            HistoryView()
                .tabItem {
                    Label("History", systemImage: "clock")
                }
        }
        .tint(.svdPrimary)
    }
}

#Preview {
    ContentView()
}
