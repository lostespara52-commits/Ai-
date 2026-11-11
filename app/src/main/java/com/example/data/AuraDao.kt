package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AuraDao {
    // Chat History
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesSync(limit: Int = 14): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    // 5-Tier Memory
    @Query("SELECT * FROM memory_entries ORDER BY importance DESC, updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries ORDER BY importance DESC, updatedAt DESC LIMIT :limit")
    suspend fun getTopMemoriesSync(limit: Int = 20): List<MemoryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntryEntity)

    @Query("DELETE FROM memory_entries WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM memory_entries")
    suspend fun clearAllMemories()

    // Workflows
    @Query("SELECT * FROM workflows ORDER BY updatedAt DESC")
    fun getAllWorkflows(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :id LIMIT 1")
    suspend fun getWorkflowById(id: Long): WorkflowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: WorkflowEntity): Long

    @Update
    suspend fun updateWorkflow(workflow: WorkflowEntity)

    @Query("DELETE FROM workflows WHERE id = :id")
    suspend fun deleteWorkflowById(id: Long)

    // Projects
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT 5")
    suspend fun getActiveProjectsSync(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    // Scheduled Tasks & Offline Queue
    @Query("SELECT * FROM scheduled_tasks ORDER BY triggerAtMillis ASC")
    fun getAllScheduledTasks(): Flow<List<ScheduledTaskEntity>>

    @Query("SELECT * FROM scheduled_tasks WHERE status = 'QUEUED_OFFLINE' OR status = 'SCHEDULED'")
    suspend fun getPendingTasksSync(): List<ScheduledTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTask(task: ScheduledTaskEntity): Long

    @Update
    suspend fun updateScheduledTask(task: ScheduledTaskEntity)

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun deleteScheduledTaskById(id: Long)

    // API Providers
    @Query("SELECT * FROM api_providers ORDER BY isPrimary DESC, displayName ASC")
    fun getAllApiProviders(): Flow<List<ApiProviderEntity>>

    @Query("SELECT * FROM api_providers")
    suspend fun getAllApiProvidersSync(): List<ApiProviderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiProvider(provider: ApiProviderEntity)

    @Update
    suspend fun updateApiProvider(provider: ApiProviderEntity)
}
