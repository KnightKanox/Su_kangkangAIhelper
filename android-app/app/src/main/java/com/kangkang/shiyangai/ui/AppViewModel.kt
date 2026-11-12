package com.kangkang.shiyangai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kangkang.shiyangai.data.Recipe
import com.kangkang.shiyangai.data.RecipeRepository
import com.kangkang.shiyangai.data.SolarTerms
import com.kangkang.shiyangai.logic.ConstitutionCalculator
import com.kangkang.shiyangai.logic.RecommendationEngine
import com.kangkang.shiyangai.network.DeepSeekClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 一级 Tab。 */
enum class Tab { HOME, FOOD, PROFILE }

/** 二级全屏页；`null` 表示当前显示 Tab 外壳。 */
enum class Screen { QUESTIONNAIRE, SUMMARY }

data class AppUiState(
    val tab: Tab = Tab.HOME,
    val screen: Screen? = null,
    val currentQuestion: Int = 0,
    val q1: String = "",
    val q2: String = "",
    val q3: String = "",
    val q4: String = "",
    val q5: String = "",
    val q6: String = "",
    val q7: String = "",
    val constitution: String = "",
    val constitutionDesc: String = "",
    val summary: String = "",
    val summaryLoading: Boolean = false,
    val currentTerm: String = SolarTerms.currentTerm(LocalDate.now()),
    val selectedTerm: String = SolarTerms.currentTerm(LocalDate.now()),
    val recipes: List<Recipe> = emptyList(),
    val recipeReasons: Map<String, String> = emptyMap(),
    val recommendLoading: Boolean = false,
    val recommendFailed: Boolean = false,
    val recipeOffset: Int = 0,
    val hasMoreRecipes: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui.asStateFlow()
    private var requestGeneration = 0

    private val allRecipes: List<Recipe> by lazy { RecipeRepository.load(getApplication()) }

    // ---------- 一级 Tab 与二级页 ----------

    fun selectTab(tab: Tab) {
        _ui.update { it.copy(tab = tab, screen = null) }
    }

    fun openQuestionnaire() {
        _ui.update { it.copy(screen = Screen.QUESTIONNAIRE, currentQuestion = 0) }
    }

    fun openSummary() {
        if (_ui.value.constitution.isBlank()) return
        _ui.update { it.copy(screen = Screen.SUMMARY) }
    }

    fun backToTab() {
        _ui.update { it.copy(screen = null) }
    }

    // ---------- 问卷 ----------

    fun setAnswer(question: Int, value: String) {
        _ui.update { st ->
            when (question) {
                0 -> st.copy(q1 = value)
                1 -> st.copy(q2 = value)
                2 -> st.copy(q3 = value)
                3 -> st.copy(q4 = value)
                4 -> st.copy(q5 = value)
                5 -> st.copy(q6 = value)
                6 -> st.copy(q7 = value)
                else -> st
            }
        }
    }

    fun nextQuestion() {
        _ui.update { st ->
            if (st.currentQuestion < 6) st.copy(currentQuestion = st.currentQuestion + 1) else st
        }
    }

    fun previousQuestion() {
        _ui.update { st ->
            if (st.currentQuestion > 0) st.copy(currentQuestion = st.currentQuestion - 1) else st
        }
    }

    fun finishQuestionnaire() {
        val st = _ui.value
        val result = ConstitutionCalculator.calculate(st.q1, st.q2, st.q3, st.q4)
        val age = st.q5.ifBlank { "未提供" }
        val pref = st.q6.ifBlank { "无" }
        val taboo = st.q7.ifBlank { "无" }
        val generation = ++requestGeneration

        _ui.update {
            it.copy(
                screen = Screen.SUMMARY,
                constitution = result.name,
                constitutionDesc = result.desc,
                summary = "",
                summaryLoading = true
            )
        }
        generateSummary(result.name, result.desc, age, pref, taboo, st, generation)
    }

    private fun generateSummary(
        name: String, desc: String, age: String, pref: String, taboo: String,
        answers: AppUiState, generation: Int
    ) {
        viewModelScope.launch {
            val system = "你是康康食养AI的健康画像总结助手，面向社区老年居民。依据用户提交的体质问卷生成通俗、亲切、适合老人阅读的健康画像摘要。要求：语言通俗短句；内容顺序为九种体质结果→年龄段与饮食偏好、忌口→身体状态；年龄段仅作为用户提供的信息，不据此推断疾病；排版2-3小段；严禁编造用户未给出的健康信息，空缺标注【未提供】。"
            val user = "体质类型：$name\n体质描述：$desc\n年龄段：$age\n饮食偏好：$pref\n忌口：$taboo\n身体状态（问卷答案）：\n- 怕冷怕热：${answers.q1}\n- 出汗情况：${answers.q2}\n- 口干情况：${answers.q3}\n- 大便情况：${answers.q4}"
            val ai = DeepSeekClient.chat(system, user)
            if (generation != requestGeneration) return@launch
            _ui.update {
                it.copy(
                    summary = ai ?: fallbackSummary(name, desc, age, pref, taboo, answers),
                    summaryLoading = false
                )
            }
        }
    }

    private fun fallbackSummary(
        name: String, desc: String, age: String, pref: String, taboo: String,
        answers: AppUiState
    ): String {
        return "您的体质类型：$name。$desc\n\n年龄段：$age。饮食偏好：$pref。忌口：$taboo。\n\n身体状态：怕冷怕热方面：${answers.q1}；出汗方面：${answers.q2}；口干方面：${answers.q3}；大便方面：${answers.q4}。"
    }

    fun refill() {
        requestGeneration++
        _ui.update {
            it.copy(
                screen = Screen.QUESTIONNAIRE,
                tab = Tab.HOME,
                currentQuestion = 0,
                q1 = "", q2 = "", q3 = "", q4 = "", q5 = "", q6 = "", q7 = "",
                constitution = "",
                constitutionDesc = "",
                summary = "",
                summaryLoading = false,
                selectedTerm = it.currentTerm,
                recipes = emptyList(),
                recipeReasons = emptyMap(),
                recommendLoading = false,
                recommendFailed = false,
                recipeOffset = 0,
                hasMoreRecipes = false
            )
        }
    }

    // ---------- 画像确认与推荐 ----------

    fun confirm() {
        val generation = ++requestGeneration
        _ui.update {
            it.copy(
                screen = null,
                tab = Tab.FOOD,
                recipes = emptyList(),
                recipeReasons = emptyMap(),
                recommendLoading = true,
                recommendFailed = false
            )
        }
        runRecommendation(generation)
    }

    fun retryRecommendation() {
        if (_ui.value.constitution.isBlank()) return
        val generation = ++requestGeneration
        _ui.update {
            it.copy(recommendLoading = true, recommendFailed = false)
        }
        runRecommendation(generation)
    }

    fun selectTerm(term: String) {
        val st = _ui.value
        if (st.constitution.isBlank()) {
            _ui.update { it.copy(selectedTerm = term) }
            return
        }
        val generation = ++requestGeneration
        _ui.update {
            it.copy(selectedTerm = term, recipeOffset = 0, recommendLoading = true, recommendFailed = false)
        }
        runRecommendation(generation)
    }

    fun nextRecipeGroup() {
        if (_ui.value.recommendLoading || !_ui.value.hasMoreRecipes) return
        val generation = ++requestGeneration
        _ui.update { it.copy(recipeOffset = it.recipeOffset + 3, recommendLoading = true) }
        runRecommendation(generation)
    }

    fun updateFoodPreferences(preference: String, taboo: String) {
        val st = _ui.value
        if (st.constitution.isBlank()) return
        val generation = ++requestGeneration
        _ui.update {
            it.copy(q6 = preference.trim(), q7 = taboo.trim(), recipeOffset = 0,
                summaryLoading = true, recommendLoading = true)
        }
        val updated = _ui.value
        generateSummary(updated.constitution, updated.constitutionDesc,
            updated.q5.ifBlank { "未提供" }, updated.q6.ifBlank { "无" },
            updated.q7.ifBlank { "无" }, updated, generation)
        runRecommendation(generation)
    }

    /**
     * 本地筛选先出卡片与本地理由，随后异步请求 AI 理由并按菜名逐条回填。
     * AI 失败、解析失败或未匹配时保留本地理由，不重新筛选菜谱。
     */
    private fun runRecommendation(generation: Int) {
        viewModelScope.launch {
            val st = _ui.value
            val term = st.selectedTerm
            val constitution = st.constitution
            val top = try {
                RecommendationEngine.recommend(
                    allRecipes,
                    term,
                    constitution,
                    st.q7.ifBlank { "无" },
                    st.recipeOffset
                )
            } catch (e: Exception) {
                if (generation == requestGeneration) {
                    _ui.update {
                        it.copy(
                            recipes = emptyList(),
                            recipeReasons = emptyMap(),
                            recommendLoading = false,
                            recommendFailed = true
                        )
                    }
                }
                return@launch
            }
            if (generation != requestGeneration) return@launch

            val localReasons = top.associate { it.name to localReason(it, term, constitution) }
            val hasMore = RecommendationEngine.recommend(
                allRecipes, term, constitution, st.q7.ifBlank { "无" }, st.recipeOffset + 3
            ).isNotEmpty()
            _ui.update {
                it.copy(
                    recipes = top,
                    recipeReasons = localReasons,
                    recommendLoading = false,
                    recommendFailed = false,
                    hasMoreRecipes = hasMore
                )
            }
            if (top.isEmpty()) return@launch

            val ai = DeepSeekClient.chat(REASON_SYSTEM_PROMPT, buildReasonUser(st, top))
            if (generation != requestGeneration) return@launch
            val parsed = parseReasons(ai, top)
            if (parsed.isNotEmpty()) {
                _ui.update { state -> state.copy(recipeReasons = state.recipeReasons + parsed) }
            }
        }
    }

    private fun buildReasonUser(st: AppUiState, recipes: List<Recipe>): String {
        val sb = StringBuilder()
        sb.append("当前节气：${st.selectedTerm}\n")
        sb.append("体质：${st.constitution.ifBlank { "未提供" }}\n")
        sb.append("饮食偏好：${st.q6.ifBlank { "无" }}\n")
        sb.append("忌口：${st.q7.ifBlank { "无" }}\n")
        sb.append("候选菜谱：\n")
        recipes.forEachIndexed { i, r ->
            sb.append(
                "${i + 1}. ${r.name}｜${r.cuisine}｜主料：${r.ingredients}｜" +
                    "食养特点：${r.healthFeatures}｜体质宜忌：${r.constitutionFit}\n"
            )
        }
        return sb.toString()
    }

    private fun parseReasons(text: String?, recipes: List<Recipe>): Map<String, String> {
        if (text.isNullOrBlank()) return emptyMap()
        val names = recipes.map { it.name }.toSet()
        val result = mutableMapOf<String, String>()
        text.replace("\r\n", "\n").lines().forEach { line ->
            val match = REASON_LINE.find(line) ?: return@forEach
            val name = match.groupValues[1].trim()
            val reason = cleanReason(match.groupValues[2])
            if (name in names && reason.isNotEmpty()) result[name] = reason
        }
        return result
    }

    private fun cleanReason(raw: String): String =
        raw.replace("\n", " ")
            .replace(Regex("[*#`_>]+"), "")
            .trim()
            .take(40)

    /** 本地理由模板：节气 + 食养特点首句（≤24 字）+ 体质。 */
    private fun localReason(recipe: Recipe, term: String, constitution: String): String {
        val feature = recipe.healthFeatures.substringBefore("。").trim().take(24)
        val suffix = if (constitution.isBlank()) {
            "可作为日常食养参考。"
        } else {
            "可作为${constitution}体质的日常食养参考。"
        }
        return if (feature.isEmpty()) "${term}时令菜，${suffix}" else "${term}时令菜，${feature}，${suffix}"
    }

    private companion object {
        const val REASON_SYSTEM_PROMPT =
            "你是康康食养AI的节气菜谱推荐助手，面向社区老年居民。只输出每道菜的推荐理由。" +
                "严格按格式输出 3 行：序号. 菜名｜理由。理由不超过40字，结合节气、体质和菜谱的食养特点，" +
                "不得编造未提供的食材、功效或疾病信息。"

        val REASON_LINE = Regex("""^\s*\d+[.、．]\s*(.+?)\s*[｜|]\s*(.+?)\s*$""")
    }
}
