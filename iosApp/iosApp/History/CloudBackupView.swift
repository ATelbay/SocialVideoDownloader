import SwiftUI
import shared_feature_history

// MARK: - CloudBackupView

/// Cloud backup section embedded in HistoryView.
///
/// Displays sign-in state, backup toggle, capacity bar, last sync time,
/// and restore button. Auth/backup state comes from [CloudBackupState] and
/// capacity info comes from [CloudCapacity] (both exposed by SharedHistoryViewModel).
///
/// Sign-in flow on iOS uses Sign in with Apple (required per App Store Guidelines §4.8
/// for apps that offer third-party sign-in) alongside Google Sign-In.
///
/// TODO: Wire Google Sign-In and Sign in with Apple once Firebase is integrated.
///       Currently the sign-in button shows a placeholder action.
struct CloudBackupView: View {

    let cloudState: CloudBackupState
    /// Capacity snapshot from ObserveCloudCapacityUseCase; nil while loading.
    let cloudCapacity: CloudCapacity?
    let onToggleBackup: () -> Void
    let onSignOut: () -> Void
    let onRestore: () -> Void
    let onTapUpgrade: () -> Void
    @State private var showSignInSheet = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            sectionHeader
            Divider().background(Color.svdOnSurfaceVariant.opacity(0.2))
            if cloudState.isSignedIn {
                signedInContent
            } else {
                signedOutContent
            }
        }
        .background(Color.svdSurface)
        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous))
        .shadow(color: .black.opacity(0.06), radius: 6, x: 0, y: 2)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .sheet(isPresented: $showSignInSheet) {
            SignInSheet(
                onSignInWithGoogle: {
                    showSignInSheet = false
                    // TODO: Launch Google Sign-In SDK, obtain idToken, then call:
                    // onSignInWithGoogle(idToken)
                    // For now, this is a placeholder.
                },
                onSignInWithApple: {
                    showSignInSheet = false
                    // TODO: Launch Sign in with Apple via ASAuthorizationController
                    // For now, this is a placeholder.
                },
                onDismiss: { showSignInSheet = false }
            )
        }
    }

    // MARK: Section header

    private var sectionHeader: some View {
        HStack {
            Image(systemName: "icloud")
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(.svdAccent)
            Text("Cloud Backup")
                .font(SVDFont.labelLarge())
                .foregroundColor(.svdOnSurface)
            Spacer()
            if cloudState.isSignedIn {
                syncStatusBadge
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    // MARK: Sync status badge

    @ViewBuilder
    private var syncStatusBadge: some View {
        switch cloudState.syncStatus {
        case is SyncStatusSyncing:
            HStack(spacing: 4) {
                ProgressView()
                    .scaleEffect(0.7)
                    .tint(.svdAccent)
                Text("Syncing")
                    .font(SVDFont.caption())
                    .foregroundColor(.svdAccent)
            }
        case let synced as SyncStatusSynced:
            HStack(spacing: 4) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 12))
                    .foregroundColor(.svdAccent)
                Text(synced.lastSyncTimestamp.formattedSyncTime)
                    .font(SVDFont.caption())
                    .foregroundColor(.svdOnSurfaceVariant)
            }
        case let paused as SyncStatusPaused:
            HStack(spacing: 4) {
                Image(systemName: "pause.circle")
                    .font(.system(size: 12))
                    .foregroundColor(.svdWarning)
                Text(paused.reason)
                    .font(SVDFont.caption())
                    .foregroundColor(.svdWarning)
            }
        case let error as SyncStatusError:
            HStack(spacing: 4) {
                Image(systemName: "exclamationmark.circle")
                    .font(.system(size: 12))
                    .foregroundColor(.red)
                Text("Error")
                    .font(SVDFont.caption())
                    .foregroundColor(.red)
            }
        default:
            EmptyView()
        }
    }

    // MARK: Signed-out content

    private var signedOutContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Sign in to back up your download history and access it on all your devices.")
                .font(SVDFont.bodyMedium())
                .foregroundColor(.svdOnSurfaceVariant)
                .padding(.horizontal, 16)
                .padding(.top, 12)

            if cloudState.isSigningIn {
                HStack {
                    Spacer()
                    ProgressView()
                        .tint(.svdPrimary)
                    Spacer()
                }
                .padding(.vertical, 8)
            } else {
                signInButton(
                    title: "Sign in",
                    systemImage: "person.crop.circle",
                    action: { showSignInSheet = true }
                )
                .padding(.horizontal, 16)
            }

            if let error = cloudState.signInError {
                Text(error)
                    .font(SVDFont.caption())
                    .foregroundColor(.red)
                    .padding(.horizontal, 16)
            }
        }
        .padding(.bottom, 16)
    }

    private func signInButton(title: String, systemImage: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.system(size: 14, weight: .medium))
                Text(title)
                    .font(SVDFont.labelLarge())
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(Color.svdBg)
            .foregroundColor(.svdOnSurface)
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous)
                    .stroke(Color.svdOnSurfaceVariant.opacity(0.25), lineWidth: 1)
            )
        }
    }

    // MARK: Signed-in content

    private var signedInContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            // User info row
            if let name = cloudState.userName {
                HStack(spacing: 8) {
                    Image(systemName: "person.circle.fill")
                        .font(.system(size: 24))
                        .foregroundColor(.svdOnSurfaceVariant)
                    Text(name)
                        .font(SVDFont.bodyMedium())
                        .foregroundColor(.svdOnSurface)
                    Spacer()
                    Button("Sign out") {
                        onSignOut()
                    }
                    .font(SVDFont.caption())
                    .foregroundColor(.svdOnSurfaceVariant)
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 8)
            }

            Divider()
                .background(Color.svdOnSurfaceVariant.opacity(0.15))
                .padding(.horizontal, 16)

            // Backup toggle row
            Toggle(isOn: Binding(
                get: { cloudState.isCloudBackupEnabled },
                set: { _ in onToggleBackup() }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Cloud Backup")
                        .font(SVDFont.bodyLarge())
                        .foregroundColor(.svdOnSurface)
                    Text(cloudState.isCloudBackupEnabled
                        ? "Download history is backed up"
                        : "Backup is turned off")
                        .font(SVDFont.caption())
                        .foregroundColor(.svdOnSurfaceVariant)
                }
            }
            .toggleStyle(SwitchToggleStyle(tint: .svdAccent))
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            // Capacity bar (only shown when backup is enabled)
            if cloudState.isCloudBackupEnabled {
                capacityRow
                    .padding(.horizontal, 16)
                    .padding(.bottom, 8)
            }

            Divider()
                .background(Color.svdOnSurfaceVariant.opacity(0.15))
                .padding(.horizontal, 16)

            // Action buttons row
            HStack(spacing: 12) {
                Button {
                    onRestore()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.counterclockwise")
                            .font(.system(size: 13, weight: .medium))
                        Text("Restore")
                            .font(SVDFont.labelLarge())
                    }
                    .foregroundColor(.svdAccent)
                }

                Spacer()

                Button {
                    onTapUpgrade()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "star")
                            .font(.system(size: 13, weight: .medium))
                        Text("Upgrade")
                            .font(SVDFont.labelLarge())
                    }
                    .foregroundColor(.svdPrimary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
    }

    // MARK: Capacity bar

    private var capacityRow: some View {
        let used = Int(cloudCapacity?.used ?? 0)
        let total = Int(cloudCapacity?.limit ?? 1000)
        let fraction = total > 0 ? min(Double(used) / Double(total), 1.0) : 0.0
        let isNearLimit = cloudCapacity?.isNearLimit == true

        return VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text("\(used) / \(total) records")
                    .font(SVDFont.caption())
                    .foregroundColor(.svdOnSurfaceVariant)
                Spacer()
                if isNearLimit {
                    Text("Near limit")
                        .font(SVDFont.caption())
                        .foregroundColor(.svdWarning)
                }
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: SVDRadius.pill, style: .continuous)
                        .fill(Color.svdOnSurfaceVariant.opacity(0.15))
                        .frame(height: 6)
                    RoundedRectangle(cornerRadius: SVDRadius.pill, style: .continuous)
                        .fill(isNearLimit ? Color.svdWarning : Color.svdAccent)
                        .frame(width: max(geo.size.width * fraction, 6), height: 6)
                }
            }
            .frame(height: 6)
        }
    }
}

