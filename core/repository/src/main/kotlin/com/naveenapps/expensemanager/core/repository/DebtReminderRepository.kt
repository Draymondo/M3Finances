package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.DebtReminder
import com.naveenapps.expensemanager.core.model.Resource
import kotlinx.coroutines.flow.Flow

interface DebtReminderRepository {

    fun getRemindersForDebt(debtId: String): Flow<List<DebtReminder>>

    /** Every reminder across every debt — used to reconcile scheduled notifications against the
     * current state of the world (see `DebtReminderTrigger`). */
    fun getAllReminders(): Flow<List<DebtReminder>>

    suspend fun addReminder(reminder: DebtReminder): Resource<Boolean>

    suspend fun deleteReminder(id: String): Resource<Boolean>
}
