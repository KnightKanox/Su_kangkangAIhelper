# Tasks

> 共享约束：先读 `spec.md` 的设计令牌表。两端色板/字号必须同源；不新增 Gradle 依赖与图片资源；不改 `logic.py`、`AppViewModel.kt`、`DeepSeekClient.kt`、体质/节气/推荐规则与既有文案（仅允许新增装饰性标签「食养参考」与加载提示文案）。

- [x] Task 1: 建立两端共享设计令牌
  - [x] SubTask 1.1: Android `ui/theme/Theme.kt` 扩展为完整 `lightColorScheme`（含 paper/card/primary/primaryTint/secondary/secondaryTint/ink/inkSoft/inkFaint/line）、`Typography`（标题 Serif、正文 Sans + 行高）与 `Shapes`（16/14/12）
  - [x] SubTask 1.2: `windows-app/app.py` 顶部新增色板常量（同色值）、统一字体层级函数（标题/题干/正文/辅助），并把现有硬编码颜色替换为常量
  - [x] SubTask 1.3: 移除页面中散落的临时色值，确认两端语义色一致

- [x] Task 2: Android 通用组件
  - [x] SubTask 2.1: 新增 `ui/Components.kt`：`ScreenHeader`（小标签 + 主标题 + 细分隔线）、`SectionCard`（卡片底 + 描边 + 圆角）
  - [x] SubTask 2.2: 实现 `StepProgress`（7 段分段进度条，填充/当前/未达三态）
  - [x] SubTask 2.3: 实现 `OptionCard`（未选/选中两态，选中含左侧 4dp 色条 + 内置勾选图标，`animateColorAsState` 过渡）
  - [x] SubTask 2.4: 实现 `LoadingRow`（进度指示 + 提示文字）与 `FoodTag`（墨绿「食养参考」标签）

- [x] Task 3: Android 页面重构与动效
  - [x] SubTask 3.1: `AppRoot` 外层改用 `AnimatedContent`（fade + 12dp 上移，220ms）承载四个页面，保留 `safeDrawingPadding`
  - [x] SubTask 3.2: 问候页：品牌字标 + 副标题 + 欢迎语纸卡（左侧主色竖条）+ 主按钮；Canvas 极淡同心弧装饰
  - [x] SubTask 3.3: 问卷页：`ScreenHeader` + 「第 x / 7 题」+ `StepProgress` + 选项卡片/卡片内输入 + 底部固定操作区（上一题描边、下一题主色、重新开始次级）
  - [x] SubTask 3.4: 画像页：体质 Hero 卡（体质名 + 描述 + 「食养参考」标签）+ 摘要卡（加载态→淡入正文）+ 确认/重填按钮；加载中禁用确认
  - [x] SubTask 3.5: 推荐页：页头 + 当前节气胶囊 + 24 节气 `ModalBottomSheet` 三列网格 + 推荐卡（标题/列表/引用竖线/免责声明弱化）+ 重新开始次级按钮
  - [x] SubTask 3.6: `MarkdownText` 排版增强（标题墨绿加粗、列表缩进、引用左侧竖线、正文行高），解析规则不变
  - [x] SubTask 3.7: `MainActivity`/`themes.xml`：窗口底色为 paper、`enableEdgeToEdge` 或等价系统栏配色，保持浅色状态栏与导航栏图标

- [x] Task 4: Windows 端页面美化
  - [x] SubTask 4.1: 用 `ttk.Style` 定义主/次/幽灵按钮与 Combobox 样式，统一 hover/press 反馈
  - [x] SubTask 4.2: 新增 `Card` 辅助方法（描边 Frame + 内边距）并用于欢迎语、摘要、推荐文本区
  - [x] SubTask 4.3: 问卷页新增 Canvas 分段进度条（含一次填充动画）与卡片式选项按钮（选中高亮 4px 左边条）
  - [x] SubTask 4.4: 画像页体质 Hero 卡与标签；推荐页节气选择行样式对齐；窗口最小尺寸与内容滚动不回归

- [x] Task 5: 版本号与构建验证
  - [x] SubTask 5.1: Android `versionName` → `1.1.0`、`versionCode` → 2
  - [x] SubTask 5.2: 离线构建 `.\gradlew.bat assembleDebug --offline --no-daemon --console=plain` 通过
  - [x] SubTask 5.3: 在 Android 16 模拟器安装并逐页截图核对（问候/问卷/画像/推荐/节气弹层/加载态/大字号）
  - [x] SubTask 5.4: 运行 `windows-app/app.py` 走通七题 → 画像 → 推荐 → 重新开始（GUI 无法在沙箱内验证时，给出用户可直接执行的命令并请求验证）

- [x] Task 6: 两端一致性收尾
  - [x] SubTask 6.1: 对照校验收色板、字号层级、按钮主次、进度条语义
  - [x] SubTask 6.2: 检查无原始 Markdown 标记外露、无文案行为回归（问卷选项、提示语、免责声明与改版前一致）

# Task Dependencies
- Task 2 依赖 Task 1
- Task 3 依赖 Task 1、Task 2
- Task 4 依赖 Task 1（与 Task 2/3 可并行）
- Task 5 依赖 Task 3、Task 4
- Task 6 依赖 Task 5
