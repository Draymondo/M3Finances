#!/data/data/com.termux/files/usr/bin/bash
# Ajoute les budgets Semaine/Jour + la carte d'equivalence (creation ET detail).
# Ce script ECRASE entierement chaque fichier liste ci-dessous avec son contenu
# final complet (pas un patch) -- plus sur pour une modification aussi profonde.
# A executer depuis la racine du repo (~/repo).
set -euo pipefail

if [ ! -f "settings.gradle.kts" ]; then
  echo "Erreur : lance ce script depuis la racine du repo (la ou se trouve settings.gradle.kts)."
  exit 1
fi

echo "== Ecriture des fichiers (nouveaux + modifies) =="

echo "  core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model/BudgetPeriod.kt"
mkdir -p "core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model"
cat > "core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model/BudgetPeriod.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.model

/**
 * How a [Budget]'s [Budget.selectedMonth] key should be interpreted and how its spending should
 * be aggregated: against a single day, a calendar week (Monday→Sunday), a calendar month, or an
 * entire calendar year.
 *
 * MONTHLY must stay the first entry (ordinal 0) and YEARLY the second (ordinal 1) — both are
 * persisted as a raw ordinal Int on `BudgetEntity`, and existing rows are backfilled to `0` by
 * migration `MIGRATION_5_6`, so every budget created before this field existed must continue to
 * resolve to MONTHLY. WEEKLY and DAILY were added later and must stay appended after them so
 * already-persisted ordinals never shift.
 */
enum class BudgetPeriod {
    MONTHLY,
    YEARLY,
    WEEKLY,
    DAILY,
}
CLAUDE_FIX_EOF

echo "  core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model/Budget.kt"
mkdir -p "core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model"
cat > "core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model/Budget.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.model

import java.util.Date

data class Budget(
    val id: String,
    val amount: Double = 0.0,
    /**
     * Round-trip key identifying the period this budget covers. Which format applies is
     * determined by [periodType]: a month+year key (`Date.toMonthAndYearKey`) for
     * [BudgetPeriod.MONTHLY], a year-only key (`Date.toYear`) for [BudgetPeriod.YEARLY], a
     * day key (`Date.toDayKey`) for [BudgetPeriod.DAILY], or that same day-key format applied to
     * the Monday of the covered week (`Date.toWeekKey`) for [BudgetPeriod.WEEKLY].
     */
    val selectedMonth: String,
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val categories: List<String>,
    val accounts: List<String>,
    val isAllAccountsSelected: Boolean,
    val isAllCategoriesSelected: Boolean,
    val createdOn: Date,
    val updatedOn: Date,
)
CLAUDE_FIX_EOF

echo "  core/common/src/main/kotlin/com/naveenapps/expensemanager/core/common/utils/DateUtils.kt"
mkdir -p "core/common/src/main/kotlin/com/naveenapps/expensemanager/core/common/utils"
cat > "core/common/src/main/kotlin/com/naveenapps/expensemanager/core/common/utils/DateUtils.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.common.utils

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Formats below fall into two groups:
//
// 1. Purely numeric patterns (digits only, no month/day *names*) — these are pinned to a fixed
//    Locale.US regardless of the app's selected display language. Some of them (notably
//    DateMonthAndYearFormat) are used as round-tripped keys: a value they format is later parsed
//    back with the *same* formatter (e.g. to group transactions by day). If that formatter's
//    Locale changes — which it now can, since the app supports switching its display language at
//    runtime — a value written under one locale can fail to parse under another. Numeric digits
//    don't need localization anyway, so pinning these removes that whole class of bug for free.
//
// 2. Patterns with localized month/day *names* (MonthAndYearFormat, ElabratedMonthDataFormat,
//    DayFormat, HourAndMinutesIn12HoursFormat) are kept locale-aware because they're only ever
//    used for one-way, human-facing display text — never parsed back — so following the app's
//    selected language is both safe and desirable.
//
// MonthAndYearFormat is the one exception that's used for *both* roles (see MonthAndYearKeyFormat
// below for the round-tripped one, used for Budget.selectedMonth).

private val YearDataFormat by lazy {
    SimpleDateFormat("yyyy", Locale.US)
}

private val HourAndMinutesIn24HoursFormat by lazy {
    SimpleDateFormat("HH:mm", Locale.US)
}

private val HourAndMinutesIn12HoursFormat by lazy {
    SimpleDateFormat("hh:mm a", Locale.getDefault())
}

private val MonthFormat by lazy {
    SimpleDateFormat("MM", Locale.US)
}

private val MonthAndYearFormat by lazy {
    SimpleDateFormat("MMMM yyyy", Locale.getDefault())
}

/**
 * Same pattern as [MonthAndYearFormat] but always English, used only for values that get
 * persisted or matched later (currently `Budget.selectedMonth`, and the transaction date it's
 * compared against). Never use this to show text to the user — use [Date.toMonthAndYear] for
 * that instead.
 */
private val MonthAndYearKeyFormat by lazy {
    SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
}

private val DateMonthAndYearFormat by lazy {
    SimpleDateFormat("dd/MM/yyyy", Locale.US)
}

private val DateFormat by lazy {
    SimpleDateFormat("dd", Locale.US)
}

private val DateAndMonthFormat by lazy {
    SimpleDateFormat("dd/MM", Locale.US)
}

private val ElabratedMonthDataFormat by lazy {
    SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
}

private val DayFormat by lazy {
    SimpleDateFormat("EEEE", Locale.getDefault())
}

private val ShortMontAndYearFormat by lazy {
    SimpleDateFormat("MM-yyyy", Locale.US)
}


@OptIn(ExperimentalTime::class)
fun getTodayRange(timeZone: TimeZone = TimeZone.currentSystemDefault()): List<Long> {
    val clock = kotlin.time.Clock.System.now()
    val todayStartTime = clock.toLocalDateTime(timeZone).date
    val nextDateStartTime = todayStartTime.plus(1, DateTimeUnit.DAY)
    return listOf(
        todayStartTime.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        nextDateStartTime.atStartOfDayIn(timeZone).toEpochMilliseconds()
    )
}

