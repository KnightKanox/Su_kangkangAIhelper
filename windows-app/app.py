# -*- coding: utf-8 -*-
"""康康食养AI —— Windows 桌面版 GUI（tkinter，仅标准库）。

流程：问候 → 7 题问卷（分步大按钮/文本框）→ 画像摘要（对话式）→
确认/重填 → 节气菜谱推荐（对话式）。
"""
import datetime
import re
import threading
import tkinter as tk
from tkinter import ttk

import logic

GREETING = (
    "您好！欢迎使用康康食养AI，我是你的社区营养助手。"
    "接下来我会问你几个简单问题，了解你的身体状况，之后结合节气为你推荐合适的膳食。"
    "我们现在开始啦！"
)

AGE_INDEX = 4
PREFERENCE_INDEX = 5
TABOO_INDEX = 6

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
    """在 tkinter.Text 中呈现常见 Markdown 标记。"""
    text_widget.tag_configure("strong", font=("Microsoft YaHei", 12, "bold"))
    text_widget.tag_configure("emphasis", font=("Microsoft YaHei", 12, "italic"))
    text_widget.tag_configure("heading", font=("Microsoft YaHei", 16, "bold"), foreground="#b3541e")
    text_widget.tag_configure("subheading", font=("Microsoft YaHei", 14, "bold"), foreground="#7a5c2e")
    text_widget.tag_configure("code", font=("Consolas", 11), background="#eee8da")
    text_widget.tag_configure("quote", foreground="#6b645a")
    text_widget.tag_configure("link", foreground="#215c8e", underline=True)

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


