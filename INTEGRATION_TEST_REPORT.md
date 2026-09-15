# AL-thmany Sender + Extractor Integration Test Report

## Integrated feature set
- Extraction engine and checkpoint database
- Invite-link scan engine
- Publish engine
- Runtime operation coordinator
- Extractor Accessibility runtime
- Extractor Shizuku runtime and AIDL user service
- Compose workspace UI launched from the Sender toolbar menu

## Coordination
Extractor operations refuse to start while the Sender invitation batch is active. This prevents two active engines from controlling the WhatsApp UI simultaneously.

## Validation completed in this workspace
1. `python3 scripts/validate_source.py` — PASS
2. `python3 scripts/validate_integrated_extractor.py` — PASS
3. `bash scripts/run_integrated_extractor_pure_checks.sh` — PASS
   - extraction/profile pure checks — PASS
   - scan pure checks — PASS
   - publish pure checks — PASS
   - runtime coordinator pure checks — PASS
   - profile-control pure checks — PASS
4. `bash scripts/run_pure_kotlin_regressions.sh` — PASS on the merged tree before final packaging
5. Sender runtime micro-benchmark — PASS
6. Android resource/manifest XML parsing — 104 XML files, 0 parse errors

## Full Android build
A full `assembleDebug` could not be executed in the current sandbox because Gradle and the Android SDK are not installed and outbound downloads are unavailable. The included GitHub Actions workflow installs Android SDK/Gradle and now also runs the integrated Extractor validators before building the debug APK.

## Entry point
Open Sender -> toolbar menu -> `الاستخراج والفحص والنشر`.
