package com.naveenapps.expensemanager.core.domain.usecase.di

import com.naveenapps.expensemanager.core.domain.usecase.account.AccountUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.category.CategoryUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.country.CountryUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.currencyconverter.CurrencyConverterUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.debt.DebtUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.envelope.EnvelopeUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.networth.NetWorthUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.recurringtransaction.RecurringTransactionUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.savingsgoal.SavingsGoalUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.ShoppingListUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.settings.SettingsUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.settings.filter.FilterUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.tools.ToolsUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.transaction.TransactionUseCaseModule
import com.naveenapps.expensemanager.core.domain.usecase.worktime.WorkTimeUseCaseModule
import org.koin.dsl.module

val UseCaseModule = module {
    includes(
        AccountUseCaseModule,
        BudgetUseCaseModule,
        CategoryUseCaseModule,
        CountryUseCaseModule,
        SettingsUseCaseModule,
        TransactionUseCaseModule,
        FilterUseCaseModule,
        RecurringTransactionUseCaseModule,
        DebtUseCaseModule,
        EnvelopeUseCaseModule,
        SavingsGoalUseCaseModule,
        NetWorthUseCaseModule,
        ShoppingListUseCaseModule,
        ToolsUseCaseModule,
        WorkTimeUseCaseModule,
        CurrencyConverterUseCaseModule,
        com.naveenapps.expensemanager.core.domain.usecase.analysis.AnalysisUseCaseModule,
    )
}

