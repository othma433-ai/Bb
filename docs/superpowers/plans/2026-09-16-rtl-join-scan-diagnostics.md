# AL-thmany 3.4.1 Professional Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox syntax.

**Goal:** Rewire Join to the proven Sender engine, make Scan classification-only, normalize Arabic RTL UX, compact link inputs, and expose real diagnostics.

**Architecture:** Keep the existing runtime engines. Add an adapter (`OriginalJoinCoordinator`) instead of duplicating join logic. Keep Scan but force classification-only at every start path. Reuse existing runtime diagnostic sources.

**Tech Stack:** Kotlin, Android, Jetpack Compose Material3, AccessibilityService, Shizuku, SQLite, JUnit4.

**Spec:** `docs/superpowers/specs/2026-09-16-rtl-join-scan-diagnostics-design.md`

## Global Constraints
- Version display stays 3.4.1.
- Launcher remains `com.althmany.extractor.MainActivity`.
- Arabic RTL is enforced at the Compose root.
- Scan never performs membership actions.
- Join uses the original Sender runtime.
- RuntimeOperation remains single-owner.

### Task 1 — Compact link preview
- [ ] Add `LinkPreviewPolicy`.
- [ ] Add JUnit tests for 4-line collapse/expand.
- [ ] Run tests.

### Task 2 — Original Sender join adapter
- [ ] Add `OriginalJoinCoordinator`.
- [ ] Reuse original MainViewModel / Accessibility / Shizuku.
- [ ] Preserve queue, verification, pause/resume/stop.

### Task 3 — WhatsApp target synchronization
- [ ] Synchronize Workspace target with Sender preferences.
- [ ] Use only discovered launchable WhatsApp packages.

### Task 4 — Classification-only scan
- [ ] Force SCAN_ONLY on every scan start.
- [ ] Remove join action choices from Scan UI.
- [ ] Show status, group name, member count when available, type and confidence.

### Task 5 — Arabic RTL sizing
- [ ] Use readable typography and spacing.
- [ ] Compact Join/Scan link inputs to four rows.
- [ ] Preserve RTL root.

### Task 6 — Real diagnostics
- [ ] Add diagnostics screen.
- [ ] Show Accessibility, Shizuku, profile, owner, stage, health and runtime journal.
- [ ] Copy/export/clear.

### Task 7 — Verification
- [ ] Run professional source contract.
- [ ] Run existing validators.
- [ ] Run unit tests.
- [ ] `git diff --check`.
- [ ] Commit/push only after passing local unit tests.
