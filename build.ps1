# OpenClaw Android 构建脚本 (Windows)

Write-Host "🔨 开始构建 OpenClaw Android..." -ForegroundColor Green

# 检查 Gradle wrapper
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Host "❌ Gradle wrapper 不存在" -ForegroundColor Red
    exit 1
}

# 清理
Write-Host "🧹 清理旧的构建..." -ForegroundColor Yellow
.\gradlew.bat clean

# 构建 Debug APK
Write-Host "📦 构建 Debug APK..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ 构建成功！" -ForegroundColor Green
    Write-Host "📱 APK 位置: mobile\build\outputs\apk\debug\mobile-debug.apk" -ForegroundColor Cyan
} else {
    Write-Host "❌ 构建失败" -ForegroundColor Red
    exit 1
}
