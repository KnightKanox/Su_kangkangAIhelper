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
    val q7: String = "",
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
    private var requestGeneration = 0

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
                currentQuestion = 0,
                q1 = "", q2 = "", q3 = "", q4 = "", q5 = "", q6 = "", q7 = "",
                constitution = "",
                constitutionDesc = "",
                summary = "",
                summaryLoading = false,
                selectedTerm = it.currentTerm,
                recipes = emptyList(),
                recommendationText = "",
                recommendLoading = false
            )
        }
    }

    fun confirm() {
        val generation = ++requestGeneration
        _ui.update {
            it.copy(
                screen = Screen.RECOMMEND,
                recommendLoading = true,
                recommendationText = ""
            )
        }
        runRecommendation(generation)
    }

    fun selectTerm(term: String) {
        val generation = ++requestGeneration
        _ui.update {
            it.copy(
                selectedTerm = term,
                recommendLoading = true,
                recommendationText = ""
            )
        }
        runRecommendation(generation)
    }

    private fun runRecommendation(generation: Int) {
        viewModelScope.launch {
            val st = _ui.value
            val top = RecommendationEngine.recommend(
                allRecipes,
                st.selectedTerm,
                st.constitution,
                st.q7.ifBlank { "无" }
            )
            if (generation != requestGeneration) return@launch
            _ui.update { it.copy(recipes = top) }

            val system = "你是康康食养AI的节气菜谱推荐助手，面向社区老年居民，仅普通话。根据用户体质、忌口、饮食偏好和当前节气，从给定候选菜谱中挑选并推荐3道最合适的菜。年龄段仅用于调整表述，不据此推断疾病或改变忌口。用通俗亲切的话术。每道包含：菜名、菜系、适合原因（结合体质/节气）、主料、简单做法。结尾必须附：本推荐为食养参考，不构成医疗建议。"
            val user = buildRecommendationUser(st, top)
            val ai = DeepSeekClient.chat(system, user)
            if (generation != requestGeneration) return@launch
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
        sb.append("年龄段：${st.q5.ifBlank { "未提供" }}\n")
        sb.append("忌口：${st.q7.ifBlank { "无" }}\n")
        sb.append("饮食偏好：${st.q6.ifBlank { "无" }}\n")
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
