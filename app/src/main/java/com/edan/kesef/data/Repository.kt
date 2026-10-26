package com.edan.kesef.data

import android.content.Context
import androidx.room.withTransaction
import com.edan.kesef.importer.ImportRow
import com.edan.kesef.notify.Notifications
import com.edan.kesef.wallet.AutoCategorizer
import com.edan.kesef.wallet.WalletParser
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * שכבת ביניים בין המסכים למסד הנתונים. כאן נמצאת "הלוגיקה העסקית":
 * איך מסווגים תשלום, מה קורה כשמוחקים קטגוריה וכו'.
 * המסכים לא מדברים עם ה-DAO ישירות, רק עם המחלקה הזו.
 */
class Repository(private val context: Context, private val db: AppDatabase) {

    private val dao: KesefDao = db.dao()

    val importBatches: Flow<List<ImportBatch>> = dao.importBatches()

    /**
     * מוחק את כל מה שיובא מאמצעי תשלום מסוים (למשל קובץ שיובא בטעות).
     * תנועות שהוזנו ידנית או נקלטו מ-Wallet לא נפגעות.
     * אחר כך מוחקים קטגוריות שהוספו ונשארו ריקות.
     */
    suspend fun deleteImported(account: String): Int = db.withTransaction {
        val deleted = dao.deleteImported(account)
        dao.deleteEmptyCustomCategories()
        deleted
    }

    data class ImportSummary(val added: Int, val skipped: Int, val newCategories: List<String>)

    /**
     * מכניס שורות מקובץ ייבוא. קטגוריה שלא קיימת נוצרת אוטומטית.
     * ייבוא חוזר של אותו קובץ לא יוצר כפילויות: לכל שורה יש externalId קבוע.
     */
    suspend fun importRows(rows: List<ImportRow>): ImportSummary = db.withTransaction {
        val categories = dao.categoriesOnce().toMutableList()
        val newCategories = mutableListOf<String>()
        val seen = HashMap<String, Int>()
        var added = 0
        var skipped = 0
        for (r in rows) {
            val category = categories.firstOrNull { it.type == r.type && it.name.trim() == r.category }
                ?: Category(
                    name = r.category,
                    emoji = DefaultCategories.emojiFor(r.category),
                    color = DefaultCategories.palette[newCategories.size % DefaultCategories.palette.size],
                    type = r.type,
                ).let { c -> c.copy(id = dao.insertCategory(c)) }
                    .also { categories += it; newCategories += it.name }
            // שתי שורות זהות לגמרי באותו קובץ (למשל שני קפה באותו יום) מקבלות מספר רץ
            val key = "import|${r.date}|${r.amountAgorot}|${r.type}|${r.description}|${r.account}"
            val n = (seen[key] ?: 0) + 1
            seen[key] = n
            val id = dao.insertEntry(
                Entry(
                    amount = r.amountAgorot,
                    type = r.type,
                    categoryId = category.id,
                    note = r.note,
                    merchant = r.description,
                    timestamp = r.date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
                    source = EntrySource.IMPORT,
                    externalId = "$key|$n",
                    account = r.account,
                ),
            )
            if (id == -1L) skipped++ else added++
        }
        ImportSummary(added, skipped, newCategories)
    }

    val prefs = Prefs(context)
    val categories: Flow<List<Category>> = dao.categories()
    val walletLog: Flow<List<WalletLog>> = dao.recentLogs()

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun entriesForMonth(month: YearMonth): Flow<List<Entry>> {
        val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.entriesBetween(from, to)
    }

    suspend fun entry(id: Long): Entry? = dao.entry(id)

    /** שמירה של תנועה חדשה (id = 0) או עדכון של קיימת. */
    suspend fun save(entry: Entry) {
        if (entry.id == 0L) {
            dao.insertEntry(entry)
        } else {
            val before = dao.entry(entry.id)
            dao.updateEntry(entry)
            // שינית קטגוריה לתשלום מ-Wallet? נזכור את זה לפעם הבאה באותו בית עסק.
            if (entry.source == EntrySource.WALLET && entry.merchant.isNotBlank() &&
                before?.categoryId != entry.categoryId
            ) {
                dao.upsertRule(MerchantRule(normalize(entry.merchant), entry.categoryId))
            }
        }
    }

    suspend fun delete(id: Long) = dao.deleteEntry(id)

    suspend fun addCategory(name: String, emoji: String, color: Long, type: EntryType) {
        dao.insertCategory(Category(name = name, emoji = emoji, color = color, type = type))
    }

    suspend fun deleteCategory(category: Category) {
        if (category.builtIn) return
        val fallback = if (category.type == EntryType.EXPENSE) CatIds.OTHER_EXPENSE else CatIds.OTHER_INCOME
        dao.moveEntries(category.id, fallback)
        dao.deleteCategory(category)
    }

    data class DaySummary(val count: Int, val expenses: Long, val uncategorized: Int)

    /** בשביל התזכורת היומית: כמה הוצאת היום, וכמה תנועות עדיין ב"אחר". */
    suspend fun todaySummary(): DaySummary {
        val today = LocalDate.now()
        val from = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val list = dao.entriesBetweenOnce(from, to)
        return DaySummary(
            count = list.size,
            expenses = list.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount },
            uncategorized = list.count { it.categoryId == CatIds.OTHER_EXPENSE },
        )
    }

    /**
     * נקרא בכל פעם שמגיעה התראה מ-Google Wallet (או מכפתור "הדמיה" בהגדרות).
     * מפענח את הטקסט, מסווג, שומר ושולח התראה "נרשמה הוצאה".
     */
    suspend fun handleWalletNotification(title: String, text: String, postTime: Long) {
        val parsed = WalletParser.parse(title, text)
        dao.insertLog(
            WalletLog(
                time = postTime, title = title, text = text,
                result = parsed?.let { "נקלט: ${it.amountAgorot / 100.0} ${it.currency} · ${it.merchant}" }
                    ?: "לא זוהה סכום, דילגתי",
            ),
        )
        dao.trimLogs()
        if (parsed == null) return

        val type = if (parsed.isRefund) EntryType.INCOME else EntryType.EXPENSE
        val categoryId = when {
            parsed.isRefund -> CatIds.REFUND
            else -> dao.ruleFor(normalize(parsed.merchant))?.takeIf { dao.categoryExists(it) }
                ?: AutoCategorizer.guess(parsed.merchant)
                ?: CatIds.OTHER_EXPENSE
        }
        val entry = Entry(
            amount = parsed.amountAgorot,
            type = type,
            categoryId = categoryId,
            merchant = parsed.merchant,
            note = if (parsed.currency != "ILS") "סכום ב-${parsed.currency}" else "",
            timestamp = postTime,
            source = EntrySource.WALLET,
            account = "Google Wallet",
            // אותה התראה באותה דקה = אותו תשלום. מונע כפילויות כש-Wallet מעדכן התראה.
            externalId = "${postTime / 60_000}|$title|$text",
        )
        val id = dao.insertEntry(entry)
        if (id == -1L) return // כבר נרשם

        if (prefs.captureNotify) {
            Notifications.showCaptured(context, entry.copy(id = id), dao.category(categoryId))
        }
    }

    /** "שופרסל דיל " ו-"שופרסל דיל" הם אותו בית עסק */
    private fun normalize(merchant: String) = merchant.trim().lowercase()
}
