package com.naveenapps.expensemanager

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.naveenapps.designsystem.theme.NaveenAppsTheme
import com.naveenapps.expensemanager.core.designsystem.utils.shouldUseDarkTheme
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.navigation.ExpenseManagerScreens
import com.naveenapps.expensemanager.core.repository.ActivityComponentProvider
import com.naveenapps.expensemanager.ui.AppLockScreen
import com.naveenapps.expensemanager.ui.DynamicAppTheme
import com.naveenapps.expensemanager.ui.MainScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.activityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope

internal class MainActivity : AppCompatActivity(), AndroidScopeComponent {

    private val appComposeNavigator: AppComposeNavigator by inject()

    override val scope: Scope by activityScope()

    private val viewModel: MainViewModel by viewModel()

    private val activityComponentProvider: ActivityComponentProvider by scope.inject()

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result: ActivityResult ->
        if (result.resultCode != RESULT_OK) {
            Log.i("App", "Update flow failed! Result code: " + result.resultCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Schedule Health Check Worker
        val healthWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.naveenapps.expensemanager.service.ServiceHealthWorker>(
            12, java.util.concurrent.TimeUnit.HOURS
        ).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ServiceHealthWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            healthWorkRequest
        )
        
        // Schedule Weekly AI Summary Worker
        val weeklyWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.naveenapps.expensemanager.service.WeeklySummaryWorker>(
            7, java.util.concurrent.TimeUnit.DAYS
        ).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeeklySummaryWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            weeklyWorkRequest
        )

        enableEdgeToEdge()

        activityComponentProvider.getBackupRepository()

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()
            val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
            val isAuthenticated by viewModel.isAuthenticated.collectAsState()
            val isDarkTheme = shouldUseDarkTheme(theme = currentTheme.mode)

            val showLock = isAppLockEnabled && !isAuthenticated

            if (showLock) {
                DynamicAppTheme(isDarkTheme = isDarkTheme) {
                    LaunchedEffect(Unit) { showBiometricPrompt() }
                    AppLockScreen(onUnlockClick = ::showBiometricPrompt)
                }
            } else {
                MainScreen(
                    composeNavigator = appComposeNavigator,
                    componentProvider = activityComponentProvider,
                    isDarkTheme = isDarkTheme,
                    landingScreen = landingScreenFromShortcut() ?: ExpenseManagerScreens.Home,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        launchAppUpdateCheck()
    }

    override fun onStop() {
        super.onStop()
        viewModel.lockApp()
    }

    /** Maps the `shortcut_destination` extra set by `res/xml/shortcuts.xml` to the NavHost's
     * start destination, so a long-press shortcut lands directly on that screen instead of
     * Home. `null` (no extra, or an unrecognised value) falls back to the normal Home landing. */
    private fun landingScreenFromShortcut(): ExpenseManagerScreens? {
        return when (intent?.getStringExtra(SHORTCUT_DESTINATION_EXTRA)) {
            "new_transaction" -> ExpenseManagerScreens.TransactionCreate(id = null)
            "debt_list" -> ExpenseManagerScreens.DebtList
            "shopping_list" -> ExpenseManagerScreens.ShoppingListList
            else -> null
        }
    }

    private fun showBiometricPrompt() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuthenticate = BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // Faille de type "échec ouvert" corrigée : on n'autorise plus l'accès.
            // On informe l'utilisateur ou on l'invite à configurer la sécurité de l'appareil.
            if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                val enrollIntent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                startActivity(enrollIntent)
                android.widget.Toast.makeText(this, "Veuillez configurer un code ou une empreinte dans les paramètres", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(this, "Authentification système indisponible", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

        val prompt = BiometricPrompt(
            /* activity = */ this,
            /* executor = */ ContextCompat.getMainExecutor(this),
            /* callback = */ object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.onAuthenticationSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User canceled or hardware error — lock screen stays visible for retry
                    Log.d("AppLock", "Auth error $errorCode: $errString")
                }

                override fun onAuthenticationFailed() {
                    // Biometric not recognised — lock screen stays visible for retry
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_lock_title))
            .setDescription(getString(R.string.app_lock_description))
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun launchAppUpdateCheck() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        // appUpdateInfo() is async (backed by a Play Core service call), so the result can land
        // after this Activity instance has already been destroyed — e.g. a config change like a
        // rotation or an in-app locale switch (AppCompatDelegate.setApplicationLocales triggers a
        // recreate). Once destroyed, activityResultLauncher is unregistered by the framework, so
        // calling launch() on it from a stale callback crashes with
        // "Attempting to launch an unregistered ActivityResultLauncher". Scoping the listener to
        // this Activity (rather than a plain addOnSuccessListener) makes Play Services drop the
        // callback automatically once the Activity stops, so it never fires against a destroyed
        // instance.
        appUpdateInfoTask.addOnSuccessListener(this) { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
                (appUpdateInfo.clientVersionStalenessDays() ?: -1) >= DAYS_FOR_FLEXIBLE_UPDATE
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activityResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                )
            }
        }
    }

    companion object {
        private const val DAYS_FOR_FLEXIBLE_UPDATE: Int = 3
        private const val SHORTCUT_DESTINATION_EXTRA = "shortcut_destination"
    }
}
