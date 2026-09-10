package com.naveenapps.expensemanager.feature.dashboard

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RequiredIncomeAlignmentTest {

    @Test
    fun `when required income is zero or negative it is considered aligned`() {
        assertThat(isRequiredIncomeAligned(requiredAmount = 0.0, budgetAmount = 1000.0)).isTrue()
        assertThat(isRequiredIncomeAligned(requiredAmount = -50.0, budgetAmount = 1000.0)).isTrue()
        assertThat(isRequiredIncomeAligned(requiredAmount = 0.0, budgetAmount = null)).isTrue()
    }

    @Test
    fun `when budget amount is null or zero it is not aligned`() {
        assertThat(isRequiredIncomeAligned(requiredAmount = 1500.0, budgetAmount = null)).isFalse()
        assertThat(isRequiredIncomeAligned(requiredAmount = 1500.0, budgetAmount = 0.0)).isFalse()
        assertThat(isRequiredIncomeAligned(requiredAmount = 1500.0, budgetAmount = -100.0)).isFalse()
    }

    @Test
    fun `when budget amount equals required amount exactly it is aligned`() {
        assertThat(isRequiredIncomeAligned(requiredAmount = 2000.0, budgetAmount = 2000.0)).isTrue()
    }

    @Test
    fun `when budget amount is within plus or minus 1 percent tolerance it is aligned`() {
        // 1% of 2000 is 20. Range [1980..2020]
        assertThat(isRequiredIncomeAligned(requiredAmount = 2000.0, budgetAmount = 2015.0)).isTrue()
        assertThat(isRequiredIncomeAligned(requiredAmount = 2000.0, budgetAmount = 1985.0)).isTrue()
        assertThat(isRequiredIncomeAligned(requiredAmount = 2000.0, budgetAmount = 2020.0)).isTrue()
        assertThat(isRequiredIncomeAligned(requiredAmount = 2000.0, budgetAmount = 1980.0)).isTrue()
    }

    @Test
    fun `when budget amount deviates by more than 1 percent it is not aligned`() {
        // 1% of 2000 is 20. Values outside [1980..2020] diverge
        assertThat(isRequiredIncomeAligned(requiredAmount = 2000.0, budgetAmount = 2025.0)).isFalse()
        assertThat(isRequiredIncomeAligned(requiredAmount = 2000.0, budgetAmount = 1975.0)).isFalse()
        assertThat(isRequiredIncomeAligned(requiredAmount = 2500.0, budgetAmount = 2000.0)).isFalse()
    }
}

