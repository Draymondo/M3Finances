# Enveloppe — Phase 3 (UI)

Ce patch ajoute l'interface : écran liste, écran création/édition, et
l'entrée "Enveloppes" dans Paramètres > Outils. C'est la première phase où
la fonctionnalité devient réellement visible et utilisable dans l'app.

## Nouveau module `feature/envelope` (13 fichiers)
- `list/EnvelopeListScreen.kt` — liste des enveloppes : icône + nom de
  catégorie, montant alloué, reste, barre de progression (rouge si dépassée),
  FAB pour créer
- `create/EnvelopeCreateScreen.kt` — sélection de catégorie (réutilise
  l'écran `CategorySelectionScreen` déjà utilisé ailleurs dans l'app),
  montant, nom optionnel, bouton supprimer en mode édition, message d'erreur
  visible si la règle d'exclusivité (Phase 2) bloque la sauvegarde
- ViewModels correspondants + `EnvelopeViewModelModule` (DI)
- `strings.xml` (EN) + `strings.xml` (FR)

**Choix de scope** : la période est fixée au mois en cours (pas de
sélecteur mois/année/semaine/jour comme sur les Budgets classiques) — à
ajouter plus tard si besoin.

## Fichiers modifiés (9)
- `ToolType.kt` : ajout de `ENVELOPES`
- `ExpenseManagerScreens.kt` : ajout des routes `EnvelopeList` / `EnvelopeCreate`
- `ToolsViewModel.kt` (feature/settings) : nouvelle entrée "Enveloppes"
  dans Paramètres > Outils, avec icône et navigation
- `HomeScreen.kt` (app) : câblage des deux nouveaux écrans dans le NavHost
- `ViewModelModule.kt` (app) : ajout du module DI
- `settings.gradle.kts` : `include(":feature:envelope")`
- `app/build.gradle.kts` : dépendance vers `:feature:envelope`
- `feature/settings/.../strings.xml` (EN + FR) : libellé de l'entrée Outils

## Application depuis Termux

```bash
mkdir -p ~/envelope_phase3_patch
unzip -o ~/storage/downloads/envelope_phase3_patch.zip -d ~/envelope_phase3_patch
cd ~/repo
cp -r ~/envelope_phase3_patch/envelope_phase3_patch/* .
git status
```

⚠️ Cette fois le patch touche aussi `settings.gradle.kts` et
`app/build.gradle.kts` à la racine, donc la commande `cp -r` ci-dessus copie
le contenu du patch directement à la racine du repo (pas seulement le
dossier `core`). Vérifie bien avec `git status` que tu retrouves les 22
fichiers attendus (13 nouveaux dans `feature/envelope/` + 9 modifiés) avant
de commiter :

```bash
git add -A
git commit -m "Enveloppe: UI (liste, creation/edition, entree Outils)"
git push
```

C'est un module Gradle entièrement nouveau (`feature/envelope`), donc le
premier `assembleDebug` après ce patch prendra un peu plus de temps que
d'habitude (Gradle doit le configurer). Dis-moi si la CI passe — c'est la
première fois que la fonctionnalité sera testable dans l'app elle-même.
