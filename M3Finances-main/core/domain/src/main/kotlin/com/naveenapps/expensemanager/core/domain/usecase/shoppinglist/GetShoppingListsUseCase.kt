package com.naveenapps.expensemanager.core.domain.usecase.shoppinglist

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.common.utils.AppCoroutineDispatchers
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.repository.ShoppingListItemRepository
import com.naveenapps.expensemanager.core.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

/**
 * Lists every shopping list along with how many items are still pending and their combined
 * price, so the list screen doesn't need to open each list to show a preview. Re-emits whenever
 * any list's pending items change (checking an item off elsewhere updates this automatically).
 */
class GetShoppingListsUseCase(
    private val repository: ShoppingListRepository,
    private val itemRepository: ShoppingListItemRepository,
    private val getCurrencyUseCase: GetCurrencyUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val appCoroutineDispatchers: AppCoroutineDispatchers,
) {
    operator fun invoke(): Flow<List<ShoppingListUiModel>> {
        return repository.getShoppingLists()
            .flatMapLatest { shoppingLists ->
                if (shoppingLists.isEmpty()) {
                    return@flatMapLatest flowOf(emptyList())
                }
                combine(
                    shoppingLists.map { shoppingList ->
                        itemRepository.getShoppingListItems(shoppingList.id)
                    },
                ) { itemLists ->
                    shoppingLists.mapIndexed { index, shoppingList ->
                        shoppingList to itemLists[index]
                    }
                }
            }
            .combine(getCurrencyUseCase.invoke()) { pairs, currency ->
                pairs.map { (shoppingList, items) ->
                    ShoppingListUiModel(
                        shoppingList = shoppingList,
                        pendingItemCount = items.size,
                        pendingTotal = getFormattedAmountUseCase.invoke(
                            items.sumOf { it.price },
                            currency,
                        ),
                    )
                }
            }
            .flowOn(appCoroutineDispatchers.computation)
    }
}

@Stable
data class ShoppingListUiModel(
    val shoppingList: ShoppingList,
    val pendingItemCount: Int,
    val pendingTotal: Amount,
)
