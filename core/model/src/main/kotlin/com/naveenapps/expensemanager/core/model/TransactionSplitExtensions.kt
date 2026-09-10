package com.naveenapps.expensemanager.core.model

fun Transaction.expandedForCategoryAccounting(): List<Transaction> {
    if (splitItems.size < 2) return listOf(this)

    return splitItems.map { item ->
        copy(
            id = "${id}-split-${item.id}",
            notes = item.notes ?: notes,
            categoryId = item.categoryId,
            amount = item.amount,
            category = item.category ?: category,
            splitItems = emptyList(),
        )
    }
}

fun List<Transaction>.expandedForCategoryAccounting(): List<Transaction> = flatMap {
    it.expandedForCategoryAccounting()
}
