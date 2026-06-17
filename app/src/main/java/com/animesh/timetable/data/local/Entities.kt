package com.animesh.timetable.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Attendance status for a particular class instance on a particular date. */
enum class AttendanceStatus { PRESENT, ABSENT, CANCELLED }

/**
 * One attendance record. Identified by the class [entryKey] plus the ISO [date]
 * so each weekly occurrence is tracked independently.
 */
@Entity(tableName = "attendance", primaryKeys = ["entryKey", "date"])
data class AttendanceEntity(
    val entryKey: String,
    val date: String,            // ISO yyyy-MM-dd
    val subject: String,
    val status: AttendanceStatus
)

/** A user-added extra class. Recurring weekly on [day], or one-off when [date] is set. */
@Entity(tableName = "custom_class")
data class CustomClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: String,
    val start: String,
    val end: String,
    val subject: String,
    val faculty: String = "",
    val room: String = "",
    val date: String? = null     // null = recurring weekly; set = one-time on this date
)

/**
 * A per-date override of a master/custom class: change its time or room, or cancel it
 * for that single date only. Identified by the base class key + the date it applies to.
 */
@Entity(tableName = "day_override")
data class DayOverrideEntity(
    @PrimaryKey val id: String,   // "<entryKey>@<date>"
    val entryKey: String,
    val date: String,
    val newStart: String? = null,
    val newEnd: String? = null,
    val newRoom: String? = null,
    val cancelled: Boolean = false
)
