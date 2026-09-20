package com.jhainusa.jss_student

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button as M3Button
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jhainusa.jss_student.RoomDatabase.DaySchedule
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.RoomDatabase.Schedule
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.jhainusa.jss_student.UserPref.UserPreferences
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.absoluteValue

val PrimaryColor = Color(0xFF262626)
val SelectedDayColor = PrimaryColor
val UnselectedDayColor = Color.White
val OutlineColor = Color(0xFFE0E0E0)
val colorPalette = listOf(
    Color(0xFFEDE2FF), // Darker light lavender
    Color(0xFFFFE6E6), // Darker soft blush pink
    Color(0xFFFFF3CC), // Darker cream yellow
    Color(0xFFDFF7EC), // Darker soft mint
    Color(0xFFDFF7F7), // Darker powder aqua
    Color(0xFFFFE8D6), // Darker peach cream
    Color(0xFFE6F5EF), // Darker light teal mint
    Color(0xFFFFE6F2), // Darker pinkish white
    Color(0xFFDCE8FF), // Darker cloud blue
    Color(0xFFF5F5E6)
)

fun assignColor(name: String): Color {
    val index = name.hashCode().absoluteValue % colorPalette.size
    return colorPalette[index]
}


@Composable
fun AnimatedDialogContent(content: @Composable () -> Unit) {
    val transition = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = transition,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            initialScale = 0.7f
        ),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(
            animationSpec = tween(150),
            targetScale = 0.8f
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun AnimatedDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AnimatedDialogContent {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddClassScreen(viewModel: MainVIewModel, subjectId: Int = -1, onDismiss: () -> Unit) {
    val jakartaFont = plusJak
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val semesterStart by UserPreferences.getSemesterStartDate(context).collectAsState(initial = null)
    val semesterEnd by UserPreferences.getSemesterEndDate(context).collectAsState(initial = null)
    
    val scheduleToEdit by if (subjectId != -1) {
        viewModel.observeSchedule(subjectId).observeAsState()
    } else {
        remember { mutableStateOf<Schedule?>(null) }
    }

    val selectedDays = remember { 
        mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<Pair<String, String>>>()
    }

    var subject by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var roomNo by remember { mutableStateOf("") }
    var initialPresent by remember { mutableStateOf("") }
    var initialTotal by remember { mutableStateOf("") }
    var initialStartDate by remember { mutableStateOf("") }
    var initialEndDate by remember { mutableStateOf("") }
    var showAttendanceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scheduleToEdit) {
        scheduleToEdit?.let {
            subject = it.subject
            teacher = it.teacher
            roomNo = it.roomNo
            initialPresent = it.initialPresent.toString()
            initialTotal = it.initialTotal.toString()
            initialStartDate = it.initialStartDate
            initialEndDate = it.initialEndDate
            selectedDays.clear()
            it.scheduleday.forEach { daySched ->
                val times = daySched.timing.split(" - ")
                if (times.size == 2) {
                    val list = selectedDays.getOrPut(daySched.day) { androidx.compose.runtime.mutableStateListOf() }
                    list.add(times[0] to times[1])
                }
            }
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var currentPickingKey by remember { mutableStateOf<String?>(null) }
    var currentPickingIndex by remember { mutableStateOf(0) }
    var isPickingStartTime by remember { mutableStateOf(true) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    LazyColumn(
        modifier = Modifier.fillMaxWidth().background(
            MaterialTheme.colorScheme.background
        )
            .padding(16.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Top,

    ) {
        item {
            Text(
                text = if (scheduleToEdit == null) "Add New Class" else "Edit Class",
                fontFamily = jakartaFont,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            inputBox("Subject Name", subject, onValueChange = { subject = it })
        }

        item {
            inputBox("Teacher Name", teacher, onValueChange = { teacher = it })
        }

        item {
            inputBox("Room Number", roomNo, onValueChange = { roomNo = it })
        }

        item {
            AttendanceCard(
                attended = initialPresent,
                total = initialTotal,
                startDate = if (initialStartDate.isNotBlank()) initialStartDate else semesterStart,
                endDate = if (initialEndDate.isNotBlank()) initialEndDate else semesterEnd,
                onClick = { showAttendanceDialog = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = "Select Days & Timings", 
                fontFamily = jakartaFont, 
                fontWeight = FontWeight.SemiBold, 
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(days.size) { index ->
            val day = days[index]
            val key = day
            val isSelected = selectedDays.containsKey(key)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp) 
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceTint else Color.Transparent)
                    .border(
                        1.dp, 
                        if (isSelected) MaterialTheme.colorScheme.onBackground.copy(0.12f) else OutlineColor,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        if (!isSelected) {
                            selectedDays[key] = androidx.compose.runtime.mutableStateListOf("09:00 AM" to "10:00 AM")
                        }
                    }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.onBackground else OutlineColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                            fontFamily = jakartaFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = if (isSelected) "Selected" else "Tap to select",
                        fontFamily = jakartaFont,
                        fontSize = 14.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (isSelected) {
                        IconButton(
                            onClick = { selectedDays.remove(key) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.cross),
                                contentDescription = "Deselect",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val timeSlots = selectedDays[key]!!
                    
                    timeSlots.forEachIndexed { index, (start, end) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeSelectionBox(
                                label = "Start",
                                time = start,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    currentPickingKey = key
                                    currentPickingIndex = index
                                    isPickingStartTime = true
                                    showTimePicker = true
                                }
                            )

                            TimeSelectionBox(
                                label = "End",
                                time = end,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    currentPickingKey = key
                                    currentPickingIndex = index
                                    isPickingStartTime = false
                                    showTimePicker = true
                                }
                            )
                            
                            if (timeSlots.size > 1) {
                                IconButton(
                                    onClick = { timeSlots.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = "Remove Period",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Text(
                        text = " + Add another period",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = jakartaFont,
                        modifier = Modifier
                            .clickable {
                                timeSlots.add("10:00 AM" to "11:00 AM")
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            val schedules = selectedDays.flatMap { (day, list) ->
                list.map { timingPair ->
                    DaySchedule(day, "${timingPair.first} - ${timingPair.second}")
                }
            }
            Button(
                onClick = {
                    if (subject.isNotBlank()) {
                        viewModel.insertSchedule(
                            Schedule(
                                subjectId = scheduleToEdit?.subjectId ?: 0,
                                subject = subject,
                                teacher = teacher,
                                roomNo = roomNo,
                                scheduleday = schedules,
                                color = scheduleToEdit?.color ?: assignColor(subject).value.toLong(),
                                totalClasses = scheduleToEdit?.totalClasses ?: 0,
                                initialPresent = initialPresent.toIntOrNull() ?: 0,
                                initialTotal = initialTotal.toIntOrNull() ?: 0,
                                initialStartDate = initialStartDate,
                                initialEndDate = initialEndDate
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = subject.isNotBlank() && selectedDays.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    disabledBackgroundColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Text(
                    text = if (scheduleToEdit == null) "Save Class Schedule" else "Update Schedule", 
                    fontFamily = jakartaFont, 
                    color = MaterialTheme.colorScheme.background,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp)) 
        }
    }

    if (showTimePicker && currentPickingKey != null) {
        val timeSlots = selectedDays[currentPickingKey!!]!!
        val currentPair = timeSlots[currentPickingIndex]
        val initialTimeStr = if (isPickingStartTime) currentPair.first else currentPair.second
        
        val hour = try { 
            var h = initialTimeStr.split(":")[0].toInt()
            if (initialTimeStr.contains("PM") && h < 12) h += 12
            if (initialTimeStr.contains("AM") && h == 12) h = 0
            h
        } catch (e: Exception) { 12 }
        
        val minute = try { initialTimeStr.split(":")[1].split(" ")[0].toInt() } catch (e: Exception) { 0 }

        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onTimeSelected = { h, m ->
                val formatted = formatTime(h, m)
                val pair = timeSlots[currentPickingIndex]
                if (isPickingStartTime) {
                    timeSlots[currentPickingIndex] = formatted to pair.second
                } else {
                    timeSlots[currentPickingIndex] = pair.first to formatted
                }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showAttendanceDialog) {
        AttendanceEditDialog(
            initialAttended = initialPresent,
            initialTotal = initialTotal,
            initialStart = if (initialStartDate.isNotBlank()) initialStartDate else semesterStart,
            initialEnd = if (initialEndDate.isNotBlank()) initialEndDate else semesterEnd,
            onDismiss = { showAttendanceDialog = false },
            onConfirm = { attended, total, start, end ->
                initialPresent = attended
                initialTotal = total
                initialStartDate = start ?: ""
                initialEndDate = end ?: ""
                showAttendanceDialog = false
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDateForDisplay(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    return try {
        val date = java.time.LocalDate.parse(dateStr)
        date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (e: Exception) {
        dateStr
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AttendanceCard(
    attended: String,
    total: String,
    startDate: String?,
    endDate: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Previous  Class  Attendance",
                fontFamily = plusJak,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${attended.ifBlank { "0" }} attended  /  ${total.ifBlank { "0" }} total",
                fontFamily = plusJak,
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
            if (!startDate.isNullOrBlank() && !endDate.isNullOrBlank()) {
                val formattedStart = formatDateForDisplay(startDate)
                val formattedEnd = formatDateForDisplay(endDate)
                Text(
                    text = "Range: $formattedStart - $formattedEnd",
                    fontFamily = plusJak,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.arrow_sm_right_svgrepo_com),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFF94A3B8)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AttendanceEditDialog(
    initialAttended: String,
    initialTotal: String,
    initialStart: String?,
    initialEnd: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String?) -> Unit
) {
    var attended by remember { mutableStateOf(initialAttended) }
    var total by remember { mutableStateOf(initialTotal) }
    var startDate by remember { mutableStateOf(initialStart) }
    var endDate by remember { mutableStateOf(initialEnd) }
    var showRangePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AnimatedDialog(showDialog = true, onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Initial Attendance",
                fontFamily = plusJak,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 20.dp),
                color = Color(0xFF0F172A)
            )

            androidx.compose.material.OutlinedTextField(
                value = attended,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() }) {
                        attended = it
                        errorMessage = null
                    }
                },
                label = { Text("Classes Attended", fontFamily = plusJak) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = PrimaryColor,
                    unfocusedBorderColor = OutlineColor,
                    cursorColor = PrimaryColor
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            androidx.compose.material.OutlinedTextField(
                value = total,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() }) {
                        total = it
                        errorMessage = null
                    }
                },
                label = { Text("Total Classes", fontFamily = plusJak) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = PrimaryColor,
                    unfocusedBorderColor = OutlineColor,
                    cursorColor = PrimaryColor
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontFamily = plusJak,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Previous Class Range",
                fontFamily = plusJak,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable { showRangePicker = true }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.calendar_svgrepo_com),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val rangeText = if (!startDate.isNullOrBlank() && !endDate.isNullOrBlank()) {
                        "${formatDateForDisplay(startDate)} - ${formatDateForDisplay(endDate)}"
                    } else {
                        "Select Previous Class Range"
                    }
                    Text(
                        text = rangeText,
                        fontFamily = plusJak,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    val attendedInt = attended.toIntOrNull() ?: 0
                    val totalInt = total.toIntOrNull() ?: 0
                    if (attendedInt > totalInt) {
                        errorMessage = "Attended classes cannot be more than total classes"
                    } else {
                        onConfirm(attended, total, startDate, endDate)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.elevation(0.dp)
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = plusJak)
            }
        }
    }

    if (showRangePicker) {
        val currentStart = try { LocalDate.parse(startDate) } catch(e: Exception) { LocalDate.now().minusDays(15) }
        val currentEnd = try { LocalDate.parse(endDate) } catch(e: Exception) { LocalDate.now() }
        
        DateRangePickerDialog(
            text = "Previous Attendance range",
            initialStart = currentStart,
            initialEnd = currentEnd,
            onDismiss = { showRangePicker = false },
            onRangeSelected = { start, end ->
                startDate = start.toString()
                endDate = end.toString()
                showRangePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateRangePickerDialog(
    text : String = "Filter by range",
    initialStart: LocalDate,
    initialEnd: LocalDate,
    onDismiss: () -> Unit,
    onRangeSelected: (LocalDate, LocalDate) -> Unit
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
        initialSelectedEndDateMillis = if (initialEnd.isBefore(initialStart)) {
            initialStart.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        } else {
            initialEnd.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }
    )
    
    val pickerColors = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.background,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        headlineContentColor = MaterialTheme.colorScheme.onBackground,
        weekdayContentColor = MaterialTheme.colorScheme.tertiary,
        subheadContentColor = MaterialTheme.colorScheme.onBackground,
        yearContentColor = MaterialTheme.colorScheme.onBackground,
        currentYearContentColor = MaterialTheme.colorScheme.onBackground,
        selectedYearContainerColor = MaterialTheme.colorScheme.onBackground,
        selectedYearContentColor = MaterialTheme.colorScheme.background,
        dayContentColor = MaterialTheme.colorScheme.tertiary,
        disabledDayContentColor = Color.Gray.copy(alpha = 0.3f),
        selectedDayContainerColor = MaterialTheme.colorScheme.onBackground,
        selectedDayContentColor = MaterialTheme.colorScheme.background,
        todayContentColor = MaterialTheme.colorScheme.onBackground,
        todayDateBorderColor = MaterialTheme.colorScheme.onBackground,
        dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
        dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onBackground,
        dividerColor = Color.Transparent
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            M3Button(
                onClick = {
                    val start = state.selectedStartDateMillis?.let {
                        java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                    } ?: initialStart

                    val end = state.selectedEndDateMillis?.let {
                        java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                    } ?: initialEnd
                    onRangeSelected(start, end)
                },
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 12.dp)
                    .height(44.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Select Range",
                    color = MaterialTheme.colorScheme.background,
                    fontFamily = plusJak,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = pickerColors,
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
            ) {
                Text("Cancel",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = plusJak,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.weight(1f),
            colors = pickerColors,
            showModeToggle = false,
            title = {
                Text(
                    text = text,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = plusJak,
                    fontWeight = FontWeight.Bold
                )
            },
            headline = {
                val start = state.selectedStartDateMillis
                val end = state.selectedEndDateMillis
                if (start != null && end != null) {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
                    val startD = java.time.Instant.ofEpochMilli(start)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate()
                    val endD = java.time.Instant.ofEpochMilli(end)
                        .atZone(java.time.ZoneOffset.UTC)
                        .toLocalDate()
                    Text(
                        text = "${startD.format(formatter)} - ${endD.format(formatter)}",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontFamily = plusJak,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Start Date - End Date",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontFamily = plusJak,
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            }
        )
    }
}

@Composable
fun TimeSelectionBox(
    label: String,
    time: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier.clickable { onClick() }) {
        Text(
            text = label, 
            fontSize = 12.sp, 
            color = Color(0xFFA0A0A0),
            fontFamily = plusJak,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                .border(1.dp, OutlineColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.calendar_svgrepo_com),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = time,
                    fontFamily = plusJak,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = false)
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Select Time",
                    fontFamily = plusJak,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 20.dp)
                )
                
                TimePicker(state = state)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        androidx.compose.material3.Text("Cancel", color = PrimaryColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.TextButton(
                        onClick = { onTimeSelected(state.hour, state.minute) }
                    ) {
                        androidx.compose.material3.Text("OK", color = PrimaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
}
