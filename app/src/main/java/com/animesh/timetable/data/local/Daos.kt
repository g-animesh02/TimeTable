package com.animesh.timetable.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance")
    fun observeAll(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun observeForDate(date: String): Flow<List<AttendanceEntity>>

    @Upsert
    suspend fun upsert(record: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE entryKey = :entryKey AND date = :date")
    suspend fun delete(entryKey: String, date: String)
}

@Dao
interface CustomClassDao {
    @Query("SELECT * FROM custom_class")
    fun observeAll(): Flow<List<CustomClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomClassEntity): Long

    @Delete
    suspend fun delete(entity: CustomClassEntity)

    @Query("DELETE FROM custom_class WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface DayOverrideDao {
    @Query("SELECT * FROM day_override")
    fun observeAll(): Flow<List<DayOverrideEntity>>

    @Upsert
    suspend fun upsert(entity: DayOverrideEntity)

    @Query("DELETE FROM day_override WHERE id = :id")
    suspend fun deleteById(id: String)
}
