package com.naveenapps.expensemanager.feature.analysis

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.settings.filter.daterange.GetDateRangeUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.theme.GetCurrentThemeUseCase
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetAmountStateUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetAverageDataUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetChartDataUseCase
import com.naveenapps.expensemanager.core.domain.usecase.networth.GetNetWorthChartDataUseCase
import com.naveenapps.expensemanager.core.model.AverageData
import com.naveenapps.expensemanager.core.model.ExpenseFlowState
import com.naveenapps.expensemanager.core.model.Theme
import com.naveenapps.expensemanager.core.model.TransactionUiItem
import com.naveenapps.expensemanager.core.model.WholeAverageData
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class AnalysisScreenViewModel(
    getCurrentThemeUseCase: GetCurrentThemeUseCase,
    getChartDataUseCase: GetChartDataUseCase,
    getAverageDataUseCase: GetAverageDataUseCase,
    getAmountStateUseCase: GetAmountStateUseCase,
    getDateRangeUseCase: GetDateRangeUseCase,
    private val settingsRepository: SettingsRepository,
    getNetWorthChartDataUseCase: GetNetWorthChartDataUseCase,
    private val geminiRepository: com.naveenapps.expensemanager.core.repository.GeminiRepository
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(
        Theme(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            R.string.analysis,
        ),
    )
    val currentTheme = _currentTheme.asStateFlow()

    private val _expenseFlowState = MutableStateFlow(ExpenseFlowState())
    val amountUiState = _expenseFlowState.asStateFlow()

    private val _transactionPeriod = MutableStateFlow("")
    val transactionPeriod = _transactionPeriod.asStateFlow()

    private val _isCompactSummary = MutableStateFlow(false)
    val isCompactSummary = _isCompactSummary.asStateFlow()

    private val _graphItems = MutableStateFlow<AnalysisUiData?>(null)
    val graphItems = _graphItems.asStateFlow()

    private val _averageData = MutableStateFlow(
        WholeAverageData(
            AverageData(
                "0.00$",
                "0.00$",
                "0.00$",
            ),
            AverageData(
                "0.00$",
                "0.00$",
                "0.00$",
            ),
        ),
    )
    val averageData = _averageData.asStateFlow()

    private val _netWorthChartItems = MutableStateFlow<NetWorthUiChartData?>(null)
    val netWorthChartItems = _netWorthChartItems.asStateFlow()

    init {
        getChartDataUseCase.invoke().onEach { response ->
            _graphItems.value = AnalysisUiData(
                transactions = response.transactions,
                chartData = response.chartData?.let { chart ->
                    AnalysisUiChartData(
                        chartData = ChartEntryModelProducer(
                            chart.chartData.map {
                                it.map { entry ->
                                    entryOf(
                                        entry.index,
                                        entry.total,
                                    )
                                }
                            },
                        ).getModel(),
                        dates = chart.dates,
                    )
                },
            )
        }.launchIn(viewModelScope)

        getAverageDataUseCase.invoke().onEach { response ->
            _averageData.value = response
        }.launchIn(viewModelScope)

        getAmountStateUseCase.invoke().onEach { response ->
            _expenseFlowState.value = response
        }.launchIn(viewModelScope)

        getCurrentThemeUseCase.invoke().onEach {
            _currentTheme.value = it
        }.launchIn(viewModelScope)

        getDateRangeUseCase.invoke().onEach {
            _transactionPeriod.value = if (it.description.isNotEmpty()) {
                "${it.name} (${it.description})"
            } else {
                it.name
            }
        }.launchIn(viewModelScope)

        settingsRepository.getHomeSummaryCompact().onEach {
            _isCompactSummary.value = it
        }.launchIn(viewModelScope)

        getNetWorthChartDataUseCase.invoke().onEach { response ->
            _netWorthChartItems.value = response?.let { chart ->
                NetWorthUiChartData(
                    chartData = ChartEntryModelProducer(
                        listOf(
                            chart.chartData.map { entry -> entryOf(entry.index, entry.total) },
                        ),
                    ).getModel(),
                    dates = chart.dates,
                )
            }
        }.launchIn(viewModelScope)
    }

    private val _aiReportState = MutableStateFlow<AiReportState>(AiReportState.Idle)
    val aiReportState = _aiReportState.asStateFlow()

    fun generateAiReport() {
        viewModelScope.launch {
            _aiReportState.value = AiReportState.Loading
            val apiKey = settingsRepository.getGeminiApiKey().firstOrNull() ?: ""
            if (apiKey.isEmpty()) {
                _aiReportState.value = AiReportState.Error("API Key de Gemini manquante.")
                return@launch
            }

            val txs = _graphItems.value?.transactions ?: emptyList()
            if (txs.isEmpty()) {
                val periodLabel = _transactionPeriod.value.ifBlank { "cette période" }
                _aiReportState.value = AiReportState.Error("Aucune transaction sur $periodLabel.")
                return@launch
            }

            val text = buildString {
                appendLine("Période sélectionnée : ${_transactionPeriod.value.ifBlank { "non précisée" }}")
                appendLine("Entrées totales: ${_expenseFlowState.value.income}")
                appendLine("Dépenses totales: ${_expenseFlowState.value.expense}")

                val categoryTotals = txs
                    .groupBy { it.categoryName }
                    .mapValues { (_, items) -> items.sumOf { it.amount.amount } }
                    .entries
                    .sortedByDescending { it.value }

                appendLine("Répartition des dépenses/revenus par catégorie (liste EXHAUSTIVE — aucune autre catégorie n'existe sur cette période) :")
                categoryTotals.forEach { (category, total) ->
                    appendLine("- $category: $total")
                }
            }

            val res = geminiRepository.generateMonthlyReport(text, apiKey)
            if (res is com.naveenapps.expensemanager.core.model.Resource.Success) {
                _aiReportState.value = AiReportState.Success(res.data)
            } else if (res is com.naveenapps.expensemanager.core.model.Resource.Error) {
                _aiReportState.value = AiReportState.Error(res.exception.message ?: "Erreur")
            }
        }
    }

    fun dismissAiReport() {
        _aiReportState.value = AiReportState.Idle
    }
}

sealed class AiReportState {
    object Idle : AiReportState()
    object Loading : AiReportState()
    data class Success(val text: String) : AiReportState()
    data class Error(val error: String) : AiReportState()
}

data class AnalysisUiData(
    val transactions: List<TransactionUiItem>,
    val chartData: AnalysisUiChartData? = null,
)

data class AnalysisUiChartData(
    val chartData: ChartEntryModel,
    val dates: List<String>,
    val title: String? = null,
)

data class NetWorthUiChartData(
    val chartData: ChartEntryModel,
    val dates: List<String>,
)
