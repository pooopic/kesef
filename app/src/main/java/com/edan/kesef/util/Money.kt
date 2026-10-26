package com.edan.kesef.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * כל הסכומים באפליקציה נשמרים כ-Long באגורות (4590 = ₪45.90).
 * למה לא Double? כי Double לא מדויק לכסף: 0.1 + 0.2 = 0.30000000000000004.
 */
object Money {

    fun format(agorot: Long, showPlus: Boolean = false): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            // ₪50 ולא ₪50.00, אבל ₪45.90 ולא ₪45.9
            minimumFractionDigits = if (agorot % 100 == 0L) 0 else 2
            maximumFractionDigits = 2
        }
        val sign = when {
            agorot < 0 -> "-"
            showPlus && agorot > 0 -> "+"
            else -> ""
        }
        return "$sign₪${nf.format(abs(agorot) / 100.0)}"
    }

    /**
     * כמו format, אבל "עטוף" כטקסט משמאל לימין.
     * למה? בתוך משפט בעברית, אנדרואיד עלול להציג "-₪45" כ-"₪45-". העטיפה מונעת את זה.
     */
    fun display(agorot: Long, showPlus: Boolean = false) = "⁦${format(agorot, showPlus)}⁩"

    /** סכום כטקסט לעריכה, בלי ₪ ובלי פסיקים: 4590 → "45.90", 5000 → "50" */
    fun toEditable(agorot: Long): String =
        if (agorot % 100 == 0L) (agorot / 100).toString()
        else String.format(Locale.US, "%.2f", agorot / 100.0)

    /**
     * ממיר טקסט של סכום לאגורות. מבין גם "1,234.50" וגם "45,90" (פסיק עשרוני).
     * מחזיר null אם זה לא מספר.
     */
    fun parse(text: String): Long? {
        var s = text.trim().replace(" ", "").trim(',', '.')
        if (s.isEmpty()) return null
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        s = when {
            // יש גם נקודה וגם פסיק: מה שמופיע אחרון הוא המפריד העשרוני
            lastDot >= 0 && lastComma >= 0 ->
                if (lastDot > lastComma) s.replace(",", "")
                else s.replace(".", "").replace(',', '.')
            // רק פסיק: עם 1-2 ספרות אחריו זה עשרוני ("45,90"), אחרת אלפים ("1,234")
            lastComma >= 0 ->
                if (s.length - lastComma - 1 in 1..2) s.replace(',', '.') else s.replace(",", "")
            else -> s
        }
        return try {
            BigDecimal(s).movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
        } catch (e: Exception) {
            null
        }
    }
}
