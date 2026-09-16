package com.althmany.extractor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.althmany.extractor.ExtractorFeatureRuntime
import com.althmany.groupmanager.GroupManagerApp
import com.althmany.groupmanager.model.PreferredTarget
import com.althmany.groupmanager.model.AutomationBackend
import com.althmany.groupmanager.domain.RuntimeSpeedMode
import com.althmany.groupmanager.util.WhatsAppLauncher
import com.althmany.extractor.profile.RuntimeBackendPreference
import com.althmany.extractor.profile.UnifiedRemoteTarget
import com.althmany.extractor.profile.UnifiedRuntimeSnapshot
import com.althmany.extractor.profile.UnifiedRuntimeTargetStore
import com.althmany.extractor.profile.WhatsAppInstanceRegistry
import com.althmany.extractor.data.ExtractionMode
import com.althmany.extractor.data.LinkRecord
import com.althmany.extractor.data.ExtractionLog
import com.althmany.extractor.data.GroupSelectionPreset
import com.althmany.extractor.data.SpeedProfile
import com.althmany.extractor.data.ScanRecord
import com.althmany.extractor.data.TargetGroup
import com.althmany.extractor.data.PublishContentMode
import com.althmany.extractor.data.PublishItem
import com.althmany.extractor.engine.ExtractionController
import com.althmany.extractor.engine.ExtractionUiState
import com.althmany.extractor.engine.ScanController
import com.althmany.extractor.engine.PublishController
import com.althmany.extractor.engine.PublishUiState
import com.althmany.extractor.engine.PublishSpeedProfile
import com.althmany.extractor.engine.PublishNavigationMode
import com.althmany.extractor.engine.ScanUiState
import com.althmany.extractor.engine.ScanScope
import com.althmany.extractor.engine.ScanActionMode
import com.althmany.extractor.engine.ScanSpeedProfile
import com.althmany.extractor.engine.RuntimeOperationCoordinator
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo get() = ExtractorFeatureRuntime.repository

    val engineState: StateFlow<ExtractionUiState> = ExtractionController.state
    val scanState: StateFlow<ScanUiState> = ScanController.state
    val publishState: StateFlow<PublishUiState> = PublishController.state

    private val _runtimeTarget = MutableStateFlow(
        UnifiedRuntimeTargetStore.resolve(
            application,
            ExtractionController.state.value.selectedWhatsAppPackage
        )
    )
    val runtimeTarget: StateFlow<UnifiedRuntimeSnapshot> = _runtimeTarget.asStateFlow()

    private val _remoteRuntimeTargets = MutableStateFlow<List<UnifiedRemoteTarget>>(emptyList())
    val remoteRuntimeTargets: StateFlow<List<UnifiedRemoteTarget>> = _remoteRuntimeTargets.asStateFlow()

    private val _groups = MutableStateFlow<List<TargetGroup>>(emptyList())
    val groups: StateFlow<List<TargetGroup>> = _groups.asStateFlow()

    private val _links = MutableStateFlow<List<LinkRecord>>(emptyList())
    val links: StateFlow<List<LinkRecord>> = _links.asStateFlow()

    private val _logs = MutableStateFlow<List<ExtractionLog>>(emptyList())
    val logs: StateFlow<List<ExtractionLog>> = _logs.asStateFlow()

    private val _scanItems = MutableStateFlow<List<ScanRecord>>(emptyList())
    val scanItems: StateFlow<List<ScanRecord>> = _scanItems.asStateFlow()

    private val _publishItems = MutableStateFlow<List<PublishItem>>(emptyList())
    val publishItems: StateFlow<List<PublishItem>> = _publishItems.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var globalJob: Job? = null
    private var targetDiscoveryJob: Job? = null

    private suspend fun <T> queryIo(block: suspend () -> T): T =
        withContext(Dispatchers.IO) { block() }

    init {
        viewModelScope.launch {
            engineState
                .map { it.selectedWhatsAppPackage }
                .distinctUntilChanged()
                .collect { packageName ->
                    _runtimeTarget.value = UnifiedRuntimeTargetStore.resolve(
                        getApplication<Application>(),
                        packageName
                    )
                }
        }
        viewModelScope.launch {
            engineState
                .map { it.shizukuReady }
                .distinctUntilChanged()
                .collect { ready ->
                    if (ready && _remoteRuntimeTargets.value.isEmpty()) {
                        refreshRemoteTargetsInternal(silent = true)
                    }
                }
        }
        refresh()
        viewModelScope.launch {
            var lastIndex = -1
            var lastTerminal = false
            ScanController.state.collect { state ->
                val terminal = state.status.name in setOf("COMPLETED", "ERROR", "STOPPED")
                if (state.currentIndex != lastIndex || terminal != lastTerminal) {
                    lastIndex = state.currentIndex
                    lastTerminal = terminal
                    _scanItems.value = queryIo { repo.scanItems() }
                }
            }
        }
        viewModelScope.launch {
            var lastIndex = -1
            var lastRunId: Long? = null
            var lastTerminal = false
            PublishController.state.collect { state ->
                val terminal = state.status.name in setOf("COMPLETED", "ERROR", "STOPPED")
                val runId = state.activeRunId
                if (state.currentIndex != lastIndex || runId != lastRunId || terminal != lastTerminal) {
                    lastIndex = state.currentIndex
                    lastRunId = runId
                    lastTerminal = terminal
                    _publishItems.value = if (runId == null) emptyList()
                    else queryIo { repo.publishItems(runId) }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val publishRunId = PublishController.state.value.activeRunId
            val groups = queryIo { repo.groups() }
            val links = queryIo { repo.links() }
            val logs = queryIo { repo.logs() }
            val scanItems = queryIo { repo.scanItems() }
            val publishItems = if (publishRunId == null) emptyList()
            else queryIo { repo.publishItems(publishRunId) }

            _groups.value = groups
            _links.value = links
            _logs.value = logs
            _scanItems.value = scanItems
            _publishItems.value = publishItems

            ExtractionController.refreshStats()
            ScanController.refreshStats()
            PublishController.refreshStats()
        }
    }

    fun addGroups(text: String) {
        viewModelScope.launch {
            repo.addGroupsFromText(text, engineState.value.selectedWhatsAppPackage.orEmpty())
            _groups.value = repo.groups()
            _message.value = "تم حفظ قائمة المجموعات"
        }
    }

    fun syncGroups() {
        viewModelScope.launch {
            runCatching { ExtractionController.syncGroupsNow() }
                .onSuccess { added -> _message.value = "اكتملت المزامنة — أضيف $added عنصر جديد" }
                .onFailure { _message.value = it.message ?: "تعذرت المزامنة" }
            _groups.value = repo.groups()
            ExtractionController.refreshStats()
        }
    }

    private suspend fun ensureGroupsReady(): Boolean {
        val packageName = engineState.value.selectedWhatsAppPackage
        if (packageName.isNullOrBlank()) {
            _message.value = "RUNTIME_NO_TARGET: اختر نسخة واتساب أولاً"
            return false
        }
        fun usable(list: List<TargetGroup>) = list.filter {
            !it.stale && it.active && (it.discovered || it.selected) &&
                (it.whatsappPackage.isBlank() || it.whatsappPackage == packageName)
        }

        var current = usable(repo.groups())
        if (current.isNotEmpty()) {
            _groups.value = repo.groups()
            return true
        }

        _message.value = "SYNC_REQUIRED: لا توجد قروبات — بدء المزامنة تلقائيًا"
        val sync = runCatching { ExtractionController.syncGroupsNow() }
        if (sync.isFailure) {
            _message.value = "SYNC_FAILED: ${sync.exceptionOrNull()?.message ?: "تعذرت المزامنة"}"
            _groups.value = repo.groups()
            return false
        }

        val all = repo.groups()
        _groups.value = all
        current = usable(all)
        if (current.isEmpty()) {
            _message.value = "NO_GROUPS_SYNCED: انتهت المزامنة بدون قروبات مؤكدة"
            return false
        }
        _message.value = "SYNC_READY: ${current.size} قروب جاهز"
        return true
    }

    private suspend fun prepareExplicitOperation(target: String): Boolean {
        val senderApp = getApplication<Application>() as? GroupManagerApp
        if (senderApp?.preferences?.accessibilityBatchRunning == true) {
            _message.value = "SENDER_OPERATION_ACTIVE: أوقف تشغيل Sender الحالي أولاً حتى لا تتحكم عمليتان بواجهة واتساب معًا"
            return false
        }
        when (target) {
            "EXTRACTION" -> {
                if (ScanController.isRunning()) ScanController.stop()
                if (PublishController.isRunning()) PublishController.stop()
            }
            "SCAN" -> {
                if (ExtractionController.isBusy()) ExtractionController.stop()
                if (PublishController.isRunning()) PublishController.stop()
            }
            "PUBLISH" -> {
                if (ExtractionController.isBusy()) ExtractionController.stop()
                if (ScanController.isRunning()) ScanController.stop()
            }
        }

        var wait = 0
        while (
            (ExtractionController.isBusy() ||
                (target != "SCAN" && ScanController.isRunning()) ||
                (target != "PUBLISH" && PublishController.isRunning())) &&
            wait++ < 50
        ) {
            delay(50L)
        }

        val owner = RuntimeOperationCoordinator.current()
        if (owner != null) {
            _message.value = "OPERATION_SWITCH_TIMEOUT: ما زالت ${owner.labelAr} تحرر واجهة واتساب"
            return false
        }
        return true
    }

    private fun applyUnifiedPerformanceProfile() {
        // Fastest supported pacing for every engine; reliability retry policies stay engine-owned.
        ExtractionController.setSpeed(SpeedProfile.HYPER)
        ExtractionController.setBetweenItemsDelayMs(0L)

        ScanController.setSpeed(ScanSpeedProfile.HYPER)
        ScanController.setMaxAttempts(1)

        PublishController.setSpeed(PublishSpeedProfile.INSTANT)

        (getApplication<Application>() as? GroupManagerApp)?.preferences?.apply {
            runtimeSpeedMode = RuntimeSpeedMode.MAX
            fastHandsFreeMode = true
            interLinkDelayMs = 0
            autoAdvance = true
        }
    }

    fun startExtractionSmart() {
        if (globalJob?.isActive == true) return
        viewModelScope.launch {
            if (!prepareExplicitOperation("EXTRACTION")) return@launch
            applyUnifiedPerformanceProfile()
            if (ensureGroupsReady()) ExtractionController.start()
        }
    }

    fun startScanWithInput(input: String) {
        if (globalJob?.isActive == true) return
        viewModelScope.launch {
            if (input.isNotBlank()) {
                val added = queryIo { repo.addScanLinksFromText(input) }
                _message.value = "تمت إضافة $added رابط جديد — بدء الفحص"
            }
            _scanItems.value = queryIo { repo.scanItems() }
            ScanController.refreshStats()
            if (_scanItems.value.isEmpty()) {
                _message.value = "NO_SCAN_ITEMS: الصق روابط أو استوردها من الاستخراج"
                return@launch
            }
            if (!prepareExplicitOperation("SCAN")) return@launch
            applyUnifiedPerformanceProfile()
            // 3.4.1: Scan classifies only. Membership actions belong to original Sender/Join.
            ScanController.setActionMode(ScanActionMode.SCAN_ONLY)
            ScanController.setRequestToJoinEnabled(false)
            ScanController.start()
        }
    }

    fun startPublishSmart(message: String) {
        if (globalJob?.isActive == true) return
        viewModelScope.launch {
            if (message.isBlank()) {
                _message.value = "NO_PUBLISH_MESSAGE: اكتب رسالة النشر أولاً"
                return@launch
            }
            if (!prepareExplicitOperation("PUBLISH")) return@launch
            applyUnifiedPerformanceProfile()
            PublishController.start(message)
        }
    }

    private suspend fun waitExtractionFinished(): Boolean {
        var started = false
        while (true) {
            started = started || ExtractionController.isBusy()
            val status = engineState.value.status
            if (status == com.althmany.extractor.data.EngineStatus.ERROR ||
                status == com.althmany.extractor.data.EngineStatus.STOPPED) return false
            if (started && !ExtractionController.isBusy()) {
                return status == com.althmany.extractor.data.EngineStatus.COMPLETED
            }
            delay(100L)
        }
    }

    private suspend fun waitScanFinished(): Boolean {
        var started = false
        while (true) {
            started = started || ScanController.isRunning()
            val status = scanState.value.status
            if (status == com.althmany.extractor.engine.ScanEngineStatus.ERROR ||
                status == com.althmany.extractor.engine.ScanEngineStatus.STOPPED) return false
            if (started && !ScanController.isRunning()) {
                return status == com.althmany.extractor.engine.ScanEngineStatus.COMPLETED
            }
            delay(100L)
        }
    }

    private suspend fun waitPublishFinished(): Boolean {
        var started = false
        while (true) {
            started = started || PublishController.isRunning()
            val status = publishState.value.status
            if (status == com.althmany.extractor.engine.PublishEngineStatus.ERROR ||
                status == com.althmany.extractor.engine.PublishEngineStatus.STOPPED) return false
            if (started && !PublishController.isRunning()) {
                return status == com.althmany.extractor.engine.PublishEngineStatus.COMPLETED
            }
            delay(100L)
        }
    }

    fun startAllSmart() {
        if (globalJob?.isActive == true ||
            ExtractionController.isBusy() || ScanController.isRunning() || PublishController.isRunning()) {
            _message.value = "لا يمكن بدء الكل: توجد عملية نشطة"
            return
        }

        globalJob = viewModelScope.launch {
            applyUnifiedPerformanceProfile()
            if (!ensureGroupsReady()) return@launch

            _message.value = "1/4 • القروبات جاهزة — بدء الاستخراج"
            ExtractionController.start()
            if (!waitExtractionFinished()) {
                _message.value = "PIPELINE_STOPPED: الاستخراج • ${engineState.value.message}"
                return@launch
            }

            val imported = queryIo { repo.importInviteLinksFromExtraction() }
            _scanItems.value = queryIo { repo.scanItems() }
            ScanController.refreshStats()

            if (_scanItems.value.isNotEmpty()) {
                _message.value = "3/4 • بدء الفحص والتصنيف • جديد $imported"
                ScanController.setActionMode(ScanActionMode.SCAN_ONLY)
                ScanController.setRequestToJoinEnabled(false)
                ScanController.start()
                if (!waitScanFinished()) {
                    _message.value = "PIPELINE_STOPPED: الفحص • ${scanState.value.message}"
                    return@launch
                }
            } else {
                _message.value = "3/4 • لا توجد روابط دعوة — تخطي الفحص"
            }

            val draft = publishState.value.messageText.trim()
            if (draft.isNotBlank()) {
                _message.value = "4/4 • بدء النشر"
                PublishController.start(draft)
                if (!waitPublishFinished()) {
                    _message.value = "PIPELINE_STOPPED: النشر • ${publishState.value.info}"
                    return@launch
                }
                _message.value = "PIPELINE_COMPLETED: اكتملت جميع المراحل"
            } else {
                _message.value = "PIPELINE_COMPLETED: اكتمل الاستخراج والفحص — النشر متخطى لعدم وجود رسالة"
            }
        }
    }

    fun setMode(mode: ExtractionMode) = ExtractionController.setMode(mode)
    fun setSpeed(speed: SpeedProfile) = ExtractionController.setSpeed(speed)
    fun setMaxRounds(value: Int) = ExtractionController.setMaxScrollIterations(value)
    fun setExtractionRetries(value: Int) = ExtractionController.setMaxSameGroupRetries(value)
    fun setExtractionDelayMs(value: Long) = ExtractionController.setBetweenItemsDelayMs(value)
    fun setTargetWhatsApp(packageName: String) {
        val activeSender = (getApplication<Application>() as? GroupManagerApp)
            ?.preferences?.accessibilityBatchRunning == true
        if (activeSender) {
            _message.value = "أوقف الانضمام الحالي قبل تغيير نسخة واتساب"
            return
        }
        ExtractionController.setTargetWhatsAppPackage(packageName)
        UnifiedRuntimeTargetStore.setLocalTarget(getApplication<Application>(), packageName)
        if (!ExtractorFeatureRuntime.switchRuntimeEnvironment(getApplication<Application>())) {
            _message.value = "تعذر تبديل بيئة البيانات أثناء وجود عملية نشطة"
            return
        }
        val senderApp = getApplication<Application>() as? GroupManagerApp
        if (senderApp != null) {
            val selectedLabel = engineState.value.availableWhatsApp
                .firstOrNull { it.packageName == packageName }
                ?.labelAr
                ?: WhatsAppInstanceRegistry.labelFor(packageName)
            senderApp.preferences.clearRemoteSecureTarget()
            senderApp.preferences.selectedWhatsAppPackage = packageName
            senderApp.preferences.selectedWhatsAppLabel = selectedLabel
            senderApp.preferences.preferredTarget = PreferredTarget.AUTO
        }
        _runtimeTarget.value = UnifiedRuntimeTargetStore.resolve(getApplication<Application>(), packageName)
        refresh()
    }

    private suspend fun refreshRemoteTargetsInternal(silent: Boolean) {
        if (!engineState.value.shizukuReady) {
            if (!silent) _message.value = "Shizuku غير جاهز لاكتشاف Dual / Work / Secure"
            return
        }
        if (!silent) {
            _message.value = "جارٍ اكتشاف جميع بيئات واتساب عبر Shizuku…"
        }
        val result = runCatching {
            withContext(Dispatchers.IO) {
                UnifiedRuntimeTargetStore.discoverRemoteTargets(getApplication<Application>())
            }
        }
        val targets = result.getOrElse {
            if (!silent) _message.value = "فشل اكتشاف البيئات: ${it.message.orEmpty()}"
            emptyList()
        }
        _remoteRuntimeTargets.value = targets
        if (!silent) {
            _message.value = if (targets.isEmpty()) {
                "لم يجد Shizuku نسخة واتساب إضافية قابلة للتحقق"
            } else {
                "تم اكتشاف ${targets.size} نسخة واتساب إضافية"
            }
        }
    }

    fun discoverRemoteRuntimeTargets() {
        if (ExtractionController.isBusy() || ScanController.isRunning() || PublishController.isRunning()) {
            _message.value = "أوقف العملية الحالية قبل تحديث نسخ واتساب"
            return
        }
        if (targetDiscoveryJob?.isActive == true) return
        targetDiscoveryJob = viewModelScope.launch {
            refreshRemoteTargetsInternal(silent = false)
        }
    }

    fun refreshTargetCatalog() {
        if (ExtractionController.isBusy() || ScanController.isRunning() || PublishController.isRunning()) {
            _message.value = "أوقف العملية الحالية قبل تحديث نسخ واتساب"
            return
        }
        if (targetDiscoveryJob?.isActive == true) return
        targetDiscoveryJob = viewModelScope.launch {
            _message.value = "جارٍ تحديث جميع نسخ واتساب…"
            runCatching {
                withContext(Dispatchers.IO) {
                    WhatsAppInstanceRegistry.available(getApplication<Application>(), forceRefresh = true)
                }
            }
            ExtractionController.refreshRuntimeEnvironment()
            _runtimeTarget.value = UnifiedRuntimeTargetStore.resolve(
                getApplication<Application>(),
                engineState.value.selectedWhatsAppPackage
            )
            if (engineState.value.shizukuReady) {
                refreshRemoteTargetsInternal(silent = true)
            }
            _message.value = "اكتمل تحديث نسخ واتساب"
        }
    }

    fun setRemoteRuntimeTarget(target: UnifiedRemoteTarget) {
        if (ExtractionController.isBusy() || ScanController.isRunning() || PublishController.isRunning()) {
            _message.value = "أوقف العملية الحالية قبل تغيير بيئة التشغيل"
            return
        }
        val senderAppBeforeSwitch = getApplication<Application>() as? GroupManagerApp
        if (senderAppBeforeSwitch?.preferences?.accessibilityBatchRunning == true) {
            _message.value = "أوقف الانضمام الحالي قبل تغيير بيئة التشغيل"
            return
        }
        UnifiedRuntimeTargetStore.setRemoteTarget(getApplication<Application>(), target)
        if (!ExtractorFeatureRuntime.switchRuntimeEnvironment(getApplication<Application>())) {
            _message.value = "تعذر تبديل بيئة البيانات أثناء وجود عملية نشطة"
            return
        }
        ExtractionController.refreshRuntimeEnvironment()

        val senderApp = getApplication<Application>() as? GroupManagerApp
        if (senderApp != null) {
            senderApp.preferences.setRemoteSecureTarget(
                target.androidUserId,
                target.packageName,
                target.environmentLabel
            )
            senderApp.preferences.automationBackend = AutomationBackend.SHIZUKU
            senderApp.preferences.runtimeAutomationBackend = AutomationBackend.SHIZUKU
            senderApp.preferences.selectedWhatsAppPackage = target.packageName
            senderApp.preferences.selectedWhatsAppLabel = target.whatsappLabel
            senderApp.preferences.preferredTarget = PreferredTarget.AUTO
        }

        _runtimeTarget.value = UnifiedRuntimeTargetStore.resolve(getApplication<Application>())
        refresh()
        _message.value = "تم اختيار ${target.environmentLabel} • ${target.whatsappLabel} عبر Shizuku"
    }

    fun setRuntimeBackendPreference(value: RuntimeBackendPreference) {
        if (ExtractionController.isBusy() || ScanController.isRunning() || PublishController.isRunning()) {
            _message.value = "أوقف العملية الحالية قبل تغيير محرك التشغيل"
            return
        }
        val senderApp = getApplication<Application>() as? GroupManagerApp
        if (senderApp?.preferences?.accessibilityBatchRunning == true) {
            _message.value = "أوقف الانضمام الحالي قبل تغيير محرك التشغيل"
            return
        }
        if (_runtimeTarget.value.remoteTarget && value == RuntimeBackendPreference.ACCESSIBILITY) {
            _message.value = "الهدف في Android user آخر يحتاج Shizuku؛ Accessibility تعمل داخل الملف المحلي فقط"
            return
        }
        UnifiedRuntimeTargetStore.setPreference(getApplication<Application>(), value)
        (getApplication<Application>() as? GroupManagerApp)?.preferences?.let { prefs ->
            prefs.automationBackend = when (value) {
                RuntimeBackendPreference.AUTO -> AutomationBackend.AUTO
                RuntimeBackendPreference.ACCESSIBILITY -> AutomationBackend.ACCESSIBILITY
                RuntimeBackendPreference.SHIZUKU -> AutomationBackend.SHIZUKU
            }
        }
        ExtractionController.refreshRuntimeEnvironment()
        _runtimeTarget.value = UnifiedRuntimeTargetStore.resolve(
            getApplication<Application>(),
            engineState.value.selectedWhatsAppPackage
        )
        _message.value = "تم اختيار محرك التشغيل: ${value.labelAr}"
    }

    fun refreshRuntimeEnvironment() {
        ExtractionController.refreshRuntimeEnvironment()
        _runtimeTarget.value = UnifiedRuntimeTargetStore.resolve(
            getApplication<Application>(),
            engineState.value.selectedWhatsAppPackage
        )
    }

    /** Global controls used by every screen. Only the engine that currently owns WhatsApp reacts. */
    fun pauseActiveOperation() {
        when {
            publishState.value.running && !publishState.value.paused -> PublishController.pause()
            scanState.value.running && !scanState.value.paused -> ScanController.pause()
            engineState.value.status !in setOf(
                com.althmany.extractor.data.EngineStatus.IDLE,
                com.althmany.extractor.data.EngineStatus.PAUSED,
                com.althmany.extractor.data.EngineStatus.COMPLETED,
                com.althmany.extractor.data.EngineStatus.STOPPED,
                com.althmany.extractor.data.EngineStatus.ERROR
            ) -> ExtractionController.pause()
        }
    }

    fun resumeActiveOperation() {
        when {
            publishState.value.paused -> PublishController.resume()
            scanState.value.paused -> ScanController.resume()
            engineState.value.status == com.althmany.extractor.data.EngineStatus.PAUSED -> ExtractionController.resume()
        }
    }

    /** Emergency/global stop: cancels every engine and releases the single WhatsApp UI owner. */
    fun stopAllOperations() {
        globalJob?.cancel()
        globalJob = null
        ExtractionController.stop()
        ScanController.stop()
        PublishController.stop()
        _message.value = "تم إيقاف جميع العمليات والمسار العام"
    }

    fun setSelected(id: Long, selected: Boolean) {
        viewModelScope.launch {
            queryIo { repo.setSelected(id, selected) }
            _groups.value = queryIo { repo.groups() }
            ExtractionController.refreshStats()
        }
    }

    fun setAllSelected(selected: Boolean) {
        viewModelScope.launch {
            queryIo { repo.setAllSelected(selected) }
            _groups.value = queryIo { repo.groups() }
            ExtractionController.refreshStats()
        }
    }

    fun applyGroupSelectionPreset(preset: GroupSelectionPreset) {
        viewModelScope.launch {
            queryIo { repo.setSelectionPreset(preset, engineState.value.selectedWhatsAppPackage) }
            _groups.value = queryIo { repo.groups() }
            ExtractionController.refreshStats()
            _message.value = preset.labelAr
        }
    }

    fun addScanLinks(text: String) {
        viewModelScope.launch {
            val added = queryIo { repo.addScanLinksFromText(text) }
            _scanItems.value = queryIo { repo.scanItems() }
            ScanController.refreshStats()
            _message.value = "تمت إضافة $added رابط دعوة جديد للفحص"
        }
    }

    fun importScanFile(uri: android.net.Uri) {
        if (globalJob?.isActive == true) return
        viewModelScope.launch {
            val links = runCatching {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.althmany.extractor.engine.ScanInputImporter.readLinks(
                        getApplication<Application>().contentResolver,
                        uri
                    )
                }
            }.getOrElse {
                _message.value = "FILE_IMPORT_FAILED: ${it.message ?: "تعذر قراءة الملف"}"
                return@launch
            }
            if (links.isEmpty()) {
                _message.value = "لم يتم العثور على روابط دعوات واتساب داخل الملف"
                return@launch
            }
            val added = queryIo { repo.addScanLinksFromText(links.joinToString("\n")) }
            _scanItems.value = queryIo { repo.scanItems() }
            ScanController.refreshStats()
            _message.value = "تم استيراد $added رابط جديد من الملف • الإجمالي ${_scanItems.value.size}"
        }
    }

    fun importScanLinksFromExtraction() {
        viewModelScope.launch {
            val added = queryIo { repo.importInviteLinksFromExtraction() }
            _scanItems.value = queryIo { repo.scanItems() }
            ScanController.refreshStats()
            _message.value = "تم استيراد $added رابط دعوة جديد من نتائج الاستخراج"
        }
    }

    fun reloadScanItems() {
        viewModelScope.launch {
            _scanItems.value = queryIo { repo.scanItems() }
            ScanController.refreshStats()
        }
    }

    fun setScanSpeed(value: ScanSpeedProfile) = ScanController.setSpeed(value)
    fun setScanScope(value: ScanScope) = ScanController.setScope(value)
    fun setScanActionMode(value: ScanActionMode) = ScanController.setActionMode(value)
    fun setScanRequestToJoinEnabled(value: Boolean) = ScanController.setRequestToJoinEnabled(value)
    fun setScanMaxAttempts(value: Int) = ScanController.setMaxAttempts(value)

    fun clearScan() {
        ScanController.stop()
        viewModelScope.launch {
            queryIo { repo.clearScan() }
            _scanItems.value = emptyList()
            ScanController.refreshStats()
            _message.value = "تم مسح نتائج الفحص"
        }
    }

    fun setPublishSpeed(value: PublishSpeedProfile) = PublishController.setSpeed(value)
    fun setPublishMaxAttempts(value: Int) = PublishController.setMaxAttempts(value)
    fun setPublishNavigationMode(value: PublishNavigationMode) = PublishController.setNavigationMode(value)
    fun setPublishDraft(value: String) = PublishController.setDraft(value)
    fun setPublishContentMode(value: PublishContentMode) = PublishController.setContentMode(value)
    fun setPublishAttachment(uri: String?, mime: String?) = PublishController.setAttachment(uri, mime)
    fun reloadPublishItems() {
        viewModelScope.launch {
            val runId = PublishController.state.value.activeRunId
            _publishItems.value = if (runId == null) emptyList() else queryIo { repo.publishItems(runId) }
            PublishController.refreshStats(runId)
        }
    }
    fun clearPublishHistory() {
        PublishController.clearHistory()
        _publishItems.value = emptyList()
    }

    fun clearAll() {
        viewModelScope.launch {
            ExtractionController.stop()
            ScanController.stop()
            PublishController.stop()
            queryIo { repo.clearAll() }
            refresh()
            _message.value = "تم مسح البيانات"
        }
    }

    fun reloadLinks() { viewModelScope.launch { _links.value = queryIo { repo.links() } } }
    fun reloadLogs() { viewModelScope.launch { _logs.value = queryIo { repo.logs() } } }
    fun consumeMessage() { _message.value = null }
}
