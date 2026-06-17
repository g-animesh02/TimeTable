package com.animesh.timetable.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.timetable.data.model.ScheduleEntry
import com.animesh.timetable.ui.MainViewModel
import com.animesh.timetable.ui.theme.DayColors
import kotlinx.coroutines.launch

private val DAY_ORDER = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val schedule = state.weeklySchedule

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Weekly Timetable") }) },
        floatingActionButton = {
            if (schedule.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            val bmp = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            val uri = com.animesh.timetable.ui.export.ImageExporter.save(
                                context, bmp, "Timetable"
                            )
                            if (uri != null) {
                                Toast.makeText(context, "Saved to Pictures/TimeTable", Toast.LENGTH_SHORT).show()
                                context.startActivity(
                                    com.animesh.timetable.ui.export.ImageExporter.shareIntent(context, uri)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } else {
                                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.IosShare, contentDescription = null) },
                    text = { Text("Export image") }
                )
            }
        }
    ) { padding ->
        if (schedule.isEmpty()) {
            EmptyState(
                "No classes yet",
                "Go to Settings and pick the subjects you're enrolled in.",
                Modifier.padding(padding)
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    }
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TimetableSheet(schedule)
            }
            Spacer(Modifier.height(88.dp))
        }
    }
}

/** The full self-contained timetable rendered for both on-screen display and image export. */
@Composable
private fun TimetableSheet(schedule: Map<String, List<ScheduleEntry>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            "My Timetable — Module 5",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))

        DAY_ORDER.forEachIndexed { idx, day ->
            val entries = schedule[day].orEmpty()
            if (entries.isEmpty()) return@forEachIndexed
            DayCard(day, DayColors[idx % DayColors.size], entries)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DayCard(day: String, accent: Color, entries: List<ScheduleEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(day, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("${entries.size} class${if (entries.size == 1) "" else "es"}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
        }
        entries.forEach { e ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Column(Modifier.width(86.dp)) {
                    Text(e.start, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(e.end, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    Modifier
                        .width(3.dp)
                        .height(38.dp)
                        .background(accent, RoundedCornerShape(2.dp))
                )
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        e.subject + if (e.isExtra) "  (extra)" else "",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    val detail = buildString {
                        if (e.room.isNotBlank()) append("Room ${e.room}")
                        if (e.faculty.isNotBlank()) { if (isNotEmpty()) append("  •  "); append(e.faculty) }
                    }
                    if (detail.isNotBlank()) {
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
