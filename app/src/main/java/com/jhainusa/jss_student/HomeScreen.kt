package com.jhainusa.jss_student

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.UserPref.UserPreferences
import com.jhainusa.jss_student.ui.theme.black1a
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val plusJak = FontFamily(
    Font(R.font.plus_jakarta)
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FullPAge(
    viewModel: MainVIewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val nameFlow = remember { UserPreferences.getName(context) }
    val name by nameFlow.collectAsState(initial = null)

    val desiredAttendanceFlow = remember { UserPreferences.getDesiredAttendance(context) }
    val desiredAttendance by desiredAttendanceFlow.collectAsState(initial = 75f)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        AnalyticsHelper.logScreenView("HomeScreen", "HomeScreen")
    }

    val subjectsList by viewModel.getAll().observeAsState(emptyList())
    val allAttendance by viewModel.getAllAttendance().observeAsState(emptyList())

    var showSubjectWiseDialog by remember { mutableStateOf(false) }

    val currentDay = remember {
        LocalDate.now().dayOfWeek.name.lowercase()
            .replaceFirstChar { it.uppercase() }.take(3) // "Mon", "Tue", etc.
    }

    val attendanceData by remember(allAttendance, subjectsList, desiredAttendance) {
        derivedStateOf {
            val relevantAttendance = allAttendance.filter { it.attendanceStatus == 1 || it.attendanceStatus == 2 }
            
            val totalMarked = relevantAttendance.size + subjectsList.sumOf { it.initialTotal }
            val presentCount = relevantAttendance.count { it.attendanceStatus == 1 } + subjectsList.sumOf { it.initialPresent }
            
            val percentage = if (totalMarked > 0) (presentCount.toFloat() / totalMarked * 100).toInt() else 0

            val status = when {
                totalMarked == 0 -> "No data yet"
                percentage >= desiredAttendance -> "Great job!"
                percentage >= desiredAttendance * 0.8f -> "Getting close"
                else -> "High Alert"
            }

            val greeting = when {
                totalMarked == 0 -> "Welcome,"
                percentage >= 90 -> "You're a Star,"
                percentage >= desiredAttendance -> "Nice Streak,"
                percentage >= desiredAttendance * 0.8f -> "Keep it up,"
                else -> "Stay Focused,"
            }
            Triple(percentage, status, greeting)
        }
    }

    val subjectWiseAttendance by remember(subjectsList, allAttendance) {
        derivedStateOf {
            subjectsList.map { subject ->
                val subjectRecords = allAttendance.filter { it.subjectOwnerId == subject.subjectId && (it.attendanceStatus == 1 || it.attendanceStatus == 2) }
                val totalMarked = subjectRecords.size + subject.initialTotal
                val presentCount = subjectRecords.count { it.attendanceStatus == 1 } + subject.initialPresent

                val percentage = if (totalMarked > 0) (presentCount.toFloat() / totalMarked * 100).toInt() else 0
                SubjectAttendanceData(
                    subjectId = subject.subjectId,
                    subjectName = subject.subject,
                    percentage = percentage,
                    totalClasses = totalMarked,
                    presentClasses = presentCount,
                    color = Color(subject.color.toULong())
                )
            }.sortedByDescending { it.percentage }
        }
    }

    val todayClasses by remember(subjectsList) {
        derivedStateOf {
            val now = LocalTime.now()
            subjectsList.flatMap { schedule ->
                schedule.scheduleday
                    .filter { it.day.startsWith(currentDay, ignoreCase = true) }
                    .map { daySchedule ->
                        TodayClassItem(
                            subject = schedule.subject,
                            teacher = schedule.teacher,
                            time = daySchedule.timing,
                            roomNo = schedule.roomNo,
                            color = Color(schedule.color.toULong())
                        )
                    }
            }
                .filter { parseEndTime(it.time).isAfter(now) } // Only upcoming or ongoing
                .sortedBy { parseStartTime(it.time) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp)) {
                GreetingHeader(attendanceData.third, name)
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                AttendanceOverview(
                    totalPercentage = "${attendanceData.first}%",
                    status = attendanceData.second,
                    onTotalClick = { showSubjectWiseDialog = true },
                )
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
                TodayClassesSection(todayClasses, onClick = {})
            }
        }
    }

    if (showSubjectWiseDialog) {
        AnimatedDialog(showDialog = showSubjectWiseDialog, onDismiss = { showSubjectWiseDialog = false }) {
            SubjectWiseAttendanceDialogContent(
                attendanceList = subjectWiseAttendance,
                desiredAttendance = desiredAttendance,
                onDismiss = { showSubjectWiseDialog = false },
                onItemClick = { subject ->
                    showSubjectWiseDialog = false
                    navController.navigate("bunk_analytics/${subject.subjectId}")
                }
            )
        }
    }
}

