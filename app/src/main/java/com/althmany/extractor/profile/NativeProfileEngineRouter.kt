package com.althmany.extractor.profile

import android.content.Context
import com.althmany.extractor.accessibility.AccessibilityRuntimeBridge
import com.althmany.extractor.shizuku.ShizukuBridge

enum class RuntimeBackendKind { ACCESSIBILITY, SHIZUKU, NONE }

data class NativeProfileEngineSnapshot(
    val profileKey: String,
    val accessibilityLocalReady: Boolean,
    val shizukuReady: Boolean,
    val recommended: RuntimeBackendKind,
    val reason: String
)

object NativeProfileEngineRouter {
    fun inspect(context: Context): NativeProfileEngineSnapshot {
        val profile = RuntimeProfileDetector.detect(context)
        val access = AccessibilityRuntimeBridge.currentEvenIfQuiet() != null
        val shizuku = runCatching { ShizukuBridge.status().ready }.getOrDefault(false)
        val preference = UnifiedRuntimeTargetStore.preference(context)
        val remote = UnifiedRuntimeTargetStore.isRemoteTarget(context)
        val remoteUser = UnifiedRuntimeTargetStore.selectedAndroidUserId(context)

        val recommended = if (remote) {
            when {
                preference == RuntimeBackendPreference.ACCESSIBILITY -> RuntimeBackendKind.NONE
                shizuku -> RuntimeBackendKind.SHIZUKU
                else -> RuntimeBackendKind.NONE
            }
        } else {
            when (preference) {
                RuntimeBackendPreference.ACCESSIBILITY ->
                    if (access) RuntimeBackendKind.ACCESSIBILITY else RuntimeBackendKind.NONE
                RuntimeBackendPreference.SHIZUKU ->
                    if (shizuku) RuntimeBackendKind.SHIZUKU else RuntimeBackendKind.NONE
                RuntimeBackendPreference.AUTO -> when {
                    access -> RuntimeBackendKind.ACCESSIBILITY
                    shizuku -> RuntimeBackendKind.SHIZUKU
                    else -> RuntimeBackendKind.NONE
                }
            }
        }

        val reason = when {
            remote && preference == RuntimeBackendPreference.ACCESSIBILITY ->
                "الهدف Android user $remoteUser بعيد؛ يحتاج Shizuku"
            remote && shizuku ->
                "Shizuku جاهز للهدف البعيد Android user $remoteUser"
            remote ->
                "الهدف Android user $remoteUser يحتاج Shizuku جاهزاً"
            preference == RuntimeBackendPreference.ACCESSIBILITY && !access ->
                "Accessibility محددة يدويًا لكنها غير متصلة داخل ${profile.labelAr}"
            preference == RuntimeBackendPreference.SHIZUKU && !shizuku ->
                "Shizuku محدد يدويًا لكنه غير جاهز داخل ${profile.labelAr}"
            recommended == RuntimeBackendKind.ACCESSIBILITY ->
                "Accessibility service حي داخل ${profile.labelAr}"
            recommended == RuntimeBackendKind.SHIZUKU ->
                "Shizuku جاهز؛ UIAutomation سيستخدم نفس Target Lock"
            else ->
                "لا يوجد Backend جاهز للبيئة الحالية"
        }

        return NativeProfileEngineSnapshot(
            profileKey = if (remote) "REMOTE:$remoteUser" else profile.profileKey,
            accessibilityLocalReady = access,
            shizukuReady = shizuku,
            recommended = recommended,
            reason = reason
        )
    }
}
