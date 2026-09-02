package com.naveenapps.expensemanager.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class MobileMoneyNotificationListenerService : NotificationListenerService() {

    private val supportedPackageNames = setOf(
        "com.wave.personal",
        "com.djamo.app",
        "ci.moovmoney.mmpayapi",
        "com.mobiblanc.moov.mymoov_ci",
        "mtnft.momo.consumer",
        "com.orange.myorange.oci",
        "com.paypal.android.p2pmobile",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName
        if (packageName in supportedPackageNames) {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
            val bigText = extras.getString(Notification.EXTRA_BIG_TEXT) ?: ""

            val fullText = "$title\n$text\n$bigText".trim()

            if (fullText.isNotBlank()) {
                Log.d("MobileMoneyNotification", "Intercepted notification from: $packageName -> $fullText")
                processNotification(fullText, resolveSource(packageName))
            }
        }
    }

    private fun resolveSource(packageName: String): TransactionSource {
        return when (packageName) {
            "com.wave.personal" -> TransactionSource.WAVE
            "com.orange.myorange.oci" -> TransactionSource.ORANGE_MONEY
            "mtnft.momo.consumer" -> TransactionSource.MTN_MOMO
            "ci.moovmoney.mmpayapi", "com.mobiblanc.moov.mymoov_ci" -> TransactionSource.MOOV_MONEY
            "com.djamo.app" -> TransactionSource.DJAMO
            "com.paypal.android.p2pmobile" -> TransactionSource.PAYPAL
            "com.google.android.apps.messaging", "com.samsung.android.messaging" -> TransactionSource.SMS
            else -> TransactionSource.UNKNOWN
        }
    }

    private fun processNotification(text: String, source: TransactionSource) {
        val workData = workDataOf(
            NotificationProcessorWorker.KEY_TEXT to text,
            NotificationProcessorWorker.KEY_SOURCE to source.name
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationProcessorWorker>()
            .setConstraints(constraints)
            .setInputData(workData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // Create a unique name to prevent duplicate exact texts from piling up at the same millisecond
        val uniqueWorkName = "Tx_${text.hashCode()}"

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}

