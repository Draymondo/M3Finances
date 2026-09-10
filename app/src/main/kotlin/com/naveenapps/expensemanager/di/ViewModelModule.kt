package com.naveenapps.expensemanager.di

import com.naveenapps.expensemanager.MainViewModel
import com.naveenapps.expensemanager.core.designsystem.components.CommonViewModelModule
import com.naveenapps.expensemanager.core.settings.di.CoreSettingsModule
import com.naveenapps.expensemanager.feature.account.di.AccountViewModelModule
import com.naveenapps.expensemanager.feature.analysis.di.AnalysisViewModelModule
import com.naveenapps.expensemanager.feature.budget.di.BudgetViewModelModule
import com.naveenapps.expensemanager.feature.category.di.CategoryViewModelModule
import com.naveenapps.expensemanager.feature.country.di.CountryViewModelModule
import com.naveenapps.expensemanager.feature.currency.di.CurrencyViewModelModule
import com.naveenapps.expensemanager.feature.dashboard.di.DashboardViewModelModule
import com.naveenapps.expensemanager.feature.export.di.ExportViewModelModule
import com.naveenapps.expensemanager.feature.filter.di.FilterViewModelModule
import com.naveenapps.expensemanager.feature.language.di.LanguageViewModelModule
import com.naveenapps.expensemanager.feature.reminder.di.ReminderViewModelModule
import com.naveenapps.expensemanager.feature.recurring.di.RecurringViewModelModule
import com.naveenapps.expensemanager.feature.debt.di.DebtViewModelModule
import com.naveenapps.expensemanager.feature.savingsgoal.di.SavingsGoalViewModelModule
import com.naveenapps.expensemanager.feature.shoppinglist.di.ShoppingListViewModelModule
import com.naveenapps.expensemanager.feature.settings.di.SettingsViewModelModule
import com.naveenapps.expensemanager.feature.theme.di.ThemeViewModelModule
import com.naveenapps.expensemanager.feature.transaction.di.TransactionViewModelModule
import com.naveenapps.expensemanager.ui.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val MainViewModelModule = module {
    viewModel {
        MainViewModel(
            getCurrentThemeUseCase = get(),
            settingsRepository = get(),
        )
    }
    viewModel {
        HomeViewModel(
            updateReminderStatusUseCase = get(),
            notificationScheduler = get(),
            cloudBackupRepository = get(),
        )
    }
}

val ViewModelModule = module {
    includes(
        MainViewModelModule,
        AccountViewModelModule,
        AnalysisViewModelModule,
        BudgetViewModelModule,
        CategoryViewModelModule,
        DashboardViewModelModule,
        TransactionViewModelModule,
        SettingsViewModelModule,
        ThemeViewModelModule,
        LanguageViewModelModule,
        ExportViewModelModule,
        ReminderViewModelModule,
        CurrencyViewModelModule,
        FilterViewModelModule,
        CountryViewModelModule,
        CommonViewModelModule,
        CoreSettingsModule,
        RecurringViewModelModule,
        DebtViewModelModule,
        SavingsGoalViewModelModule,
        ShoppingListViewModelModule,
    )
}
