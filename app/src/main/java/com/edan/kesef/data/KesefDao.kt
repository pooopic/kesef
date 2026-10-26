package com.edan.kesef.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO = כל השאילתות למסד הנתונים במקום אחד.
 * פונקציה שמחזירה Flow "מאזינה": כל שינוי בטבלה מעדכן את המסך אוטומטית.
 */
@Dao
interface KesefDao {

    // ---------- קטגוריות ----------

    @Query("SELECT * FROM categories ORDER BY type, builtIn DESC, id")
    fun categories(): Flow<List<Category>>

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE id = :id)")
    suspend fun categoryExists(id: Long): Boolean

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun category(id: Long): Category?

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)

    /** לפני מחיקת קטגוריה מעבירים את התנועות שלה ל"אחר", כדי שלא יתייתמו. */
    @Query("UPDATE entries SET categoryId = :to WHERE categoryId = :from")
    suspend fun moveEntries(from: Long, to: Long)

    // ---------- תנועות ----------

    @Query("SELECT * FROM entries WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun entriesBetween(from: Long, to: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE timestamp >= :from AND timestamp < :to")
    suspend fun entriesBetweenOnce(from: Long, to: Long): List<Entry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun entry(id: Long): Entry?

    /** IGNORE: אם externalId כבר קיים (התראה כפולה), לא מכניס ומחזיר 1-. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: Entry): Long

    @Update
    suspend fun updateEntry(entry: Entry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    // ---------- חוקי בתי עסק ----------

    @Query("SELECT categoryId FROM merchant_rules WHERE merchant = :merchant")
    suspend fun ruleFor(merchant: String): Long?

    @Upsert
    suspend fun upsertRule(rule: MerchantRule)

    // ---------- יומן Wallet ----------

    @Insert
    suspend fun insertLog(log: WalletLog)

    @Query("SELECT * FROM wallet_log ORDER BY time DESC LIMIT 30")
    fun recentLogs(): Flow<List<WalletLog>>

    /** שומרים רק 100 רשומות אחרונות ביומן */
    @Query("DELETE FROM wallet_log WHERE id NOT IN (SELECT id FROM wallet_log ORDER BY time DESC LIMIT 100)")
    suspend fun trimLogs()
}
