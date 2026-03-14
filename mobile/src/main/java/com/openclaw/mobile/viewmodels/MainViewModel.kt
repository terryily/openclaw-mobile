package com.openclaw.mobile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.mobile.OpenClawClient
import com.openclaw.mobile.models.Agent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private var client: OpenClawClient? = null
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()
    
    private val _currentAgent = MutableStateFlow<Agent?>(null)
    val currentAgent: StateFlow<Agent?> = _currentAgent.asStateFlow()
    
    fun connect(gatewayUrl: String, token: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true)
            
            try {
                client = OpenClawClient(gatewayUrl, token)
                val result = client!!.connect()
                
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isConnected = true,
                        isConnecting = false,
                        connectionError = null
                    )
                    loadAgents()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        connectionError = result.exceptionOrNull()?.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    connectionError = e.message
                )
            }
        }
    }
    
    private suspend fun loadAgents() {
        client?.let { c ->
            c.agents.listAgents().onSuccess { agentList ->
                _agents.value = agentList
                if (agentList.isNotEmpty()) {
                    selectAgent(agentList.first().id)
                }
            }
        }
    }
    
    fun selectAgent(agentId: String) {
        val agent = _agents.value.find { it.id == agentId }
        _currentAgent.value = agent
        _uiState.value = _uiState.value.copy(currentAgentId = agentId)
    }
    
    fun getClient(): OpenClawClient? = client
    
    override fun onCleared() {
        super.onCleared()
        client?.disconnect()
    }
}
