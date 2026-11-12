package com.kangkang.shiyangai.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangkang.shiyangai.ui.theme.KangKangColors

/** 应用外壳：一级 Tab（首页/食养/我的）与问卷、画像两个二级全屏页。 */
@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val state by vm.ui.collectAsState()

    AnimatedContent(
        targetState = state.screen ?: state.tab,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(animationSpec = tween(220)) { it / 40 })
                .togetherWith(fadeOut(tween(160)))
        },
        label = "page"
    ) { page ->
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .background(KangKangColors.Paper)
        ) {
            when (page) {
                is Screen -> SecondaryPage(onBack = vm::backToTab) {
                    when (page) {
                        Screen.QUESTIONNAIRE -> QuestionnaireScreen(
                            state = state,
                            onAnswer = vm::setAnswer,
                            onNext = vm::nextQuestion,
                            onPrevious = vm::previousQuestion,
                            onFinish = vm::finishQuestionnaire,
                            onRefill = vm::refill
                        )

                        Screen.SUMMARY -> SummaryScreen(
                            state = state,
                            onConfirm = vm::confirm,
                            onRefill = vm::refill
                        )
                    }
                }

                is Tab -> Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        when (page) {
                            Tab.HOME -> HomeScreen(
                                state = state,
                                onStartQuestionnaire = vm::openQuestionnaire,
                                onOpenSummary = vm::openSummary,
                                onOpenFood = { vm.selectTab(Tab.FOOD) },
                                onRefill = vm::refill
                            )

                            Tab.FOOD -> FoodScreen(
                                state = state,
                                onSelectTerm = vm::selectTerm,
                                onRetry = vm::retryRecommendation,
                                onNextGroup = vm::nextRecipeGroup,
                                onRefill = vm::refill,
                                onOpenQuestionnaire = vm::openQuestionnaire
                            )

                            Tab.PROFILE -> ProfileScreen(
                                state = state,
                                onOpenQuestionnaire = vm::openQuestionnaire,
                                onOpenSummary = vm::openSummary,
                                onRefill = vm::refill,
                                onUpdateFoodPreferences = vm::updateFoodPreferences
                            )
                        }
                    }
                    BottomTabBar(selected = state.tab, onSelect = vm::selectTab)
                }
            }
        }
    }
}

/** 二级全屏页：顶部「返回」入口（56 高）+ 页面内容。 */
@Composable
private fun SecondaryPage(
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = "返回",
                    fontSize = 18.sp,
                    color = KangKangColors.InkSoft
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}
