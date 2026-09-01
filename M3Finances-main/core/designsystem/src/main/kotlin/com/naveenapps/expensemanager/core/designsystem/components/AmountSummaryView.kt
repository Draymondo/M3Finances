package com.naveenapps.expensemanager.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.designsystem.R
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView

@Composable
fun WidgetHeader(
    title: String,
    subTitle: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                ),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = subTitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun AmountInfoWidget(
    expenseAmount: String,
    incomeAmount: String,
    balanceAmount: String,
    transactionPeriod: String,
    trendText: String = "0.00%",
    modifier: Modifier = Modifier,
) {
    val incomeColor = colorResource(
        id = com.naveenapps.expensemanager.core.common.R.color.green_500,
    )
    val expenseColor = colorResource(
        id = com.naveenapps.expensemanager.core.common.R.color.red_500,
    )

    AppCardView(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(0.dp, Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF15191D))
                .padding(18.dp),
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Color(0xFFB7BDC3),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = balanceAmount,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                    ),
                    color = Color(0xFFF4F6F8),
                )
                Spacer(Modifier.width(12.dp))
                val trendColor = if (trendText.startsWith("-")) expenseColor else incomeColor
                val trendBackground = if (trendText.startsWith("-")) Color(0xFF3A1D22) else Color(0xFF163A2B)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(trendBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = trendColor,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryCard(
                    label = "Sortie",
                    amount = expenseAmount,
                    icon = Icons.Default.ArrowUpward,
                    tintColor = expenseColor,
                    modifier = Modifier.weight(1f),
                    cardColor = Color(0xFF1E2429),
                )
                SummaryCard(
                    label = "Entrée",
                    amount = incomeAmount,
                    icon = Icons.Default.ArrowDownward,
                    tintColor = incomeColor,
                    modifier = Modifier.weight(1f),
                    cardColor = Color(0xFF1E2429),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: String,
    icon: ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
    cardColor: Color = Color(0xFF1E2429),
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tintColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Color(0xFFB7BDC3),
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
            color = Color(0xFFF4F6F8),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
fun AmountInfoWidgetCompact(
    expenseAmount: String,
    incomeAmount: String,
    balanceAmount: String,
    transactionPeriod: String,
    modifier: Modifier = Modifier,
) {
    val incomeColor = colorResource(
        id = com.naveenapps.expensemanager.core.common.R.color.green_500,
    )
    val expenseColor = colorResource(
        id = com.naveenapps.expensemanager.core.common.R.color.red_500,
    )

    AppCardView(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ── Header ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.transaction_summary),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = 0.25f),
                        ),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = transactionPeriod,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Income / Expense / Balance in one row ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompactAmountCell(
                    label = stringResource(id = R.string.income),
                    amount = incomeAmount,
                    icon = Icons.Default.ArrowDownward,
                    tintColor = incomeColor,
                    modifier = Modifier.weight(1f),
                )
                CompactAmountCell(
                    label = stringResource(id = R.string.expense),
                    amount = expenseAmount,
                    icon = Icons.Default.ArrowUpward,
                    tintColor = expenseColor,
                    modifier = Modifier.weight(1f),
                )
                CompactBalanceCell(
                    amount = balanceAmount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CompactAmountCell(
    label: String,
    amount: String,
    icon: ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tintColor.copy(alpha = 0.07f))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
                    .copy(alpha = 0.55f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
            color = tintColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompactBalanceCell(
    amount: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest
                    .copy(alpha = 0.45f),
            )
            .padding(10.dp),
    ) {
        Text(
            text = stringResource(id = R.string.total),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
                .copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@AppPreviewsLightAndDarkMode
@Composable
private fun AmountViewPreview() {
    NaveenAppsPreviewTheme {
        AmountInfoWidget(
            expenseAmount = AMOUNT_VALUE,
            incomeAmount = AMOUNT_VALUE,
            balanceAmount = AMOUNT_VALUE,
            transactionPeriod = "This Month(Oct 2023)",
        )
        AmountInfoWidgetCompact(
            expenseAmount = AMOUNT_VALUE,
            incomeAmount = AMOUNT_VALUE,
            balanceAmount = AMOUNT_VALUE,
            transactionPeriod = "This Month(Oct 2023)",
        )
    }
}