# AL-thmany 3.4.1 — Unified Runtime / Scan / Export Redesign Specification

**Target repository:** `othma433-ai/Bb`  
**Target branch:** `feat/full-features-3.4.1`  
**Intended repository path:** `docs/superpowers/specs/2026-09-16-unified-runtime-scan-export-redesign.md`  
**Status:** Design approved in chat; awaiting final spec approval before implementation.

---

## 1. Objective

Rebuild the runtime architecture so that **Join, Scan, Extraction, Publish, Group Sync, and Diagnostics** all use one shared execution environment instead of each feature maintaining its own targeting/runtime assumptions.

The redesign must also:

- Keep the full app usable in the **Owner profile**, **Work Profile**, **Secure Folder**, and supported **Samsung Dual Messenger / cloned WhatsApp** contexts.
- Support **Accessibility** and **Shizuku** through the same runtime-selection layer.
- Fix Join result verification so a successful Join action is not incorrectly recorded as `ACTION_TIMEOUT`.
- Make Scan a **read-only classification workflow** that never clicks Join or Request.
- Improve screen/card layout consistency and Arabic RTL presentation.
- Produce clean TXT/XLSX scan exports grouped by category and sorted by member count **descending**.

---

## 2. Core Architecture

### 2.1 Unified Execution Target

Introduce one canonical runtime target model:

```text
ExecutionTarget
├── AndroidEnvironment
│   ├── OWNER
│   ├── WORK_PROFILE
│   ├── SECURE_FOLDER / SAMSUNG_ISOLATED
│   ├── SECONDARY_USER
│   └── UNKNOWN
├── WhatsAppInstance
│   ├── Personal
│   ├── Business
│   ├── Dual / Clone
│   └── Discovered vendor instance
└── RuntimeBackend
    ├── AUTO
    ├── ACCESSIBILITY
    └── SHIZUKU
```

Every feature must resolve and lock the same `ExecutionTarget` before taking ownership of WhatsApp UI.

### 2.2 Single Runtime Owner

The existing single-owner invariant remains mandatory:

```text
RuntimeOperationCoordinator
```

Only one operation may control WhatsApp UI at a time.

Operations:

- JOIN
- SCAN
- EXTRACTION
- PUBLISH
- SYNC

A feature may not start until the previous owner has fully released the runtime.

---

## 3. Android Environment Model

### 3.1 Owner Profile

Normal installation in the Android owner user.

Supported targets are WhatsApp packages visible to that installation.

### 3.2 Work Profile

The preferred model is **profile-local execution**:

- Install AL-thmany inside the Work Profile.
- Install/enable WhatsApp inside the same Work Profile.
- AL-thmany detects the current profile automatically.
- All features run against WhatsApp visible to that profile.

Shizuku may be used when it can explicitly access the target Android user/profile, but the application must never assume cross-profile access exists.

### 3.3 Secure Folder

The supported model is:

- Install AL-thmany inside Secure Folder.
- Install WhatsApp inside Secure Folder.
- The Secure Folder installation operates as a complete AL-thmany application.

No Knox bypass is attempted.

If Shizuku exposes a verified remote Android user + target package, that route can be offered as an advanced runtime target; otherwise local profile execution is required.

### 3.4 Samsung Dual Messenger

Dual Messenger is not represented as a separate legacy Sender screen.

If the cloned package is visible/launchable, it appears directly in the unified target selector as a WhatsApp instance.

The complete AL-thmany feature set must target it through the same runtime interface.

---

## 4. Shared Runtime Resolver

Create a runtime service such as:

```text
UnifiedRuntimeResolver
```

Responsibilities:

1. Detect current Android profile.
2. Discover all WhatsApp instances available in that environment.
3. Detect Accessibility readiness.
4. Detect Shizuku readiness.
5. Determine whether the selected backend can actually control the selected target.
6. Produce a locked `ExecutionTarget`.
7. Expose a diagnostic reason when runtime preparation fails.

Backend resolution:

```text
AUTO
├── Prefer verified local Accessibility when target is local and service is connected.
├── Prefer Shizuku when a remote/isolated target explicitly requires it.
└── Fail with a concrete diagnostic if neither route can control the target.
```

No feature should implement its own independent package/profile/backend resolver after this redesign.

---

## 5. Join Workflow

### 5.1 Required Flow

