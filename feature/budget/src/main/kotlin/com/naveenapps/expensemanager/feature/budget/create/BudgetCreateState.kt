package com.naveenapps.expensemanager.feature.budget.create

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetEquivalentUiModel
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.TextFieldValue
import java.util.Date

@Stable
data class BudgetCreateState(
    val isLoading: Boolean,
    val name: TextFieldValue<String>,
    val amount: TextFieldValue<String>,
    val month: TextFieldValue<Date>,
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,
    val goalType: com.naveenapps.expensemanager.core.model.BudgetGoalType = com.naveenapps.expensemanager.core.model.BudgetGoalType.EXPENSE,
    val isAllAccountSelected: Boolean,
    val selectedAccounts: List<AccountUiModel>,
    val isAllCategorySelected: Boolean,
    val selectedCategories: List<Category>,
    val currency: Currency,
    val showDeleteButton: Boolean,
    val showDeleteDialog: Boolean,
    val showAccountSelectionDialog: Boolean,
    val showCategorySelectionDialog: Boolean,
    val showMonthSelection: Boolean,
    /** Read-only "at this pace" comparison against the other 3 periods — see
     * GetBudgetEquivalentsUseCase. Recomputed live as amount/period/accounts/categories change. */
    val equivalents: List<BudgetEquivalentUiModel> = emptyList(),
)
