package com.animesh.timetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.timetable.ui.MainViewModel
import com.animesh.timetable.ui.components.BatchSelector
import com.animesh.timetable.ui.components.SubjectRow

@Composable
fun SetupScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                Button(
                    onClick = { vm.completeSetup() },
                    enabled = state.selectedSubjects.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue (${state.selectedSubjects.size} selected)")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Set up your timetable",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Module 5 • Pick your batch, then choose the subjects you're enrolled in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Your batch", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    BatchSelector(batch = state.batch, onSelect = vm::setBatch)
                    Spacer(Modifier.height(16.dp))
                    Text("Subjects", fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider()
            }
            items(state.allSubjects, key = { it }) { subject ->
                SubjectRow(
                    subject = subject,
                    selected = subject in state.selectedSubjects,
                    meetings = vm.offeringsForSubject(subject).filter { it.batch.isBlank() || it.batch == state.batch },
                    onToggle = { vm.toggleSubject(subject) }
                )
                HorizontalDivider()
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
