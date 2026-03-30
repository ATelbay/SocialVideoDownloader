import SwiftUI
import shared_feature_library

// MARK: - ViewModel Wrapper

/// ObservableObject wrapper around [SharedLibraryViewModel].
///
/// Bridges Kotlin StateFlow → Swift @Published via the SKIE AsyncSequence API.
@MainActor
final class LibraryViewModelWrapper: ObservableObject {

    // Initial value — will be overwritten immediately when the Kotlin StateFlow emits.
    // Using a nullable wrapper avoids constructing a data object directly.
    @Published var state: LibraryUiState? = nil

    let shared: SharedLibraryViewModel
    private var stateTask: Task<Void, Never>?
    private var effectTask: Task<Void, Never>?

    // One-shot effects forwarded to the view
    @Published var openContentUri: String? = nil
    @Published var shareContentUri: String? = nil
    @Published var toastMessage: String? = nil

    init() {
        shared = KoinViewModelFactory.makeLibraryViewModel()
        startObserving()
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
        shared.cleanup()
    }

    private func startObserving() {
        stateTask = Task { [weak self] in
            guard let self else { return }
            for await newState in shared.uiState {
                self.state = newState
            }
        }
        effectTask = Task { [weak self] in
            guard let self else { return }
            for await effect in shared.effect {
                switch effect {
                case let open as LibraryEffectOpenContent:
                    self.openContentUri = open.contentUri
                case let share as LibraryEffectShareContent:
                    self.shareContentUri = share.contentUri
                case let message as LibraryEffectShowMessage:
                    self.toastMessage = self.resolveMessage(message.messageType)
                default:
                    break
                }
            }
        }
    }

    func send(_ intent: LibraryIntent) {
        shared.onIntent(intent: intent)
    }

    private func resolveMessage(_ type: LibraryMessageType) -> String {
        switch type {
        case .deleteSuccess:
            return "File deleted."
        case .fileNotFound:
            return "File not found."
        case .shareError:
            return "Unable to share file."
        case .openError:
            return "Unable to open file."
        default:
            return "Something went wrong."
        }
    }
}

// MARK: - LibraryView

/// Root library screen showing downloaded videos in a 2-column grid.
struct LibraryView: View {

    @StateObject private var viewModel = LibraryViewModelWrapper()
    @State private var showShareSheet = false
    @State private var shareUri: String? = nil
    @State private var showToast = false
    @State private var toastText: String = ""

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                Color.svdBg.ignoresSafeArea()
                contentView
            }
            .navigationTitle("Library")
            .navigationBarTitleDisplayMode(.inline)
        }
        // Handle OpenContent effect — open file URL with system player
        .onChange(of: viewModel.openContentUri) { uri in
            if let uri {
                openContent(uri: uri)
                viewModel.openContentUri = nil
            }
        }
        // Handle ShareContent effect
        .onChange(of: viewModel.shareContentUri) { uri in
            if let uri {
                shareUri = uri
                showShareSheet = true
                viewModel.shareContentUri = nil
            }
        }
        // Handle ShowMessage effect
        .onChange(of: viewModel.toastMessage) { message in
            if let message {
                toastText = message
                showToast = true
                viewModel.toastMessage = nil
            }
        }
        .sheet(isPresented: $showShareSheet) {
            if let uri = shareUri {
                LibraryShareSheet(contentUri: uri)
            }
        }
        .overlay(alignment: .bottom) {
            if showToast {
                ToastBanner(message: toastText)
                    .padding(.bottom, 24)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            withAnimation { showToast = false }
                        }
                    }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: showToast)
    }

    // MARK: - Content switching

    @ViewBuilder
    private var contentView: some View {
        if let state = viewModel.state {
            switch state {
            case is LibraryUiState.Loading:
                loadingView

            case is LibraryUiState.Empty:
                emptyView

            case let content as LibraryUiState.Content:
                gridView(items: content.items)

            default:
                loadingView
            }
        } else {
            loadingView
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.4)
                .tint(.svdPrimary)
            Text("Loading library…")
                .font(SVDFont.bodyMedium())
                .foregroundColor(.svdOnSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Empty state

    private var emptyView: some View {
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
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Grid

    private func gridView(items: [LibraryListItem]) -> some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(items, id: \.id) { item in
                    LibraryItemRow(
                        item: item,
                        onTap: {
                            viewModel.send(LibraryIntentItemClicked(itemId: item.id))
                        },
                        onShare: {
                            viewModel.send(LibraryIntentItemLongPressed(itemId: item.id))
                        }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 24)
        }
        .refreshable {
            // StateFlow will automatically re-emit when repository updates;
            // a brief yield lets any in-flight DB query settle.
            try? await Task.sleep(nanoseconds: 300_000_000)
        }
    }

    // MARK: - File open helper

    private func openContent(uri: String) {
        // On iOS the contentUri is a file path string stored in the DB.
        // Try constructing a URL and opening it with the system.
        let url: URL?
        if uri.hasPrefix("file://") {
            url = URL(string: uri)
        } else {
            url = URL(fileURLWithPath: uri)
        }
        guard let fileURL = url else { return }
        UIApplication.shared.open(fileURL, options: [:], completionHandler: nil)
    }
}

// MARK: - Share sheet helper

private struct LibraryShareSheet: UIViewControllerRepresentable {
    let contentUri: String

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let url: URL
        if contentUri.hasPrefix("file://"), let parsed = URL(string: contentUri) {
            url = parsed
        } else {
            url = URL(fileURLWithPath: contentUri)
        }
        return UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Toast banner

private struct ToastBanner: View {
    let message: String

    var body: some View {
        Text(message)
            .font(SVDFont.bodyMedium())
            .foregroundColor(.svdSurface)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color.svdOnSurface.opacity(0.88))
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.pill, style: .continuous))
            .shadow(radius: 4)
    }
}

#Preview {
    LibraryView()
}
