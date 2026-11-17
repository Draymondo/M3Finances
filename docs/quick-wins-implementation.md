# Quick wins — ce qui a été implémenté

**Branche :** `arena/42e82d05-m3finances`
**Référence :** `quick-wins-mockup.html` (5 écrans)
**Audit d'origine :** `docs/audit-maquette-quick-wins.md`

Cette branche part de `main` (`bbb4de2`) et a **fusionné** `arena/a9c3e9c8-m3finances`
(commit de merge `5667f3a`) : l'application livrée est donc celle qui tournait déjà sur le
téléphone (paiements échelonnés, logo, correctifs du parser Wave, base Room v22) **plus** les
quick wins de la maquette, en une seule implémentation par écran.

---

## Résumé

| Quick win de la maquette | État |
|---|---|
| Chiffres tabulaires + devise en corps réduit | ✅ `AmountText` (tnum, unité à 42 % de la taille, 55 % d'opacité) |
| Héros en dégradé émeraude (clair **et** sombre) | ✅ dégradé + halo radial, texte blanc |
| Chip de tendance neutre (jamais verte sur un zéro) | ✅ pastille translucide, toujours neutre |
| Donut sans étiquettes qui se chevauchent | ✅ `setDrawEntryLabels(false)`, centre dessiné en Compose |
| Outils : recherche + 3 groupes + grille 2 colonnes | ✅ `items.chunked(2)`, 10 outils |
| Badge `NOUVEAU` sur « Paiements échelonnés » | ✅ **retiré** (demande explicite) |
| Montant dominant sur la création de transaction | ✅ + CTA pleine largeur 52 dp en barre basse |
| Date retirée des lignes de transaction | ✅ paramètre `showDate` (accueil + liste à `false`) |
| Comptes à 0 FCFA en gris muet | ✅ `getAmountTextColor()` → `gray_500` |
| Barres de catégories normalisées (max = 100 %) | ⏳ la barre suit la part réelle du mois |
| Légende de catégories sur 2 lignes | ⏳ reste sur 1 ligne |
| Marchand en titre + chip source (WAVE) | ⏳ hors scope (données) |
| « Part du mois · 19,65 % » dans le champ catégorie | ⏳ hors scope (agrégation) |
| Phrase contextuelle de la carte budget vide | ⏳ hors scope (données) |
| Centre du donut avec devise réduite | ⏳ dessiné par Compose, une seule taille de texte |

---

## 1. Composant `AmountText`

`core/designsystem/.../components/AmountText.kt`

Rend un montant déjà formaté en nombre + unité :

- **chiffres tabulaires** (`fontFeatureSettings = "tnum"`) → plus rien ne « danse » quand le montant change ;
- **devise en corps réduit** (`unitSizeFactor`, 0,42 par défaut) et atténuée (`unitColor`, 55 %) ;
- découpe sur le dernier séparateur (espace, insécable `U+00A0`, fine insécable `U+202F`) et
  **seulement si la fin ne contient pas de chiffre** — un montant décimal n'est jamais coupé ;
- repli propre : si aucune découpe n'est trouvée, la chaîne est rendue telle quelle.

Utilisé par le héros, ses cellules, les lignes et barres de catégories, les lignes de transaction
et les cartes de compte.

> Le formateur (`CurrencyRepositoryImpl`) n'a pas été touché : 8 assertions de
> `CurrencyRepositoryImplTest` figent les chaînes actuelles (`"100.0$"`, `"-€1,200"`…) et
> `amountString` circule dans tout le domaine.

## 2. Héros du tableau de bord

`core/designsystem/.../components/AmountSummaryView.kt`

- Dégradé linéaire émeraude `#144D33 → #0B281B → #07130C` dans un `AppCardView` (rayon 28 dp,
  bordure `#26513F`), plus un **halo radial** `#2FA36B` à 40 % ancré en haut à droite
  (`drawBehind`).
- Libellé `TOTAL · <période>` en 11 sp, interlettrage 1,4, majuscules, teinte `#9FD4BC`.
- Total blanc cassé (`#F2FBF6`) en 32 sp extra-gras, tabulaire, devise réduite.
- **Chip de tendance neutre** : translucide blanc à 10 %, jamais vert sur un zéro
  (la maquette reprochait justement la chip verte à `0,00 %`).
- Deux cellules translucides (Dépense ↑ / Revenu ↓) avec pastille d'icône colorée et montants
  en `AmountText`.
- La variante compacte (`AmountInfoWidgetCompact`, utilisée hors tableau de bord) suit la même
  grammaire avec un dégradé plus large.

## 3. Donut des catégories

- Aucune étiquette sur les parts : `setDrawEntryLabels(false)`.
- Le texte central n'est plus dessiné par MPAndroidChart (`setDrawCenterText(false)`) mais par
  Compose par-dessus le trou — une seule taille de texte, donc pas de devise réduite au centre.
- Rotation figée à 270° (départ à midi), valeurs en pourcentage, marges resserrées quand les
  valeurs sont masquées.

## 4. Écran Outils

`feature/settings/.../tools/{ToolsScreen,ToolsState,ToolsAction,ToolsViewModel}.kt`

- **Recherche** en haut (`OutlinedTextField` avec ses couleurs M3), filtrage insensible à la casse
  sur le titre et le sous-titre.
- **3 groupes sémantiques** avec code couleur propre (suivi/planification, argent/objectifs,
  pratique) via `ToolGroup.entries`.
- **Grille 2 colonnes** : `items.chunked(2)` + une `Row` de deux `ToolTile` à `weight(1f)`.
- **Aucun badge « NOUVEAU »** : `tool.isNew` n'est plus rendu, la couleur `NewBadgeGreen` et le
  bloc de badge ont été supprimés de l'écran. Le drapeau reste dans le modèle mais n'a aucun effet
  visuel — c'est une demande explicite, l'outil « Paiements échelonnés » ne doit pas être mis en avant.
- État vide traduit.

## 5. Création de transaction

`feature/transaction/.../create/TransactionCreateScreen.kt`

- **CTA pleine largeur, 52 dp**, dans la `bottomBar` : icône + libellé `Enregistrer`, ou
  `Planifier` avec icône calendrier si la date est future, couleur `#15803D`.
- Montant en champ principal, sélecteurs de date/heure via `AppDatePickerDialog`.
- Le segmented Revenu / Dépense / Virement et la suppression (dialog) sont conservés.

## 6. Incohérences transverses corrigées

- **Compte à 0 FCFA** : `getAmountTextColor()` renvoie `gray_500` (`#8A939C`) pour un montant nul
  (epsilon 0,005), rouge en négatif, vert en positif. `AccountCreateViewModel.getBalanceBackgroundColor`
  réutilise cette règle : plus de héros vert pour un compte vide.
- **Date des lignes** : paramètre `showDate` sur `TransactionItem` et `CategoryTransactionItem`.
  `false` sur l'accueil et la liste des transactions (l'en-tête de jour ou la section porte la date),
  `true` par défaut ailleurs (calendrier, détails budget/catégorie, récurrentes).
