@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.naveenapps.expensemanager.ui

import android.Manifest
import android.os.Build
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.naveenapps.expensemanager.R
import kotlinx.coroutines.launch
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.naveenapps.expensemanager.core.designsystem.utils.BackHandler
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.ActivityComponentProvider
import com.naveenapps.expensemanager.feature.account.create.AccountCreateScreen
import com.naveenapps.expensemanager.feature.account.list.AccountListScreen
import com.naveenapps.expensemanager.feature.account.reorder.AccountReOrderScreen
import com.naveenapps.expensemanager.feature.analysis.AnalysisScreen
import com.naveenapps.expensemanager.feature.budget.create.BudgetCreateScreen
import com.naveenapps.expensemanager.feature.budget.details.BudgetDetailScreen
import com.naveenapps.expensemanager.feature.budget.list.BudgetListScreen
import com.naveenapps.expensemanager.feature.category.create.CategoryCreateScreen
import com.naveenapps.expensemanager.feature.category.details.CategoryDetailScreen
import com.naveenapps.expensemanager.feature.category.list.CategoryListScreen
import com.naveenapps.expensemanager.feature.category.transaction.CategoryTransactionTabScreen
import com.naveenapps.expensemanager.feature.currency.CurrencyCustomiseScreen
import com.naveenapps.expensemanager.feature.dashboard.DashboardScreen
import com.naveenapps.expensemanager.feature.export.ExportScreen
import com.naveenapps.expensemanager.feature.reminder.ReminderScreen
import com.naveenapps.expensemanager.feature.recurring.create.RecurringTransactionCreateScreen
import com.naveenapps.expensemanager.feature.recurring.list.RecurringTransactionListScreen
import com.naveenapps.expensemanager.feature.debt.create.DebtCreateScreen
import com.naveenapps.expensemanager.feature.debt.list.DebtListScreen
import com.naveenapps.expensemanager.feature.savingsgoal.create.SavingsGoalCreateScreen
import com.naveenapps.expensemanager.feature.savingsgoal.list.SavingsGoalListScreen
import com.naveenapps.expensemanager.feature.shoppinglist.create.ShoppingListCreateScreen
import com.naveenapps.expensemanager.feature.shoppinglist.detail.ShoppingListDetailScreen
import com.naveenapps.expensemanager.feature.shoppinglist.list.ShoppingListListScreen
import com.naveenapps.expensemanager.feature.settings.CloudSyncConflictDialog
import com.naveenapps.expensemanager.feature.settings.SettingsScreen
import com.naveenapps.expensemanager.feature.settings.advanced.AdvancedSettingsScreen
import com.naveenapps.expensemanager.feature.transaction.create.TransactionCreateScreen
import com.naveenapps.expensemanager.feature.transaction.list.TransactionListScreen
import com.naveenapps.expensemanager.feature.transaction.search.TransactionSearchScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomePageNavHostContainer(
    backupRepository: ActivityComponentProvider,
    navHostController: NavHostController,
    landingScreen: ExpenseManagerScreens,
) {
    NavHost(
        navController = navHostController,
        startDestination = landingScreen,
    ) {
        this.expenseManagerNavigation(backupRepository)
    }
}

