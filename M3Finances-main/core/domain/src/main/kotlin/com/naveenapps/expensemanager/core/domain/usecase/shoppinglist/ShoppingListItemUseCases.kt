package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingListItem
import com.naveenapps.expensemanager.core.repository.ShoppingListItemRepository
import kotlinx.coroutines.flow.Flow

class GetShoppingListItemsUseCase(
    private val repository: ShoppingListItemRepository,
) {
    operator fun invoke(shoppingListId: String): Flow<List<ShoppingListItem>> {
        return repository.getShoppingListItems(shoppingListId)
    }
}

class AddShoppingListItemUseCase(
    private val repository: ShoppingListItemRepository,
) {
    suspend operator fun invoke(item: ShoppingListItem): Resource<Boolean> {
        if (item.name.isBlank()) {
            return Resource.Error(Exception("Name shouldn't be blank"))
        }
        if (item.price < 0.0) {
            return Resource.Error(Exception("Price can't be negative"))
        }
        return repository.addShoppingListItem(item)
    }
}

/** Removes a pending item without buying it — no transaction is created. Use
 * [CheckOffShoppingListItemUseCase] to record the purchase instead. */
class DeleteShoppingListItemUseCase(
    private val repository: ShoppingListItemRepository,
) {
    suspend operator fun invoke(itemId: String): Resource<Boolean> {
        return repository.deleteShoppingListItemById(itemId)
    }
}
