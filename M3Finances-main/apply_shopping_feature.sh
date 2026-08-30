#!/data/data/com.termux/files/usr/bin/bash
# Ajoute la fonctionnalite Liste de courses (5e et derniere fonctionnalite prevue).
# Nouveau module feature/shopping (liste, creation/edition, detail avec articles)
# + cablage navigation/DI/gradle + entree dans Parametres.
# A executer depuis la RACINE de ton clone git (la ou se trouve settings.gradle.kts).
set -e
echo "Application de la fonctionnalite Liste de courses..."

echo "--- Nouveaux fichiers ---"
echo "  feature/shopping/build.gradle.kts"
mkdir -p "feature/shopping"
cat > "feature/shopping/build.gradle.kts" << 'CLAUDE_EOF_1'
plugins {
    id("naveenapps.plugin.android.feature")
    id("naveenapps.plugin.kotlin.basic")
    id("naveenapps.plugin.compose")
    id("naveenapps.plugin.di")
}

android {
    namespace = "com.naveenapps.expensemanager.feature.shoppinglist"
}

dependencies {
    implementation(project(":core:settings"))

    implementation(project(":feature:category"))
    implementation(project(":feature:account"))
}
CLAUDE_EOF_1

echo "  feature/shopping/src/main/AndroidManifest.xml"
mkdir -p "feature/shopping/src/main"
cat > "feature/shopping/src/main/AndroidManifest.xml" << 'CLAUDE_EOF_2'
<?xml version="1.0" encoding="utf-8"?>
<manifest>

</manifest>
CLAUDE_EOF_2

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create/ShoppingListCreateAction.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create/ShoppingListCreateAction.kt" << 'CLAUDE_EOF_3'
package com.naveenapps.expensemanager.feature.shoppinglist.create

import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Category

sealed class ShoppingListCreateAction {

    data object ClosePage : ShoppingListCreateAction()

    data object Save : ShoppingListCreateAction()

    data object Delete : ShoppingListCreateAction()

    data object ShowDeleteDialog : ShoppingListCreateAction()

    data object DismissDeleteDialog : ShoppingListCreateAction()

    data object OpenCategoryCreate : ShoppingListCreateAction()

    data object ShowCategorySelection : ShoppingListCreateAction()

    data object DismissCategorySelection : ShoppingListCreateAction()

    data class SelectCategory(val category: Category) : ShoppingListCreateAction()

    data object OpenAccountCreate : ShoppingListCreateAction()

    data object ShowAccountSelection : ShoppingListCreateAction()

    data object DismissAccountSelection : ShoppingListCreateAction()

    data class SelectAccount(val account: AccountUiModel) : ShoppingListCreateAction()
}
CLAUDE_EOF_3

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create/ShoppingListCreateScreen.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create/ShoppingListCreateScreen.kt" << 'CLAUDE_EOF_4'
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
CLAUDE_EOF_4

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create/ShoppingListCreateState.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create/ShoppingListCreateState.kt" << 'CLAUDE_EOF_5'
package com.naveenapps.expensemanager.feature.shoppinglist.create

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.TextFieldValue

@Stable
data class ShoppingListCreateState(
    val isEditing: Boolean,
    val name: TextFieldValue<String>,
    val categories: List<Category>,
    val selectedCategory: Category,
    val accounts: List<AccountUiModel>,
    val selectedAccount: AccountUiModel,
    val showDeleteButton: Boolean,
    val showDeleteDialog: Boolean,
    val showCategorySelection: Boolean,
    val showAccountSelection: Boolean,
)
CLAUDE_EOF_5

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create/ShoppingListCreateViewModel.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/create/ShoppingListCreateViewModel.kt" << 'CLAUDE_EOF_6'
package com.naveenapps.expensemanager.feature.shoppinglist.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.AddShoppingListUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.DeleteShoppingListUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.FindShoppingListByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.UpdateShoppingListUseCase
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.AccountUiModel
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.model.getAvailableCreditLimit
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.model.toAccountUiModel
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerArgsNames
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class ShoppingListCreateViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getAllCategoryUseCase: GetAllCategoryUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val findShoppingListByIdUseCase: FindShoppingListByIdUseCase,
    private val addShoppingListUseCase: AddShoppingListUseCase,
    private val updateShoppingListUseCase: UpdateShoppingListUseCase,
    private val deleteShoppingListUseCase: DeleteShoppingListUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val accountsAndCategoriesReady = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        ShoppingListCreateState(
            isEditing = false,
            name = TextFieldValue(value = "", valueError = false, onValueChange = this::setName),
            categories = emptyList(),
            selectedCategory = defaultCategory,
            accounts = emptyList(),
            selectedAccount = defaultAccount,
            showDeleteButton = false,
            showDeleteDialog = false,
            showCategorySelection = false,
            showAccountSelection = false,
        ),
    )
    val state = _state.asStateFlow()

    private var editingShoppingList: ShoppingList? = null

    init {
        observeAccountsAndCategories(getCurrencyUseCase, getAllAccountsUseCase, getAllCategoryUseCase)
        loadShoppingListWhenReady(savedStateHandle)
    }

    private fun observeAccountsAndCategories(
        getCurrencyUseCase: GetCurrencyUseCase,
        getAllAccountsUseCase: GetAllAccountsUseCase,
        getAllCategoryUseCase: GetAllCategoryUseCase,
    ) {
        combine(
            getCurrencyUseCase.invoke(),
            getAllAccountsUseCase.invoke(),
            getAllCategoryUseCase.invoke(),
        ) { currency, accounts, categories ->
            val mappedAccounts = accounts.map { account ->
                account.toAccountUiModel(
                    getFormattedAmountUseCase.invoke(account.amount, currency),
                    if (account.type == AccountType.CREDIT) {
                        getFormattedAmountUseCase.invoke(account.getAvailableCreditLimit(), currency)
                    } else {
                        null
                    },
                )
            }
            val expenseCategories = categories.filter { it.type.isExpense() }
            _state.update {
                it.copy(
                    accounts = mappedAccounts,
                    selectedAccount = if (it.isEditing) {
                        it.selectedAccount
                    } else {
                        mappedAccounts.firstOrNull() ?: defaultAccount
                    },
                    categories = expenseCategories,
                    selectedCategory = if (it.isEditing) {
                        it.selectedCategory
                    } else {
                        expenseCategories.firstOrNull() ?: defaultCategory
                    },
                )
            }
            accountsAndCategoriesReady.update { true }
        }.launchIn(viewModelScope)
    }

    private fun loadShoppingListWhenReady(savedStateHandle: SavedStateHandle) {
        val id = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID) ?: return
        viewModelScope.launch {
            accountsAndCategoriesReady.first { it }
            loadEditingShoppingList(id)
        }
    }

    private suspend fun loadEditingShoppingList(id: String) {
        when (val response = findShoppingListByIdUseCase.invoke(id)) {
            is Resource.Error -> Unit
            is Resource.Success -> {
                val shoppingList = response.data
                editingShoppingList = shoppingList
                _state.update {
                    val matchedAccount = it.accounts.find { account ->
                        account.id == shoppingList.accountId
                    } ?: it.selectedAccount
                    it.copy(
                        isEditing = true,
                        name = it.name.copy(value = shoppingList.name),
                        selectedCategory = shoppingList.category,
                        selectedAccount = matchedAccount,
                        showDeleteButton = true,
                    )
                }
            }
        }
    }

    private fun save() {
        val currentState = _state.value

        if (currentState.name.value.isBlank()) {
            _state.update { it.copy(name = it.name.copy(valueError = true)) }
            return
        }

        if (currentState.isEditing) {
            val shoppingList = editingShoppingList ?: return
            viewModelScope.launch {
                val updated = shoppingList.copy(
                    name = currentState.name.value,
                    categoryId = currentState.selectedCategory.id,
                    accountId = currentState.selectedAccount.id,
                    updatedOn = Date(),
                )
                if (updateShoppingListUseCase.invoke(updated) is Resource.Success) {
                    closePage()
                }
            }
            return
        }

        viewModelScope.launch {
            val now = Date()
            val newShoppingList = ShoppingList(
                id = UUID.randomUUID().toString(),
                name = currentState.name.value,
                categoryId = currentState.selectedCategory.id,
                accountId = currentState.selectedAccount.id,
                createdOn = now,
                updatedOn = now,
            )
            if (addShoppingListUseCase.invoke(newShoppingList) is Resource.Success) {
                closePage()
            }
        }
    }

    private fun delete() {
        val shoppingList = editingShoppingList ?: return
        viewModelScope.launch {
            if (deleteShoppingListUseCase.invoke(shoppingList) is Resource.Success) {
                closePage()
            }
        }
    }

    private fun setName(name: String) {
        _state.update {
            it.copy(name = it.name.copy(value = name, valueError = name.isBlank()))
        }
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    private fun openAccountCreate() {
        appComposeNavigator.navigate(ExpenseManagerScreens.AccountCreate(null))
    }

    private fun openCategoryCreate() {
        appComposeNavigator.navigate(ExpenseManagerScreens.CategoryCreate(null))
    }

    fun processAction(action: ShoppingListCreateAction) {
        when (action) {
            ShoppingListCreateAction.ClosePage -> closePage()
            ShoppingListCreateAction.Save -> save()
            ShoppingListCreateAction.Delete -> delete()

            ShoppingListCreateAction.ShowDeleteDialog -> _state.update { it.copy(showDeleteDialog = true) }
            ShoppingListCreateAction.DismissDeleteDialog -> _state.update { it.copy(showDeleteDialog = false) }

            ShoppingListCreateAction.OpenCategoryCreate -> openCategoryCreate()
            ShoppingListCreateAction.ShowCategorySelection -> _state.update {
                it.copy(showCategorySelection = true)
            }
            ShoppingListCreateAction.DismissCategorySelection -> _state.update {
                it.copy(showCategorySelection = false)
            }
            is ShoppingListCreateAction.SelectCategory -> _state.update {
                it.copy(selectedCategory = action.category, showCategorySelection = false)
            }

            ShoppingListCreateAction.OpenAccountCreate -> openAccountCreate()
            ShoppingListCreateAction.ShowAccountSelection -> _state.update {
                it.copy(showAccountSelection = true)
            }
            ShoppingListCreateAction.DismissAccountSelection -> _state.update {
                it.copy(showAccountSelection = false)
            }
            is ShoppingListCreateAction.SelectAccount -> _state.update {
                it.copy(selectedAccount = action.account, showAccountSelection = false)
            }
        }
    }

    companion object {
        private val defaultCategory = Category(
            id = "1",
            name = "Shopping",
            type = CategoryType.EXPENSE,
            storedIcon = StoredIcon(name = "ic_calendar", backgroundColor = "#000000"),
            createdOn = Date(),
            updatedOn = Date(),
        )

        private val defaultAccount = AccountUiModel(
            id = "1",
            name = "",
            storedIcon = StoredIcon(name = "", backgroundColor = "#000000"),
            amount = Amount(0.0, "$ 0.00"),
            amountTextColor = com.naveenapps.expensemanager.core.common.R.color.green_500,
        )
    }
}
CLAUDE_EOF_6

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail/ShoppingListDetailAction.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail/ShoppingListDetailAction.kt" << 'CLAUDE_EOF_7'
package com.naveenapps.expensemanager.feature.shoppinglist.detail

