#!/data/data/com.termux/files/usr/bin/bash
# Correctif : ajoute les alertes de dépassement de budget (notification quotidienne
# groupée, seuils 80%/100%, réutilise GetBudgetsUseCase existant).
# A exécuter depuis la racine du repo (~/repo).
set -euo pipefail

if [ ! -f "settings.gradle.kts" ]; then
  echo "Erreur : lance ce script depuis la racine du repo (là où se trouve settings.gradle.kts)."
  exit 1
fi

echo "== Création des nouveaux fichiers =="

echo "  core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetNotificationHelper.kt"
mkdir -p "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget"
cat > "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetNotificationHelper.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.notification.budget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.naveenapps.expensemanager.core.notification.DESTINATION_CLASS
import com.naveenapps.expensemanager.core.notification.R

private const val CHANNEL_BUDGET_ALERTS = "BudgetAlerts"

/** Own channel, own on/off switch at the OS level — same reasoning as
 * `canShowDebtNotification`: budget alerts aren't gated behind the general daily-reminder
 * toggle in Settings. */
internal fun canShowBudgetNotification(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }
    return true
}

internal fun showBudgetNotification(
    context: Context,
    requestCode: Int,
    title: String,
    content: String,
) {
    if (!canShowBudgetNotification(context)) return

    createBudgetAlertChannelIfRequired(context)

    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val notificationIntent = Intent(context, Class.forName(DESTINATION_CLASS))
    notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

    val pendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        notificationIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
        .setContentTitle(title)
        .setContentText(content)
        .setAutoCancel(true)
        .setSound(alarmSound)
        .setSmallIcon(R.drawable.account_balance_wallet)
        .setContentIntent(pendingIntent)
        .build()

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(requestCode, notification)
}

private fun createBudgetAlertChannelIfRequired(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_BUDGET_ALERTS,
        CHANNEL_BUDGET_ALERTS,
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.budget_alert_channel_description)
        lightColor = Color.BLUE
    }
    NotificationManagerCompat.from(context).createNotificationChannel(channel)
}
CLAUDE_FIX_EOF

echo "  core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetAlertWorker.kt"
mkdir -p "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget"
cat > "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetAlertWorker.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.notification.budget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.common.utils.toMonthAndYearKey
import com.naveenapps.expensemanager.core.common.utils.toYear
import com.naveenapps.expensemanager.core.domain.usecase.budget.BudgetUiModel
import com.naveenapps.expensemanager.core.domain.usecase.budget.GetBudgetsUseCase
import com.naveenapps.expensemanager.core.model.BudgetPeriod
import com.naveenapps.expensemanager.core.notification.R
import kotlinx.coroutines.flow.first
import java.util.Date

/** Same fixed-id grouped-notification reasoning as `DebtOverdueWorker` — one summary
 * notification, not one per budget. */
private val BUDGET_ALERT_NOTIFICATION_REQUEST_CODE = "budget_alert_summary".hashCode()

/** A budget is only ever considered "close" or "exceeded" for its *current* period — a budget
 * created for a past month/year keeps whatever final percentage it ended on, and re-alerting on
 * that every day forever would be noise, not a warning. */
private fun BudgetUiModel.isCurrentPeriod(): Boolean {
    val now = Date()
    return if (periodType == BudgetPeriod.YEARLY) {
        selectedMonth == now.toYear()
    } else {
        selectedMonth == now.toMonthAndYearKey()
    }
}

private const val WARNING_THRESHOLD_PERCENT = 80f
private const val EXCEEDED_THRESHOLD_PERCENT = 100f

/**
 * Daily check (see `BudgetAlertScheduler`) — not triggered right when a transaction is added, to
 * keep this a simple, self-contained addition on top of the existing `GetBudgetsUseCase` percent
 * calculation rather than threading a new side effect through `AddTransactionUseCase`. Reminds
 * daily for as long as a budget stays over a threshold, same behavior as `DebtOverdueWorker` for
 * an unsettled debt — a budget that's still over 100% tomorrow is still worth mentioning.
 *
 * If any budget has exceeded 100%, that takes priority over ones merely approaching 80% — only
 * one notification is shown per day, so this avoids ever combining two different messages into
 * one string.
 */
class BudgetAlertWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val getBudgetsUseCase: GetBudgetsUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val currentBudgets = getBudgetsUseCase.invoke().first().filter { it.isCurrentPeriod() }

        val exceeded = currentBudgets.filter { it.percent >= EXCEEDED_THRESHOLD_PERCENT }
        val approaching = currentBudgets.filter {
            it.percent in WARNING_THRESHOLD_PERCENT until EXCEEDED_THRESHOLD_PERCENT
        }

        val (title, content) = when {
            exceeded.isNotEmpty() -> {
                applicationContext.getString(R.string.budget_exceeded_title) to
                    if (exceeded.size == 1) {
                        applicationContext.getString(
                            R.string.budget_exceeded_content_one,
                            exceeded.first().name,
                        )
                    } else {
                        applicationContext.getString(
                            R.string.budget_exceeded_content_other,
                            exceeded.size,
                        )
                    }
            }

            approaching.isNotEmpty() -> {
                applicationContext.getString(R.string.budget_approaching_title) to
                    if (approaching.size == 1) {
                        applicationContext.getString(
                            R.string.budget_approaching_content_one,
                            approaching.first().name,
                        )
                    } else {
                        applicationContext.getString(
                            R.string.budget_approaching_content_other,
                            approaching.size,
                        )
                    }
            }

            else -> return Result.success()
        }

        showBudgetNotification(
            context = applicationContext,
            requestCode = BUDGET_ALERT_NOTIFICATION_REQUEST_CODE,
            title = title,
            content = content,
        )

        return Result.success()
    }
}
CLAUDE_FIX_EOF

echo "  core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetAlertScheduler.kt"
mkdir -p "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget"
cat > "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetAlertScheduler.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.notification.budget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val BUDGET_ALERT_WORK_NAME = "budget_alert_daily_check"

/** Same shape as `RecurringTransactionScheduler.scheduleDailyCheck` — a true periodic check
 * (once a day) so a budget that tips over 80%/100% is still flagged even on a day the person
 * never opens the app. `KEEP` means calling this on every app start doesn't reset the timer. */
