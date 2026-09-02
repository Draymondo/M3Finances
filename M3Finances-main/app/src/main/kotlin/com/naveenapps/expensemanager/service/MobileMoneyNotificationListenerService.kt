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

class MobileMoneyNotificationListenerService : NotificationListenerService() {

    private val parseWaveNotificationUseCase: ParseWaveNotificationUseCase by inject()
    private val pendingTransactionRepository: PendingTransactionRepository by inject()

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

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

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
        scope.launch {
            when (val result = parseWaveNotificationUseCase.invoke(text, source)) {
                is Resource.Success -> {
                    val pendingTx = result.data
                    pendingTransactionRepository.addPendingTransaction(pendingTx)
                    Log.d("MobileMoneyNotification", "Saved Pending Transaction: \$pendingTx")
                }
                is Resource.Error -> {
                    Log.e("MobileMoneyNotification", "Error parsing Wave notification", result.exception)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

