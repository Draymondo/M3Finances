package com.naveenapps.expensemanager.feature.envelope.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.envelope.AddEnvelopeUseCase
import com.naveenapps.expensemanager.core.domain.usecase.envelope.DeleteEnvelopeUseCase
import com.naveenapps.expensemanager.core.domain.usecase.envelope.FindEnvelopeByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.envelope.UpdateEnvelopeUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.Envelope
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerArgsNames
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class EnvelopeCreateViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getAllCategoryUseCase: GetAllCategoryUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    private val findEnvelopeByIdUseCase: FindEnvelopeByIdUseCase,
    private val addEnvelopeUseCase: AddEnvelopeUseCase,
    private val updateEnvelopeUseCase: UpdateEnvelopeUseCase,
    private val deleteEnvelopeUseCase: DeleteEnvelopeUseCase,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
) : ViewModel() {

    private val categoriesReady = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        EnvelopeCreateState(
            isEditing = false,
            name = TextFieldValue(value = "", valueError = false, onValueChange = this::setName),
            amount = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setAmount,
            ),
            categories = emptyList(),
            selectedCategory = null,
            categoryError = false,
            currency = getDefaultCurrencyUseCase.invoke(),
            showDeleteButton = false,
            showDeleteDialog = false,
            showCategorySelection = false,
        ),
    )
    val state = _state.asStateFlow()

    /** Preserved as-is from the loaded envelope on edit, since the create screen has no period
     * picker yet (see [EnvelopeCreateViewModel] class docs) — new envelopes default to the
     * current month. */
    private var selectedMonth: String = Date().toMonthAndYearKey()
    private var periodType: BudgetPeriod = BudgetPeriod.MONTHLY
    private var editingEnvelope: Envelope? = null

    init {
        observeCategoriesAndCurrency(getCurrencyUseCase, getAllCategoryUseCase)
        loadEnvelopeWhenReady(savedStateHandle)
    }

    private fun observeCategoriesAndCurrency(
        getCurrencyUseCase: GetCurrencyUseCase,
        getAllCategoryUseCase: GetAllCategoryUseCase,
    ) {
        combine(
            getCurrencyUseCase.invoke(),
            getAllCategoryUseCase.invoke(),
        ) { currency, categories ->
            val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
            _state.update {
                it.copy(
                    currency = currency,
                    categories = expenseCategories,
                    selectedCategory = it.selectedCategory
                        ?: editingEnvelope?.categoryId?.let { categoryId ->
                            expenseCategories.find { category -> category.id == categoryId }
                        },
                )
            }
            categoriesReady.update { true }
        }.launchIn(viewModelScope)
    }

    private fun loadEnvelopeWhenReady(savedStateHandle: SavedStateHandle) {
        val id = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID) ?: return
        viewModelScope.launch {
            categoriesReady.first { it }
            loadEditingEnvelope(id)
        }
    }

    private suspend fun loadEditingEnvelope(id: String) {
        when (val response = findEnvelopeByIdUseCase.invoke(id)) {
            is Resource.Error -> Unit
            is Resource.Success -> {
                val envelope = response.data
                editingEnvelope = envelope
                selectedMonth = envelope.selectedMonth
                periodType = envelope.periodType
                _state.update {
                    it.copy(
                        isEditing = true,
                        name = it.name.copy(value = envelope.name.orEmpty()),
                        amount = it.amount.copy(
                            value = numberFormatRepository.formatForEditing(envelope.amount),
                        ),
                        selectedCategory = it.categories.find { category ->
                            category.id == envelope.categoryId
                        },
                        showDeleteButton = true,
                    )
                }
            }
        }
    }

    private fun save() {
        val currentState = _state.value

        val category = currentState.selectedCategory
        if (category == null) {
            _state.update { it.copy(categoryError = true) }
            return
        }

        val amountValue = numberFormatRepository.parseToDouble(currentState.amount.value)
        if (amountValue == null || amountValue <= 0.0) {
            _state.update { it.copy(amount = it.amount.copy(valueError = true)) }
            return
        }

        viewModelScope.launch {
            val now = Date()
            val response = if (currentState.isEditing) {
                val existing = editingEnvelope ?: return@launch
                updateEnvelopeUseCase.invoke(
                    existing.copy(
                        categoryId = category.id,
                        name = currentState.name.value.ifBlank { null },
                        amount = amountValue,
                        updatedOn = now,
                    ),
                )
            } else {
                addEnvelopeUseCase.invoke(
                    Envelope(
                        id = UUID.randomUUID().toString(),
                        categoryId = category.id,
                        name = currentState.name.value.ifBlank { null },
                        amount = amountValue,
                        selectedMonth = selectedMonth,
                        periodType = periodType,
                        createdOn = now,
                        updatedOn = now,
                    ),
                )
            }
            when (response) {
                is Resource.Success -> closePage()
                is Resource.Error -> _state.update {
                    it.copy(errorMessage = response.exception.message)
                }
            }
        }
    }

    private fun delete() {
        val envelope = editingEnvelope ?: return
        viewModelScope.launch {
            if (deleteEnvelopeUseCase.invoke(envelope) is Resource.Success) {
                closePage()
            }
        }
    }

    private fun setName(name: String) {
        _state.update { it.copy(name = it.name.copy(value = name), errorMessage = null) }
    }

    private fun setAmount(amount: String) {
        _state.update {
            it.copy(
                amount = it.amount.copy(value = amount, valueError = false),
                errorMessage = null,
            )
        }
    }

    private fun selectCategory(categoryId: String) {
        val category = _state.value.categories.find { it.id == categoryId } ?: return
        _state.update {
            it.copy(
                selectedCategory = category,
                categoryError = false,
                showCategorySelection = false,
                errorMessage = null,
            )
        }
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: EnvelopeCreateAction) {
        when (action) {
            EnvelopeCreateAction.ClosePage -> closePage()
            EnvelopeCreateAction.Save -> save()
            EnvelopeCreateAction.Delete -> delete()
            EnvelopeCreateAction.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = true) }
            EnvelopeCreateAction.DismissDeleteDialog -> _state.update { it.copy(showDeleteDialog = false) }
            EnvelopeCreateAction.ShowCategorySelection -> _state.update { it.copy(showCategorySelection = true) }
            EnvelopeCreateAction.DismissCategorySelection -> _state.update { it.copy(showCategorySelection = false) }
            is EnvelopeCreateAction.SelectCategory -> selectCategory(action.categoryId)
        }
    }
}
