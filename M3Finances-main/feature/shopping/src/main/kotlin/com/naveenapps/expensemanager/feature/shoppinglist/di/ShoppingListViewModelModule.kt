package com.naveenapps.expensemanager.feature.shoppinglist.di

import com.naveenapps.expensemanager.feature.shoppinglist.create.ShoppingListCreateViewModel
import com.naveenapps.expensemanager.feature.shoppinglist.detail.ShoppingListDetailViewModel
import com.naveenapps.expensemanager.feature.shoppinglist.list.ShoppingListListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val ShoppingListViewModelModule = module {
    viewModel {
        ShoppingListListViewModel(
            getShoppingListsUseCase = get(),
            appComposeNavigator = get(),
        )
    }

    viewModel {
        ShoppingListCreateViewModel(
            savedStateHandle = get(),
            getCurrencyUseCase = get(),
            getAllAccountsUseCase = get(),
            getAllCategoryUseCase = get(),
            getFormattedAmountUseCase = get(),
            findShoppingListByIdUseCase = get(),
            addShoppingListUseCase = get(),
            updateShoppingListUseCase = get(),
            deleteShoppingListUseCase = get(),
            appComposeNavigator = get(),
        )
    }

    viewModel {
        ShoppingListDetailViewModel(
            savedStateHandle = get(),
            getCurrencyUseCase = get(),
            getDefaultCurrencyUseCase = get(),
            getShoppingListItemsUseCase = get(),
            getFormattedAmountUseCase = get(),
            findShoppingListByIdUseCase = get(),
            addShoppingListItemUseCase = get(),
            checkOffShoppingListItemUseCase = get(),
            deleteShoppingListItemUseCase = get(),
            appComposeNavigator = get(),
            numberFormatRepository = get(),
        )
    }
}