@OptIn(ExperimentalTime::class)
fun getThisWeekRange(timeZone: TimeZone = TimeZone.currentSystemDefault()): List<Long> {
    val clock = kotlin.time.Clock.System.now()
    val todayDateTime = clock.toLocalDateTime(timeZone)
    val startOfTheWeekDay =
        todayDateTime.date.minus(todayDateTime.dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
    val endTimeOfTheWeek = startOfTheWeekDay.plus(1, DateTimeUnit.WEEK)
    return listOf(
        startOfTheWeekDay.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        endTimeOfTheWeek.atStartOfDayIn(timeZone).toEpochMilliseconds()
    )
}

@OptIn(ExperimentalTime::class)
fun getThisMonthRange(timeZone: TimeZone = TimeZone.currentSystemDefault()): List<Long> {
    val clock = kotlin.time.Clock.System.now()
    val todayDateTime = clock.toLocalDateTime(timeZone)
    val startOfTheWeekDay = todayDateTime.date.minus(todayDateTime.dayOfMonth - 1, DateTimeUnit.DAY)
    val endTimeOfTheWeek = startOfTheWeekDay.plus(1, DateTimeUnit.MONTH)
    return listOf(
        startOfTheWeekDay.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        endTimeOfTheWeek.atStartOfDayIn(timeZone).toEpochMilliseconds()
    )
}

@OptIn(ExperimentalTime::class)
fun getThisYearRange(timeZone: TimeZone = TimeZone.currentSystemDefault()): List<Long> {
    val clock = kotlin.time.Clock.System.now()
    val todayDateTime = clock.toLocalDateTime(timeZone)
    val startOfTheWeekDay = todayDateTime.date.minus(todayDateTime.dayOfYear - 1, DateTimeUnit.DAY)
    val endTimeOfTheWeek = startOfTheWeekDay.plus(1, DateTimeUnit.YEAR)
    return listOf(
        startOfTheWeekDay.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        endTimeOfTheWeek.atStartOfDayIn(timeZone).toEpochMilliseconds()
    )
}

@OptIn(ExperimentalTime::class)
fun Long.fromLocalToUTCTimeStamp(): Long {
    return Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toInstant(TimeZone.UTC)
        .toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun Long.fromUTCToLocalTimeStamp(): Long {
    return Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.UTC)
        .toInstant(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}

fun Long.fromUTCToLocalDate(): Date {
    return Date(this.fromUTCToLocalTimeStamp())
}

@OptIn(ExperimentalTime::class)
fun Long.toExactStartOfTheDay(): Date {
    val dateTime = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC)
    return Date(dateTime.date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds())
}

@OptIn(ExperimentalTime::class)
fun Date.getStartOfTheMonth(): Long {
    val dateTime =
        Instant.fromEpochMilliseconds(this.time).toLocalDateTime(TimeZone.currentSystemDefault())
    return dateTime.date.minus(dateTime.dayOfMonth, DateTimeUnit.DAY)
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun Date.getEndOfTheMonth(): Long {
    val dateTime =
        Instant.fromEpochMilliseconds(this.time).toLocalDateTime(TimeZone.currentSystemDefault())
    val startOfTheWeekDay = dateTime.date.minus(dateTime.dayOfMonth, DateTimeUnit.DAY)
        .atStartOfDayIn(TimeZone.currentSystemDefault())
    return startOfTheWeekDay.plus(1, DateTimeUnit.MONTH, TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}

/** Year-scoped counterpart of [getStartOfTheMonth], used for yearly (`BudgetPeriod.YEARLY`) budgets. */
@OptIn(ExperimentalTime::class)
fun Date.getStartOfTheYear(): Long {
    val dateTime =
        Instant.fromEpochMilliseconds(this.time).toLocalDateTime(TimeZone.currentSystemDefault())
    return dateTime.date.minus(dateTime.dayOfYear, DateTimeUnit.DAY)
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}

/** Year-scoped counterpart of [getEndOfTheMonth], used for yearly (`BudgetPeriod.YEARLY`) budgets. */
@OptIn(ExperimentalTime::class)
fun Date.getEndOfTheYear(): Long {
    val dateTime =
        Instant.fromEpochMilliseconds(this.time).toLocalDateTime(TimeZone.currentSystemDefault())
    val startOfTheYear = dateTime.date.minus(dateTime.dayOfYear, DateTimeUnit.DAY)
        .atStartOfDayIn(TimeZone.currentSystemDefault())
    return startOfTheYear.plus(1, DateTimeUnit.YEAR, TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}

/** Day-scoped counterpart of [getStartOfTheMonth], used for daily (`BudgetPeriod.DAILY`) budgets. */
@OptIn(ExperimentalTime::class)
fun Date.getStartOfTheDay(): Long {
    val dateTime =
        Instant.fromEpochMilliseconds(this.time).toLocalDateTime(TimeZone.currentSystemDefault())
    return dateTime.date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

/** Day-scoped counterpart of [getEndOfTheMonth], used for daily (`BudgetPeriod.DAILY`) budgets. */
@OptIn(ExperimentalTime::class)
fun Date.getEndOfTheDay(): Long {
    val dateTime =
        Instant.fromEpochMilliseconds(this.time).toLocalDateTime(TimeZone.currentSystemDefault())
    return dateTime.date.atStartOfDayIn(TimeZone.currentSystemDefault())
        .plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}

/** Week-scoped counterpart of [getStartOfTheMonth], used for weekly (`BudgetPeriod.WEEKLY`)
 * budgets. Weeks run Monday→Sunday — same convention already used by [getThisWeekRange]. */
@OptIn(ExperimentalTime::class)
fun Date.getStartOfTheWeek(): Long {
    val dateTime =
        Instant.fromEpochMilliseconds(this.time).toLocalDateTime(TimeZone.currentSystemDefault())
    return dateTime.date.minus(dateTime.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}

/** Week-scoped counterpart of [getEndOfTheMonth], used for weekly (`BudgetPeriod.WEEKLY`) budgets. */
@OptIn(ExperimentalTime::class)
fun Date.getEndOfTheWeek(): Long {
    val dateTime =
        Instant.fromEpochMilliseconds(this.time).toLocalDateTime(TimeZone.currentSystemDefault())
    val startOfTheWeek = dateTime.date.minus(dateTime.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        .atStartOfDayIn(TimeZone.currentSystemDefault())
    return startOfTheWeek.plus(1, DateTimeUnit.WEEK, TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}

fun Long.toCompleteDate(): Date {
    return Date(this)
}

fun Date.toDate(): String {
    return synchronized(DateFormat) { DateFormat.format(this) }
}

fun Date.toDateAndMonth(): String {
    return synchronized(DateAndMonthFormat) { DateAndMonthFormat.format(this) }
}

fun Date.toCompleteDate(): String {
    return synchronized(ElabratedMonthDataFormat) { ElabratedMonthDataFormat.format(this) }
}

fun Date.toCompleteDateWithDate(): String {
    return synchronized(DateMonthAndYearFormat) { DateMonthAndYearFormat.format(this) }
}

fun String.fromCompleteDate(): Date {
    return kotlin.runCatching {
        synchronized(DateMonthAndYearFormat) { DateMonthAndYearFormat.parse(this) }
    }.getOrNull() ?: Date()
}

fun Date.toMonthAndYear(): String {
    return synchronized(MonthAndYearFormat) { MonthAndYearFormat.format(this) }
}

fun String.fromMonthAndYear(): Date? {
    return kotlin.runCatching {
        synchronized(MonthAndYearFormat) { MonthAndYearFormat.parse(this) }
    }.getOrNull()
}

/**
 * Locale-independent counterpart of [toMonthAndYear], for values that get persisted or compared
 * later (e.g. `Budget.selectedMonth`). Always formats/parses in English so a budget saved while
 * the app was in one language can still be read back correctly after the user switches to
 * another — see [MonthAndYearKeyFormat].
 */
fun Date.toMonthAndYearKey(): String {
    return synchronized(MonthAndYearKeyFormat) { MonthAndYearKeyFormat.format(this) }
}

/** See [Date.toMonthAndYearKey]. Returns null instead of throwing on unparseable input. */
fun String.fromMonthAndYearKey(): Date? {
    return kotlin.runCatching {
        synchronized(MonthAndYearKeyFormat) { MonthAndYearKeyFormat.parse(this) }
    }.getOrNull()
}

fun Date.toMonth(): Int {
    return synchronized(MonthFormat) { MonthFormat.format(this) }.toInt()
}

fun Date.toYearInt(): Int {
    return this.toYear().toInt()
}

fun Date.toYear(): String {
    return synchronized(YearDataFormat) { YearDataFormat.format(this) }
}

/**
 * See [Date.toYear]. Returns null instead of throwing on unparseable input. Used as the
 * round-trip key for yearly (`BudgetPeriod.YEARLY`) `Budget.selectedMonth` values, mirroring
 * [fromMonthAndYearKey]. Safe to round-trip like the other purely-numeric formats in this file
 * (see the file header comment) since [YearDataFormat] is already pinned to `Locale.US`.
 */
fun String.fromYear(): Date? {
    return kotlin.runCatching {
        synchronized(YearDataFormat) { YearDataFormat.parse(this) }
    }.getOrNull()
}

/** Purely numeric, `Locale.US`-pinned — same round-trip-safety reasoning as [YearDataFormat] and
 * [MonthAndYearKeyFormat] (see file header). Used for daily (`BudgetPeriod.DAILY`) budgets, and
 * as the basis for [toWeekKey] below. */
private val DayKeyFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd", Locale.US)
}

/** Round-trip key for a single day, used for `Budget.selectedMonth` on daily budgets. Never use
 * this to show text to the user. */
fun Date.toDayKey(): String {
    return synchronized(DayKeyFormat) { DayKeyFormat.format(this) }
}

/** See [Date.toDayKey]. Returns null instead of throwing on unparseable input. */
fun String.fromDayKey(): Date? {
    return kotlin.runCatching {
        synchronized(DayKeyFormat) { DayKeyFormat.parse(this) }
    }.getOrNull()
}

/** Round-trip key for a whole week, used for `Budget.selectedMonth` on weekly budgets — simply
 * the [toDayKey] of that week's Monday, so no new date format is needed and the key sorts/parses
 * exactly like a daily one. Weeks run Monday→Sunday (see [getStartOfTheWeek]). */
fun Date.toWeekKey(): String {
    return this.getStartOfTheWeek().toCompleteDate().toDayKey()
}

/** See [Date.toWeekKey]. Returns null instead of throwing on unparseable input. */
fun String.fromWeekKey(): Date? {
    return this.fromDayKey()
}

fun Date.toTimeAndMinutes(): String {
    return synchronized(HourAndMinutesIn24HoursFormat) { HourAndMinutesIn24HoursFormat.format(this) }
}

fun Date.toMonthYear(): String {
    return synchronized(MonthAndYearFormat) { MonthAndYearFormat.format(this) }
}

fun Date.toDay(): String {
    return synchronized(DayFormat) { DayFormat.format(this) }
}

fun String.fromTimeAndHour(): Date {
    return kotlin.runCatching {
        synchronized(HourAndMinutesIn24HoursFormat) { HourAndMinutesIn24HoursFormat.parse(this) }
    }.getOrNull() ?: Date()
}

fun Date.toTimeAndMinutesWithAMPM(): String {
    return synchronized(HourAndMinutesIn12HoursFormat) { HourAndMinutesIn12HoursFormat.format(this) }
}

fun String.fromShortMonthAndYearToDate(): Date? {
    return kotlin.runCatching {
        synchronized(ShortMontAndYearFormat) { ShortMontAndYearFormat.parse(this) }
    }.getOrNull()
}
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/GetBudgetTransactionsUseCase.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/GetBudgetTransactionsUseCase.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.naveenapps.expensemanager.core.common.utils.fromDayKey
import com.naveenapps.expensemanager.core.common.utils.fromMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.fromWeekKey
import com.naveenapps.expensemanager.core.common.utils.fromYear
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheDay
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheMonth
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheYear
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheDay
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheMonth
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheYear
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.expandedForCategoryAccounting
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.CategoryRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull

class GetBudgetTransactionsUseCase(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(budget: Budget): Resource<List<Transaction>> {
        val accounts: List<String> = if (budget.isAllAccountsSelected) {
            accountRepository.getAccounts().firstOrNull()?.map { it.id } ?: emptyList()
        } else {
            budget.accounts
        }

        val categories: List<String> = if (budget.isAllCategoriesSelected) {
            categoryRepository.getCategories().firstOrNull()?.map { it.id } ?: emptyList()
        } else {
            budget.categories
        }

        val date = when (budget.periodType) {
            BudgetPeriod.YEARLY -> budget.selectedMonth.fromYear()
            BudgetPeriod.MONTHLY -> budget.selectedMonth.fromMonthAndYearKey()
            BudgetPeriod.WEEKLY -> budget.selectedMonth.fromWeekKey()
            BudgetPeriod.DAILY -> budget.selectedMonth.fromDayKey()
        }

        date ?: return Resource.Error(IllegalArgumentException("Unknown month value"))

        val startDate = when (budget.periodType) {
            BudgetPeriod.YEARLY -> date.getStartOfTheYear()
            BudgetPeriod.MONTHLY -> date.getStartOfTheMonth()
            BudgetPeriod.WEEKLY -> date.getStartOfTheWeek()
            BudgetPeriod.DAILY -> date.getStartOfTheDay()
        }
        val endDate = when (budget.periodType) {
            BudgetPeriod.YEARLY -> date.getEndOfTheYear()
            BudgetPeriod.MONTHLY -> date.getEndOfTheMonth()
            BudgetPeriod.WEEKLY -> date.getEndOfTheWeek()
            BudgetPeriod.DAILY -> date.getEndOfTheDay()
        }

        val transaction = transactionRepository.getFilteredTransaction(
            accounts = accounts,
            categories = categories,
            transactionType = TransactionType.entries.map { it.ordinal }.toList(),
            startDate = startDate,
            endDate = endDate,
        ).firstOrNull()?.filter {
            when (budget.periodType) {
                BudgetPeriod.YEARLY -> it.createdOn.toYear() == budget.selectedMonth
                BudgetPeriod.MONTHLY -> it.createdOn.toMonthAndYearKey() == budget.selectedMonth
                BudgetPeriod.WEEKLY -> it.createdOn.toWeekKey() == budget.selectedMonth
                BudgetPeriod.DAILY -> it.createdOn.toDayKey() == budget.selectedMonth
            }
        }

        return Resource.Success(transaction?.expandedForCategoryAccounting() ?: emptyList())
    }
}
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/GetBudgetsUseCase.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/GetBudgetsUseCase.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.budget

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.R
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.common.utils.fromDayKey
import com.naveenapps.expensemanager.core.common.utils.fromMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.fromWeekKey
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionWithFilterUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionUiItem
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetBudgetsUseCase(
    private val budgetRepository: BudgetRepository,
    private val getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val getBudgetTransactionsUseCase: GetBudgetTransactionsUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers
) {
    operator fun invoke(): Flow<List<BudgetUiModel>> {
        return combine(
            getCurrencyUseCase.invoke(),
            getTransactionWithFilterUseCase.invoke(),
            budgetRepository.getBudgets(),
        ) { currency, _, budgets ->
            budgets.map { budget ->
                val transactions = when (val response = getBudgetTransactionsUseCase.invoke(budget)) {
                    is Resource.Error -> null
                    is Resource.Success -> response.data.filter { it.type.isExpense() }
                }
                val transactionAmount = transactions?.sumOf { it.amount.amount } ?: 0.0
                val percent = (transactionAmount / budget.amount).toFloat() * 100
                budget.toBudgetUiModel(
                    name = budgetName(budget.selectedMonth, budget.periodType),
                    budgetAmount = getFormattedAmountUseCase(budget.amount, currency),
                    transactionAmount = getFormattedAmountUseCase(transactionAmount, currency),
                    percent,
                    transactions?.map {
                        it.toTransactionUIModel(getFormattedAmountUseCase(it.amount.amount, currency))
                    },
                )
            }
        }.flowOn(appCoroutineDispatchers.computation)
    }
}

private val shortMonthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
private val shortDayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

fun budgetName(selectedMonth: String, periodType: BudgetPeriod = BudgetPeriod.MONTHLY): String {
    return when (periodType) {
        BudgetPeriod.YEARLY -> {
            val currentYear = Date().toYear()
            if (selectedMonth == currentYear) {
                "This Year Budget"
            } else {
                "$selectedMonth Budget"
            }
        }

        BudgetPeriod.MONTHLY -> {
            val currentMonth = Date().toMonthAndYearKey()
            if (selectedMonth == currentMonth) {
                "This Month Budget"
            } else {
                val short = selectedMonth.fromMonthAndYearKey()
                    ?.let { shortMonthFormat.format(it) }
                    ?: selectedMonth
                "$short Budget"
            }
        }

        BudgetPeriod.WEEKLY -> {
            val currentWeek = Date().toWeekKey()
            if (selectedMonth == currentWeek) {
                "This Week Budget"
            } else {
                val short = selectedMonth.fromWeekKey()
                    ?.let { shortDayFormat.format(it) }
                    ?: selectedMonth
                "Week of $short Budget"
            }
        }

        BudgetPeriod.DAILY -> {
            val currentDay = Date().toDayKey()
            if (selectedMonth == currentDay) {
                "Today Budget"
            } else {
                val short = selectedMonth.fromDayKey()
                    ?.let { shortDayFormat.format(it) }
                    ?: selectedMonth
                "$short Budget"
            }
        }
    }
}

/** Shared with `GetBudgetEquivalentsUseCase.progressBarColor` so a budget and its equivalence
 * breakdown always use the same color thresholds for the same percentage. */
fun budgetProgressColor(percent: Float): Int = when {
    percent < 0f -> R.color.green_500
    percent in 0f..35f -> R.color.green_500
    percent in 36f..60f -> R.color.light_green_500
    percent in 61f..85f -> R.color.orange_500
    else -> R.color.red_500
}

fun Budget.toBudgetUiModel(
    name: String,
    budgetAmount: Amount,
    transactionAmount: Amount,
    percent: Float,
    transactions: List<TransactionUiItem>? = null,
    equivalents: List<BudgetEquivalentUiModel>? = null,
) = BudgetUiModel(
    id = this.id,
    name = name,
    selectedMonth = this.selectedMonth,
    periodType = this.periodType,
    progressBarColor = budgetProgressColor(percent),
    amount = budgetAmount,
    transactionAmount = transactionAmount,
    percent = percent,
    transactions = transactions,
    equivalents = equivalents,
)

@Stable
data class BudgetUiModel(
    val id: String,
    val name: String,
    val selectedMonth: String,
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val progressBarColor: Int,
    val amount: Amount,
    val transactionAmount: Amount,
    val percent: Float,
    val transactions: List<TransactionUiItem>? = null,
    /** Only populated by `GetBudgetDetailUseCase` — left null in the list (`GetBudgetsUseCase`)
     * to avoid tripling the transaction queries for every row just to show a list. See
     * `GetBudgetEquivalentsUseCase`. */
    val equivalents: List<BudgetEquivalentUiModel>? = null,
)
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/GetBudgetEquivalentsUseCase.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/GetBudgetEquivalentsUseCase.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.budget

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheDay
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheMonth
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheYear
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheDay
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheMonth
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheYear
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.expandedForCategoryAccounting
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.repository.AccountRepository
import com.naveenapps.expensemanager.core.repository.CategoryRepository
import com.naveenapps.expensemanager.core.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

/**
 * Given an amount entered for one [BudgetPeriod], shows what that pace works out to at the other
 * three granularities *right now* — e.g. entering 5000 for a MONTHLY budget also shows what
 * ~1152/week and ~164/day (and 60000/year) look like, each compared against what's actually been
 * spent so far in *today's* current day/week/month/year. Deliberately anchored to the current
 * period rather than to whatever specific period instance the source budget covers, so a budget
 * for a past year still shows a meaningful "at today's pace" comparison — see the design
 * discussion this was built from.
 *
 * Purely a read-only, on-the-fly comparison: it never creates, updates, or deletes any budget.
 * Call this any time (create screen while typing, or the detail screen) to get a fresh number;
 * nothing here is persisted, so there's no linked budget to keep in sync or clean up.
 */
class GetBudgetEquivalentsUseCase(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
) {
    suspend operator fun invoke(
        amount: Double,
        periodType: BudgetPeriod,
        accounts: List<String>,
        categories: List<String>,
        isAllAccountsSelected: Boolean,
        isAllCategoriesSelected: Boolean,
    ): List<BudgetEquivalentUiModel> {
        if (amount <= 0.0) return emptyList()

        val resolvedAccounts = if (isAllAccountsSelected) {
            accountRepository.getAccounts().firstOrNull()?.map { it.id } ?: emptyList()
        } else {
            accounts
        }

        val resolvedCategories = if (isAllCategoriesSelected) {
            categoryRepository.getCategories().firstOrNull()?.map { it.id } ?: emptyList()
        } else {
            categories
        }

        val currency = getCurrencyUseCase.invoke().first()
        val now = Date()

        return PERIOD_DISPLAY_ORDER
            .filter { it != periodType }
            .map { targetPeriod ->
                val equivalentAmount =
                    amount / daysInPeriod(periodType) * daysInPeriod(targetPeriod)

                val startDate = when (targetPeriod) {
                    BudgetPeriod.YEARLY -> now.getStartOfTheYear()
                    BudgetPeriod.MONTHLY -> now.getStartOfTheMonth()
                    BudgetPeriod.WEEKLY -> now.getStartOfTheWeek()
                    BudgetPeriod.DAILY -> now.getStartOfTheDay()
                }
                val endDate = when (targetPeriod) {
                    BudgetPeriod.YEARLY -> now.getEndOfTheYear()
                    BudgetPeriod.MONTHLY -> now.getEndOfTheMonth()
                    BudgetPeriod.WEEKLY -> now.getEndOfTheWeek()
                    BudgetPeriod.DAILY -> now.getEndOfTheDay()
                }

                val spent = transactionRepository.getFilteredTransaction(
                    accounts = resolvedAccounts,
                    categories = resolvedCategories,
                    transactionType = TransactionType.entries.map { it.ordinal },
                    startDate = startDate,
                    endDate = endDate,
                ).firstOrNull()
                    ?.expandedForCategoryAccounting()
                    ?.filter { it.type.isExpense() }
                    ?.sumOf { it.amount.amount }
                    ?: 0.0

                val percent = if (equivalentAmount > 0.0) {
                    (spent / equivalentAmount).toFloat() * 100
                } else {
                    0f
                }

                BudgetEquivalentUiModel(
                    periodType = targetPeriod,
                    equivalentAmount = getFormattedAmountUseCase(equivalentAmount, currency),
                    spentAmount = getFormattedAmountUseCase(spent, currency),
                    percent = percent,
                    progressBarColor = budgetProgressColor(percent),
                )
            }
    }

    companion object {
        /** Average day counts, not calendar-exact (a month isn't always 30.4375 days) — picked
         * so a conversion gives the same numbers regardless of which period you start from. */
        private fun daysInPeriod(periodType: BudgetPeriod): Double = when (periodType) {
            BudgetPeriod.DAILY -> 1.0
            BudgetPeriod.WEEKLY -> 7.0
            BudgetPeriod.MONTHLY -> 365.25 / 12
            BudgetPeriod.YEARLY -> 365.25
        }

        private val PERIOD_DISPLAY_ORDER = listOf(
            BudgetPeriod.YEARLY,
            BudgetPeriod.MONTHLY,
            BudgetPeriod.WEEKLY,
            BudgetPeriod.DAILY,
        )
    }
}

@Stable
data class BudgetEquivalentUiModel(
    val periodType: BudgetPeriod,
    val equivalentAmount: Amount,
    val spentAmount: Amount,
    val percent: Float,
    val progressBarColor: Int,
)
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/GetBudgetDetailUseCase.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/GetBudgetDetailUseCase.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.budget

import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionWithFilterUseCase
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class GetBudgetDetailUseCase(
    private val budgetRepository: BudgetRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val getBudgetTransactionsUseCase: GetBudgetTransactionsUseCase,
    private val getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase,
    private val getBudgetEquivalentsUseCase: GetBudgetEquivalentsUseCase,
) {
    operator fun invoke(budgetId: String): Flow<BudgetUiModel?> {
        return combine(
            budgetRepository.findBudgetByIdFlow(budgetId),
            getTransactionWithFilterUseCase.invoke(),
        ) { budget, _ ->
            budget?.let {
                val currency = getCurrencyUseCase.invoke().first()
                val budgetTransactions =
                    when (val transaction = getBudgetTransactionsUseCase.invoke(budget)) {
                        is Resource.Error -> {
                            null
                        }

                        is Resource.Success -> {
                            transaction.data.filter {
                                it.type.isExpense()
                            }
                        }
                    }
                val transactionAmount = budgetTransactions?.sumOf { it.amount.amount } ?: 0.0
                val percent = (transactionAmount / budget.amount).toFloat() * 100
                val equivalents = getBudgetEquivalentsUseCase.invoke(
                    amount = budget.amount,
                    periodType = budget.periodType,
                    accounts = budget.accounts,
                    categories = budget.categories,
                    isAllAccountsSelected = budget.isAllAccountsSelected,
                    isAllCategoriesSelected = budget.isAllCategoriesSelected,
                )
                budget.toBudgetUiModel(
                    name = budgetName(budget.selectedMonth, budget.periodType),
                    budgetAmount = getFormattedAmountUseCase(budget.amount, currency),
                    transactionAmount = getFormattedAmountUseCase(transactionAmount, currency),
                    percent,
                    budgetTransactions?.map {
                        it.toTransactionUIModel(
                            getFormattedAmountUseCase(it.amount.amount, currency),
                        )
                    },
                    equivalents,
                )
            }
        }
    }
}
CLAUDE_FIX_EOF

echo "  core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/BudgetUseCaseModule.kt"
mkdir -p "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget"
cat > "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/budget/BudgetUseCaseModule.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.domain.usecase.budget

import org.koin.dsl.module

val BudgetUseCaseModule = module {
    single {
        AddBudgetUseCase(
            repository = get(),
            checkBudgetValidateUseCase = get()
        )
    }
    single { CheckBudgetValidateUseCase() }
    single {
        DeleteBudgetUseCase(
            repository = get(),
            checkBudgetValidateUseCase = get()
        )
    }
    single { FindBudgetByIdUseCase(repository = get()) }
    single {
        GetBudgetDetailUseCase(
            budgetRepository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            getBudgetTransactionsUseCase = get(),
            getTransactionWithFilterUseCase = get(),
            getBudgetEquivalentsUseCase = get(),
        )
    }
    single {
        GetBudgetEquivalentsUseCase(
            categoryRepository = get(),
            accountRepository = get(),
            transactionRepository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
        )
    }
    single {
        GetBudgetTransactionsUseCase(
            categoryRepository = get(),
            accountRepository = get(),
            transactionRepository = get()
        )
    }
    single {
        GetBudgetsUseCase(
            budgetRepository = get(),
            getTransactionWithFilterUseCase = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            getBudgetTransactionsUseCase = get(),
            appCoroutineDispatchers = get()
        )
    }
    single {
        UpdateBudgetUseCase(
            repository = get(),
            checkBudgetValidateUseCase = get()
        )
    }
}

CLAUDE_FIX_EOF

echo "  core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetAlertWorker.kt"
mkdir -p "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget"
cat > "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetAlertWorker.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.notification.budget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetUiModel
import com.naveenapps.expensemanager.core.domain.usecase.budget.GetBudgetsUseCase
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.notification.R
import kotlinx.coroutines.flow.first
import java.util.Date

/** Same fixed-id grouped-notification reasoning as `DebtOverdueWorker` — one summary
 * notification, not one per budget. */
private val BUDGET_ALERT_NOTIFICATION_REQUEST_CODE = "budget_alert_summary".hashCode()

/** A budget is only ever considered "close" or "exceeded" for its *current* period — a budget
 * created for a past day/week/month/year keeps whatever final percentage it ended on, and
 * re-alerting on that every day forever would be noise, not a warning. */
private fun BudgetUiModel.isCurrentPeriod(): Boolean {
    val now = Date()
    return when (periodType) {
        BudgetPeriod.YEARLY -> selectedMonth == now.toYear()
        BudgetPeriod.MONTHLY -> selectedMonth == now.toMonthAndYearKey()
        BudgetPeriod.WEEKLY -> selectedMonth == now.toWeekKey()
        BudgetPeriod.DAILY -> selectedMonth == now.toDayKey()
    }
}

private const val WARNING_THRESHOLD_PERCENT = 80f
private const val EXCEEDED_THRESHOLD_PERCENT = 100f

/**
 * Daily check (see `BudgetAlertScheduler`) — not triggered right when a transaction is added, to
 * keep this a simple, self-contained addition on top of the existing `GetBudgetsUseCase` percent
 * calculation rather than threading a new side effect through `AddTransactionUseCase`. Reminds
 * daily for as long as a budget stays over a threshold, same behavior as `DebtOverdueWorker` for
 * an unsettled debt — a budget that's still over 100% tomorrow is still worth mentioning.
 *
 * If any budget has exceeded 100%, that takes priority over ones merely approaching 80% — only
 * one notification is shown per day, so this avoids ever combining two different messages into
 * one string.
 */
class BudgetAlertWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val getBudgetsUseCase: GetBudgetsUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val currentBudgets = getBudgetsUseCase.invoke().first().filter { it.isCurrentPeriod() }

        val exceeded = currentBudgets.filter { it.percent >= EXCEEDED_THRESHOLD_PERCENT }
        val approaching = currentBudgets.filter {
            it.percent in WARNING_THRESHOLD_PERCENT until EXCEEDED_THRESHOLD_PERCENT
        }

        val (title, content) = when {
            exceeded.isNotEmpty() -> {
                applicationContext.getString(R.string.budget_exceeded_title) to
                    if (exceeded.size == 1) {
                        applicationContext.getString(
                            R.string.budget_exceeded_content_one,
                            exceeded.first().name,
                        )
                    } else {
                        applicationContext.getString(
                            R.string.budget_exceeded_content_other,
                            exceeded.size,
                        )
                    }
            }

            approaching.isNotEmpty() -> {
                applicationContext.getString(R.string.budget_approaching_title) to
                    if (approaching.size == 1) {
                        applicationContext.getString(
                            R.string.budget_approaching_content_one,
                            approaching.first().name,
                        )
                    } else {
                        applicationContext.getString(
                            R.string.budget_approaching_content_other,
                            approaching.size,
                        )
                    }
            }

            else -> return Result.success()
        }

        showBudgetNotification(
            context = applicationContext,
            requestCode = BUDGET_ALERT_NOTIFICATION_REQUEST_CODE,
            title = title,
            content = content,
        )

        return Result.success()
    }
}
CLAUDE_FIX_EOF

echo "  feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/create/BudgetCreateState.kt"
mkdir -p "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/create"
cat > "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/create/BudgetCreateState.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.feature.budget.create

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetEquivalentUiModel
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.TextFieldValue
import java.util.Date

@Stable
data class BudgetCreateState(
    val isLoading: Boolean,
    val amount: TextFieldValue<String>,
    val month: TextFieldValue<Date>,
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val isAllAccountSelected: Boolean,
    val selectedAccounts: List<AccountUiModel>,
    val isAllCategorySelected: Boolean,
    val selectedCategories: List<Category>,
    val currency: Currency,
    val showDeleteButton: Boolean,
    val showDeleteDialog: Boolean,
    val showAccountSelectionDialog: Boolean,
    val showCategorySelectionDialog: Boolean,
    val showMonthSelection: Boolean,
    /** Read-only "at this pace" comparison against the other 3 periods — see
     * GetBudgetEquivalentsUseCase. Recomputed live as amount/period/accounts/categories change. */
    val equivalents: List<BudgetEquivalentUiModel> = emptyList(),
)
CLAUDE_FIX_EOF

echo "  feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/create/BudgetCreateViewModel.kt"
mkdir -p "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/create"
cat > "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/create/BudgetCreateViewModel.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.feature.budget.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.common.utils.fromDayKey
import com.naveenapps.expensemanager.core.common.utils.fromMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.fromWeekKey
import com.naveenapps.expensemanager.core.common.utils.fromYear
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.account.FindAccountByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.AddBudgetUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.DeleteBudgetUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.FindBudgetByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.GetBudgetEquivalentsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.UpdateBudgetUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.FindCategoryByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Budget
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.toAccountUiModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerArgsNames
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID


class BudgetCreateViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    private val findBudgetByIdUseCase: FindBudgetByIdUseCase,
    private val findAccountByIdUseCase: FindAccountByIdUseCase,
    private val findCategoryByIdUseCase: FindCategoryByIdUseCase,
    private val addBudgetUseCase: AddBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getBudgetEquivalentsUseCase: GetBudgetEquivalentsUseCase,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BudgetCreateState(
            isLoading = true,
            amount = TextFieldValue(
                value = "",
                valueError = false,
                onValueChange = this::setAmountChange
            ),
            month = TextFieldValue(
                value = Date(),
                valueError = false,
                onValueChange = this::setDateChange
            ),
            periodType = BudgetPeriod.MONTHLY,
            currency = getDefaultCurrencyUseCase.invoke(),
            isAllAccountSelected = true,
            selectedAccounts = emptyList(),
            isAllCategorySelected = true,
            selectedCategories = emptyList(),
            showDeleteButton = false,
            showDeleteDialog = false,
            showAccountSelectionDialog = false,
            showCategorySelectionDialog = false,
            showMonthSelection = false
        )
    )
    val state = _state.asStateFlow()

    private var budget: Budget? = null
    private var equivalentsJob: Job? = null

    init {
        getCurrencyUseCase.invoke().onEach { updatedCurrency ->
            _state.update {
                it.copy(currency = updatedCurrency)
            }
        }.launchIn(viewModelScope)

        readBudgetInfo(savedStateHandle.get<String>(ExpenseManagerArgsNames.ID))
    }

    private suspend fun updateBudgetInfo(budget: Budget) {
        this.budget = budget

        val accounts = budget.accounts.map {
            return@map when (val response = findAccountByIdUseCase.invoke(it)) {
                is Resource.Error -> null
                is Resource.Success -> {
                    val data = response.data
                    data.toAccountUiModel(
                        Amount(data.amount, currency = _state.value.currency),
                    )
                }
            }
        }.filterNotNull()

        if (accounts.isNotEmpty()) {
            setAccounts(accounts, budget.isAllAccountsSelected)
        }

        val categories = budget.categories.map {
            return@map when (val response = findCategoryByIdUseCase.invoke(it)) {
                is Resource.Error -> null
                is Resource.Success -> response.data
            }
        }.filterNotNull()

        if (categories.isNotEmpty()) {
            setCategories(categories, budget.isAllAccountsSelected)
        }

        val loadedDate = when (budget.periodType) {
            BudgetPeriod.YEARLY -> budget.selectedMonth.fromYear()
            BudgetPeriod.MONTHLY -> budget.selectedMonth.fromMonthAndYearKey()
            BudgetPeriod.WEEKLY -> budget.selectedMonth.fromWeekKey()
            BudgetPeriod.DAILY -> budget.selectedMonth.fromDayKey()
        } ?: Date()

        _state.update { state ->
            state.copy(
                isLoading = false,
                amount = state.amount.copy(value = numberFormatRepository.formatForEditing(budget.amount)),
                month = state.month.copy(value = loadedDate),
                periodType = budget.periodType,
                isAllAccountSelected = budget.isAllAccountsSelected,
                selectedAccounts = emptyList(),
                isAllCategorySelected = budget.isAllCategoriesSelected,
                selectedCategories = emptyList(),
                showDeleteButton = true,
            )
        }
        recomputeEquivalents()
    }

    private fun readBudgetInfo(budgetId: String?) {
        budgetId ?: return
        viewModelScope.launch {
            when (val response = findBudgetByIdUseCase.invoke(budgetId)) {
                is Resource.Error -> Unit
                is Resource.Success -> {
                    updateBudgetInfo(response.data)
                }
            }
        }
    }

    private fun deleteBudget() {
        viewModelScope.launch {
            budget?.let { budget ->
                when (deleteBudgetUseCase.invoke(budget)) {
                    is Resource.Error -> Unit
                    is Resource.Success -> {
                        closePage()
                    }
                }
            }
        }
    }

    private fun saveOrUpdateBudget() {
        val date: Date = _state.value.month.value
        val amount: Double? = numberFormatRepository.parseToDouble(_state.value.amount.value)

        var isError = false

        if (amount == null || amount == 0.0) {
            _state.update { it.copy(amount = it.amount.copy(valueError = true)) }
            isError = true
        }

        if (isError) {
            return
        }

        val categories = _state.value.selectedCategories.map { it.id }

        val accounts = _state.value.selectedAccounts.map { it.id }

        val periodType = _state.value.periodType
        val budget = Budget(
            id = budget?.id ?: UUID.randomUUID().toString(),
            amount = amount ?: 0.0,
            selectedMonth = when (periodType) {
                BudgetPeriod.YEARLY -> date.toYear()
                BudgetPeriod.MONTHLY -> date.toMonthAndYearKey()
                BudgetPeriod.WEEKLY -> date.toWeekKey()
                BudgetPeriod.DAILY -> date.toDayKey()
            },
            periodType = periodType,
            categories = categories,
            accounts = accounts,
            isAllCategoriesSelected = _state.value.isAllCategorySelected,
            isAllAccountsSelected = _state.value.isAllAccountSelected,
            createdOn = Calendar.getInstance().time,
            updatedOn = Calendar.getInstance().time,
        )

        viewModelScope.launch {
            val response = if (this@BudgetCreateViewModel.budget != null) {
                updateBudgetUseCase(budget)
            } else {
                addBudgetUseCase(budget)
            }
            when (response) {
                is Resource.Error -> Unit
                is Resource.Success -> {
                    closePage()
                }
            }
        }
    }

    private fun setAmountChange(amount: String) {
        val amountValue = numberFormatRepository.parseToDouble(amount)
        _state.update {
            it.copy(
                amount = it.amount.copy(
                    value = amount,
                    valueError = amountValue == null || amountValue == 0.0
                )
            )
        }
        recomputeEquivalents()
    }

    private fun setDateChange(date: Date) {
        _state.update {
            it.copy(
                month = it.month.copy(value = date),
                showMonthSelection = false
            )
        }
    }

    private fun setPeriodType(periodType: BudgetPeriod) {
        _state.update { it.copy(periodType = periodType) }
        recomputeEquivalents()
    }

    private fun setAccounts(selectedAccounts: List<AccountUiModel>, isAllSelected: Boolean) {
        _state.update {
            it.copy(
                isAllAccountSelected = isAllSelected,
                selectedAccounts = selectedAccounts,
                showAccountSelectionDialog = false
            )
        }
        recomputeEquivalents()
    }

    private fun setCategories(selectedCategories: List<Category>, isAllSelected: Boolean) {
        _state.update {
            it.copy(
                isAllCategorySelected = isAllSelected,
                selectedCategories = selectedCategories,
                showCategorySelectionDialog = false
            )
        }
        recomputeEquivalents()
    }

    /** "Latest wins" — cancels any in-flight computation before starting a new one, so rapid
     * typing doesn't race and land an older amount's result after a newer one. */
    private fun recomputeEquivalents() {
        val state = _state.value
        val amount = numberFormatRepository.parseToDouble(state.amount.value)
        if (amount == null || amount <= 0.0) {
            _state.update { it.copy(equivalents = emptyList()) }
            return
        }
        equivalentsJob?.cancel()
        equivalentsJob = viewModelScope.launch {
            val equivalents = getBudgetEquivalentsUseCase.invoke(
                amount = amount,
                periodType = state.periodType,
                accounts = state.selectedAccounts.map { it.id },
                categories = state.selectedCategories.map { it.id },
                isAllAccountsSelected = state.isAllAccountSelected,
                isAllCategoriesSelected = state.isAllCategorySelected,
            )
            _state.update { it.copy(equivalents = equivalents) }
        }
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    private fun closeAccountSelection() {
        _state.update {
            it.copy(
                showAccountSelectionDialog = false
            )
        }
    }

    private fun openAccountSelection() {
        _state.update {
            it.copy(
                showAccountSelectionDialog = true
            )
        }
    }

    private fun closeCategorySelection() {
        _state.update {
            it.copy(
                showCategorySelectionDialog = false,
            )
        }
    }

    private fun openCategorySelection() {
        _state.update {
            it.copy(
                showCategorySelectionDialog = true,
            )
        }
    }

    private fun closeDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    private fun openDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    private fun closeMonthSelection() {
        _state.update { it.copy(showMonthSelection = false) }
    }

    private fun openMonthSelection() {
        _state.update { it.copy(showMonthSelection = true) }
    }

    fun processAction(action: BudgetCreateAction) {
        when (action) {
            BudgetCreateAction.ClosePage -> closePage()
            is BudgetCreateAction.SelectPeriodType -> setPeriodType(action.periodType)
            BudgetCreateAction.OpenAccountSelectionDialog -> openAccountSelection()
            BudgetCreateAction.CloseAccountSelectionDialog -> closeAccountSelection()
            BudgetCreateAction.OpenCategorySelectionDialog -> openCategorySelection()
            BudgetCreateAction.CloseCategorySelectionDialog -> closeCategorySelection()

            BudgetCreateAction.CloseDeleteDialog -> closeDeleteDialog()
            BudgetCreateAction.ShowDeleteDialog -> openDeleteDialog()
            BudgetCreateAction.Save -> saveOrUpdateBudget()
            BudgetCreateAction.Delete -> deleteBudget()
            is BudgetCreateAction.SelectAccounts -> setAccounts(
                action.accounts,
                action.isAllSelected
            )

            is BudgetCreateAction.SelectCategories -> setCategories(
                action.categories,
                action.isAllSelected
            )

            BudgetCreateAction.CloseMonthSelection -> closeMonthSelection()
            BudgetCreateAction.ShowMonthSelection -> openMonthSelection()
        }
    }

    companion object {
        private const val DEFAULT_COLOR = "#43A546"
        private const val DEFAULT_ICON = "account_balance"
    }
}
CLAUDE_FIX_EOF

echo "  feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/create/BudgetCreateScreen.kt"
mkdir -p "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/create"
cat > "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/create/BudgetCreateScreen.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.feature.budget.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.common.utils.fromShortMonthAndYearToDate
import com.naveenapps.expensemanager.core.common.utils.fromYear
import com.naveenapps.expensemanager.core.common.utils.getEndOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.getStartOfTheWeek
import com.naveenapps.expensemanager.core.common.utils.toCompleteDateWithDate
import com.naveenapps.expensemanager.core.common.utils.toCompleteDate
import com.naveenapps.expensemanager.core.common.utils.toMonth
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYear
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.common.utils.toYearInt
import com.naveenapps.expensemanager.core.designsystem.components.DeleteDialogItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppDatePickerDialog
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppFilterChip
import com.naveenapps.expensemanager.core.designsystem.ui.components.ClickableTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.DecimalTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.MonthPicker
import com.naveenapps.expensemanager.core.designsystem.ui.components.SafeModalBottomSheet
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingRow
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.designsystem.ui.components.YearPicker
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetEquivalentUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.feature.account.selection.MultipleAccountSelectionScreen
import com.naveenapps.expensemanager.feature.budget.R
import com.naveenapps.expensemanager.feature.budget.list.BudgetItem
import com.naveenapps.expensemanager.feature.budget.periodLabelRes
import com.naveenapps.expensemanager.feature.category.selection.MultipleCategoriesSelectionScreen
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date

@Composable
fun BudgetCreateScreen(
    viewModel: BudgetCreateViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsState()

    BudgetCreateScreenContentView(
        state = state,
        onAction = viewModel::processAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetCreateScreenContentView(
    state: BudgetCreateState,
    onAction: (BudgetCreateAction) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    if (state.showDeleteDialog) {
        DeleteDialogItem(
            confirm = {
                onAction.invoke(BudgetCreateAction.Delete)
            },
            dismiss = {
                onAction.invoke(BudgetCreateAction.ClosePage)
            },
        )
    }

    if (state.showAccountSelectionDialog) {
        SafeModalBottomSheet(
            onDismissRequest = {
                onAction.invoke(BudgetCreateAction.CloseAccountSelectionDialog)
            },
        ) {
            MultipleAccountSelectionScreen(
                selectedAccounts = state.selectedAccounts,
            ) { items, selected ->
                onAction.invoke(BudgetCreateAction.SelectAccounts(selected, items))
            }
        }
    }

    if (state.showCategorySelectionDialog) {
        SafeModalBottomSheet(
            onDismissRequest = {
                onAction.invoke(BudgetCreateAction.CloseCategorySelectionDialog)
            },
        ) {
            MultipleCategoriesSelectionScreen(
                selectedCategories = state.selectedCategories,
                onItemSelection = { items, selected ->
                    onAction.invoke(BudgetCreateAction.SelectCategories(selected, items))
                }
            )
        }
    }

    if (state.showMonthSelection) {
        if (state.periodType == BudgetPeriod.YEARLY) {
            YearPicker(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                    ),
                currentYear = state.month.value.toYearInt(),
                confirmButtonCLicked = { year ->
                    year.toString().fromYear()?.let {
                        state.month.onValueChange?.invoke(it)
                    } ?: run {
                        onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                    }
                },
                cancelClicked = {
                    onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                },
            )
        } else if (state.periodType == BudgetPeriod.MONTHLY) {
            MonthPicker(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                    ),
                currentMonth = state.month.value.toMonth(),
                currentYear = state.month.value.toYearInt(),
                confirmButtonCLicked = { month, year ->
                    ("$month-$year").fromShortMonthAndYearToDate()?.let {
                        state.month.onValueChange?.invoke(it)
                    } ?: run {
                        onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                    }
                },
                cancelClicked = {
                    onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                },
            )
        } else {
            // WEEKLY and DAILY both pick a single calendar date — a week budget's key is
            // derived from whichever Monday contains the picked date (see Date.toWeekKey), so
            // picking any day within the intended week is enough.
            AppDatePickerDialog(
                selectedDate = state.month.value,
                onDateSelected = {
                    state.month.onValueChange?.invoke(it)
                },
                onDismiss = {
                    onAction.invoke(BudgetCreateAction.CloseMonthSelection)
                },
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = {
                    onAction.invoke(BudgetCreateAction.ClosePage)
                },
                title = if (state.showDeleteButton)
                    stringResource(R.string.edit_budget)
                else
                    stringResource(R.string.create_budget),
                actions = {
                    if (state.showDeleteButton) {
                        IconButton(onClick = { onAction.invoke(BudgetCreateAction.ShowDeleteDialog) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(com.naveenapps.expensemanager.feature.category.R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction.invoke(BudgetCreateAction.Save) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                    )
                },
                text = {
                    Text(text = stringResource(com.naveenapps.expensemanager.feature.account.R.string.save))
                },
            )
        },
    ) { innerPadding ->
        BudgetCreateScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            amountField = state.amount,
            currencyIconField = state.currency.symbol,
            selectedDate = state.month,
            periodType = state.periodType,
            accountCount = if (state.isAllAccountSelected) {
                stringResource(R.string.all)
            } else {
                state.selectedAccounts.size.toString()
            },
            categoriesCount = if (state.isAllCategorySelected) {
                stringResource(R.string.all)
            } else {
                state.selectedCategories.size.toString()
            },
            equivalents = state.equivalents,
            onAction = onAction,
        )
    }
}

@Composable
fun BudgetCreateScreen(
    amountField: TextFieldValue<String>,
    currencyIconField: String,
    selectedDate: TextFieldValue<Date>,
    accountCount: String,
    categoriesCount: String,
    onAction: (BudgetCreateAction) -> Unit,
    modifier: Modifier = Modifier,
    periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    equivalents: List<BudgetEquivalentUiModel> = emptyList(),
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(
            title = stringResource(R.string.budget_for),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            AppCardView {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppFilterChip(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f),
                            centerAlign = true,
                            filterName = stringResource(id = R.string.monthly),
                            isSelected = periodType == BudgetPeriod.MONTHLY,
                            onClick = {
                                onAction.invoke(
                                    BudgetCreateAction.SelectPeriodType(BudgetPeriod.MONTHLY),
                                )
                            },
                        )
                        AppFilterChip(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f),
                            centerAlign = true,
                            filterName = stringResource(id = R.string.annual),
                            isSelected = periodType == BudgetPeriod.YEARLY,
                            onClick = {
                                onAction.invoke(
                                    BudgetCreateAction.SelectPeriodType(BudgetPeriod.YEARLY),
                                )
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppFilterChip(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f),
                            centerAlign = true,
                            filterName = stringResource(id = R.string.weekly),
                            isSelected = periodType == BudgetPeriod.WEEKLY,
                            onClick = {
                                onAction.invoke(
                                    BudgetCreateAction.SelectPeriodType(BudgetPeriod.WEEKLY),
                                )
                            },
                        )
                        AppFilterChip(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f),
                            centerAlign = true,
                            filterName = stringResource(id = R.string.daily),
                            isSelected = periodType == BudgetPeriod.DAILY,
                            onClick = {
                                onAction.invoke(
                                    BudgetCreateAction.SelectPeriodType(BudgetPeriod.DAILY),
                                )
                            },
                        )
                    }
                    ClickableTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = when (periodType) {
                            BudgetPeriod.YEARLY -> selectedDate.value.toYear()
                            BudgetPeriod.MONTHLY -> selectedDate.value.toMonthAndYear()
                            BudgetPeriod.WEEKLY -> {
                                val start = selectedDate.value.getStartOfTheWeek().toCompleteDate()
                                // getEndOfTheWeek is exclusive (start of the *next* week), so the
                                // displayed range shows the actual last millisecond-inclusive day.
                                val end = (selectedDate.value.getEndOfTheWeek() - 1).toCompleteDate()
                                "${start.toCompleteDateWithDate()} - ${end.toCompleteDateWithDate()}"
                            }
                            BudgetPeriod.DAILY -> selectedDate.value.toCompleteDateWithDate()
                        },
                        label = when (periodType) {
                            BudgetPeriod.YEARLY -> R.string.select_year
                            BudgetPeriod.MONTHLY -> R.string.select_month
                            BudgetPeriod.WEEKLY -> R.string.select_week
                            BudgetPeriod.DAILY -> R.string.select_day
                        },
                        leadingIcon = Icons.Default.EditCalendar,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onAction.invoke(BudgetCreateAction.ShowMonthSelection)
                        },
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.what_is_your_budget_limit)) {
            AppCardView {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DecimalTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = amountField.value,
                        isError = amountField.valueError,
                        errorMessage = stringResource(id = R.string.budget_amount_error),
                        onValueChange = amountField.onValueChange,
                        label = R.string.budget_amount,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }
            }
        }

        if (equivalents.isNotEmpty()) {
            SettingsSection(title = stringResource(R.string.budget_equivalents)) {
                AppCardView {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        equivalents.forEach { equivalent ->
                            BudgetItem(
                                name = stringResource(id = periodLabelRes(equivalent.periodType)),
                                progressBarColor = equivalent.progressBarColor,
                                amount = equivalent.equivalentAmount,
                                transactionAmount = equivalent.spentAmount,
                                percentage = equivalent.percent,
                            )
                        }
                    }
                }
            }
        }

        SettingsSection(title = stringResource(R.string.budget_scope)) {
            AppCardView {
                Column {
                    SettingRow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(id = R.string.select_account),
                        icon = Icons.Default.AccountBalance,
                        value = accountCount,
                        onClick = {
                            onAction.invoke(BudgetCreateAction.OpenAccountSelectionDialog)
                        },
                        showDivider = true
                    )
                    SettingRow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(id = R.string.select_category),
                        icon = Icons.Default.Category,
                        value = categoriesCount,
                        onClick = {
                            onAction.invoke(BudgetCreateAction.OpenCategorySelectionDialog)
                        }
                    )
                }
            }
        }


        // FAB clearance
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@AppPreviewsLightAndDarkMode
@Composable
private fun BudgetCreateStatePreview() {
    val amountField = TextFieldValue(
        value = "0.0",
        valueError = false,
        onValueChange = { }
    )
    val dateField = TextFieldValue(
        value = Date(),
        valueError = false,
        onValueChange = { }
    )
    NaveenAppsPreviewTheme(padding = 0.dp) {
        BudgetCreateScreenContentView(
            state = BudgetCreateState(
                isLoading = false,
                amount = amountField,
                month = dateField,
                isAllCategorySelected = true,
                isAllAccountSelected = true,
                currency = Currency(symbol = "$", name = ""),
                showDeleteDialog = false,
                showDeleteButton = true,
                showAccountSelectionDialog = false,
                showCategorySelectionDialog = false,
                selectedCategories = emptyList(),
                selectedAccounts = emptyList(),
                showMonthSelection = false,
            ),
            onAction = {},
        )
    }
}
CLAUDE_FIX_EOF

