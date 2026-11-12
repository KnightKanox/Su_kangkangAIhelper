# 康康食养AI

Windows 桌面版与 Android 版共享一套体质问卷和节气粤菜推荐流程。菜谱数据来自 [三系菜谱总表](12/02_菜谱资料/三系菜谱总表_288道.csv)，包含广府菜、潮汕菜、客家菜共 288 道。

## 使用流程

启动进入**首页**（三 Tab：首页 / 食养 / 我的），首页完整展示问候语并提供状态化入口。从首页开始 7 题问卷：怕冷怕热、出汗、口干、大便、年龄段、饮食偏好、忌口。前 4 题用于本地计算体质；年龄段供画像和推荐文案参考，不用于疾病判断或改变菜谱筛选。第 6、7 题可以留空。

确认健康画像后进入「食养」，按当前节气展示 3 张结构化菜谱卡（菜名、菜系、主料、做法、推荐理由、忌口提示），也可以手动切换到其他节气。卡片字段来自本地菜谱 CSV，DeepSeek 只生成每道菜的推荐理由；AI 不可用时使用本地理由并保留固定免责声明。

食养页可「换一组菜」浏览后续候选；含忌口食材的主料会从候选中排除。「我的」可直接修改饮食偏好与忌口，保存后刷新画像摘要及推荐，无需重做前 5 题。

四季节气图、三菜系插画及状态图用于食养页与空态。应用内循环播放「拂晓车站」，顶部小按钮可静音；Android 进入后台时暂停播放。

问卷、画像、食养空态与我的页都可重新填写问卷，无需重启应用。生成中的旧请求不会在重试或切换节气后覆盖当前内容。画像摘要中的常见 Markdown 标记会按格式显示。

## 工程与构建

| 平台 | 位置 | 构建产物 |
| --- | --- | --- |
| Windows | [windows-app](windows-app/README.md) | `windows-app/dist/KangKangShiYangAI.exe` |
| Android | [android-app](android-app/README.md) | `android-app/app/build/outputs/apk/debug/app-debug.apk` |

功能细节见[功能规格](.trae/specs/build-kangkang-health-apps/spec.md)，已完成项见[验收清单](.trae/specs/build-kangkang-health-apps/checklist.md)。

美术素材：原始图片在仓库外的 `art/`，运行资源分别在 `android-app/app/src/main/res/drawable-nodpi`（及 `mipmap-*` 图标）与 `windows-app/assets/`；裁切坐标、派生尺寸与用途见 [assets/README.md](windows-app/assets/README.md)。

背景音乐原件在仓库外的 `media/`；原文件虽然以 `.mp3` 命名，实际为 MP4/AAC。Android 使用保留原始 AAC 音轨的 `res/raw/fuxiao_station.m4a`；Windows 使用从原件直接解码的 `assets/fuxiao_station.wav`，通过 pygame-ce 循环播放并静音。两端均避免再次有损转码；Windows exe 因 PCM 音频增至约 77.7MB。

Windows 版使用 Python、tkinter 与 pygame-ce，打包使用 PyInstaller。Android 版使用 Kotlin 与 Jetpack Compose，最低支持 Android 12（API 31），当前面向 Android 16（API 36）。具体运行和构建命令见各工程 README。本机工具链记录见工作区的 `agent.md` 和 `本机工具链全览与AI分步教学指南.md`；GitHub 仓库根目录也附有 `agent.md`。

## DeepSeek 密钥

两端均使用 `DEEPSEEK_API_KEY` 环境变量。Windows 在启动前设置；Android 在构建前设置。未设置时应用显示本地回退文案。Android 构建会把密钥写入 APK，请勿公开分发带真实密钥的 APK；正式分发应改用服务端代理。

推荐仅作食养参考，不构成医疗建议。
