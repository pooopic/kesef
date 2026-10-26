package com.edan.kesef.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.edan.kesef.data.EntryType
import com.edan.kesef.importer.CsvImport
import com.edan.kesef.util.Money
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edan.kesef.BuildConfig
import com.edan.kesef.notify.Notifications
import com.edan.kesef.ui.theme.K
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(vm: MainViewModel, padding: PaddingValues, onManageCategories: () -> Unit) {
    val context = LocalContext.current
    val update by vm.update.collectAsStateWithLifecycle()
    val log by vm.walletLog.collectAsStateWithLifecycle()

    // כשחוזרים מהגדרות הטלפון, בודקים מחדש אם ההרשאות אושרו
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val listenerOn = remember(refresh) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    val notificationsOn = remember(refresh) { Notifications.canPost(context) }

    var reminderOn by remember { mutableStateOf(vm.prefs.reminderEnabled) }
    var hour by remember { mutableIntStateOf(vm.prefs.reminderHour) }
    var captureNotify by remember { mutableStateOf(vm.prefs.captureNotify) }
    var showLog by remember { mutableStateOf(false) }

    val importState by vm.importState.collectAsStateWithLifecycle()
    // בוחר הקבצים של אנדרואיד. מחזיר כתובת (Uri) של הקובץ שנבחר, או null אם ביטלת.
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.loadImportFile(it) }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("הגדרות", color = K.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        // ---------- Google Wallet ----------
        item {
            SectionCard("קליטה אוטומטית מ-Google Wallet") {
                StatusLine(listenerOn, "פעיל: תשלומים נקלטים אוטומטית", "כבוי: צריך לאשר גישה להתראות")
                if (!listenerOn) {
                    Hint(
                        "1. לחץ על הכפתור, מצא את \"כסף\" והפעל.\n" +
                            "2. אם האפשרות אפורה (\"הגדרה מוגבלת\"): לחץ \"פרטי האפליקציה\" → ⋮ למעלה → " +
                            "\"התר הגדרות מוגבלות\", ואז חזור לשלב 1.",
                    )
                    PrimaryButton("פתח גישה להתראות") {
                        context.startSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    OutlinedButton(onClick = { context.openAppDetails() }, modifier = Modifier.fillMaxWidth()) {
                        Text("פרטי האפליקציה")
                    }
                }
                SwitchRow("הודע לי על כל תשלום שנקלט", captureNotify) {
                    captureNotify = it
                    vm.prefs.captureNotify = it
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.simulateWalletPayment() }, modifier = Modifier.weight(1f)) {
                        Text("הדמיית תשלום")
                    }
                    OutlinedButton(onClick = { showLog = !showLog }, modifier = Modifier.weight(1f)) {
                        Text(if (showLog) "הסתר יומן" else "יומן Wallet")
                    }
                }
                Hint("\"הדמיית תשלום\" יוצרת תנועה לדוגמה (₪87.40 בשופרסל) כדי לבדוק שהכול עובד. אפשר למחוק אותה אחר כך.")
            }
        }
        if (showLog) {
            if (log.isEmpty()) item { Hint("עוד לא הגיעו התראות מ-Wallet.") }
            items(log, key = { it.id }) { l ->
                Column(Modifier.fillMaxWidth().card(14).padding(12.dp)) {
                    Text(
                        Instant.ofEpochMilli(l.time).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                        color = K.Muted, fontSize = 12.sp,
                    )
                    Text("${l.title}\n${l.text}", color = K.Text, fontSize = 14.sp)
                    Text(l.result, color = if (l.result.startsWith("נקלט")) K.Income else K.Expense, fontSize = 13.sp)
                }
            }
        }

        // ---------- התראות ותזכורות ----------
        item {
            SectionCard("התראות ותזכורות") {
                StatusLine(notificationsOn, "התראות מאושרות", "התראות חסומות")
                if (!notificationsOn) {
                    PrimaryButton("אשר התראות") {
                        context.startSafely(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }
                }
                SwitchRow("תזכורת יומית לעבור על ההוצאות", reminderOn) {
                    reminderOn = it
                    vm.setReminder(it, hour)
                }
                if (reminderOn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("שעת התזכורת", color = K.Muted, modifier = Modifier.weight(1f))
                        TextButton(onClick = { hour = (hour + 23) % 24; vm.setReminder(true, hour) }) { Text("−", fontSize = 22.sp) }
                        Text("%02d:00".format(hour), color = K.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { hour = (hour + 1) % 24; vm.setReminder(true, hour) }) { Text("+", fontSize = 22.sp) }
                    }
                }
            }
        }

        // ---------- ייבוא ----------
        item {
            SectionCard("ייבוא נתונים מקובץ") {
                Hint(
                    "קובץ CSV (באקסל: שמירה בשם ← CSV UTF-8) שהשורה הראשונה שלו היא:\n" +
                        CsvImport.HEADER.joinToString(",") +
                        "\nחובה: תאריך, סכום, קטגוריה. ההסבר המלא: docs/IMPORT_FORMAT.md ב-GitHub.",
                )
                when (val s = importState) {
                    ImportState.Idle -> PrimaryButton("בחר קובץ") { pickFile.launch(arrayOf("*/*")) }
                    ImportState.Importing -> Text("מייבא…", color = K.Muted)
                    is ImportState.Failed -> {
                        Text(s.message, color = K.Expense)
                        PrimaryButton("בחר קובץ אחר") { pickFile.launch(arrayOf("*/*")) }
                    }
                    is ImportState.Done -> {
                        Text(
                            "✅ נוספו ${s.added} תנועות" + if (s.skipped > 0) " (${s.skipped} כבר היו קיימות)" else "",
                            color = K.Income, fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedButton(onClick = { vm.cancelImport() }, modifier = Modifier.fillMaxWidth()) {
                            Text("סגור")
                        }
                    }
                    is ImportState.Preview -> ImportPreview(
                        s,
                        onConfirm = { vm.confirmImport(s.rows) },
                        onCancel = { vm.cancelImport() },
                    )
                }
            }
        }

        // ---------- קטגוריות ----------
        item {
            SectionCard("קטגוריות") {
                Hint("הוספת קטגוריות משלך להוצאות ולהכנסות.")
                OutlinedButton(onClick = onManageCategories, modifier = Modifier.fillMaxWidth()) {
                    Text("ניהול קטגוריות")
                }
            }
        }

        // ---------- עדכונים ----------
        item {
            SectionCard("גרסה ועדכונים") {
                Text("גרסה מותקנת: ${BuildConfig.VERSION_NAME}", color = K.Muted)
                when (val u = update) {
                    UpdateState.Idle -> {}
                    UpdateState.Checking -> Text("בודק…", color = K.Muted)
                    UpdateState.UpToDate -> Text("✅ יש לך את הגרסה האחרונה", color = K.Income)
                    is UpdateState.Error -> Text(u.message, color = K.Expense)
                    is UpdateState.Downloading -> {
                        Text("מוריד… ${(u.progress * 100).toInt()}%", color = K.Text)
                        LinearProgressIndicator(progress = { u.progress }, modifier = Modifier.fillMaxWidth(), color = K.Income)
                    }
                    is UpdateState.Available -> {
                        Text("✨ גרסה ${u.info.versionName} זמינה", color = K.Text, fontWeight = FontWeight.Bold)
                        if (u.info.notes.isNotBlank()) Text(u.info.notes, color = K.Muted, fontSize = 13.sp)
                        PrimaryButton("עדכן עכשיו") { vm.downloadAndInstall(u.info) }
                    }
                }
                if (update !is UpdateState.Available && update !is UpdateState.Downloading) {
                    OutlinedButton(onClick = { vm.checkForUpdate() }, modifier = Modifier.fillMaxWidth()) {
                        Text("בדוק עדכונים")
                    }
                }
            }
        }
    }
}

