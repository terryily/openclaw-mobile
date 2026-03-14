package com.openclaw.mobile.models

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Agent 数据模型
 */
@Serializable
data class Agent(
    val id: String,
    val name: String,
    val model: String? = null,
    val avatar: String? = null,
    val description: String? = null,
    val enabled: Boolean = true,
    val skills: List<String>? = null,
    val config: AgentConfig? = null
)

@Serializable
data class AgentConfig(
    val exec: ExecConfig? = null,
    val web: WebConfig? = null,
    val files: FilesConfig? = null,
    val display: DisplayConfig? = null
)

@Serializable
data class ExecConfig(
    val mode: String = "auto"  // off, ask, auto
)

@Serializable
data class WebConfig(
    val enabled: Boolean = true
)

@Serializable
data class FilesConfig(
    val enabled: Boolean = true
)

@Serializable
data class DisplayConfig(
    val showToolCalls: Boolean = false,
    val showThinking: Boolean = false
)

/**
 * 消息数据模型
 */
@Serializable
data class Message(
    val id: String,
    val role: String,  // user, assistant, system, tool
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: MessageMetadata? = null
)

@Serializable
data class MessageMetadata(
    val model: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val thinking: String? = null
)

@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject,
    val result: String? = null
)

/**
 * 会话数据模型
 */
@Serializable
data class Session(
    val key: String,
    val agentId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val lastMessageAt: Long? = null
)

/**
 * Skill 数据模型
 */
@Serializable
data class Skill(
    val name: String,
    val description: String,
    val skillKey: String,
    val source: String,
    val eligible: Boolean,
    val enabled: Boolean = true,
    val missing: List<String>? = null,
    val metadata: SkillMetadata? = null
)

@Serializable
data class SkillMetadata(
    val primaryEnv: String? = null,
    val requires: SkillRequires? = null,
    val always: Boolean = false
)

@Serializable
data class SkillRequires(
    val bins: List<String>? = null,
    val env: List<String>? = null,
    val config: Map<String, String>? = null
)

/**
 * Schedule 数据模型
 */
@Serializable
data class Schedule(
    val id: String,
    val agentId: String,
    val cron: String,
    val enabled: Boolean = true,
    val message: String? = null,
    val lastRunAt: Long? = null,
    val nextRunAt: Long? = null,
    val timezone: String = "UTC"
)

/**
 * 运行状态
 */
enum class RunStatus {
    IDLE,
    RUNNING,
    ERROR
}

/**
 * Agent 运行时状态
 */
@Serializable
data class AgentState(
    val agentId: String,
    val sessionKey: String,
    val status: String = "idle",
    val runId: String? = null,
    val model: String? = null
)