echo "  feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/details/BudgetDetailScreen.kt"
mkdir -p "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/details"
cat > "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/details/BudgetDetailScreen.kt" << 'CLAUDE_FIX_EOF'
@file:OptIn(ExperimentalMaterial3Api::class)

package com.naveenapps.expensemanager.feature.budget.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.designsystem.components.EmptyItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardViewDefaults
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.TransactionUiItem
import com.naveenapps.expensemanager.feature.budget.R
import com.naveenapps.expensemanager.feature.budget.list.BudgetItem
import com.naveenapps.expensemanager.feature.budget.periodLabelRes
import com.naveenapps.expensemanager.feature.transaction.list.TransactionItem
import com.naveenapps.expensemanager.feature.transaction.list.getTransactionItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BudgetDetailScreen(
    viewModel: BudgetDetailViewModel = koinViewModel(),
) {

    val budget by viewModel.budget.collectAsState()

    BudgetDetailsScaffoldView(
        budget = budget,
        openBudgetEditScreen = viewModel::openBudgetCreateScreen,
        closePage = viewModel::closePage,
        openTransactionCreateScreen = viewModel::openTransactionCreateScreen
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BudgetDetailsScaffoldView(
    budget: BudgetUiModel?,
    openBudgetEditScreen: () -> Unit,
    closePage: () -> Unit,
    openTransactionCreateScreen: (String?) -> Unit,
) {

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = closePage) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null
                                )
                            }
                        },
                        title = {
                            Text(stringResource(R.string.budgets))
                        },
                        actions = {
                            budget?.let {
                                IconButton(onClick = openBudgetEditScreen) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "",
                                    )
                                }
                            }
                        }
                    )
                    budget?.let {
                        BudgetItem(
                            name = budget.name,
                            progressBarColor = budget.progressBarColor,
                            amount = budget.amount,
                            transactionAmount = budget.transactionAmount,
                            percentage = budget.percent,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { openTransactionCreateScreen.invoke(null) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "",
                )
            }
        },
    ) { innerPadding ->
        BudgetDetailContent(
            budget = budget,
            onItemClick = openTransactionCreateScreen,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}

@Composable
private fun BudgetDetailContent(
    budget: BudgetUiModel?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions = budget?.transactions
    val equivalents = budget?.equivalents
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 78.dp,
            top = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (!equivalents.isNullOrEmpty()) {
            item(key = "budget_equivalents") {
                SettingsSection(title = stringResource(R.string.budget_equivalents)) {
                    AppCardView {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            equivalents.forEach { equivalent ->
                                BudgetItem(
                                    name = stringResource(id = periodLabelRes(equivalent.periodType)),
                                    progressBarColor = equivalent.progressBarColor,
                                    amount = equivalent.equivalentAmount,
                                    transactionAmount = equivalent.spentAmount,
                                    percentage = equivalent.percent,
                                )
                            }
                        }
                    }
                }
            }
            item(key = "budget_equivalents_spacer") {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        if (transactions?.isNotEmpty() == true) {
            itemsIndexed(
                items = transactions,
                key = { _, item -> item.id },
            ) { index, item ->
                AppCardView(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = AppCardViewDefaults.cardShape(index, transactions),
                ) {
                    TransactionItem(
                        modifier = Modifier
                            .fillMaxWidth(),
                        categoryName = item.categoryName,
                        categoryColor = item.categoryIcon.backgroundColor,
                        categoryIcon = item.categoryIcon.name,
                        amount = item.amount,
                        date = item.date,
                        notes = item.notes,
                        transactionType = item.transactionType,
                        fromAccountName = item.fromAccountName,
                        fromAccountIcon = item.fromAccountIcon.name,
                        fromAccountColor = item.fromAccountIcon.backgroundColor,
                        toAccountName = item.toAccountName,
                        toAccountIcon = item.toAccountIcon?.name,
                        toAccountColor = item.toAccountIcon?.backgroundColor,
                        onEdit = {
                            onItemClick.invoke(item.id)
                        }
                    )
                }
            }
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                )
            }
        } else {
            item(key = "no_transactions") {
                EmptyItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(),
                    emptyItemText = stringResource(id = com.naveenapps.expensemanager.feature.category.R.string.no_transactions_available),
                    icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_transaction,
                )
            }
        }
    }
}

