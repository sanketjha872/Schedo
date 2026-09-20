package com.jhainusa.jss_student

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.jhainusa.jss_student.RoomDatabase.DaySchedule
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.RoomDatabase.Schedule
import com.jhainusa.jss_student.UserPref.UserPreferences
import com.jhainusa.jss_student.ciaPaperPage.SBar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UploadTimeTableScreen(viewModel: MainVIewModel, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        AnalyticsHelper.logScreenView("SettingsScreen", "UploadTimeTable")
    }

    val subjectsList by viewModel.getAll().observeAsState(emptyList())
    var searchSubject by remember { mutableStateOf("") }

    // Tooltip logic
    var isTooltipVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val shown = UserPreferences.isBunkTooltipShown(context).first()
        if(!shown) isTooltipVisible = true
    }

    var selectedSubjectForHistory by remember { mutableStateOf<Schedule?>(null) }


    var showHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Schedule?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var isAiLoading by remember { mutableStateOf(false) }
    val globalLazyListState = rememberLazyListState()

    var isBottomBarAndFabVisible by remember { mutableStateOf(true) }
    var previousFirstVisibleItemIndex by remember { mutableIntStateOf(0) }
    var previousFirstVisibleItemScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(globalLazyListState) {
        snapshotFlow {
            globalLazyListState.firstVisibleItemIndex to globalLazyListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            if (globalLazyListState.isScrollInProgress) {
                if (index > previousFirstVisibleItemIndex) {
                    isBottomBarAndFabVisible = false
                } else if (index < previousFirstVisibleItemIndex) {
                    isBottomBarAndFabVisible = true
                } else {
                    if (offset > previousFirstVisibleItemScrollOffset) {
                        isBottomBarAndFabVisible = false
                    } else if (offset < previousFirstVisibleItemScrollOffset) {
                        isBottomBarAndFabVisible = true
                    }
                }
            }
            previousFirstVisibleItemIndex = index
            previousFirstVisibleItemScrollOffset = offset
        }
    }


    val filteredList by remember(subjectsList, searchSubject) {
        derivedStateOf {
            if (searchSubject.isEmpty()) {
                subjectsList
            } else {
                subjectsList.filter { it.subject.contains(searchSubject, ignoreCase = true) }
            }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp,10.dp,0.dp,0.dp),
        floatingActionButton = {
            AnimatedVisibility(
                visible = isBottomBarAndFabVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("add_class")
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Subjects",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                    fontSize = 30.sp,
                )
                Box(
                    contentAlignment = Alignment.TopEnd
                ) {
                    DropdownMenuExample(
                        vIewModel = viewModel,
                        isEditMode = isEditMode,
                        onEditModeToggle = { isEditMode = !isEditMode },
                        navController = navController
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "College Semester 2026",
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily(Font(R.font.plusjakartasansregular)),
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(20.dp))

            SBar(searchSubject, placeholder = "Search subjects...", onQueryChange = { searchSubject = it })

            Spacer(modifier = Modifier.height(20.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(15.dp),
                state = globalLazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList) { sub ->
                    val isFirst = filteredList.indexOf(sub) == 0

                    val transition = rememberInfiniteTransition(label = "pulse")
                    val scale by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isFirst && isTooltipVisible) 1.02f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "scale"
                    )

                    Column(modifier = Modifier.scale(scale)) {
                        if (isFirst && isTooltipVisible) {
                            BunkTooltip(
                                visible = isTooltipVisible,
                                onDismiss = {
                                    isTooltipVisible = false
                                    scope.launch { UserPreferences.setBunkTooltipShown(context) }
                                },
                                text = "Tap to see bunk analytics"
                            )
                        }

                        SubjectCard(
                            subname = sub.subject,
                            teacher = sub.teacher,
                            daysSchedule = sub.scheduleday,
                            color = Color(sub.color.toULong()),
                            isEditMode = isEditMode,
                            onClick = {
                                if (isTooltipVisible) {
                                    isTooltipVisible = false
                                    scope.launch { UserPreferences.setBunkTooltipShown(context) }
                                }
                                if (!isEditMode) {
                                    navController.navigate("bunk_analytics/${sub.subjectId}")
                                }
                            },
                            onEditClick = {
                                navController.navigate("add_class?subjectId=${sub.subjectId}")
                            },
                            onDeleteClick = {
                                subjectToDelete = sub
                                showDeleteDialog = true
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    if (isAiLoading) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LottieLoader("AI is processing your timetable...\n Ai can make mistakes so please check it", R.raw.handloader)
        }
    }

    if (showDeleteDialog && subjectToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                subjectToDelete = null
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the subject \"${subjectToDelete?.subject}\"?",
                    fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                    style = TextStyle(
                        lineHeight = 24.sp,
                        color = Color.DarkGray
                    ),
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        subjectToDelete?.let { viewModel.deleteSchedule(it) }
                        showDeleteDialog = false
                        subjectToDelete = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = Color(0xFFF13D3D),
                        fontFamily = plusJak,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        subjectToDelete = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.Black,
                        fontFamily = plusJak,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showHistoryDialog && selectedSubjectForHistory != null) {
        AnimatedDialog(showDialog = showHistoryDialog, onDismiss = { showHistoryDialog = false }) {
            AttendanceHistoryDialog(
                viewModel = viewModel,
                subject = selectedSubjectForHistory!!,
                onDismiss = { showHistoryDialog = false }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AttendanceHistoryDialog(
    viewModel: MainVIewModel,
    subject: Schedule,
    onDismiss: () -> Unit
) {
    val history by viewModel.getAttendanceHistory(subject.subjectId).observeAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Attendance History",
            fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
            fontSize = 20.sp,
            color = Color.Black
        )
        Text(
            text = subject.subject,
            fontFamily = plusJak,
            fontSize = 16.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No attendance marked yet.",
                    fontFamily = plusJak,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val formattedDate = try {
                                java.time.LocalDate.parse(record.date).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
                            } catch (e: Exception) { record.date }
                            Text(
                                text = formattedDate,
                                fontFamily = plusJak,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = record.day,
                                fontFamily = plusJak,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (record.attendanceStatus) {
                                        1 -> Color(0xFFE8F5E9)
                                        2 -> Color(0xFFFFEBEE)
                                        3 -> Color(0xFFE3F2FD)
                                        else -> Color.LightGray
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (record.attendanceStatus) {
                                    1 -> "Present"
                                    2 -> "Absent"
                                    3 -> "Holiday"
                                    else -> "Unmarked"
                                },
                                color = when (record.attendanceStatus) {
                                    1 -> Color(0xFF2E7D32)
                                    2 -> Color(0xFFC62828)
                                    3 -> Color(0xFF1565C0)
                                    else -> Color.DarkGray
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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

@Composable
fun SubjectCard(
    subname: String,
    teacher: String,
    color: Color,
    daysSchedule: List<DaySchedule>,
    isEditMode: Boolean = false,
    onClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(color)
                .clickable { onClick() }
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subname,
                        fontFamily = FontFamily(Font(R.font.plusjakartasansmedium)),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF262626),
                        fontSize = 17.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = teacher,
                        fontFamily = plusJak,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }

                if (isEditMode) {
                    Row {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                painter = painterResource(R.drawable.edit_svgrepo_com),
                                contentDescription = "Edit",
                                tint = Color(0xFF262626),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = "Delete",
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            daysSchedule.forEach { schedule ->
                Text(
                    text = "${schedule.day}\t\t\t${schedule.timing}",
                    fontFamily = plusJak,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview
@Composable
fun SubjectCardPreview() {
    SubjectCard(
        subname = "Mathematics",
        teacher = "Mr. Smith",
        color = Color(0xF8DFECDE),
        daysSchedule = listOf(DaySchedule("Mon", "11:00-12:00")),
        onClick = {}
    )
}
