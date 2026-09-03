package com.naveenapps.expensemanager.di

import com.naveenapps.expensemanager.service.NotificationProcessorWorker
import com.naveenapps.expensemanager.service.ServiceHealthWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val WorkerModule = module {
    worker {
        NotificationProcessorWorker(
            context = get(),
            params = get(),
        )
    }
    worker { ServiceHealthWorker(get(), get()) }
    worker { com.naveenapps.expensemanager.service.WeeklySummaryWorker(get(), get()) }
}
