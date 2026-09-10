package com.naveenapps.expensemanager.core.notification.recurringtransaction

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val RecurringTransactionNotificationModule = module {
    single {
        RecurringTransactionScheduler(
            context = androidContext(),
        )
    }
    worker {
        RecurringTransactionWorker(
            context = androidContext(),
            workerParams = get(),
            processDueRecurringTransactionsUseCase = get(),
        )
    }
}