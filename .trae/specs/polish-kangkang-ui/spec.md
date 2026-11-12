# 康康食养AI 前端页面美化 Spec

## Why
现有界面功能完整，但视觉停留在「能用的默认控件」：Android 仅有一套极简绿色配色，页面靠字号和间距区分层级；Windows 端虽为暖色，但按钮/卡片为 tkinter 原生样式，缺少统一的品牌感与进度反馈。面向广州市海珠区社区老人，界面需要在**更温润的食养气质**与**更清晰的视觉层级**之间取得平衡，让老人一眼看出「现在第几题、该点哪里、结果在哪里」。

## What Changes
- 两端统一「温润食养国风」设计语言：宣纸底、赭橙主色、墨绿点缀、墨色文字、细描边卡片；色板、圆角、间距、字号层级在两端保持同源。
- **仅视觉 + 结构 + 动效**：不改问卷题目、选项、体质规则、节气表、菜谱筛选、DeepSeek 提示词与流程顺序；不新增业务文案（仅新增装饰性标签「食养参考」与加载提示补充）。
- Android 页面结构升级：
  - 新增页头样式（小标签 + 主标题 + 细分隔线）。
  - 问卷页把「第 x / 7 题」文字升级为**7 段分段进度条 + 题号**，选项按钮改为可选中卡片（选中态：赭橙浅底 + 左侧色条 + 勾选图标），底部操作区固定。
  - 画像页新增「体质结果」Hero 卡（体质名 + 墨绿「食养参考」标签），摘要卡片化。
  - 推荐页把节气下拉框升级为「当前节气胶囊 + 24 节气底部弹层网格」，推荐正文卡片化并强化 Markdown 排版（标题、列表、引用竖线、免责声明弱化显示）。
  - 页面切换、选项选中、加载态与结果出现加入轻动效（180–260ms，fade + 8–12dp 位移）。
- Windows 页面同步：统一色板与字体层级，`ttk.Style` 定义主/次/幽灵按钮，候选答案改为带描边与 hover/press 反馈的卡片式按钮，问卷进度条用 Canvas 绘制并带填充动画，推荐页节气选择与按钮样式对齐 Android。
- 不新增 Gradle 依赖（保持离线可构建）、不新增图片资源；图标只用 `material-icons-core` 内置图标或 Canvas/Compose 自绘。
- 版本号：Android `versionName` 1.0 → **1.1.0**、`versionCode` 1 → 2（界面改版属于功能级更新）；Windows 无版本字段，产物名称保持不变。

## Impact
- Affected specs: 体质问卷采集、健康画像摘要、画像确认与重新开始问卷、节气确定、节气菜谱推荐、生成文案排版（以上均为**展示层**修改，行为不变）。
- Affected code:
  - `android-app/app/src/main/java/com/kangkang/shiyangai/ui/theme/Theme.kt`（色板、字体、形状）
  - `android-app/app/src/main/java/com/kangkang/shiyangai/ui/AppRoot.kt`（四个页面结构与动效）
  - `android-app/app/src/main/java/com/kangkang/shiyangai/ui/MarkdownText.kt`（排版增强，解析规则不变）
  - `android-app/app/src/main/java/com/kangkang/shiyangai/ui/Components.kt`（新增：页头、分段进度条、选项卡、节气胶囊、加载态）
  - `android-app/app/src/main/java/com/kangkang/shiyangai/MainActivity.kt`、`res/values/themes.xml`（状态栏/窗口底色与浅色系统栏）
  - `android-app/app/build.gradle.kts`（仅版本号）
  - `windows-app/app.py`（色板常量、按钮样式、问卷进度条、卡片式选项、页面布局）
- 不修改：`logic.py`、`recipes.csv`、`AndroidManifest.xml`、`AppViewModel.kt`、`DeepSeekClient.kt`、`ConstitutionCalculator.kt`、`RecommendationEngine.kt`。

