package com.openclaw.shared.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import okhttp3.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OpenClaw Gateway 客户端
 * 基于 OpenClaw Studio GatewayClient 实现
 */
class GatewayClient(
    private val gatewayUrl: String = "wss://wjwly140920.eu.org"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private val pendingCalls = ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val _events = MutableSharedFlow<EventFrame>()
    val events: Flow<EventFrame> = _events.asSharedFlow()
    
    private var isConnected = false
    private var connectionListener: ((Boolean) -> Unit)? = null
    
    /**
     * 连接到 Gateway
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        if (isConnected) return@withContext
        
        val request = Request.Builder()
            .url(gatewayUrl)
            .build()
        
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                connectionListener?.invoke(true)
                println("[Gateway] Connected to $gatewayUrl")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                isConnected = false
                connectionListener?.invoke(false)
                println("[Gateway] Closing: $code / $reason")
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                connectionListener?.invoke(false)
                println("[Gateway] Failure: ${t.message}")
                
                // 完成所有待处理的调用
                pendingCalls.forEach { (_, deferred) ->
                    deferred.completeExceptionally(t)
                }
                pendingCalls.clear()
                
                // 3秒后自动重连
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    kotlinx.coroutines.delay(3000)
                    connect()
                }
            }
        })
    }
    
    /**
     * 处理收到的消息
     */
    private fun handleMessage(text: String) {
        try {
            // 尝试解析为事件帧
            if (text.contains("\"type\":\"event\"")) {
                val eventFrame = json.decodeFromString<EventFrame>(text)
                _events.tryEmit(eventFrame)
                return
            }
            
            // 尝试解析为响应帧
            if (text.contains("\"type\":\"res\"")) {
                val responseFrame = json.decodeFromString<ResponseFrame>(text)
                val deferred = pendingCalls.remove(responseFrame.id)
                if (deferred != null) {
                    if (responseFrame.error != null) {
                        deferred.completeExceptionally(
                            GatewayException(responseFrame.error.message)
                        )
                    } else {
                        deferred.complete(text)
                    }
                }
                return
            }
            
            // 尝试解析为错误帧
            if (text.contains("\"type\":\"error\"")) {
                val errorFrame = json.decodeFromString<ErrorFrame>(text)
                val deferred = pendingCalls.remove(errorFrame.id)
                if (deferred != null) {
                    deferred.completeExceptionally(
                        GatewayException(errorFrame.error.message)
                    )
                }
                return
            }
            
            println("[Gateway] Unknown message: $text")
        } catch (e: Exception) {
            println("[Gateway] Failed to parse message: ${e.message}")
        }
    }
    
    /**
     * RPC 调用
     */
    suspend inline fun <reified T> call(
        method: String,
        params: Any? = null
    ): T = withContext(Dispatchers.IO) {
        if (!isConnected) {
            connect()
        }
        
        val id = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<String>()
        pendingCalls[id] = deferred
        
        val request = RequestFrame(
            id = id,
            method = method,
            params = if (params != null) {
                json.parseToJsonElement(json.encodeToString(params))
            } else null
        )
        
        val requestJson = json.encodeToString(request)
        webSocket?.send(requestJson)
        
        // 等待响应
        val responseJson = deferred.await()
        json.decodeFromString<T>(responseJson)
    }
    
    /**
     * 设置连接监听器
     */
    fun setConnectionListener(listener: (Boolean) -> Unit) {
        connectionListener = listener
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
        pendingCalls.clear()
    }
}

class GatewayException(message: String) : Exception(message)