- **Formulaire** : `AppTextField` transmet `textStyle` (correctif material3 1.4).

---

## Fusion et diagnostic du crash au démarrage

L'APK de cette branche se refermait au lancement. Cause : **downgrade de base Room**, pas une
régression d'interface.

| | version `@Database` |
|---|---|
| `arena/a9c3e9c8-m3finances` (APK alors installé) | **22** (table `installment_plans`) |
| cette branche avant fusion (depuis `main`) | **21** |

La base de l'appareil était déjà en 22 ; ouvrir une base plus récente que la version attendue lève
`IllegalStateException: A migration from 22 to 21 was required but not found` au premier accès
(l'application ouvre la base au démarrage, via son initialiseur de préchargement), d'où la
fermeture immédiate. Aucun `fallbackToDestructiveMigration()` n'est déclaré, ce qui est voulu.

La fusion règle le problème par la base : `version = 22`, `MIGRATION_21_22` dans
`DatabaseMigrations.kt`, déclarée dans `KoinDatabaseModule` avec les 18 précédentes. Une base
v21 **migre** donc vers 22 au lieu de planter, et une base déjà en 22 est ouverte telle quelle :
**aucune désinstallation n'est nécessaire**, les données de test sont conservées.

Points de merge qui méritent d'être connus :

- **Conflits résolus en faveur de la branche déjà validée sur appareil** pour les 10 fichiers
  d'interface (héros, `PieChartView`, `TransactionItem`, écrans catégorie/compte, création de
  transaction, `AppTextField`), pour ne pas laisser deux variantes concurrentes.
