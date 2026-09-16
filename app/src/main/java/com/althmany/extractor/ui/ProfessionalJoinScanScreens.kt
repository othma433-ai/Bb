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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.althmany.extractor.data.ScanRecord
import com.althmany.extractor.data.ScanStatus
import com.althmany.extractor.engine.ExtractionUiState
import com.althmany.extractor.engine.ScanSpeedProfile
import com.althmany.extractor.engine.ScanUiState
import com.althmany.extractor.export.ExportFormat
import com.althmany.extractor.export.ScanResultOrganizer
import com.althmany.extractor.join.OriginalJoinUiState
import com.althmany.extractor.profile.RuntimeBackendPreference
import com.althmany.extractor.profile.UnifiedRuntimeSnapshot

private val PBg = Color(0xFF020B13)
private val PBg2 = Color(0xFF061522)
private val PPanel = Color(0xFF081722)
private val PPanel2 = Color(0xFF0C1D2A)
private val PLine = Color(0xFF183448)
private val PCyan = Color(0xFF08D9FF)
private val PBlue = Color(0xFF389BFF)
private val PGreen = Color(0xFF00E696)
private val PPurple = Color(0xFF8B65FF)
private val POrange = Color(0xFFFFB347)
private val PRed = Color(0xFFFF536F)
private val PText = Color(0xFFF2F7FA)
private val PMuted = Color(0xFF91A3B2)

@Composable
private fun PCard(
    modifier: Modifier = Modifier,
    accent: Color = PLine,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = PPanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun PSectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = PCyan
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = PText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(7.dp))
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    PCard(accent = PCyan.copy(alpha = .6f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                color = PCyan.copy(alpha = .10f),
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(1.dp, PCyan.copy(alpha = .55f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = PCyan, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    title,
                    color = PText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.End
                )
                Text(
                    subtitle,
                    color = PMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun PChoice(
    text: String,
    selected: Boolean,
    tint: Color = PCyan,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        color = if (selected) tint.copy(alpha = .15f) else PPanel2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) tint else PLine)
    ) {
        Box(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (selected) tint else PText,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PStat(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        color = tint.copy(alpha = .10f),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = .55f))
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = tint, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = PText, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun PCompactLinkEditor(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    var expanded by remember { mutableStateOf(false) }
    val preview = LinkPreviewPolicy.analyze(value, expanded = false)

    if (!expanded && preview.totalLines > LinkPreviewPolicy.COLLAPSED_LIMIT) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PPanel2,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PCyan.copy(alpha = .55f))
        ) {
            Column(Modifier.padding(12.dp)) {
                preview.visibleLines.forEachIndexed { index, line ->
                    Text(
                        "${index + 1}. $line",
                        color = PText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (index != preview.visibleLines.lastIndex) Spacer(Modifier.height(5.dp))
                }
            }
        }
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("عرض المزيد (${preview.hiddenCount})", color = PCyan, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(120_000)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (expanded) 230.dp else 112.dp),
            minLines = if (expanded) 8 else 4,
            maxLines = if (expanded) 18 else 4,
            placeholder = {
                Text(
                    placeholder,
                    color = PMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.End
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PCyan,
                unfocusedBorderColor = PLine,
                focusedTextColor = PText,
                unfocusedTextColor = PText,
                cursorColor = PCyan
            ),
            shape = RoundedCornerShape(16.dp)
        )

        if (expanded && preview.totalLines > LinkPreviewPolicy.COLLAPSED_LIMIT) {
            TextButton(
                onClick = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("عرض أقل", color = PCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PControls(
    running: Boolean,
    paused: Boolean,
    startEnabled: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    PCard(accent = PCyan.copy(alpha = .5f)) {
        PSectionTitle("التحكم", Icons.Default.Bolt)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            PControl("بدء", Icons.Default.PlayArrow, PCyan, startEnabled && !running && !paused, Modifier.weight(1f), onStart)
            PControl("إيقاف مؤقت", Icons.Default.Pause, POrange, running && !paused, Modifier.weight(1f), onPause)
            PControl("استكمال", Icons.Default.Refresh, PBlue, paused, Modifier.weight(1f), onResume)
            PControl("إيقاف نهائي", Icons.Default.Stop, PRed, running || paused, Modifier.weight(1f), onStop)
        }
    }
}

@Composable
private fun PControl(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) tint.copy(alpha = .14f) else PPanel2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) tint else PLine)
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (enabled) tint else PMuted, modifier = Modifier.size(18.dp))
            Text(label, color = if (enabled) PText else PMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ProfessionalJoinScreen(
    padding: PaddingValues,
    engine: ExtractionUiState,
    runtime: UnifiedRuntimeSnapshot,
    join: OriginalJoinUiState,
    onTargetWhatsApp: (String) -> Unit,
    onBackendPreference: (RuntimeBackendPreference) -> Unit,
    onDraft: (String) -> Unit,
    onImportFile: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PBg, PBg2, PBg)))
            .padding(padding)
    ) {
        val side = if (maxWidth >= 600.dp) 28.dp else 16.dp
        val startEnabled = join.draft.isNotBlank() || join.remaining > 0

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = side, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PHeader(
                    "الانضمام",
                    "نفس محرك Sender الأصلي: فتح الرابط، تحديد الإجراء، التنفيذ، التحقق ثم الانتقال",
                    Icons.Default.Groups
                )
            }

            item { UnifiedRuntimeStatusStrip(runtime) }

            item {
                PCard(accent = PCyan.copy(alpha = .65f)) {
                    PSectionTitle("روابط الدعوة", Icons.Default.Link)
                    Spacer(Modifier.height(8.dp))
                    PCompactLinkEditor(
                        value = join.draft,
                        onValueChange = onDraft,
                        placeholder = "ألصق روابط الدعوة هنا...\nرابط واحد في كل سطر"
                    )
                    Spacer(Modifier.height(5.dp))
                    OutlinedButton(
                        onClick = onImportFile,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, PBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, null, tint = PBlue)
                        Spacer(Modifier.width(6.dp))
                        Text("استيراد روابط من ملف", color = PText)
                    }
                }
            }

            item {
                PSectionTitle("ملخص الانضمام", Icons.Default.BarChart)
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item { PStat("الكل", join.total.toString(), PCyan, Modifier.width(92.dp)) }
                    item { PStat("تم الانضمام", join.joined.toString(), PGreen, Modifier.width(100.dp)) }
                    item { PStat("موافقة المشرف", join.requested.toString(), POrange, Modifier.width(112.dp)) }
                    item { PStat("فشل", join.failed.toString(), PRed, Modifier.width(92.dp)) }
                    item { PStat("متبقي", join.remaining.toString(), PBlue, Modifier.width(92.dp)) }
                }
            }

            item {
                PCard {
                    PSectionTitle("حالة المحرك الأصلي", Icons.Default.Bolt)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "المحرك: ${join.backend} • التقدم: ${join.progressPercent}% • الرابط: ${join.currentPosition ?: "—"}/${join.total}",
                        color = PMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        join.message,
                        color = PText,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }

            item {
                Button(
                    onClick = onStart,
                    enabled = startEnabled && !join.running,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PCyan,
                        contentColor = Color(0xFF00131A)
                    ),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (join.remaining > 0 && join.draft.isBlank()) "استكمال قائمة الانضمام" else "بدء الانضمام",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                PControls(
                    running = join.running,
                    paused = join.paused,
                    startEnabled = startEnabled,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop
                )
            }
        }
    }
}

