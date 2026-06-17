package com.animesh.timetable.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus { PRESENT, ABSENT, CANCELLED }

/** A module (e.g. "Module 5"). The built-in one is seeded from bundled assets. */
@Entity(tableName = "module")
data class ModuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val source: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val builtIn: Boolean = false
)

/** One class offering parsed from a module's timetable. */
@Entity(
    tableName = "offering",
    indices = [Index("moduleId")]
)
data class OfferingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val day: String,
    val start: String,
    val end: String,
    val slot: String = "",
    val section: String = "",   // "" = elective open to all; otherwise the section it belongs to
    val subject: String,
    val faculty: String = "",
    val room: String = ""
)

/** Subjects the student selected, per module. */
@Entity(tableName = "selected_subject", primaryKeys = ["moduleId", "subject"])
data class SelectedSubjectEntity(
    val moduleId: Long,
    val subject: String
)

/** Attendance for a class instance on a date, scoped to a module. */
@Entity(tableName = "attendance", primaryKeys = ["moduleId", "entryKey", "date"])
data class AttendanceEntity(
    val moduleId: Long,
    val entryKey: String,
    val date: String,
    val subject: String,
    val status: AttendanceStatus
)

/** A user-added extra class. Recurring weekly on [day], or one-off when [date] is set. */
@Entity(tableName = "custom_class", indices = [Index("moduleId")])
data class CustomClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val day: String,
    val start: String,
    val end: String,
    val subject: String,
    val faculty: String = "",
    val room: String = "",
    val date: String? = null
)

/** A per-date override of a class: change time/room or cancel for a single date. */
@Entity(tableName = "day_override")
data class DayOverrideEntity(
    @PrimaryKey val id: String,   // "<moduleId>:<entryKey>@<date>"
    val moduleId: Long,
    val entryKey: String,
    val date: String,
    val newStart: String? = null,
    val newEnd: String? = null,
    val newRoom: String? = null,
    val cancelled: Boolean = false
)
