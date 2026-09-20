package com.jhainusa.jss_student

import android.os.Build
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhainusa.jss_student.RoomDatabase.ClassSchedule
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.RoomDatabase.Schedule
import com.jhainusa.jss_student.UserPref.UserPreferences
import com.jhainusa.jss_student.ui.theme.JSS_STUDENTTheme
import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BunkAnalyticsScreen(viewModel: MainVIewModel, subjectId: Int) {
    val context = LocalContext.current
    val subject by viewModel.observeSchedule(subjectId).observeAsState()
    val attendanceHistory by viewModel.getAttendanceHistory(subjectId).observeAsState(emptyList())
    val desiredAttendance by UserPreferences.getDesiredAttendance(context).collectAsState(initial = 75f)
    val prevStart by UserPreferences.getSemesterStartDate(context).collectAsState(initial = null)
    val prevEnd by UserPreferences.getSemesterEndDate(context).collectAsState(initial = null)

    androidx.compose.runtime.LaunchedEffect(subjectId) {
        AnalyticsHelper.logScreenView("BunkAnalyticsScreen", "BunkAnalytics")
        subject?.let {
            AnalyticsHelper.logEvent("view_bunk_analytics", android.os.Bundle().apply {
                putString("subject", it.subject)
            })
        }
    }

    BunkAnalyticsContent(
        subject = subject,
        attendanceHistory = attendanceHistory,
        desiredAttendance = desiredAttendance,
        prevRange = if (prevStart != null && prevEnd != null) {
            try {
                LocalDate.parse(prevStart!!) to LocalDate.parse(prevEnd!!)
            } catch (e: Exception) { null }
        } else null,
        isOverall = subjectId == -1
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BunkAnalyticsContent(
    subject: Schedule?,
    attendanceHistory: List<ClassSchedule>,
    desiredAttendance: Float,
    prevRange: Pair<LocalDate, LocalDate>? = null,
    isOverall: Boolean = false
) {
    val threshold = desiredAttendance / 100.0

    val minDate = remember(attendanceHistory) {
        attendanceHistory.mapNotNull {
            try { LocalDate.parse(it.date) } catch (e: Exception) { null }
        }.minOrNull() ?: LocalDate.now().minusMonths(3)
    }

    var startDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    
    // For date selection dialogs
    var showStartDatePicker by remember { mutableStateOf(false) }

    val filteredAttendance = attendanceHistory.filter {
        val date = try { LocalDate.parse(it.date, DateTimeFormatter.ISO_DATE) } catch(e: Exception) { null }
        date != null && !date.isBefore(startDate) && !date.isAfter(endDate)
    }

    val relevantAttendance = filteredAttendance.filter { it.attendanceStatus == 1 || it.attendanceStatus == 2 }

    val subjectStart = remember(subject) {
        val sDate = subject?.initialStartDate
        if (!sDate.isNullOrBlank()) {
            try { LocalDate.parse(sDate) } catch (e: Exception) { null }
        } else null
    }
    val subjectEnd = remember(subject) {
        val eDate = subject?.initialEndDate
        if (!eDate.isNullOrBlank()) {
            try { LocalDate.parse(eDate) } catch (e: Exception) { null }
        } else null
    }

    val finalRange = remember(prevRange, subjectStart, subjectEnd) {
        if (subjectStart != null && subjectEnd != null) subjectStart to subjectEnd
        else prevRange
    }

    val initialTotalInRange = remember(startDate, endDate, subject, finalRange) {
        if (finalRange == null) 0
        else {
            val (pStart, pEnd) = finalRange
            val overlapStart = if (startDate.isAfter(pStart)) startDate else pStart
            val overlapEnd = if (endDate.isBefore(pEnd)) endDate else pEnd

            if (!overlapStart.isAfter(overlapEnd)) {
                subject?.initialTotal ?: 0
            } else 0
        }
    }

    val initialPresentInRange = remember(startDate, endDate, subject, finalRange) {
        if (finalRange == null) 0
        else {
            val (pStart, pEnd) = finalRange
            val overlapStart = if (startDate.isAfter(pStart)) startDate else pStart
            val overlapEnd = if (endDate.isBefore(pEnd)) endDate else pEnd

            if (!overlapStart.isAfter(overlapEnd)) {
                subject?.initialPresent ?: 0
            } else 0
        }
    }

    val totalClassesInRange = relevantAttendance.size + initialTotalInRange
    val attendedClassesInRange = relevantAttendance.count { it.attendanceStatus == 1 } + initialPresentInRange
    val missedClassesInRange = relevantAttendance.count { it.attendanceStatus == 2 } + (initialTotalInRange - initialPresentInRange)
    val rangeAttendanceRate = if (totalClassesInRange > 0) (attendedClassesInRange.toDouble() / totalClassesInRange) else 0.0
    val rangeAttendanceRatePercent = (rangeAttendanceRate * 100).toInt()


    val predictionText: String
    val predictionTitle: String
    val predictionSubtitle: String
    val predictionColor: Color

    if (rangeAttendanceRate >= threshold) {
        val maxBunks = if (totalClassesInRange > 0) ((attendedClassesInRange / threshold) - totalClassesInRange).toInt() else 0
        predictionTitle = "$maxBunks More"
        predictionSubtitle = "Safe bunks in range"
        predictionText = "In this period, you could miss $maxBunks more classes to stay above ${desiredAttendance.toInt()}%."
        predictionColor = Color(0xFFFBE7D7)
    } else {
        val classesToAttend = if (totalClassesInRange > 0) {
            ceil((threshold * totalClassesInRange - attendedClassesInRange) / (1.0 - threshold)).toInt().coerceAtLeast(0)
        } else {
            0
        }
        predictionTitle = "Next $classesToAttend"
        predictionSubtitle = "Needed in range"
        predictionText = "To reach ${desiredAttendance.toInt()}% for this period, you would need to attend $classesToAttend more classes."
        predictionColor = Color(0xFFDBF8EB)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            ) {
                Text(
                        text = "Analytics",
                        fontFamily = FontFamily(Font(R.font.plusjakartasansmedium)),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 30.sp,
                    )
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    text = if (subject == null) "Overall Analytics" else subject.subject,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = plusJak,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                )
            }
        },
        modifier = Modifier
            .background(Color.White)
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            AttendanceCalendarCard(attendanceHistory, startDate, endDate)

            Spacer(modifier = Modifier.height(24.dp))

            DateRangeFilterPresets(
                minDate = minDate,
                prevRange = finalRange,
                onRangeSelected = { start, end ->
                    startDate = start
                    endDate = end
                },
                currentStart = startDate,
                currentEnd = endDate
            )

            val isPrevRangeSelected = finalRange != null && startDate == finalRange.first && endDate == finalRange.second
            RangeSummarySection(
                rate = "$rangeAttendanceRatePercent%",
                attended = attendedClassesInRange.toString(),
                missed = missedClassesInRange.toString(),
                total = totalClassesInRange.toString(),
                dateRangeText = "Showing: ${startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))} - ${endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                onEditRange = { showStartDatePicker = true },
                isPrevRangeSelected = isPrevRangeSelected,
                isSubjectSpecific = subjectStart != null && subjectEnd != null
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Two small cards
            Row(modifier = Modifier.fillMaxWidth()) {
                StatsSmallCard(
                    modifier = Modifier.weight(1f),
                    title = predictionTitle,
                    subtitle = predictionSubtitle,
                    icon = ImageVector.vectorResource(R.drawable.schoolbell),
                    backgroundColor = predictionColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                StatsSmallCard(
                    modifier = Modifier.weight(1f),
                    title = "${attendedClassesInRange}/${totalClassesInRange}",
                    subtitle = "Range Attendance",
                    icon = Icons.Default.ElectricBolt,
                    backgroundColor = Color(0xFFDBEEFB)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            FuturePredictionCard(predictionText)

            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom nav
        }
    }

    if (showStartDatePicker) {
        DateRangePickerDialog(
            initialStart = startDate,
            initialEnd = endDate,
            onDismiss = { showStartDatePicker = false },
            onRangeSelected = { start, end ->
                startDate = start
                endDate = end
                showStartDatePicker = false
            }
        )
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AttendanceCalendarCard(
    attendanceHistory: List<ClassSchedule>,
    startDate: LocalDate,
    endDate: LocalDate
) {
    val accentColor = MaterialTheme.colorScheme.onSurface
    var currentMonth by remember(endDate) { mutableStateOf(YearMonth.from(endDate)) }
    
    val attendanceMap = attendanceHistory.groupBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut())
                }
            }
        ) { targetMonth ->
            Text(
                text = targetMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = plusJak,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { currentMonth = currentMonth.minusMonths(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accentColor
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(
                    onClick = { currentMonth = currentMonth.plusMonths(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accentColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontFamily = plusJak,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val firstDayOfMonth = currentMonth.atDay(1)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 for Sunday
        val daysInMonth = currentMonth.lengthOfMonth()
        
        val weeks = mutableListOf<List<String>>()
        var currentWeek = mutableListOf<String>()
        
        for (i in 0 until firstDayOfWeek) {
            currentWeek.add("")
        }
        
        for (i in 1..daysInMonth) {
            currentWeek.add(i.toString())
            if (currentWeek.size == 7) {
                weeks.add(currentWeek)
                currentWeek = mutableListOf()
            }
        }
        
        if (currentWeek.isNotEmpty()) {
            while (currentWeek.size < 7) {
                currentWeek.add("")
            }
            weeks.add(currentWeek)
        }

        weeks.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day.isNotEmpty()) {
                            val date = currentMonth.atDay(day.toInt())
                            val dateString = date.format(DateTimeFormatter.ISO_DATE)
                            
                            val isWithinRange = !date.isBefore(startDate) && !date.isAfter(endDate)
                            val attendanceList = if (isWithinRange) attendanceMap[dateString] ?: emptyList() else emptyList()

                            val hasAbsent = attendanceList.any { it.attendanceStatus == 2 }
                            val hasPresent = attendanceList.any { it.attendanceStatus == 1 }

                            val targetBgColor = when {
                                attendanceList.isEmpty() -> Color.Transparent
                                hasAbsent && hasPresent -> Color(0xFFF5C278) // Mixed - Orange
                                hasAbsent -> Color(0xF3F24D4D) // All Absent - Red
                                hasPresent -> Color(0xFF77BB7E) // All Present - Green
                                attendanceList.any { it.attendanceStatus == 3 } -> Color(0xFF2196F3) // Holiday - Blue
                                else -> Color.Transparent
                            }
                            val animatedBgColor by animateColorAsState(
                                targetValue = targetBgColor,
                                animationSpec = tween(durationMillis = 400)
                            )

                            val targetTextColor = when {
                                attendanceList.isNotEmpty() -> MaterialTheme.colorScheme.onBackground
                                !isWithinRange -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                else -> MaterialTheme.colorScheme.tertiary
                            }
                            val animatedTextColor by animateColorAsState(
                                targetValue = targetTextColor,
                                animationSpec = tween(durationMillis = 400)
                            )

                            val scale by animateFloatAsState(
                                targetValue = if (isWithinRange) 1f else 0.9f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        alpha = if (isWithinRange) 1f else 0.6f
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(animatedBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 16.sp,
                                    fontFamily = plusJak,
                                    color = animatedTextColor,
                                    fontWeight = if (attendanceList.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                )

                                if (attendanceList.size > 1) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        attendanceList.sortedBy { it.attendanceStatus }.take(4).forEach { cls ->
                                            Box(
                                                modifier = Modifier
                                                    .size(3.5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (cls.attendanceStatus) {
                                                            1 -> Color(0xFF059F13) // Present
                                                            2 -> Color(0xF3C41616) // Absent
                                                            else -> Color(0xD81A4FF4)
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RangeSummarySection(rate: String, attended: String, missed: String, total: String, dateRangeText: String, onEditRange: () -> Unit, isPrevRangeSelected: Boolean = false, isSubjectSpecific: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onEditRange() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = dateRangeText,
                        fontSize = 14.sp,
                        fontFamily = plusJak,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (isPrevRangeSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = if (isSubjectSpecific) "Subject Attendance Range" else "Previous Attendance",
                                fontSize = 10.sp,
                                fontFamily = plusJak,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Edit",
                    fontSize = 14.sp,
                    fontFamily = plusJak,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "RANGE SUMMARY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = plusJak,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RangeSummaryItem(
                modifier = Modifier.weight(1f),
                label = "RATE",
                value = rate,
                backgroundColor = Color(0xFFE2DFFB),
                textColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(20.dp, 5.dp, 5.dp, 20.dp)
            )
            RangeSummaryItem(
                modifier = Modifier.weight(1f),
                label = "PRESENT",
                value = attended,
                backgroundColor = Color(0xFFDBF8EB),
                textColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(5.dp)

            )
            RangeSummaryItem(
                modifier = Modifier.weight(1f),
                label = "ABSENT",
                value = missed,
                backgroundColor = Color(0xFFF9E0E0),
                textColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(5.dp, 20.dp, 20.dp, 5.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateRangeFilterPresets(
    minDate: LocalDate,
    prevRange: Pair<LocalDate, LocalDate>? = null,
    onRangeSelected: (LocalDate, LocalDate) -> Unit,
    currentStart: LocalDate,
    currentEnd: LocalDate
) {
    val today = LocalDate.now()
    val options = mutableListOf(
        "7D" to today.minusDays(7) to today,
        "1M" to today.minusMonths(1) to today,
        "3M" to today.minusMonths(3) to today,
    )

    prevRange?.let {
        options.add("PREV" to it.first to it.second)
    }

    options.add("ALL" to minDate to today)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (labelRange, range) ->
            val (label, start) = labelRange
            val end = range
            val isSelected = currentStart == start && currentEnd == end
            val animatedColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primaryContainer)
            val animatedContentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.tertiary)
            
            Surface(
                onClick = { onRangeSelected(start, end) },
                shape = RoundedCornerShape(12.dp),
                color = animatedColor,
                contentColor = animatedContentColor,
                modifier = Modifier
                    .height(36.dp)
                    .weight(1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        fontFamily = plusJak)
                }
            }
        }
    }
}


@Composable
fun RangeSummaryItem(
    modifier : Modifier = Modifier,
    label: String,
    value: String,
    backgroundColor: Color,
    textColor: Color,
    shape : RoundedCornerShape
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = plusJak,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(5.dp))
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut())
            }
        ) { targetValue ->
            Text(
                text = targetValue,
                fontSize = 26.sp,
                fontFamily = plusJak,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun StatsSmallCard(
    modifier : Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(vertical = 20.dp, horizontal = 18.dp)
            .height(120.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(24.dp),
            tint = PrimaryColor
        )
        Column(){
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                            scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                }
            ) { targetTitle ->
                Text(
                    text = targetTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = plusJak,
                    color = PrimaryColor,
                    modifier = Modifier.padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(90))
                }
            ) { targetSubtitle ->
                Text(
                    text = targetSubtitle,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontFamily = plusJak,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(5.dp)
                )
            }
        }
    }
}

@Composable
fun FuturePredictionCard(predictionText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Future Prediction",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = plusJak
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = predictionText,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            }
        ) { targetText ->
            Text(
                text = targetText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = plusJak,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val displayThreshold = remember(predictionText) {
                    val match = """(\d+)%""".toRegex().find(predictionText)
                    match?.groupValues?.get(1) ?: "75"
                }
                Text(
                    text = "Threshold: $displayThreshold%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = plusJak
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun DateRangePickerDialogPreview() {
    JSS_STUDENTTheme {
        DateRangePickerDialog(
            initialStart = LocalDate.now(),
            initialEnd = LocalDate.now().plusDays(7),
            onDismiss = {},
            onRangeSelected = { _, _ -> }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun BunkAnalyticsPreview() {
    JSS_STUDENTTheme {
        val sampleSubject = Schedule(
            subjectId = 1,
            subject = "Data Structures",
            teacher = "Dr. Jain",
            scheduleday = emptyList(),
            color = 0xFFBB86FC,
            totalClasses = 10
        )
        val today = LocalDate.now()
        val sampleAttendance = listOf(
            ClassSchedule(subjectOwnerId = 1, date = today.minusDays(1).toString(), attendanceStatus = 1, day = "Monday"),
            ClassSchedule(subjectOwnerId = 1, date = today.minusDays(2).toString(), attendanceStatus = 2, day = "Sunday"),
            ClassSchedule(subjectOwnerId = 1, date = today.minusDays(3).toString(), attendanceStatus = 1, day = "Saturday"),
            ClassSchedule(subjectOwnerId = 1, date = today.minusDays(4).toString(), attendanceStatus = 1, day = "Friday"),
            ClassSchedule(subjectOwnerId = 1, date = today.minusDays(5).toString(), attendanceStatus = 1, day = "Thursday"),
            ClassSchedule(subjectOwnerId = 1, date = today.minusDays(6).toString(), attendanceStatus = 2, day = "Wednesday")
        )
        Surface(color = MaterialTheme.colorScheme.background) {
            BunkAnalyticsContent(
                subject = sampleSubject,
                attendanceHistory = sampleAttendance,
                desiredAttendance = 75f
            )
        }
    }
}
