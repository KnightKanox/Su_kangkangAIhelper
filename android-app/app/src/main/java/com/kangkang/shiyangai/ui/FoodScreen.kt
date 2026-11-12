package com.kangkang.shiyangai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kangkang.shiyangai.data.SolarTerms
import com.kangkang.shiyangai.ui.theme.KangKangColors

/** 食养 Tab：节气选择 + 3 张结构化菜谱卡 + 空态/异常态 + 固定免责声明。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    state: AppUiState,
    onSelectTerm: (String) -> Unit,
    onRetry: () -> Unit,
    onNextGroup: () -> Unit,
    onRefill: () -> Unit,
    onOpenQuestionnaire: () -> Unit
) {
    var showTerms by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        ScreenHeader(label = "节气食养", title = "时令推荐")
        Spacer(Modifier.height(16.dp))

        Surface(
            onClick = { showTerms = true },
            modifier = Modifier.heightIn(min = 56.dp),
            shape = RoundedCornerShape(999.dp),
            color = KangKangColors.PrimaryTint
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "推荐节气",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.selectedTerm,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(seasonArt(state.selectedTerm)),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "今日节气：${state.currentTerm}",
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = KangKangColors.InkSoft
            )
        }
        Spacer(Modifier.height(20.dp))

        when {
            state.constitution.isBlank() -> EmptyState(
                title = EMPTY_PROFILE_TITLE,
                message = EMPTY_PROFILE_MESSAGE,
                primaryText = ACTION_GO_QUESTIONNAIRE,
                onPrimary = onOpenQuestionnaire,
                motif = R.drawable.state_empty
            )

            state.recommendLoading -> SectionCard {
                Image(painterResource(R.drawable.state_loading), null,
                    modifier = Modifier.size(88.dp))
                LoadingRow(LOADING_RECIPE)
            }

            state.recommendFailed -> EmptyState(
                title = LOAD_FAIL_TITLE,
                message = LOAD_FAIL_MESSAGE,
                primaryText = ACTION_RETRY,
                onPrimary = onRetry,
                motif = R.drawable.state_error
            )

            state.recipes.isEmpty() -> EmptyState(
                title = EMPTY_RECIPE_TITLE,
                message = EMPTY_RECIPE_MESSAGE,
                primaryText = ACTION_SWITCH_TERM,
                onPrimary = { showTerms = true },
                motif = R.drawable.state_empty,
                secondaryText = ACTION_REFILL,
                onSecondary = onRefill
            )

            else -> {
                state.recipes.forEachIndexed { index, recipe ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    RecipeCard(
                        recipe = recipe,
                        reason = state.recipeReasons[recipe.name].orEmpty(),
                        expanded = expandedId == recipe.totalIndex,
                        onToggle = {
                            expandedId =
                                if (expandedId == recipe.totalIndex) null else recipe.totalIndex
                        }
                    )
                }
                if (state.hasMoreRecipes) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        onClick = onNextGroup,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = KangKangColors.PrimaryTint
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("换一组菜", fontSize = 20.sp, color = KangKangColors.Primary)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = DISCLAIMER_TEXT,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    color = KangKangColors.InkSoft
                )
            }
        }

        Spacer(Modifier.height(12.dp))
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

    if (showTerms) {
        ModalBottomSheet(
            onDismissRequest = { showTerms = false },
            sheetState = sheetState,
            containerColor = KangKangColors.Card
        ) {
            Text(
                text = ACTION_SWITCH_TERM,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(SolarTerms.ALL, key = { it.name }) { term ->
                    val selected = term.name == state.selectedTerm
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else KangKangColors.PrimaryTint
                            )
                            .clickable {
                                onSelectTerm(term.name)
                                showTerms = false
                            }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = term.name,
                            fontSize = 18.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun seasonArt(term: String): Int = when (SolarTerms.ALL.indexOfFirst { it.name == term }) {
    in 0..5 -> R.drawable.season_spring
    in 6..11 -> R.drawable.season_summer
    in 12..17 -> R.drawable.season_autumn
    else -> R.drawable.season_winter
}
