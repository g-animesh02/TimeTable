package com.animesh.timetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.timetable.ui.MainViewModel
import com.animesh.timetable.ui.SubjectStat

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsState()
    val stats = vm.subjectStats(state)
    val totalPresent = stats.sumOf { it.present }
    val totalHeld = stats.sumOf { it.held }
    val overall = if (totalHeld == 0) 0 else (totalPresent * 100) / totalHeld

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("Attendance Stats") }) }) { padding ->
        if (stats.isEmpty()) {
            EmptyState("No attendance yet", "Mark Present/Absent in the Attendance tab to build your record.", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("Overall attendance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "$overall%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "$totalPresent of $totalHeld classes attended",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            items(stats, key = { it.subject }) { stat -> SubjectStatRow(stat) }
        }
    }
}

@Composable
private fun SubjectStatRow(stat: SubjectStat) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stat.subject, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${stat.percent}%",
                    fontWeight = FontWeight.Bold,
                    color = colorFor(stat.percent),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { stat.percent / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = colorFor(stat.percent),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Present ${stat.present} • Absent ${stat.absent} • Held ${stat.held}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun colorFor(percent: Int): Color = when {
    percent >= 75 -> Color(0xFF2E7D32)
    percent >= 60 -> Color(0xFFE5A33B)
    else -> Color(0xFFC62828)
}
