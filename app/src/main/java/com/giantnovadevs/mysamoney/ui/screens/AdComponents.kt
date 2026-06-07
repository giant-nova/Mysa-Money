package com.giantnovadevs.mysamoney.ui.screens

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.giantnovadevs.mysamoney.ads.AdUnitIds
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdUnitIds.banner(context)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun AdMobNative(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF6F7F9)),
        factory = {
                val nativeAdView = NativeAdView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                val headlineView = TextView(context).apply {
                    textSize = 16f
                    setPadding(24, 20, 24, 8)
                }

                val mediaView = MediaView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        context.dpToPx(200)
                    )
                    visibility = View.GONE
                }

                val bodyView = TextView(context).apply {
                    textSize = 14f
                    setPadding(24, 8, 24, 8)
                    visibility = View.GONE
                }

                val ctaButton = Button(context).apply {
                    setPadding(24, 8, 24, 20)
                    visibility = View.GONE
                }

                container.addView(headlineView)
                container.addView(mediaView)
                container.addView(bodyView)
                container.addView(ctaButton)
                nativeAdView.addView(container)

                nativeAdView.headlineView = headlineView
                nativeAdView.bodyView = bodyView
                nativeAdView.callToActionView = ctaButton
                // mediaView is NOT registered here — it starts GONE (0×0).
                // Registering it now would make the AdMob validator measure 0×0
                // and flag "MediaView too small for video". It is registered inside
                // forNativeAd only when actual media content is present and the
                // view is already VISIBLE with its full layout params applied.

                val adLoader = AdLoader.Builder(context, AdUnitIds.native(context))
                    .forNativeAd { nativeAd ->
                        headlineView.text = nativeAd.headline

                        val bodyText = nativeAd.body
                        bodyView.text = bodyText ?: ""
                        bodyView.visibility = if (bodyText.isNullOrBlank()) View.GONE else View.VISIBLE

                        val ctaText = nativeAd.callToAction
                        ctaButton.text = ctaText ?: ""
                        ctaButton.visibility = if (ctaText.isNullOrBlank()) View.GONE else View.VISIBLE

                        val media = nativeAd.mediaContent
                        if (media != null) {
                            mediaView.setMediaContent(media)
                            mediaView.visibility = View.VISIBLE
                            // Register only now: view is VISIBLE with 200dp height,
                            // so the validator sees a valid ≥120dp MediaView.
                            nativeAdView.mediaView = mediaView
                        } else {
                            mediaView.visibility = View.GONE
                        }

                        nativeAdView.setNativeAd(nativeAd)
                    }
                    .build()

                adLoader.loadAd(AdRequest.Builder().build())
                nativeAdView
            }
        )
}

@Composable
fun AdMobMediumRectangle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier
            .width(300.dp)
            .height(250.dp),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.MEDIUM_RECTANGLE)
                adUnitId = AdUnitIds.mediumRectangle(context)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

private fun Context.dpToPx(value: Int): Int = (value * resources.displayMetrics.density).toInt()
