package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.repository.ShoppingListRepository

class FindShoppingListByIdUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(id: String): Resource<ShoppingList> {
        return repository.findShoppingListById(id)
    }
}

class AddShoppingListUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(shoppingList: ShoppingList): Resource<Boolean> {
        if (shoppingList.name.isBlank()) {
            return Resource.Error(Exception("Le nom ne doit pas être vide"))
        }
        return repository.addShoppingList(shoppingList)
    }
}

class UpdateShoppingListUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(shoppingList: ShoppingList): Resource<Boolean> {
        if (shoppingList.name.isBlank()) {
            return Resource.Error(Exception("Le nom ne doit pas être vide"))
        }
        return repository.updateShoppingList(shoppingList)
    }
}

/** Deleting a shopping list only removes its metadata row and, via the `shopping_list_item`
 * table's `ON DELETE CASCADE` foreign key, its pending items. It never touches transactions or
 * accounts — items only ever become transactions when explicitly checked off. */
class DeleteShoppingListUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(shoppingList: ShoppingList): Resource<Boolean> {
        return repository.deleteShoppingList(shoppingList)
    }
}
