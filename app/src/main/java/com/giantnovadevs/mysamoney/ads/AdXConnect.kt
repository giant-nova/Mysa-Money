package com.giantnovadevs.mysamoney.ads

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.mediation.*
import com.google.android.gms.ads.mediation.customevent.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.gms.ads.VersionInfo
import com.google.android.gms.ads.rewarded.RewardItem

/**
 * AdXConnect: A Unified Custom Event Bridge for AdMob.
 * - Extends Adapter: Required for Modern Rewarded, Rewarded Interstitial, and App Open formats.
 * - Implements CustomEvent*: Required for Legacy Banner, Interstitial, and Native formats.
 */
@Suppress("DEPRECATION")
class AdXConnect : Adapter(), CustomEventBanner, CustomEventInterstitial, CustomEventNative,
    MediationRewardedAd, MediationAppOpenAd {

    private val TAG = "AdXConnect"

    // --- BANNER VARS ---
    private var adManagerAdView: AdManagerAdView? = null

    // --- STANDARD INTERSTITIAL VARS ---
    private var adManagerInterstitialAd: AdManagerInterstitialAd? = null
    private var interstitialContext: Context? = null

    // --- NATIVE VARS ---
    private var nativeAdLoader: AdLoader? = null
    private var activeNativeAd: NativeAd? = null

    // --- REWARDED VARS ---
    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var rewardedAdCallback: MediationRewardedAdCallback? = null

    // --- APP OPEN VARS ---
    private var appOpenAd: AppOpenAd? = null
    private var appOpenAdCallback: MediationAppOpenAdCallback? = null


    // ==========================================
    //      ADAPTER INITIALIZATION (MODERN)
    // ==========================================
    override fun getVersionInfo() = VersionInfo(1, 0, 0)
    override fun getSDKVersionInfo() = VersionInfo(1, 0, 0)

    override fun initialize(
        context: Context,
        initializationCompleteCallback: InitializationCompleteCallback,
        mediationConfigurations: MutableList<MediationConfiguration>
    ) {
        Log.d(TAG, "initialize() called. mediationConfigurations=${mediationConfigurations.size}")
        initializationCompleteCallback.onInitializationSucceeded()
        Log.d(TAG, "initialize() success")
    }


    // ==========================================
    //              1. BANNER LOGIC
    // ==========================================
    override fun requestBannerAd(
        context: Context,
        listener: CustomEventBannerListener,
        serverParameter: String?,
        adSize: AdSize,
        mediationAdRequest: MediationAdRequest,
        customEventExtras: Bundle?
    ) {
        Log.d(
            TAG,
            "requestBannerAd() called. adUnit=${serverParameter ?: "null"}, size=${adSize.width}x${adSize.height}, extras=${customEventExtras?.keySet()?.joinToString() ?: "none"}"
        )
        if (serverParameter.isNullOrEmpty()) {
            Log.e(TAG, "Banner failed: Missing Ad Unit ID")
            listener.onAdFailedToLoad(LoadAdError(0, "No Ad Unit ID", TAG, null, null))
            return
        }

        adManagerAdView = AdManagerAdView(context)
        adManagerAdView?.adUnitId = serverParameter
        adManagerAdView?.setAdSizes(adSize)
        Log.d(TAG, "Banner AdView configured. adUnit=$serverParameter")

        adManagerAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner loaded. adUnit=$serverParameter")
                listener.onAdLoaded(adManagerAdView!!)
            }
            override fun onAdFailedToLoad(e: LoadAdError) {
                Log.e(TAG, "Banner failed. code=${e.code}, message=${e.message}")
                listener.onAdFailedToLoad(e)
            }
            override fun onAdClicked() {
                Log.d(TAG, "Banner clicked")
                listener.onAdClicked()
            }
            override fun onAdOpened() {
                Log.d(TAG, "Banner opened")
                listener.onAdOpened()
            }
            override fun onAdClosed() {
                Log.d(TAG, "Banner closed")
                listener.onAdClosed()
            }
        }

        Log.d(TAG, "Requesting GAM Banner: $serverParameter")
        adManagerAdView?.loadAd(AdManagerAdRequest.Builder().build())
    }


    // ==========================================
    //           2. INTERSTITIAL LOGIC
    // ==========================================
    override fun requestInterstitialAd(
        context: Context,
        listener: CustomEventInterstitialListener,
        serverParameter: String?,
        mediationAdRequest: MediationAdRequest,
        customEventExtras: Bundle?
    ) {
        Log.d(
            TAG,
            "requestInterstitialAd() called. adUnit=${serverParameter ?: "null"}, extras=${customEventExtras?.keySet()?.joinToString() ?: "none"}"
        )
        interstitialContext = context

        if (serverParameter.isNullOrEmpty()) {
            Log.e(TAG, "Interstitial failed: Missing Ad Unit ID")
            listener.onAdFailedToLoad(LoadAdError(0, "No Ad Unit ID", TAG, null, null))
            return
        }

        val request = AdManagerAdRequest.Builder().build()
        Log.d(TAG, "Requesting GAM Interstitial: $serverParameter")
        AdManagerInterstitialAd.load(context, serverParameter, request, object : AdManagerInterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: AdManagerInterstitialAd) {
                Log.d(TAG, "Interstitial loaded. adUnit=$serverParameter")
                adManagerInterstitialAd = ad
                adManagerInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial showed")
                        listener.onAdOpened()
                    }
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial dismissed")
                        listener.onAdClosed()
                    }
                    override fun onAdClicked() {
                        Log.d(TAG, "Interstitial clicked")
                        listener.onAdClicked()
                    }
                }
                listener.onAdLoaded()
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Interstitial failed. code=${error.code}, message=${error.message}")
                listener.onAdFailedToLoad(error)
            }
        })
    }

    override fun showInterstitial() {
        if (adManagerInterstitialAd != null && interstitialContext is Activity) {
            Log.d(TAG, "Showing GAM Interstitial")
            adManagerInterstitialAd?.show(interstitialContext as Activity)
        } else {
            Log.e(TAG, "Failed to show Interstitial: Context is not Activity or Ad is null")
        }
    }


    // ==========================================
    //             3. NATIVE LOGIC
    // ==========================================
    override fun requestNativeAd(
        context: Context,
        listener: CustomEventNativeListener,
        serverParameter: String?,
        mediationAdRequest: NativeMediationAdRequest,
        customEventExtras: Bundle?
    ) {
        Log.d(
            TAG,
            "requestNativeAd() called. adUnit=${serverParameter ?: "null"}, extras=${customEventExtras?.keySet()?.joinToString() ?: "none"}"
        )
        if (serverParameter.isNullOrEmpty()) {
            Log.e(TAG, "Native failed: Missing Ad Unit ID")
            listener.onAdFailedToLoad(LoadAdError(0, "No Ad Unit ID", TAG, null, null))
            return
        }

        val options = NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build()

        nativeAdLoader = AdLoader.Builder(context, serverParameter)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native loaded. adUnit=$serverParameter")
                activeNativeAd = nativeAd
                listener.onAdLoaded(GamNativeAdMapper(nativeAd))
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(e: LoadAdError) {
                    Log.e(TAG, "Native failed. code=${e.code}, message=${e.message}")
                    listener.onAdFailedToLoad(e)
                }
                override fun onAdClicked() {
                    Log.d(TAG, "Native clicked")
                    listener.onAdClicked()
                }
                override fun onAdOpened() {
                    Log.d(TAG, "Native opened")
                    listener.onAdOpened()
                }
                override fun onAdClosed() {
                    Log.d(TAG, "Native closed")
                    listener.onAdClosed()
                }
            })
            .withNativeAdOptions(options)
            .build()

        Log.d(TAG, "Requesting GAM Native: $serverParameter")
        nativeAdLoader?.loadAd(AdManagerAdRequest.Builder().build())
    }

    // --- NATIVE MAPPER ---
    private inner class GamNativeAdMapper(private val gamAd: NativeAd) : UnifiedNativeAdMapper() {
        init {
            gamAd.headline?.let { headline = it }
            gamAd.body?.let { body = it }
            gamAd.callToAction?.let { callToAction = it }
            gamAd.advertiser?.let { advertiser = it }
            gamAd.price?.let { price = it }
            gamAd.store?.let { store = it }
            gamAd.starRating?.let { starRating = it }

            gamAd.icon?.let { modernIcon ->
                icon = object : com.google.android.gms.ads.formats.NativeAd.Image() {
                    override fun getDrawable() = modernIcon.drawable ?: ColorDrawable(Color.TRANSPARENT)
                    override fun getUri() = modernIcon.uri ?: Uri.EMPTY
                    override fun getScale() = modernIcon.scale
                }
            }

            if (gamAd.images.isNotEmpty()) {
                val legacyImagesList = mutableListOf<com.google.android.gms.ads.formats.NativeAd.Image>()
                for (modernImg in gamAd.images) {
                    legacyImagesList.add(object : com.google.android.gms.ads.formats.NativeAd.Image() {
                        override fun getDrawable() = modernImg.drawable ?: ColorDrawable(Color.TRANSPARENT)
                        override fun getUri() = modernImg.uri ?: Uri.EMPTY
                        override fun getScale() = modernImg.scale
                    })
                }
                images = legacyImagesList
            }

            setHasVideoContent(gamAd.mediaContent?.hasVideoContent() ?: false)
            overrideImpressionRecording = false
            overrideClickHandling = false
        }

        override fun trackViews(
            containerView: View,
            clickableAssetViews: MutableMap<String, View>,
            nonclickableAssetViews: MutableMap<String, View>
        ) {
            Log.d(
                TAG,
                "Native trackViews(). clickable=${clickableAssetViews.size}, nonclickable=${nonclickableAssetViews.size}"
            )
            super.trackViews(containerView, clickableAssetViews, nonclickableAssetViews)
        }
    }


    // ==========================================
    //    4. REWARDED & APP OPEN LOAD LOGIC
    // ==========================================
    override fun loadRewardedAd(
        configuration: MediationRewardedAdConfiguration,
        callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
    ) {
        Log.d(TAG, "loadRewardedAd() called")
        val adUnit = configuration.serverParameters.getString("parameter")
        if (adUnit.isNullOrEmpty()) {
            Log.e(TAG, "Rewarded failed: Missing Ad Unit ID")
            callback.onFailure(AdError(0, "No Ad Unit ID", TAG))
            return
        }

        Log.d(TAG, "Requesting GAM Rewarded: $adUnit")
        RewardedAd.load(configuration.context, adUnit, AdManagerAdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Rewarded loaded. adUnit=$adUnit")
                rewardedAd = ad
                rewardedAdCallback = callback.onSuccess(this@AdXConnect)
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Rewarded failed. code=${error.code}, message=${error.message}")
                callback.onFailure(error)
            }
        })
    }

    override fun loadRewardedInterstitialAd(
        configuration: MediationRewardedAdConfiguration,
        callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
    ) {
        Log.d(TAG, "loadRewardedInterstitialAd() called")
        val adUnit = configuration.serverParameters.getString("parameter")
        if (adUnit.isNullOrEmpty()) {
            Log.e(TAG, "Rewarded Interstitial failed: Missing Ad Unit ID")
            callback.onFailure(AdError(0, "No Ad Unit ID", TAG))
            return
        }

        Log.d(TAG, "Requesting GAM Rewarded Interstitial: $adUnit")
        RewardedInterstitialAd.load(configuration.context, adUnit, AdManagerAdRequest.Builder().build(), object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                Log.d(TAG, "Rewarded Interstitial loaded. adUnit=$adUnit")
                rewardedInterstitialAd = ad
                rewardedAdCallback = callback.onSuccess(this@AdXConnect)
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Rewarded Interstitial failed. code=${error.code}, message=${error.message}")
                callback.onFailure(error)
            }
        })
    }

    // NEW: App Open Loading
    override fun loadAppOpenAd(
        configuration: MediationAppOpenAdConfiguration,
        callback: MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback>
    ) {
        Log.d(TAG, "loadAppOpenAd() called")
        val adUnit = configuration.serverParameters.getString("parameter")
        if (adUnit.isNullOrEmpty()) {
            Log.e(TAG, "App Open failed: Missing Ad Unit ID")
            callback.onFailure(AdError(0, "No Ad Unit ID", TAG))
            return
        }

        Log.d(TAG, "Requesting GAM App Open Ad: $adUnit")
        AppOpenAd.load(
            configuration.context,
            adUnit,
            AdManagerAdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open loaded. adUnit=$adUnit")
                    appOpenAd = ad
                    appOpenAdCallback = callback.onSuccess(this@AdXConnect)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "App Open failed. code=${error.code}, message=${error.message}")
                    callback.onFailure(error)
                }
            }
        )
    }

    // ==========================================
    //   5. SHOW LOGIC (Rewarded & App Open)
    // ==========================================
    // Both MediationRewardedAd and MediationAppOpenAd use this exact same method signature.
    // We route the request based on which ad object was successfully loaded.
    override fun showAd(context: Context) {
        Log.d(
            TAG,
            "showAd() called. rewardedLoaded=${rewardedAd != null}, rewardedInterstitialLoaded=${rewardedInterstitialAd != null}, appOpenLoaded=${appOpenAd != null}"
        )
        if (context !is Activity) {
            Log.e(TAG, "Show failed: Context is not an Activity")
            rewardedAdCallback?.onAdFailedToShow(AdError(1, "Context is not an Activity", TAG))
            appOpenAdCallback?.onAdFailedToShow(AdError(1, "Context is not an Activity", TAG))
            return
        }

        when {
            // Path A: It's a Rewarded Ad
            rewardedAd != null -> {
                Log.d(TAG, "Showing GAM Rewarded")
                rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Rewarded showed")
                        rewardedAdCallback?.onAdOpened()
                        rewardedAdCallback?.reportAdImpression()
                    }
                    override fun onAdClicked() {
                        Log.d(TAG, "Rewarded clicked")
                        rewardedAdCallback?.reportAdClicked()
                    }
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Rewarded dismissed")
                        rewardedAdCallback?.onAdClosed()
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.e(TAG, "Rewarded failed to show. code=${error.code}, message=${error.message}")
                        rewardedAdCallback?.onAdFailedToShow(error)
                    }
                }

                rewardedAd?.show(context) { gamReward ->
                    Log.d(TAG, "Rewarded earned. type=${gamReward.type}, amount=${gamReward.amount}")
                    val adMobReward = object : RewardItem {
                        override fun getAmount() = gamReward.amount
                        override fun getType() = gamReward.type
                    }
                    rewardedAdCallback?.onUserEarnedReward(adMobReward)
                    rewardedAdCallback?.onVideoComplete()
                }
            }

            // Path B: It's a Rewarded Interstitial
            rewardedInterstitialAd != null -> {
                Log.d(TAG, "Showing GAM Rewarded Interstitial")
                rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Rewarded Interstitial showed")
                        rewardedAdCallback?.onAdOpened()
                        rewardedAdCallback?.reportAdImpression()
                    }
                    override fun onAdClicked() {
                        Log.d(TAG, "Rewarded Interstitial clicked")
                        rewardedAdCallback?.reportAdClicked()
                    }
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Rewarded Interstitial dismissed")
                        rewardedAdCallback?.onAdClosed()
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.e(TAG, "Rewarded Interstitial failed to show. code=${error.code}, message=${error.message}")
                        rewardedAdCallback?.onAdFailedToShow(error)
                    }
                }

                rewardedInterstitialAd?.show(context) { gamReward ->
                    Log.d(TAG, "Rewarded Interstitial earned. type=${gamReward.type}, amount=${gamReward.amount}")
                    val adMobReward = object : RewardItem {
                        override fun getAmount() = gamReward.amount
                        override fun getType() = gamReward.type
                    }
                    rewardedAdCallback?.onUserEarnedReward(adMobReward)
                    rewardedAdCallback?.onVideoComplete()
                }
            }

            // Path C: It's an App Open Ad
            appOpenAd != null -> {
                Log.d(TAG, "Showing GAM App Open")
                appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "App Open showed")
                        appOpenAdCallback?.onAdOpened()
                        appOpenAdCallback?.reportAdImpression()
                    }
                    override fun onAdClicked() {
                        Log.d(TAG, "App Open clicked")
                        appOpenAdCallback?.reportAdClicked()
                    }
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "App Open dismissed")
                        appOpenAdCallback?.onAdClosed()
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        Log.e(TAG, "App Open failed to show. code=${error.code}, message=${error.message}")
                        appOpenAdCallback?.onAdFailedToShow(error)
                    }
                }

                appOpenAd?.show(context)
            }

            else -> {
                Log.e(TAG, "No ad loaded to show")
                rewardedAdCallback?.onAdFailedToShow(AdError(2, "No ad loaded", TAG))
                appOpenAdCallback?.onAdFailedToShow(AdError(2, "No ad loaded", TAG))
            }
        }
    }


    // ==========================================
    //              6. LIFECYCLE
    // ==========================================
    override fun onDestroy() {
        Log.d(TAG, "onDestroy() clearing ad resources")
        adManagerAdView?.destroy()
        adManagerAdView = null

        adManagerInterstitialAd = null
        interstitialContext = null

        activeNativeAd?.destroy()
        activeNativeAd = null

        rewardedAd = null
        rewardedInterstitialAd = null
        rewardedAdCallback = null

        appOpenAd = null
        appOpenAdCallback = null
    }

    override fun onPause() {
        Log.d(TAG, "onPause() pausing banner view")
        adManagerAdView?.pause()
    }

    override fun onResume() {
        Log.d(TAG, "onResume() resuming banner view")
        adManagerAdView?.resume()
    }
}