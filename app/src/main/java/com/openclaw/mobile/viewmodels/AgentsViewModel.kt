package com.openclaw.mobile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.mobile.models.Agent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgentsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AgentsUiState())
    val uiState: StateFlow<AgentsUiState> = _uiState.asStateFlow()
    
    private var mainViewModel: MainViewModel? = null
    
    fun init(mainViewModel: MainViewModel) {
        this.mainViewModel = mainViewModel
        
        viewModelScope.launch {
            mainViewModel.agents.collect { agents ->
                _uiState.value = _uiState.value.copy(
                    agents = agents,
                    isLoading = false
                )
            }
        }
        
        viewModelScope.launch {
            mainViewModel.currentAgent.collect { agent ->
                _uiState.value = _uiState.value.copy(
                    selectedAgentId = agent?.id
                )
            }
        }
    }
    
    fun createAgent(name: String) {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            client.agents.createAgent(name)
        }
    }
    
    fun deleteAgent(agentId: String) {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            client.agents.deleteAgent(agentId)
        }
    }
}

data class AgentsUiState(
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = true,
    val selectedAgentId: String? = null
)
