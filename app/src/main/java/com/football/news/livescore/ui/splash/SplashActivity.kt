package com.football.news.livescore.ui.splash

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.messaging.FirebaseMessaging
import com.football.news.livescore.R
import com.football.news.livescore.base.BaseActivity
import com.football.news.livescore.data.local.RequestLimitManager
import com.football.news.livescore.data.remote.DeviceRegistrationManager
import com.football.news.livescore.data.remote.RemoteConfigManager
import com.football.news.livescore.data.remote.adjust.RetentionTracker
import com.football.news.livescore.data.remote.getRemoteAdId
import com.football.news.livescore.databinding.ActivitySplashBinding
import com.football.news.livescore.ui.language.LanguageActivity
import com.football.news.livescore.ui.main.MainActivity
import com.football.news.livescore.utils.ActivityFullCallback
import com.football.news.livescore.utils.ActivityLoadNativeFullV1
import com.football.news.livescore.utils.SharePreferenceUtils
import com.football.news.livescore.data.remote.PushClickTracker
import com.football.news.livescore.utils.LogEvent
import com.football.news.livescore.utils.PushDataParser
import com.football.news.livescore.utils.PushNavigationExecutor
import com.football.news.livescore.utils.PushPayload
import com.google.android.gms.ads.nativead.NativeAd
import com.football.news.livescore.utils.AdsConfig
import com.cscmobi.libraryads.ads.banner_ads.CSCBanner
import com.cscmobi.libraryads.ads.inter_ads.CSCInter
import com.cscmobi.libraryads.commons.consents.GoogleMobileAdsConsentManager
import com.football.news.livescore.utils.NativeCallback
import com.football.news.livescore.utils.InterCallback
import com.football.news.livescore.utils.Admob
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    companion object {
        private const val PREFS_NAME = "livescore_onboarding_prefs"
        private const val KEY_APP_LAUNCH_COUNT = "app_launch_count"
    }

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var limitManager: RequestLimitManager

    @Inject
    lateinit var deviceRegistrationManager: DeviceRegistrationManager

    @Inject
    lateinit var liveScoreApiService: com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    @Inject
    lateinit var pushClickTracker: PushClickTracker

    private lateinit var binding: ActivitySplashBinding
    private var interCallback: InterCallback? = null
    private var isTransitioning = false
    private var pendingPushPayload: PushPayload? = null

    override fun bind() {
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pendingPushPayload = PushDataParser.parseFromIntent(intent)
        pendingPushPayload?.let { payload ->
            android.util.Log.d("FCMService", "SplashActivity launched via push: push_id=${payload.pushId}, push_type=${payload.rawData["push_type"]}, navigation=${payload.navigation}")
        }
        trackPushClickIfNeeded()
        trackAppLaunch()

        lifecycleScope.launch(Dispatchers.IO) {
            RetentionTracker.checkAndTrackRetention(this@SplashActivity)
        }

        lifecycleScope.launch {
            deviceRegistrationManager.registerDevice(null)
        }

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrEmpty()) {
                        android.util.Log.d("FCMToken", "Device FCM Token: $token")
                        lifecycleScope.launch {
                            deviceRegistrationManager.registerDevice(token)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        lifecycleScope.launch {
            remoteConfigManager.fetchAndActivate()

            runOnUiThread {
                initAdsAfterConfig()
            }
        }
    }

    private fun initAdsAfterConfig() {
        if (limitManager.isPremium()) {
            binding.frAdsBanner.visibility = View.GONE
        } else {
            binding.frAdsBanner.visibility = View.VISIBLE
        }

        var actualLoadedAdId = remoteConfigManager.getAdId("inter_splash", getString(R.string.inter_splash))

        interCallback = object : InterCallback() {
            override fun onInterstitialLoad(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                super.onInterstitialLoad(interstitialAd)
                actualLoadedAdId = interstitialAd.adUnitId
            }

            override fun onAdClosedByUser() {
                super.onAdClosedByUser()
                LogEvent.log(this@SplashActivity, "inter_splash_view")

                com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadSuccess(
                    liveScoreApiService, this@SplashActivity, "interstitial", actualLoadedAdId, "Splash"
                )
                com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                    liveScoreApiService, this@SplashActivity, "interstitial", actualLoadedAdId, "Splash"
                )

                if (!SharePreferenceUtils.isOrganic(applicationContext)) {
                    ActivityLoadNativeFullV1.Companion.open(
                        this@SplashActivity,
                        getRemoteAdId("native_splash_full_high", R.string.native_splash_full_high),
                        getRemoteAdId("native_splash_full", R.string.native_splash_full),
                        object : ActivityFullCallback {
                            override fun onResultFromActivityFull() {
                                navigateAfterSplash()
                            }
                        })
                } else {
                    navigateAfterSplash()
                }

            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadFailed(
                    liveScoreApiService, this@SplashActivity, "interstitial", actualLoadedAdId, "Splash", i?.code
                )

                if (!SharePreferenceUtils.isOrganic(applicationContext)) {
                    ActivityLoadNativeFullV1.Companion.open(
                        this@SplashActivity,
                        getRemoteAdId("native_splash_full_high", R.string.native_splash_full_high),
                        getRemoteAdId("native_splash_full",     R.string.native_splash_full),
                        object : ActivityFullCallback {
                            override fun onResultFromActivityFull() {
                                navigateAfterSplash()
                            }
                        })
                } else {
                    navigateAfterSplash()
                }
            }
        }
        loadAdsInter()
    }


    private fun loadAdsInter() {
        val isVIP = limitManager.isPremium()

        lifecycleScope.launch {
            for (progress in 0..99) {
                binding.tvProgressPercent.text = "$progress%"
                delay(if (isVIP) 10L else 150L)
            }
            if (isVIP) {
                navigateAfterSplash()
            }
        }

        if (isVIP) {
            return
        }

        val consentManager = GoogleMobileAdsConsentManager.getInstance(this)
        consentManager.gatherConsent(this) { consentError ->
            if (consentError != null) {
                android.util.Log.d("SplashActivity", "Consent gathering error: ${consentError.message}")
            }

            // Load banner ad after consent is obtained
            if (!limitManager.isPremium()) {
                val bannerId = getRemoteAdId("banner_splash", R.string.banner_splash)
                com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
                    liveScoreApiService, this@SplashActivity, "banner", bannerId, "Splash"
                )
                CSCBanner.requestBanner(
                    activity = this@SplashActivity,
                    id = bannerId,
                    typeAds = CSCBanner.TypeAds.BANNER_NORMAL,
                    adFrame = binding.frAdsBanner,
                    canShowAd = true,
                    onResult = {
                        com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                            liveScoreApiService, this@SplashActivity, "banner", bannerId, "Splash"
                        )
                        LogEvent.log(this@SplashActivity, "banner_splash_view")
                    }
                )
            }

            // Load splash interstitial ad after consent is obtained
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    val highId = remoteConfigManager.getAdId("inter_splash_high", getString(R.string.inter_splash_high))
                    val interId = remoteConfigManager.getAdId("inter_splash", getString(R.string.inter_splash))
                    
                    if (highId.isNotEmpty()) {
                        com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
                            liveScoreApiService, this@SplashActivity, "interstitial", highId, "Splash"
                        )
                    }
                    if (interId.isNotEmpty()) {
                        com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdRequest(
                            liveScoreApiService, this@SplashActivity, "interstitial", interId, "Splash"
                        )
                    }

                    CSCInter.loadAndShowInterSplash(
                        activity = this@SplashActivity,
                        idHigh = highId,
                        idAllPrice = interId,
                        timeOut = 30000L,
                        nextAction = { isSuccess ->
                            LogEvent.log(this@SplashActivity, "inter_splash_view")
                            if (!SharePreferenceUtils.isOrganic(applicationContext)) {
                                ActivityLoadNativeFullV1.Companion.open(
                                    this@SplashActivity,
                                    getRemoteAdId("native_splash_full_high", R.string.native_splash_full_high),
                                    getRemoteAdId("native_splash_full", R.string.native_splash_full),
                                    object : ActivityFullCallback {
                                        override fun onResultFromActivityFull() {
                                            navigateAfterSplash()
                                        }
                                    }
                                )
                            } else {
                                navigateAfterSplash()
                            }
                        },
                        onShown = {
                            com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdShow(
                                liveScoreApiService, this@SplashActivity, "interstitial", interId, "Splash"
                            )
                        },
                        onLoadFailed = {
                            com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.AdTrackingHelper.logAdLoadFailed(
                                liveScoreApiService, this@SplashActivity, "interstitial", interId, "Splash", null
                            )
                        }
                    )
                }
            }, 500)
        }
    }

    private fun trackAppLaunch() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val count = prefs.getInt(KEY_APP_LAUNCH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_APP_LAUNCH_COUNT, count).apply()
    }

    private fun isReturningUser(): Boolean {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(KEY_APP_LAUNCH_COUNT, 0) >= 2
    }

    private fun trackPushClickIfNeeded() {
        val pushId = pendingPushPayload?.pushId ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            pushClickTracker.trackClick(pushId)
        }
    }

    private fun navigateAfterSplash() {
        if (isTransitioning) return
        isTransitioning = true

        val pushPayload = pendingPushPayload
        val intent = if (pushPayload != null) {
            PushNavigationExecutor.toDestinationIntent(this, pushPayload.navigation)
        } else if (isReturningUser()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LanguageActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPushPayload = PushDataParser.parseFromIntent(intent)
        pendingPushPayload?.let { payload ->
            android.util.Log.d("FCMService", "SplashActivity onNewIntent: push_id=${payload.pushId}, push_type=${payload.rawData["push_type"]}, navigation=${payload.navigation}")
        }
        trackPushClickIfNeeded()
        isTransitioning = false
    }


    protected override fun onStop() {
        super.onStop()
        Admob.getInstance().dismissLoadingDialog()
    }

    override fun onResume() {
        super.onResume()
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.ScreenTracker.trackScreenView(
            apiService = liveScoreApiService,
            deviceId = deviceId,
            newScreen = "Splash"
        )
        Admob.getInstance().onCheckShowSplashWhenFail(this, interCallback, 5000)
    }
}