@AppPreviewsLightAndDarkMode
@Composable
fun BudgetDetailsScaffoldViewPreview() {
    NaveenAppsPreviewTheme(padding = 0.dp) {
        BudgetDetailsScaffoldView(
            budget = BudgetUiModel(
                id = "sample",
                name = "Jun 2026 Budget",
                selectedMonth = "June 2026",
                progressBarColor = com.naveenapps.expensemanager.core.common.R.color.green_500,
                amount = Amount(100.0, "$100.00"),
                transactionAmount = Amount(100.0, "$100.00"),
                percent = 50.0f,
                transactions = getTransactionItems()
            ),
            openBudgetEditScreen = {},
            closePage = {},
            openTransactionCreateScreen = {},
        )
    }
}

private fun getTransactionItems(total: Int = 10): List<TransactionUiItem> {
    return buildList {
        repeat(total) {
            add(getTransactionItem(it.toString()))
        }
    }
}
CLAUDE_FIX_EOF

echo "  feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/BudgetPeriodDisplay.kt"
mkdir -p "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget"
cat > "feature/budget/src/main/kotlin/com/naveenapps/expensemanager/feature/budget/BudgetPeriodDisplay.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.feature.budget

import androidx.annotation.StringRes
import com.naveenapps.expensemanager.core.model.BudgetPeriod

