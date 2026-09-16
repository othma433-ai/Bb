#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing file: {rel}")
        return ""
    return p.read_text(encoding="utf-8", errors="ignore")

main = read("app/src/main/java/com/althmany/extractor/MainActivity.kt")
vm = read("app/src/main/java/com/althmany/extractor/ui/AppViewModel.kt")
workspace = read("app/src/main/java/com/althmany/extractor/ui/WorkspaceV341Screens.kt")
professional = read("app/src/main/java/com/althmany/extractor/ui/ProfessionalJoinScanScreens.kt")
join = read("app/src/main/java/com/althmany/extractor/join/OriginalJoinCoordinator.kt")
diag = read("app/src/main/java/com/althmany/extractor/ui/V341DiagnosticsScreen.kt")
preview = read("app/src/main/java/com/althmany/extractor/ui/LinkPreviewPolicy.kt")
manifest = read("app/src/main/AndroidManifest.xml")

join_start = main.find("AppScreen.JOIN -> ProfessionalJoinScreen(")
scan_start = main.find("AppScreen.SCAN -> ProfessionalScanScreen(", join_start + 1)

join_block = (
    main[join_start:scan_start]
    if join_start >= 0 and scan_start > join_start
    else ""
)

checks = {
    "new workspace launcher": (
        'android:name="com.althmany.extractor.MainActivity"' in manifest
        and 'android.intent.category.LAUNCHER' in manifest
    ),
    "original join coordinator": "class OriginalJoinCoordinator" in join,
    "join reuses original sender engine": (
        "MainViewModel" in join
        and "QuickJoinAccessibilityService" in join
        and "ShizukuAutomationService" in join
        and "ScanController" not in join
    ),
    "classification-only scan enforced": (
        "ScanController.setActionMode(ScanActionMode.SCAN_ONLY)" in vm
        and "ScanController.setRequestToJoinEnabled(false)" in vm
    ),
    "scan UI has no membership mode selector":
        "ScanActionMode.entries" not in professional,
    "four-link preview": (
        "COLLAPSED_LIMIT = 4" in preview
        and "LinkPreviewPolicy.analyze" in professional
        and "عرض المزيد" in professional
        and "عرض أقل" in professional
    ),
    "professional join wired": (
        join_start >= 0
        and scan_start > join_start
        and "join = joinState" in join_block
        and "onStart = joinCoordinator::start" in join_block
        and "scan = scanState" not in join_block
    ),
    "professional scan wired":
        "AppScreen.SCAN -> ProfessionalScanScreen(" in main,
    "member count displayed safely": (
        "memberCountText" in professional
        and "عدد الأعضاء غير متاح" in professional
    ),
    "target synced to original sender": (
        "selectedWhatsAppPackage = packageName" in vm
        and "preferredTarget = PreferredTarget.AUTO" in vm
    ),
    "real diagnostics present": (
        "RuntimeDiagnosticStore.readRecent" in diag
        and "RuntimeHealthMonitor.snapshot" in diag
        and "AccessibilityStatus.readiness" in diag
        and "ShizukuBridge.status" in diag
    ),
    "diagnostics routed":
        "AppScreen.LOGS -> V341DiagnosticsScreen" in main,
    "diagnostics in bottom navigation":
        'Triple(AppScreen.LOGS, "التشخيص"' in workspace,
    "rtl root preserved":
        "LocalLayoutDirection provides LayoutDirection.Rtl" in main,
}

for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
    if not ok:
        errors.append(name)

if errors:
    print("\nPROFESSIONAL REBUILD CONTRACT: FAIL")
    for item in errors:
        print(" -", item)
    sys.exit(1)

print("\nPROFESSIONAL REBUILD CONTRACT: PASS")
