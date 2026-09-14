# Convertisseur de devises — Phase 3 (UI)

Le convertisseur devient visible et utilisable dans l'app.

## Nouveau module `feature/currencyconverter` (13 fichiers)
- `converter/CurrencyConverterScreen.kt` — montant, sélection devise
  source/cible (réutilise `CountryCurrencySelectionBottomSheet` déjà
  existant), bouton d'inversion, résultat, date de dernière mise à jour +
  bouton actualiser, état vide si pas de clé API enregistrée
- `settings/CurrencyConverterSettingsScreen.kt` — champ clé API + lien vers
  exchangerate-api.com
- ViewModels, DI (`CurrencyConverterViewModelModule`), strings FR + EN

**Rappel du comportement** : aucune devise par défaut, tu choisis à chaque
fois ; dès que tu choisis la devise source, les taux se rafraîchissent
automatiquement (un seul appel réseau, il couvre toutes les devises
cibles) ; si le réseau échoue, le dernier taux connu en cache s'affiche
avec un message discret.

## Fichiers modifiés (9)
- `ToolType.kt` (+`CURRENCY_CONVERTER`)
- `ExpenseManagerScreens.kt` (+2 routes)
- `ToolsViewModel.kt` (feature/settings) : nouvelle entrée "Convertisseur
  de devises" dans Paramètres > Outils
- `feature/settings/.../strings.xml` (EN + FR)
- `HomeScreen.kt`, `ViewModelModule.kt` (app) : câblage navigation + DI
- `settings.gradle.kts`, `app/build.gradle.kts`

## Application depuis Termux

```bash
mkdir -p ~/currency_phase3_patch
unzip -o ~/storage/downloads/currency_phase3_patch.zip -d ~/currency_phase3_patch
cd ~/repo
cp -r ~/currency_phase3_patch/currency_phase3_patch/* .
git status   # doit lister 22 fichiers (13 nouveaux + 9 modifies)
git add -A
git commit -m "Convertisseur devises: UI (ecran principal, reglages, entree Outils)"
git push
```

⚠️ Comme pour la Phase 3 d'Enveloppe, ce patch touche la racine du repo
(`settings.gradle.kts`, `app/build.gradle.kts`) — copie bien tout le
contenu du dossier patch à la racine (`cp -r .../* .`), pas seulement
`core`.

Dis-moi si la CI passe — ce sera la première fois que tu pourras tester le
convertisseur dans l'app (il te faudra ta clé API exchangerate-api.com).
