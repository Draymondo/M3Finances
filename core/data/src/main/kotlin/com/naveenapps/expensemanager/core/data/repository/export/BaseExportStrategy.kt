package com.naveenapps.expensemanager.core.data.repository.export

import android.content.Context
import androidx.core.net.toUri
import com.naveenapps.expensemanager.core.model.ExportData
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

internal fun Double.toTrimAmount(): String = String.format(Locale.getDefault(), "%.2f", this)

/**
 * Écriture du fichier d'export (BUG-001).
 *
 * L'export passe exclusivement par le sélecteur de documents du système (SAF,
 * `ACTION_CREATE_DOCUMENT`) : [uri] est l'URI retournée par le système. Il n'écrit plus
 * jamais dans `Environment.getExternalStorageDirectory()` et ne dépend donc d'aucune
 * permission de stockage (`WRITE_EXTERNAL_STORAGE`), y compris sur Android 10+.
 *
 * Toute erreur (URI absente, fichier inaccessible, échec d'écriture) est propagée à
 * l'appelant : les stratégies la convertissent en `Resource.Error`, qui se traduit par un
 * message d'erreur explicite pour l'utilisateur — plus d'échec silencieux.
 */
abstract class BaseExportStrategy(protected val context: Context) : ExportStrategy {

    protected fun generateFile(
        uri: String?,
        write: (FileOutputStream) -> Unit,
    ): ExportData {
        val exportUri = uri?.toUri()
            ?: throw IOException("Aucun fichier sélectionné pour l'export (URI absente)")
        val descriptor = context.contentResolver.openFileDescriptor(exportUri, "w")
            ?: throw IOException("Impossible d'ouvrir le fichier sélectionné en écriture")
        descriptor.use { parcelFileDescriptor ->
            write.invoke(FileOutputStream(parcelFileDescriptor.fileDescriptor))
        }
        return ExportData(uri, null)
    }
}
