package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.WorkTimeSettings
import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetWorkTimeSettingsUseCaseTest : BaseCoroutineTest() {

    private val repository: WorkTimeSettingsRepository = mock()
    private lateinit var useCase: GetWorkTimeSettingsUseCase

    override fun onCreate() {
        super.onCreate()
        useCase = GetWorkTimeSettingsUseCase(repository)
    }

    @Test
    fun whenRepositoryEmitsSettingsShouldReturnThem() = runTest {
        val settings = WorkTimeSettings(manualIncome = 8000.0)
        whenever(repository.getSettings()).thenReturn(flowOf(settings))

        val result = mutableListOf<WorkTimeSettings>()
        useCase().collect { result.add(it) }

        assertThat(result).hasSize(1)
        assertThat(result[0].manualIncome).isEqualTo(8000.0)
    }
}

