import java.io.File

private fun requireCount(path: String, token: String, minimum: Int) {
    val text = File(path).readText()
    val count = Regex(Regex.escape(token)).findAll(text).count()
    check(count >= minimum) { "$path must contain '$token' at least $minimum time(s); found $count" }
}

fun main() {
    requireCount(
        "app/src/main/java/com/althmany/extractor/engine/ExtractionController.kt",
        "SenderRuntimeGuard.isSenderRunning(appContext)",
        3
    )
    requireCount(
        "app/src/main/java/com/althmany/extractor/engine/ScanController.kt",
        "SenderRuntimeGuard.isSenderRunning(appContext)",
        1
    )
    requireCount(
        "app/src/main/java/com/althmany/extractor/engine/PublishController.kt",
        "SenderRuntimeGuard.isSenderRunning(appContext)",
        2
    )
    println("SenderGuardPureChecks: PASS")
}
