package com.naveenapps.expensemanager.core.domain.usecase.tools

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val ToolsUseCaseModule = module {
    factoryOf(::GetToolUsageUseCase)
    factoryOf(::TrackToolUsageUseCase)
}
