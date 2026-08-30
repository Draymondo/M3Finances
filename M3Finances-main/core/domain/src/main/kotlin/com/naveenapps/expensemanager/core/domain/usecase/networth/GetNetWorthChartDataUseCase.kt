package com.naveenapps.expensemanager.core.domain.usecase.networth

import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.common.utils.toCompleteDate
import com.naveenapps.expensemanager.core.common.utils.toCompleteDateWithDate
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYear
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.filter.daterange.GetDateRangeUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.filter.daterange.GetTransactionGroupTypeUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.FloatEntryModel
import com.naveenapps.expensemanager.core.model.DateRangeType
import com.naveenapps.expensemanager.core.model.GroupType
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.isDebt
import com.naveenapps.expensemanager.core.model.isIncome
import com.naveenapps.expensemanager.core.model.isSavingsGoal
import com.naveenapps.expensemanager.core.model.isTransfer
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.util.Date
import kotlin.time.ExperimentalTime

/**
 * Net worth over time, computed entirely from account balances and the transaction ledger — no
 * separate history table, same "derive from transactions instead of a parallel record" approach
 * as the rest of the app (see e.g. `SavingsGoal`).
 *
 * The trick: account balances only exist as a single current number (see `TransactionDao`'s
 * `adjustAccountBalance`), not a dated history, so there's no direct way to read "net worth as of
 * 3 months ago". Instead, this walks backward conceptually — starting from today's net worth and
 * undoing transactions one at a time — but computes it as a single forward pass for efficiency:
 * `netWorth(T) = baseNetWorth + (sum of every transaction's effect on visible-account balances,
 * dated on or before T)`, where `baseNetWorth` is what's left after backing out every
 * transaction's effect from today's total (i.e. the net worth implied before any transaction
 * ever happened). Walking chronologically forward and accumulating that sum bucket by bucket
 * avoids recomputing it from scratch at each point.
 *
 * "Visible" mirrors `GetAllAccountsUseCase`: the hidden [com.naveenapps.expensemanager.core.model.AccountType.DEBT]
 * and [com.naveenapps.expensemanager.core.model.AccountType.SAVINGS_GOAL] counterparty accounts are excluded, so
 * (consistent with how those already affect the dashboard's current total) contributing to a
 * goal or lending money shows up here as the visible dip it already causes today — this chart
 * just shows that dip's history instead of only its latest value.
 */
class GetNetWorthChartDataUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val getDateRangeUseCase: GetDateRangeUseCase,
    private val getTransactionGroupTypeUseCase: GetTransactionGroupTypeUseCase,
    private val dispatcher: AppCoroutineDispatchers,
) {
    @OptIn(ExperimentalTime::class)
    fun invoke(): Flow<NetWorthChartData?> {
        return combine(
            getDateRangeUseCase.invoke(),
            getCurrencyUseCase.invoke(),
            accountRepository.getAccounts(),
            transactionRepository.getAllTransaction(),
        ) { dateRangeModel, currency, accounts, transactionsOrNull ->
            val transactions = transactionsOrNull.orEmpty()

            if (accounts.isEmpty() || transactions.isEmpty()) {
                // Nothing to plot a trend from — every account's balance has been constant
                // since it was created (or there are no accounts at all).
                return@combine null
            }

            val visibleAccountIds = accounts
                .filterNot { it.type.isDebt() || it.type.isSavingsGoal() }
                .map { it.id }
                .toSet()

            val currentNetWorth = accounts
                .filter { it.id in visibleAccountIds }
                .sumOf { it.amount }

            val totalEffect = transactions.sumOf { visibleEffect(it, visibleAccountIds) }
            // What net worth would have been before any transaction ever happened — the
            // forward walk below builds back up from here.
            val baseNetWorth = currentNetWorth - totalEffect

            val isAllTime = dateRangeModel.type == DateRangeType.ALL
            val groupType = if (isAllTime) {
                GroupType.YEAR
            } else {
                getTransactionGroupTypeUseCase.invoke(dateRangeModel.type)
            }

            val sortedTransactions = transactions.sortedBy { it.createdOn.time }
            val earliestTransactionMs = sortedTransactions.first().createdOn.time
            val latestTransactionMs = sortedTransactions.last().createdOn.time
            val nowMs = System.currentTimeMillis()

            val ranges = dateRangeModel.dateRanges
            val effectiveFromMs = if (isAllTime) {
                kotlin.time.Instant.fromEpochMilliseconds(earliestTransactionMs)
                    .minus(1, DateTimeUnit.YEAR, TimeZone.UTC)
                    .toEpochMilliseconds()
            } else {
                maxOf(ranges[0], earliestTransactionMs)
            }
            val effectiveToMs = if (isAllTime) {
                kotlin.time.Instant.fromEpochMilliseconds(latestTransactionMs)
                    .plus(1, DateTimeUnit.YEAR, TimeZone.UTC)
                    .toEpochMilliseconds()
            } else {
                // Never project past today — there's no data beyond "now" to plot.
                minOf(ranges[1], nowMs)
            }

            var fromDate = kotlin.time.Instant.fromEpochMilliseconds(effectiveFromMs)
            val toDate = kotlin.time.Instant.fromEpochMilliseconds(effectiveToMs)

            val dates = ArrayList<String>()
            val points = ArrayList<FloatEntryModel>()
            var index = 0
            var transactionCursor = 0
            var cumulativeEffect = 0.0

            while (fromDate <= toDate && index < MAX_CHART_POINTS) {
                val boundaryMs = fromDate.toEpochMilliseconds()
                while (
                    transactionCursor < sortedTransactions.size &&
                    sortedTransactions[transactionCursor].createdOn.time <= boundaryMs
                ) {
                    cumulativeEffect += visibleEffect(sortedTransactions[transactionCursor], visibleAccountIds)
                    transactionCursor++
                }

                val netWorthAtBoundary = baseNetWorth + cumulativeEffect

                dates.add(groupValue(groupType, fromDate.toEpochMilliseconds().toCompleteDate()))
                points.add(
                    FloatEntryModel(
                        index,
                        netWorthAtBoundary,
                        getFormattedAmountUseCase.invoke(netWorthAtBoundary, currency),
                    ),
                )

                fromDate = getAdjustedDateTime(groupType, fromDate)
                index++
            }

            if (points.isEmpty()) null else NetWorthChartData(points, dates)
        }.flowOn(dispatcher.computation)
    }

    /** This transaction's net effect on the combined balance of every *visible* account (i.e.
     * excluding [com.naveenapps.expensemanager.core.model.AccountType.DEBT]/[com.naveenapps.expensemanager.core.model.AccountType.SAVINGS_GOAL]
     * hidden accounts) — mirrors the sign conventions `TransactionRepositoryImpl`/`TransactionDao`
     * use to actually apply a transaction to account balances, just without touching the
     * database. A transfer between two visible accounts nets to zero here, same as it does on
     * the dashboard's current total, since it's only moving money internally. */
    private fun visibleEffect(transaction: Transaction, visibleAccountIds: Set<String>): Double {
        val fromDelta = if (transaction.type.isIncome()) {
            transaction.amount.amount
        } else {
            -transaction.amount.amount
        }
        var effect = if (transaction.fromAccountId in visibleAccountIds) fromDelta else 0.0

        if (transaction.type.isTransfer()) {
            val toAccountId = transaction.toAccountId
            if (toAccountId != null && toAccountId in visibleAccountIds) {
                effect += -fromDelta
            }
        }

        return effect
    }

    @OptIn(ExperimentalTime::class)
    private fun getAdjustedDateTime(
        groupType: GroupType,
        fromDate: kotlin.time.Instant,
    ) = when (groupType) {
        GroupType.YEAR -> fromDate.plus(1, DateTimeUnit.YEAR, TimeZone.UTC)
        GroupType.MONTH -> fromDate.plus(1, DateTimeUnit.MONTH, TimeZone.UTC)
        GroupType.DATE -> fromDate.plus(1, DateTimeUnit.DAY, TimeZone.UTC)
    }

    private fun groupValue(groupType: GroupType, date: Date) = when (groupType) {
        GroupType.YEAR -> date.toYear()
        GroupType.MONTH -> date.toMonthAndYear()
        GroupType.DATE -> date.toCompleteDateWithDate()
    }

    companion object {
        // Same reasoning/cap as GetChartDataUseCase — keeps the series readable on a phone
        // screen and bounds the loop for wide CUSTOM ranges.
        private const val MAX_CHART_POINTS = 500
    }
}

data class NetWorthChartData(
    val chartData: List<FloatEntryModel>,
    val dates: List<String>,
)
