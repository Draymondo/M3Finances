package com.naveenapps.expensemanager.feature.transaction.chat

import com.naveenapps.expensemanager.core.model.AccountType
import com.naveenapps.expensemanager.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChatViewModelTest {

    @Test
    fun `transaction command parses into editable proposal`() {
        val proposal = parseTransactionCommand("TRANSACTION|42.50|Food|Courses supermarché")

        assertNotNull(proposal)
        assertEquals(42.5, proposal!!.amount, 0.0)
        assertEquals("Food", proposal.categoryName)
        assertEquals("Courses supermarché", proposal.note)
    }

    @Test
    fun `account command parses into editable proposal`() {
        val proposal = parseAccountCommand("ACCOUNT|Compte principal|REGULAR")

        assertNotNull(proposal)
        assertEquals("Compte principal", proposal!!.name)
        assertEquals(AccountType.REGULAR, proposal.type)
    }

    @Test
    fun `budget command with income or expense type parses correctly`() {
        val proposal = parseRecurringCommand("RECURRING|Forfait mobile|18.90|EXPENSE")

        assertNotNull(proposal)
        assertEquals("Forfait mobile", proposal!!.name)
        assertEquals(18.9, proposal.amount, 0.0)
        assertEquals(TransactionType.EXPENSE, proposal.type)
    }
}
