# 康康食养AI

面向社区居民的体质问卷与节气粤菜推荐应用，提供 Windows 桌面版和 Android 版。菜谱数据包含 288 道广府、潮汕及客家菜。

## 工程

- `windows-app/`：Python 标准库与 tkinter。七题问卷包含年龄段选择，可随时重新开始；画像和推荐支持常见 Markdown 格式。
- `android-app/`：Kotlin 与 Jetpack Compose，最低 Android 12，面向 Android 16（API 36）。
- `12/02_菜谱资料/`：菜谱源数据与说明。

具体运行和构建步骤见各工程的 README。

## DeepSeek 配置

两个工程均可从 `DEEPSEEK_API_KEY` 环境变量读取密钥；未配置时使用本地回退文案。Windows 在运行前设置，Android 在构建前设置。Android 构建会把密钥写入 APK，因此不要公开发布含真实密钥的 APK；正式分发应改用服务端代理。

食养推荐仅供参考，不构成医疗建议。
