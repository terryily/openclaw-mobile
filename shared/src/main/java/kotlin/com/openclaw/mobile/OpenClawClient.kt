package com.openclaw.mobile

import com.openclaw.mobile.gateway.GatewayClient
import com.openclaw.mobile.operations.*
import com.openclaw.mobile.models.*

/**
 * OpenClaw Mobile 客户端
 * 
 * 统一的客户端接口，整合所有操作
 * 
 * 使用示例：
 * 
 * ```kotlin
 * // 创建客户端
 * val client = OpenClawClient(
 *     gatewayUrl = "wss://your-gateway.com",
 *     token = "your-token"
 * )
 * 
 * // 连接
 * client.connect()
 * 
 * // 列出 Agent
 * val agents = client.agents.listAgents()
 * 
 * // 发送消息并监听流式响应
 * client.chat.sendMessageWithStreaming(
 *     sessionKey = "session-123",
 *     message = "Hello!",
 *     onDelta = { text -> println("收到: $text") },
 *     onComplete = { println("完成") },
 *     onError = { error -> println("错误: $error") }
 * )
 * 
 * // 断开连接
 * client.disconnect()
 * ```
 */
class OpenClawClient(
    gatewayUrl: String,
    token: String? = null
) {
    private val gateway = GatewayClient(gatewayUrl, token)
    
    /**
     * 聊天操作
     */
    val chat = ChatOperation(gateway)
    
    /**
     * Agent 操作
     */
    val agents = AgentOperation(gateway)
    
    /**
     * 技能操作
     */
    val skills = SkillOperation(gateway)
    
    /**
     * 定时任务操作
     */
    val schedules = ScheduleOperation(gateway)
    
    /**
     * 是否已连接
     */
    val isConnected: Boolean
        get() = gateway.isConnected
    
    /**
     * 连接错误
     */
    val connectionError: String?
        get() = gateway.connectionError
    
    /**
     * 连接到 Gateway
     */
    suspend fun connect(): Result<Unit> {
        return gateway.connect()
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        gateway.disconnect()
    }
    
    /**
     * 监听事件
     */
    fun onEvent(handler: (EventFrame) -> Unit) = gateway.onEvent(handler)
}
