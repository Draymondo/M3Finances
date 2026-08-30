package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {

    fun getSavingsGoals(): Flow<List<SavingsGoal>>

    suspend fun findSavingsGoalById(id: String): Resource<SavingsGoal>

    suspend fun addSavingsGoal(savingsGoal: SavingsGoal): Resource<Boolean>

    suspend fun updateSavingsGoal(savingsGoal: SavingsGoal): Resource<Boolean>
}
