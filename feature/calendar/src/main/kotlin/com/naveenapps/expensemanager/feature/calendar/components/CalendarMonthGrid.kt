package com.naveenapps.expensemanager.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.expensemanager.core.model.CalendarDayData
import com.naveenapps.expensemanager.feature.calendar.R
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun CalendarMonthGrid(
    days: List<CalendarDayData>,
    selectedDate: Date,
    onDayClick: (Date) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayHeaders = listOf(
        stringResource(R.string.calendar_day_mon),
        stringResource(R.string.calendar_day_tue),
        stringResource(R.string.calendar_day_wed),
        stringResource(R.string.calendar_day_thu),
        stringResource(R.string.calendar_day_fri),
        stringResource(R.string.calendar_day_sat),
        stringResource(R.string.calendar_day_sun),
    )

    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    val selectedCal = remember(selectedDate) {
        Calendar.getInstance().apply { time = selectedDate }
    }
    val checkCal = remember { Calendar.getInstance() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        // En-têtes LUN .. DIM
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            dayHeaders.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Grille des jours (semaines de 7 jours)
        val weeks = days.chunked(7)
        weeks.forEach { weekDays ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                weekDays.forEach { day ->
                    checkCal.time = day.date
                    val isSelected = checkCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                        checkCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)

                    CalendarDayCell(
                        day = day,
                        isSelected = isSelected,
                        numberFormat = numberFormat,
                        onClick = { onDayClick(day.date) },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayData,
    isSelected: Boolean,
    numberFormat: NumberFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greenColor = colorResource(com.naveenapps.expensemanager.core.common.R.color.green_500)
    val redColor = colorResource(com.naveenapps.expensemanager.core.common.R.color.red_500)

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        day.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        // Numéro du jour (avec badge si sélectionné)
        Box(
            modifier = Modifier
                .size(26.dp)
                .then(
                    if (isSelected) {
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                ),
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Revenu du jour en vert (+4 800)
        if (day.totalIncome > 0) {
            Text(
                text = "+${numberFormat.format(day.totalIncome.roundToLong())}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (day.isCurrentMonth) greenColor else greenColor.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        // Dépense du jour en rouge (-1 350)
        if (day.totalExpense > 0) {
            Text(
                text = "-${numberFormat.format(day.totalExpense.roundToLong())}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (day.isCurrentMonth) redColor else redColor.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

