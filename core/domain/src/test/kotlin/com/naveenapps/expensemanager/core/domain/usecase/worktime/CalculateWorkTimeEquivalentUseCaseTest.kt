package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.WorkTimePeriod
import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import com.naveenapps.expensemanager.core.model.WorkTimeUnit
import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CalculateWorkTimeEquivalentUseCaseTest : BaseCoroutineTest() {

    private val repository: WorkTimeSettingsRepository = mock()
    private val getAutoDetectedIncomeUseCase: GetAutoDetectedIncomeUseCase = mock()
    private lateinit var getEffectiveHourlyRateUseCase: GetEffectiveHourlyRateUseCase
    private lateinit var useCase: CalculateWorkTimeEquivalentUseCase

    // Réglages de base : 8000 FCFA/mois, 8h/jour, 5j/sem
    // hoursPerMonth = 4.33 * 5 * 8 = 173.2
    // tauxHoraire = 8000 / 173.2 ≈ 46.19
    private val baseSettings = WorkTimeSettings(
        manualIncome = 8000.0,
        manualIncomePeriod = WorkTimePeriod.MONTH,
        hoursPerDay = 8.0,
        daysPerWeek = 5,
        autoDetectEnabled = false,
    )

    override fun onCreate() {
        super.onCreate()
        getEffectiveHourlyRateUseCase =
            GetEffectiveHourlyRateUseCase(repository, getAutoDetectedIncomeUseCase)
        useCase = CalculateWorkTimeEquivalentUseCase(repository, getEffectiveHourlyRateUseCase)
    }

    @Test
    fun whenPriceIsZeroShouldReturnError() = runTest {
        val result = useCase(0.0, WorkTimeUnit.HOUR)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenPriceIsNegativeShouldReturnError() = runTest {
        val result = useCase(-100.0, WorkTimeUnit.HOUR)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenUnitIsHourShouldReturnCorrectValue() = runTest {
        // prix = 1000 FCFA, rate ≈ 46.19 → heuresTotal ≈ 21.65h
        whenever(repository.getSettings()).thenReturn(flowOf(baseSettings))
        val result = useCase(1000.0, WorkTimeUnit.HOUR)
        assertThat(result).isInstanceOf(Resource.Success::class.java)
        val expected = 1000.0 / (8000.0 / (4.33 * 5 * 8))
        assertThat((result as Resource.Success).data.value).isWithin(0.01).of(expected)
        assertThat(result.data.unit).isEqualTo(WorkTimeUnit.HOUR)
    }

    @Test
    fun whenUnitIsMinuteShouldReturnHoursTimessixty() = runTest {
        whenever(repository.getSettings()).thenReturn(flowOf(baseSettings))
        val resultHour = useCase(1000.0, WorkTimeUnit.HOUR)
        val resultMin = useCase(1000.0, WorkTimeUnit.MINUTE)
        assertThat(resultHour).isInstanceOf(Resource.Success::class.java)
        assertThat(resultMin).isInstanceOf(Resource.Success::class.java)
        val hoursValue = (resultHour as Resource.Success).data.value
        val minValue = (resultMin as Resource.Success).data.value
        assertThat(minValue).isWithin(0.01).of(hoursValue * 60)
    }

    @Test
    fun whenUnitIsDayShouldDivideByHoursPerDay() = runTest {
        whenever(repository.getSettings()).thenReturn(flowOf(baseSettings))
        val resultHour = useCase(1000.0, WorkTimeUnit.HOUR)
        val resultDay = useCase(1000.0, WorkTimeUnit.DAY)
        assertThat(resultHour).isInstanceOf(Resource.Success::class.java)
        assertThat(resultDay).isInstanceOf(Resource.Success::class.java)
        val hoursValue = (resultHour as Resource.Success).data.value
        val dayValue = (resultDay as Resource.Success).data.value
        assertThat(dayValue).isWithin(0.01).of(hoursValue / 8.0)
    }

    @Test
    fun whenUnitIsMonthShouldDivideByHoursPerMonth() = runTest {
        whenever(repository.getSettings()).thenReturn(flowOf(baseSettings))
        val resultHour = useCase(1000.0, WorkTimeUnit.HOUR)
        val resultMonth = useCase(1000.0, WorkTimeUnit.MONTH)
        assertThat(resultHour).isInstanceOf(Resource.Success::class.java)
        assertThat(resultMonth).isInstanceOf(Resource.Success::class.java)
        val hoursValue = (resultHour as Resource.Success).data.value
        val monthValue = (resultMonth as Resource.Success).data.value
        val hoursPerMonth = 4.33 * 5 * 8
        assertThat(monthValue).isWithin(0.01).of(hoursValue / hoursPerMonth)
    }

    @Test
    fun whenSettingsInvalidShouldPropagateError() = runTest {
        val badSettings = WorkTimeSettings(hoursPerDay = 0.0, daysPerWeek = 5)
        whenever(repository.getSettings()).thenReturn(flowOf(badSettings))
        val result = useCase(1000.0, WorkTimeUnit.HOUR)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }
}

