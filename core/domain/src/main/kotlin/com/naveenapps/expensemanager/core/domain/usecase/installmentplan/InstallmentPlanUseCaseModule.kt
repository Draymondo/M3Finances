package com.naveenapps.expensemanager.core.domain.usecase.installmentplan

import org.koin.dsl.module

val InstallmentPlanUseCaseModule = module {
    single {
        AddInstallmentPlanUseCase(
            installmentPlanRepository = get(),
            accountRepository = get(),
        )
    }
    single {
        AddInstallmentPaymentUseCase(
            addTransactionUseCase = get(),
            getAllCategoryUseCase = get(),
        )
    }
    single { UpdateInstallmentPlanUseCase(installmentPlanRepository = get()) }
    single { DeleteInstallmentPlanUseCase(accountRepository = get()) }
    single { FindInstallmentPlanByIdUseCase(installmentPlanRepository = get()) }
    single {
        GetInstallmentPlansUseCase(
            repository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            appCoroutineDispatchers = get(),
        )
    }
    single {
        GetInstallmentPlanPaymentsUseCase(
            transactionRepository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
        )
    }
}
