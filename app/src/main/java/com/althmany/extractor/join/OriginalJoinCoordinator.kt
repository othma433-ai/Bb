package com.althmany.extractor.join

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.althmany.extractor.engine.RuntimeOperation
import com.althmany.extractor.engine.RuntimeOperationCoordinator
import com.althmany.extractor.profile.RuntimeBackendPreference
import com.althmany.extractor.profile.UnifiedRuntimeTargetStore
import com.althmany.groupmanager.GroupManagerApp
import com.althmany.groupmanager.accessibility.AccessibilityStatus
import com.althmany.groupmanager.accessibility.QuickJoinAccessibilityService
import com.althmany.groupmanager.domain.AutomationPolicy
import com.althmany.groupmanager.domain.AutomationStopReason
import com.althmany.groupmanager.domain.NativeEngineSetupAction
import com.althmany.groupmanager.domain.RestrictionHandlingMode
import com.althmany.groupmanager.domain.SessionRules
import com.althmany.groupmanager.model.AutomationBackend
import com.althmany.groupmanager.model.LinkSource
import com.althmany.groupmanager.shizuku.ShizukuAutomationService
import com.althmany.groupmanager.shizuku.ShizukuBridge
import com.althmany.groupmanager.ui.MainEvent
import com.althmany.groupmanager.ui.MainViewModel
import com.althmany.groupmanager.util.AutomationScreenAwakeGuard
import com.althmany.groupmanager.util.LaunchDestination
import com.althmany.groupmanager.util.NativeProfileEngineRouter
import com.althmany.groupmanager.util.QuickJoinNotification
import com.althmany.groupmanager.util.WhatsAppLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class OriginalJoinUiState(
    val draft: String = "",
    val running: Boolean = false,
    val paused: Boolean = false,
    val total: Int = 0,
    val joined: Int = 0,
    val requested: Int = 0,
    val failed: Int = 0,
    val remaining: Int = 0,
    val progressPercent: Int = 0,
    val currentPosition: Int? = null,
    val backend: String = "—",
    val selectedPackage: String? = null,
    val message: String = "جاهز للانضمام"
)

/**
 * Adapter from the 3.4.1 Compose workspace to the original Sender engine.
 *
 * This class deliberately contains no WhatsApp button-matching or membership-click implementation.
 * Those actions remain owned by QuickJoinAccessibilityService/ShizukuAutomationService.
 */
