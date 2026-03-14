package com.openclaw.shared.operations

import com.openclaw.shared.gateway.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * 聊天操作
 * 基于 OpenClaw Studio chatSendOperation
 */
class ChatOperation(
    private val client: GatewayClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    
    private val _streamText = MutableStateFlow("")
    val streamText: StateFlow<String> = _streamText.asStateFlow()
    
    private var currentSessionKey: String? = null
    
    init {
        // 监听流式事件
        kotlinx.coroutines.GlobalScope.launch {
            client.events.collect { event ->
                handleEvent(event)
            }
        }
    }
    
    /**
     * 发送消息
     */
    suspend fun sendMessage(
        sessionKey: String,
        message: String
    ): Result<Unit> {
        return try {
            currentSessionKey = sessionKey
            
            // 添加用户消息
            val userMessage = Message(
                role = "user",
                content = message,
                timestamp = System.currentTimeMillis()
            )
            _messages.value = _messages.value + userMessage
            
            _isStreaming.value = true
            _streamText.value = ""
            
            // 调用 chat.send
            val params = ChatSendParams(
                sessionKey = sessionKey,
                message = message
            )
            
            client.call<ResponseFrame>("chat.send", params)
            
            Result.success(Unit)
        } catch (e: Exception) {
            _isStreaming.value = false
            Result.failure(e)
        }
    }
    
    /**
     * 加载历史消息
     */
    suspend fun loadHistory(
        sessionKey: String,
        limit: Int = 50
    ): Result<List<Message>> {
        return try {
            val params = ChatHistoryParams(
                sessionKey = sessionKey,
                limit = limit
            )
            
            val response = client.call<ResponseFrame>("chat.history", params)
            
            // 解析历史消息
            // TODO: 根据实际响应格式解析
            
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 中止运行
     */
    suspend fun abortRun(sessionKey: String): Result<Unit> {
        return try {
            val params = ChatAbortParams(sessionKey = sessionKey)
            client.call<ResponseFrame>("chat.abort", params)
            _isStreaming.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 处理流式事件
     */
    private fun handleEvent(event: EventFrame) {
        when (event.event.type) {
            "assistant_text_delta" -> {
                // 流式文本增量
                event.event.delta?.let { delta ->
                    _streamText.value += delta
                }
            }
            "assistant_text_done" -> {
                // 流式文本完成
                _streamText.value.let { text ->
                    if (text.isNotEmpty()) {
                        val assistantMessage = Message(
                            role = "assistant",
                            content = text,
                            timestamp = System.currentTimeMillis()
                        )
                        _messages.value = _messages.value + assistantMessage
                    }
                }
                _streamText.value = ""
                _isStreaming.value = false
            }
            "run_done" -> {
                // 运行完成
                _isStreaming.value = false
            }
        }
    }
    
    /**
     * 清空消息
     */
    fun clearMessages() {
        _messages.value = emptyList()
        _streamText.value = ""
        _isStreaming.value = false
    }
}
