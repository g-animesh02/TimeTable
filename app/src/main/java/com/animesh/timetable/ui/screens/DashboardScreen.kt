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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.animesh.timetable.ui.theme.Bad
import com.animesh.timetable.ui.theme.Good
import com.animesh.timetable.ui.theme.Warn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TODAY_FMT = DateTimeFormatter.ofPattern("EEEE, dd MMM")

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: MainViewModel, modifier: Modifier = Modifier, onSeeAll: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val stats = vm.subjectStats(state)
    val totalPresent = stats.sumOf { it.present }
    val totalHeld = stats.sumOf { it.held }
    val overall = if (totalHeld == 0) 0 else (totalPresent * 100) / totalHeld
    val today = vm.todayEntries(state)

    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(state.activeModuleName) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Overall attendance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("$overall%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("$totalPresent of $totalHeld classes attended", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text("Sec ${state.section}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Today · ${LocalDate.now().format(TODAY_FMT)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onSeeAll) { Text("Mark") }
                }
            }
            if (today.isEmpty()) {
                item { Text("No classes today 🎉", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(today, key = { "t-${it.entry.key}" }) { de ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(de.entry.start, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 12.dp))
                            Text(de.entry.subject, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            val (label, color) = when (de.status?.name) {
                                "PRESENT" -> "P" to Good
                                "ABSENT" -> "A" to Bad
                                "CANCELLED" -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
                                else -> "·" to MaterialTheme.colorScheme.outlineVariant
                            }
                            Box(Modifier.size(26.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                                Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("By subject", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (stats.isEmpty()) {
                item { Text("Mark attendance to see your stats build up here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(stats, key = { it.subject }) { stat -> SubjectStatRow(stat) }
            }
        }
    }
}

@Composable
private fun SubjectStatRow(stat: SubjectStat) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stat.subject, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                Text("${stat.percent}%", fontWeight = FontWeight.Bold, color = colorFor(stat.percent), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { stat.percent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = colorFor(stat.percent), trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text("Present ${stat.present}  ·  Absent ${stat.absent}  ·  Held ${stat.held}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun colorFor(percent: Int): Color = when {
    percent >= 75 -> Good
    percent >= 60 -> Warn
    else -> Bad
}
