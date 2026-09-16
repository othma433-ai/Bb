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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val URMuted = Color(0xFF91A3B2)

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
    Surface(
        color = URPanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (runtime.ready) URGreen.copy(alpha = .7f) else UROrange.copy(alpha = .7f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onRefresh != null) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = URCyan, modifier = Modifier.size(19.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("بيئة التشغيل", color = URText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        runtime.summaryAr,
                        color = if (runtime.ready) URGreen else UROrange,
                        fontSize = 10.sp,
                        textAlign = TextAlign.End
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Bolt, null, tint = URCyan, modifier = Modifier.size(22.dp))
            }

            Surface(
                color = URPanel2,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, URLine)
            ) {
                Column(Modifier.fillMaxWidth().padding(11.dp)) {
                    RuntimeLine("البيئة", runtime.environmentLabel)
                    RuntimeLine("واتساب", runtime.selectedWhatsAppLabel)
                    RuntimeLine("المحرك الفعلي", runtime.effectiveBackendLabelAr)
                    RuntimeLine(
                        "الجاهزية",
                        if (runtime.ready) "جاهز" else runtime.detail
                    )
                }
            }

            Text(
                "نسخة واتساب",
                color = URMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            val targets = engine.availableWhatsApp.filter { it.launchable && !it.profileKey.startsWith("REMOTE:") }
            if (targets.isEmpty()) {
                Text(
                    "لا توجد نسخة واتساب قابلة للتشغيل داخل ${runtime.profileInfo.labelAr}",
                    color = UROrange,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(targets, key = { it.packageName }) { target ->
                        RuntimeChoice(
                            label = "${target.labelAr}\n${target.packageName}",
                            selected = !runtime.remoteTarget && runtime.selectedWhatsAppPackage == target.packageName,
                            modifier = Modifier.width(164.dp)
                        ) {
                            onTargetWhatsApp(target.packageName)
                        }
                    }
                }
            }

            Text(
                "المحرك",
                color = URMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
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

            if (onDiscoverRemoteTargets != null) {
                OutlinedButton(
                    onClick = onDiscoverRemoteTargets,
                    enabled = runtime.shizukuReady,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, if (runtime.shizukuReady) URCyan else URLine),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "اكتشاف Dual / Work / Secure عبر Shizuku",
                        color = if (runtime.shizukuReady) URText else URMuted,
                        fontSize = 10.sp
                    )
                }

                if (remoteTargets.isNotEmpty() && onRemoteTarget != null) {
                    Text(
                        "بيئات Android المكتشفة عبر Shizuku",
                        color = URMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(remoteTargets, key = { it.stableKey }) { target ->
                            RuntimeChoice(
                                label = "${target.environmentLabel}\n${target.whatsappLabel}",
                                selected = runtime.remoteTarget &&
                                    runtime.targetAndroidUserId == target.androidUserId &&
                                    runtime.selectedWhatsAppPackage == target.packageName,
                                modifier = Modifier.width(190.dp)
                            ) {
                                onRemoteTarget(target)
                            }
                        }
                    }
                }
            }

            Text(
                when {
                    runtime.remoteTarget ->
                        "هدف Shizuku بعيد: Android user ${runtime.targetAndroidUserId}. كل الخصائص تستخدم نفس Target Lock."
                    runtime.profileInfo.isLikelySecureFolder ->
                        "التطبيق يعمل داخل Secure Folder كبيئة كاملة؛ لا يتم تجاوز Knox."
                    runtime.profileInfo.isManagedProfile ->
                        "التطبيق يعمل داخل Work Profile؛ جميع الخصائص تستخدم واتساب الموجود في نفس الملف."
                    else ->
                        "Dual Messenger يمكن اكتشافه عبر Shizuku أو كنسخة محلية مرئية للنظام."
                },
                color = URMuted,
                fontSize = 9.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun UnifiedRuntimeStatusStrip(
    runtime: UnifiedRuntimeSnapshot,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = URPanel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (runtime.ready) URGreen.copy(alpha = .55f) else UROrange.copy(alpha = .65f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (runtime.ready) "جاهز" else "يحتاج إعداد",
                color = if (runtime.ready) URGreen else UROrange,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    runtime.summaryAr,
                    color = URText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
                if (!runtime.ready) {
                    Text(
                        runtime.detail,
                        color = URMuted,
                        fontSize = 9.sp,
                        maxLines = 2,
                        textAlign = TextAlign.End
                    )
                }
            }
            Spacer(Modifier.width(7.dp))
            Icon(Icons.Default.Bolt, null, tint = URCyan, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RuntimeLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(value, color = URText, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        Text(label, color = URMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        color = if (selected) URCyan.copy(alpha = .15f) else URPanel2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) URCyan else URLine)
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) URCyan else URText,
                fontSize = 10.sp,
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
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (ready) URGreen.copy(alpha = .65f) else URLine)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bolt, null, tint = if (ready) URGreen else URMuted, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                "$label • ${if (ready) "جاهز" else "غير جاهز"}",
                color = if (ready) URGreen else URMuted,
                fontSize = 9.sp
            )
        }
    }
}
