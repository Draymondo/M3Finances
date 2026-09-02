<#
    apply-source-confidence.ps1

    Ajoute les champs `source` (TransactionSource) et `confidence` (Float?) au flux
    PendingTransaction : modele -> entite Room (+ migration 13->14) -> service
    d'ecoute des notifications -> parsing Gemini -> ecran d'affichage.

    A LANCER DEPUIS LA RACINE DU REPO (le dossier qui contient app/, core/, feature/),
    dans le terminal integre de VS Code (PowerShell).

    Usage :
        cd C:\chemin\vers\M3Finances
        .\apply-source-confidence.ps1

    Le script est idempotent-safe : si un motif n'est pas trouve (deja applique,
    ou fichier different), il previent et passe au suivant sans planter.
#>

$ErrorActionPreference = "Continue"

function Replace-Content {
    param(
        [Parameter(Mandatory=$true)][string]$Path,
        [Parameter(Mandatory=$true)][string]$Old,
        [Parameter(Mandatory=$true)][string]$New
    )
    if (-not (Test-Path $Path)) {
        Write-Warning "Fichier introuvable, ignore : $Path"
        return
    }
    $content = Get-Content -Raw -Path $Path
    if ($content.Contains($Old)) {
        $newContent = $content.Replace($Old, $New)
        Set-Content -Path $Path -Value $newContent -NoNewline
        Write-Host "OK   - $Path" -ForegroundColor Green
    } else {
        Write-Warning "Motif non trouve (deja applique ou fichier different) : $Path"
    }
}

function New-FileWithContent {
    param(
        [Parameter(Mandatory=$true)][string]$Path,
        [Parameter(Mandatory=$true)][string]$Content
    )
    $dir = Split-Path -Path $Path -Parent
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    if (Test-Path $Path) {
        Write-Warning "Fichier deja existant, non ecrase : $Path"
        return
    }
    Set-Content -Path $Path -Value $Content -NoNewline
    Write-Host "CREE - $Path" -ForegroundColor Cyan
}

Write-Host "=== 1/11 : nouveau fichier TransactionSource.kt ===" -ForegroundColor Yellow
$transactionSourceContent = @'
package com.naveenapps.expensemanager.core.model

enum class TransactionSource {
    WAVE,
    ORANGE_MONEY,
    MTN_MOMO,
    MOOV_MONEY,
    DJAMO,
    PAYPAL,
    SMS,
    UNKNOWN
}
'@
New-FileWithContent -Path "core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model/TransactionSource.kt" -Content $transactionSourceContent

Write-Host "=== 2/11 : nouveau fichier TransactionSourceConverter.kt ===" -ForegroundColor Yellow
$converterContent = @'
package com.naveenapps.expensemanager.core.database.utils

import androidx.room.TypeConverter
import com.naveenapps.expensemanager.core.model.TransactionSource

object TransactionSourceConverter {

    @TypeConverter
    fun ordinalToTransactionSource(value: Int?): TransactionSource? {
        return value?.let { TransactionSource.entries[it] }
    }

    @TypeConverter
    fun transactionSourceToOrdinal(source: TransactionSource?): Int? {
        return source?.ordinal
    }
}
'@
New-FileWithContent -Path "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/utils/TransactionSourceConverter.kt" -Content $converterContent

Write-Host "=== 3/11 : PendingTransaction.kt (modele) ===" -ForegroundColor Yellow
Replace-Content -Path "core/model/src/main/kotlin/com/naveenapps/expensemanager/core/model/PendingTransaction.kt" `
    -Old @'
data class PendingTransaction(
    val id: String,
    val amount: Double,
    val fee: Double?,
    val merchant: String?,
    val date: Date,
    val transactionType: TransactionType,
    val suggestedCategory: String?,
    val rawNotification: String?,
)
'@ `
    -New @'
data class PendingTransaction(
    val id: String,
    val amount: Double,
    val fee: Double?,
    val merchant: String?,
    val date: Date,
    val transactionType: TransactionType,
    val suggestedCategory: String?,
    val rawNotification: String?,
    val source: TransactionSource = TransactionSource.UNKNOWN,
    val confidence: Float? = null,
)
'@

