package com.naveenapps.expensemanager.service

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.model.PendingNotificationProcessing
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.PendingNotificationProcessingRepository
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
import com.naveenapps.expensemanager.network.NetworkMonitor
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests de la logique offline (chantier 3I) : décision de mise en file, conservation du contenu
 * original, suppression après succès ou idempotence, conservation en cas d'erreur temporaire et
 * abandon en cas d'erreur permanente.
 *
 * Le processor ne dépend ni de WorkManager ni d'Android : il est testé directement.
 */
class NotificationTransactionProcessorTest {

    private val parseUseCase: ParseWaveNotificationUseCase = mock()
    private val transactionRepository: PendingTransactionRepository = mock()
    private val queueRepository: PendingNotificationProcessingRepository = mock()
    private val networkMonitor: NetworkMonitor = mock()

    private val processor = NotificationTransactionProcessor(
        parseWaveNotificationUseCase = parseUseCase,
        pendingTransactionRepository = transactionRepository,
        pendingNotificationRepository = queueRepository,
        networkMonitor = networkMonitor,
    )

    @Test
    fun `offline notification is queued and the original enriched text is preserved`() = runTest {
        whenever(networkMonitor.isNetworkAvailable()).thenReturn(false)
        whenever(parseUseCase.requiresGemini(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(true)
        var queued: PendingNotificationProcessing? = null
        whenever(queueRepository.enqueue(any())).thenAnswer { invocation ->
            queued = invocation.getArgument(0)
            Resource.Success(true)
        }

        val outcome = processor.process(request())

        assertThat(outcome).isInstanceOf(NotificationProcessingOutcome.Queued::class.java)
        assertThat(queued?.eventId).isEqualTo(EVENT_ID)
        assertThat(queued?.fullText).isEqualTo(FULL_TEXT)
        assertThat(queued?.postTime).isEqualTo(POST_TIME)
        assertThat(queued?.source).isEqualTo(TransactionSource.ORANGE_MONEY)
    }

    @Test
    fun `queue write failure is never reported as success`() = runTest {
        whenever(networkMonitor.isNetworkAvailable()).thenReturn(false)
        whenever(parseUseCase.requiresGemini(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(true)
        whenever(queueRepository.enqueue(any()))
            .thenReturn(Resource.Error(IllegalStateException("database locked")))

        val outcome = processor.process(request())

        assertThat(outcome).isNotEqualTo(NotificationProcessingOutcome.Success)
    }


    @Test
    fun `successful processing removes the queue entry`() = runTest {
        givenGeminiAvailable()

        val outcome = processor.process(request())

        assertThat(outcome).isEqualTo(NotificationProcessingOutcome.Success)
        verify(queueRepository).delete(EVENT_ID)
    }

    @Test
    fun `idempotent success is treated as processed and removes the queue entry`() = runTest {
        givenGeminiAvailable()
        // 3F : Success(false) = transaction déjà connue = succès fonctionnel.
        whenever(transactionRepository.addPendingTransaction(any())).thenReturn(Resource.Success(false))

        val outcome = processor.process(request())

        assertThat(outcome).isEqualTo(NotificationProcessingOutcome.Success)
        verify(queueRepository).delete(EVENT_ID)
    }

    @Test
    fun `temporary error keeps the entry in the queue`() = runTest {
        givenGeminiAvailable()
        whenever(parseUseCase.invoke(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(Resource.Error(Exception("Délai dépassé lors de l'appel au service d'IA")))

        val outcome = processor.process(request())

        assertThat(outcome).isInstanceOf(NotificationProcessingOutcome.Retry::class.java)
        verify(queueRepository).recordFailure(eq(EVENT_ID), any())
        verify(queueRepository, times(0)).delete(EVENT_ID)
    }

    @Test
    fun `permanent error removes the entry to avoid an infinite loop`() = runTest {
        givenGeminiAvailable()
        whenever(parseUseCase.invoke(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(Resource.Error(Exception("Type de transaction absent ou invalide")))

        val outcome = processor.process(request())

        assertThat(outcome).isInstanceOf(NotificationProcessingOutcome.Failure::class.java)
        verify(queueRepository).delete(EVENT_ID)
        verify(queueRepository, times(0)).recordFailure(any(), any())
    }

    @Test
    fun `exhausted retry budget fails instead of looping forever`() = runTest {
        givenGeminiAvailable()
        whenever(parseUseCase.invoke(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(Resource.Error(Exception("Délai dépassé lors de l'appel au service d'IA")))

        val outcome = processor.process(request(attemptCount = NotificationProcessorWorker.MAX_ATTEMPTS - 1))

        assertThat(outcome).isInstanceOf(NotificationProcessingOutcome.Failure::class.java)
        verify(queueRepository).delete(EVENT_ID)
    }

    private suspend fun givenGeminiAvailable() {
        whenever(networkMonitor.isNetworkAvailable()).thenReturn(true)
        whenever(parseUseCase.requiresGemini(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(true)
        whenever(parseUseCase.invoke(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(Resource.Success(pendingTransaction()))
        whenever(transactionRepository.addPendingTransaction(any())).thenReturn(Resource.Success(true))
    }

    private fun request(attemptCount: Int = 0) = NotificationProcessingRequest(
        eventId = EVENT_ID,
        packageName = "com.orange.money",
        source = TransactionSource.ORANGE_MONEY,
        channel = TransactionChannel.PUSH,
        title = "Orange Money",
        fullText = FULL_TEXT,
        postTime = POST_TIME,
        receivedAt = POST_TIME,
        attemptCount = attemptCount,
    )

    private fun pendingTransaction() = PendingTransaction(
        id = "tx-1", amount = 5000.0, fee = null, merchant = "Raymond", date = Date(0L),
        transactionType = TransactionType.INCOME, suggestedCategory = null,
        rawNotification = FULL_TEXT, source = TransactionSource.ORANGE_MONEY,
    )

    private companion object {
        const val EVENT_ID = "event-1"
        const val FULL_TEXT = "Orange Money\nVous avez reçu 5000 FCFA de Raymond"
        const val POST_TIME = 1_700_000_000_000L
    }
}
