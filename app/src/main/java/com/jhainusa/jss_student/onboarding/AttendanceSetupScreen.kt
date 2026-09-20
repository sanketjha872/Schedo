package com.jhainusa.jss_student.onboarding

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import com.jhainusa.jss_student.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhainusa.jss_student.DateRangePickerDialog
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.RoomDatabase.Schedule
import com.jhainusa.jss_student.UserPref.UserPreferences
import com.jhainusa.jss_student.plusJak
import com.jhainusa.jss_student.ui.theme.NavyText
import com.jhainusa.jss_student.ui.theme.PlusJakartaSans
import com.jhainusa.jss_student.ui.theme.SubtitleGray
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AttendanceSetupScreen(
    viewModel: MainVIewModel,
    onFinish: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AttendanceSetupScreenContent(viewModel, onFinish)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AttendanceSetupScreenContent(
    viewModel: MainVIewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subjects by viewModel.getAll().observeAsState(emptyList())

    AttendanceSetupContent(
        subjects = subjects,
        onCompleteSetup = { perSubjectData, startDate, endDate ->
            scope.launch {
                perSubjectData.forEach { (id, pair) ->
                    val present = pair.first.toIntOrNull() ?: 0
                    val total = pair.second.toIntOrNull() ?: 0
                    viewModel.updateInitialAttendance(id, present, total)
                }
                
                startDate?.let { UserPreferences.setSemesterStartDate(context, it.toString()) }
                endDate?.let { UserPreferences.setSemesterEndDate(context, it.toString()) }

                UserPreferences.setInitialAttendanceDone(context, true)
                onFinish()
            }
        },
        onFinishToday = {
            scope.launch {
                UserPreferences.setInitialAttendanceDone(context, true)
                onFinish()
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AttendanceSetupContent(
    subjects: List<Schedule>,
    initialStep: Int = 1,
    onCompleteSetup: (Map<Int, Pair<String, String>>, LocalDate?, LocalDate?) -> Unit,
    onFinishToday: () -> Unit
) {
    var step by remember { mutableIntStateOf(initialStep) }
    
    // Step 1 state
    var startFromToday by remember { mutableStateOf(true) }
    
    // Step 2 state
    var initialAttendanceData by remember { mutableStateOf<Map<Int, Pair<String, String>>>(emptyMap()) }

    var semesterStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var semesterEndDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(subjects) {
        if (initialAttendanceData.isEmpty() && subjects.isNotEmpty()) {
            initialAttendanceData = subjects.associate { it.subjectId to Pair("", "") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    if (step > 1) {
                        IconButton(onClick = { step-- }) {
                            Icon(painter = painterResource(R.drawable.arrow_prev_small_svgrepo_com), contentDescription = "Back",
                                modifier = Modifier.size(32.dp), tint = Color.DarkGray)
                        }
                    }
                },
                backgroundColor = Color.White,
                contentColor = NavyText,
                elevation = 0.dp,
                modifier = Modifier.statusBarsPadding()
                    .padding(top = 5.dp,start = 4.dp,end = 16.dp)
            )
        },
        modifier = Modifier.navigationBarsPadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 18.dp,end = 18.dp, bottom = 16.dp)
        ) {
            when (step) {
                1 -> ModeSelection(
                    startFromToday = startFromToday,
                    onModeSelected = { startFromToday = it },
                    onNext = {
                        if (startFromToday) {
                            onFinishToday()
                        } else {
                            step = 2
                        }
                    }
                )
                2 -> InitialAttendanceInput(
                    subjects = subjects,
                    initialData = initialAttendanceData,
                    onDataChange = { id, data ->
                        initialAttendanceData = initialAttendanceData.toMutableMap().apply {
                            this[id] = data
                        }
                    },
                    startDate = semesterStartDate,
                    endDate = semesterEndDate,
                    onDateRangeChange = { start, end ->
                        semesterStartDate = start
                        semesterEndDate = end
                    },
                    onFinish = {
                        onCompleteSetup(initialAttendanceData, semesterStartDate, semesterEndDate)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Step 1: Mode")
@Composable
fun AttendanceSetupStep1Preview() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AttendanceSetupContent(
            subjects = emptyList(),
            initialStep = 1,
            onCompleteSetup = { _, _, _ -> },
            onFinishToday = {}
        )
    }
}

@Preview(showBackground = true, name = "Step 2: Initial Data")
@Composable
fun AttendanceSetupStep2Preview() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val demoSubjects = listOf(
            Schedule(
                subjectId = 1,
                subject = "Mathematics",
                teacher = "Dr. Amit Sharma",
                scheduleday = emptyList()
            ),
            Schedule(
                subjectId = 2,
                subject = "Computer Networks",
                teacher = "Prof. Priya Verma",
                scheduleday = emptyList()
            ),
            Schedule(
                subjectId = 3,
                subject = "Operating Systems",
                teacher = "Dr. Raj Singh",
                scheduleday = emptyList()
            ),
            Schedule(
                subjectId = 2,
                subject = "Computer Networks",
                teacher = "Prof. Priya Verma",
                scheduleday = emptyList()
            ),
            Schedule(
                subjectId = 2,
                subject = "Computer Networks",
                teacher = "Prof. Priya Verma",
                scheduleday = emptyList()
            ),
            Schedule(
                subjectId = 2,
                subject = "Computer Networks",
                teacher = "Prof. Priya Verma",
                scheduleday = emptyList()
            ),
            Schedule(
                subjectId = 2,
                subject = "Computer Networks",
                teacher = "Prof. Priya Verma",
                scheduleday = emptyList()
            ),
            Schedule(
                subjectId = 2,
                subject = "Computer Networks",
                teacher = "Prof. Priya Verma",
                scheduleday = emptyList()
            ),
            Schedule(
                subjectId = 2,
                subject = "Computer Networks",
                teacher = "Prof. Priya Verma",
                scheduleday = emptyList()
            ),
        )
        AttendanceSetupContent(
            subjects = demoSubjects,
            initialStep = 2,
            onCompleteSetup = { _, _, _ -> },
            onFinishToday = {}
        )
    }
}

@Composable
fun ModeSelection(
    startFromToday: Boolean,
    onModeSelected: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Attendance History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            color = NavyText
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Are you joining mid-semester?",
            fontSize = 18.sp,
            fontFamily = PlusJakartaSans,
            color = SubtitleGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        SelectionCard(
            title = "Start from today",
            description = "I'm starting my semester now or don't want to add previous data.",
            isSelected = startFromToday,
            onClick = { onModeSelected(true) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SelectionCard(
            title = "Add previous attendance",
            description = "Enter classes already held to keep your analytics accurate.",
            isSelected = !startFromToday,
            onClick = { onModeSelected(false) }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF2E2E33),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (startFromToday) "Finish Setup" else "Next",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = plusJak
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun InitialAttendanceInput(
    subjects: List<Schedule>,
    initialData: Map<Int, Pair<String, String>>,
    onDataChange: (Int, Pair<String, String>) -> Unit,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateRangeChange: (LocalDate?, LocalDate?) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Previous Attendance",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = PlusJakartaSans,
            color = NavyText,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter classes attended and total classes held for each subject so far.",
            fontSize = 16.sp,
            fontFamily = plusJak,
            color = SubtitleGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Date Range Selector
        Card(
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            onClick = {showDatePicker = true},
            modifier = Modifier
                .fillMaxWidth(),
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.calendar_svgrepo_com),
                    contentDescription = null,
                    tint = NavyText,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Previous Attendance Duration",
                        fontSize = 12.sp,
                        fontFamily = plusJak,
                        color = SubtitleGray
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (startDate != null && endDate != null) {
                            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
                            "${startDate.format(formatter)} - ${endDate.format(formatter)}"
                        } else "Select start and end date",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = plusJak,
                        color = NavyText
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.arrow_prev_small_svgrepo_com),
                    contentDescription = null,
                    tint = SubtitleGray,
                    modifier = Modifier.size(22.dp).rotate(270f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.weight(1.8f))
            Text(
                text = "Attended",
                fontSize = 12.sp,
                fontFamily = plusJak,
                fontWeight = FontWeight.SemiBold,
                color = SubtitleGray,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Text(
                text = "Total",
                fontSize = 12.sp,
                fontFamily = plusJak,
                fontWeight = FontWeight.SemiBold,
                color = SubtitleGray,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            subjects.forEachIndexed { index, subject ->
                val isLast = index == subjects.size - 1
                SubjectAttendanceInput(
                    subjectName = subject.subject,
                    data = initialData[subject.subjectId] ?: Pair("", ""),
                    onDataChange = { onDataChange(subject.subjectId, it) },
                    isLastRow = isLast
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val hasError = initialData.values.any { (attended, total) ->
                    val a = attended.toIntOrNull() ?: 0
                    val t = total.toIntOrNull() ?: 0
                    a > t
                }

                if (hasError) {
                    Toast.makeText(context, "Attended classes cannot exceed total classes", Toast.LENGTH_SHORT).show()
                } else if (startDate == null || endDate == null) {
                    Toast.makeText(context, "Please select semester start and end dates", Toast.LENGTH_SHORT).show()
                } else {
                    onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF2E2E33),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Complete Setup", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = plusJak)
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            text = "Previous Attendance range",
            initialStart = startDate ?: LocalDate.now(),
            initialEnd = endDate ?: LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onRangeSelected = { start, end ->
                onDateRangeChange(start, end)
                showDatePicker = false
            }
        )
    }
}

@Composable
fun SubjectAttendanceInput(
    subjectName: String,
    data: Pair<String, String>,
    onDataChange: (Pair<String, String>) -> Unit,
    isLastRow: Boolean = false
) {
    val isError = remember(data) {
        val attended = data.first.toIntOrNull() ?: 0
        val total = data.second.toIntOrNull() ?: 0
        attended > total
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = subjectName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = plusJak,
            color = NavyText,
            modifier = Modifier.weight(1.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        OutlinedTextField(
            value = data.first,
            onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) onDataChange(it to data.second) },
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            isError = isError,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = if (isError) Color.Red else Color(0xFFCBD5E1),
                unfocusedBorderColor = if (isError) Color.Red else Color(0xFFE2E8F0),
                errorBorderColor = Color.Red,
                backgroundColor = Color.White,
                cursorColor = NavyText
            ),
            textStyle = TextStyle(
                fontSize = 15.sp, 
                fontFamily = plusJak,
                fontWeight = FontWeight.SemiBold,
                color = NavyText,
                textAlign = TextAlign.Center
            )
        )
        
        OutlinedTextField(
            value = data.second,
            onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) onDataChange(data.first to it) },
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = if (isLastRow) ImeAction.Done else ImeAction.Next
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            isError = isError,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = if (isError) Color.Red else Color(0xFFCBD5E1),
                unfocusedBorderColor = if (isError) Color.Red else Color(0xFFE2E8F0),
                errorBorderColor = Color.Red,
                backgroundColor = Color.White,
                cursorColor = NavyText
            ),
            textStyle = TextStyle(
                fontSize = 15.sp, 
                fontFamily = plusJak,
                fontWeight = FontWeight.SemiBold,
                color = NavyText,
                textAlign = TextAlign.Center
            )
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SelectionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = 0.dp,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF9DA7B2) else Color(0xFFE0E0E0)
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = if(isSelected) Color(0xFFEFF4F8) else Color.Transparent
    ) {
        Row(modifier = Modifier.padding(25.dp),
            verticalAlignment = Alignment.Top) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF464C52))
                )
            Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                    color = NavyText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    style = TextStyle(
                        lineHeight = 20.sp
                    ),
                    fontFamily = FontFamily(Font(R.font.plusjakartasansmedium)),
                    color = Color.DarkGray,
                )
            }

        }
    }
}
