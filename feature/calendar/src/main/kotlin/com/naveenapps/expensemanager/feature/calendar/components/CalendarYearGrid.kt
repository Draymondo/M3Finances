package com.naveenapps.expensemanager.feature.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.model.CalendarYearMonthData
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun CalendarYearGrid(
    months: List<CalendarYearMonthData>,
    onMonthClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val greenColor = colorResource(com.naveenapps.expensemanager.core.common.R.color.green_500)
    val redColor = colorResource(com.naveenapps.expensemanager.core.common.R.color.red_500)

    // 12 mois découpés en 4 lignes de 3 colonnes
    val rows = months.chunked(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { rowMonths ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowMonths.forEach { monthData ->
                    AppCardView(
                        modifier = Modifier.weight(1f),
                        onClick = { onMonthClick(monthData.month) },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = monthData.monthName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Revenu en vert
                            if (monthData.totalIncome > 0) {
                                Text(
                                    text = "+${numberFormat.format(monthData.totalIncome.roundToLong())}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = greenColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            } else {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                )
                            }

                            // Dépense en rouge
                            if (monthData.totalExpense > 0) {
                                Text(
                                    text = "-${numberFormat.format(monthData.totalExpense.roundToLong())}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = redColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            } else {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Net
                            val net = monthData.netAmount
                            val netColor = when {
                                net > 0 -> greenColor
                                net < 0 -> redColor
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = "${if (net > 0) "+" else ""}${numberFormat.format(net.roundToLong())}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = netColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

