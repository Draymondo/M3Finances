package com.naveenapps.expensemanager.di

import com.naveenapps.expensemanager.service.NotificationProcessorWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val WorkerModule = module {
    worker {
        NotificationProcessorWorker(
            context = get(),
            params = get(),
        )
    }
}

