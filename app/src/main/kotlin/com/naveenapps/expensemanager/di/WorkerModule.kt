package com.naveenapps.expensemanager.di

import com.naveenapps.expensemanager.network.AndroidNetworkMonitor
import com.naveenapps.expensemanager.network.NetworkMonitor
import com.naveenapps.expensemanager.service.NotificationProcessorWorker
import com.naveenapps.expensemanager.service.PendingNotificationWorker
import com.naveenapps.expensemanager.service.ServiceHealthWorker
import org.koin.android.ext.koin.androidContext
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
    worker {
        PendingNotificationWorker(
            context = get(),
            params = get(),
        )
    }

    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
}
