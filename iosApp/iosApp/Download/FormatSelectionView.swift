import SwiftUI
@preconcurrency import shared_feature_library

/// Format selection screen — shown when the download state is [DownloadUiState.FormatSelection].
///
/// Displays a video info card with thumbnail, title, and duration,
/// followed by a scrollable grid of format chips. The selected format
/// is highlighted and the Download button is enabled.
struct FormatSelectionView: View {

    let metadata: DomainVideoMetadata
    let selectedFormatId: String
    let onIntent: (DownloadIntent) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                videoInfoCard
                formatGrid
                actionButtons
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 24)
        }
        .background(Color.svdBg.ignoresSafeArea())
    }

    // MARK: - Video info card

    private var videoInfoCard: some View {
        HStack(alignment: .top, spacing: 14) {
            // Thumbnail
            AsyncImage(url: URL(string: metadata.thumbnailUrl ?? "")) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(16 / 9, contentMode: .fill)
                default:
                    RoundedRectangle(cornerRadius: SVDRadius.small, style: .continuous)
                        .fill(Color.svdOnSurfaceVariant.opacity(0.15))
                        .overlay(Image(systemName: "video").foregroundColor(.svdOnSurfaceVariant))
                }
            }
            .frame(width: 120, height: 68)
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.small, style: .continuous))

            VStack(alignment: .leading, spacing: 6) {
                Text(metadata.title)
                    .font(SVDFont.labelLarge())
                    .foregroundColor(.svdOnSurface)
                    .lineLimit(3)
                if let author = metadata.author {
                    Text(author)
                        .font(SVDFont.caption())
                        .foregroundColor(.svdOnSurfaceVariant)
                }
                if metadata.durationSeconds > 0 {
                    Text(formattedDuration(metadata.durationSeconds))
                        .font(SVDFont.caption())
                        .foregroundColor(.svdOnSurfaceVariant)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(16)
        .background(Color.svdSurface)
        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)
    }

    // MARK: - Format grid

    private var formatGrid: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Choose format")
                .font(SVDFont.headlineMedium())
                .foregroundColor(.svdOnSurface)

            LazyVGrid(
                columns: [GridItem(.flexible()), GridItem(.flexible())],
                spacing: 10
            ) {
                ForEach(metadata.formats, id: \.formatId) { format in
                    FormatChip(
                        format: format,
                        isSelected: format.formatId == selectedFormatId,
                        onTap: { onIntent(DownloadIntentFormatSelected(formatId: format.formatId)) }
                    )
                }
            }
        }
    }

    // MARK: - Action buttons

    private var actionButtons: some View {
        VStack(spacing: 12) {
            Button {
                onIntent(DownloadIntentDownloadClicked())
            } label: {
                Label("Download", systemImage: "arrow.down.circle.fill")
                    .font(SVDFont.labelLarge())
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.svdPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
                    .foregroundColor(.white)
            }

            Button {
                onIntent(DownloadIntentBackToIdleClicked())
            } label: {
                Text("Back")
                    .font(SVDFont.bodyMedium())
                    .foregroundColor(.svdOnSurfaceVariant)
            }
        }
    }

    // MARK: - Helpers

    private func formattedDuration(_ seconds: Int32) -> String {
        let total = Int(seconds)
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        if h > 0 {
            return String(format: "%d:%02d:%02d", h, m, s)
        }
        return String(format: "%d:%02d", m, s)
    }
}

// MARK: - FormatChip

private struct FormatChip: View {
    let format: DomainVideoFormatOption
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 4) {
                Text(format.label)
                    .font(SVDFont.labelLarge())
                    .foregroundColor(isSelected ? .white : .svdOnSurface)
                    .lineLimit(1)
                if let size = format.fileSizeBytes?.int64Value {
                    Text(formattedSize(size))
                        .font(SVDFont.caption())
                        .foregroundColor(isSelected ? .white.opacity(0.8) : .svdOnSurfaceVariant)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(isSelected ? Color.svdPrimary : Color.svdSurface)
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
            .shadow(
                color: isSelected ? Color.svdPrimary.opacity(0.3) : Color.black.opacity(0.05),
                radius: isSelected ? 6 : 3,
                x: 0, y: isSelected ? 3 : 1
            )
            .overlay(
                RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous)
                    .stroke(isSelected ? Color.clear : Color.svdOnSurfaceVariant.opacity(0.15), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func formattedSize(_ bytes: Int64) -> String {
        let mb = Double(bytes) / 1_000_000
        if mb >= 1000 {
            return String(format: "%.1f GB", mb / 1000)
        }
        return String(format: "%.0f MB", mb)
    }
}