```text
Parse URL
→ Resolve target
→ Open invite
→ Detect preview/action
→ Execute Join / Request action
→ Observe post-action transition
→ Verify final membership state
→ Record result
→ Advance
```

### 5.2 Fix Current False Failure

The current runtime journal shows cases where:

```text
CLICK: Executed JOIN
→ loading
→ MULTIPLE_POSITIVE_ACTIONS
→ ACTION_TIMEOUT
```

This must be replaced with a post-action verification state machine.

After an action click, the engine must not immediately reclassify the old Join button as a new unresolved action.

Introduce a state similar to:

```text
POST_ACTION_VERIFY
```

Verification evidence may include:

- Conversation/group screen opened.
- Invite preview closed and group UI visible.
- Join button disappeared.
- Request-pending state appeared.
- "Already member"/group-open state appeared.
- Stable package/window transition compatible with success.

Result mapping:

```text
JOIN + verified membership        → JOINED
REQUEST + verified pending        → REQUESTED / REQUEST_PENDING
Action executed but unverified    → ACTION_UNCERTAIN
No action executed                → failure/timeout as appropriate
```

`ACTION_TIMEOUT` must never be used merely because the post-action screen still contains multiple positive-looking accessibility nodes.

---

## 6. Scan Workflow

### 6.1 Scan is Read-Only

Scan must never click:

- Join
- Request to Join
- Send Request
- membership actions

It only opens the invite preview and observes it.

### 6.2 Classification Categories

At minimum:

- Direct Join / بدون موافقة المشرف
- Approval Required / يحتاج موافقة المشرف
- Request Pending / طلب معلق
- Already Member / عضو بالفعل
- Invalid / منتهي أو غير صالح
- Full / ممتلئ
- Removed / تمت الإزالة
- Account Limit
- Network Error
- Unknown / غير مؤكد

### 6.3 Scan Evidence

Every result should store:

- normalized URL
- invite code
- group name
- numeric member count when available
- original member-count text
- invite kind
- status
- confidence
- signal code
- target environment
- WhatsApp package
- backend used
- attempts
- duration
- scanned timestamp

### 6.4 Completion Criteria

A scan item is completed only after:

- a definitive category is detected; or
- the configured retry/timeout policy is exhausted.

No membership action is used as a probing technique.

---

## 7. Member Count Normalization

Add a dedicated parser:

```text
MemberCountParser
```

It must understand common forms such as:

```text
1,024 members
1.024 members
١٬٠٢٤ عضو
1024 مشارك
1.2K members
```

Store:

```text
memberCountText: String?
memberCount: Int?
```

Sorting for results/export:

1. Known numeric counts first.
2. **Largest member count → smallest member count.**
3. Unknown counts after numeric counts.
4. Then group name.
5. Then URL.

---

## 8. Scan Export Redesign

### 8.1 XLSX

Workbook structure:

```text
All Results
Direct Join
Approval Required
Request Pending
Already Member
Invalid
Full
Removed
Account Limit
Network Error
Unknown
```

Each sheet contains columns such as:

```text
#
Group Name
Members
Status
Invite Kind
Confidence
URL
Invite Code
Backend
Environment
WhatsApp Package
Attempts
Duration
Signal
Detail
Scanned At
```

Rows are sorted by numeric members descending.

`All Results` contains all categories.

### 8.2 TXT

TXT must be human-readable and grouped:

```text
================================================
بدون موافقة المشرف — 27
================================================
1. Group A
   Members: 1024
   URL: ...
...

================================================
يحتاج موافقة المشرف — 18
================================================
...
```

Each section is independently sorted by members descending.

### 8.3 CSV / JSON

Keep machine-readable formats, but use the same normalized ordering and include `memberCount`.

---

## 9. UI Redesign

### 9.1 Remove Legacy Advanced Manager Entry

Remove the Join-page button:

```text
إدارة Work Profile / Secure Folder / Dual Messenger
```

that opens the legacy Sender application.

### 9.2 Centralize Environment Control

The **full interactive environment card appears only on the Home screen / main settings surface**. It owns:

- Environment / Android-user target selection
- WhatsApp target selection
- Shizuku remote-target discovery
- Backend choice: AUTO / Accessibility / Shizuku
- Runtime refresh and readiness

Operational screens (Join / Extraction / Scan / Publish) must **not repeat those selectors**. They show only a compact read-only status strip, for example:

