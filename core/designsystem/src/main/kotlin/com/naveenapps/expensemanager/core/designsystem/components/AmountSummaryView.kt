package com.naveenapps.expensemanager.core.designsystem.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
import com.naveenapps.expensemanager.core.designsystem.ui.components.AmountText
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import kotlin.math.abs

// ── Héros émeraude ──────────────────────────────────────────────────────
// Dégradé linéaire profond + halo radial en haut à droite. Le halo est
// volontairement très doux : il guide le regard vers le total sans jamais
// gêner la lecture des chiffres (qui sont blancs sur les deux thèmes).
private val HeroGradientStart = Color(0xFF0E3F2B)
private val HeroGradientMiddle = Color(0xFF0A2C20)
private val HeroGradientEnd = Color(0xFF071F16)
private val HeroHalo = Color(0x473DDC84)

private val HeroIncome = Color(0xFF3DDC84)
private val HeroExpense = Color(0xFFFF6B64)
private val HeroOnIncome = Color(0xFF7BF0B0)
private val HeroOnExpense = Color(0xFFFF8A80)

private val HeroShape = RoundedCornerShape(22.dp)
private val HeroCellShape = RoundedCornerShape(14.dp)

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

/**
 * Carte « héros » du tableau de bord : total du mois sur un dégradé émeraude,
 * chip de tendance honnête (neutre à 0,00 %, jamais verte sur un zéro), puis
 * dépense / revenu en deux cellules translucides.
 */
@Composable
fun AmountInfoWidget(
    expenseAmount: String,
    incomeAmount: String,
    balanceAmount: String,
    transactionPeriod: String,
    trendText: String = "0.00%",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            HeroGradientStart,
                            HeroGradientMiddle,
                            HeroGradientEnd,
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(HeroHalo, Color.Transparent),
                        center = Offset(size.width * 0.85f, 0f),
                        radius = size.width * 0.85f,
                    ),
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.09f), HeroShape)
            .padding(18.dp),
    ) {
        Text(
            text = listOf(stringResource(R.string.total), transactionPeriod)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            ),
            color = Color.White.copy(alpha = 0.62f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            AmountText(
                amount = balanceAmount,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    letterSpacing = (-1).sp,
                ),
                color = Color.White,
            )
            Spacer(Modifier.width(10.dp))
            TrendChip(trendText = trendText)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeroSplitCell(
                label = stringResource(R.string.expense),
                amount = expenseAmount,
                icon = Icons.Default.ArrowUpward,
                iconTint = HeroOnExpense,
                iconBackground = Color(0x2EFF6B64),
                modifier = Modifier.weight(1f),
            )
            HeroSplitCell(
                label = stringResource(R.string.income),
                amount = incomeAmount,
                icon = Icons.Default.ArrowDownward,
                iconTint = HeroOnIncome,
                iconBackground = Color(0x2E3DDC84),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroSplitCell(
    label: String,
    amount: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(HeroCellShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), HeroCellShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(10.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(6.dp))

        AmountText(
            amount = amount,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = (-0.3).sp,
            ),
            color = Color.White,
        )
    }
}

/**
 * Chip de tendance : rouge si négative, verte si positive, **neutre** (gris
 * translucide) quand la variation est nulle — un « 0,00 % » vert serait un
 * mensonge visuel.
 */
@Composable
private fun TrendChip(
    trendText: String,
    modifier: Modifier = Modifier,
) {
    val trimmed = trendText.trim()
    val numeric = trimmed
        .removePrefix("+")
        .removePrefix("-")
        .removePrefix("−")
        .removeSuffix("%")
        .trim()
        .toDoubleOrNull()

    val isNeutral = numeric == null || abs(numeric) < 0.005
    val isNegative = !isNeutral && (
        trimmed.startsWith("-") || trimmed.startsWith("−")
        )

    val chipBackground = when {
        isNeutral -> Color.White.copy(alpha = 0.12f)
        isNegative -> Color(0x29FF6B64)
        else -> Color(0x293DDC84)
    }
    val chipContent = when {
        isNeutral -> Color.White.copy(alpha = 0.75f)
        isNegative -> HeroOnExpense
        else -> HeroOnIncome
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(chipBackground)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = trimmed,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
            color = chipContent,
            maxLines = 1,
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
        AmountText(
            amount = amount,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
            color = tintColor,
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
        AmountText(
            amount = amount,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
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
