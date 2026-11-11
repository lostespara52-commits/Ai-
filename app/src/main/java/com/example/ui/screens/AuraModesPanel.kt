package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.automation.AuraAccessibilityService
import com.example.automation.ScreenElementNode
import com.example.automation.ScreenSnapshot
import com.example.automation.SupportedAppInfo
import com.example.data.AuraSphereState
import com.example.data.ChatMessageEntity
import com.example.ui.components.AuraGlowingSphere
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.DuaCrimsonPrimary
import com.example.ui.theme.DuaRubyBright
import org.json.JSONArray

@Composable
fun AuraLiveVoiceModeScreen(
    sphereState: AuraSphereState,
    voiceAmplitude: Float,
    isPoweredOn: Boolean,
    continuousVoiceActive: Boolean,
    instantVoiceReplyMode: Boolean = true,
    selectedLocaleTag: String,
    statusSubtitle: String,
    liveTranscript: String,
    proactiveBanner: String,
    latestAuraMessage: ChatMessageEntity?,
    onSphereClick: () -> Unit,
    onToggleContinuousVoice: () -> Unit,
    onToggleInstantReplyMode: () -> Unit = {},
    onSelectLocale: (String) -> Unit,
    onQuickCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 1. Live Audio Call Status & Zero-Latency Speed Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                0.8.dp,
                if (continuousVoiceActive) AuraEmerald.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("proactive_assistant_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (continuousVoiceActive) Icons.Default.Call else Icons.Default.GraphicEq,
                        contentDescription = "Live Call Status",
                        tint = if (continuousVoiceActive) AuraEmerald else DuaCrimsonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (continuousVoiceActive) {
                                "LIVE AUDIO CALL ACTIVE • FULL DUPLEX"
                            } else {
                                "LIVE VOICE & CALL AGENT"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (continuousVoiceActive) AuraEmerald else DuaCrimsonPrimary
                        )
                        Text(
                            text = if (instantVoiceReplyMode) {
                                "Zero-Delay Instant Reply (<0.1s) ON — Say 'Hello' or any command!"
                            } else {
                                proactiveBanner
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Instant <0.1s Reply Toggle Pill
                Surface(
                    color = if (instantVoiceReplyMode) AuraEmerald.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    border = BorderStroke(0.8.dp, if (instantVoiceReplyMode) AuraEmerald else MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .testTag("instant_voice_reply_toggle")
                        .clickable { onToggleInstantReplyMode() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Instant Reply Mode",
                            tint = if (instantVoiceReplyMode) AuraEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (instantVoiceReplyMode) "Instant <0.1s" else "Normal",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (instantVoiceReplyMode) AuraEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. CENTER: Properly Sized Animated Crimson AI Sphere (no overlapping borders)
        AuraGlowingSphere(
            state = sphereState,
            voiceAmplitude = voiceAmplitude,
            isPoweredOn = isPoweredOn,
            sphereSize = 168.dp,
            onSphereClick = onSphereClick
        )

        Text(
            text = liveTranscript.ifBlank { statusSubtitle },
            style = MaterialTheme.typography.bodyMedium,
            color = if (liveTranscript.isNotBlank()) DuaCrimsonPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (latestAuraMessage != null && liveTranscript.isBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = latestAuraMessage.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // 3. Live Call & Microphone Action Row (Compact, Clean Proportions)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onSphereClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DuaCrimsonPrimary,
                    contentColor = Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("live_voice_mic_button")
            ) {
                Icon(
                    imageVector = if (sphereState == AuraSphereState.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Toggle Live Voice Microphone",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (sphereState == AuraSphereState.LISTENING) "Listening..." else "Tap to Speak",
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onToggleContinuousVoice,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (continuousVoiceActive) Color(0xFFD50000) else AuraEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("continuous_duplex_button")
            ) {
                Icon(
                    imageVector = if (continuousVoiceActive) Icons.Default.CallEnd else Icons.Default.Call,
                    contentDescription = "Live Audio Call",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (continuousVoiceActive) "End Live Call" else "Start Live Call",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Multilingual Voice Selector Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Voice Language",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            listOf(
                "ur-PK" to "اردو (Urdu)",
                "hi-IN" to "हिन्दी (Hindi)",
                "en-IN" to "Hinglish / EN"
            ).forEach { (tag, label) ->
                FilterChip(
                    selected = selectedLocaleTag == tag,
                    onClick = { onSelectLocale(tag) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DuaCrimsonPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = DuaCrimsonPrimary
                    )
                )
            }
        }

        // 5. Instant Voice Call Test & Quick Command Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf(
                "⚡ Assalam-o-Alaikum" to "Assalam o Alaikum, kaisi ho?",
                "🤖 ChatGPT + 10 Stories" to "ChatGPT kholo aur mere liye 10 horror stories generate karne ka prompt likho.",
                "📋 Paste on Screen" to "Screen par paste karo",
                "📍 Meri Location / Maps" to "Meri location dikhao",
                "⚡ Battery Status" to "Battery kitni hai",
                "⚡ Flashlight ON" to "Flashlight on karo",
                "Chrome: YouTube Policy" to "Chrome kholo aur latest YouTube monetization policy research karo.",
                "Call Contact" to "Mummy ko phone call karo"
            ).forEach { (shortLabel, fullCommand) ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable { onQuickCommand(fullCommand) }
                ) {
                    Text(
                        text = shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = DuaCrimsonPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AuraChatModeScreen(
    messages: List<ChatMessageEntity>,
    onPickImageForAnalysis: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onSendMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var chatInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "DUA FATIMA CONVERSATION & VISION",
                style = MaterialTheme.typography.labelLarge,
                color = DuaCrimsonPrimary
            )
            OutlinedButton(
                onClick = onPickImageForAnalysis,
                shape = CircleShape,
                border = BorderStroke(1.dp, DuaCrimsonPrimary),
                modifier = Modifier.testTag("attach_image_vision_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Attach Image for Vision Analysis",
                    tint = DuaCrimsonPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyze Image", color = DuaCrimsonPrimary, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == "USER"
                Surface(
                    color = if (isUser) DuaCrimsonPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isUser) DuaCrimsonPrimary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isUser) "YOU" else "DUA FATIMA",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser) DuaRubyBright else DuaCrimsonPrimary
                            )
                            if (msg.actionTaken.isNotBlank() && msg.actionTaken != "NONE") {
                                Text(
                                    text = msg.actionTaken.take(36),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuraEmerald
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (msg.sourcesJson.isNotBlank()) {
                            val sources = remember(msg.sourcesJson) {
                                try {
                                    val arr = JSONArray(msg.sourcesJson)
                                    (0 until arr.length()).map {
                                        val o = arr.getJSONObject(it)
                                        o.optString("title") to o.optString("url")
                                    }
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                            if (sources.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    sources.forEach { (title, url) ->
                                        Surface(
                                            color = DuaCrimsonPrimary.copy(alpha = 0.12f),
                                            shape = CircleShape,
                                            modifier = Modifier.clickable { onOpenUrl(url) }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.OpenInNew,
                                                    contentDescription = "Open Source",
                                                    tint = DuaCrimsonPrimary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = title.take(28),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DuaCrimsonPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Inline Chat Input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Message Dua Fatima (Urdu, Hindi, English)...") },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            Surface(
                color = DuaCrimsonPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("chat_send_button")
                    .clickable {
                        if (chatInput.isNotBlank()) {
                            onSendMessage(chatInput)
                            chatInput = ""
                        }
                    }
            ) {
                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            onSendMessage(chatInput)
                            chatInput = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuraScreenAgentScreen(
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean = false,
    isVoiceKeyboardEnabled: Boolean = false,
    screenSnapshot: ScreenSnapshot,
    supportedApps: List<SupportedAppInfo>,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestOverlayPermission: () -> Unit = {},
    onOpenVoiceKeyboardSettings: () -> Unit = {},
    onShowKeyboardPicker: () -> Unit = {},
    onScanScreen: () -> Unit,
    onTypeOnScreen: (String) -> Unit,
    onAppendOnScreen: (String) -> Unit = {},
    onPasteOnScreen: (String) -> Unit = {},
    onClearScreenText: () -> Unit = {},
    onTapOnScreen: (String) -> Unit,
    onTapElementNode: (ScreenElementNode) -> Unit = {},
    onScrollScreen: (Boolean) -> Unit = {},
    onToggleFlashlight: () -> Unit = {},
    onAdjustVolume: (Boolean) -> Unit = {},
    onCheckLocation: () -> Unit = {},
    onOpenSystemSetting: (String) -> Unit = {},
    onLaunchApp: (String) -> Unit,
    onRefreshApps: () -> Unit = {},
    onRequestPhoneCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textToType by remember { mutableStateOf("") }
    var elementToTap by remember { mutableStateOf("") }
    var contactToCall by remember { mutableStateOf("") }
    var appSearchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(supportedApps, appSearchQuery) {
        if (appSearchQuery.isBlank()) {
            supportedApps
        } else {
            supportedApps.filter {
                it.name.contains(appSearchQuery, ignoreCase = true) ||
                    it.id.contains(appSearchQuery, ignoreCase = true) ||
                    it.packageName.contains(appSearchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Complete Permissions & Keyboard Control Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (isAccessibilityEnabled && isOverlayEnabled) AuraEmerald else AuraAmber
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.AccessibilityNew,
                                contentDescription = "Accessibility Status",
                                tint = if (isAccessibilityEnabled) AuraEmerald else AuraAmber
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isAccessibilityEnabled) {
                                        "SCREEN, OVERLAY & KEYBOARD ENGINE: ACTIVE"
                                    } else {
                                        "SCREEN, OVERLAY & KEYBOARD SETUP"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isAccessibilityEnabled) AuraEmerald else AuraAmber
                                )
                                Text(
                                    text = "Turn on Screen Control, Display Over Other Apps & Dua Fatima Voice Keyboard for full A-to-Z phone control.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenAccessibilitySettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAccessibilityEnabled) AuraEmerald else DuaCrimsonPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("enable_accessibility_button")
                        ) {
                            Icon(Icons.Default.AccessibilityNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAccessibilityEnabled) "Screen Control: ON" else "1. Enable Screen Control")
                        }

                        Button(
                            onClick = onRequestOverlayPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOverlayEnabled) AuraEmerald else DuaCrimsonPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("enable_overlay_button")
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isOverlayEnabled) "Display Overlay: ON" else "2. Display Over Apps")
                        }

                        Button(
                            onClick = onOpenVoiceKeyboardSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVoiceKeyboardEnabled) AuraEmerald else DuaCrimsonPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("enable_voice_keyboard_button")
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isVoiceKeyboardEnabled) "Voice Keyboard: ON" else "3. Turn On Keyboard")
                        }

                        OutlinedButton(
                            onClick = onShowKeyboardPicker,
                            modifier = Modifier.testTag("switch_keyboard_picker_button")
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch Keyboard")
                        }

                        OutlinedButton(
                            onClick = onScanScreen,
                            modifier = Modifier.testTag("scan_screen_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan Screen", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read Screen Now")
                        }
                    }
                }
            }
        }

        // 2. Direct Screen Typing, Adding, Removing, Tapping, Scrolling & Calling
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TYPE, ADD, REMOVE TEXT, TAP BUTTONS & SCROLL SCREEN",
                        style = MaterialTheme.typography.labelLarge,
                        color = DuaCrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = textToType,
                        onValueChange = { textToType = it },
                        placeholder = { Text("Text or prompt to type / add on screen...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("screen_type_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                onTypeOnScreen(textToType)
                                textToType = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DuaCrimsonPrimary, contentColor = Color.White),
                            modifier = Modifier.testTag("screen_type_submit")
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = "Type", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Type")
                        }

                        OutlinedButton(
                            onClick = {
                                onPasteOnScreen(textToType)
                                textToType = ""
                            },
                            modifier = Modifier.testTag("screen_paste_submit")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste Text", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste")
                        }

                        OutlinedButton(
                            onClick = {
                                onAppendOnScreen(textToType)
                                textToType = ""
                            },
                            modifier = Modifier.testTag("screen_append_submit")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Text", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }

                        OutlinedButton(
                            onClick = onClearScreenText,
                            border = BorderStroke(1.dp, DuaRubyBright),
                            modifier = Modifier.testTag("screen_clear_text_button")
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Remove Text", tint = DuaRubyBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", color = DuaRubyBright)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = elementToTap,
                            onValueChange = { elementToTap = it },
                            placeholder = { Text("Button/Element to tap (e.g. Send, Search, Post)...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("screen_tap_input")
                        )
                        Button(
                            onClick = {
                                onTapOnScreen(elementToTap)
                                elementToTap = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DuaCrimsonPrimary, contentColor = Color.White),
                            modifier = Modifier.testTag("screen_tap_submit")
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = "Tap Element", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tap")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = contactToCall,
                            onValueChange = { contactToCall = it },
                            placeholder = { Text("Call Contact Name or Number...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("call_contact_input")
                        )
                        IconButton(
                            onClick = {
                                if (contactToCall.isNotBlank()) {
                                    onRequestPhoneCall(contactToCall)
                                    contactToCall = ""
                                }
                            },
                            modifier = Modifier.testTag("call_contact_submit")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Initiate Phone Call", tint = AuraEmerald)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // System & Scroll Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { AuraAccessibilityService.instance?.performNavigateBack() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Back")
                        }
                        OutlinedButton(
                            onClick = { AuraAccessibilityService.instance?.performNavigateHome() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Home")
                        }
                        OutlinedButton(
                            onClick = { AuraAccessibilityService.instance?.performOpenRecents() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ViewCarousel, contentDescription = "Recents", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Apps")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { onScrollScreen(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Scroll Up", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scroll Up")
                        }
                        OutlinedButton(
                            onClick = { onScrollScreen(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Scroll Down", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scroll Down")
                        }
                    }
                }
            }
        }

        // 2B. Advanced Phone Hardware & System Controls (Flashlight, Volume, Wi-Fi, Bluetooth, Sound, Display)
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ADVANCED HARDWARE & SYSTEM CONTROLS",
                        style = MaterialTheme.typography.labelLarge,
                        color = DuaCrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onToggleFlashlight,
                            modifier = Modifier.testTag("hardware_flashlight_button")
                        ) {
                            Icon(Icons.Default.FlashlightOn, contentDescription = "Flashlight", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Torch / Flashlight")
                        }
                        OutlinedButton(
                            onClick = { onAdjustVolume(true) },
                            modifier = Modifier.testTag("hardware_volume_up_button")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vol +")
                        }
                        OutlinedButton(
                            onClick = { onAdjustVolume(false) },
                            modifier = Modifier.testTag("hardware_volume_down_button")
                        ) {
                            Icon(Icons.Default.VolumeDown, contentDescription = "Volume Down", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vol -")
                        }
                        OutlinedButton(
                            onClick = { onOpenSystemSetting(android.provider.Settings.ACTION_WIFI_SETTINGS) }
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wi-Fi")
                        }
                        OutlinedButton(
                            onClick = { onOpenSystemSetting(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS) }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Bluetooth", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bluetooth")
                        }
                        OutlinedButton(
                            onClick = { onOpenSystemSetting(android.provider.Settings.ACTION_SOUND_SETTINGS) }
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Sound Settings", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sound")
                        }
                        OutlinedButton(
                            onClick = onCheckLocation,
                            modifier = Modifier.testTag("hardware_location_button")
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = "GPS Location & Maps", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Live Location / Maps")
                        }
                        OutlinedButton(
                            onClick = { onOpenSystemSetting(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS) }
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = "GPS Settings", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GPS Settings")
                        }
                        OutlinedButton(
                            onClick = { onOpenSystemSetting(android.provider.Settings.ACTION_DISPLAY_SETTINGS) }
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = "Display Settings", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Display")
                        }
                    }
                }
            }
        }

        // 3. All Installed Apps & Core App Launcher with Search
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "OPEN ANY APP ON PHONE (${filteredApps.size} APPS)",
                            style = MaterialTheme.typography.labelLarge,
                            color = DuaCrimsonPrimary
                        )
                        IconButton(onClick = onRefreshApps) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Installed Apps", tint = DuaCrimsonPrimary)
                        }
                    }

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Search installed apps (YouTube, WhatsApp, Camera, Settings)...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search Apps")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredApps.take(40).forEach { app ->
                            Surface(
                                color = DuaCrimsonPrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .testTag("launch_app_${app.id}")
                                    .clickable { onLaunchApp(app.name) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = app.name,
                                        tint = DuaCrimsonPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Screen Telemetry & 1-Tap Interactive Element Inspector
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LIVE SCREEN INSPECTOR (${screenSnapshot.activePackage})",
                        style = MaterialTheme.typography.labelLarge,
                        color = DuaCrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last Action: ${screenSnapshot.lastActionLog}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuraEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = screenSnapshot.visibleTextSummary.ifBlank {
                            "Tap 'Read Screen Now' or open any app with Screen Control enabled to inspect live text and tap any button."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (screenSnapshot.interactiveElements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DETECTED SCREEN ELEMENTS (TAP ANY ROW TO CLICK IT):",
                            style = MaterialTheme.typography.labelSmall,
                            color = DuaCrimsonPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        screenSnapshot.interactiveElements.take(18).forEach { node ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { onTapElementNode(node) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = node.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(node.className)
                                                if (node.isEditable) append(" • Input Box")
                                                if (node.isClickable) append(" • Clickable")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = DuaCrimsonPrimary.copy(alpha = 0.16f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "Tap",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DuaCrimsonPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
