package com.naveenapps.expensemanager.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.InvalidStateException
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.RequestTimeoutException
import com.google.ai.client.generativeai.type.ServerException
import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.database.ExpenseManagerDatabase
import com.naveenapps.expensemanager.core.database.dao.PendingTransactionDao
import com.naveenapps.expensemanager.core.database.entity.PendingTransactionEntity
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.PendingTransactionDeduplication
import com.naveenapps.expensemanager.core.model.ProcessingErrorType
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionChannel
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class PendingTransactionRepositoryImplTest {

    private lateinit var database: ExpenseManagerDatabase
    private lateinit var repository: PendingTransactionRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseManagerDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = PendingTransactionRepositoryImpl(database.pendingTransactionDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `provider reference duplicate is acknowledged without replacing original row`() = runTest {
        val original = pendingTransaction(
            id = "original",
            providerReference = "wave-123",
            categoryId = "already-selected",
        )
        val duplicate = pendingTransaction(
            id = "replayed",
            providerReference = "wave-123",
            categoryId = "different-selection",
        )

        assertThat(repository.addPendingTransaction(original)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(duplicate)).isEqualTo(Resource.Success(false))

        val stored = database.pendingTransactionDao().getPendingTransactionById("original").first()
        assertThat(stored?.categoryId).isEqualTo("already-selected")
        assertThat(database.pendingTransactionDao().getPendingTransactionById("replayed").first()).isNull()
        assertThat(database.pendingTransactionDao().getAllPendingTransactionEntities()).hasSize(1)
    }

    @Test
    fun `push and sms with known provider and same reference are deduplicated`() = runTest {
        val push = pendingTransaction(
            id = "push-1",
            providerReference = "wave-ref-123",
            source = TransactionSource.WAVE,
            channel = TransactionChannel.PUSH,
        )
        val sms = pendingTransaction(
            id = "sms-1",
            providerReference = "wave-ref-123",
            source = TransactionSource.WAVE,
            channel = TransactionChannel.SMS,
        )

        assertThat(repository.addPendingTransaction(push)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(sms)).isEqualTo(Resource.Success(false))

        val storedList = database.pendingTransactionDao().getAllPendingTransactionEntities()
        assertThat(storedList).hasSize(1)
        assertThat(storedList.first().id).isEqualTo("push-1")
    }

    @Test
    fun `same provider reference with different providers creates separate transactions`() = runTest {
        val wave = pendingTransaction(
            id = "wave-row",
            providerReference = "provider-123",
            source = TransactionSource.WAVE,
            channel = TransactionChannel.PUSH,
        )
        val orange = pendingTransaction(
            id = "orange-row",
            providerReference = "provider-123",
            source = TransactionSource.ORANGE_MONEY,
            channel = TransactionChannel.SMS,
        )

        assertThat(repository.addPendingTransaction(wave)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(orange)).isEqualTo(Resource.Success(true))
        assertThat(database.pendingTransactionDao().getAllPendingTransactionEntities()).hasSize(2)
    }

    @Test
    fun `two identical notifications on the same channel without reference create separate transactions`() = runTest {
        val key = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.INCOME,
            amount = 5000.0,
            senderOrReceiver = "Raymond",
            fee = null,
        )
        val baseDate = Date(1_700_000_000_000L)
        val first = pendingTransaction(
            id = "push-first",
            deduplicationKey = key,
            source = TransactionSource.WAVE,
            channel = TransactionChannel.PUSH,
            date = baseDate,
        )
        val second = pendingTransaction(
            id = "push-second",
            deduplicationKey = key,
            source = TransactionSource.WAVE,
            channel = TransactionChannel.PUSH,
            date = Date(baseDate.time + 10_000L),
        )

        assertThat(repository.addPendingTransaction(first)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(second)).isEqualTo(Resource.Success(true))

        assertThat(database.pendingTransactionDao().getAllPendingTransactionEntities()).hasSize(2)
    }

    @Test
    fun `cross channel notifications without reference with same known provider within window are deduplicated`() = runTest {
        val key = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.INCOME,
            amount = 5000.0,
            senderOrReceiver = "Raymond",
            fee = null,
        )
        val baseDate = Date(1_700_000_000_000L)
        val push = pendingTransaction(
            id = "push-row",
            deduplicationKey = key,
            source = TransactionSource.WAVE,
            channel = TransactionChannel.PUSH,
            date = baseDate,
        )
        val sms = pendingTransaction(
            id = "sms-row",
            deduplicationKey = key,
            source = TransactionSource.WAVE,
            channel = TransactionChannel.SMS,
            date = Date(baseDate.time + 30_000L), // 30s < 90s window
        )

        assertThat(repository.addPendingTransaction(push)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(sms)).isEqualTo(Resource.Success(false))

        val entities = database.pendingTransactionDao().getAllPendingTransactionEntities()
        assertThat(entities).hasSize(1)
        assertThat(entities.first().id).isEqualTo("push-row")
    }

    @Test
    fun `cross channel notifications without reference with same known provider outside window create separate transactions`() = runTest {
        val key = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.INCOME,
            amount = 5000.0,
            senderOrReceiver = "Raymond",
            fee = null,
        )
        val baseDate = Date(1_700_000_000_000L)
        val push = pendingTransaction(
            id = "push-row",
            deduplicationKey = key,
            source = TransactionSource.WAVE,
            channel = TransactionChannel.PUSH,
            date = baseDate,
        )
        val sms = pendingTransaction(
            id = "sms-row",
            deduplicationKey = key,
            source = TransactionSource.WAVE,
            channel = TransactionChannel.SMS,
            date = Date(baseDate.time + PendingTransactionDeduplication.CROSS_CHANNEL_WINDOW_MS + 1000L), // > 90s
        )

        assertThat(repository.addPendingTransaction(push)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(sms)).isEqualTo(Resource.Success(true))

        assertThat(database.pendingTransactionDao().getAllPendingTransactionEntities()).hasSize(2)
    }

    @Test
    fun `cross channel notifications without reference with unknown provider on sms create separate transactions`() = runTest {
        val key = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.INCOME,
            amount = 5000.0,
            senderOrReceiver = "Raymond",
            fee = null,
        )
        val baseDate = Date(1_700_000_000_000L)
        val push = pendingTransaction(
            id = "push-row",
            deduplicationKey = key,
            source = TransactionSource.WAVE,
            channel = TransactionChannel.PUSH,
            date = baseDate,
        )
        val smsUnknownProvider = pendingTransaction(
            id = "sms-row",
            deduplicationKey = key,
            source = TransactionSource.UNKNOWN,
            channel = TransactionChannel.SMS,
            date = Date(baseDate.time + 10_000L),
        )

        assertThat(repository.addPendingTransaction(push)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(smsUnknownProvider)).isEqualTo(Resource.Success(true))

        assertThat(database.pendingTransactionDao().getAllPendingTransactionEntities()).hasSize(2)
    }

    @Test
    fun `replaying same workmanager notification event id is idempotent and does not create duplicate`() = runTest {
        val eventId = "wm-event-uuid-12345"
        val first = pendingTransaction(
            id = "first-try",
            notificationEventId = eventId,
        )
        val retry = pendingTransaction(
            id = "retry-try",
            notificationEventId = eventId,
        )

        assertThat(repository.addPendingTransaction(first)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(retry)).isEqualTo(Resource.Success(false))

        val entities = database.pendingTransactionDao().getAllPendingTransactionEntities()
        assertThat(entities).hasSize(1)
        assertThat(entities.first().id).isEqualTo("first-try")
    }

    @Test
    fun `different provider references keep otherwise identical transactions separate`() = runTest {
        val key = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.INCOME,
            amount = 5000.0,
            senderOrReceiver = "Raymond",
            fee = null,
        )
        val first = pendingTransaction(
            id = "first-reference",
            providerReference = "WAVE-REF-1",
            deduplicationKey = key,
            date = Date(1_700_000_000_000L),
        )
        val second = pendingTransaction(
            id = "second-reference",
            providerReference = "WAVE-REF-2",
            deduplicationKey = key,
            date = Date(1_700_000_010_000L),
        )

        assertThat(repository.addPendingTransaction(first)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(second)).isEqualTo(Resource.Success(true))

        assertThat(database.pendingTransactionDao().getAllPendingTransactionEntities()).hasSize(2)
    }

    @Test
    fun `same primary key never replaces an existing transaction`() = runTest {
        val first = pendingTransaction(
            id = "same-primary-key",
            categoryId = "first-selection",
        )
        val second = pendingTransaction(
            id = "same-primary-key",
            categoryId = "second-selection",
        )

        assertThat(repository.addPendingTransaction(first)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(second)).isEqualTo(Resource.Success(false))

        val stored = database.pendingTransactionDao().getPendingTransactionById("same-primary-key").first()
        assertThat(stored?.categoryId).isEqualTo("first-selection")
        assertThat(database.pendingTransactionDao().getAllPendingTransactionEntities()).hasSize(1)
    }

    @Test
    fun `transactions with null deduplication key and null provider reference are both inserted without index collision`() = runTest {
        val first = pendingTransaction(
            id = "null-key-1",
            deduplicationKey = null,
            providerReference = null,
            channel = TransactionChannel.PUSH,
        )
        val second = pendingTransaction(
            id = "null-key-2",
            deduplicationKey = null,
            providerReference = null,
            channel = TransactionChannel.PUSH,
        )

        assertThat(repository.addPendingTransaction(first)).isEqualTo(Resource.Success(true))
        assertThat(repository.addPendingTransaction(second)).isEqualTo(Resource.Success(true))

        val entities = database.pendingTransactionDao().getAllPendingTransactionEntities()
        assertThat(entities).hasSize(2)
        assertThat(entities.map { it.id }).containsExactly("null-key-1", "null-key-2")
    }

    @Test
    fun `DAO exception is returned as Resource_Error`() = runTest {
        val dao = mock<PendingTransactionDao>()
        whenever(dao.insertWithDeduplication(any(), any()))
            .thenThrow(IllegalStateException("database locked"))
        val repositoryWithFailingDao = PendingTransactionRepositoryImpl(dao)

        val result = repositoryWithFailingDao.addPendingTransaction(pendingTransaction(id = "room-error"))

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    private fun pendingTransaction(
        id: String,
        providerReference: String? = null,
        deduplicationKey: String? = null,
        categoryId: String? = null,
        source: TransactionSource = TransactionSource.WAVE,
        channel: TransactionChannel? = TransactionChannel.PUSH,
        notificationEventId: String? = null,
        date: Date = Date(1_700_000_000_000L),
    ) = PendingTransaction(
        id = id,
        amount = 5000.0,
        fee = null,
        merchant = "Raymond",
        date = date,
        transactionType = TransactionType.INCOME,
        suggestedCategory = "Salary",
        rawNotification = "Vous avez reçu 5000 FCFA de Raymond",
        source = source,
        categoryId = categoryId,
        providerReference = providerReference,
        deduplicationKey = deduplicationKey,
        channel = channel,
        notificationEventId = notificationEventId,
    )
}

