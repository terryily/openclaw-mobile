package com.openclaw.mobile.operations

import com.openclaw.mobile.gateway.GatewayClient
import com.openclaw.mobile.gateway.EventFrame
import com.openclaw.mobile.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * 聊天操作
 * 
 * 负责处理聊天相关的所有操作：
 * - 发送消息
 * - 接收流式响应
 * - 加载历史
 * - 中止运行
 */
class ChatOperation(
    private val client: GatewayClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 发送消息
     * 
     * @param sessionKey 会话 ID
     * @param message 消息内容
     * @return 运行 ID
     */
    suspend fun sendMessage(
        sessionKey: String,
        message: String
    ): Result<String> {
        val params = buildJsonObject {
            put("sessionKey", sessionKey)
            put("message", message)
        }
        
        return client.call<SendMessageResult>("chat.send", params).map { it.runId }
    }
    
    /**
     * 发送消息并监听流式响应
     * 
     * @param sessionKey 会话 ID
     * @param message 消息内容
     * @param onDelta 文本增量回调
     * @param onToolCall 工具调用回调
     * @param onThinking 思考过程回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    suspend fun sendMessageWithStreaming(
        sessionKey: String,
        message: String,
        onDelta: (String) -> Unit,
        onToolCall: ((ToolCall) -> Unit)? = null,
        onThinking: ((String) -> Unit)? = null,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val eventJob = GlobalScope.launch {
            client.eventStream().collect { event ->
                when (event.type) {
                    "assistant_text_delta" -> {
                        val delta = event.data["delta"]?.jsonPrimitive?.content ?: ""
                        onDelta(delta)
                    }
                    "assistant_text_done" -> {
                        onComplete()
                    }
                    "tool_call" -> {
                        onToolCall?.let { callback ->
                            val toolCall = json.decodeFromJsonElement<ToolCall>(event.data)
                            callback(toolCall)
                        }
                    }
                    "thinking_delta" -> {
                        onThinking?.let { callback ->
                            val delta = event.data["delta"]?.jsonPrimitive?.content ?: ""
                            callback(delta)
                        }
                    }
                    "run_done" -> {
                        onComplete()
                    }
                    "run_error" -> {
                        val errorMsg = event.data["error"]?.jsonPrimitive?.content ?: "Unknown error"
                        onError(errorMsg)
                    }
                }
            }
        }
        
        // 发送消息
        val result = sendMessage(sessionKey, message)
        
        if (result.isFailure) {
            eventJob.cancel()
            onError(result.exceptionOrNull()?.message ?: "Failed to send message")
        }
    }
    
    /**
     * 加载历史消息
     * 
     * @param sessionKey 会话 ID
     * @param limit 限制数量
     * @param before 在某个消息 ID 之前
     */
    suspend fun loadHistory(
        sessionKey: String,
        limit: Int = 50,
        before: String? = null
    ): Result<List<Message>> {
        val params = buildJsonObject {
            put("sessionKey", sessionKey)
            put("limit", limit)
            if (before != null) {
                put("before", before)
            }
        }
        
        return client.call<HistoryResult>("chat.history", params).map { it.messages }
    }
    
    /**
     * 中止运行
     */
    suspend fun abortRun(sessionKey: String): Result<Unit> {
        val params = buildJsonObject {
            put("sessionKey", sessionKey)
        }
        
        return client.call("chat.abort", params)
    }
}

@Serializable
data class SendMessageResult(
    val status: String,
    val runId: String
)

@Serializable
data class HistoryResult(
    val messages: List<Message>,
    val hasMore: Boolean = false
)
