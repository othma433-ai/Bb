#!/usr/bin/env python3
from pathlib import Path
import sys
import re

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

def screen_block_containing(screen, needle):
    pattern = re.compile(
        rf"(?m)^[ \t]*AppScreen\.{screen}[ \t]*->[ \t]*"
    )

    matches = list(pattern.finditer(main))

    for index, match in enumerate(matches):
        next_match = re.search(
            r"(?m)^[ \t]*AppScreen\.[A-Z_]+[ \t]*->[ \t]*",
            main[match.end():]
        )

        block_end = (
            match.end() + next_match.start()
            if next_match
            else len(main)
        )

        block = main[match.start():block_end]

        if needle in block:
            return block

    return ""


join_block = screen_block_containing("JOIN", "ProfessionalJoinScreen(")
scan_block = screen_block_containing("SCAN", "ProfessionalScanScreen(")

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
        "ProfessionalJoinScreen(" in join_block
        and "join = joinState" in join_block
        and "onStart = joinCoordinator::start" in join_block
        and "scan = scanState" not in join_block
    ),
    "professional scan wired":
        "ProfessionalScanScreen(" in scan_block,
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
