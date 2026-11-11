package com.kangkang.shiyangai.data

import android.content.Context

/**
 * 从 assets/recipes.csv 读取全部菜谱。CSV 为 UTF-8 且首行带 BOM，行尾为 CRLF。
 */
object RecipeRepository {

    fun load(context: Context): List<Recipe> {
        return context.assets.open("recipes.csv").bufferedReader(Charsets.UTF_8).use { reader ->
            val lines = reader.readLines()
            lines.drop(1).mapNotNull { line -> parseLine(line) }
        }
    }

    private fun parseLine(line: String): Recipe? {
        var trimmed = line.trim()
        trimmed = trimmed.removePrefix("\uFEFF") // 去掉 BOM
        if (trimmed.isEmpty()) return null

        val parts = trimmed.split(",")
        if (parts.size < 15) return null

        return try {
            Recipe(
                totalIndex = parts[0].trim().toInt(),
                solarTerm = parts[1].trim(),
                cuisine = parts[2].trim(),
                innerIndex = parts[3].trim().toInt(),
                name = parts[4].trim(),
                currentTerm = parts[5].trim(),
                ingredients = parts[6].trim(),
                healthFeatures = parts[7].trim(),
                constitutionFit = parts[8].trim(),
                tabooTips = parts[9].trim(),
                difficulty = parts[10].trim(),
                costLevel = parts[11].trim(),
                cultureSource = parts[12].trim(),
                cookingSteps = parts[13].trim(),
                sourceFile = parts[14].trim()
            )
        } catch (e: Exception) {
            null
        }
    }
}
