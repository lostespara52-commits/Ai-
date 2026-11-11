package com.example.data

import kotlinx.coroutines.flow.Flow

class AuraRepository(private val dao: AuraDao) {
    val allMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessages()
    val allMemories: Flow<List<MemoryEntryEntity>> = dao.getAllMemories()
    val allWorkflows: Flow<List<WorkflowEntity>> = dao.getAllWorkflows()
    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()
    val allScheduledTasks: Flow<List<ScheduledTaskEntity>> = dao.getAllScheduledTasks()
    val allApiProviders: Flow<List<ApiProviderEntity>> = dao.getAllApiProviders()

    suspend fun ensureSeeded() {
        val existingProviders = dao.getAllApiProvidersSync()
        if (existingProviders.isEmpty()) {
            seedDefaultProviders()
            seedDefaultWorkflowsAndProjects()
            seedDefaultMemories()
        }
    }

    private suspend fun seedDefaultProviders() {
        dao.insertApiProvider(
            ApiProviderEntity(
                providerId = "gemini",
                displayName = "Google Gemini (Firebase AI Logic)",
                textModel = "gemini-3.8-flash",
                voiceModel = "gemini-3.1-flash-tts-preview",
                baseUrl = "firebase-ai-logic://google-gemini",
                encryptedApiKey = "",
                isPrimary = true,
                isBackup = false,
                isEnabled = true,
                lastStatus = "ACTIVE • App Check Verified"
            )
        )
        dao.insertApiProvider(
            ApiProviderEntity(
                providerId = "openai",
                displayName = "OpenAI",
                textModel = "gpt-4o",
                voiceModel = "gpt-4o-realtime-preview",
                baseUrl = "https://api.openai.com/v1/chat/completions",
                encryptedApiKey = "",
                isPrimary = false,
                isBackup = true,
                isEnabled = true,
                lastStatus = "STANDBY • Add Key in API Center"
            )
        )
        dao.insertApiProvider(
            ApiProviderEntity(
                providerId = "anthropic",
                displayName = "Anthropic Claude",
                textModel = "claude-3-7-sonnet-latest",
                voiceModel = "Native System Voice",
                baseUrl = "https://api.anthropic.com/v1/messages",
                encryptedApiKey = "",
                isPrimary = false,
                isBackup = false,
                isEnabled = true,
                lastStatus = "STANDBY • Add Key in API Center"
            )
        )
        dao.insertApiProvider(
            ApiProviderEntity(
                providerId = "groq",
                displayName = "Groq LPU Ultra-Fast",
                textModel = "llama-3.3-70b-versatile",
                voiceModel = "whisper-large-v3-turbo",
                baseUrl = "https://api.groq.com/openai/v1/chat/completions",
                encryptedApiKey = "",
                isPrimary = false,
                isBackup = false,
                isEnabled = true,
                lastStatus = "STANDBY • Add Key in API Center"
            )
        )
        dao.insertApiProvider(
            ApiProviderEntity(
                providerId = "custom",
                displayName = "Custom OpenAI-Compatible Endpoint",
                textModel = "custom-agent-model",
                voiceModel = "Native System Voice",
                baseUrl = "https://api.example.com/v1/chat/completions",
                encryptedApiKey = "",
                isPrimary = false,
                isBackup = false,
                isEnabled = false,
                lastStatus = "DISABLED"
            )
        )
    }