class GeminiRepositoryImplTest {

    @Test
    fun `Gemini timeout and server errors are classified as temporary`() {
        val timeout = GeminiRepositoryImpl.classifyGeminiException(
            RequestTimeoutException("timeout", null)
        )
        val server = GeminiRepositoryImpl.classifyGeminiException(
            ServerException("server", null)
        )

        assertThat(timeout.type).isEqualTo(ProcessingErrorType.TEMPORARY)
        assertThat(timeout.message).isEqualTo("timeout")
        assertThat(server.type).isEqualTo(ProcessingErrorType.TEMPORARY)
        assertThat(server.message).isEqualTo("server")
    }

    @Test
    fun `Gemini quota and server errors are classified as temporary`() {
        assertThat(
            GeminiRepositoryImpl.classifyGeminiException(QuotaExceededException("quota", null)).type
        ).isEqualTo(ProcessingErrorType.TEMPORARY)
        assertThat(
            GeminiRepositoryImpl.classifyGeminiException(ServerException("server", null)).type
        ).isEqualTo(ProcessingErrorType.TEMPORARY)
    }

    @Test
    fun `Gemini invalid API key is classified as permanent`() {
        val classified = GeminiRepositoryImpl.classifyGeminiException(
            InvalidAPIKeyException("invalid", null)
        )

        assertThat(classified.type).isEqualTo(ProcessingErrorType.PERMANENT)
    }

