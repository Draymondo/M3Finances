package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import org.koin.dsl.module

val ShoppingListUseCaseModule = module {
    single {
        GetShoppingListsUseCase(
            repository = get(),
            itemRepository = get(),
            getCurrencyUseCase = get(),
            getFormattedAmountUseCase = get(),
            appCoroutineDispatchers = get(),
        )
    }
    single { FindShoppingListByIdUseCase(repository = get()) }
    single { AddShoppingListUseCase(repository = get()) }
    single { UpdateShoppingListUseCase(repository = get()) }
    single { DeleteShoppingListUseCase(repository = get()) }

    single { GetShoppingListItemsUseCase(repository = get()) }
    single { AddShoppingListItemUseCase(repository = get()) }
    single { DeleteShoppingListItemUseCase(repository = get()) }
    single {
        CheckOffShoppingListItemUseCase(
            itemRepository = get(),
            shoppingListRepository = get(),
            addTransactionUseCase = get(),
        )
    }
}
