# 康康食养AI：项目上下文

- `windows-app/` 使用 Python 标准库和 tkinter。问卷含四道体质题、一道年龄段题和两道饮食题；体质规则位于 `logic.py`，界面位于 `app.py`。
- `android-app/` 使用 Kotlin 与 Jetpack Compose；`compileSdk`/`targetSdk` 为 36，`minSdk` 为 31。使用自带 Gradle 8.13 Wrapper 和 JDK 17。
- `12/02_菜谱资料/三系菜谱总表_288道.csv` 是菜谱源数据，两个应用内的副本应保持一致。
- Windows 构建：在 `windows-app/` 执行 `build.bat`。Android 构建：在 `android-app/` 执行 `./gradlew assembleDebug`（Windows 使用 `.\gradlew.bat assembleDebug`）。
- DeepSeek 密钥从 `DEEPSEEK_API_KEY` 环境变量读取；未提供时使用本地文案。不要提交密钥或含密钥的构建产物。Android APK 会包含构建时提供的密钥，公开分发前应改用服务端代理。
- 推荐内容仅作食养参考，不构成医疗建议。
