import SwiftUI
@preconcurrency import shared_feature_library

/// A single row in the history list.
///
/// Shows thumbnail, title, platform badge, date, and file size.
/// Supports swipe-to-delete and a context menu for copy/share.
struct HistoryItemRow: View {

    let item: HistoryListItem
    let onTap: () -> Void
    let onCopyLink: () -> Void
    let onDelete: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 12) {
                thumbnailView
                infoColumn
                Spacer(minLength: 0)
                statusBadge
            }
            .padding(12)
            .background(Color.svdSurface)
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous))
            .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive, action: onDelete) {
                Label("Delete", systemImage: "trash")
            }
            Button(action: onCopyLink) {
                Label("Copy Link", systemImage: "link")
            }
            .tint(.svdAccent)
        }
        .contextMenu {
            Button {
                onCopyLink()
            } label: {
                Label("Copy Link", systemImage: "link")
            }
            Button(role: .destructive) {
                onDelete()
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }

    // MARK: - Thumbnail

    private var thumbnailView: some View {
        Group {
            if let urlString = item.thumbnailUrl, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        placeholderThumbnail
                    case .empty:
                        ProgressView()
                            .frame(width: 72, height: 54)
                    @unknown default:
                        placeholderThumbnail
                    }
                }
            } else {
                placeholderThumbnail
            }
        }
        .frame(width: 72, height: 54)
        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.small, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: SVDRadius.small, style: .continuous)
                .stroke(Color.svdBg, lineWidth: 0.5)
        )
    }

    private var placeholderThumbnail: some View {
        ZStack {
            Color.svdBg
            Image(systemName: "film")
                .font(.system(size: 22))
                .foregroundColor(.svdOnSurfaceVariant)
        }
    }

    // MARK: - Info column

    private var infoColumn: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(item.title)
                .font(SVDFont.labelLarge())
                .foregroundColor(.svdOnSurface)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            HStack(spacing: 6) {
                platformBadge

                if let label = item.formatLabel {
                    Text(label)
                        .font(SVDFont.caption())
                        .foregroundColor(.svdOnSurfaceVariant)
                        .lineLimit(1)
                }
            }

            HStack(spacing: 6) {
                Text(formattedDate)
                    .font(SVDFont.caption())
                    .foregroundColor(.svdOnSurfaceVariant)

                if let size = item.fileSizeBytes {
                    Text("·")
                        .font(SVDFont.caption())
                        .foregroundColor(.svdOnSurfaceVariant)
                    Text(formattedSize(bytes: size.int64Value))
                        .font(SVDFont.caption())
                        .foregroundColor(.svdOnSurfaceVariant)
                }
            }
        }
    }

    // MARK: - Platform badge

    private var platformBadge: some View {
        let domain = extractDomain(from: item.sourceUrl)
        return Text(domain)
            .font(SVDFont.caption())
            .foregroundColor(.svdSurface)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color.svdAccent)
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.small, style: .continuous))
    }

    // MARK: - Status badge

    private var statusBadge: some View {
        VStack {
            statusIcon
                .font(.system(size: 14, weight: .semibold))
        }
        .padding(.top, 2)
    }

    @ViewBuilder
    private var statusIcon: some View {
        let statusName = item.status.name.lowercased()
        switch statusName {
        case "completed":
            if item.isFileAccessible {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.svdAccent)
            } else {
                Image(systemName: "exclamationmark.circle")
                    .foregroundColor(.svdWarning)
            }
        case "failed":
            Image(systemName: "xmark.circle.fill")
                .foregroundColor(.svdPrimary)
        case "downloading":
            ProgressView()
                .scaleEffect(0.7)
                .tint(.svdPrimary)
        default:
            EmptyView()
        }
    }

    // MARK: - Helpers

    private var formattedDate: String {
        let date = Date(timeIntervalSince1970: Double(item.createdAt) / 1000.0)
        let formatter = DateFormatter()
        formatter.doesRelativeDateFormatting = true
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }

    private func formattedSize(bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .file
        formatter.allowedUnits = [.useKB, .useMB, .useGB]
        return formatter.string(fromByteCount: bytes)
    }

    private func extractDomain(from urlString: String) -> String {
        guard let url = URL(string: urlString),
              let host = url.host else { return "video" }
        // Strip "www." prefix and take the first component (e.g. "youtube" from "youtube.com")
        let stripped = host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
        return stripped.components(separatedBy: ".").first ?? stripped
    }
}

#Preview {
    // Preview with a synthetic item — real HistoryListItem is Kotlin-generated
    VStack(spacing: 8) {
        Text("Preview requires a live KMP runtime.")
            .font(SVDFont.bodyMedium())
            .foregroundColor(.svdOnSurfaceVariant)
            .padding()
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color.svdBg)
}