class App:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("康康食养AI")
        self.root.geometry("760x860")
        self.root.configure(bg="#f5f1e8")

        self.container = tk.Frame(self.root, bg="#f5f1e8")
        self.container.pack(fill="both", expand=True)

        self.answers = [None] * len(QUESTIONS)
        self.constitution = None
        self.desc = None
        self._view_generation = 0
        self._request_generation = 0
        self.current_term = logic.current_solar_term(
            datetime.date.today().month, datetime.date.today().day
        )

        self.show_greeting()

    # ------------------------------------------------------------ 工具
    def clear(self):
        self._view_generation += 1
        self._request_generation += 1
        for w in self.container.winfo_children():
            w.destroy()

    def chat_text(self, parent, content, height=24):
        """对话式文本展示框（只读、带滚动条）。"""
        frame = tk.Frame(parent, bg="#f5f1e8")
        frame.pack(fill="both", expand=True, padx=20, pady=10)

        head = tk.Label(
            frame, text="康康食养AI 说：", bg="#f5f1e8", fg="#7a5c2e",
            font=("Microsoft YaHei", 12, "bold"), anchor="w",
        )
        head.pack(fill="x")

        txt = tk.Text(
            frame, wrap="word", bg="#fffdf7", fg="#3a3226",
            font=("Microsoft YaHei", 12), relief="flat",
            padx=14, pady=12, height=height, spacing1=4, spacing3=4,
        )
        scroll = tk.Scrollbar(frame, command=txt.yview)
        txt.configure(yscrollcommand=scroll.set)
        scroll.pack(side="right", fill="y")
        txt.pack(side="left", fill="both", expand=True)
        render_markdown(txt, content)
        txt.configure(state="disabled")
        return txt

    def run_async(self, fn, on_done):
        """后台线程执行耗时任务，完成后回主线程更新界面。"""
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

    # ------------------------------------------------------------ 页面
    def show_greeting(self):
        self.clear()
        tk.Label(
            self.container, text="康康食养AI", bg="#f5f1e8", fg="#b3541e",
            font=("Microsoft YaHei", 30, "bold"),
        ).pack(pady=(60, 10))
        tk.Label(
            self.container, text="社区老人健康食养助手", bg="#f5f1e8", fg="#7a5c2e",
            font=("Microsoft YaHei", 14),
        ).pack(pady=(0, 30))
        tk.Label(
            self.container, text=GREETING, bg="#f5f1e8", fg="#3a3226",
            font=("Microsoft YaHei", 13), wraplength=620, justify="left",
        ).pack(pady=(0, 40), padx=40)
        tk.Button(
            self.container, text="开始", command=lambda: self.show_question(0),
            font=("Microsoft YaHei", 16, "bold"), bg="#b3541e", fg="white",
            activebackground="#8f4218", activeforeground="white",
            relief="flat", padx=60, pady=12, cursor="hand2",
        ).pack()

    def show_question(self, idx):
        self.clear()
        title, options, qtype = QUESTIONS[idx]

        tk.Label(
            self.container, text="体质小问卷", bg="#f5f1e8", fg="#b3541e",
            font=("Microsoft YaHei", 20, "bold"),
        ).pack(pady=(30, 6))
        tk.Label(
            self.container, text=f"进度 {idx + 1} / {len(QUESTIONS)}",
            bg="#f5f1e8", fg="#8a8a8a", font=("Microsoft YaHei", 11),
        ).pack(pady=(0, 10))
        tk.Label(
            self.container, text=title, bg="#f5f1e8", fg="#3a3226",
            font=("Microsoft YaHei", 15, "bold"), wraplength=620, justify="center",
        ).pack(pady=(10, 20), padx=40)

        bottom = tk.Frame(self.container, bg="#f5f1e8")
        bottom.pack(side="bottom", fill="x", pady=24)

        if qtype == "choice":
            for opt in options:
                tk.Button(
                    self.container, text=opt,
                    command=lambda o=opt: self._choose(idx, o),
                    font=("Microsoft YaHei", 15), bg="#ffffff", fg="#3a3226",
                    activebackground="#f0d9c0", relief="solid", bd=1,
                    width=30, pady=12, cursor="hand2",
                ).pack(pady=7)
        else:
            self.entry_var = tk.StringVar()
            entry = tk.Entry(
                self.container, textvariable=self.entry_var,
                font=("Microsoft YaHei", 15), width=40, relief="solid", bd=1,
            )
            entry.pack(pady=20, ipady=8)
            tk.Button(
                self.container, text="下一题（留空按“无”）",
                command=lambda: self._choose(idx, self.entry_var.get()),
                font=("Microsoft YaHei", 14), bg="#b3541e", fg="white",
                activebackground="#8f4218", relief="flat", padx=30, pady=10,
                cursor="hand2",
            ).pack(pady=10)

        if idx > 0:
            tk.Button(
                bottom, text="上一题", command=lambda: self.show_question(idx - 1),
                font=("Microsoft YaHei", 11), bg="#d9d2c4", fg="#3a3226",
                activebackground="#c5bda9", relief="flat", padx=18, pady=6,
                cursor="hand2",
            ).pack(side="left", padx=30)
        tk.Button(
            bottom, text="重新开始问卷", command=self.restart,
            font=("Microsoft YaHei", 11), bg="#ffffff", fg="#3a3226",
            activebackground="#f0d9c0", relief="solid", bd=1,
            padx=18, pady=6, cursor="hand2",
        ).pack(side="right", padx=30)

    def _choose(self, idx, value):
        self.answers[idx] = (value or "").strip() if value else ""
        if idx < len(QUESTIONS) - 1:
            self.show_question(idx + 1)
        else:
            self.show_summary()

    def show_summary(self):
        self.clear()
        tk.Label(
            self.container, text="您的健康画像", bg="#f5f1e8", fg="#b3541e",
            font=("Microsoft YaHei", 20, "bold"),
        ).pack(pady=(24, 6))

        a = self.answers
        self.constitution, self.desc = logic.calculate_constitution(
            a[0], a[1], a[2], a[3]
        )
        age_group = a[AGE_INDEX]
        preference = a[PREFERENCE_INDEX] or "无"
        taboo = a[TABOO_INDEX] or "无"

        loading = tk.Label(
            self.container, text="正在生成画像摘要，请稍候……",
            bg="#f5f1e8", fg="#8a8a8a", font=("Microsoft YaHei", 12),
        )
        loading.pack(pady=10)
        retry_while_loading = tk.Button(
            self.container, text="重新开始问卷", command=self.restart,
            font=("Microsoft YaHei", 11), bg="#ffffff", fg="#3a3226",
            relief="solid", bd=1, padx=18, pady=6, cursor="hand2",
        )
        retry_while_loading.pack(pady=6)
        self.container.update_idletasks()

        def make_summary():
            return logic.generate_summary(
                self.constitution, self.desc, preference, taboo,
                a[0], a[1], a[2], a[3], age_group,
            )

        def on_done(result):
            if loading.winfo_exists():
                loading.destroy()
            if retry_while_loading.winfo_exists():
                retry_while_loading.destroy()
            self.chat_text(self.container, result, height=16)
            btn = tk.Frame(self.container, bg="#f5f1e8")
            btn.pack(pady=16)
            tk.Button(
                btn, text="确认无误", command=self.show_recommend,
                font=("Microsoft YaHei", 14, "bold"), bg="#b3541e", fg="white",
                activebackground="#8f4218", relief="flat", padx=28, pady=10,
                cursor="hand2",
            ).pack(side="left", padx=12)
            tk.Button(
                btn, text="重新开始问卷", command=self.restart,
                font=("Microsoft YaHei", 14), bg="#ffffff", fg="#3a3226",
                activebackground="#f0d9c0", relief="solid", bd=1,
                padx=28, pady=10, cursor="hand2",
            ).pack(side="left", padx=12)

        self.run_async(make_summary, on_done)

    def restart(self):
        self.answers = [None] * len(QUESTIONS)
        self.constitution = None
        self.desc = None
        self.show_question(0)

    def show_recommend(self):
        self.clear()
        tk.Label(
            self.container, text="节气时令推荐", bg="#f5f1e8", fg="#b3541e",
            font=("Microsoft YaHei", 20, "bold"),
        ).pack(pady=(24, 6))

        bar = tk.Frame(self.container, bg="#f5f1e8")
        bar.pack(pady=6)
        tk.Label(
            bar, text="选择节气：", bg="#f5f1e8", fg="#3a3226",
            font=("Microsoft YaHei", 12),
        ).pack(side="left", padx=(0, 6))
        self.term_var = tk.StringVar(value=self.current_term)
        combo = ttk.Combobox(
            bar, textvariable=self.term_var, values=logic.SOLAR_TERM_NAMES,
            state="readonly", font=("Microsoft YaHei", 12), width=10,
        )
        combo.pack(side="left", padx=(0, 12))
        tk.Button(
            bar, text="重新推荐", command=self._render_recommend,
            font=("Microsoft YaHei", 12), bg="#b3541e", fg="white",
            activebackground="#8f4218", relief="flat", padx=16, pady=6,
            cursor="hand2",
        ).pack(side="left")

        self.recommend_area = tk.Frame(self.container, bg="#f5f1e8")
        self.recommend_area.pack(fill="both", expand=True)

        self._render_recommend()

    def _render_recommend(self):
        term = self.term_var.get() or self.current_term
        taboo = self.answers[TABOO_INDEX] or ""
        preference = self.answers[PREFERENCE_INDEX] or ""
        age_group = self.answers[AGE_INDEX] or ""

        for w in self.recommend_area.winfo_children():
            w.destroy()

        loading = tk.Label(
            self.recommend_area, text="正在生成推荐，请稍候……",
            bg="#f5f1e8", fg="#8a8a8a", font=("Microsoft YaHei", 12),
        )
        loading.pack(pady=10)
        retry_while_loading = tk.Button(
            self.recommend_area, text="重新开始问卷", command=self.restart,
            font=("Microsoft YaHei", 11), bg="#ffffff", fg="#3a3226",
            relief="solid", bd=1, padx=18, pady=6, cursor="hand2",
        )
        retry_while_loading.pack(pady=6)

        def make_recommend():
            recipes = logic.recommend(
                term, self.constitution, taboo, preference
            )
            text = logic.generate_recommendation(
                term, self.constitution, taboo, preference, recipes, age_group
            )
            return text

        def on_done(result):
            if loading.winfo_exists():
                loading.destroy()
            if retry_while_loading.winfo_exists():
                retry_while_loading.destroy()
            self.chat_text(self.recommend_area, result, height=22)
            tk.Button(
                self.recommend_area, text="重新开始问卷",
                command=self.restart,
                font=("Microsoft YaHei", 12), bg="#ffffff", fg="#3a3226",
                activebackground="#f0d9c0", relief="solid", bd=1,
                padx=20, pady=8, cursor="hand2",
            ).pack(pady=(6, 16))

        self.run_async(make_recommend, on_done)

    def run(self):
        self.root.mainloop()


def main():
    App().run()


if __name__ == "__main__":
    main()
