import SwiftUI

/// History screen placeholder — will be implemented in Phase 7.
struct HistoryView: View {
    var body: some View {
        NavigationStack {
            ZStack {
                Color.svdBg.ignoresSafeArea()
                VStack(spacing: 16) {
                    Image(systemName: "clock")
                        .font(.system(size: 56))
                        .foregroundStyle(.svdOnSurfaceVariant)
                    Text("History")
                        .font(SVDFont.headlineLarge())
                        .foregroundColor(.svdOnSurface)
                    Text("Your download history will appear here.")
                        .font(SVDFont.bodyMedium())
                        .foregroundColor(.svdOnSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                }
            }
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

/// Placeholder row — will be fleshed out in Phase 7.
struct HistoryItemRow: View {
    var body: some View {
        EmptyView()
    }
}

#Preview {
    HistoryView()
}
