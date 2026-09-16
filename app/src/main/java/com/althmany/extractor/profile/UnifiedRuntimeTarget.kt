package com.althmany.extractor.profile

import android.content.Context
import android.os.Process
import com.althmany.extractor.accessibility.AccessibilityRuntimeBridge
import com.althmany.extractor.shizuku.ShizukuBridge

enum class RuntimeBackendPreference(val labelAr: String) {
    AUTO("تلقائي"),
    ACCESSIBILITY("Accessibility"),
    SHIZUKU("Shizuku")
}

data class UnifiedRemoteTarget(
    val androidUserId: Int,
    val environmentLabel: String,
    val packageName: String,
    val whatsappLabel: String
) {
    val stableKey: String get() = "$androidUserId:$packageName"
}

data class UnifiedRuntimeSnapshot(
    val profileInfo: RuntimeProfileInfo,
    val environmentLabel: String,
    val targetAndroidUserId: Int,
    val remoteTarget: Boolean,
    val selectedWhatsAppPackage: String?,
    val selectedWhatsAppLabel: String,
    val preference: RuntimeBackendPreference,
    val effectiveBackend: RuntimeBackendKind,
    val accessibilityReady: Boolean,
    val shizukuReady: Boolean,
    val targetVisible: Boolean,
    val ready: Boolean,
    val detail: String
) {
    val effectiveBackendLabelAr: String
        get() = when (effectiveBackend) {
            RuntimeBackendKind.ACCESSIBILITY -> "Accessibility"
            RuntimeBackendKind.SHIZUKU -> "Shizuku"
            RuntimeBackendKind.NONE -> "غير جاهز"
        }

    val summaryAr: String
        get() = "$environmentLabel • $selectedWhatsAppLabel • $effectiveBackendLabelAr"
}

/**
 * Canonical target shared by Join / Scan / Extraction / Publish / Sync.
 *
 * Local Work Profile / Secure Folder operation is profile-local: install AL-thmany and WhatsApp in
 * the same environment. A second path is available for Samsung Dual/Work/Secure Android users that
 * Shizuku can explicitly verify; those targets are launched by Android user id and force Shizuku.
 */
object UnifiedRuntimeTargetStore {
    private const val PREFS = "althmany_unified_runtime_v341"
    private const val KEY_BACKEND = "backend"
    private const val KEY_PACKAGE = "whatsapp_package"
    private const val KEY_USER_ID = "android_user_id"
    private const val KEY_USER_LABEL = "android_user_label"
    private const val KEY_REMOTE = "remote_target"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun currentProcessUserId(): Int = (Process.myUid() / 100_000).coerceAtLeast(0)

    fun preference(context: Context): RuntimeBackendPreference =
        runCatching {
            RuntimeBackendPreference.valueOf(
                prefs(context).getString(KEY_BACKEND, RuntimeBackendPreference.AUTO.name)
                    ?: RuntimeBackendPreference.AUTO.name
            )
        }.getOrDefault(RuntimeBackendPreference.AUTO)

    fun setPreference(context: Context, value: RuntimeBackendPreference) {
        prefs(context).edit().putString(KEY_BACKEND, value.name).apply()
    }

    fun selectedPackage(context: Context): String? =
        prefs(context).getString(KEY_PACKAGE, null)?.takeIf(String::isNotBlank)

    fun selectedAndroidUserId(context: Context): Int =
        prefs(context).getInt(KEY_USER_ID, currentProcessUserId())

    fun isRemoteTarget(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMOTE, false) &&
            selectedAndroidUserId(context) != currentProcessUserId()

    fun setSelectedPackage(context: Context, packageName: String?) {
        prefs(context).edit().apply {
            if (packageName.isNullOrBlank()) remove(KEY_PACKAGE)
            else putString(KEY_PACKAGE, packageName)
        }.apply()
    }

    fun setLocalTarget(context: Context, packageName: String?) {
        val profile = RuntimeProfileDetector.detect(context)
        prefs(context).edit().apply {
            if (packageName.isNullOrBlank()) remove(KEY_PACKAGE)
            else putString(KEY_PACKAGE, packageName)
            putInt(KEY_USER_ID, currentProcessUserId())
            putString(KEY_USER_LABEL, profile.labelAr)
            putBoolean(KEY_REMOTE, false)
        }.apply()
    }

    fun setRemoteTarget(context: Context, target: UnifiedRemoteTarget) {
        prefs(context).edit()
            .putString(KEY_PACKAGE, target.packageName)
            .putInt(KEY_USER_ID, target.androidUserId)
            .putString(KEY_USER_LABEL, target.environmentLabel)
            .putBoolean(KEY_REMOTE, true)
            .putString(KEY_BACKEND, RuntimeBackendPreference.SHIZUKU.name)
            .apply()
    }

