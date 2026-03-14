package com.openclaw.mobile.gateway

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import java.util.concurrent.TimeUnit
import okhttp3.*

/**
 * OpenClaw Gateway WebSocket 客户端
 * 
 * 负责与 OpenClaw Gateway 建立 WebSocket 连接，
 * 处理 RPC 调用和事件流。
 */
class GatewayClient(
    private val gatewayUrl: String,
    private val token: String? = null
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private val pendingCalls = ConcurrentHashMap<String, CompletableDeferred<JsonElement>>()
    private val eventChannel = Channel<EventFrame>(Channel.UNLIMITED)
    private val json = Json { ignoreUnknownKeys = true }
    
    @Volatile
    var isConnected: Boolean = false
        private set
    
    @Volatile
    var connectionError: String? = null
        private set
    
    private val listeners = mutableListOf<(EventFrame) -> Unit>()
    
    /**
     * 连接到 Gateway
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isConnected) {
            return@withContext Result.success(Unit)
        }
        
        try {
            val requestBuilder = Request.Builder().url(gatewayUrl)
            
            // 如果有 token，添加到 header
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            val request = requestBuilder.build()
            val deferred = CompletableDeferred<Unit>()
            
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnected = true
                    connectionError = null
                    deferred.complete(Unit)
                }
                
                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(text)
                }
                
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    isConnected = false
                    connectionError = t.message
                    deferred.completeExceptionally(t)
                    
                    // 通知所有等待中的调用
                    pendingCalls.values.forEach { 
                        it.completeExceptionally(t)
                    }
                    pendingCalls.clear()
                }
                
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                    isConnected = false
                }
                
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    isConnected = false
                }
            })
            
            deferred.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    
    /**
     * RPC 调用
     */
    suspend inline fun <reified T> call(
        method: String,
        params: Any? = null
    ): Result<T> = withContext(Dispatchers.IO) {
        if (!isConnected) {
            return@withContext Result.failure(IllegalStateException("Not connected"))
        }
        
        val id = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<JsonElement>()
        pendingCalls[id] = deferred
        
        try {
            val frame = buildJsonObject {
                put("type", "req")
                put("id", id)
                put("method", method)
                if (params != null) {
                    put("params", Json.encodeToJsonElement(params))
                }
            }
            
            val sent = webSocket?.send(frame.toString()) ?: false
            if (!sent) {
                pendingCalls.remove(id)
                return@withContext Result.failure(IllegalStateException("Failed to send request"))
            }
            
            // 等待响应
            val response = withTimeoutOrNull(30000) {
                deferred.await()
            } ?: run {
                pendingCalls.remove(id)
                return@withContext Result.failure(TimeoutCancellationException("Request timeout"))
            }
            
            val result = json.decodeFromJsonElement<T>(response)
            Result.success(result)
        } catch (e: Exception) {
            pendingCalls.remove(id)
            Result.failure(e)
        }
    }
    
    /**
     * 监听事件
     */
    fun onEvent(handler: (EventFrame) -> Unit): () -> Unit {
        listeners.add(handler)
        return { listeners.remove(handler) }
    }
    
    /**
     * 获取事件流
     */
    fun eventStream(): ReceiveChannel<EventFrame> = eventChannel
    
    /**
     * 处理收到的消息
     */
    private fun handleMessage(text: String) {
        try {
            val frame = json.parseToJsonElement(text).jsonObject
            val type = frame["type"]?.jsonPrimitive?.content
            
            when (type) {
                "res" -> handleResponse(frame)
                "event" -> handleEvent(frame)
                "error" -> handleError(frame)
            }
        } catch (e: Exception) {
            // 解析失败，忽略
        }
    }
    
    /**
     * 处理响应
     */
    private fun handleResponse(frame: JsonObject) {
        val id = frame["id"]?.jsonPrimitive?.content ?: return
        val result = frame["result"]
        
        pendingCalls.remove(id)?.let { deferred ->
            if (result != null) {
                deferred.complete(result)
            } else {
                deferred.complete(JsonNull)
            }
        }
    }
    
    /**
     * 处理事件
     */
    private fun handleEvent(frame: JsonObject) {
        val event = frame["event"]?.jsonObject ?: return
        val eventFrame = EventFrame(
            type = event["type"]?.jsonPrimitive?.content ?: "",
            data = event
        )
        
        // 通知监听器
        listeners.forEach { it(eventFrame) }
        
        // 发送到 channel
        try {
            eventChannel.trySend(eventFrame)
        } catch (e: Exception) {
            // Channel full, ignore
        }
    }
    
    /**
     * 处理错误
     */
    private fun handleError(frame: JsonObject) {
        val id = frame["id"]?.jsonPrimitive?.content ?: return
        val error = frame["error"]?.jsonObject
        val message = error?.get("message")?.jsonPrimitive?.content ?: "Unknown error"
        
        pendingCalls.remove(id)?.completeExceptionally(Exception(message))
    }
}

/**
 * 事件帧
 */
@Serializable
data class EventFrame(
    val type: String,
    val data: JsonObject
)

/**
 * RPC 请求帧
 */
@Serializable
data class RequestFrame(
    val type: String = "req",
    val id: String = UUID.randomUUID().toString(),
    val method: String,
    val params: JsonElement? = null
)

/**
 * RPC 响应帧
 */
@Serializable
data class ResponseFrame(
    val type: String = "res",
    val id: String,
    val result: JsonElement? = null,
    val error: ErrorInfo? = null
)

/**
 * 错误信息
 */
@Serializable
data class ErrorInfo(
    val code: String? = null,
    val message: String
)
