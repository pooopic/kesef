package com.edan.kesef.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.edan.kesef.KesefApp
import com.edan.kesef.data.Prefs
import com.edan.kesef.util.Money
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * תזכורת יומית. WorkManager מריץ אותה פעם ביום בשעה שבחרת,
 * גם אם האפליקציה סגורה וגם אחרי הפעלה מחדש של הטלפון.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as KesefApp).repo
        if (!repo.prefs.reminderEnabled) return Result.success()

        val today = repo.todaySummary()
        val (title, text) = if (today.count == 0) {
            "איך היה היום? 💸" to "לא נרשמו תנועות היום. שילמת במזומן או בהעברה? כדאי לרשום עכשיו."
        } else {
            val extra = if (today.uncategorized > 0) {
                "\n${today.uncategorized} תנועות מחכות לסיווג (בקטגוריה \"אחר\")."
            } else ""
            "סיכום היום" to "הוצאת היום ${Money.display(today.expenses)} ב-${today.count} תנועות.$extra"
        }
        Notifications.showReminder(applicationContext, title, text)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_reminder"

        /**
         * @param replace true כשהמשתמש שינה שעה, כדי לתזמן מחדש. false בפתיחת האפליקציה,
         *                כדי לא לאפס תזמון קיים.
         */
        fun schedule(context: Context, replace: Boolean) {
            val prefs = Prefs(context)
            val wm = WorkManager.getInstance(context)
            if (!prefs.reminderEnabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            // כמה זמן עד הפעם הבאה שהשעה תהיה X:00
            val now = LocalDateTime.now()
            var next = now.withHour(prefs.reminderHour).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delayMinutes = Duration.between(now, next).toMinutes()

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
