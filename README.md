# OpenClaw Android & Wear OS 应用

完整的 OpenClaw 移动端和手表端应用，包含 OpenClaw Studio 的所有功能。

## 🎯 功能特性

### ✅ 核心功能
- WebSocket 连接到 OpenClaw Gateway
- Agent 列表和管理
- 流式聊天界面
- 消息历史

### ✅ Agent 管理
- 列出所有 Agent
- 切换 Agent
- 创建新 Agent
- 删除 Agent

### ✅ 聊天功能
- 流式响应显示
- 思考过程显示（可选）
- 工具调用可视化（可选）
- 消息输入和发送
- 中止运行
- 新建会话

### ✅ 设置控制
- 命令执行模式（Off/Ask/Auto）
- Web 访问开关
- 文件工具开关
- 显示设置

### 🚧 待完成
- 技能管理界面
- 自动化（定时任务）
- Personality 编辑
- Markdown 渲染
- 手表端优化

## 📱 构建 APK

### 前置要求

1. **JDK 8+**
2. **Android SDK** (API 34)
3. **Gradle 8.2** (已包含 wrapper)

### 构建步骤

#### Windows:
```powershell
cd C:\Users\wj\.openclaw\workspace\openclaw-android
.\build.ps1
```

#### macOS/Linux:
```bash
cd /path/to/openclaw-android
chmod +x build.sh
./build.sh
```

#### 手动构建:
```bash
# 清理
./gradlew clean

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

### APK 位置

- **Debug**: `mobile/build/outputs/apk/debug/mobile-debug.apk`
- **Release**: `mobile/build/outputs/apk/release/mobile-release.apk`

## 📲 安装

### 通过 ADB

```bash
# 连接设备
adb devices

# 安装 APK
adb install mobile-debug.apk
```

### 通过 Android Studio

1. 打开 Android Studio
2. File → Open → 选择 `openclaw-android` 目录
3. 等待 Gradle 同步
4. Run → Run 'mobile'

## 🚀 使用指南

### 首次使用

1. 打开应用
2. 输入 Gateway URL（例如：`ws://127.0.0.1:18789`）
3. 如果需要，输入 Token
4. 点击"连接"

### 连接成功后

1. **聊天** - 直接与 Agent 对话
2. **Agents** - 查看、创建、切换 Agent
3. **设置** - 调整 Agent 配置

## 🏗️ 项目结构

```
openclaw-android/
├── shared/              # 共享模块（Android + Wear）
│   └── src/commonMain/
│       └── kotlin/com/openclaw/mobile/
│           ├── gateway/        # WebSocket 客户端
│           ├── models/         # 数据模型
│           ├── operations/     # API 操作
│           └── OpenClawClient.kt
│
├── mobile/              # 手机应用
│   └── src/main/
│       └── java/com/openclaw/mobile/
│           ├── MainActivity.kt
│           ├── viewmodels/     # 状态管理
│           └── ui/
│               ├── screens/    # 界面
│               └── theme/      # 主题
│
└── wear/                # 手表应用（待开发）
```

## 🔧 技术栈

- **Kotlin** - 编程语言
- **Jetpack Compose** - UI 框架
- **Material Design 3** - 设计系统
- **Kotlinx Coroutines** - 异步处理
- **Kotlinx Serialization** - JSON 序列化
- **OkHttp** - WebSocket 客户端
- **Jetpack Navigation** - 导航
- **Jetpack Lifecycle** - 生命周期管理

## 🐛 调试

### 查看日志

```bash
# 实时日志
adb logcat | grep OpenClaw

# 清除日志
adb logcat -c
```

### 常见问题

#### 1. 无法连接到 Gateway

- 确保 Gateway 正在运行
- 检查 URL 是否正确
- 如果使用 `ws://`，确保 Gateway 允许非 TLS 连接

#### 2. 构建失败

```bash
# 清理并重新构建
./gradlew clean build
```

#### 3. 依赖问题

```bash
# 刷新依赖
./gradlew build --refresh-dependencies
```

## 📝 开发指南

### 添加新功能

1. 在 `shared` 模块添加数据模型和操作
2. 在 `mobile` 模块创建 ViewModel
3. 创建 Compose 界面
4. 添加导航

### 代码风格

- 使用 Kotlin 官方代码风格
- 遵循 Material Design 3 指南
- 使用 StateFlow 管理状态

## 🔗 相关链接

- [OpenClaw 文档](https://docs.openclaw.ai)
- [OpenClaw Studio](https://github.com/openclaw/openclaw-studio)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

## 📄 许可证

MIT License

---

*创建时间: 2026-03-14*
*版本: 1.0.0*