    @Test
    fun `unknown Gemini exception remains bounded as unknown`() {
        val classified = GeminiRepositoryImpl.classifyGeminiException(IllegalStateException("unknown"))

        assertThat(classified.type).isEqualTo(ProcessingErrorType.UNKNOWN)
    }

    @Test
    fun `transaction type parser accepts supported types`() {
        assertThat(GeminiRepositoryImpl.parseTransactionType("INCOME")).isEqualTo(TransactionType.INCOME)
        assertThat(GeminiRepositoryImpl.parseTransactionType("EXPENSE")).isEqualTo(TransactionType.EXPENSE)
        assertThat(GeminiRepositoryImpl.parseTransactionType("TRANSFER")).isEqualTo(TransactionType.TRANSFER)
    }

    @Test
    fun `transaction type parser normalizes supported type casing`() {
        assertThat(GeminiRepositoryImpl.parseTransactionType("income")).isEqualTo(TransactionType.INCOME)
        assertThat(GeminiRepositoryImpl.parseTransactionType("expense")).isEqualTo(TransactionType.EXPENSE)
        assertThat(GeminiRepositoryImpl.parseTransactionType("transfer")).isEqualTo(TransactionType.TRANSFER)
    }

    @Test
    fun `transaction type parser rejects missing and unsupported types`() {
        assertThat(GeminiRepositoryImpl.parseTransactionType(null)).isNull()
        assertThat(GeminiRepositoryImpl.parseTransactionType("")).isNull()
        assertThat(GeminiRepositoryImpl.parseTransactionType("UNKNOWN")).isNull()
        assertThat(GeminiRepositoryImpl.parseTransactionType("foobar")).isNull()
    }

