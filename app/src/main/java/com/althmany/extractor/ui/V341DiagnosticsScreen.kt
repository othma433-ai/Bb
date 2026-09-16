package com.althmany.extractor.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.althmany.extractor.engine.RuntimeOperationCoordinator
import com.althmany.extractor.profile.UnifiedRuntimeTargetStore
import com.althmany.groupmanager.GroupManagerApp
import com.althmany.groupmanager.accessibility.AccessibilityStatus
import com.althmany.groupmanager.shizuku.ShizukuBridge
import com.althmany.groupmanager.util.DocumentIO
import com.althmany.groupmanager.util.ProfileEnvironment
import com.althmany.groupmanager.util.RuntimeDiagnosticStore
import com.althmany.groupmanager.util.RuntimeHealthMonitor
import kotlinx.coroutines.delay

private val DBg = Color(0xFF020B13)
private val DBg2 = Color(0xFF061522)
private val DPanel = Color(0xFF081722)
private val DLine = Color(0xFF183448)
private val DCyan = Color(0xFF08D9FF)
private val DGreen = Color(0xFF00E696)
private val DOrange = Color(0xFFFFB347)
private val DRed = Color(0xFFFF536F)
private val DText = Color(0xFFF2F7FA)
private val DMuted = Color(0xFF91A3B2)

data class V341DiagnosticSnapshot(
    val accessibilityConfigured: Boolean,
    val accessibilityConnected: Boolean,
    val accessibilityProfile: String,
    val accessibilityHeartbeatMs: Long?,
    val shizukuBinderAlive: Boolean,
    val shizukuPermission: Boolean,
    val shizukuReady: Boolean,
    val shizukuUserServiceBound: Boolean,
    val androidProfile: String,
    val selectedWhatsApp: String,
    val runtimeBackend: String,
    val operationOwner: String,
    val automationStage: String,
    val automationDiagnostic: String,
    val healthDirective: String,
    val healthAction: String,
    val healthConflict: String,
    val healthConfidence: String,
    val healthWatchdog: String,
    val recentLog: String
) {
    fun report(): String = buildString {
        appendLine("AL-thmany 3.4.1 Runtime Diagnostic")
        appendLine("============================================================")
        appendLine("Android profile: $androidProfile")
        appendLine("Selected WhatsApp: $selectedWhatsApp")
        appendLine("Operation owner: $operationOwner")
        appendLine("Runtime backend: $runtimeBackend")
        appendLine("Accessibility configured: $accessibilityConfigured")
        appendLine("Accessibility connected: $accessibilityConnected")
        appendLine("Accessibility profile: $accessibilityProfile")
        appendLine("Accessibility heartbeat ms: ${accessibilityHeartbeatMs ?: -1}")
        appendLine("Shizuku binder alive: $shizukuBinderAlive")
        appendLine("Shizuku permission: $shizukuPermission")
        appendLine("Shizuku ready: $shizukuReady")
        appendLine("Shizuku user service bound: $shizukuUserServiceBound")
        appendLine("Automation stage: $automationStage")
        appendLine("Automation diagnostic: $automationDiagnostic")
        appendLine("Health directive: $healthDirective")
        appendLine("Health action: $healthAction")
        appendLine("Health conflict: $healthConflict")
        appendLine("Health confidence: $healthConfidence")
        appendLine("Health watchdog: $healthWatchdog")
        appendLine()
        appendLine("Recent runtime journal")
        appendLine("------------------------------------------------------------")
        append(recentLog.ifBlank { "No runtime diagnostic records yet." })
    }
}

private fun captureV341Diagnostics(context: Context): V341DiagnosticSnapshot {
    val app = context.applicationContext as GroupManagerApp
    val readiness = AccessibilityStatus.readiness(context)
    val shizuku = runCatching { ShizukuBridge.status() }.getOrNull()
    val health = RuntimeHealthMonitor.snapshot()
    val profile = ProfileEnvironment.current(context)
    val prefs = app.preferences
    val unified = UnifiedRuntimeTargetStore.resolve(
        context,
        UnifiedRuntimeTargetStore.selectedPackage(context) ?: prefs.selectedWhatsAppPackage
    )

    return V341DiagnosticSnapshot(
        accessibilityConfigured = readiness.systemEnabled,
        accessibilityConnected = readiness.localServiceConnected,
        accessibilityProfile = readiness.profileKey,
        accessibilityHeartbeatMs = readiness.heartbeatAgeMs,
        shizukuBinderAlive = shizuku?.binderAlive == true,
        shizukuPermission = shizuku?.permissionGranted == true,
        shizukuReady = shizuku?.ready == true,
        shizukuUserServiceBound = shizuku?.userServiceBound == true,
        androidProfile = if (unified.remoteTarget) "${unified.environmentLabel} (user ${unified.targetAndroidUserId})" else unified.profileInfo.profileKey,
        selectedWhatsApp = unified.selectedWhatsAppLabel,
        runtimeBackend = "${unified.preference.name} -> ${unified.effectiveBackend.name}",
        operationOwner = RuntimeOperationCoordinator.current()?.labelAr ?: "لا توجد عملية",
        automationStage = prefs.automationStage.name,
        automationDiagnostic = prefs.automationDiagnostic.ifBlank { "لا يوجد" },
        healthDirective = health?.directive ?: "—",
        healthAction = health?.action ?: "—",
        healthConflict = health?.conflict ?: "—",
        healthConfidence = health?.confidence?.let { "$it%" } ?: "—",
        healthWatchdog = health?.watchdog ?: "—",
        recentLog = RuntimeDiagnosticStore.readRecent(context, 16_000)
    )
}

