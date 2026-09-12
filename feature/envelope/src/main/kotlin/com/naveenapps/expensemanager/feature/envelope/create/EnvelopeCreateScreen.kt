package com.naveenapps.expensemanager.feature.envelope.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.DecimalTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.SafeModalBottomSheet
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingsSection
import com.naveenapps.expensemanager.core.designsystem.ui.components.StringTextField
import com.naveenapps.expensemanager.core.model.Category
import com.naveenapps.expensemanager.core.model.CategoryType
import com.naveenapps.expensemanager.core.model.Currency
import com.naveenapps.expensemanager.core.model.StoredIcon
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.feature.category.selection.CategoryItem
import com.naveenapps.expensemanager.feature.category.selection.CategoryItemDefaults
import com.naveenapps.expensemanager.feature.category.selection.CategorySelectionScreen
import com.naveenapps.expensemanager.feature.envelope.R
import java.util.Date
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EnvelopeCreateScreen(
    viewModel: EnvelopeCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    EnvelopeCreateScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun EnvelopeCreateScreenContent(
    state: EnvelopeCreateState,
    onAction: (EnvelopeCreateAction) -> Unit,
) {
    if (state.showDeleteDialog) {
        DeleteDialogItem(
            confirm = { onAction.invoke(EnvelopeCreateAction.Delete) },
            dismiss = { onAction.invoke(EnvelopeCreateAction.DismissDeleteDialog) },
        )
    } else if (state.showCategorySelection) {
        CategorySelectionView(state, onAction)
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(EnvelopeCreateAction.ClosePage) },
                title = if (state.showDeleteButton) {
                    stringResource(R.string.edit_envelope)
                } else {
                    stringResource(R.string.create_envelope)
                },
                actions = {
                    if (state.showDeleteButton) {
                        IconButton(
                            onClick = { onAction.invoke(EnvelopeCreateAction.ShowDeleteDialog) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_envelope),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction.invoke(EnvelopeCreateAction.Save) },
                icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                text = { Text(text = stringResource(R.string.save)) },
            )
        },
    ) { innerPadding ->
        EnvelopeCreateBody(
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
    state: EnvelopeCreateState,
    onAction: (EnvelopeCreateAction) -> Unit,
) {
    SafeModalBottomSheet(
        onDismissRequest = { onAction.invoke(EnvelopeCreateAction.DismissCategorySelection) },
    ) {
        CategorySelectionScreen(
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            onItemSelection = {
                onAction.invoke(EnvelopeCreateAction.SelectCategory(it.id))
            },
        )
    }
}

@Composable
private fun EnvelopeCreateBody(
    state: EnvelopeCreateState,
    onAction: (EnvelopeCreateAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.errorMessage?.let { message ->
            AppCardView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        SettingsSection(
            title = stringResource(R.string.category),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            val category = state.selectedCategory
            if (category != null) {
                CategoryItem(
                    name = category.titleResId?.let { stringResource(it) } ?: category.name,
                    icon = category.storedIcon.name,
                    iconBackgroundColor = category.storedIcon.backgroundColor,
                    modifier = Modifier.fillMaxWidth(),
                    border = CategoryItemDefaults.border(!state.categoryError),
                    onClick = {
                        focusManager.clearFocus(force = true)
                        onAction.invoke(EnvelopeCreateAction.ShowCategorySelection)
                    },
                    trailingContent = { CategoryItemDefaults.ChevronTrailing() },
                )
            } else {
                CategoryItem(
                    name = stringResource(R.string.select_category_placeholder),
                    icon = "category",
                    iconBackgroundColor = "#9E9E9E",
                    modifier = Modifier.fillMaxWidth(),
                    border = CategoryItemDefaults.border(!state.categoryError),
                    onClick = {
                        focusManager.clearFocus(force = true)
                        onAction.invoke(EnvelopeCreateAction.ShowCategorySelection)
                    },
                    trailingContent = { CategoryItemDefaults.ChevronTrailing() },
                )
            }
            if (state.categoryError) {
                Text(
                    text = stringResource(R.string.select_category_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
        }

        SettingsSection(title = stringResource(R.string.details)) {
            AppCardView {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DecimalTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.amount.value,
                        isError = state.amount.valueError,
                        onValueChange = state.amount.onValueChange,
                        label = R.string.envelope_amount,
                        errorMessage = stringResource(R.string.amount_error),
                    )

                    StringTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.name.value,
                        isError = false,
                        onValueChange = state.name.onValueChange,
                        label = R.string.envelope_name,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.envelope_monthly_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@AppPreviewsLightAndDarkMode
@Composable
private fun EnvelopeCreateScreenPreview() {
    NaveenAppsPreviewTheme {
        EnvelopeCreateScreenContent(
            state = EnvelopeCreateState(
                isEditing = false,
                name = TextFieldValue(value = "", valueError = false, onValueChange = {}),
                amount = TextFieldValue(value = "50000", valueError = false, onValueChange = {}),
                categories = emptyList(),
                selectedCategory = Category(
                    id = "1",
                    name = "Nourriture",
                    type = CategoryType.EXPENSE,
                    storedIcon = StoredIcon("restaurant", "#FF5722"),
                    createdOn = Date(),
                    updatedOn = Date(),
                ),
                categoryError = false,
                currency = Currency("F", "Franc CFA"),
                showDeleteButton = false,
                showDeleteDialog = false,
                showCategorySelection = false,
            ),
            onAction = {},
        )
    }
}
