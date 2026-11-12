# 康康食养AI（Windows 桌面版）

面向社区老人的健康食养应用：首页（首页 / 食养 / 我的三 Tab）→ 7 道题采集体质、年龄段和饮食信息 → 计算九种体质 → 生成健康画像摘要 → 结合当前 24 节气，从 288 道粤菜（广府菜 / 潮汕菜 / 客家菜）中推荐 3 道时令菜，以结构化菜谱卡展示。问卷、画像、食养空态与我的页均可重新填写问卷，无需重启程序。

- 界面与推荐逻辑使用 Python 和 tkinter；背景音乐使用 pygame-ce 2.5.8。
- 大模型采用 DeepSeek（`deepseek-chat`），调用失败时自动回退本地文案，不阻塞界面。
- 画像摘要会呈现常见 Markdown 格式（标题、加粗、列表等）；推荐结果为结构化菜谱卡，AI 只生成每道菜的推荐理由。
- 「我的」可修改饮食偏好与忌口；食养页可「换一组菜」。含忌口词的主料会从候选中排除。
- 食养页接入四季与菜系插画，空内容、加载和失败使用状态插画；「拂晓车站」从原件解码为 PCM WAV，由 pygame-ce 循环播放，顶部小按钮可静音。

第 5 题年龄段可选「60岁以下 / 60—69岁 / 70—79岁 / 80岁及以上」；第 6、7 题可留空。年龄段进入画像和推荐文案，不改变本地体质计算和菜谱筛选。问卷、画像、食养空态与我的页均有「重新填写问卷」，生成中也可重试；旧请求结果不会覆盖新页面。

使用 DeepSeek 时，在启动程序前设置当前终端的 `DEEPSEEK_API_KEY` 环境变量；未设置时使用本地文案。请勿把密钥写入源码或提交到仓库。

PowerShell 示例：`$env:DEEPSEEK_API_KEY = '你的密钥'; py -3 app.py`。不设置密钥时直接运行 `py -3 app.py`。Markdown 支持标题、加粗、斜体、列表、引用、行内代码、代码块和链接文字等基本格式。

## 文件结构

```
windows-app/
├── app.py           # tkinter GUI：问候 → 7题问卷 → 画像摘要 → 节气推荐
├── logic.py         # 纯逻辑（不依赖 GUI）：数据加载、节气/体质计算、筛选评分、DeepSeek 调用、回退文案
├── build.bat        # Windows 打包脚本（PyInstaller）
├── data/
│   └── recipes.csv  # 288 道菜谱数据
├── assets/          # 页面插画、图标与 fuxiao_station.wav
└── README.md
```

## 在 Windows 上运行

源码运行需要 Python、tkinter 与 pygame-ce；当前验证环境为 Python 3.14.7。先运行 `py -3 -m pip install pygame-ce==2.5.8`。

```
cd windows-app
python app.py
```

## 打包成 exe

在 Windows 上安装 Python（本机验证版本为 3.14.7），然后双击 `build.bat`。
脚本会创建 `.venv`、安装 PyInstaller、把菜谱 CSV、插画与背景音乐打入单文件 EXE，输出到
`dist/KangKangShiYangAI.exe`。也可以在命令提示符中运行 `build.bat` 查看错误信息。

手动打包命令（在 `windows-app` 目录执行）：

```
py -3 -m pip install pyinstaller pygame-ce==2.5.8
py -3 -m PyInstaller --onefile --windowed --name KangKangShiYangAI --icon "assets\app.ico" --add-data "assets;assets" --add-data "data\recipes.csv;data" app.py
```

生成的 `KangKangShiYangAI.exe` 位于 `dist/` 目录，菜谱数据已包含在 EXE 内。

本机 `py_compile` 与 PyInstaller 构建通过。原音频虽以 `.mp3` 命名，实际为 MP4/AAC 容器；Windows 运行副本直接解码为 PCM WAV，避免再次有损压缩。pygame-ce 循环播放与静音测试通过，重建的 EXE 内已确认包含 WAV。原件音轨约 52kb/s，源头音质仍受其限制。

## 纯逻辑验证（无 GUI）

`logic.py` 不依赖 tkinter，可在无图形界面的 Linux 上直接测试。

```bash
cd windows-app
python3 -c "
import logic
print(logic.calculate_constitution('怕冷','容易出汗','经常口干','偏软不成形')[0])
print(logic.calculate_constitution('正常','正常','正常','正常')[0])
print(logic.calculate_constitution(None,None,None,None)[0])
print(logic.current_solar_term(11,15))
print(logic.current_solar_term(1,3))
print(logic.current_solar_term(2,5))
for t in ['立春','大寒','小满','冬至']:
    rs = logic.recommend(t, '阳虚', '', '')
    print(t, len(rs), [r['菜名'] for r in rs])
"
```

实际运行输出：

```
阳虚
平和
参数错误
立冬
大寒
立春
立春 3 ['韭菜炒虾仁', '韭菜粿', '春笋煲咸鸡']
大寒 3 ['腊味萝卜糕', '牛杂汤', '巧烧雁鹅']
小满 3 ['沙茶牛肉', '冬瓜煲咸肉', '白灼虾']
冬至 3 ['腊味糯米饭', '广府盆菜', '冬节丸']
```

对应结论：

- 体质 `(怕冷, 容易出汗, 经常口干, 偏软不成形)` → 阳虚；`(正常,正常,正常,正常)` → 平和；全空 → 参数错误。
- 节气 `(11,15)` → 立冬；`(1,3)` → 大寒；`(2,5)` → 立春。
- 任意节气下均能返回 ≤3 道、且每道都带菜名的推荐。

## 说明

- 推荐内容仅供参考，结尾会附带「本推荐为食养参考，不构成医疗建议」。
- DeepSeek 需联网；无网络或调用失败时自动使用本地回退文案。
