# OpenClaw Mobile & Wear Architecture

## 基于 OpenClaw Studio 源码分析

### 核心功能模块

#### 1. GatewayClient (WebSocket 客户端)
```kotlin
interface GatewayClient {
    suspend fun <T> call(method: String, params: Any): T
    fun onEvent(handler: (EventFrame) -> Unit): () -> Unit
    fun onGap(handler: (GapInfo) -> Unit): () -> Unit
}
```

**关键方法**:
- `call("chat.send", params)` - 发送消息
- `call("chat.history", params)` - 获取历史
- `call("chat.abort", params)` - 中止运行

**RPC 协议**:
```json
{
  "type": "req",
  "id": "uuid",
  "method": "chat.send",
  "params": {
    "sessionKey": "session-id",
    "message": "user message"
  }
}
```

**响应格式**:
```json
{
  "type": "res",
  "id": "uuid",
  "result": { ... }
}
```

**事件格式** (流式响应):
```json
{
  "type": "event",
  "event": {
    "type": "assistant_text_delta",
    "delta": "text chunk"
  }
}
```

#### 2. AgentState (状态管理)
```kotlin
data class AgentState(
    val agentId: String,
    val name: String,
    val sessionKey: String,
    val status: AgentStatus,
    val outputLines: List<String>,
    val streamText: String?,
    val thinkingTrace: String?,
    val model: String?
)

enum class AgentStatus {
    IDLE, RUNNING, ERROR
}
```

#### 3. ChatOperation (聊天操作)
```kotlin
class ChatOperation(
    private val client: GatewayClient
) {
    suspend fun sendMessage(
        sessionKey: String,
        message: String
    ): Result<Unit>
    
    suspend fun loadHistory(
        sessionKey: String,
        limit: Int
    ): Result<List<Message>>
    
    suspend fun abortRun(sessionKey: String): Result<Unit>
}
```

### UI 适配

#### 手机端 (Mobile)
- 完整聊天界面
- Agent 列表侧边栏
- 设置面板
- Markdown 渲染
- 执行批准处理

#### 手表端 (Wear)
- 简化聊天界面
- 单一 Agent
- 文字输入
- 基础消息显示

### 项目结构

```
openclaw-android/
├── shared/           # 共享模块
│   ├── gateway/      # WebSocket 客户端
│   ├── models/       # 数据模型
│   ├── operations/   # 业务操作
│   └── state/        # 状态管理
├── mobile/           # 手机应用
│   ├── ui/           # 界面组件
│   ├── navigation/   # 导航
│   └── features/     # 功能模块
└── wear/             # 手表应用
    ├── ui/           # 界面组件（简化）
    └── features/     # 功能模块（简化）
```

### 技术栈

- **Kotlin** - 编程语言
- **Jetpack Compose** - UI 框架
- **OkHttp** - WebSocket 客户端
- **Kotlinx Coroutines** - 异步处理
- **Kotlinx Serialization** - JSON 序列化
- **Coil** - 图片加载（手机端）
- **Jetpack Wear Compose** - 手表 UI

### 关键实现细节

#### WebSocket 连接
```kotlin
class GatewayClientImpl(
    private val url: String
) : GatewayClient, WebSocketListener() {
    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private val pendingCalls = ConcurrentHashMap<String, CompletableDeferred<Any>>()
    
    override fun onMessage(webSocket: WebSocket, text: String) {
        val frame = Json.decodeFromString<Frame>(text)
        when (frame.type) {
            "res" -> handleResponse(frame)
            "event" -> handleEvent(frame)
            "error" -> handleError(frame)
        }
    }
}
```

#### 流式响应处理
```kotlin
suspend fun sendMessageWithStreaming(
    sessionKey: String,
    message: String,
    onDelta: (String) -> Unit
): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            client.call<Unit>("chat.send", mapOf(
                "sessionKey" to sessionKey,
                "message" to message
            ))
            
            // 事件监听会自动处理流式响应
            // onEvent { event ->
            //     if (event.event.type == "assistant_text_delta") {
            //         onDelta(event.event.delta)
            //     }
            // }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

#### 设备适配
```kotlin
// 手机端
@Composable
fun MobileChatScreen() {
    Row {
        FleetSidebar()  // Agent 列表
        ChatPanel()     // 聊天面板
    }
}

// 手表端
@Composable
fun WearChatScreen() {
    Column {
        ChatPanel()     // 简化版聊天面板
    }
}
```

### 下一步

1. ✅ 分析源码
2. ⏳ 创建 shared 模块
3. ⏳ 实现 GatewayClient
4. ⏳ 实现 mobile 应用
5. ⏳ 实现 wear 应用
6. ⏳ 测试和调试
