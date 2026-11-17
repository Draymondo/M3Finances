package com.naveenapps.expensemanager.core.designsystem.ui.components

import android.widget.LinearLayout
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

data class PieChartUiData(
    val name: String,
    val value: Float,
    val color: Int,
)

/**
 * Donut sans aucune étiquette périphérique.
 *
 * Les callouts (« Retr…don », « Internet », « hygiène » empilés) se
 * superposaient dès que deux parts étaient fines : le centre garde le total,
 * et c'est la liste située sous le graphique qui porte le détail
 * (montant + barre + pourcentage). Une seule source d'information, zéro
 * chevauchement.
 */
@Composable
fun PieChartView(
    totalAmountText: String,
    chartData: List<PieChartUiData>,
    modifier: Modifier = Modifier,
    chartHeight: Int = 600,
    hideValues: Boolean = true,
    chartWidth: Int = LinearLayout.LayoutParams.MATCH_PARENT,
    centerTextSize: Float = 12f,
) {
    var isAnimated by remember { mutableStateOf(false) }

    val onBackgroundColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val holeColor = MaterialTheme.colorScheme.background.toArgb()

    Crossfade(targetState = chartData, label = "pie_chart") { pieChartData ->
        AndroidView(
            modifier = modifier.wrapContentSize(),
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
                    isDrawHoleEnabled = true
                    holeRadius = 76f
                    transparentCircleRadius = 80f
                    setTransparentCircleAlpha(40)
                    setTransparentCircleColor(holeColor)
                    setHoleColor(holeColor)
                    setDrawSlicesUnderHole(false)

                    // ── Centre text ─────────────────────────────────
                    setDrawCenterText(true)
                    setCenterTextColor(onBackgroundColor)
                    setCenterTextSize(centerTextSize)
                    setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD)
                    centerText = totalAmountText

                    // ── No labels at all ────────────────────────────
                    // Ni sur les parts, ni autour : la liste sous le donut
                    // porte les noms, les montants et les pourcentages.
                    setDrawEntryLabels(false)
                    setDrawValues(false)

                    // ── Percent mode ────────────────────────────────
                    setUsePercentValues(true)

                    // ── Start at 12 o'clock ─────────────────────────
                    rotationAngle = 270f
                    isRotationEnabled = false

                    setExtraOffsets(8f, 8f, 8f, 8f)

                    // ── Animate only on first composition ───────────
                    if (!isAnimated) {
                        isAnimated = true
                        animateY(900, Easing.EaseInOutCubic)
                    }
                }
            },
            update = { chart ->
                // Re-apply theme colours on recomposition (e.g. dark mode toggle)
                chart.setCenterTextColor(onBackgroundColor)
                chart.setHoleColor(holeColor)
                chart.setTransparentCircleColor(holeColor)
                chart.centerText = totalAmountText
                updatePieChartWithData(chart, pieChartData)
            },
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Data binding
// ═══════════════════════════════════════════════════════════════════════

private fun updatePieChartWithData(
    chart: PieChart,
    data: List<PieChartUiData>,
) {
    val entries = data.map { PieEntry(it.value, "", it.name) }
    val colors = data.map { it.color }

    val dataSet = PieDataSet(entries, "").apply {
        isHighlightEnabled = false
        this.colors = colors
        sliceSpace = if (data.size > 1) 3f else 0f
        selectionShift = 0f
        setDrawValues(false)
    }

    chart.data = PieData(dataSet)
    chart.invalidate()
}
