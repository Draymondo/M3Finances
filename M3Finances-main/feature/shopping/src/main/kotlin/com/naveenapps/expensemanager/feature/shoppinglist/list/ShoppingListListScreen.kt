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
