package com.kangkang.shiyangai.logic

data class ConstitutionResult(val name: String, val desc: String)

/**
 * 九种体质计算，严格按需求中的计分规则实现。
 */
object ConstitutionCalculator {

    private val DESCRIPTIONS = mapOf(
        "平和" to "各方面比较均衡，跟着节气吃就好。",
        "气虚" to "山药、莲子、红枣这类家常食材比较合适。",
        "阳虚" to "生姜、核桃、羊肉这类温热的更合适，生冷的少吃一点。",
        "阴虚" to "银耳、雪梨、鸭肉这类滋润的更合适，辛辣的少吃一点。",
        "痰湿" to "冬瓜、薏米、陈皮这类更清爽，甜腻的少吃一点。",
        "湿热" to "苦瓜、绿豆这类更清爽，油炸辛辣的少吃一点。",
        "血瘀" to "山楂、黑木耳这类可以常吃，生冷的少吃一点。",
        "气郁" to "陈皮、玫瑰花泡水比较合适。",
        "特禀" to "发物类少吃一点，推荐时会先帮你筛一遍。"
    )

    private val PRIORITY = listOf("阳虚", "气郁", "气虚", "阴虚", "湿热", "痰湿", "血瘀", "特禀", "平和")

    private val ALL_NAMES = listOf("平和", "气虚", "阳虚", "阴虚", "痰湿", "湿热", "血瘀", "气郁", "特禀")

    fun calculate(q1: String, q2: String, q3: String, q4: String): ConstitutionResult {
        val a1 = codeOfQuestion1(q1)
        val a2 = codeOfQuestion2(q2)
        val a3 = codeOfQuestion3(q3)
        val a4 = codeOfQuestion4(q4)

        if (a1 == null && a2 == null && a3 == null && a4 == null) {
            return ConstitutionResult("参数错误", "未能识别答案文本，请确认传入的内容和问卷选项文字匹配")
        }

        val scores = ALL_NAMES.associateWith { 0 }.toMutableMap()

        // 题1
        when (a1) {
            "A" -> scores["阳虚"] = scores.getValue("阳虚") + 5
            "B" -> {
                scores["阴虚"] = scores.getValue("阴虚") + 3
                scores["湿热"] = scores.getValue("湿热") + 2
            }
            "C" -> scores["平和"] = scores.getValue("平和") + 2
            "D" -> {
                scores["气虚"] = scores.getValue("气虚") + 2
                scores["阳虚"] = scores.getValue("阳虚") + 2
            }
        }

        // 题2
        when (a2) {
            "A" -> {
                scores["气虚"] = scores.getValue("气虚") + 4
                scores["阳虚"] = scores.getValue("阳虚") + 2
            }
            "B" -> {
                scores["阳虚"] = scores.getValue("阳虚") + 3
                scores["血瘀"] = scores.getValue("血瘀") + 2
            }
            "C" -> scores["平和"] = scores.getValue("平和") + 2
            "D" -> scores["阴虚"] = scores.getValue("阴虚") + 4
        }

        // 题3
        when (a3) {
            "A" -> if (a1 == "A") {
                scores["阳虚"] = scores.getValue("阳虚") + 4
            } else {
                scores["阴虚"] = scores.getValue("阴虚") + 4
                scores["湿热"] = scores.getValue("湿热") + 2
            }
            "B" -> {
                scores["平和"] = scores.getValue("平和") + 2
                scores["痰湿"] = scores.getValue("痰湿") + 1
            }
            "C" -> scores["平和"] = scores.getValue("平和") + 2
            "D" -> {
                scores["湿热"] = scores.getValue("湿热") + 3
                scores["气郁"] = scores.getValue("气郁") + 2
            }
        }

        // 题4
        when (a4) {
            "A" -> {
                scores["气虚"] = scores.getValue("气虚") + 2
                scores["阳虚"] = scores.getValue("阳虚") + 2
                scores["痰湿"] = scores.getValue("痰湿") + 3
            }
            "B" -> {
                scores["阴虚"] = scores.getValue("阴虚") + 3
                scores["湿热"] = scores.getValue("湿热") + 1
            }
            "C" -> scores["平和"] = scores.getValue("平和") + 2
            "D" -> {
                scores["气郁"] = scores.getValue("气郁") + 4
                scores["气虚"] = scores.getValue("气虚") + 2
            }
        }

        val max = scores.values.maxOrNull() ?: 0
        val top = scores.filterValues { it == max }.keys

        val name = when {
            max < 4 -> "平和"
            top.size == 1 -> top.first()
            else -> PRIORITY.first { it in top }
        }

        return ConstitutionResult(name, DESCRIPTIONS[name] ?: "")
    }

    private fun codeOfQuestion1(option: String): String? = when (option) {
        "怕冷" -> "A"
        "怕热" -> "B"
        "正常" -> "C"
        "冷热都怕" -> "D"
        else -> null
    }

    private fun codeOfQuestion2(option: String): String? = when (option) {
        "容易出汗" -> "A"
        "很少出汗" -> "B"
        "正常" -> "C"
        "晚上睡觉出汗" -> "D"
        else -> null
    }

    private fun codeOfQuestion3(option: String): String? = when (option) {
        "经常口干" -> "A"
        "很少口干" -> "B"
        "正常" -> "C"
        "口苦" -> "D"
        else -> null
    }

    private fun codeOfQuestion4(option: String): String? = when (option) {
        "偏软不成形" -> "A"
        "偏干硬" -> "B"
        "正常" -> "C"
        "时干时稀" -> "D"
        else -> null
    }
}
