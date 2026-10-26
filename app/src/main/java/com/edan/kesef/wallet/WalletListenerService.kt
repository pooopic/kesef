package com.edan.kesef.wallet

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.edan.kesef.KesefApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * אנדרואיד מעביר לשירות הזה כל התראה שמופיעה בטלפון (אחרי שאישרת "גישה להתראות").
 * אנחנו מתעלמים מכל מה שלא מגיע מ-Google Wallet.
 *
 * חשוב לדעת: אפשר לקלוט רק התראות שמגיעות מעכשיו והלאה, לא היסטוריה.
 * ואם ב-Wallet כבויות התראות התשלום, אין מה לקלוט.
 */
class WalletListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in WALLET_PACKAGES) return
        val n = sbn.notification
        // התראת "סיכום קבוצה" חוזרת על התראה אמיתית שכבר קיבלנו
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()

        val repo = (application as KesefApp).repo
        // עבודה עם מסד נתונים לא נעשית על ה-thread הראשי
        scope.launch { repo.handleWalletNotification(title, text, sbn.postTime) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        val WALLET_PACKAGES = setOf(
            "com.google.android.apps.walletnfcrel", // Google Wallet
        )
    }
}
