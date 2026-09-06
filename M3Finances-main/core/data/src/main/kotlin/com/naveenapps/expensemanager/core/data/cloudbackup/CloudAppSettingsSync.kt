package com.naveenapps.expensemanager.core.data.cloudbackup

import com.naveenapps.expensemanager.core.datastore.CurrencyDataStore
import com.naveenapps.expensemanager.core.datastore.DateRangeDataStore
import com.naveenapps.expensemanager.core.datastore.LocaleDataStore
import com.naveenapps.expensemanager.core.datastore.ReminderTimeDataStore
import com.naveenapps.expensemanager.core.datastore.SettingsDataStore
import com.naveenapps.expensemanager.core.datastore.ThemeDataStore
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.CurrencyPosition
import com.naveenapps.expensemanager.core.model.DateRangeType
import com.naveenapps.expensemanager.core.model.ReminderTimeState
import com.naveenapps.expensemanager.core.settings.data.datastore.NumberFormatSettingsDatastore
import com.naveenapps.expensemanager.core.settings.domain.model.NumberFormatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CloudAppSettingsSync(
    private val themeDataStore: ThemeDataStore,
    private val localeDataStore: LocaleDataStore,
    private val currencyDataStore: CurrencyDataStore,
    private val reminderTimeDataStore: ReminderTimeDataStore,
    private val settingsDataStore: SettingsDataStore,
    private val dateRangeDataStore: DateRangeDataStore,
    private val numberFormatSettingsDatastore: NumberFormatSettingsDatastore,
) {

    suspend fun read(): Map<String, Any?> = snapshot().toFirestoreMap()

    suspend fun restore(data: Map<String, Any?>) {
        snapshotFrom(data)?.restore()
    }

    fun observeChanges(): Flow<Unit> = combine(
        themeDataStore.getTheme(DEFAULT_THEME),
        localeDataStore.getLocaleTag(DEFAULT_LOCALE),
        currencyDataStore.getCurrency(DEFAULT_CURRENCY),
        reminderTimeDataStore.getReminderTime(DEFAULT_REMINDER),
        reminderTimeDataStore.isReminderOn(),
        settingsDataStore.getTransactionType(),
        settingsDataStore.getAccounts(),
        settingsDataStore.getCategories(),
        settingsDataStore.isPreloaded(),
        settingsDataStore.getDefaultAccount(),
        settingsDataStore.getDefaultExpenseCategory(),
        settingsDataStore.getDefaultIncomeCategory(),
        settingsDataStore.getHomeSummaryCompact(),
        dateRangeDataStore.getFilterType(),
        dateRangeDataStore.getDateRanges(),
        numberFormatSettingsDatastore.getNumberFormatType(),
    ) { values -> values.toList() }
        .distinctUntilChanged()
        .map { }

    private suspend fun snapshot(): Snapshot = Snapshot(
        themeMode = themeDataStore.getTheme(DEFAULT_THEME).first(),
        localeTag = localeDataStore.getLocaleTag(DEFAULT_LOCALE).first(),
        currency = currencyDataStore.getCurrency(DEFAULT_CURRENCY).first(),
        reminderTime = reminderTimeDataStore.getReminderTime(DEFAULT_REMINDER).first(),
        reminderEnabled = reminderTimeDataStore.isReminderOn().first(),
        transactionTypes = settingsDataStore.getTransactionType().first()?.map { it.ordinal },
        selectedAccounts = settingsDataStore.getAccounts().first(),
        selectedCategories = settingsDataStore.getCategories().first(),
        preloaded = settingsDataStore.isPreloaded().first(),
        defaultAccount = settingsDataStore.getDefaultAccount().first(),
        defaultExpenseCategory = settingsDataStore.getDefaultExpenseCategory().first(),
        defaultIncomeCategory = settingsDataStore.getDefaultIncomeCategory().first(),
        homeSummaryCompact = settingsDataStore.getHomeSummaryCompact().first(),
        dateFilterType = dateRangeDataStore.getFilterType().first().ordinal,
        dateRanges = dateRangeDataStore.getDateRanges().first(),
        numberFormatType = numberFormatSettingsDatastore.getNumberFormatType().first().ordinal,
    )

    private suspend fun Snapshot.restore() {
        themeDataStore.setTheme(themeMode)
        localeDataStore.setLocaleTag(localeTag)
        currencyDataStore.setCurrency(
            name = currency.name,
            symbol = currency.symbol,
            code = currency.code,
            position = currency.position.ordinal,
        )
        reminderTimeDataStore.setReminderTime(reminderTime)
        reminderTimeDataStore.setReminder(reminderEnabled)
        settingsDataStore.setTransactionType(transactionTypes?.map(::transactionTypeFromOrdinal))
        settingsDataStore.setAccounts(selectedAccounts)
        settingsDataStore.setCategories(selectedCategories)
        settingsDataStore.setPreloaded(preloaded)
        settingsDataStore.setDefaultAccount(defaultAccount)
        settingsDataStore.setDefaultExpenseCategory(defaultExpenseCategory)
        settingsDataStore.setDefaultIncomeCategory(defaultIncomeCategory)
        settingsDataStore.setHomeSummaryCompact(homeSummaryCompact)
        dateRangeDataStore.setFilterType(DateRangeType.entries[dateFilterType])
        dateRanges?.let { dateRangeDataStore.setDateRanges(it[0], it[1]) }
        numberFormatSettingsDatastore.setNumberFormatType(NumberFormatType.entries[numberFormatType])
    }

    private fun Snapshot.toFirestoreMap(): Map<String, Any?> = mapOf(
        KEY_VERSION to VERSION,
        KEY_THEME_MODE to themeMode,
        KEY_LOCALE_TAG to localeTag,
        KEY_CURRENCY_NAME to currency.name,
        KEY_CURRENCY_SYMBOL to currency.symbol,
        KEY_CURRENCY_CODE to currency.code,
        KEY_CURRENCY_POSITION to currency.position.ordinal,
        KEY_REMINDER_HOUR to reminderTime.hour,
        KEY_REMINDER_MINUTE to reminderTime.minute,
        KEY_REMINDER_24_HOUR to reminderTime.is24Hour,
        KEY_REMINDER_ENABLED to reminderEnabled,
        KEY_TRANSACTION_TYPES to transactionTypes,
        KEY_SELECTED_ACCOUNTS to selectedAccounts,
        KEY_SELECTED_CATEGORIES to selectedCategories,
        KEY_PRELOADED to preloaded,
        KEY_DEFAULT_ACCOUNT to defaultAccount,
        KEY_DEFAULT_EXPENSE_CATEGORY to defaultExpenseCategory,
        KEY_DEFAULT_INCOME_CATEGORY to defaultIncomeCategory,
        KEY_HOME_SUMMARY_COMPACT to homeSummaryCompact,
        KEY_DATE_FILTER_TYPE to dateFilterType,
        KEY_DATE_RANGES to dateRanges,
        KEY_NUMBER_FORMAT_TYPE to numberFormatType,
    )

    private fun snapshotFrom(data: Map<String, Any?>): Snapshot? {
        val currencyPosition = data.int(KEY_CURRENCY_POSITION) ?: return null
        val dateFilterType = data.int(KEY_DATE_FILTER_TYPE) ?: return null
        val numberFormatType = data.int(KEY_NUMBER_FORMAT_TYPE) ?: return null
        val reminderHour = data.int(KEY_REMINDER_HOUR) ?: return null
        val reminderMinute = data.int(KEY_REMINDER_MINUTE) ?: return null
        return runCatching {
            Snapshot(
                themeMode = data.int(KEY_THEME_MODE) ?: DEFAULT_THEME,
                localeTag = data.string(KEY_LOCALE_TAG) ?: DEFAULT_LOCALE,
                currency = Currency(
                    name = data.string(KEY_CURRENCY_NAME) ?: DEFAULT_CURRENCY.name,
                    symbol = data.string(KEY_CURRENCY_SYMBOL) ?: DEFAULT_CURRENCY.symbol,
                    code = data.string(KEY_CURRENCY_CODE) ?: DEFAULT_CURRENCY.code,
                    position = CurrencyPosition.entries[currencyPosition],
                ),
                reminderTime = ReminderTimeState(
                    hour = reminderHour,
                    minute = reminderMinute,
                    is24Hour = data.boolean(KEY_REMINDER_24_HOUR) ?: false,
                ),
                reminderEnabled = data.boolean(KEY_REMINDER_ENABLED) ?: true,
                transactionTypes = data.list(KEY_TRANSACTION_TYPES)?.map { it.toInt() },
                selectedAccounts = data.stringList(KEY_SELECTED_ACCOUNTS),
                selectedCategories = data.stringList(KEY_SELECTED_CATEGORIES),
                preloaded = data.boolean(KEY_PRELOADED) ?: false,
                defaultAccount = data.string(KEY_DEFAULT_ACCOUNT),
                defaultExpenseCategory = data.string(KEY_DEFAULT_EXPENSE_CATEGORY),
                defaultIncomeCategory = data.string(KEY_DEFAULT_INCOME_CATEGORY),
                homeSummaryCompact = data.boolean(KEY_HOME_SUMMARY_COMPACT) ?: false,
                dateFilterType = dateFilterType,
                dateRanges = data.longList(KEY_DATE_RANGES),
                numberFormatType = numberFormatType,
            ).also {
                DateRangeType.entries[dateFilterType]
                NumberFormatType.entries[numberFormatType]
            }
        }.getOrNull()
    }

    private fun transactionTypeFromOrdinal(ordinal: Int) =
        com.naveenapps.expensemanager.core.model.TransactionType.entries[ordinal]

    private data class Snapshot(
        val themeMode: Int,
        val localeTag: String,
        val currency: Currency,
        val reminderTime: ReminderTimeState,
        val reminderEnabled: Boolean,
        val transactionTypes: List<Int>?,
        val selectedAccounts: List<String>?,
        val selectedCategories: List<String>?,
        val preloaded: Boolean,
        val defaultAccount: String?,
        val defaultExpenseCategory: String?,
        val defaultIncomeCategory: String?,
        val homeSummaryCompact: Boolean,
        val dateFilterType: Int,
        val dateRanges: List<Long>?,
        val numberFormatType: Int,
    )

    private fun Map<String, Any?>.int(key: String) = (this[key] as? Number)?.toInt()
    private fun Map<String, Any?>.string(key: String) = this[key] as? String
    private fun Map<String, Any?>.boolean(key: String) = this[key] as? Boolean
    private fun Map<String, Any?>.list(key: String) = (this[key] as? List<*>)?.mapNotNull {
        (it as? Number)?.toInt()
    }
    private fun Map<String, Any?>.stringList(key: String) =
        (this[key] as? List<*>)?.mapNotNull { it as? String }
    private fun Map<String, Any?>.longList(key: String) =
        (this[key] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() }

    companion object {
        private const val VERSION = 1
        private const val DEFAULT_THEME = 0
        private const val DEFAULT_LOCALE = "en"
        private val DEFAULT_CURRENCY = Currency(
            name = "US Dollars",
            symbol = "$",
            code = "USD",
            position = CurrencyPosition.SUFFIX,
        )
        private const val DEFAULT_REMINDER = "10:0:false"
        private const val KEY_VERSION = "version"
        private const val KEY_THEME_MODE = "themeMode"
        private const val KEY_LOCALE_TAG = "localeTag"
        private const val KEY_CURRENCY_NAME = "currencyName"
        private const val KEY_CURRENCY_SYMBOL = "currencySymbol"
        private const val KEY_CURRENCY_CODE = "currencyCode"
        private const val KEY_CURRENCY_POSITION = "currencyPosition"
        private const val KEY_REMINDER_HOUR = "reminderHour"
        private const val KEY_REMINDER_MINUTE = "reminderMinute"
        private const val KEY_REMINDER_24_HOUR = "reminder24Hour"
        private const val KEY_REMINDER_ENABLED = "reminderEnabled"
        private const val KEY_TRANSACTION_TYPES = "transactionTypes"
        private const val KEY_SELECTED_ACCOUNTS = "selectedAccounts"
        private const val KEY_SELECTED_CATEGORIES = "selectedCategories"
        private const val KEY_PRELOADED = "preloaded"
        private const val KEY_DEFAULT_ACCOUNT = "defaultAccount"
        private const val KEY_DEFAULT_EXPENSE_CATEGORY = "defaultExpenseCategory"
        private const val KEY_DEFAULT_INCOME_CATEGORY = "defaultIncomeCategory"
        private const val KEY_HOME_SUMMARY_COMPACT = "homeSummaryCompact"
        private const val KEY_DATE_FILTER_TYPE = "dateFilterType"
        private const val KEY_DATE_RANGES = "dateRanges"
        private const val KEY_NUMBER_FORMAT_TYPE = "numberFormatType"
    }
}
