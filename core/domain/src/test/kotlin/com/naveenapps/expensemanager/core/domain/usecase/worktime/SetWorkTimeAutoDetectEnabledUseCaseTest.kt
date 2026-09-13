package com.naveenapps.expensemanager.core.domain.usecase.worktime

import com.naveenapps.expensemanager.core.repository.WorkTimeSettingsRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SetWorkTimeAutoDetectEnabledUseCaseTest : BaseCoroutineTest() {

    private val repository: WorkTimeSettingsRepository = mock()
    private lateinit var useCase: SetWorkTimeAutoDetectEnabledUseCase

    override fun onCreate() {
        super.onCreate()
        useCase = SetWorkTimeAutoDetectEnabledUseCase(repository)
    }

    @Test
    fun whenInvokedShouldDelegateToRepository() = runTest {
        useCase(true)
        verify(repository).setAutoDetectEnabled(true)

        useCase(false)
        verify(repository).setAutoDetectEnabled(false)
    }
}

