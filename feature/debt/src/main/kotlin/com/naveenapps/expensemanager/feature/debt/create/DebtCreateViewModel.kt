package com.naveenapps.expensemanager.feature.debt.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.AddDebtRepaymentUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.AddDebtReminderUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.AddDebtUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.DeleteDebtReminderUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.DeleteDebtUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.FindDebtByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.GetDebtRemindersUseCase
import com.naveenapps.expensemanager.core.domain.usecase.debt.UpdateDebtUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.Debt
import com.naveenapps.expensemanager.core.model.DebtDirection
import com.naveenapps.expensemanager.core.model.DebtReminder
import com.naveenapps.expensemanager.core.model.Resource
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import kotlin.math.abs

class DebtCreateViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val findDebtByIdUseCase: FindDebtByIdUseCase,
    private val addDebtUseCase: AddDebtUseCase,
    private val updateDebtUseCase: UpdateDebtUseCase,
    private val deleteDebtUseCase: DeleteDebtUseCase,
    private val addDebtRepaymentUseCase: AddDebtRepaymentUseCase,
    private val getDebtRemindersUseCase: GetDebtRemindersUseCase,
    private val addDebtReminderUseCase: AddDebtReminderUseCase,
    private val deleteDebtReminderUseCase: DeleteDebtReminderUseCase,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
) : ViewModel() {

    private val accountsReady = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        DebtCreateState(
            isEditing = false,
            personName = TextFieldValue(value = "", valueError = false, onValueChange = this::setPersonName),
            notes = TextFieldValue(value = "", valueError = false, onValueChange = this::setNotes),
            direction = DebtDirection.LENT,
            amount = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setAmountOnChange,
            ),
            linkedToAccount = true,
            dueDate = null,
            isSettled = false,
            currency = getDefaultCurrencyUseCase.invoke(),
            selectedAccount = defaultAccount,
            accounts = emptyList(),
            remainingAmount = null,
            showDeleteButton = false,
            showDeleteDialog = false,
            showAccountSelection = false,
            showDueDateSelection = false,
        )
    )
    val state = _state.asStateFlow()

    private var editingDebt: Debt? = null

    init {
        observeAccountsAndCurrency(getCurrencyUseCase, getAllAccountsUseCase)
        loadDebtWhenReady(savedStateHandle)
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

    private fun loadDebtWhenReady(savedStateHandle: SavedStateHandle) {
        val debtId = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID) ?: return
        viewModelScope.launch {
            // Accounts aren't actually needed to display an existing debt (its fields are all
            // fixed/read-only), but waiting keeps this consistent with the other create/edit
            // ViewModels in the app and avoids a state flicker if accounts arrive later.
            accountsReady.first { it }
            loadEditingDebt(debtId)
        }
    }

    private suspend fun loadEditingDebt(id: String) {
        when (val response = findDebtByIdUseCase.invoke(id)) {
            is Resource.Error -> Unit
            is Resource.Success -> {
                val debt = response.data
                editingDebt = debt
                observeReminders(id)
                _state.update {
                    it.copy(
                        isEditing = true,
                        personName = it.personName.copy(value = debt.personName),
                        notes = it.notes.copy(value = debt.notes),
                        direction = debt.direction,
                        dueDate = debt.dueDate,
                        isSettled = debt.isSettled,
                        remainingAmount = getFormattedAmountUseCase.invoke(
                            abs(debt.account.amount),
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

        if (currentState.personName.value.isBlank()) {
            _state.update { it.copy(personName = it.personName.copy(valueError = true)) }
            return
        }

        if (currentState.isEditing) {
            val debt = editingDebt ?: return
            viewModelScope.launch {
                val updated = debt.copy(
                    personName = currentState.personName.value,
                    notes = currentState.notes.value,
                    dueDate = currentState.dueDate,
                    isSettled = currentState.isSettled,
                    updatedOn = Date(),
                )
                if (updateDebtUseCase.invoke(updated) is Resource.Success) {
                    closePage()
                }
            }
            return
        }

        val amountValue = numberFormatRepository.parseToDouble(currentState.amount.value)
        if (amountValue == null || amountValue <= 0.0) {
            _state.update { it.copy(amount = it.amount.copy(valueError = true)) }
            return
        }

        viewModelScope.launch {
            val now = Date()
            val newDebt = Debt(
                id = UUID.randomUUID().toString(),
                accountId = UUID.randomUUID().toString(),
                personName = currentState.personName.value,
                direction = currentState.direction,
                dueDate = currentState.dueDate,
                notes = currentState.notes.value,
                isSettled = false,
                createdOn = now,
                updatedOn = now,
            )
            val response = addDebtUseCase.invoke(
                debt = newDebt,
                initialAmount = amountValue,
                realAccountId = if (currentState.linkedToAccount) {
                    currentState.selectedAccount.id
                } else {
                    null
                },
            )
            if (response is Resource.Success) {
                closePage()
            }
        }
    }

    private fun observeReminders(debtId: String) {
        getDebtRemindersUseCase.invoke(debtId)
            .onEach { reminders -> _state.update { it.copy(reminders = reminders) } }
            .launchIn(viewModelScope)
    }

    private fun addReminder(date: Date) {
        val debt = editingDebt ?: return
        viewModelScope.launch {
            addDebtReminderUseCase.invoke(
                DebtReminder(
                    id = UUID.randomUUID().toString(),
                    debtId = debt.id,
                    reminderDate = date,
                    createdOn = Date(),
                ),
            )
            _state.update { it.copy(showAddReminderDialog = false) }
        }
    }

    private fun deleteReminder(id: String) {
        viewModelScope.launch { deleteDebtReminderUseCase.invoke(id) }
    }

    private fun delete() {
        val debt = editingDebt ?: return
        viewModelScope.launch {
            if (deleteDebtUseCase.invoke(debt) is Resource.Success) {
                closePage()
            }
        }
    }

    private fun setPersonName(name: String) {
        _state.update {
            it.copy(personName = it.personName.copy(value = name, valueError = name.isBlank()))
        }
    }

    private fun setNotes(notes: String) {
        _state.update { it.copy(notes = it.notes.copy(value = notes)) }
    }

    private fun setAmountOnChange(amount: String) {
        val amountValue = numberFormatRepository.parseToDouble(amount)
        _state.update {
            it.copy(
                amount = it.amount.copy(
                    value = amount,
                    valueError = amount.isBlank() || amountValue == null || amountValue <= 0.0,
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

    private fun setRepaymentAmount(amount: String) {
        val amountValue = numberFormatRepository.parseToDouble(amount)
        _state.update {
            it.copy(
                repaymentAmount = it.repaymentAmount.copy(
                    value = amount,
                    valueError = amount.isBlank() || amountValue == null || amountValue <= 0.0,
                ),
            )
        }
    }

    private fun showRepaymentSheet() {
        _state.update {
            it.copy(
                showRepaymentSheet = true,
                repaymentAmount = TextFieldValue(
                    value = numberFormatRepository.formatForEditing(0.0),
                    valueError = false,
                    onValueChange = this::setRepaymentAmount,
                ),
                repaymentAccount = it.repaymentAccount ?: it.accounts.firstOrNull(),
            )
        }
    }

    private fun confirmRepayment() {
        val debt = editingDebt ?: return
        val currentState = _state.value
        val repaymentAccount = currentState.repaymentAccount ?: return

        val amountValue = numberFormatRepository.parseToDouble(currentState.repaymentAmount.value)
        if (amountValue == null || amountValue <= 0.0) {
            _state.update { it.copy(repaymentAmount = it.repaymentAmount.copy(valueError = true)) }
            return
        }

        viewModelScope.launch {
            val response = addDebtRepaymentUseCase.invoke(
                debt = debt,
                amount = amountValue,
                realAccountId = repaymentAccount.id,
                notes = "",
            )
            if (response is Resource.Success) {
                _state.update { it.copy(showRepaymentSheet = false) }
                // The debt account's balance changed — reload it so remainingAmount reflects
                // the repayment just recorded.
                loadEditingDebt(debt.id)
            }
        }
    }

    fun processAction(action: DebtCreateAction) {
        when (action) {
            DebtCreateAction.ClosePage -> closePage()
            DebtCreateAction.Save -> save()
            DebtCreateAction.Delete -> delete()

            DebtCreateAction.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = true) }
            DebtCreateAction.DismissDeleteDialog -> _state.update { it.copy(showDeleteDialog = false) }

            is DebtCreateAction.ChangeDirection -> _state.update { it.copy(direction = action.direction) }

            DebtCreateAction.ToggleLinkedToAccount -> _state.update {
                it.copy(linkedToAccount = !it.linkedToAccount)
            }

            DebtCreateAction.OpenAccountCreate -> openAccountCreate()

            DebtCreateAction.ShowAccountSelection -> _state.update { it.copy(showAccountSelection = true) }
            DebtCreateAction.DismissAccountSelection -> _state.update { it.copy(showAccountSelection = false) }

            is DebtCreateAction.SelectAccount -> _state.update {
                it.copy(selectedAccount = action.account, showAccountSelection = false)
            }

            DebtCreateAction.ShowDueDateSelection -> _state.update { it.copy(showDueDateSelection = true) }
            DebtCreateAction.DismissDueDateSelection -> _state.update { it.copy(showDueDateSelection = false) }

            is DebtCreateAction.SelectDueDate -> _state.update {
                it.copy(dueDate = action.date, showDueDateSelection = false)
            }

            DebtCreateAction.ClearDueDate -> _state.update { it.copy(dueDate = null) }

            DebtCreateAction.ToggleSettled -> _state.update { it.copy(isSettled = !it.isSettled) }

            DebtCreateAction.ShowRepaymentSheet -> showRepaymentSheet()
            DebtCreateAction.DismissRepaymentSheet -> _state.update { it.copy(showRepaymentSheet = false) }

            DebtCreateAction.ShowRepaymentAccountSelection -> _state.update {
                it.copy(showRepaymentAccountSelection = true)
            }
            DebtCreateAction.DismissRepaymentAccountSelection -> _state.update {
                it.copy(showRepaymentAccountSelection = false)
            }
            is DebtCreateAction.SelectRepaymentAccount -> _state.update {
                it.copy(repaymentAccount = action.account, showRepaymentAccountSelection = false)
            }

            DebtCreateAction.ConfirmRepayment -> confirmRepayment()

            DebtCreateAction.ShowAddReminderDialog -> _state.update { it.copy(showAddReminderDialog = true) }
            DebtCreateAction.DismissAddReminderDialog -> _state.update { it.copy(showAddReminderDialog = false) }
            is DebtCreateAction.AddReminder -> addReminder(action.date)
            is DebtCreateAction.DeleteReminder -> deleteReminder(action.id)
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
