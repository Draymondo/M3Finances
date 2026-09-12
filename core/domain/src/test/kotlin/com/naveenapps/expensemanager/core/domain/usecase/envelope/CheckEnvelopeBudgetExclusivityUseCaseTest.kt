package com.naveenapps.expensemanager.core.domain.usecase.envelope

import com.google.common.truth.Truth
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.BudgetRepository
import com.naveenapps.expensemanager.core.repository.EnvelopeRepository
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import com.naveenapps.expensemanager.core.testing.FAKE_BUDGET
import com.naveenapps.expensemanager.core.testing.FAKE_ENVELOPE
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CheckEnvelopeBudgetExclusivityUseCaseTest : BaseCoroutineTest() {

    private val envelopeRepository: EnvelopeRepository = mock()
    private val budgetRepository: BudgetRepository = mock()
    private val checkEnvelopeBudgetExclusivityUseCase =
        CheckEnvelopeBudgetExclusivityUseCase(envelopeRepository, budgetRepository)

    @Test
    fun checkForEnvelopeShouldFailWhenAClassicBudgetCoversTheCategory() = runTest {
        whenever(budgetRepository.getBudgets()).thenReturn(
            flowOf(listOf(FAKE_BUDGET.copy(isAllCategoriesSelected = false, categories = listOf(FAKE_ENVELOPE.categoryId)))),
        )

        val response = checkEnvelopeBudgetExclusivityUseCase.checkForEnvelope(
            categoryId = FAKE_ENVELOPE.categoryId,
            selectedMonth = FAKE_ENVELOPE.selectedMonth,
            periodType = FAKE_ENVELOPE.periodType,
        )

        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun checkForEnvelopeShouldFailWhenAnAllCategoriesBudgetExistsForThePeriod() = runTest {
        whenever(budgetRepository.getBudgets()).thenReturn(
            flowOf(listOf(FAKE_BUDGET.copy(isAllCategoriesSelected = true))),
        )

        val response = checkEnvelopeBudgetExclusivityUseCase.checkForEnvelope(
            categoryId = FAKE_ENVELOPE.categoryId,
            selectedMonth = FAKE_BUDGET.selectedMonth,
            periodType = FAKE_BUDGET.periodType,
        )

        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun checkForEnvelopeShouldSucceedWhenNoBudgetCoversThePeriod() = runTest {
        whenever(budgetRepository.getBudgets()).thenReturn(flowOf(emptyList()))

        val response = checkEnvelopeBudgetExclusivityUseCase.checkForEnvelope(
            categoryId = FAKE_ENVELOPE.categoryId,
            selectedMonth = FAKE_ENVELOPE.selectedMonth,
            periodType = FAKE_ENVELOPE.periodType,
        )

        Truth.assertThat(response).isInstanceOf(Resource.Success::class.java)
    }

    @Test
    fun checkForBudgetShouldFailWhenAnEnvelopeExistsOnOneOfTheCategories() = runTest {
        whenever(
            envelopeRepository.findEnvelopesByPeriod(any(), any()),
        ).thenReturn(listOf(FAKE_ENVELOPE))

        val response = checkEnvelopeBudgetExclusivityUseCase.checkForBudget(
            categories = listOf(FAKE_ENVELOPE.categoryId),
            isAllCategoriesSelected = false,
            selectedMonth = FAKE_ENVELOPE.selectedMonth,
            periodType = FAKE_ENVELOPE.periodType,
        )

        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun checkForBudgetShouldFailWhenIsAllCategoriesSelectedAndAnEnvelopeExistsForThePeriod() = runTest {
        whenever(
            envelopeRepository.findEnvelopesByPeriod(any(), any()),
        ).thenReturn(listOf(FAKE_ENVELOPE))

        val response = checkEnvelopeBudgetExclusivityUseCase.checkForBudget(
            categories = emptyList(),
            isAllCategoriesSelected = true,
            selectedMonth = FAKE_ENVELOPE.selectedMonth,
            periodType = FAKE_ENVELOPE.periodType,
        )

        Truth.assertThat(response).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun checkForBudgetShouldSucceedWhenNoEnvelopeExistsForThePeriod() = runTest {
        whenever(
            envelopeRepository.findEnvelopesByPeriod(any(), any()),
        ).thenReturn(emptyList())

        val response = checkEnvelopeBudgetExclusivityUseCase.checkForBudget(
            categories = listOf(FAKE_ENVELOPE.categoryId),
            isAllCategoriesSelected = false,
            selectedMonth = FAKE_ENVELOPE.selectedMonth,
            periodType = FAKE_ENVELOPE.periodType,
        )

        Truth.assertThat(response).isInstanceOf(Resource.Success::class.java)
    }
}
