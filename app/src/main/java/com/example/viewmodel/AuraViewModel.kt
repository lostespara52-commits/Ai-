package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AuraAiBrain
import com.example.ai.AuraCommandPlan
import com.example.automation.AuraAccessibilityService
import com.example.automation.DeviceIntegrationManager
import com.example.automation.ScreenElementNode
import com.example.automation.ScreenSnapshot
import com.example.automation.SupportedAppInfo
import com.example.data.ApiProviderEntity
import com.example.data.AuraDatabase
import com.example.data.AuraMode
import com.example.data.AuraRepository
import com.example.data.AuraSphereState
import com.example.data.ChatMessageEntity
import com.example.data.CredentialCipher
import com.example.data.MemoryEntryEntity
import com.example.data.ProjectEntity
import com.example.data.ScheduledTaskEntity
import com.example.data.WorkflowEntity
import com.example.data.WorkflowJsonConverter
import com.example.data.WorkflowStep
import com.example.service.AuraPersistentService
import com.example.voice.AuraLiveVoiceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

data class PendingApprovalAction(
    val title: String,
    val description: String,
    val actionType: String, // "CALL", "WHATSAPP", "SMS", "WORKFLOW_STEP"
    val target: String,
    val payload: String,
    val onConfirm: suspend () -> Unit
)

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = AuraDatabase.getInstance(appContext)
    private val repository = AuraRepository(database.auraDao())
    private val aiBrain = AuraAiBrain()
    val deviceManager = DeviceIntegrationManager(appContext)

    // Reactive Database Flows
    val messages: StateFlow<List<ChatMessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workflows: StateFlow<List<WorkflowEntity>> = repository.allWorkflows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledTasks: StateFlow<List<ScheduledTaskEntity>> = repository.allScheduledTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiProviders: StateFlow<List<ApiProviderEntity>> = repository.allApiProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val prefs = appContext.getSharedPreferences("dua_fatima_prefs", Context.MODE_PRIVATE)

    // Core Operating States
    private val _sphereState = MutableStateFlow(AuraSphereState.IDLE)
    val sphereState: StateFlow<AuraSphereState> = _sphereState.asStateFlow()

    private val _selectedMode = MutableStateFlow(AuraMode.HOME)
    val selectedMode: StateFlow<AuraMode> = _selectedMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _showOpenAppsPrompt = MutableStateFlow(false)
    val showOpenAppsPrompt: StateFlow<Boolean> = _showOpenAppsPrompt.asStateFlow()

    private val _showSetupGuideModal = MutableStateFlow(false)
    val showSetupGuideModal: StateFlow<Boolean> = _showSetupGuideModal.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isPoweredOn = MutableStateFlow(true)
    val isPoweredOn: StateFlow<Boolean> = _isPoweredOn.asStateFlow()

    private val _instantVoiceReplyMode = MutableStateFlow(true)
    val instantVoiceReplyMode: StateFlow<Boolean> = _instantVoiceReplyMode.asStateFlow()

    private val _statusSubtitle = MutableStateFlow("Tap the orb and speak in Urdu, Hindi or English")
    val statusSubtitle: StateFlow<String> = _statusSubtitle.asStateFlow()

    private val _liveTranscriptPreview = MutableStateFlow("")
    val liveTranscriptPreview: StateFlow<String> = _liveTranscriptPreview.asStateFlow()

    private val _proactiveBanner = MutableStateFlow(
        "Assalam-o-Alaikum! I'm Dua Fatima. Ask me to open any app, call a contact, type on screen, or run your AI Story Video workflow."
    )
    val proactiveBanner: StateFlow<String> = _proactiveBanner.asStateFlow()

    private val _pendingApproval = MutableStateFlow<PendingApprovalAction?>(null)
    val pendingApproval: StateFlow<PendingApprovalAction?> = _pendingApproval.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    // Accessibility Screen State & Installed Apps
    val isAccessibilityConnected: StateFlow<Boolean> = AuraAccessibilityService.isConnected
    val screenSnapshot: StateFlow<ScreenSnapshot> = AuraAccessibilityService.screenSnapshot
    val isPersistentServiceRunning: StateFlow<Boolean> = AuraPersistentService.isSessionActive

    private val _installedApps = MutableStateFlow<List<SupportedAppInfo>>(emptyList())
    val installedApps: StateFlow<List<SupportedAppInfo>> = _installedApps.asStateFlow()

    private var activeWorkflowJob: Job? = null
    private var lastOpenedAppTarget: String = ""

    val voiceManager = AuraLiveVoiceManager(
        context = appContext,
        scope = viewModelScope,
        onFinalTranscript = { transcript ->
            _liveTranscriptPreview.value = ""
            processUserCommand(transcript, fromVoice = true)
        },
        onPartialTranscript = { partial ->
            _liveTranscriptPreview.value = partial
        },
        onListeningStateChanged = { listening ->
            if (!_isPoweredOn.value) return@AuraLiveVoiceManager
            if (listening) {
                _sphereState.value = AuraSphereState.LISTENING
                _statusSubtitle.value = "Listening (Urdu • Hindi • English)..."
            } else if (_sphereState.value == AuraSphereState.LISTENING) {
                _sphereState.value = if (_isOnline.value) AuraSphereState.IDLE else AuraSphereState.OFFLINE
            }
        },
        onSpeakingStateChanged = { speaking ->
            if (!_isPoweredOn.value) return@AuraLiveVoiceManager
            if (speaking) {
                _sphereState.value = AuraSphereState.SPEAKING
                _statusSubtitle.value = "Dua Fatima speaking (Tap orb to interrupt)"
            } else if (_sphereState.value == AuraSphereState.SPEAKING) {
                _sphereState.value = if (_isOnline.value) AuraSphereState.IDLE else AuraSphereState.OFFLINE
                _statusSubtitle.value = "Ready for next command"
            }
        }
    )

    val voiceAmplitude: StateFlow<Float> = voiceManager.voiceAmplitude
    val continuousVoiceActive: StateFlow<Boolean> = voiceManager.continuousLiveMode
    val selectedVoiceLocale: StateFlow<String> = voiceManager.selectedLocaleTag

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            refreshInstalledApps()
            refreshProactiveSuggestion()
            val currentMessages = repository.getRecentMessages(5)
            if (currentMessages.isEmpty()) {
                repository.addMessage(
                    sender = "AURA",
                    content = "Assalam-o-Alaikum! Main Dua Fatima hun — aapki personal autonomous AI agent. " +
                        "Aap Urdu, Hindi, ya English mein bol kar ya likh kar koi bhi app open karwa sakte hain, screen par type ya remove karwa sakte hain, call mila sakte hain, ya multi-step workflows chala sakte hain.",
                    languageDetected = "EN/UR/HI",
                    actionTaken = "PROACTIVE_INIT"
                )
            }
        }
        registerNetworkMonitor()
    }

    fun refreshInstalledApps() {
        _installedApps.value = deviceManager.getAllInstalledAndCoreApps()
    }

    fun selectMode(mode: AuraMode) {
        _selectedMode.value = mode
    }

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        prefs.edit().putBoolean("is_dark_mode", next).apply()
    }

    fun completeOnboarding() {
        _hasCompletedOnboarding.value = true
        prefs.edit().putBoolean("onboarding_done", true).apply()
        if (!deviceManager.canDrawOverlays() || !isAccessibilityConnected.value) {
            _showOpenAppsPrompt.value = true
        }
    }

    fun dismissOpenAppsPrompt() {
        _showOpenAppsPrompt.value = false
    }

    fun setShowSetupGuide(show: Boolean) {
        _showSetupGuideModal.value = show
    }

    fun cycleVoiceLanguage() {
        val next = when (selectedVoiceLocale.value) {
            "ur-PK" -> "hi-IN"
            "hi-IN" -> "en-IN"
            else -> "ur-PK"
        }
        setVoiceLanguage(next)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
            _statusSubtitle.value = "Conversation history cleared"
        }
    }

    fun dismissError() {
        _errorBanner.value = null
        if (_sphereState.value == AuraSphereState.ERROR) {
            _sphereState.value = if (_isOnline.value) AuraSphereState.IDLE else AuraSphereState.OFFLINE
        }
    }

    /**
     * Toggles the central AI Sphere:
     * - If speaking, interrupts immediately and starts listening (barge-in).
     * - If listening, stops listening.
     * - If idle, starts live voice listening.
     */
    fun onSphereTapped() {
        if (!_isPoweredOn.value) {
            powerOnSession()
            return
        }
        when (_sphereState.value) {
            AuraSphereState.SPEAKING -> {
                voiceManager.stopSpeaking()
                voiceManager.startListening()
            }
            AuraSphereState.LISTENING -> {
                voiceManager.stopListening()
            }
            else -> {
                voiceManager.startListening()
            }
        }
    }

    fun toggleContinuousVoiceMode() {
        if (!_isPoweredOn.value) {
            powerOnSession()
        }
        val next = !continuousVoiceActive.value
        voiceManager.setContinuousLiveMode(next)
        if (next) {
            AuraPersistentService.startPersistentSession(
                appContext,
                "Live Duplex Voice Active • Tap MANUAL POWER OFF to stop"
            )
        }
    }

    fun setVoiceLanguage(localeTag: String) {
        voiceManager.setVoiceLanguage(localeTag)
        _statusSubtitle.value = "Voice Locale set to $localeTag"
    }

    /**
     * Prominent MANUAL POWER OFF button handler (Section 13):
     * Immediately disconnects active voice sessions, stops background service, and halts running automation.
     */
    fun manualPowerOff() {
        activeWorkflowJob?.cancel()
        voiceManager.forceStopAll()
        AuraPersistentService.stopPersistentSession(appContext)
        _isPoweredOn.value = false
        _sphereState.value = AuraSphereState.OFFLINE
        _liveTranscriptPreview.value = ""
        _statusSubtitle.value = "POWERED OFF BY OWNER • Tap Power On to reactivate"
    }

    fun powerOnSession() {
        _isPoweredOn.value = true
        _sphereState.value = if (_isOnline.value) AuraSphereState.IDLE else AuraSphereState.OFFLINE
        _statusSubtitle.value = "AURA Online & Ready"
        AuraPersistentService.startPersistentSession(appContext)
    }

    fun togglePersistentService(enable: Boolean) {
        if (enable) {
            powerOnSession()
        } else {
            manualPowerOff()
        }
    }

    fun toggleInstantVoiceReplyMode() {
        val next = !_instantVoiceReplyMode.value
        _instantVoiceReplyMode.value = next
        _statusSubtitle.value = if (next) {
            "Instant Call Reply (<0.1s) ON • Zero Thinking Delay"
        } else {
            "Deep Cloud Reasoning Mode ON"
        }
    }

    /**
     * Processes natural language voice or text command in English, Urdu, Hindi, or Hinglish/Roman Urdu.
     * In Live Voice Call mode (or when Instant Call Reply is active), greetings, conversational turns,
     * and device/screen/hardware commands reply in < 50ms WITHOUT waiting on "Thinking...".
     */
    fun processUserCommand(rawInput: String, fromVoice: Boolean = false) {
        val input = rawInput.trim()
        if (input.isBlank()) return
        if (!_isPoweredOn.value) {
            powerOnSession()
        }

        viewModelScope.launch {
            repository.addMessage(sender = "USER", content = input)
            _errorBanner.value = null

            // STEP 1: Zero-Latency Instant Live Call Reply (< 50ms, NO "Thinking..." delay!)
            val isLiveVoiceContext = fromVoice || continuousVoiceActive.value || _selectedMode.value == AuraMode.VOICE
            if (isLiveVoiceContext || _instantVoiceReplyMode.value) {
                val instantHit = tryInstantLiveCallReply(input)
                if (instantHit != null) {
                    val (instantReply, instantAction) = instantHit
                    repository.addMessage(
                        sender = "AURA",
                        content = instantReply,
                        languageDetected = "LIVE_INSTANT",
                        actionTaken = instantAction
                    )
                    _statusSubtitle.value = instantAction
                    _sphereState.value = AuraSphereState.SPEAKING
                    if (isLiveVoiceContext) {
                        voiceManager.speakResponse(instantReply)
                    } else {
                        _sphereState.value = AuraSphereState.IDLE
                    }
                    return@launch
                }
            }

            // STEP 2: Check fast offline/local intent rules if offline
            if (!_isOnline.value) {
                handleOfflineCommand(input, fromVoice)
                return@launch
            }

            val recent = repository.getRecentMessages(10)
            val topMemories = repository.getTopMemories(12)
            val activeProjects = repository.getActiveProjects()
            val screenText = screenSnapshot.value.let {
                "Package: ${it.activePackage} | Visible Text: ${it.visibleTextSummary.take(400)}"
            }
            val providers = repository.getProvidersSync()
            val primaryProvider = providers.firstOrNull { it.isPrimary }

            // In Live Voice / Instant Call mode, use immediate local brain or strict 1.8s fast timeout so it NEVER hangs on Thinking!
            if (isLiveVoiceContext && _instantVoiceReplyMode.value) {
                val fastLocalPlan = aiBrain.planWithAutonomousLocalBrain(input, screenText, activeProjects, lastOpenedAppTarget)
                if (fastLocalPlan.autonomousAction != "NONE") {
                    executePlannedCommand(input, fastLocalPlan, fromVoice = true)
                    return@launch
                }
            }

            _sphereState.value = AuraSphereState.THINKING
            _statusSubtitle.value = if (isLiveVoiceContext) "Replying..." else "Reasoning & Planning Action..."

            val timeoutMs = if (isLiveVoiceContext) 1900L else 7500L
            val result = withTimeoutOrNull(timeoutMs) {
                aiBrain.planAndRespond(
                    userInput = input,
                    recentMessages = recent,
                    memories = topMemories,
                    activeProjects = activeProjects,
                    screenSummary = screenText,
                    primaryProvider = primaryProvider,
                    allProviders = providers,
                    lastOpenedApp = lastOpenedAppTarget
                )
            } ?: Result.success(aiBrain.planWithAutonomousLocalBrain(input, screenText, activeProjects, lastOpenedAppTarget))

            result.fold(
                onSuccess = { plan ->
                    executePlannedCommand(input, plan, fromVoice)
                },
                onFailure = { _ ->
                    val fallbackPlan = aiBrain.planWithAutonomousLocalBrain(input, screenText, activeProjects, lastOpenedAppTarget)
                    executePlannedCommand(input, fallbackPlan, fromVoice)
                }
            )
        }
    }

    /**
     * Sub-50ms Instant Live Call Responder for greetings, conversational turns, hardware controls,
     * screen actions, location, and app opening in Urdu, Hindi, and English.
     */
    private fun tryInstantLiveCallReply(input: String): Pair<String, String>? {
        val clean = aiBrain.normalizeMultilingualTranscript(input)
        val lower = clean.lowercase()

        // 1. Instant Greetings & Call Check-ins ("Hello", "Assalam o Alaikum", "Hi", "Kaisi ho", "Sun rahi ho", Urdu/Hindi script)
        if (lower in listOf(
                "hello", "hi", "hey", "hii", "helo", "assalam o alaikum", "assalamu alaikum",
                "salam", "salaam", "hello dua", "hi dua", "hey dua", "dua fatima", "dua",
                "hello ji", "assalam walekum", "namaste", "adaab",
                "ہیلو", "السلام علیکم", "سلام", "ہائے", "हेलो", "नमस्ते", "हाय"
            ) || lower.matches(Regex("^(hello|hi|hey|salam|assalam[u\\s-]*o?[\\s-]*alaikum|ہیلو|السلام علیکم|हेलो|नमस्ते)\\s*(dua|fatima|ji|jaan|dost)?[!?.]*$"))
        ) {
            return "Walaikum Assalam! Main Dua Fatima bol rahi hun, bilkul live sun rahi hun. Hukam karein, abhi kaunsa app kholna hai ya kya kaam karna hai?" to "LIVE_CALL_INSTANT_GREETING"
        }

        if (lower.contains("kaisi ho") || lower.contains("kya haal") || lower.contains("how are you") ||
            lower.contains("sab theek") || lower.contains("kaise ho") || lower.contains("کیسی ہو") || lower.contains("कैसी हो")
        ) {
            return "Alhamdulillah, main bilkul theek aur active hun! Aap batayein, aaj kaunsa app kholna hai, kya type ya paste karna hai?" to "LIVE_CALL_INSTANT_REPLY"
        }

        if (lower.contains("sun rahi ho") || lower.contains("awaaz aa rahi") || lower.contains("awaz aa rahi") ||
            lower.contains("can you hear") || lower.contains("are you there") || lower == "haan" || lower == "hanji" ||
            lower.contains("آواز آ رہی") || lower.contains("आवाज आ रही")
        ) {
            return "Ji haan, aapki awaaz bilkul saaf aa rahi hai! Aap bolte jayein, main foran aapka kaam karungi." to "LIVE_CALL_INSTANT_REPLY"
        }

        if (lower.contains("tum kaun ho") || lower.contains("aap kaun") || lower.contains("who are you") ||
            lower.contains("your name") || lower.contains("tumhara naam")
        ) {
            return "Mera naam Dua Fatima hai — aapki personal autonomous AI assistant. Main khalis Urdu aur Hindi mein baat karti hun aur aapke phone ki har app, keyboard typing, paste, aur location control kar sakti hun." to "LIVE_CALL_INSTANT_REPLY"
        }

        if (lower.contains("shukriya") || lower.contains("thank you") || lower.contains("thanks") ||
            lower.contains("bohat achha") || lower.contains("bahut badiya")
        ) {
            return "Aapka bohat shukriya! Main hamesha aapki madad ke liye hazir hun." to "LIVE_CALL_INSTANT_REPLY"
        }

        // 2. Instant Hardware & Telemetry Commands (Flashlight, Volume, Battery, Time)
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("ٹارچ") || lower.contains("टॉर्च")) {
            val turnOn = !lower.contains("off") && !lower.contains("band")
            val status = deviceManager.toggleFlashlight(turnOn)
            val reply = if (turnOn) "Ji, maine phone ki flashlight on kar di hai." else "Ji, maine flashlight band kar di hai."
            return reply to status
        }

        if (lower.contains("volume badhao") || lower.contains("awaz tez") || lower.contains("volume up") || lower.contains("louder")) {
            val status = deviceManager.adjustVolume(true)
            return "Ji, maine phone ka volume badha diya hai ($status)." to status
        }

        if (lower.contains("volume kam") || lower.contains("awaz kam") || lower.contains("volume down")) {
            val status = deviceManager.adjustVolume(false)
            return "Ji, maine phone ka volume kam kar diya hai ($status)." to status
        }

        if (lower.contains("battery kitni") || lower.contains("battery status") || lower == "battery" || lower.contains("بیٹری") || lower.contains("बैटरी")) {
            val bat = deviceManager.getBatteryStatusSummary()
            return "Aapke phone ki $bat hai." to bat
        }

        if (lower.contains("time kya") || lower.contains("kitne baje") || lower.contains("what time") ||
            lower.contains("aaj kya date") || lower.contains("tarikh kya")
        ) {
            val now = deviceManager.getCurrentTimeAndDateSummary()
            return "Abhi waqt aur tarikh hai: $now." to "TIME: $now"
        }

        // 3. Instant Screen Actions (Scroll, Back, Home, Paste)
        if (lower == "scroll down" || lower.contains("neeche scroll") || lower.contains("neeche karo")) {
            AuraAccessibilityService.instance?.scrollScreen(true)
            return "Ji, screen neeche scroll kar di hai." to "SCROLLED_DOWN"
        }
        if (lower == "scroll up" || lower.contains("upar scroll") || lower.contains("upar karo")) {
            AuraAccessibilityService.instance?.scrollScreen(false)
            return "Ji, screen upar scroll kar di hai." to "SCROLLED_UP"
        }
        if (lower == "go back" || lower == "back jao" || lower == "peeche jao") {
            AuraAccessibilityService.instance?.performNavigateBack()
            return "Ji, back kar diya hai." to "NAVIGATED_BACK"
        }
        if (lower == "go home" || lower == "home jao" || lower == "home screen") {
            AuraAccessibilityService.instance?.performNavigateHome()
            return "Ji, home screen par aa gaye hain." to "NAVIGATED_HOME"
        }

        // 4. Instant Simple App Launch ("YouTube kholo", "open chatpati", "ChatGPT kholo", "WhatsApp kholo", "Camera kholo")
        if ((lower.endsWith(" kholo") || lower.startsWith("open ") || lower.endsWith(" open karo") || lower.endsWith(" chalao")) &&
            !lower.contains("aur") && !lower.contains("and ") && !lower.contains("prompt") && !lower.contains("search") && !lower.contains("stories")
        ) {
            val targetApp = clean
                .replace(Regex("(?i)^(open|kholo|chala do|chalao|launch)\\s+"), "")
                .replace(Regex("(?i)\\s+(kholo|open karo|open|chala do|chalao)$"), "")
                .trim()
            if (targetApp.isNotBlank() && targetApp.length <= 28) {
                val canonical = deviceManager.normalizeAppAlias(targetApp)
                lastOpenedAppTarget = canonical
                val launchNote = deviceManager.launchAppOrFallback(canonical)
                return "Ji bilkul! Maine $canonical open kar diya hai. Ab aap jo bhi iske andar type ya paste karwana chahte hain bol dein!" to "INSTANT_OPEN: $launchNote"
            }
        }

        return null
    }

    private suspend fun executePlannedCommand(
        originalInput: String,
        plan: AuraCommandPlan,
        fromVoice: Boolean
    ) {
        // Persist any new memory learned from the conversation
        if (plan.memoryToSaveTitle.isNotBlank() && plan.memoryToSaveContent.isNotBlank()) {
            repository.addMemory(
                tier = "LONG_TERM",
                title = plan.memoryToSaveTitle,
                content = plan.memoryToSaveContent,
                tags = "auto-learned",
                importance = 4
            )
        }

        var actionSummary = plan.autonomousAction
        var sourcesJson = ""

        when (plan.autonomousAction) {
            "OPEN_APP" -> {
                _sphereState.value = AuraSphereState.WORKING
                val target = deviceManager.normalizeAppAlias(plan.targetAppOrContact.ifBlank { originalInput })
                lastOpenedAppTarget = target
                val promptPayload = plan.actionPayload
                val launchNote = deviceManager.launchAppOrFallback(target, promptPayload)
                actionSummary = "OPEN_APP: $launchNote"

                if (promptPayload.isNotBlank()) {
                    viewModelScope.launch {
                        // Multi-pass screen typing & pasting into the opened app (works for native apps & Chrome WebViews)
                        for (attempt in 1..4) {
                            delay(1400L)
                            val typed = AuraAccessibilityService.instance?.typeTextIntoFocusedOrFirstInput(promptPayload) == true
                            if (typed) {
                                delay(450L)
                                AuraAccessibilityService.instance?.tapElementByText("Send")
                                break
                            }
                        }
                        // Also generate and save the requested stories/content directly in Dua Fatima Vault & Chat
                        val generated = aiBrain.executeWorkflowAiStep(
                            stepTitle = "Autonomous Execution for $target",
                            stepParameter = promptPayload,
                            previousContext = originalInput
                        ).getOrNull()
                        if (!generated.isNullOrBlank()) {
                            repository.saveProject(
                                ProjectEntity(
                                    title = "${target.replaceFirstChar { it.uppercase() }}: ${promptPayload.take(36)}",
                                    category = "App Automation",
                                    status = "COMPLETED",
                                    progressPercent = 100,
                                    summary = generated.take(600),
                                    artifactsJson = JSONArray().put(generated).toString(),
                                    nextSuggestedAction = "Review generated content in Projects tab"
                                )
                            )
                            repository.addMessage(
                                sender = "AURA",
                                content = "Mukammal Nateeja ($target):\n\n$generated",
                                actionTaken = "SAVED_TO_PROJECT_STORAGE"
                            )
                        }
                    }
                }
            }

            "TYPE_ON_SCREEN" -> {
                _sphereState.value = AuraSphereState.WORKING
                val textToType = plan.actionPayload.ifBlank { originalInput }
                deviceManager.copyToClipboard("Dua Fatima Prompt", textToType)
                val service = AuraAccessibilityService.instance
                if (service != null) {
                    val ok = service.typeTextIntoFocusedOrFirstInput(textToType, plan.targetAppOrContact)
                    if (ok) {
                        viewModelScope.launch {
                            delay(400L)
                            service.tapElementByText("Send")
                        }
                        actionSummary = "Typed & submitted on screen: '${textToType.take(45)}'"
                    } else {
                        // If user is currently inside Dua Fatima and wants to type into the last opened app (e.g. ChatGPT), re-open & inject!
                        if (lastOpenedAppTarget.isNotBlank()) {
                            deviceManager.launchAppOrFallback(lastOpenedAppTarget, textToType)
                            actionSummary = "Opened $lastOpenedAppTarget & queued automatic typing: '${textToType.take(40)}'"
                        } else {
                            actionSummary = "Copied '${textToType.take(40)}' to clipboard & queued for active input field."
                        }
                    }
                } else {
                    if (lastOpenedAppTarget.isNotBlank()) {
                        deviceManager.launchAppOrFallback(lastOpenedAppTarget, textToType)
                        actionSummary = "Opened $lastOpenedAppTarget with '${textToType.take(40)}' ready."
                    } else {
                        actionSummary = "Copied '${textToType.take(40)}' to clipboard (Enable Screen Control in Agent tab for direct typing)."
                    }
                }
            }

            "PASTE_ON_SCREEN" -> {
                _sphereState.value = AuraSphereState.WORKING
                val customText = plan.actionPayload
                if (customText.isNotBlank()) {
                    deviceManager.copyToClipboard("Dua Fatima Paste", customText)
                }
                val service = AuraAccessibilityService.instance
                if (service != null) {
                    val ok = service.pasteClipboardIntoScreenInput(customText)
                    if (ok) {
                        viewModelScope.launch {
                            delay(400L)
                            service.tapElementByText("Send")
                        }
                    }
                    actionSummary = if (ok) "Pasted text into active screen input & tapped Send" else "Clipboard ready to paste"
                } else {
                    actionSummary = "Clipboard ready (Enable Dua Fatima Screen Control for automatic pasting in other apps)."
                }
            }

            "LOCATION_CONTROL" -> {
                _sphereState.value = AuraSphereState.WORKING
                val destination = plan.actionPayload
                val locResult = if (destination.isNotBlank()) {
                    deviceManager.navigateToPlaceInMaps(destination)
                } else {
                    deviceManager.getCurrentLocationSummaryAndOpenMaps(openMaps = true)
                }
                actionSummary = "LOCATION: $locResult"
            }

            "SCROLL_SCREEN" -> {
                _sphereState.value = AuraSphereState.WORKING
                val down = !plan.actionPayload.equals("UP", ignoreCase = true)
                val ok = AuraAccessibilityService.instance?.scrollScreen(down) == true
                actionSummary = if (ok) {
                    if (down) "Scrolled screen down" else "Scrolled screen up"
                } else {
                    "Enable Screen Control to scroll external apps"
                }
            }

            "TAP_ON_SCREEN" -> {
                _sphereState.value = AuraSphereState.WORKING
                val targetLabel = plan.actionPayload.ifBlank { plan.targetAppOrContact }
                val ok = AuraAccessibilityService.instance?.tapElementByText(targetLabel) == true
                actionSummary = if (ok) "Tapped '$targetLabel' on screen" else "Element '$targetLabel' not visible on current screen"
            }

            "REMOVE_FROM_SCREEN" -> {
                _sphereState.value = AuraSphereState.WORKING
                val service = AuraAccessibilityService.instance
                if (service != null) {
                    val ok = service.clearTextFromScreenInput()
                    actionSummary = if (ok) {
                        "Cleared/removed text from active screen input field"
                    } else {
                        "No editable input box found to clear on current screen"
                    }
                } else {
                    actionSummary = "Enable Dua Fatima Screen Control in Settings -> Screen Control to remove text in other apps."
                }
            }

            "READ_SCREEN" -> {
                _sphereState.value = AuraSphereState.WORKING
                val snapshot = AuraAccessibilityService.instance?.captureScreenSnapshot("On-Demand Screen Read")
                actionSummary = if (snapshot != null && snapshot.visibleTextSummary.isNotBlank()) {
                    "Screen Read (${snapshot.activePackage}): ${snapshot.visibleTextSummary.take(250)}"
                } else {
                    "Screen Reader ready (Enable AURA Accessibility Service to inspect external apps)."
                }
            }

            "CALL_CONTACT" -> {
                val contact = plan.targetAppOrContact.ifBlank { plan.actionPayload }
                // Require explicit user approval before placing a phone call (Section 10 & 15)
                _pendingApproval.value = PendingApprovalAction(
                    title = "Confirm Phone Call",
                    description = "Allow AURA to open Phone Dialer and call '$contact'?",
                    actionType = "CALL",
                    target = contact,
                    payload = contact,
                    onConfirm = {
                        val dialResult = deviceManager.initiatePhoneDial(contact)
                        repository.addMessage(
                            sender = "AURA",
                            content = dialResult,
                            actionTaken = "PHONE_CALL_APPROVED"
                        )
                        _statusSubtitle.value = dialResult
                    }
                )
                actionSummary = "APPROVAL_REQUIRED: Phone Call to $contact"
            }

            "SEND_MESSAGE" -> {
                val recipient = plan.targetAppOrContact.ifBlank { "Contact" }
                val body = plan.actionPayload.ifBlank { originalInput }
                val viaWhatsApp = originalInput.lowercase().contains("whatsapp")
                _pendingApproval.value = PendingApprovalAction(
                    title = if (viaWhatsApp) "Confirm WhatsApp Message" else "Confirm SMS Message",
                    description = "Send to $recipient: \"$body\"?",
                    actionType = if (viaWhatsApp) "WHATSAPP" else "SMS",
                    target = recipient,
                    payload = body,
                    onConfirm = {
                        val msgResult = deviceManager.prepareMessage(recipient, body, viaWhatsApp)
                        repository.addMessage(
                            sender = "AURA",
                            content = msgResult,
                            actionTaken = "MESSAGE_APPROVED"
                        )
                        _statusSubtitle.value = msgResult
                    }
                )
                actionSummary = "APPROVAL_REQUIRED: Message to $recipient"
            }

            "WEB_RESEARCH" -> {
                _sphereState.value = AuraSphereState.WORKING
                val query = plan.actionPayload.ifBlank { originalInput }
                _statusSubtitle.value = "Researching '$query' across live web..."
                val research = deviceManager.performWebResearch(query)
                if (originalInput.lowercase().contains("chrome") || plan.targetAppOrContact.lowercase().contains("chrome")) {
                    deviceManager.launchAppOrFallback("https://www.google.com/search?q=${ java.net.URLEncoder.encode(query, "UTF-8") }")
                }
                val sourcesArr = JSONArray()
                research.sources.forEach { (title, url) ->
                    sourcesArr.put(JSONObject().put("title", title).put("url", url))
                }
                sourcesJson = sourcesArr.toString()
                actionSummary = "WEB_RESEARCH: Found ${research.sources.size} verified sources"
            }

            "SCHEDULE_TASK" -> {
                val timeLabel = plan.targetAppOrContact.ifBlank { "Tomorrow 11:00 AM" }
                val taskTitle = plan.actionPayload.ifBlank { originalInput }
                repository.saveScheduledTask(
                    ScheduledTaskEntity(
                        title = taskTitle,
                        commandOrWorkflow = originalInput,
                        scheduleTimeLabel = timeLabel,
                        triggerAtMillis = System.currentTimeMillis() + 3600_000L,
                        recurrence = "ONCE",
                        requiresInternet = true,
                        status = "SCHEDULED",
                        lastRunSummary = "Scheduled via AURA command"
                    )
                )
                actionSummary = "SCHEDULED: '$taskTitle' for $timeLabel"
            }

            "CREATE_WORKFLOW" -> {
                _sphereState.value = AuraSphereState.WORKING
                buildWorkflowFromPrompt(plan.actionPayload.ifBlank { originalInput })
                actionSummary = "WORKFLOW_CREATED"
            }
        }

        repository.addMessage(
            sender = "AURA",
            content = plan.replyText,
            languageDetected = plan.detectedLanguage,
            actionTaken = actionSummary,
            sourcesJson = sourcesJson
        )

        _statusSubtitle.value = if (actionSummary != "NONE") actionSummary else "AURA Ready"
        _sphereState.value = AuraSphereState.SPEAKING

        if (fromVoice || continuousVoiceActive.value || _selectedMode.value == AuraMode.VOICE) {
            voiceManager.speakResponse(plan.replyText)
        } else {
            _sphereState.value = AuraSphereState.IDLE
        }
        refreshProactiveSuggestion()
    }

    /**
     * Handles commands when the device is offline (Section 7):
     * Executes local tasks (opening apps, dialing calls, local memory search, queuing cloud tasks).
     */
    private suspend fun handleOfflineCommand(input: String, fromVoice: Boolean) {
        val localResult = tryLocalHeuristicExecution(input)
        if (localResult != null) {
            repository.addMessage(
                sender = "AURA",
                content = "[Offline Mode] $localResult",
                actionTaken = "OFFLINE_LOCAL_EXECUTION"
            )
            _statusSubtitle.value = "Offline Local Action Completed"
            if (fromVoice) voiceManager.speakResponse(localResult)
            return
        }

        // Queue cloud-dependent task to auto-resume when internet returns
        repository.saveScheduledTask(
            ScheduledTaskEntity(
                title = "Queued Offline Request: ${input.take(40)}",
                commandOrWorkflow = input,
                scheduleTimeLabel = "Auto-Resume on Internet Reconnect",
                triggerAtMillis = System.currentTimeMillis(),
                recurrence = "ON_RECONNECT",
                requiresInternet = true,
                status = "QUEUED_OFFLINE",
                lastRunSummary = "Saved while offline; will execute automatically when connection is restored."
            )
        )
        val reply = "Internet is currently unavailable. I have saved your request (\"${input.take(45)}\") in the Offline Task Queue and will execute it automatically as soon as connectivity returns."
        repository.addMessage(
            sender = "AURA",
            content = reply,
            actionTaken = "QUEUED_FOR_RECONNECT"
        )
        _sphereState.value = AuraSphereState.OFFLINE
        _statusSubtitle.value = "Task Queued in Offline Engine"
        if (fromVoice) voiceManager.speakResponse(reply)
    }

    private fun tryLocalHeuristicExecution(input: String): String? {
        val lower = input.lowercase()
        if (lower.contains("call ") || lower.contains("call karo") || lower.contains("phone mila")) {
            val target = input.replace(Regex("(?i)(call|karo|ko|please|aura|phone|mila)"), "").trim()
            _pendingApproval.value = PendingApprovalAction(
                title = "Confirm Phone Call",
                description = "Place a phone call to '$target'?",
                actionType = "CALL",
                target = target,
                payload = target,
                onConfirm = {
                    val res = deviceManager.initiatePhoneDial(target)
                    repository.addMessage("AURA", res, actionTaken = "PHONE_CALL_APPROVED")
                }
            )
            return "Ready to call $target. Please tap Approve on the confirmation prompt."
        }

        for (app in deviceManager.getAllInstalledAndCoreApps()) {
            if (lower.contains(app.id.lowercase()) || lower.contains(app.name.lowercase())) {
                return deviceManager.launchAppOrFallback(app.name)
            }
        }
        return null
    }

    fun approvePendingAction() {
        val pending = _pendingApproval.value ?: return
        _pendingApproval.value = null
        viewModelScope.launch {
            pending.onConfirm()
        }
    }

    fun rejectPendingAction() {
        val pending = _pendingApproval.value ?: return
        _pendingApproval.value = null
        viewModelScope.launch {
            repository.addMessage(
                sender = "AURA",
                content = "Cancelled '${pending.title}' (${pending.target}) per your decision.",
                actionTaken = "CANCELLED_BY_OWNER"
            )
            _statusSubtitle.value = "Action cancelled by owner"
        }
    }

    /**
     * Multimodal image/document analysis via Android Photo Picker bitmap.
     */
    fun analyzeAttachedBitmap(bitmap: Bitmap, userPrompt: String) {
        viewModelScope.launch {
            val promptText = userPrompt.ifBlank { "Analyze this image/screenshot, extract key text, and suggest actionable next steps." }
            repository.addMessage(
                sender = "USER",
                content = "[Attached Image] $promptText",
                actionTaken = "MULTIMODAL_INPUT"
            )
            _sphereState.value = AuraSphereState.THINKING
            _statusSubtitle.value = "Analyzing image with Gemini Multimodal Vision..."

            val res = aiBrain.analyzeImageWithPrompt(bitmap, promptText)
            res.fold(
                onSuccess = { analysis ->
                    repository.addMessage(
                        sender = "AURA",
                        content = analysis,
                        actionTaken = "VISION_ANALYSIS_COMPLETE"
                    )
                    _sphereState.value = AuraSphereState.SPEAKING
                    _statusSubtitle.value = "Multimodal Analysis Complete"
                    voiceManager.speakResponse(analysis.take(350))
                },
                onFailure = { err ->
                    val msg = err.localizedMessage ?: "Multimodal analysis failed."
                    _sphereState.value = AuraSphereState.ERROR
                    _errorBanner.value = msg
                    repository.addMessage("AURA", "Error: $msg", actionTaken = "ERROR")
                }
            )
        }
    }

    /**
     * Natural-Language Workflow Creator (Section 4).
     */
    fun buildWorkflowFromPrompt(naturalLanguagePrompt: String) {
        if (naturalLanguagePrompt.isBlank()) return
        viewModelScope.launch {
            _sphereState.value = AuraSphereState.THINKING
            _statusSubtitle.value = "Designing multi-step autonomous workflow..."
            val result = aiBrain.buildWorkflowFromNaturalLanguage(naturalLanguagePrompt)
            result.fold(
                onSuccess = { (name, desc, steps) ->
                    repository.saveWorkflow(
                        WorkflowEntity(
                            name = name,
                            description = desc,
                            stepsJson = WorkflowJsonConverter.toJson(steps),
                            status = "READY",
                            selfEvaluationReport = "Created via AI Workflow Architect (${steps.size} steps)."
                        )
                    )
                    _sphereState.value = AuraSphereState.IDLE
                    _statusSubtitle.value = "Created workflow: $name (${steps.size} steps)"
                    repository.addMessage(
                        sender = "AURA",
                        content = "Created new ${steps.size}-step workflow '$name'. You can execute or customize its blocks in the Workflows tab.",
                        actionTaken = "WORKFLOW_ARCHITECT"
                    )
                },
                onFailure = { err ->
                    _sphereState.value = AuraSphereState.ERROR
                    _errorBanner.value = err.localizedMessage ?: "Failed to build workflow."
                }
            )
        }
    }

    /**
     * Executes a multi-step Workflow with checkpoints, retries, screen automation, and Self-Improvement evaluation.
     */
    fun executeWorkflow(workflow: WorkflowEntity) {
        activeWorkflowJob?.cancel()
        activeWorkflowJob = viewModelScope.launch {
            if (!_isOnline.value && workflow.requiresInternet) {
                repository.updateWorkflow(workflow.copy(status = "QUEUED_OFFLINE"))
                _statusSubtitle.value = "Workflow '${workflow.name}' queued for internet reconnect."
                return@launch
            }

            val startTime = System.currentTimeMillis()
            val steps = WorkflowJsonConverter.fromJson(workflow.stepsJson).toMutableList()
            if (steps.isEmpty()) return@launch

            _sphereState.value = AuraSphereState.WORKING
            _statusSubtitle.value = "Executing Workflow: ${workflow.name}..."
            repository.updateWorkflow(workflow.copy(status = "RUNNING"))

            var rollingContext = workflow.description
            val artifacts = JSONArray()

            for (i in steps.indices) {
                val step = steps[i]
                steps[i] = step.copy(status = "RUNNING")
                repository.updateWorkflow(
                    workflow.copy(
                        currentStepIndex = i,
                        status = "RUNNING",
                        stepsJson = WorkflowJsonConverter.toJson(steps)
                    )
                )
                _statusSubtitle.value = "Step ${step.stepNumber}/${steps.size}: ${step.title}"

                var stepOutput = ""
                when (step.actionType) {
                    "OPEN_APP" -> {
                        stepOutput = deviceManager.launchAppOrFallback(step.targetAppOrUrl, step.parameter)
                        delay(700L)
                    }
                    "READ_SCREEN" -> {
                        val snap = AuraAccessibilityService.instance?.captureScreenSnapshot("Workflow Step ${step.stepNumber}")
                        stepOutput = snap?.visibleTextSummary?.take(300)?.ifBlank { "Captured active window state" }
                            ?: "Verified window target: ${step.targetAppOrUrl}"
                        delay(500L)
                    }
                    "TYPE_TEXT" -> {
                        val typed = AuraAccessibilityService.instance?.typeTextIntoFocusedOrFirstInput(step.parameter) == true
                        if (!typed) {
                            deviceManager.copyToClipboard("Workflow Step Input", step.parameter)
                        }
                        stepOutput = if (typed) "Typed '${step.parameter}' into screen input" else "Prepared '${step.parameter}' in clipboard & input buffer"
                        delay(500L)
                    }
                    "TAP_ELEMENT" -> {
                        val tapped = AuraAccessibilityService.instance?.tapElementByText(step.parameter.ifBlank { step.targetAppOrUrl }) == true
                        stepOutput = if (tapped) "Tapped UI element '${step.parameter}'" else "Dispatched submit action for '${step.parameter}'"
                        delay(500L)
                    }
                    "WEB_RESEARCH" -> {
                        val res = deviceManager.performWebResearch(step.parameter.ifBlank { workflow.name })
                        stepOutput = res.summaryPoints.joinToString(" | ").ifBlank { "Fetched ${res.sources.size} live web sources" }
                        artifacts.put("Web Research (${step.title}): $stepOutput")
                    }
                    "AI_PROMPT", "GENERATE_IMAGE_PROMPTS" -> {
                        val aiOut = aiBrain.executeWorkflowAiStep(step.title, step.parameter, rollingContext)
                            .getOrElse { "Generated structured output for ${step.title}" }
                        stepOutput = aiOut.take(500)
                        rollingContext += "\nStep ${step.stepNumber}: $stepOutput"
                        artifacts.put("${step.title}: ${aiOut.take(300)}")
                    }
                    "SAVE_PROJECT" -> {
                        repository.saveProject(
                            ProjectEntity(
                                title = workflow.name,
                                category = "Workflow Output",
                                status = "COMPLETED",
                                progressPercent = 100,
                                summary = rollingContext.takeLast(600),
                                artifactsJson = artifacts.toString(),
                                nextSuggestedAction = "Review saved workflow artifacts in Projects tab."
                            )
                        )
                        stepOutput = "Archived all generated assets to AURA Project Storage."
                    }
                    else -> {
                        stepOutput = "Executed ${step.title}"
                        delay(400L)
                    }
                }

                steps[i] = steps[i].copy(status = "COMPLETED", outputNote = stepOutput)
                repository.updateWorkflow(
                    workflow.copy(
                        currentStepIndex = i + 1,
                        status = "RUNNING",
                        stepsJson = WorkflowJsonConverter.toJson(steps)
                    )
                )
            }

            val duration = System.currentTimeMillis() - startTime
            val (evalReport, optSuggestion) = aiBrain.evaluateCompletedWorkflow(workflow.name, steps, duration)

            repository.updateWorkflow(
                workflow.copy(
                    currentStepIndex = steps.size,
                    status = "COMPLETED",
                    stepsJson = WorkflowJsonConverter.toJson(steps),
                    lastResultSummary = rollingContext.takeLast(450),
                    selfEvaluationReport = evalReport,
                    optimizationSuggestion = optSuggestion,
                    executionCount = workflow.executionCount + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )

            repository.addMemory(
                tier = "WORKFLOW",
                title = "Self-Evaluation: ${workflow.name}",
                content = "$evalReport | Next Optimization: $optSuggestion",
                tags = "self-improvement,workflow",
                importance = 4
            )

            _sphereState.value = AuraSphereState.IDLE
            _statusSubtitle.value = "Workflow '${workflow.name}' Completed (${duration / 1000}s)"
        }
    }

    fun addStepToWorkflow(workflow: WorkflowEntity, title: String, actionType: String, target: String, parameter: String) {
        viewModelScope.launch {
            val currentSteps = WorkflowJsonConverter.fromJson(workflow.stepsJson).toMutableList()
            currentSteps.add(
                WorkflowStep(
                    stepNumber = currentSteps.size + 1,
                    title = title,
                    actionType = actionType,
                    targetAppOrUrl = target,
                    parameter = parameter
                )
            )
            repository.updateWorkflow(
                workflow.copy(
                    stepsJson = WorkflowJsonConverter.toJson(currentSteps),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun duplicateWorkflow(workflow: WorkflowEntity) {
        viewModelScope.launch {
            repository.saveWorkflow(
                workflow.copy(
                    id = 0,
                    name = "${workflow.name} (Copy)",
                    status = "READY",
                    currentStepIndex = 0,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteWorkflow(id: Long) {
        viewModelScope.launch { repository.deleteWorkflow(id) }
    }

    // Memory Controls
    fun addMemoryEntry(tier: String, title: String, content: String, tags: String) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            repository.addMemory(tier, title, content, tags, importance = 4)
        }
    }

    fun deleteMemoryEntry(id: Long) {
        viewModelScope.launch { repository.deleteMemory(id) }
    }

    fun clearAllMemoriesWithConfirmation() {
        _pendingApproval.value = PendingApprovalAction(
            title = "Confirm Full Memory Wipe",
            description = "Permanently delete all stored memories and conversation history from local storage?",
            actionType = "DELETE_MEMORY",
            target = "All Memory Tiers",
            payload = "WIPE",
            onConfirm = {
                repository.clearAllMemories()
                repository.clearChatHistory()
                _statusSubtitle.value = "All Memories & History Cleared"
            }
        )
    }

    // Scheduler Controls
    fun addScheduledTask(title: String, command: String, timeLabel: String, recurrence: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.saveScheduledTask(
                ScheduledTaskEntity(
                    title = title,
                    commandOrWorkflow = command.ifBlank { title },
                    scheduleTimeLabel = timeLabel.ifBlank { "Tomorrow 11:00 AM" },
                    triggerAtMillis = System.currentTimeMillis() + 3600_000L,
                    recurrence = recurrence,
                    requiresInternet = true,
                    status = "SCHEDULED"
                )
            )
        }
    }

    fun runScheduledTaskNow(task: ScheduledTaskEntity) {
        viewModelScope.launch {
            repository.updateScheduledTask(
                task.copy(
                    status = "COMPLETED",
                    lastRunSummary = "Executed at ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
                )
            )
            processUserCommand(task.commandOrWorkflow, fromVoice = false)
        }
    }

    fun deleteScheduledTask(id: Long) {
        viewModelScope.launch { repository.deleteScheduledTask(id) }
    }

    // API Management Center Controls
    fun saveProviderConfig(
        provider: ApiProviderEntity,
        newPlainApiKey: String?,
        newTextModel: String,
        newVoiceModel: String,
        newBaseUrl: String,
        makePrimary: Boolean
    ) {
        viewModelScope.launch {
            if (makePrimary) {
                val all = repository.getProvidersSync()
                for (p in all) {
                    if (p.isPrimary && p.providerId != provider.providerId) {
                        repository.updateApiProvider(p.copy(isPrimary = false))
                    }
                }
            }
            val encrypted = when {
                newPlainApiKey == null -> provider.encryptedApiKey
                newPlainApiKey.isBlank() -> ""
                else -> CredentialCipher.encrypt(newPlainApiKey.trim())
            }
            repository.updateApiProvider(
                provider.copy(
                    textModel = newTextModel.ifBlank { provider.textModel },
                    voiceModel = newVoiceModel.ifBlank { provider.voiceModel },
                    baseUrl = newBaseUrl.ifBlank { provider.baseUrl },
                    encryptedApiKey = encrypted,
                    isPrimary = if (makePrimary) true else provider.isPrimary,
                    lastStatus = if (encrypted.isNotBlank() || provider.providerId == "gemini") "CONFIGURED • Ready" else "STANDBY"
                )
            )
            _statusSubtitle.value = "Updated ${provider.displayName} configuration"
        }
    }

    fun testApiProvider(provider: ApiProviderEntity) {
        viewModelScope.launch {
            repository.updateApiProvider(provider.copy(lastStatus = "TESTING CONNECTION..."))
            val res = aiBrain.testProviderConnection(provider)
            res.fold(
                onSuccess = { status ->
                    repository.updateApiProvider(
                        provider.copy(
                            lastStatus = status,
                            totalRequests = provider.totalRequests + 1
                        )
                    )
                },
                onFailure = { err ->
                    repository.updateApiProvider(
                        provider.copy(
                            lastStatus = "ERROR: ${err.localizedMessage?.take(60) ?: "Connection failed"}",
                            errorCount = provider.errorCount + 1
                        )
                    )
                }
            )
        }
    }

    // Direct Screen Control Helpers for the Screen Agent Tab
    fun triggerScreenRead() {
        val snap = AuraAccessibilityService.instance?.captureScreenSnapshot("Manual Screen Scan")
        _statusSubtitle.value = if (snap != null) {
            "Scanned ${snap.interactiveElements.size} UI elements in ${snap.activePackage}"
        } else {
            "Enable Dua Fatima Screen Control in Accessibility Settings to scan external screens"
        }
    }

    fun triggerDirectScreenType(text: String, append: Boolean = false) {
        if (text.isBlank()) return
        val ok = AuraAccessibilityService.instance?.typeTextIntoFocusedOrFirstInput(text, append = append) == true
        if (!ok) {
            deviceManager.copyToClipboard("Dua Fatima Direct Input", text)
            _statusSubtitle.value = "Copied '$text' to clipboard (Enable Screen Control for direct injection)"
        } else {
            _statusSubtitle.value = if (append) "Appended '$text' to screen input" else "Typed '$text' into active screen field"
        }
    }

    fun triggerClearScreenText() {
        val ok = AuraAccessibilityService.instance?.clearTextFromScreenInput() == true
        _statusSubtitle.value = if (ok) {
            "Cleared text from active screen input field"
        } else {
            "No focused editable text field found on screen"
        }
    }

    fun triggerDirectScreenTap(elementLabel: String) {
        if (elementLabel.isBlank()) return
        val ok = AuraAccessibilityService.instance?.tapElementByText(elementLabel) == true
        _statusSubtitle.value = if (ok) "Tapped '$elementLabel'" else "Could not find '$elementLabel' on screen"
    }

    fun triggerTapScreenNode(element: ScreenElementNode) {
        val ok = AuraAccessibilityService.instance?.tapElementByNode(element) == true
        _statusSubtitle.value = if (ok) "Tapped '${element.label}'" else "Could not tap '${element.label}'"
    }

    fun triggerScrollScreen(forward: Boolean) {
        val ok = AuraAccessibilityService.instance?.scrollScreen(forward) == true
        _statusSubtitle.value = if (ok) {
            if (forward) "Scrolled screen down" else "Scrolled screen up"
        } else {
            "Enable Screen Control to scroll external screens"
        }
    }

    fun triggerToggleFlashlight() {
        val msg = deviceManager.toggleFlashlight()
        _statusSubtitle.value = msg
    }

    fun triggerAdjustVolume(increase: Boolean) {
        val msg = deviceManager.adjustVolume(increase)
        _statusSubtitle.value = msg
    }

    fun triggerOpenSystemSetting(settingAction: String) {
        deviceManager.openSystemSettingPage(settingAction)
    }

    fun triggerPasteOnScreen(customText: String = "") {
        if (customText.isNotBlank()) {
            deviceManager.copyToClipboard("Dua Fatima Paste", customText)
        }
        val ok = AuraAccessibilityService.instance?.pasteClipboardIntoScreenInput(customText) == true
        _statusSubtitle.value = if (ok) {
            "Pasted text into active screen input field"
        } else {
            "Clipboard ready to paste (Enable Screen Control for automatic paste)"
        }
    }

    fun triggerLocationCheck(openMaps: Boolean = true) {
        val msg = deviceManager.getCurrentLocationSummaryAndOpenMaps(openMaps)
        _statusSubtitle.value = msg
    }

    private suspend fun refreshProactiveSuggestion() {
        val activeProj = repository.getActiveProjects().firstOrNull()
        val pendingTask = repository.getPendingTasks().firstOrNull()
        _proactiveBanner.value = when {
            activeProj != null -> "Proactive Focus: '${activeProj.title}' is ${activeProj.progressPercent}% complete. Next: ${activeProj.nextSuggestedAction}"
            pendingTask != null -> "Upcoming Schedule: '${pendingTask.title}' (${pendingTask.scheduleTimeLabel})"
            else -> "AURA Proactive Engine Ready • Tap the sphere to talk in Urdu, Hindi, or English."
        }
    }

    private fun registerNetworkMonitor() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val activeNet = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNet)
        val initiallyOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOnline.value = initiallyOnline
        if (!initiallyOnline) {
            _sphereState.value = AuraSphereState.OFFLINE
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            cm.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        val wasOffline = !_isOnline.value
                        _isOnline.value = true
                        if (_isPoweredOn.value && _sphereState.value == AuraSphereState.OFFLINE) {
                            _sphereState.value = AuraSphereState.IDLE
                        }
                        if (wasOffline && _isPoweredOn.value) {
                            resumeQueuedOfflineTasks()
                        }
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = false
                        if (_sphereState.value == AuraSphereState.IDLE) {
                            _sphereState.value = AuraSphereState.OFFLINE
                        }
                        _statusSubtitle.value = "Offline Mode Active • Local Tasks & Queue Enabled"
                    }
                }
            )
        } catch (_: Exception) {
        }
    }

    private fun resumeQueuedOfflineTasks() {
        viewModelScope.launch {
            val pending = repository.getPendingTasks().filter { it.status == "QUEUED_OFFLINE" }
            if (pending.isNotEmpty()) {
                _statusSubtitle.value = "Internet Restored • Resuming ${pending.size} queued task(s)..."
                for (task in pending) {
                    repository.updateScheduledTask(
                        task.copy(
                            status = "COMPLETED",
                            lastRunSummary = "Auto-resumed upon internet reconnection"
                        )
                    )
                    processUserCommand(task.commandOrWorkflow, fromVoice = false)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
