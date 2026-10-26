package com.edan.kesef.importer

import com.edan.kesef.data.EntryType
import com.edan.kesef.util.Money
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** שורה אחת מקובץ הייבוא, אחרי בדיקה ופענוח */
data class ImportRow(
    val date: LocalDate,
    val amountAgorot: Long,
    val type: EntryType,
    val category: String,
    val description: String,
    val account: String,
    val note: String,
)

data class ParsedFile(val rows: List<ImportRow>, val errors: List<String>)

/**
 * קורא קובץ CSV בפורמט הייבוא של האפליקציה (ראו docs/IMPORT_FORMAT.md).
 *
 * עמודות (לפי השם בשורה הראשונה, הסדר לא משנה):
 *   תאריך*, סכום*, קטגוריה*, סוג, תיאור, אמצעי תשלום, הערה      (* = חובה)
 *
 * כמו WalletParser, אין כאן קוד אנדרואיד, ולכן יש לזה בדיקות (CsvImportTest).
 */
object CsvImport {

    const val DATE = "תאריך"
    const val AMOUNT = "סכום"
    const val TYPE = "סוג"
    const val CATEGORY = "קטגוריה"
    const val DESCRIPTION = "תיאור"
    const val ACCOUNT = "אמצעי תשלום"
    const val NOTE = "הערה"

    val HEADER = listOf(DATE, AMOUNT, TYPE, CATEGORY, DESCRIPTION, ACCOUNT, NOTE)

    /** קטגוריה שאליה נכנס זיכוי (סכום שלילי בהוצאה) */
    const val REFUND_CATEGORY = "החזרים"

    private val dateFormats = listOf("d/M/yyyy", "d/M/yy", "yyyy-M-d", "d.M.yyyy", "d-M-yyyy")
        .map { DateTimeFormatter.ofPattern(it) }

    /**
     * Excel שומר CSV בשני קידודים: "CSV UTF-8" (מומלץ), או "CSV" רגיל שבעברית יוצא Windows-1255.
     * מנסים UTF-8 קודם; אם הבייטים לא חוקיים ב-UTF-8, זה כנראה Windows-1255.
     */
    fun decode(bytes: ByteArray): String = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: CharacterCodingException) {
        String(bytes, Charset.forName("windows-1255"))
    }

    fun parse(text: String): ParsedFile {
        val clean = text.removePrefix("﻿") // BOM ש-Excel מוסיף בתחילת הקובץ
        val firstLine = clean.lineSequence().firstOrNull().orEmpty()
        // ב-Excel עם הגדרות אזוריות מסוימות המפריד הוא ; ולא ,
        val sep = when {
            firstLine.contains('\t') -> '\t'
            firstLine.count { it == ';' } > firstLine.count { it == ',' } -> ';'
            else -> ','
        }
        val records = splitCsv(clean, sep).filter { r -> r.any { it.isNotBlank() } }
        if (records.isEmpty()) return ParsedFile(emptyList(), listOf("הקובץ ריק"))

        val header = records.first().map { it.trim() }
        val missing = listOf(DATE, AMOUNT, CATEGORY).filter { it !in header }
        if (missing.isNotEmpty()) {
            return ParsedFile(
                emptyList(),
                listOf("חסרות עמודות: ${missing.joinToString()}. השורה הראשונה צריכה להיות: ${HEADER.joinToString(",")}"),
            )
        }
        fun List<String>.col(name: String): String {
            val i = header.indexOf(name)
            return if (i in indices) this[i].trim() else ""
        }

        val rows = mutableListOf<ImportRow>()
        val errors = mutableListOf<String>()
        records.drop(1).forEachIndexed { index, r ->
            val line = index + 2 // +1 לכותרת, +1 כי סופרים מ-1
            val date = parseDate(r.col(DATE))
            if (date == null) {
                errors += "שורה $line: תאריך לא תקין \"${r.col(DATE)}\""
                return@forEachIndexed
            }
            val amount = Money.parse(r.col(AMOUNT).replace("₪", ""))
            if (amount == null) {
                errors += "שורה $line: סכום לא תקין \"${r.col(AMOUNT)}\""
                return@forEachIndexed
            }
            if (amount == 0L) return@forEachIndexed // שורות של 0 (הנחות, הערות) פשוט מדלגים

            var type = when (r.col(TYPE)) {
                "", "הוצאה" -> EntryType.EXPENSE
                "הכנסה" -> EntryType.INCOME
                else -> {
                    errors += "שורה $line: סוג צריך להיות \"הוצאה\" או \"הכנסה\", לא \"${r.col(TYPE)}\""
                    return@forEachIndexed
                }
            }
            var category = r.col(CATEGORY).ifBlank { "אחר" }
            var note = r.col(NOTE)
            // סכום שלילי בהוצאה = זיכוי (החזר כספי). נרשם כהכנסה בקטגוריית "החזרים".
            if (amount < 0) {
                if (type == EntryType.INCOME) {
                    errors += "שורה $line: הכנסה עם סכום שלילי"
                    return@forEachIndexed
                }
                type = EntryType.INCOME
                note = listOf("זיכוי ($category)", note).filter { it.isNotBlank() }.joinToString(" · ")
                category = REFUND_CATEGORY
            }
            rows += ImportRow(
                date = date,
                amountAgorot = kotlin.math.abs(amount),
                type = type,
                category = category,
                description = r.col(DESCRIPTION),
                account = r.col(ACCOUNT),
                note = note,
            )
        }
        return ParsedFile(rows, errors)
    }

    fun parseDate(s: String): LocalDate? {
        val t = s.trim().substringBefore(' ') // "01/03/2026 00:00" → "01/03/2026"
        for (f in dateFormats) {
            try {
                val d = LocalDate.parse(t, f)
                // "d/M/yy" מפרש 26 כ-2026; שנה דו-ספרתית אחרת לא הגיונית
                if (d.year in 2000..2100) return d
            } catch (e: Exception) {
                // מנסים את הפורמט הבא
            }
        }
        return null
    }

    /** פיצול CSV תקני: תומך בשדות במירכאות שיש בתוכם פסיקים ("פנאי, בילוי ותיירות") */
    fun splitCsv(text: String, sep: Char = ','): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var record = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == sep -> { record += field.toString(); field.clear() }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    record += field.toString(); field.clear()
                    records += record; record = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || record.isNotEmpty()) {
            record += field.toString()
            records += record
        }
        return records
    }
}
