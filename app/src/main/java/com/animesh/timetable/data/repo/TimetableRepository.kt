package com.animesh.timetable.data.repo

import android.content.Context
import android.net.Uri
import com.animesh.timetable.data.local.AppDatabase
import com.animesh.timetable.data.local.AttendanceEntity
import com.animesh.timetable.data.local.AttendanceStatus
import com.animesh.timetable.data.local.CustomClassEntity
import com.animesh.timetable.data.local.DayOverrideEntity
import com.animesh.timetable.data.local.ModuleEntity
import com.animesh.timetable.data.local.OfferingEntity
import com.animesh.timetable.data.local.SelectedSubjectEntity
import com.animesh.timetable.data.local.SettingsStore
import com.animesh.timetable.data.model.TimetableData
import com.animesh.timetable.data.xlsx.XlsxImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Single source of truth: modules, offerings, selections, attendance and edits. */
class TimetableRepository(private val context: Context) {

    private val db = AppDatabase.get(context)
    val settings = SettingsStore(context)

    val moduleDao = db.moduleDao()
    val offeringDao = db.offeringDao()
    val selectedSubjectDao = db.selectedSubjectDao()
    val attendanceDao = db.attendanceDao()
    val customClassDao = db.customClassDao()
    val dayOverrideDao = db.dayOverrideDao()

    private val json = Json { ignoreUnknownKeys = true }

    /** Seeds the built-in Module 5 from assets on first launch, returns its id. */
    suspend fun ensureSeeded(): Long = withContext(Dispatchers.IO) {
        val existing = moduleDao.getAll()
        if (existing.isNotEmpty()) return@withContext existing.first().id

        val text = context.assets.open("timetable.json").bufferedReader().use { it.readText() }
        val data = json.decodeFromString<TimetableData>(text)
        val moduleId = moduleDao.insert(
            ModuleEntity(name = data.module.ifBlank { "Module 5" }, source = data.source, builtIn = true)
        )
        offeringDao.insertAll(
            data.offerings.map { o ->
                OfferingEntity(
                    moduleId = moduleId, day = o.day, start = o.start, end = o.end,
                    slot = o.slot, section = o.section, subject = o.subject,
                    faculty = o.faculty, room = o.room
                )
            }
        )
        moduleId
    }

    /** Parses an uploaded .xlsx and creates a new module; returns its id. */
    suspend fun importModule(uri: Uri, name: String): Long = withContext(Dispatchers.IO) {
        val drafts = context.contentResolver.openInputStream(uri)?.use { XlsxImporter.parse(it) }
            ?: throw XlsxImporter.ParseException("Could not open the selected file.")
        val moduleId = moduleDao.insert(ModuleEntity(name = name, source = "import", builtIn = false))
        offeringDao.insertAll(
            drafts.map { d ->
                OfferingEntity(
                    moduleId = moduleId, day = d.day, start = d.start, end = d.end,
                    slot = d.slot, section = d.section, subject = d.subject,
                    faculty = d.faculty, room = d.room
                )
            }
        )
        moduleId
    }

    suspend fun deleteModule(id: Long) = withContext(Dispatchers.IO) {
        offeringDao.deleteForModule(id)
        moduleDao.delete(id)
    }

    // selections
    suspend fun toggleSubject(moduleId: Long, subject: String, selected: Boolean) {
        if (selected) selectedSubjectDao.add(SelectedSubjectEntity(moduleId, subject))
        else selectedSubjectDao.remove(moduleId, subject)
    }

    // attendance
    suspend fun setAttendance(moduleId: Long, entryKey: String, date: String, subject: String, status: AttendanceStatus) =
        attendanceDao.upsert(AttendanceEntity(moduleId, entryKey, date, subject, status))

    suspend fun clearAttendance(moduleId: Long, entryKey: String, date: String) =
        attendanceDao.delete(moduleId, entryKey, date)

    // custom + overrides
    suspend fun addCustomClass(entity: CustomClassEntity) = customClassDao.insert(entity)
    suspend fun deleteCustomClass(id: Long) = customClassDao.deleteById(id)
    suspend fun upsertOverride(entity: DayOverrideEntity) = dayOverrideDao.upsert(entity)
    suspend fun deleteOverride(id: String) = dayOverrideDao.deleteById(id)
}
