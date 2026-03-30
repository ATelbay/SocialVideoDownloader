import SwiftUI
import shared_feature_library

/// A single grid card representing a downloaded video in the Library screen.
struct LibraryItemRow: View {
    let item: LibraryListItem
    let onTap: () -> Void
    let onShare: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                thumbnailView
                infoView
            }
            .background(Color.svdSurface)
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous))
            .shadow(color: .black.opacity(0.07), radius: 6, x: 0, y: 2)
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button {
                onTap()
            } label: {
                Label("Open", systemImage: "play.fill")
            }
            Button {
                onShare()
            } label: {
                Label("Share", systemImage: "square.and.arrow.up")
            }
        }
    }

    // MARK: - Thumbnail

    private var thumbnailView: some View {
        ZStack(alignment: .bottomLeading) {
            if let urlString = item.thumbnailUrl, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    case .failure:
                        thumbnailPlaceholder
                    case .empty:
                        thumbnailPlaceholder
                            .overlay(ProgressView().tint(.svdPrimary))
                    @unknown default:
                        thumbnailPlaceholder
                    }
                }
            } else {
                thumbnailPlaceholder
            }
        }
        .frame(height: 110)
        .clipped()

    }

    private var thumbnailPlaceholder: some View {
        Color.svdBg
            .overlay(
                Image(systemName: "play.rectangle.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(.svdOnSurfaceVariant)
            )
    }

    // MARK: - Info

    private var infoView: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(item.title)
                .font(SVDFont.labelLarge())
                .foregroundColor(.svdOnSurface)
                .lineLimit(2)

            HStack(spacing: 6) {
                if !item.platformName.isEmpty {
                    Text(item.platformName)
                        .font(SVDFont.caption())
                        .foregroundColor(.svdPrimary)
                        .lineLimit(1)
                }

                Spacer(minLength: 0)

                if let bytes = item.fileSizeBytes {
                    Text(formatFileSize(bytes))
                        .font(SVDFont.caption())
                        .foregroundColor(.svdOnSurfaceVariant)
                }
            }

            Text(formatDate(item.completedAt))
                .font(SVDFont.caption())
                .foregroundColor(.svdOnSurfaceVariant)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
    }

    // MARK: - Formatters

    private func formatFileSize(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .file
        formatter.allowedUnits = [.useMB, .useGB, .useKB]
        formatter.zeroPadsFractionDigits = false
        return formatter.string(fromByteCount: bytes)
    }

    private func formatDate(_ epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMillis) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }
}