/** Shared between the create and detail screens so both equivalence cards label each row the
 * same way. */
@StringRes
fun periodLabelRes(periodType: BudgetPeriod): Int = when (periodType) {
    BudgetPeriod.YEARLY -> R.string.budget_equivalent_yearly
    BudgetPeriod.MONTHLY -> R.string.budget_equivalent_monthly
    BudgetPeriod.WEEKLY -> R.string.budget_equivalent_weekly
    BudgetPeriod.DAILY -> R.string.budget_equivalent_daily
}
CLAUDE_FIX_EOF

echo "  feature/budget/src/main/res/values/strings.xml"
mkdir -p "feature/budget/src/main/res/values"
cat > "feature/budget/src/main/res/values/strings.xml" << 'CLAUDE_FIX_EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="budgets">Budgets</string>
    <string name="active_budgets">Active Budgets</string>
    <string name="no_budget_available">No budgets available\nTap + button to create new budget</string>
    <string name="no_budget_available_short">No budgets available</string>
    <string name="budget_name">Budget Name</string>
    <string name="budget_delete_error_message">Budget update failed</string>
    <string name="budget_name_error">Name shouldn\'t be empty</string>
    <string name="budget_create_error">Unable to create/update the budget</string>
    <string name="budget_create_success">Budget created/updated successfully</string>
    <string name="budget_amount">Budget Amount</string>
    <string name="budget_amount_error">Budget amount shouldn\'t be empty or zero</string>

    <string name="select_date">Select Date</string>
    <string name="select_account">Select Account</string>
    <string name="clear_all">Clear All</string>
    <string name="select">Select</string>
    <string name="select_category">Select Category</string>

    <string name="all">All</string>

    <string name="edit_budget">Edit Budget</string>
    <string name="create_budget">Create Budget</string>
    <string name="period">Period</string>
    <string name="budget_scope">Budget Scope</string>
    <string name="details">Details</string>
    <string name="appearance">Appearance</string>
    <string name="select_month">Select Month</string>
    <string name="select_year">Select Year</string>
    <string name="monthly">Monthly</string>
    <string name="annual">Annual</string>
    <string name="weekly">Weekly</string>
    <string name="daily">Daily</string>
    <string name="select_week">Select Week</string>
    <string name="select_day">Select Day</string>
    <string name="budget_for">Budget for</string>
    <string name="what_is_your_budget_limit">What is your total budget limit</string>
    <string name="budget_equivalents">At this pace</string>
    <string name="budget_equivalent_yearly">Yearly</string>
    <string name="budget_equivalent_monthly">Monthly</string>
    <string name="budget_equivalent_weekly">Weekly</string>
    <string name="budget_equivalent_daily">Daily</string>
