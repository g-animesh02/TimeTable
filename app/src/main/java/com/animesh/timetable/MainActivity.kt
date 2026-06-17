package com.animesh.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animesh.timetable.ui.MainViewModel
import com.animesh.timetable.ui.screens.AttendanceScreen
import com.animesh.timetable.ui.screens.DashboardScreen
import com.animesh.timetable.ui.screens.ModulesScreen
import com.animesh.timetable.ui.screens.ScheduleScreen
import com.animesh.timetable.ui.screens.SetupScreen
import com.animesh.timetable.ui.screens.SettingsScreen
import com.animesh.timetable.ui.theme.TimeTableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Dashboard("Home", Icons.Filled.SpaceDashboard),
    Schedule("Schedule", Icons.AutoMirrored.Filled.ViewList),
    Attendance("Attendance", Icons.Filled.CalendarMonth),
    Modules("Modules", Icons.Filled.GridView),
    Settings("Settings", Icons.Filled.Tune)
}

@Composable
private fun AppRoot(vm: MainViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    TimeTableTheme(themeMode = state.themeMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                state.loading -> Unit
                state.needsSetup -> SetupScreen(vm)
                else -> MainScaffold(vm)
            }
        }
    }
}

@Composable
private fun MainScaffold(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        val mod = Modifier.padding(padding)
        when (Tab.entries[tab]) {
            Tab.Dashboard -> DashboardScreen(vm, mod, onSeeAll = { tab = 2 })
            Tab.Schedule -> ScheduleScreen(vm, mod)
            Tab.Attendance -> AttendanceScreen(vm, mod)
            Tab.Modules -> ModulesScreen(vm, mod)
            Tab.Settings -> SettingsScreen(vm, mod)
        }
    }
}