    // --- Chantier 3J : validation et normalisation de la sortie Gemini --------------------

    @Test
    fun `amount validation accepts a strictly positive value`() {
        assertThat(GeminiRepositoryImpl.parseAmount(1000.0)).isEqualTo(1000.0)
        assertThat(GeminiRepositoryImpl.parseAmount(0.01)).isEqualTo(0.01)
    }

    @Test
    fun `amount validation rejects missing zero negative and non finite values`() {
        assertThat(GeminiRepositoryImpl.parseAmount(null)).isNull()
        assertThat(GeminiRepositoryImpl.parseAmount(0.0)).isNull()
        assertThat(GeminiRepositoryImpl.parseAmount(-500.0)).isNull()
        assertThat(GeminiRepositoryImpl.parseAmount(Double.NaN)).isNull()
        assertThat(GeminiRepositoryImpl.parseAmount(Double.POSITIVE_INFINITY)).isNull()
    }

    @Test
    fun `fee validation accepts absent zero and positive values`() {
        assertThat(GeminiRepositoryImpl.isFeeValid(null)).isTrue()
        assertThat(GeminiRepositoryImpl.isFeeValid(0.0)).isTrue()
        assertThat(GeminiRepositoryImpl.isFeeValid(5.0)).isTrue()
    }

    @Test
    fun `fee validation rejects negative and non finite values`() {
        assertThat(GeminiRepositoryImpl.isFeeValid(-1.0)).isFalse()
        assertThat(GeminiRepositoryImpl.isFeeValid(Double.NaN)).isFalse()
        assertThat(GeminiRepositoryImpl.isFeeValid(Double.NEGATIVE_INFINITY)).isFalse()
    }

    @Test
    fun `optional text keeps meaningful values and trims them`() {
        assertThat(GeminiRepositoryImpl.normalizeOptionalText("Pharmacie de la Paix"))
            .isEqualTo("Pharmacie de la Paix")
        assertThat(GeminiRepositoryImpl.normalizeOptionalText("  Au Gabi Choco  "))
            .isEqualTo("Au Gabi Choco")
    }

    @Test
    fun `optional text normalizes empty and placeholder values to null`() {
        assertThat(GeminiRepositoryImpl.normalizeOptionalText(null)).isNull()
        assertThat(GeminiRepositoryImpl.normalizeOptionalText("")).isNull()
        assertThat(GeminiRepositoryImpl.normalizeOptionalText("   ")).isNull()
        assertThat(GeminiRepositoryImpl.normalizeOptionalText("N/A")).isNull()
        assertThat(GeminiRepositoryImpl.normalizeOptionalText("null")).isNull()
        assertThat(GeminiRepositoryImpl.normalizeOptionalText("unknown")).isNull()
        assertThat(GeminiRepositoryImpl.normalizeOptionalText("Inconnu")).isNull()
    }

