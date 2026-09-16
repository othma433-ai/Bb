# AL-thmany 3.4.1 — Unified Runtime / Scan / Export Implementation Plan

> **Execution rule:** implement on `feat/full-features-3.4.1`; never modify `main` directly.

**Goal:** Unify the Android environment, WhatsApp target and backend across Join, Scan, Extraction, Publish and Sync; fix Join post-action verification; make Scan strictly read-only; redesign scan exports; normalize the Arabic RTL card system.

**Architecture:** One persisted `UnifiedRuntimeTargetStore` owns the selected Android user/environment, WhatsApp package, and backend preference. Local profile installations operate locally; Shizuku can discover and lock verified remote Android-user WhatsApp targets (including supported Dual Messenger / Work / Secure profiles). All UI controllers consume the same target.

**Tech stack:** Kotlin, Android, Jetpack Compose, AccessibilityService, Shizuku, JUnit4.

**Design:** `docs/superpowers/specs/2026-09-16-unified-runtime-scan-export-redesign.md`

### Task 1 — Canonical runtime target
- [ ] Add `RuntimeBackendPreference`.
- [ ] Add local + Shizuku-verified remote environment targets.
- [ ] Persist package, Android user, environment label, backend.
- [ ] Make the native router honor the shared preference.

### Task 2 — All engines use the same target
- [ ] Physical DB isolation per Android environment; preserve the historic local DB.
- [ ] Extraction uses the shared target.
- [ ] Group Sync uses the shared target.
- [ ] Scan uses the shared target.
- [ ] Publish uses the shared target.
- [ ] Join maps the same target into the proven Sender runtime.
- [ ] Remote Android-user targets require Shizuku.

### Task 3 — Join verification repair
- [ ] Preserve existing action execution.
- [ ] Hold transient `MULTIPLE_POSITIVE_ACTIONS` after a click.
- [ ] Let post-action verification/watchdog determine Joined/Requested/Uncertain.
- [ ] Do not prematurely convert a just-clicked Join into `ACTION_TIMEOUT`.

### Task 4 — Read-only Scan
- [ ] Force `SCAN_ONLY` at the engine boundary.
- [ ] Disable Request/Join actions for every Scan start path.
- [ ] Surface live status/message/confidence in the UI.
- [ ] Preserve name/member/invite evidence without fabricating missing values.

### Task 5 — Member count and exports
- [ ] Add Arabic/English member-count parser.
- [ ] Sort numeric member counts largest → smallest, unknown last.
- [ ] TXT sections per classification.
- [ ] XLSX `All Results` plus separate status sheets.
- [ ] CSV/JSON share the same normalized order.
- [ ] Add unit tests.

### Task 6 — RTL runtime UI
- [ ] Remove legacy Sender target-manager entry.
- [ ] Add one interactive `بيئة التشغيل` card on Home only.
- [ ] Show compact read-only runtime status on Join / Extraction / Scan / Publish.
- [ ] Do not repeat target/backend selectors in operational tabs.
- [ ] Show current environment, Android user, WhatsApp, backend and readiness.
- [ ] Allow Shizuku discovery of remote Dual/Work/Secure targets from the new app.
- [ ] Normalize margins, card padding, radii and vertical rhythm.

### Task 7 — Diagnostics and verification
- [ ] Diagnostics report the unified runtime target/backend.
- [ ] Run the new source contract.
- [ ] Run existing source contracts.
- [ ] `git diff --check`.
- [ ] Push feature branch.
- [ ] GitHub Actions: unit tests, lint, assembleDebug.
- [ ] Do not call the release complete until CI produces a non-empty APK.
