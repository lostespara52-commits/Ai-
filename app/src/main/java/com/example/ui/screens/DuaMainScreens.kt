package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AuraMode
import com.example.data.AuraSphereState
import com.example.data.ChatMessageEntity
import com.example.ui.components.AuraGlowingSphere
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.DuaCrimsonPrimary
import com.example.ui.theme.DuaGoldButton
import com.example.ui.theme.DuaRoseGold
import com.example.ui.theme.DuaRubyBright
import com.example.ui.theme.DuaScarletDeep

/**
 * Matches the Home Screen in the reference video (00:28, 00:38-00:40, 00:44)
 * styled in Vibrant Crimson Red for Dua Fatima, with Light/Dark theme adaptability.
 */
@Composable
fun DuaHomeDashboardScreen(
    sphereState: AuraSphereState,
    statusSubtitle: String,
    liveTranscript: String,
    isDarkMode: Boolean,
    continuousVoiceActive: Boolean = false,
    onTapToTalk: () -> Unit,
    onStartLiveCall: () -> Unit = {},
    onSubmitQuestion: (String) -> Unit,
    onNavigateMode: (AuraMode) -> Unit,
    onOpenQuickActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var askText by remember { mutableStateOf("") }
    val infiniteTransition = rememberInfiniteTransition(label = "tap_to_talk_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    val cardBg = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 1. Hero Portrait Banner with "Hi, I'm Dua Fatima ♡" & Overlapping "Tap to Talk" Orb Button
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .height(268.dp)
        ) {
            // Hero Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(222.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(24.dp))
                    .border(0.8.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dua_fatima_hero_1794030554440),
                    contentDescription = "Dua Fatima AI Assistant",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom gradient fade to blend seamlessly
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f),
                                    Color(0xFF140408).copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                // Left Floating Script Text: "Hi, I'm Dua Fatima ♡"
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 18.dp)
                ) {
                    Text(
                        text = "Hi,\nI'm\nDua Fatima\n♡",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = FontStyle.Italic,
                            lineHeight = 19.sp
                        ),
                        color = Color(0xFFFFD9DF)
                    )
                }

                // Right Floating Script Text: "Always Here For You ♡"
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 16.dp)
                ) {
                    Text(
                        text = "Always\nHere\nFor You\n♡",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontStyle = FontStyle.Italic,
                            lineHeight = 15.sp
                        ),
                        textAlign = TextAlign.End,
                        color = Color(0xFFFFB8C6)
                    )
                }
            }

            // Overlapping Circular Glowing Crimson "Tap to Talk" Button (Clean single border)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size((96 * ringScale).dp)
                    .shadow(14.dp, CircleShape, ambientColor = DuaCrimsonPrimary, spotColor = DuaCrimsonPrimary)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                DuaRubyBright,
                                DuaCrimsonPrimary,
                                Color(0xFF280610)
                            )
                        )
                    )
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF2B0711), Color(0xFF140308))
                        )
                    )
                    .testTag("home_tap_to_talk_button")
                    .clickable { onTapToTalk() }
            ) {
                Icon(
                    imageVector = if (sphereState == AuraSphereState.LISTENING) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = "Tap to Talk to Dua Fatima",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // "Tap to Talk" Label + Live Audio Call (<0.1s) Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (sphereState == AuraSphereState.LISTENING) "Listening... Speak now" else "Tap to Talk",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Surface(
                color = if (continuousVoiceActive) AuraEmerald.copy(alpha = 0.2f) else DuaCrimsonPrimary.copy(alpha = 0.16f),
                shape = CircleShape,
                border = BorderStroke(0.8.dp, if (continuousVoiceActive) AuraEmerald else DuaCrimsonPrimary),
                modifier = Modifier
                    .testTag("home_live_call_pill")
                    .clickable { onStartLiveCall() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Live Audio Call",
                        tint = if (continuousVoiceActive) AuraEmerald else DuaCrimsonPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (continuousVoiceActive) "Live Call ON" else "Live Call (<0.1s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (continuousVoiceActive) AuraEmerald else DuaCrimsonPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Subtle Equalizer Waveform Indicator Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.height(18.dp)
        ) {
            val heights = listOf(4, 4, 6, 4, 10, 16, 12, 6, 4, 12, 16, 10, 4, 6, 4, 4)
            heights.forEachIndexed { idx, h ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(
                            if (sphereState == AuraSphereState.LISTENING || sphereState == AuraSphereState.SPEAKING) {
                                (h + 4).dp
                            } else {
                                h.dp
                            }
                        )
                        .clip(CircleShape)
                        .background(
                            if (idx in 4..11) DuaCrimsonPrimary else DuaCrimsonPrimary.copy(alpha = 0.4f)
                        )
                )
            }
        }

        if (liveTranscript.isNotBlank() || statusSubtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = liveTranscript.ifBlank { statusSubtitle },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. "Ask me anything..." Input Bar with Sparkle Left & Crimson Send Button Right
        Surface(
            color = cardBg,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(0.8.dp, borderColor.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Surface(
                    color = DuaCrimsonPrimary.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onOpenQuickActions() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Quick Actions",
                            tint = DuaCrimsonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = askText,
                    onValueChange = { askText = it },
                    placeholder = {
                        Text(
                            text = "Ask me anything...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_ask_input")
                )

                Surface(
                    color = DuaCrimsonPrimary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("home_send_button")
                        .clickable {
                            if (askText.isNotBlank()) {
                                onSubmitQuestion(askText)
                                askText = ""
                            } else {
                                onTapToTalk()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Three Feature Cards Row: Chat | Create Image | Write (matches 00:28 in video)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            HomeFeatureCard(
                icon = Icons.Default.ChatBubbleOutline,
                title = "Chat",
                subtitle = "Get instant\nanswers",
                onClick = { onNavigateMode(AuraMode.CHAT) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_card_chat")
            )
            HomeFeatureCard(
                icon = Icons.Default.Image,
                title = "Create Image",
                subtitle = "Story scenes &\nprompts",
                onClick = { onNavigateMode(AuraMode.WORKFLOW) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_card_create")
            )
            HomeFeatureCard(
                icon = Icons.Default.Description,
                title = "Write & Control",
                subtitle = "Notes, code,\nscreen agent",
                onClick = { onNavigateMode(AuraMode.AGENT) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_card_write")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun HomeFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            Surface(
                color = DuaCrimsonPrimary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = DuaCrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/**
 * Matches the "Quick actions" screen in the reference video (00:41-00:43).
 */
@Composable
fun DuaQuickActionsScreen(
    onBack: () -> Unit,
    onRunQuickAction: (String) -> Unit,
    onStartLiveVoice: () -> Unit,
    onOpenMode: (AuraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        Triple(Icons.Default.Call, "Live Audio Call (<0.1s)", "Say 'Hello' for instant zero-delay reply") to "VOICE_MODE",
        Triple(Icons.Default.OpenInNew, "Open an app", "\"YouTube kholo\" / \"ChatGPT kholo\"") to "YouTube kholo",
        Triple(Icons.Default.Call, "Call someone", "\"Mummy ko call karo\"") to "Mummy ko call karo",
        Triple(Icons.AutoMirrored.Filled.Send, "Send a message", "\"Papa ko WhatsApp karo\"") to "Papa ko WhatsApp karo: Assalam-o-Alaikum, main abhi pahunch raha hun",
        Triple(Icons.Default.Keyboard, "ChatGPT 10 Horror Stories", "\"ChatGPT kholo aur 10 horror stories likho\"") to "ChatGPT kholo aur mere liye 10 horror stories generate karne ka prompt likho.",
        Triple(Icons.Default.Brush, "Story Video Workflow", "\"Automatic AI story video banao\"") to "WORKFLOW_MODE",
        Triple(Icons.Default.Language, "Chrome Research", "\"Latest YouTube monetization policy\"") to "Chrome kholo aur latest YouTube monetization policy research karo.",
        Triple(Icons.Default.PhoneAndroid, "Full Screen & Hardware", "Type, remove text, tap, torch & volume") to "AGENT_MODE"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Quick actions",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap one to start. Say it the way you would to a friend - Urdu, Hindi, English or all three.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2-Column Grid of Quick Action Cards
        for (rowIdx in actions.indices step 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                for (colOffset in 0..1) {
                    val idx = rowIdx + colOffset
                    if (idx < actions.size) {
                        val (triple, command) = actions[idx]
                        val (icon, title, subtitle) = triple
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .height(152.dp)
                                .clickable {
                                    when (command) {
                                        "VOICE_MODE" -> onStartLiveVoice()
                                        "WORKFLOW_MODE" -> onOpenMode(AuraMode.WORKFLOW)
                                        "AGENT_MODE" -> onOpenMode(AuraMode.AGENT)
                                        else -> onRunQuickAction(command)
                                    }
                                }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Surface(
                                    color = DuaCrimsonPrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = DuaCrimsonPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Matches the "Conversations" / History screen in the reference video (00:48-00:49).
 */
@Composable
fun DuaConversationsHistoryScreen(
    messages: List<ChatMessageEntity>,
    sphereState: AuraSphereState,
    onStartConversation: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val userConversations = remember(messages, searchQuery) {
        val list = if (searchQuery.isBlank()) {
            messages
        } else {
            messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
        }
        list.reversed()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
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
                text = "Conversations",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (messages.isNotEmpty()) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = DuaCrimsonPrimary,
                    modifier = Modifier.clickable { onClearHistory() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search conversations") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DuaCrimsonPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (userConversations.size <= 1 && searchQuery.isBlank()) {
            // Empty state matching 00:48-00:49 in video
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                AuraGlowingSphere(
                    state = sphereState,
                    voiceAmplitude = 0.25f,
                    isPoweredOn = true,
                    sphereSize = 136.dp,
                    showBadge = false,
                    onSphereClick = onStartConversation
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "No conversations yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Chats and voice conversations are saved\nhere automatically.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStartConversation,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DuaCrimsonPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(50.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Start a conversation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(userConversations, key = { it.id }) { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (msg.sender == "USER") "You" else "Dua Fatima",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (msg.sender == "USER") DuaRubyBright else DuaCrimsonPrimary
                                )
                                if (msg.actionTaken.isNotBlank() && msg.actionTaken != "NONE") {
                                    Text(
                                        text = msg.actionTaken.take(28),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AuraEmerald
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.content,
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

/**
 * Matches the "Settings" screen in the reference video (00:45-00:47)
 * with Light/Dark Mode toggle, Connections, Voice & Language, Memory, Screen Control, Workflows, and Power Off.
 */
@Composable
fun DuaSettingsDashboardScreen(
    isDarkMode: Boolean,
    isPoweredOn: Boolean,
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean = false,
    isVoiceKeyboardEnabled: Boolean = false,
    selectedLocaleTag: String,
    memoriesCount: Int,
    workflowsCount: Int,
    onToggleDarkMode: () -> Unit,
    onTogglePower: () -> Unit,
    onCycleVoiceLanguage: () -> Unit,
    onOpenMode: (AuraMode) -> Unit,
    onOpenSetupGuide: () -> Unit,
    onRequestOverlayPermission: () -> Unit = {},
    onOpenVoiceKeyboardSettings: () -> Unit = {},
    onShowKeyboardPicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val languageLabel = when (selectedLocaleTag) {
        "ur-PK" -> "Urdu (اردو) • Auto • 1.0x"
        "hi-IN" -> "Hindi (हिन्दी) • Auto • 1.0x"
        else -> "English + Urdu/Hindi • Auto • 1.0x"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header with Dua Fatima Portrait Preview (matches 00:45 in video)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DUA FATIMA",
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
                    color = DuaCrimsonPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Customize your AI experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Image(
                painter = painterResource(id = R.drawable.img_dua_fatima_hero_1794030554440),
                contentDescription = "Dua Fatima",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(126.dp)
                    .height(82.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DuaCrimsonPrimary, RoundedCornerShape(16.dp))
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Identity & Manual Power Control Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Surface(
                    color = DuaCrimsonPrimary.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = DuaCrimsonPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DUA FATIMA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your Personal Autonomous AI Agent • v2.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = if (isPoweredOn) DuaCrimsonPrimary else AuraEmerald,
                    shape = CircleShape,
                    modifier = Modifier.clickable { onTogglePower() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPoweredOn) "Power Off" else "Power On",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Watch the setup guide card (Red bordered card from 00:45)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, DuaCrimsonPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSetupGuide() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Surface(
                    color = DuaCrimsonPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Watch Setup Guide",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Watch the setup guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "API keys, permissions, Accessibility and voice setup, step by step",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section 1: AI SETTINGS
        Text(
            text = "✦ AI SETTINGS   Configure your AI assistant",
            style = MaterialTheme.typography.labelLarge,
            color = DuaCrimsonPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                SettingsItemRow(
                    icon = Icons.Default.VpnKey,
                    title = "Connections",
                    subtitle = "Gemini (Active), Groq, OpenAI & Anthropic keys",
                    badgeText = "Add a key",
                    onClick = { onOpenMode(AuraMode.API_CENTER) }
                )
                SettingsItemRow(
                    icon = Icons.Default.Language,
                    title = "Voice & language",
                    subtitle = languageLabel,
                    badgeText = "Switch",
                    onClick = onCycleVoiceLanguage
                )
                SettingsItemRow(
                    icon = Icons.Default.Memory,
                    title = "Chats & memory",
                    subtitle = "$memoriesCount saved memories across 5 tiers",
                    onClick = { onOpenMode(AuraMode.MEMORY) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section 2: APP SETTINGS (includes Dark / Light Mode toggle!)
        Text(
            text = "⚙ APP SETTINGS   Appearance and behavior",
            style = MaterialTheme.typography.labelLarge,
            color = DuaCrimsonPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                // Look & Feel (Dark Mode / Light Mode Toggle)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_look_and_feel_row")
                        .clickable { onToggleDarkMode() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        color = DuaCrimsonPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Look & feel",
                                tint = DuaCrimsonPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Look & feel (Light / Dark Mode)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDarkMode) "Dark Mode • Crimson Red Orb" else "Light Mode • Crimson Rose Orb",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DuaCrimsonPrimary
                        )
                    )
                }

                SettingsItemRow(
                    icon = Icons.Default.Keyboard,
                    title = "Voice keyboard",
                    subtitle = "Speak to type in any app, with Urdu + Hindi + English",
                    badgeText = if (isVoiceKeyboardEnabled) "Switch" else "Turn on",
                    onClick = {
                        if (isVoiceKeyboardEnabled) {
                            onShowKeyboardPicker()
                        } else {
                            onOpenVoiceKeyboardSettings()
                        }
                    }
                )

                SettingsItemRow(
                    icon = Icons.Default.Layers,
                    title = "Display over other apps",
                    subtitle = "Allow Dua Fatima to open apps from background",
                    badgeText = if (isOverlayEnabled) "Allowed" else "Allow",
                    onClick = onRequestOverlayPermission
                )

                SettingsItemRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Full Screen & App Control",
                    subtitle = "Read screen, type text, remove text & tap buttons",
                    badgeText = if (isAccessibilityEnabled) "Active" else "Turn on",
                    onClick = { onOpenMode(AuraMode.AGENT) }
                )

                SettingsItemRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Autonomous Workflows",
                    subtitle = "$workflowsCount workflows (Story Video, ChatGPT, Chrome)",
                    onClick = { onOpenMode(AuraMode.WORKFLOW) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Schedule,
                    title = "Smart Scheduler & Offline Queue",
                    subtitle = "Scheduled background tasks & auto-reconnect queue",
                    onClick = { onOpenMode(AuraMode.SCHEDULER) }
                )

                SettingsItemRow(
                    icon = Icons.Default.OpenInNew,
                    title = "Phone control & Quick actions",
                    subtitle = "Quick actions, app launchers and permissions",
                    onClick = { onOpenMode(AuraMode.QUICK_ACTIONS) }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section 3: ABOUT & PRIVACY
        Text(
            text = "ℹ ABOUT   App information and security",
            style = MaterialTheme.typography.labelLarge,
            color = DuaCrimsonPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                SettingsItemRow(
                    icon = Icons.Default.Security,
                    title = "Privacy, Projects & Security",
                    subtitle = "AES-256-GCM encrypted keys, project vault & memory wipe",
                    onClick = { onOpenMode(AuraMode.PROJECTS) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            color = DuaCrimsonPrimary.copy(alpha = 0.18f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = DuaCrimsonPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (badgeText != null) {
            Surface(
                color = DuaCrimsonPrimary.copy(alpha = 0.16f),
                shape = CircleShape,
                border = BorderStroke(1.dp, DuaCrimsonPrimary)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelLarge,
                    color = DuaCrimsonPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
