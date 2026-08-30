package com.naveenapps.expensemanager.core.domain.usecase.transaction

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.Amount
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionSplitItem
import com.naveenapps.expensemanager.core.testing.FAKE_EXPENSE_TRANSACTION
import com.naveenapps.expensemanager.core.testing.FAKE_SECOND_CATEGORY
import com.naveenapps.expensemanager.core.testing.FAKE_CATEGORY
import org.junit.Test

class TransactionSplitValidationTest {

    @Test
    fun fewerThanTwoItemsAreTreatedAsNormalTransaction() {
        val transaction = FAKE_EXPENSE_TRANSACTION.copy(
            splitItems = listOf(
                TransactionSplitItem("split-1", "1", FAKE_CATEGORY.id, Amount(200.0)),
            ),
        )

        val result = validateAndNormalizeSplit(transaction)

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        assertThat((result as Resource.Success).data.splitItems).isEmpty()
    }

    @Test
    fun splitItemsMustExactlyMatchTransactionAmount() {
        val transaction = FAKE_EXPENSE_TRANSACTION.copy(
            splitItems = listOf(
                TransactionSplitItem("split-1", "1", FAKE_CATEGORY.id, Amount(100.0)),
                TransactionSplitItem("split-2", "1", FAKE_SECOND_CATEGORY.id, Amount(99.99)),
            ),
        )

        val result = validateAndNormalizeSplit(transaction)

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun largestSplitItemBecomesDefaultCategory() {
        val transaction = FAKE_EXPENSE_TRANSACTION.copy(
            splitItems = listOf(
                TransactionSplitItem("split-1", "1", FAKE_CATEGORY.id, Amount(80.0)),
                TransactionSplitItem("split-2", "1", FAKE_SECOND_CATEGORY.id, Amount(120.0)),
            ),
        )

        val result = validateAndNormalizeSplit(transaction)

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        assertThat((result as Resource.Success).data.categoryId).isEqualTo(FAKE_SECOND_CATEGORY.id)
    }
}
