#!/usr/bin/env python3
from pathlib import Path
import sys
import re

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
failures = []

def read(rel: str) -> str:
    path = ROOT / rel
    if not path.exists():
        failures.append(f"missing file: {rel}")
        return ""
    return path.read_text(encoding="utf-8", errors="ignore")

runtime = read("app/src/main/java/com/althmany/extractor/profile/UnifiedRuntimeTarget.kt")
router = read("app/src/main/java/com/althmany/extractor/profile/NativeProfileEngineRouter.kt")
scan = read("app/src/main/java/com/althmany/extractor/engine/ScanController.kt")
publish = read("app/src/main/java/com/althmany/extractor/engine/PublishController.kt")
extract = read("app/src/main/java/com/althmany/extractor/engine/ExtractionController.kt")
join = read("app/src/main/java/com/althmany/extractor/join/OriginalJoinCoordinator.kt")
quick = read("app/src/main/java/com/althmany/groupmanager/accessibility/QuickJoinAccessibilityService.kt")
main = read("app/src/main/java/com/althmany/extractor/MainActivity.kt")
workspace = read("app/src/main/java/com/althmany/extractor/ui/WorkspaceV341Screens.kt")
professional = read("app/src/main/java/com/althmany/extractor/ui/ProfessionalJoinScanScreens.kt")
card = read("app/src/main/java/com/althmany/extractor/ui/UnifiedRuntimeCard.kt")
diag = read("app/src/main/java/com/althmany/extractor/ui/V341DiagnosticsScreen.kt")
shizuku_bridge = read("app/src/main/java/com/althmany/extractor/shizuku/ShizukuBridge.kt")
export_manager = read("app/src/main/java/com/althmany/extractor/export/ExportManager.kt")
structured = read("app/src/main/java/com/althmany/extractor/export/StructuredScanExporter.kt")
members = read("app/src/main/java/com/althmany/extractor/export/MemberCountParser.kt")
member_test = read("app/src/test/java/com/althmany/extractor/export/MemberCountParserTest.kt")
app_vm = read("app/src/main/java/com/althmany/extractor/ui/AppViewModel.kt")
profile = read("app/src/main/java/com/althmany/extractor/profile/ProfileEnvironment.kt")
registry = read("app/src/main/java/com/althmany/extractor/profile/WhatsAppInstanceRegistry.kt")
env_db = read("app/src/main/java/com/althmany/extractor/profile/EnvironmentDatabasePolicy.kt")
feature_runtime = read("app/src/main/java/com/althmany/extractor/ExtractorApp.kt")
database = read("app/src/main/java/com/althmany/extractor/data/ExtractorDatabase.kt")
env_db_test = read("app/src/test/java/com/althmany/extractor/profile/EnvironmentDatabasePolicyTest.kt")



def screen_block_containing(screen, needle):
    pattern = re.compile(
        rf"(?m)^[ \t]*AppScreen\.{screen}[ \t]*->[ \t]*"
    )

    matches = list(pattern.finditer(main))

    for match in matches:
        rest = main[match.end():]

        next_match = re.search(
            r"(?m)^[ \t]*AppScreen\.[A-Z_]+[ \t]*->[ \t]*",
            rest
        )

        end = (
            match.end() + next_match.start()
            if next_match
            else len(main)
        )

        block = main[match.start():end]

        if needle in block:
            return block

    return ""


def screen_wires(screen, composable):
    block = screen_block_containing(
        screen,
        f"{composable}("
    )

    return (
        f"{composable}(" in block
        and "runtime = runtimeTarget" in block
    )



def composable_has_runtime(name):
    pattern = re.compile(
        rf"{re.escape(name)}\s*\(",
        re.M
    )

    for match in pattern.finditer(main):
        window = main[match.start(): match.start() + 2200]

        if "runtime = runtimeTarget" in window:
            return True

    return False

