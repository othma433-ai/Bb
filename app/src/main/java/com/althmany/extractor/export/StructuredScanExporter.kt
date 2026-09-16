package com.althmany.extractor.export

import android.content.ContentResolver
import android.net.Uri
import com.althmany.extractor.data.ScanRecord
import com.althmany.extractor.data.ScanStatus
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object StructuredScanExporter {
    fun export(
        resolver: ContentResolver,
        uri: Uri,
        format: ExportFormat,
        items: List<ScanRecord>
    ) {
        val sorted = ScanResultOrganizer.sorted(items)
        when (format) {
            ExportFormat.CSV -> exportCsv(resolver, uri, sorted)
            ExportFormat.TXT -> exportTxt(resolver, uri, sorted)
            ExportFormat.JSON -> exportJson(resolver, uri, sorted)
            ExportFormat.XLSX -> exportXlsx(resolver, uri, sorted)
        }
    }

    private fun exportCsv(
        resolver: ContentResolver,
        uri: Uri,
        items: List<ScanRecord>
    ) {
        resolver.openOutputStream(uri)?.use { out ->
            BufferedWriter(OutputStreamWriter(out, Charsets.UTF_8)).use { w ->
                w.write(
                    "Group,Members,Members Text,Status,Invite Kind,Confidence,URL,Invite Code," +
                        "Source Group,Signal,Detail,Attempts,Duration ms,Target Package,Scanned At\n"
                )
                items.forEach { r ->
                    val numeric = MemberCountParser.parse(r.memberCountText)
                    w.write(
                        listOf(
                            r.groupName.orEmpty(),
                            numeric?.toString().orEmpty(),
                            r.memberCountText.orEmpty(),
                            r.status.labelAr,
                            r.inviteKind.labelAr,
                            r.confidence.toString(),
                            r.normalizedUrl,
                            r.inviteCode,
                            r.sourceGroup.orEmpty(),
                            r.signalCode.orEmpty(),
                            r.detail.orEmpty(),
                            r.attempts.toString(),
                            r.durationMs?.toString().orEmpty(),
                            r.targetPackage.orEmpty(),
                            r.scannedAt?.toString().orEmpty()
                        ).joinToString(",") { csv(it) }
                    )
                    w.newLine()
                }
            }
        } ?: error("تعذر فتح ملف تصدير الفحص")
    }

    private fun exportTxt(
        resolver: ContentResolver,
        uri: Uri,
        items: List<ScanRecord>
    ) {
        val grouped = ScanResultOrganizer.grouped(items)
        resolver.openOutputStream(uri)?.use { out ->
            BufferedWriter(OutputStreamWriter(out, Charsets.UTF_8)).use { w ->
                grouped.forEach { (status, bucket) ->
                    w.write("============================================================")
                    w.newLine()
                    w.write("${status.labelAr} — ${bucket.size}")
                    w.newLine()
                    w.write("============================================================")
                    w.newLine()
                    bucket.forEachIndexed { index, r ->
                        w.write("${index + 1}. ${r.groupName ?: "اسم القروب غير متاح"}")
                        w.newLine()
                        w.write("   الأعضاء: ${MemberCountParser.parse(r.memberCountText)?.toString() ?: "غير متاح"}")
                        if (!r.memberCountText.isNullOrBlank()) {
                            w.write(" (${r.memberCountText})")
                        }
                        w.newLine()
                        w.write("   النوع: ${r.inviteKind.labelAr} • الثقة: ${r.confidence}%")
                        w.newLine()
                        w.write("   الرابط: ${r.normalizedUrl}")
                        w.newLine()
                        if (!r.detail.isNullOrBlank()) {
                            w.write("   التفاصيل: ${r.detail}")
                            w.newLine()
                        }
                        w.newLine()
                    }
                    w.newLine()
                }
            }
        } ?: error("تعذر فتح ملف تصدير الفحص")
    }

    private fun exportJson(
        resolver: ContentResolver,
        uri: Uri,
        items: List<ScanRecord>
    ) {
        resolver.openOutputStream(uri)?.use { out ->
            BufferedWriter(OutputStreamWriter(out, Charsets.UTF_8)).use { w ->
                w.write("[\n")
                items.forEachIndexed { index, r ->
                    val numeric = MemberCountParser.parse(r.memberCountText)
                    w.write(
                        "  {" +
                            "\"groupName\":\"${json(r.groupName.orEmpty())}\"," +
                            "\"memberCount\":${numeric ?: "null"}," +
                            "\"memberCountText\":\"${json(r.memberCountText.orEmpty())}\"," +
                            "\"status\":\"${json(r.status.name)}\"," +
                            "\"statusAr\":\"${json(r.status.labelAr)}\"," +
                            "\"inviteKind\":\"${json(r.inviteKind.name)}\"," +
                            "\"inviteKindAr\":\"${json(r.inviteKind.labelAr)}\"," +
                            "\"confidence\":${r.confidence}," +
                            "\"url\":\"${json(r.normalizedUrl)}\"," +
                            "\"inviteCode\":\"${json(r.inviteCode)}\"," +
                            "\"sourceGroup\":\"${json(r.sourceGroup.orEmpty())}\"," +
                            "\"signalCode\":\"${json(r.signalCode.orEmpty())}\"," +
                            "\"detail\":\"${json(r.detail.orEmpty())}\"," +
                            "\"attempts\":${r.attempts}," +
                            "\"durationMs\":${r.durationMs ?: 0}," +
                            "\"targetPackage\":\"${json(r.targetPackage.orEmpty())}\"," +
                            "\"scannedAt\":${r.scannedAt ?: 0}" +
                            "}"
                    )
                    if (index != items.lastIndex) w.write(",")
                    w.newLine()
                }
                w.write("]\n")
            }
        } ?: error("تعذر فتح ملف تصدير الفحص")
    }

    private fun exportXlsx(
        resolver: ContentResolver,
        uri: Uri,
        items: List<ScanRecord>
    ) {
        val grouped = ScanResultOrganizer.grouped(items)
        val sheets = mutableListOf<Pair<String, List<ScanRecord>>>()
        sheets += "All Results" to items
        grouped.forEach { (status, bucket) ->
            sheets += ScanResultOrganizer.sheetName(status) to bucket
        }

        resolver.openOutputStream(uri)?.use { raw ->
            ZipOutputStream(raw).use { zip ->
                val overrides = buildString {
                    sheets.indices.forEach { i ->
                        append(
                            """<Override PartName="/xl/worksheets/sheet${i + 1}.xml" """ +
                                """ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
                        )
                    }
                }

                put(
                    zip,
                    "[Content_Types].xml",
                    """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      $overrides
                    </Types>
                    """.trimIndent()
                )

                put(
                    zip,
                    "_rels/.rels",
                    """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """.trimIndent()
                )

                val workbookSheets = buildString {
                    sheets.forEachIndexed { index, (name, _) ->
                        append(
                            """<sheet name="${xml(name.take(31))}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
                        )
                    }
                }

                put(
                    zip,
                    "xl/workbook.xml",
                    """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>$workbookSheets</sheets>
                    </workbook>
                    """.trimIndent()
                )

                val rels = buildString {
                    sheets.indices.forEach { index ->
                        append(
                            """<Relationship Id="rId${index + 1}" """ +
                                """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" """ +
                                """Target="worksheets/sheet${index + 1}.xml"/>"""
                        )
                    }
                }

                put(
                    zip,
                    "xl/_rels/workbook.xml.rels",
                    """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      $rels
                    </Relationships>
                    """.trimIndent()
                )

                sheets.forEachIndexed { index, (_, bucket) ->
                    put(
                        zip,
                        "xl/worksheets/sheet${index + 1}.xml",
                        buildWorksheet(bucket)
                    )
                }
            }
        } ?: error("تعذر فتح ملف تصدير الفحص")
    }

    private fun buildWorksheet(items: List<ScanRecord>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        row(
            1,
            listOf(
                "#",
                "Group Name",
                "Members",
                "Members Text",
                "Status",
                "Invite Kind",
                "Confidence",
                "URL",
                "Invite Code",
                "Source Group",
                "Signal",
                "Detail",
                "Attempts",
                "Duration ms",
                "Target Package",
                "Scanned At"
            )
        )
        items.forEachIndexed { index, r ->
            row(
                index + 2,
                listOf(
                    (index + 1).toString(),
                    r.groupName.orEmpty(),
                    MemberCountParser.parse(r.memberCountText)?.toString().orEmpty(),
                    r.memberCountText.orEmpty(),
                    r.status.labelAr,
                    r.inviteKind.labelAr,
                    r.confidence.toString(),
                    r.normalizedUrl,
                    r.inviteCode,
                    r.sourceGroup.orEmpty(),
                    r.signalCode.orEmpty(),
                    r.detail.orEmpty(),
                    r.attempts.toString(),
                    r.durationMs?.toString().orEmpty(),
                    r.targetPackage.orEmpty(),
                    r.scannedAt?.toString().orEmpty()
                )
            )
        }
        append("</sheetData></worksheet>")
    }

    private fun StringBuilder.row(index: Int, cells: List<String>) {
        append("""<row r="$index">""")
        cells.forEachIndexed { col, value ->
            append("""<c r="${columnName(col + 1)}$index" t="inlineStr"><is><t xml:space="preserve">""")
            append(xml(value))
            append("</t></is></c>")
        }
        append("</row>")
    }

    private fun columnName(index: Int): String {
        var n = index
        val out = StringBuilder()
        while (n > 0) {
            n--
            out.append(('A'.code + n % 26).toChar())
            n /= 26
        }
        return out.reverse().toString()
    }

    private fun put(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun json(value: String): String =
        value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

    private fun xml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
