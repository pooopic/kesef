package com.edan.kesef.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edan.kesef.data.Category
import com.edan.kesef.data.DefaultCategories
import com.edan.kesef.data.EntryType
import com.edan.kesef.ui.theme.K

/** ניהול קטגוריות: הוספה של קטגוריות משלך ומחיקה שלהן */
@Composable
fun CategoriesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Category?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה", tint = K.Muted) }
            Text("קטגוריות", Modifier.weight(1f), color = K.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "הוסף", tint = K.Income) }
        }
        LazyColumn(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(EntryType.EXPENSE to "הוצאות", EntryType.INCOME to "הכנסות").forEach { (type, title) ->
                item {
                    Text(title, color = K.Muted, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(categories.filter { it.type == type }, key = { it.id }) { c ->
                    Row(
                        Modifier.fillMaxWidth().card(16).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryBadge(c, size = 38)
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, color = K.Text, modifier = Modifier.weight(1f))
                        if (!c.builtIn) {
                            IconButton(onClick = { toDelete = c }) {
                                Icon(Icons.Default.Delete, "מחק", tint = K.Muted)
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        AddCategoryDialog(
            onDismiss = { adding = false },
            onAdd = { name, emoji, color, type ->
                vm.addCategory(name, emoji, color, type)
                adding = false
            },
        )
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("למחוק את \"${c.name}\"?") },
            text = { Text("התנועות שבקטגוריה יעברו ל\"אחר\".") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCategory(c); toDelete = null }) { Text("מחק", color = K.Expense) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("ביטול") } },
        )
    }
}

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onAdd: (String, String, Long, EntryType) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("⭐") }
    var type by remember { mutableStateOf(EntryType.EXPENSE) }
    var color by remember { mutableStateOf(DefaultCategories.palette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = K.Bg2,
        title = { Text("קטגוריה חדשה") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeToggle(type, { type = it })
                OutlinedTextField(name, { name = it }, label = { Text("שם") }, singleLine = true)
                OutlinedTextField(emoji, { emoji = it.take(2) }, label = { Text("אימוג'י") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DefaultCategories.palette.forEach { c ->
                        Spacer(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(2.dp, if (c == color) K.Text else Color.Transparent, CircleShape)
                                .clickable { color = c },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name.trim(), emoji.ifBlank { "⭐" }, color, type) },
                enabled = name.isNotBlank(),
            ) { Text("הוסף") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )
}
