package com.edan.kesef

import com.edan.kesef.data.EntryType
import com.edan.kesef.importer.CsvImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvImportTest {

    private val header = "תאריך,סכום,סוג,קטגוריה,תיאור,אמצעי תשלום,הערה\n"

    @Test fun basicRows() {
        val f = CsvImport.parse(
            "﻿" + header +
                "29/12/2025,208.23,הוצאה,סופר,CARREFOUR מזכרת בתיה,ויזה 3447,\n" +
                "10/01/2026,8500,הכנסה,משכורת,משכורת ינואר,בנק,\n",
        )
        assertTrue(f.errors.isEmpty())
        assertEquals(2, f.rows.size)
        val r = f.rows[0]
        assertEquals(LocalDate.of(2025, 12, 29), r.date)
        assertEquals(20823L, r.amountAgorot)
        assertEquals(EntryType.EXPENSE, r.type)
        assertEquals("ויזה 3447", r.account)
        assertEquals(EntryType.INCOME, f.rows[1].type)
    }

    @Test fun quotedCommaInCategory() {
        val f = CsvImport.parse(header + "01/02/2026,50,,\"פנאי, בילוי ותיירות\",\"מקום \"\"מיוחד\"\"\",,\n")
        assertEquals("פנאי, בילוי ותיירות", f.rows[0].category)
        assertEquals("מקום \"מיוחד\"", f.rows[0].description)
        assertEquals(EntryType.EXPENSE, f.rows[0].type) // סוג ריק = הוצאה
    }

    @Test fun negativeExpenseBecomesRefund() {
        val f = CsvImport.parse(header + "08/12/2025,-150,הוצאה,תחבורה ודלק,פנגו,ויזה 3447,\n")
        val r = f.rows[0]
        assertEquals(EntryType.INCOME, r.type)
        assertEquals(15000L, r.amountAgorot)
        assertEquals(CsvImport.REFUND_CATEGORY, r.category)
    }

    @Test fun semicolonAndColumnOrder() {
        val f = CsvImport.parse("קטגוריה;סכום;תאריך\nסופר;45,90;2026-03-01\n")
        assertEquals(4590L, f.rows[0].amountAgorot)
        assertEquals(LocalDate.of(2026, 3, 1), f.rows[0].date)
    }

    @Test fun errorsAreReported() {
        val f = CsvImport.parse(header + "32/13/2026,10,,סופר,,,\n01/01/2026,abc,,סופר,,,\n01/01/2026,10,משהו,סופר,,,\n")
        assertEquals(0, f.rows.size)
        assertEquals(3, f.errors.size)
    }

    @Test fun missingColumns() {
        val f = CsvImport.parse("date,amount\n01/01/2026,10\n")
        assertTrue(f.errors.first().contains("חסרות עמודות"))
    }

    @Test fun windows1255Fallback() {
        val bytes = "תאריך,סכום,קטגוריה\n".toByteArray(charset("windows-1255"))
        assertEquals("תאריך,סכום,קטגוריה\n", CsvImport.decode(bytes))
    }
}
