package com.althmany.extractor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.althmany.extractor.data.*
import com.althmany.extractor.engine.*
import com.althmany.extractor.export.ExportFormat
import com.althmany.extractor.profile.RuntimeBackendPreference
import com.althmany.extractor.profile.UnifiedRemoteTarget
import com.althmany.extractor.profile.UnifiedRuntimeSnapshot

private val VBg = Color(0xFF020B13)
private val VBg2 = Color(0xFF061522)
private val VPanel = Color(0xFF081722)
private val VPanel2 = Color(0xFF0C1D2A)
private val VLine = Color(0xFF183448)
private val VCyan = Color(0xFF08D9FF)
private val VBlue = Color(0xFF389BFF)
private val VGreen = Color(0xFF00E696)
private val VPurple = Color(0xFF8B65FF)
private val VOrange = Color(0xFFFFB347)
private val VRed = Color(0xFFFF536F)
private val VText = Color(0xFFF2F7FA)
private val VMuted = Color(0xFF91A3B2)

private fun vBrush() = Brush.verticalGradient(listOf(VBg, VBg2, VBg))

@Composable
private fun VCard(
    modifier: Modifier = Modifier,
    accent: Color = VLine,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = VPanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.85f))
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun VTitle(title: String, icon: ImageVector, tint: Color = VCyan) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = VText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun VHeader(title: String, subtitle: String, engine: ExtractionUiState, icon: ImageVector) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    "Al-othmany Sender 3.4.1",
                    color = VText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "إدارة الروابط، الانضمام، الاستخراج، الفحص، النشر",
                    color = VMuted,
                    fontSize = 11.sp
                )
            }
            Icon(Icons.Default.Settings, null, tint = VText, modifier = Modifier.size(24.dp))
        }

        VCard(accent = VCyan.copy(alpha = 0.6f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = VCyan.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, VCyan.copy(alpha = 0.55f)),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = VCyan, modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(title, color = VText, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, color = VMuted, fontSize = 10.sp, textAlign = TextAlign.End)
                }
            }
        }

    }
}

