package com.naveenapps.expensemanager.core.notification.budget

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val BudgetNotificationModule = module {
    single {
        BudgetAlertScheduler(
            context = androidContext(),
        )
    }
    worker {
        BudgetAlertWorker(
            context = androidContext(),
            workerParams = get(),
            getBudgetsUseCase = get(),
        )
    }
}
