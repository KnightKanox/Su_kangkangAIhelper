package com.kangkang.shiyangai.data

import java.time.LocalDate

data class SolarTerm(val name: String, val month: Int, val day: Int)

object SolarTerms {

    val ALL: List<SolarTerm> = listOf(
        SolarTerm("立春", 2, 4),
        SolarTerm("雨水", 2, 19),
        SolarTerm("惊蛰", 3, 5),
        SolarTerm("春分", 3, 20),
        SolarTerm("清明", 4, 5),
        SolarTerm("谷雨", 4, 20),
        SolarTerm("立夏", 5, 5),
        SolarTerm("小满", 5, 21),
        SolarTerm("芒种", 6, 6),
        SolarTerm("夏至", 6, 21),
        SolarTerm("小暑", 7, 7),
        SolarTerm("大暑", 7, 22),
        SolarTerm("立秋", 8, 7),
        SolarTerm("处暑", 8, 23),
        SolarTerm("白露", 9, 7),
        SolarTerm("秋分", 9, 23),
        SolarTerm("寒露", 10, 8),
        SolarTerm("霜降", 10, 23),
        SolarTerm("立冬", 11, 7),
        SolarTerm("小雪", 11, 22),
        SolarTerm("大雪", 12, 7),
        SolarTerm("冬至", 12, 22),
        SolarTerm("小寒", 1, 5),
        SolarTerm("大寒", 1, 20)
    )

    private val NAMES = ALL.map { it.name }.toSet()

    /**
     * 计算当前节气：遍历上表，保留最后一个「年内天数 <= 当前年内天数」的节气。
     * 小寒、大寒（1 月）按「次年 1 月」处理（年内天数 +365），避免跨年误匹配。
     * 1 月 1–4 日无匹配时默认「大寒」。
     */
    fun currentTerm(now: LocalDate = LocalDate.now()): String {
        val m = now.monthValue
        val d = now.dayOfMonth

        fun doy(month: Int, day: Int): Int {
            val monthDays = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
            var sum = 0
            for (i in 1 until month) sum += monthDays[i]
            return sum + day
        }

        var cur = doy(m, d)
        if (m == 1) cur += 365

        var result: String? = null
        for (term in ALL) {
            var o = doy(term.month, term.day)
            if (term.month == 1) o += 365
            if (o <= cur) result = term.name
        }
        return result ?: "大寒"
    }

    /** 循环取上一个节气。 */
    fun previous(name: String): String {
        val idx = ALL.indexOfFirst { it.name == name }
        return if (idx <= 0) ALL.last().name else ALL[idx - 1].name
    }

    /** 循环取下一个节气。 */
    fun next(name: String): String {
        val idx = ALL.indexOfFirst { it.name == name }
        return if (idx < 0 || idx == ALL.lastIndex) ALL.first().name else ALL[idx + 1].name
    }

    fun isValid(name: String): Boolean = name in NAMES
}
