import SwiftUI
import shared_feature_history

// MARK: - UpgradeView

/// Full-screen upgrade sheet presenting the Free vs Premium tier comparison.
///
/// Purchase flow delegates to [SharedHistoryViewModel] via [HistoryIntent.TapUpgrade].
/// Actual StoreKit 2 purchase is initiated in Kotlin [StoreKitBillingRepository]
/// which bridges to Swift via [PlatformBillingProvider].
///
/// TODO: Wire real purchase confirmation/error feedback once StoreKit 2
///       is fully integrated via App Store Connect product configuration.
struct UpgradeView: View {

    let onPurchase: () -> Void
    let onRestorePurchases: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 32) {
                    headerSection
                    tierComparison
                    purchaseSection
                    legalText
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
            .background(Color.svdBg.ignoresSafeArea())
            .navigationTitle("Upgrade to Premium")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") { onDismiss() }
                        .font(SVDFont.labelLarge())
                        .foregroundColor(.svdPrimary)
                }
            }
        }
    }

    // MARK: Header

    private var headerSection: some View {
        VStack(spacing: 12) {
            Image(systemName: "star.circle.fill")
                .font(.system(size: 56))
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color.svdPrimary, Color.svdWarning],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            Text("Unlock Premium")
                .font(SVDFont.headlineLarge())
                .foregroundColor(.svdOnSurface)
            Text("Back up more downloads and access your history on all your devices.")
                .font(SVDFont.bodyMedium())
                .foregroundColor(.svdOnSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 8)
        }
        .padding(.top, 16)
    }

    // MARK: Tier comparison

    private var tierComparison: some View {
        HStack(alignment: .top, spacing: 12) {
            TierCard(
                title: "Free",
                price: "Free",
                features: [
                    TierFeature(icon: "clock", text: "1,000 download records"),
                    TierFeature(icon: "icloud", text: "Cloud backup"),
                    TierFeature(icon: "arrow.counterclockwise", text: "Restore history"),
                ],
                isHighlighted: false
            )
            TierCard(
                title: "Premium",
                price: "$2.99 / mo",
                features: [
                    TierFeature(icon: "clock", text: "10,000 download records"),
                    TierFeature(icon: "icloud.and.arrow.up", text: "Priority cloud sync"),
                    TierFeature(icon: "arrow.counterclockwise", text: "Full history restore"),
                    TierFeature(icon: "star", text: "Future premium features"),
                ],
                isHighlighted: true
            )
        }
    }

    // MARK: Purchase buttons

    private var purchaseSection: some View {
        VStack(spacing: 12) {
            Button(action: onPurchase) {
                HStack(spacing: 8) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 14, weight: .semibold))
                    Text("Subscribe — $2.99 / month")
                        .font(SVDFont.labelLarge())
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(
                    LinearGradient(
                        colors: [Color.svdPrimary, Color.svdPrimary.opacity(0.85)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .foregroundColor(.white)
                .clipShape(RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous))
                .shadow(color: Color.svdPrimary.opacity(0.35), radius: 8, x: 0, y: 4)
            }

            Button(action: onRestorePurchases) {
                Text("Restore Purchases")
                    .font(SVDFont.labelLarge())
                    .foregroundColor(.svdOnSurfaceVariant)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
        }
    }

    // MARK: Legal

    private var legalText: some View {
        VStack(spacing: 6) {
            Text("Subscription renews automatically at $2.99/month. Cancel any time in App Store settings.")
                .font(SVDFont.caption())
                .foregroundColor(.svdOnSurfaceVariant)
                .multilineTextAlignment(.center)
            HStack(spacing: 16) {
                Link("Privacy Policy", destination: URL(string: "https://socialvideodownloader.com/privacy")!)
                    .font(SVDFont.caption())
                    .foregroundColor(.svdAccent)
                Link("Terms of Service", destination: URL(string: "https://socialvideodownloader.com/terms")!)
                    .font(SVDFont.caption())
                    .foregroundColor(.svdAccent)
            }
        }
        .padding(.horizontal, 8)
    }
}

// MARK: - TierFeature model

private struct TierFeature {
    let icon: String
    let text: String
}

// MARK: - TierCard

private struct TierCard: View {
    let title: String
    let price: String
    let features: [TierFeature]
    let isHighlighted: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(SVDFont.headlineMedium())
                    .foregroundColor(isHighlighted ? .svdPrimary : .svdOnSurface)
                Text(price)
                    .font(SVDFont.bodyLarge())
                    .foregroundColor(isHighlighted ? .svdOnSurface : .svdOnSurfaceVariant)
            }

            Divider()
                .background(
                    isHighlighted
                        ? Color.svdPrimary.opacity(0.25)
                        : Color.svdOnSurfaceVariant.opacity(0.2)
                )

            VStack(alignment: .leading, spacing: 8) {
                ForEach(features, id: \.text) { feature in
                    HStack(spacing: 8) {
                        Image(systemName: feature.icon)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(isHighlighted ? .svdAccent : .svdOnSurfaceVariant)
                            .frame(width: 18, alignment: .center)
                        Text(feature.text)
                            .font(SVDFont.bodyMedium())
                            .foregroundColor(.svdOnSurface)
                    }
                }
            }

            if isHighlighted {
                Text("Most popular")
                    .font(SVDFont.caption())
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color.svdPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: SVDRadius.small, style: .continuous))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .background(Color.svdSurface)
        .clipShape(RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous)
                .stroke(
                    isHighlighted ? Color.svdPrimary.opacity(0.5) : Color.svdOnSurfaceVariant.opacity(0.15),
                    lineWidth: isHighlighted ? 2 : 1
                )
        )
        .shadow(color: .black.opacity(isHighlighted ? 0.08 : 0.04), radius: isHighlighted ? 8 : 4, x: 0, y: 2)
    }
}

// MARK: - Preview

#Preview {
    UpgradeView(
        onPurchase: {},
        onRestorePurchases: {},
        onDismiss: {}
    )
}
