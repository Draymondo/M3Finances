package com.naveenapps.expensemanager.feature.installmentplan.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.AddInstallmentPaymentUseCase
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.AddInstallmentPlanUseCase
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.DeleteInstallmentPlanUseCase
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.FindInstallmentPlanByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.GetInstallmentPlanPaymentsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.installmentplan.UpdateInstallmentPlanUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.getAvailableCreditLimit
import com.naveenapps.expensemanager.core.model.progress
import com.naveenapps.expensemanager.core.model.toAccountUiModel
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

class InstallmentPlanCreateViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val findInstallmentPlanByIdUseCase: FindInstallmentPlanByIdUseCase,
    private val addInstallmentPlanUseCase: AddInstallmentPlanUseCase,
    private val updateInstallmentPlanUseCase: UpdateInstallmentPlanUseCase,
    private val deleteInstallmentPlanUseCase: DeleteInstallmentPlanUseCase,
    private val addInstallmentPaymentUseCase: AddInstallmentPaymentUseCase,
    private val getInstallmentPlanPaymentsUseCase: GetInstallmentPlanPaymentsUseCase,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
) : ViewModel() {

    private val accountsReady = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        InstallmentPlanCreateState(
            isEditing = false,
            name = TextFieldValue(value = "", valueError = false, onValueChange = this::setName),
            notes = TextFieldValue(value = "", valueError = false, onValueChange = this::setNotes),
            targetAmount = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setTargetAmountOnChange,
            ),
            deadline = null,
            isCompleted = false,
            currency = getDefaultCurrencyUseCase.invoke(),
            accounts = emptyList(),
            paidAmount = null,
            remainingAmount = null,
            progress = 0f,
            payments = emptyList(),
            showDeleteButton = false,
            showDeleteDialog = false,
            showDeadlineSelection = false,
        )
    )
    val state = _state.asStateFlow()

    private var editingPlan: InstallmentPlan? = null

    init {
        observeAccountsAndCurrency(getCurrencyUseCase, getAllAccountsUseCase)
        loadPlanWhenReady(savedStateHandle)
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
                    paymentAccount = it.paymentAccount ?: mappedAccounts.firstOrNull(),
                )
            }
            accountsReady.update { true }
        }.launchIn(viewModelScope)
    }

    private fun loadPlanWhenReady(savedStateHandle: SavedStateHandle) {
        val id = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID) ?: return
        viewModelScope.launch {
            accountsReady.first { it }
            loadEditingPlan(id)
        }
    }

    private suspend fun loadEditingPlan(id: String) {
        when (val response = findInstallmentPlanByIdUseCase.invoke(id)) {
            is Resource.Error -> Unit
            is Resource.Success -> {
                val plan = response.data
                editingPlan = plan
                _state.update {
                    it.copy(
                        isEditing = true,
                        name = it.name.copy(value = plan.name),
                        notes = it.notes.copy(value = plan.notes),
                        targetAmount = it.targetAmount.copy(
                            value = numberFormatRepository.formatForEditing(plan.targetAmount),
                        ),
                        deadline = plan.deadline,
                        isCompleted = plan.isCompleted,
                        paidAmount = getFormattedAmountUseCase.invoke(
                            plan.account.amount,
                            it.currency,
                        ),
                        remainingAmount = getFormattedAmountUseCase.invoke(
                            (plan.targetAmount - plan.account.amount).coerceAtLeast(0.0),
                            it.currency,
                        ),
                        progress = plan.progress(),
                        showDeleteButton = true,
                    )
                }
                refreshPayments(plan.accountId)
            }
        }
    }

    private suspend fun refreshPayments(accountId: String) {
        val payments = getInstallmentPlanPaymentsUseCase.invoke(accountId)
        _state.update { it.copy(payments = payments) }
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
            val plan = editingPlan ?: return
            viewModelScope.launch {
                val updated = plan.copy(
                    name = currentState.name.value,
                    notes = currentState.notes.value,
                    targetAmount = targetAmountValue,
                    deadline = currentState.deadline,
                    isCompleted = currentState.isCompleted,
                    updatedOn = Date(),
                )
                if (updateInstallmentPlanUseCase.invoke(updated) is Resource.Success) {
                    closePage()
                }
            }
            return
        }

        viewModelScope.launch {
            val now = Date()
            val newPlan = InstallmentPlan(
                id = UUID.randomUUID().toString(),
                accountId = UUID.randomUUID().toString(),
                name = currentState.name.value,
                targetAmount = targetAmountValue,
                deadline = currentState.deadline,
                notes = currentState.notes.value,
                isCompleted = false,
                createdOn = now,
                updatedOn = now,
            )
            val response = addInstallmentPlanUseCase.invoke(newPlan)
            if (response is Resource.Success) {
                closePage()
            }
        }
    }

    private fun delete() {
        val plan = editingPlan ?: return
        viewModelScope.launch {
            if (deleteInstallmentPlanUseCase.invoke(plan) is Resource.Success) {
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

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    private fun setPaymentAmount(amount: String) {
        val amountValue = numberFormatRepository.parseToDouble(amount)
        _state.update {
            it.copy(
                paymentAmount = it.paymentAmount.copy(
                    value = amount,
                    valueError = amount.isBlank() || amountValue == null || amountValue <= 0.0,
                ),
            )
        }
    }

    private fun showPaymentSheet() {
        _state.update {
            it.copy(
                showPaymentSheet = true,
                isRefund = false,
                paymentAmount = TextFieldValue(
                    value = numberFormatRepository.formatForEditing(0.0),
                    valueError = false,
                    onValueChange = this::setPaymentAmount,
                ),
                paymentAccount = it.paymentAccount ?: it.accounts.firstOrNull(),
            )
        }
    }

    private fun confirmPayment() {
        val plan = editingPlan ?: return
        val currentState = _state.value
        val paymentAccount = currentState.paymentAccount ?: return

        val amountValue = numberFormatRepository.parseToDouble(currentState.paymentAmount.value)
        if (amountValue == null || amountValue <= 0.0) {
            _state.update { it.copy(paymentAmount = it.paymentAmount.copy(valueError = true)) }
            return
        }

        viewModelScope.launch {
            val response = addInstallmentPaymentUseCase.invoke(
                installmentPlan = plan,
                amount = amountValue,
                realAccountId = paymentAccount.id,
                isRefund = currentState.isRefund,
                notes = "",
            )
            if (response is Resource.Success) {
                _state.update { it.copy(showPaymentSheet = false) }
                // The plan account's balance changed — reload it so paidAmount/progress and
                // the payment history reflect the operation just recorded.
                loadEditingPlan(plan.id)
            }
        }
    }

    fun processAction(action: InstallmentPlanCreateAction) {
        when (action) {
            InstallmentPlanCreateAction.ClosePage -> closePage()
            InstallmentPlanCreateAction.Save -> save()
            InstallmentPlanCreateAction.Delete -> delete()

            InstallmentPlanCreateAction.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = true) }
            InstallmentPlanCreateAction.DismissDeleteDialog -> _state.update { it.copy(showDeleteDialog = false) }

            InstallmentPlanCreateAction.ShowDeadlineSelection -> _state.update {
                it.copy(showDeadlineSelection = true)
            }
            InstallmentPlanCreateAction.DismissDeadlineSelection -> _state.update {
                it.copy(showDeadlineSelection = false)
            }

            is InstallmentPlanCreateAction.SelectDeadline -> _state.update {
                it.copy(deadline = action.date, showDeadlineSelection = false)
            }

            InstallmentPlanCreateAction.ClearDeadline -> _state.update { it.copy(deadline = null) }

            InstallmentPlanCreateAction.ToggleCompleted -> _state.update { it.copy(isCompleted = !it.isCompleted) }

            InstallmentPlanCreateAction.ShowPaymentSheet -> showPaymentSheet()
            InstallmentPlanCreateAction.DismissPaymentSheet -> _state.update {
                it.copy(showPaymentSheet = false)
            }
            InstallmentPlanCreateAction.TogglePaymentDirection -> _state.update {
                it.copy(isRefund = !it.isRefund)
            }

            InstallmentPlanCreateAction.ShowPaymentAccountSelection -> _state.update {
                it.copy(showPaymentAccountSelection = true)
            }
            InstallmentPlanCreateAction.DismissPaymentAccountSelection -> _state.update {
                it.copy(showPaymentAccountSelection = false)
            }
            is InstallmentPlanCreateAction.SelectPaymentAccount -> _state.update {
                it.copy(paymentAccount = action.account, showPaymentAccountSelection = false)
            }

            InstallmentPlanCreateAction.ConfirmPayment -> confirmPayment()
        }
    }
}
