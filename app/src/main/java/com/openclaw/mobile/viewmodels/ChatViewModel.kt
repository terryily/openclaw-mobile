package com.openclaw.mobile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.mobile.models.Message
import com.openclaw.mobile.models.ToolCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var sessionKey = UUID.randomUUID().toString()
    private var mainViewModel: MainViewModel? = null
    
    fun init(mainViewModel: MainViewModel) {
        this.mainViewModel = mainViewModel
        
        // 监听当前 Agent 变化
        viewModelScope.launch {
            mainViewModel.currentAgent.collect { agent ->
                agent?.let {
                    _uiState.value = _uiState.value.copy(
                        agentId = it.id,
                        agentName = it.name,
                        model = it.model
                    )
                    sessionKey = UUID.randomUUID().toString()
                    _uiState.value = _uiState.value.copy(
                        messages = emptyList(),
                        streamingText = "",
                        thinkingText = "",
                        toolCalls = emptyList()
                    )
                }
            }
        }
    }
    
    fun sendMessage(text: String) {
        val client = mainViewModel?.getClient() ?: return
        
        // 添加用户消息
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = text
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            status = "running"
        )
        
        viewModelScope.launch {
            var fullResponse = ""
            var thinkingBuffer = ""
            val toolCallsBuffer = mutableListOf<ToolCall>()
            
            client.chat.sendMessageWithStreaming(
                sessionKey = sessionKey,
                message = text,
                onDelta = { delta ->
                    fullResponse += delta
                    _uiState.value = _uiState.value.copy(streamingText = fullResponse)
                },
                onToolCall = { toolCall ->
                    toolCallsBuffer.add(toolCall)
                    _uiState.value = _uiState.value.copy(toolCalls = toolCallsBuffer.toList())
                },
                onThinking = { thinking ->
                    thinkingBuffer += thinking
                    _uiState.value = _uiState.value.copy(thinkingText = thinkingBuffer)
                },
                onComplete = {
                    // 添加 AI 消息
                    val assistantMessage = Message(
                        id = UUID.randomUUID().toString(),
                        role = "assistant",
                        content = fullResponse
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + assistantMessage,
                        streamingText = "",
                        status = "idle"
                    )
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        status = "error",
                        streamingText = ""
                    )
                    val errorMessage = Message(
                        id = UUID.randomUUID().toString(),
                        role = "system",
                        content = "错误: $error"
                    )
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + errorMessage
                    )
                }
            )
        }
    }
    
    fun abortRun() {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            client.chat.abortRun(sessionKey)
            _uiState.value = _uiState.value.copy(status = "idle")
        }
    }
    
    fun newSession() {
        sessionKey = UUID.randomUUID().toString()
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            streamingText = "",
            thinkingText = "",
            toolCalls = emptyList(),
            status = "idle"
        )
    }
}

data class ChatUiState(
    val agentId: String? = null,
    val agentName: String? = null,
    val model: String? = null,
    val messages: List<Message> = emptyList(),
    val streamingText: String = "",
    val thinkingText: String = "",
    val toolCalls: List<ToolCall> = emptyList(),
    val status: String = "idle",
    val showToolCalls: Boolean = true,
    val showThinking: Boolean = false
)
