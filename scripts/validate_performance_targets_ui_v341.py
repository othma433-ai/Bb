#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
failures = []

def read(rel: str) -> str:
    p = ROOT / rel
    if not p.exists():
        failures.append(f"missing: {rel}")
        return ""
    return p.read_text(encoding="utf-8", errors="ignore")

runtime = read("app/src/main/java/com/althmany/extractor/profile/UnifiedRuntimeTarget.kt")
extract = read("app/src/main/java/com/althmany/extractor/engine/ExtractionController.kt")
scan_controller = read("app/src/main/java/com/althmany/extractor/engine/ScanController.kt")
publish_controller = read("app/src/main/java/com/althmany/extractor/engine/PublishController.kt")
vm = read("app/src/main/java/com/althmany/extractor/ui/AppViewModel.kt")
main = read("app/src/main/java/com/althmany/extractor/MainActivity.kt")
theme = read("app/src/main/java/com/althmany/extractor/ui/Theme.kt")
card = read("app/src/main/java/com/althmany/extractor/ui/UnifiedRuntimeCard.kt")
workspace = read("app/src/main/java/com/althmany/extractor/ui/WorkspaceV341Screens.kt")
professional = read("app/src/main/java/com/althmany/extractor/ui/ProfessionalJoinScanScreens.kt")
join = read("app/src/main/java/com/althmany/extractor/join/OriginalJoinCoordinator.kt")
registry = read("app/src/main/java/com/althmany/extractor/profile/WhatsAppInstanceRegistry.kt")

checks = {
    "runtime resolve is cache-first":
        "WhatsAppInstanceRegistry.available(appContext, forceRefresh = false)" in runtime,

    "runtime refresh has explicit force flag":
        "fun refreshRuntimeEnvironment(forceDiscovery: Boolean = false)" in extract
        and "forceRefresh = forceDiscovery" in extract
        and "refreshRuntimeEnvironment(forceDiscovery = true)" in extract,

    "engine target collector is package-only":
        ".map { it.selectedWhatsAppPackage }" in vm
        and ".distinctUntilChanged()" in vm
        and "engineState.collect { state ->" not in vm,

    "Shizuku remote discovery is background":
        "withContext(Dispatchers.IO)" in vm
        and "refreshRemoteTargetsInternal" in vm
        and "targetDiscoveryJob" in vm,

    "SQLite query helper exists":
        "private suspend fun <T> queryIo" in vm
        and "withContext(Dispatchers.IO)" in vm,

    "controller stats queries are off Main":
        "withContext(Dispatchers.IO) { repository.stats() }" in extract
        and "withContext(Dispatchers.IO) { repository.scanStats() }" in scan_controller
        and "withContext(Dispatchers.IO) { repository.publishStats(runId) }" in publish_controller,


    "scan list does not reload on stats hash":
        "lastStatsHash" not in vm
        and "stats.hashCode()" not in vm
        and "_scanItems.value = queryIo { repo.scanItems() }" in vm,

    "publish list IO throttling exists":
        "lastRunId" in vm
        and "lastTerminal" in vm
        and "queryIo { repo.publishItems(runId) }" in vm,

    "target tap avoids Sender package rescan":
        "WhatsAppLauncher.discoverWhatsAppApps(senderApp)" not in vm
        and "engineState.value.availableWhatsApp" in vm,

    "explicit target catalog refresh exists":
        "fun refreshTargetCatalog()" in vm
        and "forceRefresh = true" in vm
        and "WhatsAppInstanceRegistry.available" in vm,

    "MainActivity no longer root-collects heavy lists":
        "val groups by viewModel.groups.collectAsState()" not in main.split("when (screen)")[0]
        and "val links by viewModel.links.collectAsState()" not in main.split("when (screen)")[0]
        and "val scanItems by viewModel.scanItems.collectAsState()" not in main.split("when (screen)")[0]
        and "val publishItems by viewModel.publishItems.collectAsState()" not in main.split("when (screen)")[0],

    "screen-local list collection exists":
        main.count("val groups by viewModel.groups.collectAsState()") >= 3
        and "val scanItems by viewModel.scanItems.collectAsState()" in main
        and "val links by viewModel.links.collectAsState()" in main,

    "Accessibility polling bounded":
        "repeat(4)" in main
        and "delay(1_000L)" in main
        and "repeat(20)" not in main,

    "one selector includes local and remote WhatsApp":
        "val localTargets = engine.availableWhatsApp" in card
        and 'items(localTargets, key = { "local:${it.packageName}" })' in card
        and 'items(remoteTargets, key = { "remote:${it.stableKey}" })' in card
        and '"جميع نسخ واتساب"' in card,

    "all known WhatsApp types remain discoverable":
        'WHATSAPP = "com.whatsapp"' in registry
        and 'WHATSAPP_BUSINESS = "com.whatsapp.w4b"' in registry
        and 'WHATSAPP_CLONED = "com.whatsapp2"' in registry
        and "WhatsAppInstanceKind.DISCOVERED" in registry
        and 'packageName.contains("whatsapp", ignoreCase = true)' in runtime
        and "remotePackages" in runtime,

    "home refreshes full target catalog":
        "onRefreshTargets" in workspace
        and "onRefresh = onRefreshTargets" in workspace
        and "onRefreshTargets = viewModel::refreshTargetCatalog" in main,

    "professional typography is custom":
        "private val AppTypography = Typography(" in theme
        and "FontFamily.SansSerif" in theme
        and "typography = AppTypography" in theme
        and "typography = Typography()" not in theme,

    "primary workspace has no 9sp text":
        "fontSize = 9.sp" not in workspace
        and "fontSize = 9.sp" not in professional
        and "fontSize = 9.sp" not in card,

    "primary workspace has no 10sp text":
        "fontSize = 10.sp" not in workspace
        and "fontSize = 10.sp" not in professional
        and "fontSize = 10.sp" not in card,

    "runtime card touch targets readable":
        ".size(48.dp)" in card
        and ".heightIn(min = 50.dp)" in card
        and "MaterialTheme.typography.bodyMedium" in card,

    "Join automation engine remains original":
        "QuickJoinAccessibilityService" in join
        and "ShizukuAutomationService" in join,

    "Join UI polling reduced without touching engine timings":
        "delay(700L)" in join
        and "delay(350L)" not in join,

    "Scan remains read-only":
        "ScanController.setActionMode(ScanActionMode.SCAN_ONLY)" in vm
        and "ScanController.setRequestToJoinEnabled(false)" in vm,
}

for name, ok in checks.items():
    print(f"{'PASS' if ok else 'FAIL'}: {name}")
    if not ok:
        failures.append(name)

if failures:
    print("\nPERFORMANCE / TARGETS / RTL 3.4.1 CONTRACT: FAIL")
    for item in failures:
        print(" -", item)
    sys.exit(1)

print("\nPERFORMANCE / TARGETS / RTL 3.4.1 CONTRACT: PASS")
