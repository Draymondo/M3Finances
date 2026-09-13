package com.naveenapps.expensemanager.feature.worktime.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.tools.TrackToolUsageUseCase
import com.naveenapps.expensemanager.core.domain.usecase.worktime.CalculateWorkTimeEquivalentUseCase
import com.naveenapps.expensemanager.core.domain.usecase.worktime.GetWorkTimeSettingsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.worktime.SetWorkTimeAutoDetectEnabledUseCase
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.model.WorkTimeUnit
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkTimeCalculatorViewModel(
    getWorkTimeSettingsUseCase: GetWorkTimeSettingsUseCase,
    private val calculateWorkTimeEquivalentUseCase: CalculateWorkTimeEquivalentUseCase,
    private val setWorkTimeAutoDetectEnabledUseCase: SetWorkTimeAutoDetectEnabledUseCase,
    private val numberFormatRepository: NumberFormatRepository,
    private val appComposeNavigator: AppComposeNavigator,
    private val trackToolUsageUseCase: TrackToolUsageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        WorkTimeCalculatorState(
            price = TextFieldValue(value = "", valueError = false, onValueChange = this::setPrice),
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            trackToolUsageUseCase(ToolType.WORK_TIME)
        }
        // Lit les settings pour savoir si l'outil est configuré et observer autoDetectEnabled
        getWorkTimeSettingsUseCase().onEach { settings ->
            val configured = settings.hoursPerDay > 0 && settings.daysPerWeek > 0 &&
                (settings.manualIncome > 0 || settings.autoDetectCategoryIds.isNotEmpty())
            val previousAutoDetect = _state.value.autoDetectEnabled
            val autoDetectChanged = previousAutoDetect != settings.autoDetectEnabled
            _state.update {
                it.copy(
                    isSettingsConfigured = configured,
                    autoDetectEnabled = settings.autoDetectEnabled,
                )
            }
            if (autoDetectChanged && _state.value.result != null) {
                calculate()
            }
        }.launchIn(viewModelScope)
    }

    private fun setPrice(value: String) {
        _state.update {
            it.copy(
                price = it.price.copy(value = value, valueError = false),
                result = null,
                errorMessage = null,
            )
        }
    }

    private fun setUnit(unit: WorkTimeUnit) {
        _state.update { it.copy(selectedUnit = unit, result = null, errorMessage = null) }
    }

    private fun toggleAutoDetect(enabled: Boolean) {
        viewModelScope.launch {
            setWorkTimeAutoDetectEnabledUseCase(enabled)
        }
    }

    private fun calculate() {
        val priceStr = _state.value.price.value
        val price = numberFormatRepository.parseToDouble(priceStr)
            ?: priceStr.replace(',', '.').toDoubleOrNull()

        if (price == null || price <= 0.0) {
            _state.update {
                it.copy(price = it.price.copy(valueError = true), errorMessage = null)
            }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null, result = null) }
        viewModelScope.launch {
            when (val result = calculateWorkTimeEquivalentUseCase(price, _state.value.selectedUnit)) {
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message)
                }
                is Resource.Success -> _state.update {
                    it.copy(isLoading = false, result = result.data.value)
                }
            }
        }
    }

    private fun openSettings() {
        appComposeNavigator.navigate(ExpenseManagerScreens.WorkTimeCalculatorSettings)
    }

    fun processAction(action: WorkTimeCalculatorAction) {
        when (action) {
            is WorkTimeCalculatorAction.SetPrice -> setPrice(action.value)
            is WorkTimeCalculatorAction.SetUnit -> setUnit(action.unit)
            is WorkTimeCalculatorAction.ToggleAutoDetect -> toggleAutoDetect(action.enabled)
            WorkTimeCalculatorAction.Calculate -> calculate()
            WorkTimeCalculatorAction.OpenSettings -> openSettings()
        }
    }
}

