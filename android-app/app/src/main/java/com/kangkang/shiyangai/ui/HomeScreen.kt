package com.kangkang.shiyangai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kangkang.shiyangai.R
import com.kangkang.shiyangai.ui.theme.KangKangColors

private const val GREETING_TEXT =
    "您好！欢迎使用康康食养AI，我是你的社区营养助手。接下来我会问你几个简单问题，" +
        "了解你的身体状况，之后结合节气为你推荐合适的膳食。我们现在开始啦！"

/** 首页：品牌字标 + 问候语卡 + 今日节气 + 状态化主 CTA + 体质卡 + 推荐预览。 */
@Composable
fun HomeScreen(
    state: AppUiState,
    onStartQuestionnaire: () -> Unit,
    onOpenSummary: () -> Unit,
    onOpenFood: () -> Unit,
    onRefill: () -> Unit
) {
    val hasConstitution = state.constitution.isNotBlank()
    val hasRecipes = state.recipes.isNotEmpty()

    val ctaText = when {
        !hasConstitution -> "开始健康问卷"
        !hasRecipes -> "继续查看健康画像"
        else -> "查看今日推荐"
    }
    val ctaAction: () -> Unit = when {
        !hasConstitution -> onStartQuestionnaire
        !hasRecipes -> onOpenSummary
        else -> onOpenFood
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.art_brand_logo),
            contentDescription = "康康食养AI",
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(10.dp))

        Text(
            text = "康康食养AI",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "社区老人健康食养助手",
            fontSize = 18.sp,
            lineHeight = 28.sp,
            letterSpacing = 1.sp,
            color = KangKangColors.Secondary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "今日节气：${state.currentTerm}",
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            color = KangKangColors.InkSoft
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = ctaAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(text = ctaText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(R.drawable.art_hero_home),
            contentDescription = null,
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))

        SectionCard {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = GREETING_TEXT,
                    modifier = Modifier.weight(1f),
                    fontSize = 20.sp,
                    lineHeight = 32.sp,
                    color = KangKangColors.Ink
                )
            }
        }

        if (hasConstitution) {
            Spacer(Modifier.height(20.dp))
            ProfileCard(
                constitution = state.constitution,
                desc = state.constitutionDesc
            )
        }

        if (hasRecipes) {
            val first = state.recipes.first()
            val reason = state.recipeReasons[first.name].orEmpty()
            Spacer(Modifier.height(20.dp))
            SectionCard(modifier = Modifier.clickable(onClick = onOpenFood)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = first.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = KangKangColors.Ink,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FoodTag()
                }
                Spacer(Modifier.height(8.dp))
                CuisineTag(text = first.cuisine)
                if (reason.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = reason,
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        color = KangKangColors.Ink
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onRefill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = ACTION_REFILL,
                    fontSize = 18.sp,
                    color = KangKangColors.InkSoft
                )
            }
        }
    }
}
