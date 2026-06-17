package com.animesh.timetable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.animesh.timetable.data.local.AttendanceStatus
import com.animesh.timetable.data.model.ScheduleEntry
import androidx.compose.ui.graphics.luminance
import com.animesh.timetable.ui.DatedEntry
import com.animesh.timetable.ui.MainViewModel
import com.animesh.timetable.ui.theme.colorForKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DAY_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM")
private val RANGE_FMT = DateTimeFormatter.ofPattern("dd MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state by vm.uiState.collectAsState()
    val weekStart by vm.weekStart.collectAsState()
    val days = vm.weekSchedule(state, weekStart)

    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Pair<ScheduleEntry, LocalDate>?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Attendance")
                        Text(
                            "${weekStart.format(RANGE_FMT)} – ${weekStart.plusDays(5).format(RANGE_FMT)}",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.prevWeek() }) { Icon(Icons.Filled.ChevronLeft, "Previous week") }
                    TextButton(onClick = { vm.thisWeek() }) { Text("Today") }
                    IconButton(onClick = { vm.nextWeek() }) { Icon(Icons.Filled.ChevronRight, "Next week") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Add class") }
        }
    ) { padding ->
        if (days.all { it.entries.isEmpty() }) {
            EmptyState("Nothing scheduled", "Pick subjects in Settings, or tap + to add a class.", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            days.forEach { day ->
                if (day.entries.isEmpty()) return@forEach
                item(key = "h-${day.day}") {
                    Text(
                        day.date.format(DAY_FMT), style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 4.dp)
                    )
                }
                items(day.entries, key = { "${it.entry.key}-${it.date}" }) { de ->
                    AttendanceCard(
                        de = de,
                        onMark = { status -> if (de.status == status) vm.clearMark(de.entry, de.date) else vm.mark(de.entry, de.date, status) },
                        onEdit = { editTarget = de.entry to de.date }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddClassDialog(
            weekStart = weekStart,
            onDismiss = { showAdd = false },
            onAdd = { day, date, start, end, subject, faculty, room ->
                vm.addExtraClass(day, date, start, end, subject, faculty, room); showAdd = false
            }
        )
    }

    editTarget?.let { (entry, date) ->
        EditClassDialog(
            entry = entry,
            onDismiss = { editTarget = null },
            onReschedule = { s, e, r -> vm.rescheduleForDate(entry, date, s, e, r); editTarget = null },
            onCancelDay = { vm.cancelForDate(entry, date); editTarget = null },
            onClearOverride = { vm.clearOverride(entry, date); editTarget = null },
            onDeleteExtra = { vm.deleteCustomClass(entry.key); editTarget = null }
        )
    }
}

@Composable
private fun AttendanceCard(de: DatedEntry, onMark: (AttendanceStatus) -> Unit, onEdit: () -> Unit) {
    val e = de.entry
    val coded = e.section.isBlank() && !e.isExtra
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val subjectColor = if (coded) colorForKey("subject:${e.subject}", dark) else MaterialTheme.colorScheme.onSurface
    val profColor = if (coded) colorForKey("prof:${e.faculty}", dark) else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        e.subject + if (e.isExtra) "  ·  extra" else "",
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge,
                        color = subjectColor,
                        textDecoration = if (e.isCancelled) TextDecoration.LineThrough else null
                    )
                    val detail = buildString {
                        append(e.timeRange)
                        if (e.room.isNotBlank()) append("  ·  Room ${e.room}")
                        if (e.isModified) append("  ·  changed")
                    }
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = if (coded) subjectColor else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (e.faculty.isNotBlank()) Text(e.faculty, style = MaterialTheme.typography.bodySmall, color = profColor, fontWeight = if (coded) FontWeight.Medium else FontWeight.Normal)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit class") }
            }
            if (!e.isCancelled) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("Present", de.status == AttendanceStatus.PRESENT) { onMark(AttendanceStatus.PRESENT) }
                    StatusChip("Absent", de.status == AttendanceStatus.ABSENT) { onMark(AttendanceStatus.ABSENT) }
                    StatusChip("Off", de.status == AttendanceStatus.CANCELLED) { onMark(AttendanceStatus.CANCELLED) }
                }
            } else {
                Text("Cancelled for this day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddClassDialog(
    weekStart: LocalDate,
    onDismiss: () -> Unit,
    onAdd: (day: String, date: LocalDate, start: String, end: String, subject: String, faculty: String, room: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("09:30") }
    var end by remember { mutableStateOf("11:00") }
    var room by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var dayIndex by remember { mutableStateOf(0) }
    var dayMenu by remember { mutableStateOf(false) }
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a class") },
        text = {
            Column {
                OutlinedTextField(subject, { subject = it }, label = { Text("Subject *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = dayMenu, onExpandedChange = { dayMenu = it }) {
                    OutlinedTextField(
                        value = "${dayNames[dayIndex]} (${weekStart.plusDays(dayIndex.toLong()).format(RANGE_FMT)})",
                        onValueChange = {}, readOnly = true, label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                        dayNames.forEachIndexed { i, name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { dayIndex = i; dayMenu = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(start, { start = it }, label = { Text("Start HH:mm") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(end, { end = it }, label = { Text("End HH:mm") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(room, { room = it }, label = { Text("Room") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(faculty, { faculty = it }, label = { Text("Faculty") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("Added to this date only.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = subject.isNotBlank() && isTime(start) && isTime(end),
                onClick = { onAdd(dayNames[dayIndex], weekStart.plusDays(dayIndex.toLong()), start, end, subject.trim(), faculty.trim(), room.trim()) }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditClassDialog(
    entry: ScheduleEntry,
    onDismiss: () -> Unit,
    onReschedule: (start: String?, end: String?, room: String?) -> Unit,
    onCancelDay: () -> Unit,
    onClearOverride: () -> Unit,
    onDeleteExtra: () -> Unit
) {
    var start by remember { mutableStateOf(entry.start) }
    var end by remember { mutableStateOf(entry.end) }
    var room by remember { mutableStateOf(entry.room) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${entry.subject}") },
        text = {
            Column {
                Text("Change timing or room for this day:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(start, { start = it }, label = { Text("Start") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(end, { end = it }, label = { Text("End") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(room, { room = it }, label = { Text("Room") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                if (entry.isExtra) {
                    TextButton(onClick = onDeleteExtra) { Text("Delete this extra class") }
                } else {
                    TextButton(onClick = onCancelDay) { Text("Cancel class for this day") }
                    TextButton(onClick = onClearOverride) { Text("Reset to original") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = isTime(start) && isTime(end), onClick = { onReschedule(start, end, room) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun isTime(s: String): Boolean = Regex("^\\d{1,2}:\\d{2}$").matches(s.trim())
