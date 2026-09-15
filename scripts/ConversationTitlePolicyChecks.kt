import com.althmany.extractor.engine.ConversationTitlePolicy

private fun reject(value: String) = check(!ConversationTitlePolicy.isCandidate(value)) { "must reject: $value" }
private fun accept(value: String) = check(ConversationTitlePolicy.isCandidate(value)) { "must accept: $value" }

fun main() {
    listOf(
        "Manage groups", "إدارة المجموعات",
        "New group", "مجموعة جديدة",
        "Archived", "المؤرشفة",
        "Communities", "المجتمعات",
        "Search", "بحث",
        "New chat", "دردشة جديدة",
        "Group info", "معلومات المجموعة",
        "Add group", "إضافة مجموعة",
        "Create community", "إنشاء مجتمع",
        "Ask Meta AI or Search", "اسأل Meta AI أو ابحث",
        "https://chat.whatsapp.com/AbCdEf",
        "12:45"
    ).forEach(::reject)

    listOf(
        "طلاب الجامعة",
        "Surgery Batch 5",
        "Archived Cases Study Group",
        "مجموعة جديدة للمراجعة",
        "Community Medicine Batch"
    ).forEach(::accept)

    println("ConversationTitlePolicyChecks: PASS")
}