Write-Host "=== 4/11 : PendingTransactionEntity.kt (entite Room) ===" -ForegroundColor Yellow
Replace-Content -Path "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/entity/PendingTransactionEntity.kt" `
    -Old @'
import com.naveenapps.expensemanager.core.model.TransactionType
'@ `
    -New @'
import com.naveenapps.expensemanager.core.model.TransactionType
import com.naveenapps.expensemanager.core.model.TransactionSource
'@

Replace-Content -Path "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/entity/PendingTransactionEntity.kt" `
    -Old @'
    @ColumnInfo(name = "raw_notification")
    val rawNotification: String?,
    @ColumnInfo(name = "created_on")
    val createdOn: Long
)
'@ `
    -New @'
    @ColumnInfo(name = "raw_notification")
    val rawNotification: String?,
    @ColumnInfo(name = "source")
    val source: TransactionSource,
    @ColumnInfo(name = "confidence")
    val confidence: Float?,
    @ColumnInfo(name = "created_on")
    val createdOn: Long
)
'@

Write-Host "=== 5/11 : DatabaseMigrations.kt (migration 13->14) ===" -ForegroundColor Yellow
Replace-Content -Path "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/DatabaseMigrations.kt" `
    -Old @'
internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_transaction` (" +
                "`id` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`fee` REAL, " +
                "`merchant` TEXT, " +
                "`date` INTEGER NOT NULL, " +
                "`transaction_type` INTEGER NOT NULL, " +
                "`suggested_category` TEXT, " +
                "`raw_notification` TEXT, " +
                "`created_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}
'@ `
    -New @'
internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_transaction` (" +
                "`id` TEXT NOT NULL, " +
                "`amount` REAL NOT NULL, " +
                "`fee` REAL, " +
                "`merchant` TEXT, " +
                "`date` INTEGER NOT NULL, " +
                "`transaction_type` INTEGER NOT NULL, " +
                "`suggested_category` TEXT, " +
                "`raw_notification` TEXT, " +
                "`created_on` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

internal val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `pending_transaction` ADD COLUMN `source` INTEGER NOT NULL DEFAULT 7"
        )
        db.execSQL(
            "ALTER TABLE `pending_transaction` ADD COLUMN `confidence` REAL"
        )
    }
}
'@

Write-Host "=== 6/11 : ExpenseManagerDatabase.kt (version + TypeConverters) ===" -ForegroundColor Yellow
Replace-Content -Path "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/ExpenseManagerDatabase.kt" `
    -Old @'
import com.naveenapps.expensemanager.core.database.utils.TransactionTypeConverter
'@ `
    -New @'
import com.naveenapps.expensemanager.core.database.utils.TransactionTypeConverter
import com.naveenapps.expensemanager.core.database.utils.TransactionSourceConverter
'@

Replace-Content -Path "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/ExpenseManagerDatabase.kt" `
    -Old @'
    version = 13,
    exportSchema = true,
'@ `
    -New @'
    version = 14,
    exportSchema = true,
'@

Replace-Content -Path "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/ExpenseManagerDatabase.kt" `
    -Old @'
@TypeConverters(
    DateConverter::class,
    TransactionTypeConverter::class,
    CategoryTypeConverter::class,
    AccountTypeConverter::class,
    RecurrenceFrequencyConverter::class,
    DebtDirectionConverter::class,
)
'@ `
    -New @'
@TypeConverters(
    DateConverter::class,
    TransactionTypeConverter::class,
    CategoryTypeConverter::class,
    AccountTypeConverter::class,
    RecurrenceFrequencyConverter::class,
    DebtDirectionConverter::class,
    TransactionSourceConverter::class,
)
'@

Write-Host "=== 7/11 : KoinDatabaseModule.kt (enregistrement migration) ===" -ForegroundColor Yellow
Replace-Content -Path "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/di/KoinDatabaseModule.kt" `
    -Old @'
import com.naveenapps.expensemanager.core.database.MIGRATION_12_13
import org.koin.android.ext.koin.androidContext
'@ `
    -New @'
import com.naveenapps.expensemanager.core.database.MIGRATION_12_13
import com.naveenapps.expensemanager.core.database.MIGRATION_13_14
import org.koin.android.ext.koin.androidContext
'@

Replace-Content -Path "core/database/src/main/kotlin/com/naveenapps/expensemanager/core/database/di/KoinDatabaseModule.kt" `
    -Old @'
            MIGRATION_12_13,
        ).build()
'@ `
    -New @'
            MIGRATION_12_13,
            MIGRATION_13_14,
        ).build()
