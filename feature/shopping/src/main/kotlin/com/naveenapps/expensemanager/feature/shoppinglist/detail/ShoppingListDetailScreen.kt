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
