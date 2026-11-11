# 康康食养AI：项目与构建上下文

## 项目范围

- `RESET/windows-app/`：Python 标准库 + tkinter 桌面程序；`logic.py` 放体质、节气、推荐与 DeepSeek 回退逻辑，`app.py` 放界面。Windows 问卷现有 7 题，含可选年龄段；各阶段可重新开始问卷。
- `RESET/android-app/`：Kotlin + Jetpack Compose 应用；最低 Android API 31，当前 `compileSdk`/`targetSdk` 为 36（Android 16）。Android 问卷已同步为 7 题，含年龄段单选；问卷、画像、推荐页均可重新开始，AI 文案会渲染常见 Markdown 格式。
- `RESET/12/02_菜谱资料/三系菜谱总表_288道.csv` 是菜谱源数据。两个应用内的 `recipes.csv` 应与源文件保持一致（288 行、15 列）。
- `RESET/.trae/specs/build-kangkang-health-apps/spec.md` 是功能规格；修改体质规则、节气表或推荐规则时，核对两个平台的一致性。
- `RESET/Healthy_profiling-draft.yaml` 是原扣子工作流参考，不是应用运行依赖。

## 本机工具链

以工作区根目录的 `本机工具链全览与AI分步教学指南.md` 为环境参考，执行前以当前机器状态为准。当前已核对：Python 3.14.7、JDK 17（`E:\Android\tools\jdk-17.0.21.0.2+1`）、Android SDK（`E:\Android\Sdk`）。Android 工程使用 Gradle 8.13 Wrapper、AGP 8.13.2、SDK API 36 和 Build Tools 36.0.0；本机对应工具链已验证可用。

## 构建命令（PowerShell）

Windows：

```powershell
Set-Location 'F:\my\SuLinMei\RESET\windows-app'
py -3 -m venv .venv
& '.\.venv\Scripts\python.exe' -m pip install pyinstaller
& '.\.venv\Scripts\python.exe' -m PyInstaller --noconfirm --clean --onefile --windowed --name KangKangShiYangAI --add-data 'data\recipes.csv:data' app.py
# 产物：dist\KangKangShiYangAI.exe
```

Android：

```powershell
Set-Location 'F:\my\SuLinMei\RESET\android-app'
$env:ANDROID_HOME = 'E:\Android\Sdk'
.\gradlew.bat assembleDebug --offline --no-daemon --console=plain
# 产物：app\build\outputs\apk\debug\app-debug.apk
```

首次准备依赖时去掉 `--offline`；若 Wrapper 下载超时，可用本机 Gradle 8.13。项目仓库已设置阿里云 Maven 镜像及官方仓库后备。当前 Android 依赖已缓存，以上离线构建命令已验证。构建产物存在不代表已经完成设备安装或界面验收。

## 开发注意

- 目前根目录不是 Git 仓库；提交、差异检查前先确认项目实际的版本管理位置。
- 两端源码已改为从 `DEEPSEEK_API_KEY` 环境变量读取 DeepSeek 密钥；未设置时使用本地回退文案。Android 构建会把提供的密钥写入 APK，公开分发前应改用服务端代理。旧密钥曾出现在源码中，应轮换。
- 健康内容仅作食养参考，不构成医疗建议；不要把本地推荐规则解释成诊断。