data class TodayClassItem(
    val subject: String,
    val teacher: String,
    val time: String,
    val roomNo: String,
    val color: Color
)

data class SubjectAttendanceData(
    val subjectId: Int,
    val subjectName: String,
    val percentage: Int,
    val totalClasses: Int,
    val presentClasses: Int,
    val color: Color
)

@RequiresApi(Build.VERSION_CODES.O)
fun parseStartTime(timeRange: String): LocalTime {
    return try {
        val startTimeStr = timeRange.split("-").first().trim()
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        LocalTime.parse(startTimeStr, formatter)
    } catch (e: Exception) {
        LocalTime.MIDNIGHT
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun parseEndTime(timeRange: String): LocalTime {
    return try {
        val endTimeStr = timeRange.split("-").last().trim()
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        LocalTime.parse(endTimeStr, formatter)
    } catch (e: Exception) {
        LocalTime.MAX
    }
}

@Composable
fun GreetingHeader(
    greeting: String,
    name: String?
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$greeting\n$name",
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily(Font(R.font.plusjakartasansbold))
        )

        Button(
            onClick = {
                AnalyticsHelper.logEvent("invite_friends")
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Hey, check out this cool app to manage your attendance: https://play.google.com/store/apps/details?id=${context.packageName}"
                    )
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share via"))

            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.add_plus_svgrepo_com),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Invite\nFriends",
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                textAlign = TextAlign.Center,
                fontSize = 14.sp)
        }
    }
}

@Composable
fun TodayClassesSection(classes: List<TodayClassItem>, onClick : () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Today classes",
                fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 19.sp
            )
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(8.dp)
                    .clickable(onClick = onClick)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_forward_24),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(15.dp))

        if (classes.isEmpty()) {
            Text(
                text = "No upcoming classes for today",
                fontFamily = plusJak,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(0.65f),
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            classes.forEach { item ->
                classComp(
                    time = item.time,
                    teacher = item.teacher,
                    subject = item.subject,
                    roomNo = item.roomNo,
                    icon = painterResource(R.drawable.baseline_code_24), // Default icon
                    color = item.color,
                    onColorChange = {}
                )
            }
        }
    }
}

@Composable
fun classComp(
    time: String = "",
    teacher: String,
    subject: String,
    roomNo : String = "",
    icon: Painter,
    color: Color,
    onColorChange: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .clickable(
                onClick = {
                    onColorChange()
                }
            )
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = subject,
                fontFamily = FontFamily(
                    Font(R.font.plusjakartasansmedium)
                ),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = time + "\t\t\t" + teacher + "\t\t\t" + roomNo,
                fontFamily = plusJak,
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
        Box(
            modifier = Modifier
                .padding(5.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(8.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.DarkGray
            )
        }
    }
    Spacer(modifier = Modifier.height(15.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttendanceOverview(
    totalPercentage: String,
    status: String,
    onTotalClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AttendancePerBox(
            "Total\nAttendance",
            totalPercentage,
            "Overall Score",
            Color(0XFFF8E9C8), // Light Yellowish
            Modifier.weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .combinedClickable(
                    onClick = onTotalClick,
                )
        )

        AttendancePerBox(
            "Attendance\nStatus",
            status,
            "Today's Insight",
            Color(0XFFDEECEC), // Light Blue/Greenish
            Modifier.weight(1f)
        )
    }
}

@Composable
fun AttendancePerBox(
    title: String,
    per: String,
    streak: String,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(0.83f)
            .clip(RoundedCornerShape(32.dp))
            .background(bg)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
            fontSize = 16.sp,
            color = black1a,
            fontWeight = FontWeight.Bold,
            lineHeight = 20.sp
        )

        Text(
            text = per,
            fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
            fontSize = if (per.length > 8) 22.sp else 42.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.8f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = streak,
                fontFamily = plusJak,
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubjectWiseAttendanceDialogContent(
    attendanceList: List<SubjectAttendanceData>,
    desiredAttendance: Float,
    onDismiss: () -> Unit,
    onItemClick: (SubjectAttendanceData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Subject-wise Attendance",
            fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (attendanceList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No subjects found.",
                    fontFamily = plusJak,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(attendanceList) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(item.color.copy(alpha = 0.3f))
                            .clickable { onItemClick(item) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.subjectName,
                                fontFamily = plusJak,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${item.presentClasses}/${item.totalClasses} classes attended",
                                fontFamily = plusJak,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Text(
                            text = "${item.percentage}%",
                            fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = if (item.percentage >= desiredAttendance) Color(0xFF0FB517) else Color(
                                0xFFEE2828
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Close",
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onDismiss() }
                .padding(8.dp),
            fontFamily = plusJak,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF262626)
        )
    }
}
