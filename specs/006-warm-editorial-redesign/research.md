# Research: Warm Editorial UI Redesign

**Date**: 2026-03-17
**Feature**: 006-warm-editorial-redesign

## Research Tasks

No NEEDS CLARIFICATION items in Technical Context — all technical decisions are straightforward for this visual-only redesign. Research below covers the key implementation decisions.

---

### R1: Space Grotesk Font — Weight Availability

**Decision**: Bundle only `space_grotesk_bold.ttf` (weight 700).

**Rationale**: The spec uses Space Grotesk exclusively at weight 700 (bold) for display headline (28sp), progress percentage (62sp), and any other headline contexts. No regular, medium, or semibold weights are specified. Space Grotesk is available as an open-source font (SIL Open Font License) from Google Fonts.

**Alternatives considered**:
- Bundle multiple weights (Regular, Medium, Bold): Rejected — spec only uses 700. Adding unused weights increases APK size unnecessarily.
- Use Google Fonts downloadable provider: Rejected — requires network access and adds complexity. Bundled font is more reliable for an offline-first utility.

---

### R2: Light Color Scheme — M3 Mapping Strategy

**Decision**: Use `lightColorScheme()` with custom token overrides. Extend `ExtendedColors` data class with new tokens not mappable to M3 roles.

**Rationale**: The spec defines 22 color tokens. Some map naturally to M3 roles (background, surface, primary, error, onBackground, onSurface). Others are custom (surfaceAlt, surfaceStrong, primaryStrong, primarySoft, accent, accentSoft, borderStrong, warning, shadow). These go into `ExtendedColors` accessed via `LocalExtendedColors`.

**M3 Role Mapping**:
| Spec Token | M3 Role |
|-----------|---------|
| background (#F6F3EC) | `background` |
| surface (#FFFDFC) | `surface` |
| primary (#F26B3A) | `primary` |
| foreground (#1F2328) | `onBackground`, `onSurface` |
| error (#D9534F) | `error` |
| errorSoft (#FDE5E3) | `errorContainer` |

**Extended (non-M3)**:
surfaceAlt, surfaceStrong, card, primaryStrong, primarySoft, warning, accent, accentSoft, mutedForeground, subtleForeground, border, borderStrong, success, successSoft, shadow

**Alternatives considered**:
- Map everything to M3 roles (stretching role semantics): Rejected — leads to confusing code where `tertiary` means "accent teal" and `secondary` means "warning yellow".
- Use only extended colors, skip M3 mapping: Rejected — loses M3 component defaults (e.g., `CircularProgressIndicator` auto-uses `primary`).

---

### R3: Shape System — Pill Radius Value

**Decision**: Use `RoundedCornerShape(999.dp)` for pill shapes.

**Rationale**: The spec calls for 999dp radius for chips, nav bar outer, and status chips. In Compose, `RoundedCornerShape` with a value larger than half the component height produces a perfect pill shape. 999dp is safely larger than any component height in the app.

**Alternatives considered**:
- `CircleShape`: Only works for equal width/height — not suitable for rectangular pills.
- `RoundedCornerShape(50)` (percent): Works but 999dp is more explicit and matches the spec literal value.

---

### R4: PlusJakartaSans Font Files — Removal

**Decision**: Remove all three PlusJakartaSans TTF files from `res/font/`. Replace all references in Type.kt with SpaceGrotesk.

**Rationale**: PlusJakartaSans is only used in Type.kt to define the `PlusJakartaSans` FontFamily, which is then used in the M3 Typography scale for display/headline/title styles. The redesign replaces all these with SpaceGrotesk. No other code references these font files directly.

**Risk**: Zero — font files are only referenced from Type.kt via `R.font.*` resource IDs. After updating Type.kt, the old files become unused.

---

### R5: Library Tab — Navigation Architecture

**Decision**: Add a `LibraryRoute` data object and a composable destination in `AppNavHost`. The composable shows a centered placeholder message. Tab index mapping: Download=0, Library=1, History=2.

**Rationale**: The spec requires a Library tab that "navigates nowhere or shows empty state." The simplest approach is a real navigation destination with a placeholder composable. This avoids special-casing "do nothing" in the nav bar click handler and keeps the navigation architecture consistent.

**Alternatives considered**:
- No-op click handler (tab does nothing): Rejected — confusing UX, no visual feedback of tab selection.
- Separate `:feature:library` module: Rejected — over-engineering for a placeholder. Keep in `:app` navigation until the feature gets real content.

---

### R6: System Bar Styling — Light Theme

**Decision**: Update `enableEdgeToEdge()` call to use light system bars (dark icons on transparent background) to match the warm light theme.

**Rationale**: The current dark theme uses light icons on dark status/navigation bars. With the light background (#F6F3EC), dark status bar icons are needed for readability. Compose's `enableEdgeToEdge()` with default parameters auto-detects, but explicit styling ensures consistency.

---

### R7: Top Bar — Composable Redesign

**Decision**: Redesign `SvdTopBar` to accept `title: String`, `actionLabel: String`, and `onActionClick: (() -> Unit)?` parameters. Remove the back-button-only design. Each screen passes its own title/action pair.

**Rationale**: The spec defines the top bar as a card-like row with title on the left and action chip on the right. This is a fundamentally different layout from the current back-button + title design. The action chip is screen-specific (Tips, Back, Hide, Filter).

**Alternatives considered**:
- Keep existing SvdTopBar + add new composable: Rejected — no screen uses the old design anymore.
- Per-screen inline top bars: Rejected — violates DRY. The component is identical across screens, only content differs.
