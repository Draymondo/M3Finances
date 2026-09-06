package com.naveenapps.expensemanager.feature.dashboard

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.BudgetGoalType
import org.junit.Test

class BudgetWeatherTest {

    @Test
    fun `expense budget exceeding 100 percent triggers critical alert`() {
        val result = calculateBudgetWeather(
            percent = 105f,
            expectedPercent = 50f,
            goalType = BudgetGoalType.EXPENSE,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.CRITICAL_ALERT)
        assertThat(result.emoji).isEqualTo("⛈️")
        assertThat(result.title).isEqualTo("Alerte Rouge")
        assertThat(result.message).isEqualTo("Budget du mois dépassé !")
    }

    @Test
    fun `expense budget spending faster than pace triggers warning`() {
        // Expected 30%, spent 50% -> paceDelta = -20 (< -15)
        val result = calculateBudgetWeather(
            percent = 50f,
            expectedPercent = 30f,
            goalType = BudgetGoalType.EXPENSE,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.WARNING)
        assertThat(result.emoji).isEqualTo("🌧️")
        assertThat(result.title).isEqualTo("Attention")
        assertThat(result.message).isEqualTo("Tu dépenses plus vite que prévu.")
    }

    @Test
    fun `expense budget slight overpace triggers caution`() {
        // Expected 30%, spent 35% -> paceDelta = -5 (< 0)
        val result = calculateBudgetWeather(
            percent = 35f,
            expectedPercent = 30f,
            goalType = BudgetGoalType.EXPENSE,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.CAUTION)
        assertThat(result.emoji).isEqualTo("☁️")
        assertThat(result.title).isEqualTo("Prudence")
        assertThat(result.message).isEqualTo("Léger dépassement sur le rythme.")
    }

    @Test
    fun `expense budget on track triggers on track`() {
        // Expected 30%, spent 25% -> paceDelta = +5 (in 0..15)
        val result = calculateBudgetWeather(
            percent = 25f,
            expectedPercent = 30f,
            goalType = BudgetGoalType.EXPENSE,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.ON_TRACK)
        assertThat(result.emoji).isEqualTo("⛅")
        assertThat(result.title).isEqualTo("Dans les clous")
        assertThat(result.message).isEqualTo("Tu respectes bien ton rythme.")
    }

    @Test
    fun `expense budget well under budget triggers excellent`() {
        // Expected 50%, spent 20% -> paceDelta = +30 (> 15)
        val result = calculateBudgetWeather(
            percent = 20f,
            expectedPercent = 50f,
            goalType = BudgetGoalType.EXPENSE,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.EXCELLENT)
        assertThat(result.emoji).isEqualTo("☀️")
        assertThat(result.title).isEqualTo("Excellent")
        assertThat(result.message).isEqualTo("Tu es en dessous de ton budget !")
    }

    @Test
    fun `income budget exceeding 100 percent triggers excellent goal reached`() {
        val result = calculateBudgetWeather(
            percent = 105f,
            expectedPercent = 50f,
            goalType = BudgetGoalType.INCOME,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.EXCELLENT)
        assertThat(result.emoji).isEqualTo("☀️")
        assertThat(result.title).isEqualTo("Objectif atteint !")
        assertThat(result.message).isEqualTo("Objectif de revenus du mois dépassé !")
    }

    @Test
    fun `income budget ahead of pace triggers excellent`() {
        // Received 50%, expected 30% -> paceDelta = +20 (> 15)
        val result = calculateBudgetWeather(
            percent = 50f,
            expectedPercent = 30f,
            goalType = BudgetGoalType.INCOME,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.EXCELLENT)
        assertThat(result.emoji).isEqualTo("☀️")
        assertThat(result.title).isEqualTo("Excellent")
        assertThat(result.message).isEqualTo("Tu es en avance sur tes revenus !")
    }

    @Test
    fun `income budget following pace triggers on track`() {
        // Received 35%, expected 30% -> paceDelta = +5 (in 0..15)
        val result = calculateBudgetWeather(
            percent = 35f,
            expectedPercent = 30f,
            goalType = BudgetGoalType.INCOME,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.ON_TRACK)
        assertThat(result.emoji).isEqualTo("⛅")
        assertThat(result.title).isEqualTo("Dans les clous")
        assertThat(result.message).isEqualTo("Tes rentrées d'argent suivent le rythme.")
    }

    @Test
    fun `income budget slightly lagging pace triggers caution`() {
        // Received 25%, expected 30% -> paceDelta = -5 (in -15..0)
        val result = calculateBudgetWeather(
            percent = 25f,
            expectedPercent = 30f,
            goalType = BudgetGoalType.INCOME,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.CAUTION)
        assertThat(result.emoji).isEqualTo("☁️")
        assertThat(result.title).isEqualTo("Prudence")
        assertThat(result.message).isEqualTo("Léger retard sur tes revenus prévus.")
    }

    @Test
    fun `income budget significantly lagging pace triggers warning`() {
        // Received 10%, expected 30% -> paceDelta = -20 (in -30..-15)
        val result = calculateBudgetWeather(
            percent = 10f,
            expectedPercent = 30f,
            goalType = BudgetGoalType.INCOME,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.WARNING)
        assertThat(result.emoji).isEqualTo("🌧️")
        assertThat(result.title).isEqualTo("Attention")
        assertThat(result.message).isEqualTo("Tes revenus rentrent moins vite que prévu.")
    }

    @Test
    fun `income budget severely lagging pace triggers critical alert`() {
        // Received 5%, expected 45% -> paceDelta = -40 (< -30)
        val result = calculateBudgetWeather(
            percent = 5f,
            expectedPercent = 45f,
            goalType = BudgetGoalType.INCOME,
        )

        assertThat(result.condition).isEqualTo(BudgetWeatherCondition.CRITICAL_ALERT)
        assertThat(result.emoji).isEqualTo("⛈️")
        assertThat(result.title).isEqualTo("Alerte Rouge")
        assertThat(result.message).isEqualTo("Fort retard sur tes revenus du mois !")
    }
}