sealed class ShoppingListDetailAction {

    data object ClosePage : ShoppingListDetailAction()

    data object OpenEdit : ShoppingListDetailAction()

    data object ShowAddItemSheet : ShoppingListDetailAction()

    data object DismissAddItemSheet : ShoppingListDetailAction()

    data object AddItem : ShoppingListDetailAction()

    data class CheckOffItem(val itemId: String) : ShoppingListDetailAction()

    data class DeleteItem(val itemId: String) : ShoppingListDetailAction()
}
CLAUDE_EOF_7

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail/ShoppingListDetailScreen.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail/ShoppingListDetailScreen.kt" << 'CLAUDE_EOF_8'
package com.naveenapps.expensemanager.feature.shoppinglist.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.designsystem.components.EmptyItem
import com.naveenapps.expensemanager.core.designsystem.components.LoadingItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.DecimalTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.SafeModalBottomSheet
import com.naveenapps.expensemanager.core.designsystem.ui.components.StringTextField
import com.naveenapps.expensemanager.core.designsystem.ui.utils.ItemSpecModifier
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.ShoppingListItem
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.feature.shoppinglist.R
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date

@Composable
fun ShoppingListDetailScreen(
    viewModel: ShoppingListDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    ShoppingListDetailScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun ShoppingListDetailScreenContent(
    state: ShoppingListDetailState,
    onAction: (ShoppingListDetailAction) -> Unit,
) {
    if (state.showAddItemSheet) {
        AddItemSheetView(state, onAction)
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(ShoppingListDetailAction.ClosePage) },
                title = state.shoppingList?.name.orEmpty(),
                actions = {
                    IconButton(onClick = { onAction.invoke(ShoppingListDetailAction.OpenEdit) }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.edit_shopping_list),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction.invoke(ShoppingListDetailAction.ShowAddItemSheet) },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { innerPadding ->
        ShoppingListDetailContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddItemSheetView(
    state: ShoppingListDetailState,
    onAction: (ShoppingListDetailAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(ShoppingListDetailAction.DismissAddItemSheet) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.add_item),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            StringTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.itemName.value,
                isError = state.itemName.valueError,
                onValueChange = state.itemName.onValueChange,
                label = R.string.item_name,
                errorMessage = stringResource(R.string.item_name_error),
            )
            DecimalTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.itemPrice.value,
                isError = state.itemPrice.valueError,
                onValueChange = state.itemPrice.onValueChange,
                label = R.string.item_price,
                errorMessage = stringResource(R.string.item_price_error),
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAction.invoke(ShoppingListDetailAction.AddItem) },
            ) {
                Text(text = stringResource(R.string.add_item))
            }
        }
    }
}