fun NavGraphBuilder.expenseManagerNavigation(
    componentProvider: ActivityComponentProvider,
) {
    composable<ExpenseManagerScreens.Home> {
        HomeScreen()
    }
    composable<ExpenseManagerScreens.CategoryList> {
        CategoryListScreen()
    }
    composable<ExpenseManagerScreens.CategoryCreate> {
        CategoryCreateScreen()
    }
    composable<ExpenseManagerScreens.CategoryDetails> {
        CategoryDetailScreen()
    }
    composable<ExpenseManagerScreens.TransactionList> {
        TransactionListScreen(showBackNavigationIcon = true)
    }
    composable<ExpenseManagerScreens.PendingTransactionList> {
        com.naveenapps.expensemanager.feature.transaction.pending.PendingTransactionListScreen()
    }
    composable<ExpenseManagerScreens.Chat> {
        com.naveenapps.expensemanager.feature.transaction.chat.ChatScreen()
    }
    composable<ExpenseManagerScreens.TransactionSearch> {
        TransactionSearchScreen()
    }
    composable<ExpenseManagerScreens.TransactionCreate> {
        TransactionCreateScreen(shareRepository = componentProvider.getShareRepository())
    }
    composable<ExpenseManagerScreens.AccountList> {
        AccountListScreen()
    }
    composable<ExpenseManagerScreens.AccountCreate> {
        AccountCreateScreen()
    }
    composable<ExpenseManagerScreens.BudgetList> {
        BudgetListScreen()
    }
    composable<ExpenseManagerScreens.BudgetCreate> {
        BudgetCreateScreen()
    }
    composable<ExpenseManagerScreens.BudgetDetails> {
        BudgetDetailScreen()
    }
    composable<ExpenseManagerScreens.AnalysisScreen> {
        AnalysisScreen()
    }
    composable<ExpenseManagerScreens.Settings> {
        SettingsScreen(
            shareRepository = componentProvider.getShareRepository(),
            backupRepository = componentProvider.getBackupRepository(),
        )
    }
    composable<ExpenseManagerScreens.ExportScreen> {
        ExportScreen()
    }
    composable<ExpenseManagerScreens.ReminderScreen> {
        ReminderScreen(
            shareRepository = componentProvider.getShareRepository()
        )
    }
    composable<ExpenseManagerScreens.CurrencyCustomiseScreen> {
        CurrencyCustomiseScreen()
    }
    composable<ExpenseManagerScreens.CategoryTransaction> {
        CategoryTransactionTabScreen()
    }
    composable<ExpenseManagerScreens.AdvancedSettingsScreen> {
        AdvancedSettingsScreen()
    }
    composable<ExpenseManagerScreens.AccountReOrderScreen> {
        AccountReOrderScreen()
    }
    composable<ExpenseManagerScreens.RecurringTransactionList> {
        RecurringTransactionListScreen()
    }
    composable<ExpenseManagerScreens.RecurringTransactionCreate> {
        RecurringTransactionCreateScreen()
    }
    composable<ExpenseManagerScreens.DebtList> {
        DebtListScreen()
    }
    composable<ExpenseManagerScreens.DebtCreate> {
        DebtCreateScreen()
    }
    composable<ExpenseManagerScreens.SavingsGoalList> {
        SavingsGoalListScreen()
    }
    composable<ExpenseManagerScreens.SavingsGoalCreate> {
        SavingsGoalCreateScreen()
    }
    composable<ExpenseManagerScreens.ShoppingListList> {
        ShoppingListListScreen()
    }
    composable<ExpenseManagerScreens.ShoppingListCreate> {
        ShoppingListCreateScreen()
    }
    composable<ExpenseManagerScreens.ShoppingListDetail> {
        ShoppingListDetailScreen()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {

    val context = LocalActivity.current

    val homeScreenBottomBarItems by viewModel.homeScreenBottomBarItems.collectAsState()
    val pages = HomeScreenBottomBarItems.entries
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(homeScreenBottomBarItems).coerceAtLeast(0),
        pageCount = { pages.size }
    )

    val coroutineScope = rememberCoroutineScope()

    // Sync: when user swipes pager → update ViewModel ONLY when the swipe settles
    LaunchedEffect(pagerState.settledPage) {
        val currentItem = pages[pagerState.settledPage]
        if (homeScreenBottomBarItems != currentItem) {
            viewModel.setUISystem(currentItem)
        }
    }

    var hasNotificationPermission by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            hasNotificationPermission = it
            if (it) {
                viewModel.turnOnNotification()
            }
        }
    )

    LaunchedEffect(key1 = "permission") {
        if (hasNotificationPermission.not()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.turnOnNotification()
            }
        }
    }

    BackHandler {
        if (pagerState.currentPage != 0) {
            viewModel.setUISystem(HomeScreenBottomBarItems.Home)
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        } else {
            context?.finish()
        }
    }

    val showCloudSyncConflict by viewModel.showCloudSyncConflict.collectAsState()
    val isCloudConflictResolving by viewModel.isCloudConflictResolving.collectAsState()
    if (showCloudSyncConflict) {
        CloudSyncConflictDialog(
            onUseCloud = viewModel::useCloudData,
            onKeepLocal = viewModel::keepPhoneData,
            resolving = isCloudConflictResolving,
        )
    }

    var isListenerEnabled by remember(context) {
        mutableStateOf(
            if (context != null) {
                androidx.core.app.NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
            } else true
        )
    }
    var isDismissedForSession by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && context != null) {
                val enabled = androidx.core.app.NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
                isListenerEnabled = enabled
                if (enabled) {
                    isDismissedForSession = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar {
                pages.forEachIndexed { index, uiSystem ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { 
                            viewModel.setUISystem(uiSystem)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = {
                            Icon(
                                painterResource(uiSystem.iconResourceID),
                                stringResource(uiSystem.labelResourceID),
                            )
                        },
                        label = { Text(stringResource(uiSystem.labelResourceID)) },
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
            AnimatedVisibility(
                visible = !isListenerEnabled && !isDismissedForSession,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                NotificationListenerWarningCard(
                    onRepairClick = {
                        try {
                            context?.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        } catch (e: Exception) {
                            // Ignored if intent is not available
                        }
                    },
                    onDismissClick = {
                        isDismissedForSession = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 3,
            ) { page ->
                when (pages[page]) {
                    HomeScreenBottomBarItems.Home -> {
                        DashboardScreen()
                    }

                    HomeScreenBottomBarItems.Analysis -> {
                        AnalysisScreen()
                    }

                    HomeScreenBottomBarItems.Transaction -> {
                        TransactionListScreen()
                    }

                    HomeScreenBottomBarItems.Category -> {
                        CategoryTransactionTabScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationListenerWarningCard(
    onRepairClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.notification_listener_warning_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onDismissClick,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.notification_listener_warning_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onRepairClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.notification_listener_warning_action),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

