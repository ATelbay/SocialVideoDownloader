import SwiftUI

/// Corner radius tokens.
enum SVDRadius {
    /// Large card / sheet corners (22 pt)
    static let card: CGFloat = 22
    /// Button / control corners (18 pt)
    static let control: CGFloat = 18
    /// Pill / chip corners — effectively circular for small elements (999 pt)
    static let pill: CGFloat = 999
    /// Small inline elements such as badges (8 pt)
    static let small: CGFloat = 8
}

/// Reusable SwiftUI shapes derived from the corner radius tokens.
struct CardShape: Shape {
    func path(in rect: CGRect) -> Path {
        RoundedRectangle(cornerRadius: SVDRadius.card, style: .continuous).path(in: rect)
    }
}

struct ControlShape: Shape {
    func path(in rect: CGRect) -> Path {
        RoundedRectangle(cornerRadius: SVDRadius.control, style: .continuous).path(in: rect)
    }
}
