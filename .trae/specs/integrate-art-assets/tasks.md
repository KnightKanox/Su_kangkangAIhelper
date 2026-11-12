# Tasks

> 共享约束：先读 `spec.md`。素材原图不得压缩或改色；仅允许裁切与多密度/多尺寸派生。不改问卷题目、体质/节气/筛选规则、推荐理由逻辑与既有文案。

- [x] Task 1: 素材盘点与派生资源
  - [x] SubTask 1.1: 复制 4 张原图到 `RESET/android-app/app/src/main/res/drawable-nodpi/`（`art_brand_logo.png` = Logo-2、`art_hero_emblem.png` = Emblem-3、`art_app_icon_source.png` = Emblem-1、`art_icon_set.png` = Icon Set-4）
  - [x] SubTask 1.2: 从 Icon Set-4 裁切 3 个 Tab 图标（房屋 / 汤碗 / 人物）与 2 个空态点缀（叶子 / 莲花），保留透明通道，输出 `art_tab_home.png`、`art_tab_food.png`、`art_tab_profile.png`、`art_motif_leaf.png`、`art_motif_lotus.png`
  - [x] SubTask 1.3: 由 Emblem-1 生成 Android launcher 五档尺寸（mdpi 48 / hdpi 72 / xhdpi 96 / xxhdpi 144 / xxxhdpi 192）到 `res/mipmap-*/ic_launcher.png` 与 `ic_launcher_round.png`
  - [x] SubTask 1.4: 由 Emblem-1 生成 Windows `app.ico`（16/32/48/64/128/256），并把原图、Tab 小图、空态小图复制到 `RESET/windows-app/assets/`
  - [x] SubTask 1.5: 新建 `RESET/windows-app/assets/README.md`：记录素材来源、裁切坐标、派生尺寸与「原图打包」说明

- [x] Task 2: Android 接入
  - [x] SubTask 2.1: `AndroidManifest.xml` 增加 `android:icon="@mipmap/ic_launcher"`、`android:roundIcon="@mipmap/ic_launcher_round"`
  - [x] SubTask 2.2: `HomeScreen` 顶部加 Logo 横幅（限高 72–96dp、`ContentScale.Fit`）与 Emblem-3 主插图（限高 160–220dp）；保留文字标题、问候卡、CTA 顺序
  - [x] SubTask 2.3: `ProfileScreen` 顶部使用同一插图的小尺寸版本（限高 ≤120dp）
  - [x] SubTask 2.4: `BottomTabBar` 改为「24dp 图标 + 18sp 文字」，选中主色、未选辅助色，指示条保留；图标用 `null` contentDescription（文字已表达语义）
  - [x] SubTask 2.5: `EmptyState` 支持可选装饰图标（64–96dp），三种空态分别用叶子/莲花/叶片；装饰 `contentDescription = null`
  - [x] SubTask 2.6: `app/build.gradle.kts`：`versionCode 4`、`versionName "1.3.0"`

- [x] Task 3: Android 构建与视觉验证
  - [x] SubTask 3.1: 离线 `assembleDebug` 通过；aapt 确认版本与 `application-icon`；解包确认 `res/drawable-nodpi` 与 mipmap 资源存在
  - [x] SubTask 3.2: 模拟器截图：首页（Logo + 插图）、食养 Tab（带图标）、我的、未完成问卷空态；保存为 `RESET\.screenshots\v13-*.png`

- [x] Task 4: Windows 接入
  - [x] SubTask 4.1: `app.py` 增加资源路径解析（兼容 PyInstaller `_MEIPASS`），并缓存 `PhotoImage`
  - [x] SubTask 4.2: 首页加 Logo 横幅（限宽 ~560px）与主插图（限高 ~200px）；「我的」加小插图
  - [x] SubTask 4.3: 顶部导航改为「图标 + 文字」，空态增加点缀图标；图片缺失时降级为纯文字不报错
  - [x] SubTask 4.4: 窗口图标用 `iconphoto`；`build.bat` 增加 `--icon assets\app.ico` 与 `--add-data "assets;assets"`

- [x] Task 5: Windows 验证
  - [x] SubTask 5.1: `py -3 -m py_compile app.py logic.py` 通过；`mainloop` 冒烟确认图片可加载、Tab 与空态正常、无异常
  - [x] SubTask 5.2: 用 `.venv` 的 PyInstaller 构建一次 exe，确认产物存在且 exe 关联图标为汤碗徽标（可用 `Icon.ExtractAssociatedIcon` 校验）

- [x] Task 6: 一致性与文档
  - [x] SubTask 6.1: 核对两端图标语义、首页顺序、空态点缀与色板一致；问卷/画像/推荐结果与改造前一致
  - [x] SubTask 6.2: 更新 `README.md`（素材来源与用途、体积说明）、`RESET\README.md`；在 `修改日志.md` 顶部追加 v1.3.0

# Task Dependencies
- Task 2 依赖 Task 1
- Task 3 依赖 Task 2
- Task 4 依赖 Task 1，可与 Task 2/3 并行
- Task 5 依赖 Task 4
- Task 6 依赖 Task 3、Task 5