@Composable
fun V341DiagnosticsScreen(padding: PaddingValues, onSettings: () -> Unit) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(captureV341Diagnostics(context)) }
    var status by remember { mutableStateOf("") }

    val export = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            DocumentIO.writeText(context.contentResolver, uri, snapshot.report())
                .onSuccess { status = "تم تصدير تقرير التشخيص" }
                .onFailure { status = "فشل التصدير: ${it.message.orEmpty()}" }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            snapshot = captureV341Diagnostics(context)
            delay(700L)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DBg, DBg2, DBg)))
            .padding(padding)
    ) {
        val side = if (maxWidth >= 600.dp) 28.dp else 16.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = side, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DCard(accent = DCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "التشخيص الحقيقي",
                                color = DText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "حالة المحركات والهدف والـRuntime والأخطاء الفعلية",
                                color = DMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.End
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Default.Settings, null, tint = DCyan, modifier = Modifier.size(31.dp))
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, DCyan),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Settings, null, tint = DCyan)
                    Spacer(Modifier.width(6.dp))
                    Text("فتح إعدادات التطبيق", color = DText)
                }
            }

            item {
                DCard {
                    DTitle("بيئة التشغيل")
                    DLine("Android profile", snapshot.androidProfile)
                    DLine("واتساب المحدد", snapshot.selectedWhatsApp)
                    DLine("مالك واجهة واتساب", snapshot.operationOwner)
                    DLine("المحرك الفعلي", snapshot.runtimeBackend)
                }
            }

            item {
                DCard(accent = if (snapshot.accessibilityConnected) DGreen else DOrange) {
                    DTitle("Accessibility")
                    DLine("مهيأة", if (snapshot.accessibilityConfigured) "نعم" else "لا")
                    DLine("متصلة محلياً", if (snapshot.accessibilityConnected) "نعم" else "لا")
                    DLine("الملف", snapshot.accessibilityProfile)
                    DLine(
                        "Heartbeat",
                        snapshot.accessibilityHeartbeatMs?.let { "$it ms" } ?: "غير متاح"
                    )
                }
            }

            item {
                DCard(accent = if (snapshot.shizukuReady) DGreen else DOrange) {
                    DTitle("Shizuku")
                    DLine("Binder", if (snapshot.shizukuBinderAlive) "متصل" else "غير متصل")
                    DLine("الإذن", if (snapshot.shizukuPermission) "ممنوح" else "غير ممنوح")
                    DLine("جاهز", if (snapshot.shizukuReady) "نعم" else "لا")
                    DLine(
                        "UserService",
                        if (snapshot.shizukuUserServiceBound) "مرتبط" else "غير مرتبط"
                    )
                }
            }

            item {
                DCard {
                    DTitle("حالة التنفيذ")
                    DLine("Stage", snapshot.automationStage)
                    DLine("Diagnostic", snapshot.automationDiagnostic)
                    DLine("Directive", snapshot.healthDirective)
                    DLine("Action", snapshot.healthAction)
                    DLine("Conflict", snapshot.healthConflict)
                    DLine("Confidence", snapshot.healthConfidence)
                    DLine("Watchdog", snapshot.healthWatchdog)
                }
            }

            item {
                DCard {
                    DTitle("سجل Runtime الأخير")
                    Text(
                        snapshot.recentLog.ifBlank { "لا توجد سجلات حتى الآن" },
                        color = DText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("AL-thmany diagnostics", snapshot.report())
                            )
                            status = "تم نسخ تقرير التشخيص"
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, DCyan)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, tint = DCyan)
                        Spacer(Modifier.width(5.dp))
                        Text("نسخ", color = DText)
                    }

                    OutlinedButton(
                        onClick = { export.launch("AL-thmany-runtime-diagnostics.txt") },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, DGreen)
                    ) {
                        Icon(Icons.Default.Download, null, tint = DGreen)
                        Spacer(Modifier.width(5.dp))
                        Text("تصدير", color = DText)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            snapshot = captureV341Diagnostics(context)
                            status = "تم تحديث التشخيص"
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, DCyan)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = DCyan)
                        Spacer(Modifier.width(5.dp))
                        Text("تحديث", color = DText)
                    }

                    OutlinedButton(
                        onClick = {
                            RuntimeDiagnosticStore.clear(context)
                            snapshot = captureV341Diagnostics(context)
                            status = "تم مسح سجل التشخيص"
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, DRed)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = DRed)
                        Spacer(Modifier.width(5.dp))
                        Text("مسح السجل", color = DRed)
                    }
                }
            }

            if (status.isNotBlank()) {
                item {
                    Text(
                        status,
                        color = DCyan,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun DCard(
    accent: Color = DLine,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = DPanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .72f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@Composable
private fun DTitle(value: String) {
    Text(
        value,
        color = DCyan,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End
    )
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun DLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            value,
            color = DText,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(
            label,
            color = DMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
    Spacer(Modifier.height(4.dp))
}
