package com.naveenapps.expensemanager.core.model

import androidx.compose.runtime.Stable
import java.util.Date

/**
 * A named shopping list tied to a real expense [Category] and a real [Account] — unlike [Debt]
 * or a savings goal, this never creates a hidden counterparty account: checking an item off
 * (see [ShoppingListItem]) records an ordinary [TransactionType.EXPENSE] straight against
 * [accountId], so the list integrates with existing budgets automatically.
 */
@Stable
data class ShoppingList(
    val id: String,
    val name: String,
    val categoryId: String,
    val accountId: String,
    val createdOn: Date,
    val updatedOn: Date,
    /** Populated for display by the domain layer; empty/default until then. */
    val category: Category = Category(
        id = "",
        name = "",
        type = CategoryType.EXPENSE,
        storedIcon = StoredIcon(name = "", backgroundColor = ""),
        createdOn = Date(),
        updatedOn = Date(),
    ),
    val account: Account = Account(
        id = "",
        name = "",
        type = AccountType.REGULAR,
        storedIcon = StoredIcon(name = "", backgroundColor = ""),
        createdOn = Date(),
        updatedOn = Date(),
    ),
)

/**
 * A single pending item on a [ShoppingList]. There is no "checked" flag: checking an item off
 * immediately turns it into a real [Transaction] (see `CheckOffShoppingListItemUseCase`) and
 * deletes this row — a row existing at all means it's still pending. Unchecked items stay in the
 * list indefinitely.
 */
@Stable
data class ShoppingListItem(
    val id: String,
    val shoppingListId: String,
    val name: String,
    val price: Double,
    val createdOn: Date,
)
