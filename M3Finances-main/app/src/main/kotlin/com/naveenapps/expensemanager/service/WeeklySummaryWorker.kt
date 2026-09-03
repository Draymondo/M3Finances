package com.naveenapps.expensemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetCurrencyUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.currency.GetFormattedAmountUseCase
import com.naveenapps.expensemanager.core.domain.usecase.transaction.GetTransactionWithFilterUseCase
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class WeeklySummaryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val geminiRepository: GeminiRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val getTransactionWithFilterUseCase: GetTransactionWithFilterUseCase by inject()
    private val getCurrencyUseCase: GetCurrencyUseCase by inject()
    private val getFormattedAmountUseCase: GetFormattedAmountUseCase by inject()

    override suspend fun doWork(): Result {
        val apiKey = settingsRepository.getGeminiApiKey().firstOrNull() ?: ""
        if (apiKey.isEmpty()) return Result.success()

        // Get transactions from the last 7 days
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = cal.timeInMillis

        val transactionsFlow = getTransactionWithFilterUseCase.invoke()
        val allTransactions = transactionsFlow.firstOrNull() ?: emptyList()
        val weeklyTransactions = allTransactions.filter { it.createdOn.time in startTime..endTime }

        if (weeklyTransactions.isEmpty()) return Result.success()

        val currency = getCurrencyUseCase.invoke().firstOrNull() ?: com.naveenapps.expensemanager.core.model.Currency(symbol = "FCFA", name = "Franc CFA")
        
        val totalIncome = weeklyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.amount }
        val totalExpense = weeklyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.amount }

        val incomeStr = getFormattedAmountUseCase.invoke(totalIncome, currency).amountString
        val expenseStr = getFormattedAmountUseCase.invoke(totalExpense, currency).amountString

        val text = "Total des entrées de la semaine: $incomeStr. Total des dépenses: $expenseStr. Fais un résumé amusant et encourageant en 2 phrases pour Ray (le dev de l'appli) pour clôturer sa semaine."
        
        val response = geminiRepository.generateMonthlyReport(text, apiKey)

        if (response is com.naveenapps.expensemanager.core.model.Resource.Success) {
            showNotification("Bilan de la Semaine 📊", response.data)
        }

        return Result.success()
    }

    private fun showNotification(title: String, content: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "weekly_summary",
                "Bilan Hebdomadaire",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, "weekly_summary")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // using a default icon for simplicity
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(1001, builder.build())
    }
}

