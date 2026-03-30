import SwiftUI

/// Typography tokens for Social Video Downloader.
///
/// Preferred fonts: SpaceGrotesk for display/headline, Inter for body.
/// Falls back to system fonts if these are not bundled in the app target.
enum SVDFont {
    // MARK: - Display / Headline (SpaceGrotesk)

    static func displayLarge(size: CGFloat = 32) -> Font {
        customOrSystem(name: "SpaceGrotesk-Bold", size: size, systemDesign: .rounded, weight: .bold)
    }

    static func headlineLarge(size: CGFloat = 24) -> Font {
        customOrSystem(name: "SpaceGrotesk-SemiBold", size: size, systemDesign: .rounded, weight: .semibold)
    }

    static func headlineMedium(size: CGFloat = 20) -> Font {
        customOrSystem(name: "SpaceGrotesk-Medium", size: size, systemDesign: .rounded, weight: .medium)
    }

    // MARK: - Body / Caption (Inter)

    static func bodyLarge(size: CGFloat = 16) -> Font {
        customOrSystem(name: "Inter-Regular", size: size, systemDesign: .default, weight: .regular)
    }

    static func bodyMedium(size: CGFloat = 14) -> Font {
        customOrSystem(name: "Inter-Regular", size: size, systemDesign: .default, weight: .regular)
    }

    static func labelLarge(size: CGFloat = 14) -> Font {
        customOrSystem(name: "Inter-SemiBold", size: size, systemDesign: .default, weight: .semibold)
    }

    static func caption(size: CGFloat = 12) -> Font {
        customOrSystem(name: "Inter-Regular", size: size, systemDesign: .default, weight: .regular)
    }

    // MARK: - Private helper

    private static func customOrSystem(
        name: String,
        size: CGFloat,
        systemDesign: Font.Design,
        weight: Font.Weight
    ) -> Font {
        if UIFont(name: name, size: size) != nil {
            return .custom(name, size: size)
        }
        return .system(size: size, weight: weight, design: systemDesign)
    }
}
