package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ads.AdManager
import com.example.auth.AuthService
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationReadEntity
import com.example.data.local.TaskHistoryEntity
import com.example.data.model.AdConfig
import com.example.data.model.AdUnitItem
import com.example.data.model.AppConfig
import com.example.data.model.NotificationItem
import com.example.data.model.PayoutMethod
import com.example.data.model.ReferralRecord
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRecord
import com.example.data.remote.FirestoreRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel(
    private val firestoreRepository: FirestoreRepository,
    val authService: AuthService,
    val adManager: AdManager,
    private val localDb: AppDatabase
) : ViewModel() {

    private val tag = "MainViewModel"

    private val _currentUserId = MutableStateFlow(authService.currentUser?.uid ?: "")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    val isBlocked: StateFlow<Boolean> = combine(_userProfile) { profiles ->
        profiles[0]?.isBlocked == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _adConfig = MutableStateFlow(AdConfig())
    val adConfig: StateFlow<AdConfig> = _adConfig.asStateFlow()

    private val _adUnits = MutableStateFlow<List<AdUnitItem>>(emptyList())
    val adUnits: StateFlow<List<AdUnitItem>> = _adUnits.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val _taskCategories = MutableStateFlow<List<TaskCategory>>(emptyList())
    val taskCategories: StateFlow<List<TaskCategory>> = _taskCategories.asStateFlow()

    private val _completedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val completedTaskIds: StateFlow<Set<String>> = _completedTaskIds.asStateFlow()

    private val _payoutMethods = MutableStateFlow<List<PayoutMethod>>(emptyList())
    val payoutMethods: StateFlow<List<PayoutMethod>> = _payoutMethods.asStateFlow()

    private val _withdrawals = MutableStateFlow<List<WithdrawalRecord>>(emptyList())
    val withdrawals: StateFlow<List<WithdrawalRecord>> = _withdrawals.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()

    val unreadNotificationsCount: StateFlow<Int> = combine(_notifications, _readNotificationIds) { list, readIds ->
        list.count { !readIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _leaderboardDaily = MutableStateFlow<List<UserProfile>>(emptyList())
    val leaderboardDaily: StateFlow<List<UserProfile>> = _leaderboardDaily.asStateFlow()

    private val _leaderboardWeekly = MutableStateFlow<List<UserProfile>>(emptyList())
    val leaderboardWeekly: StateFlow<List<UserProfile>> = _leaderboardWeekly.asStateFlow()

    private val _leaderboardAllTime = MutableStateFlow<List<UserProfile>>(emptyList())
    val leaderboardAllTime: StateFlow<List<UserProfile>> = _leaderboardAllTime.asStateFlow()

    private val _videoCooldownSecondsRemaining = MutableStateFlow(0)
    val videoCooldownSecondsRemaining: StateFlow<Int> = _videoCooldownSecondsRemaining.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _referrals = MutableStateFlow<List<ReferralRecord>>(emptyList())
    val referrals: StateFlow<List<ReferralRecord>> = _referrals.asStateFlow()

    private var appConfigJob: Job? = null
    private var adConfigJob: Job? = null
    private var adUnitsJob: Job? = null
    private var tasksJob: Job? = null
    private var taskCategoriesJob: Job? = null
    private var payoutMethodsJob: Job? = null
    private var leaderboardDailyJob: Job? = null
    private var leaderboardWeeklyJob: Job? = null
    private var leaderboardAllTimeJob: Job? = null
    private var profileJob: Job? = null
    private var withdrawalsJob: Job? = null
    private var notificationsJob: Job? = null
    private var referralsJob: Job? = null

    init {
        // Observe local cached data
        listenLocalTaskHistory()
        listenLocalReadNotifications()

        // If user already logged in at startup, start all Firestore listeners
        val currentUid = authService.currentUser?.uid
        if (!currentUid.isNullOrEmpty()) {
            _currentUserId.value = currentUid
            viewModelScope.launch {
                val local = localDb.userBalanceDao().getBalanceOnce(currentUid)
                if (local != null && _userProfile.value == null) {
                    _userProfile.value = UserProfile(
                        uid = local.uid,
                        coins = local.coins,
                        gems = local.gems,
                        displayName = local.displayName.ifBlank { "Quest Gamer" },
                        email = local.email,
                        photoUrl = local.photoUrl
                    )
                }
            }
            startAllFirestoreListeners(currentUid)
        }

        // Start video cooldown ticker
        startCooldownTicker()
    }

    private fun listenLocalTaskHistory() {
        viewModelScope.launch {
            localDb.taskHistoryDao().getCompletedTasks().collect { list ->
                _completedTaskIds.value = list.map { it.taskId }.toSet()
            }
        }
    }

    private fun listenLocalReadNotifications() {
        viewModelScope.launch {
            localDb.notificationReadDao().getReadNotificationIds().collect { list ->
                _readNotificationIds.value = list.toSet()
            }
        }
    }

    fun startAllFirestoreListeners(uid: String) {
        stopAllFirestoreListeners()

        appConfigJob = viewModelScope.launch {
            firestoreRepository.getAppConfig().collect { config ->
                _appConfig.value = config
            }
        }

        adConfigJob = viewModelScope.launch {
            firestoreRepository.getAdConfig().collect { config ->
                _adConfig.value = config
                adManager.updateConfig(config)
            }
        }

        adUnitsJob = viewModelScope.launch {
            firestoreRepository.getAdUnits().collect { units ->
                _adUnits.value = units
                adManager.updateAdUnits(units)
            }
        }

        tasksJob = viewModelScope.launch {
            firestoreRepository.getTasks().collect { taskList ->
                _tasks.value = taskList
            }
        }

        taskCategoriesJob = viewModelScope.launch {
            firestoreRepository.getTaskCategories().collect { catList ->
                _taskCategories.value = catList
            }
        }

        payoutMethodsJob = viewModelScope.launch {
            firestoreRepository.getPayoutMethods().collect { methodList ->
                _payoutMethods.value = methodList
            }
        }

        leaderboardDailyJob = viewModelScope.launch {
            firestoreRepository.getLeaderboard("DAILY").collect { _leaderboardDaily.value = it }
        }

        leaderboardWeeklyJob = viewModelScope.launch {
            firestoreRepository.getLeaderboard("WEEKLY").collect { _leaderboardWeekly.value = it }
        }

        leaderboardAllTimeJob = viewModelScope.launch {
            firestoreRepository.getLeaderboard("ALL_TIME").collect { _leaderboardAllTime.value = it }
        }

        profileJob = viewModelScope.launch {
            firestoreRepository.getUserProfile(uid).collect { profile ->
                if (profile != null) {
                    _userProfile.value = profile
                }
            }
        }

        withdrawalsJob = viewModelScope.launch {
            firestoreRepository.getUserWithdrawals(uid).collect { list ->
                _withdrawals.value = list
            }
        }

        notificationsJob = viewModelScope.launch {
            firestoreRepository.getNotifications(uid).collect { list ->
                _notifications.value = list
            }
        }

        referralsJob = viewModelScope.launch {
            firestoreRepository.getReferralsForUser(uid).collect { list ->
                _referrals.value = list
            }
        }
    }

    fun stopAllFirestoreListeners() {
        appConfigJob?.cancel()
        adConfigJob?.cancel()
        adUnitsJob?.cancel()
        tasksJob?.cancel()
        taskCategoriesJob?.cancel()
        payoutMethodsJob?.cancel()
        leaderboardDailyJob?.cancel()
        leaderboardWeeklyJob?.cancel()
        leaderboardAllTimeJob?.cancel()
        profileJob?.cancel()
        withdrawalsJob?.cancel()
        notificationsJob?.cancel()
        referralsJob?.cancel()
    }

    private fun startCooldownTicker() {
        viewModelScope.launch {
            while (isActive) {
                val profile = _userProfile.value
                val adCfg = _adConfig.value
                if (profile != null) {
                    val cooldownMs = adCfg.videoCooldownSeconds * 1000L
                    val diff = System.currentTimeMillis() - profile.lastVideoAt
                    if (diff < cooldownMs) {
                        _videoCooldownSecondsRemaining.value = ((cooldownMs - diff) / 1000L).toInt()
                    } else {
                        _videoCooldownSecondsRemaining.value = 0
                    }
                } else {
                    _videoCooldownSecondsRemaining.value = 0
                }
                delay(1000L)
            }
        }
    }

    // Google Sign-In
    fun signInWithGoogle(activityContext: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            authService.signInWithGoogle(activityContext)
                .onSuccess { user ->
                    _currentUserId.value = user.uid
                    startAllFirestoreListeners(user.uid)
                    _isAuthLoading.value = false
                    onSuccess()
                }
                .onFailure { error ->
                    _isAuthLoading.value = false
                    if (error is androidx.credentials.exceptions.GetCredentialCancellationException) {
                        _authError.value = null
                    } else {
                        _authError.value = error.localizedMessage ?: "Failed to sign in with Google"
                    }
                }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    // Logout
    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authService.signOut()
            stopAllFirestoreListeners()
            _userProfile.value = null
            _referrals.value = emptyList()
            _currentUserId.value = ""
            onComplete()
        }
    }

    // Watch & Earn video ad
    fun playWatchAndEarnVideo(
        activity: Activity,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        if (uid.isEmpty()) {
            onError("Please sign in to earn coins")
            return
        }

        val profile = _userProfile.value
        val adCfg = _adConfig.value

        if (profile != null && profile.videosWatchedToday >= adCfg.dailyVideoLimit) {
            onError("Daily limit reached (${adCfg.dailyVideoLimit}/${adCfg.dailyVideoLimit}). Check back tomorrow!")
            return
        }

        if (_videoCooldownSecondsRemaining.value > 0) {
            onError("Please wait ${_videoCooldownSecondsRemaining.value}s before watching another video")
            return
        }

        adManager.showRewardedOrVideoAd(
            activity = activity,
            placement = "WATCH_EARN_VIDEO",
            onRewardEarned = {
                viewModelScope.launch {
                    val reward = adCfg.videoRewardCoins
                    firestoreRepository.recordVideoWatchedAndReward(uid, reward)
                        .onSuccess {
                            onSuccess(reward)
                        }
                        .onFailure { e ->
                            onError("Failed to record reward: ${e.message}")
                        }
                }
            },
            onFailedOrUnavailable = { errorMsg ->
                onError(errorMsg)
            }
        )
    }

    // Image Finder Game Reward
    fun recordImageFinderWon(
        activity: Activity,
        onRewardGranted: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        if (uid.isEmpty()) {
            onError("Not signed in")
            return
        }

        // Show Rewarded ad assigned to IMAGE_FINDER_REWARD
        adManager.showRewardedOrVideoAd(
            activity = activity,
            placement = "IMAGE_FINDER_REWARD",
            onRewardEarned = {
                viewModelScope.launch {
                    firestoreRepository.awardGem(uid, 1L)
                        .onSuccess {
                            // Check interstitial trigger
                            adManager.showInterstitialIfAllowed(activity, "INTERSTITIAL_AFTER_GAME")
                            onRewardGranted()
                        }
                        .onFailure { e ->
                            onError("Could not award gem: ${e.message}")
                        }
                }
            },
            onFailedOrUnavailable = { _ ->
                // If ad fails or isn't available, still award gem or inform user
                viewModelScope.launch {
                    firestoreRepository.awardGem(uid, 1L)
                    onRewardGranted()
                }
            }
        )
    }

    // Super Offer
    fun executeSuperOffer(
        activity: Activity,
        targetPackageName: String,
        targetUrl: String,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        val profile = _userProfile.value
        val config = _appConfig.value

        if (uid.isEmpty() || profile == null) {
            onError("User not signed in")
            return
        }

        if (profile.gems < config.superOfferUnlockGems) {
            onError("You need ${config.superOfferUnlockGems} Gems to unlock Super Offer. (Current: ${profile.gems})")
            return
        }

        // 1. Show Rewarded Ad assigned to SUPER_OFFER_REWARD
        adManager.showRewardedOrVideoAd(
            activity = activity,
            placement = "SUPER_OFFER_REWARD",
            onRewardEarned = {
                // 2. Open sponsored app URL / package for install/use verification
                try {
                    val intent = if (targetPackageName.isNotBlank() && isPackageInstalled(activity, targetPackageName)) {
                        activity.packageManager.getLaunchIntentForPackage(targetPackageName)
                    } else {
                        Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl.ifBlank { "https://play.google.com" }))
                    }
                    if (intent != null) {
                        activity.startActivity(intent)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Could not launch sponsored intent: ${e.message}")
                }

                // 3. Deduct Gems and grant +200 Coins
                viewModelScope.launch {
                    firestoreRepository.completeSuperOffer(
                        uid = uid,
                        requiredGems = config.superOfferUnlockGems,
                        rewardCoins = config.superOfferUnlockCoins
                    ).onSuccess {
                        onSuccess(config.superOfferUnlockCoins)
                    }.onFailure { e ->
                        onError("Super offer processing failed: ${e.message}")
                    }
                }
            },
            onFailedOrUnavailable = { errorMsg ->
                onError("Offer unavailable: $errorMsg")
            }
        )
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Task Execution
    fun executeTask(
        activity: Activity,
        task: TaskItem,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        if (uid.isEmpty()) {
            onError("Please sign in first")
            return
        }

        if (_completedTaskIds.value.contains(task.id)) {
            onError("You have already completed this task!")
            return
        }

        when (task.category.uppercase()) {
            "VIDEO" -> {
                adManager.showRewardedOrVideoAd(
                    activity = activity,
                    placement = "TASK_VIDEO",
                    onRewardEarned = {
                        completeTaskInternal(uid, task, onSuccess, onError)
                    },
                    onFailedOrUnavailable = { err ->
                        onError(err)
                    }
                )
            }
            else -> {
                // Open external URL if present
                if (task.actionUrl.isNotBlank()) {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(task.actionUrl))
                        activity.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Log.w(tag, "Could not open actionUrl: ${e.message}")
                    }
                }
                completeTaskInternal(uid, task, onSuccess, onError)
            }
        }
    }

    private fun completeTaskInternal(
        uid: String,
        task: TaskItem,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            firestoreRepository.completeTask(uid, task.id, task.rewardCoins)
                .onSuccess {
                    localDb.taskHistoryDao().recordCompletion(
                        TaskHistoryEntity(
                            taskId = task.id,
                            title = task.title,
                            rewardCoins = task.rewardCoins,
                            category = task.category,
                            completedAt = System.currentTimeMillis()
                        )
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    onError("Failed to record task reward: ${e.message}")
                }
        }
    }

    // Submit Withdrawal
    fun submitWithdrawal(
        method: PayoutMethod,
        accountDetail: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        val profile = _userProfile.value
        if (uid.isEmpty() || profile == null) {
            onError("Not signed in")
            return
        }

        if (profile.coins < method.minCoins) {
            onError("You need at least ${method.minCoins} Coins to withdraw (Balance: ${profile.coins})")
            return
        }

        if (accountDetail.isBlank()) {
            onError("Please enter your account details")
            return
        }

        viewModelScope.launch {
            firestoreRepository.submitWithdrawal(
                uid = uid,
                userName = profile.displayName,
                userEmail = profile.email,
                method = method,
                accountDetail = accountDetail.trim()
            ).onSuccess {
                onSuccess()
            }.onFailure { e ->
                onError(e.message ?: "Withdrawal submission failed")
            }
        }
    }

    // Notification Read
    fun markNotificationAsRead(notifId: String) {
        viewModelScope.launch {
            localDb.notificationReadDao().markAsRead(
                NotificationReadEntity(notificationId = notifId, readAt = System.currentTimeMillis())
            )
        }
    }

    // Update Notification Settings
    fun setNotificationsEnabled(enabled: Boolean) {
        val uid = _currentUserId.value
        viewModelScope.launch {
            if (uid.isNotEmpty()) {
                firestoreRepository.setNotificationsEnabled(uid, enabled)
            }
            if (enabled) {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users").await()
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users").await()
            }
        }
    }

    // Apply Referral Code
    fun applyReferralCode(
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        val user = _userProfile.value
        if (uid.isEmpty() || user == null) {
            onError("You must be logged in to apply a referral code")
            return
        }
        viewModelScope.launch {
            firestoreRepository.applyReferralCode(uid, user, code)
                .onSuccess { onSuccess() }
                .onFailure { e -> onError(e.message ?: "Failed to apply referral code") }
        }
    }

    // Update Display Name
    fun updateDisplayName(
        newName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        if (uid.isEmpty()) {
            onError("Not signed in")
            return
        }
        viewModelScope.launch {
            firestoreRepository.updateDisplayName(uid, newName)
                .onSuccess {
                    // Also sync local Room balance
                    val current = _userProfile.value
                    if (current != null) {
                        localDb.userBalanceDao().saveBalance(
                            com.example.data.local.UserBalanceEntity(
                                uid = uid,
                                coins = current.coins,
                                gems = current.gems,
                                displayName = newName.trim(),
                                email = current.email,
                                photoUrl = current.photoUrl,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    onSuccess()
                }
                .onFailure { e -> onError(e.message ?: "Could not update display name") }
        }
    }

    // Upload Profile Picture
    fun uploadProfilePicture(
        imageBytes: ByteArray,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        if (uid.isEmpty()) {
            onError("Not signed in")
            return
        }
        viewModelScope.launch {
            firestoreRepository.uploadProfileImage(uid, imageBytes)
                .onSuccess { url ->
                    // Also sync local Room balance
                    val current = _userProfile.value
                    if (current != null) {
                        localDb.userBalanceDao().saveBalance(
                            com.example.data.local.UserBalanceEntity(
                                uid = uid,
                                coins = current.coins,
                                gems = current.gems,
                                displayName = current.displayName,
                                email = current.email,
                                photoUrl = url,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    onSuccess(url)
                }
                .onFailure { e -> onError(e.message ?: "Failed to upload profile picture") }
        }
    }
}

class MainViewModelFactory(
    private val app: com.example.QuestRewardsApp
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                firestoreRepository = app.firestoreRepository,
                authService = app.authService,
                adManager = app.adManager,
                localDb = app.appDatabase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