@Composable
private fun VConnection(engine: ExtractionUiState) {
    VCard {
        VTitle("حالة الاتصال", Icons.Default.Bolt)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VStatus(
                "Accessibility",
                when {
                    engine.serviceConnected -> "متصل"
                    engine.accessibilityEnabledInSettings -> "مفعّل"
                    else -> "غير متصل"
                },
                engine.serviceConnected || engine.accessibilityEnabledInSettings,
                Modifier.weight(1f)
            )
            VStatus(
                "Shizuku",
                if (engine.shizukuReady) "متصل" else "غير متصل",
                engine.shizukuReady,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VStatus(label: String, value: String, good: Boolean, modifier: Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(14.dp),
        color = VPanel2,
        border = BorderStroke(1.dp, if (good) VCyan.copy(alpha = .55f) else VLine)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = VText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = if (good) VGreen else VMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun VChoice(
    label: String,
    selected: Boolean,
    tint: Color = VCyan,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 44.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) tint.copy(alpha = .15f) else VPanel2,
        border = BorderStroke(1.dp, if (selected) tint else VLine)
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) tint else VText,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun VStat(label: String, value: String, tint: Color, modifier: Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(15.dp),
        color = tint.copy(alpha = .1f),
        border = BorderStroke(1.dp, tint.copy(alpha = .55f))
    ) {
        Column(Modifier.padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = tint, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = VText, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun VControls(
    running: Boolean,
    paused: Boolean,
    startEnabled: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    VCard(accent = VCyan.copy(alpha = .5f)) {
        VTitle("التحكم السريع", Icons.Default.Bolt)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VControl("بدء", Icons.Default.PlayArrow, VCyan, startEnabled && !running && !paused, Modifier.weight(1f), onStart)
            VControl("إيقاف مؤقت", Icons.Default.Pause, VOrange, running && !paused, Modifier.weight(1f), onPause)
            VControl("استكمال", Icons.Default.Refresh, VBlue, paused, Modifier.weight(1f), onResume)
            VControl("إيقاف نهائي", Icons.Default.Stop, VRed, running || paused, Modifier.weight(1f), onStop)
        }
    }
}

@Composable
private fun VControl(
    label: String,
    icon: ImageVector,
    tint: Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(52.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (enabled) tint.copy(alpha = .15f) else VPanel2,
        border = BorderStroke(1.dp, if (enabled) tint else VLine)
    ) {
        Column(
            Modifier.padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (enabled) tint else VMuted, modifier = Modifier.size(17.dp))
            Text(label, color = if (enabled) VText else VMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun V341BottomBar(current: AppScreen, onNavigate: (AppScreen) -> Unit) {
    val entries = listOf(
        Triple(AppScreen.HOME, "الرئيسية", Icons.Default.Home),
        Triple(AppScreen.JOIN, "الانضمام", Icons.Default.Groups),
        Triple(AppScreen.EXTRACT, "الاستخراج", Icons.Default.Download),
        Triple(AppScreen.SCAN, "الفحص", Icons.Default.Search),
        Triple(AppScreen.PUBLISH, "النشر", Icons.Default.Send),
        Triple(AppScreen.LOGS, "التشخيص", Icons.Default.Settings)
    )
    NavigationBar(containerColor = Color(0xFF061520), tonalElevation = 0.dp, modifier = Modifier.height(64.dp)) {
        entries.forEach { (screen, label, icon) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { onNavigate(screen) },
                icon = { Icon(icon, null, modifier = Modifier.size(19.dp)) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = VCyan,
                    selectedTextColor = VCyan,
                    indicatorColor = VCyan.copy(alpha = .14f),
                    unselectedIconColor = VMuted,
                    unselectedTextColor = VMuted
                )
            )
        }
    }
}

private fun extractionBusy(engine: ExtractionUiState): Boolean = engine.status in setOf(
    EngineStatus.PREPARING,
    EngineStatus.SYNCING_GROUPS,
    EngineStatus.OPENING_WHATSAPP,
    EngineStatus.SEARCHING_GROUP,
    EngineStatus.OPENING_GROUP,
    EngineStatus.VERIFYING_GROUP,
    EngineStatus.EXTRACTING,
    EngineStatus.LINKS_TAB,
    EngineStatus.VERIFYING_END,
    EngineStatus.RECOVERING,
    EngineStatus.PROFILE_MISMATCH
)

@Composable
fun V341HomeScreen(
    padding: PaddingValues,
    engine: ExtractionUiState,
    runtime: UnifiedRuntimeSnapshot,
    scan: ScanUiState,
    publish: PublishUiState,
    onExtract: () -> Unit,
    onScan: () -> Unit,
    onPublish: () -> Unit,
    onAutoJoin: () -> Unit,
    onTargetWhatsApp: (String) -> Unit,
    onBackendPreference: (RuntimeBackendPreference) -> Unit,
    remoteTargets: List<UnifiedRemoteTarget>,
    onDiscoverRemoteTargets: () -> Unit,
    onRemoteTarget: (UnifiedRemoteTarget) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAll: () -> Unit,
    onSettings: () -> Unit
) {
    val running = extractionBusy(engine) || scan.running || publish.running
    val paused = engine.status == EngineStatus.PAUSED || scan.paused || publish.paused

    LazyColumn(
        Modifier.fillMaxSize().background(vBrush()).padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { VHeader("مرحباً بك", "كل ما تحتاجه لإدارة مجموعاتك في مكان واحد", engine, Icons.Default.Home) }
        item {
            UnifiedRuntimeCard(
                engine = engine,
                runtime = runtime,
                onTargetWhatsApp = onTargetWhatsApp,
                onBackendPreference = onBackendPreference,
                remoteTargets = remoteTargets,
                onDiscoverRemoteTargets = onDiscoverRemoteTargets,
                onRemoteTarget = onRemoteTarget
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VFeature("الانضمام", "الانضمام التلقائي للمجموعات", Icons.Default.Groups, VPurple, Modifier.weight(1f), onAutoJoin)
                VFeature("الاستخراج", "استخراج الروابط من القروبات", Icons.Default.Link, VBlue, Modifier.weight(1f), onExtract)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VFeature("الفحص", "فحص الروابط والتحقق منها", Icons.Default.Shield, VGreen, Modifier.weight(1f), onScan)
                VFeature("النشر", "نشر الرسائل في المجموعات", Icons.Default.Send, VOrange, Modifier.weight(1f), onPublish)
            }
        }
        item {
            VTitle("ملخص الأداء", Icons.Default.BarChart)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                VStat("الروابط", engine.stats.totalUniqueLinks.toString(), VBlue, Modifier.weight(1f))
                VStat("تم الانضمام", scan.stats.joined.toString(), VGreen, Modifier.weight(1f))
                VStat("قيد الانتظار", scan.stats.pending.toString(), VOrange, Modifier.weight(1f))
                VStat("فشل", (engine.stats.failedGroups + scan.stats.invalid + publish.stats.failed).toString(), VRed, Modifier.weight(1f))
            }
        }
        item { VControls(running, paused, engine.selectedWhatsAppPackage != null, onStart, onPause, onResume, onStopAll) }
        item {
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, VCyan)) {
                Icon(Icons.Default.Settings, null, tint = VCyan)
                Spacer(Modifier.width(6.dp))
                Text("الإعدادات المتقدمة", color = VText)
            }
        }
    }
}

@Composable
private fun VFeature(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier.height(112.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = tint.copy(alpha = .12f),
        border = BorderStroke(1.dp, tint.copy(alpha = .7f))
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(31.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = VText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = VMuted, fontSize = 11.sp, textAlign = TextAlign.End, maxLines = 2)
        }
    }
}

@Composable
fun V341JoinScreen(
    padding: PaddingValues,
    engine: ExtractionUiState,
    scan: ScanUiState,
    items: List<ScanRecord>,
    onTargetWhatsApp: (String) -> Unit,
    onAddLinks: (String) -> Unit,
    onImportExtraction: () -> Unit,
    onImportFile: () -> Unit,
    onAction: (ScanActionMode) -> Unit,
    onRequestToJoin: (Boolean) -> Unit,
    onSpeed: (ScanSpeedProfile) -> Unit,
    onAttempts: (Int) -> Unit,
    onStart: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAll: () -> Unit,
    onClear: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val failed = scan.stats.invalid + scan.stats.network + scan.stats.other
    val startEnabled = (items.isNotEmpty() || text.isNotBlank()) && engine.selectedWhatsAppPackage != null

    LazyColumn(
        Modifier.fillMaxSize().background(vBrush()).padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { VHeader("الانضمام", "الانضمام التلقائي إلى القروبات والروابط المحددة", engine, Icons.Default.Groups) }
        item {
            VCard {
                VTitle("تطبيق الهدف", Icons.Default.Chat)
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(engine.availableWhatsApp.filter { it.launchable }, key = { it.packageName }) { instance ->
                        VChoice(
                            "${instance.labelAr}\n${instance.packageName}",
                            engine.selectedWhatsAppPackage == instance.packageName,
                            VCyan,
                            Modifier.width(150.dp)
                        ) { onTargetWhatsApp(instance.packageName) }
                    }
                }
            }
        }
        item {
            VCard(accent = VCyan.copy(alpha = .6f)) {
                VTitle("روابط الدعوة", Icons.Default.Link)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(16000) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 115.dp),
                    placeholder = { Text("ألصق روابط الدعوة هنا...\nرابط واحد في كل سطر", color = VMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VCyan,
                        unfocusedBorderColor = VLine,
                        focusedTextColor = VText,
                        unfocusedTextColor = VText
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        onClick = { if (text.isNotBlank()) { onAddLinks(text); text = "" } },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, VCyan)
                    ) { Text("إضافة", color = VText, fontSize = 10.sp) }
                    OutlinedButton(onClick = onImportFile, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, VBlue)) {
                        Text("استيراد ملف", color = VText, fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = onImportExtraction, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, VPurple)) {
                        Text("من الاستخراج", color = VText, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            VCard {
                VTitle("إعدادات الجلسة", Icons.Default.Settings)
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VChoice("انضمام مباشر", scan.actionMode == ScanActionMode.JOIN_ONLY && !scan.requestToJoinEnabled, VCyan, Modifier.weight(1f)) {
                        onAction(ScanActionMode.JOIN_ONLY); onRequestToJoin(false)
                    }
                    VChoice("طلب انضمام", scan.actionMode == ScanActionMode.JOIN_ONLY && scan.requestToJoinEnabled, VOrange, Modifier.weight(1f)) {
                        onAction(ScanActionMode.JOIN_ONLY); onRequestToJoin(true)
                    }
                    VChoice("ذكي", scan.actionMode == ScanActionMode.SCAN_AND_JOIN, VPurple, Modifier.weight(1f)) {
                        onAction(ScanActionMode.SCAN_AND_JOIN); onRequestToJoin(true)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScanSpeedProfile.entries.forEach { speed ->
                        VChoice(speed.labelAr, scan.speed == speed, VCyan, Modifier.weight(1f)) { onSpeed(speed) }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { n ->
                        VChoice(n.toString(), scan.maxAttempts == n, VCyan, Modifier.weight(1f)) { onAttempts(n) }
                    }
                }
            }
        }
        item {
            VTitle("ملخص الجلسة", Icons.Default.BarChart)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                VStat("تم الانضمام", scan.stats.joined.toString(), VGreen, Modifier.weight(1f))
                VStat("طلب انضمام", scan.stats.requestPending.toString(), VOrange, Modifier.weight(1f))
                VStat("فشل", failed.toString(), VRed, Modifier.weight(1f))
                VStat("متبقي", scan.stats.pending.toString(), VCyan, Modifier.weight(1f))
            }
        }
        item {
            Button(
                onClick = { val pending = text; text = ""; onStart(pending) },
                enabled = startEnabled && !scan.running,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VCyan, contentColor = Color(0xFF00131A)),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("بدء الانضمام", fontWeight = FontWeight.Bold)
            }
        }
        item {
            VControls(
                scan.running,
                scan.paused,
                startEnabled,
                { val pending = text; text = ""; onStart(pending) },
                onPause,
                onResume,
                onStopAll
            )
        }
        item {
            OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, VRed)) {
                Icon(Icons.Default.Delete, null, tint = VRed)
                Spacer(Modifier.width(5.dp))
                Text("مسح قائمة الانضمام", color = VRed)
            }
        }
    }
}

