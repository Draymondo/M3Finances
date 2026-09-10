<#
    rename-notification-service.ps1

    Renomme WaveNotificationListenerService -> MobileMoneyNotificationListenerService
    (classe + fichier + declaration AndroidManifest.xml + tags de log).

    A LANCER DEPUIS LA RACINE DU PROJET (le dossier qui contient app/, core/, feature/).

    IMPORTANT : ce renommage change le ComponentName Android du service. Apres avoir
    installe la nouvelle version, il faudra reactiver manuellement l'autorisation
    "Acces aux notifications" pour l'app (elle sera revoquee automatiquement pour
    l'ancien nom de composant).
#>

$ErrorActionPreference = "Stop"

$oldFile = "app/src/main/kotlin/com/naveenapps/expensemanager/service/WaveNotificationListenerService.kt"
$newFile = "app/src/main/kotlin/com/naveenapps/expensemanager/service/MobileMoneyNotificationListenerService.kt"
$manifestFile = "app/src/main/AndroidManifest.xml"

if (-not (Test-Path $oldFile)) {
    Write-Error "Fichier introuvable : $oldFile (verifie que tu es a la racine du projet)"
    exit 1
}

Write-Host "=== 1/2 : renommage de la classe + du fichier Kotlin ===" -ForegroundColor Yellow

$content = (Get-Content -Raw -Path $oldFile) -replace "`r`n", "`n"

$beforeClass = ($content -split "WaveNotificationListenerService").Count - 1
$beforeTag = ($content -split '"WaveNotification"').Count - 1
Write-Host "Occurrences trouvees - classe: $beforeClass, tag de log: $beforeTag"

$content = $content.Replace("WaveNotificationListenerService", "MobileMoneyNotificationListenerService")
$content = $content.Replace('"WaveNotification"', '"MobileMoneyNotification"')

Set-Content -Path $newFile -Value $content -NoNewline
Remove-Item -Path $oldFile -Force

Write-Host "OK   : $oldFile -> $newFile" -ForegroundColor Green

Write-Host "=== 2/2 : mise a jour du AndroidManifest.xml ===" -ForegroundColor Yellow

if (-not (Test-Path $manifestFile)) {
    Write-Warning "Manifest introuvable, ignore : $manifestFile"
} else {
    $manifestContent = (Get-Content -Raw -Path $manifestFile) -replace "`r`n", "`n"
    $old = '.service.WaveNotificationListenerService'
    $new = '.service.MobileMoneyNotificationListenerService'
    if ($manifestContent.Contains($old)) {
        $manifestContent = $manifestContent.Replace($old, $new)
        Set-Content -Path $manifestFile -Value $manifestContent -NoNewline
        Write-Host "OK   : $manifestFile mis a jour" -ForegroundColor Green
    } else {
        Write-Warning "Motif non trouve dans le manifest (deja renomme ?) : $manifestFile"
    }
}

Write-Host ""
Write-Host "=== Termine ===" -ForegroundColor Yellow
Write-Host "N'oublie pas apres compilation/installation : reactiver 'Acces aux notifications'"
Write-Host "pour M3Finances dans les reglages Android (le nom de composant a change)."
Write-Host ""
Write-Host "Pour que git detecte le renommage proprement :"
Write-Host "    git add -A"
Write-Host "    git status"
