package com.naveenapps.expensemanager.core.data.mappers

import com.naveenapps.expensemanager.core.database.entity.ShoppingListEntity
import com.naveenapps.expensemanager.core.database.entity.ShoppingListItemEntity
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.model.ShoppingListItem

fun ShoppingList.toEntityModel(): ShoppingListEntity {
    return ShoppingListEntity(
        id = id,
        name = name,
        categoryId = categoryId,
        accountId = accountId,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun ShoppingListEntity.toDomainModel(): ShoppingList {
    return ShoppingList(
        id = id,
        name = name,
        categoryId = categoryId,
        accountId = accountId,
        createdOn = createdOn,
        updatedOn = updatedOn,
    )
}

fun ShoppingListItem.toEntityModel(): ShoppingListItemEntity {
    return ShoppingListItemEntity(
        id = id,
        shoppingListId = shoppingListId,
        name = name,
        price = price,
        createdOn = createdOn,
    )
}

fun ShoppingListItemEntity.toDomainModel(): ShoppingListItem {
    return ShoppingListItem(
        id = id,
        shoppingListId = shoppingListId,
        name = name,
        price = price,
        createdOn = createdOn,
    )
}
