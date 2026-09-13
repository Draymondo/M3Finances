package com.naveenapps.expensemanager.feature.worktime.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.worktime.GetWorkTimeSettingsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.worktime.UpdateWorkTimeSettingsUseCase
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.WorkTimePeriod
import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkTimeSettingsViewModel(
    private val getWorkTimeSettingsUseCase: GetWorkTimeSettingsUseCase,
    getAllCategoryUseCase: GetAllCategoryUseCase,
    private val updateWorkTimeSettingsUseCase: UpdateWorkTimeSettingsUseCase,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        WorkTimeSettingsState(
            income = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setIncome,
            ),
            hoursPerDay = TextFieldValue(
                value = "8",
                valueError = false,
                onValueChange = this::setHoursPerDay,
            ),
            daysPerWeek = TextFieldValue(
                value = "5",
                valueError = false,
                onValueChange = this::setDaysPerWeek,
            ),
        )
    )
    val state = _state.asStateFlow()

    init {
        // Charge les réglages existants et les catégories INCOME disponibles
        combine(
            getWorkTimeSettingsUseCase(),
            getAllCategoryUseCase(),
        ) { settings, allCategories ->
            val incomeCategories = allCategories.filter { it.type == CategoryType.INCOME }
            val selected = incomeCategories.filter { it.id in settings.autoDetectCategoryIds }
            _state.update {
                it.copy(
                    income = it.income.copy(
                        value = numberFormatRepository.formatForEditing(settings.manualIncome)
                    ),
                    selectedPeriod = settings.manualIncomePeriod,
                    hoursPerDay = it.hoursPerDay.copy(
                        value = if (settings.hoursPerDay % 1.0 == 0.0) settings.hoursPerDay.toInt().toString() else settings.hoursPerDay.toString()
                    ),
                    daysPerWeek = it.daysPerWeek.copy(
                        value = settings.daysPerWeek.toString()
                    ),
                    availableCategories = incomeCategories,
                    selectedCategoryIds = settings.autoDetectCategoryIds,
                    selectedCategories = selected,
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun setIncome(value: String) {
        _state.update {
            it.copy(income = it.income.copy(value = value, valueError = false), errorMessage = null)
        }
    }

    private fun setHoursPerDay(value: String) {
        _state.update {
            it.copy(
                hoursPerDay = it.hoursPerDay.copy(value = value, valueError = false),
                errorMessage = null,
            )
        }
    }

    private fun setDaysPerWeek(value: String) {
        _state.update {
            it.copy(
                daysPerWeek = it.daysPerWeek.copy(value = value, valueError = false),
                errorMessage = null,
            )
        }
    }

    private fun save() {
        val currentState = _state.value

        val income = numberFormatRepository.parseToDouble(currentState.income.value) ?: 0.0
        val hoursPerDay = numberFormatRepository.parseToDouble(currentState.hoursPerDay.value)
            ?: currentState.hoursPerDay.value.replace(',', '.').toDoubleOrNull()
        val daysPerWeek = currentState.daysPerWeek.value.trim().toIntOrNull()

        if (hoursPerDay == null || hoursPerDay <= 0.0) {
            _state.update {
                it.copy(hoursPerDay = it.hoursPerDay.copy(valueError = true))
            }
            return
        }
        if (daysPerWeek == null || daysPerWeek <= 0) {
            _state.update {
                it.copy(daysPerWeek = it.daysPerWeek.copy(valueError = true))
            }
            return
        }

        viewModelScope.launch {
            val currentSettings = getWorkTimeSettingsUseCase().first()
            val settings = WorkTimeSettings(
                manualIncome = income,
                manualIncomePeriod = currentState.selectedPeriod,
                hoursPerDay = hoursPerDay,
                daysPerWeek = daysPerWeek,
                autoDetectEnabled = currentSettings.autoDetectEnabled,
                autoDetectCategoryIds = currentState.selectedCategoryIds,
            )
            when (val result = updateWorkTimeSettingsUseCase(settings)) {
                is Resource.Error -> _state.update {
                    it.copy(errorMessage = result.exception.message)
                }
                is Resource.Success -> {
                    _state.update { it.copy(isSaved = true, errorMessage = null) }
                    appComposeNavigator.popBackStack()
                }
            }
        }
    }

    fun processAction(action: WorkTimeSettingsAction) {
        when (action) {
            is WorkTimeSettingsAction.SetIncome -> setIncome(action.value)
            is WorkTimeSettingsAction.SetPeriod -> _state.update {
                it.copy(selectedPeriod = action.period, errorMessage = null)
            }
            is WorkTimeSettingsAction.SetHoursPerDay -> setHoursPerDay(action.value)
            is WorkTimeSettingsAction.SetDaysPerWeek -> setDaysPerWeek(action.value)
            WorkTimeSettingsAction.ShowCategorySelection -> _state.update {
                it.copy(showCategorySelection = true)
            }
            WorkTimeSettingsAction.DismissCategorySelection -> _state.update {
                it.copy(showCategorySelection = false)
            }
            is WorkTimeSettingsAction.SetCategories -> _state.update {
                it.copy(
                    selectedCategories = action.categories,
                    selectedCategoryIds = action.categories.map { category -> category.id },
                    showCategorySelection = false,
                )
            }
            WorkTimeSettingsAction.Save -> save()
            WorkTimeSettingsAction.ClosePage -> appComposeNavigator.popBackStack()
        }
    }
}

