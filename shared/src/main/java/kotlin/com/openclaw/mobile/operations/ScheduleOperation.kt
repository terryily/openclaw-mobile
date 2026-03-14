package com.openclaw.mobile.operations

import com.openclaw.mobile.gateway.GatewayClient
import com.openclaw.mobile.models.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Schedule 操作
 * 
 * 负责定时任务管理相关的所有操作：
 * - 列出定时任务
 * - 创建定时任务
 * - 更新定时任务
 * - 删除定时任务
 */
class ScheduleOperation(
    private val client: GatewayClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 列出所有定时任务
     */
    suspend fun listSchedules(): Result<List<Schedule>> {
        return client.call<SchedulesListResult>("schedules.list").map { it.schedules }
    }
    
    /**
     * 创建定时任务
     * 
     * @param agentId Agent ID
     * @param cron Cron 表达式
     * @param message 要发送的消息（可选）
     * @param timezone 时区（默认 UTC）
     */
    suspend fun createSchedule(
        agentId: String,
        cron: String,
        message: String? = null,
        timezone: String = "UTC"
    ): Result<Schedule> {
        val params = buildJsonObject {
            put("agentId", agentId)
            put("cron", cron)
            if (message != null) put("message", message)
            put("timezone", timezone)
        }
        
        return client.call("schedules.create", params)
    }
    
    /**
     * 更新定时任务
     * 
     * @param scheduleId 定时任务 ID
     * @param updates 更新的字段
     */
    suspend fun updateSchedule(
        scheduleId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        val params = buildJsonObject {
            put("scheduleId", scheduleId)
            put("updates", Json.encodeToJsonElement(updates))
        }
        
        return client.call("schedules.update", params)
    }
    
    /**
     * 删除定时任务
     */
    suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        val params = buildJsonObject {
            put("scheduleId", scheduleId)
        }
        
        return client.call("schedules.delete", params)
    }
    
    /**
     * 启用/禁用定时任务
     */
    suspend fun toggleSchedule(
        scheduleId: String,
        enabled: Boolean
    ): Result<Unit> {
        return updateSchedule(scheduleId, mapOf("enabled" to enabled))
    }
    
    /**
     * 更新定时任务的 Cron 表达式
     */
    suspend fun updateScheduleCron(
        scheduleId: String,
        cron: String
    ): Result<Unit> {
        return updateSchedule(scheduleId, mapOf("cron" to cron))
    }
    
    /**
     * 更新定时任务的消息
     */
    suspend fun updateScheduleMessage(
        scheduleId: String,
        message: String?
    ): Result<Unit> {
        return updateSchedule(scheduleId, mapOf("message" to message))
    }
}

@Serializable
data class SchedulesListResult(
    val schedules: List<Schedule>
)
