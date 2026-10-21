package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AdConfig
import com.example.data.model.AdUnitItem
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdManager(private val context: Context) {
    private val tag = "AdManager"

    private var isAdMobInitialized = false
    private var isUnityInitialized = false

    private val _adConfig = MutableStateFlow(AdConfig())
    val adConfig: StateFlow<AdConfig> = _adConfig.asStateFlow()

    private val _adUnits = MutableStateFlow<List<AdUnitItem>>(emptyList())
    val adUnits: StateFlow<List<AdUnitItem>> = _adUnits.asStateFlow()

    // Cache preloaded AdMob ads: unitId -> Ad
    private val preloadedRewarded = mutableMapOf<String, RewardedAd>()
    private val preloadedInterstitial = mutableMapOf<String, InterstitialAd>()

    // Track interstitial trigger count for interstitialFrequency
    private var interstitialTriggerCount = 0

    // Standard AdMob test unit IDs for debug mode
    private val admobTestRewarded = "ca-app-pub-3940256099942544/5224354917"
    private val admobTestInterstitial = "ca-app-pub-3940256099942544/1033173712"
    private val admobTestBanner = "ca-app-pub-3940256099942544/6300978111"

    fun initMobileAds(activity: Activity) {
        if (!isAdMobInitialized) {
            try {
                MobileAds.initialize(context) { status ->
                    isAdMobInitialized = true
                    Log.d(tag, "AdMob initialized: $status")
                }
            } catch (e: Throwable) {
                Log.e(tag, "AdMob initialization error: ${e.message}", e)
            }
        }
    }

    fun updateConfig(config: AdConfig) {
        _adConfig.value = config
        // Initialize Unity Ads when config arrives or updates
        if (config.adsEnabled && config.unityGameId.isNotBlank() && !isUnityInitialized) {
            try {
                UnityAds.initialize(
                    context,
                    config.unityGameId,
                    config.unityTestMode,
                    object : IUnityAdsInitializationListener {
                        override fun onInitializationComplete() {
                            isUnityInitialized = true
                            Log.d(tag, "Unity Ads initialized successfully")
                        }

                        override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                            Log.w(tag, "Unity Ads init failed: $error - $message")
                        }
                    }
                )
            } catch (e: Throwable) {
                Log.e(tag, "Unity Ads initialization error: ${e.message}", e)
            }
        }
    }

    fun updateAdUnits(units: List<AdUnitItem>) {
        _adUnits.value = units
        // Preload rewarded and interstitial units
        if (_adConfig.value.adsEnabled) {
            units.filter { it.isEnabled }.forEach { unit ->
                when (unit.adType.uppercase()) {
                    "REWARDED", "VIDEO" -> preloadRewardedAd(unit)
                    "INTERSTITIAL" -> preloadInterstitialAd(unit)
                }
            }
        }
    }

    private fun getEffectiveUnitId(unit: AdUnitItem): String {
        if (BuildConfig.DEBUG) {
            if (unit.provider.equals("ADMOB", ignoreCase = true)) {
                return when (unit.adType.uppercase()) {
                    "REWARDED", "VIDEO" -> admobTestRewarded
                    "INTERSTITIAL" -> admobTestInterstitial
                    "BANNER", "SLIDER" -> admobTestBanner
                    else -> admobTestRewarded
                }
            }
        }
        return unit.unitId
    }

    fun getUnitsForPlacement(placement: String): List<AdUnitItem> {
        if (!_adConfig.value.adsEnabled) return emptyList()
        return _adUnits.value
            .filter { it.isEnabled && it.placement.equals(placement, ignoreCase = true) }
            .sortedBy { it.order }
    }

    // Preload AdMob Rewarded Ad
    private fun preloadRewardedAd(unit: AdUnitItem) {
        if (unit.provider.equals("UNITY", ignoreCase = true)) {
            // Unity preloads via placement ID
            val placementId = getEffectiveUnitId(unit)
            UnityAds.load(placementId, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    Log.d(tag, "Unity ad loaded: $placementId")
                }
                override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                    Log.w(tag, "Unity ad load failed: $message")
                }
            })
            return
        }

        val effectiveId = getEffectiveUnitId(unit)
        val request = AdRequest.Builder().build()
        RewardedAd.load(context, effectiveId, request, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                preloadedRewarded[effectiveId] = ad
                Log.d(tag, "AdMob Rewarded ad loaded: $effectiveId")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(tag, "AdMob Rewarded ad failed to load: ${error.message}")
            }
        })
    }

    // Preload Interstitial Ad
    private fun preloadInterstitialAd(unit: AdUnitItem) {
        if (unit.provider.equals("UNITY", ignoreCase = true)) {
            val placementId = getEffectiveUnitId(unit)
            UnityAds.load(placementId, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {}
                override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {}
            })
            return
        }

        val effectiveId = getEffectiveUnitId(unit)
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, effectiveId, request, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                preloadedInterstitial[effectiveId] = ad
                Log.d(tag, "AdMob Interstitial loaded: $effectiveId")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(tag, "AdMob Interstitial failed to load: ${error.message}")
            }
        })
    }

    // Show Rewarded / Video ad with fallback logic
    fun showRewardedOrVideoAd(
        activity: Activity,
        placement: String,
        onRewardEarned: () -> Unit,
        onFailedOrUnavailable: (String) -> Unit
    ) {
        if (!_adConfig.value.adsEnabled) {
            // Ads are disabled by admin; treat as completed or show message
            onRewardEarned()
            return
        }

        val units = getUnitsForPlacement(placement)
        if (units.isEmpty()) {
            onFailedOrUnavailable("No enabled ad units for this placement")
            return
        }

        showAdCandidate(activity, units, 0, onRewardEarned, onFailedOrUnavailable)
    }

    private fun showAdCandidate(
        activity: Activity,
        candidates: List<AdUnitItem>,
        index: Int,
        onRewardEarned: () -> Unit,
        onFailedOrUnavailable: (String) -> Unit
    ) {
        if (index >= candidates.size) {
            onFailedOrUnavailable("Video not available, try later")
            return
        }

        val unit = candidates[index]
        val effectiveId = getEffectiveUnitId(unit)

        if (unit.provider.equals("UNITY", ignoreCase = true)) {
            // Show Unity Ad
            val showOptions = UnityAdsShowOptions()
            UnityAds.show(activity, effectiveId, showOptions, object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                    Log.w(tag, "Unity show failed: $message, trying next candidate")
                    activity.runOnUiThread {
                        showAdCandidate(activity, candidates, index + 1, onRewardEarned, onFailedOrUnavailable)
                    }
                }

                override fun onUnityAdsShowStart(placementId: String?) {}
                override fun onUnityAdsShowClick(placementId: String?) {}

                override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                    activity.runOnUiThread {
                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            onRewardEarned()
                        } else {
                            onFailedOrUnavailable("Ad closed early without reward")
                        }
                    }
                }
            })
            return
        }

        // AdMob candidate
        val cachedAd = preloadedRewarded[effectiveId]
        if (cachedAd != null) {
            preloadedRewarded.remove(effectiveId)
            cachedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    preloadRewardedAd(unit) // Reload
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(tag, "AdMob show failed: ${error.message}, trying next candidate")
                    showAdCandidate(activity, candidates, index + 1, onRewardEarned, onFailedOrUnavailable)
                }
            }

            var rewardGranted = false
            cachedAd.show(activity) { _ ->
                rewardGranted = true
                onRewardEarned()
            }
        } else {
            // Not preloaded; load on the fly
            val request = AdRequest.Builder().build()
            RewardedAd.load(activity, effectiveId, request, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            preloadRewardedAd(unit)
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            showAdCandidate(activity, candidates, index + 1, onRewardEarned, onFailedOrUnavailable)
                        }
                    }
                    ad.show(activity) {
                        onRewardEarned()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(tag, "AdMob load failed: ${error.message}, trying next candidate")
                    showAdCandidate(activity, candidates, index + 1, onRewardEarned, onFailedOrUnavailable)
                }
            })
        }
    }

    // Show Interstitial (respects interstitialFrequency)
    fun showInterstitialIfAllowed(
        activity: Activity,
        placement: String = "INTERSTITIAL_AFTER_GAME",
        onAdClosed: () -> Unit = {}
    ) {
        if (!_adConfig.value.adsEnabled) {
            onAdClosed()
            return
        }

        interstitialTriggerCount++
        val frequency = _adConfig.value.interstitialFrequency.coerceAtLeast(1)
        if (interstitialTriggerCount % frequency != 0) {
            // Not yet time for interstitial
            onAdClosed()
            return
        }

        val units = getUnitsForPlacement(placement)
        if (units.isEmpty()) {
            onAdClosed()
            return
        }

        val unit = units.first()
        val effectiveId = getEffectiveUnitId(unit)

        val cached = preloadedInterstitial[effectiveId]
        if (cached != null) {
            preloadedInterstitial.remove(effectiveId)
            cached.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    preloadInterstitialAd(unit)
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    preloadInterstitialAd(unit)
                    onAdClosed()
                }
            }
            cached.show(activity)
        } else {
            // Unity or fallback
            if (unit.provider.equals("UNITY", ignoreCase = true)) {
                UnityAds.show(activity, effectiveId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                        activity.runOnUiThread { onAdClosed() }
                    }
                    override fun onUnityAdsShowStart(placementId: String?) {}
                    override fun onUnityAdsShowClick(placementId: String?) {}
                    override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                        activity.runOnUiThread { onAdClosed() }
                    }
                })
            } else {
                onAdClosed()
            }
        }
    }
}
