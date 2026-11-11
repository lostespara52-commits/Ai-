package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuraSphereState {
    IDLE,       // Subtle ambient cyan/blue core
    LISTENING,  // Animated blue sphere
    THINKING,   // Rotating purple sphere
    SPEAKING,   // Pulsing cyan sphere
    WORKING,    // Dynamic multicolor sphere
    OFFLINE,    // Subtle gray sphere
    ERROR       // Red notification state
}

enum class AuraMode(val label: String) {
    HOME("Home"),
    CHAT("Chat"),
    VOICE("Live Orb"),
    HISTORY("History"),
    SETTINGS("Settings"),
    QUICK_ACTIONS("Quick Actions"),
    AGENT("Screen Control"),
    WORKFLOW("Workflows"),
    MEMORY("Memory"),
    SCHEDULER("Scheduler"),
    PROJECTS("Projects"),
    API_CENTER("Connections")
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER", "AURA", "SYSTEM"
    val content: String,
    val languageDetected: String = "EN/UR/HI",
    val actionTaken: String = "",
    val sourcesJson: String = "",
    val projectId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tier: String, // "SHORT_TERM", "LONG_TERM", "PROJECT", "WORKFLOW", "PREFERENCE"
    val title: String,
    val content: String,
    val tags: String = "",
    val importance: Int = 3, // 1..5
    val isUserApproved: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val stepsJson: String, // JSON array of WorkflowStep
    val currentStepIndex: Int = 0,
    val status: String = "READY", // "READY", "RUNNING", "PAUSED_APPROVAL", "COMPLETED", "FAILED", "QUEUED_OFFLINE"
    val lastResultSummary: String = "",
    val selfEvaluationReport: String = "",
    val optimizationSuggestion: String = "",
    val executionCount: Int = 0,
    val requiresInternet: Boolean = true,
    val requiresApprovalForSensitive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class WorkflowStep(
    val stepNumber: Int,
    val title: String,
    val actionType: String, // OPEN_APP, TYPE_TEXT, TAP_ELEMENT, READ_SCREEN, AI_PROMPT, WEB_RESEARCH,GENERATE_IMAGE_PROMPTS, APPROVAL_GATE, SAVE_PROJECT
    val targetAppOrUrl: String = "",
    val parameter: String = "",
    val requiresApproval: Boolean = false,
    val status: String = "PENDING", // PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
    val outputNote: String = ""
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "AI Story Video", "YouTube Research", "App Automation", "General"
    val status: String, // "ACTIVE", "IN_PROGRESS", "COMPLETED"
    val progressPercent: Int = 0,
    val summary: String,
    val artifactsJson: String = "[]", // Scenes, prompts, stories, research notes
    val nextSuggestedAction: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scheduled_tasks")
data class ScheduledTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val commandOrWorkflow: String,
    val scheduleTimeLabel: String, // e.g., "Tomorrow 11:00 AM", "Daily 09:00 AM"
    val triggerAtMillis: Long,
    val recurrence: String = "ONCE", // "ONCE", "DAILY", "WEEKLY", "ON_RECONNECT"
    val requiresInternet: Boolean = true,
    val status: String = "SCHEDULED", // "SCHEDULED", "QUEUED_OFFLINE", "COMPLETED", "RUNNING"
    val lastRunSummary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_providers")
data class ApiProviderEntity(
    @PrimaryKey val providerId: String, // "gemini", "openai", "anthropic", "groq", "custom"
    val displayName: String,
    val textModel: String,
    val voiceModel: String,
    val baseUrl: String,
    val encryptedApiKey: String = "",
    val isPrimary: Boolean = false,
    val isBackup: Boolean = false,
    val isEnabled: Boolean = true,
    val lastStatus: String = "READY",
    val totalRequests: Int = 0,
    val errorCount: Int = 0
)