@Composable
private fun ShoppingListDetailContent(
    state: ShoppingListDetailState,
    onAction: (ShoppingListDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when {
            state.isLoading -> LoadingItem(modifier = Modifier.align(Alignment.Center))

            state.items.isEmpty() -> EmptyItem(
                emptyItemText = stringResource(id = R.string.no_items_available),
                icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_accounts,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.pending_total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = state.pendingTotal.amountString.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = state.items,
                        key = { it.item.id },
                    ) { model ->
                        AppCardView(modifier = ItemSpecModifier) {
                            ShoppingListItemRow(model = model, onAction = onAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingListItemRow(
    model: ShoppingListItemUiModel,
    onAction: (ShoppingListDetailAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = false,
            onCheckedChange = {
                onAction.invoke(ShoppingListDetailAction.CheckOffItem(model.item.id))
            },
        )
        Text(
            text = model.item.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = model.formattedPrice.amountString.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = { onAction.invoke(ShoppingListDetailAction.DeleteItem(model.item.id)) }) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete_item),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@AppPreviewsLightAndDarkMode
@Composable
private fun ShoppingListDetailScreenPreview() {
    NaveenAppsPreviewTheme {
        ShoppingListDetailScreenContent(
            state = ShoppingListDetailState(
                isLoading = false,
                shoppingList = null,
                items = listOf(
                    ShoppingListItemUiModel(
                        item = ShoppingListItem(
                            id = "1",
                            shoppingListId = "1",
                            name = "Riz",
                            price = 1500.0,
                            createdOn = Date(),
                        ),
                        formattedPrice = Amount(1500.0, "1 500 F"),
                    ),
                ),
                currency = Currency("F", "XOF"),
                pendingTotal = Amount(1500.0, "1 500 F"),
                showAddItemSheet = false,
                itemName = TextFieldValue(value = "", valueError = false, onValueChange = {}),
                itemPrice = TextFieldValue(value = "0", valueError = false, onValueChange = {}),
            ),
            onAction = {},
        )
    }
}
CLAUDE_EOF_8

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail/ShoppingListDetailState.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail/ShoppingListDetailState.kt" << 'CLAUDE_EOF_9'
package com.naveenapps.expensemanager.feature.shoppinglist.detail

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.model.ShoppingListItem
import com.naveenapps.expensemanager.core.model.TextFieldValue

/** One pending item paired with its price already formatted for display in [state.currency]. */
@Stable
data class ShoppingListItemUiModel(
    val item: ShoppingListItem,
    val formattedPrice: Amount,
)

@Stable
data class ShoppingListDetailState(
    val isLoading: Boolean,
    val shoppingList: ShoppingList?,
    val items: List<ShoppingListItemUiModel>,
    val currency: Currency,
    val pendingTotal: Amount,
    val showAddItemSheet: Boolean,
    val itemName: TextFieldValue<String>,
    val itemPrice: TextFieldValue<String>,
)
CLAUDE_EOF_9

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail/ShoppingListDetailViewModel.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/detail/ShoppingListDetailViewModel.kt" << 'CLAUDE_EOF_10'
package com.naveenapps.expensemanager.feature.shoppinglist.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.AddShoppingListItemUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.CheckOffShoppingListItemUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.DeleteShoppingListItemUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.FindShoppingListByIdUseCase
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.GetShoppingListItemsUseCase
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.ShoppingListItem
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerArgsNames
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.settings.domain.repository.NumberFormatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

class ShoppingListDetailViewModel(
    savedStateHandle: SavedStateHandle,
    getCurrencyUseCase: GetCurrencyUseCase,
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    getShoppingListItemsUseCase: GetShoppingListItemsUseCase,
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase,
    private val findShoppingListByIdUseCase: FindShoppingListByIdUseCase,
    private val addShoppingListItemUseCase: AddShoppingListItemUseCase,
    private val checkOffShoppingListItemUseCase: CheckOffShoppingListItemUseCase,
    private val deleteShoppingListItemUseCase: DeleteShoppingListItemUseCase,
    private val appComposeNavigator: AppComposeNavigator,
    private val numberFormatRepository: NumberFormatRepository,
) : ViewModel() {

    private val shoppingListId: String = savedStateHandle.get<String>(ExpenseManagerArgsNames.ID).orEmpty()

    private val _state = MutableStateFlow(
        ShoppingListDetailState(
            isLoading = true,
            shoppingList = null,
            items = emptyList(),
            currency = getDefaultCurrencyUseCase.invoke(),
            pendingTotal = Amount(0.0, ""),
            showAddItemSheet = false,
            itemName = TextFieldValue(value = "", valueError = false, onValueChange = this::setItemName),
            itemPrice = TextFieldValue(
                value = numberFormatRepository.formatForEditing(0.0),
                valueError = false,
                onValueChange = this::setItemPrice,
            ),
        ),
    )
    val state = _state.asStateFlow()

    init {
        loadShoppingList()
        observeItems(getCurrencyUseCase, getShoppingListItemsUseCase)
    }

    private fun loadShoppingList() {
        if (shoppingListId.isBlank()) {
            closePage()
            return
        }
        viewModelScope.launch {
            when (val response = findShoppingListByIdUseCase.invoke(shoppingListId)) {
                is Resource.Error -> closePage()
                is Resource.Success -> _state.update { it.copy(shoppingList = response.data) }
            }
        }
    }

    private fun observeItems(
        getCurrencyUseCase: GetCurrencyUseCase,
        getShoppingListItemsUseCase: GetShoppingListItemsUseCase,
    ) {
        combine(
            getShoppingListItemsUseCase.invoke(shoppingListId),
            getCurrencyUseCase.invoke(),
        ) { items, currency ->
            val formattedItems = items.map { item ->
                ShoppingListItemUiModel(
                    item = item,
                    formattedPrice = getFormattedAmountUseCase.invoke(item.price, currency),
                )
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    items = formattedItems,
                    currency = currency,
                    pendingTotal = getFormattedAmountUseCase.invoke(items.sumOf { item -> item.price }, currency),
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun showAddItemSheet() {
        _state.update {
            it.copy(
                showAddItemSheet = true,
                itemName = it.itemName.copy(value = "", valueError = false),
                itemPrice = it.itemPrice.copy(
                    value = numberFormatRepository.formatForEditing(0.0),
                    valueError = false,
                ),
            )
        }
    }

    private fun addItem() {
        val currentState = _state.value

        if (currentState.itemName.value.isBlank()) {
            _state.update { it.copy(itemName = it.itemName.copy(valueError = true)) }
            return
        }

        val price = numberFormatRepository.parseToDouble(currentState.itemPrice.value)
        if (price == null || price < 0.0) {
            _state.update { it.copy(itemPrice = it.itemPrice.copy(valueError = true)) }
            return
        }

        viewModelScope.launch {
            val response = addShoppingListItemUseCase.invoke(
                ShoppingListItem(
                    id = UUID.randomUUID().toString(),
                    shoppingListId = shoppingListId,
                    name = currentState.itemName.value,
                    price = price,
                    createdOn = Date(),
                ),
            )
            if (response is Resource.Success) {
                _state.update { it.copy(showAddItemSheet = false) }
            }
        }
    }

    private fun checkOffItem(itemId: String) {
        viewModelScope.launch {
            checkOffShoppingListItemUseCase.invoke(itemId)
        }
    }

    private fun deleteItem(itemId: String) {
        viewModelScope.launch {
            deleteShoppingListItemUseCase.invoke(itemId)
        }
    }

    private fun setItemName(name: String) {
        _state.update {
            it.copy(itemName = it.itemName.copy(value = name, valueError = name.isBlank()))
        }
    }

    private fun setItemPrice(price: String) {
        val priceValue = numberFormatRepository.parseToDouble(price)
        _state.update {
            it.copy(
                itemPrice = it.itemPrice.copy(
                    value = price,
                    valueError = price.isBlank() || priceValue == null || priceValue < 0.0,
                ),
            )
        }
    }

    private fun openEdit() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ShoppingListCreate(shoppingListId))
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: ShoppingListDetailAction) {
        when (action) {
            ShoppingListDetailAction.ClosePage -> closePage()
            ShoppingListDetailAction.OpenEdit -> openEdit()
            ShoppingListDetailAction.ShowAddItemSheet -> showAddItemSheet()
            ShoppingListDetailAction.DismissAddItemSheet -> _state.update {
                it.copy(showAddItemSheet = false)
            }
            ShoppingListDetailAction.AddItem -> addItem()
            is ShoppingListDetailAction.CheckOffItem -> checkOffItem(action.itemId)
            is ShoppingListDetailAction.DeleteItem -> deleteItem(action.itemId)
        }
    }
}
CLAUDE_EOF_10

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/di/ShoppingListViewModelModule.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/di"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/di/ShoppingListViewModelModule.kt" << 'CLAUDE_EOF_11'
package com.naveenapps.expensemanager.feature.shoppinglist.di

import com.naveenapps.expensemanager.feature.shoppinglist.create.ShoppingListCreateViewModel
import com.naveenapps.expensemanager.feature.shoppinglist.detail.ShoppingListDetailViewModel
import com.naveenapps.expensemanager.feature.shoppinglist.list.ShoppingListListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val ShoppingListViewModelModule = module {
    viewModel {
        ShoppingListListViewModel(
            getShoppingListsUseCase = get(),
            appComposeNavigator = get(),
        )
    }

    viewModel {
        ShoppingListCreateViewModel(
            savedStateHandle = get(),
            getCurrencyUseCase = get(),
            getAllAccountsUseCase = get(),
            getAllCategoryUseCase = get(),
            getFormattedAmountUseCase = get(),
            findShoppingListByIdUseCase = get(),
            addShoppingListUseCase = get(),
            updateShoppingListUseCase = get(),
            deleteShoppingListUseCase = get(),
            appComposeNavigator = get(),
        )
    }

    viewModel {
        ShoppingListDetailViewModel(
            savedStateHandle = get(),
            getCurrencyUseCase = get(),
            getDefaultCurrencyUseCase = get(),
            getShoppingListItemsUseCase = get(),
            getFormattedAmountUseCase = get(),
            findShoppingListByIdUseCase = get(),
            addShoppingListItemUseCase = get(),
            checkOffShoppingListItemUseCase = get(),
            deleteShoppingListItemUseCase = get(),
            appComposeNavigator = get(),
            numberFormatRepository = get(),
        )
    }
}
CLAUDE_EOF_11

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list/ShoppingListListAction.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list/ShoppingListListAction.kt" << 'CLAUDE_EOF_12'
package com.naveenapps.expensemanager.feature.shoppinglist.list

sealed class ShoppingListListAction {

    data object ClosePage : ShoppingListListAction()

    data object OpenShoppingListCreate : ShoppingListListAction()

    data class OpenShoppingListDetail(val shoppingListId: String) : ShoppingListListAction()
}
CLAUDE_EOF_12

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list/ShoppingListListScreen.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list/ShoppingListListScreen.kt" << 'CLAUDE_EOF_13'
package com.naveenapps.expensemanager.feature.shoppinglist.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.expensemanager.core.designsystem.components.EmptyItem
import com.naveenapps.expensemanager.core.designsystem.components.LoadingItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.utils.ItemSpecModifier
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.ShoppingListUiModel
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.ShoppingList
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.feature.shoppinglist.R
import org.koin.compose.viewmodel.koinViewModel
import java.util.Date

@Composable
fun ShoppingListListScreen(
    viewModel: ShoppingListListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    ShoppingListListScaffoldView(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun ShoppingListListScaffoldView(
    state: ShoppingListListState,
    onAction: (ShoppingListListAction) -> Unit,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(ShoppingListListAction.ClosePage) },
                title = stringResource(id = R.string.shopping_lists),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction.invoke(ShoppingListListAction.OpenShoppingListCreate) },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { innerPadding ->
        ShoppingListListContent(
            modifier = Modifier.padding(innerPadding),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun ShoppingListListContent(
    state: ShoppingListListState,
    onAction: (ShoppingListListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingItem(modifier = Modifier.align(Alignment.Center))

            state.shoppingLists.isEmpty() -> EmptyItem(
                emptyItemText = stringResource(id = R.string.no_shopping_lists_available),
                icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_accounts,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = state.shoppingLists,
                    key = { it.shoppingList.id },
                ) { model ->
                    AppCardView(
                        modifier = ItemSpecModifier,
                        onClick = {
                            onAction.invoke(
                                ShoppingListListAction.OpenShoppingListDetail(model.shoppingList.id),
                            )
                        },
                    ) {
                        ShoppingListRow(model = model)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingListRow(model: ShoppingListUiModel) {
    val shoppingList = model.shoppingList
    val iconColor = colorResource(id = com.naveenapps.expensemanager.core.common.R.color.blue_500)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ShoppingCart,
                contentDescription = null,
                tint = iconColor,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shoppingList.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            val categoryName = shoppingList.category.titleResId?.let { stringResource(it) }
                ?: shoppingList.category.name
            Text(
                text = stringResource(
                    id = R.string.shopping_list_subtitle,
                    categoryName,
                    shoppingList.account.name,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = model.pendingTotal.amountString.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = iconColor,
            )
            Text(
                text = stringResource(id = R.string.pending_items_count, model.pendingItemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun ShoppingListListScreenPreview() {
    NaveenAppsPreviewTheme {
        ShoppingListListScaffoldView(
            state = ShoppingListListState(
                isLoading = false,
                shoppingLists = listOf(
                    ShoppingListUiModel(
                        shoppingList = ShoppingList(
                            id = "1",
                            name = "Courses hebdo",
                            categoryId = "cat1",
                            accountId = "acc1",
                            createdOn = Date(),
                            updatedOn = Date(),
                            category = Category(
                                id = "cat1",
                                name = "Alimentation",
                                type = CategoryType.EXPENSE,
                                storedIcon = StoredIcon("", ""),
                                createdOn = Date(),
                                updatedOn = Date(),
                            ),
                            account = Account(
                                id = "acc1",
                                name = "Wave",
                                type = AccountType.REGULAR,
                                storedIcon = StoredIcon("", ""),
                                createdOn = Date(),
                                updatedOn = Date(),
                            ),
                        ),
                        pendingItemCount = 3,
                        pendingTotal = Amount(4500.0, "4 500 F"),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
CLAUDE_EOF_13

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list/ShoppingListListState.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list/ShoppingListListState.kt" << 'CLAUDE_EOF_14'
package com.naveenapps.expensemanager.feature.shoppinglist.list

import androidx.compose.runtime.Stable
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.ShoppingListUiModel

@Stable
data class ShoppingListListState(
    val isLoading: Boolean,
    val shoppingLists: List<ShoppingListUiModel>,
)
CLAUDE_EOF_14

echo "  feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list/ShoppingListListViewModel.kt"
mkdir -p "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list"
cat > "feature/shopping/src/main/kotlin/com/naveenapps/expensemanager/feature/shoppinglist/list/ShoppingListListViewModel.kt" << 'CLAUDE_EOF_15'
package com.naveenapps.expensemanager.feature.shoppinglist.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.shoppinglist.GetShoppingListsUseCase
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ShoppingListListViewModel(
    getShoppingListsUseCase: GetShoppingListsUseCase,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ShoppingListListState(
            isLoading = true,
            shoppingLists = emptyList(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        getShoppingListsUseCase.invoke().onEach { shoppingLists ->
            _state.update {
                it.copy(isLoading = false, shoppingLists = shoppingLists)
            }
        }.launchIn(viewModelScope)
    }

    private fun openCreateScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ShoppingListCreate(null))
    }

    private fun openDetailScreen(shoppingListId: String) {
        appComposeNavigator.navigate(ExpenseManagerScreens.ShoppingListDetail(shoppingListId))
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    fun processAction(action: ShoppingListListAction) {
        when (action) {
            ShoppingListListAction.ClosePage -> closePage()
            ShoppingListListAction.OpenShoppingListCreate -> openCreateScreen()
            is ShoppingListListAction.OpenShoppingListDetail -> openDetailScreen(action.shoppingListId)
        }
    }
}
CLAUDE_EOF_15

echo "  feature/shopping/src/main/res/values-fr/strings.xml"
mkdir -p "feature/shopping/src/main/res/values-fr"
cat > "feature/shopping/src/main/res/values-fr/strings.xml" << 'CLAUDE_EOF_16'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="shopping_lists">Listes de courses</string>
    <string name="no_shopping_lists_available">Aucune liste de courses pour l\'instant\nAppuie sur + pour en créer une</string>
    <string name="shopping_list_subtitle">%1$s · %2$s</string>
    <string name="pending_items_count">%1$d en attente</string>

    <string name="create_shopping_list">Créer une liste de courses</string>
    <string name="edit_shopping_list">Modifier la liste de courses</string>
    <string name="delete_shopping_list">Supprimer la liste de courses</string>
    <string name="save">Enregistrer</string>
    <string name="shopping_list_name">Nom de la liste</string>
    <string name="shopping_list_name_error">Le nom de la liste ne doit pas être vide</string>
    <string name="select_category">Catégorie</string>
    <string name="select_account">Compte</string>

    <string name="add_item">Ajouter un article</string>
    <string name="item_name">Nom de l\'article</string>
    <string name="item_name_error">Le nom de l\'article ne doit pas être vide</string>
    <string name="item_price">Prix</string>
    <string name="item_price_error">Le prix ne peut pas être négatif</string>
    <string name="delete_item">Retirer l\'article</string>
    <string name="pending_total">Total en attente</string>
    <string name="no_items_available">Aucun article pour l\'instant\nAppuie sur + pour en ajouter un</string>
</resources>
CLAUDE_EOF_16

echo "  feature/shopping/src/main/res/values/strings.xml"
mkdir -p "feature/shopping/src/main/res/values"
cat > "feature/shopping/src/main/res/values/strings.xml" << 'CLAUDE_EOF_17'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="shopping_lists">Shopping lists</string>
    <string name="no_shopping_lists_available">No shopping lists yet\nTap + to start one</string>
    <string name="shopping_list_subtitle">%1$s · %2$s</string>
    <string name="pending_items_count">%1$d pending</string>

    <string name="create_shopping_list">Create shopping list</string>
    <string name="edit_shopping_list">Edit shopping list</string>
    <string name="delete_shopping_list">Delete shopping list</string>
    <string name="save">Save</string>
    <string name="shopping_list_name">List name</string>
    <string name="shopping_list_name_error">List name shouldn\'t be blank</string>
    <string name="select_category">Category</string>
    <string name="select_account">Account</string>

    <string name="add_item">Add item</string>
    <string name="item_name">Item name</string>
    <string name="item_name_error">Item name shouldn\'t be blank</string>
    <string name="item_price">Price</string>
    <string name="item_price_error">Price can\'t be negative</string>
    <string name="delete_item">Remove item</string>
    <string name="pending_total">Pending total</string>
    <string name="no_items_available">No items yet\nTap + to add one</string>
</resources>
CLAUDE_EOF_17

echo "--- Fichiers modifies ---"
echo "  app/build.gradle.kts"
cat > "app/build.gradle.kts" << 'CLAUDE_EOF_18'
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("naveenapps.plugin.android.app")
    id("com.google.android.gms.oss-licenses-plugin")
    id("naveenapps.plugin.kotlin.basic")
    id("naveenapps.plugin.compose")
    id("naveenapps.plugin.di")
    id("com.github.triplet.play")
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler)
}


val keysFolderPath: String =
    if (File("${rootDir.absolutePath}/keys/credentials.properties").exists()) {
        "${rootDir.absolutePath}/keys"
    } else {
        rootDir.absolutePath
    }

fun getCredentialsFile(): File {
    val credentialFilePath = "$keysFolderPath/credentials.properties"
    return File(credentialFilePath)
}

fun getKeystoreFile(): File {
    val keystoreFilePath = "$keysFolderPath/android_keystore.jks"
    return File(keystoreFilePath)
}

fun getPlayStorePublisherFile(): File {
    val playStorePublisherFile = "$keysFolderPath/play_publish.json"
    return File(playStorePublisherFile)
}

val credentials = getCredentialsFile()
val keystore = getKeystoreFile()
if (credentials.exists() && keystore.exists()) {
    println("----- Both Keystore & Credentials available -----")
    println("----- ${credentials.absolutePath} -----")
    val properties = Properties().apply {
        load(FileInputStream(credentials))
    }

    android {
        signingConfigs {
            create("release") {
                keyAlias = properties.getProperty("KEY_ALIAS")
                storePassword = properties.getProperty("KEY_STORE_PASSWORD")
                keyPassword = properties.getProperty("KEY_PASSWORD")
                storeFile = keystore
            }
        }
    }
} else {
    println("----- Credentials not available -----")
}

val playStorePublisher = getPlayStorePublisherFile()
if (playStorePublisher.exists()) {
    println("----- Play Store Publisher available -----")
    println("----- ${playStorePublisher.absolutePath} -----")
    val track = System.getenv()["PLAYSTORE_TRACK"]
    val status = System.getenv()["PLAYSTORE_RELEASE_STATUS"]?.uppercase()
    println("----- ENV: $track & $status -----")
    val playStoreTrack = track ?: "beta"
    val playStoreReleaseStatus =
        runCatching { ReleaseStatus.valueOf(status!!) }.getOrNull() ?: ReleaseStatus.DRAFT

    println("----- $playStoreTrack & $playStoreReleaseStatus-----")

    android {
        play {
            this.serviceAccountCredentials.set(playStorePublisher)
            this.track.set(playStoreTrack)
            this.releaseStatus.set(playStoreReleaseStatus)
            println(this.serviceAccountCredentials.get().asFile.absolutePath)
        }
    }
} else {
    println("----- Publisher not available -----")
}

android {

    namespace = "com.naveenapps.expensemanager"

    defaultConfig {
        applicationId = "com.naveenapps.expensemanager"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Usage perso uniquement sur appareil arm64 (Android moderne) : on ne construit pas
        // les binaires natifs pour armeabi-v7a / x86 / x86_64, ce qui réduit nettement la
        // taille de l'APK. À retirer si l'app doit un jour tourner sur un appareil 32-bit/x86.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        // Keystore debug standard (alias/mot de passe par défaut d'Android), committé dans le
        // repo à keys/debug.keystore. Le brancher explicitement ici garantit que CHAQUE build
        // debug (local ou CI GitHub Actions, qui tourne sur un runner neuf à chaque fois) est
        // signé avec la même clé — sinon AGP génère un keystore debug aléatoire par run, et le
        // SHA-1 ne matche jamais celui enregistré côté Firebase pour le Google Sign-In.
        getByName("debug") {
            storeFile = file("${rootDir.absolutePath}/keys/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            // applicationIdSuffix ".debug" retiré : le client OAuth Android enregistré dans
            // google-services.json ne couvre que com.naveenapps.expensemanager (sans suffixe).
            // Avec le suffixe, Google Play Services ne trouve aucun identifiant pour le
            // package installé et le Credential Manager renvoie NoCredentialException
            // ("No credentials available").
            enableUnitTestCoverage = true
        }
        create("macrobenchmark") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val keyStore = runCatching { signingConfigs.getByName("release") }.getOrNull()
                ?: signingConfigs.getByName("debug")

            signingConfig = keyStore
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    testOptions {
        managedDevices {
            devices {
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel2api30").apply {
                    // Use device profiles you typically see in Android Studio.
                    device = "Pixel 2"
                    // Use only API levels 27 and higher.
                    apiLevel = 30
                    // To include Google services, use "google".
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:navigation"))
    implementation(project(":core:notification"))
    implementation(project(":core:repository"))
    implementation(project(":core:settings"))

    implementation(project(":feature:account"))
    implementation(project(":feature:analysis"))
    implementation(project(":feature:budget"))
    implementation(project(":feature:category"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:transaction"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:filter"))
    implementation(project(":feature:country"))
    implementation(project(":feature:currency"))

    implementation(project(":feature:settings"))
    implementation(project(":feature:theme"))
    implementation(project(":feature:language"))
    implementation(project(":feature:export"))
    implementation(project(":feature:reminder"))
    implementation(project(":feature:currency"))
    implementation(project(":feature:recurring"))
    implementation(project(":feature:debt"))
    implementation(project(":feature:savings"))
    implementation(project(":feature:shopping"))

    implementation(libs.androidx.splash.screen)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.google.oss.licenses)

    implementation(libs.app.update.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    testImplementation(project(":core:testing"))
    androidTestImplementation(project(":core:testing"))
}
CLAUDE_EOF_18

echo "  app/src/main/kotlin/com/naveenapps/expensemanager/di/ViewModelModule.kt"
cat > "app/src/main/kotlin/com/naveenapps/expensemanager/di/ViewModelModule.kt" << 'CLAUDE_EOF_19'
package com.naveenapps.expensemanager.di

import com.naveenapps.expensemanager.MainViewModel
import com.naveenapps.expensemanager.core.designsystem.components.CommonViewModelModule
import com.naveenapps.expensemanager.core.settings.di.CoreSettingsModule
import com.naveenapps.expensemanager.feature.account.di.AccountViewModelModule
import com.naveenapps.expensemanager.feature.analysis.di.AnalysisViewModelModule
import com.naveenapps.expensemanager.feature.budget.di.BudgetViewModelModule
import com.naveenapps.expensemanager.feature.category.di.CategoryViewModelModule
import com.naveenapps.expensemanager.feature.country.di.CountryViewModelModule
import com.naveenapps.expensemanager.feature.currency.di.CurrencyViewModelModule
import com.naveenapps.expensemanager.feature.dashboard.di.DashboardViewModelModule
import com.naveenapps.expensemanager.feature.export.di.ExportViewModelModule
import com.naveenapps.expensemanager.feature.filter.di.FilterViewModelModule
import com.naveenapps.expensemanager.feature.language.di.LanguageViewModelModule
import com.naveenapps.expensemanager.feature.onboarding.di.OnboardingViewModelModule
import com.naveenapps.expensemanager.feature.reminder.di.ReminderViewModelModule
import com.naveenapps.expensemanager.feature.recurring.di.RecurringViewModelModule
import com.naveenapps.expensemanager.feature.debt.di.DebtViewModelModule
import com.naveenapps.expensemanager.feature.savingsgoal.di.SavingsGoalViewModelModule
import com.naveenapps.expensemanager.feature.shoppinglist.di.ShoppingListViewModelModule
import com.naveenapps.expensemanager.feature.settings.di.SettingsViewModelModule
import com.naveenapps.expensemanager.feature.theme.di.ThemeViewModelModule
import com.naveenapps.expensemanager.feature.transaction.di.TransactionViewModelModule
import com.naveenapps.expensemanager.ui.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val MainViewModelModule = module {
    viewModel {
        MainViewModel(
            getCurrentThemeUseCase = get(),
            getOnboardingStatusUseCase = get(),
            settingsRepository = get(),
        )
    }
    viewModel {
        HomeViewModel(
            updateReminderStatusUseCase = get(),
            notificationScheduler = get()
        )
    }
}

val ViewModelModule = module {
    includes(
        MainViewModelModule,
        AccountViewModelModule,
        AnalysisViewModelModule,
        BudgetViewModelModule,
        CategoryViewModelModule,
        DashboardViewModelModule,
        TransactionViewModelModule,
        OnboardingViewModelModule,
        SettingsViewModelModule,
        ThemeViewModelModule,
        LanguageViewModelModule,
        ExportViewModelModule,
        ReminderViewModelModule,
        CurrencyViewModelModule,
        FilterViewModelModule,
        CountryViewModelModule,
        CommonViewModelModule,
        CoreSettingsModule,
        RecurringViewModelModule,
        DebtViewModelModule,
        SavingsGoalViewModelModule,
        ShoppingListViewModelModule,
    )
}
CLAUDE_EOF_19

echo "  app/src/main/kotlin/com/naveenapps/expensemanager/ui/HomeScreen.kt"
cat > "app/src/main/kotlin/com/naveenapps/expensemanager/ui/HomeScreen.kt" << 'CLAUDE_EOF_20'
package com.naveenapps.expensemanager.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.naveenapps.expensemanager.core.designsystem.utils.BackHandler
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.ActivityComponentProvider
import com.naveenapps.expensemanager.feature.account.create.AccountCreateScreen
import com.naveenapps.expensemanager.feature.account.list.AccountListScreen
import com.naveenapps.expensemanager.feature.account.reorder.AccountReOrderScreen
import com.naveenapps.expensemanager.feature.analysis.AnalysisScreen
import com.naveenapps.expensemanager.feature.budget.create.BudgetCreateScreen
import com.naveenapps.expensemanager.feature.budget.details.BudgetDetailScreen
import com.naveenapps.expensemanager.feature.budget.list.BudgetListScreen
import com.naveenapps.expensemanager.feature.category.create.CategoryCreateScreen
import com.naveenapps.expensemanager.feature.category.details.CategoryDetailScreen
import com.naveenapps.expensemanager.feature.category.list.CategoryListScreen
import com.naveenapps.expensemanager.feature.category.transaction.CategoryTransactionTabScreen
import com.naveenapps.expensemanager.feature.currency.CurrencyCustomiseScreen
import com.naveenapps.expensemanager.feature.dashboard.DashboardScreen
import com.naveenapps.expensemanager.feature.export.ExportScreen
import com.naveenapps.expensemanager.feature.onboarding.OnboardingScreen
import com.naveenapps.expensemanager.feature.onboarding.into.IntroScreen
import com.naveenapps.expensemanager.feature.reminder.ReminderScreen
import com.naveenapps.expensemanager.feature.recurring.create.RecurringTransactionCreateScreen
import com.naveenapps.expensemanager.feature.recurring.list.RecurringTransactionListScreen
import com.naveenapps.expensemanager.feature.debt.create.DebtCreateScreen
import com.naveenapps.expensemanager.feature.debt.list.DebtListScreen
import com.naveenapps.expensemanager.feature.savingsgoal.create.SavingsGoalCreateScreen
import com.naveenapps.expensemanager.feature.savingsgoal.list.SavingsGoalListScreen
import com.naveenapps.expensemanager.feature.shoppinglist.create.ShoppingListCreateScreen
import com.naveenapps.expensemanager.feature.shoppinglist.detail.ShoppingListDetailScreen
import com.naveenapps.expensemanager.feature.shoppinglist.list.ShoppingListListScreen
import com.naveenapps.expensemanager.feature.settings.SettingsScreen
import com.naveenapps.expensemanager.feature.settings.advanced.AdvancedSettingsScreen
import com.naveenapps.expensemanager.feature.transaction.create.TransactionCreateScreen
import com.naveenapps.expensemanager.feature.transaction.list.TransactionListScreen
import com.naveenapps.expensemanager.feature.transaction.search.TransactionSearchScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomePageNavHostContainer(
    backupRepository: ActivityComponentProvider,
    navHostController: NavHostController,
    landingScreen: ExpenseManagerScreens,
) {
    NavHost(
        navController = navHostController,
        startDestination = landingScreen,
    ) {
        this.expenseManagerNavigation(backupRepository)
    }
}

fun NavGraphBuilder.expenseManagerNavigation(
    componentProvider: ActivityComponentProvider,
) {
    composable<ExpenseManagerScreens.IntroScreen> {
        IntroScreen(componentProvider.getShareRepository())
    }
    composable<ExpenseManagerScreens.Onboarding> {
        OnboardingScreen()
    }
    composable<ExpenseManagerScreens.Home> {
        HomeScreen()
    }
    composable<ExpenseManagerScreens.CategoryList> {
        CategoryListScreen()
    }
    composable<ExpenseManagerScreens.CategoryCreate> {
        CategoryCreateScreen()
    }
    composable<ExpenseManagerScreens.CategoryDetails> {
        CategoryDetailScreen()
    }
    composable<ExpenseManagerScreens.TransactionList> {
        TransactionListScreen(showBackNavigationIcon = true)
    }
    composable<ExpenseManagerScreens.TransactionSearch> {
        TransactionSearchScreen()
    }
    composable<ExpenseManagerScreens.TransactionCreate> {
        TransactionCreateScreen(shareRepository = componentProvider.getShareRepository())
    }
    composable<ExpenseManagerScreens.AccountList> {
        AccountListScreen()
    }
    composable<ExpenseManagerScreens.AccountCreate> {
        AccountCreateScreen()
    }
    composable<ExpenseManagerScreens.BudgetList> {
        BudgetListScreen()
    }
    composable<ExpenseManagerScreens.BudgetCreate> {
        BudgetCreateScreen()
    }
    composable<ExpenseManagerScreens.BudgetDetails> {
        BudgetDetailScreen()
    }
    composable<ExpenseManagerScreens.AnalysisScreen> {
        AnalysisScreen()
    }
    composable<ExpenseManagerScreens.Settings> {
        SettingsScreen(
            shareRepository = componentProvider.getShareRepository(),
            backupRepository = componentProvider.getBackupRepository(),
        )
    }
    composable<ExpenseManagerScreens.ExportScreen> {
        ExportScreen()
    }
    composable<ExpenseManagerScreens.ReminderScreen> {
        ReminderScreen(
            shareRepository = componentProvider.getShareRepository()
        )
    }
    composable<ExpenseManagerScreens.CurrencyCustomiseScreen> {
        CurrencyCustomiseScreen()
    }
    composable<ExpenseManagerScreens.CategoryTransaction> {
        CategoryTransactionTabScreen()
    }
    composable<ExpenseManagerScreens.AdvancedSettingsScreen> {
        AdvancedSettingsScreen()
    }
    composable<ExpenseManagerScreens.AccountReOrderScreen> {
        AccountReOrderScreen()
    }
    composable<ExpenseManagerScreens.RecurringTransactionList> {
        RecurringTransactionListScreen()
    }
    composable<ExpenseManagerScreens.RecurringTransactionCreate> {
        RecurringTransactionCreateScreen()
    }
    composable<ExpenseManagerScreens.DebtList> {
        DebtListScreen()
    }
    composable<ExpenseManagerScreens.DebtCreate> {
        DebtCreateScreen()
    }
    composable<ExpenseManagerScreens.SavingsGoalList> {
        SavingsGoalListScreen()
    }
    composable<ExpenseManagerScreens.SavingsGoalCreate> {
        SavingsGoalCreateScreen()
    }
    composable<ExpenseManagerScreens.ShoppingListList> {
        ShoppingListListScreen()
    }
    composable<ExpenseManagerScreens.ShoppingListCreate> {
        ShoppingListCreateScreen()
    }
    composable<ExpenseManagerScreens.ShoppingListDetail> {
        ShoppingListDetailScreen()
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {

    val context = LocalActivity.current

    val homeScreenBottomBarItems by viewModel.homeScreenBottomBarItems.collectAsState()

    var hasNotificationPermission by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            hasNotificationPermission = it
            if (it) {
                viewModel.turnOnNotification()
            }
        }
    )

    LaunchedEffect(key1 = "permission") {
        if (hasNotificationPermission.not()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.turnOnNotification()
            }
        }
    }

    BackHandler {
        if (homeScreenBottomBarItems != HomeScreenBottomBarItems.Home) {
            viewModel.setUISystem(HomeScreenBottomBarItems.Home)
        } else {
            context?.finish()
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                HomeScreenBottomBarItems.entries.forEach { uiSystem ->
                    NavigationBarItem(
                        selected = homeScreenBottomBarItems == uiSystem,
                        onClick = { viewModel.setUISystem(uiSystem) },
                        icon = {
                            Icon(
                                painterResource(uiSystem.iconResourceID),
                                stringResource(uiSystem.labelResourceID),
                            )
                        },
                        label = { Text(stringResource(uiSystem.labelResourceID)) },
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(
                bottom = paddingValues.calculateBottomPadding(),
            ),
        ) {
            when (homeScreenBottomBarItems) {
                HomeScreenBottomBarItems.Home -> {
                    DashboardScreen()
                }

                HomeScreenBottomBarItems.Analysis -> {
                    AnalysisScreen()
                }

                HomeScreenBottomBarItems.Transaction -> {
                    TransactionListScreen()
                }

                HomeScreenBottomBarItems.Category -> {
                    CategoryTransactionTabScreen()
                }
            }
        }
    }
}
CLAUDE_EOF_20

echo "  core/navigation/src/main/kotlin/com/naveenapps/expensemanager/core/navigation/ExpenseManagerScreens.kt"
cat > "core/navigation/src/main/kotlin/com/naveenapps/expensemanager/core/navigation/ExpenseManagerScreens.kt" << 'CLAUDE_EOF_21'
package com.naveenapps.expensemanager.core.navigation

import kotlinx.serialization.Serializable

sealed class ExpenseManagerScreens {
    @Serializable
    data object IntroScreen : ExpenseManagerScreens()

    @Serializable
    data object Onboarding : ExpenseManagerScreens()

    @Serializable
    data object Home : ExpenseManagerScreens()

    @Serializable
    data object AccountList : ExpenseManagerScreens()

    @Serializable
    data object CategoryList : ExpenseManagerScreens()

    @Serializable
    data object BudgetList : ExpenseManagerScreens()

    @Serializable
    data object TransactionList : ExpenseManagerScreens()

    @Serializable
    data object TransactionSearch : ExpenseManagerScreens()

    @Serializable
    data object Settings : ExpenseManagerScreens()

    @Serializable
    data object CategoryTransaction : ExpenseManagerScreens()

    @Serializable
    data object AnalysisScreen : ExpenseManagerScreens()

    @Serializable
    data object ExportScreen : ExpenseManagerScreens()

    @Serializable
    data object ReminderScreen : ExpenseManagerScreens()

    @Serializable
    data object CurrencyCustomiseScreen : ExpenseManagerScreens()

    @Serializable
    data object AdvancedSettingsScreen : ExpenseManagerScreens()

    @Serializable
    data object AccountReOrderScreen : ExpenseManagerScreens()

    @Serializable
    data class AccountCreate(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data class CategoryCreate(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data class CategoryDetails(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data class BudgetCreate(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data class BudgetDetails(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data class TransactionCreate(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data object RecurringTransactionList : ExpenseManagerScreens()

    @Serializable
    data class RecurringTransactionCreate(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data object DebtList : ExpenseManagerScreens()

    @Serializable
    data class DebtCreate(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data object SavingsGoalList : ExpenseManagerScreens()

    @Serializable
    data class SavingsGoalCreate(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data object ShoppingListList : ExpenseManagerScreens()

    @Serializable
    data class ShoppingListCreate(val id: String?) : ExpenseManagerScreens()

    @Serializable
    data class ShoppingListDetail(val id: String?) : ExpenseManagerScreens()
}

object ExpenseManagerArgsNames {
    const val ID: String = "id"
}
CLAUDE_EOF_21

echo "  feature/settings/src/main/kotlin/com/naveenapps/expensemanager/feature/settings/SettingAction.kt"
cat > "feature/settings/src/main/kotlin/com/naveenapps/expensemanager/feature/settings/SettingAction.kt" << 'CLAUDE_EOF_22'
package com.naveenapps.expensemanager.feature.settings

import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.Category

sealed class SettingAction {

    data object ClosePage : SettingAction()

    data object OpenExport : SettingAction()

    data object OpenRateUs : SettingAction()

    data object OpenAdvancedSettings : SettingAction()


    data object OpenNotification : SettingAction()

    data object OpenRecurringTransactions : SettingAction()

    data object OpenDebts : SettingAction()

    data object OpenSavingsGoals : SettingAction()

    data object OpenShoppingLists : SettingAction()

    data object OpenCurrencyEdit : SettingAction()

    data object ShowThemeSelection : SettingAction()

    data object DismissThemeSelection : SettingAction()

    data object ShowLanguageSelection : SettingAction()

    data object DismissLanguageSelection : SettingAction()

    // Defaults section (moved here from AdvancedSettingAction)
    data class SelectAccount(val account: Account) : SettingAction()

    data class SelectExpenseCategory(val category: Category) : SettingAction()

    data class SelectIncomeCategory(val category: Category) : SettingAction()

    data object ToggleCompactSummary : SettingAction()

    // Data & Backup section
    data object Backup : SettingAction()

    data object Restore : SettingAction()

    // Security section
    data object ToggleAppLock : SettingAction()

    // Cloud backup section
    data object ConnectCloudBackup : SettingAction()

    data object DisconnectCloudBackup : SettingAction()
}
CLAUDE_EOF_22

echo "  feature/settings/src/main/kotlin/com/naveenapps/expensemanager/feature/settings/SettingsScreen.kt"
cat > "feature/settings/src/main/kotlin/com/naveenapps/expensemanager/feature/settings/SettingsScreen.kt" << 'CLAUDE_EOF_23'
package com.naveenapps.expensemanager.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.EditNotifications
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.designsystem.utils.AppPreviewsLightAndDarkMode
import com.naveenapps.expensemanager.core.common.utils.toCompleteDate
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingRow
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingToggleRow
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.designsystem.utils.ObserveAsEvents
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.repository.BackupRepository
import com.naveenapps.expensemanager.core.repository.ShareRepository
import com.naveenapps.expensemanager.feature.language.LanguageDialogView
import com.naveenapps.expensemanager.feature.theme.ThemeDialogView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    shareRepository: ShareRepository,
    backupRepository: BackupRepository,
    viewModel: SettingsViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsState()

    ObserveAsEvents(viewModel.event) {
        when (it) {
            SettingEvent.RateUs -> {
                shareRepository.openRateUs()
            }

            SettingEvent.Backup -> {
                backupRepository.backupData(null)
            }

            SettingEvent.Restore -> {
                backupRepository.restoreData(null)
            }
        }
    }

    SettingsScreenScaffoldView(
        state = state,
        onAction = viewModel::processAction
    )
}

@Composable
private fun SettingsScreenScaffoldView(
    state: SettingState,
    onAction: (SettingAction) -> Unit,
) {
    if (state.showThemeSelection) {
        ThemeDialogView {
            onAction.invoke(SettingAction.DismissThemeSelection)
        }
    }

    if (state.showLanguageSelection) {
        LanguageDialogView {
            onAction.invoke(SettingAction.DismissLanguageSelection)
        }
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = {
                    onAction.invoke(SettingAction.ClosePage)
                },
                title = stringResource(R.string.settings),
            )
        },
    ) { innerPadding ->
        SettingsScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    state: SettingState,
    onAction: (SettingAction) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(0.dp))

        // Personalization — shapes how the app looks and feels, so it comes first.
        SettingsSection(title = stringResource(R.string.personalization)) {
            AppCardView {
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.ShowThemeSelection) },
                    title = stringResource(id = R.string.theme),
                    subtitle = if (state.theme != null) {
                        stringResource(id = state.theme.titleResId)
                    } else {
                        stringResource(id = R.string.system_default)
                    },
                    icon = Icons.Outlined.Palette,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.ShowLanguageSelection) },
                    title = stringResource(
                        id = com.naveenapps.expensemanager.feature.language.R.string.language,
                    ),
                    subtitle = if (state.locale != null) {
                        stringResource(id = state.locale.titleResId)
                    } else {
                        stringResource(id = R.string.system_default)
                    },
                    icon = Icons.Outlined.Language,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenCurrencyEdit) },
                    title = stringResource(id = R.string.currency),
                    subtitle = "${state.currency.name} (${state.currency.symbol})",
                    icon = Icons.Outlined.Payments,
                )
            }
        }

        // Notifications
        SettingsSection(title = stringResource(R.string.notifications)) {
            AppCardView {
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenNotification) },
                    title = stringResource(id = R.string.reminder_notification),
                    subtitle = stringResource(id = R.string.selected_daily_reminder_time),
                    icon = Icons.Outlined.EditNotifications,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenRecurringTransactions) },
                    title = stringResource(id = R.string.recurring_transactions_settings),
                    subtitle = stringResource(id = R.string.recurring_transactions_settings_subtitle),
                    icon = Icons.Outlined.Autorenew,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenDebts) },
                    title = stringResource(id = R.string.debts_settings),
                    subtitle = stringResource(id = R.string.debts_settings_subtitle),
                    icon = Icons.Outlined.Handshake,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenSavingsGoals) },
                    title = stringResource(id = R.string.savings_goals_settings),
                    subtitle = stringResource(id = R.string.savings_goals_settings_subtitle),
                    icon = Icons.Outlined.Savings,
                    showDivider = true,
                )
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenShoppingLists) },
                    title = stringResource(id = R.string.shopping_lists_settings),
                    subtitle = stringResource(id = R.string.shopping_lists_settings_subtitle),
                    icon = Icons.Outlined.ShoppingCart,
                )
            }
        }

        // Defaults — affects daily data entry, so it's promoted out of "Advanced".
        SettingsSection(title = stringResource(R.string.defaults)) {
            AppCardView {
                if (state.accounts.isNotEmpty() && state.selectedAccount != null) {
                    DropdownSettingItem(
                        label = stringResource(id = R.string.default_account),
                        selectedValue = state.selectedAccount.name,
                        icon = Icons.Outlined.AccountBalance,
                        items = state.accounts.map { it.name },
                        onItemSelected = { index ->
                            onAction.invoke(SettingAction.SelectAccount(state.accounts[index]))
                        },
                    )
                }

                if (state.expenseCategories.isNotEmpty() && state.selectedExpenseCategory != null) {
                    DropdownSettingItem(
                        label = stringResource(id = R.string.default_expense_category),
                        selectedValue = state.selectedExpenseCategory.titleResId?.let {
                            stringResource(it)
                        } ?: state.selectedExpenseCategory.name,
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        items = state.expenseCategories.map {
                            it.titleResId?.let { resId -> stringResource(resId) } ?: it.name
                        },
                        onItemSelected = { index ->
                            onAction.invoke(SettingAction.SelectExpenseCategory(state.expenseCategories[index]))
                        },
                    )
                }

                if (state.incomeCategories.isNotEmpty() && state.selectedIncomeCategory != null) {
                    DropdownSettingItem(
                        label = stringResource(id = R.string.default_income_category),
                        selectedValue = state.selectedIncomeCategory.titleResId?.let {
                            stringResource(it)
                        } ?: state.selectedIncomeCategory.name,
                        icon = Icons.AutoMirrored.Outlined.TrendingDown,
                        items = state.incomeCategories.map {
                            it.titleResId?.let { resId -> stringResource(resId) } ?: it.name
                        },
                        onItemSelected = { index ->
                            onAction.invoke(SettingAction.SelectIncomeCategory(state.incomeCategories[index]))
                        },
                    )
                }
                SettingToggleRow(
                    title = stringResource(id = R.string.compact_summary),
                    subtitle = stringResource(id = R.string.compact_summary_message),
                    icon = Icons.Outlined.DashboardCustomize,
                    checked = state.isCompactSummary,
                    onCheckedChange = { onAction.invoke(SettingAction.ToggleCompactSummary) },
                )
            }
        }

        // Data & Backup — export lives here. Backup/Restore entries are hidden for now
        // (personal-use build); the underlying repository code is kept intact.
        SettingsSection(title = stringResource(R.string.data_and_backup)) {
            AppCardView {
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenExport) },
                    title = stringResource(id = R.string.export),
                    subtitle = stringResource(id = R.string.export_message),
                    icon = Icons.Outlined.Upload,
                )
            }
        }

        // Cloud backup — explicit, user-initiated connection (see SettingsViewModel /
        // GoogleAuthRepository). The app never prompts for this on its own; this switch is the
        // only place the very first sign-in can be triggered from.
        SettingsSection(title = stringResource(R.string.cloud_backup)) {
            AppCardView {
                SettingToggleRow(
                    title = stringResource(id = R.string.cloud_backup),
                    subtitle = when {
                        state.isCloudSyncInProgress -> stringResource(id = R.string.cloud_backup_syncing)
                        state.cloudBackupErrorMessage != null -> state.cloudBackupErrorMessage
                        state.isCloudBackupConnected && state.lastCloudBackupTime != null ->
                            stringResource(
                                id = R.string.cloud_backup_last_synced,
                                state.lastCloudBackupTime.toCompleteDate(),
                            )

                        state.isCloudBackupConnected -> stringResource(id = R.string.cloud_backup_connected)
                        else -> stringResource(id = R.string.cloud_backup_message)
                    },
                    icon = Icons.Outlined.CloudSync,
                    checked = state.isCloudBackupConnected,
                    onCheckedChange = {
                        // Ignored while a sync is already running rather than disabled outright
                        // (SettingToggleRow has no enabled/disabled visual state) — a tap here
                        // mid-sync is a no-op, not an error.
                        if (!state.isCloudSyncInProgress) {
                            onAction.invoke(
                                if (state.isCloudBackupConnected) {
                                    SettingAction.DisconnectCloudBackup
                                } else {
                                    SettingAction.ConnectCloudBackup
                                },
                            )
                        }
                    },
                )
            }
        }

        // Security
        SettingsSection(title = stringResource(R.string.security)) {
            AppCardView {
                SettingToggleRow(
                    title = stringResource(id = R.string.app_lock),
                    subtitle = stringResource(id = R.string.app_lock_message),
                    icon = Icons.Outlined.Lock,
                    checked = state.isAppLockEnabled,
                    onCheckedChange = { onAction.invoke(SettingAction.ToggleAppLock) },
                )
            }
        }

        // Advanced — only the genuinely rare, set-once-and-forget items live behind this now.
        SettingsSection(title = stringResource(R.string.advanced)) {
            AppCardView {
                SettingRow(
                    onClick = { onAction.invoke(SettingAction.OpenAdvancedSettings) },
                    title = stringResource(id = R.string.advanced),
                    subtitle = stringResource(id = R.string.advanced_config_message),
                    icon = Icons.Outlined.SettingsApplications,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Moved here from AdvancedSettingsScreen along with the Defaults section it belongs to.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingItem(
    label: String,
    selectedValue: String,
    icon: ImageVector,
    items: List<String>,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier.fillMaxWidth(),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            fontWeight = if (item == selectedValue) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = if (item == selectedValue) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else null,
                    onClick = {
                        onItemSelected(index)
                        expanded = false
                    },
                )
            }
        }
    }
}


@AppPreviewsLightAndDarkMode
@Composable
fun SettingsScreenPreview() {
    NaveenAppsPreviewTheme(padding = 0.dp) {
        SettingsScreenScaffoldView(
            state = SettingState(
                currency = Currency("$", "US Dollar"),
                theme = null,
                showThemeSelection = false
            ),
            onAction = {}
        )
    }
}
CLAUDE_EOF_23

echo "  feature/settings/src/main/kotlin/com/naveenapps/expensemanager/feature/settings/SettingsViewModel.kt"
cat > "feature/settings/src/main/kotlin/com/naveenapps/expensemanager/feature/settings/SettingsViewModel.kt" << 'CLAUDE_EOF_24'
package com.naveenapps.expensemanager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naveenapps.expensemanager.core.domain.usecase.account.GetAllAccountsUseCase
import com.naveenapps.expensemanager.core.domain.usecase.category.GetAllCategoryUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetDefaultCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.locale.GetCurrentLocaleUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.theme.GetCurrentThemeUseCase
import com.naveenapps.expensemanager.core.model.Account
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.isExpense
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.CloudBackupRepository
import com.naveenapps.expensemanager.core.repository.GoogleAuthRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class SettingsViewModel(
    getDefaultCurrencyUseCase: GetDefaultCurrencyUseCase,
    getCurrencyUseCase: GetCurrencyUseCase,
    getCurrentThemeUseCase: GetCurrentThemeUseCase,
    getCurrentLocaleUseCase: GetCurrentLocaleUseCase,
    getAllAccountsUseCase: GetAllAccountsUseCase,
    getAllCategoryUseCase: GetAllCategoryUseCase,
    private val settingsRepository: SettingsRepository,
    private val googleAuthRepository: GoogleAuthRepository,
    private val cloudBackupRepository: CloudBackupRepository,
    private val appComposeNavigator: AppComposeNavigator,
) : ViewModel() {

    private val _event = Channel<SettingEvent>()
    val event = _event.receiveAsFlow()

    private val _state = MutableStateFlow(
        SettingState(
            currency = getDefaultCurrencyUseCase.invoke(),
            theme = null,
            showThemeSelection = false
        )
    )
    val state = _state.asStateFlow()

    init {
        getCurrencyUseCase.invoke().onEach { currency ->
            _state.update { it.copy(currency = currency) }
        }.launchIn(viewModelScope)

        getCurrentThemeUseCase.invoke().onEach { theme ->
            _state.update { it.copy(theme = theme) }
        }.launchIn(viewModelScope)

        getCurrentLocaleUseCase.invoke().onEach { locale ->
            _state.update { it.copy(locale = locale) }
        }.launchIn(viewModelScope)

        // Defaults + Security section (moved here from AdvancedSettingsViewModel)
        settingsRepository.getHomeSummaryCompact().onEach { compact ->
            _state.update { it.copy(isCompactSummary = compact) }
        }.launchIn(viewModelScope)

        settingsRepository.isAppLockEnabled().onEach { enabled ->
            _state.update { it.copy(isAppLockEnabled = enabled) }
        }.launchIn(viewModelScope)

        getAllAccountsUseCase.invoke().onEach { accounts ->
            val accountId = settingsRepository.getDefaultAccount().firstOrNull()
            val account = accounts.find { it.id == accountId }
            _state.update {
                it.copy(
                    accounts = accounts,
                    selectedAccount = account ?: accounts.firstOrNull()
                )
            }
        }.launchIn(viewModelScope)

        getAllCategoryUseCase.invoke().onEach { categories ->
            val (expenses, incomes) = categories.partition { category -> category.type.isExpense() }

            val expenseCategoryId = settingsRepository.getDefaultExpenseCategory().firstOrNull()
            val expenseCategory = expenses.find { it.id == expenseCategoryId }

            val incomeCategoryId = settingsRepository.getDefaultIncomeCategory().firstOrNull()
            val incomeCategory = incomes.find { it.id == incomeCategoryId }

            _state.update {
                it.copy(
                    expenseCategories = expenses,
                    selectedExpenseCategory = expenseCategory ?: expenses.firstOrNull(),
                    incomeCategories = incomes,
                    selectedIncomeCategory = incomeCategory ?: incomes.firstOrNull(),
                )
            }
        }.launchIn(viewModelScope)

        googleAuthRepository.isSignedIn().onEach { isSignedIn ->
            _state.update { it.copy(isCloudBackupConnected = isSignedIn) }
            if (isSignedIn) {
                refreshLastCloudBackupTime()
            }
        }.launchIn(viewModelScope)
    }

    private fun refreshLastCloudBackupTime() {
        viewModelScope.launch {
            val lastBackupTime = cloudBackupRepository.getLastBackupTime()
            _state.update { it.copy(lastCloudBackupTime = lastBackupTime) }
        }
    }

    private fun connectCloudBackup() {
        viewModelScope.launch {
            _state.update { it.copy(isCloudSyncInProgress = true, cloudBackupErrorMessage = null) }

            when (val signInResult = googleAuthRepository.signInSilentlyOrPrompt()) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isCloudSyncInProgress = false,
                            cloudBackupErrorMessage = signInResult.exception.message,
                        )
                    }
                    return@launch
                }

                is Resource.Success -> Unit
            }

            // First connection: push whatever is already on this device right away, rather than
            // waiting for the person's next edit to trigger the debounced background sync (see
            // DatabaseChangeCloudBackupTrigger) — otherwise the cloud would stay empty until
            // something changes locally, which would be a confusing "connected but nothing there
            // yet" state.
            when (val syncResult = cloudBackupRepository.syncAll()) {
                is Resource.Error -> {
                    _state.update {
                        it.copy(
                            isCloudSyncInProgress = false,
                            cloudBackupErrorMessage = syncResult.exception.message,
                        )
                    }
                }

                is Resource.Success -> {
                    _state.update { it.copy(isCloudSyncInProgress = false) }
                    refreshLastCloudBackupTime()
                }
            }
        }
    }

    private fun disconnectCloudBackup() {
        viewModelScope.launch {
            googleAuthRepository.signOut()
            _state.update { it.copy(lastCloudBackupTime = null, cloudBackupErrorMessage = null) }
        }
    }

    private fun openCurrencyCustomiseScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.CurrencyCustomiseScreen)
    }

    private fun openExportScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ExportScreen)
    }

    private fun openNotificationScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ReminderScreen)
    }

    private fun openRecurringTransactionsScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.RecurringTransactionList)
    }

    private fun openDebtsScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.DebtList)
    }

    private fun openSavingsGoalsScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.SavingsGoalList)
    }

    private fun openShoppingListsScreen() {
        appComposeNavigator.navigate(ExpenseManagerScreens.ShoppingListList)
    }

    private fun closePage() {
        appComposeNavigator.popBackStack()
    }

    private fun openAdvancedSettings() {
        appComposeNavigator.navigate(ExpenseManagerScreens.AdvancedSettingsScreen)
    }

    private fun changeDefaultAccount(account: Account) {
        viewModelScope.launch {
            when (val response = settingsRepository.setDefaultAccount(account.id)) {
                is Resource.Error -> Unit
                is Resource.Success -> {
                    if (response.data) {
                        _state.update { it.copy(selectedAccount = account) }
                    }
                }
            }
        }
    }

    private fun changeSelectedExpenseCategory(category: Category) {
        viewModelScope.launch {
            when (val response = settingsRepository.setDefaultExpenseCategory(category.id)) {
                is Resource.Error -> Unit
                is Resource.Success -> {
                    if (response.data) {
                        _state.update { it.copy(selectedExpenseCategory = category) }
                    }
                }
            }
        }
    }

    private fun changeSelectedIncomeCategory(category: Category) {
        viewModelScope.launch {
            when (val response = settingsRepository.setDefaultIncomeCategory(category.id)) {
                is Resource.Error -> Unit
                is Resource.Success -> {
                    if (response.data) {
                        _state.update { it.copy(selectedIncomeCategory = category) }
                    }
                }
            }
        }
    }

    fun processAction(action: SettingAction) {
        when (action) {
            SettingAction.ClosePage -> closePage()
            SettingAction.OpenAdvancedSettings -> openAdvancedSettings()
            SettingAction.OpenCurrencyEdit -> openCurrencyCustomiseScreen()
            SettingAction.OpenExport -> openExportScreen()
            SettingAction.OpenNotification -> openNotificationScreen()
            SettingAction.OpenRecurringTransactions -> openRecurringTransactionsScreen()
            SettingAction.OpenDebts -> openDebtsScreen()
            SettingAction.OpenSavingsGoals -> openSavingsGoalsScreen()
            SettingAction.OpenShoppingLists -> openShoppingListsScreen()
            SettingAction.OpenRateUs -> {
                viewModelScope.launch {
                    _event.send(SettingEvent.RateUs)
                }
            }

            SettingAction.DismissThemeSelection -> {
                _state.update { it.copy(showThemeSelection = false) }
            }

            SettingAction.ShowThemeSelection -> {
                _state.update { it.copy(showThemeSelection = true) }
            }

            SettingAction.DismissLanguageSelection -> {
                _state.update { it.copy(showLanguageSelection = false) }
            }

            SettingAction.ShowLanguageSelection -> {
                _state.update { it.copy(showLanguageSelection = true) }
            }

            is SettingAction.SelectAccount -> changeDefaultAccount(action.account)

            is SettingAction.SelectExpenseCategory -> changeSelectedExpenseCategory(action.category)

            is SettingAction.SelectIncomeCategory -> changeSelectedIncomeCategory(action.category)

            SettingAction.ToggleCompactSummary -> {
                viewModelScope.launch {
                    val newValue = !_state.value.isCompactSummary
                    settingsRepository.setHomeSummaryCompact(newValue)
                    _state.update { it.copy(isCompactSummary = newValue) }
                }
            }

            SettingAction.ToggleAppLock -> {
                viewModelScope.launch {
                    val newValue = !_state.value.isAppLockEnabled
                    settingsRepository.setAppLockEnabled(newValue)
                    _state.update { it.copy(isAppLockEnabled = newValue) }
                }
            }

            SettingAction.Backup -> {
                viewModelScope.launch {
                    _event.send(SettingEvent.Backup)
                }
            }

            SettingAction.Restore -> {
                viewModelScope.launch {
                    _event.send(SettingEvent.Restore)
                }
            }

            SettingAction.ConnectCloudBackup -> connectCloudBackup()

            SettingAction.DisconnectCloudBackup -> disconnectCloudBackup()
        }
    }
}
CLAUDE_EOF_24

