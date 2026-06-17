package com.animesh.timetable.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animesh.timetable.data.local.AttendanceEntity
import com.animesh.timetable.data.local.AttendanceStatus
import com.animesh.timetable.data.local.CustomClassEntity
import com.animesh.timetable.data.local.DayOverrideEntity
import com.animesh.timetable.data.model.ClassOffering
import com.animesh.timetable.data.model.ScheduleEntry
import com.animesh.timetable.data.repo.TimetableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DAY_ORDER = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/** Resolved class for a specific calendar date, including any attendance status. */
data class DatedEntry(
    val entry: ScheduleEntry,
    val date: LocalDate,
    val status: AttendanceStatus?
)

data class DaySchedule(
    val day: String,
    val date: LocalDate,
    val entries: List<DatedEntry>
)

data class SubjectStat(
    val subject: String,
    val present: Int,
    val absent: Int
) {
    val held: Int get() = present + absent
    val percent: Int get() = if (held == 0) 0 else (present * 100) / held
}

data class UiState(
    val loading: Boolean = true,
    val setupDone: Boolean = false,
    val batch: String = "A",
    val allSubjects: List<String> = emptyList(),
    val selectedSubjects: Set<String> = emptySet(),
    val offerings: List<ClassOffering> = emptyList(),
    val customClasses: List<CustomClassEntity> = emptyList(),
    val overrides: List<DayOverrideEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList()
) {
    /** The recurring weekly schedule (no dates) grouped & sorted by day then time. */
    val weeklySchedule: Map<String, List<ScheduleEntry>>
        get() {
            val byBatch = offerings.filter {
                it.subject in selectedSubjects && (it.batch.isBlank() || it.batch == batch)
            }.map { o ->
                ScheduleEntry(
                    key = "off-${o.id}", day = o.day, start = o.start, end = o.end,
                    subject = o.subject, faculty = o.faculty, room = o.room,
                    slot = o.slot, batch = o.batch, isExtra = false
                )
            }
            val recurringCustom = customClasses.filter { it.date == null }.map { c ->
                ScheduleEntry(
                    key = "custom-${c.id}", day = c.day, start = c.start, end = c.end,
                    subject = c.subject, faculty = c.faculty, room = c.room,
                    slot = "", batch = "", isExtra = true
                )
            }
            return (byBatch + recurringCustom)
                .groupBy { it.day }
                .mapValues { (_, list) -> list.sortedBy { toMinutes(it.start) } }
        }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TimetableRepository(app)

    private val _loading = MutableStateFlow(true)
    private val _offerings = MutableStateFlow<List<ClassOffering>>(emptyList())
    private val _allSubjects = MutableStateFlow<List<String>>(emptyList())

    /** Anchor date for the week navigator (current week shows by default). */
    val weekStart = MutableStateFlow(mondayOf(LocalDate.now()))

    val uiState: StateFlow<UiState> = combine(
        combine(repo.settings.setupDone, repo.settings.batch, repo.settings.selectedSubjects) { s, b, subj -> Triple(s, b, subj) },
        combine(repo.observeCustomClasses(), repo.observeOverrides(), repo.observeAttendance()) { c, o, a -> Triple(c, o, a) },
        _loading, _offerings, _allSubjects
    ) { settings, data, loading, offerings, allSubjects ->
        val (setupDone, batch, subjects) = settings
        val (customs, overrides, attendance) = data
        UiState(
            loading = loading,
            setupDone = setupDone,
            batch = batch,
            allSubjects = allSubjects,
            selectedSubjects = subjects,
            offerings = offerings,
            customClasses = customs,
            overrides = overrides,
            attendance = attendance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    init {
        viewModelScope.launch {
            val master = repo.loadMaster()
            _offerings.value = master.offerings
            _allSubjects.value = master.offerings.map { it.subject }.distinct().sorted()
            _loading.value = false
        }
    }

    // ---- Configuration ----
    fun toggleSubject(subject: String) = viewModelScope.launch {
        val current = uiState.value.selectedSubjects.toMutableSet()
        if (!current.add(subject)) current.remove(subject)
        repo.settings.setSubjects(current)
    }

    fun setBatch(batch: String) = viewModelScope.launch { repo.settings.setBatch(batch) }

    fun completeSetup() = viewModelScope.launch { repo.settings.setSetupDone(true) }

    fun resetConfig() = viewModelScope.launch {
        repo.settings.setSubjects(emptySet())
        repo.settings.setSetupDone(false)
    }

    fun offeringsForSubject(subject: String): List<ClassOffering> =
        uiState.value.offerings.filter { it.subject == subject }

    // ---- Week navigation ----
    fun nextWeek() { weekStart.value = weekStart.value.plusWeeks(1) }
    fun prevWeek() { weekStart.value = weekStart.value.minusWeeks(1) }
    fun thisWeek() { weekStart.value = mondayOf(LocalDate.now()) }

    /** Builds the dated schedule for the week beginning at [start], applying overrides & one-off classes. */
    fun weekSchedule(state: UiState, start: LocalDate): List<DaySchedule> {
        val recurring = state.weeklySchedule
        val attByKeyDate = state.attendance.associateBy { it.entryKey to it.date }
        val overrideByKeyDate = state.overrides.associateBy { it.entryKey to it.date }

        return DAY_ORDER.mapIndexed { idx, day ->
            val date = start.plusDays(idx.toLong())
            val iso = date.format(ISO)
            val base = recurring[day].orEmpty().toMutableList()

            // One-off custom classes scheduled on this exact date.
            val oneOff = state.customClasses.filter { it.date == iso }.map { c ->
                ScheduleEntry(
                    key = "custom-${c.id}", day = day, start = c.start, end = c.end,
                    subject = c.subject, faculty = c.faculty, room = c.room,
                    slot = "", batch = "", isExtra = true
                )
            }
            val entries = (base + oneOff).map { e ->
                val ov = overrideByKeyDate[e.key to iso]
                val resolved = if (ov != null) e.copy(
                    start = ov.newStart ?: e.start,
                    end = ov.newEnd ?: e.end,
                    room = ov.newRoom ?: e.room,
                    isModified = ov.newStart != null || ov.newEnd != null || ov.newRoom != null,
                    isCancelled = ov.cancelled
                ) else e
                DatedEntry(resolved, date, attByKeyDate[e.key to iso]?.status)
            }.sortedBy { toMinutes(it.entry.start) }

            DaySchedule(day, date, entries)
        }
    }

    // ---- Attendance ----
    fun mark(entry: ScheduleEntry, date: LocalDate, status: AttendanceStatus) = viewModelScope.launch {
        repo.setAttendance(entry.key, date.format(ISO), entry.subject, status)
    }

    fun clearMark(entry: ScheduleEntry, date: LocalDate) = viewModelScope.launch {
        repo.clearAttendance(entry.key, date.format(ISO))
    }

    fun subjectStats(state: UiState): List<SubjectStat> {
        return state.attendance
            .filter { it.status != AttendanceStatus.CANCELLED }
            .groupBy { it.subject }
            .map { (subject, records) ->
                SubjectStat(
                    subject = subject,
                    present = records.count { it.status == AttendanceStatus.PRESENT },
                    absent = records.count { it.status == AttendanceStatus.ABSENT }
                )
            }.sortedBy { it.subject }
    }

    // ---- Per-day edits ----
    fun addExtraClass(
        day: String, date: LocalDate?, start: String, end: String,
        subject: String, faculty: String, room: String
    ) = viewModelScope.launch {
        repo.addCustomClass(
            CustomClassEntity(
                day = day, start = start, end = end, subject = subject,
                faculty = faculty, room = room, date = date?.format(ISO)
            )
        )
    }

    fun deleteCustomClass(id: Long) = viewModelScope.launch { repo.deleteCustomClass(id) }

    fun rescheduleForDate(
        entry: ScheduleEntry, date: LocalDate,
        newStart: String?, newEnd: String?, newRoom: String?
    ) = viewModelScope.launch {
        val iso = date.format(ISO)
        repo.upsertOverride(
            DayOverrideEntity(
                id = "${entry.key}@$iso", entryKey = entry.key, date = iso,
                newStart = newStart, newEnd = newEnd, newRoom = newRoom, cancelled = false
            )
        )
    }

    fun cancelForDate(entry: ScheduleEntry, date: LocalDate) = viewModelScope.launch {
        val iso = date.format(ISO)
        repo.upsertOverride(
            DayOverrideEntity(id = "${entry.key}@$iso", entryKey = entry.key, date = iso, cancelled = true)
        )
    }

    fun clearOverride(entry: ScheduleEntry, date: LocalDate) = viewModelScope.launch {
        repo.deleteOverride("${entry.key}@${date.format(ISO)}")
    }

    companion object {
        fun mondayOf(d: LocalDate): LocalDate = d.with(DayOfWeek.MONDAY)
        fun toMinutes(t: String): Int {
            val parts = t.split(":")
            return if (parts.size == 2) (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) else 0
        }
    }
}

fun toMinutes(t: String): Int = MainViewModel.toMinutes(t)
