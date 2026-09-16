package com.althmany.extractor.ui

data class LinkPreview(
    val totalLines: Int,
    val visibleLines: List<String>,
    val hiddenCount: Int
)

object LinkPreviewPolicy {
    const val COLLAPSED_LIMIT = 4

    fun analyze(
        text: String,
        expanded: Boolean,
        collapsedLimit: Int = COLLAPSED_LIMIT
    ): LinkPreview {
        require(collapsedLimit > 0)
        val lines = text.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()
        val visible = if (expanded) lines else lines.take(collapsedLimit)
        return LinkPreview(
            totalLines = lines.size,
            visibleLines = visible,
            hiddenCount = if (expanded) 0 else (lines.size - visible.size).coerceAtLeast(0)
        )
    }
}
