package com.naveenapps.expensemanager.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.TransactionSource
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Vérifie que chaque notification capturée est mise en file comme un travail WorkManager
 * distinct, y compris lorsque deux notifications ont exactement le même texte.
 */
class NotificationWorkEnqueuerTest {

    private val context: Context = mock()
    private val workManager: WorkManager = mock()

    @Test
    fun `two notifications with the exact same text are enqueued as two distinct work items`() {
        val enqueuer = NotificationWorkEnqueuer(context = context, workManager = workManager)

        val firstEventId = enqueueWaveNotification(enqueuer, "Vous avez reçu 5000 FCFA de Raymond")
        val secondEventId = enqueueWaveNotification(enqueuer, "Vous avez reçu 5000 FCFA de Raymond")

        assertThat(firstEventId).isNotEmpty()
        assertThat(secondEventId).isNotEmpty()
        assertThat(secondEventId).isNotEqualTo(firstEventId)

        val (uniqueNames, requests) = captureEnqueuedWork(expectedCount = 2)

        // Deux identités de travail distinctes pour deux événements distincts…
        assertThat(uniqueNames.toSet()).hasSize(2)
        // …et deux requêtes WorkManager réellement distinctes : aucune n'est écartée par le KEEP.
        assertThat(requests.map { it.id }.toSet()).hasSize(2)
    }

    @Test
    fun `two different notifications are both enqueued`() {
        val enqueuer = NotificationWorkEnqueuer(context = context, workManager = workManager)

        enqueueWaveNotification(enqueuer, "Vous avez reçu 5000 FCFA de Raymond")
        enqueueWaveNotification(enqueuer, "Paiement de 2000 FCFA accepté chez le marchand")

        val (uniqueNames, requests) = captureEnqueuedWork(expectedCount = 2)

        assertThat(uniqueNames.toSet()).hasSize(2)
        assertThat(requests.map { it.id }.toSet()).hasSize(2)
    }

    @Test
    fun `all notification information is carried over to the worker input data`() {
        val enqueuer = NotificationWorkEnqueuer(
            context = context,
            workManager = workManager,
            eventIdProvider = { FIXED_EVENT_ID },
            clock = { RECEIVED_AT },
        )

        val eventId = enqueueWaveNotification(enqueuer, NOTIFICATION_BODY)

        assertThat(eventId).isEqualTo(FIXED_EVENT_ID)

        val request = captureEnqueuedWork(expectedCount = 1).second.single()
        val input = request.workSpec.input

        assertThat(input.getString(NotificationProcessorWorker.KEY_EVENT_ID)).isEqualTo(FIXED_EVENT_ID)
        assertThat(input.getString(NotificationProcessorWorker.KEY_PACKAGE_NAME)).isEqualTo(WAVE_PACKAGE)
        assertThat(input.getString(NotificationProcessorWorker.KEY_SOURCE))
            .isEqualTo(TransactionSource.WAVE.name)
        assertThat(input.getString(NotificationProcessorWorker.KEY_TITLE)).isEqualTo("Wave")
        assertThat(input.getString(NotificationProcessorWorker.KEY_BODY)).isEqualTo(NOTIFICATION_BODY)
        assertThat(input.getString(NotificationProcessorWorker.KEY_BIG_TEXT)).isEqualTo(DEFAULT_BIG_TEXT)
        assertThat(input.getString(NotificationProcessorWorker.KEY_TEXT)).isEqualTo(
            NotificationProcessorWorker.assembleNotificationText("Wave", NOTIFICATION_BODY, DEFAULT_BIG_TEXT)
        )
        assertThat(input.getLong(NotificationProcessorWorker.KEY_RECEIVED_AT, -1L)).isEqualTo(RECEIVED_AT)
        assertThat(input.getLong(NotificationProcessorWorker.KEY_POST_TIME, -1L)).isEqualTo(POST_TIME)
        assertThat(input.getString(NotificationProcessorWorker.KEY_NOTIFICATION_KEY))
            .isEqualTo(NOTIFICATION_KEY)
    }

    @Test
    fun `network constraint, exponential backoff and tag are still applied`() {
        val enqueuer = NotificationWorkEnqueuer(context = context, workManager = workManager)

        enqueueWaveNotification(enqueuer, NOTIFICATION_BODY)

        val request = captureEnqueuedWork(expectedCount = 1).second.single()

        assertThat(request.workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
        assertThat(request.workSpec.backoffPolicy).isEqualTo(BackoffPolicy.EXPONENTIAL)
        assertThat(request.workSpec.backoffDelayDuration).isEqualTo(WorkRequest.MIN_BACKOFF_MILLIS)
        assertThat(request.tags).contains(NotificationWorkEnqueuer.TAG_NOTIFICATION_TRANSACTION)
    }

    private fun enqueueWaveNotification(enqueuer: NotificationWorkEnqueuer, body: String): String =
        enqueuer.enqueue(
            packageName = WAVE_PACKAGE,
            source = TransactionSource.WAVE,
            title = "Wave",
            body = body,
            bigText = DEFAULT_BIG_TEXT,
            postTime = POST_TIME,
            notificationKey = NOTIFICATION_KEY,
        )

    private fun captureEnqueuedWork(
        expectedCount: Int,
    ): Pair<List<String>, List<OneTimeWorkRequest>> {
        val uniqueNames = argumentCaptor<String>()
        val requests = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager, times(expectedCount)).enqueueUniqueWork(
            uniqueNames.capture(),
            eq(ExistingWorkPolicy.KEEP),
            requests.capture(),
        )
        return uniqueNames.allValues to requests.allValues
    }

    companion object {
        private const val WAVE_PACKAGE = "com.wave.personal"
        private const val FIXED_EVENT_ID = "event-42"
        private const val NOTIFICATION_BODY = "Vous avez reçu 5000 FCFA de Raymond"
        private const val DEFAULT_BIG_TEXT = "Wave • Transfert reçu"
        private const val RECEIVED_AT = 1_700_000_000_000L
        private const val POST_TIME = 1_699_999_999_999L
        private const val NOTIFICATION_KEY = "0|com.wave.personal|1|null|42"
    }
}
