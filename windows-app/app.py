# -*- coding: utf-8 -*-
"""康康食养AI —— Windows 桌面版 GUI（tkinter，仅标准库）。

信息架构：顶部三 Tab（首页 / 食养 / 我的）+ 二级全屏页（问卷 / 画像）。
推荐以本地 CSV 为事实来源渲染结构化菜谱卡，DeepSeek 仅生成推荐理由。
"""
import datetime
import os
import re
import sys
import threading
import tkinter as tk
import tkinter.font as tkfont
from tkinter import ttk

import pygame

import logic

# ---------------------------------------------------------------- 设计令牌
# 与 Android 端 Theme.kt 同源，勿单独改动色值。
PAPER = "#F7F2E7"
CARD = "#FFFDF7"
PRIMARY = "#B3541E"
PRIMARY_PRESSED = "#8F4218"
PRIMARY_TINT = "#F4E4D4"
SECONDARY = "#4E6E58"
SECONDARY_TINT = "#E7EFE7"
INK = "#3A3226"
INK_SOFT = "#74685A"
INK_FAINT = "#8C8275"
LINE = "#E4D9C3"
WHITE = "#FFFFFF"

# 字体层级：品牌 / 页面主标题 / 题干 / 正文 / 辅助
FONT_FAMILY = "Microsoft YaHei"
FONT_BRAND = (FONT_FAMILY, 30, "bold")
FONT_TITLE = (FONT_FAMILY, 20, "bold")
FONT_QUESTION = (FONT_FAMILY, 16, "bold")
FONT_BODY = (FONT_FAMILY, 13)
FONT_CAPTION = (FONT_FAMILY, 11)
FONT_TAG = (FONT_FAMILY, 11, "bold")
FONT_SUBTITLE = (FONT_FAMILY, 14)
FONT_OPTION = (FONT_FAMILY, 15)
FONT_BUTTON = (FONT_FAMILY, 14)
FONT_SMALL_BOLD = (FONT_FAMILY, 12, "bold")
FONT_COMBO = (FONT_FAMILY, 13)
FONT_TAB = (FONT_FAMILY, 15, "bold")
FONT_RECIPE = (FONT_FAMILY, 17, "bold")

GREETING = (
    "您好！欢迎使用康康食养AI，我是你的社区营养助手。"
    "接下来我会问你几个简单问题，了解你的身体状况，之后结合节气为你推荐合适的膳食。"
    "我们现在开始啦！"
)

AGE_INDEX = 4
PREFERENCE_INDEX = 5
TABOO_INDEX = 6

# 一级 Tab（名称与 Android 端逐字一致）
TABS = (("home", "首页"), ("food", "食养"), ("profile", "我的"))


# ---------------------------------------------------------------- 图片资源
def resource_path(rel):
    """资源绝对路径：兼容 PyInstaller 单文件（sys._MEIPASS）与源码运行。"""
    base = getattr(sys, "_MEIPASS", None)
    if base:
        return os.path.join(base, rel)
    return os.path.join(os.path.dirname(os.path.abspath(__file__)), rel)


_IMAGE_CACHE = {}


def load_image(name):
    """加载 assets 下的 PNG 并缓存（含失败结果），缺失时返回 None。"""
    if name in _IMAGE_CACHE:
        return _IMAGE_CACHE[name]
    image = None
    try:
        image = tk.PhotoImage(file=resource_path(os.path.join("assets", name)))
    except (tk.TclError, OSError):
        image = None
    _IMAGE_CACHE[name] = image
    return image


def load_image_small(name, factor=10):
    """按界面尺寸加载透明插画，避免 Tk 常驻大尺寸位图。"""
    key = (name, factor)
    if key not in _IMAGE_CACHE:
        try:
            source = tk.PhotoImage(file=resource_path(os.path.join("assets", name)))
            _IMAGE_CACHE[key] = source.subsample(factor)
        except (tk.TclError, OSError):
            _IMAGE_CACHE[key] = None
    return _IMAGE_CACHE[key]


