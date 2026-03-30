import SwiftUI
import shared_feature_history

// MARK: - ViewModel Wrapper

/// ObservableObject wrapper around [SharedHistoryViewModel].
///
/// Bridges Kotlin StateFlow → Swift @Published via the SKIE AsyncSequence API.
@MainActor
final class HistoryViewModelWrapper: ObservableObject {

    @Published var state: HistoryUiState = HistoryUiState.Loading()
    @Published var cloudState: CloudBackupState = CloudBackupState()

    let shared: SharedHistoryViewModel
    private var stateTask: Task<Void, Never>?
    private var cloudStateTask: Task<Void, Never>?
    private var effectTask: Task<Void, Never>?

    // One-shot effects
    @Published var toastMessage: String? = nil
    @Published var retryUrl: String? = nil
    @Published var showUpgradeSheet = false
    @Published var showSignInSheet = false
    @Published var shareContentUri: String? = nil

    init() {
        shared = KoinViewModelFactory.makeHistoryViewModel()
        startObserving()
    }

    deinit {
        stateTask?.cancel()
        cloudStateTask?.cancel()
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
        cloudStateTask = Task { [weak self] in
            guard let self else { return }
            for await newCloudState in shared.cloudBackupState {
                self.cloudState = newCloudState
            }
        }
        effectTask = Task { [weak self] in
            guard let self else { return }
            for await effect in shared.effect {
                switch effect {
                case let showMessage as HistoryEffectShowMessage:
                    self.toastMessage = showMessage.messageType.localizedString
                case let retry as HistoryEffectRetryDownload:
                    self.retryUrl = retry.sourceUrl
                case is HistoryEffectLaunchUpgradeFlow:
                    self.showUpgradeSheet = true
                case is HistoryEffectLaunchGoogleSignIn:
                    self.showSignInSheet = true
                case let openContent as HistoryEffectOpenContent:
                    self.shareContentUri = openContent.contentUri
                case let shareContent as HistoryEffectShareContent:
                    self.shareContentUri = shareContent.contentUri
                default:
                    break
                }
            }
        }
    }

    func send(_ intent: HistoryIntent) {
        shared.onIntent(intent: intent)
    }
}

// MARK: - HistoryMessageType helpers

private extension HistoryMessageType {
    var localizedString: String {
        switch self {
        case .deleteSuccess: return "Item deleted."
        case .deleteAllSuccess: return "All items deleted."
        case .copyUrlSuccess: return "Link copied to clipboard."
        case .cloudSyncError: return "Cloud sync failed."
        case .fileUnavailable: return "File is no longer available."
        case .deleteFileFailed: return "Could not delete the file."
        default: return "Something went wrong."
        }
    }
}

// MARK: - HistoryView

struct HistoryView: View {

    @StateObject private var viewModel = HistoryViewModelWrapper()
    @State private var showToast = false
    @State private var currentToast = ""
    @State private var showShareSheet = false
    @State private var shareUri: String? = nil