'@

Write-Host "=== 8/11 : GeminiRepository.kt (interface) ===" -ForegroundColor Yellow
Replace-Content -Path "core/repository/src/main/kotlin/com/naveenapps/expensemanager/core/repository/GeminiRepository.kt" `
    -Old @'
    suspend fun parseWaveNotification(notificationText: String, apiKey: String): Resource<com.naveenapps.expensemanager.core.model.PendingTransaction>
'@ `
    -New @'
    suspend fun parseWaveNotification(
        notificationText: String,
        source: com.naveenapps.expensemanager.core.model.TransactionSource,
        apiKey: String,
    ): Resource<com.naveenapps.expensemanager.core.model.PendingTransaction>
'@

Write-Host "=== 9/11 : ParseWaveNotificationUseCase.kt ===" -ForegroundColor Yellow
Replace-Content -Path "core/domain/src/main/kotlin/com/naveenapps/expensemanager/core/domain/usecase/transaction/ParseWaveNotificationUseCase.kt" `
    -Old @'
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
class ParseWaveNotificationUseCase(
    private val settingsRepository: SettingsRepository,
    private val geminiRepository: GeminiRepository,
) {
    suspend fun invoke(notificationText: String): Resource<PendingTransaction> {
        val apiKey = settingsRepository.getGeminiApiKey().firstOrNull().orEmpty()
        if (apiKey.isBlank()) {
            return Resource.Error(
                Exception("Clé d'API Gemini non configurée.")
            )
        }
        return geminiRepository.parseWaveNotification(notificationText, apiKey)
    }
}
'@ `
    -New @'
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.repository.GeminiRepository
import com.naveenapps.expensemanager.core.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
class ParseWaveNotificationUseCase(
    private val settingsRepository: SettingsRepository,
    private val geminiRepository: GeminiRepository,
) {
    suspend fun invoke(notificationText: String, source: TransactionSource): Resource<PendingTransaction> {
        val apiKey = settingsRepository.getGeminiApiKey().firstOrNull().orEmpty()
        if (apiKey.isBlank()) {
            return Resource.Error(
                Exception("Clé d'API Gemini non configurée.")
            )
        }
        return geminiRepository.parseWaveNotification(notificationText, source, apiKey)
    }
}
'@

Write-Host "=== 10/11 : GeminiRepositoryImpl.kt (parsing + calcul confidence) ===" -ForegroundColor Yellow
Replace-Content -Path "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository/GeminiRepositoryImpl.kt" `
    -Old @'
    override suspend fun parseWaveNotification(
        notificationText: String,
        apiKey: String
    ): Resource<com.naveenapps.expensemanager.core.model.PendingTransaction> =
'@ `
    -New @'
    override suspend fun parseWaveNotification(
        notificationText: String,
        source: com.naveenapps.expensemanager.core.model.TransactionSource,
        apiKey: String
    ): Resource<com.naveenapps.expensemanager.core.model.PendingTransaction> =
'@

