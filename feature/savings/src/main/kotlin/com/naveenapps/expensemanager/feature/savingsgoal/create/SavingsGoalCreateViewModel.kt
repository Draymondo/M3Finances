package com.naveenapps.expensemanager.feature.savingsgoal.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.AddSavingsGoalContributionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.AddSavingsGoalUseCase
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.DeleteSavingsGoalUseCase
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.FindSavingsGoalByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.UpdateSavingsGoalUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.SavingsGoal
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.getAvailableCreditLimit
import com.naveenapps.expensemanager.core.model.toAccountUiModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerArgsNames
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class SavingsGoalCreateViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val findSavingsGoalByIdUseCase: FindSavingsGoalByIdUseCase,
    private val addSavingsGoalUseCase: AddSavingsGoalUseCase,
    private val updateSavingsGoalUseCase: UpdateSavingsGoalUseCase,
    private val deleteSavingsGoalUseCase: DeleteSavingsGoalUseCase,
    private val addSavingsGoalContributionUseCase: AddSavingsGoalContributionUseCase,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
) : ViewModel() {

    private val accountsReady = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        SavingsGoalCreateState(
            isEditing = false,
            name = TextFieldValue(value = "", valueError = false, onValueChange = this::setName),
            notes = TextFieldValue(value = "", valueError = false, onValueChange = this::setNotes),
            targetAmount = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setTargetAmountOnChange,
            ),
            initialAmount = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setInitialAmountOnChange,
            ),
            targetDate = null,
            isAchieved = false,
            savingsStrategy = com.naveenapps.expensemanager.core.model.SavingsStrategy.FIXED,
            targetPercentage = TextFieldValue(
                value = "",
                valueError = false,
                onValueChange = this::setTargetPercentageOnChange,
            ),
            currency = getDefaultCurrencyUseCase.invoke(),
            selectedAccount = defaultAccount,
            accounts = emptyList(),
            savedAmount = null,
            showDeleteButton = false,
            showDeleteDialog = false,
            showAccountSelection = false,
            showTargetDateSelection = false,
        )
    )
    val state = _state.asStateFlow()

    private var editingSavingsGoal: SavingsGoal? = null

    init {
        observeAccountsAndCurrency(getCurrencyUseCase, getAllAccountsUseCase)
        loadSavingsGoalWhenReady(savedStateHandle)
    }

    private fun observeAccountsAndCurrency(
        getCurrencyUseCase: GetCurrencyUseCase,
        getAllAccountsUseCase: GetAllAccountsUseCase,
    ) {
        combine(
            getCurrencyUseCase.invoke(),
            getAllAccountsUseCase.invoke(),
        ) { currency, accounts ->
            val mappedAccounts = accounts.map { account ->
                account.toAccountUiModel(
                    getFormattedAmountUseCase.invoke(account.amount, currency),
                    if (account.type == AccountType.CREDIT) {
                        getFormattedAmountUseCase.invoke(account.getAvailableCreditLimit(), currency)
                    } else {
                        null
                    },
                )
            }
            _state.update {
                it.copy(
                    currency = currency,
                    accounts = mappedAccounts,
                    selectedAccount = if (it.isEditing) {
                        it.selectedAccount
                    } else {
                        mappedAccounts.firstOrNull() ?: defaultAccount
                    },
                )
            }
            accountsReady.update { true }
        }.launchIn(viewModelScope)
    }

    private fun loadSavingsGoalWhenReady(savedStateHandle: SavedStateHandle) {
        val id = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID) ?: return
        viewModelScope.launch {
            accountsReady.first { it }
            loadEditingSavingsGoal(id)
        }
    }

    private suspend fun loadEditingSavingsGoal(id: String) {
        when (val response = findSavingsGoalByIdUseCase.invoke(id)) {
            is Resource.Error -> Unit
            is Resource.Success -> {
                val savingsGoal = response.data
                editingSavingsGoal = savingsGoal
                _state.update {
                    it.copy(
                        isEditing = true,
                        name = it.name.copy(value = savingsGoal.name),
                        notes = it.notes.copy(value = savingsGoal.notes),
                        targetAmount = it.targetAmount.copy(
                            value = numberFormatRepository.formatForEditing(savingsGoal.targetAmount),
                        ),
                        targetDate = savingsGoal.targetDate,
                        isAchieved = savingsGoal.isAchieved,
                        savingsStrategy = savingsGoal.savingsStrategy,
                        targetPercentage = it.targetPercentage.copy(
                            value = savingsGoal.targetPercentage?.let { pct -> numberFormatRepository.formatForEditing(pct) } ?: ""
                        ),
                        savedAmount = getFormattedAmountUseCase.invoke(
                            savingsGoal.account.amount,
                            it.currency,
                        ),
                        showDeleteButton = true,
                    )
                }
            }
        }
    }

    private fun save() {
        val currentState = _state.value

        if (currentState.name.value.isBlank()) {
            _state.update { it.copy(name = it.name.copy(valueError = true)) }
            return
        }

        val targetAmountValue = numberFormatRepository.parseToDouble(currentState.targetAmount.value)
        if (targetAmountValue == null || targetAmountValue <= 0.0) {
            _state.update { it.copy(targetAmount = it.targetAmount.copy(valueError = true)) }
            return
        }

        if (currentState.isEditing) {
            val savingsGoal = editingSavingsGoal ?: return
            viewModelScope.launch {
                val updated = savingsGoal.copy(
                    name = currentState.name.value,
                    notes = currentState.notes.value,
                    targetAmount = targetAmountValue,
                    targetDate = currentState.targetDate,
                    isAchieved = currentState.isAchieved,
                    savingsStrategy = currentState.savingsStrategy,
                    targetPercentage = numberFormatRepository.parseToDouble(currentState.targetPercentage.value),
                    updatedOn = Date(),
                )
                if (updateSavingsGoalUseCase.invoke(updated) is Resource.Success) {
                    closePage()
                }
            }
            return
        }

        val initialAmountValue = numberFormatRepository.parseToDouble(currentState.initialAmount.value) ?: 0.0
        if (initialAmountValue < 0.0) {
            _state.update { it.copy(initialAmount = it.initialAmount.copy(valueError = true)) }
            return
        }

        viewModelScope.launch {
            val now = Date()
            val newSavingsGoal = SavingsGoal(
                id = UUID.randomUUID().toString(),
                accountId = UUID.randomUUID().toString(),
                name = currentState.name.value,
                targetAmount = targetAmountValue,
                targetDate = currentState.targetDate,
                notes = currentState.notes.value,
                isAchieved = false,
                savingsStrategy = currentState.savingsStrategy,
                targetPercentage = numberFormatRepository.parseToDouble(currentState.targetPercentage.value),
                createdOn = now,
                updatedOn = now,
            )
            val response = addSavingsGoalUseCase.invoke(
                savingsGoal = newSavingsGoal,
                initialAmount = initialAmountValue,
                realAccountId = if (initialAmountValue > 0.0) currentState.selectedAccount.id else null,
            )
            if (response is Resource.Success) {
                closePage()
            }
        }
    }

    private fun delete() {
        val savingsGoal = editingSavingsGoal ?: return
        viewModelScope.launch {
            if (deleteSavingsGoalUseCase.invoke(savingsGoal) is Resource.Success) {
                closePage()
            }
        }
    }

    private fun setName(name: String) {
        _state.update {
            it.copy(name = it.name.copy(value = name, valueError = name.isBlank()))
        }
    }

    private fun setNotes(notes: String) {
        _state.update { it.copy(notes = it.notes.copy(value = notes)) }
    }

    private fun setTargetPercentageOnChange(percentage: String) {
        val pctValue = numberFormatRepository.parseToDouble(percentage)
        _state.update {
            it.copy(
                targetPercentage = it.targetPercentage.copy(
                    value = percentage,
                    valueError = percentage.isBlank() || pctValue == null || pctValue <= 0.0 || pctValue > 100.0,
                ),
            )
        }
    }

    private fun setTargetAmountOnChange(amount: String) {
        val amountValue = numberFormatRepository.parseToDouble(amount)
        _state.update {
            it.copy(
                targetAmount = it.targetAmount.copy(
                    value = amount,
                    valueError = amount.isBlank() || amountValue == null || amountValue <= 0.0,
                ),
            )
        }
    }

    private fun setInitialAmountOnChange(amount: String) {
        val amountValue = numberFormatRepository.parseToDouble(amount)
        _state.update {
            it.copy(
                initialAmount = it.initialAmount.copy(
                    value = amount,
                    valueError = amount.isNotBlank() && (amountValue == null || amountValue < 0.0),
                ),
            )
        }
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    private fun openAccountCreate() {
        appComposeNavigator.navigate(ExpenseManagerScreens.AccountCreate(null))
    }

    private fun setContributionAmount(amount: String) {
        val amountValue = numberFormatRepository.parseToDouble(amount)
        _state.update {
            it.copy(
                contributionAmount = it.contributionAmount.copy(
                    value = amount,
                    valueError = amount.isBlank() || amountValue == null || amountValue <= 0.0,
                ),
            )
        }
    }

    private fun showContributionSheet() {
        _state.update {
            it.copy(
                showContributionSheet = true,
                isWithdrawal = false,
                contributionAmount = TextFieldValue(
                    value = numberFormatRepository.formatForEditing(0.0),
                    valueError = false,
                    onValueChange = this::setContributionAmount,
                ),
                contributionAccount = it.contributionAccount ?: it.accounts.firstOrNull(),
            )
        }
    }

    private fun confirmContribution() {
        val savingsGoal = editingSavingsGoal ?: return
        val currentState = _state.value
        val contributionAccount = currentState.contributionAccount ?: return

        val amountValue = numberFormatRepository.parseToDouble(currentState.contributionAmount.value)
        if (amountValue == null || amountValue <= 0.0) {
            _state.update { it.copy(contributionAmount = it.contributionAmount.copy(valueError = true)) }
            return
        }

        viewModelScope.launch {
            val response = addSavingsGoalContributionUseCase.invoke(
                savingsGoal = savingsGoal,
                amount = amountValue,
                realAccountId = contributionAccount.id,
                isWithdrawal = currentState.isWithdrawal,
                notes = "",
            )
            if (response is Resource.Success) {
                _state.update { it.copy(showContributionSheet = false) }
                // The goal account's balance changed — reload it so savedAmount reflects the
                // contribution/withdrawal just recorded.
                loadEditingSavingsGoal(savingsGoal.id)
            }
        }
    }

    fun processAction(action: SavingsGoalCreateAction) {
        when (action) {
            SavingsGoalCreateAction.ClosePage -> closePage()
            SavingsGoalCreateAction.Save -> save()
            SavingsGoalCreateAction.Delete -> delete()

            SavingsGoalCreateAction.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = true) }
            SavingsGoalCreateAction.DismissDeleteDialog -> _state.update { it.copy(showDeleteDialog = false) }

            SavingsGoalCreateAction.OpenAccountCreate -> openAccountCreate()

            SavingsGoalCreateAction.ShowAccountSelection -> _state.update { it.copy(showAccountSelection = true) }
            SavingsGoalCreateAction.DismissAccountSelection -> _state.update { it.copy(showAccountSelection = false) }

            is SavingsGoalCreateAction.SelectAccount -> _state.update {
                it.copy(selectedAccount = action.account, showAccountSelection = false)
            }

            SavingsGoalCreateAction.ShowTargetDateSelection -> _state.update {
                it.copy(showTargetDateSelection = true)
            }
            SavingsGoalCreateAction.DismissTargetDateSelection -> _state.update {
                it.copy(showTargetDateSelection = false)
            }

            is SavingsGoalCreateAction.SelectTargetDate -> _state.update {
                it.copy(targetDate = action.date, showTargetDateSelection = false)
            }

            SavingsGoalCreateAction.ClearTargetDate -> _state.update { it.copy(targetDate = null) }

            SavingsGoalCreateAction.ToggleAchieved -> _state.update { it.copy(isAchieved = !it.isAchieved) }
            SavingsGoalCreateAction.ToggleSavingsStrategy -> _state.update { 
                val newStrategy = if (it.savingsStrategy == com.naveenapps.expensemanager.core.model.SavingsStrategy.FIXED) {
                    com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME
                } else {
                    com.naveenapps.expensemanager.core.model.SavingsStrategy.FIXED
                }
                it.copy(savingsStrategy = newStrategy) 
            }

            SavingsGoalCreateAction.ShowContributionSheet -> showContributionSheet()
            SavingsGoalCreateAction.DismissContributionSheet -> _state.update {
                it.copy(showContributionSheet = false)
            }
            SavingsGoalCreateAction.ToggleContributionDirection -> _state.update {
                it.copy(isWithdrawal = !it.isWithdrawal)
            }

            SavingsGoalCreateAction.ShowContributionAccountSelection -> _state.update {
                it.copy(showContributionAccountSelection = true)
            }
            SavingsGoalCreateAction.DismissContributionAccountSelection -> _state.update {
                it.copy(showContributionAccountSelection = false)
            }
            is SavingsGoalCreateAction.SelectContributionAccount -> _state.update {
                it.copy(contributionAccount = action.account, showContributionAccountSelection = false)
            }

            SavingsGoalCreateAction.ConfirmContribution -> confirmContribution()
        }
    }

    companion object {
        private val defaultAccount = AccountUiModel(
            id = "1",
            name = "",
            storedIcon = StoredIcon(name = "", backgroundColor = "#000000"),
            amount = Amount(0.0, "$ 0.00"),
            amountTextColor = com.naveenapps.expensemanager.core.common.R.color.green_500,
        )
    }
}
