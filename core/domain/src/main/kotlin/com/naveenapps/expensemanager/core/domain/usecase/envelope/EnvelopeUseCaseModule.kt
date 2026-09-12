package com.naveenapps.expensemanager.core.domain.usecase.envelope

import org.koin.dsl.module

val EnvelopeUseCaseModule = module {
    single { CheckEnvelopeValidateUseCase() }
    single {
        CheckEnvelopeBudgetExclusivityUseCase(
            envelopeRepository = get(),
            budgetRepository = get(),
        )
    }
    single {
        AddEnvelopeUseCase(
            repository = get(),
            checkEnvelopeValidateUseCase = get(),
            checkEnvelopeBudgetExclusivityUseCase = get(),
        )
    }
    single {
        UpdateEnvelopeUseCase(
            repository = get(),
            checkEnvelopeValidateUseCase = get(),
            checkEnvelopeBudgetExclusivityUseCase = get(),
        )
    }
    single { DeleteEnvelopeUseCase(repository = get()) }
    single { FindEnvelopeByIdUseCase(repository = get()) }
    single {
        GetEnvelopeTransactionsUseCase(
            accountRepository = get(),
            transactionRepository = get(),
        )
    }
    single {
        GetEnvelopeRemainingUseCase(
            getEnvelopeTransactionsUseCase = get(),
        )
    }
    single {
        GetEnvelopesUseCase(
            envelopeRepository = get(),
            categoryRepository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            getEnvelopeRemainingUseCase = get(),
        )
    }
}
