package com.openclaw.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openclaw.mobile.viewmodels.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSkills: () -> Unit = {},
    onNavigateToAutomations: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Agent 信息
            if (uiState.agent != null) {
                SettingsSection(title = "Agent 信息") {
                    OutlinedTextField(
                        value = uiState.agent!!.name,
                        onValueChange = { },
                        label = { Text("名称") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = uiState.agent!!.model ?: "未设置",
                        onValueChange = { },
                        label = { Text("模型") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                }
            }
            
            Divider()
            
            // Capabilities
            SettingsSection(title = "能力控制") {
                SettingItem(
                    icon = Icons.Default.Terminal,
                    title = "命令执行",
                    subtitle = when (uiState.execMode) {
                        "off" -> "关闭"
                        "ask" -> "询问"
                        "auto" -> "自动"
                        else -> "未知"
                    }
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (uiState.execMode) {
                                "off" -> "关闭"
                                "ask" -> "询问"
                                "auto" -> "自动"
                                else -> "未知"
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("关闭") },
                                onClick = {
                                    viewModel.updateExecMode("off")
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("询问") },
                                onClick = {
                                    viewModel.updateExecMode("ask")
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("自动") },
                                onClick = {
                                    viewModel.updateExecMode("auto")
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                SettingItem(
                    icon = Icons.Default.Language,
                    title = "Web 访问",
                    subtitle = if (uiState.webEnabled) "已启用" else "已禁用"
                ) {
                    Switch(
                        checked = uiState.webEnabled,
                        onCheckedChange = { viewModel.updateWebAccess(it) }
                    )
                }
                
                SettingItem(
                    icon = Icons.Default.Folder,
                    title = "文件工具",
                    subtitle = if (uiState.filesEnabled) "已启用" else "已禁用"
                ) {
                    Switch(
                        checked = uiState.filesEnabled,
                        onCheckedChange = { viewModel.updateFilesAccess(it) }
                    )
                }
            }
            
            Divider()
            
            // 显示设置
            SettingsSection(title = "显示设置") {
                SettingItem(
                    icon = Icons.Default.Build,
                    title = "显示工具调用",
                    subtitle = "在聊天中显示工具调用详情"
                ) {
                    Switch(
                        checked = uiState.showToolCalls,
                        onCheckedChange = { viewModel.updateShowToolCalls(it) }
                    )
                }
                
                SettingItem(
                    icon = Icons.Default.Lightbulb,
                    title = "显示思考过程",
                    subtitle = "显示 AI 的思考过程"
                ) {
                    Switch(
                        checked = uiState.showThinking,
                        onCheckedChange = { viewModel.updateShowThinking(it) }
                    )
                }
            }
            
            Divider()
            
            // 快捷方式
            SettingsSection(title = "更多设置") {
                ListItem(
                    headlineContent = { Text("技能管理") },
                    supportingContent = { Text("管理 Agent 可用的技能") },
                    leadingContent = {
                        Icon(Icons.Default.Extension, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    onClick = onNavigateToSkills
                )
                
                ListItem(
                    headlineContent = { Text("自动化") },
                    supportingContent = { Text("定时任务和自动执行") },
                    leadingContent = {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    onClick = onNavigateToAutomations
                )
                
                ListItem(
                    headlineContent = { Text("Personality") },
                    supportingContent = { Text("编辑 Agent 的个性和行为") },
                    leadingContent = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}
