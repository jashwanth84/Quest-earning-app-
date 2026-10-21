package com.example.data.remote

import android.net.Uri
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.UserBalanceEntity
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localDb: AppDatabase
) {
    private val tag = "FirestoreRepository"

    val defaultTasks = listOf(
        TaskItem(
            id = "task_daily_checkin",
            title = "Daily Check-in Quest",
            description = "Open the app daily to collect your daily reward bonus",
            rewardCoins = 25L,
            category = "Daily Quests",
            iconType = "star",
            actionUrl = "claim://daily_checkin",
            order = 1,
            isEnabled = true
        ),
        TaskItem(
            id = "task_watch_video",
            title = "Watch Gaming Video",
            description = "Watch a high-paying sponsored gaming trailer",
            rewardCoins = 15L,
            category = "Video Rewards",
            iconType = "video",
            actionUrl = "action://watch_earn",
            order = 2,
            isEnabled = true
        ),
        TaskItem(
            id = "task_image_finder",
            title = "Play Image Finder",
            description = "Spot the target items in 30 seconds to earn bonus gems",
            rewardCoins = 50L,
            category = "Gaming",
            iconType = "game",
            actionUrl = "action://image_finder",
            order = 3,
            isEnabled = true
        ),
        TaskItem(
            id = "task_survey",
            title = "Quick Gaming Survey",
            description = "Complete a short 2-minute gaming feedback survey",
            rewardCoins = 100L,
            category = "Surveys",
            iconType = "survey",
            actionUrl = "https://play.google.com/store",
            order = 4,
            isEnabled = true
        ),
        TaskItem(
            id = "task_refer_friend",
            title = "Invite Friends & Earn",
            description = "Share your referral code and get 100 coins + 10 gems each",
            rewardCoins = 100L,
            category = "Daily Quests",
            iconType = "share",
            actionUrl = "action://refer_earn",
            order = 5,
            isEnabled = true
        )
    )

    val defaultCategories = listOf(
        TaskCategory("cat_all", "ALL", "category", 1, true),
        TaskCategory("cat_gaming", "Gaming", "category", 2, true),
        TaskCategory("cat_surveys", "Surveys", "category", 3, true),
        TaskCategory("cat_daily", "Daily Quests", "category", 4, true),
        TaskCategory("cat_video", "Video Rewards", "category", 5, true)
    )

    val defaultPayoutMethods = listOf(
        PayoutMethod(
            id = "pm_paypal",
            title = "PayPal Cash",
            subtitle = "Instant cash transfer to your PayPal wallet",
            iconType = "wallet",
            minCoins = 1000L,
            cashAmount = 1.0,
            currency = "USD",
            inputLabel = "PayPal Email Address",
            inputPlaceholder = "you@example.com",
            order = 1,
            isEnabled = true
        ),
        PayoutMethod(
            id = "pm_google_play",
            title = "Google Play Gift Code",
            subtitle = "Play Store redemption gift code delivered via email",
            iconType = "wallet",
            minCoins = 1000L,
            cashAmount = 1.0,
            currency = "USD",
            inputLabel = "Delivery Email Address",
            inputPlaceholder = "you@example.com",
            order = 2,
            isEnabled = true
        ),
        PayoutMethod(
            id = "pm_amazon",
            title = "Amazon Gift Voucher",
            subtitle = "Amazon shopping gift card voucher code",
            iconType = "wallet",
            minCoins = 2000L,
            cashAmount = 2.0,
            currency = "USD",
            inputLabel = "Email Address for Voucher",
            inputPlaceholder = "you@example.com",
            order = 3,
            isEnabled = true
        ),
        PayoutMethod(
            id = "pm_crypto",
            title = "Crypto USDT",
            subtitle = "USDT TRC20 / BEP20 cryptocurrency payout",
            iconType = "wallet",
            minCoins = 5000L,
            cashAmount = 5.0,
            currency = "USD",
            inputLabel = "USDT Wallet Address",
            inputPlaceholder = "T...",
            order = 4,
            isEnabled = true
        )
    )

    val defaultLeaderboard = listOf(
        UserProfile(uid = "seed1", displayName = "ShadowNinja", coins = 12450L, dailyCoins = 1450L, weeklyCoins = 4850L),
        UserProfile(uid = "seed2", displayName = "AlexRider", coins = 9820L, dailyCoins = 1120L, weeklyCoins = 3900L),
        UserProfile(uid = "seed3", displayName = "CrystalQueen", coins = 8740L, dailyCoins = 950L, weeklyCoins = 3400L),
        UserProfile(uid = "seed4", displayName = "PixelMaster", coins = 7610L, dailyCoins = 820L, weeklyCoins = 2950L),
        UserProfile(uid = "seed5", displayName = "DragonSlayer", coins = 6490L, dailyCoins = 710L, weeklyCoins = 2400L)
    )

    val defaultAdUnits = listOf(
        AdUnitItem(
            id = "au_banner",
            title = "Bottom Banner",
            provider = "ADMOB",
            adType = "BANNER",
            unitId = "ca-app-pub-3940256099942544/6300978111",
            placement = "home_bottom_banner",
            order = 1,
            isEnabled = true
        ),
        AdUnitItem(
            id = "au_rewarded",
            title = "Watch & Earn Video",
            provider = "ADMOB",
            adType = "REWARDED",
            unitId = "ca-app-pub-3940256099942544/5224354917",
            placement = "watch_earn_rewarded",
            order = 2,
            isEnabled = true
        )
    )

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getCurrentWeekString(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "$year-W$week"
    }

    // Real-time user profile listener
    fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        // Emit cached profile if present
        launch {
            try {
                val local = localDb.userBalanceDao().getBalanceOnce(uid)
                if (local != null) {
                    trySend(
                        UserProfile(
                            uid = local.uid,
                            coins = local.coins,
                            gems = local.gems,
                            displayName = local.displayName.ifBlank { "Quest Gamer" },
                            email = local.email,
                            photoUrl = local.photoUrl
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not listen to user profile: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        trySend(profile)
                        launch {
                            try {
                                localDb.userBalanceDao().saveBalance(
                                    UserBalanceEntity(
                                        uid = profile.uid,
                                        coins = profile.coins,
                                        gems = profile.gems,
                                        displayName = profile.displayName,
                                        email = profile.email,
                                        photoUrl = profile.photoUrl,
                                        updatedAt = profile.updatedAt
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    // App Config listener
    fun getAppConfig(): Flow<AppConfig> = callbackFlow {
        trySend(AppConfig())
        val listener = firestore.collection("app_config").document("settings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch app_config: ${error.message}")
                    trySend(AppConfig())
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val config = snapshot.toObject(AppConfig::class.java) ?: AppConfig()
                    trySend(config)
                } else {
                    trySend(AppConfig())
                }
            }
        awaitClose { listener.remove() }
    }

    // Ad Config listener
    fun getAdConfig(): Flow<AdConfig> = callbackFlow {
        trySend(AdConfig())
        val listener = firestore.collection("ad_config").document("settings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch ad_config: ${error.message}")
                    trySend(AdConfig())
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val config = snapshot.toObject(AdConfig::class.java) ?: AdConfig()
                    trySend(config)
                } else {
                    trySend(AdConfig())
                }
            }
        awaitClose { listener.remove() }
    }

    // Ad Units listener
    fun getAdUnits(): Flow<List<AdUnitItem>> = callbackFlow {
        trySend(defaultAdUnits)
        val listener = firestore.collection("ad_units")
            .whereEqualTo("isEnabled", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch ad_units: ${error.message}")
                    trySend(defaultAdUnits)
                    return@addSnapshotListener
                }
                val units = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AdUnitItem::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.order } ?: emptyList()
                if (units.isEmpty()) {
                    trySend(defaultAdUnits)
                } else {
                    trySend(units)
                }
            }
        awaitClose { listener.remove() }
    }

    // Tasks listener
    fun getTasks(): Flow<List<TaskItem>> = callbackFlow {
        trySend(defaultTasks)
        val listener = firestore.collection("tasks")
            .whereEqualTo("isEnabled", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch tasks: ${error.message}")
                    trySend(defaultTasks)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TaskItem::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.order } ?: emptyList()
                if (tasks.isEmpty()) {
                    trySend(defaultTasks)
                } else {
                    trySend(tasks)
                }
            }
        awaitClose { listener.remove() }
    }

    // Task categories listener
    fun getTaskCategories(): Flow<List<TaskCategory>> = callbackFlow {
        trySend(defaultCategories)
        val listener = firestore.collection("task_categories")
            .whereEqualTo("isEnabled", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch task_categories: ${error.message}")
                    trySend(defaultCategories)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TaskCategory::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.order } ?: emptyList()
                if (categories.isEmpty()) {
                    trySend(defaultCategories)
                } else {
                    trySend(categories)
                }
            }
        awaitClose { listener.remove() }
    }

    // Payout methods listener
    fun getPayoutMethods(): Flow<List<PayoutMethod>> = callbackFlow {
        trySend(defaultPayoutMethods)
        val listener = firestore.collection("payout_methods")
            .whereEqualTo("isEnabled", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch payout_methods: ${error.message}")
                    trySend(defaultPayoutMethods)
                    return@addSnapshotListener
                }
                val methods = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PayoutMethod::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.order } ?: emptyList()
                if (methods.isEmpty()) {
                    trySend(defaultPayoutMethods)
                } else {
                    trySend(methods)
                }
            }
        awaitClose { listener.remove() }
    }

    // User's withdrawals listener
    fun getUserWithdrawals(uid: String): Flow<List<WithdrawalRecord>> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("withdrawals")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch withdrawals: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WithdrawalRecord::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // Notifications listener (ALL or targetUserId == uid)
    fun getNotifications(uid: String): Flow<List<NotificationItem>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch notifications: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val item = doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                    if (item != null && (item.target == "ALL" || item.targetUserId == uid)) item else null
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    // Leaderboards
    fun getLeaderboard(type: String): Flow<List<UserProfile>> = callbackFlow {
        trySend(defaultLeaderboard)
        val sortField = when (type) {
            "DAILY" -> "dailyCoins"
            "WEEKLY" -> "weeklyCoins"
            else -> "coins"
        }
        val listener = firestore.collection("users")
            .orderBy(sortField, Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch leaderboard: ${error.message}")
                    trySend(defaultLeaderboard)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    val user = doc.toObject(UserProfile::class.java)
                    if (user != null && !user.isBlocked && user.displayName.isNotBlank()) user else null
                } ?: emptyList()
                if (users.isEmpty()) {
                    trySend(defaultLeaderboard)
                } else {
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }

    // Initialize or sync user on login
    suspend fun initOrSyncUser(uid: String, displayName: String, email: String, photoUrl: String): UserProfile {
        val today = getTodayDateString()
        val thisWeek = getCurrentWeekString()

        try {
            val userDocRef = firestore.collection("users").document(uid)
            val snapshot = userDocRef.get().await()

            if (!snapshot.exists()) {
                val configDoc = try {
                    firestore.collection("app_config").document("settings").get().await()
                } catch (_: Exception) {
                    null
                }
                val welcomeCoins = configDoc?.getLong("welcomeBonusCoins") ?: 50L
                val welcomeGems = configDoc?.getLong("welcomeBonusGems") ?: 5L
                val refCode = generateUniqueReferralCode(uid)

                val newUser = UserProfile(
                    uid = uid,
                    displayName = displayName.ifBlank { "Quest Gamer" },
                    email = email,
                    photoUrl = photoUrl,
                    coins = welcomeCoins,
                    gems = welcomeGems,
                    dailyCoins = welcomeCoins,
                    weeklyCoins = welcomeCoins,
                    lastDailyDate = today,
                    lastWeeklyDate = thisWeek,
                    videosWatchedToday = 0,
                    lastVideoDate = today,
                    lastVideoAt = 0L,
                    notificationsEnabled = true,
                    platform = "Android",
                    isBlocked = false,
                    referralCode = refCode,
                    referredBy = "",
                    referralCount = 0,
                    referralCoinsEarned = 0L,
                    referralGemsEarned = 0L,
                    updatedAt = System.currentTimeMillis()
                )
                try {
                    userDocRef.set(newUser).await()
                    firestore.collection("referral_codes").document(refCode).set(
                        mapOf("uid" to uid, "createdAt" to System.currentTimeMillis())
                    ).await()
                } catch (e: Exception) {
                    Log.w(tag, "Could not set referral code remotely: ${e.message}")
                }

                // Sync with Room
                localDb.userBalanceDao().saveBalance(
                    UserBalanceEntity(
                        uid = uid,
                        coins = welcomeCoins,
                        gems = welcomeGems,
                        displayName = newUser.displayName,
                        email = email,
                        photoUrl = photoUrl,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return newUser
            } else {
                val existing = snapshot.toObject(UserProfile::class.java)!!
                var updated = existing.copy(
                    displayName = if (existing.displayName.isBlank()) displayName else existing.displayName,
                    email = if (existing.email.isBlank()) email else existing.email,
                    photoUrl = if (existing.photoUrl.isBlank()) photoUrl else existing.photoUrl
                )

                if (updated.referralCode.isBlank()) {
                    val newCode = generateUniqueReferralCode(uid)
                    updated = updated.copy(referralCode = newCode)
                    try {
                        firestore.collection("referral_codes").document(newCode).set(
                            mapOf("uid" to uid, "createdAt" to System.currentTimeMillis())
                        ).await()
                        userDocRef.set(mapOf("referralCode" to newCode), SetOptions.merge()).await()
                    } catch (e: Exception) {
                        Log.w(tag, "Could not set referral code: ${e.message}")
                    }
                }

                // Reset daily/weekly if dates changed
                val needsDailyReset = updated.lastDailyDate != today
                val needsWeeklyReset = updated.lastWeeklyDate != thisWeek
                val needsVideoReset = updated.lastVideoDate != today

                if (needsDailyReset || needsWeeklyReset || needsVideoReset) {
                    updated = updated.copy(
                        dailyCoins = if (needsDailyReset) 0L else updated.dailyCoins,
                        lastDailyDate = today,
                        weeklyCoins = if (needsWeeklyReset) 0L else updated.weeklyCoins,
                        lastWeeklyDate = thisWeek,
                        videosWatchedToday = if (needsVideoReset) 0 else updated.videosWatchedToday,
                        lastVideoDate = today,
                        updatedAt = System.currentTimeMillis()
                    )
                    try {
                        userDocRef.set(updated, SetOptions.merge()).await()
                    } catch (e: Exception) {
                        Log.w(tag, "Could not reset dates remotely: ${e.message}")
                    }
                }

                localDb.userBalanceDao().saveBalance(
                    UserBalanceEntity(
                        uid = uid,
                        coins = updated.coins,
                        gems = updated.gems,
                        displayName = updated.displayName,
                        email = updated.email,
                        photoUrl = updated.photoUrl,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return updated
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not sync user remotely: ${e.message}. Using local cache.")
            val cached = localDb.userBalanceDao().getBalanceOnce(uid)
            if (cached != null) {
                return UserProfile(
                    uid = cached.uid,
                    displayName = cached.displayName.ifBlank { displayName.ifBlank { "Quest Gamer" } },
                    email = cached.email.ifBlank { email },
                    photoUrl = cached.photoUrl.ifBlank { photoUrl },
                    coins = cached.coins,
                    gems = cached.gems,
                    dailyCoins = cached.coins,
                    weeklyCoins = cached.coins,
                    lastDailyDate = today,
                    lastWeeklyDate = thisWeek,
                    updatedAt = cached.updatedAt
                )
            }
            val fallback = UserProfile(
                uid = uid,
                displayName = displayName.ifBlank { "Quest Gamer" },
                email = email,
                photoUrl = photoUrl,
                coins = 50L,
                gems = 5L,
                dailyCoins = 50L,
                weeklyCoins = 50L,
                lastDailyDate = today,
                lastWeeklyDate = thisWeek,
                updatedAt = System.currentTimeMillis()
            )
            localDb.userBalanceDao().saveBalance(
                UserBalanceEntity(
                    uid = uid,
                    coins = fallback.coins,
                    gems = fallback.gems,
                    displayName = fallback.displayName,
                    email = fallback.email,
                    photoUrl = fallback.photoUrl,
                    updatedAt = System.currentTimeMillis()
                )
            )
            return fallback
        }
    }

    // Award Gem (e.g. Image Finder)
    suspend fun awardGem(uid: String, amount: Long = 1L): Result<Unit> = runCatching {
        val userDocRef = firestore.collection("users").document(uid)
        firestore.runTransaction { tx ->
            val snap = tx.get(userDocRef)
            val currentGems = snap.getLong("gems") ?: 0L
            val currentCoins = snap.getLong("coins") ?: 0L
            val displayName = snap.getString("displayName") ?: ""
            val email = snap.getString("email") ?: ""
            val photoUrl = snap.getString("photoUrl") ?: ""
            val newGems = currentGems + amount

            tx.update(userDocRef, mapOf(
                "gems" to newGems,
                "updatedAt" to System.currentTimeMillis()
            ))
        }.await()
    }

    // Unlock & complete Super Offer (deduct gems, add coins)
    suspend fun completeSuperOffer(uid: String, requiredGems: Long, rewardCoins: Long): Result<Unit> = runCatching {
        val userDocRef = firestore.collection("users").document(uid)
        val today = getTodayDateString()
        val thisWeek = getCurrentWeekString()

        firestore.runTransaction { tx ->
            val snap = tx.get(userDocRef)
            val currentGems = snap.getLong("gems") ?: 0L
            if (currentGems < requiredGems) {
                throw IllegalStateException("Insufficient gems ($currentGems < $requiredGems)")
            }
            val currentCoins = snap.getLong("coins") ?: 0L
            val currentDaily = snap.getLong("dailyCoins") ?: 0L
            val currentWeekly = snap.getLong("weeklyCoins") ?: 0L

            tx.update(userDocRef, mapOf(
                "gems" to (currentGems - requiredGems),
                "coins" to (currentCoins + rewardCoins),
                "dailyCoins" to (currentDaily + rewardCoins),
                "weeklyCoins" to (currentWeekly + rewardCoins),
                "lastDailyDate" to today,
                "lastWeeklyDate" to thisWeek,
                "updatedAt" to System.currentTimeMillis()
            ))
        }.await()
    }

    // Watch & Earn video reward
    suspend fun recordVideoWatchedAndReward(uid: String, rewardCoins: Long): Result<Unit> = runCatching {
        val userDocRef = firestore.collection("users").document(uid)
        val today = getTodayDateString()
        val thisWeek = getCurrentWeekString()
        val now = System.currentTimeMillis()

        firestore.runTransaction { tx ->
            val snap = tx.get(userDocRef)
            val lastDate = snap.getString("lastVideoDate") ?: ""
            val watchedToday = if (lastDate == today) (snap.getLong("videosWatchedToday")?.toInt() ?: 0) else 0

            val currentCoins = snap.getLong("coins") ?: 0L
            val currentDaily = snap.getLong("dailyCoins") ?: 0L
            val currentWeekly = snap.getLong("weeklyCoins") ?: 0L

            tx.update(userDocRef, mapOf(
                "coins" to (currentCoins + rewardCoins),
                "dailyCoins" to (currentDaily + rewardCoins),
                "weeklyCoins" to (currentWeekly + rewardCoins),
                "videosWatchedToday" to (watchedToday + 1),
                "lastVideoDate" to today,
                "lastVideoAt" to now,
                "lastDailyDate" to today,
                "lastWeeklyDate" to thisWeek,
                "updatedAt" to now
            ))
        }.await()
    }

    // Complete Task
    suspend fun completeTask(uid: String, taskId: String, rewardCoins: Long): Result<Unit> = runCatching {
        val userDocRef = firestore.collection("users").document(uid)
        val today = getTodayDateString()
        val thisWeek = getCurrentWeekString()

        firestore.runTransaction { tx ->
            val snap = tx.get(userDocRef)
            val currentCoins = snap.getLong("coins") ?: 0L
            val currentDaily = snap.getLong("dailyCoins") ?: 0L
            val currentWeekly = snap.getLong("weeklyCoins") ?: 0L

            tx.update(userDocRef, mapOf(
                "coins" to (currentCoins + rewardCoins),
                "dailyCoins" to (currentDaily + rewardCoins),
                "weeklyCoins" to (currentWeekly + rewardCoins),
                "lastDailyDate" to today,
                "lastWeeklyDate" to thisWeek,
                "updatedAt" to System.currentTimeMillis()
            ))
        }.await()
    }

    // Submit withdrawal with transaction
    suspend fun submitWithdrawal(
        uid: String,
        userName: String,
        userEmail: String,
        method: PayoutMethod,
        accountDetail: String
    ): Result<Unit> = runCatching {
        val userDocRef = firestore.collection("users").document(uid)
        val withdrawalDocRef = firestore.collection("withdrawals").document()

        firestore.runTransaction { tx ->
            val snap = tx.get(userDocRef)
            val currentCoins = snap.getLong("coins") ?: 0L
            if (currentCoins < method.minCoins) {
                throw IllegalStateException("Insufficient coins balance ($currentCoins < ${method.minCoins})")
            }

            // Deduct coins
            tx.update(userDocRef, mapOf(
                "coins" to (currentCoins - method.minCoins),
                "updatedAt" to System.currentTimeMillis()
            ))

            // Create withdrawal record
            val record = WithdrawalRecord(
                id = withdrawalDocRef.id,
                userId = uid,
                userName = userName,
                userEmail = userEmail,
                methodId = method.id,
                methodTitle = method.title,
                accountDetail = accountDetail,
                coinsDeducted = method.minCoins,
                cashAmount = method.cashAmount,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
            tx.set(withdrawalDocRef, record)
        }.await()
    }

    // Update FCM token
    suspend fun updateFcmToken(uid: String, token: String) {
        if (uid.isEmpty()) return
        try {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "fcmToken" to token,
                    "platform" to "Android",
                    "tokenUpdatedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(tag, "Could not update FCM token: ${e.message}")
        }
    }

    // Clear FCM token on logout
    suspend fun clearFcmToken(uid: String) {
        if (uid.isEmpty()) return
        try {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "fcmToken" to "",
                    "tokenUpdatedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(tag, "Could not clear FCM token: ${e.message}")
        }
    }

    // Update notification toggle
    suspend fun setNotificationsEnabled(uid: String, enabled: Boolean) {
        if (uid.isEmpty()) return
        try {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "notificationsEnabled" to enabled,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(tag, "Could not update notificationsEnabled: ${e.message}")
        }
    }

    private fun generateUniqueReferralCode(uid: String): String {
        val allowed = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val prefix = "QR"
        val uidPart = uid.filter { it.uppercaseChar() in allowed }.take(2).uppercase()
        val randomPart = (1..4).map { allowed.random() }.joinToString("")
        return "$prefix$uidPart$randomPart"
    }

    fun getReferralsForUser(referrerUid: String): Flow<List<ReferralRecord>> = callbackFlow {
        if (referrerUid.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("referrals")
            .whereEqualTo("referrerUid", referrerUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Could not fetch referrals: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ReferralRecord::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun applyReferralCode(
        currentUid: String,
        currentUser: UserProfile,
        code: String,
        bonusCoins: Long = 100L,
        bonusGems: Long = 10L
    ): Result<Unit> = runCatching {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) throw IllegalArgumentException("Referral code cannot be blank")
        if (cleanCode == currentUser.referralCode) throw IllegalArgumentException("You cannot use your own referral code")
        if (currentUser.referredBy.isNotBlank()) throw IllegalStateException("You have already used a referral code")

        var referrerUid = ""
        val codeDoc = firestore.collection("referral_codes").document(cleanCode).get().await()
        if (codeDoc.exists()) {
            referrerUid = codeDoc.getString("uid") ?: ""
        } else {
            val userQuery = firestore.collection("users")
                .whereEqualTo("referralCode", cleanCode)
                .limit(1)
                .get()
                .await()
            if (!userQuery.isEmpty) {
                referrerUid = userQuery.documents.first().id
            }
        }

        if (referrerUid.isBlank()) {
            throw IllegalArgumentException("Invalid referral code. Please check and try again.")
        }
        if (referrerUid == currentUid) {
            throw IllegalArgumentException("You cannot use your own referral code")
        }

        val referrerDocRef = firestore.collection("users").document(referrerUid)
        val userDocRef = firestore.collection("users").document(currentUid)
        val refDocRef = firestore.collection("referrals").document()
        val today = getTodayDateString()
        val thisWeek = getCurrentWeekString()
        val now = System.currentTimeMillis()

        firestore.runTransaction { tx ->
            val refSnap = tx.get(referrerDocRef)
            val userSnap = tx.get(userDocRef)

            if (!refSnap.exists()) throw IllegalArgumentException("Referrer account does not exist")
            val currentReferredBy = userSnap.getString("referredBy") ?: ""
            if (currentReferredBy.isNotBlank()) {
                throw IllegalStateException("You have already used a referral code")
            }

            val userCoins = userSnap.getLong("coins") ?: 0L
            val userGems = userSnap.getLong("gems") ?: 0L
            val userDaily = userSnap.getLong("dailyCoins") ?: 0L
            val userWeekly = userSnap.getLong("weeklyCoins") ?: 0L

            val referrerCoins = refSnap.getLong("coins") ?: 0L
            val referrerGems = refSnap.getLong("gems") ?: 0L
            val referrerDaily = refSnap.getLong("dailyCoins") ?: 0L
            val referrerWeekly = refSnap.getLong("weeklyCoins") ?: 0L
            val refCount = (refSnap.getLong("referralCount")?.toInt() ?: 0) + 1
            val refCoinsEarned = (refSnap.getLong("referralCoinsEarned") ?: 0L) + bonusCoins
            val refGemsEarned = (refSnap.getLong("referralGemsEarned") ?: 0L) + bonusGems

            // Update referred user (current user)
            tx.update(userDocRef, mapOf(
                "coins" to (userCoins + bonusCoins),
                "gems" to (userGems + bonusGems),
                "dailyCoins" to (userDaily + bonusCoins),
                "weeklyCoins" to (userWeekly + bonusCoins),
                "referredBy" to cleanCode,
                "lastDailyDate" to today,
                "lastWeeklyDate" to thisWeek,
                "updatedAt" to now
            ))

            // Update referrer user
            tx.update(referrerDocRef, mapOf(
                "coins" to (referrerCoins + bonusCoins),
                "gems" to (referrerGems + bonusGems),
                "dailyCoins" to (referrerDaily + bonusCoins),
                "weeklyCoins" to (referrerWeekly + bonusCoins),
                "referralCount" to refCount,
                "referralCoinsEarned" to refCoinsEarned,
                "referralGemsEarned" to refGemsEarned,
                "lastDailyDate" to today,
                "lastWeeklyDate" to thisWeek,
                "updatedAt" to now
            ))

            // Create referral record
            val referralRecord = ReferralRecord(
                id = refDocRef.id,
                referrerUid = referrerUid,
                referredUid = currentUid,
                referredName = currentUser.displayName,
                referredEmail = currentUser.email,
                coinsAwarded = bonusCoins,
                gemsAwarded = bonusGems,
                createdAt = now
            )
            tx.set(refDocRef, referralRecord)
        }.await()
    }

    suspend fun updateDisplayName(uid: String, newName: String): Result<Unit> = runCatching {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) throw IllegalArgumentException("Name cannot be empty")

        firestore.collection("users").document(uid).update(
            mapOf(
                "displayName" to cleanName,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()

        val fbUser = FirebaseAuth.getInstance().currentUser
        if (fbUser != null && fbUser.uid == uid) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(cleanName)
                .build()
            fbUser.updateProfile(profileUpdates).await()
        }
    }

    suspend fun updateProfilePhoto(uid: String, photoUrl: String): Result<Unit> = runCatching {
        firestore.collection("users").document(uid).update(
            mapOf(
                "photoUrl" to photoUrl,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()

        val fbUser = FirebaseAuth.getInstance().currentUser
        if (fbUser != null && fbUser.uid == uid) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(Uri.parse(photoUrl))
                .build()
            fbUser.updateProfile(profileUpdates).await()
        }
    }

    suspend fun uploadProfileImage(uid: String, imageBytes: ByteArray): Result<String> = runCatching {
        val storageRef = FirebaseStorage.getInstance().reference
        val profileImageRef = storageRef.child("profile_pictures/$uid.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        profileImageRef.putBytes(imageBytes, metadata).await()
        val downloadUri = profileImageRef.downloadUrl.await()
        val photoUrl = downloadUri.toString()
        updateProfilePhoto(uid, photoUrl).getOrThrow()
        photoUrl
    }
}
