package com.edan.kesef.wallet

import com.edan.kesef.util.Money

data class ParsedPayment(
    val amountAgorot: Long,
    val merchant: String,
    /** "ILS", "USD", "EUR", "GBP" */
    val currency: String,
    /** החזר כספי (זיכוי) ולא חיוב */
    val isRefund: Boolean,
)

/**
 * מפענח את הטקסט של התראת תשלום מ-Google Wallet.
 *
 * דוגמאות לפורמטים שגוגל משתמשת בהם (משתנה לפי שפה ומדינה):
 *   כותרת: "שופרסל דיל"     טקסט: "‏₪87.40 עם Visa •••• 4242"
 *   כותרת: "Shufersal"       טקסט: "₪87.40 with Mastercard •••• 1234"
 *   כותרת: "₪23.50 at Aroma" טקסט: "Visa •••• 4242"
 *
 * אין כאן קוד של אנדרואיד, רק טקסט. לכן אפשר לבדוק אותו בבדיקות יחידה (WalletParserTest).
 * אם מגיע פורמט שלא נקלט: רואים אותו ב"יומן Wallet" בהגדרות, מוסיפים בדיקה ומתקנים כאן.
 */
object WalletParser {

    // תווים בלתי נראים שגוגל מכניסה לטקסט בעברית (סימני כיווניות RTL/LTR).
    // הם "שוברים" את החיפוש אם לא מנקים אותם.
    private val invisible = Regex("[‎‏‪-‮⁦-⁩]")

    private const val CURRENCY = """₪|\bILS\b|\bNIS\b|ש"ח|ש״ח|\$|\bUSD\b|€|\bEUR\b|£|\bGBP\b"""
    private const val NUMBER = """\d[\d.,]*\d|\d"""

    // סכום עם מטבע לפני ("₪45.90") או אחרי ("45.90 ₪")
    private val amountRegex = Regex(
        """(?:($CURRENCY)\s*($NUMBER))|(?:($NUMBER)\s*($CURRENCY))""",
        RegexOption.IGNORE_CASE,
    )

    // "at Aroma" / "ב-ארומה" / "אצל ארומה"
    private val merchantAfterAmount = Regex("""(?:\bat\b|אצל|ב[-־])\s*(.+)""", RegexOption.IGNORE_CASE)

    // פרטי הכרטיס שבאים אחרי שם העסק: "with Visa ••••", "באמצעות", "עם Visa"
    private val cardTail = Regex("""\s+(?:with|using|via|עם|באמצעות|בכרטיס)\b.*$|\s*[•·*]{2,}.*$""", RegexOption.IGNORE_CASE)

    private val genericTitles = setOf("google wallet", "google pay", "wallet", "ארנק google")
    private val refundWords = listOf("refund", "החזר", "זיכוי", "returned")

    fun parse(title: String?, text: String?): ParsedPayment? {
        val t = clean(title)
        val x = clean(text)
        val combined = "$t\n$x"

        val match = amountRegex.find(combined) ?: return null
        val symbol = match.groupValues[1].ifEmpty { match.groupValues[4] }
        val number = match.groupValues[2].ifEmpty { match.groupValues[3] }
        val amount = Money.parse(number) ?: return null
        if (amount <= 0) return null

        return ParsedPayment(
            amountAgorot = amount,
            merchant = extractMerchant(t, x, match.value),
            currency = currencyCode(symbol),
            isRefund = refundWords.any { combined.contains(it, ignoreCase = true) },
        )
    }

    private fun extractMerchant(title: String, text: String, amountText: String): String {
        val titleIsUseful = title.isNotBlank() &&
            title.lowercase() !in genericTitles &&
            !title.contains(amountText)
        if (titleIsUseful) return title.replace(cardTail, "").trim()

        // הכותרת היא הסכום עצמו או "Google Wallet": מחפשים את שם העסק בשורה שיש בה סכום
        val line = listOf(title, text).firstOrNull { it.contains(amountText) } ?: text
        val after = line.substringAfter(amountText).trim()
        val name = merchantAfterAmount.find(after)?.groupValues?.get(1)
            ?: after.takeIf { it.isNotBlank() && !cardTail.matches(" $it") }
            ?: ""
        return name.replace(cardTail, "").trim().ifBlank { "Google Wallet" }
    }

    private fun currencyCode(symbol: String) = when (symbol.uppercase()) {
        "$", "USD" -> "USD"
        "€", "EUR" -> "EUR"
        "£", "GBP" -> "GBP"
        else -> "ILS"
    }

    private fun clean(s: String?) = s.orEmpty()
        .replace(invisible, "")
        .replace(' ', ' ')
        .trim()
}
