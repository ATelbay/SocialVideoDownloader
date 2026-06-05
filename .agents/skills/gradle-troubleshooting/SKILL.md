---
name: gradle-troubleshooting
description: "Troubleshoot Gradle, Kotlin, KSP, ktlint, Android, and KMP build failures in SocialVideoDownloader. Use for failing builds/tests/lint or dependency/convention-plugin issues."
user-invocable: true
---

# Gradle Troubleshooting

## First steps

1. Re-run the smallest failing task with `--stacktrace` if the error is unclear.
2. Capture only relevant error lines; do not paste huge logs into context.
3. Check recent edits and version catalog/convention plugin changes.
4. For KSP/Room/Hilt failures, verify generated code inputs: annotations, visibility, schema, module dependencies.

## Common commands

```bash
./gradlew tasks --all
./gradlew :app:assembleDebug --stacktrace
./gradlew :feature:download:compileDebugKotlin --stacktrace
./gradlew ktlintCheck --continue
./gradlew ktlintFormat
```

## KMP notes

- Common source sets cannot reference Android/JVM-only APIs.
- iOS compile failures often point to missing expect/actual, cinterop, SKIE, or dependency ABI mismatches.
- Prefer fixing source-set dependencies over adding broad `api` leaks.

## ktlint

- Run `ktlintFormat` only when formatting changes are acceptable.
- Do not combine broad formatting churn with behavior changes unless requested.
