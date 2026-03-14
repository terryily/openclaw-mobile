package com.openclaw.mobile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.mobile.models.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AutomationsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AutomationsUiState())
    val uiState: StateFlow<AutomationsUiState> = _uiState.asStateFlow()
    
    private var mainViewModel: MainViewModel? = null
    
    fun init(mainViewModel: MainViewModel) {
        this.mainViewModel = mainViewModel
        
        // 加载 Agent 列表
        viewModelScope.launch {
            mainViewModel.agents.collect { agents ->
                _uiState.value = _uiState.value.copy(agents = agents)
            }
        }
    }
    
    fun loadSchedules() {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            client.schedules.listSchedules().onSuccess { schedules ->
                _uiState.value = _uiState.value.copy(
                    schedules = schedules,
                    isLoading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = it.message
                )
            }
        }
    }
    
    fun createSchedule(agentId: String, cron: String, message: String?) {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            client.schedules.createSchedule(agentId, cron, message)
            loadSchedules()
        }
    }
    
    fun updateSchedule(scheduleId: String, cron: String, message: String?) {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            client.schedules.updateScheduleCron(scheduleId, cron)
            client.schedules.updateScheduleMessage(scheduleId, message)
            loadSchedules()
        }
    }
    
    fun toggleSchedule(scheduleId: String, enabled: Boolean) {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            client.schedules.toggleSchedule(scheduleId, enabled)
            
            // 更新本地状态
            _uiState.value = _uiState.value.copy(
                schedules = _uiState.value.schedules.map { schedule ->
                    if (schedule.id == scheduleId) {
                        schedule.copy(enabled = enabled)
                    } else {
                        schedule
                    }
                }
            )
        }
    }
    
    fun deleteSchedule(scheduleId: String) {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            client.schedules.deleteSchedule(scheduleId)
            
            // 从列表中移除
            _uiState.value = _uiState.value.copy(
                schedules = _uiState.value.schedules.filter { it.id != scheduleId }
            )
        }
    }
}

data class AutomationsUiState(
    val schedules: List<Schedule> = emptyList(),
    val agents: List<com.openclaw.mobile.models.Agent> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
