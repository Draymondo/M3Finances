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

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import com.naveenapps.expensemanager.core.model.Resource
import androidx.compose.ui.Alignment

@Composable
fun AnalysisScreen(
    viewModel: AnalysisScreenViewModel = koinViewModel(),
) {
    val netWorthChart by viewModel.netWorthChartItems.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val aiInsight by viewModel.aiInsight.collectAsState()
    val isGeneratingInsight by viewModel.isGeneratingInsight.collectAsState()
    val isDarkTheme = shouldUseDarkTheme(theme = currentTheme.mode)

    AnalysisScreenScaffoldView(
        netWorthChart = netWorthChart,
        isDarkTheme = isDarkTheme,
        aiInsight = aiInsight,
        isGeneratingInsight = isGeneratingInsight,
        onGenerateInsight = viewModel::generateInsight
    )
}

@Composable
private fun AnalysisScreenScaffoldView(
    netWorthChart: NetWorthUiChartData?,
    isDarkTheme: Boolean,
    aiInsight: Resource<String>?,
    isGeneratingInsight: Boolean,
    onGenerateInsight: () -> Unit
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
            aiInsight = aiInsight,
            isGeneratingInsight = isGeneratingInsight,
            onGenerateInsight = onGenerateInsight,
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
    aiInsight: Resource<String>?,
    isGeneratingInsight: Boolean,
    onGenerateInsight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FilterView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 6.dp),
        )
        
        FinancialInsightsView(
            aiInsight = aiInsight,
            isGeneratingInsight = isGeneratingInsight,
            onGenerateInsight = onGenerateInsight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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

@Composable
fun FinancialInsightsView(
    aiInsight: Resource<String>?,
    isGeneratingInsight: Boolean,
    onGenerateInsight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✨ Conseil de l'IA",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                if (!isGeneratingInsight) {
                    TextButton(onClick = onGenerateInsight) {
                        Text(if (aiInsight == null || aiInsight is Resource.Error) "Générer" else "Actualiser")
                    }
                }
            }
            
            if (isGeneratingInsight) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Analyse de vos données en cours...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                when (aiInsight) {
                    is Resource.Success -> {
                        Text(
                            text = aiInsight.data,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    is Resource.Error -> {
                        Text(
                            text = "Erreur lors de la génération. Veuillez réessayer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    null -> {
                        Text(
                            text = "Cliquez pour obtenir une analyse personnalisée de vos dépenses.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AnalysisScreenPreview() {
    NaveenAppsPreviewTheme(padding = 0.dp) {
        AnalysisScreenScaffoldView(
            netWorthChart = null,
            isDarkTheme = false,
            aiInsight = null,
            isGeneratingInsight = false,
            onGenerateInsight = {}
        )
    }
}
