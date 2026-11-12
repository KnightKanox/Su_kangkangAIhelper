# 美术素材接入 Spec

## Why
`F:\my\SuLinMei\art` 下已有 4 张与「温润食养国风」色板高度一致的透明背景素材，但目前两个客户端仍是纯文字品牌区、纯文字 Tab 与默认应用图标。把这些素材接入应用，可以显著提升识别度与亲切感，同时不改变业务流程、算法与文案。

## What Changes
- 接入全部 4 张素材，**两端同步**：应用图标（Android launcher / Windows exe 与窗口图标）、首页品牌横幅与主插图、底部 Tab 图标、空态点缀。
- 素材使用方式：**大图按原图直接打包（不压缩、不转 WebP）**；仅图标类做必要的裁切与多密度缩放，输出到各自资源目录。
- 新增 Android `res/drawable-nodpi/`（原图）与 `res/mipmap-*`（launcher 图标）；`AndroidManifest.xml` 增加 `android:icon` / `android:roundIcon`。
- 新增 Windows `assets/`（原图 + Tab/空态小图 + `app.ico`）；窗口用 `iconphoto`，PyInstaller 用 `--icon`。
- 首页品牌区从「纯文字」升级为「Logo 横幅 + 文字副标题」；首页增加一张主插图（Emblem-3），「我的」页顶部复用同一插图的小尺寸版本。
- `BottomTabBar` 从纯文字改为「图标 + 文字」，图标取自 Icon Set-4 裁切（首页 / 汤碗 / 人物）。
- 空态卡片增加 64–96dp 的叶子/莲花点缀图标（取自 Icon Set-4），不遮挡文字。
- 版本：Android `versionName 1.3.0`、`versionCode 4`；Windows 无版本字段。
- 说明：v1.1.0 规格中「不新增图片资源」的约束由本规格显式取代。

## Impact
- Affected specs: 首页、三 Tab 信息架构、空态与异常态、应用打包产物。
- Affected code:
  - `RESET/android-app/app/src/main/res/drawable-nodpi/`（新增原图）
  - `RESET/android-app/app/src/main/res/mipmap-*/`（新增 launcher 图标）
  - `RESET/android-app/app/src/main/AndroidManifest.xml`（icon / roundIcon）
  - `RESET/android-app/.../ui/HomeScreen.kt`、`ProfileScreen.kt`、`Components.kt`（Tab 图标、空态图标）
  - `RESET/android-app/app/build.gradle.kts`（仅版本号）
  - `RESET/windows-app/assets/`（新增原图、裁切小图、`app.ico`）
  - `RESET/windows-app/app.py`（窗口图标、品牌横幅、首页插图、Tab 图标、空态图标）
  - `RESET/windows-app/build.bat`（PyInstaller `--icon` 与 `--add-data`）
  - 根 `README.md`、`RESET/README.md`、`修改日志.md`
- 不修改：问卷题目/选项、体质计算、节气表、菜谱 CSV、筛选与忌口规则、DeepSeek 提示词与理由解析。

## 素材清单与用途

| 源文件 | 尺寸 / 透明 | 用途 |
| --- | --- | --- |
| `Steaming Bowl Wellness Emblem-1.png` | 1254×1254，带米色圆角底 | Android launcher 图标（多密度）；Windows `app.ico`（16–256 多尺寸）与窗口图标 |
| `Earthy Herbal Soup Logo-2.png` | 2172×724，透明 | 首页顶部品牌横幅（替代纯文字字标上方的空白区） |
| `Herbal Soup Wellness Emblem-3.png` | 1199×1312，透明 | 首页主插图；「我的」页顶部小尺寸复用 |
| `Warm Earthy Wellness Icon Set-4.png` | 1536×1024，透明 | 裁切出 Tab 图标（首页/汤碗/人物）与空态点缀（叶子/莲花） |

## 素材处理约束
- **大图保持原图**：Logo-2、Emblem-1、Emblem-3、Icon Set-4 原文件直接复制进两端资源目录，不压缩、不转格式。
- 允许的必要派生：
  - launcher 图标：从 Emblem-1 生成 mdpi 48 / hdpi 72 / xhdpi 96 / xxhdpi 144 / xxxhdpi 192 五档 PNG；
  - Windows `app.ico`：16/32/48/64/128/256 多尺寸；
  - Tab 与空态小图：从 Icon Set-4 按固定坐标裁切为独立 PNG（保留透明通道）。
