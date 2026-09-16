# AL-thmany 3.4.1 Performance / Unified Targets / RTL UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make AL-thmany 3.4.1 responsive during normal use, expose every verified WhatsApp target through one selector, and replace the inconsistent 9–24sp screen styling with one professional Arabic RTL design system.

**Architecture:** Keep the proven Join/Extraction/Scan/Publish engines. Move expensive package/Shizuku/SQLite work out of high-frequency UI state paths, make runtime-target discovery cached and explicit, and reduce root Compose subscriptions so screens recompose only for state they render. Use a single reusable WhatsApp selector backed by the existing local registry plus cached remote Shizuku targets.

**Tech Stack:** Kotlin 2.3.21, Android/Jetpack Compose Material3, Kotlin Coroutines/StateFlow, SQLiteOpenHelper, Shizuku, AccessibilityService, JUnit4, GitHub Actions (JDK 17 / Gradle 8.13 / Android 36).

**Spec:** `docs/superpowers/specs/2026-09-16-performance-targets-rtl-design.md`

## Global Constraints

- Keep visible product version **3.4.1**.
- Do not replace the original Sender Join engine.
- Scan remains read-only and must not click Join/Request.
- Do not bypass Android profile or Samsung Knox boundaries.
- Local and remote environment database isolation remains intact.
- Every operational feature uses the same selected target.
- All actual verified WhatsApp targets are visible through one selector.
- No external font files are bundled.
- GitHub Actions must pass `testDebugUnitTest`, `lintDebug`, and `assembleDebug` before the release is called ready.

---

### Task 1: Remove expensive target discovery from the hot state path

**Files:**
- Modify: `app/src/main/java/com/althmany/extractor/profile/UnifiedRuntimeTarget.kt`
- Modify: `app/src/main/java/com/althmany/extractor/engine/ExtractionController.kt`
- Modify: `app/src/main/java/com/althmany/extractor/ui/AppViewModel.kt`
- Test/contract: `scripts/validate_performance_targets_ui_v341.py`

**Interfaces:**
- Consumes: `WhatsAppInstanceRegistry.available(context, forceRefresh)`
- Produces: cached `UnifiedRuntimeSnapshot`; explicit `refreshRuntimeEnvironment(forceDiscovery: Boolean = false)`

- [ ] **Step 1: Write a source-contract test that rejects force refresh in normal resolution.**

Require:
```text
UnifiedRuntimeTargetStore.resolve -> WhatsAppInstanceRegistry.available(... forceRefresh = false)
ExtractionController.refreshRuntimeEnvironment(forceDiscovery: Boolean = false)
engine state collector -> selectedWhatsAppPackage + distinctUntilChanged()
```

- [ ] **Step 2: Run the contract and verify RED on the current branch.**

Run:
```bash
python3 scripts/validate_performance_targets_ui_v341.py
```

Expected: FAIL on hot-path discovery rules.

- [ ] **Step 3: Make runtime resolution cache-first.**

Change normal `resolve()` and normal runtime refresh to use the registry cache. Allow `forceDiscovery=true` only for explicit discovery/refresh actions.

- [ ] **Step 4: Replace full engine-state runtime resolution collector.**

Use:
```kotlin
engineState
    .map { it.selectedWhatsAppPackage }
    .distinctUntilChanged()
    .collect { packageName ->
        _runtimeTarget.value =
            UnifiedRuntimeTargetStore.resolve(getApplication(), packageName)
    }
```

- [ ] **Step 5: Run source contracts.**

Expected: PASS.

### Task 2: Move SQLite reads away from the Compose/main-thread hot path

**Files:**
- Modify: `app/src/main/java/com/althmany/extractor/ui/AppViewModel.kt`
- Test/contract: `scripts/validate_performance_targets_ui_v341.py`

**Interfaces:**
- Produces helper:
```kotlin
private suspend fun <T> queryIo(block: () -> T): T =
    withContext(Dispatchers.IO) { block() }
```

- [ ] **Step 1:** Contract rejects direct `repo.scanItems()` / `repo.publishItems()` refreshes in high-frequency collectors without `Dispatchers.IO`.
- [ ] **Step 2:** Remove `stats.hashCode()` as a reason to reload the entire Scan table.
- [ ] **Step 3:** Reload Scan rows only when current index changes or the engine reaches a terminal status.
- [ ] **Step 4:** Reload Publish rows only when current index/run/status meaningfully changes.
- [ ] **Step 5:** Make `refresh()` bulk database reads run on IO, then publish values back to StateFlows.
- [ ] **Step 6:** Run contracts.

### Task 3: Reduce full-root Compose recomposition

