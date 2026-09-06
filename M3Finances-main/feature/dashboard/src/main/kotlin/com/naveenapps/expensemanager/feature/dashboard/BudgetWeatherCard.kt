package com.naveenapps.expensemanager.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetUiModel
import com.naveenapps.expensemanager.core.model.BudgetGoalType
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import java.util.Calendar

enum class BudgetWeatherCondition {
    CRITICAL_ALERT, // ⛈️
    WARNING,        // 🌧️
    CAUTION,        // ☁️
    ON_TRACK,       // ⛅
    EXCELLENT,      // ☀️
}

data class BudgetWeatherData(
    val emoji: String,
    val title: String,
    val message: String,
    val condition: BudgetWeatherCondition,
)

/**
 * Fonction commune de calcul de la météo budgétaire paramétrée par goalType :
 *
 * Pour EXPENSE :
 * - dépasser 100% = alerte rouge (budget dépassé)
 * - rythme : être sous le rythme prévu = excellent, dépasser le rythme = warning
 *
 * Pour INCOME :
 * - atteindre ou dépasser 100% = excellent (objectif atteint)
 * - rythme : être en avance sur le rythme prévu = excellent, être en retard = alerte
 */
fun calculateBudgetWeather(
    percent: Float,
    expectedPercent: Float,
    goalType: BudgetGoalType,
): BudgetWeatherData {
    val isExpense = goalType == BudgetGoalType.EXPENSE

    // paceDelta positif = bonne situation financière, négatif = en retard / surconsommation
    // EXPENSE : expectedPercent - percent (avoir consommé moins que prévu au jour J = positif)
    // INCOME  : percent - expectedPercent (avoir encaissé plus que prévu au jour J = positif)
    val paceDelta = if (isExpense) expectedPercent - percent else percent - expectedPercent

    val condition = when {
        isExpense && percent >= 100f -> BudgetWeatherCondition.CRITICAL_ALERT
        !isExpense && (percent >= 100f || paceDelta > 15f) -> BudgetWeatherCondition.EXCELLENT
        paceDelta > 15f -> BudgetWeatherCondition.EXCELLENT
        paceDelta >= 0f -> BudgetWeatherCondition.ON_TRACK
        paceDelta >= -15f -> BudgetWeatherCondition.CAUTION
        isExpense || paceDelta >= -30f -> BudgetWeatherCondition.WARNING
        else -> BudgetWeatherCondition.CRITICAL_ALERT
    }

    val (emoji, title, message) = when (condition) {
        BudgetWeatherCondition.CRITICAL_ALERT -> if (isExpense) {
            Triple("⛈️", "Alerte Rouge", "Budget du mois dépassé !")
        } else {
            Triple("⛈️", "Alerte Rouge", "Fort retard sur tes revenus du mois !")
        }
        BudgetWeatherCondition.WARNING -> if (isExpense) {
            Triple("🌧️", "Attention", "Tu dépenses plus vite que prévu.")
        } else {
            Triple("🌧️", "Attention", "Tes revenus rentrent moins vite que prévu.")
        }
        BudgetWeatherCondition.CAUTION -> if (isExpense) {
            Triple("☁️", "Prudence", "Léger dépassement sur le rythme.")
        } else {
            Triple("☁️", "Prudence", "Léger retard sur tes revenus prévus.")
        }
        BudgetWeatherCondition.ON_TRACK -> if (isExpense) {
            Triple("⛅", "Dans les clous", "Tu respectes bien ton rythme.")
        } else {
            Triple("⛅", "Dans les clous", "Tes rentrées d'argent suivent le rythme.")
        }
        BudgetWeatherCondition.EXCELLENT -> if (isExpense) {
            Triple("☀️", "Excellent", "Tu es en dessous de ton budget !")
        } else {
            if (percent >= 100f) {
                Triple("☀️", "Objectif atteint !", "Objectif de revenus du mois dépassé !")
            } else {
                Triple("☀️", "Excellent", "Tu es en avance sur tes revenus !")
            }
        }
    }

    return BudgetWeatherData(
        emoji = emoji,
        title = title,
        message = message,
        condition = condition,
    )
}

@Composable
fun BudgetWeatherCondition.colors(): Pair<Color, Color> = when (this) {
    BudgetWeatherCondition.CRITICAL_ALERT,
    BudgetWeatherCondition.WARNING -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    BudgetWeatherCondition.CAUTION -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    BudgetWeatherCondition.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    BudgetWeatherCondition.EXCELLENT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
}

@Composable
fun BudgetWeatherCard(
    budgets: List<BudgetUiModel>,
    modifier: Modifier = Modifier,
) {
    // Conserve tous les budgets mensuels (Dépense et Revenu)
    val monthlyBudgets = remember(budgets) {
        budgets.filter { it.periodType == BudgetPeriod.MONTHLY }
    }

    if (monthlyBudgets.isEmpty()) {
        return
    }

    val calendar = Calendar.getInstance()
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val expectedPercent = (dayOfMonth.toFloat() / maxDays.toFloat()) * 100f

    val pagerState = rememberPagerState(
        pageCount = { monthlyBudgets.size }
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val budget = monthlyBudgets[page]
            val weatherData = remember(budget.percent, expectedPercent, budget.goalType) {
                calculateBudgetWeather(
                    percent = budget.percent,
                    expectedPercent = expectedPercent,
                    goalType = budget.goalType,
                )
            }
            val (bgColor, textColor) = weatherData.condition.colors()

            BudgetWeatherCardItem(
                budget = budget,
                weatherData = weatherData,
                bgColor = bgColor,
                textColor = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // Indicateur de pagination (points) si plus d'un budget
        if (monthlyBudgets.size > 1) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(monthlyBudgets.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 8.dp else 6.dp)
                            .background(color, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetWeatherCardItem(
    budget: BudgetUiModel,
    weatherData: BudgetWeatherData,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = weatherData.emoji,
                fontSize = 40.sp,
            )
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = weatherData.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = weatherData.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
    }
}