/** סיכום של מה שעומד להיכנס, לפני שמאשרים */
@Composable
private fun ImportPreview(p: ImportState.Preview, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val expenses = p.rows.filter { it.type == EntryType.EXPENSE }
    val income = p.rows.filter { it.type == EntryType.INCOME }
    val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    Text("נמצאו ${p.rows.size} תנועות", color = K.Text, fontWeight = FontWeight.Bold)
    if (p.rows.isNotEmpty()) {
        Text(
            "מ-${p.rows.minOf { it.date }.format(dateFmt)} עד ${p.rows.maxOf { it.date }.format(dateFmt)}\n" +
                "${expenses.size} הוצאות: ${Money.display(expenses.sumOf { it.amountAgorot })}\n" +
                "${income.size} הכנסות: ${Money.display(income.sumOf { it.amountAgorot })}",
            color = K.Muted, fontSize = 14.sp,
        )
        val accounts = p.rows.map { it.account }.filter { it.isNotBlank() }.distinct()
        if (accounts.isNotEmpty()) Text("אמצעי תשלום: ${accounts.joinToString()}", color = K.Muted, fontSize = 14.sp)
    }
    if (p.newCategories.isNotEmpty()) {
        Text("קטגוריות חדשות שייווצרו: ${p.newCategories.joinToString()}", color = K.Accent, fontSize = 14.sp)
    }
    if (p.errors.isNotEmpty()) {
        Text(
            "${p.errors.size} שורות עם בעיה (לא ייובאו):\n" + p.errors.take(5).joinToString("\n") +
                if (p.errors.size > 5) "\n…" else "",
            color = K.Expense, fontSize = 13.sp,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("ביטול") }
        Button(
            onClick = onConfirm,
            enabled = p.rows.isNotEmpty(),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = K.Income, contentColor = Color(0xFF052E1C)),
        ) { Text("ייבא", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun StatusLine(ok: Boolean, okText: String, badText: String) {
    Text(
        if (ok) "✅  $okText" else "⚠️  $badText",
        color = if (ok) K.Income else Color(0xFFFBBF24),
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun Hint(text: String) = Text(text, color = K.Muted, fontSize = 13.sp)

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = K.Text, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = K.Income),
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = K.Income, contentColor = Color(0xFF052E1C)),
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: Exception) {
        openAppDetails() // יש יצרנים שאין אצלם את המסך המבוקש
    }
}

private fun Context.openAppDetails() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
}