    var body: some View {
        NavigationStack {
            ZStack {
                Color.svdBg.ignoresSafeArea()
                content
            }
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.inline)
        }
        .onChange(of: viewModel.toastMessage) { message in
            if let message {
                currentToast = message
                showToast = true
                viewModel.toastMessage = nil
            }
        }
        .onChange(of: viewModel.shareContentUri) { uri in
            if let uri {
                shareUri = uri
                showShareSheet = true
                viewModel.shareContentUri = nil
            }
        }
        .sheet(isPresented: $showShareSheet) {
            if let uri = shareUri {
                HistoryShareSheet(contentUri: uri)
            }
        }
        .sheet(isPresented: $viewModel.showUpgradeSheet) {
            UpgradeView(
                onPurchase: {
                    viewModel.send(HistoryIntentTapUpgrade())
                },
                onRestorePurchases: {
                    // TODO: Add RestorePurchases intent to HistoryIntent when StoreKit is wired
                },
                onDismiss: { viewModel.showUpgradeSheet = false }
            )
        }
        .overlay(alignment: .bottom) {
            if showToast {
                ToastBanner(message: currentToast)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            withAnimation { showToast = false }
                        }
                    }
                    .padding(.bottom, 24)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: showToast)
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case is HistoryUiState.Loading:
            loadingView

        case let emptyState as HistoryUiState.Empty:
            emptyView(state: emptyState)

        case let contentState as HistoryUiState.Content:
            contentView(state: contentState)

        default:
            loadingView
        }
    }

    // MARK: Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.4)
                .tint(.svdPrimary)
            Text("Loading history…")
                .font(SVDFont.bodyMedium())
                .foregroundColor(.svdOnSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: Empty

    private func emptyView(state: HistoryUiState.Empty) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                // Cloud backup section shown even when history is empty
                cloudBackupSection
                    .padding(.top, 8)

                Spacer(minLength: 32)
                Image(systemName: "clock")
                    .font(.system(size: 56))
                    .foregroundStyle(Color.svdOnSurfaceVariant)
                if state.isFiltering {
                    Text("No results for "\(state.query)"")
                        .font(SVDFont.headlineMedium())
                        .foregroundColor(.svdOnSurface)
                        .multilineTextAlignment(.center)
                    Text("Try a different search term.")
                        .font(SVDFont.bodyMedium())
                        .foregroundColor(.svdOnSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                } else {
                    Text("No downloads yet")
                        .font(SVDFont.headlineMedium())
                        .foregroundColor(.svdOnSurface)
                    Text("Your download history will appear here.")
                        .font(SVDFont.bodyMedium())
                        .foregroundColor(.svdOnSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                }
                Spacer(minLength: 32)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: Content

    private func contentView(state: HistoryUiState.Content) -> some View {
        VStack(spacing: 0) {
            searchBar(query: state.query)
            itemList(state: state)
        }
        .alert(
            isPresented: Binding(
                get: { state.deleteConfirmation != nil },
                set: { if !$0 { viewModel.send(HistoryIntentDismissDeletionDialog()) } }
            )
        ) {
            deleteAlert(confirmation: state.deleteConfirmation!)
        }
    }

    // MARK: Cloud backup section

    private var cloudBackupSection: some View {
        CloudBackupView(
            cloudState: viewModel.cloudState,
            cloudCapacity: nil, // CloudCapacity comes from HistoryUiState.Content.cloudCapacity
            onToggleBackup: { viewModel.send(HistoryIntentToggleCloudBackup()) },
            onSignOut: { viewModel.send(HistoryIntentSignOutCloud()) },
            onRestore: { viewModel.send(HistoryIntentRestoreFromCloud()) },
            onTapUpgrade: { viewModel.send(HistoryIntentTapUpgrade()) }
        )
    }

    private func cloudBackupSectionWithCapacity(capacity: CloudCapacity?) -> some View {
        CloudBackupView(
            cloudState: viewModel.cloudState,
            cloudCapacity: capacity,
            onToggleBackup: { viewModel.send(HistoryIntentToggleCloudBackup()) },
            onSignOut: { viewModel.send(HistoryIntentSignOutCloud()) },
            onRestore: { viewModel.send(HistoryIntentRestoreFromCloud()) },
            onTapUpgrade: { viewModel.send(HistoryIntentTapUpgrade()) }
        )
    }

    // MARK: Search bar

    private func searchBar(query: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.svdOnSurfaceVariant)
            TextField("Search history…", text: Binding(
                get: { query },
                set: { viewModel.send(HistoryIntentSearchQueryChanged(query: $0)) }
            ))
            .font(SVDFont.bodyLarge())
            .foregroundColor(.svdOnSurface)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
            if !query.isEmpty {
                Button {
                    viewModel.send(HistoryIntentSearchQueryChanged(query: ""))
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.svdOnSurfaceVariant)
                }
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.svdSurface)
        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    // MARK: Item list

    private func itemList(state: HistoryUiState.Content) -> some View {
        List {
            // Cloud backup section pinned at the top of the history list
            Section {
                cloudBackupSectionWithCapacity(capacity: state.cloudCapacity)
                    .listRowBackground(Color.svdBg)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
            }

            ForEach(state.items, id: \.id) { item in
                HistoryItemRow(
                    item: item,
                    onTap: { viewModel.send(HistoryIntentHistoryItemClicked(itemId: item.id)) },
                    onCopyLink: { viewModel.send(HistoryIntentCopyLinkClicked(itemId: item.id)) },
                    onDelete: { viewModel.send(HistoryIntentDeleteItemClicked(itemId: item.id)) }
                )
                .listRowBackground(Color.svdBg)
                .listRowSeparator(.hidden)
                .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    // MARK: Delete alert

    private func deleteAlert(confirmation: DeleteConfirmationState) -> Alert {
        Alert(
            title: Text("Delete item?"),
            message: Text("This will remove the entry from your history."),
            primaryButton: .destructive(Text("Delete")) {
                viewModel.send(HistoryIntentConfirmDeletion())
            },
            secondaryButton: .cancel {
                viewModel.send(HistoryIntentDismissDeletionDialog())
            }
        )
    }
}

// MARK: - Share Sheet helper

private struct HistoryShareSheet: UIViewControllerRepresentable {
    let contentUri: String

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let url = URL(string: contentUri) ?? URL(fileURLWithPath: contentUri)
        return UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Toast Banner

private struct ToastBanner: View {
    let message: String

    var body: some View {
        Text(message)
            .font(SVDFont.bodyMedium())
            .foregroundColor(.svdSurface)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color.svdOnSurface.opacity(0.9))
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.pill, style: .continuous))
            .shadow(color: .black.opacity(0.15), radius: 8, x: 0, y: 4)
    }
}

#Preview {
    HistoryView()
}
