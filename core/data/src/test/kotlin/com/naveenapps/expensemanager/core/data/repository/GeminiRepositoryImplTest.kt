package com.naveenapps.expensemanager.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.database.ExpenseManagerDatabase
import com.naveenapps.expensemanager.core.database.dao.PendingTransactionDao
import com.naveenapps.expensemanager.core.database.entity.PendingTransactionEntity
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.PendingTransactionDeduplication
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
import java.util.Date

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
        assertThat(repository.addPendingTransaction(duplicate)).isEqualTo(Resource.Success(true))

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
        assertThat(repository.addPendingTransaction(sms)).isEqualTo(Resource.Success(true))

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
        assertThat(repository.addPendingTransaction(sms)).isEqualTo(Resource.Success(true))

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
        assertThat(repository.addPendingTransaction(retry)).isEqualTo(Resource.Success(true))

        val entities = database.pendingTransactionDao().getAllPendingTransactionEntities()
        assertThat(entities).hasSize(1)
        assertThat(entities.first().id).isEqualTo("first-try")
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
}
