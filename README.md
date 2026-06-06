# CaptchaArena

Android CAPTCHA 攻防测试平台。集成多种主流验证码类型，用于研究和测试验证码破解方案。

## 功能

- 🧩 **滑块拼图验证码** - 自实现，含轨迹记录和反 bot 检测
- 📝 **文字点选验证码** - 开发中
- 🔄 **旋转验证码** - 开发中
- 📊 **轨迹分析** - 实时显示触摸轨迹数据（坐标、速度、压力、偏移）

## 下载

从 [Releases](https://github.com/briqt/captcha-arena/releases) 页面下载最新 APK，或从 Actions artifacts 获取开发版本。

## 构建

```bash
./gradlew assembleDebug
```

## 技术栈

- Kotlin + Jetpack Compose
- 自定义 View 实现验证码
- GitHub Actions 自动打包

## License

MIT
