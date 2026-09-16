package com.althmany.extractor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.althmany.extractor.data.PublishContentMode
import com.althmany.extractor.engine.ExtractionController
import com.althmany.extractor.engine.PublishController
import com.althmany.extractor.engine.ScanActionMode
import com.althmany.extractor.engine.ScanController
import com.althmany.extractor.export.ExportFormat
import com.althmany.extractor.export.ExportManager
import com.althmany.extractor.join.OriginalJoinCoordinator
import com.althmany.groupmanager.GroupManagerApp
import com.althmany.groupmanager.ui.MainViewModel
import com.althmany.groupmanager.util.DocumentIO
import com.althmany.extractor.ui.AlThmanyTheme
import com.althmany.extractor.ui.AppScreen
import com.althmany.extractor.ui.AppViewModel
import com.althmany.extractor.ui.LogsScreen
import com.althmany.extractor.ui.ResultsScreen
import com.althmany.extractor.ui.WorkspaceBottomBar
import com.althmany.extractor.ui.WorkspaceExtractionScreen
import com.althmany.extractor.ui.WorkspaceGroupsScreen
import com.althmany.extractor.ui.WorkspaceGlobalMiniBar
import com.althmany.extractor.ui.WorkspaceHomeScreen
import com.althmany.extractor.ui.WorkspacePublishScreen
import com.althmany.extractor.ui.WorkspaceScanScreen
import com.althmany.extractor.ui.WorkspaceSettingsScreen
import com.althmany.extractor.ui.V341BottomBar
import com.althmany.extractor.ui.V341HomeScreen
import com.althmany.extractor.ui.V341JoinScreen
import com.althmany.extractor.ui.V341ExtractionScreen
import com.althmany.extractor.ui.V341ScanScreen
import com.althmany.extractor.ui.V341PublishScreen
import com.althmany.extractor.ui.ProfessionalJoinScreen
import com.althmany.extractor.ui.ProfessionalScanScreen
import com.althmany.extractor.ui.V341DiagnosticsScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private val senderViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application as GroupManagerApp)
    }
    private lateinit var joinCoordinator: OriginalJoinCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ExtractorFeatureRuntime.initialize(applicationContext)
        joinCoordinator = OriginalJoinCoordinator(
            context = this,
            app = application as GroupManagerApp,
            senderViewModel = senderViewModel,
            scope = lifecycleScope
        )
        setContent {
            AlThmanyTheme { ExtractorAppUi(viewModel, joinCoordinator) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRuntimeEnvironment()
    }
}

