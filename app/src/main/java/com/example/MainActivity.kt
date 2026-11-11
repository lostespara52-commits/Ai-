package com.example

import android.Manifest
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.AuraMode
import com.example.data.AuraSphereState
import com.example.ui.components.AuraGlowingSphere
import com.example.ui.screens.AuraApiCenterScreen
import com.example.ui.screens.AuraChatModeScreen
import com.example.ui.screens.AuraLiveVoiceModeScreen
import com.example.ui.screens.AuraMemoryScreen
import com.example.ui.screens.AuraProjectsScreen
import com.example.ui.screens.AuraSchedulerAndOfflineScreen
import com.example.ui.screens.AuraScreenAgentScreen
import com.example.ui.screens.AuraWorkflowBuilderScreen
import com.example.ui.screens.DuaConversationsHistoryScreen
import com.example.ui.screens.DuaHomeDashboardScreen
import com.example.ui.screens.DuaOnboardingFlow
import com.example.ui.screens.DuaQuickActionsScreen
import com.example.ui.screens.DuaSettingsDashboardScreen
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraCrimson
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.DuaCrimsonPrimary
import com.example.ui.theme.DuaRubyBright
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuraViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val auraViewModel: AuraViewModel = viewModel()
            val isDarkMode by auraViewModel.isDarkMode.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DuaFatimaOperatingSystemApp(viewModel = auraViewModel)
                }
            }
        }
    }
}

