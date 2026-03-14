package com.openclaw.mobile.operations

import com.openclaw.mobile.gateway.GatewayClient
import com.openclaw.mobile.models.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Skill 操作
 * 
 * 负责技能管理相关的所有操作：
 * - 获取技能状态
 * - 安装技能
 * - 更新技能配置
 */
class SkillOperation(
    private val client: GatewayClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 获取技能状态
     */
    suspend fun getSkillsStatus(): Result<SkillStatusReport> {
        return client.call("skills.status")
    }
    
    /**
     * 安装技能依赖
     * 
     * @param skillKey 技能 key
     * @param installId 安装选项 ID
     */
    suspend fun installSkill(
        skillKey: String,
        installId: String
    ): Result<Unit> {
        val params = buildJsonObject {
            put("skillKey", skillKey)
            put("installId", installId)
        }
        
        return client.call("skills.install", params)
    }
    
    /**
     * 更新技能配置
     * 
     * @param skillKey 技能 key
     * @param enabled 是否启用
     * @param apiKey API 密钥（可选）
     * @param env 环境变量（可选）
     */
    suspend fun updateSkillConfig(
        skillKey: String,
        enabled: Boolean? = null,
        apiKey: String? = null,
        env: Map<String, String>? = null
    ): Result<Unit> {
        val params = buildJsonObject {
            put("skillKey", skillKey)
            if (enabled != null) put("enabled", enabled)
            if (apiKey != null) put("apiKey", apiKey)
            if (env != null) put("env", Json.encodeToJsonElement(env))
        }
        
        return client.call("skills.update", params)
    }
    
    /**
     * 为 Agent 启用/禁用技能
     * 
     * @param agentId Agent ID
     * @param skillKeys 技能 key 列表（null 表示所有，空数组表示无）
     */
    suspend fun updateAgentSkills(
        agentId: String,
        skillKeys: List<String>?
    ): Result<Unit> {
        val params = buildJsonObject {
            put("agentId", agentId)
            if (skillKeys != null) {
                put("skills", Json.encodeToJsonElement(skillKeys))
            }
        }
        
        return client.call("agents.update", params)
    }
}

@Serializable
data class SkillStatusReport(
    val skills: List<Skill>,
    val bins: Map<String, Boolean>? = null,
    val configChecks: Map<String, ConfigCheck>? = null
)

@Serializable
data class ConfigCheck(
    val path: String,
    val satisfied: Boolean
)
