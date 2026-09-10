package com.naveenapps.expensemanager.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.naveenapps.expensemanager.core.database.entity.TransactionEntity
import com.naveenapps.expensemanager.core.database.entity.TransactionRelation
import com.naveenapps.expensemanager.core.database.entity.TransactionSplitItemEntity
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.isTransfer
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao : BaseDao<TransactionEntity> {

    @Query("SELECT * FROM transaction_split_item WHERE transaction_id = :transactionId")
    suspend fun getSplitItemsForTransaction(transactionId: String): List<TransactionSplitItemEntity>

    @Query("SELECT * FROM transaction_split_item")
    suspend fun getAllSplitItems(): List<TransactionSplitItemEntity>

    @Insert
    suspend fun insertSplitItems(items: List<TransactionSplitItemEntity>)

    @Query("DELETE FROM transaction_split_item WHERE transaction_id = :transactionId")
    suspend fun deleteSplitItemsForTransaction(transactionId: String)

    @Query("DELETE FROM transaction_split_item")
    suspend fun deleteAllSplitItems()

    @Query("DELETE FROM `transaction`")
    suspend fun deleteAllTransactions()

    @Query("SELECT * from `transaction` WHERE id=:id")
    fun findById(id: String): TransactionEntity?

    @Transaction
    @Query("SELECT * from `transaction` WHERE id=:id")
    suspend fun findRelationById(id: String): TransactionRelation?

    @Query("SELECT * FROM `transaction`")
    fun getAllTransaction(): Flow<List<TransactionRelation>?>

    @Query(
        """
        SELECT * FROM `transaction`
        WHERE (`transaction`.from_account_id IN(:accounts) OR `transaction`.to_account_id IN(:accounts))
        AND (`transaction`.category_id IN(:categories) OR EXISTS (SELECT 1 FROM transaction_split_item WHERE transaction_id = `transaction`.id AND category_id IN(:categories)))
        AND `transaction`.type IN(:transactionTypes)
        ORDER BY `transaction`.created_on DESC
        """,
    )
    fun getAllFilteredTransaction(
        accounts: List<String>,
        categories: List<String>,
        transactionTypes: List<Int>,
    ): Flow<List<TransactionRelation>?>

    @Query(
        """
        SELECT * FROM `transaction`
        WHERE (`transaction`.from_account_id IN(:accounts) OR `transaction`.to_account_id IN(:accounts))
        AND (`transaction`.category_id IN(:categories) OR EXISTS (SELECT 1 FROM transaction_split_item WHERE transaction_id = `transaction`.id AND category_id IN(:categories)))
        AND `transaction`.type IN(:transactionTypes)
        AND `transaction`.created_on BETWEEN :fromDate AND :toDate
        ORDER BY `transaction`.created_on DESC
        """,
    )
    fun getFilteredTransaction(
        accounts: List<String>,
        categories: List<String>,
        transactionTypes: List<Int>,
        fromDate: Long,
        toDate: Long,
    ): Flow<List<TransactionRelation>?>

    @Query("SELECT * from `transaction` WHERE from_account_id = :accountId OR to_account_id = :accountId")
    suspend fun getTransactionsByAccountId(accountId: String): List<TransactionEntity>

    @Query("SELECT * from `transaction` WHERE category_id = :categoryId")
    suspend fun getTransactionsByCategoryId(categoryId: String): List<TransactionEntity>

    /** Raw, unjoined snapshot of every transaction — used by the cloud sync, which pushes
     * entities as-is rather than the UI-oriented [TransactionRelation] joins above. */
    @Query("SELECT * from `transaction`")
    suspend fun getAllTransactionEntities(): List<TransactionEntity>

    /**
     * Atomically adjusts an account's balance by [amountDelta] in a single SQL statement
     * (`amount = amount + :amountDelta`), instead of a read-then-write (find + copy + update)
     * round trip. This avoids lost updates when two coroutines touch the same account's balance
     * concurrently (e.g. two transactions on the same account saved at nearly the same time).
     * No-ops if the account no longer exists (0 rows affected).
     */
    @Query("UPDATE account SET amount = amount + :amountDelta WHERE id = :accountId")
    suspend fun adjustAccountBalance(accountId: String, amountDelta: Double)

    @Transaction
    suspend fun insertTransaction(
        transactionEntity: TransactionEntity,
        splitItems: List<TransactionSplitItemEntity>,
        amountToDetect: Double,
        isTransfer: Boolean,
    ): Long {
        val id = insert(transactionEntity)
        if (id != -1L) {
            insertSplitItems(splitItems)
            adjustAccountBalance(transactionEntity.fromAccountId, amountToDetect)
            if (isTransfer && transactionEntity.toAccountId?.isNotBlank() == true) {
                adjustAccountBalance(transactionEntity.toAccountId!!, amountToDetect * -1)
            }
        }
        return id
    }

    @Transaction
    suspend fun removePreviousEnteredAmount(transactionEntity: TransactionEntity) {
        val previousTransaction = findById(transactionEntity.id)

        if (previousTransaction != null) {
            val previousAmountToDetect = if (previousTransaction.type == TransactionType.INCOME) {
                previousTransaction.amount * -1
            } else {
                previousTransaction.amount
            }
            adjustAccountBalance(previousTransaction.fromAccountId, previousAmountToDetect)

            if (previousTransaction.type.isTransfer() && previousTransaction.toAccountId?.isNotBlank() == true) {
                adjustAccountBalance(previousTransaction.toAccountId!!, previousAmountToDetect * -1)
            }
        }
    }

    @Transaction
    suspend fun updateTransaction(
        transactionEntity: TransactionEntity,
        splitItems: List<TransactionSplitItemEntity>,
        amountToDetect: Double,
        isTransfer: Boolean,
    ) {
        update(transactionEntity)
        deleteSplitItemsForTransaction(transactionEntity.id)
        insertSplitItems(splitItems)

        adjustAccountBalance(transactionEntity.fromAccountId, amountToDetect)
        if (isTransfer && transactionEntity.toAccountId?.isNotBlank() == true) {
            adjustAccountBalance(transactionEntity.toAccountId!!, amountToDetect * -1)
        }
    }

    @Query(
        """
        SELECT category_id FROM `transaction`
        WHERE LOWER(notes) = LOWER(:notes) AND type = :transactionType
        GROUP BY category_id
        ORDER BY COUNT(*) DESC
        LIMIT 1
        """
    )
    suspend fun getMostFrequentCategoryIdForNotes(notes: String, transactionType: TransactionType): String?
}
