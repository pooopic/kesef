package com.edan.kesef.data

/**
 * מזהים קבועים לקטגוריות המובנות. הסיווג האוטומטי מחזיר את המזהים האלה,
 * ולכן הם לא משתנים לעולם (קטגוריות שתוסיף יקבלו מזהים חדשים).
 */
object CatIds {
    const val GROCERIES = 1L
    const val FOOD_OUT = 2L
    const val TRANSPORT = 3L
    const val HOUSING = 4L
    const val BILLS = 5L
    const val SHOPPING = 6L
    const val HEALTH = 7L
    const val FUN = 8L
    const val STUDY = 9L
    const val GIFTS_OUT = 10L
    const val OTHER_EXPENSE = 11L

    const val SALARY = 20L
    const val SCHOLARSHIP = 21L
    const val GIFT_IN = 22L
    const val REFUND = 23L
    const val OTHER_INCOME = 24L
}

object DefaultCategories {
    val all = listOf(
        Category(CatIds.GROCERIES, "סופר", "🛒", 0xFF34D399, EntryType.EXPENSE, true),
        Category(CatIds.FOOD_OUT, "אוכל בחוץ", "🍔", 0xFFFBBF24, EntryType.EXPENSE, true),
        Category(CatIds.TRANSPORT, "תחבורה ודלק", "🚗", 0xFF60A5FA, EntryType.EXPENSE, true),
        Category(CatIds.HOUSING, "דיור", "🏠", 0xFFA78BFA, EntryType.EXPENSE, true),
        Category(CatIds.BILLS, "חשבונות", "💡", 0xFFF472B6, EntryType.EXPENSE, true),
        Category(CatIds.SHOPPING, "קניות", "🛍️", 0xFFFB7185, EntryType.EXPENSE, true),
        Category(CatIds.HEALTH, "בריאות", "💊", 0xFF2DD4BF, EntryType.EXPENSE, true),
        Category(CatIds.FUN, "בילויים", "🎬", 0xFFF97316, EntryType.EXPENSE, true),
        Category(CatIds.STUDY, "לימודים", "📚", 0xFF818CF8, EntryType.EXPENSE, true),
        Category(CatIds.GIFTS_OUT, "מתנות", "🎁", 0xFFE879F9, EntryType.EXPENSE, true),
        Category(CatIds.OTHER_EXPENSE, "אחר", "📦", 0xFF94A3B8, EntryType.EXPENSE, true),

        Category(CatIds.SALARY, "משכורת", "💼", 0xFF34D399, EntryType.INCOME, true),
        Category(CatIds.SCHOLARSHIP, "מלגה", "🎓", 0xFF60A5FA, EntryType.INCOME, true),
        Category(CatIds.GIFT_IN, "מתנה", "🎁", 0xFFE879F9, EntryType.INCOME, true),
        Category(CatIds.REFUND, "החזרים", "↩️", 0xFFFBBF24, EntryType.INCOME, true),
        Category(CatIds.OTHER_INCOME, "אחר", "➕", 0xFF94A3B8, EntryType.INCOME, true),
    )

    /** צבעים לבחירה כשמוסיפים קטגוריה חדשה */
    val palette = listOf(
        0xFF34D399, 0xFFFBBF24, 0xFF60A5FA, 0xFFA78BFA, 0xFFF472B6,
        0xFFFB7185, 0xFF2DD4BF, 0xFFF97316, 0xFF818CF8, 0xFF94A3B8,
    )
}
