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

enum class Screen { GREETING, QUESTIONNAIRE, SUMMARY, RECOMMEND }

data class AppUiState(
    val screen: Screen = Screen.GREETING,
    val currentQuestion: Int = 0,
    val q1: String = "",
    val q2: String = "",
    val q3: String = "",
    val q4: String = "",
    val q5: String = "",
    val q6: String = "",
    val constitution: String = "",
    val constitutionDesc: String = "",
    val summary: String = "",
    val summaryLoading: Boolean = false,
    val currentTerm: String = SolarTerms.currentTerm(LocalDate.now()),
    val selectedTerm: String = SolarTerms.currentTerm(LocalDate.now()),
    val recipes: List<Recipe> = emptyList(),
    val recommendationText: String = "",
    val recommendLoading: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui.asStateFlow()

    private val allRecipes: List<Recipe> by lazy { RecipeRepository.load(getApplication()) }

    fun start() {
        _ui.update { it.copy(screen = Screen.QUESTIONNAIRE, currentQuestion = 0) }
    }

    fun setAnswer(question: Int, value: String) {
        _ui.update { st ->
            when (question) {
                0 -> st.copy(q1 = value)
                1 -> st.copy(q2 = value)
                2 -> st.copy(q3 = value)
                3 -> st.copy(q4 = value)
                4 -> st.copy(q5 = value)
                5 -> st.copy(q6 = value)
                else -> st
            }
        }
    }

    fun nextQuestion() {
        _ui.update { st ->
            if (st.currentQuestion < 5) st.copy(currentQuestion = st.currentQuestion + 1) else st
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
        val pref = st.q5.ifBlank { "无" }
        val taboo = st.q6.ifBlank { "无" }

        _ui.update {
            it.copy(
                screen = Screen.SUMMARY,
                constitution = result.name,
                constitutionDesc = result.desc,
                summary = "",
                summaryLoading = true
            )
        }
        generateSummary(result.name, result.desc, pref, taboo)
    }

    private fun generateSummary(name: String, desc: String, pref: String, taboo: String) {
        viewModelScope.launch {
            val system = "你是康康食养AI的健康画像总结助手，面向社区老年居民。依据用户提交的体质问卷生成通俗、亲切、适合老人阅读的健康画像摘要。要求：语言通俗短句；内容顺序为九种体质结果→饮食偏好与忌口→身体状态；排版2-3小段；严禁编造用户未给出的健康信息，空缺标注【未提供】。"
            val user = "体质类型：$name\n体质描述：$desc\n饮食偏好：$pref\n忌口：$taboo"
            val ai = DeepSeekClient.chat(system, user)
            _ui.update {
                it.copy(
                    summary = ai ?: fallbackSummary(name, desc, pref, taboo),
                    summaryLoading = false
                )
            }
        }
    }

    private fun fallbackSummary(name: String, desc: String, pref: String, taboo: String): String {
        return "您的体质类型：$name。$desc\n\n饮食偏好：$pref\n忌口：$taboo\n\n身体状态：问卷结果如上，具体健康状态请以专业评估为准。"
    }

    fun refill() {
        _ui.update {
            it.copy(
                screen = Screen.QUESTIONNAIRE,
                currentQuestion = 0,
                q1 = "", q2 = "", q3 = "", q4 = "", q5 = "", q6 = "",
                constitution = "",
                constitutionDesc = "",
                summary = "",
                summaryLoading = false
            )
        }
    }

    fun confirm() {
        _ui.update {
            it.copy(
                screen = Screen.RECOMMEND,
                recommendLoading = true,
                recommendationText = ""
            )
        }
        runRecommendation()
    }

    fun selectTerm(term: String) {
        _ui.update {
            it.copy(
                selectedTerm = term,
                recommendLoading = true,
                recommendationText = ""
            )
        }
        runRecommendation()
    }

    private fun runRecommendation() {
        viewModelScope.launch {
            val st = _ui.value
            val top = RecommendationEngine.recommend(
                allRecipes,
                st.selectedTerm,
                st.constitution,
                st.q6.ifBlank { "无" }
            )
            _ui.update { it.copy(recipes = top) }

            val system = "你是康康食养AI的节气菜谱推荐助手，面向社区老年居民，仅普通话。根据用户体质、忌口、饮食偏好和当前节气，从给定候选菜谱中挑选并推荐3道最合适的菜，用通俗亲切的话术。每道包含：菜名、菜系、适合原因（结合体质/节气）、主料、简单做法。结尾必须附：本推荐为食养参考，不构成医疗建议。"
            val user = buildRecommendationUser(st, top)
            val ai = DeepSeekClient.chat(system, user)
            _ui.update {
                it.copy(
                    recommendationText = ai ?: fallbackRecommendation(top),
                    recommendLoading = false
                )
            }
        }
    }

    private fun buildRecommendationUser(st: AppUiState, recipes: List<Recipe>): String {
        val sb = StringBuilder()
        sb.append("当前节气：${st.selectedTerm}\n")
        sb.append("体质：${st.constitution}\n")
        sb.append("忌口：${st.q6.ifBlank { "无" }}\n")
        sb.append("饮食偏好：${st.q5.ifBlank { "无" }}\n")
        sb.append("候选菜谱：\n")
        recipes.forEachIndexed { i, r ->
            sb.append("${i + 1}. ${r.name}｜${r.cuisine}｜主料：${r.ingredients}｜做法：${r.cookingSteps}\n")
        }
        return sb.toString()
    }

    private fun fallbackRecommendation(recipes: List<Recipe>): String {
        val sb = StringBuilder()
        recipes.forEachIndexed { i, r ->
            sb.append("${i + 1}. ${r.name}（${r.cuisine}）\n")
            sb.append("主料：${r.ingredients}\n")
            sb.append("做法：${r.cookingSteps}\n\n")
        }
        sb.append("本推荐为食养参考，不构成医疗建议。")
        return sb.toString()
    }
}
