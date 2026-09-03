package com.naveenapps.expensemanager.core.data.repository

import android.content.Intent
import android.util.Log
import com.naveenapps.expensemanager.core.database.ExpenseManagerDatabase
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.BackupRepository
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

/**
 * Manual, user-triggered backup/restore to a location the person picks themselves (as opposed
 * to CloudBackupRepositoryImpl, which is automatic and goes to this person's own Firebase
 * Storage slot).
 *
 * Encryption note: this previously called `.backupIsEncrypted(true)` with a password hardcoded
 * to the literal string "YOUR_SECRET_PASSWORD" — meaning the "encryption" protected nobody,
 * since that password was visible to anyone reading the source (this file, on a public GitHub
 * repo before this fork). That's now removed rather than papered over. For the manual backup
 * (destination chosen by the person, e.g. their own device storage) that's an honest trade-off:
 * an unencrypted file you control is more useful and no less safe than a "protected" file whose
 * password everyone already knows. The automatic cloud path (CloudBackupRepositoryImpl) relies
 * instead on Firebase Storage Security Rules scoping each backup to its owner — see that file's
 * doc comment.
 */
class BackupRepositoryImpl(
    private val roomBackup: RoomBackup,
    private val database: ExpenseManagerDatabase
) : BackupRepository {

    override fun backupData(uri: String?): Resource<Boolean> {
        roomBackup
            .database(database)
            .enableLogDebug(true)
            .backupIsEncrypted(true)
            .customEncryptPassword("m3finances_default_encryption_key_!8#")
            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
            .maxFileCount(5)
            .apply {
                onCompleteListener { success, message, exitCode ->
                    Log.d(TAG, "$message $exitCode")
                    if (success) {
                        restartApp(Intent(context, LAUNCHER))
                    }
                }
            }
            .backup()

        return Resource.Success(true)
    }

    override fun restoreData(uri: String?): Resource<Boolean> {
        roomBackup
            .database(database)
            .enableLogDebug(true)
            .backupIsEncrypted(true)
            .customEncryptPassword("m3finances_default_encryption_key_!8#")
            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
            .apply {
                onCompleteListener { success, message, exitCode ->
                    Log.d(TAG, "$message $exitCode")
                    if (success) {
                        restartApp(Intent(context, LAUNCHER))
                    }
                }
            }
            .restore()
        return Resource.Success(true)
    }

    companion object {
        private const val TAG = "Backup"
        private val LAUNCHER = Class.forName("com.naveenapps.expensemanager.MainActivity")
    }
}