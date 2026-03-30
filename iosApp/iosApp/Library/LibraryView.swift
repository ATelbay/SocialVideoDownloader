import SwiftUI

/// Library screen placeholder — will be implemented in Phase 8.
struct LibraryView: View {
    var body: some View {
        NavigationStack {
            ZStack {
                Color.svdBg.ignoresSafeArea()
                VStack(spacing: 16) {
                    Image(systemName: "square.grid.2x2")
                        .font(.system(size: 56))
                        .foregroundStyle(.svdOnSurfaceVariant)
                    Text("Library")
                        .font(SVDFont.headlineLarge())
                        .foregroundColor(.svdOnSurface)
                    Text("Your downloaded videos will appear here.")
                        .font(SVDFont.bodyMedium())
                        .foregroundColor(.svdOnSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                }
            }
            .navigationTitle("Library")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

/// Placeholder row — will be fleshed out in Phase 8.
struct LibraryItemRow: View {
    var body: some View {
        EmptyView()
    }
}

#Preview {
    LibraryView()
}