class BudgetAlertScheduler(
    private val context: Context,
) {

    fun scheduleDailyCheck() {
        val request = PeriodicWorkRequestBuilder<BudgetAlertWorker>(1, TimeUnit.DAYS).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BUDGET_ALERT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
CLAUDE_FIX_EOF

echo "  core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetNotificationModule.kt"
mkdir -p "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget"
cat > "core/notification/src/main/kotlin/com/naveenapps/expensemanager/core/notification/budget/BudgetNotificationModule.kt" << 'CLAUDE_FIX_EOF'
package com.naveenapps.expensemanager.core.notification.budget

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

val BudgetNotificationModule = module {
    single {
        BudgetAlertScheduler(
            context = androidContext(),
        )
    }
    worker {
        BudgetAlertWorker(
            context = androidContext(),
            workerParams = get(),
            getBudgetsUseCase = get(),
        )
    }
}
CLAUDE_FIX_EOF

echo
echo "== Application des correctifs sur les fichiers existants =="
python3 - << 'CLAUDE_PY_EOF'
import sys

EDITS = [
  (
    "core/notification/src/main/res/values/strings.xml",
    '    <string name="debt_overdue_content_other">%1$d debts are overdue and not yet settled</string>\n</resources>',
    '    <string name="debt_overdue_content_other">%1$d debts are overdue and not yet settled</string>\n    <string name="budget_alert_channel_description">Budget alerts</string>\n    <string name="budget_exceeded_title">Budget exceeded</string>\n    <string name="budget_exceeded_content_one">%1$s has exceeded its budget</string>\n    <string name="budget_exceeded_content_other">%1$d budgets have exceeded their limit</string>\n    <string name="budget_approaching_title">Budget almost reached</string>\n    <string name="budget_approaching_content_one">%1$s is close to its budget limit</string>\n    <string name="budget_approaching_content_other">%1$d budgets are close to their limit</string>\n</resources>',
  ),
  (
    "core/notification/src/main/res/values-fr/strings.xml",
    '    <string name="debt_overdue_content_other">%1$d dettes sont en retard et pas encore réglées</string>\n</resources>',
    '    <string name="debt_overdue_content_other">%1$d dettes sont en retard et pas encore réglées</string>\n    <string name="budget_alert_channel_description">Alertes budget</string>\n    <string name="budget_exceeded_title">Budget dépassé</string>\n    <string name="budget_exceeded_content_one">%1$s a dépassé son budget</string>\n    <string name="budget_exceeded_content_other">%1$d budgets ont dépassé leur limite</string>\n    <string name="budget_approaching_title">Budget bientôt atteint</string>\n    <string name="budget_approaching_content_one">%1$s approche de sa limite</string>\n    <string name="budget_approaching_content_other">%1$d budgets approchent de leur limite</string>\n</resources>',
  ),
  (
    "app/src/main/kotlin/com/naveenapps/expensemanager/initializer/KoinInitializer.kt",
    "import com.naveenapps.expensemanager.core.notification.recurringtransaction.RecurringTransactionNotificationModule\nimport com.naveenapps.expensemanager.core.notification.debt.DebtNotificationModule",
    "import com.naveenapps.expensemanager.core.notification.recurringtransaction.RecurringTransactionNotificationModule\nimport com.naveenapps.expensemanager.core.notification.debt.DebtNotificationModule\nimport com.naveenapps.expensemanager.core.notification.budget.BudgetNotificationModule",
  ),
  (
    "app/src/main/kotlin/com/naveenapps/expensemanager/initializer/KoinInitializer.kt",
    "                RecurringTransactionNotificationModule,\n                DebtNotificationModule,\n            )",
    "                RecurringTransactionNotificationModule,\n                DebtNotificationModule,\n                BudgetNotificationModule,\n            )",
  ),
  (
    "app/src/main/kotlin/com/naveenapps/expensemanager/initializer/AppInitializer.kt",
    "import com.naveenapps.expensemanager.core.notification.debt.DebtReminderTrigger",
    "import com.naveenapps.expensemanager.core.notification.debt.DebtReminderTrigger\nimport com.naveenapps.expensemanager.core.notification.budget.BudgetAlertScheduler",
  ),
  (
    "app/src/main/kotlin/com/naveenapps/expensemanager/initializer/AppInitializer.kt",
    "        val debtReminderScheduler: DebtReminderScheduler = GlobalContext.get().get()",
    "        val debtReminderScheduler: DebtReminderScheduler = GlobalContext.get().get()\n        val budgetAlertScheduler: BudgetAlertScheduler = GlobalContext.get().get()",
  ),
  (
    "app/src/main/kotlin/com/naveenapps/expensemanager/initializer/AppInitializer.kt",
    "            debtReminderTrigger.start()\n            debtReminderScheduler.scheduleOverdueDailyCheck()\n        }",
    "            debtReminderTrigger.start()\n            debtReminderScheduler.scheduleOverdueDailyCheck()\n\n            // Same daily-check pattern as the two schedulers above — see BudgetAlertWorker.\n            budgetAlertScheduler.scheduleDailyCheck()\n        }",
  ),
]

def main():
    ok = True
    for path, old, new in EDITS:
        with open(path, "r", encoding="utf-8") as fh:
            content = fh.read()
        if new in content:
            print(f"  [déjà appliqué] {path}")
            continue
        count = content.count(old)
        if count == 0:
            print(f"  [ERREUR] motif introuvable dans {path} — vérifie ce fichier à la main.")
            print("           Motif recherché : " + old.splitlines()[0][:80])
            ok = False
            continue
        if count > 1:
            print(f"  [ERREUR] motif trouvé {count} fois dans {path} (attendu 1), abandon pour ce fichier.")
            ok = False
            continue
        content = content.replace(old, new, 1)
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(content)
        print(f"  [ok] {path}")
    if not ok:
        print("\nCertains correctifs n'ont pas pu être appliqués automatiquement.")
        sys.exit(1)

if __name__ == "__main__":
    main()
CLAUDE_PY_EOF

echo
echo "Correctif appliqué. Étapes suivantes :"
echo "  git add -A"
echo "  git commit -m \"Ajout : alerte de dépassement de budget\""
echo "  git pull --rebase origin main"
echo "  git push"
