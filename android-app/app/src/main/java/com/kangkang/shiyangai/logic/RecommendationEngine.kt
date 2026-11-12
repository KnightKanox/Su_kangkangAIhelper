package com.kangkang.shiyangai.logic

import com.kangkang.shiyangai.data.Recipe
import com.kangkang.shiyangai.data.SolarTerms

/**
 * 本地确定性筛选 + 评分，返回排序后前 3 道（不足 3 道则返回全部）。
 */
object RecommendationEngine {

    fun recommend(
        recipes: List<Recipe>,
        term: String,
        constitution: String,
        taboo: String,
        offset: Int = 0
    ): List<Recipe> {
        val tokens = splitTaboo(taboo)
        val eligible = recipes.filter { recipe ->
            tokens.none { token -> recipe.ingredients.contains(token, ignoreCase = true) }
        }
        val candidates = eligible.filter { it.solarTerm == term }
            .distinctBy { it.totalIndex }
            .toMutableList()
        val seen = candidates.map { it.totalIndex }.toMutableSet()

        var prev = SolarTerms.previous(term)
        var next = SolarTerms.next(term)
        while (candidates.size < offset + 3) {
            var addedAny = false
            for (r in eligible.filter { it.solarTerm == prev }) {
                if (candidates.size >= offset + 3) break
                if (seen.add(r.totalIndex)) {
                    candidates.add(r)
                    addedAny = true
                }
            }
            for (r in eligible.filter { it.solarTerm == next }) {
                if (candidates.size >= offset + 3) break
                if (seen.add(r.totalIndex)) {
                    candidates.add(r)
                    addedAny = true
                }
            }
            if (!addedAny) break
            prev = SolarTerms.previous(prev)
            next = SolarTerms.next(next)
        }

        return candidates
            .map { it to score(it, constitution, tokens) }
            .sortedByDescending { it.second }
            .drop(offset)
            .take(3)
            .map { it.first }
    }

    private fun score(recipe: Recipe, constitution: String, tabooTokens: List<String>): Int {
        var s = 0
        val fit = recipe.constitutionFit

        if (fit.contains("全质皆宜")) s += 6

        val idx = fit.indexOf(constitution)
        if (idx >= 0) {
            val window = fit.substring(idx + constitution.length).take(4)
            val hasNegative = window.contains("宜去") || window.contains("宜少") || window.contains("宜减")
            if (window.contains("宜") && !hasNegative) s += 5
        }

        if (fit.contains("忌") || fit.contains("禁") || fit.contains("慎")) s -= 1

        for (token in tabooTokens) {
            if (token.isEmpty()) continue
            if (recipe.ingredients.contains(token)) s -= 10
            if (recipe.tabooTips.contains(token)) s -= 3
        }
        return s
    }

    private fun splitTaboo(taboo: String): List<String> {
        return taboo.split("、", "，", ",", " ", "；")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
