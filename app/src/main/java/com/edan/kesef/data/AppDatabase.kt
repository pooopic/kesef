package com.edan.kesef.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Category::class, Entry::class, MerchantRule::class, WalletLog::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): KesefDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * "הגירה" (Migration): כשמשנים את מבנה הטבלאות, צריך להסביר ל-Room איך לעדכן
         * את מסד הנתונים שכבר קיים בטלפון, בלי למחוק את הנתונים.
         * גרסה 2: נוספה עמודת "אמצעי תשלום" (account).
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN account TEXT NOT NULL DEFAULT ''")
            }
        }

        /** גרסה 3: סט קטגוריות חדש וכללי יותר, ועמודת סדר תצוגה (sort). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN sort INTEGER NOT NULL DEFAULT 999")
                writeDefaultCategories(db)
            }
        }

        // מסד נתונים אחד לכל האפליקציה (Singleton). פתיחה כפולה גורמת לבעיות.
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "kesef.db")
                .addCallback(SeedCallback)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }

        /**
         * כותב את הקטגוריות המובנות: מוסיף חסרות, ומעדכן שם/אימוג'י/צבע/סדר של קיימות לפי המזהה.
         * תנועות לא זזות: הן מצביעות על המזהה, והמזהה נשאר.
         */
        private fun writeDefaultCategories(db: SupportSQLiteDatabase) {
            DefaultCategories.all.forEachIndexed { index, c ->
                val values = ContentValues().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("emoji", c.emoji)
                    put("color", c.color)
                    put("type", c.type.name)
                    put("builtIn", 1)
                    put("sort", index)
                }
                db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
                db.update("categories", SQLiteDatabase.CONFLICT_IGNORE, values, "id = ?", arrayOf(c.id))
            }
        }
    }

    /** רץ פעם אחת בלבד, כשמסד הנתונים נוצר: מכניס את הקטגוריות המובנות. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) = writeDefaultCategories(db)
    }
}
