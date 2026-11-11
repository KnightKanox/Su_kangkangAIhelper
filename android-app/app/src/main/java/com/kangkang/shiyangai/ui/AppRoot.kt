package com.kangkang.shiyangai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangkang.shiyangai.data.SolarTerms

private const val GREETING_TEXT =
    "您好！欢迎使用康康食养AI，我是你的社区营养助手。接下来我会问你几个简单问题，" +
        "了解你的身体状况，之后结合节气为你推荐合适的膳食。我们现在开始啦！"

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

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val state by vm.ui.collectAsState()

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when (state.screen) {
            Screen.GREETING -> GreetingScreen(onStart = vm::start)
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
            Screen.RECOMMEND -> RecommendScreen(
                state = state,
                onSelectTerm = vm::selectTerm,
                onRefill = vm::refill
            )
        }
    }
}

@Composable
private fun GreetingScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "康康食养AI",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = GREETING_TEXT,
            fontSize = 22.sp,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text("开始", fontSize = 24.sp)
        }
    }
}

@Composable
private fun QuestionnaireScreen(
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "第 ${idx + 1} / ${QUESTIONS.size} 题",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = QUESTIONS[idx],
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 38.sp
        )
        Spacer(Modifier.height(24.dp))

        if (OPTIONS[idx].isNotEmpty()) {
            val selected = selectedAnswer(state, idx)
            OPTIONS[idx].forEach { option ->
                val isSelected = selected == option
                OutlinedButton(
                    onClick = { onAnswer(idx, option) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(option, fontSize = 22.sp)
                }
                Spacer(Modifier.height(12.dp))
            }
        } else {
            val value = selectedAnswer(state, idx)
            OutlinedTextField(
                value = value,
                onValueChange = { onAnswer(idx, it) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 22.sp),
                minLines = 2,
                placeholder = { Text("可直接输入，也可留空", fontSize = 20.sp) }
            )
        }

        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            if (idx > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Text("上一题", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
            }
            val canNext = OPTIONS[idx].isEmpty() || selectedAnswer(state, idx).isNotEmpty()
            Button(
                onClick = { if (idx == QUESTIONS.lastIndex) onFinish() else onNext() },
                enabled = canNext,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            ) {
                Text(if (idx == QUESTIONS.lastIndex) "完成" else "下一题", fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRefill, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("重新开始问卷", fontSize = 18.sp)
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

@Composable
private fun SummaryScreen(
    state: AppUiState,
    onConfirm: () -> Unit,
    onRefill: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("您的健康画像", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "体质：${state.constitution}",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(state.constitutionDesc, fontSize = 20.sp, lineHeight = 30.sp)
        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (state.summaryLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("正在生成您的健康画像摘要…", fontSize = 18.sp)
                    }
                } else {
                    MarkdownText(state.summary.ifBlank { "摘要生成失败，请重新填写后重试。" })
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onRefill,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            ) {
                Text("重新开始问卷", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onConfirm,
                enabled = !state.summaryLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            ) {
                Text("确认无误", fontSize = 20.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendScreen(
    state: AppUiState,
    onSelectTerm: (String) -> Unit,
    onRefill: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "${state.selectedTerm} 时令推荐",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "当前节气：${state.currentTerm}",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = state.selectedTerm,
                onValueChange = {},
                readOnly = true,
                label = { Text("切换节气") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SolarTerms.ALL.forEach { term ->
                    DropdownMenuItem(
                        text = { Text(term.name, fontSize = 18.sp) },
                        onClick = {
                            onSelectTerm(term.name)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (state.recommendLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("正在为您挑选时令菜…", fontSize = 18.sp)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MarkdownText(state.recommendationText.ifBlank { "暂无可推荐的菜谱。" })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRefill, modifier = Modifier.fillMaxWidth().height(60.dp)) {
            Text("重新开始问卷", fontSize = 20.sp)
        }
    }
}
