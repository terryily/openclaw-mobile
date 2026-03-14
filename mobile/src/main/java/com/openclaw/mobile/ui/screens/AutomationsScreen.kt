package com.openclaw.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openclaw.mobile.models.Schedule
import com.openclaw.mobile.viewmodels.AutomationsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsScreen(
    viewModel: AutomationsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Schedule?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadSchedules()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自动化") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "创建定时任务")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "还没有定时任务",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("创建第一个任务")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.schedules) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onToggle = { enabled ->
                            viewModel.toggleSchedule(schedule.id, enabled)
                        },
                        onEdit = {
                            showEditDialog = schedule
                        },
                        onDelete = {
                            viewModel.deleteSchedule(schedule.id)
                        }
                    )
                }
            }
        }
        
        // 创建对话框
        if (showCreateDialog) {
            CreateScheduleDialog(
                agents = uiState.agents,
                onDismiss = { showCreateDialog = false },
                onCreate = { agentId, cron, message ->
                    viewModel.createSchedule(agentId, cron, message)
                    showCreateDialog = false
                }
            )
        }
        
        // 编辑对话框
        showEditDialog?.let { schedule ->
            EditScheduleDialog(
                schedule = schedule,
                onDismiss = { showEditDialog = null },
                onSave = { cron, message ->
                    viewModel.updateSchedule(schedule.id, cron, message)
                    showEditDialog = null
                }
            )
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: Schedule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatCron(schedule.cron),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Cron 表达式
                    Text(
                        text = "Cron: ${schedule.cron}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 消息
                    if (schedule.message != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "消息: ${schedule.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    
                    // 下次运行时间
                    schedule.nextRunAt?.let { nextRun ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "下次运行: ${formatTime(nextRun)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = onToggle
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑")
                }
                
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除定时任务") },
            text = { Text("确定要删除这个定时任务吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScheduleDialog(
    agents: List<com.openclaw.mobile.models.Agent>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String?) -> Unit
) {
    var selectedAgentId by remember { mutableStateOf(agents.firstOrNull()?.id ?: "") }
    var cronExpression by remember { mutableStateOf("0 9 * * *") }  // 默认每天上午 9 点
    var message by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建定时任务") },
        text = {
            Column {
                // Agent 选择
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = agents.find { it.id == selectedAgentId }?.name ?: "选择 Agent",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Agent") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        agents.forEach { agent ->
                            DropdownMenuItem(
                                text = { Text(agent.name) },
                                onClick = {
                                    selectedAgentId = agent.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cron 表达式
                OutlinedTextField(
                    value = cronExpression,
                    onValueChange = { cronExpression = it },
                    label = { Text("Cron 表达式") },
                    placeholder = { Text("0 9 * * *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "示例：\n• 0 9 * * * - 每天 9:00\n• 0 */2 * * * - 每 2 小时\n• 0 9 * * 1 - 每周一 9:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 消息（可选）
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("消息（可选）") },
                    placeholder = { Text("要发送的消息...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        selectedAgentId,
                        cronExpression,
                        message.ifBlank { null }
                    )
                },
                enabled = selectedAgentId.isNotBlank() && cronExpression.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EditScheduleDialog(
    schedule: Schedule,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var cronExpression by remember { mutableStateOf(schedule.cron) }
    var message by remember { mutableStateOf(schedule.message ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑定时任务") },
        text = {
            Column {
                // Cron 表达式
                OutlinedTextField(
                    value = cronExpression,
                    onValueChange = { cronExpression = it },
                    label = { Text("Cron 表达式") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 消息
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("消息") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(cronExpression, message.ifBlank { null })
                },
                enabled = cronExpression.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 辅助函数
fun formatCron(cron: String): String {
    return when {
        cron.contains("*/") -> "定期执行"
        cron.contains("* * *") -> "每天"
        else -> "自定义"
    }
}

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
