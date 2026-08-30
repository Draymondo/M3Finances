package com.naveenapps.expensemanager.feature.analysis

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naveenapps.expensemanager.core.common.utils.getCompactNumber
import com.naveenapps.expensemanager.core.designsystem.components.DashboardWidgetTitle
import com.naveenapps.expensemanager.core.designsystem.components.EmptyItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.patrykandpatrick.vico.compose.axis.axisLabelComponent
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer

/**
 * A single-line variant of [ChartScreen] — net worth is one series, not the expense/income pair
 * that screen is built around, so it gets its own (much simpler) chart composable rather than
 * reusing that one with an unused second line.
 */
@Composable
fun NetWorthGraphView(
    netWorthChart: NetWorthUiChartData?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    AppCardView(modifier = modifier) {
        DashboardWidgetTitle(
            modifier = Modifier.padding(16.dp),
            title = stringResource(id = R.string.net_worth),
        )

        if (netWorthChart == null) {
            EmptyItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                emptyItemText = stringResource(id = R.string.no_chart_available),
                icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_analysis,
            )
            return@AppCardView
        }

        val lineColor = MaterialTheme.colorScheme.primary
        val marker = rememberMarker()

        ProvideChartStyle(rememberChartStyle(listOf(lineColor), isDarkTheme)) {
            Chart(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                chart = lineChart(
                    lines = listOf(
                        lineSpec(
                            lineColor = lineColor,
                            lineBackgroundShader = verticalGradient(
                                arrayOf(
                                    lineColor.copy(0.5f),
                                    lineColor.copy(alpha = 0f),
                                ),
                            ),
                        ),
                    ),
                ),
                startAxis = rememberStartAxis(
                    itemPlacer = AxisItemPlacer.Vertical.default(6),
                    label = axisLabelComponent(),
                    valueFormatter = { value, _ -> getCompactNumber(value) },
                ),
                bottomAxis = rememberBottomAxis(
                    itemPlacer = AxisItemPlacer.Horizontal.default(5),
                    label = axisLabelComponent(),
                    valueFormatter = { value, _ ->
                        if (netWorthChart.dates.isNotEmpty()) {
                            netWorthChart.dates.getOrElse(value.toInt()) { "" }
                        } else {
                            ""
                        }
                    },
                ),
                model = netWorthChart.chartData,
                marker = marker,
            )
        }
    }
}
