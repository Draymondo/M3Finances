package com.naveenapps.expensemanager.core.domain.usecase.analysis

import org.koin.dsl.module

val AnalysisUseCaseModule = module {
    single { GenerateInsightsUseCase(geminiRepository = get(), settingsRepository = get()) }
}

