package com.edan.kesef.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Category::class, Entry::class, MerchantRule::class, WalletLog::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): KesefDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // מסד נתונים אחד לכל האפליקציה (Singleton). פתיחה כפולה גורמת לבעיות.
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "kesef.db")
                .addCallback(SeedCallback)
                .build()
                .also { instance = it }
        }
    }

    /** רץ פעם אחת בלבד, כשמסד הנתונים נוצר: מכניס את הקטגוריות המובנות. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            DefaultCategories.all.forEach { c ->
                val values = ContentValues().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("emoji", c.emoji)
                    put("color", c.color)
                    put("type", c.type.name)
                    put("builtIn", if (c.builtIn) 1 else 0)
                }
                db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, values)
            }
        }
    }
}
