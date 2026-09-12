package com.naveenapps.expensemanager.feature.envelope.di

import com.naveenapps.expensemanager.feature.envelope.create.EnvelopeCreateViewModel
import com.naveenapps.expensemanager.feature.envelope.list.EnvelopeListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val EnvelopeViewModelModule = module {
    viewModel {
        EnvelopeListViewModel(
            getEnvelopesUseCase = get(),
            trackToolUsageUseCase = get(),
            appComposeNavigator = get(),
        )
    }

    viewModel {
        EnvelopeCreateViewModel(
            savedStateHandle = get(),
            getCurrencyUseCase = get(),
            getAllCategoryUseCase = get(),
            getDefaultCurrencyUseCase = get(),
            findEnvelopeByIdUseCase = get(),
            addEnvelopeUseCase = get(),
            updateEnvelopeUseCase = get(),
            deleteEnvelopeUseCase = get(),
            appComposeNavigator = get(),
            numberFormatRepository = get(),
        )
    }
}
