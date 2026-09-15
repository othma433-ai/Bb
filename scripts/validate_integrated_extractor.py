#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors=[]

def req(rel):
    p=ROOT/rel
    if not p.exists(): errors.append(f'MISSING: {rel}')
    return p

def text(rel):
    p=req(rel)
    return p.read_text(encoding='utf-8') if p.exists() else ''

required = [
    'app/src/main/java/com/althmany/extractor/MainActivity.kt',
    'app/src/main/java/com/althmany/extractor/ExtractorApp.kt',
    'app/src/main/java/com/althmany/extractor/engine/ExtractionController.kt',
    'app/src/main/java/com/althmany/extractor/engine/ScanController.kt',
    'app/src/main/java/com/althmany/extractor/engine/PublishController.kt',
    'app/src/main/java/com/althmany/extractor/engine/RuntimeOperationCoordinator.kt',
    'app/src/main/java/com/althmany/extractor/engine/SenderRuntimeGuard.kt',
    'app/src/main/java/com/althmany/extractor/engine/ConversationTitlePolicy.kt',
    'app/src/main/java/com/althmany/extractor/accessibility/WhatsAppAccessibilityService.kt',
    'app/src/main/java/com/althmany/extractor/data/ExtractorDatabase.kt',
    'app/src/main/java/com/althmany/extractor/data/ExtractorRepository.kt',
    'app/src/main/java/com/althmany/extractor/shizuku/ShizukuBridge.kt',
    'app/src/main/java/com/althmany/extractor/shizuku/ShizukuShellUserService.kt',
    'app/src/main/aidl/com/althmany/extractor/shizuku/IShizukuShellService.aidl',
    'app/src/main/res/xml/extractor_accessibility_service_config.xml',
    'app/src/main/res/drawable/ic_extractor_tool.xml',
]
for x in required: req(x)

app_build=text('app/build.gradle.kts')
root_build=text('build.gradle.kts')
manifest=text('app/src/main/AndroidManifest.xml')
app=text('app/src/main/java/com/althmany/groupmanager/GroupManagerApp.kt')
main=text('app/src/main/java/com/althmany/groupmanager/ui/MainActivity.kt')
vm=text('app/src/main/java/com/althmany/extractor/ui/AppViewModel.kt')
feature=text('app/src/main/java/com/althmany/extractor/ExtractorApp.kt')
menu=text('app/src/main/res/menu/menu_main.xml')
shizuku=text('app/src/main/java/com/althmany/extractor/shizuku/ShizukuBridge.kt')
coordinator=text('app/src/main/java/com/althmany/extractor/engine/RuntimeOperationCoordinator.kt')
extraction=text('app/src/main/java/com/althmany/extractor/engine/ExtractionController.kt')
scan=text('app/src/main/java/com/althmany/extractor/engine/ScanController.kt')
publish=text('app/src/main/java/com/althmany/extractor/engine/PublishController.kt')
ui_adapter=text('app/src/main/java/com/althmany/extractor/engine/WhatsAppUiAdapter.kt')
accessibility=text('app/src/main/java/com/althmany/extractor/accessibility/WhatsAppAccessibilityService.kt')
app_prefs=text('app/src/main/java/com/althmany/groupmanager/data/AppPreferences.kt')
quick_join=text('app/src/main/java/com/althmany/groupmanager/accessibility/QuickJoinAccessibilityService.kt')
shizuku_sender=text('app/src/main/java/com/althmany/groupmanager/shizuku/ShizukuAutomationService.kt')

