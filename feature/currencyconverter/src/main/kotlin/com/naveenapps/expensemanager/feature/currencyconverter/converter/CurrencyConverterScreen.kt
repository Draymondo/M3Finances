package com.naveenapps.expensemanager.feature.currencyconverter.converter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.naveenapps.designsystem.theme.NaveenAppsPreviewTheme
import com.naveenapps.expensemanager.core.designsystem.components.EmptyItem
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppCardView
import com.naveenapps.expensemanager.core.designsystem.ui.components.DecimalTextField
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.designsystem.ui.components.SettingRow
import com.naveenapps.expensemanager.core.model.Country
import com.naveenapps.expensemanager.core.model.CurrencyPosition
import com.naveenapps.expensemanager.core.model.TextFieldValue
import com.naveenapps.expensemanager.feature.country.CountryCurrencySelectionBottomSheet
import com.naveenapps.expensemanager.feature.country.CountrySelectionEvent
import com.naveenapps.expensemanager.feature.currencyconverter.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CurrencyConverterScreen(
    viewModel: CurrencyConverterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    CurrencyConverterScreenContent(
        state = state,
        onAction = viewModel::processAction,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CurrencyConverterScreenContent(
    state: CurrencyConverterState,
    onAction: (CurrencyConverterAction) -> Unit,
) {
    if (state.showFromCurrencySelection || state.showToCurrencySelection) {
        CountryCurrencySelectionBottomSheet(
            onEvent = { event ->
                when (event) {
                    CountrySelectionEvent.Dismiss ->
                        onAction.invoke(CurrencyConverterAction.DismissCurrencySelection)

                    is CountrySelectionEvent.CountrySelected -> {
                        if (state.showFromCurrencySelection) {
                            onAction.invoke(CurrencyConverterAction.SelectFromCurrency(event.country))
                        } else {
                            onAction.invoke(CurrencyConverterAction.SelectToCurrency(event.country))
                        }
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { onAction.invoke(CurrencyConverterAction.ClosePage) },
                title = stringResource(R.string.currency_converter),
                actions = {
                    IconButton(onClick = { onAction.invoke(CurrencyConverterAction.OpenSettings) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.currency_converter_settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!state.hasApiKey) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                EmptyItem(
                    emptyItemText = stringResource(R.string.no_api_key_configured),
                    icon = com.naveenapps.expensemanager.core.designsystem.R.drawable.ic_no_accounts,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        } else {
            CurrencyConverterBody(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                state = state,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun CurrencyConverterBody(
    state: CurrencyConverterState,
    onAction: (CurrencyConverterAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DecimalTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.amount.value,
            isError = state.amount.valueError,
            onValueChange = state.amount.onValueChange,
            label = R.string.amount,
            errorMessage = stringResource(R.string.amount_error),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SettingRow(
                    icon = Icons.Outlined.ArrowCircleUp,
                    title = stringResource(R.string.from_currency),
                    value = state.fromCurrency?.currencyCode
                        ?: stringResource(R.string.select_currency_placeholder),
                    onClick = { onAction.invoke(CurrencyConverterAction.ShowFromCurrencySelection) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingRow(
                    icon = Icons.Outlined.ArrowCircleDown,
                    title = stringResource(R.string.to_currency),
                    value = state.toCurrency?.currencyCode
                        ?: stringResource(R.string.select_currency_placeholder),
                    onClick = { onAction.invoke(CurrencyConverterAction.ShowToCurrencySelection) },
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(onClick = { onAction.invoke(CurrencyConverterAction.SwapCurrencies) }) {
                Icon(imageVector = Icons.Filled.SwapVert, contentDescription = stringResource(R.string.swap))
            }
        }

        AppCardView(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.height(32.dp))

                    state.result != null -> Text(
                        text = state.result,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    else -> Text(
                        text = stringResource(R.string.currency_converter_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.lastUpdated?.let { lastUpdated ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.last_updated,
                                formatLastUpdated(lastUpdated),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { onAction.invoke(CurrencyConverterAction.Refresh) }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.refresh),
                            )
                        }
                    }
                }
            }
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun formatLastUpdated(date: Date): String {
    // L'API ne rafraîchit ses taux qu'une fois par jour (minuit UTC) — l'heure serait donc
    // presque toujours "00:00" et n'apporterait aucune information utile, d'où la date seule.
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
}

@Preview
@Composable
private fun CurrencyConverterScreenPreview() {
    NaveenAppsPreviewTheme {
        CurrencyConverterScreenContent(
            state = CurrencyConverterState(
                amount = TextFieldValue(value = "100", valueError = false, onValueChange = {}),
                fromCurrency = Country(
                    name = "United States",
                    countryCode = "US",
                    currencyCode = "USD",
                    currency = com.naveenapps.expensemanager.core.model.Currency(
                        symbol = "$",
                        name = "US Dollar",
                        position = CurrencyPosition.PREFIX,
                    ),
                ),
                toCurrency = Country(
                    name = "Côte d'Ivoire",
                    countryCode = "CI",
                    currencyCode = "XOF",
                    currency = com.naveenapps.expensemanager.core.model.Currency(
                        symbol = "F",
                        name = "Franc CFA",
                        position = CurrencyPosition.SUFFIX,
                    ),
                ),
                result = "60 160 F",
                lastUpdated = Date(),
            ),
            onAction = {},
        )
    }
}