```text
جاهز • Work Profile • WhatsApp Personal • Shizuku
```

This keeps one source of truth and avoids duplicate controls in every tab.

### 9.3 Card Layout Rules

All main cards must share:

- same outer horizontal margin
- same corner radius
- same title hierarchy
- same border weight
- consistent internal padding
- consistent button heights
- RTL alignment
- no mixed oversized/undersized cards

Recommended baseline:

```text
Screen horizontal padding: 16dp
Card radius: 20dp
Card internal padding: 16dp
Vertical gap: 12dp
Primary action height: 54–58dp
Secondary action height: 48–52dp
```

### 9.4 Bottom Navigation

Keep:

- الرئيسية
- الانضمام
- الاستخراج
- الفحص
- النشر
- التشخيص

The global runtime control bar must not visually collide with page content or bottom navigation.

---

## 10. Diagnostics

Diagnostics must show the **same unified runtime state** used by engines.

Required fields:

- Current Android environment
- Target profile/user
- Target WhatsApp package
- Selected backend
- Effective backend
- Accessibility readiness
- Shizuku binder/permission/UserService
- Current runtime owner
- Current engine phase
- Last transition
- Last failure reason
- Target lock
- Recent runtime journal

Diagnostics must distinguish:

```text
Configuration problem
Runtime availability problem
Target problem
Classifier uncertainty
Action verification failure
```

---

## 11. Migration / Compatibility

Existing data should be retained where possible.

Legacy Sender logic may be reused internally for Join action execution, but:

- The legacy Sender UI must no longer be required to configure operational targets.
- Profile/target/backend selection must move into the unified AL-thmany workspace.
- Old preferences may be migrated into the new target model on first run.

---

## 12. Testing Strategy

### Unit Tests

- MemberCountParser
- Scan category classifier
- Join post-action state machine
- Unified target/backend resolver
- Export grouping/sorting
- Four-link preview
- Runtime ownership rules

### Integration Tests

- Scan never executes membership click.
- Join performs one membership action maximum per link before verification.
- All engines resolve the same target.
- Target lock survives operation transitions.
- Export sheets/categories are deterministic.
- Unknown member counts sort after numeric counts.

### CI Gate

Required before APK delivery:

```text
source contracts PASS
unit tests PASS
lintDebug PASS
assembleDebug PASS
APK exists and non-empty
```

### Device Acceptance

Minimum matrix:

```text
Owner + WhatsApp Personal + Accessibility
Owner + WhatsApp Business + Accessibility
Owner + supported Dual Messenger + Accessibility
Owner/remote verified target + Shizuku
Work Profile local installation
Secure Folder local installation
```

For unsupported cross-profile configurations, the app must report a clear diagnostic rather than silently operate on the wrong WhatsApp instance.

---

## 13. Acceptance Criteria

The redesign is accepted only when all of the following are true:

1. Join no longer converts verified successful joins into `ACTION_TIMEOUT`.
2. Scan starts reliably and performs no membership action.
3. Scan classification results are visible and persisted.
4. Member count is numeric when parsable.
5. Scan exports are grouped and sorted largest-to-smallest.
6. Operational screens use a consistent RTL card system.
7. The legacy advanced-target button is removed.
8. Target environment/backend selection is shared by all features.
9. The full app can operate when installed in Work Profile and Secure Folder.
10. Supported Dual Messenger packages are first-class targets.
11. Shizuku is available as a real backend through the shared runtime layer.
12. Diagnostics report the exact target/backend/environment used by the active feature.
13. CI produces a valid APK before the release is called complete.

---

## 14. Implementation Boundary

This redesign intentionally does **not** attempt to:

- bypass Android profile security
- bypass Samsung Knox/Secure Folder isolation
- fabricate member counts not exposed by WhatsApp
- use Join/Request clicks as a scan-detection technique
- run two WhatsApp-control engines concurrently


## Physical database isolation

The selected Android environment is also a data boundary. Local execution preserves the historic
`althmany_extractor.db` so existing data is retained. A verified Shizuku target in another Android
user uses a dedicated database file (`althmany_extractor_remote_u<id>.db`). Controllers are rebound
only while idle. This physically separates groups, extraction checkpoints, links, scan rows and
publish runs between Android users instead of relying only on `whatsapp_package`.
