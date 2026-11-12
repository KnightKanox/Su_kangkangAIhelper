package com.kangkang.shiyangai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kangkang.shiyangai.R
import com.kangkang.shiyangai.data.Recipe
import com.kangkang.shiyangai.ui.theme.KangKangColors

// ---------- 跨页面共享文案（与 Windows 端逐字一致） ----------
internal const val DISCLAIMER_TEXT = "本推荐为食养参考，不构成医疗建议。"
internal const val EMPTY_PROFILE_TITLE = "还没有健康画像"
internal const val EMPTY_PROFILE_MESSAGE = "先完成 7 题问卷，才能按体质和节气为你推荐菜谱。"
internal const val ACTION_GO_QUESTIONNAIRE = "去填写问卷"
internal const val EMPTY_RECIPE_TITLE = "当前节气与忌口条件下暂无合适菜谱"
internal const val EMPTY_RECIPE_MESSAGE = "可以换个节气，或调整忌口后重新填写。"
internal const val ACTION_SWITCH_TERM = "切换节气"
internal const val ACTION_REFILL = "重新填写问卷"
internal const val LOAD_FAIL_TITLE = "推荐加载失败"
internal const val LOAD_FAIL_MESSAGE = "网络或本地数据异常，请重试。"
internal const val ACTION_RETRY = "重新加载"
internal const val LOADING_RECIPE = "正在为您挑选时令菜…"
internal val ButtonShape = RoundedCornerShape(12.dp)

/** 页头：小标签 + Serif 主标题 + 细分隔线。 */
@Composable
fun ScreenHeader(label: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = KangKangColors.Ink
        )
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(KangKangColors.Line)
        )
    }
}

/** 纸卡：卡片底 + 1dp 描边 + 16dp 圆角 + 16dp 内边距。 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KangKangColors.Card)
            .border(1.dp, KangKangColors.Line, RoundedCornerShape(16.dp))
            .padding(contentPadding),
        content = content
    )
}

/** 7 段等宽进度条：已完成/当前段为主色，未达段为描边色。 */
@Composable
fun StepProgress(current: Int, total: Int = 7, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { index ->
            val filled = index < current
            val color by animateColorAsState(
                targetValue = if (filled) MaterialTheme.colorScheme.primary else KangKangColors.Line,
                animationSpec = tween(200),
                label = "stepColor$index"
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

/** 选项卡片：未选为纸卡底 + 描边，选中为浅主色底 + 主色描边 + 左侧色条 + 勾选。 */
@Composable
fun OptionCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val container by animateColorAsState(
        targetValue = if (selected) KangKangColors.PrimaryTint else KangKangColors.Card,
        animationSpec = tween(200),
        label = "optionContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) KangKangColors.Primary else KangKangColors.Line,
        animationSpec = tween(200),
        label = "optionBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = tween(200),
        label = "optionBorderWidth"
    )
    val barColor by animateColorAsState(
        targetValue = if (selected) KangKangColors.Primary else Color.Transparent,
        animationSpec = tween(200),
        label = "optionBar"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(container)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 22.sp,
            lineHeight = 32.sp,
            color = KangKangColors.Ink
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** 加载行：进度指示 + 辅助色提示文字，出现时淡入。 */
@Composable
fun LoadingRow(text: String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(120)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = KangKangColors.InkSoft
            )
        }
    }
}

/** 墨绿「食养参考」标签。 */
@Composable
fun FoodTag(text: String = "食养参考", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(KangKangColors.SecondaryTint)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = KangKangColors.Secondary
        )
    }
}

/** 墨绿菜系胶囊标签。 */
@Composable
fun CuisineTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(KangKangColors.SecondaryTint)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = KangKangColors.Secondary
        )
    }
}

/** 底部三 Tab：高 64，24dp 图标 + 18sp 文字，选中 3dp 主色指示条。 */
@Composable
fun BottomTabBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth(), color = KangKangColors.Card) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(KangKangColors.Line)
            )
            Row(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Tab.values().forEach { tab ->
                    val isSelected = tab == selected
                    val tint = if (isSelected) KangKangColors.Primary else KangKangColors.InkSoft
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelect(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(tabIcon(tab)),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(tint)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = tabLabel(tab),
                                    fontSize = 18.sp,
                                    lineHeight = 26.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = tint
                                )
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth(0.4f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(
                                    if (isSelected) KangKangColors.Primary else Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun tabLabel(tab: Tab): String = when (tab) {
    Tab.HOME -> "首页"
    Tab.FOOD -> "食养"
    Tab.PROFILE -> "我的"
}

private fun tabIcon(tab: Tab): Int = when (tab) {
    Tab.HOME -> R.drawable.art_tab_home
    Tab.FOOD -> R.drawable.art_tab_food
    Tab.PROFILE -> R.drawable.art_tab_profile
}

/** 空态/异常态卡片：可选点缀图标 + 标题、说明、主按钮与可选次按钮。 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    motif: Int? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    SectionCard(modifier = modifier) {
        if (motif != null) {
            Image(
                painter = painterResource(motif),
                contentDescription = null,
                modifier = Modifier.size(88.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = KangKangColors.Ink
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            color = KangKangColors.InkSoft
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(text = primaryText, fontSize = 20.sp)
        }
        if (secondaryText != null && onSecondary != null) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = secondaryText, fontSize = 18.sp, color = KangKangColors.InkSoft)
            }
        }
    }
}

/** 体质结果卡：墨绿竖条 + 体质名 + 描述 + 「食养参考」标签。 */
@Composable
fun ProfileCard(constitution: String, desc: String, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(KangKangColors.Secondary)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "体质：$constitution",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FoodTag()
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = KangKangColors.Ink
                )
            }
        }
    }
}

/** 结构化菜谱卡：事实字段来自本地 CSV，理由为纯文本（不解析 Markdown）。 */
@Composable
fun RecipeCard(
    recipe: Recipe,
    reason: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.titleLarge,
                color = KangKangColors.Ink,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(cuisineArt(recipe.cuisine)),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            CuisineTag(text = recipe.cuisine)
            Spacer(Modifier.width(8.dp))
            FoodTag()
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "主料：${recipe.ingredients}",
            fontSize = 18.sp,
            lineHeight = 28.sp,
            color = KangKangColors.Ink
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(12.dp))
                .background(KangKangColors.PrimaryTint)
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(KangKangColors.Primary)
            )
            Text(
                text = reason,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = KangKangColors.Ink,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }

        if (expanded) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "做法：${recipe.cookingSteps}",
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = KangKangColors.Ink
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "食养特点：${recipe.healthFeatures}",
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = KangKangColors.Ink
            )
        }
        if (recipe.tabooTips.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "忌口提示：${recipe.tabooTips}",
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = KangKangColors.InkSoft
            )
        }

        TextButton(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (expanded) "收起" else "展开更多",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun cuisineArt(cuisine: String): Int = when {
    cuisine.contains("潮汕") -> R.drawable.cuisine_chaoshan
    cuisine.contains("客家") -> R.drawable.cuisine_hakka
    else -> R.drawable.cuisine_guangfu
}