Replace-Content -Path "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository/GeminiRepositoryImpl.kt" `
    -Old @'
                Resource.Success(
                    com.naveenapps.expensemanager.core.model.PendingTransaction(
                        id = java.util.UUID.randomUUID().toString(),
                        amount = parsed.amount,
                        fee = resolvedFee,
                        merchant = parsed.merchant,
                        date = parsedDate,
                        transactionType = transactionType,
                        suggestedCategory = parsed.category,
                        rawNotification = notificationText
                    )
                )
'@ `
    -New @'
                // Confiance calculee localement selon les champs reellement extraits
                // par Gemini (pas un pourcentage auto-declare par le modele).
                val filledFieldCount = listOfNotNull(
                    parsed.merchant?.takeIf { it.isNotBlank() },
                    parsed.date,
                    parsed.category,
                ).size
                val confidence = (0.4f + filledFieldCount * 0.2f).coerceAtMost(1f)

                Resource.Success(
                    com.naveenapps.expensemanager.core.model.PendingTransaction(
                        id = java.util.UUID.randomUUID().toString(),
                        amount = parsed.amount,
                        fee = resolvedFee,
                        merchant = parsed.merchant,
                        date = parsedDate,
                        transactionType = transactionType,
                        suggestedCategory = parsed.category,
                        rawNotification = notificationText,
                        source = source,
                        confidence = confidence
                    )
                )
'@

Write-Host "=== 11/11 : PendingTransactionRepositoryImpl.kt (mapping entite <-> modele) ===" -ForegroundColor Yellow
Replace-Content -Path "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository/PendingTransactionRepositoryImpl.kt" `
    -Old @'
    private fun PendingTransactionEntity.toDomainModel(): PendingTransaction {
        return PendingTransaction(
            id = id,
            amount = amount,
            fee = fee,
            merchant = merchant,
            date = Date(date),
            transactionType = transactionType,
            suggestedCategory = suggestedCategory,
            rawNotification = rawNotification
        )
    }
'@ `
    -New @'
    private fun PendingTransactionEntity.toDomainModel(): PendingTransaction {
        return PendingTransaction(
            id = id,
            amount = amount,
            fee = fee,
            merchant = merchant,
            date = Date(date),
            transactionType = transactionType,
            suggestedCategory = suggestedCategory,
            rawNotification = rawNotification,
            source = source,
            confidence = confidence
        )
    }
'@

Replace-Content -Path "core/data/src/main/kotlin/com/naveenapps/expensemanager/core/data/repository/PendingTransactionRepositoryImpl.kt" `
    -Old @'
    private fun PendingTransaction.toEntityModel(): PendingTransactionEntity {
        return PendingTransactionEntity(
            id = id,
            amount = amount,
            fee = fee,
            merchant = merchant,
            date = date.time,
            transactionType = transactionType,
            suggestedCategory = suggestedCategory,
            rawNotification = rawNotification,
            createdOn = System.currentTimeMillis()
        )
    }
'@ `
    -New @'
    private fun PendingTransaction.toEntityModel(): PendingTransactionEntity {
        return PendingTransactionEntity(
            id = id,
            amount = amount,
            fee = fee,
            merchant = merchant,
            date = date.time,
            transactionType = transactionType,
            suggestedCategory = suggestedCategory,
            rawNotification = rawNotification,
            source = source,
            confidence = confidence,
            createdOn = System.currentTimeMillis()
        )
    }
'@

Write-Host "=== 12 : WaveNotificationListenerService.kt (detection de la source) ===" -ForegroundColor Yellow
Replace-Content -Path "app/src/main/kotlin/com/naveenapps/expensemanager/service/WaveNotificationListenerService.kt" `
    -Old @'
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
'@ `
    -New @'
import com.naveenapps.expensemanager.core.domain.usecase.transaction.ParseWaveNotificationUseCase
import com.naveenapps.expensemanager.core.model.Resource
import com.naveenapps.expensemanager.core.model.TransactionSource
import com.naveenapps.expensemanager.core.repository.PendingTransactionRepository
'@

