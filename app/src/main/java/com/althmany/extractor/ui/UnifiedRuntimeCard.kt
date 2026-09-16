package com.althmany.extractor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.althmany.extractor.engine.ExtractionUiState
import com.althmany.extractor.profile.RuntimeBackendPreference
import com.althmany.extractor.profile.UnifiedRemoteTarget
import com.althmany.extractor.profile.UnifiedRuntimeSnapshot

private val URPanel = Color(0xFF081722)
private val URPanel2 = Color(0xFF0C1D2A)
private val URLine = Color(0xFF183448)
private val URCyan = Color(0xFF08D9FF)
private val URGreen = Color(0xFF00E696)
private val UROrange = Color(0xFFFFB347)
private val URText = Color(0xFFF2F7FA)
private val URMuted = Color(0xFF9AAAB7)

@Composable
fun UnifiedRuntimeCard(
    engine: ExtractionUiState,
    runtime: UnifiedRuntimeSnapshot,
    onTargetWhatsApp: (String) -> Unit,
    onBackendPreference: (RuntimeBackendPreference) -> Unit,
    onRefresh: (() -> Unit)? = null,
    remoteTargets: List<UnifiedRemoteTarget> = emptyList(),
    onDiscoverRemoteTargets: (() -> Unit)? = null,
    onRemoteTarget: ((UnifiedRemoteTarget) -> Unit)? = null
) {
    val localTargets = engine.availableWhatsApp
        .filter { it.launchable && !it.profileKey.startsWith("REMOTE:") }
        .distinctBy { it.packageName }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = URPanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (runtime.ready) URGreen.copy(alpha = .55f) else UROrange.copy(alpha = .65f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onRefresh != null || onDiscoverRemoteTargets != null) {
                    IconButton(
                        onClick = {
                            if (onRefresh != null) onRefresh()
                            else onDiscoverRemoteTargets?.invoke()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "تحديث نسخ واتساب",
                            tint = URCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "بيئة التشغيل",
                        color = URText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        runtime.summaryAr,
                        color = if (runtime.ready) URGreen else UROrange,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Bolt, null, tint = URCyan, modifier = Modifier.size(24.dp))
            }

            Surface(
                color = URPanel2,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, URLine)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    RuntimeLine("البيئة", runtime.environmentLabel)
                    RuntimeLine("واتساب", runtime.selectedWhatsAppLabel)
                    RuntimeLine("المحرك الفعلي", runtime.effectiveBackendLabelAr)
                    RuntimeLine("الجاهزية", if (runtime.ready) "جاهز" else runtime.detail)
                }
            }

            Text(
                "جميع نسخ واتساب",
                color = URText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            if (localTargets.isEmpty() && remoteTargets.isEmpty()) {
                Text(
                    "لم يتم اكتشاف نسخة واتساب قابلة للتشغيل بعد. اضغط تحديث النسخ.",
                    color = UROrange,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(localTargets, key = { "local:${it.packageName}" }) { target ->
                        TargetTile(
                            title = target.labelAr,
                            environment = runtime.profileInfo.labelAr,
                            packageName = target.packageName,
                            backendHint = "Accessibility / Shizuku",
                            selected = !runtime.remoteTarget &&
                                runtime.selectedWhatsAppPackage == target.packageName,
                            onClick = { onTargetWhatsApp(target.packageName) }
                        )
                    }

                    if (onRemoteTarget != null) {
                        items(remoteTargets, key = { "remote:${it.stableKey}" }) { target ->
                            TargetTile(
                                title = target.whatsappLabel,
                                environment = target.environmentLabel,
                                packageName = target.packageName,
                                backendHint = "Shizuku",
                                selected = runtime.remoteTarget &&
                                    runtime.targetAndroidUserId == target.androidUserId &&
                                    runtime.selectedWhatsAppPackage == target.packageName,
                                onClick = { onRemoteTarget(target) }
                            )
                        }
                    }
                }
            }

            if (runtime.shizukuReady && remoteTargets.isEmpty()) {
                Text(
                    "Shizuku جاهز. تحديث النسخ يبحث أيضًا عن Dual Messenger وWork Profile وSecure Folder.",
                    color = URMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }

            Text(
                "محرك التحكم",
                color = URText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RuntimeBackendPreference.entries.forEach { backend ->
                    RuntimeChoice(
                        label = backend.labelAr,
                        selected = runtime.preference == backend,
                        modifier = Modifier.weight(1f)
                    ) {
                        onBackendPreference(backend)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RuntimeStatus(
                    label = "Accessibility",
                    ready = runtime.accessibilityReady,
                    modifier = Modifier.weight(1f)
                )
                RuntimeStatus(
                    label = "Shizuku",
                    ready = runtime.shizukuReady,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                when {
                    runtime.remoteTarget ->
                        "الهدف الحالي داخل Android user ${runtime.targetAndroidUserId} ويستخدم Shizuku."
                    runtime.profileInfo.isLikelySecureFolder ->
                        "هذه نسخة AL-thmany داخل Secure Folder، وجميع الخصائص تستخدم واتساب الموجود في نفس البيئة."
                    runtime.profileInfo.isManagedProfile ->
                        "هذه نسخة AL-thmany داخل Work Profile، وجميع الخصائص تستخدم واتساب الموجود في نفس الملف."
                    else ->
                        "Personal وBusiness والنسخ المستنسخة المحلية تظهر مباشرة؛ البيئات الأخرى تظهر بعد اكتشاف Shizuku."
                },
                color = URMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun UnifiedRuntimeStatusStrip(
    runtime: UnifiedRuntimeSnapshot,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick)

    Surface(
        modifier = clickableModifier.fillMaxWidth(),
        color = URPanel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (runtime.ready) URGreen.copy(alpha = .5f) else UROrange.copy(alpha = .65f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (runtime.ready) "جاهز" else "يحتاج إعداد",
                color = if (runtime.ready) URGreen else UROrange,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    runtime.summaryAr,
                    color = URText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
                Text(
                    if (onClick != null) "اضغط لتغيير نسخة واتساب" else runtime.detail,
                    color = URMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    textAlign = TextAlign.End
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Bolt, null, tint = URCyan, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TargetTile(
    title: String,
    environment: String,
    packageName: String,
    backendHint: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(210.dp)
            .heightIn(min = 112.dp)
            .clickable(onClick = onClick),
        color = if (selected) URCyan.copy(alpha = .14f) else URPanel2,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) URCyan else URLine)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                color = if (selected) URCyan else URText,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Text(
                environment,
                color = URMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            Text(
                packageName,
                color = URText,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
            Text(
                backendHint,
                color = if (selected) URGreen else URMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RuntimeLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            value,
            color = URText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = URMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RuntimeChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick),
        color = if (selected) URCyan.copy(alpha = .15f) else URPanel2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) URCyan else URLine)
    ) {
        Box(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = if (selected) URCyan else URText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun RuntimeStatus(
    label: String,
    ready: Boolean,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        color = URPanel2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (ready) URGreen.copy(alpha = .6f) else URLine)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Bolt,
                null,
                tint = if (ready) URGreen else URMuted,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "$label • ${if (ready) "جاهز" else "غير جاهز"}",
                color = if (ready) URGreen else URMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