- Duplicats supprimés : `AmountText` du package `ui.components` (deux classes identiques),
  argument `textStyle` présent deux fois dans le même `OutlinedTextField` (les deux branches
  avaient appliqué le même correctif material3 1.4), chaîne `tools_group_money` définie deux fois
  (`packageDebugResources` échouait sur « Found item … more than one time »), et state/action de
  recherche des Outils devenus inutiles (la recherche est locale à l'écran).

---

## Ce qui reste volontairement de côté

| Point de la maquette | Pourquoi |
|---|---|
| Marchand en titre de ligne + chip source compacte | `TransactionUiItem` n'a pas de marchand : il faudrait extraire/stocker le libellé de la notification mobile money. Évolution de données, pas de style. |
| « Part du mois · 19,65 % » dans le champ catégorie | demande un use case d'agrégation par catégorie sur la période, appelé depuis l'écran de création. |
| Phrase contextuelle de la carte budget vide | le libellé actuel est générique faute de savoir **quelle** catégorie est vide. |
| Devise réduite au centre du donut | le centre est rendu par Compose en un seul `Text` ; mélanger deux tailles demande un `AnnotatedString` (faisable, non fait ici). |
| Barres de catégories normalisées sur la plus grosse | la barre suit aujourd'hui la part réelle du mois (`barPercent / 100`), ce qui reste lisible. |
| Montants hors des 5 écrans de la maquette (budgets « sur X », dettes, épargne) | ils interpolent le montant dans une phrase : il faut d'abord les passer en composition (nombre + devise séparés). |

---

## Vérifications

- **CI GitHub Actions, run [`44060988725`](https://github.com/Draymondo/M3Finances/actions/runs/44060988725)**
  sur le commit `cae3551` : `debug-apk` ✅ et `unit-test` ✅.
  L'APK se télécharge depuis l'artefact `app-debug-apk` de ce run.
- Runs intermédiaires du merge, tous corrigés : `44059829029` (doublon de chaîne
  `tools_group_money`), `44060317791` (argument `textStyle` dupliqué dans `AppTextField`).
- Le sandbox de développement n'a ni JDK ni SDK Android et l'accès réseau y est restreint : la
  compilation passe donc obligatoirement par la CI, et le lancement ne peut être validé que sur
  l'appareil.

## CI

`.github/workflows/build.yml` a été adapté pour rendre cette boucle exploitable :

- ajout de `workflow_dispatch` → n'importe quelle branche peut être compilée à la demande
  (`gh workflow run Build --ref <branche>`), sans devoir merger dans `main` ;
- les deux jobs tournent avec `--no-configuration-cache` (`org.gradle.configuration-cache=true`
  dans `gradle.properties` faisait échouer la phase de configuration, en ~30 s, sans erreur Kotlin) ;
- en cas d'échec, les lignes `e: …`, le bloc « What went wrong » et les `Caused by:` sont republiés
  en **annotations de check-run** — récupérables via l'API même quand les logs bruts d'Actions sont
  inaccessibles depuis un sandbox.
