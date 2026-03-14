package com.openclaw.mobile.operations

import com.openclaw.mobile.gateway.GatewayClient
import com.openclaw.mobile.models.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Agent 操作
 * 
 * 负责 Agent 管理相关的所有操作：
 * - 列出 Agent
 * - 创建 Agent
 * - 更新 Agent
 * - 删除 Agent
 */
class AgentOperation(
    private val client: GatewayClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 列出所有 Agent
     */
    suspend fun listAgents(): Result<List<Agent>> {
        return client.call<AgentsListResult>("agents.list").map { it.agents }
    }
    
    /**
     * 创建新 Agent
     * 
     * @param name Agent 名称
     * @param model 模型（可选）
     * @param avatar 头像（可选）
     */
    suspend fun createAgent(
        name: String,
        model: String? = null,
        avatar: String? = null
    ): Result<Agent> {
        val params = buildJsonObject {
            put("name", name)
            if (model != null) put("model", model)
            if (avatar != null) put("avatar", avatar)
        }
        
        return client.call("agents.create", params)
    }
    
    /**
     * 更新 Agent 配置
     * 
     * @param agentId Agent ID
     * @param updates 更新的字段
     */
    suspend fun updateAgent(
        agentId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        val params = buildJsonObject {
            put("agentId", agentId)
            put("updates", Json.encodeToJsonElement(updates))
        }
        
        return client.call("agents.update", params)
    }
    
    /**
     * 删除 Agent
     */
    suspend fun deleteAgent(agentId: String): Result<Unit> {
        val params = buildJsonObject {
            put("agentId", agentId)
        }
        
        return client.call("agents.delete", params)
    }
    
    /**
     * 获取 Agent 文件内容
     * 
     * @param agentId Agent ID
     * @param filename 文件名（如 SOUL.md, AGENTS.md, USER.md, IDENTITY.md）
     */
    suspend fun getAgentFile(
        agentId: String,
        filename: String
    ): Result<String> {
        val params = buildJsonObject {
            put("agentId", agentId)
            put("filename", filename)
        }
        
        return client.call<AgentFileResult>("agents.getFile", params).map { it.content }
    }
    
    /**
     * 更新 Agent 文件
     * 
     * @param agentId Agent ID
     * @param filename 文件名
     * @param content 文件内容
     */
    suspend fun updateAgentFile(
        agentId: String,
        filename: String,
        content: String
    ): Result<Unit> {
        val params = buildJsonObject {
            put("agentId", agentId)
            put("filename", filename)
            put("content", content)
        }
        
        return client.call("agents.updateFile", params)
    }
    
    /**
     * 重命名 Agent
     */
    suspend fun renameAgent(
        agentId: String,
        newName: String
    ): Result<Unit> {
        return updateAgent(agentId, mapOf("name" to newName))
    }
    
    /**
     * 更新 Agent 执行模式
     */
    suspend fun updateExecMode(
        agentId: String,
        mode: String  // off, ask, auto
    ): Result<Unit> {
        return updateAgent(agentId, mapOf(
            "config" to mapOf("exec" to mapOf("mode" to mode))
        ))
    }
    
    /**
     * 更新 Agent Web 访问设置
     */
    suspend fun updateWebAccess(
        agentId: String,
        enabled: Boolean
    ): Result<Unit> {
        return updateAgent(agentId, mapOf(
            "config" to mapOf("web" to mapOf("enabled" to enabled))
        ))
    }
    
    /**
     * 更新 Agent 文件工具设置
     */
    suspend fun updateFilesAccess(
        agentId: String,
        enabled: Boolean
    ): Result<Unit> {
        return updateAgent(agentId, mapOf(
            "config" to mapOf("files" to mapOf("enabled" to enabled))
        ))
    }
}

@Serializable
data class AgentsListResult(
    val agents: List<Agent>
)

@Serializable
data class AgentFileResult(
    val content: String
)
