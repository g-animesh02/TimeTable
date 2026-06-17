package com.animesh.timetable.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animesh.timetable.data.local.AttendanceEntity
import com.animesh.timetable.data.local.AttendanceStatus
import com.animesh.timetable.data.local.CustomClassEntity
import com.animesh.timetable.data.local.DayOverrideEntity
import com.animesh.timetable.data.local.ModuleEntity
import com.animesh.timetable.data.local.OfferingEntity
import com.animesh.timetable.data.local.ThemeMode
import com.animesh.timetable.data.model.ScheduleEntry
import com.animesh.timetable.data.repo.TimetableRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DAY_ORDER = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

data class DatedEntry(val entry: ScheduleEntry, val date: LocalDate, val status: AttendanceStatus?)
data class DaySchedule(val day: String, val date: LocalDate, val entries: List<DatedEntry>)

data class SubjectStat(val subject: String, val present: Int, val absent: Int) {
    val held: Int get() = present + absent
    val percent: Int get() = if (held == 0) 0 else (present * 100) / held
}

private data class Scoped(
    val offerings: List<OfferingEntity> = emptyList(),
    val selected: Set<String> = emptySet(),
    val customClasses: List<CustomClassEntity> = emptyList(),
    val overrides: List<DayOverrideEntity> = emptyList()
)

