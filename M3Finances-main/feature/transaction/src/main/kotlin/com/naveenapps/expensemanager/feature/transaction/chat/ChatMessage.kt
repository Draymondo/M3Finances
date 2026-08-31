package com.naveenapps.expensemanager.feature.transaction.chat

import android.graphics.Bitmap
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String?,
    val imageBitmap: Bitmap? = null,
    val proposedTransaction: ProposedTransaction? = null,
    val proposedCategory: ProposedCategory? = null,
    val proposedAccount: ProposedAccount? = null,
    val proposedShoppingList: ProposedShoppingList? = null,
    val proposedSavingsGoal: ProposedSavingsGoal? = null,
    val proposedBudget: ProposedBudget? = null,
    val proposedDebt: ProposedDebt? = null,
    val proposedRecurring: ProposedRecurring? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

data class ProposedTransaction(
    val amount: Double,
    val categoryName: String,
    val note: String,
    val date: String? = null,
    val accountId: String? = null,
    val accountName: String? = null,
    val splitItems: List<ProposedSplitItem>? = null
)

data class ProposedSplitItem(
    val amount: Double,
    val categoryName: String,
    val note: String
)

data class ProposedCategory(
    val name: String,
    val type: String // "EXPENSE" or "INCOME"
)

data class ProposedAccount(
    val name: String,
    val type: com.naveenapps.expensemanager.core.model.AccountType
)

data class ProposedShoppingList(
    val name: String,
    val items: List<String>
)

data class ProposedSavingsGoal(
    val name: String,
    val targetAmount: Double
)

data class ProposedBudget(
    val amount: Double
)

data class ProposedDebt(
    val personName: String,
    val amount: Double,
    val direction: com.naveenapps.expensemanager.core.model.DebtDirection
)

data class ProposedRecurring(
    val name: String,
    val amount: Double,
    val type: com.naveenapps.expensemanager.core.model.TransactionType
)
