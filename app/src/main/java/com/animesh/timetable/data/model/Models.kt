package com.animesh.timetable.data.model

import kotlinx.serialization.Serializable

/** Root structure of the bundled timetable.json asset. */
@Serializable
data class TimetableData(
    val source: String = "",
    val module: String = "",
    val days: List<String> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val offerings: List<ClassOffering> = emptyList()
)

@Serializable
data class TimeSlot(
    val start: String,
    val end: String
)

/**
 * A single class offering parsed from the master timetable.
 * Each elective basket subject maps to one offering with its own faculty/room.
 */
@Serializable
data class ClassOffering(
    val id: Int,
    val day: String,
    val start: String,
    val end: String,
    val slot: String = "",
    val batch: String = "",
    val subject: String,
    val faculty: String = "",
    val room: String = "",
    val allFaculty: List<String> = emptyList(),
    val sectioned: Boolean = false
)

/**
 * A unified class entry shown in the timetable. It may originate from the master
 * timetable (an offering the student selected) or be a user-added extra class.
 * Day-specific overrides may adjust its time/room or cancel it for one date.
 */
data class ScheduleEntry(
    val key: String,          // stable identity: "off-<id>" or "custom-<id>"
    val day: String,
    val start: String,
    val end: String,
    val subject: String,
    val faculty: String,
    val room: String,
    val slot: String,
    val batch: String,
    val isExtra: Boolean,     // true = user-added class
    val isModified: Boolean = false,
    val isCancelled: Boolean = false
) {
    val timeRange: String get() = "$start - $end"
}
