import SwiftUI

/// Error screen — shown when the download state is [DownloadUiState.Error].
struct DownloadErrorView: View {

    let title: String
    let message: String?
    let onRetry: () -> Void
    let onReset: () -> Void

    var body: some View {
        VStack(spacing: 28) {
            Spacer()

            // Error icon
            ZStack {
                Circle()
                    .fill(Color.red.opacity(0.1))
                    .frame(width: 100, height: 100)
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 48))
                    .foregroundStyle(.red.opacity(0.8))
            }

            VStack(spacing: 10) {
                Text(title)
                    .font(SVDFont.headlineMedium())
                    .foregroundColor(.svdOnSurface)
                    .multilineTextAlignment(.center)
                if let msg = message, !msg.isEmpty {
                    Text(msg)
                        .font(SVDFont.bodyMedium())
                        .foregroundColor(.svdOnSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }
            }

            VStack(spacing: 12) {
                Button(action: onRetry) {
                    Label("Try Again", systemImage: "arrow.clockwise")
                        .font(SVDFont.labelLarge())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Color.svdPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 24)

                Button(action: onReset) {
                    Text("Start Over")
                        .font(SVDFont.bodyMedium())
                        .foregroundColor(.svdOnSurfaceVariant)
                }
            }

            Spacer()
        }
    }
}
