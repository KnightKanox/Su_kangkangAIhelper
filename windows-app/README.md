# 康康食养AI（Windows 桌面版）

面向社区老人的健康食养应用：通过 7 道题采集体质、年龄段和饮食信息 → 计算九种体质 → 生成健康画像摘要 → 结合当前 24 节气，从 288 道粤菜（广府菜 / 潮汕菜 / 客家菜）中推荐 3 道时令菜。问卷、画像和推荐页面均可重新开始问卷，无需重启程序。

- 仅使用 Python 标准库（tkinter、urllib.request、csv、json、datetime、re），无任何第三方依赖。
- 大模型采用 DeepSeek（`deepseek-chat`），调用失败时自动回退本地文案，不阻塞界面。
- 画像与推荐文本会呈现常见 Markdown 格式（标题、加粗、列表等）。

第 5 题年龄段可选「60岁以下 / 60—69岁 / 70—79岁 / 80岁及以上」；第 6、7 题可留空。年龄段进入画像和推荐文案，不改变本地体质计算和菜谱筛选。问卷、画像和推荐页均有「重新开始问卷」，生成中也可重试；旧请求结果不会覆盖新页面。

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
└── README.md
```

## 在 Windows 上运行

需要 Python 3.8+（自带 tkinter），无需安装任何第三方库。

```
cd windows-app
python app.py
```

## 打包成 exe

在 Windows 上安装 Python 3.8+（勾选 Add Python to PATH），然后双击 `build.bat`。
脚本会创建 `.venv`、安装 PyInstaller、把菜谱 CSV 打入单文件 EXE，输出到
`dist/KangKangShiYangAI.exe`。也可以在命令提示符中运行 `build.bat` 查看错误信息。

手动打包命令（在 `windows-app` 目录执行）：

```
py -3 -m pip install pyinstaller
py -3 -m PyInstaller --onefile --windowed --name KangKangShiYangAI --add-data "data\recipes.csv:data" app.py
```

生成的 `KangKangShiYangAI.exe` 位于 `dist/` 目录，菜谱数据已包含在 EXE 内。

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
