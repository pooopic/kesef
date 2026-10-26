package com.edan.kesef.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edan.kesef.data.Category
import com.edan.kesef.data.EntryType
import com.edan.kesef.ui.theme.K
import com.edan.kesef.util.Money
import kotlin.math.roundToInt

/** פילוח החודש לפי קטגוריות: לאן הלך הכסף ומאיפה הוא הגיע */
@Composable
fun StatsScreen(vm: MainViewModel, padding: PaddingValues) {
    val month by vm.month.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    var type by remember { mutableStateOf(EntryType.EXPENSE) }

    // סכום לכל קטגוריה, מהגדולה לקטנה
    val rows: List<Pair<Category?, Long>> = remember(entries, categories, type) {
        val byId = categories.associateBy { it.id }
        entries.filter { it.type == type }
            .groupBy { it.categoryId }
            .map { (id, list) -> byId[id] to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }
    val total = rows.sumOf { it.second }
    val color = if (type == EntryType.EXPENSE) K.Expense else K.Income

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { MonthSwitcher(month, vm::shiftMonth) }
        item { TypeToggle(type, { type = it }) }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (type == EntryType.EXPENSE) "סה\"כ הוצאות" else "סה\"כ הכנסות", color = K.Muted)
                Text(Money.display(total), color = color, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (rows.isEmpty()) {
            item { EmptyState("📊", "אין עדיין נתונים לחודש הזה") }
        }
        items(rows) { (cat, sum) ->
            CategoryBar(cat, sum, if (total > 0) sum.toFloat() / total else 0f)
        }
    }
}

@Composable
private fun CategoryBar(category: Category?, sum: Long, fraction: Float) {
    val barColor = category?.uiColor() ?: K.Muted
    Column(Modifier.fillMaxWidth().card(18).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(category, size = 36)
            Spacer(Modifier.width(10.dp))
            Text(category?.name ?: "ללא קטגוריה", color = K.Text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(Money.display(sum), color = K.Text, fontWeight = FontWeight.Bold)
            Text(
                "${(fraction * 100).roundToInt()}%",
                color = K.Muted, fontSize = 13.sp, textAlign = TextAlign.End,
                modifier = Modifier.width(44.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        // פס התקדמות: אחוז הקטגוריה מתוך הסך הכל
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(K.Track)) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor),
            )
        }
    }
}
