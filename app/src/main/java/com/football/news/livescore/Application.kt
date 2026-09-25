package com.football.news.livescore

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.facebook.FacebookSdk
import com.google.firebase.FirebaseApp
import com.cscmobi.libraryads.CSCApplication
import com.cscmobi.libraryads.data.AdsLanguageConfig
import com.cscmobi.libraryads.data.AdsOBConfig
import com.cscmobi.libraryads.data.AdsSplashConfig
import com.cscmobi.libraryads.data.LanguageConfig
import com.cscmobi.libraryads.data.LanguageSetting
import com.cscmobi.libraryads.data.OBConfig
import com.cscmobi.libraryads.data.OnActivityCallBack
import com.cscmobi.libraryads.data.SplashConfig
import com.cscmobi.libraryads.data.UiLanguageConfig
import com.cscmobi.libraryads.data.UiOBConfig
import com.cscmobi.libraryads.data.UiSplashConfig
import com.football.news.livescore.data.local.BillingManager
import com.football.news.livescore.data.remote.adjust.AppAdjustTokens
import com.football.news.livescore.ui.main.MainActivity
import com.football.news.livescore.utils.RemoteConfigs
import com.football.news.livescore.utils.SharePreferenceUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class Application : android.app.Application() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var gsmManager: com.football.news.livescore.data.remote.gsm.GsmManager

    @Inject
    lateinit var liveScoreApiService: com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.LiveScoreApiService

    override fun onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val processName = android.app.Application.getProcessName()
            if (packageName != processName) {
                val suffix = processName.replace(":", "_")
                try {
                    android.webkit.WebView.setDataDirectorySuffix(suffix)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate()

        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"
        com.football.news.livescore.utils.LivescoreTrackingSDKKotlin.GlobalCrashHandler(
            context = this,
            apiService = liveScoreApiService,
            deviceId = deviceId
        )

        initCSCAds()

        Executors.newSingleThreadExecutor().execute {
            try {
                FirebaseApp.initializeApp(this)
                FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            kotlinx.coroutines.delay(5000)
            try {
                gsmManager.loginGSM()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initCSCAds() {
        try {
            val cscLibrary = CSCApplication(this, RemoteConfigs, BuildConfig.DEBUG)
            cscLibrary.initSdk(
                adjustAppToken = AppAdjustTokens.ADJUST_APP_TOKEN,
                gsmAppId = com.football.news.livescore.data.remote.gsm.GsmConfig.GSM_APP_ID,
                splashConfig = SplashConfig(
                    uiSplashConfig = UiSplashConfig(
                        resLayout = R.layout.activity_splash,
                        showFOForever = true,
                        homeActivity = MainActivity::class.java,
                        timeout = 45_000
                    ),
                    adsSplashConfig = AdsSplashConfig(
                        bannerId = getString(R.string.banner_splash),
                        interHighId = getString(R.string.inter_splash_high),
                        interAllId = getString(R.string.inter_splash),
                        admobAOAId = getString(R.string.resume_open_app),
                        isCheckOrganicUser = true
                    )
                ),
                languageConfig = LanguageConfig(
                    uiLanguageConfig = UiLanguageConfig(
                        resLayout = R.layout.activity_language,
                        itemLangDefault = R.layout.item_language_selection,
                        itemLangSelected = R.layout.item_language_selection,
                        listLanguage = arrayListOf<com.cscmobi.libraryads.data.LanguageModel>(),
                        languageSetting = object : LanguageSetting {
                            override fun onDone(activity: Activity) {}
                        }
                    ),
                    adsLanguageConfig = AdsLanguageConfig(
                        nativeLangHighId = getString(R.string.native_language_high),
                        nativeLangId = getString(R.string.native_language),
                        nativeLangClickHighId = getString(R.string.native_language_click_high),
                        nativeLangClickId = getString(R.string.native_language_click),
                        layoutNative = R.layout.layout_native_media,
                        layoutNativeClick = R.layout.layout_native_media
                    )
                ),
                obConfig = OBConfig(
                    uiOBConfig = UiOBConfig(
                        resFragmentOB1 = R.layout.fragment_intro1,
                        resFragmentOB2 = R.layout.fragment_intro2,
                        resFragmentOB3 = R.layout.fragment_intro3,
                        resFragmentOB4 = R.layout.fragment_intro4,
                        resFragmentOBAdFull = R.layout.native_full_language,
                        activityCallback = object : OnActivityCallBack {
                            override fun onNextActivity(activity: Activity, inSession2: Boolean) {}
                        }
                    ),
                    adsOBConfig = AdsOBConfig(
                        nativeOB1Id = getString(R.string.native_onboarding_1),
                        nativeOB4Id = getString(R.string.native_onboarding_4),
                        nativeOBFull23Id = getString(R.string.native_onboarding_full_1),
                        layoutNativeOB1 = R.layout.layout_native_media,
                        layoutNativeOB4 = R.layout.layout_native_media,
                        layoutNativeFullOB = R.layout.native_full_language,
                        isLoadNativeOBInLanguage = true
                    )
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
