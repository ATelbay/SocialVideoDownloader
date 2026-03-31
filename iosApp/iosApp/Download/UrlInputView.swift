import SwiftUI
@preconcurrency import shared_feature_library

/// URL input screen — shown when the download state is [DownloadUiState.Idle].
///
/// Allows the user to type or paste a URL and optionally shows a banner
/// if a previous download for the same URL already exists.
struct UrlInputView: View {

    let prefillUrl: String?
    let existingDownload: DomainExistingDownload?
    let onIntent: (DownloadIntent) -> Void

    @State private var urlText: String = ""
    @FocusState private var isFieldFocused: Bool

    init(
        prefillUrl: String?,
        existingDownload: DomainExistingDownload?,
        onIntent: @escaping (DownloadIntent) -> Void
    ) {
        self.prefillUrl = prefillUrl
        self.existingDownload = existingDownload
        self.onIntent = onIntent
        _urlText = State(initialValue: prefillUrl ?? "")
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                headerSection
                inputCard
                if let existing = existingDownload {
                    existingDownloadBanner(existing: existing)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(spacing: 8) {
            Image(systemName: "arrow.down.circle.fill")
                .font(.system(size: 52))
                .foregroundColor(.svdPrimary)
            Text("Paste a video URL")
                .font(SVDFont.headlineLarge())
                .foregroundColor(.svdOnSurface)
            Text("YouTube, Instagram, TikTok and more")
                .font(SVDFont.bodyMedium())
                .foregroundColor(.svdOnSurfaceVariant)
        }
        .padding(.bottom, 8)
    }

    // MARK: - Input card

    private var inputCard: some View {
        VStack(spacing: 16) {
            HStack(spacing: 12) {
                TextField("https://…", text: $urlText, axis: .vertical)
                    .font(SVDFont.bodyLarge())
                    .foregroundColor(.svdOnSurface)
                    .keyboardType(.URL)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                    .lineLimit(3)
                    .focused($isFieldFocused)
                    .onChange(of: urlText) { newValue in
                        onIntent(DownloadIntentUrlChanged(url: newValue))
                    }

                if !urlText.isEmpty {
                    Button {
                        urlText = ""
                        onIntent(DownloadIntentUrlChanged(url: ""))
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.svdOnSurfaceVariant)
                    }
                }
            }
            .padding(16)
            .background(Color.svdSurface)
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
            .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)

            HStack(spacing: 12) {
                Button {
                    pasteFromClipboard()
                } label: {
                    Label("Paste", systemImage: "doc.on.clipboard")
                        .font(SVDFont.labelLarge())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.svdSurface)
                        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
                        .foregroundColor(.svdPrimary)
                        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 1)
                }

                Button {
                    guard !urlText.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                    isFieldFocused = false
                    onIntent(DownloadIntentExtractClicked())
                } label: {
                    Text("Extract")
                        .font(SVDFont.labelLarge())
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(urlText.trimmingCharacters(in: .whitespaces).isEmpty ? Color.svdOnSurfaceVariant.opacity(0.2) : Color.svdPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
                        .foregroundColor(.white)
                }
                .disabled(urlText.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
    }

    // MARK: - Existing download banner

    private func existingDownloadBanner(existing: DomainExistingDownload) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.svdAccent)
                Text("Already downloaded")
                    .font(SVDFont.labelLarge())
                    .foregroundColor(.svdOnSurface)
                Spacer()
                Button {
                    onIntent(DownloadIntentDismissExistingBanner())
                } label: {
                    Image(systemName: "xmark")
                        .font(.caption)
                        .foregroundColor(.svdOnSurfaceVariant)
                }
            }

            HStack(spacing: 12) {
                if FileManager.default.fileExists(atPath: existing.contentUri) {
                    Button {
                        onIntent(DownloadIntentOpenExistingClicked())
                    } label: {
                        Label("Open", systemImage: "play.fill")
                            .font(SVDFont.bodyMedium())
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.svdAccent.opacity(0.15))
                            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.pill, style: .continuous))
                            .foregroundColor(.svdAccent)
                    }

                    Button {
                        onIntent(DownloadIntentShareExistingClicked())
                    } label: {
                        Label("Share", systemImage: "square.and.arrow.up")
                            .font(SVDFont.bodyMedium())
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.svdPrimary.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.pill, style: .continuous))
                            .foregroundColor(.svdPrimary)
                    }
                } else {
                    Text("Previous file no longer available")
                        .font(SVDFont.caption())
                        .foregroundColor(.svdOnSurfaceVariant)
                }
            }
        }
        .padding(16)
        .background(Color.svdSurface)
        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)
    }

    // MARK: - Clipboard

    private func pasteFromClipboard() {
        if let text = UIPasteboard.general.string, !text.isEmpty {
            urlText = text
            onIntent(DownloadIntentUrlChanged(url: text))
        }
    }
}
