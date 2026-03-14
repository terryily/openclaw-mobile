package com.openclaw.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openclaw.mobile.ui.screens.*
import com.openclaw.mobile.ui.theme.OpenClawTheme
import com.openclaw.mobile.viewmodels.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            OpenClawTheme {
                OpenClawApp()
            }
        }
    }
}

@Composable
fun OpenClawApp(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = viewModel()
) {
    val uiState by mainViewModel.uiState.collectAsState()
    
    Scaffold(
        bottomBar = {
            if (uiState.isConnected) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, "聊天") },
                        label = { Text("聊天") },
                        selected = uiState.currentRoute == "chat",
                        onClick = { navController.navigate("chat") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, "Agents") },
                        label = { Text("Agents") },
                        selected = uiState.currentRoute == "agents",
                        onClick = { navController.navigate("agents") }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, "设置") },
                        label = { Text("设置") },
                        selected = uiState.currentRoute == "settings",
                        onClick = { navController.navigate("settings") }
                    )
                }
            }
        }
    ) { padding ->
        if (!uiState.isConnected && !uiState.isConnecting) {
            // 连接界面
            ConnectionScreen(
                onConnect = { url, token ->
                    mainViewModel.connect(url, token)
                }
            )
        } else if (uiState.isConnecting) {
            // 连接中界面
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Text("连接中...", modifier = Modifier.padding(top = 16.dp))
            }
        } else {
            // 主界面
            NavHost(
                navController = navController,
                startDestination = "chat",
                modifier = Modifier.padding(padding)
            ) {
                composable("chat") {
                    val chatViewModel: ChatViewModel = viewModel()
                    LaunchedEffect(chatViewModel) {
                        chatViewModel.init(mainViewModel)
                    }
                    ChatScreen(
                        viewModel = chatViewModel,
                        onOpenSettings = { navController.navigate("settings") }
                    )
                }
                
                composable("agents") {
                    val agentsViewModel: AgentsViewModel = viewModel()
                    LaunchedEffect(agentsViewModel) {
                        agentsViewModel.init(mainViewModel)
                    }
                    AgentsScreen(
                        viewModel = agentsViewModel,
                        onSelectAgent = { agentId ->
                            mainViewModel.selectAgent(agentId)
                            navController.navigate("chat")
                        }
                    )
                }
                
                composable("settings") {
                    val settingsViewModel: SettingsViewModel = viewModel()
                    LaunchedEffect(settingsViewModel) {
                        settingsViewModel.init(mainViewModel)
                    }
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSkills = { navController.navigate("skills") },
                        onNavigateToAutomations = { navController.navigate("automations") }
                    )
                }
                
                composable("skills") {
                    val skillsViewModel: SkillsViewModel = viewModel()
                    LaunchedEffect(skillsViewModel) {
                        skillsViewModel.init(mainViewModel)
                    }
                    SkillsScreen(
                        viewModel = skillsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                
                composable("automations") {
                    val automationsViewModel: AutomationsViewModel = viewModel()
                    LaunchedEffect(automationsViewModel) {
                        automationsViewModel.init(mainViewModel)
                    }
                    AutomationsScreen(
                        viewModel = automationsViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/**
 * 应用 UI 状态
 */
data class MainUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val connectionError: String? = null,
    val currentRoute: String = "chat",
    val currentAgentId: String? = null
)
