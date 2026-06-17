package com.animesh.timetable.data.repo

import android.content.Context
import com.animesh.timetable.data.local.AppDatabase
import com.animesh.timetable.data.local.AttendanceEntity
import com.animesh.timetable.data.local.AttendanceStatus
import com.animesh.timetable.data.local.CustomClassEntity
import com.animesh.timetable.data.local.DayOverrideEntity
import com.animesh.timetable.data.local.SettingsStore
import com.animesh.timetable.data.model.ClassOffering
import com.animesh.timetable.data.model.ScheduleEntry
import com.animesh.timetable.data.model.TimetableData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Single source of truth combining the bundled master timetable with user data. */
class TimetableRepository(private val context: Context) {

    private val db = AppDatabase.get(context)
    val settings = SettingsStore(context)

    val attendanceDao = db.attendanceDao()
    val customClassDao = db.customClassDao()
    val dayOverrideDao = db.dayOverrideDao()

    private var cached: TimetableData? = null

    private val json = Json { ignoreUnknownKeys = true }

    /** Loads and caches the master timetable from assets. */
    suspend fun loadMaster(): TimetableData = cached ?: withContext(Dispatchers.IO) {
        val text = context.assets.open("timetable.json")
            .bufferedReader().use { it.readText() }
        json.decodeFromString<TimetableData>(text).also { cached = it }
    }

    /** All distinct subjects available for selection, alphabetically sorted. */
    suspend fun allSubjects(): List<String> =
        loadMaster().offerings.map { it.subject }.distinct().sorted()

    /** Offerings grouped by subject (so the user can preview when a subject meets). */
    suspend fun offeringsForSubject(subject: String): List<ClassOffering> =
        loadMaster().offerings.filter { it.subject == subject }

    fun observeAttendance(): Flow<List<AttendanceEntity>> = attendanceDao.observeAll()
    fun observeCustomClasses(): Flow<List<CustomClassEntity>> = customClassDao.observeAll()
    fun observeOverrides(): Flow<List<DayOverrideEntity>> = dayOverrideDao.observeAll()

    suspend fun setAttendance(entryKey: String, date: String, subject: String, status: AttendanceStatus) =
        attendanceDao.upsert(AttendanceEntity(entryKey, date, subject, status))

    suspend fun clearAttendance(entryKey: String, date: String) =
        attendanceDao.delete(entryKey, date)

    suspend fun addCustomClass(entity: CustomClassEntity) = customClassDao.insert(entity)
    suspend fun deleteCustomClass(id: Long) = customClassDao.deleteById(id)

    suspend fun upsertOverride(entity: DayOverrideEntity) = dayOverrideDao.upsert(entity)
    suspend fun deleteOverride(id: String) = dayOverrideDao.deleteById(id)
}