checks={
    'Compose plugin root': 'org.jetbrains.kotlin.plugin.compose' in root_build,
    'Compose plugin app': 'id("org.jetbrains.kotlin.plugin.compose")' in app_build,
    'Compose enabled': 'compose = true' in app_build,
    'Compose BOM integrated': 'compose-bom:2026.04.01' in app_build,
    'Extractor activity manifest': 'com.althmany.extractor.MainActivity' in manifest,
    'Extractor accessibility manifest': 'com.althmany.extractor.accessibility.WhatsAppAccessibilityService' in manifest,
    'Extractor notification receivers': all(x in manifest for x in ['ExtractionActionReceiver','ScanActionReceiver','PublishActionReceiver']),
    'Unique extractor accessibility XML': '@xml/extractor_accessibility_service_config' in manifest,
    'Sender remains Application': 'android:name=".GroupManagerApp"' in manifest,
    'Feature runtime object': 'object ExtractorFeatureRuntime' in feature,
    'Feature runtime controller initialization': all(x in feature for x in ['ExtractionController.initialize','ScanController.initialize','PublishController.initialize']),
    'Sender initializes feature runtime': 'ExtractorFeatureRuntime.initialize(this)' in app,
    'Sender menu entry': 'action_extractor_workspace' in menu and 'R.id.action_extractor_workspace' in main,
    'Sender/Extractor operation collision guard': 'SENDER_OPERATION_ACTIVE' in vm and 'accessibilityBatchRunning' in vm,
    'Extractor R points to Sender namespace': 'import com.althmany.groupmanager.R' in text('app/src/main/java/com/althmany/extractor/notification/ExtractionNotifier.kt'),
    'Extractor BuildConfig points to Sender namespace': 'import com.althmany.groupmanager.BuildConfig' in shizuku,
    'Extraction engine present': 'object ExtractionController' in text('app/src/main/java/com/althmany/extractor/engine/ExtractionController.kt'),
    'Scan engine present': 'object ScanController' in text('app/src/main/java/com/althmany/extractor/engine/ScanController.kt'),
    'Publish engine present': 'object PublishController' in text('app/src/main/java/com/althmany/extractor/engine/PublishController.kt'),
    'Shizuku AIDL enabled': 'aidl = true' in app_build,
    '4.0 Sender operation type': 'SENDER("الانضمام")' in coordinator,
    '4.0 idempotent Sender ownership': 'fun ensureOwned(operation: RuntimeOperation)' in coordinator,
    '4.0 Sender lifecycle owns UI': 'ensureOwned(RuntimeOperation.SENDER)' in app_prefs and 'release(RuntimeOperation.SENDER)' in app_prefs,
    '4.0 Accessibility restores Sender ownership': 'UI_OPERATION_COLLISION' in quick_join and 'ensureOwned(RuntimeOperation.SENDER)' in quick_join,
    '4.0 Shizuku restores Sender ownership': 'UI_OPERATION_COLLISION' in shizuku_sender and 'ensureOwned(RuntimeOperation.SENDER)' in shizuku_sender,
    '4.0 extraction Sender guards': extraction.count('SenderRuntimeGuard.isSenderRunning(appContext)') >= 3,
    '4.0 scan Sender guard': scan.count('SenderRuntimeGuard.isSenderRunning(appContext)') >= 1,
    '4.0 publish Sender guards': publish.count('SenderRuntimeGuard.isSenderRunning(appContext)') >= 2,
    '4.0 Sender route isolation': 'RuntimeOperation.SENDER -> Unit' in accessibility,
    '4.0 pure conversation title policy': 'object ConversationTitlePolicy' in text('app/src/main/java/com/althmany/extractor/engine/ConversationTitlePolicy.kt'),
    '4.0 adapter uses title policy': 'ConversationTitlePolicy.isCandidate(value)' in ui_adapter,
}
for label,ok in checks.items():
    print(('PASS' if ok else 'FAIL')+': '+label)
    if not ok: errors.append(label)

# Namespace hazards caused by embedding a second package into the Sender Android module.
for p in (ROOT/'app/src/main/java/com/althmany/extractor').rglob('*.kt'):
    s=p.read_text(encoding='utf-8',errors='ignore')
    if 'import com.althmany.extractor.R' in s:
        errors.append(f'BAD R NAMESPACE: {p.relative_to(ROOT)}')
    if 'import com.althmany.extractor.BuildConfig' in s:
        errors.append(f'BAD BUILDCONFIG NAMESPACE: {p.relative_to(ROOT)}')
    if 'application as ExtractorApp' in s:
        errors.append(f'BAD APPLICATION CAST: {p.relative_to(ROOT)}')

if errors:
    print('\nINTEGRATED EXTRACTOR VALIDATION FAILED')
    for e in errors: print(' -',e)
    sys.exit(1)
print('\nINTEGRATED EXTRACTOR SOURCE CONTRACT: PASS')
