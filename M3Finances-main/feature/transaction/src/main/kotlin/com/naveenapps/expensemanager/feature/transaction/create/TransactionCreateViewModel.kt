package com.naveenapps.expensemanager.feature.transaction.create

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.matchCategory
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.DeleteTransactionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.FindTransactionByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.UpdateTransactionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.PredictCategoryForNotesUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ScanReceiptUseCase
import com.naveenapps.expensemanager.core.repository.FeedbackRepository
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.TransactionSplitItem
import com.naveenapps.expensemanager.core.model.getAvailableCreditLimit
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.isIncome
import com.naveenapps.expensemanager.core.model.isTransfer
import com.naveenapps.expensemanager.core.model.toAccountUiModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerArgsNames
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.math.BigDecimal


class TransactionCreateViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getAllCategoryUseCase: GetAllCategoryUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val findTransactionByIdUseCase: FindTransactionByIdUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val settingsRepository: SettingsRepository,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
    private val feedbackRepository: FeedbackRepository,
    private val predictCategoryForNotesUseCase: PredictCategoryForNotesUseCase,
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val pendingTransactionRepository: com.naveenapps.expensemanager.core.repository.PendingTransactionRepository,
    private val suggestCategoryUseCase: com.naveenapps.expensemanager.core.domain.usecase.transaction.SuggestCategoryUseCase,
) : ViewModel() {

    private val _event = Channel<TransactionCreateEvent>()
    val event = _event.receiveAsFlow()

    private val transactionType = MutableStateFlow(TransactionType.EXPENSE)

    // Tracks whether both the accounts and categories flows have emitted their first value,
    // so we only populate an editing transaction once both are ready.
    private val syncState = MutableStateFlow(
        TransactionCreateInitSetupState(
            isCategorySyncCompleted = false,
            isAccountSyncCompleted = false,
        )
    )

    private val _state = MutableStateFlow(
        TransactionCreateState(
            amount = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setAmountOnChange
            ),
            notes = TextFieldValue(
                value = "",
                valueError = false,
                onValueChange = this::setNotes
            ),
            dateTime = Date(),
            transactionType = TransactionType.EXPENSE,
            currency = getDefaultCurrencyUseCase.invoke(),
            selectedCategory = defaultCategory,
            selectedFromAccount = defaultAccount,
            selectedToAccount = defaultAccount,
            accounts = emptyList(),
            categories = emptyList(),
            showDeleteButton = false,
            showDeleteDialog = false,
            showCategorySelection = false,
            showAccountSelection = false,
            showNumberPad = false,
            showTimeSelection = false,
            showDateSelection = false,
            accountSelection = AccountSelection.FROM_ACCOUNT
            ,isSplit = false
            ,splitItems = emptyList()
            ,splitCategorySelectionIndex = null
            ,splitTotalError = false
            ,splitRemaining = Amount(0.0)
        )
    )
    var state = _state.asStateFlow()

    // Non-null only when editing an existing transaction.
    private var editingTransaction: Transaction? = null
    private var isCategoryManuallySelected = false
    private var loadedPendingTransaction: com.naveenapps.expensemanager.core.model.PendingTransaction? = null

    init {
        observeAccountsAndCurrency(getCurrencyUseCase, getAllAccountsUseCase)
        observeCategories(getAllCategoryUseCase)
        loadTransactionWhenReady(savedStateHandle)
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
                    selectedToAccount = selectedAccount,
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
            settingsRepository.getDefaultExpenseCategory()
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

    private fun loadTransactionWhenReady(savedStateHandle: SavedStateHandle) {
        val transactionId = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID)
        val pendingTransactionId = savedStateHandle.get<String>(ExpenseManagerArgsNames.PENDING_TRANSACTION_ID)
        
        if (transactionId == null && pendingTransactionId == null) return
        
        viewModelScope.launch {
            syncState.first { it.isAccountSyncCompleted && it.isCategorySyncCompleted }
            if (transactionId != null) {
                loadEditingTransaction(transactionId)
            } else if (pendingTransactionId != null) {
                loadPendingTransaction(pendingTransactionId)
            }
        }
    }

    private suspend fun loadPendingTransaction(pendingId: String) {
        val pendingTransaction = pendingTransactionRepository.getPendingTransactionById(pendingId).firstOrNull() ?: return
        
        loadedPendingTransaction = pendingTransaction
        transactionType.value = pendingTransaction.transactionType
        
        _state.update { current ->
            // Try to match suggested category
            val suggestedCategory = current.categories.matchCategory(pendingTransaction.suggestedCategory)
                ?: defaultCategory

            // Select Mobile Money account if possible
            val mmAccount = current.accounts.find {
                it.name.contains("Wave", ignoreCase = true) ||
                it.name.contains("Orange", ignoreCase = true) ||
                it.name.contains("MTN", ignoreCase = true) ||
                it.name.contains("MoMo", ignoreCase = true) ||
                it.name.contains("Mobile Money", ignoreCase = true)
            }
            val targetAccount = mmAccount ?: current.selectedFromAccount

            current.copy(
                amount = current.amount.copy(
                    value = numberFormatRepository.formatForEditing(pendingTransaction.amount)
                ),
                transactionType = pendingTransaction.transactionType,
                dateTime = pendingTransaction.date,
                notes = current.notes.copy(value = pendingTransaction.merchant ?: ""),
                selectedCategory = suggestedCategory,
                selectedFromAccount = targetAccount,
                selectedToAccount = targetAccount
            )
        }
    }

    // endregion

    // region Transaction load (edit mode)

    private suspend fun loadEditingTransaction(transactionId: String) {
        when (val response = findTransactionByIdUseCase.invoke(transactionId)) {
            is Resource.Error -> Unit
            is Resource.Success -> {
                val transaction = response.data
                editingTransaction = transaction
                isCategoryManuallySelected = true
                transactionType.value = transaction.type
                _state.update { current ->
                    current.copy(
                        amount = current.amount.copy(
                            value = numberFormatRepository.formatForEditing(transaction.amount.amount)
                        ),
                        transactionType = transaction.type,
                        dateTime = transaction.createdOn,
                        imagePath = transaction.imagePath,
                        notes = current.notes.copy(value = transaction.notes),
                        selectedCategory = transaction.category,
                        selectedFromAccount = transaction.fromAccount.toAccountUiModel(
                            getFormattedAmountUseCase.invoke(
                                transaction.fromAccount.amount,
                                current.currency
                            ),
                        ),
                        selectedToAccount = transaction.toAccount?.let { toAccount ->
                            toAccount.toAccountUiModel(
                                getFormattedAmountUseCase.invoke(
                                    toAccount.amount,
                                    current.currency
                                ),
                            )
                        } ?: defaultAccount,
                        showDeleteButton = true,
                        isSplit = transaction.splitItems.size >= 2,
                        splitItems = transaction.splitItems.map { item ->
                            createSplitItemState(item.category ?: current.categories.firstOrNull() ?: defaultCategory, item.amount.amount, item.notes.orEmpty())
                        },
                        splitRemaining = getFormattedAmountUseCase.invoke(
                            transaction.amount.amount - transaction.splitItems.sumOf { it.amount.amount },
                            current.currency,
                        ),
                    )
                }
            }
        }
    }

    // endregion

    // region Save / Delete

    private fun save() {
        val currentState = _state.value
        val amountText = currentState.amount.value
        val amountValue = numberFormatRepository.parseToDouble(amountText)

        Log.d("TransactionCreate", "save() called: amountText='$amountText', amountValue=$amountValue, type=${currentState.transactionType}, fromAcc=${currentState.selectedFromAccount.id}, toAcc=${currentState.selectedToAccount.id}")

        if (amountText.isBlank() || amountValue == null || amountValue <= 0.0) {
            Log.d("TransactionCreate", "save() BLOCKED: amount validation failed")
            _state.update { it.copy(amount = it.amount.copy(valueError = true)) }
            return
        }

        if (currentState.transactionType.isTransfer() &&
            currentState.selectedFromAccount.id == currentState.selectedToAccount.id
        ) {
            Log.d("TransactionCreate", "save() BLOCKED: transfer with same from/to account")
            _state.update { it.copy(saveError = "Les comptes source et destination doivent être différents") }
            return
        }

        if (currentState.isSplit) {
            val splitAmounts = currentState.splitItems.map {
                numberFormatRepository.parseToDouble(it.amount.value)
            }
            val splitTotal = splitAmounts.filterNotNull()
                .map(BigDecimal::valueOf)
                .fold(BigDecimal.ZERO, BigDecimal::add)
            if (currentState.splitItems.size < 2 ||
                splitAmounts.any { it == null || it <= 0.0 } ||
                splitTotal.compareTo(BigDecimal.valueOf(amountValue)) != 0
            ) {
                _state.update { it.copy(splitTotalError = true) }
                return
            }
        }

        // amountValue is smart-cast to Double after the null check above
        persistTransaction(buildTransactionFromState(currentState, amountValue))
    }

    internal fun buildTransactionFromState(
        state: TransactionCreateState,
        amountValue: Double
    ): Transaction {
        val transactionId = editingTransaction?.id ?: UUID.randomUUID().toString()
        return Transaction(
            id = transactionId,
            notes = state.notes.value,
            categoryId = state.selectedCategory.id,
            fromAccountId = state.selectedFromAccount.id,
            toAccountId = if (state.transactionType.isTransfer()) state.selectedToAccount.id else null,
            type = state.transactionType,
            amount = Amount(amountValue),
            imagePath = state.imagePath,
            createdOn = state.dateTime,
            updatedOn = Calendar.getInstance().time,
            splitItems = if (state.isSplit) {
                state.splitItems.mapNotNull { item ->
                    numberFormatRepository.parseToDouble(item.amount.value)?.let { amount ->
                        TransactionSplitItem(
                            id = UUID.randomUUID().toString(),
                            transactionId = transactionId,
                            categoryId = item.category.id,
                            amount = Amount(amount),
                            notes = item.notes.value.ifBlank { null },
                            category = item.category,
                        )
                    }
                }
            } else emptyList(),
        )
    }

    private fun persistTransaction(transaction: Transaction) {
        val isNewTransaction = editingTransaction == null
        viewModelScope.launch {
            val response = if (editingTransaction != null) {
                updateTransactionUseCase.invoke(transaction)
            } else {
                addTransactionUseCase.invoke(transaction)
            }
            if (response is com.naveenapps.expensemanager.core.model.Resource.Success) {
                loadedPendingTransaction?.let { pending ->
                    val fee = pending.fee ?: 0.0
                    if (fee > 0.0) {
                        val feeCategory = _state.value.categories.find {
                            it.name.contains("Utilities", ignoreCase = true) ||
                            it.name.contains("Bills", ignoreCase = true) ||
                            it.name.contains("Frais", ignoreCase = true) ||
                            it.name.contains("Bank", ignoreCase = true)
                        } ?: _state.value.categories.firstOrNull { it.type == com.naveenapps.expensemanager.core.model.CategoryType.EXPENSE } ?: defaultCategory

                        val feeTransaction = com.naveenapps.expensemanager.core.model.Transaction(
                            id = java.util.UUID.randomUUID().toString(),
                            notes = "Frais : ${pending.merchant}",
                            categoryId = feeCategory.id,
                            fromAccountId = transaction.fromAccountId,
                            toAccountId = null,
                            type = com.naveenapps.expensemanager.core.model.TransactionType.EXPENSE,
                            amount = com.naveenapps.expensemanager.core.model.Amount(fee),
                            imagePath = "",
                            createdOn = transaction.createdOn,
                            updatedOn = java.util.Calendar.getInstance().time,
                        )
                        addTransactionUseCase.invoke(feeTransaction)
                    }
                    pendingTransactionRepository.deletePendingTransaction(pending.id)
                }
                if (isNewTransaction) {
                    onNewTransactionCreated()
                }
                closePage()
            } else if (response is com.naveenapps.expensemanager.core.model.Resource.Error) {
                val errorMsg = response.exception.message ?: "Une erreur inconnue est survenue"
                _state.update { it.copy(saveError = errorMsg) }
            }
        }
    }

    // Only ever called after a brand-new transaction (not an edit) is saved successfully.
    // Bumps the running "transactions created" count, and — once someone has logged enough
    // transactions and enough days have passed since install — requests Google Play's in-app
    // review popup exactly once for the life of the install.
    private suspend fun onNewTransactionCreated() {
        feedbackRepository.setTransactionCreated(true)
        if (feedbackRepository.shouldShowFeedbackDialog().first()) {
            // Marked immediately, before we know whether Play actually showed anything —
            // Play never reports that back, so "requested" is the only signal we get.
            feedbackRepository.setFeedbackDialogShown(true)
            _event.send(TransactionCreateEvent.RequestReview)
        }
    }

    private fun deleteTransaction() {
        val transaction = editingTransaction ?: return
        viewModelScope.launch {
            if (deleteTransactionUseCase.invoke(transaction) is Resource.Success) {
                closePage()
            }
        }
    }

    // endregion

    // region State helpers

    private fun mapAccountsToUiModels(
        accounts: List<Account>,
        currency: Currency
    ): List<AccountUiModel> {
        return accounts.map { account ->
            account.toAccountUiModel(
                getFormattedAmountUseCase.invoke(account.amount, currency),
                if (account.type == AccountType.CREDIT) {
                    getFormattedAmountUseCase.invoke(account.getAvailableCreditLimit(), currency)
                } else {
                    null
                }
            )
        }
    }

    private fun setAmountOnChange(amount: String) {
        val amountValue = numberFormatRepository.parseToDouble(amount)
        _state.update {
            it.copy(
                amount = it.amount.copy(
                    value = amount,
                    valueError = amount.isBlank() || amountValue == null || amountValue <= 0.0
                ),
                showNumberPad = false,
                splitRemaining = calculateSplitRemaining(it, amountValue),
            )
        }
    }

    private var aiCategoryJob: kotlinx.coroutines.Job? = null

    private fun setNotes(notes: String) {
        _state.update { it.copy(notes = it.notes.copy(value = notes)) }
        if (!isCategoryManuallySelected) {
            aiCategoryJob?.cancel()
            aiCategoryJob = viewModelScope.launch {
                // Try local first
                val predictedCategoryId = predictCategoryForNotesUseCase.invoke(notes, transactionType.value)
                if (predictedCategoryId != null) {
                    val matchedCategory = _state.value.categories.find { it.id == predictedCategoryId }
                    if (matchedCategory != null && _state.value.selectedCategory.id != matchedCategory.id) {
                        _state.update { it.copy(selectedCategory = matchedCategory, isCategoryAutoSelected = true) }
                        return@launch
                    }
                }
                
                // If local fails, and note is long enough, try AI
                if (notes.length >= 3) {
                    kotlinx.coroutines.delay(1000) // Debounce 1 second
                    val categoryNames = _state.value.categories.map { it.name }
                    when (val result = suggestCategoryUseCase.invoke(notes, categoryNames)) {
                        is com.naveenapps.expensemanager.core.model.Resource.Success -> {
                            val suggestedName = result.data
                            val matchedAiCategory = _state.value.categories.find { 
                                it.name.equals(suggestedName, ignoreCase = true) 
                            }
                            if (matchedAiCategory != null && !isCategoryManuallySelected && _state.value.selectedCategory.id != matchedAiCategory.id) {
                                _state.update { it.copy(selectedCategory = matchedAiCategory, isCategoryAutoSelected = true) }
                            }
                        }
                        is com.naveenapps.expensemanager.core.model.Resource.Error -> {
                            // Silently ignore AI prediction errors for notes
                        }
                    }
                }
            }
        }
    }

    private fun createSplitItemState(category: Category, amount: Double = 0.0, notes: String = "") =
        TransactionSplitItemState(
            category = category,
            amount = TextFieldValue(
                value = if (amount == 0.0) "" else numberFormatRepository.formatForEditing(amount),
                valueError = false,
                onValueChange = null,
            ),
            notes = TextFieldValue(value = notes, valueError = false, onValueChange = null),
        )

    private fun toggleSplit() {
        _state.update { state ->
            if (state.isSplit) state.copy(isSplit = false, splitItems = emptyList(), splitTotalError = false)
            else {
                val secondCategory = state.categories.firstOrNull { it.id != state.selectedCategory.id }
                    ?: state.selectedCategory
                state.copy(
                    isSplit = true,
                    splitItems = listOf(
                        createSplitItemState(state.selectedCategory),
                        createSplitItemState(secondCategory),
                    ),
                    splitTotalError = false,
                    splitRemaining = calculateSplitRemaining(state, numberFormatRepository.parseToDouble(state.amount.value)),
                )
            }
        }
    }

    private fun updateSplitAmount(index: Int, amount: String) {
        _state.update { state ->
            state.copy(
                splitItems = state.splitItems.mapIndexed { itemIndex, item ->
                    if (itemIndex == index) item.copy(amount = item.amount.copy(value = amount)) else item
                },
                splitTotalError = false,
                splitRemaining = calculateSplitRemaining(
                    state,
                    numberFormatRepository.parseToDouble(state.amount.value),
                    amountOverride = index to amount,
                ),
            )
        }
    }

    private fun calculateSplitRemaining(
        state: TransactionCreateState,
        amount: Double?,
        amountOverride: Pair<Int, String>? = null,
    ): Amount {
        val splitTotal = state.splitItems.mapIndexed { index, item ->
            val value = if (amountOverride?.first == index) amountOverride.second else item.amount.value
            numberFormatRepository.parseToDouble(value) ?: 0.0
        }.sum()
        val remaining = (amount ?: 0.0) - splitTotal
        return getFormattedAmountUseCase.invoke(remaining, state.currency)
    }

    private fun changeTransactionType(type: TransactionType) {
        transactionType.update { type }
        _state.update { it.copy(transactionType = type) }
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

    private fun dismissDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    private fun showDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    // endregion

    fun processAction(action: TransactionCreateAction) {
        when (action) {
            TransactionCreateAction.ClosePage -> closePage()
            TransactionCreateAction.ShowDeleteDialog -> showDeleteDialog()
            TransactionCreateAction.DismissDeleteDialog -> dismissDeleteDialog()
            TransactionCreateAction.Delete -> deleteTransaction()
            TransactionCreateAction.Save -> save()

            is TransactionCreateAction.OpenAccountCreate -> openAccountCreate()
            is TransactionCreateAction.OpenCategoryCreate -> openCategoryCreate()

            is TransactionCreateAction.ChangeTransactionType -> changeTransactionType(action.type)

            is TransactionCreateAction.SetNumberPadValue -> action.amount?.let {
                setAmountOnChange(
                    it
                )
            }

            TransactionCreateAction.ShowCategorySelection -> _state.update {
                it.copy(
                    showCategorySelection = true
                )
            }

            TransactionCreateAction.ToggleSplit -> toggleSplit()
            TransactionCreateAction.AddSplitItem -> _state.update {
                val updatedItems = it.splitItems + createSplitItemState(it.categories.firstOrNull() ?: defaultCategory)
                it.copy(
                    splitItems = updatedItems,
                    splitRemaining = calculateSplitRemaining(it, numberFormatRepository.parseToDouble(it.amount.value)),
                )
            }
            is TransactionCreateAction.RemoveSplitItem -> _state.update {
                val updatedItems = it.splitItems.filterIndexed { index, _ -> index != action.index }
                it.copy(
                    splitItems = updatedItems,
                    splitRemaining = calculateSplitRemaining(it, numberFormatRepository.parseToDouble(it.amount.value)),
                )
            }
            is TransactionCreateAction.SetSplitAmount -> updateSplitAmount(action.index, action.amount)
            is TransactionCreateAction.SetSplitNotes -> _state.update {
                it.copy(splitItems = it.splitItems.mapIndexed { index, item ->
                    if (index == action.index) item.copy(notes = item.notes.copy(value = action.notes)) else item
                })
            }
            is TransactionCreateAction.ShowSplitCategorySelection -> _state.update {
                it.copy(showCategorySelection = true, splitCategorySelectionIndex = action.index)
            }
            is TransactionCreateAction.SelectSplitCategory -> _state.update {
                it.copy(
                    showCategorySelection = false,
                    splitCategorySelectionIndex = null,
                    splitItems = it.splitItems.mapIndexed { index, item ->
                        if (index == action.index) item.copy(category = action.category) else item
                    },
                )
            }

            TransactionCreateAction.DismissCategorySelection -> _state.update {
                it.copy(
                    showCategorySelection = false
                )
            }

            is TransactionCreateAction.SelectCategory -> {
                isCategoryManuallySelected = true
                _state.update {
                    it.copy(selectedCategory = action.category, showCategorySelection = false, splitCategorySelectionIndex = null, isCategoryAutoSelected = false)
                }
            }

            is TransactionCreateAction.ShowAccountSelection -> _state.update {
                it.copy(showAccountSelection = true, accountSelection = action.type)
            }

            TransactionCreateAction.DismissAccountSelection -> _state.update {
                it.copy(
                    showAccountSelection = false
                )
            }

            is TransactionCreateAction.SelectAccount -> _state.update {
                when (it.accountSelection) {
                    AccountSelection.FROM_ACCOUNT -> it.copy(
                        selectedFromAccount = action.account,
                        showAccountSelection = false
                    )

                    AccountSelection.TO_ACCOUNT -> it.copy(
                        selectedToAccount = action.account,
                        showAccountSelection = false
                    )
                }
            }

            TransactionCreateAction.ShowNumberPad -> _state.update { it.copy(showNumberPad = true) }
            TransactionCreateAction.DismissNumberPad -> _state.update { it.copy(showNumberPad = false) }

            TransactionCreateAction.ShowDateSelection -> _state.update { it.copy(showDateSelection = true) }
            TransactionCreateAction.ShowTimeSelection -> _state.update { it.copy(showTimeSelection = true) }
            TransactionCreateAction.DismissDateSelection -> _state.update {
                it.copy(showDateSelection = false, showTimeSelection = false)
            }

            is TransactionCreateAction.SelectDate -> _state.update {
                it.copy(
                    dateTime = action.date,
                    showDateSelection = false,
                    showTimeSelection = false
                )
            }

            is TransactionCreateAction.ScanReceipt -> {
                viewModelScope.launch {
                    _state.update { it.copy(isAiScanning = true, aiScanError = null) }
                    when (val result = scanReceiptUseCase.invoke(action.imageBytes)) {
                        is com.naveenapps.expensemanager.core.model.Resource.Success -> {
                            val data = result.data
                            _state.update { current ->
                                val matchedCategory = current.categories.matchCategory(data.suggestedCategory)
                                    ?: current.selectedCategory

                                val items = data.items
                                val totalAmount = data.amount ?: 0.0
                                
                                var isSplitScan = items != null && items.size > 1
                                val finalSplitItems = mutableListOf<TransactionSplitItemState>()

                                if (isSplitScan) {
                                    items!!.forEach { aiItem ->
                                        val itemAmount = aiItem.amount ?: 0.0
                                        if (itemAmount > 0.0) {
                                            val itemMatchedCategory = current.categories.matchCategory(aiItem.suggestedCategory) ?: current.selectedCategory
                                            finalSplitItems.add(
                                                createSplitItemState(
                                                    category = itemMatchedCategory,
                                                    amount = itemAmount,
                                                    notes = aiItem.name ?: ""
                                                )
                                            )
                                        }
                                    }

                                    // Check sum and add adjustment if needed
                                    val itemsSum = finalSplitItems.sumOf { numberFormatRepository.parseToDouble(it.amount.value) ?: 0.0 }
                                    if (itemsSum < totalAmount - 0.01) {
                                        finalSplitItems.add(
                                            createSplitItemState(
                                                category = current.selectedCategory,
                                                amount = totalAmount - itemsSum,
                                                notes = "Ajustement (Taxes/Frais)"
                                            )
                                        )
                                    } else if (itemsSum > totalAmount + 0.01) {
                                        // Over sum is problematic because split amounts must be positive
                                        // We will just let the user fix it manually instead of silently failing
                                    }

                                    if (finalSplitItems.size < 2) {
                                        isSplitScan = false
                                    }
                                }

                                val newSplitItems = if (isSplitScan) {
                                    finalSplitItems
                                } else {
                                    current.splitItems
                                }

                                current.copy(
                                    isAiScanning = false,
                                    imagePath = action.imagePath ?: current.imagePath,
                                    amount = current.amount.copy(
                                        value = data.amount?.let { numberFormatRepository.formatForEditing(it) } ?: current.amount.value
                                    ),
                                    notes = current.notes.copy(
                                        value = data.merchantName ?: current.notes.value
                                    ),
                                    dateTime = data.date ?: current.dateTime,
                                    selectedCategory = matchedCategory,
                                    isSplit = isSplitScan,
                                    splitItems = newSplitItems,
                                )
                            }
                            
                            // Calculate split remaining if it's split
                            _state.update { current ->
                                if (current.isSplit) {
                                    current.copy(
                                        splitRemaining = calculateSplitRemaining(
                                            current, 
                                            numberFormatRepository.parseToDouble(current.amount.value)
                                        )
                                    )
                                } else {
                                    current
                                }
                            }
                        }
                        is com.naveenapps.expensemanager.core.model.Resource.Error -> {
                            _state.update {
                                it.copy(
                                    isAiScanning = false,
                                    aiScanError = result.exception.message ?: "Erreur lors du scan"
                                )
                            }
                        }
                    }
                }
            }

            TransactionCreateAction.ClearAiScanError -> _state.update { it.copy(aiScanError = null) }
            
            TransactionCreateAction.ClearSaveError -> _state.update { it.copy(saveError = null) }
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
