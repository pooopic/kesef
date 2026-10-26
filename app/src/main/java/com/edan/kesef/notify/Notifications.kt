package com.edan.kesef.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.edan.kesef.MainActivity
import com.edan.kesef.R
import com.edan.kesef.data.Category
import com.edan.kesef.data.Entry
import com.edan.kesef.data.EntryType
import com.edan.kesef.util.Money

/**
 * כל ההתראות שהאפליקציה שולחת.
 * "ערוץ" (Channel) = סוג התראה. המשתמש יכול לכבות ערוץ מסוים בהגדרות הטלפון בלי לכבות את כולם.
 */
object Notifications {
    private const val CH_CAPTURED = "captured"
    private const val CH_REMINDERS = "reminders"
    // "updates_v2": לערוץ קיים אי אפשר לשנות חשיבות, אז יצרנו ערוץ חדש במקום הישן (שהיה שקט מדי)
    private const val CH_UPDATES = "updates_v2"

    private const val ID_REMINDER = 1
    private const val ID_UPDATE = 2

    const val EXTRA_ENTRY_ID = "entry_id"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel("updates") // הערוץ הישן
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_CAPTURED, "תשלומים שנקלטו", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "הודעה כשתשלום מ-Google Wallet נרשם אוטומטית" },
                NotificationChannel(CH_REMINDERS, "תזכורות", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "תזכורת יומית לעבור על ההוצאות" },
                NotificationChannel(CH_UPDATES, "עדכוני גרסה", NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showCaptured(context: Context, entry: Entry, category: Category?) {
        val verb = if (entry.type == EntryType.INCOME) "נרשם זיכוי" else "נרשמה הוצאה"
        val cat = category?.let { "${it.emoji} ${it.name}" } ?: ""
        post(
            context, CH_CAPTURED, entry.id.toInt() + 1000,
            title = "$verb ${Money.display(entry.amount)}",
            text = "${entry.merchant} · $cat  ·  לחץ לשינוי",
            entryId = entry.id,
        )
    }

    fun showReminder(context: Context, title: String, text: String) =
        post(context, CH_REMINDERS, ID_REMINDER, title, text)

    fun showUpdate(context: Context, versionName: String) =
        post(context, CH_UPDATES, ID_UPDATE, "גרסה $versionName זמינה", "לחץ כדי לעדכן את האפליקציה")

    private fun post(
        context: Context,
        channel: String,
        id: Int,
        title: String,
        text: String,
        entryId: Long? = null,
    ) {
        if (!canPost(context)) return
        // לחיצה על ההתראה פותחת את האפליקציה (ואם יש entryId: ישר למסך העריכה)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            entryId?.let { putExtra(EXTRA_ENTRY_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF34D399.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // ההרשאה בוטלה בדיוק עכשיו. לא קריטי, פשוט לא נציג.
        }
    }
}
