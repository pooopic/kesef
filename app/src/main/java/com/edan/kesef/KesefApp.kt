package com.edan.kesef

import android.app.Application
import com.edan.kesef.data.AppDatabase
import com.edan.kesef.data.Repository
import com.edan.kesef.notify.Notifications
import com.edan.kesef.notify.ReminderWorker
import com.edan.kesef.update.UpdateCheckWorker

/**
 * רץ ראשון, לפני כל מסך או שירות. כאן מכינים את מה שכולם צריכים:
 * מסד הנתונים, ערוצי ההתראות ותזמון משימות הרקע.
 */
class KesefApp : Application() {

    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = Repository(this, AppDatabase.get(this))
        Notifications.createChannels(this)
        ReminderWorker.schedule(this, replace = false)
        UpdateCheckWorker.schedule(this)
    }
}
