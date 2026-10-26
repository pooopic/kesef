package com.edan.kesef

import com.edan.kesef.data.CatIds
import com.edan.kesef.util.Money
import com.edan.kesef.wallet.AutoCategorizer
import com.edan.kesef.wallet.WalletParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * בדיקות שרצות ב-GitHub Actions לפני כל בנייה. אם בדיקה נכשלת, לא תצא גרסה.
 * כשמגיע פורמט חדש של התראה מ-Wallet: מעתיקים אותו מ"יומן Wallet" לבדיקה חדשה כאן.
 */
class WalletParserTest {

    @Test fun hebrewTitleIsMerchant() {
        val p = WalletParser.parse("שופרסל דיל", "‏₪87.40 עם Visa •••• 4242")!!
        assertEquals(8740, p.amountAgorot)
        assertEquals("שופרסל דיל", p.merchant)
        assertEquals("ILS", p.currency)
        assertFalse(p.isRefund)
    }

    @Test fun englishWithCardTail() {
        val p = WalletParser.parse("Shufersal", "₪1,234.50 with Mastercard •••• 1234")!!
        assertEquals(123450, p.amountAgorot)
        assertEquals("Shufersal", p.merchant)
    }

    @Test fun amountInTitleWithAt() {
        val p = WalletParser.parse("₪23.50 at Aroma Espresso Bar", "Visa •••• 4242")!!
        assertEquals(2350, p.amountAgorot)
        assertEquals("Aroma Espresso Bar", p.merchant)
    }

    @Test fun shekelSymbolAfterNumber() {
        val p = WalletParser.parse("Google Wallet", "שילמת 45.90 ₪ ב-רמי לוי")!!
        assertEquals(4590, p.amountAgorot)
        assertEquals("רמי לוי", p.merchant)
    }

    @Test fun foreignCurrency() {
        val p = WalletParser.parse("Amazon", "$12.99 with Visa")!!
        assertEquals(1299, p.amountAgorot)
        assertEquals("USD", p.currency)
    }

    @Test fun refund() {
        val p = WalletParser.parse("Zara", "Refund of ₪199.90")!!
        assertTrue(p.isRefund)
    }

    @Test fun noAmountIsIgnored() {
        assertNull(WalletParser.parse("Google Wallet", "Your card is ready to use"))
        assertNull(WalletParser.parse("כרטיס נוסף", "Visa •••• 4242 מוכן לשימוש"))
    }

    @Test fun moneyParse() {
        assertEquals(4590L, Money.parse("45.90"))
        assertEquals(4590L, Money.parse("45,90"))
        assertEquals(123400L, Money.parse("1,234"))
        assertEquals(123450L, Money.parse("1.234,50"))
        assertEquals(5000L, Money.parse("50"))
        assertNull(Money.parse("abc"))
    }

    @Test fun moneyFormat() {
        assertEquals("₪45.90", Money.format(4590))
        assertEquals("₪1,200", Money.format(120000))
        assertEquals("-₪5", Money.format(-500))
    }

    @Test fun categorizer() {
        assertEquals(CatIds.GROCERIES, AutoCategorizer.guess("שופרסל דיל"))
        assertEquals(CatIds.HEALTH, AutoCategorizer.guess("Super-Pharm Dizengoff"))
        assertEquals(CatIds.FOOD_OUT, AutoCategorizer.guess("Aroma Espresso Bar"))
        assertNull(AutoCategorizer.guess("חנות לא מוכרת"))
    }
}
