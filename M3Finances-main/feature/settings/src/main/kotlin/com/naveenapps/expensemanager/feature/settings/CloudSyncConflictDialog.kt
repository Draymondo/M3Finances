package com.naveenapps.expensemanager.feature.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun CloudSyncConflictDialog(
    onUseCloud: () -> Unit,
    onKeepLocal: () -> Unit,
    resolving: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = stringResource(R.string.cloud_sync_conflict_title)) },
        text = { Text(text = stringResource(R.string.cloud_sync_conflict_message)) },
        confirmButton = {
            TextButton(
                onClick = onUseCloud,
                enabled = !resolving,
            ) {
                Text(text = stringResource(R.string.cloud_sync_use_cloud))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onKeepLocal,
                enabled = !resolving,
            ) {
                Text(text = stringResource(R.string.cloud_sync_keep_phone))
            }
        },
    )
}
