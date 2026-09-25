package com.naveenapps.expensemanager.service

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Tests du worker de traitement des notifications.
 *
 * Ils s'exécutent sans Robolectric : les appels `Log` sont neutralisés par
 * `testOptions.unitTests.isReturnDefaultValues` du module app, et le worker est instancié
 * directement avec de vrais [WorkerParameters].
 */
class NotificationProcessorWorkerTest {

    private val context: Context = mock()
    private val parseWaveNotificationUseCase: ParseWaveNotificationUseCase = mock()
    private val pendingTransactionRepository: PendingTransactionRepository = mock()

    @Before
    fun setUp() {
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
        startKoin {
            modules(
                module {
                    single { parseWaveNotificationUseCase }
                    single { pendingTransactionRepository }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `successful parsing saves the pending transaction and returns success`() = runTest {
        val pendingTransaction = pendingTransaction()
        val expectedSaved = pendingTransaction.copy(
            channel = TransactionChannel.PUSH,
            notificationEventId = "event-1",
        )
        whenever(parseWaveNotificationUseCase.invoke(DEFAULT_TEXT, TransactionSource.WAVE))
            .thenReturn(Resource.Success(pendingTransaction))
        whenever(pendingTransactionRepository.addPendingTransaction(expectedSaved))
            .thenReturn(Resource.Success(true))

        val result = worker().doWork()

        assertThat(result).isEqualTo(Result.success())
        verify(pendingTransactionRepository).addPendingTransaction(expectedSaved)
    }

    @Test
    fun `two notifications with identical text are processed separately`() = runTest {
        whenever(parseWaveNotificationUseCase.invoke(DEFAULT_TEXT, TransactionSource.WAVE))
            .thenReturn(Resource.Success(pendingTransaction()))
        whenever(pendingTransactionRepository.addPendingTransaction(any()))
            .thenReturn(Resource.Success(true))

        val firstResult = worker(eventId = "event-a").doWork()
        val secondResult = worker(eventId = "event-b").doWork()

        assertThat(firstResult).isEqualTo(Result.success())
        assertThat(secondResult).isEqualTo(Result.success())
        verify(pendingTransactionRepository, times(2)).addPendingTransaction(any())
    }

    @Test
    fun `missing text fails immediately without parsing nor retry`() = runTest {
        val result = worker(text = null).doWork()

        assertThat(result).isEqualTo(Result.failure())
        verifyNoInteractions(parseWaveNotificationUseCase)
        verifyNoInteractions(pendingTransactionRepository)
    }

    @Test
    fun `notification without extractable amount is a definitive failure`() = runTest {
        whenever(parseWaveNotificationUseCase.invoke(DEFAULT_TEXT, TransactionSource.WAVE))
            .thenReturn(Resource.Error(Exception("Montant introuvable dans la notification")))

        val result = worker().doWork()

        assertThat(result).isEqualTo(Result.failure())
        verifyNoInteractions(pendingTransactionRepository)
    }

    @Test
    fun `temporary parsing error is retried`() = runTest {
        whenever(parseWaveNotificationUseCase.invoke(DEFAULT_TEXT, TransactionSource.WAVE))
            .thenReturn(Resource.Error(Exception("Délai dépassé lors de l'appel au service d'IA")))

        val result = worker(runAttemptCount = 0).doWork()

        assertThat(result).isEqualTo(Result.retry())
    }

    @Test
    fun `retries are bounded for temporary parsing errors`() = runTest {
        whenever(parseWaveNotificationUseCase.invoke(DEFAULT_TEXT, TransactionSource.WAVE))
            .thenReturn(Resource.Error(Exception("Délai dépassé lors de l'appel au service d'IA")))

        val result = worker(runAttemptCount = NotificationProcessorWorker.MAX_ATTEMPTS - 1).doWork()

        assertThat(result).isEqualTo(Result.failure())
    }

    @Test
    fun `persistence failure is never reported as success`() = runTest {
        whenever(parseWaveNotificationUseCase.invoke(DEFAULT_TEXT, TransactionSource.WAVE))
            .thenReturn(Resource.Success(pendingTransaction()))
        whenever(pendingTransactionRepository.addPendingTransaction(any()))
            .thenReturn(Resource.Error(Exception("database is locked")))

        val firstAttempt = worker(runAttemptCount = 0).doWork()
        val lastAttempt = worker(runAttemptCount = NotificationProcessorWorker.MAX_ATTEMPTS - 1).doWork()

        assertThat(firstAttempt).isEqualTo(Result.retry())
        assertThat(lastAttempt).isEqualTo(Result.failure())
    }

    @Test
    fun `unexpected exception is never reported as success`() = runTest {
        whenever(parseWaveNotificationUseCase.invoke(DEFAULT_TEXT, TransactionSource.WAVE))
            .thenThrow(IllegalStateException("boom"))

        val result = worker(runAttemptCount = 0).doWork()

        assertThat(result).isNotEqualTo(Result.success())
        assertThat(result).isEqualTo(Result.retry())
    }

    private fun pendingTransaction() = PendingTransaction(
        id = "tx-1",
        amount = 5000.0,
        fee = null,
        merchant = "Raymond",
        date = Date(0L),
        transactionType = TransactionType.INCOME,
        suggestedCategory = "Salary",
        rawNotification = DEFAULT_TEXT,
        source = TransactionSource.WAVE,
    )

    private fun worker(
        eventId: String = "event-1",
        text: String? = DEFAULT_TEXT,
        source: String? = TransactionSource.WAVE.name,
        runAttemptCount: Int = 0,
    ): NotificationProcessorWorker {
        val dataBuilder = Data.Builder()
            .putString(NotificationProcessorWorker.KEY_EVENT_ID, eventId)
            .putString(NotificationProcessorWorker.KEY_PACKAGE_NAME, "com.wave.personal")
        text?.let { dataBuilder.putString(NotificationProcessorWorker.KEY_TEXT, it) }
        source?.let { dataBuilder.putString(NotificationProcessorWorker.KEY_SOURCE, it) }

        val params = WorkerParameters(
            UUID.randomUUID(),
            dataBuilder.build(),
            emptyList(),
            WorkerParameters.RuntimeExtras(),
            runAttemptCount,
            1,
            Executor { it.run() },
            EmptyCoroutineContext,
            // Paramètres techniques non utilisés par doWork() : le worker est exécuté
            // directement ici, sans passer par le moteur WorkManager.
            mock(),
            mock(),
            mock(),
            mock(),
        )
        return NotificationProcessorWorker(context, params)
    }

    companion object {
        private const val DEFAULT_TEXT = "Vous avez reçu 5000 FCFA de Raymond"
    }
}
