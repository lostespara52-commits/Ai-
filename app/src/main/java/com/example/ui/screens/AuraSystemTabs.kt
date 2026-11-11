package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ApiProviderEntity
import com.example.data.CredentialCipher
import com.example.data.MemoryEntryEntity
import com.example.data.ProjectEntity
import com.example.data.ScheduledTaskEntity
import com.example.data.WorkflowEntity
import com.example.data.WorkflowJsonConverter
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraCrimson
import com.example.ui.theme.AuraEmerald
import com.example.ui.theme.DuaCrimsonPrimary
import com.example.ui.theme.DuaRubyBright
import org.json.JSONArray

@Composable
fun AuraWorkflowBuilderScreen(
    workflows: List<WorkflowEntity>,
    onBuildFromNaturalLanguage: (String) -> Unit,
    onExecuteWorkflow: (WorkflowEntity) -> Unit,
    onAddManualStep: (WorkflowEntity, String, String, String, String) -> Unit,
    onDuplicateWorkflow: (WorkflowEntity) -> Unit,
    onDeleteWorkflow: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var nlPrompt by remember { mutableStateOf("") }
    var expandedWorkflowId by remember { mutableStateOf<Long?>(workflows.firstOrNull()?.id) }
    var newStepTitle by remember { mutableStateOf("") }
    var newStepTarget by remember { mutableStateOf("") }
    var newStepParam by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, DuaCrimsonPrimary.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Workflow Architect", tint = DuaCrimsonPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AUTONOMOUS WORKFLOW ARCHITECT",
                            style = MaterialTheme.typography.labelLarge,
                            color = DuaCrimsonPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nlPrompt,
                        onValueChange = { nlPrompt = it },
                        placeholder = {
                            Text("Describe workflow in English, Urdu, or Hindi (e.g. Open ChatGPT, write script, generate scene prompts, open Google Flow)...")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workflow_nl_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (nlPrompt.isNotBlank()) {
                                onBuildFromNaturalLanguage(nlPrompt)
                                nlPrompt = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DuaCrimsonPrimary, contentColor = Color.White),
                        modifier = Modifier.testTag("generate_workflow_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Generate", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Multi-Step Workflow", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(workflows, key = { it.id }) { workflow ->
            val steps = remember(workflow.stepsJson) { WorkflowJsonConverter.fromJson(workflow.stepsJson) }
            val isExpanded = expandedWorkflowId == workflow.id

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (workflow.status == "RUNNING") AuraEmerald else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedWorkflowId = if (isExpanded) null else workflow.id }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = workflow.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${steps.size} Steps • Status: ${workflow.status} • Runs: ${workflow.executionCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (workflow.status == "COMPLETED") AuraEmerald else DuaCrimsonPrimary
                            )
                        }
                        Row {
                            Button(
                                onClick = { onExecuteWorkflow(workflow) },
                                colors = ButtonDefaults.buttonColors(containerColor = DuaCrimsonPrimary, contentColor = Color.White),
                                modifier = Modifier.testTag("run_workflow_${workflow.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run", fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { onDuplicateWorkflow(workflow) }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate Workflow",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteWorkflow(workflow.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Workflow", tint = AuraCrimson)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = workflow.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (workflow.selfEvaluationReport.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = DuaCrimsonPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "SELF-IMPROVEMENT ENGINE EVALUATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DuaCrimsonPrimary
                                )
                                Text(
                                    text = workflow.selfEvaluationReport,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (workflow.optimizationSuggestion.isNotBlank()) {
                                    Text(
                                        text = "Next Optimization: ${workflow.optimizationSuggestion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AuraEmerald
                                    )
                                }
                            }
                        }
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        steps.forEach { step ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    when (step.status) {
                                        "COMPLETED" -> AuraEmerald.copy(alpha = 0.6f)
                                        "RUNNING" -> DuaCrimsonPrimary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "STEP ${step.stepNumber}: ${step.title}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = DuaCrimsonPrimary
                                        )
                                        Text(
                                            text = step.actionType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DuaRubyBright
                                        )
                                    }
                                    if (step.parameter.isNotBlank()) {
                                        Text(
                                            text = "Target: ${step.targetAppOrUrl} • Payload: ${step.parameter}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (step.outputNote.isNotBlank()) {
                                        Text(
                                            text = "Result: ${step.outputNote}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AuraEmerald
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newStepTitle,
                                onValueChange = { newStepTitle = it },
                                placeholder = { Text("New step title...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newStepParam,
                                onValueChange = { newStepParam = it },
                                placeholder = { Text("Prompt / Target...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (newStepTitle.isNotBlank()) {
                                        onAddManualStep(
                                            workflow,
                                            newStepTitle,
                                            "AI_PROMPT",
                                            newStepTarget.ifBlank { "gemini" },
                                            newStepParam
                                        )
                                        newStepTitle = ""
                                        newStepParam = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Step Block", tint = DuaCrimsonPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuraMemoryScreen(
    memories: List<MemoryEntryEntity>,
    onAddMemory: (String, String, String, String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onWipeAllMemory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTier by remember { mutableStateOf("ALL") }
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var newTier by remember { mutableStateOf("LONG_TERM") }

    val filtered = remember(memories, selectedTier) {
        if (selectedTier == "ALL") memories else memories.filter { it.tier == selectedTier }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "5-TIER PERSISTENT MEMORY VAULT",
                    style = MaterialTheme.typography.labelLarge,
                    color = DuaCrimsonPrimary
                )
                OutlinedButton(
                    onClick = onWipeAllMemory,
                    border = BorderStroke(1.dp, AuraCrimson),
                    modifier = Modifier.testTag("wipe_memory_button")
                ) {
                    Text("Wipe Memory", color = AuraCrimson, style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                listOf("ALL", "LONG_TERM", "PROJECT", "WORKFLOW", "PREFERENCE", "SHORT_TERM").forEach { tier ->
                    FilterChip(
                        selected = selectedTier == tier,
                        onClick = { selectedTier = tier },
                        label = { Text(tier, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("Memory Title (e.g., Owner Preferred Video Style)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        placeholder = { Text("Instruction or fact for Dua Fatima to remember permanently...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                                onAddMemory(newTier, newTitle, newContent, "manual")
                                newTitle = ""
                                newContent = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DuaCrimsonPrimary, contentColor = Color.White),
                        modifier = Modifier.testTag("save_memory_button")
                    ) {
                        Text("Store in $newTier Memory", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(filtered, key = { it.id }) { item ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "[${item.tier}] ${item.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = DuaCrimsonPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onDeleteMemory(item.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Memory", tint = AuraCrimson)
                    }
                }
            }
        }
    }
}

@Composable
fun AuraSchedulerAndOfflineScreen(
    tasks: List<ScheduledTaskEntity>,
    isOnline: Boolean,
    onAddTask: (String, String, String, String) -> Unit,
    onRunTaskNow: (ScheduledTaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var taskTitle by remember { mutableStateOf("") }
    var taskTime by remember { mutableStateOf("Tomorrow 11:00 AM") }
    var recurrence by remember { mutableStateOf("DAILY") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isOnline) AuraEmerald else AuraAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isOnline) {
                            "NETWORK ONLINE • AUTO-RESUME QUEUE READY"
                        } else {
                            "OFFLINE INTELLIGENCE MODE ACTIVE • TASKS QUEUED LOCALLY"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isOnline) AuraEmerald else AuraAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scheduled workflows run while the device is powered on. Cloud tasks queued offline automatically resume when connectivity returns.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "SMART SCHEDULER",
                        style = MaterialTheme.typography.labelLarge,
                        color = DuaCrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        placeholder = { Text("Workflow or Command (e.g. YouTube Research Workflow)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taskTime,
                            onValueChange = { taskTime = it },
                            placeholder = { Text("Time (e.g. Tomorrow 11:00 AM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (taskTitle.isNotBlank()) {
                                    onAddTask(taskTitle, taskTitle, taskTime, recurrence)
                                    taskTitle = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DuaCrimsonPrimary, contentColor = Color.White),
                            modifier = Modifier.testTag("add_schedule_button")
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = "Schedule", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Schedule")
                        }
                    }
                }
            }
        }

        items(tasks, key = { it.id }) { task ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (task.status == "QUEUED_OFFLINE") AuraAmber else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${task.scheduleTimeLabel} • ${task.recurrence} • ${task.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.status == "QUEUED_OFFLINE") AuraAmber else DuaCrimsonPrimary
                        )
                        if (task.lastRunSummary.isNotBlank()) {
                            Text(
                                text = task.lastRunSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = { onRunTaskNow(task) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run Task Now", tint = AuraEmerald)
                        }
                        IconButton(onClick = { onDeleteTask(task.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = AuraCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuraProjectsScreen(
    projects: List<ProjectEntity>,
    onRunNextStep: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Text(
                text = "ACTIVE PROJECTS & GENERATED ARTIFACTS VAULT",
                style = MaterialTheme.typography.labelLarge,
                color = DuaCrimsonPrimary
            )
        }
        items(projects, key = { it.id }) { project ->
            val artifacts = remember(project.artifactsJson) {
                try {
                    val arr = JSONArray(project.artifactsJson)
                    (0 until arr.length()).map { arr.getString(it) }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${project.progressPercent}% • ${project.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AuraEmerald
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (project.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = DuaCrimsonPrimary,
                        trackColor = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = project.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (artifacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        artifacts.forEach { artifact ->
                            Text(
                                text = "• $artifact",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DuaCrimsonPrimary,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                    if (project.nextSuggestedAction.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onRunNextStep(project.nextSuggestedAction) },
                            border = BorderStroke(1.dp, DuaCrimsonPrimary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Continue", tint = DuaCrimsonPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(project.nextSuggestedAction, color = DuaCrimsonPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuraApiCenterScreen(
    providers: List<ApiProviderEntity>,
    onSaveProvider: (ApiProviderEntity, String?, String, String, String, Boolean) -> Unit,
    onTestProvider: (ApiProviderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DuaCrimsonPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = "Encrypted Keystore", tint = DuaCrimsonPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CONNECTIONS & ENCRYPTED API VAULT",
                            style = MaterialTheme.typography.labelLarge,
                            color = DuaCrimsonPrimary
                        )
                        Text(
                            text = "Google Gemini is active out-of-the-box via Firebase AI Logic & App Check. You can also connect Groq, OpenAI, or Anthropic keys encrypted via Android Keystore AES-256-GCM.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(providers, key = { it.providerId }) { provider ->
            var textModel by remember(provider.textModel) { mutableStateOf(provider.textModel) }
            var voiceModel by remember(provider.voiceModel) { mutableStateOf(provider.voiceModel) }
            var baseUrl by remember(provider.baseUrl) { mutableStateOf(provider.baseUrl) }
            var apiKeyInput by remember { mutableStateOf("") }
            val maskedExisting = remember(provider.encryptedApiKey) {
                val plain = CredentialCipher.decrypt(provider.encryptedApiKey)
                CredentialCipher.maskKey(plain)
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    1.dp,
                    if (provider.isPrimary) AuraEmerald else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = provider.lastStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (provider.lastStatus.startsWith("ERROR")) AuraCrimson else AuraEmerald
                            )
                        }
                        if (provider.isPrimary) {
                            Surface(color = AuraEmerald.copy(alpha = 0.2f), shape = CircleShape) {
                                Text(
                                    text = "PRIMARY BRAIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuraEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (provider.providerId == "gemini") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon").forEach { modelPreset ->
                                FilterChip(
                                    selected = textModel == modelPreset,
                                    onClick = { textModel = modelPreset },
                                    label = { Text(modelPreset, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = textModel,
                        onValueChange = { textModel = it },
                        label = { Text("Reasoning / Text Model") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = {
                            Text(
                                if (provider.providerId == "gemini") {
                                    "Optional Direct Gemini API Key ($maskedExisting)"
                                } else {
                                    "Encrypted API Key ($maskedExisting)"
                                }
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (provider.providerId != "gemini") {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("Endpoint URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveProvider(
                                    provider,
                                    if (apiKeyInput.isBlank()) null else apiKeyInput,
                                    textModel,
                                    voiceModel,
                                    baseUrl,
                                    true
                                )
                                apiKeyInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DuaCrimsonPrimary, contentColor = Color.White),
                            modifier = Modifier.testTag("save_provider_${provider.providerId}")
                        ) {
                            Text(if (provider.isPrimary) "Save Config" else "Save & Set Primary", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onTestProvider(provider) },
                            modifier = Modifier.testTag("test_provider_${provider.providerId}")
                        ) {
                            Text("Test Connection")
                        }
                    }
                }
            }
        }
    }
}