data class UiState(
    val loading: Boolean = true,
    val needsSetup: Boolean = false,
    val modules: List<ModuleEntity> = emptyList(),
    val activeModuleId: Long = -1,
    val section: String = "F",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedSubjects: Set<String> = emptySet(),
    val offerings: List<OfferingEntity> = emptyList(),
    val customClasses: List<CustomClassEntity> = emptyList(),
    val overrides: List<DayOverrideEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList()
) {
    val activeModuleName: String get() = modules.firstOrNull { it.id == activeModuleId }?.name ?: "Timetable"

    /** Offerings relevant to the chosen section (electives + this section's core classes). */
    private val sectionOfferings: List<OfferingEntity>
        get() = offerings.filter { it.section.isBlank() || it.section == section }

    val allSubjects: List<String>
        get() = sectionOfferings.map { it.subject }.distinct().sorted()

    fun meetingsFor(subject: String): List<OfferingEntity> =
        sectionOfferings.filter { it.subject == subject }

    val weeklySchedule: Map<String, List<ScheduleEntry>>
        get() {
            val selectedEntries = sectionOfferings.filter { it.subject in selectedSubjects }.map { o ->
                ScheduleEntry(
                    key = "off-${o.id}", day = o.day, start = o.start, end = o.end,
                    subject = o.subject, faculty = o.faculty, room = o.room,
                    slot = o.slot, section = o.section, isExtra = false
                )
            }
            val recurringCustom = customClasses.filter { it.date == null }.map { c ->
                ScheduleEntry(
                    key = "custom-${c.id}", day = c.day, start = c.start, end = c.end,
                    subject = c.subject, faculty = c.faculty, room = c.room,
                    slot = "", section = "", isExtra = true
                )
            }
            return (selectedEntries + recurringCustom)
                .groupBy { it.day }
                .mapValues { (_, list) -> list.sortedBy { toMinutes(it.start) } }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TimetableRepository(app)
    val settings = repo.settings

    private val _loading = MutableStateFlow(true)
    val weekStart = MutableStateFlow(mondayOf(LocalDate.now()))

    private val scopedFlow = settings.activeModuleId.flatMapLatest { id ->
        if (id < 0) flowOf(Scoped())
        else combine(
            repo.offeringDao.observeForModule(id),
            repo.selectedSubjectDao.observeForModule(id),
            repo.customClassDao.observeForModule(id),
            repo.dayOverrideDao.observeForModule(id)
        ) { off, sel, cust, ovr -> Scoped(off, sel.toSet(), cust, ovr) }
    }

    val uiState: StateFlow<UiState> = combine(
        combine(repo.moduleDao.observeAll(), settings.activeModuleId, settings.section, settings.configuredModules) {
                modules, active, section, configured -> ModuleInfo(modules, active, section, configured)
        },
        combine(settings.themeMode, repo.attendanceDao.observeAll(), _loading) { theme, att, loading ->
            ThemeInfo(theme, att, loading)
        },
        scopedFlow
    ) { mi, ti, scoped ->
        val moduleAttendance = ti.attendance.filter { it.moduleId == mi.active }
        val configured = mi.active >= 0 && mi.active.toString() in mi.configured
        UiState(
            loading = ti.loading,
            needsSetup = !ti.loading && mi.active >= 0 && !configured,
            modules = mi.modules,
            activeModuleId = mi.active,
            section = mi.section,
            themeMode = ti.theme,
            selectedSubjects = scoped.selected,
            offerings = scoped.offerings,
            customClasses = scoped.customClasses,
            overrides = scoped.overrides,
            attendance = moduleAttendance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    private data class ModuleInfo(val modules: List<ModuleEntity>, val active: Long, val section: String, val configured: Set<String>)
    private data class ThemeInfo(val theme: ThemeMode, val attendance: List<AttendanceEntity>, val loading: Boolean)

    init {
        viewModelScope.launch {
            val id = repo.ensureSeeded()
            if (settings.activeModuleId.first() < 0) settings.setActiveModule(id)
            _loading.value = false
        }
    }

    // ---- Config ----
    fun toggleSubject(subject: String) = viewModelScope.launch {
        val s = uiState.value
        repo.toggleSubject(s.activeModuleId, subject, subject !in s.selectedSubjects)
    }

    fun setSection(section: String) = viewModelScope.launch { settings.setSection(section) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun completeSetup() = viewModelScope.launch { settings.markConfigured(uiState.value.activeModuleId) }
    fun reconfigure() = viewModelScope.launch { settings.unmarkConfigured(uiState.value.activeModuleId) }

    fun meetingsFor(subject: String): List<OfferingEntity> = uiState.value.meetingsFor(subject)

    // ---- Modules ----
    fun selectModule(id: Long) = viewModelScope.launch { settings.setActiveModule(id) }

    fun importModule(uri: Uri, name: String, onResult: (Result<Unit>) -> Unit) = viewModelScope.launch {
        runCatching {
            val id = repo.importModule(uri, name.ifBlank { "New Module" })
            settings.setActiveModule(id)
        }.onSuccess { onResult(Result.success(Unit)) }
            .onFailure { onResult(Result.failure(it)) }
    }

    fun deleteModule(id: Long) = viewModelScope.launch {
        val remaining = uiState.value.modules.filter { it.id != id }
        if (uiState.value.activeModuleId == id) remaining.firstOrNull()?.let { settings.setActiveModule(it.id) }
        repo.deleteModule(id)
        settings.unmarkConfigured(id)
    }

    // ---- Week navigation ----
    fun nextWeek() { weekStart.value = weekStart.value.plusWeeks(1) }
    fun prevWeek() { weekStart.value = weekStart.value.minusWeeks(1) }
    fun thisWeek() { weekStart.value = mondayOf(LocalDate.now()) }

    fun weekSchedule(state: UiState, start: LocalDate): List<DaySchedule> {
        val recurring = state.weeklySchedule
        val attByKeyDate = state.attendance.associateBy { it.entryKey to it.date }
        val overrideByKeyDate = state.overrides.associateBy { it.entryKey to it.date }
        return DAY_ORDER.mapIndexed { idx, day ->
            val date = start.plusDays(idx.toLong())
            val iso = date.format(ISO)
            val base = recurring[day].orEmpty()
            val oneOff = state.customClasses.filter { it.date == iso }.map { c ->
                ScheduleEntry("custom-${c.id}", day, c.start, c.end, c.subject, c.faculty, c.room, "", "", isExtra = true)
            }
            val entries = (base + oneOff).map { e ->
                val ov = overrideByKeyDate[e.key to iso]
                val resolved = if (ov != null) e.copy(
                    start = ov.newStart ?: e.start, end = ov.newEnd ?: e.end, room = ov.newRoom ?: e.room,
                    isModified = ov.newStart != null || ov.newEnd != null || ov.newRoom != null,
                    isCancelled = ov.cancelled
                ) else e
                DatedEntry(resolved, date, attByKeyDate[e.key to iso]?.status)
            }.sortedBy { toMinutes(it.entry.start) }
            DaySchedule(day, date, entries)
        }
    }

    fun todayEntries(state: UiState): List<DatedEntry> {
        val today = LocalDate.now()
        return weekSchedule(state, mondayOf(today)).firstOrNull { it.date == today }?.entries.orEmpty()
    }

    // ---- Attendance ----
    fun mark(entry: ScheduleEntry, date: LocalDate, status: AttendanceStatus) = viewModelScope.launch {
        repo.setAttendance(uiState.value.activeModuleId, entry.key, date.format(ISO), entry.subject, status)
    }

    fun clearMark(entry: ScheduleEntry, date: LocalDate) = viewModelScope.launch {
        repo.clearAttendance(uiState.value.activeModuleId, entry.key, date.format(ISO))
    }

    fun subjectStats(state: UiState): List<SubjectStat> =
        state.attendance
            .filter { it.status != AttendanceStatus.CANCELLED }
            .groupBy { it.subject }
            .map { (subject, records) ->
                SubjectStat(subject, records.count { it.status == AttendanceStatus.PRESENT }, records.count { it.status == AttendanceStatus.ABSENT })
            }.sortedBy { it.subject }

    // ---- Per-day edits ----
    fun addExtraClass(day: String, date: LocalDate?, start: String, end: String, subject: String, faculty: String, room: String) =
        viewModelScope.launch {
            repo.addCustomClass(
                CustomClassEntity(
                    moduleId = uiState.value.activeModuleId, day = day, start = start, end = end,
                    subject = subject, faculty = faculty, room = room, date = date?.format(ISO)
                )
            )
        }

    fun rescheduleForDate(entry: ScheduleEntry, date: LocalDate, newStart: String?, newEnd: String?, newRoom: String?) =
        viewModelScope.launch {
            val mid = uiState.value.activeModuleId
            val iso = date.format(ISO)
            repo.upsertOverride(DayOverrideEntity("$mid:${entry.key}@$iso", mid, entry.key, iso, newStart, newEnd, newRoom, false))
        }

    fun cancelForDate(entry: ScheduleEntry, date: LocalDate) = viewModelScope.launch {
        val mid = uiState.value.activeModuleId
        val iso = date.format(ISO)
        repo.upsertOverride(DayOverrideEntity("$mid:${entry.key}@$iso", mid, entry.key, iso, cancelled = true))
    }

    fun clearOverride(entry: ScheduleEntry, date: LocalDate) = viewModelScope.launch {
        repo.deleteOverride("${uiState.value.activeModuleId}:${entry.key}@${date.format(ISO)}")
    }

    fun deleteCustomClass(key: String) = viewModelScope.launch {
        key.removePrefix("custom-").toLongOrNull()?.let { repo.deleteCustomClass(it) }
    }

    companion object {
        fun mondayOf(d: LocalDate): LocalDate = d.with(DayOfWeek.MONDAY)
        fun toMinutes(t: String): Int {
            val parts = t.split(":")
            return if (parts.size >= 2) (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) else 0
        }
    }
}

fun toMinutes(t: String): Int = MainViewModel.toMinutes(t)
