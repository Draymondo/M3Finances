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

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AnalysisScreen(
    viewModel: AnalysisScreenViewModel = koinViewModel(),
) {
    val netWorthChart by viewModel.netWorthChartItems.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isDarkTheme = shouldUseDarkTheme(theme = currentTheme.mode)
    val aiReportState by viewModel.aiReportState.collectAsState()

    AnalysisScreenScaffoldView(
        netWorthChart = netWorthChart,
        isDarkTheme = isDarkTheme,
        aiReportState = aiReportState,
        onGenerateAiReport = viewModel::generateAiReport,
        onDismissAiReport = viewModel::dismissAiReport
    )
}

@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
private fun AnalysisScreenScaffoldView(
    netWorthChart: NetWorthUiChartData?,
    isDarkTheme: Boolean,
    aiReportState: AiReportState = AiReportState.Idle,
    onGenerateAiReport: () -> Unit = {},
    onDismissAiReport: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                title = stringResource(R.string.analysis),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onGenerateAiReport) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Bilan IA")
            }
        }
    ) { innerPadding ->
        AnalysisScreenContent(
            netWorthChart = netWorthChart,
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
        
        if (aiReportState !is AiReportState.Idle) {
            ModalBottomSheet(
                onDismissRequest = onDismissAiReport,
                sheetState = rememberModalBottomSheetState()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (aiReportState) {
                        is AiReportState.Loading -> {
                            CircularProgressIndicator()
                        }
                        is AiReportState.Success -> {
                            Text(text = aiReportState.text, style = MaterialTheme.typography.bodyLarge)
                        }
                        is AiReportState.Error -> {
                            Text(text = aiReportState.error, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {}
                    }
                }
            }
        }
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

@androidx.compose.material3.ExperimentalMaterial3Api
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
