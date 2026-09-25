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
    fun `two identical notifications received at different times are enqueued separately`() {
        val enqueuer = NotificationWorkEnqueuer(context = context, workManager = workManager)

        val firstEventId = enqueuer.enqueue(
            packageName = WAVE_PACKAGE,
            source = TransactionSource.WAVE,
            title = "Wave",
            body = "Vous avez reçu 5000 FCFA de Raymond",
            bigText = DEFAULT_BIG_TEXT,
            postTime = POST_TIME,
            notificationKey = NOTIFICATION_KEY,
        )
        val secondEventId = enqueuer.enqueue(
            packageName = WAVE_PACKAGE,
            source = TransactionSource.WAVE,
            title = "Wave",
            body = "Vous avez reçu 5000 FCFA de Raymond",
            bigText = DEFAULT_BIG_TEXT,
            postTime = POST_TIME + 1_000L,
            notificationKey = NOTIFICATION_KEY,
        )

        assertThat(firstEventId).isNotEmpty()
        assertThat(secondEventId).isNotEmpty()
        assertThat(secondEventId).isNotEqualTo(firstEventId)

        val (uniqueNames, requests) = captureEnqueuedWork(expectedCount = 2)
        assertThat(uniqueNames.toSet()).hasSize(2)
        assertThat(requests.map { it.id }.toSet()).hasSize(2)
    }

    @Test
    fun `same Android notification replay gets the same work identity`() {
        val enqueuer = NotificationWorkEnqueuer(context = context, workManager = workManager)

        val firstEventId = enqueuer.enqueue(
            packageName = WAVE_PACKAGE,
            source = TransactionSource.WAVE,
            title = "Wave",
            body = "Vous avez reçu 5000 FCFA de Raymond",
            bigText = DEFAULT_BIG_TEXT,
            postTime = POST_TIME,
            notificationKey = NOTIFICATION_KEY,
        )
        val replayEventId = enqueuer.enqueue(
            packageName = WAVE_PACKAGE,
            source = TransactionSource.WAVE,
            title = "Wave",
            body = "Vous avez reçu 5000 FCFA de Raymond",
            bigText = DEFAULT_BIG_TEXT,
            postTime = POST_TIME,
            notificationKey = NOTIFICATION_KEY,
        )

        assertThat(replayEventId).isEqualTo(firstEventId)
        val uniqueNames = captureEnqueuedWork(expectedCount = 2).first
        assertThat(uniqueNames).containsExactly(
            NotificationProcessorWorker.uniqueWorkNameFor(firstEventId),
            NotificationProcessorWorker.uniqueWorkNameFor(replayEventId),
        )
    }

    @Test
    fun `changed content with same Android identity gets a different work identity`() {
        val enqueuer = NotificationWorkEnqueuer(context = context, workManager = workManager)

        val firstEventId = enqueuer.enqueue(
            packageName = WAVE_PACKAGE,
            source = TransactionSource.WAVE,
            title = "Wave",
            body = "Vous avez reçu 5000 FCFA de Raymond",
            bigText = DEFAULT_BIG_TEXT,
            postTime = POST_TIME,
            notificationKey = NOTIFICATION_KEY,
        )
        val changedEventId = enqueuer.enqueue(
            packageName = WAVE_PACKAGE,
            source = TransactionSource.WAVE,
            title = "Wave",
            body = "Vous avez reçu 6000 FCFA de Raymond",
            bigText = DEFAULT_BIG_TEXT,
            postTime = POST_TIME,
            notificationKey = NOTIFICATION_KEY,
        )

        assertThat(changedEventId).isNotEqualTo(firstEventId)
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
            clock = { RECEIVED_AT },
        )

        val eventId = enqueueWaveNotification(enqueuer, NOTIFICATION_BODY)

        assertThat(eventId).isNotEmpty()

        val request = captureEnqueuedWork(expectedCount = 1).second.single()
        val input = request.workSpec.input

        assertThat(input.getString(NotificationProcessorWorker.KEY_EVENT_ID)).isEqualTo(eventId)
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
    fun `representative notification variants are forwarded to parsing`() {
        val enqueuer = NotificationWorkEnqueuer(context = context, workManager = workManager)
        val representativeTexts = listOf(
            "Vous avez reçu 5000 FCFA de Raymond",
            "Opération réussie : 5 000 F reçu de Awa",
            "Paiement de 1 234,56 F accepté",
            "Virement #A/B - 1 000 F effectué",
            "vous avez reçu 7 000 f de Fatou",
            "Vous avez reçu 5000 FCFA — référence WAVE-123",
            "Opération terminée : aucun détail",
        )

        representativeTexts.forEach { text ->
            enqueuer.enqueue(
                packageName = WAVE_PACKAGE,
                source = TransactionSource.WAVE,
                title = "Wave",
                body = text,
                bigText = "",
                postTime = POST_TIME,
                notificationKey = NOTIFICATION_KEY,
            )
        }

        val requests = captureEnqueuedWork(expectedCount = representativeTexts.size).second
        assertThat(requests.map { it.workSpec.input.getString(NotificationProcessorWorker.KEY_TEXT) })
            .containsExactlyElementsIn(
                representativeTexts.map { "Wave\n$it" }
            )
    }

    @Test
    fun `enriched notification text is carried over unchanged`() {
        val enqueuer = NotificationWorkEnqueuer(context = context, workManager = workManager)
        val enrichedText = NotificationProcessorWorker.assembleNotificationText(
            title = "Wave",
            body = "",
            bigText = "",
            subText = "Opération réussie",
            textLines = listOf("Vous avez reçu 2 500 F", "Référence: WAVE-123"),
            messages = listOf("Wave • 12:34"),
        )

        enqueuer.enqueue(
            packageName = WAVE_PACKAGE,
            source = TransactionSource.WAVE,
            title = "Wave",
            body = "",
            bigText = "",
            postTime = POST_TIME,
            notificationKey = NOTIFICATION_KEY,
            fullText = enrichedText,
        )

        val input = captureEnqueuedWork(expectedCount = 1).second.single().workSpec.input
        assertThat(input.getString(NotificationProcessorWorker.KEY_TEXT)).isEqualTo(enrichedText)
        assertThat(input.getString(NotificationProcessorWorker.KEY_TEXT)).contains("Vous avez reçu 2 500 F")
        assertThat(input.getString(NotificationProcessorWorker.KEY_TEXT)).contains("Référence: WAVE-123")
    }

    @Test
    fun `initial work has no network constraint but keeps exponential backoff and tag`() {

        val enqueuer = NotificationWorkEnqueuer(context = context, workManager = workManager)

        enqueueWaveNotification(enqueuer, NOTIFICATION_BODY)

        val request = captureEnqueuedWork(expectedCount = 1).second.single()

        // Chantier 3I : le travail de capture n'est plus bloqué par le réseau. Une notification
        // Wave reconnue par le parser local (3H-1) est traitée immédiatement, même hors
        // connexion ; si Gemini est nécessaire sans réseau, elle part dans la file persistante
        // et la reprise est planifiée séparément, sous contrainte `NetworkType.CONNECTED`.
        assertThat(request.workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.NOT_REQUIRED)
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
        private const val NOTIFICATION_BODY = "Vous avez reçu 5000 FCFA de Raymond"
        private const val DEFAULT_BIG_TEXT = "Wave • Transfert reçu"
        private const val RECEIVED_AT = 1_700_000_000_000L
        private const val POST_TIME = 1_699_999_999_999L
        private const val NOTIFICATION_KEY = "0|com.wave.personal|1|null|42"
    }
}
