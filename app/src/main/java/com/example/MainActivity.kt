package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.NotificationHelper
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppTopBar
import com.example.ui.components.NotificationExplanationDialog
import com.example.ui.components.ProfileBottomSheet
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.blocked.AccountRestrictedScreen
import com.example.ui.screens.games.ImageFinderScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.notifications.NotificationsInboxScreen
import com.example.ui.screens.offers.SuperOfferScreen
import com.example.ui.screens.profile.AccountConnectScreen
import com.example.ui.screens.profile.MyProfileScreen
import com.example.ui.screens.profile.PrivacyPolicyScreen
import com.example.ui.screens.profile.ReferAndEarnScreen
import com.example.ui.screens.profile.TaskHistoryScreen
import com.example.ui.screens.settings.NotificationSettingsScreen
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.screens.wallet.WalletScreen
import com.example.ui.theme.QuestRewardsTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

enum class SubScreen {
    NONE,
    IMAGE_FINDER,
    SUPER_OFFER,
    NOTIFICATIONS_INBOX,
    NOTIFICATION_SETTINGS,
    MY_PROFILE,
    REFER_EARN,
    ACCOUNT_CONNECT,
    TASK_HISTORY,
    PRIVACY_POLICY
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application as QuestRewardsApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Splash Screen synchronously
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 2. Initialize AdMob early
        (application as QuestRewardsApp).adManager.initMobileAds(this)

        // 3. Read deep link from intent if started via notification
        val initialRoute = intent?.getStringExtra(NotificationHelper.EXTRA_ROUTE)
            ?: intent?.extras?.getString("route")

        // 4. Synchronous check of currentUser: straight into Home (or Login for new users)
        val initialUserLoggedIn = FirebaseAuth.getInstance().currentUser != null

        setContent {
            QuestRewardsTheme {
                MainAppContent(
                    viewModel = viewModel,
                    initialRoute = initialRoute,
                    initialUserLoggedIn = initialUserLoggedIn,
                    activity = this
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    initialRoute: String?,
    initialUserLoggedIn: Boolean,
    activity: ComponentActivity
) {
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBlocked by viewModel.isBlocked.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val adConfig by viewModel.adConfig.collectAsStateWithLifecycle()
    val adUnits by viewModel.adUnits.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val taskCategories by viewModel.taskCategories.collectAsStateWithLifecycle()
    val completedTaskIds by viewModel.completedTaskIds.collectAsStateWithLifecycle()
    val payoutMethods by viewModel.payoutMethods.collectAsStateWithLifecycle()
    val withdrawals by viewModel.withdrawals.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val readNotificationIds by viewModel.readNotificationIds.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val dailyUsers by viewModel.leaderboardDaily.collectAsStateWithLifecycle()
    val weeklyUsers by viewModel.leaderboardWeekly.collectAsStateWithLifecycle()
    val allTimeUsers by viewModel.leaderboardAllTime.collectAsStateWithLifecycle()
    val cooldownRemaining by viewModel.videoCooldownSecondsRemaining.collectAsStateWithLifecycle()

    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val referrals by viewModel.referrals.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var activeSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showNotificationExplanation by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Notification Permission Launcher (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(activity, "Notifications enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle deep link route
    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            when (initialRoute.uppercase()) {
                "HOME" -> { selectedTab = 0; activeSubScreen = SubScreen.NONE }
                "TASKS" -> { selectedTab = 1; activeSubScreen = SubScreen.NONE }
                "LEADERBOARD" -> { selectedTab = 2; activeSubScreen = SubScreen.NONE }
                "WALLET" -> { selectedTab = 3; activeSubScreen = SubScreen.NONE }
                "WATCH_EARN" -> { selectedTab = 0; activeSubScreen = SubScreen.NONE }
                "SUPER_OFFER" -> { activeSubScreen = SubScreen.SUPER_OFFER }
            }
        }
    }

    // Check notification permission on Android 13+ after login
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                showNotificationExplanation = true
            }
        }
    }

    // If user is restricted/blocked
    if (isBlocked) {
        AccountRestrictedScreen(
            onLogoutClick = {
                viewModel.signOut {}
            }
        )
        return
    }

    // If user is not logged in
    if (currentUserId.isEmpty()) {
        LoginScreen(
            isLoading = isAuthLoading,
            errorMessage = authError,
            onGoogleSignInClick = {
                viewModel.signInWithGoogle(activity) {}
            },
            onErrorDismissed = {
                viewModel.clearAuthError()
            }
        )
        return
    }

