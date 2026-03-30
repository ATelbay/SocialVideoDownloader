import SwiftUI
import shared_feature_history

// MARK: - ViewModel Wrapper

/// ObservableObject wrapper around [SharedHistoryViewModel].
///
/// Bridges Kotlin StateFlow → Swift @Published via the SKIE AsyncSequence API.
@MainActor
final class HistoryViewModelWrapper: ObservableObject {

    @Published var state: HistoryUiState = HistoryUiState.Loading()

    let shared: SharedHistoryViewModel
    private var stateTask: Task<Void, Never>?
    private var effectTask: Task<Void, Never>?

    // One-shot effects
    @Published var toastMessage: String? = nil
    @Published var retryUrl: String? = nil

    init() {
        shared = KoinViewModelFactory.makeHistoryViewModel()
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
                case let showMessage as HistoryEffectShowMessage:
                    self.toastMessage = showMessage.messageType.localizedString
                case let retry as HistoryEffectRetryDownload:
                    self.retryUrl = retry.sourceUrl
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

    var body: some View {
        NavigationStack {
            ZStack {
                Color.svdBg.ignoresSafeArea()
                content
            }
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if case let contentState as HistoryUiState.Content = viewModel.state,
                   !contentState.items.isEmpty {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("Delete All") {
                            // Not supported in current ViewModel — no-op placeholder
                        }
                        .font(SVDFont.labelLarge())
                        .foregroundColor(.svdPrimary)
                    }
                }
            }
        }
        .onChange(of: viewModel.toastMessage) { message in
            if let message {
                currentToast = message
                showToast = true
                viewModel.toastMessage = nil
            }
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
        VStack(spacing: 16) {
            Spacer()
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
            Spacer()
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
