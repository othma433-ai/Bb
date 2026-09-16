#!/usr/bin/env python3
from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]

def rd(path):
    p = root / path
    return p.read_text(encoding="utf-8", errors="ignore") if p.exists() else ""

build = rd("app/build.gradle.kts")
screens = rd("app/src/main/java/com/althmany/extractor/ui/Screens.kt")
main = rd("app/src/main/java/com/althmany/extractor/MainActivity.kt")
ui = rd("app/src/main/java/com/althmany/extractor/ui/WorkspaceV341Screens.kt")
service = rd("app/src/main/java/com/althmany/extractor/accessibility/WhatsAppAccessibilityService.kt")
source = rd("scripts/validate_source.py")

checks = {
    "versionName": 'versionName = "3.4.1"' in build,
    "versionCode": "versionCode = 401" in build,
    "JOIN enum": "HOME, JOIN, EXTRACT" in screens,
    "Home screen": "fun V341HomeScreen(" in ui,
    "Join screen": "fun V341JoinScreen(" in ui,
    "Extraction screen": "fun V341ExtractionScreen(" in ui,
    "Scan screen": "fun V341ScanScreen(" in ui,
    "Publish screen": "fun V341PublishScreen(" in ui,
    "Bottom nav": "fun V341BottomBar(" in ui,
    "select all": '"تحديد الكل"' in ui and "GroupSelectionPreset.ALL" in ui,
    "clear selection": '"إلغاء التحديد"' in ui and "GroupSelectionPreset.NONE" in ui,
    "unread": '"غير المقروءة"' in ui and "GroupSelectionPreset.UNREAD" in ui,
    "active": '"النشطة"' in ui and "GroupSelectionPreset.ACTIVE" in ui,
    "pause": '"إيقاف مؤقت"' in ui,
    "resume": '"استكمال"' in ui,
    "final stop": '"إيقاف نهائي"' in ui,
    "join route": "AppScreen.JOIN -> V341JoinScreen(" in main,
    "extract route": "AppScreen.EXTRACT -> V341ExtractionScreen(" in main,
    "scan route": "AppScreen.SCAN -> V341ScanScreen(" in main,
    "publish route": "AppScreen.PUBLISH -> V341PublishScreen(" in main,
    "bottom route": "V341BottomBar(current = screen)" in main,
    "sender isolation": "RuntimeOperation.SENDER -> Unit" in service,
    "source validator version": "versionCode 401" in source and "versionName 3.4.1" in source,
}

failed = []
for label, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + label)
    if not ok:
        failed.append(label)

if failed:
    print("\nFULL FEATURE CONTRACT FAILED")
    for item in failed:
        print(" -", item)
    sys.exit(1)

print("\nAL-thmany 3.4.1 FULL FEATURE CONTRACT: PASS")
