# -*- coding: utf-8 -*-
"""康康食养AI —— 纯逻辑层（不依赖任何第三方库与 GUI）。

包含：数据加载、24节气计算、九种体质计算、菜谱筛选评分、
DeepSeek 大模型调用（urllib）、以及本地回退文案。
"""
import csv
import datetime
import json
import os
import re
import sys
import urllib.request

# ---------------------------------------------------------------- 数据路径
_BASE_DIR = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
_CSV_PATH = os.path.join(_BASE_DIR, "data", "recipes.csv")

DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"
DEEPSEEK_KEY = os.environ.get("DEEPSEEK_API_KEY", "").strip()
DEEPSEEK_MODEL = "deepseek-chat"

DISCLAIMER = "本推荐为食养参考，不构成医疗建议。"

# ---------------------------------------------------------------- 数据加载
def load_recipes():
    """读取 data/recipes.csv，返回 list[dict]，每个 dict 即一行菜谱。"""
    path = _CSV_PATH
    if not os.path.exists(path):
        # 兼容旧版分离数据目录的打包方式。
        alt = os.path.join(os.path.dirname(sys.executable), "data", "recipes.csv")
        if os.path.exists(alt):
            path = alt
    rows = []
    with open(path, encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
    return rows


# ---------------------------------------------------------------- 24 节气
# 顺序（名, 月, 日）：立春起、冬至止，随后小寒、大寒跨年收尾
SOLAR_TERMS = [
    ("立春", 2, 4), ("雨水", 2, 19), ("惊蛰", 3, 5), ("春分", 3, 20),
    ("清明", 4, 5), ("谷雨", 4, 20), ("立夏", 5, 5), ("小满", 5, 21),
    ("芒种", 6, 6), ("夏至", 6, 21), ("小暑", 7, 7), ("大暑", 7, 22),
    ("立秋", 8, 7), ("处暑", 8, 23), ("白露", 9, 7), ("秋分", 9, 23),
    ("寒露", 10, 8), ("霜降", 10, 23), ("立冬", 11, 7), ("小雪", 11, 22),
    ("大雪", 12, 7), ("冬至", 12, 22), ("小寒", 1, 5), ("大寒", 1, 20),
]
SOLAR_TERM_NAMES = [name for name, _, _ in SOLAR_TERMS]


def _doy(month, day):
    """返回 (月, 日) 在非闰年中的年内天数（1 月 1 日为 1）。"""
    return datetime.date(2001, month, day).timetuple().tm_yday


def current_solar_term(month, day):
    """按 (月, 日) 计算当前节气。

    1月1-4日无匹配时默认「大寒」。小寒、大寒按「次年 1 月」处理，
    避免跨年时误匹配到 2-12 月的日期。
    """
    if month == 1 and day <= 4:
        return "大寒"

    cur = _doy(month, day)
    if month == 1:
        cur += 365

    matched = None
    for name, m, d in SOLAR_TERMS:
        o = _doy(m, d)
        if m == 1:
            o += 365
        if o <= cur:
            matched = name

    return matched if matched is not None else "大寒"


# ---------------------------------------------------------------- 九种体质
# 各题选项（顺序对应 A/B/C/D）
Q1_OPTIONS = ["怕冷", "怕热", "正常", "冷热都怕"]
Q2_OPTIONS = ["容易出汗", "很少出汗", "正常", "晚上睡觉出汗"]
Q3_OPTIONS = ["经常口干", "很少口干", "正常", "口苦"]
Q4_OPTIONS = ["偏软不成形", "偏干硬", "正常", "时干时稀"]

DESC_MAP = {
    "平和": "各方面比较均衡，跟着节气吃就好。",
    "气虚": "山药、莲子、红枣这类家常食材比较合适。",
    "阳虚": "生姜、核桃、羊肉这类温热的更合适，生冷的少吃一点。",
    "阴虚": "银耳、雪梨、鸭肉这类滋润的更合适，辛辣的少吃一点。",
    "痰湿": "冬瓜、薏米、陈皮这类更清爽，甜腻的少吃一点。",
    "湿热": "苦瓜、绿豆这类更清爽，油炸辛辣的少吃一点。",
    "血瘀": "山楂、黑木耳这类可以常吃，生冷的少吃一点。",
    "气郁": "陈皮、玫瑰花泡水比较合适。",
    "特禀": "发物类少吃一点，推荐时会先帮你筛一遍。",
}

_PRIORITY = ["阳虚", "气郁", "气虚", "阴虚", "湿热", "痰湿", "血瘀", "特禀", "平和"]


def _letter(options, answer):
    if answer is None:
        return None
    answer = str(answer).strip()
    for i, opt in enumerate(options):
        if answer == opt:
            return "ABCD"[i]
    return None


def calculate_constitution(a1, a2, a3, a4):
    """计算九种体质。

    返回 (体质名, 体质描述)。若全部答案均未匹配任何选项，
    返回 ("参数错误", "未能识别答案文本，请确认传入的内容和问卷选项文字匹配")。
    """
    q1 = _letter(Q1_OPTIONS, a1)
    q2 = _letter(Q2_OPTIONS, a2)
    q3 = _letter(Q3_OPTIONS, a3)
    q4 = _letter(Q4_OPTIONS, a4)

    matched_count = sum(1 for x in (q1, q2, q3, q4) if x is not None)
    if matched_count == 0:
        return ("参数错误", "未能识别答案文本，请确认传入的内容和问卷选项文字匹配")

    scores = {"平和": 0, "气虚": 0, "阳虚": 0, "阴虚": 0,
              "痰湿": 0, "湿热": 0, "血瘀": 0, "气郁": 0, "特禀": 0}

    # 题1 怕冷怕热
    if q1 == "A":
        scores["阳虚"] += 5
    elif q1 == "B":
        scores["阴虚"] += 3
        scores["湿热"] += 2
    elif q1 == "C":
        scores["平和"] += 2
    elif q1 == "D":
        scores["气虚"] += 2
        scores["阳虚"] += 2

    # 题2 出汗
    if q2 == "A":
        scores["气虚"] += 4
        scores["阳虚"] += 2
    elif q2 == "B":
        scores["阳虚"] += 3
        scores["血瘀"] += 2
    elif q2 == "C":
        scores["平和"] += 2
    elif q2 == "D":
        scores["阴虚"] += 4

    # 题3 口干
    if q3 == "A":
        if q1 == "A":
            scores["阳虚"] += 4
        else:
            scores["阴虚"] += 4
            scores["湿热"] += 2
    elif q3 == "B":
        scores["平和"] += 2
        scores["痰湿"] += 1
    elif q3 == "C":
        scores["平和"] += 2
    elif q3 == "D":
        scores["湿热"] += 3
        scores["气郁"] += 2

    # 题4 大便
    if q4 == "A":
        scores["气虚"] += 2
        scores["阳虚"] += 2
        scores["痰湿"] += 3
    elif q4 == "B":
        scores["阴虚"] += 3
        scores["湿热"] += 1
    elif q4 == "C":
        scores["平和"] += 2
    elif q4 == "D":
        scores["气郁"] += 4
        scores["气虚"] += 2

    max_score = max(scores.values())
    top = [k for k, v in scores.items() if v == max_score]

    if max_score < 4:
        constitution = "平和"
    elif len(top) == 1:
        constitution = top[0]
    else:
        constitution = next((n for n in _PRIORITY if n in top), "平和")

    return (constitution, DESC_MAP.get(constitution, ""))


# ---------------------------------------------------------------- 菜谱筛选 + 评分
def _split_taboo(taboo):
    """把忌口文本切分成 token（按顿号/逗号/空格/分号）。"""
    if not taboo:
        return []
    tokens = re.split(r"[、，, ；;]", str(taboo))
    return [t.strip() for t in tokens if t.strip()]


def score_recipe(recipe, constitution, taboo=""):
    """对单道菜谱按规则计算得分（本地确定性）。"""
    score = 0
    yiji = recipe.get("体质宜忌", "") or ""

    if "全质皆宜" in yiji:
        score += 6

    if constitution:
        pos = yiji.find(constitution)
        if pos != -1:
            window = yiji[pos + len(constitution): pos + len(constitution) + 4]
            if "宜" in window and not any(w in window for w in ("宜去", "宜少", "宜减")):
                score += 5

    if any(w in yiji for w in ("忌", "禁", "慎")):
        score -= 1

    for token in _split_taboo(taboo):
        if token in (recipe.get("主料", "") or ""):
            score -= 10
        if token in (recipe.get("忌口提示", "") or ""):
            score -= 3

    return score


def recommend(term, constitution, taboo="", preference="", limit=3, offset=0):
    """按当前节气 + 体质 + 忌口筛选并评分，返回前 limit 道（不足则全部）。

    候选不足时按节气循环顺序，依次取上一个、下一个节气补齐（去重）。
    """
    rows = load_recipes()
    by_term = {}
    for r in rows:
        by_term.setdefault(r.get("节气", ""), []).append(r)

    candidates = []
    seen = set()

    def add(term_name):
        for r in by_term.get(term_name, []):
            if any(token.lower() in (r.get("主料", "") or "").lower()
                   for token in _split_taboo(taboo)):
                continue
            key = r.get("菜名") or r.get("总序号")
            if key not in seen:
                seen.add(key)
                candidates.append(r)

    add(term)

    if term in SOLAR_TERM_NAMES:
        idx = SOLAR_TERM_NAMES.index(term)
        n = len(SOLAR_TERM_NAMES)
        step = 1
        while len(candidates) < offset + limit and step < n:
            prev = SOLAR_TERM_NAMES[(idx - step) % n]
            add(prev)
            if len(candidates) >= offset + limit:
                break
            nxt = SOLAR_TERM_NAMES[(idx + step) % n]
            add(nxt)
            step += 1

    for r in candidates:
        r["_score"] = score_recipe(r, constitution, taboo)

    candidates.sort(key=lambda r: (-r["_score"], int(r.get("总序号") or 0)))
    result = candidates[offset:offset + limit]
    for r in result:
        r.pop("_score", None)
    return result


# ---------------------------------------------------------------- DeepSeek 调用
def deepseek_chat(system, user, timeout=20):
    """调用 DeepSeek。成功返回 (文本, True)，任何异常返回 (None, False)。"""
    if not DEEPSEEK_KEY:
        return (None, False)
    payload = {
        "model": DEEPSEEK_MODEL,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
        "temperature": 0.5,
        "stream": False,
    }
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(DEEPSEEK_URL, data=data, method="POST")
    req.add_header("Authorization", "Bearer " + DEEPSEEK_KEY)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read().decode("utf-8")
        obj = json.loads(body)
        return (obj["choices"][0]["message"]["content"], True)
    except Exception:
        return (None, False)


# ---------------------------------------------------------------- 画像摘要
_SUMMARY_SYSTEM = (
    "你是康康食养AI的健康画像总结助手，面向社区老年居民。"
    "依据用户提交的体质问卷生成通俗、亲切、适合老人阅读的健康画像摘要。"
    "要求：语言通俗短句；内容顺序为九种体质结果→年龄段与饮食偏好、忌口→身体状态；"
    "年龄段仅作为用户提供的信息，不据此推断疾病；排版2-3小段；"
    "严禁编造用户未给出的健康信息，空缺标注【未提供】。"
)


def _display_or(text, default):
    s = (text or "").strip()
    return s if s else default


def generate_summary(constitution, desc, preference, taboo, a1, a2, a3, a4, age_group=""):
    """生成健康画像摘要。优先 DeepSeek，失败回退本地模板。"""
    pref = _display_or(preference, "未提供")
    tab = _display_or(taboo, "未提供")
    age = _display_or(age_group, "未提供")

    user = (
        f"体质类型：{constitution}\n"
        f"体质描述：{desc}\n"
        f"年龄段：{age}\n"
        f"饮食偏好：{pref}\n"
        f"忌口食物：{tab}\n"
        "身体状态（问卷答案）：\n"
        f"- 怕冷怕热：{_display_or(a1, '未提供')}\n"
        f"- 出汗情况：{_display_or(a2, '未提供')}\n"
        f"- 口干情况：{_display_or(a3, '未提供')}\n"
        f"- 大便情况：{_display_or(a4, '未提供')}\n"
    )
    text, ok = deepseek_chat(_SUMMARY_SYSTEM, user)
    if ok and text and text.strip():
        return text.strip()
    return _local_summary(constitution, desc, pref, tab, a1, a2, a3, a4, age)


def _local_summary(constitution, desc, pref, tab, a1, a2, a3, a4, age="未提供"):
    """本地回退画像摘要模板。"""
    body = []
    if a1:
        body.append(f"怕冷怕热方面：{str(a1).strip()}")
    if a2:
        body.append(f"出汗方面：{str(a2).strip()}")
    if a3:
        body.append(f"口干方面：{str(a3).strip()}")
    if a4:
        body.append(f"大便方面：{str(a4).strip()}")
    body_text = "；".join(body) if body else "身体状态【未提供】"

    lines = [
        f"根据您的回答，您的体质偏向「{constitution}」。{desc}",
        f"年龄段：{age}。饮食偏好：{pref}。忌口食物：{tab}。",
        f"身体状态：{body_text}。",
    ]
    return "\n".join(lines)


# ---------------------------------------------------------------- 推荐话术
_RECOMMEND_SYSTEM = (
    "你是康康食养AI的节气菜谱推荐助手，面向社区老年居民，仅普通话。"
    "根据用户体质、忌口、饮食偏好和当前节气，从给定候选菜谱中挑选并推荐3道最合适的菜，"
    "年龄段仅用于调整表述，不据此推断疾病或改变忌口。"
    "用通俗亲切的话术。每道包含：菜名、菜系、适合原因（结合体质/节气）、主料、简单做法。"
    "结尾必须附：本推荐为食养参考，不构成医疗建议。"
)


def generate_recommendation(term, constitution, taboo, preference, recipes, age_group=""):
    """生成节气菜谱推荐话术。优先 DeepSeek，失败回退纯文本列表。

    结尾必定附带「本推荐为食养参考，不构成医疗建议。」。
    """
    pref = _display_or(preference, "未提供")
    tab = _display_or(taboo, "未提供")

    user_lines = [
        f"当前节气：{term}",
        f"用户体质：{constitution}",
        f"年龄段：{_display_or(age_group, '未提供')}",
        f"忌口食物：{tab}",
        f"饮食偏好：{pref}",
        "候选菜谱：",
    ]
    for i, r in enumerate(recipes, 1):
        user_lines.append(
            f"{i}. 菜名：{r.get('菜名', '')}；菜系：{r.get('菜系', '')}；"
            f"主料：{r.get('主料', '')}；食养特点：{r.get('食养特点', '')}；"
            f"做法简述：{r.get('做法简述（扩展）', '')}；体质宜忌：{r.get('体质宜忌', '')}"
        )
    user = "\n".join(user_lines)

    text, ok = deepseek_chat(_RECOMMEND_SYSTEM, user)
    if ok and text and text.strip():
        result = text.strip()
    else:
        result = _local_recommendation(recipes)

    if DISCLAIMER not in result:
        result = result.rstrip() + "\n\n" + DISCLAIMER
    return result


def _local_recommendation(recipes):
    """本地回退推荐：菜名 + 菜系 + 主料 + 做法 纯文本列表。"""
    if not recipes:
        return "暂时没有找到合适的时令菜谱。" + "\n\n" + DISCLAIMER

    lines = ["为您推荐以下时令菜：", ""]
    for i, r in enumerate(recipes, 1):
        lines.append(
            f"{i}. {r.get('菜名', '')}（{r.get('菜系', '')}）"
        )
        lines.append(f"   主料：{r.get('主料', '')}")
        lines.append(f"   做法：{r.get('做法简述（扩展）', '')}")
        lines.append("")
    lines.append(DISCLAIMER)
    return "\n".join(lines)


# ---------------------------------------------------------------- 推荐理由
# 结构化推荐以本地 CSV 为事实来源，模型只负责「每道菜一句理由」。
_REASON_SYSTEM = (
    "你是康康食养AI的菜谱推荐理由助手，面向社区老年居民，仅普通话。"
    "结合节气、用户体质和给定菜谱的食养特点，为每道菜写一句推荐理由。"
    "严格按格式输出 3 行：序号. 菜名｜理由；理由不超过40字；"
    "结合节气、体质和菜谱食养特点；不得编造未提供的食材、功效或疾病信息。"
)

_REASON_LINE = re.compile(r"^\s*\d+[.、．]\s*(.+?)\s*[｜|]\s*(.+?)\s*$")
_REASON_MARKUP = re.compile(r"[*#`]")


def _first_feature_sentence(text, limit=24):
    """取「食养特点」第一个句号之前的内容，并截断到 limit 字。"""
    s = (text or "").strip()
    if not s:
        return ""
    return s.split("。")[0].strip()[:limit]


def local_reason(term, constitution, recipe):
    """本地确定性推荐理由，AI 不可用或未匹配时使用。"""
    feature = _first_feature_sentence(recipe.get("食养特点", ""))
    term = (term or "").strip()
    if constitution:
        if feature:
            return f"{term}时令菜，{feature}，可作为{constitution}体质的日常食养参考。"
        return f"{term}时令菜，可作为{constitution}体质的日常食养参考。"
    if feature:
        return f"{term}时令菜，{feature}，可作为日常食养参考。"
    return f"{term}时令菜，可作为日常食养参考。"


def parse_reasons(text):
    """解析「序号. 菜名｜理由」格式，返回 {菜名: 理由}。"""
    reasons = {}
    for raw_line in str(text or "").replace("\r\n", "\n").split("\n"):
        line = _REASON_MARKUP.sub("", raw_line)
        match = _REASON_LINE.match(line)
        if not match:
            continue
        name = match.group(1).strip()
        reason = match.group(2).strip()
        if name and reason:
            reasons[name] = reason[:40]
    return reasons


def generate_reasons(term, constitution, taboo="", preference="",
                     recipes=(), age_group=""):
    """一次 DeepSeek 调用，仅为给定菜谱生成推荐理由。

    返回 {菜名: 理由}；未配置密钥、调用失败或解析失败时返回 {}。
    """
    if not recipes:
        return {}

    user_lines = [
        f"当前节气：{term}",
        f"用户体质：{constitution or '未提供'}",
        f"年龄段：{_display_or(age_group, '未提供')}",
        f"忌口食物：{_display_or(taboo, '未提供')}",
        f"饮食偏好：{_display_or(preference, '未提供')}",
        "候选菜谱：",
    ]
    for i, r in enumerate(recipes, 1):
        user_lines.append(
            f"{i}. 菜名：{r.get('菜名', '')}；菜系：{r.get('菜系', '')}；"
            f"主料：{r.get('主料', '')}；食养特点：{r.get('食养特点', '')}；"
            f"体质宜忌：{r.get('体质宜忌', '')}"
        )

    text, ok = deepseek_chat(_REASON_SYSTEM, "\n".join(user_lines))
    if not ok or not text:
        return {}
    return parse_reasons(text)
