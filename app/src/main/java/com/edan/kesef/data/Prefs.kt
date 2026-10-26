package com.edan.kesef.data

import android.content.Context
import androidx.core.content.edit

/** הגדרות קטנות (לא נתונים כספיים), נשמרות ב-SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("kesef_prefs", Context.MODE_PRIVATE)

    var reminderEnabled: Boolean
        get() = sp.getBoolean("reminder_enabled", true)
        set(v) = sp.edit { putBoolean("reminder_enabled", v) }

    /** שעת התזכורת היומית (0-23) */
    var reminderHour: Int
        get() = sp.getInt("reminder_hour", 21)
        set(v) = sp.edit { putInt("reminder_hour", v) }

    /** התראה על תשלום שנקלט מ-Wallet */
    var captureNotify: Boolean
        get() = sp.getBoolean("capture_notify", true)
        set(v) = sp.edit { putBoolean("capture_notify", v) }

    /** יתרת החשבון לפני התנועה הראשונה שרשומה באפליקציה, באגורות. ברירת מחדל 0. */
    var openingBalance: Long
        get() = sp.getLong("opening_balance", 0L)
        set(v) = sp.edit { putLong("opening_balance", v) }

    /** כדי לא להציק עם התראת "יש עדכון" על אותה גרסה שוב ושוב */
    var lastUpdateNotified: Int
        get() = sp.getInt("last_update_notified", 0)
        set(v) = sp.edit { putInt("last_update_notified", v) }
}
