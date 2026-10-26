package com.edan.kesef.ui

import android.app.Application
import android.net.Uri
import com.edan.kesef.importer.CsvImport
import com.edan.kesef.importer.ImportRow
import kotlinx.coroutines.withContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edan.kesef.KesefApp
import com.edan.kesef.data.Category
import com.edan.kesef.data.Entry
import com.edan.kesef.data.EntryType
import com.edan.kesef.notify.ReminderWorker
import com.edan.kesef.update.ReleaseInfo
import com.edan.kesef.update.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

/** מצב בדיקת העדכונים, כפי שהמסך מציג אותו */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: ReleaseInfo) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data class Error(val message: String) : UpdateState
}

/** מצב הייבוא מקובץ */
sealed interface ImportState {
    data object Idle : ImportState
    data class Preview(val rows: List<ImportRow>, val errors: List<String>, val newCategories: List<String>) : ImportState
    data object Importing : ImportState
    data class Done(val added: Int, val skipped: Int) : ImportState
    data class Failed(val message: String) : ImportState
}

/**
 * ה-ViewModel מחזיק את המצב של המסכים ושורד סיבוב מסך וכו'.
 * המסכים רק "מציגים" את מה שיש כאן וקוראים לפונקציות כשהמשתמש לוחץ.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as KesefApp).repo
    val prefs get() = repo.prefs

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

    val categories: StateFlow<List<Category>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** התנועות של החודש הנבחר. כשמחליפים חודש, flatMapLatest עובר אוטומטית לשאילתה החדשה. */
    val entries: StateFlow<List<Entry>> = _month
        .flatMapLatest { repo.entriesForMonth(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val walletLog = repo.walletLog.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun shiftMonth(delta: Long) {
        _month.value = _month.value.plusMonths(delta)
    }

    // ---------- תנועות וקטגוריות ----------

    suspend fun loadEntry(id: Long): Entry? = repo.entry(id)
    fun save(entry: Entry) = viewModelScope.launch { repo.save(entry) }
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }

    fun addCategory(name: String, emoji: String, color: Long, type: EntryType) =
        viewModelScope.launch { repo.addCategory(name, emoji, color, type) }

    fun deleteCategory(category: Category) = viewModelScope.launch { repo.deleteCategory(category) }

    /** כפתור בדיקה: מתנהג כאילו הגיעה התראת תשלום מ-Wallet */
    fun simulateWalletPayment() = viewModelScope.launch(Dispatchers.IO) {
        repo.handleWalletNotification("שופרסל דיל", "‏₪87.40 עם Visa •••• 4242", System.currentTimeMillis())
    }

    // ---------- תזכורות ----------

    fun setReminder(enabled: Boolean, hour: Int) {
        prefs.reminderEnabled = enabled
        prefs.reminderHour = hour.coerceIn(0, 23)
        ReminderWorker.schedule(getApplication(), replace = true)
    }

    // ---------- ייבוא מקובץ ----------

    val importState = MutableStateFlow<ImportState>(ImportState.Idle)

    /** שלב 1: קוראים ובודקים את הקובץ, ומציגים תצוגה מקדימה. עדיין לא נשמר כלום. */
    fun loadImportFile(uri: Uri) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        val parsed = withContext(Dispatchers.IO) {
            try {
                val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext null
                CsvImport.parse(CsvImport.decode(bytes))
            } catch (e: Exception) {
                null
            }
        }
        if (parsed == null) {
            importState.value = ImportState.Failed("לא הצלחתי לקרוא את הקובץ")
            return@launch
        }
        val existing = categories.value.map { it.type to it.name.trim() }.toSet()
        val newCategories = parsed.rows.map { it.type to it.category }.distinct()
            .filter { it !in existing }
            .map { it.second }
        importState.value = ImportState.Preview(parsed.rows, parsed.errors, newCategories)
    }

    /** שלב 2: המשתמש אישר. שומרים. */
    fun confirmImport(rows: List<ImportRow>) = viewModelScope.launch {
        importState.value = ImportState.Importing
        val s = repo.importRows(rows)
        importState.value = ImportState.Done(s.added, s.skipped)
    }

    fun cancelImport() {
        importState.value = ImportState.Idle
    }

    // ---------- עדכונים ----------

    val update = MutableStateFlow<UpdateState>(UpdateState.Idle)

    fun checkForUpdate() = viewModelScope.launch {
        update.value = UpdateState.Checking
        val info = Updater.fetchLatest()
        update.value = when {
            info == null -> UpdateState.Error("לא הצלחתי לבדוק מול GitHub (אין רשת?)")
            Updater.isNewer(info) -> UpdateState.Available(info)
            else -> UpdateState.UpToDate
        }
    }

    fun downloadAndInstall(info: ReleaseInfo) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        if (!Updater.canInstall(ctx)) {
            // פעם ראשונה: צריך לאשר "התקנה ממקור זה". אחרי האישור חוזרים ולוחצים שוב על "עדכן".
            Updater.openInstallPermissionSettings(ctx)
            return@launch
        }
        update.value = UpdateState.Downloading(0f)
        try {
            val file = Updater.download(ctx, info) { update.value = UpdateState.Downloading(it) }
            update.value = UpdateState.Available(info)
            Updater.install(ctx, file)
        } catch (e: Exception) {
            update.value = UpdateState.Error("ההורדה נכשלה: ${e.message}")
        }
    }
}
