package com.animesh.timetable.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM module ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM module ORDER BY createdAt ASC")
    suspend fun getAll(): List<ModuleEntity>

    @Query("SELECT COUNT(*) FROM module")
    suspend fun count(): Int

    @Insert
    suspend fun insert(module: ModuleEntity): Long

    @Query("DELETE FROM module WHERE id = :id AND builtIn = 0")
    suspend fun delete(id: Long)
}

@Dao
interface OfferingDao {
    @Query("SELECT * FROM offering WHERE moduleId = :moduleId")
    fun observeForModule(moduleId: Long): Flow<List<OfferingEntity>>

    @Insert
    suspend fun insertAll(offerings: List<OfferingEntity>)

    @Query("DELETE FROM offering WHERE moduleId = :moduleId")
    suspend fun deleteForModule(moduleId: Long)
}

@Dao
interface SelectedSubjectDao {
    @Query("SELECT subject FROM selected_subject WHERE moduleId = :moduleId")
    fun observeForModule(moduleId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entity: SelectedSubjectEntity)

    @Query("DELETE FROM selected_subject WHERE moduleId = :moduleId AND subject = :subject")
    suspend fun remove(moduleId: Long, subject: String)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance")
    fun observeAll(): Flow<List<AttendanceEntity>>

    @Upsert
    suspend fun upsert(record: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE moduleId = :moduleId AND entryKey = :entryKey AND date = :date")
    suspend fun delete(moduleId: Long, entryKey: String, date: String)
}

@Dao
interface CustomClassDao {
    @Query("SELECT * FROM custom_class WHERE moduleId = :moduleId")
    fun observeForModule(moduleId: Long): Flow<List<CustomClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomClassEntity): Long

    @Query("DELETE FROM custom_class WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface DayOverrideDao {
    @Query("SELECT * FROM day_override WHERE moduleId = :moduleId")
    fun observeForModule(moduleId: Long): Flow<List<DayOverrideEntity>>

    @Upsert
    suspend fun upsert(entity: DayOverrideEntity)

    @Query("DELETE FROM day_override WHERE id = :id")
    suspend fun deleteById(id: String)
}