## 设计令牌（两端共用）

| 用途 | 颜色 | 说明 |
| --- | --- | --- |
| 宣纸底 paper | `#F7F2E7` | 页面背景 |
| 卡片底 card | `#FFFDF7` | 内容卡片 |
| 赭橙主色 primary | `#B3541E` | 主按钮、选中态、强调数字 |
| 赭橙按压 | `#8F4218` | 按压/激活态（Windows active） |
| 赭橙浅底 primaryTint | `#F4E4D4` | 选中选项底色、标签底 |
| 墨绿 secondary | `#4E6E58` | 辅助标签、Markdown 标题 |
| 墨绿浅底 secondaryTint | `#E7EFE7` | 「食养参考」标签底 |
| 主文字 ink | `#3A3226` | 正文 |
| 次文字 inkSoft | `#74685A` | 辅助说明、加载提示、免责声明（纸底对比度 ≈4.8:1） |
| 弱文字 inkFaint | `#8C8275` | 占位符、引用等装饰性文字（仅用于 ≥18sp） |
| 描边 line | `#E4D9C3` | 卡片/输入框描边 |

- 圆角：卡片 16、选项 14、按钮 12；描边 1（Compose 为 1.dp）。
- 字号（sp/pt）：品牌标题 32/30、页面主标题 26/20、题干 26/16、正文 20/13、辅助 18/11，正文行高 ≥1.5 倍。
- 可点区域高度 ≥56（Android 现状 60/64 保持），主按钮高 60/64。
- 标题字族：Android `FontFamily.Serif`（系统宋体感），正文 `FontFamily.SansSerif`；Windows 标题沿用「Microsoft YaHei」加粗，避免引入新字体文件。

## ADDED Requirements

### Requirement: 统一设计令牌
两端 SHALL 使用上表色板、圆角与字号层级；同一语义（主按钮、选中项、卡片、描边、辅助文字）在两端取同色。Android 令牌集中在 `ui/theme/Theme.kt`，Windows 令牌集中在 `app.py` 顶部常量区。

#### Scenario: 颜色一致
- **WHEN** 对照两端的主按钮与选中项
- **THEN** 取色值相同（`#B3541E` / `#F4E4D4`），视觉上可辨认为同一产品

#### Scenario: 不引入外部资源
- **WHEN** 构建 Android APK 或 Windows exe
- **THEN** 无新增 Gradle 依赖、无新增位图资源，离线 `assembleDebug` 仍可用

### Requirement: 问卷进度可视化
问卷页 SHALL 展示「第 x / 7 题」文字与 7 段分段进度条，已完成段填充主色、当前段展示主色（可带 1 段过渡），未到达段为描边色；选择答案后立即更新。

#### Scenario: 进度推进
- **WHEN** 用户从第 1 题答到第 3 题
- **THEN** 进度条为 3 段填充、4 段留白，且文字显示「第 3 / 7 题」

#### Scenario: 重新开始
- **WHEN** 用户点「重新开始问卷」
- **THEN** 进度条回到第 1 题状态

### Requirement: 选项卡片与选中态
单选题选项 SHALL 以卡片呈现：未选为卡片底 + 描边，选中为主色浅底 + 主色描边 + 左侧 4dp 色条 + 勾选图标；文字保持可读字号，选项间距 ≥10。Windows 端使用等效实现：白底描边按钮，hover 变浅橙底，选中保留高亮描边。

#### Scenario: 选中反馈
- **WHEN** 用户点选某一选项
- **THEN** 该选项在 200ms 内呈现选中态，其余选项恢复未选态

### Requirement: 画像结果层级
画像页 SHALL 先展示「体质结果」Hero 卡（体质名 + 体质描述 + 墨绿「食养参考」标签），再展示 AI 摘要卡；摘要生成中展示带进度指示的加载态，完成后淡入正文。

