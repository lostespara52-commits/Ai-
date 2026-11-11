package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AuraSphereState
import com.example.ui.theme.AuraOfflineGray
import com.example.ui.theme.DuaCrimsonPrimary
import com.example.ui.theme.DuaGoldButton
import com.example.ui.theme.DuaRoseGold
import com.example.ui.theme.DuaRubyBright
import com.example.ui.theme.DuaScarletDeep
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuraGlowingSphere(
    state: AuraSphereState,
    voiceAmplitude: Float,
    isPoweredOn: Boolean,
    sphereSize: Dp = 220.dp,
    showBadge: Boolean = true,
    onSphereClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dua_fatima_sphere_transition")

    val primaryRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraSphereState.THINKING -> 2400
                    AuraSphereState.WORKING -> 1900
                    AuraSphereState.LISTENING -> 4200
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "primary_rotation"
    )

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AuraSphereState.SPEAKING -> 560
                    AuraSphereState.LISTENING -> 780
                    AuraSphereState.WORKING -> 640
                    AuraSphereState.ERROR -> 450
                    else -> 2100
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_phase"
    )

    val waveAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_angle"
    )

    val smoothedAmplitude by animateFloatAsState(
        targetValue = if (state == AuraSphereState.LISTENING) voiceAmplitude.coerceIn(0.08f, 1f) else 0.16f,
        animationSpec = tween(140),
        label = "voice_amp"
    )

    // Crimson Red-centered state palette for Dua Fatima
    val targetCoreColor = when {
        !isPoweredOn || state == AuraSphereState.OFFLINE -> AuraOfflineGray
        state == AuraSphereState.LISTENING -> DuaRubyBright        // Vibrant Crimson-Ruby
        state == AuraSphereState.THINKING -> Color(0xFFFF5252)     // Rotating Scarlet-Amber
        state == AuraSphereState.SPEAKING -> DuaCrimsonPrimary     // Pulsing Crimson Red
        state == AuraSphereState.WORKING -> Color(0xFFFF3D00)      // Dynamic Fiery Red-Gold
        state == AuraSphereState.ERROR -> Color(0xFFD50000)        // Deep Alert Red
        else -> DuaCrimsonPrimary
    }

    val targetSecondaryColor = when {
        !isPoweredOn || state == AuraSphereState.OFFLINE -> Color(0xFF4A353B)
        state == AuraSphereState.LISTENING -> Color(0xFFFF8A80)
        state == AuraSphereState.THINKING -> DuaGoldButton
        state == AuraSphereState.SPEAKING -> DuaScarletDeep
        state == AuraSphereState.WORKING -> DuaGoldButton
        state == AuraSphereState.ERROR -> Color(0xFFFF8A80)
        else -> DuaScarletDeep
    }

    val coreColor by animateColorAsState(targetCoreColor, tween(400), label = "core_color")
    val secondaryColor by animateColorAsState(targetSecondaryColor, tween(400), label = "sec_color")

    val stateBadgeLabel = when {
        !isPoweredOn -> "POWERED OFF"
        state == AuraSphereState.LISTENING -> "LISTENING"
        state == AuraSphereState.THINKING -> "THINKING"
        state == AuraSphereState.SPEAKING -> "SPEAKING"
        state == AuraSphereState.WORKING -> "WORKING"
        state == AuraSphereState.OFFLINE -> "OFFLINE MODE"
        state == AuraSphereState.ERROR -> "SYSTEM ALERT"
        else -> "DUA FATIMA READY"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(sphereSize)
                .clip(CircleShape)
                .testTag("aura_central_sphere")
                .semantics { contentDescription = "Dua Fatima AI Orb ($stateBadgeLabel). Tap to talk or interrupt." }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSphereClick
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f
                val dynamicScale = if (isPoweredOn) {
                    pulsePhase + (smoothedAmplitude * 0.16f)
                } else {
                    0.86f
                }

                // 1. Outer Crimson Nebula Halo (matches the glowing halo in the video)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = if (isPoweredOn) 0.45f else 0.14f),
                            secondaryColor.copy(alpha = if (isPoweredOn) 0.20f else 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius * 0.99f * dynamicScale.coerceAtMost(1.18f)
                    ),
                    radius = maxRadius * 0.99f,
                    center = center
                )

                // 2. Voice-Reactive Ripple Rings
                if (isPoweredOn && (state == AuraSphereState.LISTENING || state == AuraSphereState.SPEAKING || state == AuraSphereState.WORKING)) {
                    for (ring in 1..3) {
                        val ringRadius = maxRadius * (0.52f + ring * 0.12f * dynamicScale.coerceAtMost(1.2f))
                        drawCircle(
                            color = coreColor.copy(alpha = (0.38f / ring)),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(width = (3.5f - ring * 0.8f).dp.toPx())
                        )
                    }
                }

                // 3. Rotating Orbital Rings
                val orbitRadius = maxRadius * 0.68f
                rotate(degrees = primaryRotation, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = if (state == AuraSphereState.WORKING) {
                                listOf(DuaCrimsonPrimary, DuaGoldButton, DuaRubyBright, DuaRoseGold, DuaCrimsonPrimary)
                            } else {
                                listOf(Color.Transparent, coreColor, secondaryColor, Color.Transparent)
                            },
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 285f,
                        useCenter = false,
                        topLeft = Offset(center.x - orbitRadius, center.y - orbitRadius),
                        size = Size(orbitRadius * 2f, orbitRadius * 2f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 4. 3D Textured Nebula Core Sphere (like the glowing planet/orb in the video)
                val coreRadius = (maxRadius * 0.48f * dynamicScale).coerceAtMost(maxRadius * 0.60f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF0F3).copy(alpha = if (isPoweredOn) 0.95f else 0.35f),
                            DuaRoseGold,
                            coreColor,
                            secondaryColor,
                            Color(0xFF2B030B)
                        ),
                        center = Offset(
                            center.x - coreRadius * 0.24f * cos(waveAngle),
                            center.y - coreRadius * 0.24f * sin(waveAngle)
                        ),
                        radius = coreRadius * 1.25f
                    ),
                    radius = coreRadius,
                    center = center
                )

                // Internal Nebula Cloud Swirl Highlight
                if (isPoweredOn) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DuaGoldButton.copy(alpha = 0.42f),
                                Color.Transparent
                            ),
                            center = Offset(
                                center.x + coreRadius * 0.28f * sin(waveAngle),
                                center.y + coreRadius * 0.22f * cos(waveAngle)
                            ),
                            radius = coreRadius * 0.65f
                        ),
                        radius = coreRadius * 0.65f,
                        center = Offset(
                            center.x + coreRadius * 0.28f * sin(waveAngle),
                            center.y + coreRadius * 0.22f * cos(waveAngle)
                        )
                    )
                }
            }
        }

        if (showBadge) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = coreColor.copy(alpha = 0.16f),
                shape = CircleShape
            ) {
                Text(
                    text = stateBadgeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = coreColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
    }
}
