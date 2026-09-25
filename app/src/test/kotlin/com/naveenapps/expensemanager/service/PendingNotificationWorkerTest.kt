package com.naveenapps.expensemanager.service

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
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
import java.util.UUID
import java.util.concurrent.Executor
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests du worker de reprise offline (chantier 3I) : retour du réseau, rejeu du contenu
 * original, traitement séquentiel de plusieurs notifications, retry borné et abandon définitif.
 */
class PendingNotificationWorkerTest {

    private val context: Context = mock()
    private val parseUseCase: ParseWaveNotificationUseCase = mock()
    private val transactionRepository: PendingTransactionRepository = mock()
    private val queueRepository: PendingNotificationProcessingRepository = mock()
    private val networkMonitor: NetworkMonitor = mock()

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
        whenever(networkMonitor.isNetworkAvailable()).thenReturn(true)
        startKoin {
            modules(
                module {
                    single { parseUseCase }
                    single { transactionRepository }
                    single { queueRepository }
                    single { networkMonitor }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `empty queue completes successfully without calling Gemini`() = runTest {
        whenever(queueRepository.getPendingNotifications()).thenReturn(emptyList())

        val result = worker().doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(parseUseCase, times(0)).invoke(any(), any(), any())
    }

    @Test
    fun `queued notification is replayed with its original content and removed on success`() = runTest {
        whenever(queueRepository.getPendingNotifications()).thenReturn(listOf(entry("event-1")))
        givenGeminiSucceeds()

        val result = worker().doWork()

        assertThat(result).isEqualTo(Result.success())
        // Le contenu original est rejoué tel quel : aucune reconstruction depuis des fragments.
        verify(parseUseCase).invoke(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME)
        verify(queueRepository).delete("event-1")
    }

    @Test
    fun `multiple queued notifications are replayed sequentially`() = runTest {
        whenever(queueRepository.getPendingNotifications())
            .thenReturn(listOf(entry("event-1"), entry("event-2")))
        givenGeminiSucceeds()

        val result = worker().doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(queueRepository).delete("event-1")
        verify(queueRepository).delete("event-2")
    }


    @Test
    fun `idempotent replay still removes the queue entry`() = runTest {
        whenever(queueRepository.getPendingNotifications()).thenReturn(listOf(entry("event-1")))
        givenGeminiSucceeds()
        // 3F : Success(false) = déjà connue = succès, l'entrée doit tout de même disparaître.
        whenever(transactionRepository.addPendingTransaction(any())).thenReturn(Resource.Success(false))

        val result = worker().doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(queueRepository).delete("event-1")
    }

    @Test
    fun `temporary error triggers a bounded retry and keeps the entry`() = runTest {
        whenever(queueRepository.getPendingNotifications()).thenReturn(listOf(entry("event-1")))
        whenever(parseUseCase.requiresGemini(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(true)
        whenever(parseUseCase.invoke(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(Resource.Error(Exception("Délai dépassé lors de l'appel au service d'IA")))

        val result = worker().doWork()

        assertThat(result).isEqualTo(Result.retry())
        verify(queueRepository).recordFailure(eq("event-1"), any())
        verify(queueRepository, times(0)).delete(any())
    }

    @Test
    fun `permanent error is dropped from the queue without retrying`() = runTest {
        whenever(queueRepository.getPendingNotifications()).thenReturn(listOf(entry("event-1")))
        whenever(parseUseCase.requiresGemini(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(true)
        whenever(parseUseCase.invoke(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(Resource.Error(Exception("Clé d'API Gemini non configurée.")))

        val result = worker().doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(queueRepository).delete("event-1")
    }

    private suspend fun givenGeminiSucceeds() {
        whenever(parseUseCase.requiresGemini(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(true)
        whenever(parseUseCase.invoke(FULL_TEXT, TransactionSource.ORANGE_MONEY, POST_TIME))
            .thenReturn(Resource.Success(pendingTransaction()))
        whenever(transactionRepository.addPendingTransaction(any())).thenReturn(Resource.Success(true))
    }

    private fun entry(eventId: String) = PendingNotificationProcessing(
        eventId = eventId,
        packageName = "com.orange.money",
        source = TransactionSource.ORANGE_MONEY,
        channel = TransactionChannel.PUSH,
        title = "Orange Money",
        fullText = FULL_TEXT,
        postTime = POST_TIME,
        receivedAt = POST_TIME,
    )

    private fun pendingTransaction() = PendingTransaction(
        id = "tx-1", amount = 5000.0, fee = null, merchant = "Raymond", date = Date(0L),
        transactionType = TransactionType.INCOME, suggestedCategory = null,
        rawNotification = FULL_TEXT, source = TransactionSource.ORANGE_MONEY,
    )

    private fun worker(): PendingNotificationWorker = PendingNotificationWorker(
        context,
        WorkerParameters(
            UUID.randomUUID(),
            Data.EMPTY,
            emptyList(),
            WorkerParameters.RuntimeExtras(),
            0,
            1,
            Executor { it.run() },
            EmptyCoroutineContext,
            mock(),
            mock(),
            mock(),
            mock(),
        ),
    )

    private companion object {
        const val FULL_TEXT = "Orange Money\nVous avez reçu 5000 FCFA de Raymond"
        const val POST_TIME = 1_700_000_000_000L
    }
}
