package com.naveenapps.expensemanager.core.repository

import com.naveenapps.expensemanager.core.model.InstallmentPlan
import com.naveenapps.expensemanager.core.model.Resource
import kotlinx.coroutines.flow.Flow

interface InstallmentPlanRepository {

    fun getInstallmentPlans(): Flow<List<InstallmentPlan>>

    suspend fun findInstallmentPlanById(id: String): Resource<InstallmentPlan>

    suspend fun addInstallmentPlan(installmentPlan: InstallmentPlan): Resource<Boolean>

    suspend fun updateInstallmentPlan(installmentPlan: InstallmentPlan): Resource<Boolean>
}
