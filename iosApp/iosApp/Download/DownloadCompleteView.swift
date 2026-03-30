import SwiftUI

/// Completion screen — shown when the download state is [DownloadUiState.Done].
struct DownloadCompleteView: View {

    let videoTitle: String
    let thumbnailUrl: String?
    let filePath: String
    let onOpen: () -> Void
    let onShare: () -> Void
    let onNewDownload: () -> Void

    @State private var checkmarkScale: CGFloat = 0.3
    @State private var checkmarkOpacity: Double = 0

    var body: some View {
        VStack(spacing: 32) {
            Spacer()

            // Success checkmark animation
            ZStack {
                Circle()
                    .fill(Color.svdAccent.opacity(0.12))
                    .frame(width: 120, height: 120)
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 72))
                    .foregroundStyle(.svdAccent)
                    .scaleEffect(checkmarkScale)
                    .opacity(checkmarkOpacity)
            }
            .onAppear {
                withAnimation(.spring(response: 0.5, dampingFraction: 0.6)) {
                    checkmarkScale = 1.0
                    checkmarkOpacity = 1.0
                }
            }

            VStack(spacing: 8) {
                Text("Download Complete!")
                    .font(SVDFont.headlineLarge())
                    .foregroundColor(.svdOnSurface)
                Text(videoTitle)
                    .font(SVDFont.bodyMedium())
                    .foregroundColor(.svdOnSurfaceVariant)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            // Action buttons
            VStack(spacing: 12) {
                HStack(spacing: 12) {
                    Button(action: onOpen) {
                        Label("Open", systemImage: "play.fill")
                            .font(SVDFont.labelLarge())
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color.svdAccent)
                            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
                            .foregroundColor(.white)
                    }

                    Button(action: onShare) {
                        Label("Share", systemImage: "square.and.arrow.up")
                            .font(SVDFont.labelLarge())
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color.svdPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
                            .foregroundColor(.white)
                    }
                }
                .padding(.horizontal, 24)

                Button(action: onNewDownload) {
                    Text("New Download")
                        .font(SVDFont.bodyMedium())
                        .foregroundColor(.svdOnSurfaceVariant)
                        .padding(.top, 4)
                }
            }

            Spacer()
        }
        .padding(.horizontal, 20)
    }
}
