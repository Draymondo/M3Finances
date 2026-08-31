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
import com.naveenapps.expensemanager.feature.transaction.R
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionListScreen(
    viewModel: PendingTransactionListViewModel = koinViewModel()
) {
    val pendingTransactions by viewModel.pendingTransactions.collectAsState()

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
                        onDelete = { viewModel.dismissTransaction(transaction.id) }
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
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }
}

