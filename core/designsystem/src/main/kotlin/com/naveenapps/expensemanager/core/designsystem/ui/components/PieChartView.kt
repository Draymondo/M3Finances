package com.naveenapps.expensemanager.core.designsystem.ui.components

import android.widget.LinearLayout
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.naveenapps.expensemanager.core.designsystem.components.AmountText
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter

data class PieChartUiData(
    val name: String,
    val value: Float,
    val color: Int,
)

@Composable
fun PieChartView(
    totalAmountText: String,
    chartData: List<PieChartUiData>,
    modifier: Modifier = Modifier,
    chartHeight: Int = 600,
    hideValues: Boolean = false,
    chartWidth: Int = LinearLayout.LayoutParams.MATCH_PARENT,
    centerTopText: String? = null,
) {
    var isAnimated by remember { mutableStateOf(false) }

    val holeColor = MaterialTheme.colorScheme.background.toArgb()
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
        .copy(alpha = 0.5f).toArgb()

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = chartData, label = "pie_chart") { pieChartData ->
            AndroidView(
                modifier = Modifier.wrapContentSize(),
                factory = { context ->
                PieChart(context).apply {
                    layoutParams = LinearLayout.LayoutParams(chartWidth, chartHeight)

                    // ── Disable chrome ──────────────────────────────
                    description.isEnabled = false
                    legend.isEnabled = false
                    isHighlightPerTapEnabled = false
                    isDragDecelerationEnabled = false
                    setTouchEnabled(false)

                    // ── Donut hole ──────────────────────────────────
                    // Slightly smaller hole radius + a translucent outer
                    // ring creates a softer, more modern donut look.
                    isDrawHoleEnabled = true
                    holeRadius = 76f
                    transparentCircleRadius = 80f
                    setTransparentCircleAlpha(40)
                    setTransparentCircleColor(holeColor)
                    setHoleColor(holeColor)
                    setDrawSlicesUnderHole(false)

                    // ── Centre text — drawn by Compose, not the chart ──
                    setDrawCenterText(false)
                    centerText = ""

                    // ── Entry labels (on-slice text) off ────────────
                    // Removes the cramped text drawn directly on thin
                    // slices — outer value lines handle labelling.
                    setDrawEntryLabels(false)

                    // ── Percent mode ────────────────────────────────
                    setUsePercentValues(true)

                    // ── Start at 12 o'clock ─────────────────────────
                    rotationAngle = 270f
                    isRotationEnabled = false

                    // ── Extra offsets for outer label breathing room ─
                    if (!hideValues) {
                        setExtraOffsets(24f, 12f, 24f, 12f)
                    } else {
                        setExtraOffsets(8f, 8f, 8f, 8f)
                    }

                    // ── Animate only on first composition ───────────
                    if (!isAnimated) {
                        isAnimated = true
                        animateY(900, Easing.EaseInOutCubic)
                    }
                }
            },
            update = { chart ->
                // Re-apply theme colours on recomposition (e.g. dark mode toggle)
                chart.setHoleColor(holeColor)
                chart.setTransparentCircleColor(holeColor)
                updatePieChartWithData(chart, pieChartData, hideValues, surfaceVariantColor)
            },
        )
        }

        // ── Compose centre overlay — compact enough to sit inside the hole ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.matchParentSize(),
        ) {
            Box(modifier = Modifier.weight(1f))
            if (!centerTopText.isNullOrBlank()) {
                Text(
                    text = centerTopText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AmountText(
                text = totalAmountText,
                numberStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.5.sp,
                    letterSpacing = (-0.2).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                unitColor = MaterialTheme.colorScheme.onSurfaceVariant
                    .copy(alpha = 0.6f),
                unitSizeFactor = 0.42f,
            )
            Box(modifier = Modifier.weight(1f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Data binding
// ═══════════════════════════════════════════════════════════════════════

private fun updatePieChartWithData(
    chart: PieChart,
    data: List<PieChartUiData>,
    hideValues: Boolean,
    labelFallbackColor: Int,
) {
    val entries = data.map { PieEntry(it.value, "", it.name) }
    val colors = data.map { it.color }

    val dataSet = PieDataSet(entries, "").apply {
        isHighlightEnabled = false
        this.colors = colors

        // ── Slice gap ──────────────────────────────────────────
        // Slightly wider than default for cleaner separation.
        sliceSpace = if (data.size > 1) 3f else 0f
        selectionShift = 0f

        // ── Value lines + outer labels ─────────────────────────
        if (!hideValues) {
            setDrawValues(true)

            // Connecting lines from slice to label
            valueLinePart1Length = 0.3f
            valueLinePart1OffsetPercentage = 85f
            valueLinePart2Length = 0.4f
            valueLineWidth = 1.5f
            valueLineColor = labelFallbackColor
            isUsingSliceColorAsValueLineColor = true

            // Labels sit outside the donut
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

            valueTextSize = 11f
            setValueTextColors(colors)

            // Show category name instead of raw value
            valueFormatter = object : ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    return pieEntry?.data?.toString() ?: ""
                }
            }
        } else {
            setDrawValues(false)
        }
    }

    chart.data = PieData(dataSet)
    chart.invalidate()
}
