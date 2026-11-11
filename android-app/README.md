# 康康食养AI（Android 版）

面向社区老年居民的体质辨识 + 24 节气粤菜食养推荐应用（普通话话术）。
通过 6 道问卷采集体质，计算「九种体质」并生成健康画像摘要，再结合当前节气，
从 288 道广府菜 / 客家菜 / 潮汕菜菜谱中推荐 3 道时令菜。摘要与推荐话术由 DeepSeek
大模型生成，网络异常时自动回退为本地模板 / 纯文本菜谱。

构建时可通过 `DEEPSEEK_API_KEY` 环境变量提供 DeepSeek 密钥；未设置时应用使用本地回退文案。密钥会写入构建出的 APK，不应将含真实密钥的 APK 公开发布；正式分发宜改用服务端代理。

## 技术栈

- Kotlin + Jetpack Compose（Material3）
- minSdkVersion 31（Android 12+），compileSdk / targetSdk 36（Android 16）
- OkHttp + kotlinx.coroutines 联网（不可在主线程联网，已用 `Dispatchers.IO` 协程）
- 菜谱数据从 `app/src/main/assets/recipes.csv` 运行时读取（UTF-8，首行带 BOM，已处理）

## 版本矩阵

| 组件 | 版本 |
| --- | --- |
| Gradle | 8.13 |
| Android Gradle Plugin (AGP) | 8.13.2 |
| Kotlin | 1.9.24 |
| Compose Compiler (kotlinCompilerExtensionVersion) | 1.5.14 |
| Compose BOM | 2024.06.00 |
| JDK | 17 |
| Android SDK Platform / Build Tools | 36 / 36.0.0 |

## 构建步骤（Android Studio）

1. 用支持 Android 16 SDK 的 Android Studio 打开本目录 `android-app/`。
2. 首次打开会提示配置 Android SDK 路径，Studio 会自动生成 `local.properties`（内容为
   `sdk.dir=你的SDK路径`）；请确保已安装 **Android SDK Platform 36** 与 **Build-Tools 36.0.0**。
3. 等待 Gradle Sync 完成（会自动下载 Gradle 8.13 与依赖）。
4. 菜单 **Build → Build App Bundle(s) / APK(s) → Build APK(s)** 生成 APK；
   或命令行运行 `./gradlew assembleDebug`。

> 说明：本仓库已附带 `gradle/wrapper/gradle-wrapper.jar` 与精简版 `gradlew` / `gradlew.bat`。
> 若命令行提示 JDK 缺失，请安装 JDK 17 并配置 `JAVA_HOME`。
> 若 Android Studio 提示 Gradle Wrapper 缺失，可选择让 Studio 自动修复（File → Sync
> Project with Gradle Files），或改用本地 Gradle 8.13。

## 运行要求

- 真机 / 模拟器系统版本：Android 12（API 31）及以上。
- 需要联网权限（用于调用 DeepSeek；失败时不影响本地回退展示）。

## 目录结构

```
android-app/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.{jar,properties}
├── gradlew / gradlew.bat
├── README.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/recipes.csv
        ├── res/values/{strings.xml,themes.xml}
        └── java/com/kangkang/shiyangai/
            ├── MainActivity.kt
            ├── data/  Recipe.kt / RecipeRepository.kt / SolarTerms.kt
            ├── logic/ ConstitutionCalculator.kt / RecommendationEngine.kt
            ├── network/ DeepSeekClient.kt
            └── ui/    AppViewModel.kt / AppRoot.kt / theme/Theme.kt
```

## 流程说明

启动问候 → 6 题问卷（题 1–4 为大按钮单选，题 5–6 为可留空文本框）→ 计算九种体质 →
生成画像摘要（DeepSeek，失败回退本地模板）→ 确认 / 重新填写 → 按当前节气（可手动切换
24 节气任选）本地筛选 + 评分取前 3 道 → DeepSeek 生成推荐话术（失败回退为菜名+菜系+
主料+做法），结尾必附「本推荐为食养参考，不构成医疗建议」。