@Composable
fun DuaFatimaOperatingSystemApp(
    viewModel: AuraViewModel
) {
    val context = LocalContext.current

    val sphereState by viewModel.sphereState.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val showOpenAppsPrompt by viewModel.showOpenAppsPrompt.collectAsStateWithLifecycle()
    val showSetupGuideModal by viewModel.showSetupGuideModal.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isPoweredOn by viewModel.isPoweredOn.collectAsStateWithLifecycle()
    val statusSubtitle by viewModel.statusSubtitle.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscriptPreview.collectAsStateWithLifecycle()
    val proactiveBanner by viewModel.proactiveBanner.collectAsStateWithLifecycle()
    val pendingApproval by viewModel.pendingApproval.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()
    val voiceAmplitude by viewModel.voiceAmplitude.collectAsStateWithLifecycle()
    val continuousVoiceActive by viewModel.continuousVoiceActive.collectAsStateWithLifecycle()
    val instantVoiceReplyMode by viewModel.instantVoiceReplyMode.collectAsStateWithLifecycle()
    val selectedVoiceLocale by viewModel.selectedVoiceLocale.collectAsStateWithLifecycle()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
    val screenSnapshot by viewModel.screenSnapshot.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val workflows by viewModel.workflows.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val scheduledTasks by viewModel.scheduledTasks.collectAsStateWithLifecycle()
    val apiProviders by viewModel.apiProviders.collectAsStateWithLifecycle()

    val isOverlayEnabled = viewModel.deviceManager.canDrawOverlays()
    val isVoiceKeyboardEnabled = viewModel.deviceManager.isDuaKeyboardEnabled()

    // Return to Home screen on system Back press from any secondary screen
    BackHandler(enabled = hasCompletedOnboarding && selectedMode != AuraMode.HOME) {
        viewModel.selectMode(AuraMode.HOME)
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.onSphereTapped()
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.triggerLocationCheck(openMaps = true)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.selectMode(AuraMode.CHAT)
                        viewModel.analyzeAttachedBitmap(bitmap, "")
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // Step-by-Step Setup Guide Modal (matches "Watch the setup guide" card in video)
    if (showSetupGuideModal) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowSetupGuide(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = DuaCrimsonPrimary,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text("Dua Fatima Setup Guide", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Live Voice (Urdu, Hindi, English): Tap the Crimson Orb or 'Tap to Talk' button on Home to speak naturally.")
                    Text("2. Screen Control & Typing: Enable 'Dua Fatima Screen Control Engine' in Android Accessibility Settings to let Dua Fatima type prompts in ChatGPT, tap buttons, and read screens.")
                    Text("3. App & Call Automation: Say 'YouTube kholo', 'ChatGPT kholo aur 10 horror stories likho', or 'Mummy ko call karo'.")
                    Text("4. AI Connections: Gemini 3.8 Flash is pre-connected via Firebase AI Logic. You can also add Groq, OpenAI, or Anthropic keys in Settings -> Connections.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.setShowSetupGuide(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = DuaCrimsonPrimary, contentColor = Color.White)
                ) {
                    Text("Got it")
                }
            }
        )
    }

    // "Let Dua Fatima open apps for you" Dialog (matches 00:26-00:36 in video: Display over other apps + Screen Control)
    if (showOpenAppsPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOpenAppsPrompt() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = "Let Dua Fatima open apps for you",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To open YouTube, WhatsApp or any app when you ask by voice, Android needs 'Display over other apps' and 'Screen Control' turned on for Dua Fatima. Dua Fatima only opens apps when you ask.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (!isAccessibilityConnected) {
                        OutlinedButton(
                            onClick = {
                                viewModel.dismissOpenAppsPrompt()
                                viewModel.deviceManager.openAccessibilitySettings()
                            },
                            border = BorderStroke(1.dp, DuaCrimsonPrimary)
                        ) {
                            Text("Also Enable Screen Control (Accessibility)", color = DuaCrimsonPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissOpenAppsPrompt()
                        if (!viewModel.deviceManager.canDrawOverlays()) {
                            viewModel.deviceManager.requestDisplayOverOtherAppsPermission()
                        } else {
                            viewModel.deviceManager.openAccessibilitySettings()
                        }
                    }
                ) {
                    Text("Allow", color = DuaCrimsonPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOpenAppsPrompt() }) {
                    Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Sensitive Action Confirmation Dialog (Calls, WhatsApp/SMS, Memory Wipe)
    if (pendingApproval != null) {
        val action = pendingApproval!!
        AlertDialog(
            onDismissRequest = { viewModel.rejectPendingAction() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = DuaCrimsonPrimary,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(action.title, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column {
                    Text(action.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Target: ${action.target}",
                        style = MaterialTheme.typography.labelLarge,
                        color = AuraAmber
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.approvePendingAction() },
                    colors = ButtonDefaults.buttonColors(containerColor = DuaCrimsonPrimary, contentColor = Color.White),
                    modifier = Modifier.testTag("approve_sensitive_action_button")
                ) {
                    Text("Approve & Execute", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.rejectPendingAction() },
                    modifier = Modifier.testTag("reject_sensitive_action_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Show 4-Step Onboarding Flow first (without any Google Sign-In screen!)
    if (!hasCompletedOnboarding) {
        DuaOnboardingFlow(
            isAccessibilityEnabled = isAccessibilityConnected,
            isOverlayEnabled = isOverlayEnabled,
            isVoiceKeyboardEnabled = isVoiceKeyboardEnabled,
            onOpenAccessibilitySettings = { viewModel.deviceManager.openAccessibilitySettings() },
            onRequestOverlayPermission = { viewModel.deviceManager.requestDisplayOverOtherAppsPermission() },
            onOpenVoiceKeyboardSettings = { viewModel.deviceManager.openVoiceKeyboardSettings() },
            onShowSetupGuide = { viewModel.setShowSetupGuide(true) },
            onCompleteOnboarding = { viewModel.completeOnboarding() }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            DuaFatimaTopHeader(
                selectedMode = selectedMode,
                isDarkMode = isDarkMode,
                isPoweredOn = isPoweredOn,
                isOnline = isOnline,
                onMenuOrBackClick = {
                    if (selectedMode == AuraMode.HOME) {
                        viewModel.selectMode(AuraMode.QUICK_ACTIONS)
                    } else {
                        viewModel.selectMode(AuraMode.HOME)
                    }
                },
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onTogglePower = {
                    if (isPoweredOn) viewModel.manualPowerOff() else viewModel.powerOnSession()
                },
                onSelectMode = { viewModel.selectMode(it) },
                onSettingsClick = { viewModel.selectMode(AuraMode.SETTINGS) }
            )
        },
        bottomBar = {
            DuaFatimaBottomBarWithOrb(
                selectedMode = selectedMode,
                sphereState = sphereState,
                isPoweredOn = isPoweredOn,
                onSelectTab = { mode -> viewModel.selectMode(mode) },
                onCenterOrbClick = {
                    if (selectedMode != AuraMode.VOICE) {
                        viewModel.selectMode(AuraMode.VOICE)
                    }
                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Error Notification Banner
            AnimatedVisibility(visible = errorBanner != null) {
                Surface(
                    color = AuraCrimson.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AuraCrimson),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = AuraCrimson,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorBanner.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.dismissError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedMode) {
                    AuraMode.HOME -> DuaHomeDashboardScreen(
                        sphereState = sphereState,
                        statusSubtitle = statusSubtitle,
                        liveTranscript = liveTranscript,
                        isDarkMode = isDarkMode,
                        continuousVoiceActive = continuousVoiceActive,
                        onTapToTalk = {
                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onStartLiveCall = {
                            viewModel.selectMode(AuraMode.VOICE)
                            if (!continuousVoiceActive) {
                                viewModel.toggleContinuousVoiceMode()
                            }
                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onSubmitQuestion = { query ->
                            viewModel.processUserCommand(query, fromVoice = false)
                            viewModel.selectMode(AuraMode.CHAT)
                        },
                        onNavigateMode = { mode -> viewModel.selectMode(mode) },
                        onOpenQuickActions = { viewModel.selectMode(AuraMode.QUICK_ACTIONS) }
                    )

                    AuraMode.QUICK_ACTIONS -> DuaQuickActionsScreen(
                        onBack = { viewModel.selectMode(AuraMode.HOME) },
                        onRunQuickAction = { cmd ->
                            viewModel.processUserCommand(cmd, fromVoice = true)
                            viewModel.selectMode(AuraMode.CHAT)
                        },
                        onStartLiveVoice = {
                            viewModel.selectMode(AuraMode.VOICE)
                            if (!continuousVoiceActive) {
                                viewModel.toggleContinuousVoiceMode()
                            }
                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onOpenMode = { mode -> viewModel.selectMode(mode) }
                    )

                    AuraMode.VOICE -> AuraLiveVoiceModeScreen(
                        sphereState = sphereState,
                        voiceAmplitude = voiceAmplitude,
                        isPoweredOn = isPoweredOn,
                        continuousVoiceActive = continuousVoiceActive,
                        instantVoiceReplyMode = instantVoiceReplyMode,
                        selectedLocaleTag = selectedVoiceLocale,
                        statusSubtitle = statusSubtitle,
                        liveTranscript = liveTranscript,
                        proactiveBanner = proactiveBanner,
                        latestAuraMessage = messages.lastOrNull { it.sender == "AURA" },
                        onSphereClick = { micPermLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onToggleContinuousVoice = { viewModel.toggleContinuousVoiceMode() },
                        onToggleInstantReplyMode = { viewModel.toggleInstantVoiceReplyMode() },
                        onSelectLocale = { viewModel.setVoiceLanguage(it) },
                        onQuickCommand = { cmd -> viewModel.processUserCommand(cmd, fromVoice = true) }
                    )

                    AuraMode.CHAT -> AuraChatModeScreen(
                        messages = messages,
                        onPickImageForAnalysis = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onOpenUrl = { url -> viewModel.deviceManager.openUrlInBrowser(url) },
                        onSendMessage = { text -> viewModel.processUserCommand(text, fromVoice = false) }
                    )

                    AuraMode.HISTORY -> DuaConversationsHistoryScreen(
                        messages = messages,
                        sphereState = sphereState,
                        onStartConversation = {
                            viewModel.selectMode(AuraMode.VOICE)
                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onClearHistory = { viewModel.clearHistory() }
                    )

                    AuraMode.SETTINGS -> DuaSettingsDashboardScreen(
                        isDarkMode = isDarkMode,
                        isPoweredOn = isPoweredOn,
                        isAccessibilityEnabled = isAccessibilityConnected,
                        isOverlayEnabled = isOverlayEnabled,
                        isVoiceKeyboardEnabled = isVoiceKeyboardEnabled,
                        selectedLocaleTag = selectedVoiceLocale,
                        memoriesCount = memories.size,
                        workflowsCount = workflows.size,
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onTogglePower = {
                            if (isPoweredOn) viewModel.manualPowerOff() else viewModel.powerOnSession()
                        },
                        onCycleVoiceLanguage = { viewModel.cycleVoiceLanguage() },
                        onOpenMode = { mode -> viewModel.selectMode(mode) },
                        onOpenSetupGuide = { viewModel.setShowSetupGuide(true) },
                        onRequestOverlayPermission = { viewModel.deviceManager.requestDisplayOverOtherAppsPermission() },
                        onOpenVoiceKeyboardSettings = { viewModel.deviceManager.openVoiceKeyboardSettings() },
                        onShowKeyboardPicker = { viewModel.deviceManager.showKeyboardPicker() }
                    )

                    AuraMode.AGENT -> AuraScreenAgentScreen(
                        isAccessibilityEnabled = isAccessibilityConnected,
                        isOverlayEnabled = isOverlayEnabled,
                        isVoiceKeyboardEnabled = isVoiceKeyboardEnabled,
                        screenSnapshot = screenSnapshot,
                        supportedApps = installedApps.ifEmpty { viewModel.deviceManager.supportedApps },
                        onOpenAccessibilitySettings = { viewModel.deviceManager.openAccessibilitySettings() },
                        onRequestOverlayPermission = { viewModel.deviceManager.requestDisplayOverOtherAppsPermission() },
                        onOpenVoiceKeyboardSettings = { viewModel.deviceManager.openVoiceKeyboardSettings() },
                        onShowKeyboardPicker = { viewModel.deviceManager.showKeyboardPicker() },
                        onScanScreen = { viewModel.triggerScreenRead() },
                        onTypeOnScreen = { text -> viewModel.triggerDirectScreenType(text, append = false) },
                        onAppendOnScreen = { text -> viewModel.triggerDirectScreenType(text, append = true) },
                        onPasteOnScreen = { text -> viewModel.triggerPasteOnScreen(text) },
                        onClearScreenText = { viewModel.triggerClearScreenText() },
                        onTapOnScreen = { label -> viewModel.triggerDirectScreenTap(label) },
                        onTapElementNode = { node -> viewModel.triggerTapScreenNode(node) },
                        onScrollScreen = { forward -> viewModel.triggerScrollScreen(forward) },
                        onToggleFlashlight = { viewModel.triggerToggleFlashlight() },
                        onAdjustVolume = { inc -> viewModel.triggerAdjustVolume(inc) },
                        onCheckLocation = {
                            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        onOpenSystemSetting = { action -> viewModel.triggerOpenSystemSetting(action) },
                        onLaunchApp = { appId -> viewModel.deviceManager.launchAppOrFallback(appId) },
                        onRefreshApps = { viewModel.refreshInstalledApps() },
                        onRequestPhoneCall = { contact ->
                            viewModel.processUserCommand("Call $contact", fromVoice = false)
                        }
                    )

                    AuraMode.WORKFLOW -> AuraWorkflowBuilderScreen(
                        workflows = workflows,
                        onBuildFromNaturalLanguage = { desc -> viewModel.buildWorkflowFromPrompt(desc) },
                        onExecuteWorkflow = { wf -> viewModel.executeWorkflow(wf) },
                        onAddManualStep = { wf, title, type, target, param ->
                            viewModel.addStepToWorkflow(wf, title, type, target, param)
                        },
                        onDuplicateWorkflow = { wf -> viewModel.duplicateWorkflow(wf) },
                        onDeleteWorkflow = { id -> viewModel.deleteWorkflow(id) }
                    )

                    AuraMode.MEMORY -> AuraMemoryScreen(
                        memories = memories,
                        onAddMemory = { tier, title, content, tags ->
                            viewModel.addMemoryEntry(tier, title, content, tags)
                        },
                        onDeleteMemory = { id -> viewModel.deleteMemoryEntry(id) },
                        onWipeAllMemory = { viewModel.clearAllMemoriesWithConfirmation() }
                    )

                    AuraMode.SCHEDULER -> AuraSchedulerAndOfflineScreen(
                        tasks = scheduledTasks,
                        isOnline = isOnline,
                        onAddTask = { title, cmd, time, rec ->
                            viewModel.addScheduledTask(title, cmd, time, rec)
                        },
                        onRunTaskNow = { task -> viewModel.runScheduledTaskNow(task) },
                        onDeleteTask = { id -> viewModel.deleteScheduledTask(id) }
                    )

                    AuraMode.PROJECTS -> AuraProjectsScreen(
                        projects = projects,
                        onRunNextStep = { action ->
                            viewModel.processUserCommand(action, fromVoice = false)
                        }
                    )

                    AuraMode.API_CENTER -> AuraApiCenterScreen(
                        providers = apiProviders,
                        onSaveProvider = { prov, key, textM, voiceM, url, primary ->
                            viewModel.saveProviderConfig(prov, key, textM, voiceM, url, primary)
                        },
                        onTestProvider = { prov -> viewModel.testApiProvider(prov) }
                    )
                }
            }
        }
    }
}

/**
 * Top Bar matching the video (00:28) with:
 * - Left: Menu (`≡`) button for Quick Actions (or Back arrow on inner screens)
 * - Center: DUA FATIMA • YOUR AI ASSISTANT
 * - Right: Light/Dark Theme Toggle + Power Off pill + Settings (`⚙`) button
 * - Plus a sleek sub-bar of autonomous agent chips when exploring advanced tools.
 */
@Composable
private fun DuaFatimaTopHeader(
    selectedMode: AuraMode,
    isDarkMode: Boolean,
    isPoweredOn: Boolean,
    isOnline: Boolean,
    onMenuOrBackClick: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onTogglePower: () -> Unit,
    onSelectMode: (AuraMode) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Menu / Quick Actions Button (matches 00:28 top-left button)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("top_menu_quick_actions_button")
                    .clickable { onMenuOrBackClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (selectedMode == AuraMode.HOME) {
                            Icons.Default.Menu
                        } else {
                            Icons.AutoMirrored.Filled.ArrowBack
                        },
                        contentDescription = if (selectedMode == AuraMode.HOME) "Quick Actions Menu" else "Back to Home",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Center Brand Title: DUA FATIMA • YOUR AI ASSISTANT
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DUA ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "FATIMA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = DuaCrimsonPrimary
                    )
                }
                Text(
                    text = if (!isPoweredOn) {
                        "POWERED OFF"
                    } else if (isOnline) {
                        "YOUR AI ASSISTANT"
                    } else {
                        "OFFLINE INTELLIGENCE"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = if (!isPoweredOn) AuraCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right Controls: Light/Dark Mode Toggle + Settings Gear Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("top_theme_toggle_button")
                        .clickable { onToggleDarkMode() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Light or Dark Mode",
                            tint = DuaCrimsonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("top_settings_button")
                        .clickable { onSettingsClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Compact Autonomous Tool Strip (Quick Actions, Screen Agent, Workflows, Scheduler, Memory, Power Off)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf(
                AuraMode.QUICK_ACTIONS,
                AuraMode.AGENT,
                AuraMode.WORKFLOW,
                AuraMode.MEMORY,
                AuraMode.SCHEDULER,
                AuraMode.PROJECTS,
                AuraMode.API_CENTER
            ).forEach { mode ->
                val isSelected = selectedMode == mode
                Surface(
                    color = if (isSelected) DuaCrimsonPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) DuaCrimsonPrimary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.clickable { onSelectMode(mode) }
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                }
            }

            // Manual Power Off / On Pill
            Surface(
                color = if (isPoweredOn) AuraCrimson.copy(alpha = 0.16f) else AuraEmerald.copy(alpha = 0.2f),
                shape = CircleShape,
                border = BorderStroke(1.dp, if (isPoweredOn) AuraCrimson else AuraEmerald),
                modifier = Modifier
                    .testTag("manual_power_off_button")
                    .clickable { onTogglePower() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power Toggle",
                        tint = if (isPoweredOn) AuraCrimson else AuraEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPoweredOn) "POWER OFF" else "POWER ON",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPoweredOn) AuraCrimson else AuraEmerald
                    )
                }
            }
        }
    }
}

/**
 * Matches the Bottom Navigation Bar in the reference video (00:28, 00:45, 00:49):
 * Home | Chat | [Raised Center Glowing 3D Crimson Orb] | History | Settings
 */
@Composable
private fun DuaFatimaBottomBarWithOrb(
    selectedMode: AuraMode,
    sphereState: AuraSphereState,
    isPoweredOn: Boolean,
    onSelectTab: (AuraMode) -> Unit,
    onCenterOrbClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        // Rounded Pill Dock Background (Clean 0.8dp border)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxSize()
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    selected = selectedMode == AuraMode.HOME,
                    onClick = { onSelectTab(AuraMode.HOME) },
                    testTag = "nav_tab_home"
                )
                BottomNavItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "Chat",
                    selected = selectedMode == AuraMode.CHAT,
                    onClick = { onSelectTab(AuraMode.CHAT) },
                    testTag = "nav_tab_chat"
                )

                // Space reserved for the center glowing orb
                Spacer(modifier = Modifier.width(62.dp))

                BottomNavItem(
                    icon = Icons.Default.History,
                    label = "History",
                    selected = selectedMode == AuraMode.HISTORY,
                    onClick = { onSelectTab(AuraMode.HISTORY) },
                    testTag = "nav_tab_history"
                )
                BottomNavItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    selected = selectedMode == AuraMode.SETTINGS,
                    onClick = { onSelectTab(AuraMode.SETTINGS) },
                    testTag = "nav_tab_settings"
                )
            }
        }

        // Raised Center Glowing Crimson AI Orb (Clean non-overlapping border)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = (-5).dp)
                .size(60.dp)
                .shadow(12.dp, CircleShape, ambientColor = DuaCrimsonPrimary, spotColor = DuaCrimsonPrimary)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(DuaRubyBright, Color(0xFFFFB74D), DuaCrimsonPrimary)
                    ),
                    shape = CircleShape
                )
                .testTag("bottom_center_orb_button")
                .clickable { onCenterOrbClick() }
        ) {
            AuraGlowingSphere(
                state = sphereState,
                voiceAmplitude = 0.5f,
                isPoweredOn = isPoweredOn,
                sphereSize = 52.dp,
                showBadge = false,
                onSphereClick = onCenterOrbClick
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val tint = if (selected) DuaCrimsonPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .testTag(testTag)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = tint
        )
    }
}
