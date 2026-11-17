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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naveenapps.expensemanager.core.designsystem.ui.components.ExpenseManagerTopAppBar
import com.naveenapps.expensemanager.core.model.ToolType
import com.naveenapps.expensemanager.feature.settings.R
import org.koin.compose.viewmodel.koinViewModel

/**
 * Écran Outils : une recherche en haut, trois groupes sémantiques et une grille
 * à deux colonnes (~9 outils visibles par écran au lieu de 5).
 *
 * Volontairement **aucun badge « NOUVEAU »** : un outil qui a plus d'un mois ne
 * doit pas clignoter indéfiniment, et l'ordre reste stable à l'intérieur d'un
 * groupe pour que l'œil retrouve ses repères.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            ExpenseManagerTopAppBar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationBackClick = { viewModel.processAction(ToolsAction.ClosePage) },
                title = stringResource(id = R.string.tools),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ToolsSearchField(
                query = state.searchQuery,
                onQueryChange = {
                    viewModel.processAction(ToolsAction.UpdateSearchQuery(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            val query = state.searchQuery.trim()
            // `filter` est inline : `stringResource` reste dans le scope de
            // composition et peut donc résoudre les libellés localisés.
            val visibleTools = state.tools.filter { tool ->
                query.isBlank() ||
                    stringResource(tool.titleRes).contains(query, ignoreCase = true) ||
                    stringResource(tool.subtitleRes).contains(query, ignoreCase = true)
            }

            if (visibleTools.isEmpty()) {
                NoToolFound(
                    query = query,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 24.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ToolGroup.entries.forEach { group ->
                        val tools = visibleTools.filter { it.type.group() == group }
                        if (tools.isNotEmpty()) {
                            item(
                                key = "header_${group.name}",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                ToolGroupHeader(group = group)
                            }
                            items(items = tools, key = { it.type.name }) { tool ->
                                ToolTile(
                                    tool = tool,
                                    group = group,
                                    onClick = {
                                        viewModel.processAction(ToolsAction.OpenTool(tool.type))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
//  Groupes
// ═══════════════════════════════════════════════════════════════════════

private enum class ToolGroup(
    val titleRes: Int,
    val accent: Color,
) {
    PLANNING(R.string.tools_group_planning, Color(0xFF3B7DFF)),
    MONEY(R.string.tools_group_money, Color(0xFF1FA971)),
    UTILITY(R.string.tools_group_utility, Color(0xFFB7791F)),
}

private fun ToolType.group(): ToolGroup = when (this) {
    ToolType.SCHEDULED_TRANSACTIONS,
    ToolType.CALENDAR,
    ToolType.RECURRING_TRANSACTIONS,
    ToolType.SHOPPING_LISTS -> ToolGroup.PLANNING

    ToolType.SAVINGS_GOALS,
    ToolType.DEBTS,
    ToolType.ENVELOPES -> ToolGroup.MONEY

    ToolType.CURRENCY_CONVERTER,
    ToolType.WORK_TIME -> ToolGroup.UTILITY
}

// ═══════════════════════════════════════════════════════════════════════
//  Composants
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ToolsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = stringResource(id = R.string.search_tools_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null,
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun ToolGroupHeader(
    group: ToolGroup,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(group.accent),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(group.titleRes).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ToolTile(
    tool: ToolItemUiModel,
    group: ToolGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(15.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(group.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = group.accent,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(tool.titleRes),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = (-0.1).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = stringResource(tool.subtitleRes),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NoToolFound(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.no_tool_matches, query),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
