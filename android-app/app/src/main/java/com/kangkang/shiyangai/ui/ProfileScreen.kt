package com.kangkang.shiyangai.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kangkang.shiyangai.R
import com.kangkang.shiyangai.ui.theme.KangKangColors

/** 我的 Tab：体质卡 + 可折叠画像摘要 + 问卷摘要 + 查看完整画像/重新填写。 */
@Composable
fun ProfileScreen(
    state: AppUiState,
    onOpenQuestionnaire: () -> Unit,
    onOpenSummary: () -> Unit,
    onRefill: () -> Unit,
    onUpdateFoodPreferences: (String, String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        ScreenHeader(label = "我的", title = "健康档案")
        Spacer(Modifier.height(20.dp))

        if (state.constitution.isBlank()) {
            EmptyState(
                title = EMPTY_PROFILE_TITLE,
                message = EMPTY_PROFILE_MESSAGE,
                primaryText = ACTION_GO_QUESTIONNAIRE,
                onPrimary = onOpenQuestionnaire,
                motif = R.drawable.state_empty
            )
        } else {
            Image(
                painter = painterResource(R.drawable.art_hero_home),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            ProfileContent(state = state, onOpenSummary = onOpenSummary,
                onRefill = onRefill, onEditPreferences = { editing = true })
        }
    }
    if (editing) {
        var preference by remember { mutableStateOf(state.q6) }
        var taboo by remember { mutableStateOf(state.q7) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("修改饮食信息") },
            text = {
                Column {
                    OutlinedTextField(value = preference, onValueChange = { preference = it },
                        label = { Text("饮食偏好") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = taboo, onValueChange = { taboo = it },
                        label = { Text("忌口食物") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateFoodPreferences(preference, taboo)
                    editing = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun ProfileContent(
    state: AppUiState,
    onOpenSummary: () -> Unit,
    onRefill: () -> Unit,
    onEditPreferences: () -> Unit
) {
    var summaryExpanded by remember { mutableStateOf(false) }

    ProfileCard(
        constitution = state.constitution,
        desc = state.constitutionDesc
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "体质结果依据问卷中的怕冷怕热、出汗、口干和大便回答，供食养参考。",
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = KangKangColors.InkSoft
    )

    Spacer(Modifier.height(16.dp))

    SectionCard(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable { summaryExpanded = !summaryExpanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "健康画像摘要",
                fontSize = 20.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Medium,
                color = KangKangColors.Ink,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (summaryExpanded) "收起" else "展开",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (summaryExpanded) {
            Spacer(Modifier.height(10.dp))
            if (state.summaryLoading) {
                LoadingRow("正在生成您的健康画像摘要…")
            } else {
                MarkdownText(state.summary.ifBlank { "摘要生成失败，请重新填写后重试。" })
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    SectionCard {
        Text(
            text = "问卷信息",
            fontSize = 20.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Medium,
            color = KangKangColors.Ink
        )
        Spacer(Modifier.height(10.dp))
        ProfileInfoRow(label = "年龄段", value = state.q5.ifBlank { "未提供" })
        ProfileInfoRow(label = "饮食偏好", value = state.q6.ifBlank { "未提供" })
        ProfileInfoRow(label = "忌口", value = state.q7.ifBlank { "未提供" })
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onEditPreferences) { Text("修改饮食偏好与忌口", fontSize = 18.sp) }
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onOpenSummary,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Text(text = "查看完整画像", fontSize = 20.sp)
    }

    Spacer(Modifier.height(10.dp))

    OutlinedButton(
        onClick = onRefill,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = ButtonShape,
        border = BorderStroke(1.dp, KangKangColors.Line),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = KangKangColors.Ink
        )
    ) {
        Text(text = ACTION_REFILL, fontSize = 18.sp)
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            color = KangKangColors.InkSoft,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            color = KangKangColors.Ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.6f)
        )
    }
}