    // Notification explanation dialog before requesting permission
    if (showNotificationExplanation) {
        NotificationExplanationDialog(
            onConfirm = {
                showNotificationExplanation = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = {
                showNotificationExplanation = false
            }
        )
    }

    // Full Sub-screens routing with BackHandler
    when (activeSubScreen) {
        SubScreen.IMAGE_FINDER -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            ImageFinderScreen(
                currentGems = userProfile?.gems ?: 0L,
                onRoundWon = {
                    viewModel.recordImageFinderWon(
                        activity = activity,
                        onRewardGranted = {
                            Toast.makeText(activity, "+1 Gem Awarded!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(activity, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onBackClick = { activeSubScreen = SubScreen.NONE }
            )
            return
        }

        SubScreen.SUPER_OFFER -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            SuperOfferScreen(
                userProfile = userProfile,
                appConfig = appConfig,
                onUnlockAndExecute = { onSuccess, onError ->
                    viewModel.executeSuperOffer(
                        activity = activity,
                        targetPackageName = "",
                        targetUrl = "https://play.google.com",
                        onSuccess = onSuccess,
                        onError = onError
                    )
                },
                onBackClick = { activeSubScreen = SubScreen.NONE }
            )
            return
        }

        SubScreen.NOTIFICATIONS_INBOX -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            NotificationsInboxScreen(
                notifications = notifications,
                readNotificationIds = readNotificationIds,
                onNotificationClick = { item ->
                    viewModel.markNotificationAsRead(item.id)
                    if (item.route.isNotBlank()) {
                        when (item.route.uppercase()) {
                            "HOME" -> { selectedTab = 0; activeSubScreen = SubScreen.NONE }
                            "TASKS" -> { selectedTab = 1; activeSubScreen = SubScreen.NONE }
                            "LEADERBOARD" -> { selectedTab = 2; activeSubScreen = SubScreen.NONE }
                            "WALLET" -> { selectedTab = 3; activeSubScreen = SubScreen.NONE }
                            "WATCH_EARN" -> { selectedTab = 0; activeSubScreen = SubScreen.NONE }
                            "SUPER_OFFER" -> { activeSubScreen = SubScreen.SUPER_OFFER }
                        }
                    }
                },
                onMarkAllRead = {
                    notifications.forEach { viewModel.markNotificationAsRead(it.id) }
                },
                onBackClick = { activeSubScreen = SubScreen.NONE }
            )
            return
        }

        SubScreen.NOTIFICATION_SETTINGS -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            NotificationSettingsScreen(
                notificationsEnabled = userProfile?.notificationsEnabled ?: true,
                onToggle = { enabled ->
                    viewModel.setNotificationsEnabled(enabled)
                },
                onBackClick = { activeSubScreen = SubScreen.NONE }
            )
            return
        }

        SubScreen.MY_PROFILE -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            MyProfileScreen(
                userProfile = userProfile,
                onUpdateDisplayName = { newName, onSuccess, onError ->
                    viewModel.updateDisplayName(newName, onSuccess, onError)
                },
                onUploadProfilePicture = { bytes, onSuccess, onError ->
                    viewModel.uploadProfilePicture(bytes, onSuccess, onError)
                },
                onBackClick = { activeSubScreen = SubScreen.NONE }
            )
            return
        }

        SubScreen.REFER_EARN -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            ReferAndEarnScreen(
                userProfile = userProfile,
                referrals = referrals,
                onApplyReferralCode = { code, onSuccess, onError ->
                    viewModel.applyReferralCode(code, onSuccess, onError)
                },
                onBackClick = { activeSubScreen = SubScreen.NONE }
            )
            return
        }

