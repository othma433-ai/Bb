package com.althmany.extractor.engine

import java.text.Normalizer
import java.util.Locale

/** Pure policy for deciding whether a visible WhatsApp label can be a conversation title. */
object ConversationTitlePolicy {
    private val exactExcluded = setOf(
        "واتساب", "whatsapp",
        "الدردشات", "chats",
        "التحديثات", "updates",
        "المجتمعات", "communities",
        "المكالمات", "calls",
        "بحث", "search",
        "مؤرشفة", "المؤرشفة", "archived",
        "رسالة", "message",
        "الحالة", "status",
        "الكل", "all",
        "غير مقروءة", "غير المقروءة", "unread",
        "المفضلة", "favorites",
        "المجموعات", "groups",
        "اسأل meta ai أو ابحث", "ask meta ai or search",
        "مزامنة جهات الاتصال", "sync contacts",
        "إدارة المجموعات", "ادارة المجموعات", "manage groups",
        "مجموعة جديدة", "new group",
        "دردشة جديدة", "new chat",
        "معلومات المجموعة", "group info",
        "إضافة مجموعة", "اضافة مجموعة", "add group",
        "إنشاء مجتمع", "انشاء مجتمع", "create community"
    )

    private val systemContains = listOf(
        "جهات اتصالك غير متزامنة",
        "لا تتمكن من العثور على جهات اتصالك",
        "مراجعة جهات الاتصال",
        "your contacts aren't synced",
        "your contacts are not synced",
        "review your contacts",
        "رسائلك الشخصية مشفرة تمامًا بين الطرفين",
        "end-to-end encrypted",
        "اضغط لبدء دردشة",
        "tap to start a chat"
    )

    fun isCandidate(value: String): Boolean {
        val display = normalizeDisplay(value)
        if (display.length !in 1..120) return false
        if (display.contains("http://", ignoreCase = true) || display.contains("https://", ignoreCase = true)) return false
        if (display.matches(Regex("^[0-9٠-٩۰-۹:./\\- ]+$"))) return false

        val folded = display.lowercase(Locale.ROOT)
        if (folded in exactExcluded) return false
        return systemContains.none { folded.contains(it, ignoreCase = true) }
    }

    private fun normalizeDisplay(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u206F\\uFEFF]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
