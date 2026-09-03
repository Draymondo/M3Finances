package com.naveenapps.expensemanager.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetUiModel
import java.util.Calendar

@Composable
fun BudgetWeatherCard(
    budgets: List<BudgetUiModel>,
    modifier: Modifier = Modifier
) {
    // Find the monthly budget
    val monthlyBudget = budgets.firstOrNull { it.periodType == com.naveenapps.expensemanager.core.model.BudgetPeriod.MONTHLY }
    
    if (monthlyBudget == null) {
        return // Hide if no monthly budget is set
    }

    val percentSpent = monthlyBudget.percent
    
    // Calculate expected percent based on the day of the month
    val calendar = Calendar.getInstance()
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val expectedPercent = (dayOfMonth.toFloat() / maxDays.toFloat()) * 100f
    
    val diff = expectedPercent - percentSpent

    val (emoji, title, message, bgColor, textColor) = when {
        percentSpent >= 100f -> listOf(
            "⛈️", 
            "Alerte Rouge", 
            "Budget du mois dépassé !", 
            MaterialTheme.colorScheme.errorContainer, 
            MaterialTheme.colorScheme.onErrorContainer
        )
        diff < -15f -> listOf(
            "🌧️", 
            "Attention", 
            "Tu dépenses plus vite que prévu.", 
            MaterialTheme.colorScheme.errorContainer, 
            MaterialTheme.colorScheme.onErrorContainer
        )
        diff < 0f -> listOf(
            "☁️", 
            "Prudence", 
            "Léger dépassement sur le rythme.", 
            MaterialTheme.colorScheme.surfaceVariant, 
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        diff > 15f -> listOf(
            "☀️", 
            "Excellent", 
            "Tu es en dessous de ton budget !", 
            MaterialTheme.colorScheme.primaryContainer, 
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        else -> listOf(
            "⛅", 
            "Dans les clous", 
            "Tu respectes bien ton rythme.", 
            MaterialTheme.colorScheme.secondaryContainer, 
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor as androidx.compose.ui.graphics.Color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = emoji as String,
                fontSize = 40.sp
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title as String,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor as androidx.compose.ui.graphics.Color
                )
                Text(
                    text = message as String,
                    style = MaterialTheme.typography.bodyMedium,
                    color = (textColor as androidx.compose.ui.graphics.Color).copy(alpha = 1f)
                )
            }
        }
    }
}