        SubScreen.ACCOUNT_CONNECT -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            AccountConnectScreen(
                userProfile = userProfile,
                onBackClick = { activeSubScreen = SubScreen.NONE }
            )
            return
        }

        SubScreen.TASK_HISTORY -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            val taskHistory by (activity.application as QuestRewardsApp).appDatabase
                .taskHistoryDao()
                .getCompletedTasks()
                .collectAsStateWithLifecycle(emptyList())

            TaskHistoryScreen(
                taskHistory = taskHistory,
                onBackClick = { activeSubScreen = SubScreen.NONE }
            )
            return
        }

        SubScreen.PRIVACY_POLICY -> {
            BackHandler { activeSubScreen = SubScreen.NONE }
            PrivacyPolicyScreen(
                onBackClick = { activeSubScreen = SubScreen.NONE }
            )
            return
        }

        SubScreen.NONE -> {
            // Main 4-tab container
        }
    }

    // Main 4-tab Layout with TopBar & BottomBar
    Scaffold(
        topBar = {
            AppTopBar(
                coins = userProfile?.coins ?: 0L,
                gems = userProfile?.gems ?: 0L,
                unreadNotifications = unreadCount,
                onNotificationsClick = {
                    activeSubScreen = SubScreen.NOTIFICATIONS_INBOX
                },
                onMenuClick = {
                    showProfileSheet = true
                }
            )
        },
        bottomBar = {
            AppBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    userProfile = userProfile,
                    appConfig = appConfig,
                    adConfig = adConfig,
                    adUnits = adUnits,
                    adManager = (activity.application as QuestRewardsApp).adManager,
                    tasks = tasks,
                    videoCooldownSecondsRemaining = cooldownRemaining,
                    onWatchEarnClick = {
                        viewModel.playWatchAndEarnVideo(
                            activity = activity,
                            onSuccess = { rewarded ->
                                Toast.makeText(activity, "+$rewarded Coins Earned!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(activity, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onPlayImageFinderClick = {
                        activeSubScreen = SubScreen.IMAGE_FINDER
                    },
                    onSuperOfferClick = {
                        activeSubScreen = SubScreen.SUPER_OFFER
                    },
                    onViewAllTasksClick = {
                        selectedTab = 1
                    },
                    onTaskClick = { task ->
                        viewModel.executeTask(
                            activity = activity,
                            task = task,
                            onSuccess = {
                                Toast.makeText(activity, "Task Completed! +${task.rewardCoins} Coins", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(activity, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )

                1 -> TasksScreen(
                    tasks = tasks,
                    categories = taskCategories,
                    completedTaskIds = completedTaskIds,
                    adManager = (activity.application as QuestRewardsApp).adManager,
                    onTaskClick = { task ->
                        viewModel.executeTask(
                            activity = activity,
                            task = task,
                            onSuccess = {
                                Toast.makeText(activity, "Task Completed! +${task.rewardCoins} Coins", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(activity, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )

                2 -> LeaderboardScreen(
                    dailyUsers = dailyUsers,
                    weeklyUsers = weeklyUsers,
                    allTimeUsers = allTimeUsers,
                    currentUserId = currentUserId,
                    adManager = (activity.application as QuestRewardsApp).adManager
                )

                3 -> WalletScreen(
                    userProfile = userProfile,
                    appConfig = appConfig,
                    payoutMethods = payoutMethods,
                    withdrawals = withdrawals,
                    adManager = (activity.application as QuestRewardsApp).adManager,
                    onSubmitWithdrawal = { method, detail ->
                        viewModel.submitWithdrawal(
                            method = method,
                            accountDetail = detail,
                            onSuccess = {
                                Toast.makeText(activity, "Withdrawal Request Submitted!", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                )
            }
        }
    }

    // Profile Hamburger Modal BottomSheet
    if (showProfileSheet) {
        ProfileBottomSheet(
            userProfile = userProfile,
            sheetState = sheetState,
            onDismiss = { showProfileSheet = false },
            onMyProfileClick = {
                showProfileSheet = false
                activeSubScreen = SubScreen.MY_PROFILE
            },
            onPrivacyPolicyClick = {
                showProfileSheet = false
                activeSubScreen = SubScreen.PRIVACY_POLICY
            },
            onReferEarnClick = {
                showProfileSheet = false
                activeSubScreen = SubScreen.REFER_EARN
            },
            onAccountConnectClick = {
                showProfileSheet = false
                activeSubScreen = SubScreen.ACCOUNT_CONNECT
            },
            onTaskHistoryClick = {
                showProfileSheet = false
                activeSubScreen = SubScreen.TASK_HISTORY
            },
            onNotificationSettingsClick = {
                showProfileSheet = false
                activeSubScreen = SubScreen.NOTIFICATION_SETTINGS
            },
            onLogoutClick = {
                coroutineScope.launch {
                    sheetState.hide()
                    showProfileSheet = false
                    viewModel.signOut {}
                }
            }
        )
    }
}
