package com.openclaw.shared.gateway

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Gateway RPC 协议数据模型
 * 基于 OpenClaw Studio 源码
 */

// 请求帧
@Serializable
data class RequestFrame(
    val type: String = "req",
    val id: String,
    val method: String,
    val params: JsonElement? = null
)

// 响应帧
@Serializable
data class ResponseFrame(
    val type: String = "res",
    val id: String,
    val result: JsonElement? = null,
    val error: ErrorInfo? = null
)

// 事件帧
@Serializable
data class EventFrame(
    val type: String = "event",
    val event: EventPayload
)

// 错误帧
@Serializable
data class ErrorFrame(
    val type: String = "error",
    val id: String,
    val error: ErrorInfo
)

@Serializable
data class ErrorInfo(
    val code: Int? = null,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class EventPayload(
    val type: String,
    val delta: String? = null,
    val content: String? = null,
    val timestamp: Long? = null,
    val role: String? = null
)

/**
 * chat.send 参数
 */
@Serializable
data class ChatSendParams(
    val sessionKey: String,
    val message: String,
    val thinking: String? = null,
    val deliver: Boolean? = null,
    val attachments: List<Attachment>? = null,
    val timeoutMs: Int? = null
)

@Serializable
data class Attachment(
    val type: String,
    val data: String
)

/**
 * chat.history 参数
 */
@Serializable
data class ChatHistoryParams(
    val sessionKey: String,
    val limit: Int? = null,
    val before: Long? = null
)

/**
 * chat.abort 参数
 */
@Serializable
data class ChatAbortParams(
    val sessionKey: String
)

/**
 * 消息记录
 */
@Serializable
data class Message(
    val role: String,  // "user" or "assistant"
    val content: String,
    val timestamp: Long? = null
)

/**
 * Agent 状态
 */
@Serializable
data class AgentState(
    val agentId: String,
    val name: String,
    val sessionKey: String,
    val status: String = "idle",  // "idle", "running", "error"
    val model: String? = null,
    val lastActivityAt: Long? = null
)
