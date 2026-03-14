#!/bin/bash

# OpenClaw Android 构建脚本

echo "🔨 开始构建 OpenClaw Android..."

# 检查 Gradle wrapper
if [ ! -f "./gradlew" ]; then
    echo "❌ Gradle wrapper 不存在"
    exit 1
fi

# 给予执行权限
chmod +x ./gradlew

# 清理
echo "🧹 清理旧的构建..."
./gradlew clean

# 构建 Debug APK
echo "📦 构建 Debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ 构建成功！"
    echo "📱 APK 位置: mobile/build/outputs/apk/debug/mobile-debug.apk"
else
    echo "❌ 构建失败"
    exit 1
fi
