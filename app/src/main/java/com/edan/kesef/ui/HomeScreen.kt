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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.time.YearMonth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edan.kesef.data.EntryType
import com.edan.kesef.ui.theme.K
import com.edan.kesef.util.Money

@Composable
fun HomeScreen(vm: MainViewModel, padding: PaddingValues, onOpenEntry: (Long) -> Unit) {
    val month by vm.month.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()

    val all by vm.allEntries.collectAsStateWithLifecycle()
    val opening by vm.openingBalance.collectAsStateWithLifecycle()

    val catById = remember(categories) { categories.associateBy { it.id } }
    // מצטבר מכל הזמנים
    val totalIncome = remember(all) { all.filter { it.type == EntryType.INCOME }.sumOf { it.amount } }
    val totalExpenses = remember(all) { all.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount } }
    // החודש הנוכחי (בשביל הטבעת), בלי קשר לחודש שבחרת ברשימה
    val thisMonth = remember(all) {
        val now = YearMonth.now()
        all.filter { YearMonth.from(it.localDate()) == now }
    }
    // החודש שנבחר ברשימת התנועות
    val income = remember(entries) { entries.filter { it.type == EntryType.INCOME }.sumOf { it.amount } }
    val expenses = remember(entries) { entries.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount } }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 96.dp, // מקום לכפתור הצף
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                BalanceRing(
                    balance = opening + totalIncome - totalExpenses,
                    monthIncome = thisMonth.filter { it.type == EntryType.INCOME }.sumOf { it.amount },
                    monthExpenses = thisMonth.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount },
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill("הכנסות מצטברות", totalIncome, K.Income, Modifier.weight(1f))
                StatPill("הוצאות מצטברות", totalExpenses, K.Expense, Modifier.weight(1f))
            }
        }

        (update as? UpdateState.Available)?.let { available ->
            item { UpdateBanner(available.info.versionName) { vm.downloadAndInstall(available.info) } }
        }
        (update as? UpdateState.Downloading)?.let { d ->
            item { UpdateBanner("מוריד… ${(d.progress * 100).toInt()}%") {} }
        }

        // ---------- תנועות לפי חודש ----------
        item { Spacer(Modifier.size(10.dp)) }
        item { MonthSwitcher(month, vm::shiftMonth) }
        item {
            Text(
                "הכנסות ${Money.display(income)} · הוצאות ${Money.display(expenses)} · " +
                    "מאזן ${Money.display(income - expenses)}",
                color = K.Muted, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        if (entries.isEmpty()) {
            item { EmptyState("🌱", "אין תנועות בחודש הזה.\nתשלומים מ-Google Wallet ייכנסו לכאן אוטומטית.") }
        }

        items(entries, key = { it.id }) { e ->
            EntryRow(e, catById[e.categoryId]) { onOpenEntry(e.id) }
        }
    }
}

@Composable
private fun StatPill(label: String, amount: Long, color: Color, modifier: Modifier) {
    Column(modifier.card().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, color = K.Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.size(4.dp))
        Text(Money.display(amount), color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UpdateBanner(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .card()
            .background(K.Accent.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✨", fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("גרסה חדשה זמינה", color = K.Text, fontWeight = FontWeight.Bold)
            Text("$text · לחץ לעדכון", color = K.Muted, fontSize = 13.sp)
        }
    }
}