Replace-Content -Path "app/src/main/kotlin/com/naveenapps/expensemanager/service/WaveNotificationListenerService.kt" `
    -Old @'
            if (fullText.isNotBlank()) {
                Log.d("WaveNotification", "Intercepted notification from: $packageName -> $fullText")
                processNotification(fullText)
            }
        }
    }

    private fun processNotification(text: String) {
        scope.launch {
            when (val result = parseWaveNotificationUseCase.invoke(text)) {
'@ `
    -New @'
            if (fullText.isNotBlank()) {
                Log.d("WaveNotification", "Intercepted notification from: $packageName -> $fullText")
                processNotification(fullText, resolveSource(packageName))
            }
        }
    }

    private fun resolveSource(packageName: String): TransactionSource {
        return when (packageName) {
            "com.wave.personal" -> TransactionSource.WAVE
            "com.orange.myorange.oci" -> TransactionSource.ORANGE_MONEY
            "mtnft.momo.consumer" -> TransactionSource.MTN_MOMO
            "ci.moovmoney.mmpayapi", "com.mobiblanc.moov.mymoov_ci" -> TransactionSource.MOOV_MONEY
            "com.djamo.app" -> TransactionSource.DJAMO
            "com.paypal.android.p2pmobile" -> TransactionSource.PAYPAL
            "com.google.android.apps.messaging", "com.samsung.android.messaging" -> TransactionSource.SMS
            else -> TransactionSource.UNKNOWN
        }
    }

    private fun processNotification(text: String, source: TransactionSource) {
        scope.launch {
            when (val result = parseWaveNotificationUseCase.invoke(text, source)) {
'@

Write-Host "=== 13 : PendingTransactionListScreen.kt (affichage source + confiance) ===" -ForegroundColor Yellow
Replace-Content -Path "feature/transaction/src/main/kotlin/com/naveenapps/expensemanager/feature/transaction/pending/PendingTransactionListScreen.kt" `
    -Old @'
import com.naveenapps.expensemanager.core.model.PendingTransaction
'@ `
    -New @'
import com.naveenapps.expensemanager.core.model.PendingTransaction
import com.naveenapps.expensemanager.core.model.TransactionSource
'@

Replace-Content -Path "feature/transaction/src/main/kotlin/com/naveenapps/expensemanager/feature/transaction/pending/PendingTransactionListScreen.kt" `
    -Old @'
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant ?: "Inconnu",
                    style = MaterialTheme.typography.titleMedium
                )
'@ `
    -New @'
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sourceLabel(transaction.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = transaction.merchant ?: "Inconnu",
                    style = MaterialTheme.typography.titleMedium
                )
'@

Replace-Content -Path "feature/transaction/src/main/kotlin/com/naveenapps/expensemanager/feature/transaction/pending/PendingTransactionListScreen.kt" `
    -Old @'
                Text(
                    text = "Catégorie suggérée: ${transaction.suggestedCategory ?: "Aucune"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
'@ `
    -New @'
                Text(
                    text = "Catégorie suggérée: ${transaction.suggestedCategory ?: "Aucune"}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (transaction.confidence != null) {
                    Text(
                        text = "Confiance: ${(transaction.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
'@

$screenPath = "feature/transaction/src/main/kotlin/com/naveenapps/expensemanager/feature/transaction/pending/PendingTransactionListScreen.kt"
if (Test-Path $screenPath) {
    $helperFunction = @'


private fun sourceLabel(source: TransactionSource): String {
    return when (source) {
        TransactionSource.WAVE -> "🔵 Wave"
        TransactionSource.ORANGE_MONEY -> "🟠 Orange Money"
        TransactionSource.MTN_MOMO -> "🟡 MTN MoMo"
        TransactionSource.MOOV_MONEY -> "🟢 Moov Money"
        TransactionSource.DJAMO -> "🔷 Djamo"
        TransactionSource.PAYPAL -> "🔵 PayPal"
        TransactionSource.SMS -> "✉️ SMS"
        TransactionSource.UNKNOWN -> "❓ Source inconnue"
    }
}
'@
    Add-Content -Path $screenPath -Value $helperFunction -NoNewline
    Write-Host "OK   - fonction sourceLabel() ajoutee a la fin de $screenPath" -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Termine ===" -ForegroundColor Yellow
Write-Host "Verifie les warnings ci-dessus (motif non trouve = fichier deja modifie ou different)." -ForegroundColor Yellow
Write-Host "Prochaine etape : compiler (./gradlew :core:database:compileDebugKotlin puis build complet) et tester sur le telephone."
