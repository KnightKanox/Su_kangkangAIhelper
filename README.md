# 康康食养AI

Windows 桌面版与 Android 版共享一套体质问卷和节气粤菜推荐流程。菜谱数据来自 [三系菜谱总表](12/02_菜谱资料/三系菜谱总表_288道.csv)，包含广府菜、潮汕菜、客家菜共 288 道。

## 使用流程

启动后依次回答 7 题：怕冷怕热、出汗、口干、大便、年龄段、饮食偏好、忌口。前 4 题用于本地计算体质；年龄段供画像和推荐文案参考，不用于疾病判断或改变菜谱筛选。第 6、7 题可以留空。确认健康画像后，应用按当前节气推荐菜谱，也可以手动切换到其他节气。

问卷、画像和推荐页都可点「重新开始问卷」，无需重启应用。生成中的旧请求不会在重试或切换节气后覆盖当前内容。画像和推荐中的常见 Markdown 标记会按格式显示。DeepSeek 不可用时使用本地回退文案。

## 工程与构建

| 平台 | 位置 | 构建产物 |
| --- | --- | --- |
| Windows | [windows-app](windows-app/README.md) | `windows-app/dist/KangKangShiYangAI.exe` |
| Android | [android-app](android-app/README.md) | `android-app/app/build/outputs/apk/debug/app-debug.apk` |

功能细节见[功能规格](.trae/specs/build-kangkang-health-apps/spec.md)，已完成项见[验收清单](.trae/specs/build-kangkang-health-apps/checklist.md)。

Windows 版使用 Python 标准库与 tkinter，打包使用 PyInstaller。Android 版使用 Kotlin 与 Jetpack Compose，最低支持 Android 12（API 31），当前面向 Android 16（API 36）。具体运行和构建命令见各工程 README。本机工具链记录见工作区的 `agent.md` 和 `本机工具链全览与AI分步教学指南.md`；GitHub 仓库根目录也附有 `agent.md`。

## DeepSeek 密钥

两端均使用 `DEEPSEEK_API_KEY` 环境变量。Windows 在启动前设置；Android 在构建前设置。未设置时应用显示本地回退文案。Android 构建会把密钥写入 APK，请勿公开分发带真实密钥的 APK；正式分发应改用服务端代理。

推荐仅作食养参考，不构成医疗建议。
