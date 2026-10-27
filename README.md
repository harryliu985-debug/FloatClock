# 悬浮时钟 (FloatClock)

一个极简的 Android 系统级悬浮时钟：北京时间显示精确到十分之一秒，悬浮在所有应用之上，可拖动到任意位置。

## 功能

- **系统级悬浮窗**：使用 `TYPE_APPLICATION_OVERLAY`，悬浮在所有应用之上（需"显示在其他应用上层"权限）
- **北京时间**：固定 `Asia/Shanghai` 时区，格式 `16:39:29.4`
- **十分之一秒精度**：每 100ms 刷新一次，并对齐到下一个 100ms 边界（留 5ms 余量），保证十分位平滑跳变
- **拖动移动 / 轻点关闭**：拖动悬浮窗调整位置；位移小于 12px 视为轻点，直接关闭
- **前台服务常驻**：`specialUse` 前台服务 + 低优先级常驻通知，尽量不被系统回收；被杀后 `START_STICKY` 自动重启
- **通知栏快捷停止**：常驻通知带"停止"按钮，不用回到 App

## 技术栈

- Kotlin + Jetpack Compose (Material 3)
- minSdk 24（Android 7.0+），targetSdk / compileSdk 37
- 悬浮窗由原生 `TextView` + `WindowManager` 实现（Compose 只做控制页）

## 使用

1. 安装并打开 App，授予 **显示在其他应用上层** 权限（会自动跳转系统授权页）
2. 回到 App，点 **开始悬浮**，时钟即悬浮在屏幕左上区域
3. 拖动可移动位置，轻点悬浮窗或通知栏"停止"按钮即可关闭

## 构建

```bash
./gradlew assembleDebug    # debug APK
./gradlew assembleRelease  # release APK（当前使用 debug 签名）
```

## 项目结构

```
app/src/main/java/scenic/rd/util01/
├── MainActivity.kt        # 控制页（Compose）：权限状态、开始/停止
├── FloatClockService.kt   # 前台服务：悬浮窗创建、拖动/轻点、100ms 对齐刷新
├── BeijingTime.kt         # 北京时间格式化（十分之一秒）
└── ui/theme/              # Compose 主题
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `SYSTEM_ALERT_WINDOW` | 悬浮窗 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | 前台服务保活 |
| `POST_NOTIFICATIONS` | 常驻通知（Android 13+，不授不影响悬浮窗本身） |
