package com.example.data.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val coins: Long = 0L,
    val gems: Long = 0L,
    val dailyCoins: Long = 0L,
    val weeklyCoins: Long = 0L,
    val lastDailyDate: String = "",
    val lastWeeklyDate: String = "",
    val videosWatchedToday: Int = 0,
    val lastVideoDate: String = "",
    val lastVideoAt: Long = 0L,
    val fcmToken: String = "",
    val notificationsEnabled: Boolean = true,
    val platform: String = "Android",
    val tokenUpdatedAt: Long = 0L,
    val isBlocked: Boolean = false,
    val referralCode: String = "",
    val referredBy: String = "",
    val referralCount: Int = 0,
    val referralCoinsEarned: Long = 0L,
    val referralGemsEarned: Long = 0L,
    val updatedAt: Long = 0L
)

data class ReferralRecord(
    val id: String = "",
    val referrerUid: String = "",
    val referredUid: String = "",
    val referredName: String = "",
    val referredEmail: String = "",
    val coinsAwarded: Long = 100L,
    val gemsAwarded: Long = 10L,
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val rewardCoins: Long = 0L,
    val category: String = "DAILY", // INSTALL, VIDEO, SURVEY, DAILY, SOCIAL, or custom
    val iconType: String = "star",
    val actionUrl: String = "",
    val order: Int = 0,
    val isEnabled: Boolean = true
)

data class TaskCategory(
    val id: String = "",
    val name: String = "",
    val iconType: String = "category",
    val order: Int = 0,
    val isEnabled: Boolean = true
)

data class PayoutMethod(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val iconType: String = "wallet",
    val minCoins: Long = 500L,
    val cashAmount: Double = 5.0,
    val currency: String = "USD",
    val inputLabel: String = "Account / UPI ID",
    val inputPlaceholder: String = "e.g. user@bank or account email",
    val order: Int = 0,
    val isEnabled: Boolean = true
)

data class WithdrawalRecord(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val methodId: String = "",
    val methodTitle: String = "",
    val accountDetail: String = "",
    val coinsDeducted: Long = 0L,
    val cashAmount: Double = 0.0,
    val status: String = "PENDING", // PENDING | APPROVED | REJECTED
    val createdAt: Long = System.currentTimeMillis()
)

data class AppConfig(
    val superOfferUnlockGems: Long = 2L,
    val superOfferUnlockCoins: Long = 200L,
    val isSuperOfferEnabled: Boolean = true,
    val minWithdrawalCoins: Long = 500L,
    val conversionRateText: String = "1000 Coins = $1.00 USD",
    val welcomeBonusCoins: Long = 50L,
    val welcomeBonusGems: Long = 5L,
    val notifyOnWithdrawalStatus: Boolean = true,
    val notifyOnNewTask: Boolean = true
)

data class AdConfig(
    val adsEnabled: Boolean = true,
    val admobAppId: String = "ca-app-pub-3940256099942544~3347511713",
    val unityGameId: String = "1234567",
    val unityTestMode: Boolean = true,
    val interstitialFrequency: Int = 3,
    val videoRewardCoins: Long = 15L,
    val dailyVideoLimit: Int = 10,
    val videoCooldownSeconds: Int = 30
)

data class AdUnitItem(
    val id: String = "",
    val title: String = "",
    val provider: String = "ADMOB", // ADMOB | UNITY
    val adType: String = "BANNER", // REWARDED | VIDEO | BANNER | SLIDER | INTERSTITIAL
    val unitId: String = "",
    val placement: String = "", // HOME_BANNER, TASKS_BANNER, etc.
    val order: Int = 0,
    val isEnabled: Boolean = true,
    val createdAt: Long = 0L
)

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val target: String = "ALL", // ALL | USER
    val targetUserId: String = "",
    val route: String = "HOME", // HOME | TASKS | LEADERBOARD | WALLET | WATCH_EARN | SUPER_OFFER
    val status: String = "SENT",
    val sentCount: Int = 0,
    val error: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
