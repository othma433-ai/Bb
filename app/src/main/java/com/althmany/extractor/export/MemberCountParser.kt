package com.althmany.extractor.export

import com.althmany.extractor.data.ScanRecord
import com.althmany.extractor.data.ScanStatus
import java.util.Locale
import kotlin.math.roundToInt

object MemberCountParser {
    private val arabicDigits = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9'
    )

    fun parse(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        var value = buildString {
            text.forEach { append(arabicDigits[it] ?: it) }
        }.lowercase(Locale.ROOT)
            .replace('٬', ',')
            .replace('،', ',')
            .trim()

        val multiplier = when {
            Regex("""\d(?:[.,]\d+)?\s*[kك]\b""").containsMatchIn(value) -> 1_000.0
            Regex("""\d(?:[.,]\d+)?\s*[mم]\b""").containsMatchIn(value) -> 1_000_000.0
            else -> 1.0
        }

        val numberToken = Regex("""\d[\d\s,\.]*""").find(value)?.value?.trim() ?: return null

        if (multiplier > 1.0) {
            val decimal = numberToken
                .replace(" ", "")
                .replace(",", ".")
                .toDoubleOrNull() ?: return null
            return (decimal * multiplier).roundToInt().coerceAtLeast(0)
        }

        val compact = numberToken.replace(" ", "")
        val normalized = when {
            compact.count { it == ',' || it == '.' } == 1 -> {
                val sepIndex = compact.indexOfFirst { it == ',' || it == '.' }
                val suffix = compact.substring(sepIndex + 1)
                if (suffix.length == 3) compact.filter(Char::isDigit)
                else compact.substring(0, sepIndex).filter(Char::isDigit)
            }
            else -> compact.filter(Char::isDigit)
        }
        return normalized.toIntOrNull()
    }
}

object ScanResultOrganizer {
    val statusOrder: List<ScanStatus> = listOf(
        ScanStatus.DIRECT,
        ScanStatus.APPROVAL,
        ScanStatus.REQUEST_PENDING,
        ScanStatus.ALREADY_MEMBER,
        ScanStatus.JOINED,
        ScanStatus.FULL,
        ScanStatus.INVALID,
        ScanStatus.REMOVED,
        ScanStatus.ACCOUNT_LIMIT,
        ScanStatus.NETWORK_ERROR,
        ScanStatus.ACTION_UNCERTAIN,
        ScanStatus.UNKNOWN,
        ScanStatus.ERROR,
        ScanStatus.PENDING,
        ScanStatus.SCANNING
    )

    fun sorted(items: List<ScanRecord>): List<ScanRecord> =
        items.sortedWith(
            compareBy<ScanRecord> { MemberCountParser.parse(it.memberCountText) == null }
                .thenByDescending { MemberCountParser.parse(it.memberCountText) ?: -1 }
                .thenBy { it.groupName.orEmpty().lowercase(Locale.ROOT) }
                .thenBy { it.normalizedUrl }
        )

    fun grouped(items: List<ScanRecord>): LinkedHashMap<ScanStatus, List<ScanRecord>> {
        val source = sorted(items)
        val out = linkedMapOf<ScanStatus, List<ScanRecord>>()
        statusOrder.forEach { status ->
            val bucket = source.filter { it.status == status }
            if (bucket.isNotEmpty()) out[status] = bucket
        }
        return out
    }

    fun sheetName(status: ScanStatus): String = when (status) {
        ScanStatus.DIRECT -> "Direct Join"
        ScanStatus.APPROVAL -> "Approval Required"
        ScanStatus.REQUEST_PENDING -> "Request Pending"
        ScanStatus.ALREADY_MEMBER -> "Already Member"
        ScanStatus.JOINED -> "Joined"
        ScanStatus.INVALID -> "Invalid"
        ScanStatus.FULL -> "Full"
        ScanStatus.REMOVED -> "Removed"
        ScanStatus.ACCOUNT_LIMIT -> "Account Limit"
        ScanStatus.NETWORK_ERROR -> "Network Error"
        ScanStatus.ACTION_UNCERTAIN -> "Action Uncertain"
        ScanStatus.UNKNOWN -> "Unknown"
        ScanStatus.ERROR -> "Error"
        ScanStatus.PENDING -> "Pending"
        ScanStatus.SCANNING -> "Scanning"
    }
}
