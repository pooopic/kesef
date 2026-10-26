package com.edan.kesef.data

/**
 * מזהים קבועים לקטגוריות המובנות. הסיווג האוטומטי מחזיר את המזהים האלה,
 * ולכן הם לא משתנים לעולם (קטגוריות שתוסיף יקבלו מזהים חדשים).
 * קטגוריות שנוספו בגרסה 3 קיבלו מזהים מ-101, כדי לא להתנגש בקטגוריות שכבר הוספת.
 */
object CatIds {
    const val GROCERIES = 1L
    const val FOOD_OUT = 2L
    const val TRANSPORT = 3L
    const val HOUSING = 4L
    const val SUBSCRIPTIONS = 5L
    const val SHOPPING = 6L
    const val HEALTH = 7L
    const val LEISURE = 8L
    const val STUDY = 9L
    const val GIFTS_OUT = 10L
    const val OTHER_EXPENSE = 11L
    const val INSURANCE_TAX = 101L
    const val TRANSFERS_OUT = 102L

    const val SALARY = 20L
    const val RESERVE = 21L
    const val GRANTS = 22L
    const val REFUND = 23L
    const val OTHER_INCOME = 24L
    const val DEPOSITS = 103L
}

/**
 * הקטגוריות הקבועות: מעט קטגוריות כלליות, כדי שהסיכומים יהיו ברורים.
 * צריך משהו מפורט יותר? מוסיפים קטגוריה משלך במסך "ניהול קטגוריות".
 */
object DefaultCategories {
    val all = listOf(
        Category(CatIds.HOUSING, "דיור", "🏠", 0xFFA78BFA, EntryType.EXPENSE, true),
        Category(CatIds.SUBSCRIPTIONS, "מנויים וחשבונות", "🔁", 0xFFF472B6, EntryType.EXPENSE, true),
        Category(CatIds.GROCERIES, "מזון וסופר", "🛒", 0xFF34D399, EntryType.EXPENSE, true),
        Category(CatIds.FOOD_OUT, "אוכל בחוץ", "🍔", 0xFFFBBF24, EntryType.EXPENSE, true),
        Category(CatIds.TRANSPORT, "תחבורה ורכב", "🚗", 0xFF60A5FA, EntryType.EXPENSE, true),
        Category(CatIds.HEALTH, "בריאות וטיפוח", "💊", 0xFF2DD4BF, EntryType.EXPENSE, true),
        Category(CatIds.SHOPPING, "קניות ובית", "🛍️", 0xFFFB7185, EntryType.EXPENSE, true),
        Category(CatIds.LEISURE, "פנאי וחופשות", "🎬", 0xFFF97316, EntryType.EXPENSE, true),
        Category(CatIds.INSURANCE_TAX, "ביטוח, מיסים ועמלות", "🧾", 0xFF818CF8, EntryType.EXPENSE, true),
        Category(CatIds.STUDY, "לימודים", "📚", 0xFF38BDF8, EntryType.EXPENSE, true),
        Category(CatIds.TRANSFERS_OUT, "העברות ומזומן", "💸", 0xFFE879F9, EntryType.EXPENSE, true),
        Category(CatIds.GIFTS_OUT, "מתנות ותרומות", "🎁", 0xFFF0ABFC, EntryType.EXPENSE, true),
        Category(CatIds.OTHER_EXPENSE, "אחר", "📦", 0xFF94A3B8, EntryType.EXPENSE, true),

        Category(CatIds.SALARY, "משכורת", "💼", 0xFF34D399, EntryType.INCOME, true),
        Category(CatIds.RESERVE, "מילואים", "🎖️", 0xFF60A5FA, EntryType.INCOME, true),
        Category(CatIds.GRANTS, "מענקים וקצבאות", "🏛️", 0xFFA78BFA, EntryType.INCOME, true),
        Category(CatIds.DEPOSITS, "העברות והפקדות", "🏦", 0xFF2DD4BF, EntryType.INCOME, true),
        Category(CatIds.REFUND, "החזרים", "↩️", 0xFFFBBF24, EntryType.INCOME, true),
        Category(CatIds.OTHER_INCOME, "אחר", "➕", 0xFF94A3B8, EntryType.INCOME, true),
    )

    /** אימוג'י לקטגוריה שנוצרת בייבוא, לפי מילה בשם שלה. ברירת מחדל: 🏷️ */
    fun emojiFor(name: String): String {
        val hints = listOf(
            "ספורט" to "⚽", "רכב" to "🔧", "ביגוד" to "👕", "אופנה" to "👕", "העברות" to "💸",
            "מנוי" to "🔁", "ביטוח" to "🧾", "מיסים" to "🧾", "תרומ" to "💝", "בית" to "🛋️",
            "אלקטרוניקה" to "💻", "משכורת" to "💼", "החזר" to "↩️", "חיסכון" to "🐷",
        )
        return hints.firstOrNull { (word, _) -> name.contains(word) }?.second ?: "🏷️"
    }

    /** צבעים לבחירה כשמוסיפים קטגוריה חדשה */
    val palette = listOf(
        0xFF34D399, 0xFFFBBF24, 0xFF60A5FA, 0xFFA78BFA, 0xFFF472B6,
        0xFFFB7185, 0xFF2DD4BF, 0xFFF97316, 0xFF818CF8, 0xFF94A3B8,
    )
}
