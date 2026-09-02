$ErrorActionPreference = "Stop"
$path = "README.md"

if (-not (Test-Path $path)) {
    Write-Error "README.md introuvable (verifie que tu es a la racine du projet)"
    exit 1
}

$content = (Get-Content -Raw -Path $path) -replace "`r`n", "`n"

$old1 = "- Parsing de notifications SMS / Wave"
$new1 = "- Parsing de notifications Mobile Money (Wave, Orange Money, MTN MoMo, Moov Money, Djamo) via un `NotificationListenerService`, avec detection automatique de la source et score de confiance sur chaque proposition avant validation"

if ($content.Contains($old1)) {
    $content = $content.Replace($old1, $new1)
    Write-Host "OK   : bullet fonctionnalites IA mis a jour" -ForegroundColor Green
} else {
    Write-Warning "Motif non trouve (bullet fonctionnalites IA) - ignore"
}

$old2 = "- ``parseWaveNotification`` : ``gemini-3.1-flash-lite`` pour un texte de notification simple"
$new2 = "- ``parseWaveNotification`` : ``gemini-3.1-flash-lite`` pour un texte de notification simple (la source Mobile Money est detectee localement via le package Android de la notification, pas par Gemini ; le score de confiance est calcule localement a partir des champs reellement extraits, pas auto-declare par le modele)"

if ($content.Contains($old2)) {
    $content = $content.Replace($old2, $new2)
    Write-Host "OK   : bullet choix de modeles Gemini mis a jour" -ForegroundColor Green
} else {
    Write-Warning "Motif non trouve (bullet choix de modeles Gemini) - ignore"
}

$historyMarker = "## Historique recent"
if ($content.Contains($historyMarker)) {
    Write-Host "OK   : section Historique recent deja presente" -ForegroundColor Green
} else {
    $insert = @'

## Historique recent

- Ajout de `source` (Wave, Orange Money, MTN MoMo, Moov Money, Djamo) et `confidence` sur les transactions en attente issues des notifications Mobile Money, avec migration Room 13->14
- Service d'ecoute des notifications renomme `WaveNotificationListenerService` -> `MobileMoneyNotificationListenerService` pour refleter son perimetre reel (9 apps ecoutees, pas seulement Wave)
'@

    $anchor = "### 2. Fonctionnalites deja presentes"
    if ($content.Contains($anchor)) {
        $content = $content.Replace($anchor, $anchor + $insert)
    } else {
        $content = $content + "`n" + $insert
    }

    Write-Host "OK   : section Historique recent ajoutee" -ForegroundColor Green
}

$content = $content.Replace("WaveNotificationListenerService", "MobileMoneyNotificationListenerService")
$content = $content.Replace("parseWaveNotification", "parseMobileMoneyNotification")

Set-Content -Path $path -Value $content -NoNewline
Write-Host ""
Write-Host "=== Termine : README.md mis a jour ===" -ForegroundColor Yellow
