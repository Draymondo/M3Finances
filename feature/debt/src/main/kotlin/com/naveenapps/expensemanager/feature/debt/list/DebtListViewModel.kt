package com.naveenapps.expensemanager.feature.debt.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.debt.DebtUiModel
import com.naveenapps.expensemanager.core.domain.usecase.debt.GetDebtsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.tools.TrackToolUsageUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.core.model.isLent
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DebtListViewModel(
    getDebtsUseCase: GetDebtsUseCase,
    getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val trackToolUsageUseCase: TrackToolUsageUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(DebtListState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            trackToolUsageUseCase(ToolType.DEBTS)
        }
        combine(
            getDebtsUseCase.invoke(),
            getCurrencyUseCase.invoke(),
        ) { debts, currency ->
            buildState(debts, currency)
        }.onEach { newState ->
            _state.value = newState
        }.launchIn(viewModelScope)
    }

    private fun buildState(debts: List<DebtUiModel>, currency: Currency): DebtListState {
        val open = mutableListOf<DebtUiModel>()
        val closed = mutableListOf<DebtUiModel>()
        for (debt in debts) {
            if (debt.isClosed()) closed.add(debt) else open.add(debt)
        }

        val debtors = groups(open.filter { it.debt.direction.isLent() }, currency)
        val creditors = groups(open.filterNot { it.debt.direction.isLent() }, currency)

        return DebtListState(
            isLoading = false,
            debtors = debtors,
            creditors = creditors,
            settled = groups(closed, currency),
            debtorsTotal = total(debtors, currency),
            creditorsTotal = total(creditors, currency),
        )
    }

    /** Additionne les dettes d'une même personne (nom normalisé) en une seule carte. */
    private fun groups(debts: List<DebtUiModel>, currency: Currency): List<PersonDebtGroup> {
        val today = startOfToday()
        return debts
            .groupBy { it.debt.personName.trim().lowercase(Locale.getDefault()) }
            .map { (key, items) ->
                // En retard d'abord, puis l'échéance la plus proche, puis la plus ancienne.
                val sorted = items.sortedWith(
                    compareBy(
                        { !it.isOverdue(today) },
                        { it.debt.dueDate ?: Date(Long.MAX_VALUE) },
                        { it.debt.createdOn },
                    ),
                )
                PersonDebtGroup(
                    key = key,
                    personName = items.maxByOrNull { it.debt.updatedOn.time }
                        ?.debt?.personName?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: key,
                    debts = sorted,
                    total = getFormattedAmountUseCase.invoke(
                        items.sumOf { it.remainingAmount.amount },
                        currency,
                    ),
                    hasOverdue = items.any { it.isOverdue(today) },
                    nextDueDate = sorted.firstOrNull { it.debt.dueDate != null }?.debt?.dueDate,
                )
            }
            .sortedWith(
                compareByDescending<PersonDebtGroup> { it.hasOverdue }
                    .thenBy { it.nextDueDate ?: Date(Long.MAX_VALUE) }
                    .thenBy { it.personName.lowercase(Locale.getDefault()) },
            )
    }

    private fun total(groups: List<PersonDebtGroup>, currency: Currency): Amount? {
        if (groups.isEmpty()) return null
        val sum = groups.sumOf { group -> group.debts.sumOf { it.remainingAmount.amount } }
        return getFormattedAmountUseCase.invoke(sum, currency)
    }

    private fun DebtUiModel.isClosed(): Boolean {
        return debt.isSettled || remainingAmount.amount <= ZERO_AMOUNT
    }

    /** En retard = échéance passée (avant aujourd'hui) et dette encore ouverte. */
    private fun DebtUiModel.isOverdue(today: Date): Boolean {
        val dueDate = debt.dueDate ?: return false
        return !debt.isSettled && dueDate.before(today)
    }

    private fun startOfToday(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun openCreateScreen(debtId: String? = null) {
        appComposeNavigator.navigate(ExpenseManagerScreens.DebtCreate(debtId))
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: DebtListAction) {
        when (action) {
            DebtListAction.ClosePage -> closePage()
            DebtListAction.OpenDebtCreate -> openCreateScreen()
            is DebtListAction.OpenDebtDetail -> openCreateScreen(action.debtId)
        }
    }

    private companion object {
        /** Un reste dû sous ce seuil est considéré comme soldé (arrondis de centimes). */
        const val ZERO_AMOUNT = 0.005
    }
}