@Composable
private fun PScanResultRow(item: ScanRecord) {
    val tint = when (item.status) {
        ScanStatus.DIRECT, ScanStatus.ALREADY_MEMBER, ScanStatus.JOINED -> PGreen
        ScanStatus.APPROVAL, ScanStatus.REQUEST_PENDING -> POrange
        ScanStatus.INVALID, ScanStatus.FULL, ScanStatus.REMOVED,
        ScanStatus.ACCOUNT_LIMIT, ScanStatus.ERROR -> PRed
        ScanStatus.NETWORK_ERROR, ScanStatus.UNKNOWN, ScanStatus.ACTION_UNCERTAIN -> PPurple
        else -> PCyan
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PPanel2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = .55f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(11.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${item.confidence}%", color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(item.status.labelAr, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.groupName ?: "اسم القروب غير متاح",
                color = PText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.inviteKind.labelAr} • ${item.memberCountText ?: "عدد الأعضاء غير متاح"}",
                color = PMuted,
                fontSize = 10.sp
            )
            Text(
                item.normalizedUrl,
                color = PMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ProfessionalScanScreen(
    padding: PaddingValues,
    engine: ExtractionUiState,
    runtime: UnifiedRuntimeSnapshot,
    scan: ScanUiState,
    scanItems: List<ScanRecord>,
    onTargetWhatsApp: (String) -> Unit,
    onBackendPreference: (RuntimeBackendPreference) -> Unit,
    onAddLinks: (String) -> Unit,
    onImportExtraction: () -> Unit,
    onImportFile: () -> Unit,
    onSpeed: (ScanSpeedProfile) -> Unit,
    onAttempts: (Int) -> Unit,
    onStart: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    var text by remember { mutableStateOf("") }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PBg, PBg2, PBg)))
            .padding(padding)
    ) {
        val side = if (maxWidth >= 600.dp) 28.dp else 16.dp
        val startEnabled = (scanItems.isNotEmpty() || text.isNotBlank()) &&
            engine.selectedWhatsAppPackage != null

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = side, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PHeader(
                    "الفحص",
                    "يفتح الرابط ويقرأ واتساب ويصنف الحالة فقط — بدون تنفيذ انضمام",
                    Icons.Default.Search
                )
            }

            item { UnifiedRuntimeStatusStrip(runtime) }

            item {
                PCard(accent = PCyan.copy(alpha = .65f)) {
                    PSectionTitle("روابط الفحص", Icons.Default.Link)
                    Spacer(Modifier.height(8.dp))
                    PCompactLinkEditor(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = "ألصق روابط الدعوة هنا...\nرابط واحد في كل سطر"
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (text.isNotBlank()) {
                                    onAddLinks(text)
                                    text = ""
                                }
                            },
                            enabled = text.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, PCyan)
                        ) { Text("إضافة", color = PText, fontSize = 10.sp) }

                        OutlinedButton(
                            onClick = onImportFile,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, PBlue)
                        ) { Text("ملف", color = PText, fontSize = 10.sp) }

                        OutlinedButton(
                            onClick = onImportExtraction,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, PPurple)
                        ) { Text("من الاستخراج", color = PText, fontSize = 10.sp) }
                    }
                }
            }

            item {
                PCard(accent = PGreen.copy(alpha = .60f)) {
                    PSectionTitle("وضع الفحص الآمن", Icons.Default.Shield, PGreen)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "التصنيف فقط: لن يضغط التطبيق «انضمام» أو «طلب انضمام».",
                        color = PText,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScanSpeedProfile.entries.forEach { speed ->
                            PChoice(
                                speed.labelAr,
                                scan.speed == speed,
                                PCyan,
                                Modifier.weight(1f)
                            ) { onSpeed(speed) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "عدد محاولات التحقق",
                        color = PMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        (1..5).forEach { count ->
                            PChoice(
                                count.toString(),
                                scan.maxAttempts == count,
                                PCyan,
                                Modifier.weight(1f)
                            ) { onAttempts(count) }
                        }
                    }
                }
            }

            item {
                PCard(accent = if (scan.running) PGreen.copy(alpha = .65f) else PLine) {
                    PSectionTitle("حالة الفحص", Icons.Default.Bolt, if (scan.running) PGreen else PCyan)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "${scan.status.name} • ${scan.currentIndex}/${scan.total} • ثقة ${scan.currentConfidence}%",
                        color = PMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        scan.message,
                        color = PText,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }

            item {
                PSectionTitle("تصنيف الروابط", Icons.Default.BarChart)
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item { PStat("الكل", scan.stats.total.toString(), PCyan, Modifier.width(90.dp)) }
                    item { PStat("بدون موافقة", scan.stats.direct.toString(), PGreen, Modifier.width(105.dp)) }
                    item { PStat("موافقة المشرف", scan.stats.approval.toString(), POrange, Modifier.width(112.dp)) }
                    item { PStat("عضو مسبقاً", scan.stats.alreadyMember.toString(), PBlue, Modifier.width(100.dp)) }
                    item { PStat("طلب سابق", scan.stats.requestPending.toString(), POrange, Modifier.width(96.dp)) }
                    item { PStat("غير صالح", scan.stats.invalid.toString(), PRed, Modifier.width(92.dp)) }
                    item { PStat("غير مؤكد", scan.stats.unknown.toString(), PPurple, Modifier.width(92.dp)) }
                }
            }

            if (scanItems.isNotEmpty()) {
                item {
                    PCard {
                        PSectionTitle("نتائج الفحص", Icons.Default.Search)
                        Spacer(Modifier.height(8.dp))
                        ScanResultOrganizer.sorted(scanItems).take(20).forEach { record ->
                            PScanResultRow(record)
                            Spacer(Modifier.height(6.dp))
                        }
                        if (scanItems.size > 20) {
                            Text(
                                "يتم عرض أحدث 20 نتيجة من ${scanItems.size}",
                                color = PMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val pending = text
                        text = ""
                        onStart(pending)
                    },
                    enabled = startEnabled && !scan.running,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PCyan,
                        contentColor = Color(0xFF00131A)
                    ),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(6.dp))
                    Text("بدء الفحص والتصنيف", fontWeight = FontWeight.Bold)
                }
            }

            item {
                PControls(
                    running = scan.running,
                    paused = scan.paused,
                    startEnabled = startEnabled,
                    onStart = {
                        val pending = text
                        text = ""
                        onStart(pending)
                    },
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(ExportFormat.entries) { format ->
                        PChoice(format.name, false, PCyan, Modifier.width(92.dp)) {
                            onExport(format)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, PRed),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = PRed)
                    Spacer(Modifier.width(6.dp))
                    Text("مسح نتائج الفحص", color = PRed)
                }
            }
        }
    }
}
