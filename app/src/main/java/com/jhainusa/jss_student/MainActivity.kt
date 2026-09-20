package com.jhainusa.jss_student

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.jhainusa.jss_student.RoomDatabase.MainVIewModel
import com.jhainusa.jss_student.RoomDatabase.MainViewModelFactory
import com.jhainusa.jss_student.RoomDatabase.ScheduleDatabase
import com.jhainusa.jss_student.RoomDatabase.ScheduleRepository
import com.jhainusa.jss_student.UserPref.NameInputScreen
import com.jhainusa.jss_student.UserPref.UserPreferences
import com.jhainusa.jss_student.UserPref.UserSession
import com.jhainusa.jss_student.ciaPaperPage.InternalsListScreen
import com.jhainusa.jss_student.ciaPaperPage.PaperListScreen
import com.jhainusa.jss_student.ciaPaperPage.Papers
import com.jhainusa.jss_student.ciaPaperPage.Routes
import com.jhainusa.jss_student.ciaPaperPage.SemesterListScreen
import com.jhainusa.jss_student.onboarding.AttendanceSetupScreen
import com.jhainusa.jss_student.onboarding.DesiredAttendanceScreen
import com.jhainusa.jss_student.onboarding.OnboardingScreen
import com.jhainusa.jss_student.ui.theme.JSS_STUDENTTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var viewModel: MainVIewModel

    @OptIn(ExperimentalAnimationApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsHelper.init(this)
        AnalyticsHelper.logEvent(FirebaseAnalytics.Event.APP_OPEN)

        enableEdgeToEdge()

        val database = ScheduleDatabase.getDatabase(applicationContext)
        val dao = database.ScheduleDao()
        val classDao = database.classScheduleDao()
        val repository = ScheduleRepository(dao, classDao)
        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(repository)
        ).get(MainVIewModel::class.java)

        lifecycleScope.launch {
            UserSession.name = UserPreferences
                .getName(this@MainActivity)
                .first()
            val onboardingCompleted = UserPreferences.isOnboardingCompleted(this@MainActivity).first()
            val desiredAttendanceDone =
                UserPreferences.isDesiredAttendanceDone(this@MainActivity).first()
            val initialAttendanceDone = UserPreferences.isInitialAttendanceDone(this@MainActivity).first()
            val darkModeEnabled = UserPreferences.getDarkMode(this@MainActivity).first()

        setContent {
            val isDarkMode by UserPreferences.getDarkMode(LocalContext.current).collectAsState(initial = darkModeEnabled)
            JSS_STUDENTTheme(darkTheme = isDarkMode) {
            val context = LocalContext.current

            // 1. Define the Permission Launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (isGranted) {
                        // Permission granted: You can now show notifications
                        Toast.makeText(context, "Notifications Enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        // Permission denied: Explain to the user why you need it
                        Toast.makeText(context, "Notifications Disabled", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val isPermissionGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!isPermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            val scope = rememberCoroutineScope()
            val navController = rememberAnimatedNavController()

            val startDest = when {
                UserSession.name.isNullOrEmpty() -> "name_input"
                !onboardingCompleted -> "onboarding"
                !initialAttendanceDone -> "attendance_setup"
                !desiredAttendanceDone -> "desired_attendance"
                else -> "AllScreenNav"
            }

            AnimatedNavHost(navController,
                startDestination = startDest,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                }){
                composable("name_input") { NameInputScreen(navController=navController) }

                composable("onboarding"){
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinish = {
                            scope.launch {
                                UserPreferences.setOnboardingCompleted(context, true)
                                navController.navigate("attendance_setup") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        },
                        onSkip = {
                            scope.launch {
                                UserPreferences.skipOnboarding(context)
                                UserPreferences.setInitialAttendanceDone(context, true)
                                navController.navigate("AllScreenNav") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable("attendance_setup") {
                    AttendanceSetupScreen(
                        viewModel = viewModel,
                        onFinish = {
                            navController.navigate("desired_attendance") {
                                popUpTo("attendance_setup") { inclusive = true }
                            }
                        }
                    )
                }
                composable("desired_attendance") {
                    DesiredAttendanceScreen(
                        onFinish = {
                            navController.navigate("AllScreenNav") {
                                popUpTo("desired_attendance") { inclusive = true }
                            }
                        }
                    )
                }
                composable("AllScreenNav"){
                    AllScreenNav(viewModel,navController)
                }

                composable(
                    route = "${Routes.SEMESTER_LIST}/{yearId}",
                    arguments = listOf(navArgument("yearId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val yearId = backStackEntry.arguments?.getString("yearId") ?: return@composable
                    SemesterListScreen(
                        yearId = yearId,
                        onSemesterSelected = { semId ->
                            navController.navigate("${Routes.PAPER_LIST}/$yearId/$semId")
                        }
                    )
                }
                composable(
                    route = "${Routes.PAPER_LIST}/{yearId}/{semesterId}",
                    arguments = listOf(navArgument("yearId") { type = NavType.StringType },
                            navArgument("semesterId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val yearId = backStackEntry.arguments?.getString("yearId") ?: return@composable
                    val semesterId = backStackEntry.arguments?.getString("semesterId") ?: return@composable
                    InternalsListScreen(
                        yearId = yearId,
                        semId = semesterId,
                        onInternalSelected = { paperId ->
                            navController.navigate("${Routes.PDF_LIST}/$yearId/$semesterId/$paperId")
                        }
                    )
                }
                composable(
                    route = Routes.BUNK_ANALYTICS,
                    arguments = listOf(navArgument("subjectId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val subjectId = backStackEntry.arguments?.getInt("subjectId") ?: 0
                    BunkAnalyticsScreen(viewModel, subjectId)
                }

                composable(
                    route = Routes.ADD_CLASS,
                    enterTransition = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(300)
                        )
                    },
                    arguments = listOf(navArgument("subjectId") {
                        type = NavType.IntType
                        defaultValue = -1
                    })
                ) { backStackEntry ->
                    val subjectId = backStackEntry.arguments?.getInt("subjectId") ?: -1
                    AddClassScreen(
                        viewModel = viewModel,
                        subjectId = subjectId,
                        onDismiss = { navController.popBackStack() }
                    )
                }

                composable(Routes.MORE_OPTIONS) {
                    MoreOptionsScreen(
                        mainViewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "${Routes.PDF_LIST}/{yearId}/{semesterId}/{paperId}",
                    arguments = listOf(
                        navArgument("yearId") { type = NavType.StringType },
                        navArgument("semesterId") { type = NavType.StringType },
                        navArgument("paperId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val yearId = backStackEntry.arguments?.getString("yearId") ?: return@composable
                    val semesterId = backStackEntry.arguments?.getString("semesterId") ?: return@composable
                    val paperId = backStackEntry.arguments?.getString("paperId") ?: return@composable
                    PaperListScreen(yearId,semesterId,paperId)
                }
            }
        }
                }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AllScreenNav(viewModel: MainVIewModel, mainNav: NavController) {
    val navController = rememberAnimatedNavController()

    Scaffold(
        bottomBar = { btbar(navController) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        AnimatedNavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { FullPAge(viewModel, mainNav) }
            composable(BottomNavItem.Exams.route) { Papers(mainNav) }
            composable(BottomNavItem.Graph.route) { TimeTable(viewModel) }
            composable(BottomNavItem.Setting.route) { UploadTimeTableScreen(viewModel,mainNav) }
        }
    }
}