    @Test
    fun `valid notification date is converted in the system timezone`() {

        val timeZone = TimeZone.getTimeZone("Europe/Paris")
        val parsedDate = GeminiRepositoryImpl.parseNotificationDate(
            dateString = "2024-02-29 14:30",
            timeZone = timeZone,
        )

        val expected = GregorianCalendar(timeZone).apply {
            clear()
            set(2024, Calendar.FEBRUARY, 29, 14, 30, 0)
        }.time

        assertThat(parsedDate).isEqualTo(expected)
    }

    @Test
    fun `invalid notification date is rejected instead of using current time`() {
        val parsedDate = GeminiRepositoryImpl.parseNotificationDate("2024-02-30 14:30")

        assertThat(parsedDate).isNull()
    }

    @Test
    fun `missing notification date remains unknown`() {
        assertThat(GeminiRepositoryImpl.parseNotificationDate(null)).isNull()
    }

    @Test
    fun `notification date parser rejects trailing characters`() {
        assertThat(GeminiRepositoryImpl.parseNotificationDate("2024-02-29 14:30 UTC")).isNull()
    }

    @Test
    fun `deduplication key is stable for equivalent financial identities`() {

        val first = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.EXPENSE,
            amount = 1000.0,
            senderOrReceiver = "  Pharmacie de la Paix ",
            fee = null,
        )
        val second = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.EXPENSE,
            amount = 1000.00,
            senderOrReceiver = "pharmacie de la paix",
            fee = null,
        )

        assertThat(first).isEqualTo(second)
        assertThat(first).hasLength(64)
    }

    @Test
    fun `deduplication key separates sources and financial identities`() {
        val wave = PendingTransactionDeduplication.computeDeduplicationKey(
            TransactionSource.WAVE,
            TransactionType.EXPENSE,
            1000.0,
            "Pharmacie",
            null,
        )
        val orange = PendingTransactionDeduplication.computeDeduplicationKey(
            TransactionSource.ORANGE_MONEY,
            TransactionType.EXPENSE,
            1000.0,
            "Pharmacie",
            null,
        )
        val differentAmount = PendingTransactionDeduplication.computeDeduplicationKey(
            TransactionSource.WAVE,
            TransactionType.EXPENSE,
            1001.0,
            "Pharmacie",
            null,
        )

        assertThat(wave).isNotEqualTo(orange)
        assertThat(wave).isNotEqualTo(differentAmount)
    }

    @Test
    fun `deduplication key produces 64 character sha256 hex string and does not use java hashcode`() {
        val key = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.EXPENSE,
            amount = 1000.0,
            senderOrReceiver = "Pharmacie de la Paix",
            fee = null,
        )

        assertThat(key).hasLength(64)
        assertThat(key).matches("^[0-9a-f]{64}$")
        assertThat(key).isNotEqualTo(key.hashCode().toString())
    }

    @Test
    fun `deduplication window keeps separate timestamps outside the window`() {
        val baseTimestamp = 1_700_000_200_000L // 3 min 20 s dans sa tranche
        // Cas 1 : deux transactions à 4 minutes d'écart avec les mêmes caractéristiques
        // -> doivent être dédupliquées si dans la fenêtre réelle
        assertThat(
            PendingTransactionDeduplication.isWithinSlidingWindow(
                baseTimestamp,
                baseTimestamp + 4 * 60 * 1000L,
            )
        ).isTrue()

        // Cas 2 : deux transactions à 4 minutes 59 secondes d'écart mais à cheval sur une frontière
        // artificielle de tranche -> doivent quand même être dédupliquées si dans la fenêtre réelle
        assertThat(
            PendingTransactionDeduplication.isWithinSlidingWindow(
                baseTimestamp,
                baseTimestamp + 4 * 60 * 1000L + 59 * 1000L,
            )
        ).isTrue()

        // Horaires suffisamment séparés (> SLIDING_WINDOW_MS) -> non dédupliqués
        assertThat(
            PendingTransactionDeduplication.isWithinSlidingWindow(
                baseTimestamp,
                baseTimestamp + PendingTransactionDeduplication.SLIDING_WINDOW_MS + 1000L,
            )
        ).isFalse()
    }

    @Test
    fun `explicit fee in notification should be kept exactly`() {
        val fee = GeminiRepositoryImpl.resolveWaveFee(
            notificationText = "Transfert réussi! Vous avez envoyé 10F. Frais: 5F",
            parsedType = "TRANSFER",
            parsedFee = 5.0,
            amount = 10.0,
        )

        assertThat(fee).isEqualTo(5.0)
    }

    @Test
    fun `transfer with no explicit fee and envoyer keyword should use one percent fallback`() {
        val fee = GeminiRepositoryImpl.resolveWaveFee(
            notificationText = "Transfert réussi! Vous avez envoyé 1000F à Raymond",
            parsedType = "TRANSFER",
            parsedFee = null,
            amount = 1000.0,
        )

        assertThat(fee).isEqualTo(10.0)
    }

    @Test
    fun `non transfer payment should not create a fee automatically`() {
        val fee = GeminiRepositoryImpl.resolveWaveFee(
            notificationText = "Paiement accepté. Vous avez payé 1000F au magasin",
            parsedType = "EXPENSE",
            parsedFee = null,
            amount = 1000.0,
        )

        assertThat(fee).isNull()
    }

    @Test
    fun `explicit zero fee should not create a fee entry`() {
        val fee = GeminiRepositoryImpl.resolveWaveFee(
            notificationText = "Transfert réussi! Frais: 0F",
            parsedType = "TRANSFER",
            parsedFee = 0.0,
            amount = 1000.0,
        )

        assertThat(fee).isNull()
    }

    @Test
    fun `deduplication key normalizes formatting and merchant case`() {
        val first = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.INCOME,
            amount = 1000.0,
            senderOrReceiver = " Raymond ",
            fee = 10.0,
        )
        val second = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.INCOME,
            amount = 1000.001,
            senderOrReceiver = "raymond",
            fee = 10.001,
        )

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `deduplication key separates sources and is independent of timestamp`() {
        val baseline = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.WAVE,
            type = TransactionType.EXPENSE,
            amount = 500.0,
            senderOrReceiver = "Marchand",
            fee = null,
        )
        val otherSource = PendingTransactionDeduplication.computeDeduplicationKey(
            source = TransactionSource.DJAMO,
            type = TransactionType.EXPENSE,
            amount = 500.0,
            senderOrReceiver = "Marchand",
            fee = null,
        )

        assertThat(otherSource).isNotEqualTo(baseline)
    }

    @Test
    fun `wave notification prompt contains wave context`() {
        val prompt = GeminiRepositoryImpl.buildNotificationPrompt(
            notificationText = "Vous avez reçu 5000F de Raymond",
            source = TransactionSource.WAVE,
        )

        assertThat(prompt).contains("Wave (mobile money)")
        assertThat(prompt).contains("Vous avez reçu 5000F de Raymond")
    }

    @Test
    fun `orange money notification prompt contains orange money context`() {
        val prompt = GeminiRepositoryImpl.buildNotificationPrompt(
            notificationText = "Transfert de 5000F reçu",
            source = TransactionSource.ORANGE_MONEY,
        )

        assertThat(prompt).contains("Orange Money")
        assertThat(prompt).contains("Transfert de 5000F reçu")
    }

    @Test
    fun `mtn momo notification prompt contains mtn momo context`() {
        val prompt = GeminiRepositoryImpl.buildNotificationPrompt(
            notificationText = "Paiement effectué de 2000F",
            source = TransactionSource.MTN_MOMO,
        )

        assertThat(prompt).contains("MTN MoMo")
        assertThat(prompt).contains("Paiement effectué de 2000F")
    }

    @Test
    fun `unknown source notification prompt contains generic financial context`() {
        val prompt = GeminiRepositoryImpl.buildNotificationPrompt(
            notificationText = "Notification inconnue",
            source = TransactionSource.UNKNOWN,
        )

        assertThat(prompt).contains("financial or mobile money")
        assertThat(prompt).doesNotContain("Wave (mobile money)")
        assertThat(prompt).doesNotContain("Orange Money")
        assertThat(prompt).doesNotContain("MTN MoMo")
    }
}