</resources>
CLAUDE_FIX_EOF

echo "  feature/budget/src/main/res/values-fr/strings.xml"
mkdir -p "feature/budget/src/main/res/values-fr"
cat > "feature/budget/src/main/res/values-fr/strings.xml" << 'CLAUDE_FIX_EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="budgets">Budgets</string>
    <string name="active_budgets">Budgets actifs</string>
    <string name="no_budget_available">Aucun budget disponible\nAppuyez sur le bouton + pour créer un nouveau budget</string>
    <string name="no_budget_available_short">Aucun budget disponible</string>
    <string name="budget_name">Nom du budget</string>
    <string name="budget_delete_error_message">Échec de la mise à jour du budget</string>
    <string name="budget_name_error">Le nom ne doit pas être vide</string>
    <string name="budget_create_error">Impossible de créer/mettre à jour le budget</string>
    <string name="budget_create_success">Budget créé/mis à jour avec succès</string>
    <string name="budget_amount">Montant du budget</string>
    <string name="budget_amount_error">Le montant du budget ne doit pas être vide ou nul</string>

    <string name="select_date">Sélectionner une date</string>
    <string name="select_account">Sélectionner un compte</string>
    <string name="clear_all">Tout effacer</string>
    <string name="select">Sélectionner</string>
    <string name="select_category">Sélectionner une catégorie</string>

    <string name="all">Tout</string>

    <string name="edit_budget">Modifier le budget</string>
    <string name="create_budget">Créer un budget</string>
    <string name="period">Période</string>
    <string name="budget_scope">Portée du budget</string>
    <string name="details">Détails</string>
    <string name="appearance">Apparence</string>
    <string name="select_month">Sélectionner un mois</string>
    <string name="select_year">Sélectionner une année</string>
    <string name="monthly">Mensuel</string>
    <string name="annual">Annuel</string>
    <string name="weekly">Hebdomadaire</string>
    <string name="daily">Journalier</string>
    <string name="select_week">Sélectionner une semaine</string>
    <string name="select_day">Sélectionner un jour</string>
    <string name="budget_for">Budget pour</string>
    <string name="what_is_your_budget_limit">Quelle est votre limite budgétaire totale</string>
    <string name="budget_equivalents">À ce rythme</string>
    <string name="budget_equivalent_yearly">Annuel</string>
    <string name="budget_equivalent_monthly">Mensuel</string>
    <string name="budget_equivalent_weekly">Hebdomadaire</string>
    <string name="budget_equivalent_daily">Journalier</string>