checks = {
    "canonical ExecutionTarget model": all(token in runtime for token in [
        "enum class RuntimeBackendPreference",
        "data class UnifiedRuntimeSnapshot",
        "object UnifiedRuntimeTargetStore",
        "RuntimeBackendPreference.AUTO",
        "RuntimeBackendPreference.ACCESSIBILITY",
        "RuntimeBackendPreference.SHIZUKU",
    ]),
    "profile-local Work and Secure environments preserved": (
        "RuntimeProfileKind.WORK" in profile
        and "RuntimeProfileKind.SAMSUNG_ISOLATED" in profile
        and "Work Profile" in profile
        and "Knox" in profile
    ),
    "Dual Messenger remains first-class WhatsApp target": (
        'WHATSAPP_CLONED = "com.whatsapp2"' in registry
        and "WhatsAppInstanceKind.CLONED" in registry
    ),
    "remote Shizuku environments are discoverable": (
        "data class UnifiedRemoteTarget" in runtime
        and "discoverRemoteTargets" in runtime
        and "pm list users" in runtime
        and "Dual Messenger • user" in runtime
        and "Work Profile • user" in runtime
        and "Secure Folder • user" in runtime
    ),
    "Shizuku launches exact Android user and deep link": (
        "androidUserId: Int? = null" in shizuku_bridge
        and "am start --user" in shizuku_bridge
        and "suspend fun launchUrl" in shizuku_bridge
    ),
    "router honors backend preference": (
        "UnifiedRuntimeTargetStore.preference(context)" in router
        and "RuntimeBackendPreference.SHIZUKU" in router
        and "RuntimeBackendPreference.ACCESSIBILITY" in router
    ),
    "physical database isolation per Android environment": (
        "object EnvironmentDatabasePolicy" in env_db
        and "althmany_extractor_remote_u$androidUserId.db" in env_db
        and "LOCAL_DATABASE" in env_db
        and "switchRuntimeEnvironment" in feature_runtime
        and "activeScopeKey" in feature_runtime
        and "ExtractorDatabase(appContext, dbName)" in feature_runtime
        and "databaseName: String = DB_NAME" in database
        and "private val repo get() = ExtractorFeatureRuntime.repository" in app_vm
    ),
    "database isolation unit tests present": (
        "localKeepsHistoricDatabaseName" in env_db_test
        and "remoteAndroidUsersArePhysicallyIsolated" in env_db_test
    ),
    "Scan honors unified backend": (
        "UnifiedRuntimeTargetStore.preference(appContext)" in scan
        and "RuntimeBackendPreference.SHIZUKU" in scan
        and "RuntimeBackendPreference.ACCESSIBILITY" in scan
    ),
    "Publish honors unified backend": (
        "UnifiedRuntimeTargetStore.preference(appContext)" in publish
        and "RuntimeBackendPreference.SHIZUKU" in publish
        and "RuntimeBackendPreference.ACCESSIBILITY" in publish
    ),
    "Extraction and Sync honor unified backend": (
        "UnifiedRuntimeTargetStore.preference(appContext)" in extract
        and "Shizuku محدد كمحرك للمزامنة" in extract
        and "Shizuku محدد للاستخراج" in extract
    ),
    "Join honors unified backend": (
        "UnifiedRuntimeTargetStore.preference(context)" in join
        and "RuntimeBackendPreference.SHIZUKU" in join
        and "RuntimeBackendPreference.ACCESSIBILITY" in join
    ),
    "legacy Sender target manager removed from Join": (
        "openAdvancedTargetManager" not in join
        and "SenderMainActivity" not in join
        and "إدارة Work Profile / Secure Folder / Dual Messenger" not in professional
        and "onAdvancedTargets" not in professional
    ),
    "Scan is forced read-only at engine boundary": (
        "Scan is permanently read-only" in scan
        and "setActionMode(ScanActionMode.SCAN_ONLY)" in scan
        and "setRequestToJoinEnabled(false)" in scan
        and "ScanController.setActionMode(ScanActionMode.SCAN_ONLY)" in app_vm
    ),
    "post-action conflict hold prevents premature timeout": (
        "POST_ACTION_CONFLICT_HOLD" in quick
        and "POST_ACTION_DUPLICATE_SUPPRESSED" in quick
        and "ScreenEvidenceConflict.MULTIPLE_POSITIVE_ACTIONS" in quick
        and "holding instead of re-clicking" in quick
    ),
    "member parser supports Arabic and compact counts": (
        "object MemberCountParser" in members
        and "'٠' to '0'" in members
        and "1_000.0" in members
        and "1_000_000.0" in members
    ),
    "scan sorting is largest-to-smallest unknown-last": (
        "thenByDescending" in members
        and "MemberCountParser.parse(it.memberCountText) == null" in members
        and "ScanResultOrganizer.sorted(scanItems)" in professional
    ),
    "scan export routed through structured exporter": (
        "StructuredScanExporter.export" in export_manager
        and "object StructuredScanExporter" in structured
    ),
    "XLSX has All Results and status sheets": (
        '"All Results" to items' in structured
        and "ScanResultOrganizer.sheetName(status)" in structured
        and "worksheets/sheet${index + 1}.xml" in structured
    ),
    "TXT is grouped by classification": (
        "ScanResultOrganizer.grouped(items)" in structured
        and "status.labelAr" in structured
        and "الأعضاء:" in structured
    ),
    "member count tests present": (
        "parsesEnglishArabicAndCompactCounts" in member_test
        and "sortsLargestMemberCountFirstAndUnknownLast" in member_test
    ),
    "all primary operational screens receive unified runtime": (
        composable_has_runtime("V341HomeScreen")
        and composable_has_runtime("V341ExtractionScreen")
        and composable_has_runtime("ProfessionalJoinScreen")
        and composable_has_runtime("ProfessionalScanScreen")
        and composable_has_runtime("V341PublishScreen")
    ),
    "central environment control with compact feature status": (
        workspace.count("UnifiedRuntimeCard(") == 1
        and workspace.count("UnifiedRuntimeStatusStrip(") >= 2
        and professional.count("UnifiedRuntimeStatusStrip(") >= 2
        and "fun UnifiedRuntimeCard" in card
        and "fun UnifiedRuntimeStatusStrip" in card
        and "remoteTargets" in card
        and "onDiscoverRemoteTargets" in card
    ),
    "RTL card geometry normalized": (
        "RoundedCornerShape(20.dp)" in card
        and "padding(16.dp)" in card
        and "horizontal = 16.dp" in workspace
        and "spacedBy(12.dp)" in workspace
    ),
    "diagnostics reports unified runtime": (
        "UnifiedRuntimeTargetStore.resolve" in diag
        and 'runtimeBackend = "${unified.preference.name} -> ${unified.effectiveBackend.name}"' in diag
    ),
    "target selection shared with legacy Join internals": (
        "UnifiedRuntimeTargetStore.setLocalTarget" in app_vm
        and "UnifiedRuntimeTargetStore.setRemoteTarget" in app_vm
        and "senderApp.preferences.selectedWhatsAppPackage = packageName" in app_vm
        and "senderApp.preferences.setRemoteSecureTarget" in app_vm
    ),
}

for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
    if not ok:
        failures.append(name)

if failures:
    print("\nUNIFIED RUNTIME 3.4.1 CONTRACT: FAIL")
    for item in failures:
        print(" -", item)
    sys.exit(1)

print("\nUNIFIED RUNTIME 3.4.1 CONTRACT: PASS")
