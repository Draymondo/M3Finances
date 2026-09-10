package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import com.naveenapps.expensemanager.core.domain.usecase.transaction.AddTransactionUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.Transaction
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.ShoppingListItemRepository
import com.naveenapps.expensemanager.core.repository.ShoppingListRepository
import java.util.UUID

/**
 * Checking an item off is the whole point of the feature: it immediately turns a pending
 * [com.naveenapps.expensemanager.core.model.ShoppingListItem] into a real
 * [TransactionType.EXPENSE] against the shopping list's own real account and category — no
 * hidden account, no "finish shopping" step — then deletes the item. This is what makes the list
 * integrate automatically with the existing budgets: the transaction is completely ordinary.
 */
class CheckOffShoppingListItemUseCase(
    private val itemRepository: ShoppingListItemRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
) {
    suspend operator fun invoke(itemId: String): Resource<Boolean> {
        val itemResult = itemRepository.findShoppingListItemById(itemId)
        val item = when (itemResult) {
            is Resource.Error -> return itemResult
            is Resource.Success -> itemResult.data
        }

        val shoppingListResult = shoppingListRepository.findShoppingListById(item.shoppingListId)
        val shoppingList = when (shoppingListResult) {
            is Resource.Error -> return shoppingListResult
            is Resource.Success -> shoppingListResult.data
        }

        val transactionResult = addTransactionUseCase.invoke(
            Transaction(
                id = UUID.randomUUID().toString(),
                notes = item.name,
                categoryId = shoppingList.categoryId,
                fromAccountId = shoppingList.accountId,
                toAccountId = null,
                amount = Amount(item.price),
                imagePath = "",
                type = TransactionType.EXPENSE,
                createdOn = item.createdOn,
                updatedOn = item.createdOn,
            ),
        )
        if (transactionResult is Resource.Error) {
            return transactionResult
        }

        return itemRepository.deleteShoppingListItemById(itemId)
    }
}
