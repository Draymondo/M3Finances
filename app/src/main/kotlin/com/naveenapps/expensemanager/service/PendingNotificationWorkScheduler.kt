package com.naveenapps.expensemanager.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Planifie la reprise des notifications en attente lorsque le réseau redevient disponible.
 *
 * Aucun polling, aucun timer, aucun service permanent : c'est WorkManager qui attend la
 * contrainte [NetworkType.CONNECTED] et réveille le traitement.
 *
 * Le travail est unique et `KEEP` : plusieurs notifications mises en file pendant la même panne
 * réseau ne lancent qu'une seule reprise.
 */
object PendingNotificationWorkScheduler {

    const val UNIQUE_WORK_NAME = "pending_notification_processing_resume"

    fun enqueueResume(context: Context) {
        val request = OneTimeWorkRequestBuilder<PendingNotificationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .addTag(TAG_PENDING_NOTIFICATION)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Tag permettant de retrouver le travail de reprise. */
    const val TAG_PENDING_NOTIFICATION = "pending_notification_processing"
}
