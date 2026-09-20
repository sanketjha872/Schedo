package com.jhainusa.jss_student.RoomDatabase

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

import androidx.room.Upsert

@Dao
interface ScheduleDao {
    @Upsert
    suspend fun insertSchedule(schedule: Schedule)

    @Query("SELECT * FROM subject ORDER BY subject ASC")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Query("SELECT * FROM subject")
    suspend fun getAllSchedulesSync(): List<Schedule>

    @Query("SELECT * FROM subject WHERE subjectId = :id")
    fun observeSchedule(id: Int): Flow<Schedule>

    @Query("UPDATE subject SET totalClasses = totalClasses + 1 WHERE subjectId = :subjectId")
    suspend fun incrementTotal(subjectId: Int)

    @Query("UPDATE subject SET totalClasses = totalClasses - 1 WHERE subjectId = :subjectId")
    suspend fun decrementTotal(subjectId: Int)

    @Query("UPDATE subject SET initialPresent = :present, initialTotal = :total WHERE subjectId = :subjectId")
    suspend fun updateInitialAttendance(subjectId: Int, present: Int, total: Int)

    @Delete
    suspend fun deleteSubject(schedule: Schedule)

    @Query("DELETE FROM subject")
    suspend fun delete()
}

@Dao
interface ClassScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(schedule: ClassSchedule)

    @Update
    suspend fun updateSchedule(schedule: ClassSchedule)

    @Query("DELETE FROM class_schedule WHERE subjectOwnerId = :subjectId AND date = :date AND timing = :timing")
    suspend fun deleteScheduleForDateByTiming(subjectId: Int, date: String, timing: String)

    @Query("DELETE FROM class_schedule WHERE subjectOwnerId = :subjectId AND date = :date")
    suspend fun deleteScheduleForDate(subjectId: Int, date: String)

    @Query("SELECT * FROM class_schedule WHERE subjectOwnerId = :subjectId AND date = :date LIMIT 1")
    fun getScheduleForDate(subjectId: Int, date: String): Flow<ClassSchedule?>

    @Query("SELECT * FROM class_schedule WHERE subjectOwnerId = :subjectId AND date = :date LIMIT 1")
    suspend fun getScheduleForDateSync(subjectId: Int, date: String): ClassSchedule?

    @Query("SELECT * FROM class_schedule WHERE subjectOwnerId = :subjectId AND date = :date AND timing = :timing LIMIT 1")
    suspend fun getScheduleByTimingSync(subjectId: Int, date: String, timing: String): ClassSchedule?

    @Query("SELECT * FROM class_schedule WHERE date = :date")
    fun getAllSchedulesForDate(date: String): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedule WHERE date = :date")
    suspend fun getAllSchedulesForDateSync(date: String): List<ClassSchedule>

    @Query("SELECT * FROM class_schedule WHERE subjectOwnerId = :subjectId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getAttendanceInRange(subjectId: Int, startDate: String, endDate: String): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedule WHERE subjectOwnerId = :subjectId ORDER BY date DESC")
    fun getAttendanceHistory(subjectId: Int): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedule")
    fun getAllAttendanceRecords(): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedule WHERE classId = :classId")
    suspend fun getClassById(classId: Int): ClassSchedule?
}
