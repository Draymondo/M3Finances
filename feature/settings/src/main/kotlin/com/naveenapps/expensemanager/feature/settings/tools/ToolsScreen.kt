package com.naveenapps.expensemanager.feature.settings.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.feature.settings.R
import org.koin.compose.viewmodel.koinViewModel

// Group accent palette — tuned for the dark surface, keeps a distinct
// identity per section. (Full dynamic-color adaptation is a later stage.)
private val TrackingAccent = Color(0xFF6EA8FF)
private val TrackingAccentBg = Color(0xFF1D3355)
private val MoneyAccent = Color(0xFF54E391)
private val MoneyAccentBg = Color(0xFF123528)
private val PracticeAccent = Color(0xFFE8D97A)
private val PracticeAccentBg = Color(0xFF2C2C1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var query by remember { mutableStateOf("") }

    // Direct loops so stringResource() is called from the composable body,
    // not inside a non-composable lambda.
    val needle = query.trim().lowercase()
    val filteredGrouped = mutableListOf<Pair<ToolGroup, List<ToolItemUiModel>>>()
    for (group in ToolGroup.entries) {
        val matched = mutableListOf<ToolItemUiModel>()
        for (tool in state.tools) {
            if (tool.group != group) continue
            val title = stringResource(tool.titleRes)
            val aliases = tool.searchAliasRes
                ?.let { stringArrayResource(it).toList() }
                .orEmpty()
            val matches = needle.isEmpty() ||
                title.lowercase().contains(needle) ||
                aliases.any { it.lowercase().contains(needle) }
            if (matches) {
                matched.add(tool)
            }
        }
        if (matched.isNotEmpty()) {
            filteredGrouped.add(group to matched.sortedByDescending { it.usageCount })
        }
    }

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { viewModel.processAction(ToolsAction.ClosePage) },
                title = stringResource(id = R.string.tools),
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    placeholder = {
                        Text(text = stringResource(id = R.string.tools_search_placeholder))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    ),
                )
            }

            filteredGrouped.forEach { (group, items) ->
                item(key = "header_${group.name}") {
                    Text(
                        text = stringResource(group.titleRes()),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                }
                val rows = items.chunked(2)
                items(rows, key = { row -> "${group.name}_${row.joinToString("_") { it.type.name }}" }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { tool ->
                            ToolTile(
                                tool = tool,
                                onClick = { viewModel.processAction(ToolsAction.OpenTool(tool.type)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (filteredGrouped.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(id = R.string.tools_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun ToolGroup.titleRes(): Int = when (this) {
    ToolGroup.TRACKING -> R.string.tools_group_tracking
    ToolGroup.MONEY -> R.string.tools_group_money
    ToolGroup.PRACTICE -> R.string.tools_group_practice
}

private fun ToolGroup.accentColor(): Color = when (this) {
    ToolGroup.TRACKING -> TrackingAccent
    ToolGroup.MONEY -> MoneyAccent
    ToolGroup.PRACTICE -> PracticeAccent
}

private fun ToolGroup.accentBackground(): Color = when (this) {
    ToolGroup.TRACKING -> TrackingAccentBg
    ToolGroup.MONEY -> MoneyAccentBg
    ToolGroup.PRACTICE -> PracticeAccentBg
}

@Composable
private fun ToolTile(
    tool: ToolItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = tool.group.accentColor()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tool.group.accentBackground()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Pas de badge « NOUVEAU » : demande explicite, l'outil Paiements
            // échelonnés ne doit pas être mis en avant par un badge.
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(tool.titleRes),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(tool.subtitleRes),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 10.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
