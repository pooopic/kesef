package com.edan.kesef.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edan.kesef.data.CatIds
import com.edan.kesef.data.Category
import com.edan.kesef.data.Entry
import com.edan.kesef.data.EntryType
import com.edan.kesef.ui.theme.K
import com.edan.kesef.util.Money
import java.time.YearMonth
import kotlin.math.roundToInt

/** על איזו תקופה מסתכלים */
enum class Period(val label: String) { MONTH("חודש"), YEAR("שנה"), ALL("כל הזמנים") }

/**
 * מסך הסיכומים, בנוי לפי גיליון "סיכום" של עידן:
 * תמונת מצב, הוצאות לפי קטגוריה, הכנסות לפי סוג ולפי מקור, תשלומים קבועים, ומאזן חודשי.
 * הכל מחושב כאן מתוך כל התנועות; אין שום דבר שמור בנפרד.
 */
@Composable
fun StatsScreen(vm: MainViewModel, padding: PaddingValues) {
    val all by vm.allEntries.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val opening by vm.openingBalance.collectAsStateWithLifecycle()

    var period by remember { mutableStateOf(Period.MONTH) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var year by remember { mutableIntStateOf(YearMonth.now().year) }

    val catById = remember(categories) { categories.associateBy { it.id } }
    val entries = remember(all, period, month, year) {
        when (period) {
            Period.MONTH -> all.filter { YearMonth.from(it.localDate()) == month }
            Period.YEAR -> all.filter { it.localDate().year == year }
            Period.ALL -> all
        }
    }
    val income = entries.filter { it.type == EntryType.INCOME }
    val expenses = entries.filter { it.type == EntryType.EXPENSE }
    val totalIncome = income.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("סיכומים", color = K.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        item { PeriodPicker(period) { period = it } }
        item {
            when (period) {
                Period.MONTH -> MonthSwitcher(month) { month = month.plusMonths(it) }
                Period.YEAR -> Stepper("$year", canNext = year < YearMonth.now().year) { year += it }
                Period.ALL -> {}
            }
        }

        // ---------- תמונת מצב ----------
        item {
            SectionCard("תמונת מצב") {
                SummaryLine("סה\"כ הכנסות", totalIncome, K.Income)
                SummaryLine("סה\"כ הוצאות", totalExpenses, K.Expense)
                SummaryLine("מאזן (הכנסות פחות הוצאות)", totalIncome - totalExpenses, null, bold = true)
                if (period == Period.ALL) {
                    SummaryLine("יתרת פתיחה בחשבון", opening, null)
                    SummaryLine("יתרה בחשבון עכשיו", opening + totalIncome - totalExpenses, null, bold = true)
                }
            }
        }

        // ---------- הוצאות לפי קטגוריה ----------
        item {
            SectionCard("הוצאות לפי קטגוריה") {
                Breakdown(expenses.groupBy { it.categoryId }.map { (id, l) -> catById[id] to l.sumOf { it.amount } }, totalExpenses)
            }
        }

        // ---------- הכנסות ----------
        item {
            SectionCard("הכנסות לפי סוג") {
                Breakdown(income.groupBy { it.categoryId }.map { (id, l) -> catById[id] to l.sumOf { it.amount } }, totalIncome)
            }
        }
        item {
            SectionCard("הכנסות לפי מקור") {
                NameList(income.groupBy { it.merchant.ifBlank { "ללא מקור" } }.mapValues { (_, l) -> l.sumOf { it.amount } })
            }
        }

        // ---------- תשלומים קבועים ----------
        item {
            val fixed = expenses.filter { it.categoryId == CatIds.HOUSING || it.categoryId == CatIds.SUBSCRIPTIONS }
            SectionCard("דיור, מנויים וחשבונות") {
                Hint("תשלומים קבועים: כמה יצא על כל אחד בתקופה, ובכמה חיובים.")
                NameList(
                    fixed.groupBy { it.merchant.ifBlank { it.note.ifBlank { "ללא תיאור" } } }
                        .mapValues { (_, l) -> l.sumOf { it.amount } },
                    counts = fixed.groupBy { it.merchant.ifBlank { it.note.ifBlank { "ללא תיאור" } } }
                        .mapValues { it.value.size },
                )
            }
        }

        // ---------- מאזן חודשי ----------
        if (period != Period.MONTH) {
            item {
                SectionCard("מאזן חודשי") { MonthlyTable(entries) }
            }
        }
    }
}

@Composable
private fun PeriodPicker(selected: Period, onChange: (Period) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(K.Card).padding(4.dp),
    ) {
        Period.entries.forEach { p ->
            val on = p == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) K.Accent.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onChange(p) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(p.label, color = if (on) K.Accent else K.Muted, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/** חצים + כותרת, כמו MonthSwitcher אבל לשנה */
@Composable
private fun Stepper(title: String, canNext: Boolean, onShift: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "הקודם", tint = K.Muted)
        }
        Text(title, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = K.Text)
        IconButton(onClick = { onShift(1) }, enabled = canNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "הבא", tint = if (canNext) K.Muted else K.Track)
        }
    }
}

