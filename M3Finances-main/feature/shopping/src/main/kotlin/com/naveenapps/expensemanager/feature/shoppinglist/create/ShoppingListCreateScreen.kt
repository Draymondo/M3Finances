package com.naveenapps.expensemanager.feature.shoppinglist.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.designsystem.components.DeleteDialogItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.SafeModalBottomSheet
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.designsystem.ui.components.StringTextField
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.feature.account.selection.AccountItem
import com.naveenapps.expensemanager.feature.account.selection.AccountItemDefaults
import com.naveenapps.expensemanager.feature.account.selection.AccountSelectionScreen
import com.naveenapps.expensemanager.feature.category.selection.CategoryItem
import com.naveenapps.expensemanager.feature.category.selection.CategoryItemDefaults
import com.naveenapps.expensemanager.feature.category.selection.CategorySelectionScreen
import com.naveenapps.expensemanager.feature.shoppinglist.R
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date

@Composable
fun ShoppingListCreateScreen(
    viewModel: ShoppingListCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    ShoppingListCreateScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun ShoppingListCreateScreenContent(
    state: ShoppingListCreateState,
    onAction: (ShoppingListCreateAction) -> Unit,
) {
    if (state.showDeleteDialog) {
        DeleteDialogItem(
            confirm = { onAction.invoke(ShoppingListCreateAction.Delete) },
            dismiss = { onAction.invoke(ShoppingListCreateAction.DismissDeleteDialog) },
        )
    } else if (state.showCategorySelection) {
        CategorySelectionView(state, onAction)
    } else if (state.showAccountSelection) {
        AccountSelectionView(state, onAction)
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(ShoppingListCreateAction.ClosePage) },
                title = if (state.showDeleteButton) {
                    stringResource(R.string.edit_shopping_list)
                } else {
                    stringResource(R.string.create_shopping_list)
                },
                actions = {
                    if (state.showDeleteButton) {
                        IconButton(
                            onClick = { onAction.invoke(ShoppingListCreateAction.ShowDeleteDialog) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_shopping_list),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction.invoke(ShoppingListCreateAction.Save) },
                icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                text = { Text(text = stringResource(R.string.save)) },
            )
        },
    ) { innerPadding ->
        ShoppingListCreateBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategorySelectionView(
    state: ShoppingListCreateState,
    onAction: (ShoppingListCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(ShoppingListCreateAction.DismissCategorySelection) },
    ) {
        CategorySelectionScreen(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            createNewCallback = { onAction.invoke(ShoppingListCreateAction.OpenCategoryCreate) },
            onItemSelection = { onAction.invoke(ShoppingListCreateAction.SelectCategory(it)) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AccountSelectionView(
    state: ShoppingListCreateState,
    onAction: (ShoppingListCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(ShoppingListCreateAction.DismissAccountSelection) },
    ) {
        AccountSelectionScreen(
            accounts = state.accounts,
            selectedAccount = state.selectedAccount,
            createNewCallback = { onAction.invoke(ShoppingListCreateAction.OpenAccountCreate) },
            onItemSelection = { onAction.invoke(ShoppingListCreateAction.SelectAccount(it)) },
        )
    }
}

@Composable
private fun ShoppingListCreateBody(
    state: ShoppingListCreateState,
    onAction: (ShoppingListCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsSection(
            title = stringResource(R.string.shopping_list_name),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            StringTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.name.value,
                isError = state.name.valueError,
                onValueChange = state.name.onValueChange,
                label = R.string.shopping_list_name,
                errorMessage = stringResource(R.string.shopping_list_name_error),
            )
        }

        SettingsSection(title = stringResource(R.string.select_category)) {
            CategoryItem(
                name = state.selectedCategory.titleResId?.let { stringResource(it) }
                    ?: state.selectedCategory.name,
                icon = state.selectedCategory.storedIcon.name,
                iconBackgroundColor = state.selectedCategory.storedIcon.backgroundColor,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    focusManager.clearFocus(force = true)
                    onAction.invoke(ShoppingListCreateAction.ShowCategorySelection)
                },
                trailingContent = { CategoryItemDefaults.ChevronTrailing() },
            )
        }

        SettingsSection(title = stringResource(R.string.select_account)) {
            AccountItem(
                name = state.selectedAccount.name,
                icon = state.selectedAccount.storedIcon.name,
                iconBackgroundColor = state.selectedAccount.storedIcon.backgroundColor,
                amount = state.selectedAccount.amount.amountString,
                amountTextColor = state.selectedAccount.amountTextColor,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    focusManager.clearFocus(force = true)
                    onAction.invoke(ShoppingListCreateAction.ShowAccountSelection)
                },
                trailingContent = { AccountItemDefaults.ChevronTrailing() },
            )
        }
    }
}

@AppPreviewsLightAndDarkMode
@Composable
private fun ShoppingListCreateScreenPreview() {
    NaveenAppsPreviewTheme {
        ShoppingListCreateScreenContent(
            state = ShoppingListCreateState(
                isEditing = false,
                name = TextFieldValue(value = "Courses hebdo", valueError = false, onValueChange = {}),
                categories = emptyList(),
                selectedCategory = Category(
                    id = "1",
                    name = "Alimentation",
                    type = CategoryType.EXPENSE,
                    storedIcon = StoredIcon("ic_calendar", "#000000"),
                    createdOn = Date(),
                    updatedOn = Date(),
                ),
                accounts = emptyList(),
                selectedAccount = AccountUiModel(
                    id = "1",
                    name = "Wallet",
                    storedIcon = StoredIcon("ic_calendar", "#000000"),
                    amount = Amount(0.0, "$ 0.00"),
                    amountTextColor = com.naveenapps.expensemanager.core.common.R.color.green_500,
                ),
                showDeleteButton = false,
                showDeleteDialog = false,
                showCategorySelection = false,
                showAccountSelection = false,
            ),
            onAction = {},
        )
    }
}