// MARK: - SignInSheet

/// Bottom sheet presenting Google Sign-In and Sign in with Apple options.
private struct SignInSheet: View {
    let onSignInWithGoogle: () -> Void
    let onSignInWithApple: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                VStack(spacing: 8) {
                    Image(systemName: "icloud.and.arrow.up")
                        .font(.system(size: 48))
                        .foregroundColor(.svdAccent)
                    Text("Back Up Your History")
                        .font(SVDFont.headlineLarge())
                        .foregroundColor(.svdOnSurface)
                    Text("Sign in to sync your download history across devices and keep it safe in the cloud.")
                        .font(SVDFont.bodyMedium())
                        .foregroundColor(.svdOnSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
                .padding(.top, 32)

                VStack(spacing: 12) {
                    signInButton(
                        title: "Continue with Google",
                        systemImage: "globe",
                        action: onSignInWithGoogle
                    )
                    signInWithAppleButton
                }
                .padding(.horizontal, 24)

                Text("By signing in, you agree to our Terms of Service and Privacy Policy.")
                    .font(SVDFont.caption())
                    .foregroundColor(.svdOnSurfaceVariant)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                Spacer()
            }
            .background(Color.svdBg.ignoresSafeArea())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { onDismiss() }
                        .font(SVDFont.labelLarge())
                        .foregroundColor(.svdPrimary)
                }
            }
        }
    }

    private func signInButton(title: String, systemImage: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: systemImage)
                    .font(.system(size: 16, weight: .medium))
                Text(title)
                    .font(SVDFont.labelLarge())
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color.svdSurface)
            .foregroundColor(.svdOnSurface)
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
            .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
        }
    }

    private var signInWithAppleButton: some View {
        Button(action: onSignInWithApple) {
            HStack(spacing: 10) {
                Image(systemName: "applelogo")
                    .font(.system(size: 16, weight: .medium))
                Text("Sign in with Apple")
                    .font(SVDFont.labelLarge())
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color.svdOnSurface)
            .foregroundColor(Color.svdSurface)
            .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
        }
    }
}

// MARK: - Helpers

private extension Int64 {
    var formattedSyncTime: String {
        let date = Date(timeIntervalSince1970: Double(self) / 1000.0)
        let formatter = DateFormatter()
        formatter.doesRelativeDateFormatting = true
        formatter.dateStyle = .none
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}

#Preview {
    ScrollView {
        CloudBackupView(
            cloudState: CloudBackupState(
                isCloudBackupEnabled: true,
                syncStatus: SyncStatusSynced(lastSyncTimestamp: Int64(Date().timeIntervalSince1970 * 1000)),
                restoreState: RestoreStateIdle(),
                isSignedIn: true,
                isSigningIn: false,
                userName: "Jane Doe",
                userPhotoUrl: nil,
                signInError: nil
            ),
            cloudCapacity: CloudCapacity(used: 42, limit: 1000, isNearLimit: false),
            onToggleBackup: {},
            onSignOut: {},
            onRestore: {},
            onTapUpgrade: {}
        )
    }
    .background(Color.svdBg)
}
