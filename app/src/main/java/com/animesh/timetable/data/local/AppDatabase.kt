package com.animesh.timetable.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun toStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)

    @TypeConverter
    fun fromStatus(status: AttendanceStatus): String = status.name
}

@Database(
    entities = [
        ModuleEntity::class,
        OfferingEntity::class,
        SelectedSubjectEntity::class,
        AttendanceEntity::class,
        CustomClassEntity::class,
        DayOverrideEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun offeringDao(): OfferingDao
    abstract fun selectedSubjectDao(): SelectedSubjectDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun customClassDao(): CustomClassDao
    abstract fun dayOverrideDao(): DayOverrideDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timetable.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
