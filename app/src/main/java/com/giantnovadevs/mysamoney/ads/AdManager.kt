package com.giantnovadevs.mysamoney.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private val TAG = "AdManager"

    // Use the official Google Test Ad Unit ID for Rewarded Ads
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    /**
     * Loads a new Rewarded Ad into memory.
     */
    fun loadRewardedAd() {
        if (isLoading) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "RewardedAd failed to load: ${adError.message}")
                rewardedAd = null
                isLoading = false
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.i(TAG, "RewardedAd loaded successfully.")
                rewardedAd = ad
                isLoading = false
            }
        })
    }

    /**
     * Shows the ad if it's loaded.
     * @param onRewardEarned A callback function to be executed when the user
     * successfully finishes the ad.
     */
    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (rewardedAd == null) {
            Log.e(TAG, "Rewarded ad is not ready to be shown.")
            // Ad not loaded, so let's try loading another one
            loadRewardedAd()
            return
        }

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Ad was dismissed. Pre-load the next one.
                rewardedAd = null
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "RewardedAd failed to show: ${adError.message}")
                rewardedAd = null
                loadRewardedAd()
            }
        }

        // Show the ad
        rewardedAd?.show(activity) { rewardItem ->
            // This is the "reward" callback
            Log.i(TAG, "User earned reward. Amount: ${rewardItem.amount}")
            onRewardEarned()
        }
    }
}