# AL-thmany 4.0.0 Stability Verification Report

**Verification date:** 2026-09-16  
**Release:** 4.0.0 (`versionCode 400`)  
**Scope:** Stability-focused source release for the integrated Sender + Extractor Android application.

## Executive status

The source-level and pure-Kotlin verification suite completed successfully. The legacy regression suite also compiled and ran successfully when executed independently. Android APK compilation was **not executable in the current local environment** because neither Gradle nor an Android SDK is installed here; therefore this report does not claim a locally built APK.

The repository GitHub Actions workflow is configured to provision JDK 17, Android SDK 36, Gradle 8.13, then run unit tests, Android Lint, and `assembleDebug`, and to upload the resulting debug APK when that CI job succeeds.

## Verification matrix

| Check | Command | Status | Evidence / note |
|---|---|---|---|
| Source/resource contract | `python3 scripts/validate_source.py` | PASS | Version 4.0.0 / 400 recognized; source/resource assertions passed. |
| Integrated Extractor contract | `python3 scripts/validate_integrated_extractor.py` | PASS | Sender/Extractor ownership, controller guards, group-title policy integration all passed. |
| Integrated pure engine checks | `bash scripts/run_integrated_extractor_pure_checks.sh` | PASS | Extraction/scan/publish/runtime/profile pure checks passed. |
| 4.0 focused stability suite | `SKIP_LEGACY=1 bash scripts/run_stability_4_0_checks.sh` | PASS | Ownership, Sender guard, group-title policy and XML parse passed. |
| XML parse | Included in stability suite | PASS | 104 XML files parsed successfully. |
| Legacy Kotlin regression compile | `bash scripts/run_pure_kotlin_regressions.sh` | PASS | `LEGACY_COMPILE_PASS`. |
| Legacy Kotlin regression run | `bash scripts/run_pure_kotlin_regressions.sh` | PASS | `PURE KOTLIN REGRESSIONS PASSED` and `LEGACY_RUN_PASS`. |
| Pure Kotlin domain compile | `bash scripts/compile_pure_kotlin.sh` | PASS | `PURE KOTLIN COMPILE PASSED`. |
| Runtime micro-benchmark sanity | `bash scripts/run_runtime_benchmarks.sh` | PASS | 5,000 unique + 500 duplicate links parsed in 104.44 ms; 20,000 fingerprints in 132.67 ms; 100,000 idempotency checks in 24.02 ms; sanity checks passed. |
| Patch whitespace validation | `git diff --check` | PASS | No whitespace errors reported. |
| Android unit tests | `gradle ... testDebugUnitTest` | NOT EXECUTABLE LOCALLY | `gradle` is not installed in this environment. |
| Android Lint | `gradle ... lintDebug` | NOT EXECUTABLE LOCALLY | Gradle and Android SDK are unavailable locally. |
| Android debug APK build | `gradle ... assembleDebug` | NOT EXECUTABLE LOCALLY | No local Gradle/Android SDK; no APK build-success claim is made. |

## Verified 4.0 stability changes

1. **Single WhatsApp UI owner** — Sender, Extraction, Scan and Publish share one process-local ownership coordinator, preventing simultaneous UI automation ownership.
2. **Bidirectional collision protection** — Extractor entry points refuse to start while a persisted Sender run is active, and Sender cannot take ownership from another active operation.
3. **Service recreation recovery** — Accessibility/Shizuku Sender services can idempotently reacquire Sender ownership only when it is safe to do so.
4. **Group discovery hardening** — WhatsApp control rows such as Manage groups, New group, Archived, Communities, Search and Arabic equivalents are rejected by a pure, regression-tested title policy.
5. **Regression diagnostics** — the stability runner and legacy runner expose named phases so environment timeouts can be separated from code failures.
6. **CI build verification path** — GitHub Actions performs source checks, pure regressions, unit tests, lint and Android debug build on a provisioned Android toolchain.

## Environment used for local verification

- Java: OpenJDK 21.0.11
- Kotlin CLI: 1.9.0
- Gradle command: unavailable
- `ANDROID_HOME`: unset
- `ANDROID_SDK_ROOT`: unset

The Android project itself targets JVM 17 in its Gradle configuration; GitHub Actions explicitly provisions JDK 17 for the Android build.

## Important limitation

A source package can be verified extensively without an Android SDK, but that is not equivalent to a successful Android application build or device test. A final APK should be treated as build-verified only after the configured CI job (or another Android SDK 36 + Gradle 8.13 environment) completes `testDebugUnitTest`, `lintDebug`, and `assembleDebug` successfully. Device-level WhatsApp behavior also requires testing on the intended Android/WhatsApp variants because Accessibility UI trees can change between WhatsApp releases and Samsung profile modes.
