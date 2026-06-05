---
name: compose-ui-guidelines
description: "Compose UI guidelines for SocialVideoDownloader: Material 3, shared design tokens, state hoisting, resources/strings, previews, accessibility, and Android/KMP UI split. Use for screen/component work."
user-invocable: true
---

# Compose UI Guidelines

## Defaults

- Compose-only UI; no XML layouts/fragments.
- Material 3 and existing theme/design tokens first (`core:ui` for Android, `shared:ui` for KMP where applicable).
- Hoist state; UI receives immutable state and lambdas.
- Keep ViewModels out of reusable components; inject them at screen/route level.

## Strings and resources

- No hardcoded user-facing text in composables.
- Use Android string resources for Android-only UI and Compose Multiplatform resources for shared UI when already used in that module.
- Do not invent translations if a specific locale is requested and text is unknown; ask.

## Layout and UX

- Respect system insets and dynamic color where current screen does.
- Loading/error/empty states are first-class.
- Add content descriptions for meaningful images/actions; mark decorative images appropriately.
- Prefer stable keys in lazy lists.

## Previews

- Add/update previews for non-trivial reusable components when the module already uses previews.
- Keep previews deterministic; avoid real network/storage dependencies.
