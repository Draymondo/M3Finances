package com.naveenapps.expensemanager.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.naveenapps.expensemanager.feature.onboarding.OnboardingScreen
import com.naveenapps.expensemanager.feature.onboarding.into.IntroScreen
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
    composable<ExpenseManagerScreens.IntroScreen> {
        IntroScreen(componentProvider.getShareRepository())
    }
    composable<ExpenseManagerScreens.Onboarding> {
        OnboardingScreen()
    }
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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {

    val context = LocalActivity.current

    val homeScreenBottomBarItems by viewModel.homeScreenBottomBarItems.collectAsState()

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
        if (homeScreenBottomBarItems != HomeScreenBottomBarItems.Home) {
            viewModel.setUISystem(HomeScreenBottomBarItems.Home)
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

    Scaffold(
        bottomBar = {
            BottomAppBar {
                HomeScreenBottomBarItems.entries.forEach { uiSystem ->
                    NavigationBarItem(
                        selected = homeScreenBottomBarItems == uiSystem,
                        onClick = { viewModel.setUISystem(uiSystem) },
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
        Column(
            modifier = Modifier.padding(
                bottom = paddingValues.calculateBottomPadding(),
            ),
        ) {
            when (homeScreenBottomBarItems) {
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
