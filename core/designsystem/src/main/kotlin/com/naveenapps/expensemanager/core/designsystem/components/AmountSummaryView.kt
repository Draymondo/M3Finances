package com.naveenapps.expensemanager.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.geometry.Offset

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

private val HeroGradient = androidx.compose.ui.graphics.Brush.linearGradient(
    colors = listOf(
        Color(0xFF144D33),
        Color(0xFF0B281B),
        Color(0xFF07130C),
    ),
)
private val HeroBorder = Color(0xFF26513F)
private val HeroLabel = Color(0xFF9FD4BC)
private val HeroValue = Color(0xFFF2FBF6)
private val HeroCell = Color(0xFFFFFFFF).copy(alpha = 0.08f)
private val HeroCellBorder = Color(0xFFFFFFFF).copy(alpha = 0.08f)

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
        border = BorderStroke(1.dp, HeroBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeroGradient)
                .drawBehind {
                    // Soft emerald halo anchored top-right — the "fintech glow"
                    // from the validated mock (Wallet/Revolut grammar).
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2FA36B).copy(alpha = 0.40f),
                                Color(0xFF2FA36B).copy(alpha = 0f),
                            ),
                            center = Offset(size.width * 0.88f, -size.height * 0.30f),
                            radius = size.width * 0.80f,
                        ),
                    )
                }
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = (stringResource(R.string.total) +
                        if (transactionPeriod.isNotBlank()) "  ·  $transactionPeriod" else "").uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        fontSize = 11.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = HeroLabel.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Neutral translucent trend chip — deliberately NOT green/red
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFFFFF).copy(alpha = 0.10f))
                        .border(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = Color(0xFFDDEBE2),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            AmountText(
                text = balanceAmount,
                numberStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp,
                    fontSize = 32.sp,
                ),
                color = HeroValue,
                unitColor = HeroLabel,
                unitSizeFactor = 0.44f,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryCard(
                    label = stringResource(R.string.expense),
                    amount = expenseAmount,
                    icon = Icons.Default.ArrowUpward,
                    tintColor = expenseColor,
                    modifier = Modifier.weight(1f),
                    cardColor = HeroCell,
                )
                SummaryCard(
                    label = stringResource(R.string.income),
                    amount = incomeAmount,
                    icon = Icons.Default.ArrowDownward,
                    tintColor = incomeColor,
                    modifier = Modifier.weight(1f),
                    cardColor = HeroCell,
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
    cardColor: Color = Color(0xFFFFFFFF).copy(alpha = 0.08f),
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .border(1.dp, HeroCellBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tintColor.copy(alpha = 0.18f)),
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
                color = HeroLabel,
            )
        }

        Spacer(Modifier.height(10.dp))

        AmountText(
            text = amount,
            numberStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                fontSize = 15.sp,
            ),
            color = HeroValue,
            unitColor = HeroLabel,
            unitSizeFactor = 0.52f,
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

    AppCardView(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, HeroBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeroGradient)
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2FA36B).copy(alpha = 0.36f),
                                Color(0xFF2FA36B).copy(alpha = 0f),
                            ),
                            center = Offset(size.width * 0.88f, -size.height * 0.30f),
                            radius = size.width * 0.85f,
                        ),
                    )
                }
                .padding(14.dp),
        ) {
            // ── Header — same grammar as the main hero ─────────────
            Text(
                text = (stringResource(R.string.total) +
                    if (transactionPeriod.isNotBlank()) "  ·  $transactionPeriod" else "").uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    fontSize = 10.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = HeroLabel.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

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
            .clip(RoundedCornerShape(14.dp))
            .background(HeroCell)
            .border(1.dp, HeroCellBorder, RoundedCornerShape(14.dp))
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
                color = HeroLabel,
            )
        }
        Spacer(Modifier.height(4.dp))
        AmountText(
            text = amount,
            numberStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                fontSize = 13.sp,
            ),
            color = HeroValue,
            unitColor = HeroLabel,
            unitSizeFactor = 0.60f,
            maxLines = 1,
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
            .clip(RoundedCornerShape(14.dp))
            .background(HeroCellBorder.copy(alpha = 0.10f))
            .border(1.dp, HeroCellBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Text(
            text = stringResource(id = R.string.total),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = HeroLabel,
        )
        Spacer(Modifier.height(4.dp))
        AmountText(
            text = amount,
            numberStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                fontSize = 13.sp,
            ),
            color = HeroValue,
            unitColor = HeroLabel,
            unitSizeFactor = 0.60f,
            maxLines = 1,
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