    fun clearRemoteTarget(context: Context) {
        val packageName = selectedPackage(context)
        setLocalTarget(context, packageName)
    }

    fun resolve(
        context: Context,
        selectedPackageOverride: String? = null
    ): UnifiedRuntimeSnapshot {
        val appContext = context.applicationContext
        val profile = RuntimeProfileDetector.detect(appContext)
        val available = WhatsAppInstanceRegistry.available(appContext, forceRefresh = false)
        val storedPackage = selectedPackage(appContext)
        val storedRemote = isRemoteTarget(appContext)
        val selectedPackage = if (storedRemote) {
            storedPackage
        } else {
            selectedPackageOverride
                ?.takeIf(String::isNotBlank)
                ?: storedPackage
                ?: available.firstOrNull { it.launchable }?.packageName
        }

        if (!storedRemote && !selectedPackage.isNullOrBlank() && selectedPackage != storedPackage) {
            setLocalTarget(appContext, selectedPackage)
        }

        val userId = if (storedRemote) selectedAndroidUserId(appContext) else currentProcessUserId()
        val environmentLabel = if (storedRemote) {
            prefs(appContext).getString(KEY_USER_LABEL, null)
                ?.takeIf(String::isNotBlank)
                ?: "Android user $userId"
        } else {
            profile.labelAr
        }

        val selected = available.firstOrNull { it.packageName == selectedPackage }
        val accessibilityReady = AccessibilityRuntimeBridge.currentEvenIfQuiet() != null
        val shizukuReady = runCatching { ShizukuBridge.status().ready }.getOrDefault(false)
        val pref = preference(appContext)

        val effective = if (storedRemote) {
            when {
                pref == RuntimeBackendPreference.ACCESSIBILITY -> RuntimeBackendKind.NONE
                shizukuReady -> RuntimeBackendKind.SHIZUKU
                else -> RuntimeBackendKind.NONE
            }
        } else {
            when (pref) {
                RuntimeBackendPreference.ACCESSIBILITY ->
                    if (accessibilityReady) RuntimeBackendKind.ACCESSIBILITY else RuntimeBackendKind.NONE
                RuntimeBackendPreference.SHIZUKU ->
                    if (shizukuReady) RuntimeBackendKind.SHIZUKU else RuntimeBackendKind.NONE
                RuntimeBackendPreference.AUTO -> when {
                    accessibilityReady -> RuntimeBackendKind.ACCESSIBILITY
                    shizukuReady -> RuntimeBackendKind.SHIZUKU
                    else -> RuntimeBackendKind.NONE
                }
            }
        }

        val targetVisible = storedRemote || selected?.launchable == true
        val ready = !selectedPackage.isNullOrBlank() &&
            targetVisible &&
            effective != RuntimeBackendKind.NONE

        val label = selected?.labelAr ?: WhatsAppInstanceRegistry.labelFor(selectedPackage)

        val detail = when {
            selectedPackage.isNullOrBlank() -> "اختر نسخة واتساب داخل $environmentLabel"
            storedRemote && !shizukuReady -> "الهدف في Android user $userId يحتاج Shizuku جاهزاً"
            storedRemote && pref == RuntimeBackendPreference.ACCESSIBILITY ->
                "Accessibility لا تتحكم عبر Android users؛ اختر Shizuku"
            !targetVisible -> "نسخة واتساب المحددة غير مرئية/قابلة للتشغيل داخل $environmentLabel"
            pref == RuntimeBackendPreference.ACCESSIBILITY && !accessibilityReady ->
                "Accessibility محددة يدويًا لكنها غير متصلة داخل $environmentLabel"
            pref == RuntimeBackendPreference.SHIZUKU && !shizukuReady ->
                "Shizuku محدد يدويًا لكنه غير جاهز/غير مصرح"
            !accessibilityReady && !shizukuReady ->
                "لا يوجد Backend جاهز داخل $environmentLabel"
            else -> "جاهز: $environmentLabel • $label • ${
                when (effective) {
                    RuntimeBackendKind.ACCESSIBILITY -> "Accessibility"
                    RuntimeBackendKind.SHIZUKU -> "Shizuku"
                    RuntimeBackendKind.NONE -> "غير جاهز"
                }
            }"
        }

