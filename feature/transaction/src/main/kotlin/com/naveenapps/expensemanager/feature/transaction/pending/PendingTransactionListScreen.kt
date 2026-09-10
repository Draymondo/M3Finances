package com.naveenapps.expensemanager.feature.transaction.pending

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.feature.transaction.R
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.naveenapps.expensemanager.core.designsystem.ui.components.AppDatePickerDialog
import com.naveenapps.expensemanager.core.common.utils.toCompleteDateWithDate
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionListScreen(
    viewModel: PendingTransactionListViewModel = koinViewModel()
) {
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()
    var transactionToPostpone by remember { mutableStateOf<PendingTransaction?>(null) }

    if (transactionToPostpone != null) {
        AppDatePickerDialog(
            selectedDate = transactionToPostpone?.scheduledDate ?: Date(),
            onDateSelected = { newDate ->
                transactionToPostpone?.let { tx ->
                    viewModel.postponeTransaction(tx.id, newDate)
                }
                transactionToPostpone = null
            },
            onDismiss = {
                transactionToPostpone = null
            }
        )
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationBackClick = viewModel::closePage,
                title = "Transactions en attente",
                navigationIcon = Icons.Default.ArrowBack
            )
        }
    ) { innerPadding ->
        if (pendingTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(text = "Aucune transaction en attente.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(pendingTransactions, key = { it.id }) { transaction ->
                    PendingTransactionItem(
                        transaction = transaction,
                        onClick = { viewModel.openTransactionCreate(transaction) },
                        onDelete = { viewModel.dismissTransaction(transaction.id) },
                        onPostpone = if (transaction.source == TransactionSource.SCHEDULED) {
                            { transactionToPostpone = transaction }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun PendingTransactionItem(
    transaction: PendingTransaction,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPostpone: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sourceLabel(transaction.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (transaction.source == TransactionSource.SCHEDULED) {
                    Text(
                        text = "Prévue le : ${(transaction.scheduledDate ?: transaction.date).toCompleteDateWithDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = transaction.merchant ?: "Inconnu",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Montant: ${transaction.amount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (transaction.fee != null) {
                    Text(
                        text = "Frais: ${transaction.fee}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Catégorie suggérée: ${transaction.suggestedCategory ?: "Aucune"}",
                    style = MaterialTheme.typography.bodySmall
                )
                val confidence = transaction.confidence
                if (confidence != null) {
                    Text(
                        text = "Confiance: ${(confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onPostpone != null) {
                IconButton(onClick = onPostpone) {
                    Icon(imageVector = Icons.Default.EditCalendar, contentDescription = "Reporter")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }
}

private fun sourceLabel(source: TransactionSource): String {
    return when (source) {
        TransactionSource.WAVE -> "🔵 Wave"
        TransactionSource.ORANGE_MONEY -> "🟠 Orange Money"
        TransactionSource.MTN_MOMO -> "🟡 MTN MoMo"
        TransactionSource.MOOV_MONEY -> "🟢 Moov Money"
        TransactionSource.DJAMO -> "🔷 Djamo"
        TransactionSource.PAYPAL -> "🔵 PayPal"
        TransactionSource.SMS -> "✉️ SMS"
        TransactionSource.SCHEDULED -> "📅 Prévue"
        TransactionSource.UNKNOWN -> "❓ Source inconnue"
    }
}