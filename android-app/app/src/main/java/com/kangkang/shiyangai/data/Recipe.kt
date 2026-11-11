package com.kangkang.shiyangai.data

/**
 * 单道菜谱，字段顺序与 assets/recipes.csv 表头一一对应。
 */
data class Recipe(
    val totalIndex: Int,
    val solarTerm: String,
    val cuisine: String,
    val innerIndex: Int,
    val name: String,
    val currentTerm: String,
    val ingredients: String,
    val healthFeatures: String,
    val constitutionFit: String,
    val tabooTips: String,
    val difficulty: String,
    val costLevel: String,
    val cultureSource: String,
    val cookingSteps: String,
    val sourceFile: String
)
