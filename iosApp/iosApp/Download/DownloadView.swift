import SwiftUI
@preconcurrency import shared_feature_library

// MARK: - ViewModel Wrapper

/// ObservableObject wrapper around [SharedDownloadViewModel].
///
/// Bridges Kotlin StateFlow → Swift @Published via the SKIE AsyncSequence API.
@MainActor
final class DownloadViewModelWrapper: ObservableObject {

    @Published var state: any DownloadUiState = DownloadUiStateIdle(existingDownload: nil, prefillUrl: nil)

    let shared: SharedDownloadViewModel
    private var stateTask: Task<Void, Never>?
    private var eventTask: Task<Void, Never>?

    // One-shot events forwarded to the view
    @Published var openFileUrl: String? = nil
    @Published var shareFilePath: String? = nil
    @Published var errorMessage: String? = nil

    init() {
        shared = KoinViewModelFactory.makeDownloadViewModel()
        startObserving()
    }

    deinit {
        stateTask?.cancel()
        eventTask?.cancel()
        shared.cleanup()
    }

    private func startObserving() {
        stateTask = Task { [weak self] in
            guard let self else { return }
            for await newState in shared.uiState {
                self.state = newState
            }
        }
        eventTask = Task { [weak self] in
            guard let self else { return }
            for await event in shared.events {
                switch event {
                case let openFile as DownloadEventOpenFile:
                    self.openFileUrl = openFile.filePath
                case let shareFile as DownloadEventShareFile:
                    self.shareFilePath = shareFile.filePath
                case let showError as DownloadEventShowError:
                    self.errorMessage = showError.message ?? showError.errorType.name
                default:
                    break
                }
            }
        }
    }

    func send(_ intent: DownloadIntent) {
        shared.onIntent(intent: intent)
    }
}

// MARK: - DownloadView

/// Root download screen. Switches on [DownloadUiState] to present the
/// appropriate sub-view for each phase of the download flow.
struct DownloadView: View {

    @StateObject private var viewModel = DownloadViewModelWrapper()
    @State private var showShareSheet = false
    @State private var shareFilePath: String? = nil
    @State private var showOpenFile = false
    @State private var openFilePath: String? = nil

    var body: some View {
        NavigationStack {
            ZStack {
                Color.svdBg.ignoresSafeArea()
                currentScreen
            }
            .navigationTitle("Social Video Downloader")
            .navigationBarTitleDisplayMode(.inline)
        }
        // React to one-shot events
        .onChange(of: viewModel.shareFilePath) { newValue in
            if let path = newValue {
                shareFilePath = path
                showShareSheet = true
                viewModel.shareFilePath = nil
            }
        }
        .onChange(of: viewModel.openFileUrl) { newValue in
            if let path = newValue {
                openFilePath = path
                showOpenFile = true
                viewModel.openFileUrl = nil
            }
        }
        .sheet(isPresented: $showShareSheet) {
            if let path = shareFilePath {
                ShareSheet(filePath: path)
            }
        }
        .sheet(isPresented: $showOpenFile) {
            if let path = openFilePath {
                ShareSheet(filePath: path)
            }
        }
    }

    @ViewBuilder
    private var currentScreen: some View {
        switch viewModel.state {
        case let idle as DownloadUiStateIdle:
            UrlInputView(
                prefillUrl: idle.prefillUrl as String?,
                existingDownload: idle.existingDownload,
                onIntent: viewModel.send
            )

        case let extracting as DownloadUiStateExtracting:
            ExtractingView(url: extracting.url, onCancel: {
                viewModel.send(DownloadIntentBackToIdleClicked())
            })

        case let formatSelection as DownloadUiStateFormatSelection:
            FormatSelectionView(
                metadata: formatSelection.metadata,
                selectedFormatId: formatSelection.selectedFormatId,
                onIntent: viewModel.send
            )

        case let downloading as DownloadUiStateDownloading:
            DownloadProgressView(
                videoTitle: downloading.metadata.title,
                thumbnailUrl: downloading.metadata.thumbnailUrl as String?,
                progress: downloading.progress,
                onCancel: { viewModel.send(DownloadIntentCancelDownloadClicked()) }
            )

        case let done as DownloadUiStateDone:
            DownloadCompleteView(
                videoTitle: done.metadata.title,
                thumbnailUrl: done.metadata.thumbnailUrl as String?,
                filePath: done.filePath,
                onOpen: { viewModel.send(DownloadIntentOpenFileClicked()) },
                onShare: { viewModel.send(DownloadIntentShareFileClicked()) },
                onNewDownload: { viewModel.send(DownloadIntentNewDownloadClicked()) }
            )

        case let error as DownloadUiStateError:
            DownloadErrorView(
                title: downloadErrorTitle(for: error.errorType),
                message: error.message as String?,
                onRetry: { viewModel.send(DownloadIntentRetryClicked()) },
                onReset: { viewModel.send(DownloadIntentNewDownloadClicked()) }
            )

        default:
            ProgressView()
        }
    }

    private func downloadErrorTitle(for errorType: Any) -> String {
        switch String(describing: errorType).lowercased() {
        case let value where value.contains("network"):
            return "No Internet Connection"
        case let value where value.contains("server"):
            return "Server Unavailable"
        case let value where value.contains("extraction"):
            return "Could Not Extract Video"
        case let value where value.contains("unsupported"):
            return "Unsupported URL"
        case let value where value.contains("storage"):
            return "Storage Full"
        case let value where value.contains("download"):
            return "Download Failed"
        default:
            return "Something Went Wrong"
        }
    }
}

// MARK: - Extracting intermediate state

private struct ExtractingView: View {
    let url: String
    let onCancel: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            ProgressView()
                .scaleEffect(1.5)
                .tint(.svdPrimary)
            Text("Extracting video info…")
                .font(SVDFont.bodyLarge())
                .foregroundColor(.svdOnSurface)
            Text(url)
                .font(SVDFont.caption())
                .foregroundColor(.svdOnSurfaceVariant)
                .lineLimit(2)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            Button("Cancel", action: onCancel)
                .foregroundColor(.svdOnSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Share Sheet helper

private struct ShareSheet: UIViewControllerRepresentable {
    let filePath: String

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let url = URL(fileURLWithPath: filePath)
        return UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

#Preview {
    DownloadView()
}
