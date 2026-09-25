package com.naveenapps.expensemanager.feature.export

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.naveenapps.expensemanager.core.model.ExportFileType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * BUG-001 : l'export doit passer par `ACTION_CREATE_DOCUMENT` (SAF) sur toutes les versions
 * d'Android, sans jamais dépendre de `WRITE_EXTERNAL_STORAGE`.
 *
 * Robolectric est calé sur Android 10 (API 29, `Build.VERSION_CODES.Q`) : la version pour
 * laquelle la permission de stockage n'est plus accordable était la cible du correctif.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ExportFileCreateIntentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `csv export uses ACTION_CREATE_DOCUMENT with csv mime type and file name`() {
        val intent = getFileCreateIntent(ExportFileType.CSV)

        assertThat(intent.action).isEqualTo(Intent.ACTION_CREATE_DOCUMENT)
        assertThat(intent.categories).contains(Intent.CATEGORY_OPENABLE)
        assertThat(intent.type).isEqualTo("application/csv")
        assertThat(intent.getStringExtra(Intent.EXTRA_TITLE))
            .startsWith("expense_manager_")
        assertThat(intent.getStringExtra(Intent.EXTRA_TITLE))
            .endsWith(".csv")
    }

    @Test
    fun `pdf export uses ACTION_CREATE_DOCUMENT with pdf mime type and file name`() {
        val intent = getFileCreateIntent(ExportFileType.PDF)

        assertThat(intent.action).isEqualTo(Intent.ACTION_CREATE_DOCUMENT)
        assertThat(intent.categories).contains(Intent.CATEGORY_OPENABLE)
        assertThat(intent.type).isEqualTo("application/pdf")
        assertThat(intent.getStringExtra(Intent.EXTRA_TITLE))
            .startsWith("expense_manager_")
        assertThat(intent.getStringExtra(Intent.EXTRA_TITLE))
            .endsWith(".pdf")
    }

    @Test
    fun `android 10 export relies on SAF without WRITE_EXTERNAL_STORAGE`() {
        // La permission de stockage n'est pas disponible sur Android 10+…
        assertThat(context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .isEqualTo(PackageManager.PERMISSION_DENIED)

        // …et l'export n'en a pas besoin : l'intent SAF est toujours généré.
        val intent = getFileCreateIntent(ExportFileType.CSV)
        assertThat(intent.action).isEqualTo(Intent.ACTION_CREATE_DOCUMENT)
    }
}