@Composable
fun V341ExtractionScreen(
    padding: PaddingValues,
    engine: ExtractionUiState,
    runtime: UnifiedRuntimeSnapshot,
    groups: List<TargetGroup>,
    accessibilityEnabled: Boolean,
    onTargetWhatsApp: (String) -> Unit,
    onBackendPreference: (RuntimeBackendPreference) -> Unit,
    onMode: (ExtractionMode) -> Unit,
    onSpeed: (SpeedProfile) -> Unit,
    onRounds: (Int) -> Unit,
    onSync: () -> Unit,
    onSelected: (Long, Boolean) -> Unit,
    onPreset: (GroupSelectionPreset) -> Unit,
    onGroups: () -> Unit,
    onResults: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAll: () -> Unit,
    onOpenWhatsApp: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) groups else groups.filter { it.name.contains(query, true) }
    val running = extractionBusy(engine)
    val paused = engine.status == EngineStatus.PAUSED

    LazyColumn(
        Modifier.fillMaxSize().background(vBrush()).padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { VHeader("الاستخراج", "استخراج الروابط من القروبات والمحادثات", engine, Icons.Default.Link) }
        item { UnifiedRuntimeStatusStrip(runtime) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStart,
                    enabled = engine.selectedWhatsAppPackage != null && groups.any { it.selected } && !running,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VCyan, contentColor = Color(0xFF00131A)),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("بدء الاستخراج", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = onSync,
                    enabled = !running,
                    modifier = Modifier.weight(1f).height(56.dp),
                    border = BorderStroke(1.dp, VBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Sync, null, tint = VBlue)
                    Spacer(Modifier.width(4.dp))
                    Text("مزامنة القروبات", color = VText, fontSize = 11.sp)
                }
            }
        }
        item {
            VCard {
                VTitle("وضع الاستخراج", Icons.Default.CloudDownload)
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VChoice("ذكي", engine.mode == ExtractionMode.SMART, VCyan, Modifier.weight(1f)) { onMode(ExtractionMode.SMART) }
                    VChoice("جديد فقط", engine.mode == ExtractionMode.NEW_ONLY, VPurple, Modifier.weight(1f)) { onMode(ExtractionMode.NEW_ONLY) }
                    VChoice("عميق", engine.mode == ExtractionMode.DEEP, VBlue, Modifier.weight(1f)) { onMode(ExtractionMode.DEEP) }
                }
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(SpeedProfile.ADAPTIVE, SpeedProfile.SMART, SpeedProfile.HYPER, SpeedProfile.SAFE)) { speed ->
                        VChoice(speed.labelAr, engine.speed == speed, VCyan, Modifier.width(108.dp)) { onSpeed(speed) }
                    }
                }
            }
        }
        item {
            VCard(accent = VCyan.copy(alpha = .6f)) {
                VTitle("اختيار القروبات", Icons.Default.Groups)
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { VChoice("تحديد الكل", false, VCyan, Modifier.width(108.dp)) { onPreset(GroupSelectionPreset.ALL) } }
                    item { VChoice("إلغاء التحديد", false, VRed, Modifier.width(108.dp)) { onPreset(GroupSelectionPreset.NONE) } }
                    item { VChoice("غير المقروءة", false, VPurple, Modifier.width(108.dp)) { onPreset(GroupSelectionPreset.UNREAD) } }
                    item { VChoice("النشطة", false, VGreen, Modifier.width(108.dp)) { onPreset(GroupSelectionPreset.ACTIVE) } }
                }
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("البحث في القروبات...", color = VMuted, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = VCyan) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VCyan,
                        unfocusedBorderColor = VLine,
                        focusedTextColor = VText,
                        unfocusedTextColor = VText
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "تم تحديد ${groups.count { it.selected }} من ${groups.size}",
                    color = VCyan,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.height(5.dp))
                filtered.take(12).forEach { group ->
                    VGroupRow(group, onSelected)
                    Spacer(Modifier.height(5.dp))
                }
                if (filtered.size > 12) {
                    TextButton(onClick = onGroups, modifier = Modifier.fillMaxWidth()) {
                        Text("عرض جميع القروبات (${filtered.size})", color = VCyan)
                    }
                }
            }
        }
        item {
            VTitle("ملخص النتائج", Icons.Default.BarChart)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                VStat("إجمالي القروبات", groups.size.toString(), VCyan, Modifier.weight(1f))
                VStat("الروابط", engine.stats.totalUniqueLinks.toString(), VBlue, Modifier.weight(1f))
                VStat("مكتمل", engine.stats.completedGroups.toString(), VGreen, Modifier.weight(1f))
                VStat("فشل", engine.stats.failedGroups.toString(), VRed, Modifier.weight(1f))
            }
        }
        item {
            VControls(
                running,
                paused,
                engine.selectedWhatsAppPackage != null && groups.any { it.selected },
                onStart,
                onPause,
                onResume,
                onStopAll
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onResults, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, VCyan)) {
                    Text("النتائج", color = VText)
                }
                OutlinedButton(onClick = onOpenWhatsApp, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, VBlue)) {
                    Text("فتح واتساب", color = VText)
                }
            }
        }
    }
}

