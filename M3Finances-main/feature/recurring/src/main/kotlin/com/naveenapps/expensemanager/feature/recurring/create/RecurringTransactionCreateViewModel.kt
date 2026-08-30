package com.naveenapps.expensemanager.feature.recurring.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.AddRecurringTransactionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.DeleteRecurringTransactionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.FindRecurringTransactionByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.UpdateRecurringTransactionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.RecurrenceFrequency
import com.naveenapps.expensemanager.core.model.RecurringTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.getAvailableCreditLimit
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.isIncome
import com.naveenapps.expensemanager.core.model.toAccountUiModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerArgsNames
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.SettingsRepository
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


class RecurringTransactionCreateViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getAllCategoryUseCase: GetAllCategoryUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val findRecurringTransactionByIdUseCase: FindRecurringTransactionByIdUseCase,
    private val addRecurringTransactionUseCase: AddRecurringTransactionUseCase,
    private val updateRecurringTransactionUseCase: UpdateRecurringTransactionUseCase,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val settingsRepository: SettingsRepository,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
) : ViewModel() {

    private val transactionType = MutableStateFlow(TransactionType.EXPENSE)

    // Tracks whether both the accounts and categories flows have emitted their first value,
    // so we only populate an editing recurring transaction once both are ready — same pattern
    // as TransactionCreateViewModel.
    private val syncState = MutableStateFlow(
        RecurringTransactionCreateInitSetupState(
            isCategorySyncCompleted = false,
            isAccountSyncCompleted = false,
        )
    )

    private val _state = MutableStateFlow(
        RecurringTransactionCreateState(
            amount = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setAmountOnChange,
            ),
            notes = TextFieldValue(
                value = "",
                valueError = false,
                onValueChange = this::setNotes,
            ),
            transactionType = TransactionType.EXPENSE,
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 1,
            startDate = Date(),
            endDate = null,
            isActive = true,
            currency = getDefaultCurrencyUseCase.invoke(),
            selectedCategory = defaultCategory,
            selectedFromAccount = defaultAccount,
            selectedToAccount = null,
            accounts = emptyList(),
            categories = emptyList(),
            showDeleteButton = false,
            showDeleteDialog = false,
            showCategorySelection = false,
            showAccountSelection = false,
            accountSelection = RecurringAccountSelection.FROM_ACCOUNT,
            showDateSelection = false,
            dateSelection = RecurringDateSelection.START_DATE,
        )
    )
    val state = _state.asStateFlow()

    // Non-null only when editing an existing recurring transaction.
    private var editingRecurringTransaction: RecurringTransaction? = null

    init {
        observeAccountsAndCurrency(getCurrencyUseCase, getAllAccountsUseCase)
        observeCategories(getAllCategoryUseCase)
        loadRecurringTransactionWhenReady(savedStateHandle)
    }

    // region Observers

    private fun observeAccountsAndCurrency(
        getCurrencyUseCase: GetCurrencyUseCase,
        getAllAccountsUseCase: GetAllAccountsUseCase,
    ) {
        combine(
            getCurrencyUseCase.invoke(),
            getAllAccountsUseCase.invoke(),
        ) { currency, accounts ->
            // GetAllAccountsUseCase already excludes hidden AccountType.DEBT accounts.
            val mappedAccounts = mapAccountsToUiModels(accounts, currency)
            val defaultAccountId = settingsRepository.getDefaultAccount().firstOrNull()
            val selectedAccount = mappedAccounts.find { it.id == defaultAccountId }
                ?: mappedAccounts.firstOrNull()
                ?: defaultAccount
            _state.update {
                it.copy(
                    currency = currency,
                    accounts = mappedAccounts,
                    selectedFromAccount = selectedAccount,
                )
            }
            syncState.update { it.copy(isAccountSyncCompleted = true) }
        }.launchIn(viewModelScope)
    }

    private fun observeCategories(getAllCategoryUseCase: GetAllCategoryUseCase) {
        combine(
            transactionType,
            getAllCategoryUseCase.invoke(),
            settingsRepository.getDefaultIncomeCategory(),
            settingsRepository.getDefaultExpenseCategory(),
        ) { type, categories, defaultIncomeCategory, defaultExpenseCategory ->
            val filteredCategories = categories.filter { category ->
                if (type.isIncome()) category.type.isIncome() else category.type.isExpense()
            }
            val preferredCategoryId = when (type) {
                TransactionType.INCOME -> defaultIncomeCategory
                TransactionType.EXPENSE -> defaultExpenseCategory
                TransactionType.TRANSFER -> null
            }
            val matchedCategory = filteredCategories.find { it.id == preferredCategoryId }

            _state.update {
                it.copy(
                    transactionType = type,
                    categories = filteredCategories,
                    selectedCategory = matchedCategory ?: filteredCategories.firstOrNull()
                        ?: defaultCategory,
                )
            }
            syncState.update { it.copy(isCategorySyncCompleted = true) }
        }.launchIn(viewModelScope)
    }

    private fun loadRecurringTransactionWhenReady(savedStateHandle: SavedStateHandle) {
        val recurringTransactionId = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID)
            ?: return
        viewModelScope.launch {
            syncState.first { it.isAccountSyncCompleted && it.isCategorySyncCompleted }
            loadEditingRecurringTransaction(recurringTransactionId)
        }
    }

    // endregion

    // region Load (edit mode)

    private suspend fun loadEditingRecurringTransaction(id: String) {
        when (val response = findRecurringTransactionByIdUseCase.invoke(id)) {
            is Resource.Error -> Unit
            is Resource.Success -> {
                val recurringTransaction = response.data
                editingRecurringTransaction = recurringTransaction
                transactionType.update { recurringTransaction.type }

                val fromAccount = _state.value.accounts.find {
                    it.id == recurringTransaction.fromAccountId
                } ?: defaultAccount
                val toAccount = recurringTransaction.toAccountId?.let { toAccountId ->
                    _state.value.accounts.find { it.id == toAccountId }
                }

                _state.update {
                    it.copy(
                        amount = it.amount.copy(
                            value = numberFormatRepository.formatForEditing(
                                recurringTransaction.amount.amount,
                            ),
                        ),
                        notes = it.notes.copy(value = recurringTransaction.notes),
                        transactionType = recurringTransaction.type,
                        frequency = recurringTransaction.frequency,
                        interval = recurringTransaction.interval,
                        startDate = recurringTransaction.startDate,
                        endDate = recurringTransaction.endDate,
                        isActive = recurringTransaction.isActive,
                        selectedCategory = it.categories.find { category ->
                            category.id == recurringTransaction.categoryId
                        } ?: it.selectedCategory,
                        selectedFromAccount = fromAccount,
                        selectedToAccount = toAccount,
                        showDeleteButton = true,
                    )
                }
            }
        }
    }

    // endregion

    // region Save / Delete

    private fun save() {
        val currentState = _state.value
        val amountValue = numberFormatRepository.parseToDouble(currentState.amount.value)

        if (amountValue == null || amountValue <= 0.0) {
            _state.update { it.copy(amount = it.amount.copy(valueError = true)) }
            return
        }

        if (currentState.transactionType == TransactionType.TRANSFER &&
            currentState.selectedToAccount == null
        ) {
            return
        }

        viewModelScope.launch {
            val now = Date()
            val recurringTransaction = RecurringTransaction(
                id = editingRecurringTransaction?.id ?: UUID.randomUUID().toString(),
                notes = currentState.notes.value,
                categoryId = currentState.selectedCategory.id,
                fromAccountId = currentState.selectedFromAccount.id,
                toAccountId = if (currentState.transactionType == TransactionType.TRANSFER) {
                    currentState.selectedToAccount?.id
                } else {
                    null
                },
                amount = Amount(amountValue),
                type = currentState.transactionType,
                frequency = currentState.frequency,
                interval = currentState.interval,
                startDate = currentState.startDate,
                endDate = currentState.endDate,
                nextOccurrenceDate = editingRecurringTransaction?.nextOccurrenceDate
                    ?: currentState.startDate,
                isActive = currentState.isActive,
                createdOn = editingRecurringTransaction?.createdOn ?: now,
                updatedOn = now,
            )

            val response = if (editingRecurringTransaction != null) {
                updateRecurringTransactionUseCase.invoke(recurringTransaction)
            } else {
                addRecurringTransactionUseCase.invoke(recurringTransaction)
            }

            if (response is Resource.Success) {
                closePage()
            }
        }
    }

    private fun deleteRecurringTransaction() {
        val recurringTransaction = editingRecurringTransaction ?: return
        viewModelScope.launch {
            if (deleteRecurringTransactionUseCase.invoke(recurringTransaction) is Resource.Success) {
                closePage()
            }
        }
    }

    // endregion

    // region State helpers

    private fun mapAccountsToUiModels(
        accounts: List<Account>,
        currency: Currency,
    ): List<AccountUiModel> {
        return accounts.map { account ->
            account.toAccountUiModel(
                getFormattedAmountUseCase.invoke(account.amount, currency),
                if (account.type == AccountType.CREDIT) {
                    getFormattedAmountUseCase.invoke(account.getAvailableCreditLimit(), currency)
                } else {
                    null
                },
            )
        }
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

    private fun setNotes(notes: String) {
        _state.update { it.copy(notes = it.notes.copy(value = notes)) }
    }

    private fun changeTransactionType(type: TransactionType) {
        transactionType.update { type }
        _state.update {
            it.copy(
                transactionType = type,
                selectedToAccount = if (type == TransactionType.TRANSFER) {
                    it.selectedToAccount ?: it.accounts.firstOrNull { account ->
                        account.id != it.selectedFromAccount.id
                    }
                } else {
                    null
                },
            )
        }
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    private fun openCategoryCreate() {
        appComposeNavigator.navigate(ExpenseManagerScreens.CategoryCreate(null))
    }

    private fun openAccountCreate() {
        appComposeNavigator.navigate(ExpenseManagerScreens.AccountCreate(null))
    }

    // endregion

    fun processAction(action: RecurringTransactionCreateAction) {
        when (action) {
            RecurringTransactionCreateAction.ClosePage -> closePage()
            RecurringTransactionCreateAction.Save -> save()
            RecurringTransactionCreateAction.Delete -> deleteRecurringTransaction()

            RecurringTransactionCreateAction.ShowDeleteDialog -> _state.update {
                it.copy(showDeleteDialog = true)
            }

            RecurringTransactionCreateAction.DismissDeleteDialog -> _state.update {
                it.copy(showDeleteDialog = false)
            }

            is RecurringTransactionCreateAction.ChangeTransactionType ->
                changeTransactionType(action.type)

            is RecurringTransactionCreateAction.ChangeFrequency -> _state.update {
                it.copy(frequency = action.frequency)
            }

            is RecurringTransactionCreateAction.ChangeInterval -> _state.update {
                it.copy(interval = action.interval.coerceAtLeast(1))
            }

            RecurringTransactionCreateAction.OpenCategoryCreate -> openCategoryCreate()
            RecurringTransactionCreateAction.OpenAccountCreate -> openAccountCreate()

            RecurringTransactionCreateAction.ShowCategorySelection -> _state.update {
                it.copy(showCategorySelection = true)
            }

            RecurringTransactionCreateAction.DismissCategorySelection -> _state.update {
                it.copy(showCategorySelection = false)
            }

            is RecurringTransactionCreateAction.SelectCategory -> _state.update {
                it.copy(selectedCategory = action.category, showCategorySelection = false)
            }

            is RecurringTransactionCreateAction.ShowAccountSelection -> _state.update {
                it.copy(showAccountSelection = true, accountSelection = action.type)
            }

            RecurringTransactionCreateAction.DismissAccountSelection -> _state.update {
                it.copy(showAccountSelection = false)
            }

            is RecurringTransactionCreateAction.SelectAccount -> _state.update {
                when (it.accountSelection) {
                    RecurringAccountSelection.FROM_ACCOUNT -> it.copy(
                        selectedFromAccount = action.account,
                        showAccountSelection = false,
                    )

                    RecurringAccountSelection.TO_ACCOUNT -> it.copy(
                        selectedToAccount = action.account,
                        showAccountSelection = false,
                    )
                }
            }

            is RecurringTransactionCreateAction.ShowDateSelection -> _state.update {
                it.copy(showDateSelection = true, dateSelection = action.type)
            }

            RecurringTransactionCreateAction.DismissDateSelection -> _state.update {
                it.copy(showDateSelection = false)
            }

            is RecurringTransactionCreateAction.SelectDate -> _state.update {
                when (it.dateSelection) {
                    RecurringDateSelection.START_DATE -> it.copy(
                        startDate = action.date,
                        showDateSelection = false,
                    )

                    RecurringDateSelection.END_DATE -> it.copy(
                        endDate = action.date,
                        showDateSelection = false,
                    )
                }
            }

            RecurringTransactionCreateAction.ClearEndDate -> _state.update {
                it.copy(endDate = null)
            }

            RecurringTransactionCreateAction.ToggleActive -> _state.update {
                it.copy(isActive = !it.isActive)
            }
        }
    }

    companion object {
        private val defaultCategory = Category(
            id = "1",
            name = "Shopping",
            type = CategoryType.EXPENSE,
            storedIcon = StoredIcon(
                name = "ic_calendar",
                backgroundColor = "#000000",
            ),
            createdOn = Date(),
            updatedOn = Date(),
        )

        private val defaultAccount = AccountUiModel(
            id = "1",
            name = "Shopping",
            storedIcon = StoredIcon(
                name = "ic_calendar",
                backgroundColor = "#000000",
            ),
            amount = Amount(0.0, "$ 0.00"),
            amountTextColor = com.naveenapps.expensemanager.core.common.R.color.green_500,
        )
    }
}

private data class RecurringTransactionCreateInitSetupState(
    val isCategorySyncCompleted: Boolean,
    val isAccountSyncCompleted: Boolean,
)
