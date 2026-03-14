package com.openclaw.mobile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.mobile.models.Skill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SkillsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SkillsUiState())
    val uiState: StateFlow<SkillsUiState> = _uiState.asStateFlow()
    
    private var mainViewModel: MainViewModel? = null
    
    fun init(mainViewModel: MainViewModel) {
        this.mainViewModel = mainViewModel
    }
    
    fun loadSkills() {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            client.skills.getSkillsStatus().onSuccess { report ->
                _uiState.value = _uiState.value.copy(
                    skills = report.skills,
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
    
    fun toggleSkill(skillKey: String, enabled: Boolean) {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            client.skills.updateSkillConfig(skillKey, enabled = enabled)
            
            // 更新本地状态
            _uiState.value = _uiState.value.copy(
                skills = _uiState.value.skills.map { skill ->
                    if (skill.skillKey == skillKey) {
                        skill.copy(enabled = enabled)
                    } else {
                        skill
                    }
                }
            )
        }
    }
    
    fun configureSkill(skillKey: String, apiKey: String) {
        val client = mainViewModel?.getClient() ?: return
        
        viewModelScope.launch {
            client.skills.updateSkillConfig(skillKey, apiKey = apiKey)
        }
    }
}

data class SkillsUiState(
    val skills: List<Skill> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
