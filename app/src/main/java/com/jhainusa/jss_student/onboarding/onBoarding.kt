package com.jhainusa.jss_student.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jhainusa.jss_student.GeminiBackend.sendImageToSupabase
import com.jhainusa.jss_student.LottieLoader
import com.jhainusa.jss_student.R
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.UserPref.NameViewModel
import com.jhainusa.jss_student.plusJak
import com.jhainusa.jss_student.UserPref.UserPreferences
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class OnboardingPage(
    val title: String,
    val description: String,
    val buttonText: String,
    val imageRes: Int
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "AI Timetable\nOrganizer",
        description = "Upload your timetable image and\nlet AI organize it instantly.",
        buttonText = "Continue",
        imageRes = R.drawable.img1 // Placeholder
    ),
    OnboardingPage(
        title = "Swipe to\nMark Attendance",
        description = "Mark attendance with a simple swipe\nright for present, left for absent.",
        buttonText = "Continue",
        imageRes = R.drawable.mark_attendance_with_swipe_gestures // Placeholder
    ),
    OnboardingPage(
        title = "Bunk\nAnalytics",
        description = "See you attendance summary and know if\nit's safe to bunk your next class.",
        buttonText = "Continue",
        imageRes = R.drawable.bunk_image // Placeholder
    ),
    OnboardingPage(
        title = "Smart\nNotifications",
        description = "Get timely notifications for your upcoming\nclasses and stay on track effortlessly.",
        buttonText = "Continue",
        imageRes = R.drawable.smartnotif // Placeholder
    ),
    OnboardingPage(
        title = "Upload Your\nTimetable",
        description = "Finally, upload your timetable image\nto let AI organize your schedule.",
        buttonText = "Upload & Finish",
        imageRes = R.drawable.upload_square_svgrepo_com
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: MainVIewModel? = null,
    nameViewModel: NameViewModel = viewModel(),
    onFinish: () -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userId by nameViewModel.userIdFlow.collectAsState()
    val username by nameViewModel.nameFlow.collectAsState()
    var loading by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.jhainusa.jss_student.AnalyticsHelper.logScreenView("OnboardingScreen", "Onboarding")
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            loading = true
            if (viewModel != null) {
                sendImageToSupabase(
                    context = context,
                    uri = it,
                    userIdStr = userId ?: "unknown_user",
                    viewModel = viewModel,
                    username = username ?: "unknown_name"
                ) { success ->
                    loading = false
                    if (success) {
                        onFinish()
                    }
                }
            } else {
                loading = false
                // Fallback for preview or missing viewModel
                onFinish()
            }
        }
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Color.White,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 24.dp)
            ) {
                TextButton(
                    onClick = {
                        onSkip()
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "Skip >",
                        fontSize = 16.sp,
                        fontFamily = plusJak,
                        color = Color.Gray
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) { pageIndex ->
                val pageOffset = (
                        (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                        ).absoluteValue

                val scale = lerp(
                    start = 0.85f,
                    stop = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                )

                val alpha = lerp(
                    start = 0.5f,
                    stop = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha

                        },
                    contentAlignment = Alignment.Center
                ) {
                    OnboardingContent(
                        page = onboardingPages[pageIndex],
                        onImageClick = {
                            if (pageIndex == onboardingPages.size - 1) {
                                launcher.launch("image/*")
                            }
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 60.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(onboardingPages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val pageOffset = (
                                (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction
                                ).absoluteValue

                        val scale = lerp(
                            start = 0.85f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )

                        val alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 24.dp else 8.dp)
                                .height(8.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha

                                }
                                .background(
                                    color = if (isSelected) Color(0xFF262626) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            launcher.launch("image/*")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF2E2E33),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = onboardingPages[pagerState.currentPage].buttonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = plusJak
                    )
                }
            }
        }
    }

    if (loading) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            LottieLoader("AI is processing your timetable...\nAI can make mistakes so please recheck it", R.raw.handloader)
        }
    }
}

@Composable
fun OnboardingContent(page: OnboardingPage, onImageClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image or Upload Section
        if (page.buttonText == "Upload & Finish") {
            UploadBox(
                modifier = Modifier.padding(vertical = 20.dp),
                onClick = onImageClick
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = page.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = plusJak,
            color = Color.Black,
            textAlign = TextAlign.Center,
            lineHeight = 44.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = page.description,
            fontSize = 15.sp,
            fontFamily = plusJak,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun UploadBox(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val stroke = Stroke(
        width = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFD1D5DB),
                    style = stroke,
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF9FAFB))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.uploadimg),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Upload your image here",
                fontFamily = plusJak,
                fontSize = 16.sp,
                color = Color(0xFF4B5563)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF3F4F6),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Text(
                    text = "Browse",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    fontFamily = plusJak,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen()
}
