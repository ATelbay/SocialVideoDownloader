import SwiftUI
import shared_core_domain

/// Progress screen — shown when the download state is [DownloadUiState.Downloading].
struct DownloadProgressView: View {

    let videoTitle: String
    let thumbnailUrl: String?
    let progress: DownloadProgress?
    let onCancel: () -> Void

    var body: some View {
        VStack(spacing: 28) {
            Spacer()

            // Thumbnail
            AsyncImage(url: URL(string: thumbnailUrl ?? "")) { phase in
                if case .success(let image) = phase {
                    image
                        .resizable()
                        .aspectRatio(16 / 9, contentMode: .fill)
                        .frame(maxWidth: .infinity)
                        .frame(height: 200)
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous))
                } else {
                    RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous)
                        .fill(Color.svdOnSurfaceVariant.opacity(0.12))
                        .frame(height: 200)
                        .overlay(
                            Image(systemName: "arrow.down.circle")
                                .font(.system(size: 48))
                                .foregroundColor(.svdOnSurfaceVariant)
                        )
                }
            }
            .padding(.horizontal, 20)

            VStack(spacing: 8) {
                Text(videoTitle)
                    .font(SVDFont.headlineMedium())
                    .foregroundColor(.svdOnSurface)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)

                if let p = progress, p.progressPercent >= 0 {
                    Text(statusLine(for: p))
                        .font(SVDFont.caption())
                        .foregroundColor(.svdOnSurfaceVariant)
                }
            }

            progressBar

            Button("Cancel", action: onCancel)
                .font(SVDFont.bodyMedium())
                .foregroundColor(.svdOnSurfaceVariant)
                .padding(.bottom, 8)

            Spacer()
        }
    }

    private var progressBar: some View {
        VStack(spacing: 8) {
            let pct = progress.map { Double($0.progressPercent) } ?? 0.0
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: SVDRadius.pill, style: .continuous)
                        .fill(Color.svdOnSurfaceVariant.opacity(0.15))
                        .frame(height: 8)
                    RoundedRectangle(cornerRadius: SVDRadius.pill, style: .continuous)
                        .fill(Color.svdPrimary)
                        .frame(
                            width: pct > 0 ? geo.size.width * min(pct, 1.0) : 0,
                            height: 8
                        )
                        .animation(.linear(duration: 0.3), value: pct)
                }
            }
            .frame(height: 8)
            .padding(.horizontal, 24)

            if let p = progress, p.progressPercent >= 0 {
                Text(String(format: "%.0f%%", p.progressPercent * 100))
                    .font(SVDFont.caption())
                    .foregroundColor(.svdOnSurfaceVariant)
                    .monospacedDigit()
            } else {
                ProgressView()
                    .tint(.svdPrimary)
            }
        }
    }

    private func statusLine(for p: DownloadProgress) -> String {
        var parts: [String] = []
        if p.speedBytesPerSec > 0 {
            parts.append(formatSpeed(p.speedBytesPerSec))
        }
        if p.etaSeconds > 0 {
            parts.append("ETA \(formatEta(p.etaSeconds))")
        }
        if let total = p.totalBytes, total > 0 {
            parts.append("\(formatBytes(p.downloadedBytes)) / \(formatBytes(total))")
        }
        return parts.joined(separator: " · ")
    }

    private func formatSpeed(_ bps: Int64) -> String {
        let kbps = Double(bps) / 1024
        if kbps >= 1024 { return String(format: "%.1f MB/s", kbps / 1024) }
        return String(format: "%.0f KB/s", kbps)
    }

    private func formatEta(_ seconds: Int64) -> String {
        if seconds >= 3600 {
            return String(format: "%dh %dm", seconds / 3600, (seconds % 3600) / 60)
        }
        if seconds >= 60 {
            return String(format: "%dm %ds", seconds / 60, seconds % 60)
        }
        return "\(seconds)s"
    }

    private func formatBytes(_ bytes: Int64) -> String {
        let mb = Double(bytes) / 1_000_000
        if mb >= 1000 { return String(format: "%.1f GB", mb / 1000) }
        if mb >= 1 { return String(format: "%.1f MB", mb) }
        return String(format: "%.0f KB", Double(bytes) / 1000)
    }
}
