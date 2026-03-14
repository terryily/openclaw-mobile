package com.openclaw.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * 连接界面
 * 
 * 首次使用时显示，用于配置 Gateway 连接
 */
@Composable
fun ConnectionScreen(
    onConnect: (String, String?) -> Unit
) {
    var gatewayUrl by remember { mutableStateOf("ws://127.0.0.1:18789") }
    var token by remember { mutableStateOf("") }
    var useToken by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Text(
            text = "🦁 OpenClaw",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Mobile Client",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Gateway URL 输入
        OutlinedTextField(
            value = gatewayUrl,
            onValueChange = { gatewayUrl = it },
            label = { Text("Gateway URL") },
            placeholder = { Text("ws://127.0.0.1:18789") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Token 选项
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = useToken,
                onCheckedChange = { useToken = it }
            )
            Text("使用 Token 认证")
        }
        
        if (useToken) {
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Gateway Token") },
                placeholder = { Text("输入 token...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 连接按钮
        Button(
            onClick = { onConnect(gatewayUrl, token.ifEmpty { null }) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Link, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("连接", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 帮助信息
        Text(
            text = "确保 OpenClaw Gateway 正在运行",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 缺少的 icon 导入
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
