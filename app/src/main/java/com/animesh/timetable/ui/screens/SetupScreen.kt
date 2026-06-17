package com.animesh.timetable.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.timetable.ui.MainViewModel
import com.animesh.timetable.ui.components.SectionSelector
import com.animesh.timetable.ui.components.SubjectRow

@Composable
fun SetupScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    val subjects = state.allSubjects.filter { it.contains(query, ignoreCase = true) }

    Scaffold(
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = { vm.completeSetup() },
                    enabled = state.selectedSubjects.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue · ${state.selectedSubjects.size} selected") }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Text("Set up ${state.activeModuleName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Pick your section, then the subjects you're enrolled in.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Text("Section", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    SectionSelector(current = state.section, onSelect = { vm.setSection(it) })
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Search subjects") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