        return UnifiedRuntimeSnapshot(
            profileInfo = profile,
            environmentLabel = environmentLabel,
            targetAndroidUserId = userId,
            remoteTarget = storedRemote,
            selectedWhatsAppPackage = selectedPackage,
            selectedWhatsAppLabel = label,
            preference = pref,
            effectiveBackend = effective,
            accessibilityReady = accessibilityReady,
            shizukuReady = shizukuReady,
            targetVisible = targetVisible,
            ready = ready,
            detail = detail
        )
    }

    /**
     * Discovers verified WhatsApp installations in other Android users using Shizuku only.
     * It does not bypass profile security; the result is returned only when Android's package
     * service confirms the exact package for that user.
     */
    suspend fun discoverRemoteTargets(context: Context): List<UnifiedRemoteTarget> {
        val appContext = context.applicationContext
        if (!runCatching { ShizukuBridge.status().ready }.getOrDefault(false)) return emptyList()

        val usersResult = ShizukuBridge.execute(
            appContext,
            "{ pm list users 2>/dev/null; cmd user list 2>/dev/null; cmd user list -v 2>/dev/null; " +
                "dumpsys user 2>/dev/null; dumpsys persona 2>/dev/null; }",
            7_000
        )
        if (!usersResult.success && usersResult.output.isBlank()) return emptyList()

        val normalPairs = Regex("""UserInfo\{([0-9]+):([^:}]*)""")
            .findAll(usersResult.output)
            .mapNotNull { match ->
                val id = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                if (id !in 0..999) return@mapNotNull null
                id to match.groupValues[2].trim().ifBlank { "Android user $id" }
            }
            .toList()

        val personaPairs = Regex(
            """(?:userId|user_id|mUserId|containerId)\s*[=:]\s*([0-9]+)""",
            RegexOption.IGNORE_CASE
        ).findAll(usersResult.output)
            .mapNotNull { match ->
                val id = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                if (id !in 0..999) return@mapNotNull null
                id to "Samsung profile $id"
            }
            .toList()

        val users = (normalPairs + personaPairs).distinctBy { it.first }
        val host = currentProcessUserId()
        val knownPackages = setOf(
            WhatsAppInstanceRegistry.WHATSAPP,
            WhatsAppInstanceRegistry.WHATSAPP_BUSINESS,
            WhatsAppInstanceRegistry.WHATSAPP_CLONED
        )
        val found = mutableListOf<UnifiedRemoteTarget>()

        for ((userId, rawName) in users) {
            if (userId == host) continue

            val marker = ShizukuBridge.execute(
                appContext,
                "pm list packages --user $userId 2>/dev/null | " +
                    "grep -E 'package:com\\.samsung\\.knox\\.securefolder' | head -n1",
                2_500
            )
            val secureMarker = marker.output.contains("knox", ignoreCase = true)

            val environment = classifyEnvironment(userId, rawName, secureMarker)

            val packageListResult = ShizukuBridge.execute(
                appContext,
                "{ pm list packages --user $userId 2>/dev/null; " +
                    "cmd package list packages --user $userId 2>/dev/null; }",
                3_500
            )
            val remotePackages = packageListResult.output.lineSequence()
                .map(String::trim)
                .mapNotNull { line ->
                    line.takeIf { it.startsWith("package:") }
                        ?.removePrefix("package:")
                        ?.substringBefore(' ')
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                }
                .filter { packageName ->
                    packageName in knownPackages ||
                        packageName.contains("whatsapp", ignoreCase = true)
                }
                .distinct()
                .toList()

            for (packageName in remotePackages) {
                found += UnifiedRemoteTarget(
                    androidUserId = userId,
                    environmentLabel = environment,
                    packageName = packageName,
                    whatsappLabel = WhatsAppInstanceRegistry.labelFor(packageName)
                )
            }
        }

        return found
            .distinctBy(UnifiedRemoteTarget::stableKey)
            .sortedWith(
                compareBy<UnifiedRemoteTarget> { it.androidUserId != 95 }
                    .thenBy { it.environmentLabel }
                    .thenBy { it.packageName }
            )
    }

    private fun classifyEnvironment(
        userId: Int,
        rawName: String,
        secureMarker: Boolean
    ): String {
        val label = rawName.lowercase()
        return when {
            userId == 95 ||
                "dual_app" in label ||
                "dual app" in label ||
                "dual messenger" in label ->
                "Dual Messenger • user $userId"

            secureMarker ||
                listOf("secure", "knox", "folder", "مجلد", "آمن", "امن").any(label::contains) ->
                "Secure Folder • user $userId"

            listOf("work", "managed", "island", "profile", "العمل", "عمل").any(label::contains) ->
                "Work Profile • user $userId"

            else -> "$rawName • user $userId"
        }
    }
}
