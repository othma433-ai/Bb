package com.althmany.extractor.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkPreviewPolicyTest {
    @Test
    fun collapsedPreviewShowsFirstFourLinksOnly() {
        val text = (1..7).joinToString("\n") { "https://chat.whatsapp.com/link$it" }
        val preview = LinkPreviewPolicy.analyze(text, expanded = false)

        assertEquals(7, preview.totalLines)
        assertEquals(4, preview.visibleLines.size)
        assertEquals("https://chat.whatsapp.com/link1", preview.visibleLines.first())
        assertEquals("https://chat.whatsapp.com/link4", preview.visibleLines.last())
        assertEquals(3, preview.hiddenCount)
    }

    @Test
    fun expandedPreviewShowsAllLinks() {
        val text = (1..7).joinToString("\n") { "https://chat.whatsapp.com/link$it" }
        val preview = LinkPreviewPolicy.analyze(text, expanded = true)

        assertEquals(7, preview.visibleLines.size)
        assertEquals(0, preview.hiddenCount)
    }

    @Test
    fun blankLinesDoNotInflateHiddenCount() {
        val text = "a\n\n b \n\nc\n\nd\n\ne"
        val preview = LinkPreviewPolicy.analyze(text, expanded = false)

        assertEquals(5, preview.totalLines)
        assertEquals(listOf("a", "b", "c", "d"), preview.visibleLines)
        assertEquals(1, preview.hiddenCount)
    }
}
