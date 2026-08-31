package com.naveenapps.expensemanager.feature.analysis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.utils.shouldUseDarkTheme
import com.naveenapps.expensemanager.feature.filter.FilterView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalysisScreen(
    viewModel: AnalysisScreenViewModel = koinViewModel(),
) {
    val netWorthChart by viewModel.netWorthChartItems.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isDarkTheme = shouldUseDarkTheme(theme = currentTheme.mode)

    AnalysisScreenScaffoldView(
        netWorthChart = netWorthChart,
        isDarkTheme = isDarkTheme,
    )
}

@Composable
private fun AnalysisScreenScaffoldView(
    netWorthChart: NetWorthUiChartData?,
    isDarkTheme: Boolean,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                title = stringResource(R.string.analysis),
            )
        },
    ) { innerPadding ->
        AnalysisScreenContent(
            netWorthChart = netWorthChart,
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun AnalysisScreenContent(
    netWorthChart: NetWorthUiChartData?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FilterView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 6.dp),
        )
        AnalysisGraphScreen()
        NetWorthGraphView(
            netWorthChart = netWorthChart,
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Preview
@Composable
fun AnalysisScreenPreview() {
    NaveenAppsPreviewTheme(padding = 0.dp) {
        AnalysisScreenScaffoldView(
            netWorthChart = null,
            isDarkTheme = false,
        )
    }
}
