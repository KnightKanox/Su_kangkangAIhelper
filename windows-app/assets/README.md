# 美术素材说明（assets）

素材来源：`F:\my\SuLinMei\art`（原始交付，未被修改）。本目录中的 `art_*` 文件为应用运行时资源，**原图按原样复制，未压缩、未改色**；仅图标类做了裁切与缩放派生。

## 新增水墨食养素材与音乐（2026-11-13）

`art/` 内 10 张 `ChatGPT Image Nov 13, 2026, 12_42_*.png` 按生成顺序对应下表前 10 项；`state_loading.png` 来自 `Sage Herbal Teapot Spinner.png`。均为 1254×1254、带透明通道的 PNG，运行副本原样复制到本目录及 Android `res/drawable-nodpi/`。Windows 运行时通过 `PhotoImage.subsample()` 缩小显示。

| 运行文件 | 内容 | 当前用途 |
| --- | --- | --- |
| `season_spring.png`、`season_summer.png`、`season_autumn.png`、`season_winter.png` | 四季圆形水墨画 | 食养页节气图 |
| `cuisine_guangfu.png`、`cuisine_chaoshan.png`、`cuisine_hakka.png` | 三系菜插画 | 菜谱卡菜系图 |
| `state_empty.png` | 空碗 | 未填写或无推荐 |
| `state_loading.png` | 茶壶与加载标记 | 推荐加载中 |
| `state_error.png` | 提示茶壶 | 推荐加载失败 |
| `state_success.png` | 对勾汤碗 | 资源预留，当前无独立成功页 |

Windows 使用 `fuxiao_station.wav`，直接从工作区原件 `media/Candy_Wind - 拂晓车站_音频.mp3`（实际为 MP4/AAC 容器）解码成 48kHz、16 位双声道 PCM。pygame-ce 循环播放并提供静音键。Android 使用保留原始 AAC 音轨的 `res/raw/fuxiao_station.m4a`。

## 原图（无损复制）

| 本目录文件 | 源文件 | 尺寸 | 用途 |
| --- | --- | --- | --- |
| `art_brand_logo.png` | `Earthy Herbal Soup Logo-2.png` | 2172×724 | 首页品牌横幅 |
| `art_hero_emblem.png` | `Herbal Soup Wellness Emblem-3.png` | 1199×1312 | 首页主插图、我的页小插图 |
| `art_app_icon_source.png` | `Steaming Bowl Wellness Emblem-1.png` | 1254×1254 | 应用图标源（Android mipmap / `app.ico`） |
| `art_icon_set.png` | `Warm Earthy Wellness Icon Set-4.png` | 1536×1024 | 图标集源（Tab/空态裁切来源） |
| `art_hero_home.png` | 由 `art_hero_emblem.png` 派生 | 1199×1312 | 首页/我的页显示用：原图压平到纸色 `#F7F2E7`（不透明），四周 56px 羽化过渡；消除原图边缘的矩形柔光边界。原图 `art_hero_emblem.png` 保留未改 |

## 裁切派生（从 `art_icon_set.png`）

裁切窗口以源图像素坐标记录（左、上、右、下），裁剪后按近米色 `R>228, G>220, B>198` 且 `R-B ∈ [8,45]` 去除瓦片底色，保留透明通道与图标原色。

| 输出 | 裁切窗口（源像素） | 输出尺寸 | 用途 |
| --- | --- | --- | --- |
| `art_tab_home.png` | 672, 138 – 933, 393 | 223×248 | Tab「首页」 |
| `art_tab_food.png` | 948, 138 – 1209, 393 | 234×238 | Tab「食养」 |
| `art_tab_profile.png` | 1224, 138 – 1485, 393 | 205×240 | Tab「我的」（心形为透明镂空） |
| `art_motif_leaf.png` | 1089, 738 – 1308, 975 | 141×152 | 未完成问卷 / 失败空态点缀 |
| `art_motif_lotus.png` | 1299, 738 – 1518, 975 | 195×184 | 无推荐结果空态点缀 |

## 显示尺寸派生（仅 Windows，避免 Tk 粗糙缩放）

Tk 的 `PhotoImage` 只能整数倍 `subsample`，为保持清晰度预先生成 1:1 显示尺寸：

| 输出 | 尺寸 | 用途 |
| --- | --- | --- |
| `art_logo_560.png` | 560×187 | 首页横幅（限宽 560px） |
| `art_hero_200h.png` | 183×200 | 首页主插图（限高 200px，纸色底） |
| `art_hero_120h.png` | 110×120 | 我的页小插图（纸色底） |
| `art_tab_*_24.png` | 24×24 | 顶部导航图标 |
| `art_motif_*_96.png` | 96×96 | 空态点缀 |

## 应用图标

- Android：`app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` 与 `ic_launcher_round.png`，尺寸 48/72/96/144/192，源自 Emblem-1。
- Windows：`app.ico` 含 16/32/48/64/128/256 六个 PNG 条目，用于 PyInstaller `--icon` 与窗口图标。

## 打包

- Android：`res/drawable-nodpi/` 下原图随 APK 打包（约 7.7MB 增量）。
- Windows：PyInstaller 使用 `--add-data "assets;assets"`；运行时通过 `_MEIPASS` 解析路径，素材缺失时界面降级为纯文字，不崩溃。
