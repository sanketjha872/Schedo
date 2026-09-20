package com.jhainusa.jss_student

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.RoomDatabase.Schedule
import com.jhainusa.jss_student.RoomDatabase.ClassSchedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeTable(vIewModel : MainVIewModel){
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showExtraClassSheet by remember { mutableStateOf(false) }
    var showHowToUseSheet by remember { mutableStateOf(false) }
    var showMarkDaySheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val howToUseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val markDaySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        AnalyticsHelper.logScreenView("TimeTableScreen", "TimeTable")
    }

    Column(
        modifier=  Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(vertical = 20.dp),
        verticalArrangement = Arrangement.Absolute.spacedBy(9.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
        ) {
            Text(
                text = "Time Table",
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.padding(end = 10.dp)
                        .size(24.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFF1F1E1E),
                            ambientColor = Color(0xFF9F9999)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = CircleShape
                        ),
                ) {
                Icon(
                    painter = painterResource(R.drawable.menu_hamburger_svgrepo_com),
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onBackground
                )
                DropdownMenu(
                    offset = DpOffset(x= 10.dp,y=0.dp),
                    expanded = menuExpanded,
                    tonalElevation = 10.dp,
                    shadowElevation = 10.dp,
                    onDismissRequest = { menuExpanded = false },
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "How to Use",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp,
                                fontFamily = plusJak,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.question_mark_svgrepo_com),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showHowToUseSheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Mark Whole Day",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp,
                                fontFamily = plusJak,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.doublecheck),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showMarkDaySheet = true
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Add Extra Class",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp,
                                fontFamily = plusJak,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            showExtraClassSheet = true
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        MonthChangeUi(
            currentMonth = currentMonth,
            onPreviousMonth = {currentMonth = currentMonth.minusMonths(1)},
            onNextMonth = {currentMonth = currentMonth.plusMonths(1)}
        )
        Spacer(modifier = Modifier.height(6.dp))
        
        CalendarWithExpandableView(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it }
        )

        Box(modifier = Modifier.weight(1f)) {
            ScheduleTimeline(selectedDate, vIewModel)
        }
    }

    if (showExtraClassSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExtraClassSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AddExtraClassBottomSheet(
                viewModel = vIewModel,
                selectedDate = selectedDate,
                onDismiss = { showExtraClassSheet = false }
            )
        }
    }

    if (showHowToUseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHowToUseSheet = false },
            sheetState = howToUseSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            HowToUseScreen(onDismiss = { showHowToUseSheet = false })
        }
    }

    if (showMarkDaySheet) {
        ModalBottomSheet(
            onDismissRequest = { showMarkDaySheet = false },
            sheetState = markDaySheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            MarkDayBottomSheet(
                selectedDate = selectedDate,
                onMark = { status ->
                    val dayName = selectedDate.dayOfWeek.name.lowercase()
                        .replaceFirstChar { it.uppercase() }.take(3)
                    vIewModel.markWholeDayAttendance(selectedDate.toString(), dayName, status)

                    AnalyticsHelper.logEvent("mark_whole_day", android.os.Bundle().apply {
                        putString("date", selectedDate.toString())
                        putInt("status", status)
                    })
                },
                onDismiss = { showMarkDaySheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MarkDayBottomSheet(
    selectedDate: LocalDate,
    onMark: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Mark Whole Day",
            fontFamily = plusJak,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "For $selectedDate",
            fontFamily = plusJak,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))

        MarkDayOption(
            title = "All Present",
            icon = R.drawable.baseline_check_24,
            color = Color(0xFF4CAF50),
            onClick = { onMark(1); onDismiss() }
        )
        MarkDayOption(
            title = "All Absent",
            icon = R.drawable.cancel_svgrepo_com,
            color = Color(0xFFF44336),
            onClick = { onMark(2); onDismiss() }
        )
        MarkDayOption(
            title = "Holiday / No Classes",
            icon = R.drawable.happyy,
            color = Color(0xFF2196F3),
            onClick = { onMark(3); onDismiss() }
        )
        MarkDayOption(
            title = "Clear All",
            icon = R.drawable.baseline_code_24,
            color = Color.Gray,
            onClick = { onMark(0); onDismiss() }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MarkDayOption(
    title: String,
    icon: Int,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontFamily = plusJak,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddExtraClassBottomSheet(
    viewModel: MainVIewModel,
    selectedDate: LocalDate,
    onDismiss: () -> Unit
) {
    val subjects by viewModel.getAll().observeAsState(emptyList())
    var selectedSubject by remember { mutableStateOf<Schedule?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("09:00 AM") }
    var endTime by remember { mutableStateOf("10:00 AM") }
    var isPickingStartTime by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Add Extra Class",
            fontFamily = plusJak,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "For $selectedDate",
            fontFamily = plusJak,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Select Subject", fontFamily = plusJak, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(subjects) { subject ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedSubject == subject) MaterialTheme.colorScheme.onBackground.copy(0.1f) else Color.Transparent)
                        .border(1.dp, if (selectedSubject == subject) MaterialTheme.colorScheme.onBackground.copy(0.1f) else OutlineColor, RoundedCornerShape(12.dp))
                        .clickable { selectedSubject = subject }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(subject.color.toULong()))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = subject.subject, fontFamily = plusJak, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeSelectionBox(
                label = "Start",
                time = startTime,
                modifier = Modifier.weight(1f),
                onClick = {
                    isPickingStartTime = true
                    showTimePicker = true
                }
            )
            TimeSelectionBox(
                label = "End",
                time = endTime,
                modifier = Modifier.weight(1f),
                onClick = {
                    isPickingStartTime = false
                    showTimePicker = true
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        androidx.compose.material.Button(
            onClick = {
                selectedSubject?.let { subject ->
                    viewModel.addExtraClass(
                        subject.subjectId,
                        selectedDate.toString(),
                        selectedDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                        "$startTime - $endTime"
                    )
                    AnalyticsHelper.logEvent("add_extra_class", android.os.Bundle().apply {
                        putString("subject", subject.subject)
                        putString("date", selectedDate.toString())
                        putString("time", "$startTime - $endTime")
                    })
                    onDismiss()
                }
            },
            enabled = selectedSubject != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material.ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background)
        ) {
            Text("Add Class", fontFamily = plusJak, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.background)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showTimePicker) {
        val initialTime = if (isPickingStartTime) startTime else endTime
        val h = try {
            var hour = initialTime.split(":")[0].toInt()
            if (initialTime.contains("PM") && hour < 12) hour += 12
            if (initialTime.contains("AM") && hour == 12) hour = 0
            hour
        } catch (_: Exception) { 9 }
        val m = try { initialTime.split(":")[1].split(" ")[0].toInt() } catch (_: Exception) { 0 }

        TimePickerDialog(
            initialHour = h,
            initialMinute = m,
            onTimeSelected = { hour, min ->
                val formatted = formatTime(hour, min)
                if (isPickingStartTime) startTime = formatted else endTime = formatted
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthChangeUi(
    currentMonth : YearMonth,
    onPreviousMonth : () -> Unit,
    onNextMonth : () -> Unit
){
      Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 15.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp,
                    Color.LightGray.copy(0.4f),
                    RoundedCornerShape(13.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            IconButton(
                onClick = onPreviousMonth
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(0.5.dp,Color.LightGray,RoundedCornerShape(7.dp))
                )
            }
            Text(
                text = "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } +" "} ${ currentMonth.year}",
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily(Font(R.font.plusjakartasansmedium)),
                fontSize = 18.sp,
            )
            IconButton(
                onClick = onNextMonth
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(0.5.dp,Color.LightGray,RoundedCornerShape(7.dp))
                )
            }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun TimeTablePreview() {
    // Preview is now complex due to ViewModel dependencies
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleTimeline(selectedDate: LocalDate, viewModel: MainVIewModel) {
    val schedules by viewModel.getAll().observeAsState(emptyList())
    val extraClasses by viewModel.getAllSchedulesForDate(selectedDate.toString()).observeAsState(emptyList())
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSwipeTooltip by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val shown = com.jhainusa.jss_student.UserPref.UserPreferences.isSwipeTooltipShown(context).first()
        if (!shown && schedules.isNotEmpty()) showSwipeTooltip = true
    }
    
    val selectedDayName = selectedDate.dayOfWeek.name.lowercase()
        .replaceFirstChar { it.uppercase() }.take(3)

    val combinedList by remember(schedules, extraClasses, selectedDayName) {
        derivedStateOf {
            // 1. Get all classes that have an entry in the database for this date (Marked Regular & Extras)
            val markedItems = extraClasses.map { classSchedule ->
                val parentSubject = schedules.find { it.subjectId == classSchedule.subjectOwnerId }
                TimelineItem.Extra(classSchedule, parentSubject)
            }
            
            // 2. Get regular classes from the current schedule template that HAVEN'T been marked yet
            val unmarkedRegular = schedules.flatMap { schedule ->
                schedule.scheduleday
                    .filter { it.day.startsWith(selectedDayName, ignoreCase = true) }
                    .filter { daySchedule ->
                        // Only add if there isn't already a record in the database for this subject and timing
                        extraClasses.none { 
                            it.subjectOwnerId == schedule.subjectId && 
                            it.timing == daySchedule.timing &&
                            !it.isExtra
                        }
                    }
                    .map { daySchedule -> 
                        TimelineItem.Regular(schedule, daySchedule.timing)
                    }
            }
            
            (markedItems + unmarkedRegular).sortedBy { item ->
                val t = when(item) {
                    is TimelineItem.Regular -> item.timing
                    is TimelineItem.Extra -> item.classSchedule.timing
                }
                parseStartTime(t)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        if (combinedList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("No classes today", color = MaterialTheme.colorScheme.onBackground.copy(0.65f), fontFamily = plusJak)
                }
            }
        } else {
            items(combinedList) { item ->
                Column {
                    when(item) {
                        is TimelineItem.Regular -> {
                            ScheduleItemRow(
                                schedule = item.schedule, 
                                timing = item.timing,
                                attendanceStatus = 0, // Unmarked
                                isExtra = false,
                                onStatusChange = { newStatus ->
                                    if (showSwipeTooltip) {
                                        showSwipeTooltip = false
                                        scope.launch { com.jhainusa.jss_student.UserPref.UserPreferences.setSwipeTooltipShown(context) }
                                    }
                                    viewModel.updateAttendance(
                                        item.schedule.subjectId, 
                                        selectedDate.toString(), 
                                        selectedDayName, 
                                        newStatus,
                                        item.timing
                                    )
                                    AnalyticsHelper.logEvent("update_attendance", android.os.Bundle().apply {
                                        putString("subject", item.schedule.subject)
                                        putInt("status", newStatus)
                                        putBoolean("is_extra", false)
                                    })
                                }
                            )
                        }
                        is TimelineItem.Extra -> {
                            if (item.parentSubject != null) {
                                ScheduleItemRow(
                                    schedule = item.parentSubject,
                                    timing = item.classSchedule.timing,
                                    attendanceStatus = item.classSchedule.attendanceStatus,
                                    isExtra = item.classSchedule.isExtra,
                                    onStatusChange = { newStatus ->
                                        if (showSwipeTooltip) {
                                            showSwipeTooltip = false
                                            scope.launch { com.jhainusa.jss_student.UserPref.UserPreferences.setSwipeTooltipShown(context) }
                                        }
                                        
                                        if (item.classSchedule.isExtra) {
                                            viewModel.updateExtraClassAttendance(item.classSchedule.classId, newStatus)
                                        } else {
                                            viewModel.updateAttendance(
                                                item.classSchedule.subjectOwnerId,
                                                item.classSchedule.date,
                                                item.classSchedule.day,
                                                newStatus,
                                                item.classSchedule.timing
                                            )
                                        }

                                        AnalyticsHelper.logEvent("update_attendance", android.os.Bundle().apply {
                                            putString("subject", item.parentSubject.subject)
                                            putInt("status", newStatus)
                                            putBoolean("is_extra", item.classSchedule.isExtra)
                                        })
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

sealed class TimelineItem {
    data class Regular(val schedule: Schedule, val timing: String) : TimelineItem()
    data class Extra(val classSchedule: ClassSchedule, val parentSubject: Schedule?) : TimelineItem()
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ScheduleItemRow(
    schedule: Schedule, 
    timing: String,
    attendanceStatus: Int,
    isExtra: Boolean = false,
    onStatusChange: (Int) -> Unit
) {
    val presentAction = SwipeAction(
        onSwipe = { onStatusChange(1) },
        icon = { Icon(painterResource(R.drawable.baseline_check_24), null, tint = Color.White, modifier = Modifier.padding(16.dp)) },
        background = Color(0xFF4CAF50)
    )
    val absentAction = SwipeAction(
        onSwipe = { onStatusChange(2) },
        icon = { Icon(painterResource(R.drawable.cancel_svgrepo_com), null, tint = Color.White, modifier = Modifier.padding(16.dp).size(24.dp)) },
        background = Color(0xFFF44336),
        weight = 4.0 // Takes up more space
    )
    val holidayAction = SwipeAction(
        onSwipe = { onStatusChange(3) },
        icon = { Icon(painterResource(R.drawable.happyy), null, tint = Color.White, modifier = Modifier.size(24.dp))
        },
        background = Color(0xFF2196F3),
        weight = 1.0// Only appears after a long swipe
    )

    val backgroundColor = when (attendanceStatus) {
        1 -> Color(0xFFE8F5E9)
        2 -> Color(0xFFFFEBEE)
        3 -> Color(0xFFE3F2FD)
        else -> Color(schedule.color.toULong())
    }

    val times = timing.split("-")
    val startTime = times.firstOrNull()?.trim() ?: ""
    val endTime = if (times.size > 1) times[1].trim() else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight()
                .padding(vertical = 15.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = startTime,
                color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                fontFamily = plusJak,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = endTime,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                fontFamily = plusJak,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        SwipeableActionsBox(
            startActions = listOf(presentAction),
            endActions = listOf(absentAction, holidayAction),
            swipeThreshold = 90.dp,
            backgroundUntilSwipeThreshold = Color.Transparent,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(backgroundColor)
                    .clickable { onStatusChange(0) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_code_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Black.copy(alpha = 0.6f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isExtra) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("EXTRA", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = schedule.teacher,
                            fontFamily = plusJak,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = schedule.subject,
                        fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                        color = Color.Black
                    )
                    Text(
                        text = schedule.roomNo,
                        fontFamily = plusJak,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
                
                if (attendanceStatus != 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when(attendanceStatus) {
                            1 -> "PRESENT"
                            2 -> "ABSENT"
                            3 -> "HOLIDAY"
                            else -> ""
                        },
                        color = when(attendanceStatus) {
                            1 -> Color(0xFF2E7D32)
                            2 -> Color(0xFFC62828)
                            3 -> Color(0xFF1565C0)
                            else -> Color.Transparent
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
