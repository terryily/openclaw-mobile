package com.openclaw.mobile

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * OpenClaw Gateway Client
 * Handles WebSocket connection and RPC calls
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
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    var isConnected: Boolean = false
        private set

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isConnected) return@withContext Result.success(Unit)

        try {
            val requestBuilder = okhttp3.Request.Builder().url(gatewayUrl)
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            val deferred = CompletableDeferred<Unit>()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnected = true
                    deferred.complete(Unit)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    isConnected = false
                    deferred.completeExceptionally(t)
                    pendingCalls.values.forEach { it.completeExceptionally(t) }
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

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
        pendingCalls.clear()
    }
}
