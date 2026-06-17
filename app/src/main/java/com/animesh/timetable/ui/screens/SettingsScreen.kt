package com.animesh.timetable.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.timetable.ui.MainViewModel
import com.animesh.timetable.ui.components.SubjectRow

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsState()

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Subjects (${state.selectedSubjects.size} selected)",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Tap to add or remove subjects from your timetable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
            }
            items(state.allSubjects, key = { it }) { subject ->
                SubjectRow(
                    subject = subject,
                    selected = subject in state.selectedSubjects,
                    meetings = vm.offeringsForSubject(subject),
                    onToggle = { vm.toggleSubject(subject) }
                )
                HorizontalDivider()
            }
            item {
                Column(Modifier.padding(16.dp)) {
                    OutlinedButton(onClick = { vm.resetConfig() }) {
                        Text("Reset selection")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Source: final_TT_Mod_5.xlsx",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
