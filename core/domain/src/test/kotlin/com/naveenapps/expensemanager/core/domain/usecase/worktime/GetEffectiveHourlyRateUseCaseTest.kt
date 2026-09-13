package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.WorkTimePeriod
import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetEffectiveHourlyRateUseCaseTest : BaseCoroutineTest() {

    private val repository: WorkTimeSettingsRepository = mock()
    private val getAutoDetectedIncomeUseCase: GetAutoDetectedIncomeUseCase = mock()
    private lateinit var useCase: GetEffectiveHourlyRateUseCase

    override fun onCreate() {
        super.onCreate()
        useCase = GetEffectiveHourlyRateUseCase(repository, getAutoDetectedIncomeUseCase)
    }

    // ─── Cas limites communs ────────────────────────────────────────────────

    @Test
    fun whenHoursPerDayIsZeroShouldReturnError() = runTest {
        val settings = WorkTimeSettings(hoursPerDay = 0.0, daysPerWeek = 5, manualIncome = 8000.0)
        whenever(repository.getSettings()).thenReturn(flowOf(settings))
        val result = useCase()
        assertThat(result).isInstanceOf(Resource.Error::class.java)
        assertThat((result as Resource.Error).exception.message).contains("Heures par jour")
    }

    @Test
    fun whenDaysPerWeekIsZeroShouldReturnError() = runTest {
        val settings = WorkTimeSettings(hoursPerDay = 8.0, daysPerWeek = 0, manualIncome = 8000.0)
        whenever(repository.getSettings()).thenReturn(flowOf(settings))
        val result = useCase()
        assertThat(result).isInstanceOf(Resource.Error::class.java)
        assertThat((result as Resource.Error).exception.message).contains("Jours par semaine")
    }

    // ─── Mode manuel ────────────────────────────────────────────────────────

    @Test
    fun whenManualIncomeIsZeroShouldReturnError() = runTest {
        val settings = WorkTimeSettings(hoursPerDay = 8.0, daysPerWeek = 5, manualIncome = 0.0, autoDetectEnabled = false)
        whenever(repository.getSettings()).thenReturn(flowOf(settings))
        val result = useCase()
        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenManualModeMonthShouldComputeCorrectRate() = runTest {
        // 8000 FCFA/mois, 8h/jour, 5j/semaine → hoursPerMonth = 4.33*5*8 = 173.2
        // tauxHoraire = 8000 / 173.2 ≈ 46.19
        val settings = WorkTimeSettings(
            manualIncome = 8000.0,
            manualIncomePeriod = WorkTimePeriod.MONTH,
            hoursPerDay = 8.0,
            daysPerWeek = 5,
            autoDetectEnabled = false,
        )
        whenever(repository.getSettings()).thenReturn(flowOf(settings))
        val result = useCase()
        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val rate = (result as Resource.Success).data
        assertThat(rate).isWithin(0.01).of(8000.0 / (4.33 * 5 * 8))
    }

    @Test
    fun whenManualModeDayShouldComputeCorrectRate() = runTest {
        // 8000 FCFA/jour, 10h/jour → tauxHoraire = 8000 / 10 = 800
        val settings = WorkTimeSettings(
            manualIncome = 8000.0,
            manualIncomePeriod = WorkTimePeriod.DAY,
            hoursPerDay = 10.0,
            daysPerWeek = 6,
            autoDetectEnabled = false,
        )
        whenever(repository.getSettings()).thenReturn(flowOf(settings))
        val result = useCase()
        assertThat(result).isInstanceOf(Resource.Success::class.java)
        assertThat((result as Resource.Success).data).isWithin(0.01).of(800.0)
    }

    @Test
    fun whenManualModeHourShouldReturnIncomeAsRate() = runTest {
        val settings = WorkTimeSettings(
            manualIncome = 500.0,
            manualIncomePeriod = WorkTimePeriod.HOUR,
            hoursPerDay = 8.0,
            daysPerWeek = 5,
            autoDetectEnabled = false,
        )
        whenever(repository.getSettings()).thenReturn(flowOf(settings))
        val result = useCase()
        assertThat(result).isInstanceOf(Resource.Success::class.java)
        assertThat((result as Resource.Success).data).isEqualTo(500.0)
    }

    // ─── Mode auto-détection ────────────────────────────────────────────────

    @Test
    fun whenAutoDetectEnabledButNoCategorySelectedShouldReturnError() = runTest {
        val settings = WorkTimeSettings(
            hoursPerDay = 8.0, daysPerWeek = 5,
            autoDetectEnabled = true,
            autoDetectCategoryIds = emptyList(),
        )
        whenever(repository.getSettings()).thenReturn(flowOf(settings))
        val result = useCase()
        assertThat(result).isInstanceOf(Resource.Error::class.java)
        assertThat((result as Resource.Error).exception.message).contains("catégorie")
    }

    @Test
    fun whenAutoDetectEnabledAndNoIncomeThatMonthShouldReturnError() = runTest {
        val settings = WorkTimeSettings(
            hoursPerDay = 8.0, daysPerWeek = 5,
            autoDetectEnabled = true,
            autoDetectCategoryIds = listOf("cat1"),
        )
        whenever(repository.getSettings()).thenReturn(flowOf(settings))
        whenever(getAutoDetectedIncomeUseCase(listOf("cat1"))).thenReturn(Resource.Success(0.0))
        val result = useCase()
        assertThat(result).isInstanceOf(Resource.Error::class.java)
        assertThat((result as Resource.Error).exception.message).contains("Aucun revenu")
    }

    @Test
    fun whenAutoDetectEnabledAndIncomeDetectedShouldComputeCorrectRate() = runTest {
        // Revenu auto = 240000, 8h/jour, 5j/semaine → hoursPerMonth = 173.2, rate ≈ 1385.16
        val settings = WorkTimeSettings(
            hoursPerDay = 8.0, daysPerWeek = 5,
            autoDetectEnabled = true,
            autoDetectCategoryIds = listOf("cat1"),
        )
        whenever(repository.getSettings()).thenReturn(flowOf(settings))
        whenever(getAutoDetectedIncomeUseCase(listOf("cat1"))).thenReturn(Resource.Success(240000.0))
        val result = useCase()
        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val rate = (result as Resource.Success).data
        assertThat(rate).isWithin(0.01).of(240000.0 / (4.33 * 5 * 8))
    }
}

