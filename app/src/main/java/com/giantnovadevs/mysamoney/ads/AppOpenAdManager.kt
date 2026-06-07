package com.giantnovadevs.mysamoney.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

object AppOpenAdManager {
    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var isShowingAd = false
    private var pendingShow = false
    private var pendingActivity: Activity? = null
    private var pendingShouldShow: (() -> Boolean)? = null

    fun load(context: Context) {
        if (isLoading || appOpenAd != null) return
        isLoading = true

        AppOpenAd.load(
            context,
            AdUnitIds.appOpen(context),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoading = false
                    if (pendingShow) {
                        val shouldShow = pendingShouldShow
                        pendingShow = false
                        pendingShouldShow = null
                        // Re-check at show time — billing may have responded by now
                        if (shouldShow?.invoke() != false) {
                            pendingActivity?.let { showIfAvailable(it) }
                        }
                        pendingActivity = null
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoading = false
                    pendingShow = false
                    pendingActivity = null
                    pendingShouldShow = null
                }
            }
        )
    }

    /**
     * @param shouldShow checked immediately AND again when the ad finishes loading (if still pending).
     *                   Pass `{ !proViewModel.isProUser.value }` so late billing responses are respected.
     */
    fun showIfAvailable(activity: Activity, shouldShow: () -> Boolean = { true }, onDismissed: (() -> Unit)? = null) {
        if (isShowingAd) return
        if (!shouldShow()) return

        val ad = appOpenAd
        if (ad == null) {
            if (isLoading) {
                pendingShow = true
                pendingActivity = activity
                pendingShouldShow = shouldShow
            } else {
                load(activity)
            }
            onDismissed?.invoke()
            return
        }

        isShowingAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                load(activity)
                onDismissed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                appOpenAd = null
                isShowingAd = false
                load(activity)
                onDismissed?.invoke()
            }
        }
        ad.show(activity)
    }
}