@Composable
private fun VGroupRow(group: TargetGroup, onSelected: (Long, Boolean) -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable { onSelected(group.id, !group.selected) },
        shape = RoundedCornerShape(13.dp),
        color = if (group.selected) VCyan.copy(alpha = .08f) else VPanel2,
        border = BorderStroke(1.dp, if (group.selected) VCyan.copy(alpha = .55f) else VLine)
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = group.selected, onCheckedChange = { onSelected(group.id, it) })
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    group.name,
                    color = VText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildList {
                    if (group.unreadCount > 0) add("${group.unreadCount} غير مقروء")
                    if (group.active) add("نشط")
                    if (!group.activityText.isNullOrBlank()) add(group.activityText!!)
                }.joinToString(" • ")
                if (meta.isNotBlank()) {
                    Text(meta, color = VMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun V341ScanScreen(
    padding: PaddingValues,
    engine: ExtractionUiState,
    scan: ScanUiState,
    items: List<ScanRecord>,
    onTargetWhatsApp: (String) -> Unit,
    onAddLinks: (String) -> Unit,
    onImportExtraction: () -> Unit,
    onImportFile: () -> Unit,
    onAction: (ScanActionMode) -> Unit,
    onSpeed: (ScanSpeedProfile) -> Unit,
    onAttempts: (Int) -> Unit,
    onStart: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAll: () -> Unit,
    onClear: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val startEnabled = (items.isNotEmpty() || text.isNotBlank()) && engine.selectedWhatsAppPackage != null

    LazyColumn(
        Modifier.fillMaxSize().background(vBrush()).padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { VHeader("الفحص", "التحقق من الروابط وتصنيفها", engine, Icons.Default.Search) }
        item {
            VCard(accent = VCyan.copy(alpha = .6f)) {
                VTitle("أدخل الروابط للفحص", Icons.Default.Link)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(16000) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 115.dp),
                    placeholder = { Text("أدخل كل رابط في سطر جديد...", color = VMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VCyan,
                        unfocusedBorderColor = VLine,
                        focusedTextColor = VText,
                        unfocusedTextColor = VText
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        onClick = { if (text.isNotBlank()) { onAddLinks(text); text = "" } },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, VCyan)
                    ) { Text("إضافة", color = VText, fontSize = 10.sp) }
                    OutlinedButton(onClick = onImportFile, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, VBlue)) {
                        Text("ملف", color = VText, fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = onImportExtraction, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, VPurple)) {
                        Text("الاستخراج", color = VText, fontSize = 10.sp)
                    }
                }
            }
        }
        item {
            VCard {
                VTitle("وضع الفحص", Icons.Default.Shield)
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScanActionMode.entries.forEach { mode ->
                        VChoice(
                            mode.labelAr,
                            scan.actionMode == mode,
                            if (mode == ScanActionMode.SCAN_AND_JOIN) VPurple else VCyan,
                            Modifier.weight(1f)
                        ) { onAction(mode) }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScanSpeedProfile.entries.forEach { speed ->
                        VChoice(speed.labelAr, scan.speed == speed, VCyan, Modifier.weight(1f)) { onSpeed(speed) }
                    }
                }
            }
        }
        item {
            VTitle("نتائج الفحص", Icons.Default.BarChart)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item { VStat("الكل", scan.stats.total.toString(), VCyan, Modifier.width(85.dp)) }
                item { VStat("مباشر", scan.stats.direct.toString(), VGreen, Modifier.width(85.dp)) }
                item { VStat("طلب انضمام", scan.stats.approval.toString(), VPurple, Modifier.width(85.dp)) }
                item { VStat("تم الانضمام", scan.stats.joined.toString(), VGreen, Modifier.width(85.dp)) }
                item { VStat("غير صالح", scan.stats.invalid.toString(), VRed, Modifier.width(85.dp)) }
            }
        }
        item {
            Button(
                onClick = { val pending = text; text = ""; onStart(pending) },
                enabled = startEnabled && !scan.running,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VCyan, contentColor = Color(0xFF00131A)),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(6.dp))
                Text("بدء الفحص", fontWeight = FontWeight.Bold)
            }
        }
        item {
            VControls(
                scan.running,
                scan.paused,
                startEnabled,
                { val pending = text; text = ""; onStart(pending) },
                onPause,
                onResume,
                onStopAll
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ExportFormat.entries) { format ->
                    VChoice(format.name, false, VCyan, Modifier.width(90.dp)) { onExport(format) }
                }
            }
        }
        item {
            OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, VRed)) {
                Icon(Icons.Default.Delete, null, tint = VRed)
                Spacer(Modifier.width(5.dp))
                Text("مسح نتائج الفحص", color = VRed)
            }
        }
    }
}

