package com.giantnovadevs.mysamoney.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

object RewardedInterstitialAdManager {
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoading = false

    fun load(context: Context) {
        if (isLoading || rewardedInterstitialAd != null) return
        isLoading = true

        RewardedInterstitialAd.load(
            context,
            AdUnitIds.rewardedInterstitial(context),
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInterstitialAd = null
                    isLoading = false
                }

                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isLoading = false
                }
            }
        )
    }

    fun showIfAvailable(activity: Activity, onRewardEarned: (() -> Unit)? = null, onDismissed: (() -> Unit)? = null) {
        val ad = rewardedInterstitialAd
        if (ad == null) {
            onDismissed?.invoke()
            load(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                load(activity)
                onDismissed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                rewardedInterstitialAd = null
                load(activity)
                onDismissed?.invoke()
            }
        }

        ad.show(activity) {
            onRewardEarned?.invoke()
        }
    }
}
