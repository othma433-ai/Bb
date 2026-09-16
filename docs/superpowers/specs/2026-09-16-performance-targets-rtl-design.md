# AL-thmany 3.4.1 — Performance, Unified WhatsApp Targets & Professional RTL UI Redesign

**Status:** Design for approval
**Target branch:** `feat/full-features-3.4.1`
**Scope:** UI/state/runtime-target architecture only. Existing Join/Extraction/Scan/Publish engines remain the execution owners.

## 1. Problem statement

The current 3.4.1 workspace has three user-visible failures:

1. **Poor responsiveness**
   - taps can feel ignored;
   - screens stutter or hang while runtime/package state refreshes;
   - high-frequency engine state updates can trigger work that should not run on the UI path.

2. **WhatsApp targets are fragmented in the UI**
   - the engine still supports Personal, Business, cloned/discovered targets and remote Android-user targets;
   - however the new workspace exposes only a filtered local subset directly and hides Dual/Work/Secure behind a separate discovery path;
   - Join/Scan receive target callbacks but do not expose one consistent target selector.

3. **Typography/layout is not a coherent design system**
   - Material `Typography()` defaults are used;
   - many screens hard-code unrelated font sizes and spacings;
   - the result lacks a consistent Arabic visual hierarchy and touch geometry.

## 2. Design principles

- **Never rewrite proven automation engines solely for UI/performance work.**
- **One target catalog, one selected target, one backend policy.**
- **No package discovery, Shizuku discovery, or database reload on high-frequency UI state emissions.**
- **All operational screens must read the same immutable runtime-target snapshot.**
- **All real WhatsApp copies discovered and supported by the runtime must be visible in one selector.**
- **RTL by default; links/package names stay LTR where appropriate.**
- **Performance is a release gate, not a subjective afterthought.**

## 3. Runtime/state architecture

### 3.1 RuntimeTargetCatalog

Introduce a dedicated `RuntimeTargetCatalog` state owner.

It owns:
- local WhatsApp discovery;
- cached remote Android-user targets discovered through Shizuku;
- current selected environment + Android user + package;
- backend preference;
- readiness summary.

It does **not** run package discovery from `StateFlow.collect` of extraction/scan/publish engines.

### 3.2 Refresh policy

Heavy refresh is allowed only when:
- application starts;
- application returns to foreground after a meaningful interval;
- user presses Refresh;
- Shizuku readiness changes;
- selected environment changes.

Normal engine updates (progress, message, counters, heartbeat) update only lightweight state.

### 3.3 Cache policy

- Local PackageManager discovery: cached.
- Remote Shizuku target discovery: cached separately.
- No `forceRefresh=true` inside a high-frequency recomposition/state loop.
- Explicit invalidation when package/environment/backend actually changes.

### 3.4 UI collection boundaries

`MainActivity` stops acting as one giant collector of every high-frequency state.

Instead:
- top-level navigation collects only navigation/global-operation state;
- each screen collects only the state it renders;
- heavy lists use stable keys and immutable view models;
- derived values use mapped StateFlow/`derivedStateOf` instead of recomputing whole models.

## 4. Unified WhatsApp selector

Create one reusable `WhatsAppTargetSelector`.

It displays every verified target in one list/carousel:

- WhatsApp Personal
- WhatsApp Business
- Dual Messenger
- vendor clone/discovered WhatsApp package
- Work Profile target
- Secure Folder target
- other verified Android-user target discovered through Shizuku

Each entry displays:
- environment label;
- WhatsApp label;
- package name;
- Android user when remote;
- backend requirement/readiness;
- selected state.

There is no separate “advanced target manager” screen for normal selection.

### 4.1 Selection semantics

Local selection:
- selects local Android profile + package;
- Accessibility or Shizuku may be used according to backend preference.

Remote Android-user selection:
- locks Android user + package;
- requires Shizuku;
- Accessibility cannot be selected for a remote Android-user target.

### 4.2 Screen consistency

