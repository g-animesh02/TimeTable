package com.animesh.timetable.data.model

import kotlinx.serialization.Serializable

/** Root structure of the bundled timetable.json asset (used to seed the built-in module). */
@Serializable
data class TimetableData(
    val source: String = "",
    val module: String = "",
    val days: List<String> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val offerings: List<ClassOffering> = emptyList()
)

@Serializable
data class TimeSlot(val start: String, val end: String)

@Serializable
data class ClassOffering(
    val id: Int = 0,
    val day: String,
    val start: String,
    val end: String,
    val slot: String = "",
    val section: String = "",
    val subject: String,
    val faculty: String = "",
    val room: String = ""
)

/**
 * A unified class entry shown in the UI. Originates either from a module offering the
 * student selected or from a user-added extra class; per-date overrides may adjust it.
 */
data class ScheduleEntry(
    val key: String,
    val day: String,
    val start: String,
    val end: String,
    val subject: String,
    val faculty: String,
    val room: String,
    val slot: String,
    val section: String,
    val isExtra: Boolean,
    val isModified: Boolean = false,
    val isCancelled: Boolean = false
) {
    val timeRange: String get() = "$start - $end"
}
