package com.edan.kesef.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.edan.kesef.data.Prefs
import com.edan.kesef.notify.Notifications
import java.util.concurrent.TimeUnit

/** בודק עדכונים ברקע פעמיים ביום, ושולח התראה אם יצאה גרסה חדשה. */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val info = Updater.fetchLatest() ?: return Result.success()
        val prefs = Prefs(applicationContext)
        if (Updater.isNewer(info) && prefs.lastUpdateNotified < info.versionCode) {
            Notifications.showUpdate(applicationContext, info.versionName)
            prefs.lastUpdateNotified = info.versionCode
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("update_check", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
