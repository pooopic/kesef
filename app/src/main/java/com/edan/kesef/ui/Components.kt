package com.edan.kesef.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edan.kesef.data.Category
import com.edan.kesef.data.Entry
import com.edan.kesef.data.EntrySource
import com.edan.kesef.data.EntryType
import com.edan.kesef.ui.theme.K
import com.edan.kesef.util.Money
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** רכיבים קטנים שחוזרים בכמה מסכים. */

val Hebrew: Locale = Locale.forLanguageTag("he")

fun Category.uiColor() = Color(color)

fun YearMonth.hebrewTitle(): String =
    "${month.getDisplayName(TextStyle.FULL_STANDALONE, Hebrew)} $year"

/** כרטיס מעוגל בצבע הכרטיסים של האפליקציה */
fun Modifier.card(radius: Int = 20) = this
    .clip(RoundedCornerShape(radius.dp))
    .background(K.Card)

@Composable
fun MonthSwitcher(month: YearMonth, onShift: (Long) -> Unit) {
    val isCurrent = month >= YearMonth.now()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "חודש קודם", tint = K.Muted)
        }
        Text(
            month.hebrewTitle(),
            Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = K.Text,
        )
        IconButton(onClick = { onShift(1) }, enabled = !isCurrent) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, "חודש הבא",
                tint = if (isCurrent) K.Track else K.Muted,
            )
        }
    }
}

/** בורר הוצאה / הכנסה */
@Composable
fun TypeToggle(type: EntryType, onChange: (EntryType) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(K.Card)
            .padding(4.dp),
    ) {
        listOf(EntryType.EXPENSE to "הוצאה", EntryType.INCOME to "הכנסה").forEach { (t, label) ->
            val selected = t == type
            val color = if (t == EntryType.EXPENSE) K.Expense else K.Income
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) color.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onChange(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected) color else K.Muted,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/** עיגול צבעוני עם אימוג'י של הקטגוריה */
@Composable
fun CategoryBadge(category: Category?, size: Int = 44) {
    val c = category?.uiColor() ?: K.Muted
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(c.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(category?.emoji ?: "❔", fontSize = (size * 0.45f).sp)
    }
}

private val rowTime = DateTimeFormatter.ofPattern("dd/MM · HH:mm")

/** שורה של תנועה ברשימה */
@Composable
fun EntryRow(entry: Entry, category: Category?, onClick: () -> Unit) {
    val title = entry.merchant.ifBlank { entry.note.ifBlank { category?.name ?: "" } }
    val time = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).format(rowTime)
    val subtitle = buildString {
        append(category?.name ?: "")
        append(" · ")
        append(time)
        // אמצעי התשלום (ויזה 3447 / בנק / Wallet), כדי להבחין בין המקורות
        when {
            entry.account.isNotBlank() -> append(" · ${entry.account}")
            entry.source == EntrySource.WALLET -> append(" · Wallet")
        }
    }
    val isIncome = entry.type == EntryType.INCOME
    Row(
        Modifier
            .fillMaxWidth()
            .card(18)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = K.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = K.Muted, fontSize = 13.sp, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            Money.display(if (isIncome) entry.amount else -entry.amount, showPlus = true),
            color = if (isIncome) K.Income else K.Text,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

/** כרטיס עם כותרת, למסך ההגדרות */
@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .card()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = K.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        content()
    }
}

@Composable
fun EmptyState(emoji: String, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.size(8.dp))
        Text(text, color = K.Muted, textAlign = TextAlign.Center)
    }
}
