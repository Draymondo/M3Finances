package com.naveenapps.expensemanager.core.data.repository.export

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.Resource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * BUG-001 : l'écriture CSV passe uniquement par l'URI du sélecteur SAF.
 *
 * - URI absente (sélecteur annulé ou chemin legacy supprimé) → `Resource.Error`,
 *   jamais une écriture dans `Environment.getExternalStorageDirectory()` dépendante de
 *   `WRITE_EXTERNAL_STORAGE` ;
 * - échec d'ouverture/écriture → `Resource.Error` propagée (plus d'échec silencieux
 *   retournant un faux succès).
 */
@RunWith(AndroidJUnit4::class)
class CsvExportStrategyTest {

    private val context: Context = mock()
    private val contentResolver: ContentResolver = mock()

    @Before
    fun setUp() {
        whenever(context.contentResolver).thenReturn(contentResolver)
    }

    @Test
    fun `export without SAF uri fails explicitly instead of writing legacy external storage`() {
        val result = CsvExportStrategy(context).export(uri = null, transactions = emptyList())

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    @Test
    fun `export write failure returns Resource Error instead of silent success`() {
        // Aucun descripteur de fichier ouvertable : le sélecteur SAF n'a pas produit de
        // fichier accessible en écriture.
        whenever(contentResolver.openFileDescriptor(any(), eq("w"))).thenReturn(null)

        val result = CsvExportStrategy(context)
            .export(uri = TEST_URI, transactions = emptyList())

        assertThat(result).isInstanceOf(Resource.Error::class.java)
    }

    private companion object {
        const val TEST_URI = "content://com.android.providers.downloads/documents/expense_manager_1.csv"
    }
}