@Composable
private fun ExtractorAppUi(viewModel: AppViewModel, joinCoordinator: OriginalJoinCoordinator) {
    val context = LocalContext.current
    val engine by viewModel.engineState.collectAsState()
    val runtimeTarget by viewModel.runtimeTarget.collectAsState()
    val remoteRuntimeTargets by viewModel.remoteRuntimeTargets.collectAsState()
    val joinState by joinCoordinator.state.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val links by viewModel.links.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val scanItems by viewModel.scanItems.collectAsState()
    val publishState by viewModel.publishState.collectAsState()
    val publishItems by viewModel.publishItems.collectAsState()
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var pendingFormat by remember { mutableStateOf(ExportFormat.XLSX) }
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        viewModel.refreshRuntimeEnvironment()
        viewModel.refresh()
    }

    LaunchedEffect(engine.accessibilityEnabledInSettings, engine.serviceConnected) {
        if (engine.accessibilityEnabledInSettings && !engine.serviceConnected) {
            repeat(20) {
                delay(400L)
                viewModel.refreshRuntimeEnvironment()
                if (viewModel.engineState.value.serviceConnected) return@LaunchedEffect
            }
        }
    }

    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) runCatching { ExportManager.export(context.contentResolver, uri, pendingFormat, links) }
    }
    val createScanDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) runCatching { ExportManager.exportScan(context.contentResolver, uri, pendingFormat, scanItems) }
    }
    val createPublishDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) runCatching { ExportManager.exportPublish(context.contentResolver, uri, pendingFormat, publishItems) }
    }

    val openGroupFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.takeIf { it.isNotBlank() }?.let { text ->
                viewModel.addGroups(text)
                screen = AppScreen.GROUPS
            }
        }
    }

    val openScanFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.importScanFile(uri)
        }
    }

    val openJoinFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            DocumentIO.readText(context.contentResolver, uri, 120_000)
                .onSuccess(joinCoordinator::setDraft)
        }
    }

    val pickPublishAttachment = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.setPublishAttachment(uri.toString(), context.contentResolver.getType(uri))
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("إعدادات المحرك") },
            text = { Text("يمكنك فتح إعدادات Accessibility أو طلب/اختبار Shizuku. إعدادات السرعة والاستخراج موجودة مباشرة في لوحة التحكم.") },
            confirmButton = {
                TextButton(onClick = { showSettings = false; ExtractionController.openAccessibilitySettings() }) { Text("Accessibility") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false; ExtractionController.probeShizuku() }) { Text("اختبار Shizuku") }
            }
        )
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("المساعدة") },
            text = { Text("1) اختر نسخة واتساب. 2) فعّل Accessibility أو Shizuku. 3) مزامنة القروبات من صفحة المجموعات. 4) اختر القروبات. 5) ابدأ التشغيل الذكي. الاستخراج يفتح القروب من الذاكرة بدون Search ثم يقرأ الرسائل ويستخرج الروابط أثناء التمرير.") },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("حسنًا") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val extractionRunning = engine.status in setOf(
                com.althmany.extractor.data.EngineStatus.PREPARING,
                com.althmany.extractor.data.EngineStatus.SYNCING_GROUPS,
                com.althmany.extractor.data.EngineStatus.OPENING_WHATSAPP,
                com.althmany.extractor.data.EngineStatus.SEARCHING_GROUP,
                com.althmany.extractor.data.EngineStatus.OPENING_GROUP,
                com.althmany.extractor.data.EngineStatus.VERIFYING_GROUP,
                com.althmany.extractor.data.EngineStatus.EXTRACTING,
                com.althmany.extractor.data.EngineStatus.LINKS_TAB,
                com.althmany.extractor.data.EngineStatus.VERIFYING_END,
                com.althmany.extractor.data.EngineStatus.RECOVERING,
                com.althmany.extractor.data.EngineStatus.PROFILE_MISMATCH
            )
            val anyRunning = joinState.running || extractionRunning || scanState.running || publishState.running
            val anyPaused = joinState.paused || engine.status == com.althmany.extractor.data.EngineStatus.PAUSED || scanState.paused || publishState.paused
            // Persistent mini bar = real global sequential pipeline.
            val startEnabled = engine.selectedWhatsAppPackage != null
            val operationLabel = when {
                joinState.running || joinState.paused -> "الانضمام • ${joinState.message}"
                publishState.running || publishState.paused -> "النشر • ${publishState.info}"
                scanState.running || scanState.paused -> if (scanState.actionMode == ScanActionMode.SCAN_ONLY) "الفحص • ${scanState.message}" else "الانضمام • ${scanState.message}"
                extractionRunning || engine.status == com.althmany.extractor.data.EngineStatus.PAUSED -> "الاستخراج • ${engine.message}"
                else -> "التحكم العام • لا توجد عملية نشطة"
            }
            androidx.compose.foundation.layout.Column {
                WorkspaceGlobalMiniBar(
                    operationLabel = operationLabel,
                    running = anyRunning,
                    paused = anyPaused,
                    startEnabled = startEnabled,
                    onStart = viewModel::startAllSmart,
                    onPause = {
                        if (joinState.running || joinState.paused) joinCoordinator.pause()
                        else viewModel.pauseActiveOperation()
                    },
                    onResume = {
                        if (joinState.paused) joinCoordinator.resume()
                        else viewModel.resumeActiveOperation()
                    },
                    onStopAll = {
                        if (joinState.running || joinState.paused) joinCoordinator.stop()
                        viewModel.stopAllOperations()
                    }
                )
                V341BottomBar(current = screen) { target ->
                    screen = target
                    when (target) {
                        AppScreen.RESULTS -> viewModel.reloadLinks()
                        AppScreen.GROUPS -> viewModel.refresh()
                        AppScreen.PUBLISH -> viewModel.reloadPublishItems()
                        AppScreen.SCAN -> viewModel.importScanLinksFromExtraction()
                        AppScreen.JOIN -> joinCoordinator.refresh()
                        AppScreen.LOGS -> viewModel.reloadLogs()
                        else -> Unit
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val zero = PaddingValues(
                top = scaffoldPadding.calculateTopPadding(),
                bottom = scaffoldPadding.calculateBottomPadding()
            )
            when (screen) {
                AppScreen.HOME -> V341HomeScreen(
                    padding = zero,
                    engine = engine,
                    runtime = runtimeTarget,
                    scan = scanState,
                    publish = publishState,
                    onExtract = { screen = AppScreen.EXTRACT },
                    onScan = { viewModel.importScanLinksFromExtraction(); screen = AppScreen.SCAN },
                    onPublish = { viewModel.reloadPublishItems(); screen = AppScreen.PUBLISH },
                    onAutoJoin = { screen = AppScreen.JOIN },
                    onTargetWhatsApp = viewModel::setTargetWhatsApp,
                    onBackendPreference = viewModel::setRuntimeBackendPreference,
                    remoteTargets = remoteRuntimeTargets,
                    onDiscoverRemoteTargets = viewModel::discoverRemoteRuntimeTargets,
                    onRemoteTarget = viewModel::setRemoteRuntimeTarget,
                    onStart = viewModel::startExtractionSmart,
                    onPause = viewModel::pauseActiveOperation,
                    onResume = viewModel::resumeActiveOperation,
                    onStopAll = viewModel::stopAllOperations,
                    onSettings = { screen = AppScreen.SETTINGS }
                )

                AppScreen.EXTRACT -> V341ExtractionScreen(
                    padding = zero,
                    engine = engine,
                    runtime = runtimeTarget,
                    groups = groups,
                    accessibilityEnabled = isAccessibilityServiceEnabled(context),
                    onTargetWhatsApp = viewModel::setTargetWhatsApp,
                    onBackendPreference = viewModel::setRuntimeBackendPreference,
                    onMode = viewModel::setMode,
                    onSpeed = viewModel::setSpeed,
                    onRounds = viewModel::setMaxRounds,
                    onSync = viewModel::syncGroups,
                    onSelected = viewModel::setSelected,
                    onPreset = viewModel::applyGroupSelectionPreset,
                    onGroups = { screen = AppScreen.GROUPS },
                    onResults = { viewModel.reloadLinks(); screen = AppScreen.RESULTS },
                    onStart = viewModel::startExtractionSmart,
                    onPause = viewModel::pauseActiveOperation,
                    onResume = viewModel::resumeActiveOperation,
                    onStopAll = viewModel::stopAllOperations,
                    onOpenWhatsApp = { ExtractionController.openWhatsApp() }
                )

                AppScreen.JOIN -> ProfessionalJoinScreen(
                    padding = zero,
                    engine = engine,
                    runtime = runtimeTarget,
                    join = joinState,
                    onTargetWhatsApp = viewModel::setTargetWhatsApp,
                    onBackendPreference = viewModel::setRuntimeBackendPreference,
                    onDraft = joinCoordinator::setDraft,
                    onImportFile = {
                        openJoinFile.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "text/plain",
                                "application/octet-stream"
                            )
                        )
                    },
                    onStart = joinCoordinator::start,
                    onPause = joinCoordinator::pause,
                    onResume = joinCoordinator::resume,
                    onStop = joinCoordinator::stop
                )

                AppScreen.SCAN -> ProfessionalScanScreen(
                    padding = zero,
                    engine = engine,
                    runtime = runtimeTarget,
                    scan = scanState,
                    scanItems = scanItems,
                    onTargetWhatsApp = viewModel::setTargetWhatsApp,
                    onBackendPreference = viewModel::setRuntimeBackendPreference,
                    onAddLinks = viewModel::addScanLinks,
                    onImportExtraction = viewModel::importScanLinksFromExtraction,
                    onImportFile = {
                        openScanFile.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "text/csv",
                                "text/plain",
                                "application/octet-stream"
                            )
                        )
                    },
                    onSpeed = viewModel::setScanSpeed,
                    onAttempts = viewModel::setScanMaxAttempts,
                    onStart = viewModel::startScanWithInput,
                    onPause = viewModel::pauseActiveOperation,
                    onResume = viewModel::resumeActiveOperation,
                    onStop = viewModel::stopAllOperations,
                    onClear = viewModel::clearScan,
                    onExport = { format ->
                        pendingFormat = format
                        createScanDocument.launch("AL-thmany-scan.${format.extension}")
                    }
                )

                AppScreen.PUBLISH -> V341PublishScreen(
                    padding = zero,
                    engine = engine,
                    runtime = runtimeTarget,
                    publish = publishState,
                    groups = groups,
                    onTargetWhatsApp = viewModel::setTargetWhatsApp,
                    onBackendPreference = viewModel::setRuntimeBackendPreference,
                    onGroups = { screen = AppScreen.GROUPS },
                    onDraft = viewModel::setPublishDraft,
                    onContentMode = viewModel::setPublishContentMode,
                    onPickAttachment = { mode ->
                        val types = if (mode == PublishContentMode.IMAGE_WITH_CAPTION) arrayOf("image/*") else arrayOf("text/x-vcard", "text/vcard", "text/plain", "*/*")
                        pickPublishAttachment.launch(types)
                    },
                    onClearAttachment = { viewModel.setPublishAttachment(null, null) },
                    onSpeed = viewModel::setPublishSpeed,
                    onNavigation = viewModel::setPublishNavigationMode,
                    onAttempts = viewModel::setPublishMaxAttempts,
                    onStart = viewModel::startPublishSmart,
                    onPause = viewModel::pauseActiveOperation,
                    onResume = viewModel::resumeActiveOperation,
                    onStopAll = viewModel::stopAllOperations,
                    onExport = { format ->
                        pendingFormat = format
                        createPublishDocument.launch("AL-thmany-publish.${format.extension}")
                    }
                )

                AppScreen.GROUPS -> WorkspaceGroupsScreen(
                    padding = zero,
                    engine = engine,
                    groups = groups.filter { group ->
                        val selectedPackage = engine.selectedWhatsAppPackage
                        selectedPackage == null || group.whatsappPackage.isBlank() || group.whatsappPackage == selectedPackage
                    },
                    syncing = engine.status == com.althmany.extractor.data.EngineStatus.SYNCING_GROUPS,
                    syncFound = engine.syncFound,
                    onSync = viewModel::syncGroups,
                    onSelected = viewModel::setSelected,
                    onPreset = viewModel::applyGroupSelectionPreset,
                    onStartExtraction = { screen = AppScreen.EXTRACT; viewModel.startExtractionSmart() },
                    onPause = viewModel::pauseActiveOperation,
                    onResume = viewModel::resumeActiveOperation,
                    onStopAll = viewModel::stopAllOperations
                )

                AppScreen.RESULTS -> ResultsScreen(
                    padding = zero,
                    links = links,
                    onRefresh = viewModel::reloadLinks,
                    onExport = { format ->
                        pendingFormat = format
                        createDocument.launch("AL-thmany-links.${format.extension}")
                    },
                    onClearAll = viewModel::clearAll
                )

                AppScreen.LOGS -> V341DiagnosticsScreen(zero) { screen = AppScreen.SETTINGS }

                AppScreen.SETTINGS -> WorkspaceSettingsScreen(
                    padding = zero,
                    engine = engine,
                    scan = scanState,
                    publish = publishState,
                    onTargetWhatsApp = viewModel::setTargetWhatsApp,
                    onSpeed = viewModel::setSpeed,
                    onRetries = viewModel::setExtractionRetries,
                    onDelayMs = viewModel::setExtractionDelayMs,
                    onScanAttempts = viewModel::setScanMaxAttempts,
                    onPublishAttempts = viewModel::setPublishMaxAttempts,
                    onPublishNavigation = viewModel::setPublishNavigationMode,
                    onOpenAccessibility = ExtractionController::openAccessibilitySettings,
                    onRequestShizuku = { ExtractionController.requestShizukuPermission() },
                    onProbeShizuku = ExtractionController::probeShizuku,
                    onStart = viewModel::startAllSmart,
                    onPause = viewModel::pauseActiveOperation,
                    onResume = viewModel::resumeActiveOperation,
                    onStopAll = viewModel::stopAllOperations
                )
            }
        }
    }

}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/com.althmany.extractor.accessibility.WhatsAppAccessibilityService"
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
