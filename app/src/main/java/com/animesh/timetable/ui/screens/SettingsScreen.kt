package com.animesh.timetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.timetable.data.local.ThemeMode
import com.animesh.timetable.ui.MainViewModel
import com.animesh.timetable.ui.components.Pill
import com.animesh.timetable.ui.components.SectionSelector
import com.animesh.timetable.ui.components.SubjectRow

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    val subjects = state.allSubjects.filter { it.contains(query, ignoreCase = true) }

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.padding(20.dp)) {
                    SettingLabel("Appearance")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("System", state.themeMode == ThemeMode.SYSTEM) { vm.setThemeMode(ThemeMode.SYSTEM) }
                        Pill("Light", state.themeMode == ThemeMode.LIGHT) { vm.setThemeMode(ThemeMode.LIGHT) }
                        Pill("Dark", state.themeMode == ThemeMode.DARK) { vm.setThemeMode(ThemeMode.DARK) }
                    }
                    Spacer(Modifier.height(24.dp))
                    SettingLabel("Section")
                    Spacer(Modifier.height(10.dp))
                    SectionSelector(current = state.section, onSelect = { vm.setSection(it) })
                    Spacer(Modifier.height(24.dp))
                    SettingLabel("Subjects · ${state.selectedSubjects.size} selected")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Search subjects") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            items(subjects, key = { it }) { subject ->
                SubjectRow(
                    subject = subject,
                    selected = subject in state.selectedSubjects,
                    meetings = vm.meetingsFor(subject),
                    onToggle = { vm.toggleSubject(subject) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Active module: ${state.activeModuleName}",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
}