@Composable
private fun SummaryLine(label: String, amount: Long, color: Color?, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = K.Muted, modifier = Modifier.weight(1f))
        Text(
            Money.display(amount),
            color = color ?: if (amount < 0) K.Expense else K.Text,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = if (bold) 18.sp else 16.sp,
        )
    }
}

@Composable
private fun Hint(text: String) = Text(text, color = K.Muted, fontSize = 13.sp)

/** פסים לפי קטגוריה: סכום ואחוז מהסך הכל */
@Composable
private fun Breakdown(rows: List<Pair<Category?, Long>>, total: Long) {
    if (rows.isEmpty()) {
        Hint("אין נתונים בתקופה הזו")
        return
    }
    rows.sortedByDescending { it.second }.forEach { (cat, sum) ->
        val fraction = if (total > 0) sum.toFloat() / total else 0f
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(cat, size = 30)
                Spacer(Modifier.width(8.dp))
                Text(cat?.name ?: "ללא קטגוריה", color = K.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(Money.display(sum), color = K.Text, fontWeight = FontWeight.SemiBold)
                Text("${(fraction * 100).roundToInt()}%", color = K.Muted, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(K.Track)) {
                Box(
                    Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(6.dp)
                        .clip(RoundedCornerShape(50)).background(cat?.uiColor() ?: K.Muted),
                )
            }
        }
    }
}

/** רשימת שמות עם סכומים, מהגדול לקטן (מקורות הכנסה / תשלומים קבועים) */
@Composable
private fun NameList(sums: Map<String, Long>, counts: Map<String, Int> = emptyMap()) {
    if (sums.isEmpty()) {
        Hint("אין נתונים בתקופה הזו")
        return
    }
    sums.entries.sortedByDescending { it.value }.forEach { (name, sum) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = K.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            counts[name]?.let { Text("×$it  ", color = K.Muted, fontSize = 12.sp) }
            Text(Money.display(sum), color = K.Text, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** טבלה: לכל חודש הכנסות, הוצאות ומאזן. החודש האחרון למעלה. */
@Composable
private fun MonthlyTable(entries: List<Entry>) {
    val byMonth = entries.groupBy { YearMonth.from(it.localDate()) }.toSortedMap(compareByDescending { it })
    if (byMonth.isEmpty()) {
        Hint("אין נתונים בתקופה הזו")
        return
    }
    Row {
        listOf("חודש", "הכנסות", "הוצאות", "מאזן").forEach {
            Text(it, color = K.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        }
    }
    byMonth.forEach { (m, list) ->
        val inc = list.filter { it.type == EntryType.INCOME }.sumOf { it.amount }
        val exp = list.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
        Row {
            Text("%02d/%d".format(m.monthValue, m.year), color = K.Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(Money.display(inc), color = K.Income, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(Money.display(exp), color = K.Expense, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                Money.display(inc - exp),
                color = if (inc - exp < 0) K.Expense else K.Text,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
