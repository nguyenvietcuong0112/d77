package com.football.news.livescore.ui.onboarding.fragment

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.football.news.livescore.R
import com.football.news.livescore.base.AbsBaseFragment
import com.football.news.livescore.data.remote.RemoteConfigManager
import com.football.news.livescore.utils.LogEvent
import com.football.news.livescore.databinding.FragmentAdsBinding
import com.football.news.livescore.utils.NativeCallback
import com.football.news.livescore.utils.Admob
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FragmentIntro2ads : AbsBaseFragment<FragmentAdsBinding?>() {
    var viewPager: ViewPager2? = null

    @Inject
    lateinit var limitManager: com.football.news.livescore.data.local.RequestLimitManager

    override fun getLayout(): Int {
        return R.layout.fragment_ads
    }

    override fun initView() {
        viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
        if (::limitManager.isInitialized && limitManager.isPremium()) {
            binding!!.frAdsFull.visibility = View.GONE
            return
        }
        val adId = try {
            RemoteConfigManager.getInstance()
                .getAdId("native_onboarding_full_1", getString(R.string.native_onboarding_full_1))
        } catch (e: Exception) {
            getString(R.string.native_onboarding_full_1)
        }
        loadNativeFull(adId)
    }

    private fun loadNativeFull(adId: String?) {
        Admob.getInstance().loadNativeAds(requireActivity(), adId, 1, object : NativeCallback() {
            override fun onAdFailedToLoad() {
                super.onAdFailedToLoad()
                if (!isAdded) return
                binding!!.frAdsFull.setVisibility(View.GONE)
                binding!!.animLoading.setVisibility(View.VISIBLE)
            }

            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                super.onNativeAdLoaded(nativeAd)
                if (!isAdded) return

                val adView = LayoutInflater.from(requireActivity())
                    .inflate(R.layout.native_full_language, null) as NativeAdView
                binding!!.frAdsFull.removeAllViews()
                binding!!.frAdsFull.addView(adView)
                Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)

                val closeButton = adView.findViewById<ImageView>(R.id.close)
                val mediaView = adView.findViewById<MediaView>(R.id.media_view)
                    ?: adView.findViewById<MediaView>(R.id.ad_media)
                closeButton?.setOnClickListener(View.OnClickListener { v: View? -> mediaView?.performClick() })
                object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                    }

                    override fun onFinish() {
                        if (!isAdded) return
                        closeButton?.setOnClickListener(View.OnClickListener { v: View? ->
                            viewPager!!.setCurrentItem(3)
                        })
                    }
                }.start()

                context?.let { LogEvent.log(it, "native_onboarding_full_1") }
            }
        })
    }
}
