package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AuraSphereState
import com.example.ui.components.AuraGlowingSphere
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.DuaCrimsonPrimary
import com.example.ui.theme.DuaGoldButton
import com.example.ui.theme.DuaRubyBright
import kotlin.math.sin

@Composable
fun DuaOnboardingFlow(
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean = false,
    isVoiceKeyboardEnabled: Boolean = false,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit = {},
    onOpenVoiceKeyboardSettings: () -> Unit = {},
    onShowSetupGuide: () -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) } // 0, 1, 2, 3

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var contactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var phoneCallsReady by remember { mutableStateOf(contactsGranted) }
    var notificationsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val singlePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    val allPermsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        phoneCallsReady = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    val allGranted = micGranted && contactsGranted && phoneCallsReady && notificationsGranted

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140408),
                        Color(0xFF21070E),
                        Color(0xFF0D0306)
                    )
                )
            )
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        // Top 4-Segment Progress Bar + Skip Button (matches 00:04 - 00:13 in video)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                for (i in 0..3) {
                    val active = i <= currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) DuaCrimsonPrimary else Color.White.copy(alpha = 0.22f)
                            )
                    )
                }
            }
            if (currentStep < 3) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier
                        .testTag("onboarding_skip_button")
                        .clickable { onCompleteOnboarding() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        // Step Body
        if (currentStep < 3) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                AuraGlowingSphere(
                    state = when (currentStep) {
                        0 -> AuraSphereState.IDLE
                        1 -> AuraSphereState.SPEAKING
                        else -> AuraSphereState.WORKING
                    },
                    voiceAmplitude = 0.65f,
                    isPoweredOn = true,
                    sphereSize = 176.dp,
                    showBadge = false,
                    onSphereClick = {}
                )

                if (currentStep == 1) {
                    Spacer(modifier = Modifier.height(14.dp))
                    AnimatedVoiceWaveformRow()
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = when (currentStep) {
                        0 -> "Meet Dua Fatima"
                        1 -> "Just say it"
                        else -> "It gets things done"
                    },
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (currentStep) {
                        0 -> "Talk to your phone the way you talk to a friend - in Urdu, Hindi, English or all three."
                        1 -> "Tap the orb and speak. Dua Fatima listens, answers out loud immediately and keeps the conversation going."
                        else -> "Open apps, call a contact, send a message, type or remove text on screen, or run workflows - just ask."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Button(
                onClick = { currentStep += 1 },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DuaCrimsonPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(46.dp)
                    .testTag("onboarding_continue_button")
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Step 4: "Let Dua Fatima help" Permissions & Setup Screen (matches 00:08 - 00:13 in video)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                AuraGlowingSphere(
                    state = AuraSphereState.IDLE,
                    voiceAmplitude = 0.3f,
                    isPoweredOn = true,
                    sphereSize = 72.dp,
                    showBadge = false,
                    onSphereClick = {}
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Let Dua Fatima help",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Each one is optional. You can change them later in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Watch the setup guide card
                Surface(
                    color = Color(0xFF280912),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.8.dp, DuaCrimsonPrimary.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowSetupGuide() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DuaCrimsonPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Setup Guide",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Watch the setup guide",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "API keys, permissions, Accessibility and voice setup, step by step",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Permissions List Card
                Surface(
                    color = Color(0xFF1C070E),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(0.8.dp, Color(0xFF4F1525).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        PermissionItemRow(
                            icon = Icons.Default.Mic,
                            title = "Microphone",
                            subtitle = "So Dua Fatima can hear you",
                            isAllowed = micGranted,
                            onAllowClick = {
                                singlePermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        )
                        PermissionItemRow(
                            icon = Icons.Default.Contacts,
                            title = "Contacts",
                            subtitle = "Find a number from a name",
                            isAllowed = contactsGranted,
                            onAllowClick = {
                                singlePermLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        )
                        PermissionItemRow(
                            icon = Icons.Default.Call,
                            title = "Phone calls",
                            subtitle = "Call people directly with approval",
                            isAllowed = phoneCallsReady,
                            onAllowClick = {
                                phoneCallsReady = true
                                singlePermLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        )
                        PermissionItemRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            subtitle = "Reminders and workflow updates",
                            isAllowed = notificationsGranted,
                            onAllowClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    singlePermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationsGranted = true
                                }
                            }
                        )
                        PermissionItemRow(
                            icon = Icons.Default.Layers,
                            title = "Display over other apps",
                            subtitle = "Open apps even when minimised",
                            isAllowed = isOverlayEnabled,
                            onAllowClick = onRequestOverlayPermission
                        )
                        PermissionItemRow(
                            icon = Icons.Default.AccessibilityNew,
                            title = "Screen Control (Accessibility)",
                            subtitle = "Type in apps, remove text & tap buttons",
                            isAllowed = isAccessibilityEnabled,
                            onAllowClick = onOpenAccessibilitySettings
                        )
                        PermissionItemRow(
                            icon = Icons.Default.Keyboard,
                            title = "Voice keyboard",
                            subtitle = "Speak to type in any app (UR/HI/EN)",
                            isAllowed = isVoiceKeyboardEnabled,
                            onAllowClick = onOpenVoiceKeyboardSettings
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Button(
                    onClick = {
                        if (!allGranted) {
                            val perms = buildList {
                                add(Manifest.permission.RECORD_AUDIO)
                                add(Manifest.permission.READ_CONTACTS)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            allPermsLauncher.launch(perms.toTypedArray())
                            onCompleteOnboarding()
                        } else {
                            onCompleteOnboarding()
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DuaCrimsonPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .height(46.dp)
                        .testTag("onboarding_get_started_button")
                ) {
                    Text(
                        text = if (allGranted) "Get started" else "Allow & Get started",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onCompleteOnboarding,
                    modifier = Modifier.testTag("onboarding_not_now_button")
                ) {
                    Text(
                        text = "Not now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isAllowed: Boolean,
    onAllowClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = DuaCrimsonPrimary.copy(alpha = 0.22f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = DuaRubyBright,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f)
            )
        }
        Surface(
            color = if (isAllowed) AuraEmerald.copy(alpha = 0.18f) else DuaGoldButton.copy(alpha = 0.2f),
            shape = CircleShape,
            border = BorderStroke(
                1.dp,
                if (isAllowed) AuraEmerald.copy(alpha = 0.5f) else DuaCrimsonPrimary
            ),
            modifier = Modifier.clickable(enabled = !isAllowed) { onAllowClick() }
        ) {
            Text(
                text = if (isAllowed) "Allowed" else "Allow",
                style = MaterialTheme.typography.labelLarge,
                color = if (isAllowed) AuraEmerald else DuaRubyBright,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun AnimatedVoiceWaveformRow(
    barColor: Color = DuaCrimsonPrimary,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform_bars")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier.height(44.dp)
    ) {
        val barCount = 23
        for (i in 0 until barCount) {
            val factor = (0.3f + 0.7f * kotlin.math.abs(sin(phase + i * 0.45f))).coerceIn(0.2f, 1f)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height((10 + 32 * factor).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DuaGoldButton, barColor)
                        )
                    )
            )
        }
    }
}
