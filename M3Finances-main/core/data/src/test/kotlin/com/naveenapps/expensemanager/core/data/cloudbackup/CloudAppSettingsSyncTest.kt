package com.naveenapps.expensemanager.core.data.cloudbackup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.datastore.CurrencyDataStore
import com.naveenapps.expensemanager.core.datastore.DateRangeDataStore
import com.naveenapps.expensemanager.core.datastore.LocaleDataStore
import com.naveenapps.expensemanager.core.datastore.ReminderTimeDataStore
import com.naveenapps.expensemanager.core.datastore.SettingsDataStore
import com.naveenapps.expensemanager.core.datastore.ThemeDataStore
import com.naveenapps.expensemanager.core.model.CurrencyPosition
import com.naveenapps.expensemanager.core.model.DateRangeType
import com.naveenapps.expensemanager.core.model.ReminderTimeState
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.settings.data.datastore.NumberFormatSettingsDatastore
import com.naveenapps.expensemanager.core.settings.domain.model.NumberFormatType
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudAppSettingsSyncTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `settings round trip preserves supported values and excludes secrets`() = runTest {
        val mainStore = createStore("cloud_settings_main")
        val numberFormatStore = createStore("cloud_settings_number_format")
        val theme = ThemeDataStore(mainStore)
        val locale = LocaleDataStore(mainStore)
        val currency = CurrencyDataStore(mainStore)
        val reminder = ReminderTimeDataStore(mainStore)
        val settings = SettingsDataStore(mainStore)
        val dateRange = DateRangeDataStore(mainStore)
        val numberFormat = NumberFormatSettingsDatastore(numberFormatStore)
        val toolUsage = com.naveenapps.expensemanager.core.datastore.ToolUsageDataStore(mainStore)
        val sync = CloudAppSettingsSync(theme, locale, currency, reminder, settings, dateRange, numberFormat, toolUsage)

        theme.setTheme(2)
        locale.setLocaleTag("fr")
        currency.setCurrency("Euro", "€", "EUR", CurrencyPosition.PREFIX.ordinal)
        reminder.setReminderTime(ReminderTimeState(8, 45, true))
        reminder.setReminder(false)
        settings.setTransactionType(listOf(TransactionType.EXPENSE))
        settings.setAccounts(listOf("account-1"))
        settings.setCategories(listOf("category-1"))
        settings.setPreloaded(true)
        settings.setDefaultAccount("account-1")
        settings.setDefaultExpenseCategory("category-1")
        settings.setDefaultIncomeCategory("income-1")
        settings.setHomeSummaryCompact(true)
        dateRange.setFilterType(DateRangeType.CUSTOM)
        dateRange.setDateRanges(1000L, 2000L)
        numberFormat.setNumberFormatType(NumberFormatType.WITH_PERIOD_SEPARATOR)

        val saved = sync.read()

        assertThat(saved["localeTag"]).isEqualTo("fr")
        assertThat(saved["currencyCode"]).isEqualTo("EUR")
        assertThat(saved["numberFormatType"]).isEqualTo(NumberFormatType.WITH_PERIOD_SEPARATOR.ordinal)
        assertThat(saved).doesNotContainKey("geminiApiKey")
        assertThat(saved).doesNotContainKey("appLockEnabled")

        theme.setTheme(0)
        locale.setLocaleTag("en")
        currency.setCurrency("US Dollars", "$", "USD", CurrencyPosition.SUFFIX.ordinal)
        reminder.setReminder(true)
        settings.setPreloaded(false)
        dateRange.setFilterType(DateRangeType.TODAY)
        numberFormat.setNumberFormatType(NumberFormatType.WITHOUT_ANY_SEPARATOR)

        sync.restore(saved)

        assertThat(sync.read()).isEqualTo(saved)
    }

    private fun createStore(name: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("$name.preferences_pb")
        }
}
