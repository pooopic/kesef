package com.edan.kesef.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EntryType { EXPENSE, INCOME }

/** מאיפה הגיעה התנועה: הוזנה ידנית, או נקלטה מהתראה של Google Wallet. */
enum class EntrySource { MANUAL, WALLET }

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    /** צבע בפורמט ARGB, למשל 0xFF34D399 */
    val color: Long,
    val type: EntryType,
    /** קטגוריות מובנות לא נמחקות, כי הסיווג האוטומטי נשען עליהן. */
    val builtIn: Boolean = false,
)

/** תנועה אחת: הוצאה או הכנסה. */
@Entity(
    tableName = "entries",
    indices = [
        Index("timestamp"),
        // externalId ייחודי: אם Wallet שולח את אותה התראה פעמיים, היא תירשם פעם אחת בלבד.
        Index(value = ["externalId"], unique = true),
    ],
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** תמיד חיובי, באגורות. הסוג (type) קובע אם זה פלוס או מינוס. */
    val amount: Long,
    val type: EntryType,
    val categoryId: Long,
    val note: String = "",
    /** שם בית העסק (מגיע מ-Wallet, או ריק בהזנה ידנית) */
    val merchant: String = "",
    /** זמן התנועה במילישניות מאז 1970 */
    val timestamp: Long,
    val source: EntrySource = EntrySource.MANUAL,
    val externalId: String? = null,
)

/**
 * "חוק" שהאפליקציה לומדת: כשמשנים קטגוריה של תשלום מ-Wallet,
 * בפעם הבאה אותו בית עסק יסווג אוטומטית לאותה קטגוריה.
 */
@Entity(tableName = "merchant_rules")
data class MerchantRule(
    @PrimaryKey val merchant: String,
    val categoryId: Long,
)

/** יומן של כל התראה שהגיעה מ-Wallet, כדי לראות מה נקלט ומה לא (ולתקן את הפענוח). */
@Entity(tableName = "wallet_log")
data class WalletLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val title: String,
    val text: String,
    val result: String,
)
