package com.openclaw.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openclaw.mobile.models.Message
import com.openclaw.mobile.viewmodels.ChatViewModel
import kotlinx.coroutines.launch

/**
 * 聊天界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部栏
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = uiState.agentName ?: "OpenClaw",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (uiState.status != "idle") {
                        Text(
                            text = when (uiState.status) {
                                "running" -> "处理中..."
                                "error" -> "错误"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (uiState.status) {
                                "running" -> MaterialTheme.colorScheme.primary
                                "error" -> MaterialTheme.colorScheme.error
                                else -> Color.Unspecified
                            }
                        )
                    }
                }
            },
            actions = {
                // 新会话
                IconButton(onClick = { viewModel.newSession() }) {
                    Icon(Icons.Default.Add, "新会话")
                }
                // 设置
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, "设置")
                }
            }
        )
        
        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 历史消息
            items(uiState.messages) { message ->
                MessageItem(message = message)
            }
            
            // 流式文本
            if (uiState.streamingText.isNotEmpty()) {
                item {
                    MessageItem(
                        message = Message(
                            id = "streaming",
                            role = "assistant",
                            content = uiState.streamingText
                        ),
                        isStreaming = true
                    )
                }
            }
            
            // 思考过程
            if (uiState.showThinking && uiState.thinkingText.isNotEmpty()) {
                item {
                    ThinkingItem(text = uiState.thinkingText)
                }
            }
            
            // 工具调用
            if (uiState.showToolCalls && uiState.toolCalls.isNotEmpty()) {
                items(uiState.toolCalls) { toolCall ->
                    ToolCallItem(toolCall = toolCall)
                }
            }
        }
        
        // 自动滚动到底部
        LaunchedEffect(uiState.messages.size, uiState.streamingText) {
            if (uiState.messages.isNotEmpty() || uiState.streamingText.isNotEmpty()) {
                scope.launch {
                    listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                }
            }
        }
        
        // 输入区
        MessageInput(
            enabled = uiState.status != "running",
            onSend = { text ->
                viewModel.sendMessage(text)
            },
            onAbort = {
                viewModel.abortRun()
            }
        )
    }
}

/**
 * 消息项
 */
@Composable
fun MessageItem(
    message: Message,
    isStreaming: Boolean = false
) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 角色标签
                Text(
                    text = when (message.role) {
                        "user" -> "你"
                        "assistant" -> if (isStreaming) "AI..." else "AI"
                        "system" -> "系统"
                        else -> message.role
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (message.role) {
                        "user" -> MaterialTheme.colorScheme.onPrimaryContainer
                        "assistant" -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> Color.Gray
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 消息内容
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 思考过程项
 */
@Composable
fun ThinkingItem(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4)  // 淡黄色背景
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFF57C00)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "思考中...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF57C00)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5D4037)
            )
        }
    }
}

/**
 * 工具调用项
 */
@Composable
fun ToolCallItem(toolCall: com.openclaw.mobile.models.ToolCall) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)  // 淡蓝色背景
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "工具: ${toolCall.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1976D2)
                )
            }
            
            if (toolCall.result != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "结果: ${toolCall.result.take(100)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0D47A1),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 消息输入
 */
@Composable
fun MessageInput(
    enabled: Boolean,
    onSend: (String) -> Unit,
    onAbort: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                enabled = enabled,
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (!enabled) {
                // 中止按钮
                FilledIconButton(
                    onClick = onAbort,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, "中止")
                }
            } else {
                // 发送按钮
                FilledIconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSend(text)
                            text = ""
                        }
                    },
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, "发送")
                }
            }
        }
    }
}

// 导入缺失的类
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontWeight
