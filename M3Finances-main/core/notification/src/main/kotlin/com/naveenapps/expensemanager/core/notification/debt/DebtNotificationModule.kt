package com.naveenapps.expensemanager.core.notification.debt

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val DebtNotificationModule = module {
    single {
        DebtReminderScheduler(
            context = androidContext(),
            reminderTimeRepository = get(),
        )
    }
    single {
        DebtReminderTrigger(
            debtReminderRepository = get(),
            debtRepository = get(),
            debtReminderScheduler = get(),
        )
    }
    worker {
        DebtReminderWorker(
            context = androidContext(),
            workerParams = get(),
            findDebtByIdUseCase = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
        )
    }
    worker {
        DebtOverdueWorker(
            context = androidContext(),
            workerParams = get(),
            debtRepository = get(),
        )
    }
}