</resources>
CLAUDE_FIX_EOF

echo "  feature/dashboard/src/main/kotlin/com/naveenapps/expensemanager/feature/dashboard/DashboardViewModel.kt"
mkdir -p "feature/dashboard/src/main/kotlin/com/naveenapps/expensemanager/feature/dashboard"
cat > "feature/dashboard/src/main/kotlin/com/naveenapps/expensemanager/feature/dashboard/DashboardViewModel.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.common.utils.toDayKey
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toWeekKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.GetBudgetsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.budget.budgetName
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.filter.daterange.GetDateRangeUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionGroupByCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionWithFilterUseCase
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.CategoryTransactionState
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.DateRangeType
import com.naveenapps.expensemanager.core.model.ExpenseFlowState
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.getAvailableCreditLimit
import com.naveenapps.expensemanager.core.model.toAccountUiModel
import com.naveenapps.expensemanager.core.model.toTransactionUIModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.Date


class DashboardViewModel(
    getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase,
    getCurrencyUseCase: GetCurrencyUseCase,
    getFormattedAmountUseCase: GetFormattedAmountUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getTransactionGroupByCategoryUseCase: GetTransactionGroupByCategoryUseCase,
    getBudgetsUseCase: GetBudgetsUseCase,
    appCoroutineDispatchers: AppCoroutineDispatchers,
    getDateRangeUseCase: GetDateRangeUseCase,
    settingsRepository: SettingsRepository,
    private val appComposeNavigator: AppComposeNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(
        DashboardState(
            expenseFlowState = ExpenseFlowState(),
            transactions = emptyList(),
            budgets = emptyList(),
            accounts = emptyList(),
            categoryTransactionState = CategoryTransactionState(
                pieChartData = listOf(),
                totalAmount = Amount(0.0),
                categoryTransactions = emptyList(),
                categoryType = CategoryType.EXPENSE,
            ),
            transactionPeriod = ""
        )
    )
    val state = _state.asStateFlow()

    init {
        combine(
            getCurrencyUseCase.invoke(),
            getTransactionWithFilterUseCase.invoke(),
            getAllAccountsUseCase.invoke(),
            getDateRangeUseCase.invoke(),
        ) { currency, transactions, accounts, dateRange ->

            val filteredTransactions = (transactions?.map {
                it.toTransactionUIModel(
                    getFormattedAmountUseCase.invoke(
                        it.amount.amount,
                        currency,
                    ),
                )
            } ?: emptyList()).take(MAX_TRANSACTIONS_IN_LIST)

            val accountsConverted = accounts.map {
                it.toAccountUiModel(
                    getFormattedAmountUseCase.invoke(
                        it.amount,
                        currency,
                    ),
                    if (it.type == AccountType.CREDIT) {
                        getFormattedAmountUseCase.invoke(
                            it.getAvailableCreditLimit(),
                            currency
                        )
                    } else {
                        null
                    }
                )
            }

            val incomeValue = transactions?.filter { it.type == TransactionType.INCOME }?.sumOf {
                it.amount.amount
            } ?: 0.0

            val expenseValue = transactions?.filter { it.type == TransactionType.EXPENSE }?.sumOf {
                it.amount.amount
            } ?: 0.0


            _state.update {
                it.copy(
                    expenseFlowState = it.expenseFlowState.copy(
                        income = getFormattedAmountUseCase.invoke(
                            incomeValue,
                            currency,
                        ).amountString.orEmpty(),
                        expense = getFormattedAmountUseCase.invoke(
                            expenseValue,
                            currency,
                        ).amountString.orEmpty(),
                        balance = getFormattedAmountUseCase.invoke(
                            (incomeValue - expenseValue),
                            currency,
                        ).amountString.orEmpty(),
                    ),
                    transactions = filteredTransactions,
                    accounts = accountsConverted,
                    transactionPeriod = if (dateRange.description.isNotEmpty()) {
                        "${dateRange.name} (${dateRange.description})"
                    } else {
                        dateRange.name
                    }
                )
            }
        }.flowOn(appCoroutineDispatchers.computation)
            .launchIn(viewModelScope)

        getTransactionGroupByCategoryUseCase.invoke(CategoryType.EXPENSE).onEach {
            val categoryTransaction = it.copy(
                pieChartData = it.pieChartData.take(4),
                categoryTransactions = it.categoryTransactions.take(4),
            )
            _state.update { it.copy(categoryTransactionState = categoryTransaction) }
        }.launchIn(viewModelScope)

        combine(
            getBudgetsUseCase.invoke(),
            getDateRangeUseCase.invoke(),
        ) { allBudgets, dateRange ->
            val activeDate = when (dateRange.type) {
                DateRangeType.TODAY, DateRangeType.THIS_WEEK, DateRangeType.THIS_MONTH ->
                    Date(dateRange.dateRanges[0])
                else -> null
            }
            val activeMonth = activeDate?.toMonthAndYearKey()
            val activeWeek = activeDate?.toWeekKey()
            val activeDay = activeDate?.toDayKey()
            val filtered = if (activeMonth != null) {
                // "Active Budgets" means: the monthly budget covering the month being viewed,
                // together with the yearly budget covering the current year, the weekly budget
                // covering the week being viewed, and the daily budget covering the day being
                // viewed — all can be active at once (e.g. a monthly grocery budget alongside a
                // yearly travel budget).
                val currentYear = Date().toYear()
                allBudgets.filter { budget ->
                    when (budget.periodType) {
                        BudgetPeriod.MONTHLY -> budget.selectedMonth == activeMonth
                        BudgetPeriod.YEARLY -> budget.selectedMonth == currentYear
                        BudgetPeriod.WEEKLY -> budget.selectedMonth == activeWeek
                        BudgetPeriod.DAILY -> budget.selectedMonth == activeDay
                    }
                }
            } else {
                allBudgets
            }
            // The "create a budget for this month" nudge is about monthly budgets specifically —
            // an active yearly budget shouldn't suppress it.
            val hasMonthlyBudget = filtered.any { it.periodType == BudgetPeriod.MONTHLY }
            val showCreateBudgetForMonth = if (activeMonth != null && !hasMonthlyBudget) {
                budgetName(activeMonth)
            } else {
                null
            }
            _state.update { it.copy(budgets = filtered, showCreateBudgetForMonth = showCreateBudgetForMonth) }
        }.flowOn(appCoroutineDispatchers.computation)
            .launchIn(viewModelScope)

        settingsRepository.getHomeSummaryCompact().onEach { compact ->
            _state.update { it.copy(isCompactSummary = compact) }
        }.launchIn(viewModelScope)
    }

    private fun openSettings() {
        appComposeNavigator.navigate(ExpenseManagerScreens.Settings)
    }

    private fun openAccountList() {
        appComposeNavigator.navigate(ExpenseManagerScreens.AccountList)
    }

    private fun openAccountCreate(accountId: String?) {
        appComposeNavigator.navigate(ExpenseManagerScreens.AccountCreate(accountId))
    }

    private fun openBudgetList() {
        appComposeNavigator.navigate(ExpenseManagerScreens.BudgetList)
    }

    private fun openBudgetCreate() {
        appComposeNavigator.navigate(ExpenseManagerScreens.BudgetCreate(null))
    }

    private fun openBudgetDetails(budgetId: String?) {
        appComposeNavigator.navigate(ExpenseManagerScreens.BudgetDetails(budgetId))
    }

    private fun openTransactionList() {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionList)
    }

    private fun openTransactionCreate(transactionId: String? = null) {
        appComposeNavigator.navigate(ExpenseManagerScreens.TransactionCreate(transactionId))
    }

    fun processAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.OpenAccountEdit -> openAccountCreate(action.account.id)
            DashboardAction.OpenAccountList -> openAccountList()
            is DashboardAction.OpenBudgetDetails -> openBudgetDetails(action.budgetUiModel.id)
            DashboardAction.OpenBudgetList -> openBudgetList()
            DashboardAction.OpenBudgetCreate -> openBudgetCreate()
            DashboardAction.OpenSettings -> openSettings()
            is DashboardAction.OpenTransactionEdit -> openTransactionCreate(action.transaction?.id)
            DashboardAction.OpenTransactionList -> openTransactionList()
        }
    }

    companion object {
        private const val MAX_TRANSACTIONS_IN_LIST = 10
    }
}
CLAUDE_FIX_EOF

echo
echo "Correctif applique. Etapes suivantes :"
echo "  git add -A"
echo "  git commit -m \"Ajout : budgets Semaine/Jour + carte d'equivalence\""
echo "  git pull --rebase origin main"
echo "  git push"
