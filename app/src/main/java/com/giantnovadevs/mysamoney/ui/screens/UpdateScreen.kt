package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.tooling.preview.Preview
import com.giantnovadevs.mysamoney.ui.theme.MysaMoneyTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giantnovadevs.mysamoney.utils.UpdateState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun UpdateScreen(
    updateState: UpdateState,
    downloadProgress: Float,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit,
    onRestart: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "update")

    // Two sonar-ping rings with staggered timing
    val pulse1Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.55f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseOut), RepeatMode.Restart),
        label = "p1s"
    )
    val pulse1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseOut), RepeatMode.Restart),
        label = "p1a"
    )
    val pulse2Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.55f,
        animationSpec = infiniteRepeatable(tween(2000, delayMillis = 800, easing = EaseOut), RepeatMode.Restart),
        label = "p2s"
    )
    val pulse2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, delayMillis = 800, easing = EaseOut), RepeatMode.Restart),
        label = "p2a"
    )

    // Icon gently floats up and down
    val iconFloat by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )

    // Arc rotates when idle (AVAILABLE state)
    val arcRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "arc_rot"
    )

    // Sparkle alphas — 6 particles with different timing
    val sa1 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, 0), RepeatMode.Reverse), label = "sa1")
    val sa2 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, 400), RepeatMode.Reverse), label = "sa2")
    val sa3 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1900, 200), RepeatMode.Reverse), label = "sa3")
    val sa4 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, 700), RepeatMode.Reverse), label = "sa4")
    val sa5 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1700, 500), RepeatMode.Reverse), label = "sa5")
    val sa6 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, 900), RepeatMode.Reverse), label = "sa6")
    val sparkleAlphas = listOf(sa1, sa2, sa3, sa4, sa5, sa6)

    // Shimmer sweep across the CTA button
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -400f, targetValue = 800f,
        animationSpec = infiniteRepeatable(tween(2400, delayMillis = 600, easing = LinearEasing)),
        label = "shimmer"
    )

    // Smooth progress fill
    val animProgress by animateFloatAsState(
        targetValue = downloadProgress,
        animationSpec = tween(600),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {

        // ── Floating sparkle particles ──────────────────────────────────────
        // (x fraction, y fraction, radius dp)
        val sparkles = remember {
            listOf(
                Triple(0.10f, 0.09f, 7.dp),
                Triple(0.82f, 0.07f, 5.dp),
                Triple(0.55f, 0.14f, 9.dp),
                Triple(0.22f, 0.20f, 4.dp),
                Triple(0.75f, 0.18f, 6.dp),
                Triple(0.42f, 0.06f, 5.dp),
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            sparkles.forEachIndexed { i, (xFrac, yFrac, size) ->
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * xFrac, y = maxHeight * yFrac)
                        .size(size)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = sparkleAlphas[i] * 0.65f))
                )
            }
        }

        // ── Main content ────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {

            Spacer(Modifier.weight(1f))

            // ── Animated icon with progress ring ────────────────────────────
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {

                // Sonar ping rings (behind everything)
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .scale(pulse1Scale)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = pulse1Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .scale(pulse2Scale)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = pulse2Alpha))
                )

                // Progress / rotation arc drawn on Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 9.dp.toPx()
                    val inset = stroke / 2f
                    val arcTopLeft = Offset(inset, inset)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val radius = size.minDimension / 2f - inset

                    // Faint background track
                    drawArc(
                        color = primary.copy(alpha = 0.14f),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = arcTopLeft, size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )

                    when (updateState) {
                        UpdateState.AVAILABLE -> {
                            // Rotating comet arc
                            rotate(arcRotation) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(Color.Transparent, primary.copy(alpha = 0.9f), Color.Transparent)
                                    ),
                                    startAngle = -30f, sweepAngle = 150f, useCenter = false,
                                    topLeft = arcTopLeft, size = arcSize,
                                    style = Stroke(stroke, cap = StrokeCap.Round)
                                )
                            }
                        }
                        UpdateState.DOWNLOADING -> {
                            // Filled progress arc
                            if (animProgress > 0f) {
                                drawArc(
                                    color = primary,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animProgress,
                                    useCenter = false,
                                    topLeft = arcTopLeft, size = arcSize,
                                    style = Stroke(stroke, cap = StrokeCap.Round)
                                )
                                // Glowing tip dot at leading edge
                                val angle = Math.toRadians((-90.0 + 360.0 * animProgress))
                                val tipX = center.x + radius * cos(angle).toFloat()
                                val tipY = center.y + radius * sin(angle).toFloat()
                                drawCircle(color = primary, radius = stroke * 0.9f, center = Offset(tipX, tipY))
                                drawCircle(color = Color.White.copy(alpha = 0.85f), radius = stroke * 0.45f, center = Offset(tipX, tipY))
                            }
                        }
                        UpdateState.DOWNLOADED -> {
                            // Full solid ring in primary color
                            drawArc(
                                color = primary,
                                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                                topLeft = arcTopLeft, size = arcSize,
                                style = Stroke(stroke, cap = StrokeCap.Round)
                            )
                        }
                        else -> Unit
                    }
                }

                // Center circle + icon
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .offset(y = iconFloat.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.11f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = updateState,
                        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(350)) },
                        label = "icon"
                    ) { state ->
                        Icon(
                            imageVector = if (state == UpdateState.DOWNLOADED) Icons.Default.CheckCircle
                            else Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Title & subtitle ─────────────────────────────────────────────
            AnimatedContent(
                targetState = updateState,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically { -20 }) togetherWith
                            (fadeOut(tween(200)) + slideOutVertically { 20 })
                },
                label = "title"
            ) { state ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (state) {
                        UpdateState.AVAILABLE -> {
                            Text(
                                "Update Available",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "A new version is ready to download",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceVariant
                            )
                        }
                        UpdateState.DOWNLOADING -> {
                            Text(
                                "Downloading…",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${(downloadProgress * 100).toInt()}% complete",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceVariant
                            )
                        }
                        UpdateState.DOWNLOADED -> {
                            Text(
                                "Ready to Install!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Restart the app to apply the update",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceVariant
                            )
                        }
                        else -> Spacer(Modifier.height(0.dp))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Feature bullets (only on AVAILABLE state) ────────────────────
            AnimatedVisibility(
                visible = updateState == UpdateState.AVAILABLE,
                enter = fadeIn(tween(400)) + expandVertically(),
                exit = fadeOut(tween(300)) + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    UpdateFeatureBullet(primary, "Performance improvements & stability fixes")
                    UpdateFeatureBullet(primary, "Smoother animations and UI refinements")
                    UpdateFeatureBullet(primary, "New features and enhancements")
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Call-to-action buttons ───────────────────────────────────────
            AnimatedContent(
                targetState = updateState,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically { it / 3 }) togetherWith
                            (fadeOut(tween(250)) + slideOutVertically { -it / 3 })
                },
                label = "cta"
            ) { state ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (state) {
                        UpdateState.AVAILABLE -> {
                            // Shimmer gradient button
                            val shimmerBrush = Brush.linearGradient(
                                colors = listOf(
                                    primary, primary.copy(alpha = 0.8f),
                                    Color.White.copy(alpha = 0.28f),
                                    primary.copy(alpha = 0.8f), primary
                                ),
                                start = Offset(shimmerX, 0f),
                                end = Offset(shimmerX + 350f, 0f)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(shimmerBrush)
                                    .clickable(onClick = onUpdateNow),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Update Now",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            TextButton(onClick = onLater) {
                                Text("Not Now", color = onSurfaceVariant, fontSize = 14.sp)
                            }
                        }

                        UpdateState.DOWNLOADING -> {
                            LinearProgressIndicator(
                                progress = { animProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = primary,
                                trackColor = primary.copy(alpha = 0.18f)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Downloading in background…",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariant
                            )
                        }

                        UpdateState.DOWNLOADED -> {
                            Button(
                                onClick = onRestart,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primary)
                            ) {
                                Text(
                                    "Restart Now",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        else -> Spacer(Modifier.height(0.dp))
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, name = "Update — Available")
@Composable
private fun PreviewUpdateAvailable() {
    MysaMoneyTheme {
        UpdateScreen(
            updateState = UpdateState.AVAILABLE,
            downloadProgress = 0f,
            onUpdateNow = {}, onLater = {}, onRestart = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Update — Downloading 60%")
@Composable
private fun PreviewUpdateDownloading() {
    MysaMoneyTheme {
        UpdateScreen(
            updateState = UpdateState.DOWNLOADING,
            downloadProgress = 0.6f,
            onUpdateNow = {}, onLater = {}, onRestart = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Update — Downloaded")
@Composable
private fun PreviewUpdateDownloaded() {
    MysaMoneyTheme {
        UpdateScreen(
            updateState = UpdateState.DOWNLOADED,
            downloadProgress = 1f,
            onUpdateNow = {}, onLater = {}, onRestart = {}
        )
    }
}

@Composable
private fun UpdateFeatureBullet(primary: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(primary)
        )
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
