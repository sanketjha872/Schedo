package com.jhainusa.jss_student.RoomDatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Schedule::class, ClassSchedule::class], version = 6)
@TypeConverters(Converters::class)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun ScheduleDao(): ScheduleDao
    abstract fun classScheduleDao(): ClassScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: ScheduleDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE subject ADD COLUMN roomNo TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE subject ADD COLUMN initialPresent INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE subject ADD COLUMN initialTotal INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE subject ADD COLUMN initialStartDate TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE subject ADD COLUMN initialEndDate TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): ScheduleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScheduleDatabase::class.java,
                    "schedule_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