The same selected target is used by:
- Join
- Extraction
- Scan
- Publish
- Group Sync
- Diagnostics

Home shows the full selector. Operational screens show a compact selected-target strip with an action to open the same selector sheet/dialog, not separate target logic.

## 5. Performance plan

### 5.1 Remove expensive work from the hot path

Eliminate:
- PackageManager scans from engine-state collectors;
- repeated `forceRefresh=true` during normal runtime updates;
- database reloads caused only by progress/message changes;
- unnecessary full-screen recompositions.

### 5.2 Throttle persisted-result refreshes

Scan/Publish database lists refresh only when:
- item index advances;
- a result is committed;
- operation completes;
- user explicitly refreshes.

Not on every transient message/state emission.

### 5.3 Interaction responsiveness

Buttons:
- use immediate local pressed/disabled state;
- never synchronously perform PackageManager/Shizuku/SQLite work on the main thread;
- async actions expose progress without blocking navigation/taps.

### 5.4 Performance acceptance criteria

On supported device:
- screen navigation reacts immediately to tap under idle conditions;
- target selector opens without discovery blocking the first frame;
- tapping controls remains responsive while an automation engine updates state;
- no repeated target discovery during a steady Join/Scan/Extraction run;
- UI state collectors do not trigger blocking package/Shizuku calls.

## 6. Professional Arabic RTL design system

Create centralized tokens.

### Typography
- display/title: 24–28sp, bold
- section title: 18–20sp, bold
- body: 14–16sp
- supporting: 12–13sp
- label: 12–14sp, medium
- metric: 18–22sp, semibold/bold

Avoid 9–10sp for primary actionable/readable text.

Use one Android-safe Arabic-capable font family through Material typography; do not bundle external font files.

### Spacing
- page horizontal padding: 16dp phone / 24–28dp tablet
- card padding: 16dp
- vertical section spacing: 12–16dp
- card radius: 18–20dp
- primary touch targets: minimum 48dp height

### Components
Create reusable:
- `AppPageHeader`
- `AppCard`
- `AppSectionTitle`
- `PrimaryActionButton`
- `SecondaryActionButton`
- `MetricCard`
- `RuntimeStatusStrip`
- `WhatsAppTargetSelector`
- `OperationControls`

Screens must use these instead of private duplicate components (`V*`, `P*`) wherever practical.

## 7. Visual simplification

Reduce:
- nested cards;
- duplicate status blocks;
- repeated borders;
- duplicate start/control surfaces;
- excessive cyan outlines.

Keep:
- dark navy/black base;
- cyan/teal primary accent;
- green success;
- orange warning;
- red failure;
- purple only where semantically useful.

The interface should look like one product, not multiple independently styled screens.

## 8. Testing strategy

### Unit/source tests
- Package discovery cache does not refresh on engine progress changes.
- Unified selector contains Personal/Business/Clone/remote targets without filtering valid entries.
- Remote target requires Shizuku.
- Local target remains selectable after returning from a remote target.
- No screen reintroduces separate target-selection logic.
- typography token contract exists and is used by the main 3.4.1 screens.

### Build gates
- source validators
- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`
- non-empty APK artifact

### Manual runtime acceptance
- tap/navigation responsiveness
- selector shows all actual WhatsApp targets present on device
- buttons remain responsive during engine updates
- Join/Extraction/Scan/Publish all use the selected target
- typography/spacing is visually consistent on phone and tablet widths

## 9. Non-goals

This pass does not:
- replace the original Join automation engine;
- change Scan into a membership-action engine;
- bypass Android/Knox security boundaries;
- fabricate inaccessible Secure Folder/Work targets;
- merge remote Android-user databases back into the local database.

## 10. Release rule

The redesign is not complete until:
1. source contracts pass;
2. Android unit/lint/build pass in GitHub Actions;
3. APK artifact is produced;
4. the user can navigate and tap controls without the current stutter/hang;
5. all discoverable WhatsApp targets appear through the unified selector.
