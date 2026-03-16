# Research: UI Redesign — Figma Make Design System

**Date:** 2026-03-16
**Feature branch:** 004-figma-ui-redesign

---

## Table of Contents

1. [Custom ColorScheme extensions in Material 3 Compose](#1-custom-colorscheme-extensions-in-material-3-compose)
2. [ModalBottomSheet in Material 3 Compose](#2-modalbottomsheet-in-material-3-compose)
3. [Theme toggle with persistence](#3-theme-toggle-with-persistence)
4. [Compose animation for state transitions](#4-compose-animation-for-state-transitions)
5. [Horizontal scrollable chips in Compose](#5-horizontal-scrollable-chips-in-compose)

---

### 1. Custom ColorScheme extensions in Material 3 Compose

**Decision**: Use a dedicated `@Immutable` data class for custom semantic colors exposed via `staticCompositionLocalOf`, paired with an extension on `MaterialTheme` for ergonomic access. Do **not** add extension properties directly on `ColorScheme`.

**Rationale**: Material 3 intentionally omits success/warning/info roles from `ColorScheme` — they are not part of the tonal palette spec. Bolting extension properties directly onto `ColorScheme` creates tight coupling to the M3 type and breaks when dynamic color schemes are generated at runtime (e.g., `dynamicDarkColorScheme()`). The `CompositionLocal` approach is fully decoupled: the custom color set travels down the tree independently of the M3 scheme and can be swapped for light/dark in the same `MaterialTheme` wrapper.

`staticCompositionLocalOf` (vs `compositionLocalOf`) is correct here because color objects are replaced wholesale on theme changes — there is no need for fine-grained recomposition tracking.

**Alternatives considered**:
- **Extension properties on `ColorScheme`** — syntactically attractive (`MaterialTheme.colorScheme.success`) but requires storing ambient color per composition and doesn't compose cleanly with `dynamicDarkColorScheme()` which returns a new `ColorScheme` instance with no custom fields.
- **Mapping success onto M3 tertiary/secondary** — zero extra code, but semantics bleed; tertiary is already used for distinct accent roles.
- **MaterialKolor library** — generates full tonal palettes from a seed, useful if you need algorithmically harmonised custom hues, but overkill for a single success role.

**Implementation notes**:

```kotlin
// 1. Define the custom color holder
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

// 2. Create light and dark instances alongside Color.kt tokens
val LightExtendedColors = ExtendedColors(
    success           = Color(0xFF1B8A45),
    onSuccess         = Color(0xFFFFFFFF),
    successContainer  = Color(0xFFB7F0C8),
    onSuccessContainer= Color(0xFF002110),
)
val DarkExtendedColors = ExtendedColors(
    success           = Color(0xFF6DD99A),
    onSuccess         = Color(0xFF003920),
    successContainer  = Color(0xFF005230),
    onSuccessContainer= Color(0xFFB7F0C8),
)

// 3. Expose via CompositionLocal
val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

// 4. Provide in your theme wrapper
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

// 5. Access site — add a convenience accessor on MaterialTheme
val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current

// Usage in a composable:
// Icon(tint = MaterialTheme.extendedColors.success, ...)
```

Key gotchas:
- `staticCompositionLocalOf` throws if accessed outside a provider — always wrap at the root `MaterialTheme` call site.
- Keep `ExtendedColors` `@Immutable` so Compose's skippability analysis treats it as stable.
- Dynamic color (Material You) only replaces `ColorScheme`; your `LocalExtendedColors` value is unaffected — this is the design intent.

---

### 2. ModalBottomSheet in Material 3 Compose

**Decision**: Use `ModalBottomSheet` from `androidx.compose.material3` with `rememberModalBottomSheetState()`, a custom `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`, and a custom `dragHandle` composable for handle bar styling. Spring animation parameters are controlled through the `SheetState`'s internal `swipeableState` — direct `dampingRatio`/`stiffness` override is not exposed in the public API; the closest option is `confirmValueChange` combined with `skipPartiallyExpanded`.

**Rationale**: The M3 `ModalBottomSheet` composable is the standard API since Compose Material3 1.1. It handles scrim, back gesture, accessibility, and state coordination. Shape customisation is straightforward via the `shape` parameter. Handle bar styling is a composable slot (`dragHandle`) so any design is achievable. The internal swipe animation uses a spring spec that as of Material3 1.3+ has been tuned to match M3 motion guidelines — custom damping/stiffness require forking the composable or wrapping it in a `Scaffold`-level approach, which is inadvisable.

**Alternatives considered**:
- **`BottomSheetScaffold`** — persistent sheets, not modal; wrong component for transient overlays.
- **`compose-unstyled` / third-party bottom sheets** — fully customisable including spring params, but adds a dependency and loses M3 accessibility scaffolding.
- **Custom `Popup` + `AnimatedVisibility` with spring** — full control over animation, but requires reimplementing scrim, back handler, and focus semantics.

**Implementation notes**:

```kotlin
// Shape: 24dp top radius, zero bottom (sheet fills to edge)
val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

// Custom drag handle
@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .width(32.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    )
}

// Sheet usage
@Composable
fun FormatPickerSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,          // avoid extra tonal tint on surface
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        dragHandle = { SheetHandle() },
        content = content,
    )
}
```

Spring animation note: `ModalBottomSheet` in Material3 1.3+ uses `SwipeableV2` internally with a spring spec of approximately `dampingRatio = Spring.DampingRatioNoBouncy` and `stiffness = Spring.StiffnessMediumLow`. Matching the Figma design's spring (damping 30, stiffness 280) requires either:
1. Wrapping the show/hide calls in a custom `Animatable<Float>` that drives visibility externally, or
2. Using a third-party sheet (e.g., `compose-unstyled`) where the spring spec is a constructor parameter.

For this project, accept the M3 default spring and align Figma spec to it, rather than forking internal APIs.

`skipPartiallyExpanded = true` removes the half-expanded state, giving a direct collapsed ↔ fully-expanded transition which is closer to the intended design.

---

### 3. Theme toggle with persistence

**Decision**: Use a `ThemeMode` enum (`SYSTEM`, `LIGHT`, `DARK`) persisted to `DataStore<Preferences>` via a `SettingsRepository`. Expose the current mode as a `StateFlow<ThemeMode>` from a `SettingsViewModel` (or from the repository directly, collected in `MainActivity`). The toggle overrides the system theme when set to `LIGHT` or `DARK`; `SYSTEM` defers to `isSystemInDarkTheme()`. Dynamic color (Material You) is a separate boolean preference persisted alongside theme mode.

**Rationale**: DataStore is the current Android-recommended replacement for `SharedPreferences` — it is coroutine-native, type-safe, and handles write conflicts correctly. Storing an enum as a `String` key avoids `Proto DataStore` overhead for a single preference. Modelling the toggle as an enum (not a boolean `isDark`) cleanly supports three states: explicit light, explicit dark, and system-follow. StateFlow collected in the Activity ensures the theme wraps the entire Compose tree from the root and avoids mid-tree provider inconsistencies.

Separating dynamic color from theme mode is important: a user may want light mode with Material You wallpaper colours, or dark mode without dynamic colour.

**Alternatives considered**:
- **`SharedPreferences`** — synchronous reads block the main thread; deprecated for new code.
- **`rememberSaveable` only** — survives process death on older APIs, but is tied to a single composition and does not persist across cold starts.
- **`Proto DataStore`** — stronger typing with generated classes, but requires protobuf schema for a single enum; disproportionate overhead.
- **Delegating entirely to `AppCompatDelegate.setDefaultNightMode()`** — works but bypasses Compose's recomposition model; `isSystemInDarkTheme()` would not reflect the change until the Activity restarts.

**Implementation notes**:

```kotlin
// ThemeMode.kt
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// SettingsRepository.kt (in :core:data)
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")

    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs ->
            ThemeMode.valueOf(prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name)
        }
        .catch { emit(ThemeMode.SYSTEM) }

    val dynamicColor: Flow<Boolean> = dataStore.data
        .map { it[DYNAMIC_COLOR_KEY] ?: true }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }
}

// MainActivity.kt — collect at root before setContent
val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
val dynamicColor by settingsViewModel.dynamicColor.collectAsStateWithLifecycle()

setContent {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK  -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    AppTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        AppNavHost(...)
    }
}
```

Key gotchas:
- Collect with `collectAsStateWithLifecycle()` (not `collectAsState()`) to respect Android lifecycle and avoid collecting in the background.
- Use `SharingStarted.WhileSubscribed(5_000)` in `stateIn()` to survive configuration changes without restarting the upstream Flow.
- Provide a sensible default (`ThemeMode.SYSTEM`) so the first cold start renders correctly before DataStore emits.
- Dynamic color is only meaningful on API 31+; gate it with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` inside `AppTheme`.

---

### 4. Compose animation for state transitions

**Decision**: Use `AnimatedContent` with a custom `transitionSpec` for switching between sealed `UiState` variants (e.g., `Idle`, `Loading`, `Success`, `Error`). Use `animateFloatAsState` with a `tween` or `spring` spec for the gradient progress bar value. `AnimatedVisibility` is used only for elements that appear/disappear within a stable state (e.g., a retry button fading in on `Error`). `Crossfade` is acceptable only for simple fade-only switches with no layout size change.

**Rationale**:

| Composable | Best for |
|---|---|
| `AnimatedContent` | Switching between different layouts/states where content identity changes |
| `AnimatedVisibility` | A single element entering or leaving an otherwise stable layout |
| `Crossfade` | Simple opacity-only swap; no layout change, no directional motion |

In an MVI single-screen flow the entire content region changes shape between states (URL input vs format picker vs progress vs done card). `AnimatedContent` with `targetState = uiState` handles this correctly because it tracks content identity and runs enter/exit simultaneously. `Crossfade` would clip layout size transitions. `AnimatedVisibility` is too narrow — it only controls one element's presence.

For the gradient progress bar, `animateFloatAsState` driving the `progress: Float` parameter is the idiomatic pattern: it re-renders only the Canvas on each frame, not the parent composables, and supports both `tween` (predictable duration) and `spring` (physically-based overshoot).

**Alternatives considered**:
- **`Crossfade` for all state switching** — simpler API but does not animate size changes between states; content jumps to new height without transition.
- **Manual `Transition` + `updateTransition`** — maximum control but verbose; justified only when multiple animated properties must be synchronised frame-accurately across the same transition.
- **`SharedElementTransition`** — powerful for navigating between screens; unnecessary overhead for single-screen state switching.
- **`rememberInfiniteTransition`** for shimmer/loading skeleton** — correct for indeterminate loading shimmer, not for determinate progress.

**Implementation notes**:

```kotlin
// State-level AnimatedContent
AnimatedContent(
    targetState = uiState,
    transitionSpec = {
        val enter = fadeIn(tween(220, delayMillis = 90)) +
                    scaleIn(tween(220, delayMillis = 90), initialScale = 0.95f)
        val exit  = fadeOut(tween(90))
        enter togetherWith exit using SizeTransform(clip = false)
    },
    contentKey = { it::class }, // avoid remeasure when data inside same state changes
    label = "DownloadUiStateTransition",
) { state ->
    when (state) {
        is DownloadUiState.Idle      -> IdleContent(...)
        is DownloadUiState.Loading   -> LoadingContent(...)
        is DownloadUiState.Success   -> SuccessContent(...)
        is DownloadUiState.Error     -> ErrorContent(...)
    }
}

// Gradient progress bar
@Composable
fun GradientProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "DownloadProgress",
    )
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.extendedColors.success,
        )
    )
    Canvas(modifier = modifier.fillMaxWidth().height(4.dp)) {
        // Track
        drawRoundRect(
            color = Color.LightGray.copy(alpha = 0.2f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        // Progress
        drawRoundRect(
            brush = gradientBrush,
            size = Size(size.width * animatedProgress, size.height),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}
```

Key gotchas:
- Always set `contentKey = { state::class }` (or a stable discriminator) in `AnimatedContent`; without it the composable re-enters even when only data inside the same state subtype changes, causing jarring re-animations.
- `SizeTransform(clip = false)` prevents content from clipping during the height transition; `clip = true` causes visible cut-off when moving from a taller state to a shorter one.
- Use `label` on every `animateFloatAsState` / `AnimatedContent` call — it makes the Android Studio Animation Preview and Layout Inspector readable.
- For an indeterminate spinner during `Loading`, prefer `CircularProgressIndicator()` directly rather than animating a custom drawable — it integrates with the M3 motion system.

---

### 5. Horizontal scrollable chips in Compose

**Decision**: Use `LazyRow` for the format chip list, with selection state hoisted to the parent ViewModel as part of `UiState`. Each chip is a `FilterChip` from Material 3. Single-select behaviour is enforced by the ViewModel: selecting a chip emits an `Intent` that sets a new `selectedFormatId` and triggers a `StateFlow` update.

**Rationale**: `LazyRow` composes only visible items, which matters when yt-dlp returns 20–40 format variants (common for YouTube). `Row + horizontalScroll()` composes all items up front, which is fine for ≤ 10 static items but degrades with variable-length format lists. Since formats are data-driven (loaded from `VideoInfo.formats`), `LazyRow` with keyed items is the correct choice.

Selection state belongs in the ViewModel, not in local `remember` state, because:
1. The selected format is consumed by `DownloadVideoUseCase` — it must live at domain/VM scope.
2. Survives configuration changes via `StateFlow`.
3. Makes the composable a pure function of state, enabling Compose Preview and easier testing.

**Alternatives considered**:
- **`Row + horizontalScroll()`** — acceptable for a fixed small chip set (e.g., 3–5 quality tiers), simpler to implement, but not robust against dynamic yt-dlp format lists which can be long.
- **`FlowRow` (Compose 1.5+)** — wraps chips onto multiple lines; useful for tag/filter UIs but not right here — the design specifies a single horizontally scrollable row.
- **Local `remember { mutableStateOf<FormatId?>(null) }` for selection** — fast to write but breaks back-stack restore and isolates the selection from the use case that needs it.

**Implementation notes**:

```kotlin
// In UiState
data class DownloadUiState.FormatSelection(
    val formats: List<VideoFormat>,
    val selectedFormatId: String?,
) : DownloadUiState

// Composable
@Composable
fun FormatChipRow(
    formats: List<VideoFormat>,
    selectedFormatId: String?,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(
            items = formats,
            key = { it.formatId },         // stable key prevents item recomposition on list reorder
        ) { format ->
            FilterChip(
                selected = format.formatId == selectedFormatId,
                onClick = { onFormatSelected(format.formatId) },
                label = { Text(format.displayLabel) },
                leadingIcon = if (format.formatId == selectedFormatId) {
                    { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
            )
        }
    }
}
```

Key gotchas:
- Always supply `key = { format.formatId }` to `items()` — without a stable key, Compose recomposes all chips whenever the list reference changes (e.g., after a ViewModel update), causing visible flicker.
- `contentPadding` on `LazyRow` (not `Modifier.padding`) is important: it adds inset padding that participates in scrolling, so the first/last chip is not clipped against the screen edge during scroll.
- `FilterChipDefaults.IconSize` must be used for the leading icon `Modifier.size()` — the spec mandates 18.dp and using a different size breaks the chip's internal layout measurement.
- For very long format labels (e.g., `"1080p60 HDR (webm, vp9.2+hdr10, 180MB)"`), cap label with `maxLines = 1` + `overflow = TextOverflow.Ellipsis` inside the `FilterChip` label slot to prevent chips from growing disproportionately wide.