    private suspend fun seedDefaultWorkflowsAndProjects() {
        // 1. 11-Step Automatic AI Story Video Generator (from Master Prompt Section 4)
        val storyVideoSteps = listOf(
            WorkflowStep(1, "Open ChatGPT & Generate Story", "OPEN_APP", "com.openai.chatgpt", "Generate a gripping cinematic sci-fi mystery story in 5 scenes"),
            WorkflowStep(2, "Collect Complete Story Text", "READ_SCREEN", "com.openai.chatgpt", "Extract generated story text into workflow buffer"),
            WorkflowStep(3, "Analyze Story & Divide into Cinematic Scenes", "AI_PROMPT", "gemini", "Divide story into 5 structured cinematic scenes with camera angles and narration"),
            WorkflowStep(4, "Generate Detailed Image Prompts per Scene", "GENERATE_IMAGE_PROMPTS", "gemini", "Create hyper-detailed 16:9 visual prompts for every scene"),
            WorkflowStep(5, "Open Google Flow / Image Studio", "OPEN_APP", "https://labs.google/fx/tools/flow", "Launch compatible AI image & cinema studio"),
            WorkflowStep(6, "Generate Scene Visual Assets", "AI_PROMPT", "gemini-nano-banana-2.1", "Render keyframe visual concepts for scenes"),
            WorkflowStep(7, "Collect Generated Image Assets", "SAVE_PROJECT", "AURA Vault", "Index generated scene keyframes into Project Storage"),
            WorkflowStep(8, "Generate Video Animation Prompts", "AI_PROMPT", "gemini", "Create camera motion, lighting & pacing prompts for video generation"),
            WorkflowStep(9, "Open Video Generation Service", "OPEN_APP", "https://labs.google/fx/tools/video-fx", "Open video synthesis workspace"),
            WorkflowStep(10, "Synthesize & Organize Video Clips", "READ_SCREEN", "Video Workspace", "Verify clip sequence and organize scene timeline"),
            WorkflowStep(11, "Save Complete Project Package", "SAVE_PROJECT", "Automatic AI Story Video", "Store all scripts, prompts, and scene assets in AURA Project Memory")
        )
        dao.insertWorkflow(
            WorkflowEntity(
                name = "Automatic AI Story Video Generator",
                description = "11-step autonomous pipeline: Story generation -> Scene breakdown -> Image prompts -> Google Flow visual synthesis -> Video animation prompts -> Project vault.",
                stepsJson = WorkflowJsonConverter.toJson(storyVideoSteps),
                status = "READY",
                selfEvaluationReport = "Optimized: Steps 3, 4, and 8 use structured JSON output to reduce token latency by 38%.",
                optimizationSuggestion = "Parallelize Scene Image Prompt & Video Motion Prompt generation."
            )
        )

        // 2. ChatGPT 10 Horror Stories Screen Automation (from Master Prompt Section 3)
        val horrorSteps = listOf(
            WorkflowStep(1, "Open ChatGPT Application", "OPEN_APP", "com.openai.chatgpt", "Launch ChatGPT Android app or web fallback"),
            WorkflowStep(2, "Wait for UI Input Field", "READ_SCREEN", "com.openai.chatgpt", "Inspect Accessibility node tree for message input box"),
            WorkflowStep(3, "Type 10 Horror Stories Prompt", "TYPE_TEXT", "Input Field", "Write 10 bone-chilling short horror stories set in abandoned places with twist endings."),
            WorkflowStep(4, "Submit Prompt & Monitor Output", "TAP_ELEMENT", "Send", "Tap Send button and wait for streaming completion"),
            WorkflowStep(5, "Read & Save Stories to AURA Memory", "SAVE_PROJECT", "10 Horror Stories Collection", "Capture screen text and archive in Project Memory")
        )
        dao.insertWorkflow(
            WorkflowEntity(
                name = "ChatGPT 10 Horror Stories Automation",
                description = "\"ChatGPT kholo aur mere liye 10 horror stories generate karne ka prompt likho\" — Opens ChatGPT, types prompt, submits, reads stories, and saves to memory.",
                stepsJson = WorkflowJsonConverter.toJson(horrorSteps),
                status = "READY",
                selfEvaluationReport = "Fallback ready: Automatically switches to Gemini 3.8 Flash + Chrome if ChatGPT app is not installed."
            )
        )

        // 3. Chrome YouTube Monetization Policy Research (from Master Prompt Section 3 & 9)
        val ytResearchSteps = listOf(
            WorkflowStep(1, "Open Chrome & Search Policies", "WEB_RESEARCH", "com.android.chrome", "Latest YouTube Partner Program monetization policy updates"),
            WorkflowStep(2, "Extract & Compare Sources", "AI_PROMPT", "gemini", "Analyze YPP subscriber thresholds, Shorts ad revenue sharing, and AI disclosure rules"),
            WorkflowStep(3, "Save Executive Summary to Memory", "SAVE_PROJECT", "YouTube Monetization Research", "Store actionable creator checklist in Long-Term Memory")
        )
        dao.insertWorkflow(
            WorkflowEntity(
                name = "Chrome YouTube Monetization Research",
                description = "\"Chrome kholo aur latest YouTube monetization policy research karo\" — Performs live web research, opens Chrome, and summarizes findings with sources.",
                stepsJson = WorkflowJsonConverter.toJson(ytResearchSteps),
                status = "READY",
                selfEvaluationReport = "High reliability: Combines direct web research extraction with Chrome browser launch."
            )
        )

        // Seed Active Projects
        dao.insertProject(
            ProjectEntity(
                title = "Automatic AI Story Video Generator",
                category = "AI Story Video",
                status = "ACTIVE",
                progressPercent = 65,
                summary = "Cinematic sci-fi thriller 'The Last Signal of Sector 9' — 5 scenes structured, image & animation prompts ready for Google Flow.",
                artifactsJson = "[\"Scene 1: Orbital Station Blackout\", \"Scene 2: Hologram Transmission\", \"Scene 3: Descent into Neon Karachi\", \"Scene 4: The Autonomous Core\", \"Scene 5: Dawn Protocol\"]",
                nextSuggestedAction = "Resume Step 5: Open Google Flow or generate scene keyframes with Gemini."
            )
        )
        dao.insertProject(
            ProjectEntity(
                title = "YouTube Creator Monetization & Trend Strategy",
                category = "YouTube Research",
                status = "IN_PROGRESS",
                progressPercent = 40,
                summary = "Tracking faceless AI documentary channels, YPP policy compliance, and Urdu/Hindi tech explainers.",
                artifactsJson = "[\"YPP 500-sub tier analysis\", \"AI synthetic content disclosure checklist\", \"Top 10 high-CPM Urdu/Hindi niches\"]",
                nextSuggestedAction = "Run scheduled 11:00 AM YouTube Research Workflow."
            )
        )

        // Seed Scheduled Task example from Section 8 ("Kal subah 11 baje mera YouTube research workflow start karna")
        val tomorrow11Am = System.currentTimeMillis() + 14 * 3600 * 1000L
        dao.insertScheduledTask(
            ScheduledTaskEntity(
                title = "Morning YouTube Research Workflow",
                commandOrWorkflow = "Chrome YouTube Monetization Research",
                scheduleTimeLabel = "Tomorrow 11:00 AM",
                triggerAtMillis = tomorrow11Am,
                recurrence = "DAILY",
                requiresInternet = true,
                status = "SCHEDULED",
                lastRunSummary = "Scheduled via voice command: 'Kal subah 11 baje mera YouTube research workflow start karna'"
            )
        )
    }

