package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.NotificationProcessingException
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.ProcessingErrorType
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class ParseWaveNotificationUseCaseTest {
    private val settingsRepository: SettingsRepository = mock()
    private val geminiRepository: GeminiRepository = mock()

    @Test
    fun `Wave payment is parsed locally without Gemini`() = runTest {
        val text = """
            Paiement réussi!

            Vous avez payé 1.000F à Au Gabi Choco le 29 Aug 16h03.
            Solde Wave: 27.744F Avec UBA
        """.trimIndent()
        whenever(settingsRepository.getGeminiApiKey()).thenReturn(flowOf(""))
        val useCase = ParseWaveNotificationUseCase(settingsRepository, geminiRepository, WaveNotificationParser())

        val result = useCase.invoke(text, TransactionSource.WAVE, postTimeUtc(2026, 8, 29, 16, 3))

        val transaction = (result as Resource.Success).data
        assertThat(transaction.amount).isEqualTo(1000.0)
        assertThat(transaction.fee).isNull()
        assertThat(transaction.merchant).isEqualTo("Au Gabi Choco")
        assertThat(transaction.transactionType).isEqualTo(TransactionType.EXPENSE)
        assertThat(transaction.providerReference).isNull()
        assertThat(transaction.deduplicationKey).isNotNull()
        verifyNoInteractions(geminiRepository)
    }

    @Test
    fun `unknown Wave notification falls back to Gemini`() = runTest {
        whenever(settingsRepository.getGeminiApiKey()).thenReturn(flowOf("api-key"))
        val expected = pendingTransaction()
        whenever(geminiRepository.parseWaveNotification(any(), any(), any()))
            .thenReturn(Resource.Success(expected))
        val useCase = ParseWaveNotificationUseCase(settingsRepository, geminiRepository, WaveNotificationParser())

        val result = useCase.invoke("Notification Wave inconnue", TransactionSource.WAVE, 1L)

        assertThat(result).isEqualTo(Resource.Success(expected))
        verify(geminiRepository).parseWaveNotification("Notification Wave inconnue", TransactionSource.WAVE, "api-key")
    }

    @Test
    fun `non Wave notification still uses Gemini`() = runTest {
        whenever(settingsRepository.getGeminiApiKey()).thenReturn(flowOf("api-key"))
        val expected = pendingTransaction()
        whenever(geminiRepository.parseWaveNotification(any(), any(), any()))
            .thenReturn(Resource.Success(expected))
        val useCase = ParseWaveNotificationUseCase(settingsRepository, geminiRepository, WaveNotificationParser())

        val result = useCase.invoke("Paiement réussi! Vous avez payé 1.000F à Boutique", TransactionSource.ORANGE_MONEY)

        assertThat(result).isEqualTo(Resource.Success(expected))
        verify(geminiRepository).parseWaveNotification(any(), any(), any())
    }

    @Test
    fun `missing Gemini API key remains a permanent failure for unknown Wave notification`() = runTest {
        whenever(settingsRepository.getGeminiApiKey()).thenReturn(flowOf(""))
        val useCase = ParseWaveNotificationUseCase(settingsRepository, geminiRepository, WaveNotificationParser())

        val result = useCase.invoke("Notification Wave inconnue", TransactionSource.WAVE)

        assertThat(result).isInstanceOf(Resource.Error::class.java)
        val exception = (result as Resource.Error).exception
        assertThat(exception).isInstanceOf(NotificationProcessingException::class.java)
        assertThat((exception as NotificationProcessingException).type).isEqualTo(ProcessingErrorType.PERMANENT)
        verifyNoInteractions(geminiRepository)
    }

    private fun pendingTransaction() = PendingTransaction(
        id = "tx-1", amount = 5000.0, fee = null, merchant = "Raymond", date = Date(0L),
        transactionType = TransactionType.INCOME, suggestedCategory = null,
        rawNotification = "Vous avez reçu 5000 FCFA de Raymond", source = TransactionSource.WAVE,
    )

    private fun postTimeUtc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis
}
