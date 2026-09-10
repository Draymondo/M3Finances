package com.naveenapps.expensemanager.feature.transaction.create

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.DeleteTransactionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.FindTransactionByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.UpdateTransactionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.PredictCategoryForNotesUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ScanReceiptUseCase
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import com.naveenapps.expensemanager.core.domain.usecase.transaction.SuggestCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.SuggestContributionUseCase
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.AddSavingsGoalContributionUseCase
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.repository.FeedbackRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import com.naveenapps.expensemanager.core.testing.FAKE_EXPENSE_TRANSACTION
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionCreateViewModelTest : BaseCoroutineTest() {

    private val getCurrencyUseCase: GetCurrencyUseCase = mock()
    private val getAllAccountsUseCase: GetAllAccountsUseCase = mock()
    private val getAllCategoryUseCase: GetAllCategoryUseCase = mock()
    private val getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase = mock()
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase = mock()
    private val findTransactionByIdUseCase: FindTransactionByIdUseCase = mock()
    private val addTransactionUseCase: AddTransactionUseCase = mock()
    private val updateTransactionUseCase: UpdateTransactionUseCase = mock()
    private val deleteTransactionUseCase: DeleteTransactionUseCase = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val appComposeNavigator: AppComposeNavigator = mock()
    private val numberFormatRepository: NumberFormatRepository = mock()
    private val feedbackRepository: FeedbackRepository = mock()
    private val predictCategoryForNotesUseCase: PredictCategoryForNotesUseCase = mock()
    private val scanReceiptUseCase: ScanReceiptUseCase = mock()
    private val pendingTransactionRepository: PendingTransactionRepository = mock()
    private val suggestCategoryUseCase: SuggestCategoryUseCase = mock()
    private val suggestContributionUseCase: SuggestContributionUseCase = mock()
    private val addSavingsGoalContributionUseCase: AddSavingsGoalContributionUseCase = mock()

    private val categoriesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<Category>>(emptyList())

    private lateinit var viewModel: TransactionCreateViewModel

    private val fakeCurrency = Currency(symbol = "$", name = "USD")

    private val fakeFromAccount = AccountUiModel(
        id = "from-account-id",
        name = "Checking",
        storedIcon = StoredIcon(name = "ic_wallet", backgroundColor = "#000000"),
        amount = Amount(1000.0),
        amountTextColor = 0,
    )

    private val fakeToAccount = AccountUiModel(
        id = "to-account-id",
        name = "Savings",
        storedIcon = StoredIcon(name = "ic_wallet", backgroundColor = "#000000"),
        amount = Amount(500.0),
        amountTextColor = 0,
    )

    private val fakeCategory = Category(
        id = "category-id",
        name = "Food",
        type = CategoryType.EXPENSE,
        storedIcon = StoredIcon(name = "ic_food", backgroundColor = "#FF0000"),
        createdOn = Date(),
        updatedOn = Date(),
    )

    override fun onCreate() {
        super.onCreate()

        whenever(getDefaultCurrencyUseCase.invoke()).thenReturn(fakeCurrency)
        whenever(numberFormatRepository.formatForEditing(0.0)).thenReturn("0.00")
        whenever(numberFormatRepository.parseToDouble(any())).thenAnswer {
            it.getArgument<String>(0).toDoubleOrNull()
        }
        whenever(getCurrencyUseCase.invoke()).thenReturn(flowOf(fakeCurrency))
        whenever(getAllAccountsUseCase.invoke()).thenReturn(flowOf(emptyList()))
        whenever(getAllCategoryUseCase.invoke()).thenReturn(categoriesFlow)
        whenever(settingsRepository.getDefaultAccount()).thenReturn(flowOf(null))
        whenever(settingsRepository.getDefaultIncomeCategory()).thenReturn(flowOf(null))
        whenever(settingsRepository.getDefaultExpenseCategory()).thenReturn(flowOf(null))
        whenever(feedbackRepository.shouldShowFeedbackDialog()).thenReturn(flowOf(false))
        whenever(getFormattedAmountUseCase.invoke(any(), any())).thenAnswer {
            val amount = it.getArgument<Double>(0)
            Amount(amount = amount, amountString = "$amount $")
        }

        viewModel = TransactionCreateViewModel(
            savedStateHandle = SavedStateHandle(),
            getCurrencyUseCase = getCurrencyUseCase,
            getAllAccountsUseCase = getAllAccountsUseCase,
            getAllCategoryUseCase = getAllCategoryUseCase,
            getDefaultCurrencyUseCase = getDefaultCurrencyUseCase,
            getFormattedAmountUseCase = getFormattedAmountUseCase,
            findTransactionByIdUseCase = findTransactionByIdUseCase,
            addTransactionUseCase = addTransactionUseCase,
            updateTransactionUseCase = updateTransactionUseCase,
            deleteTransactionUseCase = deleteTransactionUseCase,
            settingsRepository = settingsRepository,
            appComposeNavigator = appComposeNavigator,
            numberFormatRepository = numberFormatRepository,
            feedbackRepository = feedbackRepository,
            predictCategoryForNotesUseCase = predictCategoryForNotesUseCase,
            scanReceiptUseCase = scanReceiptUseCase,
            pendingTransactionRepository = pendingTransactionRepository,
            suggestCategoryUseCase = suggestCategoryUseCase,
            suggestContributionUseCase = suggestContributionUseCase,
            addSavingsGoalContributionUseCase = addSavingsGoalContributionUseCase,
        )
    }

    // region helpers

    private fun buildState(
        notes: String = "Test notes",
        dateTime: Date = Date(),
        transactionType: TransactionType = TransactionType.EXPENSE,
        selectedCategory: Category = fakeCategory,
        selectedFromAccount: AccountUiModel = fakeFromAccount,
        selectedToAccount: AccountUiModel = fakeToAccount,
    ) = TransactionCreateState(
        amount = TextFieldValue(value = "100.00", valueError = false, onValueChange = null),
        notes = TextFieldValue(value = notes, valueError = false, onValueChange = null),
        dateTime = dateTime,
        transactionType = transactionType,
        currency = fakeCurrency,
        selectedCategory = selectedCategory,
        selectedFromAccount = selectedFromAccount,
        selectedToAccount = selectedToAccount,
        accounts = emptyList(),
        categories = emptyList(),
        accountSelection = AccountSelection.FROM_ACCOUNT,
        showDeleteDialog = false,
        showDeleteButton = false,
        showNumberPad = false,
        showCategorySelection = false,
        showAccountSelection = false,
        showDateSelection = false,
        showTimeSelection = false,
        isSplit = false,
        splitItems = emptyList(),
        splitCategorySelectionIndex = null,
        splitTotalError = false,
        splitRemaining = Amount(0.0),
    )

    private fun setEditingTransaction(transaction: Transaction) {
        val field = TransactionCreateViewModel::class.java.getDeclaredField("editingTransaction")
        field.isAccessible = true
        field.set(viewModel, transaction)
    }

    // endregion

    // region toAccountId

    @Test
    fun `expense transaction sets toAccountId to null`() {
        val result = viewModel.buildTransactionFromState(
            buildState(transactionType = TransactionType.EXPENSE),
            amountValue = 100.0,
        )

        assertThat(result.toAccountId).isNull()
    }

    @Test
    fun `income transaction sets toAccountId to null`() {
        val result = viewModel.buildTransactionFromState(
            buildState(transactionType = TransactionType.INCOME),
            amountValue = 100.0,
        )

        assertThat(result.toAccountId).isNull()
    }

    @Test
    fun `transfer transaction sets toAccountId to the to-account id`() {
        val result = viewModel.buildTransactionFromState(
            buildState(
                transactionType = TransactionType.TRANSFER,
                selectedToAccount = fakeToAccount,
            ),
            amountValue = 100.0,
        )

        assertThat(result.toAccountId).isEqualTo(fakeToAccount.id)
    }

    // endregion

    // region id (create vs edit mode)

    @Test
    fun `create mode generates a non-blank id`() {
        val result = viewModel.buildTransactionFromState(buildState(), amountValue = 100.0)

        assertThat(result.id).isNotEmpty()
    }

    @Test
    fun `create mode generates a unique id on each call`() {
        val first = viewModel.buildTransactionFromState(buildState(), amountValue = 100.0)
        val second = viewModel.buildTransactionFromState(buildState(), amountValue = 100.0)

        assertThat(first.id).isNotEqualTo(second.id)
    }

    @Test
    fun `edit mode reuses the existing transaction id`() {
        setEditingTransaction(FAKE_EXPENSE_TRANSACTION.copy(id = "existing-id-123"))

        val result = viewModel.buildTransactionFromState(buildState(), amountValue = 100.0)

        assertThat(result.id).isEqualTo("existing-id-123")
    }

    // endregion

    // region field mapping

    @Test
    fun `maps notes from state`() {
        val result = viewModel.buildTransactionFromState(
            buildState(notes = "Dinner at restaurant"),
            amountValue = 100.0,
        )

        assertThat(result.notes).isEqualTo("Dinner at restaurant")
    }

    @Test
    fun `maps categoryId from the selected category`() {
        val result = viewModel.buildTransactionFromState(
            buildState(selectedCategory = fakeCategory),
            amountValue = 100.0,
        )

        assertThat(result.categoryId).isEqualTo(fakeCategory.id)
    }

    @Test
    fun `maps fromAccountId from the selected from-account`() {
        val result = viewModel.buildTransactionFromState(
            buildState(selectedFromAccount = fakeFromAccount),
            amountValue = 100.0,
        )

        assertThat(result.fromAccountId).isEqualTo(fakeFromAccount.id)
    }

    @Test
    fun `maps transaction type from state`() {
        TransactionType.entries.forEach { type ->
            val result = viewModel.buildTransactionFromState(
                buildState(transactionType = type),
                amountValue = 100.0,
            )

            assertThat(result.type).isEqualTo(type)
        }
    }

    @Test
    fun `maps amount from the amountValue parameter`() {
        val result = viewModel.buildTransactionFromState(buildState(), amountValue = 250.75)

        assertThat(result.amount.amount).isEqualTo(250.75)
    }

    @Test
    fun `maps state dateTime as createdOn`() {
        val fixedDate = Date(1_000_000_000L)

        val result = viewModel.buildTransactionFromState(
            buildState(dateTime = fixedDate),
            amountValue = 100.0,
        )

        assertThat(result.createdOn).isEqualTo(fixedDate)
    }

    @Test
    fun `sets updatedOn to the current time`() {
        val before = Date()
        val result = viewModel.buildTransactionFromState(buildState(), amountValue = 100.0)
        val after = Date()

        assertThat(result.updatedOn.time).isAtLeast(before.time)
        assertThat(result.updatedOn.time).isAtMost(after.time)
    }

    @Test
    fun `sets imagePath to empty string`() {
        val result = viewModel.buildTransactionFromState(buildState(), amountValue = 100.0)

        assertThat(result.imagePath).isEmpty()
    }

    @Test
    fun `auto-categorization predicts and sets category when notes change`() = runTest {
        val notes = "Orange"
        val predictedCategoryId = "internet-category-id"
        val predictedCategory = Category(
            id = predictedCategoryId,
            name = "Internet",
            type = CategoryType.EXPENSE,
            storedIcon = StoredIcon("", ""),
            createdOn = Date(),
            updatedOn = Date()
        )

        whenever(predictCategoryForNotesUseCase.invoke(notes, TransactionType.EXPENSE)).thenReturn(predictedCategoryId)

        categoriesFlow.value = listOf(predictedCategory)
        testScheduler.runCurrent()

        viewModel.state.value.notes.onValueChange?.invoke(notes)
        testScheduler.runCurrent()

        assertThat(viewModel.state.value.selectedCategory.id).isEqualTo(predictedCategoryId)
    }

    @Test
    fun `auto-categorization does not override category if manually selected`() = runTest {
        val notes = "Orange"
        val predictedCategoryId = "internet-category-id"
        val predictedCategory = Category(
            id = predictedCategoryId,
            name = "Internet",
            type = CategoryType.EXPENSE,
            storedIcon = StoredIcon("", ""),
            createdOn = Date(),
            updatedOn = Date()
        )

        whenever(predictCategoryForNotesUseCase.invoke(notes, TransactionType.EXPENSE)).thenReturn(predictedCategoryId)

        categoriesFlow.value = listOf(predictedCategory, fakeCategory)
        testScheduler.runCurrent()

        viewModel.processAction(TransactionCreateAction.SelectCategory(fakeCategory))

        viewModel.state.value.notes.onValueChange?.invoke(notes)
        testScheduler.runCurrent()

        assertThat(viewModel.state.value.selectedCategory.id).isEqualTo(fakeCategory.id)
    }

    @Test
    fun `income save with percentage goals triggers suggestion dialog with formatted amounts`() = runTest {
        val goal = com.naveenapps.expensemanager.core.model.SavingsGoal(
            id = "goal-1",
            accountId = "acc-goal-1",
            name = "Vacances",
            targetAmount = 1000.0,
            targetDate = null,
            notes = "",
            isAchieved = false,
            createdOn = Date(),
            updatedOn = Date(),
            savingsStrategy = com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME,
            targetPercentage = 10.0,
        )
        val incomeAmount = 500.0
        val formattedAmount = Amount(50.0, "50,00 $")
        whenever(addTransactionUseCase.invoke(any())).thenReturn(com.naveenapps.expensemanager.core.model.Resource.Success(true))
        whenever(suggestContributionUseCase.invoke(incomeAmount)).thenReturn(mapOf(goal to 50.0))
        whenever(getFormattedAmountUseCase.invoke(50.0, fakeCurrency)).thenReturn(formattedAmount)
        whenever(numberFormatRepository.parseToDouble("500.00")).thenReturn(500.0)

        viewModel.processAction(TransactionCreateAction.ChangeTransactionType(TransactionType.INCOME))
        viewModel.state.value.amount.onValueChange?.invoke("500.00")
        viewModel.processAction(TransactionCreateAction.Save)
        testScheduler.runCurrent()

        val state = viewModel.state.value
        assertThat(state.showSuggestionDialog).isTrue()
        assertThat(state.suggestedContributions[goal]?.amountString).isEqualTo("50,00 $")
    }

    @Test
    fun `accepting suggestions calls AddSavingsGoalContributionUseCase and closes page on success`() = runTest {
        val goal = com.naveenapps.expensemanager.core.model.SavingsGoal(
            id = "goal-1",
            accountId = "acc-goal-1",
            name = "Vacances",
            targetAmount = 1000.0,
            targetDate = null,
            notes = "",
            isAchieved = false,
            createdOn = Date(),
            updatedOn = Date(),
            savingsStrategy = com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME,
            targetPercentage = 10.0,
        )
        val incomeAmount = 500.0
        val formattedAmount = Amount(50.0, "50,00 $")
        whenever(addTransactionUseCase.invoke(any())).thenReturn(com.naveenapps.expensemanager.core.model.Resource.Success(true))
        whenever(suggestContributionUseCase.invoke(incomeAmount)).thenReturn(mapOf(goal to 50.0))
        whenever(getFormattedAmountUseCase.invoke(50.0, fakeCurrency)).thenReturn(formattedAmount)
        whenever(numberFormatRepository.parseToDouble("500.00")).thenReturn(500.0)
        whenever(addSavingsGoalContributionUseCase.invoke(eq(goal), eq(50.0), any(), eq(false), any()))
            .thenReturn(com.naveenapps.expensemanager.core.model.Resource.Success(true))

        viewModel.processAction(TransactionCreateAction.ChangeTransactionType(TransactionType.INCOME))
        viewModel.state.value.amount.onValueChange?.invoke("500.00")
        viewModel.processAction(TransactionCreateAction.Save)
        testScheduler.runCurrent()

        viewModel.processAction(TransactionCreateAction.AcceptSuggestion)
        testScheduler.runCurrent()

        assertThat(viewModel.state.value.showSuggestionDialog).isFalse()
        verify(addSavingsGoalContributionUseCase).invoke(
            savingsGoal = eq(goal),
            amount = eq(50.0),
            realAccountId = any(),
            isWithdrawal = eq(false),
            notes = any(),
        )
        verify(appComposeNavigator).popBackStack()
    }

    @Test
    fun `accepting suggestions handles Resource Error without crashing and displays error message`() = runTest {
        val goal = com.naveenapps.expensemanager.core.model.SavingsGoal(
            id = "goal-1",
            accountId = "acc-goal-1",
            name = "Vacances",
            targetAmount = 1000.0,
            targetDate = null,
            notes = "",
            isAchieved = false,
            createdOn = Date(),
            updatedOn = Date(),
            savingsStrategy = com.naveenapps.expensemanager.core.model.SavingsStrategy.PERCENTAGE_INCOME,
            targetPercentage = 10.0,
        )
        val incomeAmount = 500.0
        val formattedAmount = Amount(50.0, "50,00 $")
        whenever(addTransactionUseCase.invoke(any())).thenReturn(com.naveenapps.expensemanager.core.model.Resource.Success(true))
        whenever(suggestContributionUseCase.invoke(incomeAmount)).thenReturn(mapOf(goal to 50.0))
        whenever(getFormattedAmountUseCase.invoke(50.0, fakeCurrency)).thenReturn(formattedAmount)
        whenever(numberFormatRepository.parseToDouble("500.00")).thenReturn(500.0)
        whenever(addSavingsGoalContributionUseCase.invoke(eq(goal), eq(50.0), any(), eq(false), any()))
            .thenReturn(com.naveenapps.expensemanager.core.model.Resource.Error(Exception("No category available")))

        viewModel.processAction(TransactionCreateAction.ChangeTransactionType(TransactionType.INCOME))
        viewModel.state.value.amount.onValueChange?.invoke("500.00")
        viewModel.processAction(TransactionCreateAction.Save)
        testScheduler.runCurrent()

        viewModel.processAction(TransactionCreateAction.AcceptSuggestion)
        testScheduler.runCurrent()

        assertThat(viewModel.state.value.showSuggestionDialog).isFalse()
        assertThat(viewModel.state.value.saveError).contains("Vacances")
    }
    // endregion
}
