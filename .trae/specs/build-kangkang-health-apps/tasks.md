# Tasks

> 共享约束：两个 App 的「体质计算分值」「体质 desc 文案」「节气表」「菜谱筛选规则」必须完全一致。实现前先读 `spec.md` 与 `12/02_菜谱资料/三系菜谱总表_288道.csv`。

- [x] Task 1: 准备共享菜谱数据与约定
  - [x] 将 `12/02_菜谱资料/三系菜谱总表_288道.csv` 复制到 `windows-app/data/recipes.csv` 与 `android-app/app/src/main/assets/recipes.csv`
  - [x] 确认 CSV 编码（含 BOM）用 utf-8-sig 读取，字段名对齐（菜名/菜系/当令节气/主料/食养特点/体质宜忌/忌口提示/做法难度/成本档/文化出处/做法简述）

- [x] Task 2: Windows 版（Python + PyInstaller → 单文件 .exe）
  - [x] Task 2.1: 工程骨架 + 数据层（读 recipes.csv、节气计算、体质计算模块）
  - [x] Task 2.2: 问卷 UI（大按钮表单，7 题，含年龄段，普通话）
  - [x] Task 2.3: 画像摘要（DeepSeek 调用 + 本地模板回退）+ 确认/重填
  - [x] Task 2.4: 菜谱筛选 + 推荐话术（DeepSeek + 回退）
  - [x] Task 2.5: PyInstaller 打包单文件 exe 并验证可运行（提供 build.bat，exe 需在 Windows 构建）

- [x] Task 3: Android 版（Kotlin + Compose → APK，minSdk 31）
  - [x] Task 3.1: 工程骨架（Gradle、minSdk 31、Compose）+ 读 assets recipes.csv + 节气/体质计算
  - [x] Task 3.2: 问卷 UI（Compose 大按钮，7 题，含年龄段，普通话）
  - [x] Task 3.3: 画像摘要（DeepSeek 网络调用 + 本地回退）+ 确认/重填
  - [x] Task 3.4: 菜谱筛选 + 推荐话术
  - [x] Task 3.5: 用本机 Gradle 8.13、JDK 17、SDK 36 离线构建 debug APK，并在 Android 16 模拟器安装和验证问卷、画像、推荐与重试流程

- [x] Task 4: 两端同步
  - [x] 问卷增加年龄段，偏好和忌口顺延为第 6、7 题
  - [x] 问卷、画像和推荐页均提供重新开始问卷入口
  - [x] 显示常见 Markdown 排版，避免显示原始 `**` 标记
  - [x] 重试及节气切换后忽略旧的异步生成结果

# Task Dependencies
- Task 2 依赖 Task 1（数据就位）
- Task 3 依赖 Task 1（数据就位）
- Task 2 与 Task 3 相互独立，可并行
