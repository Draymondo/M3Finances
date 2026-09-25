package com.naveenapps.expensemanager.feature.export

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.designsystem.ui.utils.UiText
import com.naveenapps.expensemanager.core.domain.usecase.settings.export.ExportFileUseCase
import com.naveenapps.expensemanager.core.domain.usecase.settings.filter.daterange.GetDateRangeUseCase
import com.naveenapps.expensemanager.core.model.ExportData
import com.naveenapps.expensemanager.core.model.ExportFileType
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.navigation.AppComposeNavigator
import com.naveenapps.expensemanager.core.testing.BaseCoroutineTest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * BUG-001 : parcours export côté ViewModel.
 *
 * Couvre l'annulation du sélecteur de fichier (URI nulle → aucun export, aucune erreur),
 * la propagation d'un échec d'écriture (`Resource.Error` → événement `ExportEvent.Error`)
 * et le succès pour les deux types d'export réellement présents dans l'application
 * (CSV et PDF — il n'existe pas d'export JSON).
 */
class ExportViewModelTest : BaseCoroutineTest() {

    private val getDateRangeUseCase: GetDateRangeUseCase = mock()
    private val exportFileUseCase: ExportFileUseCase = mock()
    private val appComposeNavigator: AppComposeNavigator = mock()

    private lateinit var viewModel: ExportViewModel

    override fun onCreate() {
        super.onCreate()
        whenever(getDateRangeUseCase.invoke()).thenReturn(emptyFlow())
        viewModel = ExportViewModel(
            getDateRangeUseCase = getDateRangeUseCase,
            exportFileUseCase = exportFileUseCase,
            appComposeNavigator = appComposeNavigator,
        )
    }

    @Test
    fun `file picker cancellation exports nothing and is not an error`() = runTest {
        viewModel.event.test {
            // Résultat d'un sélecteur annulé : URI absente.
            viewModel.processAction(ExportAction.StartExport(null))

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exportFileUseCase, never()).invoke(any(), any(), any(), any(), any())
    }

    @Test
    fun `csv export success emits FileExported and targets the CSV format`() = runTest {
        whenever(exportFileUseCase.invoke(any(), any(), any(), any(), any()))
            .thenReturn(Resource.Success(ExportData(TEST_URI, null)))

        viewModel.event.test {
            viewModel.processAction(ExportAction.StartExport(TEST_URI))

            val event = awaitItem()
            assertThat(event).isInstanceOf(ExportEvent.FileExported::class.java)
            assertThat((event as ExportEvent.FileExported).exportData.uri).isEqualTo(TEST_URI)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exportFileUseCase).invoke(
            eq(ExportFileType.CSV),
            eq(TEST_URI),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `pdf export success targets the PDF format`() = runTest {
        whenever(exportFileUseCase.invoke(any(), any(), any(), any(), any()))
            .thenReturn(Resource.Success(ExportData(TEST_URI, null)))

        viewModel.processAction(ExportAction.ChangeFileType(ExportFileType.PDF))

        viewModel.event.test {
            viewModel.processAction(ExportAction.StartExport(TEST_URI))

            val event = awaitItem()
            assertThat(event).isInstanceOf(ExportEvent.FileExported::class.java)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exportFileUseCase).invoke(
            eq(ExportFileType.PDF),
            eq(TEST_URI),
            any(),
            any(),
            any(),
        )
    }

    @Test
    fun `export failure surfaces an explicit error event`() = runTest {
        whenever(exportFileUseCase.invoke(any(), any(), any(), any(), any()))
            .thenReturn(Resource.Error(Exception("write failed")))

        viewModel.event.test {
            viewModel.processAction(ExportAction.StartExport(TEST_URI))

            val event = awaitItem()
            assertThat(event).isInstanceOf(ExportEvent.Error::class.java)
            val message = (event as ExportEvent.Error).message
            assertThat(message).isInstanceOf(UiText.StringResource::class.java)
            assertThat((message as UiText.StringResource).resId)
                .isEqualTo(R.string.export_error_message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val TEST_URI = "content://com.android.providers.downloads/documents/expense_manager_1.csv"
    }
}
