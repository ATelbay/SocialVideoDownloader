import SwiftUI

// MARK: - Single-item delete confirmation

/// Presents a native SwiftUI `.alert` to confirm deletion of one history item.
///
/// Usage:
/// ```swift
/// .modifier(HistoryDeleteConfirmation(
///     isPresented: $showDelete,
///     onConfirm: { viewModel.send(HistoryIntentConfirmDeletion()) },
///     onCancel:  { viewModel.send(HistoryIntentDismissDeletionDialog()) }
/// ))
/// ```
struct HistoryDeleteConfirmation: ViewModifier {
    @Binding var isPresented: Bool
    let itemTitle: String?
    let onConfirm: () -> Void
    let onCancel: () -> Void

    func body(content: Content) -> some View {
        content.alert(
            "Delete item?",
            isPresented: $isPresented,
            actions: {
                Button("Delete", role: .destructive, action: onConfirm)
                Button("Cancel", role: .cancel, action: onCancel)
            },
            message: {
                if let title = itemTitle {
                    Text("\"\(title)\" will be removed from your history.")
                } else {
                    Text("This item will be removed from your history.")
                }
            }
        )
    }
}

// MARK: - Delete-all confirmation

/// Presents a native SwiftUI `.alert` to confirm clearing the entire history.
///
/// Usage:
/// ```swift
/// .modifier(HistoryDeleteAllConfirmation(
///     isPresented: $showDeleteAll,
///     itemCount: items.count,
///     onConfirm: { viewModel.send(HistoryIntentConfirmDeletion()) },
///     onCancel:  { viewModel.send(HistoryIntentDismissDeletionDialog()) }
/// ))
/// ```
struct HistoryDeleteAllConfirmation: ViewModifier {
    @Binding var isPresented: Bool
    let itemCount: Int
    let onConfirm: () -> Void
    let onCancel: () -> Void

    func body(content: Content) -> some View {
        content.alert(
            "Delete all history?",
            isPresented: $isPresented,
            actions: {
                Button("Delete All", role: .destructive, action: onConfirm)
                Button("Cancel", role: .cancel, action: onCancel)
            },
            message: {
                Text(
                    itemCount == 1
                        ? "1 item will be permanently removed from your history."
                        : "\(itemCount) items will be permanently removed from your history."
                )
            }
        )
    }
}

// MARK: - View extensions for convenience

extension View {
    /// Attach a single-item delete confirmation alert.
    func historyDeleteConfirmation(
        isPresented: Binding<Bool>,
        itemTitle: String? = nil,
        onConfirm: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) -> some View {
        modifier(HistoryDeleteConfirmation(
            isPresented: isPresented,
            itemTitle: itemTitle,
            onConfirm: onConfirm,
            onCancel: onCancel
        ))
    }

    /// Attach a delete-all confirmation alert.
    func historyDeleteAllConfirmation(
        isPresented: Binding<Bool>,
        itemCount: Int,
        onConfirm: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) -> some View {
        modifier(HistoryDeleteAllConfirmation(
            isPresented: isPresented,
            itemCount: itemCount,
            onConfirm: onConfirm,
            onCancel: onCancel
        ))
    }
}
