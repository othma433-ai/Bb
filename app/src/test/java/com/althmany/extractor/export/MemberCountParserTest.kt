package com.althmany.extractor.export

import com.althmany.extractor.data.InviteKind
import com.althmany.extractor.data.ScanRecord
import com.althmany.extractor.data.ScanStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemberCountParserTest {
    @Test
    fun parsesEnglishArabicAndCompactCounts() {
        assertEquals(1024, MemberCountParser.parse("1,024 members"))
        assertEquals(1024, MemberCountParser.parse("١٬٠٢٤ عضو"))
        assertEquals(1200, MemberCountParser.parse("1.2K members"))
        assertEquals(2500000, MemberCountParser.parse("2.5M members"))
        assertEquals(350, MemberCountParser.parse("350 مشارك"))
        assertNull(MemberCountParser.parse("عدد الأعضاء غير متاح"))
    }

    @Test
    fun sortsLargestMemberCountFirstAndUnknownLast() {
        val rows = listOf(
            record(1, "Small", "120 عضو"),
            record(2, "Unknown", null),
            record(3, "Largest", "١٬٠٢٤ عضو"),
            record(4, "Medium", "850 members")
        )

        val sorted = ScanResultOrganizer.sorted(rows)

        assertEquals(listOf("Largest", "Medium", "Small", "Unknown"), sorted.map { it.groupName })
    }

    @Test
    fun groupsResultsByStableStatusOrder() {
        val rows = listOf(
            record(1, "Approval", "900", ScanStatus.APPROVAL),
            record(2, "Direct", "500", ScanStatus.DIRECT),
            record(3, "Invalid", null, ScanStatus.INVALID)
        )

        val grouped = ScanResultOrganizer.grouped(rows)

        assertEquals(
            listOf(ScanStatus.DIRECT, ScanStatus.APPROVAL, ScanStatus.INVALID),
            grouped.keys.toList()
        )
    }

    private fun record(
        id: Long,
        name: String,
        members: String?,
        status: ScanStatus = ScanStatus.DIRECT
    ) = ScanRecord(
        id = id,
        url = "https://chat.whatsapp.com/test$id",
        normalizedUrl = "https://chat.whatsapp.com/test$id",
        inviteCode = "test$id",
        sourceGroup = null,
        status = status,
        groupName = name,
        detail = null,
        attempts = 1,
        addedAt = 0L,
        scannedAt = 1L,
        confidence = 99,
        memberCountText = members,
        inviteKind = InviteKind.GROUP
    )
}
