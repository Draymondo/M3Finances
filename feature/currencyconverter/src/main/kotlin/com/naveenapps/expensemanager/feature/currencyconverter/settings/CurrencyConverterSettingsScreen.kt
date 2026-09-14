package com.naveenapps.expensemanager.feature.currencyconverter.settings

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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.StringTextField
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.feature.currencyconverter.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CurrencyConverterSettingsScreen(
    viewModel: CurrencyConverterSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    CurrencyConverterSettingsScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
private fun CurrencyConverterSettingsScreenContent(
    state: CurrencyConverterSettingsState,
    onAction: (CurrencyConverterSettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(CurrencyConverterSettingsAction.ClosePage) },
                title = stringResource(R.string.currency_converter_settings),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction.invoke(CurrencyConverterSettingsAction.Save) },
                icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                text = { Text(text = stringResource(R.string.save)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCardView {
                Column(modifier = Modifier.padding(16.dp)) {
                    StringTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.apiKey.value,
                        isError = state.apiKey.valueError,
                        onValueChange = state.apiKey.onValueChange,
                        label = R.string.api_key,
                        errorMessage = state.errorMessage ?: stringResource(R.string.api_key_error),
                    )
                }
            }

            Text(
                text = stringResource(R.string.api_key_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun CurrencyConverterSettingsScreenPreview() {
    NaveenAppsPreviewTheme {
        CurrencyConverterSettingsScreenContent(
            state = CurrencyConverterSettingsState(
                apiKey = TextFieldValue(value = "", valueError = false, onValueChange = {}),
            ),
            onAction = {},
        )
    }
}
