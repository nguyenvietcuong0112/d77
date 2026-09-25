package com.football.news.livescore.ui.onboarding.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.football.news.livescore.ui.main.MainActivity
import com.football.news.livescore.R
import com.football.news.livescore.base.AbsBaseFragment
import com.football.news.livescore.data.local.RequestLimitManager
import com.football.news.livescore.data.remote.RemoteConfigManager
import com.football.news.livescore.databinding.FragmentIntro4Binding
import com.football.news.livescore.utils.LogEvent
import com.football.news.livescore.ui.iap.IAPActivity
import com.football.news.livescore.ui.onboarding.IntroActivity
import com.football.news.livescore.ui.permission.PermissionActivity
import com.football.news.livescore.utils.ActivityFullCallback
import com.football.news.livescore.utils.ActivityLoadNativeFullV1.Companion.open
import com.football.news.livescore.utils.InterCallback
import com.football.news.livescore.utils.NativeCallback
import com.football.news.livescore.utils.Admob
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FragmentIntro4 : AbsBaseFragment<FragmentIntro4Binding?>() {
    @Inject
    lateinit var limitManager: RequestLimitManager

    override fun getLayout(): Int {
        return R.layout.fragment_intro4
    }

    override fun initView() {
        binding!!.txtNext.setOnClickListener(View.OnClickListener { v: View? -> navigateNextScreen() })
        loadAds()
    }

    private fun navigateNextScreen() {
        val onboardingPrefs = requireActivity().getSharedPreferences(
            "livescore_onboarding_prefs",
            Context.MODE_PRIVATE
        )
        onboardingPrefs.edit().putBoolean("onboarding_completed", true).apply()

        val showIap = try {
            RemoteConfigManager.getInstance().showOnboardingIap()
        } catch (e: Exception) {
            true
        }

        val intent: Intent = if (hasNotificationPermission()) {
            if (limitManager.isPremium() || !showIap) {
                Intent(requireActivity(), MainActivity::class.java)
            } else {
                Intent(requireActivity(), IAPActivity::class.java).apply {
                    putExtra("FROM_ONBOARDING", true)
                }
            }
        } else {
            Intent(requireActivity(), PermissionActivity::class.java)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun showLoadingNext(isLoading: Boolean) {
        if (isLoading) {
            binding?.txtNext?.text = ""
            binding?.txtNext?.isClickable = false
            binding?.loadingNext?.visibility = View.VISIBLE
        } else {
            binding?.txtNext?.text = getString(R.string.intro_next)
            binding?.txtNext?.isClickable = true
            binding?.loadingNext?.visibility = View.GONE
        }
    }

    private fun loadAds() {
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            binding!!.frAds.visibility = View.GONE
            return
        }
        val adId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_onboarding_4", getString(R.string.native_onboarding_4))
        } catch (e: Exception) {
            getString(R.string.native_onboarding_4)
        }
        if (adId.isNotEmpty()) {
            showLoadingNext(true)
            Admob.getInstance().loadNativeAd(
                requireActivity(),
                adId,
                object : NativeCallback() {
                    override fun onAdFailedToLoad() {
                        super.onAdFailedToLoad()
                        if (!isAdded) return
                        showLoadingNext(false)
                        binding!!.frAds.removeAllViews()
                        binding!!.frAds.setVisibility(View.GONE)
                    }

                    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                        super.onNativeAdLoaded(nativeAd)
                        if (!isAdded) return
                        
                        val adView = LayoutInflater.from(requireActivity())
                            .inflate(R.layout.layout_native_media, null) as NativeAdView?

                        binding!!.frAds.removeAllViews()
                        binding!!.frAds.addView(adView)
                        if (nativeAd != null && adView != null) {
                            Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                        }
                        context?.let { LogEvent.log(it, "native_onboarding_4") }

                        binding!!.frAds.postDelayed({
                            showLoadingNext(false)
                        }, 500)
                    }
                })
        } else {
            binding!!.frAds.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
    }
}