class OriginalJoinCoordinator(
    private val context: Context,
    private val app: GroupManagerApp,
    private val senderViewModel: MainViewModel,
    private val scope: CoroutineScope
) {
    private val prefs get() = app.preferences
    private val _state = MutableStateFlow(OriginalJoinUiState())
    val state: StateFlow<OriginalJoinUiState> = _state.asStateFlow()

    init {
        scope.launch {
            senderViewModel.events.collect(::handleEvent)
        }
        scope.launch {
            while (isActive) {
                refresh()
                delay(700L)
            }
        }
    }

    fun setDraft(value: String) {
        _state.value = _state.value.copy(draft = value.take(MAX_DRAFT_CHARS))
    }

    fun refresh() {
        val snapshot = senderViewModel.state.value.snapshot
        val stats = snapshot?.stats
        val current = snapshot?.links?.let {
            SessionRules.currentOpened(it) ?: SessionRules.nextActionable(it)
        }
        _state.value = _state.value.copy(
            running = prefs.accessibilityBatchRunning,
            paused = prefs.accessibilityPaused,
            total = stats?.total ?: 0,
            joined = stats?.joined ?: 0,
            requested = stats?.requested ?: 0,
            failed = stats?.failed ?: 0,
            remaining = stats?.remaining ?: 0,
            progressPercent = stats?.progressPercent ?: 0,
            currentPosition = current?.position?.plus(1),
            backend = prefs.runtimeAutomationBackend.name,
            selectedPackage = prefs.runtimeLockedWhatsAppPackage ?: prefs.selectedWhatsAppPackage
        )
    }

    fun start() {
        if (prefs.accessibilityBatchRunning) {
            if (prefs.accessibilityPaused) resume() else setMessage("الانضمام يعمل بالفعل")
            return
        }

        if (RuntimeOperationCoordinator.isOwnedByOther(RuntimeOperation.SENDER)) {
            val owner = RuntimeOperationCoordinator.current()?.labelAr ?: "عملية أخرى"
            setMessage("لا يمكن بدء الانضمام أثناء تشغيل $owner")
            return
        }

        scope.launch {
            val snapshot = senderViewModel.state.value.snapshot
            val raw = _state.value.draft.trim()

            if (raw.isBlank() && snapshot != null && snapshot.stats.remaining > 0) {
                beginSession(snapshot.sessionId, snapshot.stats.total)
                return@launch
            }

            if (raw.isBlank()) {
                setMessage("ألصق روابط الدعوة أولاً")
                return@launch
            }

            if (resolveBackend() == null || !validateAndLockTarget(dryRun = true)) return@launch
            configureRunPreferences()
            setMessage("تحضير الروابط للمحرك الأصلي")
            senderViewModel.prepareAutomaticRun(
                rawText = raw,
                source = LinkSource.PASTE,
                sourceLabel = "AL-thmany 3.4.1 Arabic Workspace"
            )
        }
    }

    fun pause() {
        if (!prefs.accessibilityBatchRunning || prefs.accessibilityPaused) return
        prefs.pauseAccessibilityBatch("Paused from 3.4.1 Arabic workspace")
        AutomationScreenAwakeGuard.sync(context, false)
        showNotification()
        setMessage("تم إيقاف الانضمام مؤقتاً")
        refresh()
    }

    fun resume() {
        if (!prefs.accessibilityBatchRunning || !prefs.accessibilityPaused) return
        prefs.resumeAccessibilityBatch("Resumed from 3.4.1 Arabic workspace")
        AutomationScreenAwakeGuard.sync(context, prefs.keepScreenAwake)

        val snapshot = senderViewModel.state.value.snapshot
        val current = snapshot?.links?.let(SessionRules::currentOpened)
        when (prefs.runtimeAutomationBackend) {
            AutomationBackend.SHIZUKU -> ShizukuAutomationService.start(context)
            AutomationBackend.ACCESSIBILITY -> {
                if (current == null) senderViewModel.openNextOrCurrent()
                else QuickJoinAccessibilityService.requestImmediateScan()
            }
            AutomationBackend.AUTO -> senderViewModel.openNextOrCurrent()
        }

        showNotification()
        setMessage("تم استكمال الانضمام")
        refresh()
    }

    fun stop() {
        if (prefs.runtimeAutomationBackend == AutomationBackend.SHIZUKU) {
            runCatching { ShizukuAutomationService.stop(context) }
        }
        prefs.stopAccessibilityBatch(
            AutomationStopReason.USER_STOPPED,
            "Stopped from 3.4.1 Arabic workspace"
        )
        QuickJoinNotification.cancel(context)
        AutomationScreenAwakeGuard.sync(context, false)
        senderViewModel.refresh()
        setMessage("تم إيقاف الانضمام نهائياً")
        refresh()
    }

    private fun configureRunPreferences() {
        prefs.runtimeShadowMode = false
        prefs.autoAdvance = true
        prefs.autoPauseOutsideWhatsApp = true
        prefs.autoResumeCurrentRun = false
        prefs.keepScreenAwake = true
        prefs.restrictionHandlingMode = RestrictionHandlingMode.SKIP_AND_CONTINUE
        prefs.interLinkDelayMs = prefs.runtimeSpeedProfile().interLinkDelayMs.toInt()
        prefs.accessibilityActionTimeoutSeconds = AutomationPolicy.FAST_ACTION_TIMEOUT_SECONDS
    }

    private fun resolveBackend(): AutomationBackend? {
        if (prefs.hasValidRemoteSecureTarget()) {
            if (runCatching { ShizukuBridge.status().ready }.getOrDefault(false)) {
                prefs.runtimeAutomationBackend = AutomationBackend.SHIZUKU
                prefs.accessibilityQuickJoin = false
                return AutomationBackend.SHIZUKU
            }
            setMessage("الهدف البعيد يحتاج Shizuku جاهزاً")
            return null
        }

        return when (UnifiedRuntimeTargetStore.preference(context)) {
            RuntimeBackendPreference.ACCESSIBILITY -> {
                if (!AccessibilityStatus.isQuickJoinServiceConnectedLocally(context)) {
                    setMessage("Accessibility محددة كمحرك لكنها غير متصلة داخل البيئة الحالية")
                    null
                } else {
                    prefs.runtimeAutomationBackend = AutomationBackend.ACCESSIBILITY
                    prefs.accessibilityQuickJoin = true
                    AutomationBackend.ACCESSIBILITY
                }
            }

            RuntimeBackendPreference.SHIZUKU -> {
                if (!runCatching { ShizukuBridge.status().ready }.getOrDefault(false)) {
                    setMessage("Shizuku محدد كمحرك لكنه غير جاهز/غير مصرح")
                    null
                } else {
                    prefs.runtimeAutomationBackend = AutomationBackend.SHIZUKU
                    prefs.accessibilityQuickJoin = false
                    AutomationBackend.SHIZUKU
                }
            }

            RuntimeBackendPreference.AUTO -> {
                val decision = NativeProfileEngineRouter.inspect(context, prefs.automationBackend).decision
                if (decision.runnable) {
                    val backend = decision.backend ?: return null
                    prefs.runtimeAutomationBackend = backend
                    prefs.accessibilityQuickJoin = backend == AutomationBackend.ACCESSIBILITY
                    backend
                } else {
                    when (decision.setupAction) {
                        NativeEngineSetupAction.ENABLE_LOCAL_ACCESSIBILITY -> {
                            setMessage("فعّل Accessibility الخاصة بالتطبيق ثم أعد المحاولة")
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                        NativeEngineSetupAction.START_OR_AUTHORIZE_SHIZUKU ->
                            setMessage("شغّل Shizuku وامنح التطبيق الإذن ثم أعد المحاولة")
                        NativeEngineSetupAction.APPLY_WORK_ACCESSIBILITY_POLICY ->
                            setMessage("بيئة العمل تحتاج Accessibility محلية أو Shizuku جاهز")
                        NativeEngineSetupAction.BLOCKED_BY_PROFILE_POLICY ->
                            setMessage("سياسة Android Profile تمنع محرك التحكم")
                        NativeEngineSetupAction.NONE ->
                            setMessage("لا يوجد محرك تحكم جاهز")
                    }
                    null
                }
            }
        }
    }

    private fun validateAndLockTarget(dryRun: Boolean): Boolean {
        if (prefs.hasValidRemoteSecureTarget()) {
            val userId = prefs.remoteSecureAndroidUserId
            val packageName = prefs.remoteSecureWhatsAppPackage ?: run {
                setMessage("هدف الملف المتقدم غير مكتمل")
                return false
            }
            if (!dryRun) {
                prefs.lockRuntimeTarget(packageName, "REMOTE_PROFILE:u$userId")
                if (!prefs.lockRuntimeAndroidUserId(userId)) {
                    setMessage("تعارض Android user مع الهدف المحدد")
                    return false
                }
            }
            return true
        }

        val validation = WhatsAppLauncher.validateTarget(
            context,
            prefs.preferredTarget,
            prefs.selectedWhatsAppPackage
        )
        val packageName = validation.packageName
        if (!validation.valid || packageName.isNullOrBlank()) {
            setMessage(
                if (validation.explicitTargetRequired)
                    "اختر نسخة واتساب صراحةً لهذا الملف"
                else
                    "نسخة واتساب المحددة غير متاحة أو لا تفتح روابط الدعوة"
            )
            return false
        }

        if (!dryRun) prefs.lockRuntimeTarget(packageName, validation.profileKey)
        return true
    }

    private fun beginSession(sessionId: String, totalLinks: Int) {
        scope.launch {
            val backend = resolveBackend() ?: return@launch
            configureRunPreferences()
            if (!validateAndLockTarget(dryRun = false)) return@launch

            prefs.runtimeAutomationBackend = backend
            prefs.accessibilityQuickJoin = backend == AutomationBackend.ACCESSIBILITY
            prefs.autoAdvance = true

            if (backend == AutomationBackend.ACCESSIBILITY &&
                !AccessibilityStatus.isQuickJoinServiceConnectedLocally(context)
            ) {
                setMessage("Accessibility غير متصلة محلياً؛ فعّل الخدمة ثم اضغط بدء مرة أخرى")
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                return@launch
            }

            if (!prefs.startAccessibilityBatch(sessionId)) {
                val owner = RuntimeOperationCoordinator.current()?.labelAr ?: "عملية أخرى"
                setMessage("تعذر بدء الانضمام لأن $owner يمتلك واجهة واتساب")
                return@launch
            }

            AutomationScreenAwakeGuard.sync(context, true)

            if (backend == AutomationBackend.ACCESSIBILITY) {
                QuickJoinNotification.showAutomation(
                    context = context,
                    processedInBatch = 0,
                    currentLinkNumber = 1,
                    totalLinks = totalLinks,
                    delaySeconds = prefs.accessibilityJoinDelaySeconds,
                    paused = false
                )
                senderViewModel.openNextOrCurrent()
            } else {
                ShizukuAutomationService.start(context)
            }

            setMessage("بدأ الانضمام بالمحرك الأصلي • ${backend.name}")
            refresh()
        }
    }

    private fun handleEvent(event: MainEvent) {
        when (event) {
            is MainEvent.Message -> {
                val text = runCatching {
                    context.getString(event.messageRes, *event.args.toTypedArray())
                }.getOrDefault("تحديث من محرك الانضمام")
                setMessage(text)
                refresh()
            }

            is MainEvent.AutomaticSessionReady ->
                beginSession(event.sessionId, event.totalLinks)

            is MainEvent.OpenLink ->
                openInvitation(event)

            is MainEvent.ExportCsv,
            is MainEvent.ShareText -> Unit
        }
    }

    private fun openInvitation(event: MainEvent.OpenLink) {
        if (prefs.runtimeLockedWhatsAppPackage.isNullOrBlank() &&
            !validateAndLockTarget(dryRun = false)
        ) return

        val destination = WhatsAppLauncher.launch(
            context = context,
            url = event.url,
            preferredTarget = event.preferredTarget,
            selectedPackage = prefs.runtimeLockedWhatsAppPackage ?: prefs.selectedWhatsAppPackage,
            strictProfileTarget = prefs.strictProfileTargeting,
            expectedProfileKey = prefs.runtimeLockedProfileKey
        )

        val supported = destination in setOf(
            LaunchDestination.PERSONAL,
            LaunchDestination.BUSINESS,
            LaunchDestination.CLONED,
            LaunchDestination.SELECTED,
            LaunchDestination.DUAL_CHOOSER
        )

        if (supported) {
            prefs.markAutomationLaunched()
            if (prefs.runtimeAutomationBackend == AutomationBackend.ACCESSIBILITY) {
                QuickJoinAccessibilityService.requestImmediateScan()
            }
            senderViewModel.onLaunchResult(
                linkId = event.linkId,
                success = true,
                browserFallback = false
            )
            return
        }

        senderViewModel.onLaunchResult(
            linkId = event.linkId,
            success = destination != LaunchDestination.NONE,
            browserFallback = destination == LaunchDestination.BROWSER
        )

        scope.launch {
            delay(100L)
            if (prefs.accessibilityBatchRunning && !prefs.accessibilityPaused) {
                senderViewModel.openNextOrCurrent()
            }
        }
    }

    private fun showNotification() {
        if (!prefs.accessibilityBatchRunning) return
        val snapshot = senderViewModel.state.value.snapshot ?: return
        val current = SessionRules.currentOpened(snapshot.links)
            ?: SessionRules.nextActionable(snapshot.links)

        QuickJoinNotification.showAutomation(
            context = context,
            processedInBatch = prefs.accessibilityProcessedCount,
            currentLinkNumber = current?.position?.plus(1),
            totalLinks = snapshot.stats.total,
            delaySeconds = prefs.accessibilityJoinDelaySeconds,
            paused = prefs.accessibilityPaused
        )
    }

    private fun setMessage(value: String) {
        _state.value = _state.value.copy(message = value.take(260))
    }

    companion object {
        private const val MAX_DRAFT_CHARS = 120_000
    }
}