@Composable
fun V341PublishScreen(
    padding: PaddingValues,
    engine: ExtractionUiState,
    runtime: UnifiedRuntimeSnapshot,
    publish: PublishUiState,
    groups: List<TargetGroup>,
    onTargetWhatsApp: (String) -> Unit,
    onBackendPreference: (RuntimeBackendPreference) -> Unit,
    onGroups: () -> Unit,
    onDraft: (String) -> Unit,
    onContentMode: (PublishContentMode) -> Unit,
    onPickAttachment: (PublishContentMode) -> Unit,
    onClearAttachment: () -> Unit,
    onSpeed: (PublishSpeedProfile) -> Unit,
    onNavigation: (PublishNavigationMode) -> Unit,
    onAttempts: (Int) -> Unit,
    onStart: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopAll: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    val publishable = groups.count { it.active && it.publishable && !it.communityParent }
    val startEnabled = publish.messageText.isNotBlank() && engine.selectedWhatsAppPackage != null && publishable > 0

    LazyColumn(
        Modifier.fillMaxSize().background(vBrush()).padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { VHeader("النشر", "إرسال الرسائل إلى القروبات المحددة", engine, Icons.Default.Send) }
        item { UnifiedRuntimeStatusStrip(runtime) }
        item {
            VCard {
                VTitle("القروبات المستهدفة", Icons.Default.Groups)
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onGroups, border = BorderStroke(1.dp, VPurple)) {
                        Text("اختيار القروبات", color = VText)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("$publishable قروب قابل للنشر", color = VCyan, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }
        }
        item {
            VCard(accent = VCyan.copy(alpha = .55f)) {
                VTitle("نص الرسالة", Icons.Default.Article)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = publish.messageText,
                    onValueChange = onDraft,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 135.dp),
                    placeholder = { Text("اكتب رسالتك هنا...", color = VMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VCyan,
                        unfocusedBorderColor = VLine,
                        focusedTextColor = VText,
                        unfocusedTextColor = VText
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(7.dp))
                VTitle("نوع المحتوى", Icons.Default.AttachFile)
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PublishContentMode.entries) { mode ->
                        VChoice(
                            mode.labelAr,
                            publish.contentMode == mode,
                            if (mode.attachmentRequired) VPurple else VCyan,
                            Modifier.width(112.dp)
                        ) { onContentMode(mode) }
                    }
                }
                if (publish.contentMode.attachmentRequired) {
                    Spacer(Modifier.height(7.dp))
                    OutlinedButton(
                        onClick = { onPickAttachment(publish.contentMode) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, VPurple)
                    ) {
                        Icon(Icons.Default.AttachFile, null, tint = VPurple)
                        Spacer(Modifier.width(5.dp))
                        Text(if (publish.attachmentUri.isNullOrBlank()) "اختيار المرفق" else "تغيير المرفق", color = VText)
                    }
                }
            }
        }
        item {
            VCard {
                VTitle("إعدادات النشر", Icons.Default.Settings)
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(PublishSpeedProfile.entries) { speed ->
                        VChoice(speed.labelAr, publish.speed == speed, VCyan, Modifier.width(110.dp)) { onSpeed(speed) }
                    }
                }
            }
        }
        item {
            VTitle("ملخص النشر", Icons.Default.BarChart)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                VStat("إجمالي الأهداف", publish.stats.total.toString(), VCyan, Modifier.weight(1f))
                VStat("قيد الانتظار", publish.stats.pending.toString(), VOrange, Modifier.weight(1f))
                VStat("فشل الإرسال", publish.stats.failed.toString(), VRed, Modifier.weight(1f))
                VStat("تم الإرسال", (publish.stats.sent + publish.stats.verified).toString(), VGreen, Modifier.weight(1f))
            }
        }
        item {
            Button(
                onClick = { onStart(publish.messageText) },
                enabled = startEnabled && !publish.running,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VCyan, contentColor = Color(0xFF00131A)),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(6.dp))
                Text("بدء النشر", fontWeight = FontWeight.Bold)
            }
        }
        item {
            VControls(
                publish.running,
                publish.paused,
                startEnabled,
                { onStart(publish.messageText) },
                onPause,
                onResume,
                onStopAll
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ExportFormat.entries) { format ->
                    VChoice(format.name, false, VCyan, Modifier.width(90.dp)) { onExport(format) }
                }
            }
        }
    }
}