- 派生图标同样使用无损 PNG；不得改变原图色值。
- 裁切坐标与生成命令记录在 `RESET/windows-app/assets/README.md`（仅记录素材来源与派生规则，不记录密钥）。

## ADDED Requirements

### Requirement: 应用图标
Android SHALL 使用 Emblem-1 作为 launcher 图标（含 `roundIcon`），并在 API 31+ 的启动器上可正常显示；Windows SHALL 使用同一素材生成的多尺寸 `.ico` 作为 exe 图标与运行时窗口图标。

#### Scenario: Android 图标
- **WHEN** APK 安装到 Android 16 模拟器
- **THEN** 启动器显示米色圆角底的汤碗徽标，不是默认绿色机器人

#### Scenario: Windows 图标
- **WHEN** 打开 Windows 程序或查看 exe 文件
- **THEN** 窗口标题栏与文件图标均为汤碗徽标，图标不模糊、不变形

### Requirement: 首页品牌与主插图
首页 SHALL 在标题区展示 Logo-2 横幅（`ContentScale.Fit`，限高约 72–96dp），下方保留「康康食养AI / 社区老人健康食养助手」文字；首页问候卡上方或卡片右侧 SHALL 展示 Emblem-3 主插图（限高约 160–220dp），插图不遮挡问候文字与主 CTA。插图在窄屏与大字体下可等比缩小或隐藏，不得挤压关键操作。

#### Scenario: 首页展示
- **WHEN** 进入首页
- **THEN** 依次看到 Logo 横幅、文字标题、主插图、问候卡、今日节气与主 CTA，全部在首屏可滚动访问

#### Scenario: 小屏与大字体
- **WHEN** 系统字体放大或屏幕较窄
- **THEN** 插图等比缩放，问候语与按钮文字不截断，页面可滚动

### Requirement: Tab 图标
`首页 / 食养 / 我的` 三个 Tab SHALL 显示 24dp 图标 + 18sp 文字；图标取自 Icon Set-4（房屋 / 汤碗 / 人物），选中时图标与文字同为主色，未选为辅助色；Tab 高度保持 ≥64，指示条保留。

#### Scenario: 切换 Tab
- **WHEN** 用户切换 Tab
- **THEN** 选中项图标与文字同步变色，页面状态不重置

### Requirement: 空态点缀
未完成问卷、无推荐结果、加载失败三种空态 SHALL 在标题上方显示 64–96dp 的素材点缀（未完成问卷用叶子，无结果用莲花，失败用叶片/纹样）；点缀为装饰性图片（`contentDescription = null`），不与文字重叠。

#### Scenario: 空态展示
- **WHEN** 用户未完成问卷进入食养
- **THEN** 空态卡片顶部出现叶子图标，标题与说明文字完整可读

### Requirement: Windows 素材渲染
Windows SHALL 使用 Tk `PhotoImage` 加载 PNG，并用 `subsample` 控制显示尺寸（不引入 Pillow）；Logo 横幅限宽约 560px、主插图限高约 200px、Tab 图标约 24px、空态图标约 56px；窗口标题栏与 exe 使用 `app.ico`。所有图片路径必须兼容 PyInstaller 单文件运行（使用 `_MEIPASS` 解析）。

#### Scenario: 打包后运行
- **WHEN** 双击 PyInstaller 生成的 exe
- **THEN** 图标与插图正常显示，缺失文件时不崩溃（降级为纯文字）

### Requirement: 素材来源与体积
README SHALL 记录素材来源目录、用途与「原图打包」策略；APK 体积增加约 11MB（实测 23.9MB → 35.2MB）为已接受成本。素材文件 MUST NOT 被压缩或改色。

#### Scenario: 体积核对
- **WHEN** 构建 debug APK
- **THEN** 新增资源为 4 张原图 + 裁切/派生图标 + 纸色版主插图，总体积增量与预期（约 11MB）一致

## MODIFIED Requirements

### Requirement: 首页品牌区
原有纯文字品牌区改为「Logo 横幅 + 文字标题 + 主插图」；文案与问候语不变，主 CTA、今日节气、体质卡与推荐预览的先后顺序不变。

### Requirement: 三 Tab 信息架构
Tab 从纯文字升级为图标 + 文字；Tab 名称、切换不清空状态、二级页隐藏 Tab 的行为保持不变。

### Requirement: 空态与异常态
空态文案与按钮不变，仅新增装饰性图标；图标不参与点击、不改变可点高度 ≥56 的既有约束。

## REMOVED Requirements
无。
