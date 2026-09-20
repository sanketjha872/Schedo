package com.jhainusa.jss_student

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.UserPref.NameViewModel
import com.jhainusa.jss_student.ui.theme.SubtitleGray


@Composable
fun MoreOptionsScreen(
    nameViewModel: NameViewModel = viewModel(),
    mainViewModel: MainVIewModel,
    moreOptionsViewModel: MoreOptionsViewModel = viewModel(),
    onBackClick: () -> Unit = {},
) {
    val subjectsList by mainViewModel.getAll().observeAsState(emptyList())
    val userName by nameViewModel.nameFlow.collectAsState()
    val notificationsEnabled by nameViewModel.notificationsEnabledFlow.collectAsState()
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkModeEnabled by nameViewModel.darkModeFlow.collectAsState(initial = systemInDarkTheme)
    val desiredAttendance by nameViewModel.desiredAttendanceFlow.collectAsState()
    val showFeedbackDialog by moreOptionsViewModel.showFeedbackDialog.collectAsState()
    val showAttendanceDialog by moreOptionsViewModel.showAttendanceDialog.collectAsState()
    val showNameDialog by moreOptionsViewModel.showNameDialog.collectAsState()
    val feedbackType by moreOptionsViewModel.feedbackType.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            moreOptionsViewModel.importSchedule(context, it, mainViewModel)
        }
    }

    LaunchedEffect(Unit) {
        AnalyticsHelper.logScreenView("MoreOptions", "MoreOptions")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MoreOptionsTopBar(onBackClick) },
        modifier = Modifier.statusBarsPadding()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Heading
            item {
                Text(
                    text = "Settings",
                    fontSize = 36.sp,
                    fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 40.sp
                )
            }

            // Premium Card
            item {
                PremiumCard()
            }

            // Support Section
            item {
                SettingsSection(
                    title = "Support & Preferences",
                    items = listOf(
                        MoreOptionItem(
                            title = "Name",
                            onClick = { moreOptionsViewModel.showNameDialog() }
                        ) {
                            Text(
                                text = userName ?: "Not Set",
                                fontSize = 18.sp,
                                fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        MoreOptionItem(
                            title = "Desired Attendance",
                            onClick = { moreOptionsViewModel.showAttendanceDialog() }
                        ) {
                            Text(
                                text = "${desiredAttendance.toInt()}%",
                                fontSize = 18.sp,
                                fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        MoreOptionItem("Report a Bug", onClick = {
                            moreOptionsViewModel.showFeedbackDialog("Bug Report")
                        }),
                        MoreOptionItem("Suggest a Feature", onClick = {
                            moreOptionsViewModel.showFeedbackDialog("Feature Suggestion")
                        }),
                        MoreOptionItem("Talk to founder / Support", onClick = {
                            moreOptionsViewModel.openInstagram(context)
                        }),
                        MoreOptionItem("Share Schedule File", onClick = {
                            moreOptionsViewModel.exportSchedule(context, subjectsList, userName)
                        }),
                        MoreOptionItem("Import Shared Schedule File", onClick = {
                            filePickerLauncher.launch("*/*")
                        }),

                        MoreOptionItem(
                            title = "Dark Mode",
                            onClick = { moreOptionsViewModel.toggleDarkMode(nameViewModel, !darkModeEnabled) }
                        ) {
                            Switch(
                                checked = darkModeEnabled,
                                onCheckedChange = { moreOptionsViewModel.toggleDarkMode(nameViewModel, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.background,
                                    checkedTrackColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        },

                        MoreOptionItem(
                            title = "Notifications",
                            onClick = { moreOptionsViewModel.toggleNotifications(nameViewModel, !notificationsEnabled) }
                        ) {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { moreOptionsViewModel.toggleNotifications(nameViewModel, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.background,
                                    checkedTrackColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    ),
                    bgColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            // About Schedo Section
            item {
                SettingsSection(
                    title = "About Schedo",
                    items = listOf(
                        MoreOptionItem("Rate Schedo", onClick = {
                            moreOptionsViewModel.openPlayStore(context)
                        }),
                        MoreOptionItem("Check for Updates", onClick = {
                            moreOptionsViewModel.openPlayStore(context)
                        }),
                        MoreOptionItem("Privacy Policy", onClick = {
                            moreOptionsViewModel.openPrivacyPolicy(context)
                        }),
                        MoreOptionItem("Share App", onClick = {
                            moreOptionsViewModel.shareApp(context)
                        })
                    ),
                    bgColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            type = feedbackType,
            onDismiss = { moreOptionsViewModel.dismissFeedbackDialog() },
            onSubmit = { message ->
                moreOptionsViewModel.submitFeedback(
                    context = context,
                    nameViewModel = nameViewModel,
                    type = feedbackType,
                    message = message
                )
            }
        )
    }

    if (showAttendanceDialog) {
        AttendanceDialog(
            currentAttendance = desiredAttendance,
            onDismiss = { moreOptionsViewModel.dismissAttendanceDialog() },
            onConfirm = { newValue ->
                moreOptionsViewModel.saveDesiredAttendance(nameViewModel, newValue)
            }
        )
    }

    if (showNameDialog) {
        NameEditDialog(
            currentName = userName ?: "",
            onDismiss = { moreOptionsViewModel.dismissNameDialog() },
            onConfirm = { newName ->
                moreOptionsViewModel.saveName(nameViewModel, newName)
            }
        )
    }
}

@Composable
fun NameEditDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Change Name",
                fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun AttendanceDialog(
    currentAttendance: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var attendance by remember { mutableFloatStateOf(currentAttendance) }

    AnimatedDialog(showDialog = true, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(10.dp))
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Desired Attendance",
                fontSize = 22.sp,
                fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set your target attendance percentage",
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.plusjakartasansmedium)),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "${attendance.toInt()}%",
                fontSize = 48.sp,
                fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            androidx.compose.material3.Slider(
                value = attendance,
                onValueChange = { attendance = it },
                valueRange = 0f..100f,
                steps = 100,
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onConfirm(attendance) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Changes", fontFamily = FontFamily(Font(R.font.plusjakartasansbold)), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    type: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    AnimatedDialog(showDialog = true, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Text(
                text = type,
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = message,
                onValueChange = { message = it },
                placeholder = {
                    Text(
                        "Tell us more...",
                        fontFamily = FontFamily(Font(R.font.plusjakartasansmedium))
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily(Font(R.font.plusjakartasansmedium)),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSubmit(message) },
                enabled = message.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Submit Feedback",
                    fontFamily = FontFamily(Font(R.font.plusjakartasansbold)),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MoreOptionsTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun PremiumCard() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                Toast.makeText(context, "Premium features coming soon!", Toast.LENGTH_SHORT).show()
            },
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFE0ECF5),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF262626), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Premium",
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.plusjakartasansmedium)),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF323131)

                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Explore all features",
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.plusjakartasansmedium)),
                    fontWeight = FontWeight.SemiBold,
                    color = SubtitleGray
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, items: List<MoreOptionItem>, bgColor: Color) {
    val plusJak = FontFamily(Font(R.font.plusjakartasansmedium))
    Column {
        Text(
            text = title,
            fontSize = 15.sp,
            fontFamily = plusJak,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 14.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = bgColor,
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsRow(item = item)
                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 1.5.dp,
                            color = MaterialTheme.colorScheme.background
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRow(item: MoreOptionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 63.dp)
            .clickable { item.onClick() }
            .padding(horizontal = 20.dp, vertical = if (item.trailingContent != null) 10.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.title,
            fontSize = 16.sp,
            fontFamily = FontFamily(Font(R.font.plusjakartasansmedium)),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        item.trailingContent?.invoke()
    }
}

data class MoreOptionItem(
    val title: String,
    val onClick: () -> Unit,
    val trailingContent: @Composable (() -> Unit)? = null
)
