# Quickstart: Compose Multiplatform Migration

**Branch**: `013-compose-multiplatform-migration` | **Date**: 2026-03-31

## Prerequisites

- Android Studio with KMP plugin (Ladybug or newer)
- Xcode 16.x (for iOS builds)
- JDK 17+
- CocoaPods (`gem install cocoapods`)

## New Dependencies

Add to `gradle/libs.versions.toml`:

```toml
[versions]
compose-multiplatform = "1.9.3"
navigation-compose-multiplatform = "2.9.1"
coil3 = "3.1.0"

[libraries]
coil3-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil3" }
coil3-network-ktor3 = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil3" }
navigation-compose-multiplatform = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation-compose-multiplatform" }

[plugins]
jetbrainsCompose = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
# kotlin.plugin.compose already exists at Kotlin version (2.2.10)
```

## New Convention Plugin: `svd.kmp.compose`

```kotlin
// build-logic/convention/src/main/kotlin/KmpComposeConventionPlugin.kt
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                val compose = extensions.getByType<ComposeExtension>().dependencies
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.resources)
            }
        }
    }
}
```

Register in `build-logic/convention/build.gradle.kts` and `gradlePlugin` block.

## Build Commands

```bash
# Android debug build (unchanged)
./gradlew assembleDebug

# iOS framework build (verify CMP compiles for iOS)
./gradlew :shared:ui:compileKotlinIosSimulatorArm64

# Full iOS build (via Xcode or command line)
xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16'

# ktlint (exclude iOS native compilation as before)
./gradlew ktlintCheck -x compileKotlinIosArm64 -x compileKotlinIosSimulatorArm64
```

## Key Patterns

### Shared composable consuming shared ViewModel

```kotlin
// shared/feature-library/src/commonMain/.../ui/LibraryScreen.kt
@Composable
fun LibraryScreen(viewModel: SharedLibraryViewModel) {
    val state by viewModel.uiState.collectAsState()
    // Render based on state...
}
```

### Android thin wrapper with Hilt bridge

```kotlin
// feature/library/src/main/.../ui/LibraryScreen.kt
@Composable
fun LibraryScreenRoute(viewModel: LibraryViewModel = hiltViewModel()) {
    LibraryScreen(viewModel = viewModel.shared)
}
```

### iOS entry point

```swift
// iosApp/iosApp/App.swift
@main
struct SocialVideoDownloaderApp: App {
    init() { KoinInitializerKt.doInitKoin() }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        SharedAppKt.SharedAppViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

### Platform expect/actual

```kotlin
// commonMain
expect fun openFile(uri: String)

// androidMain
actual fun openFile(uri: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(uri) }
    context.startActivity(intent)
}

// iosMain
actual fun openFile(uri: String) {
    NSURL.URLWithString(uri)?.let { UIApplication.sharedApplication.openURL(it) }
}
```

## Module Dependency Graph (after migration)

```
:app
├── :feature:download → :shared:feature-download, :core:ui
├── :feature:history  → :shared:feature-history, :core:ui
├── :feature:library  → :shared:feature-library, :core:ui
└── :core:*

:shared:di (iOS entry point)
├── :shared:feature-download
├── :shared:feature-history
├── :shared:feature-library
├── :shared:data
└── :shared:network

:shared:feature-* (each)
├── :shared:ui          # Shared theme + components
├── :core:domain        # Use cases, domain models
└── koin-core

:shared:ui
└── compose.* (CMP runtime, foundation, material3, resources)
```
