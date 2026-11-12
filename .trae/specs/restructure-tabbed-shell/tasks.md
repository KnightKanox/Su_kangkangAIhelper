# Tasks

> 共享约束：先读 `spec.md`。不改问卷题目/选项、`ConstitutionCalculator`、`RecommendationEngine`、`SolarTerms`、`recipes.csv`、节气表与忌口规则；不新增 Gradle 依赖与图片资源；问卷答案、体质计算与筛选口径必须两端一致。

- [x] Task 1: Android Tab 外壳与状态模型
  - [x] SubTask 1.1: `AppViewModel` 新增 `enum class Tab { HOME, FOOD, PROFILE }` 与 `tab`；二级页仍用 `Screen`（`QUESTIONNAIRE`、`SUMMARY`），移除 `GREETING`、`RECOMMEND` 的使用
  - [x] SubTask 1.2: 新增 `selectTab`、`openQuestionnaire`、`openSummary`、`backToTab`；问卷完成→画像、确认→食养 Tab；Tab 切换不清空任何已填状态
  - [x] SubTask 1.3: 推荐流程改为「本地筛选 → 卡片事实 + AI 理由」：新增 `recipeReasons: Map<String, String>`，生成一次 `菜名｜理由` 请求并按菜名回填；失败/未匹配走本地理由（节气 + `healthFeatures` 首句 + 体质）
  - [x] SubTask 1.4: 定义固定免责声明常量与「未完成问卷 / 无结果 / 加载失败」状态标记，确保旧请求不覆盖新页面（沿用 generation）

- [x] Task 2: Android 共享组件
  - [x] SubTask 2.1: `Components.kt` 新增 `BottomTabBar`（首页/食养/我的，文字 + 选中 3dp 指示条，高 64，`insets` 留白）
  - [x] SubTask 2.2: 新增 `RecipeCard`（菜名 22sp Serif / 菜系标签 / 主料 / 做法摘要 1–2 行 / 推荐理由 / 忌口提示可选行 / 「食养参考」标签 / 「展开更多」切换）
  - [x] SubTask 2.3: 新增 `EmptyState`（标题、说明、主按钮、可选次按钮）与 `ProfileCard`（体质名 + 描述 + 标签，供首页与我的复用）

- [x] Task 3: Android 页面重构
  - [x] SubTask 3.1: 新增 `HomeScreen.kt`：品牌字标 + 完整问候语卡 + 今日节气 + 按状态切换的主 CTA + 体质卡 + 推荐预览（第一道，点击跳食养）
  - [x] SubTask 3.2: 新增 `FoodScreen.kt`：节气胶囊（沿用底部弹层）+ 3 张 `RecipeCard` + 固定免责声明 + 三种状态（未完成问卷/无结果/加载失败）+ 重新填写入口
  - [x] SubTask 3.3: 新增 `ProfileScreen.kt`：体质卡 + 可折叠画像摘要（Markdown）+ 问卷摘要（年龄段/偏好/忌口，未提供显示「未提供」）+ 重新填写/查看完整画像
  - [x] SubTask 3.4: `AppRoot.kt` 改为 Tab 外壳：一级页显示 `BottomTabBar`，问卷/画像二级页全屏且可返回；保留 `AnimatedContent` 转场与 `safeDrawingPadding`
  - [x] SubTask 3.5: `MarkdownText` 仅服务画像摘要；推荐理由按纯文本 18sp 展示，不解析 Markdown

- [x] Task 4: Android 版本与验证
  - [x] SubTask 4.1: `app/build.gradle.kts`：`versionCode 3`、`versionName "1.2.0"`
  - [x] SubTask 4.2: 离线构建 `.\gradlew.bat assembleDebug --offline --no-daemon --console=plain` 通过，aapt 校验版本
  - [x] SubTask 4.3: 模拟器逐页截图（首页首启/问卷/画像/食养 3 卡/展开更多/我的/未完成问卷空态/切节气），保存到 `RESET\.screenshots\`

- [x] Task 5: Windows 三 Tab 与卡片化
  - [x] SubTask 5.1: `app.py` 增加顶部导航（首页/食养/我的）与页面容器切换，Tab 切换保留答案与推荐状态
  - [x] SubTask 5.2: 首页（问候卡 + 今日节气 + 状态化 CTA + 体质卡 + 推荐预览）、食养（节气卡片 + 3 张菜谱卡 + 免责声明 + 空态）、我的（体质卡 + 画像摘要 + 问卷摘要 + 重新填写）
  - [x] SubTask 5.3: `logic.py` 仅新增「按菜名生成/回填推荐理由」的辅助函数与本地理由模板；`recommend()`、`calculate_constitution()` 不得改动
  - [x] SubTask 5.4: `py -3 -m py_compile app.py logic.py` 通过；脚本冒烟走通 首页→问卷→画像→食养→切节气→展开更多→我的→重新填写

- [x] Task 6: 跨端一致性与回归
  - [x] SubTask 6.1: 同输入（怕冷/容易出汗/经常口干/偏软不成形，无忌口）同节气下，两端体质均为「阳虚」且 3 道菜名一致
  - [x] SubTask 6.2: 核对空态文案、免责声明、CTA 规则、Tab 名称两端一致；确认问卷题目与选项未变

- [x] Task 7: 文档与修改日志
  - [x] SubTask 7.1: 更新 `README.md` 与 `RESET\README.md` 的流程描述（三 Tab、结构化卡片）
  - [x] SubTask 7.2: 在 `修改日志.md` 顶部追加 v1.2.0 记录

# Task Dependencies
- Task 2 依赖 Task 1
- Task 3 依赖 Task 1、Task 2
- Task 4 依赖 Task 3
- Task 5 依赖 spec 与现有 `app.py`/`logic.py`，可与 Task 1–4 并行
- Task 6 依赖 Task 4、Task 5
- Task 7 依赖 Task 6