**Files:**
- Modify: `app/src/main/java/com/althmany/extractor/MainActivity.kt`

**Interfaces:**
- Export callbacks read current `viewModel.<list>.value`.
- Heavy lists are collected only inside the `when(screen)` branch that renders them.

- [ ] **Step 1:** Remove top-level collection of groups/links/logs/scanItems/publishItems.
- [ ] **Step 2:** Use snapshot values in CreateDocument callbacks.
- [ ] **Step 3:** Collect each list only in the screen branch that needs it.
- [ ] **Step 4:** Reduce Accessibility connection polling from 20 × 400ms heavy refreshes to bounded cached refreshes.
- [ ] **Step 5:** Validate source contract.

### Task 4: One selector for every WhatsApp copy

**Files:**
- Modify: `app/src/main/java/com/althmany/extractor/ui/UnifiedRuntimeCard.kt`
- Modify: `app/src/main/java/com/althmany/extractor/ui/AppViewModel.kt`
- Modify: `app/src/main/java/com/althmany/extractor/ui/WorkspaceV341Screens.kt`
- Modify: `app/src/main/java/com/althmany/extractor/ui/ProfessionalJoinScanScreens.kt`

**Interfaces:**
- Local targets: `engine.availableWhatsApp`
- Remote targets: `remoteRuntimeTargets`
- Local selection: `setTargetWhatsApp(packageName)`
- Remote selection: `setRemoteRuntimeTarget(target)`

- [ ] **Step 1:** Replace the separate local/remote selection presentation with one “جميع نسخ واتساب” target carousel.
- [ ] **Step 2:** Keep keys unique across local and remote targets.
- [ ] **Step 3:** Add one explicit “تحديث النسخ” action that refreshes local discovery and Shizuku remote discovery asynchronously.
- [ ] **Step 4:** Auto-discover remote targets once in the background when Shizuku is ready; never block first-frame rendering.
- [ ] **Step 5:** Operational screens keep a compact selected-target strip; the home/runtime area owns the full selector.
- [ ] **Step 6:** Validate that Personal, Business, Clone/Discovered and remote targets are not filtered out.

### Task 5: Professional Arabic typography and component rhythm

**Files:**
- Modify: `app/src/main/java/com/althmany/extractor/ui/Theme.kt`
- Modify: `app/src/main/java/com/althmany/extractor/ui/WorkspaceV341Screens.kt`
- Modify: `app/src/main/java/com/althmany/extractor/ui/ProfessionalJoinScanScreens.kt`
- Modify: `app/src/main/java/com/althmany/extractor/ui/UnifiedRuntimeCard.kt`

**Interfaces:**
- Material3 `MaterialTheme.typography`

- [ ] **Step 1:** Replace bare `Typography()` with custom Arabic-safe SansSerif typography.
- [ ] **Step 2:** Use 24–28sp for page headings, 18–20sp for sections, 14–16sp body, 12–14sp supporting/labels.
- [ ] **Step 3:** Remove 9sp primary/action text.
- [ ] **Step 4:** Standardize card radius 18–20dp, card padding 16dp, section spacing 12–16dp, action height >=48dp.
- [ ] **Step 5:** Preserve monospace only for URLs/package names, with readable >=12sp size.
- [ ] **Step 6:** Run source contract.

### Task 6: Reduce Join UI update churn without changing the Join engine

**Files:**
- Modify: `app/src/main/java/com/althmany/extractor/join/OriginalJoinCoordinator.kt`

**Interfaces:**
- `refresh()` remains the UI snapshot bridge.

- [ ] **Step 1:** Increase the fallback polling interval from 350ms to 700ms.
- [ ] **Step 2:** Keep event-driven refreshes (`handleEvent`) immediate.
- [ ] **Step 3:** Confirm no automation timing constant is changed.

### Task 7: Full verification and delivery

**Files:**
- Create: `scripts/validate_performance_targets_ui_v341.py`
- Create/update: design and plan docs.

- [ ] **Step 1:** Run new performance/targets/UI source contract.
- [ ] **Step 2:** Run all existing AL-thmany source validators.
- [ ] **Step 3:** Run `git diff --check`.
- [ ] **Step 4:** Commit exact modified files to `feat/full-features-3.4.1`.
- [ ] **Step 5:** Push and wait for GitHub Actions.
- [ ] **Step 6:** Require `testDebugUnitTest`, `lintDebug`, `assembleDebug` PASS.
- [ ] **Step 7:** Download non-empty `al-thmany-debug-apk`.
- [ ] **Step 8:** Device acceptance: verify tap responsiveness, navigation, all WhatsApp targets, typography, and no UI freeze during active engine updates.
