package com.naveenapps.expensemanager.feature.transaction.create

import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.TransactionType
import java.util.Date

sealed class TransactionCreateAction {

    data object ClosePage : TransactionCreateAction()

    data object ShowDeleteDialog : TransactionCreateAction()

    data object DismissDeleteDialog : TransactionCreateAction()

    data class OpenAccountCreate(val account: AccountUiModel?) : TransactionCreateAction()

    data class SelectAccount(val account: AccountUiModel) : TransactionCreateAction()

    data object DismissAccountSelection : TransactionCreateAction()

    data class ShowAccountSelection(val type: AccountSelection) : TransactionCreateAction()

    data class OpenCategoryCreate(val category: Category?) : TransactionCreateAction()

    data class SelectCategory(val category: Category) : TransactionCreateAction()

    data object DismissCategorySelection : TransactionCreateAction()

    data object ShowCategorySelection : TransactionCreateAction()

    data object ToggleSplit : TransactionCreateAction()

    data object AddSplitItem : TransactionCreateAction()

    data class RemoveSplitItem(val index: Int) : TransactionCreateAction()

    data class SetSplitAmount(val index: Int, val amount: String) : TransactionCreateAction()

    data class SetSplitNotes(val index: Int, val notes: String) : TransactionCreateAction()

    data class ShowSplitCategorySelection(val index: Int) : TransactionCreateAction()

    data class SelectSplitCategory(val index: Int, val category: Category) : TransactionCreateAction()

    data object Delete : TransactionCreateAction()

    data object Save : TransactionCreateAction()

    data class SetNumberPadValue(val amount: String?) : TransactionCreateAction()

    data object ShowNumberPad : TransactionCreateAction()

    data object DismissNumberPad : TransactionCreateAction()

    data class ChangeTransactionType(val type: TransactionType) : TransactionCreateAction()

    data class SelectDate(val date: Date) : TransactionCreateAction()

    data object ShowDateSelection : TransactionCreateAction()

    data object DismissDateSelection : TransactionCreateAction()

    data object ShowTimeSelection : TransactionCreateAction()

    data class ScanReceipt(val imageBytes: ByteArray, val imagePath: String? = null) : TransactionCreateAction()

    data object ClearAiScanError : TransactionCreateAction()
}