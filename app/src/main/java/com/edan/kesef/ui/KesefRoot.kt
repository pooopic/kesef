package com.edan.kesef.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edan.kesef.data.EntryType
import com.edan.kesef.ui.theme.K

/** הלשוניות בסרגל התחתון */
enum class Tab(val label: String) { HOME("בית"), STATS("פילוח"), SETTINGS("הגדרות") }

/** מסכים שנפתחים "מעל" הלשוניות */
sealed interface Overlay {
    data class Edit(val entryId: Long?, val type: EntryType = EntryType.EXPENSE) : Overlay
    data object Categories : Overlay
}

/**
 * השלד של האפליקציה: רקע, סרגל תחתון, ומעבר בין מסכים.
 * בחרנו ניווט פשוט עם משתני state במקום ספריית ניווט, כדי שיהיה קל לעקוב.
 */
@Composable
fun KesefRoot(entryToOpen: Long?, onEntryOpened: () -> Unit, vm: MainViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val context = LocalContext.current

    // נפתחנו מהתראה על תשלום? ישר למסך העריכה שלו
    LaunchedEffect(entryToOpen) {
        if (entryToOpen != null) {
            overlay = Overlay.Edit(entryToOpen)
            onEntryOpened()
        }
    }

    // בקשת הרשאת התראות (אנדרואיד 13+) ובדיקת עדכונים, פעם אחת בפתיחה
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.checkForUpdate()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(K.Bg2, K.Bg1))),
    ) {
        when (val o = overlay) {
            is Overlay.Edit -> {
                BackHandler { overlay = null }
                EditEntryScreen(vm, o.entryId, o.type, onClose = { overlay = null })
            }
            Overlay.Categories -> {
                BackHandler { overlay = null }
                CategoriesScreen(vm, onBack = { overlay = null })
            }
            null -> Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = K.Card) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = {
                                    Icon(
                                        when (t) {
                                            Tab.HOME -> Icons.Default.Home
                                            Tab.STATS -> Icons.AutoMirrored.Filled.List
                                            Tab.SETTINGS -> Icons.Default.Settings
                                        },
                                        contentDescription = t.label,
                                    )
                                },
                                label = { Text(t.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = K.Track,
                                    selectedIconColor = K.Income,
                                    selectedTextColor = K.Income,
                                ),
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (tab == Tab.HOME) {
                        ExtendedFloatingActionButton(
                            onClick = { overlay = Overlay.Edit(null) },
                            containerColor = K.Income,
                            contentColor = Color(0xFF052E1C),
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("תנועה חדשה") },
                        )
                    }
                },
            ) { padding ->
                when (tab) {
                    Tab.HOME -> HomeScreen(vm, padding, onOpenEntry = { overlay = Overlay.Edit(it) })
                    Tab.STATS -> StatsScreen(vm, padding)
                    Tab.SETTINGS -> SettingsScreen(vm, padding, onManageCategories = { overlay = Overlay.Categories })
                }
            }
        }
    }
}