def season_art(term):
    try:
        index = logic.SOLAR_TERM_NAMES.index(term)
    except ValueError:
        index = 18
    return ("season_spring.png", "season_summer.png",
            "season_autumn.png", "season_winter.png")[min(index // 6, 3)]


def cuisine_art(cuisine):
    if "潮汕" in cuisine:
        return "cuisine_chaoshan.png"
    if "客家" in cuisine:
        return "cuisine_hakka.png"
    return "cuisine_guangfu.png"


# 固定文案
DISCLAIMER = logic.DISCLAIMER
LOADING_TEXT = "正在为您挑选时令菜…"
EMPTY_TITLE = "还没有健康画像"
EMPTY_MESSAGE = "先完成 7 题问卷，才能按体质和节气为你推荐菜谱。"
NO_RESULT_TITLE = "当前节气与忌口条件下暂无合适菜谱"
NO_RESULT_MESSAGE = "可以换个节气，或调整忌口后重新填写。"
ERROR_TITLE = "推荐加载失败"
ERROR_MESSAGE = "网络或本地数据异常，请重试。"

# (题目文字, 选项列表 或 None, 题型 choice/text)
QUESTIONS = [
    ("第 1 题 · 您平时怕冷还是怕热？", logic.Q1_OPTIONS, "choice"),
    ("第 2 题 · 您平时出汗多不多？", logic.Q2_OPTIONS, "choice"),
    ("第 3 题 · 您平时会不会口干？", logic.Q3_OPTIONS, "choice"),
    ("第 4 题 · 您的大便通常是什么状态？", logic.Q4_OPTIONS, "choice"),
    ("第 5 题 · 您属于哪个年龄段？", ["60岁以下", "60—69岁", "70—79岁", "80岁及以上"], "choice"),
    ("第 6 题 · 您平时有什么饮食偏好吗？（可留空）", None, "text"),
    ("第 7 题 · 您有什么忌口的食物吗？（可留空）", None, "text"),
]

_INLINE_MARKDOWN = re.compile(
    r"\*\*(.+?)\*\*|__(.+?)__|`([^`\n]+)`|"
    r"\[([^\]]+)\]\((https?://[^)]+)\)|"
    r"(?<!\*)\*([^*\n]+)\*(?!\*)|(?<!\w)_([^_\n]+)_(?!\w)"
)


def _insert_inline(text_widget, line, block_tag=None):
    base_tags = (block_tag,) if block_tag else ()
    position = 0
    for match in _INLINE_MARKDOWN.finditer(line):
        text_widget.insert("end", line[position:match.start()], base_tags)
        if match.group(1) or match.group(2):
            text_widget.insert("end", match.group(1) or match.group(2), base_tags + ("strong",))
        elif match.group(3):
            text_widget.insert("end", match.group(3), base_tags + ("code",))
        elif match.group(4):
            text_widget.insert("end", f"{match.group(4)}（{match.group(5)}）", base_tags + ("link",))
        else:
            text_widget.insert("end", match.group(6) or match.group(7), base_tags + ("emphasis",))
        position = match.end()
    text_widget.insert("end", line[position:], base_tags)


def render_markdown(text_widget, content):
    """在 tkinter.Text 中呈现常见 Markdown 标记（解析规则不变）。"""
    text_widget.tag_configure("strong", font=(FONT_FAMILY, 13, "bold"), foreground=PRIMARY)
    text_widget.tag_configure("emphasis", font=(FONT_FAMILY, 13, "italic"), foreground=INK)
    text_widget.tag_configure("heading", font=(FONT_FAMILY, 15, "bold"), foreground=SECONDARY)
    text_widget.tag_configure("subheading", font=(FONT_FAMILY, 14, "bold"), foreground=SECONDARY)
    text_widget.tag_configure("code", font=("Consolas", 11), background=PAPER, foreground=INK)
    text_widget.tag_configure("quote", foreground=INK_FAINT)
    text_widget.tag_configure("link", foreground=PRIMARY, underline=True)

    in_code_block = False
    for index, raw_line in enumerate(str(content).replace("\r\n", "\n").split("\n")):
        if index:
            text_widget.insert("end", "\n")
        if raw_line.lstrip().startswith("```"):
            in_code_block = not in_code_block
            continue
        if in_code_block:
            text_widget.insert("end", raw_line, ("code",))
            continue

        heading = re.match(r"^\s{0,3}(#{1,6})\s+(.+)$", raw_line)
        bullet = re.match(r"^(\s*)[-*+]\s+(.+)$", raw_line)
        quote = re.match(r"^\s*>\s?(.*)$", raw_line)
        if heading:
            _insert_inline(text_widget, heading.group(2), "heading" if len(heading.group(1)) == 1 else "subheading")
        elif bullet:
            text_widget.insert("end", bullet.group(1) + "•  ")
            _insert_inline(text_widget, bullet.group(2))
        elif quote:
            text_widget.insert("end", "│  ", ("quote",))
            _insert_inline(text_widget, quote.group(1), "quote")
        else:
            _insert_inline(text_widget, raw_line)


class StepProgress(tk.Canvas):
    """7 段分段进度条，切换题目时做一次约 200ms 的填充动画。"""

    ANIM_MS = 200
    FRAMES = 10

    def __init__(self, parent, total=7, seg_height=6, gap=6):
        super().__init__(
            parent, bg=PAPER, highlightthickness=0, bd=0,
            height=seg_height, width=240,
        )
        self.total = total
        self.seg_height = seg_height
        self.gap = gap
        self.value = 0.0
        self._job = None
        self.bind("<Configure>", lambda _event: self._draw())

    # ---------------------------------------------------------- 对外
    def set(self, step, animate=True):
        """step 为 1..total 的当前题号。"""
        target = float(max(0, min(self.total, step)))
        if self._job is not None:
            try:
                self.after_cancel(self._job)
            except tk.TclError:
                pass
            self._job = None
        if not animate:
            self.value = target
            self._draw()
            return
        start = self.value if self.value > 0 else max(0.0, target - 1.0)
        self.value = start
        self._draw()
        self._tick(start, target, 1)

    # ---------------------------------------------------------- 动画
    def _tick(self, start, target, frame):
        try:
            if not self.winfo_exists():
                return
            ratio = min(1.0, frame / self.FRAMES)
            self.value = start + (target - start) * ratio
            self._draw()
            if frame < self.FRAMES:
                self._job = self.after(max(16, self.ANIM_MS // self.FRAMES),
                                       self._tick, start, target, frame + 1)
            else:
                self._job = None
        except tk.TclError:
            # 页面已被 clear() 销毁
            self._job = None

    # ---------------------------------------------------------- 绘制
    def _draw(self):
        try:
            if not self.winfo_exists():
                return
        except tk.TclError:
            return
        self.delete("all")
        width = self.winfo_width()
        height = self.winfo_height()
        if width <= 1:
            return
        if height <= 1:
            height = self.seg_height
        seg_w = (width - self.gap * (self.total - 1)) / float(self.total)
        if seg_w <= 0:
            return
        for i in range(self.total):
            x0 = i * (seg_w + self.gap)
            x1 = x0 + seg_w
            self.create_rectangle(x0, 0, x1, height, fill=LINE, outline=LINE)
            frac = max(0.0, min(1.0, self.value - i))
            if frac > 0:
                self.create_rectangle(
                    x0, 0, x0 + seg_w * frac, height, fill=PRIMARY, outline=PRIMARY,
                )


class App:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("康康食养AI")
        self.root.geometry("760x860")
        self.root.minsize(720, 760)
        self.root.configure(bg=PAPER)
        self._setup_window_icon()
        self._setup_styles()

        self._music_channel = None
        self._music_sound = None
        self._music_muted = False
        music_bar = tk.Frame(self.root, bg=PAPER)
        music_bar.pack(fill="x")
        self.music_button = tk.Button(
            music_bar, text="♪ 静音", command=self._toggle_music,
            bg=PAPER, fg=INK_SOFT, activebackground=PAPER,
            relief="flat", bd=0, font=FONT_CAPTION, cursor="hand2",
        )
        self.music_button.pack(side="right", padx=16, pady=2)
        self._start_music()
        self.root.protocol("WM_DELETE_WINDOW", self._close_app)

        self.container = tk.Frame(self.root, bg=PAPER)
        self.container.pack(fill="both", expand=True)

        self.answers = [None] * len(QUESTIONS)
        self.constitution = None
        self.desc = None
        self._view_generation = 0
        self._request_generation = 0
        self.current_term = logic.current_solar_term(
            datetime.date.today().month, datetime.date.today().day
        )
        self.term_var = tk.StringVar(value=self.current_term)

        # 一级 Tab 与二级页
        self.tab = "home"
        self.screen = None          # None 表示 Tab 页；否则为 question / summary
        self._return_tab = "home"

        # 推荐状态（跨 Tab 保留）
        self.confirmed = False
        self.recipes = []
        self.reasons = {}
        self.recommend_status = "idle"   # idle / loading / done / error
        self.recommend_term = self.current_term
        self._recommend_token = 0
        self.recipe_offset = 0
        self.has_more_recipes = False

        # 折叠/展开状态
        self.summary_text = None
        self._expanded_recipes = set()
        self._summary_expanded = False

        self._scroll_canvas = None
        self.select_tab("home")

    def _start_music(self):
        path = resource_path(os.path.join("assets", "fuxiao_station.wav"))
        try:
            pygame.mixer.init(frequency=48000, size=-16, channels=2, buffer=2048)
            self._music_sound = pygame.mixer.Sound(path)
            self._music_channel = self._music_sound.play(loops=-1)
            if self._music_channel is not None:
                self._music_channel.set_volume(0.25)
        except (pygame.error, OSError):
            self.music_button.configure(text="♪ 音乐不可用", state="disabled")

    def _toggle_music(self):
        self._music_muted = not self._music_muted
        volume = 0 if self._music_muted else 250
        if self._music_channel is not None:
            self._music_channel.set_volume(volume / 1000)
        self.music_button.configure(text="♪ 开启音乐" if self._music_muted else "♪ 静音")

    def _close_app(self):
        if self._music_channel is not None:
            self._music_channel.stop()
        pygame.mixer.quit()
        self.root.destroy()

    # ------------------------------------------------------------ 样式
    def _setup_window_icon(self):
        """设置窗口图标；素材缺失时静默跳过。"""
        try:
            icon = load_image("art_app_icon_source.png")
            if icon is not None:
                self.root.iconphoto(True, icon)
        except tk.TclError:
            pass
        try:
            ico = resource_path(os.path.join("assets", "app.ico"))
            if os.path.exists(ico):
                self.root.iconbitmap(default=ico)
        except (tk.TclError, OSError):
            pass

    def _setup_styles(self):
        style = ttk.Style(self.root)
        try:
            style.theme_use("clam")  # 只有 clam 允许自定义按钮/下拉框底色
        except tk.TclError:
            pass
        self.style = style

        style.configure(
            "Primary.TButton",
            background=PRIMARY, foreground=WHITE, font=FONT_BUTTON,
            padding=(28, 18), relief="flat", borderwidth=0,
            focuscolor=PRIMARY, anchor="center",
        )
        style.map(
            "Primary.TButton",
            background=[("disabled", LINE), ("pressed", PRIMARY_PRESSED), ("active", PRIMARY_PRESSED)],
            foreground=[("disabled", INK_FAINT), ("pressed", WHITE), ("active", WHITE)],
        )

        style.configure(
            "Ghost.TButton",
            background=CARD, foreground=INK, font=FONT_BUTTON,
            padding=(22, 18), relief="solid", borderwidth=1,
            bordercolor=LINE, lightcolor=LINE, darkcolor=LINE, focuscolor=CARD,
        )
        style.map(
            "Ghost.TButton",
            background=[("disabled", PAPER), ("pressed", PRIMARY_TINT), ("active", PRIMARY_TINT)],
            foreground=[("disabled", INK_FAINT), ("pressed", INK), ("active", INK)],
            bordercolor=[("pressed", PRIMARY), ("active", PRIMARY)],
        )

        style.configure(
            "Accent.TButton",
            background=PRIMARY, foreground=WHITE, font=FONT_BUTTON,
            padding=(24, 16), relief="flat", borderwidth=0,
            focuscolor=PRIMARY, anchor="center",
        )
        style.map(
            "Accent.TButton",
            background=[("disabled", LINE), ("pressed", PRIMARY_PRESSED), ("active", PRIMARY_PRESSED)],
            foreground=[("disabled", INK_FAINT), ("pressed", WHITE), ("active", WHITE)],
        )

        style.configure(
            "TCombobox",
            fieldbackground=CARD, background=CARD, foreground=INK,
            arrowcolor=INK_SOFT, bordercolor=LINE, lightcolor=CARD, darkcolor=CARD,
            selectbackground=PRIMARY_TINT, selectforeground=INK,
            padding=(12, 12), font=FONT_COMBO,
        )
        style.map(
            "TCombobox",
            fieldbackground=[("readonly", CARD), ("disabled", PAPER)],
            background=[("readonly", CARD), ("active", CARD)],
            foreground=[("readonly", INK), ("disabled", INK_FAINT)],
            selectbackground=[("readonly", PRIMARY_TINT)],
            selectforeground=[("readonly", INK)],
            bordercolor=[("focus", PRIMARY), ("hover", PRIMARY)],
            arrowcolor=[("disabled", LINE)],
        )
        # 下拉列表（Listbox）配色，避免原生白底与整体色板冲突。
        self.root.option_add("*TCombobox*Listbox.background", CARD)
        self.root.option_add("*TCombobox*Listbox.foreground", INK)
        self.root.option_add("*TCombobox*Listbox.selectBackground", PRIMARY_TINT)
        self.root.option_add("*TCombobox*Listbox.selectForeground", INK)
        self.root.option_add("*TCombobox*Listbox.font", FONT_BODY)

    # ------------------------------------------------------------ 工具
    def clear(self):
        """清空整个内容区（二级全屏页使用），并让旧异步结果失效。"""
        self._view_generation += 1
        self._request_generation += 1
        self._unbind_wheel()
        for w in self.container.winfo_children():
            w.destroy()
        self._scroll_canvas = None

    def _unbind_wheel(self):
        try:
            self.container.unbind_all("<MouseWheel>")
        except tk.TclError:
            pass

    def card(self, parent, accent=None, padx=20, pady=16):
        """创建一张卡片（CARD 底 + LINE 描边 + 内边距），可选左侧色条。

        返回 (card_frame, body_frame)：card_frame 用于定位，
        body_frame 用于放置内容。
        """
        card = tk.Frame(parent, bg=CARD, highlightbackground=LINE, highlightthickness=1)
        body = tk.Frame(card, bg=CARD, padx=padx, pady=pady)
        if accent:
            tk.Frame(card, bg=accent, width=4).pack(side="left", fill="y")
            body.pack(side="left", fill="both", expand=True)
        else:
            body.pack(fill="both", expand=True)
        return card, body

    def chat_text(self, parent, content, height=24):
        """对话式文本展示框（只读、带滚动条），放在 Card 内使用。"""
        frame = tk.Frame(parent, bg=CARD)
        frame.pack(fill="both", expand=True)

        head = tk.Label(
            frame, text="康康食养AI 说：", bg=CARD, fg=SECONDARY,
            font=FONT_SMALL_BOLD, anchor="w",
        )
        head.pack(fill="x", pady=(0, 8))

        area = tk.Frame(frame, bg=CARD)
        area.pack(fill="both", expand=True)

        txt = tk.Text(
            area, wrap="word", bg=CARD, fg=INK,
            font=FONT_BODY, relief="flat", bd=0, highlightthickness=0,
            padx=0, pady=0, height=height, spacing1=6, spacing3=6,
            insertbackground=INK, selectbackground=PRIMARY_TINT, selectforeground=INK,
        )
        scroll = tk.Scrollbar(
            area, command=txt.yview, relief="flat", bd=0, width=10,
            bg=LINE, activebackground=PRIMARY, troughcolor=PAPER,
        )
        txt.configure(yscrollcommand=scroll.set)
        scroll.pack(side="right", fill="y")
        txt.pack(side="left", fill="both", expand=True)
        render_markdown(txt, content)
        txt.configure(state="disabled")
        return txt

    def _readonly_text(self, parent, content, height=None, max_lines=30):
        """只读纯文本块（隐藏边框与滚动条），用于做法/食养特点。

        height 为 None 时按内容自动撑高（最多 max_lines 行）。
        """
        txt = tk.Text(
            parent, height=max(1, height) if height else 2, wrap="word",
            bg=CARD, fg=INK, font=FONT_BODY, relief="flat", bd=0,
            highlightthickness=0, padx=0, pady=0, spacing1=4, spacing3=4,
            cursor="arrow", selectbackground=PRIMARY_TINT, selectforeground=INK,
        )
        txt.insert("1.0", content)
        txt.configure(state="disabled")
        txt.pack(fill="x")
        if height is None:
            self._fit_text_height(txt, max_lines)
        return txt

    def _fit_text_height(self, txt, max_lines):
        """按实际宽度估算换行后的行数，让只读文本块高度贴合内容。"""
        try:
            txt.update_idletasks()
            width = txt.winfo_width()
            if width <= 1:
                width = 580
            font = tkfont.Font(font=txt.cget("font"))
            char_width = max(1, font.measure("康"))
            per_line = max(1, int((width - 4) / char_width))
            text = txt.get("1.0", "end-1c")
            lines = 0
            for paragraph in text.split("\n"):
                lines += max(1, -(-len(paragraph) // per_line))
            txt.configure(height=max(1, min(max_lines, lines)))
        except tk.TclError:
            pass

    def _scroll_page(self, parent):
        """可滚动的页面容器，返回 (inner_frame, canvas)。"""
        outer = tk.Frame(parent, bg=PAPER)
        outer.pack(fill="both", expand=True)

        canvas = tk.Canvas(outer, bg=PAPER, highlightthickness=0, bd=0)
        scroll = tk.Scrollbar(
            outer, orient="vertical", command=canvas.yview, relief="flat", bd=0,
            width=10, bg=LINE, activebackground=PRIMARY, troughcolor=PAPER,
        )
        canvas.configure(yscrollcommand=scroll.set)
        scroll.pack(side="right", fill="y")
        canvas.pack(side="left", fill="both", expand=True)

        inner = tk.Frame(canvas, bg=PAPER)
        window = canvas.create_window((0, 0), window=inner, anchor="nw")

        def on_canvas(event):
            canvas.itemconfigure(window, width=event.width)

        canvas.bind("<Configure>", on_canvas)
        inner.bind(
            "<Configure>",
            lambda _event: canvas.configure(scrollregion=canvas.bbox("all")),
        )

        def on_wheel(event):
            if isinstance(event.widget, (tk.Text, tk.Listbox)):
                return
            try:
                if not canvas.winfo_exists():
                    return
            except tk.TclError:
                return
            canvas.yview_scroll(int(-1 * (event.delta / 120)), "units")

        # 页面渲染期间全局接管滚轮，避免鼠标停在标签上时无法滚动；
        # 页面销毁时由 _unbind_wheel() 解除。
        canvas.bind_all("<MouseWheel>", on_wheel)

        self._scroll_canvas = canvas
        return inner, canvas

    def _current_scroll(self):
        canvas = self._scroll_canvas
        try:
            if canvas is not None and canvas.winfo_exists():
                return canvas.yview()[0]
        except tk.TclError:
            pass
        return 0.0

    def _restore_scroll(self, offset):
        canvas = self._scroll_canvas
        if not offset or canvas is None:
            return

        def apply():
            try:
                if canvas.winfo_exists():
                    canvas.yview_moveto(offset)
            except tk.TclError:
                pass

        self.container.after_idle(apply)

    def run_async(self, fn, on_done):
        """后台线程执行耗时任务，完成后回主线程更新界面（二级页使用）。"""
        self._request_generation += 1
        view_generation = self._view_generation
        request_generation = self._request_generation

        def deliver(result):
            if (view_generation == self._view_generation
                    and request_generation == self._request_generation):
                on_done(result)

        def worker():
            try:
                result = fn()
            except Exception as exc:  # noqa: BLE001
                result = "发生错误：%s" % exc
            try:
                self.root.after(0, lambda: deliver(result))
            except (RuntimeError, tk.TclError):
                pass  # 窗口已关闭
        threading.Thread(target=worker, daemon=True).start()

    # ------------------------------------------------------------ Tab 外壳
    def _answers_complete(self):
        return all(value is not None for value in self.answers)

    def _enter_screen(self, name):
        if self.screen is None:
            self._return_tab = self.tab
        self.screen = name

    def back_to_tab(self):
        self.screen = None
        self.select_tab(self._return_tab)

    def select_tab(self, tab):
        if tab not in {key for key, _ in TABS}:
            return
        self.tab = tab
        self.screen = None
        # 离开二级页时作废旧异步回调，避免旧结果写进 Tab 页。
        self._view_generation += 1
        self._request_generation += 1
        self._render_tab()

    def _render_tab(self):
        if self.screen is not None:
            return
        offset = self._current_scroll()
        self._unbind_wheel()
        for w in self.container.winfo_children():
            w.destroy()
        self._scroll_canvas = None

        self._build_nav(self.container)
        if self.tab == "home":
            self._render_home()
        elif self.tab == "food":
            self._render_food()
        else:
            self._render_profile()
        self._restore_scroll(offset)

    def _build_nav(self, parent):
        nav = tk.Frame(parent, bg=CARD, highlightbackground=LINE, highlightthickness=1)
        nav.pack(side="top", fill="x")
        row = tk.Frame(nav, bg=CARD)
        row.pack(fill="x", padx=12)

        for key, label in TABS:
            selected = self.tab == key
            cell = tk.Frame(row, bg=CARD, cursor="hand2")
            cell.pack(side="left", fill="both", expand=True)
            content = tk.Frame(cell, bg=CARD, cursor="hand2")
            content.pack(pady=(16, 14))
            widgets = [cell, content]
            icon = load_image("art_tab_%s_24.png" % key)
            if icon is not None:
                icon_label = tk.Label(content, image=icon, bg=CARD, cursor="hand2")
                icon_label.pack(side="left", padx=(0, 6))
                widgets.append(icon_label)
            text = tk.Label(
                content, text=label, bg=CARD,
                fg=PRIMARY if selected else INK_SOFT, font=FONT_TAB,
                cursor="hand2",
            )
            text.pack(side="left")
            indicator = tk.Frame(
                cell, bg=PRIMARY if selected else CARD, height=3,
            )
            indicator.pack(fill="x", side="bottom")
            widgets += [text, indicator]
            for widget in widgets:
                widget.bind("<Button-1>", lambda _event, k=key: self.select_tab(k))
        return nav

    def _bind_click_tree(self, widget, command):
        widget.bind("<Button-1>", lambda _event: command())
        for child in widget.winfo_children():
            self._bind_click_tree(child, command)

    def _empty_state(self, parent, title, message, buttons, icon=None):
        card, body = self.card(parent, padx=24, pady=24)
        card.pack(fill="x", padx=30, pady=(8, 12))
        image = load_image_small(icon) if icon and icon.startswith("state_") else load_image(icon) if icon else None
        if image is not None:
            tk.Label(body, image=image, bg=CARD).pack(pady=(0, 12))
        tk.Label(
            body, text=title, bg=CARD, fg=INK, font=FONT_QUESTION,
            wraplength=560, justify="center",
        ).pack(pady=(0, 8))
        tk.Label(
            body, text=message, bg=CARD, fg=INK_SOFT, font=FONT_BODY,
            wraplength=560, justify="center",
        ).pack(pady=(0, 16))
        row = tk.Frame(body, bg=CARD)
        row.pack()
        for text, style, command in buttons:
            ttk.Button(row, text=text, style=style, command=command).pack(side="left", padx=6)

    def _profile_card(self, parent):
        card, body = self.card(parent, accent=SECONDARY, padx=22, pady=18)
        card.pack(fill="x", padx=30, pady=(0, 12))
        head = tk.Frame(body, bg=CARD)
        head.pack(fill="x")
        tk.Label(
            head, text="体质：%s" % self.constitution, bg=CARD, fg=PRIMARY,
            font=FONT_TITLE,
        ).pack(side="left")
        tk.Label(
            head, text="食养参考", bg=SECONDARY_TINT, fg=SECONDARY,
            font=FONT_TAG, padx=10, pady=3,
        ).pack(side="left", padx=(12, 0))
        tk.Label(
            body, text=self.desc or "", bg=CARD, fg=INK_SOFT, font=FONT_BODY,
            wraplength=580, justify="left", anchor="w",
        ).pack(fill="x", pady=(8, 0))
        return card

    # ------------------------------------------------------------ 首页
    def _reason_for(self, recipe):
        name = recipe.get("菜名", "")
        term = self.recommend_term or self.current_term
        return self.reasons.get(name) or logic.local_reason(term, self.constitution, recipe)

    def _render_home(self):
        inner, _canvas = self._scroll_page(self.container)

        logo = load_image("art_logo_560.png")
        if logo is not None:
            tk.Label(inner, image=logo, bg=PAPER).pack(pady=(8, 2))
        tk.Label(
            inner, text="康康食养AI", bg=PAPER, fg=PRIMARY, font=FONT_BRAND,
        ).pack(pady=(0, 4) if logo is not None else (24, 4))
        tk.Label(
            inner, text="社区老人健康食养助手", bg=PAPER, fg=INK_SOFT,
            font=FONT_SUBTITLE,
        ).pack(pady=(0, 10))

        term_card, term_body = self.card(inner, padx=20, pady=14)
        term_card.pack(fill="x", padx=30, pady=(0, 8))
        tk.Label(
            term_body, text="今日节气：%s" % self.current_term, bg=CARD, fg=INK,
            font=FONT_BODY, anchor="w",
        ).pack(fill="x")

        if not self._answers_complete():
            cta_text, cta_command = "开始健康问卷", lambda: self.show_question(0)
        elif self.constitution and not self.confirmed:
            cta_text, cta_command = "继续查看健康画像", self.show_summary
        else:
            cta_text, cta_command = "查看今日推荐", lambda: self.select_tab("food")

        cta = tk.Frame(inner, bg=PAPER)
        cta.pack(fill="x", padx=30, pady=(2, 12))
        ttk.Button(
            cta, text=cta_text, style="Primary.TButton", command=cta_command,
        ).pack(fill="x")

        hero = load_image("art_hero_120h.png")
        if hero is not None:
            tk.Label(inner, image=hero, bg=PAPER).pack(pady=(0, 12))

        card, body = self.card(inner, accent=PRIMARY, padx=26, pady=18)
        card.pack(fill="x", padx=30, pady=(0, 12))
        tk.Label(
            body, text=GREETING, bg=CARD, fg=INK, font=FONT_BODY,
            wraplength=580, justify="left",
        ).pack(fill="x")

        if self.constitution:
            self._profile_card(inner)

        if self.recipes:
            preview = self.recipes[0]
            p_card, p_body = self.card(inner, padx=20, pady=16)
            p_card.pack(fill="x", padx=30, pady=(0, 12))
            p_card.configure(cursor="hand2")
            tk.Label(
                p_body, text="推荐预览", bg=CARD, fg=SECONDARY,
                font=FONT_SMALL_BOLD, anchor="w",
            ).pack(fill="x")
            head = tk.Frame(p_body, bg=CARD)
            head.pack(fill="x", pady=(6, 0))
            tk.Label(
                head, text=preview.get("菜名", ""), bg=CARD, fg=PRIMARY,
                font=FONT_RECIPE, anchor="w",
            ).pack(side="left")
            if (preview.get("菜系") or "").strip():
                tk.Label(
                    head, text=preview.get("菜系", "").strip(), bg=SECONDARY_TINT,
                    fg=SECONDARY, font=FONT_TAG, padx=10, pady=3,
                ).pack(side="right")
            tk.Label(
                p_body, text=self._reason_for(preview), bg=CARD, fg=INK_SOFT,
                font=FONT_BODY, wraplength=580, justify="left", anchor="w",
            ).pack(fill="x", pady=(8, 0))
            tk.Label(
                p_body, text="点击查看今日推荐 →", bg=CARD, fg=PRIMARY,
                font=FONT_SMALL_BOLD, anchor="w",
            ).pack(fill="x", pady=(8, 0))
            self._bind_click_tree(p_card, lambda: self.select_tab("food"))

        if self.constitution:
            ttk.Button(
                inner, text="重新填写问卷", style="Ghost.TButton", command=self.restart,
            ).pack(pady=(2, 26))

    # ------------------------------------------------------------ 食养
    def _render_food(self):
        inner, _canvas = self._scroll_page(self.container)

        tk.Label(
            inner, text="节气食养", bg=PAPER, fg=PRIMARY, font=FONT_TAG,
        ).pack(pady=(20, 4))
        tk.Label(
            inner, text="时令推荐", bg=PAPER, fg=PRIMARY, font=FONT_TITLE,
        ).pack(pady=(0, 12))

        bar_card, bar = self.card(inner, padx=18, pady=14)
        bar_card.pack(fill="x", padx=30, pady=(0, 12))
        self.term_label = tk.Label(
            bar, text="推荐节气", bg=CARD, fg=INK,
            font=FONT_BODY,
        )
        self.term_label.pack(side="left", padx=(0, 10))
        self.term_combo = ttk.Combobox(
            bar, textvariable=self.term_var, values=logic.SOLAR_TERM_NAMES,
            state="readonly", font=FONT_COMBO, width=7,
        )
        self.term_combo.pack(side="left", padx=(0, 10))
        self.term_combo.bind("<<ComboboxSelected>>", self._on_term_change)
        season = load_image_small(season_art(self.term_var.get()), 12)
        if season is not None:
            tk.Label(inner, image=season, bg=PAPER).pack(pady=(0, 8))
        ttk.Button(
            bar, text="重新推荐", style="Accent.TButton", command=self._request_recommend,
        ).pack(side="left")

        if not self._answers_complete():
            self._empty_state(
                inner, EMPTY_TITLE, EMPTY_MESSAGE,
                [("去填写问卷", "Primary.TButton", lambda: self.show_question(0))],
                icon="state_empty.png",
            )
            return

        # 已确认但尚未请求过推荐时，进入食养自动挑选一次。
        if (self.constitution and self.recommend_status == "idle"
                and not self.recipes):
            self._begin_recommend()

        if self.recommend_status == "loading":
            image = load_image_small("state_loading.png", 12)
            if image is not None:
                tk.Label(inner, image=image, bg=PAPER).pack(pady=(8, 0))
            tk.Label(
                inner, text=LOADING_TEXT, bg=PAPER, fg=INK_SOFT, font=FONT_BODY,
            ).pack(pady=24)
            return

        if self.recommend_status == "error":
            self._empty_state(
                inner, ERROR_TITLE, ERROR_MESSAGE,
                [("重新加载", "Primary.TButton", self._request_recommend)],
                icon="state_error.png",
            )
            return

        if not self.recipes:
            self._empty_state(
                inner, NO_RESULT_TITLE, NO_RESULT_MESSAGE,
                [
                    ("切换节气", "Primary.TButton", self._focus_term),
                    ("重新填写问卷", "Ghost.TButton", self.restart),
                ],
                icon="state_empty.png",
            )
            return

        for recipe in self.recipes:
            self._recipe_card(inner, recipe)

        if self.has_more_recipes:
            ttk.Button(
                inner, text="换一组菜", style="Ghost.TButton", command=self._next_recipe_group,
            ).pack(fill="x", padx=30, pady=(0, 12))

        tk.Label(
            inner, text=DISCLAIMER, bg=PAPER, fg=INK_SOFT, font=FONT_BODY,
            wraplength=620,
        ).pack(pady=(2, 10))
        ttk.Button(
            inner, text="重新填写问卷", style="Ghost.TButton", command=self.restart,
        ).pack(pady=(0, 26))

    def _on_term_change(self, _event=None):
        self.recipe_offset = 0
        term = self.term_var.get() or self.current_term
        try:
            if getattr(self, "term_label", None) is not None and self.term_label.winfo_exists():
                self.term_label.configure(text="推荐节气")
        except tk.TclError:
            pass
        # 生成中切换节气：作废旧请求，按新节气重新挑选，旧结果不得覆盖。
        if self.recommend_status == "loading":
            self._recommend_token += 1
            self.recommend_status = "idle"
            self.recipes = []
            self.reasons = {}
            self.root.after(0, self._refresh_after_term_change)

    def _refresh_after_term_change(self):
        if self.screen is None and self.tab == "food":
            self._render_tab()

    def _focus_term(self):
        combo = getattr(self, "term_combo", None)
        try:
            if combo is not None and combo.winfo_exists():
                combo.focus_set()
                self.root.tk.call("ttk::combobox::Post", str(combo))
        except tk.TclError:
            pass

    def _recipe_card(self, parent, recipe):
        name = recipe.get("菜名", "")
        expanded = name in self._expanded_recipes

        card, body = self.card(parent, padx=20, pady=16)
        card.pack(fill="x", padx=30, pady=(0, 12))

        head = tk.Frame(body, bg=CARD)
        head.pack(fill="x")
        tk.Label(
            head, text=name, bg=CARD, fg=PRIMARY, font=FONT_RECIPE, anchor="w",
        ).pack(side="left")
        cuisine = (recipe.get("菜系") or "").strip()
        illustration = load_image_small(cuisine_art(cuisine), 14)
        if illustration is not None:
            tk.Label(head, image=illustration, bg=CARD).pack(side="right")
        if cuisine:
            tk.Label(
                head, text=cuisine, bg=SECONDARY_TINT, fg=SECONDARY,
                font=FONT_TAG, padx=10, pady=3,
            ).pack(side="right")

        material = (recipe.get("主料") or "").strip()
        if material:
            tk.Label(
                body, text="主料：%s" % material, bg=CARD, fg=INK, font=FONT_BODY,
                wraplength=600, justify="left", anchor="w",
            ).pack(fill="x", pady=(10, 0))

        reason_wrap = tk.Frame(body, bg=PRIMARY_TINT)
        reason_wrap.pack(fill="x", pady=(10, 0))
        tk.Frame(reason_wrap, bg=PRIMARY, width=3).pack(side="left", fill="y")
        tk.Label(
            reason_wrap, text="推荐理由：%s" % self._reason_for(recipe),
            bg=PRIMARY_TINT, fg=INK, font=FONT_BODY, wraplength=560,
            justify="left", anchor="w",
        ).pack(side="left", fill="x", expand=True, padx=(12, 12), pady=10)

        if expanded:
            tk.Label(
                body, text="做法：", bg=CARD, fg=INK, font=FONT_SMALL_BOLD, anchor="w",
            ).pack(fill="x", pady=(10, 2))
            method = (recipe.get("做法简述（扩展）") or "").strip() or "未提供"
            self._readonly_text(body, method)
            feature = (recipe.get("食养特点") or "").strip()
            if feature:
                tk.Label(
                    body, text="食养特点：", bg=CARD, fg=INK,
                    font=FONT_SMALL_BOLD, anchor="w",
                ).pack(fill="x", pady=(10, 2))
                self._readonly_text(body, feature)

        taboo_hint = (recipe.get("忌口提示") or "").strip()
        if taboo_hint:
            tk.Label(
                body, text="忌口提示：%s" % taboo_hint, bg=CARD, fg=INK_SOFT,
                font=FONT_BODY, wraplength=600, justify="left", anchor="w",
            ).pack(fill="x", pady=(10, 0))

        toggle = tk.Button(
            body, text="收起" if expanded else "展开更多", bg=CARD, fg=PRIMARY,
            activebackground=CARD, activeforeground=PRIMARY_PRESSED,
            relief="flat", bd=0, highlightthickness=0, font=FONT_SMALL_BOLD,
            cursor="hand2", command=lambda: self._toggle_recipe(name),
        )
        toggle.pack(anchor="e", pady=(8, 0))
        return card

    def _toggle_recipe(self, name):
        if name in self._expanded_recipes:
            self._expanded_recipes.discard(name)
        else:
            self._expanded_recipes.add(name)
        self._render_tab()

    # ------------------------------------------------------------ 我的
    def _answer_text(self, index):
        value = (self.answers[index] or "").strip() if self.answers[index] else ""
        return value or "未提供"

    def _render_profile(self):
        inner, _canvas = self._scroll_page(self.container)

        tk.Label(
            inner, text="我的", bg=PAPER, fg=PRIMARY, font=FONT_TITLE,
        ).pack(pady=(24, 12))

        if not self.constitution:
            self._empty_state(
                inner, EMPTY_TITLE, EMPTY_MESSAGE,
                [("去填写问卷", "Primary.TButton", lambda: self.show_question(0))],
                icon="state_empty.png",
            )
            return

        hero = load_image("art_hero_120h.png")
        if hero is not None:
            tk.Label(inner, image=hero, bg=PAPER).pack(pady=(0, 12))

        self._profile_card(inner)
        tk.Label(
            inner, text="体质结果依据问卷中的怕冷怕热、出汗、口干和大便回答，供食养参考。",
            bg=PAPER, fg=INK_SOFT, font=FONT_CAPTION, wraplength=660, justify="left",
        ).pack(fill="x", padx=30, pady=(0, 12))

        summary_card, summary_body = self.card(inner, padx=20, pady=16)
        summary_card.pack(fill="x", padx=30, pady=(0, 12))
        head = tk.Frame(summary_body, bg=CARD)
        head.pack(fill="x")
        tk.Label(
            head, text="画像摘要", bg=CARD, fg=INK, font=FONT_QUESTION,
        ).pack(side="left")
        tk.Button(
            head, text="收起" if self._summary_expanded else "展开", bg=CARD,
            fg=PRIMARY, activebackground=CARD, activeforeground=PRIMARY_PRESSED,
            relief="flat", bd=0, highlightthickness=0, font=FONT_SMALL_BOLD,
            cursor="hand2", command=self._toggle_summary,
        ).pack(side="right")
        if self._summary_expanded:
            self.chat_text(summary_body, self.summary_text or "", height=12)

        info_card, info_body = self.card(inner, padx=20, pady=16)
        info_card.pack(fill="x", padx=30, pady=(0, 12))
        tk.Label(
            info_body, text="问卷信息", bg=CARD, fg=INK, font=FONT_QUESTION,
            anchor="w",
        ).pack(fill="x", pady=(0, 8))
        for label, value in (
            ("年龄段", self._answer_text(AGE_INDEX)),
            ("饮食偏好", self._answer_text(PREFERENCE_INDEX)),
            ("忌口", self._answer_text(TABOO_INDEX)),
        ):
            row = tk.Frame(info_body, bg=CARD)
            row.pack(fill="x", pady=2)
            tk.Label(
                row, text=label, bg=CARD, fg=INK_SOFT, font=FONT_BODY,
                width=6, anchor="w",
            ).pack(side="left")
            tk.Label(
                row, text=value, bg=CARD, fg=INK, font=FONT_BODY,
                anchor="w", wraplength=480, justify="left",
            ).pack(side="left", fill="x", expand=True)

        actions = tk.Frame(inner, bg=PAPER)
        actions.pack(fill="x", padx=30, pady=(2, 28))
        ttk.Button(
            actions, text="查看完整画像", style="Primary.TButton",
            command=self.show_summary,
        ).pack(side="left", fill="x", expand=True, padx=(0, 6))
        ttk.Button(
            actions, text="重新填写问卷", style="Ghost.TButton",
            command=self.restart,
        ).pack(side="left", fill="x", expand=True, padx=(6, 0))
        ttk.Button(
            inner, text="修改饮食偏好与忌口", style="Ghost.TButton",
            command=self._edit_food_preferences,
        ).pack(fill="x", padx=30, pady=(0, 18))

    def _edit_food_preferences(self):
        dialog = tk.Toplevel(self.root)
        dialog.title("修改饮食信息")
        dialog.geometry("520x320")
        dialog.transient(self.root)
        dialog.grab_set()
        body = tk.Frame(dialog, bg=PAPER, padx=24, pady=20)
        body.pack(fill="both", expand=True)
        fields = []
        for label, index in (("饮食偏好", PREFERENCE_INDEX), ("忌口食物", TABOO_INDEX)):
            tk.Label(body, text=label, bg=PAPER, fg=INK, font=FONT_BODY).pack(anchor="w")
            entry = tk.Entry(body, font=FONT_BODY, bg=CARD, fg=INK)
            entry.insert(0, self.answers[index] or "")
            entry.pack(fill="x", pady=(4, 14), ipady=8)
            fields.append(entry)

        def save():
            self.answers[PREFERENCE_INDEX] = fields[0].get().strip()
            self.answers[TABOO_INDEX] = fields[1].get().strip()
            self._request_generation += 1
            self.summary_text = logic._local_summary(
                self.constitution, self.desc,
                self.answers[PREFERENCE_INDEX] or "未提供",
                self.answers[TABOO_INDEX] or "未提供",
                *self.answers[:4], self.answers[AGE_INDEX] or "未提供",
            )
            self.recipe_offset = 0
            dialog.destroy()
            self._begin_recommend()
            self._render_tab()

        ttk.Button(body, text="保存", style="Primary.TButton", command=save).pack(fill="x")

    def _toggle_summary(self):
        self._summary_expanded = not self._summary_expanded
        self._render_tab()

    # ------------------------------------------------------------ 推荐请求
    def _begin_recommend(self):
        """启动一次「本地筛选 + AI 理由」请求；旧请求由 token 作废。"""
        term = self.term_var.get() or self.current_term
        self._recommend_token += 1
        token = self._recommend_token
        self.recommend_term = term
        self.recommend_status = "loading"
        self.recipes = []
        self.reasons = {}

        constitution = self.constitution
        taboo = self.answers[TABOO_INDEX] or ""
        preference = self.answers[PREFERENCE_INDEX] or ""
        age_group = self.answers[AGE_INDEX] or ""

        def make():
            recipes = logic.recommend(term, constitution, taboo, preference,
                                      offset=self.recipe_offset)
            more = bool(logic.recommend(term, constitution, taboo, preference,
                                        offset=self.recipe_offset + 3))
            reasons = logic.generate_reasons(
                term, constitution, taboo, preference, recipes, age_group
            )
            return recipes, reasons, more

        def worker():
            try:
                result = make()
            except Exception as exc:  # noqa: BLE001
                result = (None, None, exc)
            try:
                self.root.after(0, lambda: self._finish_recommend(token, result))
            except (RuntimeError, tk.TclError):
                pass

        threading.Thread(target=worker, daemon=True).start()

    def _request_recommend(self):
        if not self._answers_complete():
            self.select_tab("food")
            return
        self._begin_recommend()
        if self.screen is None and self.tab == "food":
            self._render_tab()

    def _next_recipe_group(self):
        if not self.has_more_recipes or self.recommend_status == "loading":
            return
        self.recipe_offset += 3
        self._request_recommend()

    def _finish_recommend(self, token, result):
        if token != self._recommend_token:
            return
        recipes, reasons, more = result
        if isinstance(more, Exception) or recipes is None:
            self.recommend_status = "error"
            self.recipes = []
            self.reasons = {}
        else:
            self.recipes = recipes
            self.reasons = reasons or {}
            self.recommend_status = "done"
            self.has_more_recipes = more
        if self.screen is None and self.tab in ("food", "home"):
            self._render_tab()

    # ------------------------------------------------------------ 问卷
    def show_question(self, idx):
        self._enter_screen("question")
        self.clear()
        title, options, qtype = QUESTIONS[idx]

        tk.Label(
            self.container, text="体质小问卷", bg=PAPER, fg=PRIMARY,
            font=FONT_TAG,
        ).pack(pady=(28, 6))
        tk.Label(
            self.container, text=title, bg=PAPER, fg=INK,
            font=FONT_QUESTION, wraplength=620, justify="center",
        ).pack(pady=(0, 14), padx=40)

        meta = tk.Frame(self.container, bg=PAPER)
        meta.pack(fill="x", padx=48, pady=(0, 10))
        tk.Label(
            meta, text=f"第 {idx + 1} / {len(QUESTIONS)} 题", bg=PAPER, fg=INK_SOFT,
            font=FONT_CAPTION,
        ).pack(side="left")
        self.progress = StepProgress(meta, total=len(QUESTIONS))
        self.progress.pack(side="left", fill="x", expand=True, padx=(14, 0))
        self.progress.set(idx + 1)

        bottom = tk.Frame(self.container, bg=PAPER)
        bottom.pack(side="bottom", fill="x", pady=20)

        if qtype == "choice":
            for opt in options:
                self._option_card(self.container, opt, idx, opt)
        else:
            self.entry_var = tk.StringVar()
            card, body = self.card(self.container, padx=18, pady=16)
            card.pack(fill="x", padx=90, pady=(6, 14))
            entry = tk.Entry(
                body, textvariable=self.entry_var, font=FONT_OPTION,
                bg=CARD, fg=INK, relief="flat", bd=0,
                highlightthickness=1, highlightbackground=LINE, highlightcolor=PRIMARY,
                insertbackground=INK, selectbackground=PRIMARY_TINT, selectforeground=INK,
            )
            entry.pack(fill="x", ipady=10)
            entry.focus_set()
            ttk.Button(
                self.container, text="下一题（留空按“无”）", style="Primary.TButton",
                command=lambda: self._choose(idx, self.entry_var.get()),
            ).pack(pady=(0, 12))

        if idx > 0:
            ttk.Button(
                bottom, text="上一题", style="Ghost.TButton",
                command=lambda: self.show_question(idx - 1),
            ).pack(side="left", padx=28)
        else:
            ttk.Button(
                bottom, text="返回", style="Ghost.TButton", command=self.back_to_tab,
            ).pack(side="left", padx=28)
        ttk.Button(
            bottom, text="重新开始问卷", style="Ghost.TButton", command=self.restart,
        ).pack(side="right", padx=28)

    def _option_card(self, parent, text, idx, value):
        wrapper = tk.Frame(
            parent, bg=CARD, highlightbackground=LINE, highlightthickness=1,
            cursor="hand2",
        )
        wrapper.pack(fill="x", padx=90, pady=7)
        stripe = tk.Frame(wrapper, bg=CARD, width=4)
        stripe.pack(side="left", fill="y")
        button = tk.Button(
            wrapper, text=text, font=FONT_OPTION, bg=CARD, fg=INK,
            activebackground=PRIMARY_TINT, activeforeground=INK,
            relief="flat", bd=0, highlightthickness=0,
            anchor="w", justify="left", wraplength=480,
            padx=18, pady=16, cursor="hand2",
        )
        button.pack(side="left", fill="both", expand=True)

        state = {"selected": False}

        def on_enter(_event):
            if not state["selected"]:
                button.configure(bg=PRIMARY_TINT)

        def on_leave(_event):
            if not state["selected"]:
                button.configure(bg=CARD)

        def on_click():
            if state["selected"]:
                return
            state["selected"] = True
            wrapper.configure(highlightbackground=PRIMARY)
            button.configure(bg=PRIMARY_TINT)
            stripe.configure(bg=PRIMARY)
            generation = self._view_generation
            self.root.after(120, lambda: self._advance(generation, idx, value))

        button.configure(command=on_click)
        for widget in (wrapper, stripe, button):
            widget.bind("<Enter>", on_enter)
            widget.bind("<Leave>", on_leave)
        wrapper.bind("<Button-1>", lambda _event: on_click())

    def _advance(self, generation, idx, value):
        try:
            if generation != self._view_generation:
                return
            self._choose(idx, value)
        except tk.TclError:
            pass

    def _choose(self, idx, value):
        self.answers[idx] = (value or "").strip() if value else ""
        if idx < len(QUESTIONS) - 1:
            self.show_question(idx + 1)
        else:
            self.show_summary()

    def show_summary(self):
        self._enter_screen("summary")
        self.clear()
        tk.Label(
            self.container, text="您的健康画像", bg=PAPER, fg=PRIMARY,
            font=FONT_TITLE,
        ).pack(pady=(26, 12))

        a = self.answers
        self.constitution, self.desc = logic.calculate_constitution(
            a[0], a[1], a[2], a[3]
        )
        age_group = a[AGE_INDEX]
        preference = a[PREFERENCE_INDEX] or "无"
        taboo = a[TABOO_INDEX] or "无"

        hero, hero_body = self.card(self.container, accent=SECONDARY, padx=22, pady=18)
        hero.pack(fill="x", padx=40, pady=(0, 14))
        hero_head = tk.Frame(hero_body, bg=CARD)
        hero_head.pack(fill="x")
        tk.Label(
            hero_head, text="体质：%s" % self.constitution, bg=CARD, fg=PRIMARY,
            font=FONT_TITLE,
        ).pack(side="left")
        tk.Label(
            hero_head, text="食养参考", bg=SECONDARY_TINT, fg=SECONDARY,
            font=FONT_TAG, padx=10, pady=3,
        ).pack(side="left", padx=(12, 0))
        tk.Label(
            hero_body, text=self.desc, bg=CARD, fg=INK_SOFT, font=FONT_BODY,
            wraplength=560, justify="left",
        ).pack(fill="x", pady=(8, 0))

        loading_box = tk.Frame(self.container, bg=PAPER)
        loading_box.pack(pady=(2, 0))
        tk.Label(
            loading_box, text="正在生成画像摘要，请稍候……",
            bg=PAPER, fg=INK_SOFT, font=FONT_BODY,
        ).pack()
        loading_actions = tk.Frame(loading_box, bg=PAPER)
        loading_actions.pack(pady=(12, 0))
        ttk.Button(
            loading_actions, text="返回", style="Ghost.TButton", command=self.back_to_tab,
        ).pack(side="left", padx=6)
        ttk.Button(
            loading_actions, text="重新开始问卷", style="Ghost.TButton",
            command=self.restart,
        ).pack(side="left", padx=6)
        self.container.update_idletasks()

        def make_summary():
            return logic.generate_summary(
                self.constitution, self.desc, preference, taboo,
                a[0], a[1], a[2], a[3], age_group,
            )

        def on_done(result):
            self.summary_text = result
            if loading_box.winfo_exists():
                loading_box.destroy()
            btn = tk.Frame(self.container, bg=PAPER)
            btn.pack(side="bottom", pady=(0, 20))
            ttk.Button(
                btn, text="返回", style="Ghost.TButton", command=self.back_to_tab,
            ).pack(side="left", padx=10)
            ttk.Button(
                btn, text="确认无误", style="Primary.TButton", command=self.confirm_summary,
            ).pack(side="left", padx=10)
            ttk.Button(
                btn, text="重新开始问卷", style="Ghost.TButton", command=self.restart,
            ).pack(side="left", padx=10)
            card, body = self.card(self.container, padx=16, pady=14)
            card.pack(fill="both", expand=True, padx=40, pady=(0, 12))
            self.chat_text(body, result, height=14)

        self.run_async(make_summary, on_done)

    def confirm_summary(self):
        self.confirmed = True
        self.screen = None
        self.tab = "food"
        self._begin_recommend()
        self._render_tab()

    def restart(self):
        self.recipe_offset = 0
        self.has_more_recipes = False
        self.answers = [None] * len(QUESTIONS)
        self.constitution = None
        self.desc = None
        self.confirmed = False
        self.recipes = []
        self.reasons = {}
        self.recommend_status = "idle"
        self.recommend_term = self.current_term
        self.summary_text = None
        self._expanded_recipes.clear()
        self._summary_expanded = False
        self._recommend_token += 1  # 生成中的旧结果不得覆盖新页面
        self.term_var.set(self.current_term)
        self.show_question(0)

    def run(self):
        self.root.mainloop()


def main():
    App().run()


if __name__ == "__main__":
    main()
