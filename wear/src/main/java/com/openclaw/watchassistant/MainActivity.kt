package com.openclaw.watchassistant

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.openclaw.shared.gateway.GatewayClient
import com.openclaw.shared.operations.ChatOperation
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * OpenClaw 手表应用
 * 基于 OpenClaw Studio 功能模块
 */
class MainActivity : ComponentActivity() {

    private val gatewayClient = GatewayClient("wss://wjwly140920.eu.org")
    private val chatOperation = ChatOperation(gatewayClient)
    
    private var sessionKey by mutableStateOf(UUID.randomUUID().toString())

    companion object {
        private const val TAG = "WatchAssistant"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: 启动")

        setContent {
            WatchAppTheme {
                MainScreen()
            }
        }
        
        // 连接 Gateway
        kotlinx.coroutines.GlobalScope.launch {
            try {
                gatewayClient.connect()
                gatewayClient.setConnectionListener { connected ->
                    Log.d(TAG, if (connected) "已连接" else "已断开")
                }
            } catch (e: Exception) {
                Log.e(TAG, "连接失败: ${e.message}")
            }
        }
    }

    @Composable
    fun WatchAppTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            content = content
        )
    }

    @Composable
    fun MainScreen() {
        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()
        
        // 状态
        val messages by chatOperation.messages.collectAsState()
        val isStreaming by chatOperation.isStreaming.collectAsState()
        val streamText by chatOperation.streamText.collectAsState()
        var inputText by remember { mutableStateOf("") }
        var connectionStatus by remember { mutableStateOf("连接中...") }
        
        // 监听连接状态
        DisposableEffect(Unit) {
            val listener: (Boolean) -> Unit = { connected ->
                connectionStatus = if (connected) "已连接 ✓" else "已断开"
            }
            gatewayClient.setConnectionListener(listener)
            onDispose { }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 标题卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.2f))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "OpenClaw",
                            style = MaterialTheme.typography.title1,
                            color = MaterialTheme.colors.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "手表助手",
                            style = MaterialTheme.typography.caption2,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    }
                }

                // 状态卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.DarkGray.copy(alpha = 0.2f))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                isStreaming -> "⏳ AI 思考中..."
                                else -> connectionStatus
                            },
                            style = MaterialTheme.typography.body2,
                            color = when {
                                isStreaming -> Color.Yellow
                                connectionStatus.contains("断开") -> Color.Red
                                else -> Color(0xFF81C784)
                            },
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }

                // 消息列表
                messages.forEach { message ->
                    MessageCard(
                        role = message.role,
                        content = message.content,
                        timestamp = message.timestamp
                    )
                }
                
                // 流式文本
                if (streamText.isNotEmpty()) {
                    MessageCard(
                        role = "assistant",
                        content = streamText,
                        timestamp = null,
                        isStreaming = true
                    )
                }

                // 输入框卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E88E5).copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "💬 输入消息",
                            style = MaterialTheme.typography.caption2,
                            color = Color(0xFF64B5F6),
                            fontSize = 10.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text(
                                        "输入你的问题...",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                if (inputText.isNotEmpty()) {
                                    scope.launch {
                                        chatOperation.sendMessage(sessionKey, inputText)
                                        inputText = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isStreaming || inputText.isEmpty())
                                    Color.Gray
                                else
                                    MaterialTheme.colors.primary
                            ),
                            enabled = !isStreaming && inputText.isNotEmpty()
                        ) {
                            Text(
                                text = if (isStreaming) "处理中..." else "发送",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun MessageCard(
        role: String,
        content: String,
        timestamp: Long?,
        isStreaming: Boolean = false
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when (role) {
                            "user" -> Color(0xFF1E88E5).copy(alpha = 0.3f)
                            else -> Color(0xFF43A047).copy(alpha = 0.3f)
                        }
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (role) {
                            "user" -> "💬 你说"
                            else -> if (isStreaming) "⏳ AI 回复中..." else "✨ AI 回复"
                        },
                        style = MaterialTheme.typography.caption2,
                        color = when (role) {
                            "user" -> Color(0xFF64B5F6)
                            else -> Color(0xFF81C784)
                        },
                        fontSize = 10.sp
                    )
                    
                    timestamp?.let {
                        Text(
                            text = formatTime(it),
                            style = MaterialTheme.typography.caption3,
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = content,
                    style = MaterialTheme.typography.body2,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    override fun onDestroy() {
        super.onDestroy()
        gatewayClient.disconnect()
    }
}
