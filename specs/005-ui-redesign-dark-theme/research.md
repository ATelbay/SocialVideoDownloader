# Research: UI Redesign with Custom Dark Theme

**Date**: 2026-03-16 | **Branch**: `005-ui-redesign-dark-theme`

## R1: Custom Font Bundling Strategy

**Decision**: Bundle Plus Jakarta Sans and Inter as `.ttf` files in `core/ui/src/main/res/font/`

**Rationale**: Bundling fonts as resources (not Google Fonts provider) ensures offline availability and zero network latency on first launch. The app is a personal utility with no APK size budget pressure. Both fonts are open-source (OFL license). Google Fonts provider requires network on first cold start, which conflicts with the on-device architecture principle.

**Alternatives considered**:
- `ui-text-google-fonts` dependency: Requires network on cold start, adds complexity with `GoogleFont.Provider`, font fallback handling. Rejected for offline-first design.
- System fonts only: Would not achieve the branded look required by the designs. Rejected.

**Font weights needed**:
- Plus Jakarta Sans: 600 (SemiBold), 700 (Bold), 800 (ExtraBold)
- Inter: Regular (400), 500 (Medium), 600 (SemiBold), 700 (Bold)

## R2: Theme Architecture — M3 Integration vs Full Custom

**Decision**: Keep MaterialTheme as the wrapper but supply a fully custom `ColorScheme` with SVD tokens. Remove Dynamic Color and light theme branches entirely.

**Rationale**: MaterialTheme still provides the composition locals that M3 components (Scaffold, Surface, etc.) rely on. By mapping SVD tokens to M3 color roles, we preserve compatibility with any M3 widgets we use while achieving the exact custom palette. This avoids rewriting every component from scratch.

**Mapping**: SVD tokens → M3 ColorScheme roles:
| SVD Token | M3 Role |
|-----------|---------|
| svd-bg (#0F0D15) | background |
| svd-surface (#1A1726) | surface, surfaceContainer |
| svd-surface-elevated (#241F33) | surfaceContainerHigh |
| svd-surface-bright (#2E2844) | surfaceContainerHighest |
| svd-primary (#8B5CF6) | primary |
| svd-primary-container (#2D2150) | primaryContainer |
| svd-text (#FFFFFF) | onBackground, onSurface |
| svd-text-secondary (#A09BB0) | onSurfaceVariant |
| svd-text-tertiary (#6B6580) | outline |
| svd-border (#2E2844) | outlineVariant |
| svd-error (#FF6B6B) | error |
| svd-error-container (#3D1B1B) | errorContainer |
| svd-success (#6ECF83) | ExtendedColors.success |
| svd-success-container (#1B3D25) | ExtendedColors.successContainer |

**Alternatives considered**:
- Fully custom theme without MaterialTheme: Would break all M3 component defaults (Scaffold, Surface, etc.), requiring manual color on every component. Rejected — too much work for no benefit.
- Keep Dynamic Color with SVD override: Confusing hybrid. Rejected — the designs mandate a single branded palette.

## R3: Custom Navigation Bar Implementation

**Decision**: Replace M3 `NavigationBar` in `MainActivity.kt` with a custom `PillNavigationBar` composable in `core/ui/components/`. The composable accepts selected index and onSelect callback.

**Rationale**: M3 `NavigationBar` cannot achieve the pill shape (full corner radius on container, individually rounded active tab pill, custom internal padding). A custom Row-based composable with `clip()` and `background()` modifiers is simpler than fighting M3's opinionated styling.

**Alternatives considered**:
- Heavily customized M3 NavigationBar: M3 NavigationBar applies its own padding, indicator shape, and color logic. Overriding all of these is fragile and version-dependent. Rejected.
- Third-party library: No established library for this specific pill pattern. Rejected — unnecessary dependency.

## R4: Gradient Button Implementation

**Decision**: Create a `GradientButton` composable in `core/ui/components/` that uses `Modifier.background(Brush.verticalGradient(...))` inside a Box with centered content Row.

**Rationale**: M3 `Button` does not support gradient fills natively. A custom composable wrapping a Box with gradient background is the standard Compose pattern for gradient buttons.

**Alternatives considered**:
- M3 Button with custom ContainerColor: Only supports solid colors, not gradients. Rejected.
- Custom ButtonDefaults: Would require deep M3 internal override. Rejected — fragile.

## R5: Typography Architecture

**Decision**: Define two `FontFamily` objects (`PlusJakartaSans` and `Inter`) and create a new `Typography` instance mapping them to M3 type roles. Plus Jakarta Sans maps to display/headline/title roles, Inter maps to body/label roles.

**Rationale**: M3 Typography slots align well with the two-font design system. The current Typography already uses custom sizes/weights/spacing — we just need to swap `FontFamily.Default` to the correct family per slot.

## R6: Existing String Resources

**Decision**: Reuse existing string resources wherever possible. Add new strings only for new UI elements (e.g., "SUPPORTED PLATFORMS", "DOWNLOAD", "HISTORY" nav labels, "Extract Video", stats labels).

**Rationale**: The app already has 80+ string resources covering most UI text. The redesign changes visual presentation, not copy, for most elements.

**New strings needed**: ~15 new entries for new UI elements and label variations.

## R7: SettingsViewModel / Theme Toggle Removal

**Decision**: Remove `SettingsViewModel`, `ThemeMode` enum, and theme toggle button. Simplify `SocialVideoDownloaderTheme` to always apply the SVD dark palette.

**Rationale**: With a single branded dark theme, the theme toggle has no purpose. Removing it simplifies the codebase and eliminates the `isDarkTheme`/`onToggleTheme` parameters threading through navigation.

**Alternatives considered**:
- Keep SettingsViewModel for future use: YAGNI principle (Constitution VII). Rejected.