    private suspend fun seedDefaultMemories() {
        dao.insertMemory(
            MemoryEntryEntity(
                tier = "PREFERENCE",
                title = "Multilingual Voice & Language Preference",
                content = "Owner speaks fluent Urdu, Hindi, and English (including mixed Roman Urdu / Hinglish). Respond naturally in matching language without requiring typing.",
                tags = "language,urdu,hindi,english,voice",
                importance = 5
            )
        )
        dao.insertMemory(
            MemoryEntryEntity(
                tier = "PROJECT",
                title = "Active Focus: AI Story Videos & Mobile Automation",
                content = "Owner builds automated AI story videos (ChatGPT + Google Flow + Video synthesis) and uses AURA for hands-free Android app control, calls, and research.",
                tags = "project,video,automation,chatgpt,flow",
                importance = 5
            )
        )
        dao.insertMemory(
            MemoryEntryEntity(
                tier = "WORKFLOW",
                title = "Safety & Approval Rule",
                content = "Always require explicit user confirmation before initiating phone calls, sending SMS/WhatsApp messages, or deleting files.",
                tags = "security,approval,calls,messages",
                importance = 5
            )
        )
        dao.insertMemory(
            MemoryEntryEntity(
                tier = "LONG_TERM",
                title = "Frequently Used Android Apps",
                content = "Chrome, ChatGPT, YouTube, Gmail, Phone Dialer, WhatsApp, Google Flow, Maps, Calendar, and Notes.",
                tags = "apps,shortcuts",
                importance = 4
            )
        )
    }

    suspend fun addMessage(
        sender: String,
        content: String,
        languageDetected: String = "EN/UR/HI",
        actionTaken: String = "",
        sourcesJson: String = "",
        projectId: Long? = null
    ): Long {
        return dao.insertMessage(
            ChatMessageEntity(
                sender = sender,
                content = content,
                languageDetected = languageDetected,
                actionTaken = actionTaken,
                sourcesJson = sourcesJson,
                projectId = projectId
            )
        )
    }

    suspend fun getRecentMessages(limit: Int = 14): List<ChatMessageEntity> =
        dao.getRecentMessagesSync(limit).reversed()

    suspend fun getTopMemories(limit: Int = 20): List<MemoryEntryEntity> =
        dao.getTopMemoriesSync(limit)

    suspend fun getActiveProjects(): List<ProjectEntity> =
        dao.getActiveProjectsSync()

    suspend fun addMemory(
        tier: String,
        title: String,
        content: String,
        tags: String = "",
        importance: Int = 4
    ): Long {
        return dao.insertMemory(
            MemoryEntryEntity(
                tier = tier,
                title = title,
                content = content,
                tags = tags,
                importance = importance
            )
        )
    }

    suspend fun deleteMemory(id: Long) = dao.deleteMemoryById(id)
    suspend fun clearAllMemories() = dao.clearAllMemories()
    suspend fun clearChatHistory() = dao.clearAllMessages()

    suspend fun saveWorkflow(workflow: WorkflowEntity): Long = dao.insertWorkflow(workflow)
    suspend fun updateWorkflow(workflow: WorkflowEntity) = dao.updateWorkflow(workflow)
    suspend fun deleteWorkflow(id: Long) = dao.deleteWorkflowById(id)

    suspend fun saveProject(project: ProjectEntity): Long = dao.insertProject(project)
    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)
    suspend fun deleteProject(id: Long) = dao.deleteProjectById(id)

    suspend fun saveScheduledTask(task: ScheduledTaskEntity): Long = dao.insertScheduledTask(task)
    suspend fun updateScheduledTask(task: ScheduledTaskEntity) = dao.updateScheduledTask(task)
    suspend fun deleteScheduledTask(id: Long) = dao.deleteScheduledTaskById(id)
    suspend fun getPendingTasks(): List<ScheduledTaskEntity> = dao.getPendingTasksSync()

    suspend fun updateApiProvider(provider: ApiProviderEntity) = dao.updateApiProvider(provider)
    suspend fun getProvidersSync(): List<ApiProviderEntity> = dao.getAllApiProvidersSync()
}
