package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

class UpdateWorkTimeSettingsUseCaseTest : BaseCoroutineTest() {

    private val repository: WorkTimeSettingsRepository = mock()
    private lateinit var useCase: UpdateWorkTimeSettingsUseCase

    override fun onCreate() {
        super.onCreate()
        useCase = UpdateWorkTimeSettingsUseCase(repository)
    }

    @Test
    fun whenHoursPerDayIsZeroShouldReturnError() = runTest {
        val settings = WorkTimeSettings(hoursPerDay = 0.0, daysPerWeek = 5)
        val result = useCase(settings)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
        val error = (result as Resource.Error).exception.message
        assertThat(error).contains("heures par jour")
    }

    @Test
    fun whenHoursPerDayIsNegativeShouldReturnError() = runTest {
        val settings = WorkTimeSettings(hoursPerDay = -1.0, daysPerWeek = 5)
        val result = useCase(settings)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun whenDaysPerWeekIsZeroShouldReturnError() = runTest {
        val settings = WorkTimeSettings(hoursPerDay = 8.0, daysPerWeek = 0)
        val result = useCase(settings)
        assertThat(result).isInstanceOf(Resource.Error::class.java)
        val error = (result as Resource.Error).exception.message
        assertThat(error).contains("jours par semaine")
    }

    @Test
    fun whenSettingsAreValidShouldReturnSuccess() = runTest {
        val settings = WorkTimeSettings(hoursPerDay = 8.0, daysPerWeek = 5, manualIncome = 8000.0)
        val result = useCase(settings)
        assertThat(result).isInstanceOf(Resource.Success::class.java)
    }
}