echo "  feature/settings/src/main/res/values/strings.xml"
cat > "feature/settings/src/main/res/values/strings.xml" << 'CLAUDE_EOF_25'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="settings">Settings</string>
    <string name="theme">Theme</string>
    <string name="analysis">Analysis</string>
    <string name="select_currency">Select currency</string>
    <string name="reminder_notification">Notification</string>
    <string name="info">Info</string>
    <string name="rate_us_message">It will help us to grow better</string>
    <string name="currency">Currency Customise</string>
    <string name="selected_daily_reminder_time">We will remind you to add your expense at every on time</string>
    <string name="recurring_transactions_settings">Recurring Transactions</string>
    <string name="recurring_transactions_settings_subtitle">Manage bills, subscriptions and other scheduled transactions</string>
    <string name="debts_settings">Debts</string>
    <string name="debts_settings_subtitle">Track money you\'ve lent or borrowed</string>
    <string name="savings_goals_settings">Savings goals</string>
    <string name="savings_goals_settings_subtitle">Track progress toward your savings targets</string>
    <string name="shopping_lists_settings">Shopping lists</string>
    <string name="shopping_lists_settings_subtitle">Plan purchases and record them as you shop</string>
    <string name="rate_us">Rate Us</string>
    <string name="choose_web_browser">Choose web browser</string>

    <string name="export">Export</string>
    <string name="export_message">Export your data and analyse in your preferred format</string>

    <string name="filter">Filter</string>
    <string name="filter_message">Can select the date filter to view expenses</string>

    <string name="developed_by">Developed by Naveen Kumar Kuppan</string>
    <string name="system_default">System Default</string>
    <string name="app_version">Version: %s</string>
    <string name="advanced">Advanced</string>
    <string name="advanced_config_message">Manage advanced configurations for the app</string>
    <string name="default_account">Default Account</string>
    <string name="default_expense_category">Default Expense Category</string>
    <string name="default_income_category">Default Income Category</string>
    <string name="accounts_re_order">Accounts Re-Order</string>
    <string name="accounts_re_order_message">Can change the order of the accounts list on the dashboard.</string>
    <string name="backup">Backup</string>
    <string name="backup_message">Create your data backup and restore them at any devices.</string>
    <string name="restore">Restore</string>
    <string name="restore_message">Restore your data to retrieve your data back into devices.</string>
    <string name="defaults">Defaults</string>
    <string name="others">Other Settings</string>

    <string name="personalization">Personalization</string>
    <string name="notifications">Notifications</string>
    <string name="data_and_backup"><![CDATA[Data & Backup]]></string>
    <string name="security">Security</string>
    <string name="cloud_backup">Cloud backup</string>
    <string name="cloud_backup_message">Connect Google to back up automatically and restore on a new phone</string>
    <string name="cloud_backup_connected">Connected — backing up automatically</string>
    <string name="cloud_backup_syncing">Syncing…</string>
    <string name="cloud_backup_last_synced">Last backed up %1$s</string>
    <string name="support">Support</string>

    <string name="compact_summary">Compact Summary</string>
    <string name="compact_summary_message">Show a smaller summary widget on the home screen</string>

    <string name="app_lock">App Lock</string>
    <string name="app_lock_message">Require biometric or device credentials when opening the app</string>
