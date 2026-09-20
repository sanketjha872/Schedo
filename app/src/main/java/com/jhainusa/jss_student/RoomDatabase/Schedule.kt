package com.jhainusa.jss_student.RoomDatabase

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subject")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val subjectId : Int = 0,
    val scheduleday : List<DaySchedule>,
    val subject: String,
    val teacher : String,
    val color: Long = 0,
    val totalClasses : Int = 0, // This is actually presentCount
    @ColumnInfo(defaultValue = "")
    val roomNo: String = "",
    @ColumnInfo(defaultValue = "0")
    val initialPresent: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val initialTotal: Int = 0,
    @ColumnInfo(defaultValue = "")
    val initialStartDate: String = "",
    @ColumnInfo(defaultValue = "")
    val initialEndDate: String = ""
)

@Entity(
    tableName = "class_schedule",
    foreignKeys = [
        ForeignKey(
            entity = Schedule::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectOwnerId"), Index("date")]
)
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true) val classId: Int = 0,
    val subjectOwnerId: Int,
    val day: String,
    val date : String = "",
    val attendanceStatus: Int = 0, // 0: Unmarked, 1: Present, 2: Absent, 3: Holiday
    val timing: String = "",
    val isExtra: Boolean = false
)
