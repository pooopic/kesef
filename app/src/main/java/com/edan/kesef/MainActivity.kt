package com.edan.kesef

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.edan.kesef.notify.Notifications
import com.edan.kesef.ui.KesefRoot
import com.edan.kesef.ui.theme.KesefTheme

class MainActivity : ComponentActivity() {

    /** אם נפתחנו מלחיצה על התראת "נרשמה הוצאה": איזו תנועה לפתוח לעריכה */
    private val entryToOpen = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readIntent(intent)
        setContent {
            KesefTheme {
                // כל האפליקציה מימין לשמאל, גם אם שפת הטלפון אנגלית
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    KesefRoot(
                        entryToOpen = entryToOpen.value,
                        onEntryOpened = { entryToOpen.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        val id = intent?.getLongExtra(Notifications.EXTRA_ENTRY_ID, -1L) ?: -1L
        if (id > 0) entryToOpen.value = id
    }
}
