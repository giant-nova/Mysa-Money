package com.giantnovadevs.mysamoney.ads

import android.content.Context
import androidx.annotation.StringRes
import com.giantnovadevs.mysamoney.BuildConfig
import com.giantnovadevs.mysamoney.R

/**
 * Single source of ad unit IDs. Debug builds use Google's sample units while
 * BuildConfig.USE_TEST_ADS is true (toggle with USE_TEST_ADS in local.properties).
 * Release builds always get the real units from strings.xml.
 */
object AdUnitIds {
    private fun resolve(context: Context, test: String?, @StringRes real: Int): String =
        if (BuildConfig.DEBUG && BuildConfig.USE_TEST_ADS && test != null) test
        else context.getString(real)

    fun banner(context: Context) =
        resolve(context, TestAdUnits.banner, R.string.admob_banner_ad_unit_id)

    fun mediumRectangle(context: Context) =
        resolve(context, TestAdUnits.mediumRectangle, R.string.admob_medium_rectangle_ad_unit_id)

    fun interstitial(context: Context) =
        resolve(context, TestAdUnits.interstitial, R.string.admob_interstitial_ad_unit_id)

    fun rewarded(context: Context) =
        resolve(context, TestAdUnits.rewarded, R.string.admob_rewarded_ad_unit_id)

    fun rewardedInterstitial(context: Context) =
        resolve(context, TestAdUnits.rewardedInterstitial, R.string.admob_rewarded_interstitial_ad_unit_id)

    fun native(context: Context) =
        resolve(context, TestAdUnits.native, R.string.admob_native_ad_unit_id)

    fun appOpen(context: Context) =
        resolve(context, TestAdUnits.appOpen, R.string.admob_app_open_ad_unit_id)
}
