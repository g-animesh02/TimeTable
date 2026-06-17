package com.animesh.timetable.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.timetable.data.model.ScheduleEntry
import com.animesh.timetable.ui.MainViewModel
import com.animesh.timetable.ui.export.ImageExporter
import com.animesh.timetable.ui.theme.DayColors
import com.animesh.timetable.ui.theme.colorForKey
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
                            val uri = ImageExporter.save(context, bmp, "Timetable")
                            if (uri != null) {
                                Toast.makeText(context, "Saved to Pictures/TimeTable", Toast.LENGTH_SHORT).show()
                                context.startActivity(ImageExporter.shareIntent(context, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } else Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    icon = { Icon(Icons.Filled.IosShare, contentDescription = null) },
                    text = { Text("Export") }
                )
            }
        }
    ) { padding ->
        if (schedule.isEmpty()) {
            EmptyState("No classes yet", "Pick the subjects you're enrolled in from Settings or the Modules tab.", Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                }.background(MaterialTheme.colorScheme.background)
            ) {
                TimetableSheet(state.activeModuleName, state.section, schedule)
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun TimetableSheet(moduleName: String, section: String, schedule: Map<String, List<ScheduleEntry>>) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(20.dp)) {
        Text(moduleName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Section $section", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        DAY_ORDER.forEachIndexed { idx, day ->
            val entries = schedule[day].orEmpty()
            if (entries.isEmpty()) return@forEachIndexed
            DaySection(day, DayColors[idx % DayColors.size], entries)
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DaySection(day: String, accent: Color, entries: List<ScheduleEntry>) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(day.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Column {
        entries.forEach { e -> ClassRow(e, accent) }
    }
}

@Composable
private fun ClassRow(e: ScheduleEntry, accent: Color) {
    // Color-coding applies to electives (no section): same prof -> same color, same subject -> same color.
    val coded = e.section.isBlank() && !e.isExtra
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val subjectColor = if (coded) colorForKey("subject:${e.subject}", dark) else MaterialTheme.colorScheme.onSurface
    val profColor = if (coded) colorForKey("prof:${e.faculty}", dark) else MaterialTheme.colorScheme.onSurfaceVariant
    val roomColor = if (coded) subjectColor else MaterialTheme.colorScheme.onSurfaceVariant
    val marker = if (coded) subjectColor else accent.copy(alpha = 0.5f)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.width(58.dp)) {
            Text(e.start, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(e.end, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.width(3.dp).height(40.dp).background(marker))
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                e.subject + if (e.isExtra) "  ·  extra" else "",
                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge,
                color = subjectColor
            )
            Row {
                if (e.room.isNotBlank()) {
                    Text("Room ${e.room}", style = MaterialTheme.typography.bodySmall, color = roomColor, fontWeight = FontWeight.Medium)
                }
                if (e.faculty.isNotBlank()) {
                    if (e.room.isNotBlank()) Text("  ·  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(e.faculty, style = MaterialTheme.typography.bodySmall, color = profColor, fontWeight = if (coded) FontWeight.Medium else FontWeight.Normal)
                }
            }
        }
    }
}
