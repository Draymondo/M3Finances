package com.naveenapps.expensemanager.core.domain.usecase.worktime

import org.koin.dsl.module

val WorkTimeUseCaseModule = module {
    single { GetWorkTimeSettingsUseCase(repository = get()) }
    single { UpdateWorkTimeSettingsUseCase(repository = get()) }
    single { SetWorkTimeAutoDetectEnabledUseCase(repository = get()) }
    single { GetAutoDetectedIncomeUseCase(accountRepository = get(), transactionRepository = get()) }
    single { GetEffectiveHourlyRateUseCase(repository = get(), getAutoDetectedIncomeUseCase = get()) }
    single { CalculateWorkTimeEquivalentUseCase(repository = get(), getEffectiveHourlyRateUseCase = get()) }
}

