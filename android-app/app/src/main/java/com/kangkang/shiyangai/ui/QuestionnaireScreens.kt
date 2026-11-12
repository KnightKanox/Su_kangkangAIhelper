package com.kangkang.shiyangai.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kangkang.shiyangai.ui.theme.KangKangColors

private val QUESTIONS = listOf(
    "您平时怕冷还是怕热？",
    "您平时出汗情况怎么样？",
    "您平时口干吗？",
    "您大便通常是什么情况？",
    "您的年龄段是？",
    "您有什么饮食偏好吗？（可留空，留空按「无」处理）",
    "您有什么忌口的食物吗？（可留空，留空按「无」处理）"
)

private val OPTIONS = listOf(
    listOf("怕冷", "怕热", "正常", "冷热都怕"),
    listOf("容易出汗", "很少出汗", "正常", "晚上睡觉出汗"),
    listOf("经常口干", "很少口干", "正常", "口苦"),
    listOf("偏软不成形", "偏干硬", "正常", "时干时稀"),
    listOf("60岁以下", "60—69岁", "70—79岁", "80岁及以上"),
    emptyList(),
    emptyList()
)

/** 问卷二级页：7 题逐题作答，题目与选项保持原样。 */
@Composable
fun QuestionnaireScreen(
    state: AppUiState,
    onAnswer: (Int, String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    onRefill: () -> Unit
) {
    val idx = state.currentQuestion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = "第 ${idx + 1} / ${QUESTIONS.size} 题",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(10.dp))
        StepProgress(current = idx + 1, total = QUESTIONS.size)
        Spacer(Modifier.height(14.dp))

        ScreenHeader(label = "体质小问卷", title = QUESTIONS[idx])
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (OPTIONS[idx].isNotEmpty()) {
                val selected = selectedAnswer(state, idx)
                OPTIONS[idx].forEach { option ->
                    OptionCard(
                        text = option,
                        selected = selected == option,
                        onClick = { onAnswer(idx, option) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                val value = selectedAnswer(state, idx)
                SectionCard(contentPadding = PaddingValues(8.dp)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { onAnswer(idx, it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = KangKangColors.Ink),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        placeholder = {
                            Text(
                                text = "可直接输入，也可留空",
                                fontSize = 18.sp,
                                color = KangKangColors.InkFaint
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = KangKangColors.Card,
                            unfocusedContainerColor = KangKangColors.Card,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = KangKangColors.Ink,
                            unfocusedTextColor = KangKangColors.Ink
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            if (idx > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = ButtonShape,
                    border = BorderStroke(1.dp, KangKangColors.Line),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = KangKangColors.Ink
                    )
                ) {
                    Text(text = "上一题", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
            }
            val canNext = OPTIONS[idx].isEmpty() || selectedAnswer(state, idx).isNotEmpty()
            Button(
                onClick = { if (idx == QUESTIONS.lastIndex) onFinish() else onNext() },
                enabled = canNext,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Text(
                    text = if (idx == QUESTIONS.lastIndex) "完成" else "下一题",
                    fontSize = 20.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onRefill,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "重新开始问卷",
                fontSize = 18.sp,
                color = KangKangColors.InkSoft
            )
        }
    }
}

private fun selectedAnswer(state: AppUiState, idx: Int): String = when (idx) {
    0 -> state.q1
    1 -> state.q2
    2 -> state.q3
    3 -> state.q4
    4 -> state.q5
    5 -> state.q6
    else -> state.q7
}

/** 画像二级页：体质卡 + 可滚动画像摘要 + 确认/重新填写。 */
@Composable
fun SummaryScreen(
    state: AppUiState,
    onConfirm: () -> Unit,
    onRefill: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        ScreenHeader(label = "健康画像", title = "您的体质与建议")
        Spacer(Modifier.height(20.dp))

        ProfileCard(
            constitution = state.constitution,
            desc = state.constitutionDesc
        )

        Spacer(Modifier.height(16.dp))

        SectionCard(modifier = Modifier.animateContentSize()) {
            Crossfade(
                targetState = state.summaryLoading,
                animationSpec = tween(220),
                label = "summary"
            ) { loading ->
                if (loading) {
                    LoadingRow("正在生成您的健康画像摘要…")
                } else {
                    MarkdownText(state.summary.ifBlank { "摘要生成失败，请重新填写后重试。" })
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onRefill,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = ButtonShape,
                border = BorderStroke(1.dp, KangKangColors.Line),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KangKangColors.Ink
                )
            ) {
                Text(text = "重新开始问卷", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onConfirm,
                enabled = !state.summaryLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Text(text = "确认无误", fontSize = 20.sp)
            }
        }
    }
}
