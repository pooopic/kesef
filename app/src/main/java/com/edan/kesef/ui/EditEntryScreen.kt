package com.edan.kesef.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edan.kesef.data.Entry
import com.edan.kesef.data.EntrySource
import com.edan.kesef.data.EntryType
import com.edan.kesef.ui.theme.K
import com.edan.kesef.util.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** מסך הוספה / עריכה של תנועה. entryId = null → תנועה חדשה. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(vm: MainViewModel, entryId: Long?, initialType: EntryType, onClose: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()

    var original by remember { mutableStateOf<Entry?>(null) }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // עריכה של תנועה קיימת: טוענים את הנתונים שלה לשדות
    LaunchedEffect(entryId) {
        if (entryId == null) return@LaunchedEffect
        vm.loadEntry(entryId)?.let { e ->
            original = e
            amountText = Money.toEditable(e.amount)
            type = e.type
            categoryId = e.categoryId
            note = e.note
            date = Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate()
        }
    }

    val typeCategories = categories.filter { it.type == type }
    // אם החלפת סוג (הוצאה↔הכנסה), הקטגוריה הקודמת כבר לא רלוונטית
    val selectedId = typeCategories.firstOrNull { it.id == categoryId }?.id

    fun save() {
        val amount = Money.parse(amountText)
        if (amount == null || amount <= 0) {
            error = "צריך להכניס סכום"
            return
        }
        val catId = selectedId ?: run {
            error = "בחר קטגוריה"
            return
        }
        val old = original
        // שומרים על השעה המקורית; לתנועה חדשה של היום לוקחים את השעה הנוכחית
        val time: LocalDateTime = when {
            old != null -> date.atTime(Instant.ofEpochMilli(old.timestamp).atZone(zone).toLocalTime())
            date == LocalDate.now() -> LocalDateTime.now()
            else -> date.atTime(12, 0)
        }
        val base = old ?: Entry(amount = 0, type = type, categoryId = catId, timestamp = 0)
        vm.save(
            base.copy(
                amount = amount,
                type = type,
                categoryId = catId,
                note = note.trim(),
                timestamp = time.atZone(zone).toInstant().toEpochMilli(),
            ),
        )
        onClose()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "סגור", tint = K.Muted) }
            Text(
                if (entryId == null) "תנועה חדשה" else "עריכת תנועה",
                Modifier.weight(1f),
                color = K.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            )
            if (entryId != null) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "מחק", tint = K.Expense) }
            }
        }

        TypeToggle(type, { type = it })

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            prefix = { Text("₪ ", fontSize = 32.sp, color = K.Muted) },
            placeholder = { Text("0", fontSize = 40.sp, color = K.Track) },
            textStyle = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, color = K.Text),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
        )

        original?.takeIf { it.source == EntrySource.WALLET }?.let {
            Text(
                "נקלט מ-Google Wallet · ${it.merchant}\nשינוי קטגוריה ייזכר לפעם הבאה בעסק הזה.",
                color = K.Muted, fontSize = 13.sp,
            )
        }

        Text("קטגוריה", color = K.Muted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            typeCategories.forEach { c ->
                val selected = c.id == selectedId
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) c.uiColor().copy(alpha = 0.25f) else K.Card)
                        .border(1.dp, if (selected) c.uiColor() else Color.Transparent, RoundedCornerShape(50))
                        .clickable { categoryId = c.id; error = null }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(c.emoji)
                    Spacer(Modifier.width(6.dp))
                    Text(c.name, color = if (selected) K.Text else K.Muted)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .card(14)
                .clickable { showDatePicker = true }
                .padding(16.dp),
        ) {
            Text("📅  תאריך", color = K.Muted, modifier = Modifier.weight(1f))
            Text(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), color = K.Text)
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("הערה (לא חובה)") },
        )

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = ::save,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = K.Income, contentColor = Color(0xFF052E1C)),
        ) {
            Text("שמירה", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showDatePicker) {
        // DatePicker עובד בזמן UTC, לכן ממירים דרך UTC ולא דרך אזור הזמן המקומי
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("אישור") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("ביטול") } },
        ) {
            DatePicker(state)
        }
    }

    if (confirmDelete && entryId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("למחוק את התנועה?") },
            text = { Text("אי אפשר לבטל את המחיקה.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(entryId)
                    confirmDelete = false
                    onClose()
                }) { Text("מחק", color = K.Expense) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("ביטול") } },
        )
    }
}