</resources>
CLAUDE_EOF_25

echo "  feature/settings/src/main/res/values-fr/strings.xml"
cat > "feature/settings/src/main/res/values-fr/strings.xml" << 'CLAUDE_EOF_26'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="settings">Paramètres</string>
    <string name="theme">Thème</string>
    <string name="analysis">Analyse</string>
    <string name="select_currency">Sélectionner la devise</string>
    <string name="reminder_notification">Notification</string>
    <string name="info">Infos</string>
    <string name="rate_us_message">Cela nous aidera à nous améliorer</string>
    <string name="currency">Personnaliser la devise</string>
    <string name="selected_daily_reminder_time">Nous vous rappellerons d\'ajouter votre dépense à l\'heure définie</string>
    <string name="recurring_transactions_settings">Transactions récurrentes</string>
    <string name="recurring_transactions_settings_subtitle">Gérez vos factures, abonnements et autres transactions planifiées</string>
    <string name="debts_settings">Dettes</string>
    <string name="debts_settings_subtitle">Suivez l\'argent prêté ou emprunté</string>
    <string name="savings_goals_settings">Objectifs d\'épargne</string>
    <string name="savings_goals_settings_subtitle">Suivez la progression de vos objectifs d\'épargne</string>
    <string name="shopping_lists_settings">Listes de courses</string>
    <string name="shopping_lists_settings_subtitle">Prépare tes achats et enregistre-les au fur et à mesure</string>
    <string name="rate_us">Évaluez-nous</string>
    <string name="choose_web_browser">Choisir un navigateur web</string>

    <string name="export">Exporter</string>
    <string name="export_message">Exportez vos données et analysez-les dans le format de votre choix</string>

    <string name="filter">Filtre</string>
    <string name="filter_message">Vous pouvez sélectionner le filtre de date pour afficher les dépenses</string>

    <string name="developed_by">Développé par Naveen Kumar Kuppan</string>
    <string name="system_default">Par défaut du système</string>
    <string name="app_version">Version : %s</string>
    <string name="advanced">Avancé</string>
    <string name="advanced_config_message">Gérer les configurations avancées de l\'application</string>
    <string name="default_account">Compte par défaut</string>
    <string name="default_expense_category">Catégorie de dépense par défaut</string>
    <string name="default_income_category">Catégorie de revenu par défaut</string>
    <string name="accounts_re_order">Réorganiser les comptes</string>
    <string name="accounts_re_order_message">Vous pouvez modifier l\'ordre de la liste des comptes sur le tableau de bord.</string>
    <string name="backup">Sauvegarde</string>
    <string name="backup_message">Créez une sauvegarde de vos données et restaurez-les sur n\'importe quel appareil.</string>
    <string name="restore">Restaurer</string>
    <string name="restore_message">Restaurez vos données pour les récupérer sur vos appareils.</string>
    <string name="defaults">Valeurs par défaut</string>
    <string name="others">Autres paramètres</string>

    <string name="personalization">Personnalisation</string>
    <string name="notifications">Notifications</string>
    <string name="data_and_backup"><![CDATA[Données & sauvegarde]]></string>
    <string name="security">Sécurité</string>
    <string name="cloud_backup">Sauvegarde cloud</string>
    <string name="cloud_backup_message">Connecte Google pour sauvegarder automatiquement et récupérer tes données sur un nouveau téléphone</string>
    <string name="cloud_backup_connected">Connecté — sauvegarde automatique active</string>
    <string name="cloud_backup_syncing">Synchronisation…</string>
    <string name="cloud_backup_last_synced">Dernière sauvegarde : %1$s</string>
    <string name="support">Assistance</string>

    <string name="compact_summary">Résumé compact</string>
    <string name="compact_summary_message">Afficher un widget de résumé plus petit sur l\'écran d\'accueil</string>

    <string name="app_lock">Verrouillage de l\'application</string>
    <string name="app_lock_message">Exiger une authentification biométrique ou les identifiants de l\'appareil à l\'ouverture de l\'application</string>
</resources>
CLAUDE_EOF_26

echo "  settings.gradle.kts"
cat > "settings.gradle.kts" << 'CLAUDE_EOF_27'
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "expensemanager"

include(":app")
include(":macrobenchmark")

include(":core:common")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:domain")
include(":core:model")
include(":core:navigation")
include(":core:notification")
include(":core:repository")
include(":core:testing")

include(":feature:account")
include(":feature:analysis")
include(":feature:budget")
include(":feature:category")
include(":feature:country")
include(":feature:currency")
include(":feature:dashboard")
include(":feature:filter")
include(":feature:export")
include(":feature:language")
include(":feature:onboarding")
include(":feature:reminder")
include(":feature:settings")
include(":feature:theme")
include(":feature:transaction")
include(":feature:recurring")
include(":feature:debt")
include(":feature:savings")
include(":feature:shopping")
include(":core:settings")
CLAUDE_EOF_27

echo ""
echo "Termine. Prochaine etape : git add -A && git commit -m \"Ajout fonctionnalite Liste de courses\" && git push"