#### Scenario: 摘要加载
- **WHEN** 摘要请求进行中
- **THEN** 摘要卡显示加载提示，确认按钮不可点；完成后正文淡入且「确认无误」恢复可点

### Requirement: 节气选择弹层
推荐页 SHALL 以「当前节气」胶囊展示节气，点击后弹出 24 节气网格（每行 3 个），选中项高亮并立即重新推荐；弹层可下滑/点击遮罩关闭。

#### Scenario: 切换节气
- **WHEN** 用户在弹层选择「霜降」
- **THEN** 弹层关闭、页头与胶囊显示「霜降」、推荐区进入加载态

### Requirement: 轻动效
Android SHALL 提供页面切换（fade + 8–12dp 上移/位移）、选项选中、加载态与结果淡入动效，单次时长 180–260ms；Windows SHALL 提供 hover/press 状态与进度条填充动画。两端均 MUST NOT 使用循环闪烁或大幅位移动效。

#### Scenario: 页面切换
- **WHEN** 用户从问卷完成进入画像页
- **THEN** 新页面在 ≤300ms 内完成淡入，无闪烁或内容跳动

### Requirement: 适老可读性与无障碍
两端 SHALL 保证：正文 ≥18sp（Windows ≥12pt）、正文与底色对比度 ≥7:1、主按钮文字与主色对比度 ≥4.5:1、装饰不遮挡文字、所有交互控件可点高度 ≥56；Android 支持系统字体缩放（使用 sp），状态栏/导航栏为浅色图标方案且不被内容覆盖（edge-to-edge + insets 留白）。

#### Scenario: 大字号
- **WHEN** 系统字体调到最大档
- **THEN** 文字不截断、按钮文字不溢出，页面可滚动

#### Scenario: 沉浸式安全区
- **WHEN** 在 Android 16 上运行
- **THEN** 内容不被状态栏/导航栏遮挡，状态栏图标为深色

## MODIFIED Requirements

### Requirement: 问候页
展示品牌字标「康康食养AI」、副标题「社区老人健康食养助手」、欢迎语卡片（纸卡 + 左侧主色竖条）与主色「开始」按钮；背景允许 Canvas 自绘极淡的同心弧装饰，不得影响文字对比度。

#### Scenario: 启动问候
- **WHEN** 应用启动
- **THEN** 固定问候语完整展示，主按钮位于首屏可点区域

### Requirement: 推荐页页头与节气区
推荐页页头 SHALL 采用「节气食养」小标签 + 「时令推荐」主标题；节气选择区展示当前节气（Android 为胶囊，Windows 为卡片内「当前节气：X」+ 下拉框 + 「重新推荐」），页面主体为推荐正文卡片与次级「重新开始问卷」按钮。

#### Scenario: 展示推荐页
- **WHEN** 用户确认画像后进入推荐页
- **THEN** 页头、当前节气与推荐卡片层级清晰，重新开始入口不与主内容争抢视觉

### Requirement: 推荐正文排版
Markdown 渲染结果 SHALL 使用墨绿标题、主色强调、列表缩进、引用左侧竖线与弱化免责声明；不得展示原始 `**`、`#` 等标记。解析规则与两端既有实现保持一致，不改解析范围。

#### Scenario: 标题与列表
- **WHEN** 模型返回含 `###` 标题与 `-` 列表的推荐文案
- **THEN** 标题加粗变色、列表带缩进符号，无原始标记外露

### Requirement: 重新开始问卷入口
问卷、画像、推荐页的「重新开始问卷」入口 SHALL 使用次级按钮样式（描边/幽灵），与主操作按钮形成明确主次；位置与可点范围不小于现状。

#### Scenario: 生成中重开
- **WHEN** 摘要或推荐生成中点击「重新开始问卷」
- **THEN** 立即回到第 1 题，旧请求结果不覆盖新页面（行为不变）

## REMOVED Requirements
无。
