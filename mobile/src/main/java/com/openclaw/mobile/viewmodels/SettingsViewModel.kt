package com.openclaw.mobile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.mobile.models.Agent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private var mainViewModel: MainViewModel? = null
    
    fun init(mainViewModel: MainViewModel) {
        this.mainViewModel = mainViewModel
        
        viewModelScope.launch {
            mainViewModel.currentAgent.collect { agent ->
                agent?.let {
                    _uiState.value = _uiState.value.copy(
                        agent = it,
                        execMode = it.config?.exec?.mode ?: "auto",
                        webEnabled = it.config?.web?.enabled ?: true,
                        filesEnabled = it.config?.files?.enabled ?: true,
                        showToolCalls = it.config?.display?.showToolCalls ?: false,
                        showThinking = it.config?.display?.showThinking ?: false
                    )
                }
            }
        }
    }
    
    fun updateExecMode(mode: String) {
        val client = mainViewModel?.getClient() ?: return
        val agentId = _uiState.value.agent?.id ?: return
        
        viewModelScope.launch {
            client.agents.updateExecMode(agentId, mode)
            _uiState.value = _uiState.value.copy(execMode = mode)
        }
    }
    
    fun updateWebAccess(enabled: Boolean) {
        val client = mainViewModel?.getClient() ?: return
        val agentId = _uiState.value.agent?.id ?: return
        
        viewModelScope.launch {
            client.agents.updateWebAccess(agentId, enabled)
            _uiState.value = _uiState.value.copy(webEnabled = enabled)
        }
    }
    
    fun updateFilesAccess(enabled: Boolean) {
        val client = mainViewModel?.getClient() ?: return
        val agentId = _uiState.value.agent?.id ?: return
        
        viewModelScope.launch {
            client.agents.updateFilesAccess(agentId, enabled)
            _uiState.value = _uiState.value.copy(filesEnabled = enabled)
        }
    }
    
    fun updateShowToolCalls(show: Boolean) {
        _uiState.value = _uiState.value.copy(showToolCalls = show)
    }
    
    fun updateShowThinking(show: Boolean) {
        _uiState.value = _uiState.value.copy(showThinking = show)
    }
}

data class SettingsUiState(
    val agent: Agent? = null,
    val execMode: String = "auto",
    val webEnabled: Boolean = true,
    val filesEnabled: Boolean = true,
    val showToolCalls: Boolean = false,
    val showThinking: Boolean = false
)
