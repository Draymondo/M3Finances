package com.naveenapps.expensemanager.core.domain.usecase.category

import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType

/**
 * Robustly matches a suggested category name against a list of actual database categories.
 */
fun List<Category>.matchCategory(suggestedName: String?, fallbackType: CategoryType = CategoryType.EXPENSE): Category? {
    if (suggestedName.isNullOrBlank()) {
        return this.firstOrNull { it.type == fallbackType } ?: this.firstOrNull()
    }
    
    // 1. Exact match (ignore case)
    val exactMatch = this.find { it.name.equals(suggestedName, ignoreCase = true) }
    if (exactMatch != null) return exactMatch

    // 2. Alias matching
    val aliasMap = mapOf(
        "Transport" to "Transportation",
        "Bills" to "Utilities",
        "Food & Drink" to "Food",
        "Restaurant" to "Food",
        "Groceries" to "Food",
        "Clothes" to "Clothing",
        "Travel" to "Transportation",
        "Family" to "Leisure",
        "Personal" to "Leisure",
        "Education" to "Leisure", // Map to Leisure if Education is missing
        "Other" to "Utilities" // Generic fallback
    )
    
    // Try to find if the suggested name matches any alias key
    val mappedName = aliasMap.entries.find { suggestedName.contains(it.key, ignoreCase = true) }?.value 
        ?: aliasMap[suggestedName]
        ?: suggestedName

    val aliasMatch = this.find { it.name.equals(mappedName, ignoreCase = true) }
    if (aliasMatch != null) return aliasMatch

    // 3. Fallback
    return this.firstOrNull { it.type == fallbackType } ?: this.firstOrNull()
}

