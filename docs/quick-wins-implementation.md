# Quick wins — ce qui a été implémenté

**Branche :** `arena/42e82d05-m3finances`
**Référence :** `quick-wins-mockup.html` (5 écrans)
**Audit d'origine :** `docs/audit-maquette-quick-wins.md`

---

## Résumé

| Quick win de la maquette | État |
|---|---|
| Chiffres tabulaires + devise en corps réduit + espace fine | ✅ fait (`AmountText`) |
| Héros en dégradé émeraude (clair **et** sombre) | ✅ fait |
| Chip `0,00 %` neutre (plus jamais verte sur un zéro) | ✅ fait |
| Donut sans étiquettes qui se chevauchent | ✅ fait |
| Outils : recherche + 3 groupes + grille 2 colonnes | ✅ fait |
| Bouton `NOUVEAU` | ✅ aucun badge (voir plus bas) |
| Champ date rogné | ✅ réparé (`ClickableValueField`) |
| Montant dominant + tabulaire sur la création | ✅ fait |
| CTA pleine largeur 52 dp + retour haptique | ✅ fait (+ FAB supprimé) |
| Date retirée des lignes de transaction | ✅ fait (accueil + liste) |
| Comptes à 0 FCFA en gris muet | ✅ fait |
| Barres de catégories normalisées (max = 100 %) | ✅ fait |
| Légende du donut plus tronquée | ✅ fait (2 lignes autorisées) |
| Marchand en titre + chip source (WAVE) | ⏳ hors scope (données) |
| « Part du mois · 19,65 % » dans le champ catégorie | ⏳ hors scope (agrégation) |
| Phrase contextuelle de la carte budget vide | ⏳ hors scope (données) |
| Centre du donut avec devise réduite | ⏳ limitation MPAndroidChart |

---

## 1. Nouveau composant : `AmountText`

`core/designsystem/.../ui/components/AmountText.kt`

Rend un montant déjà formaté en trois morceaux (devise préfixe, nombre, devise suffixe) :

- **chiffres tabulaires** (`fontFeatureSettings = "tnum"`) → plus rien ne « danse » quand le montant change ;
- **devise à 62 %** de la taille du nombre, à 72 % d'opacité ;
- **espace fine insécable** (U+202F) entre le nombre et la devise ;
- le **signe** reste à la taille du nombre, même en devise préfixe (`-€1 200`, pas `-€` en petit) ;
- repli propre si aucun nombre n'est détecté (texte brut).

Appliqué à : héros + cellules dépense/revenu, lignes de transaction, totaux de jour, cartes de compte,
lignes et légende de catégories.

> Pourquoi ne pas avoir modifié le formateur (`CurrencyRepositoryImpl`) ? Parce que 8 assertions de
> `CurrencyRepositoryImplTest` figent les chaînes actuelles (`"100.0$"`, `"-€1,200"`…) et que
> `amountString` circule dans tout le domaine. L'espace fine est donc ajoutée **au rendu**, pas à la donnée.

## 2. Héros du tableau de bord

`core/designsystem/.../components/AmountSummaryView.kt`

- Dégradé linéaire émeraude `#0E3F2B → #0A2C20 → #071F16` **plus** un halo radial `#3DDC84`
  à 28 % en haut à droite (`drawBehind`, deux couches).
- Le fond n'est plus codé en dur en sombre : le héros est identique en clair et en sombre, texte blanc.
- Libellé « **Total · \<période\>** » (la période n'était affichée nulle part dans cette variante).
- Total à 34 sp, tabulaire, devise réduite.
- **Chip de tendance honnête** : gris translucide si la variation est nulle ou illisible,
  rouge si négative, vert si positive. `0.00%` n'est plus vert.
- Deux cellules translucides (Dépense ↑ / Revenu ↓) avec pastille d'icône colorée, valeurs blanches.
- La variante compacte (`AmountInfoWidgetCompact`) passe aussi par `AmountText`.

## 3. Donut des catégories

- `PieChartView` ne dessine **plus aucune étiquette** : ni sur les parts, ni autour (les callouts
  `OUTSIDE_SLICE` + lignes de rappel sont supprimés, `setDrawValues(false)` systématique).
- Le centre ne contient plus que le total (l'en-tête « Dépenses · N catégories » donne déjà le type).
- Les barres de la liste sont **normalisées sur la plus grosse catégorie** (`barFraction`), le
  pourcentage affiché reste la part réelle du mois.
- Légende autorisée sur 2 lignes → fini « Alimentatio… ».

## 4. Écran Outils

`feature/settings/.../tools/{ToolsScreen,ToolsState,ToolsAction,ToolsViewModel}.kt`

- **Recherche** en haut (titre + sous-titre localisés, bouton effacer).
- **Grille 2 colonnes** dense (`LazyVerticalGrid`, tuiles de 96 dp mini, icône + titre + sous-texte).
- **3 groupes sémantiques** avec code couleur :
  - Suivi & planification (transactions prévues, calendrier, récurrentes, courses) — bleu
  - Argent & objectifs (épargne, dettes, enveloppes) — vert
  - Pratique (devises, temps de travail) — ocre
- **Aucun badge « NOUVEAU »** n'est ajouté : la maquette en prévoyait un sur « Paiements échelonnés »,
  or **cet outil n'existe pas dans le code** (`ToolType` compte 9 entrées). Rien à badger, rien à
  retirer — et aucun autre outil ne reçoit de badge.
- État vide traduit : « Aucun outil ne correspond à « … » ».
- Ordre stable à l'intérieur d'un groupe (tri par usage conservé).

## 5. Nouvelle transaction

`feature/transaction/.../create/TransactionCreateScreen.kt`

- **Bug du champ date réparé** : nouveau `ClickableValueField` (label au-dessus, valeur en dessous,
  icône à droite, `tnum`). Date et heure ne partagent plus la largeur avec un label flottant + une
  icône de tête → plus de `16/11/2…`.
- **Montant dominant** : `headlineMedium` gras + tabulaire (le champ principal de l'écran).
- **CTA pleine largeur 52 dp** en fin de formulaire, icône + libellé `Enregistrer`
  (ou `Planifier` si la date est future), **retour haptique** au clic.
- L'`ExtendedFloatingActionButton` qui chevauchait le contenu est supprimé.
- Le segmented Revenu / Dépense / Virement et la suppression dans la barre du haut sont conservés.

## 6. Incohérences transverses corrigées

- **Compte à 0 FCFA** : `getAmountTextColor()` renvoie `gray_500` (`#8A939C`) pour un montant nul,
  rouge en négatif, vert en positif. La même règle est réutilisée par l'écran de création de compte
  (`getBalanceBackgroundColor`), qui ne peut plus afficher un héros vert à 0.
- **Date des lignes** : nouveau paramètre `showDate` sur `TransactionItem`.
  `false` sur l'accueil et la liste des transactions (l'en-tête de jour, ou la section, porte la date),
  `true` par défaut ailleurs (calendrier, détails budget/catégorie, récurrentes) où la date est la
  seule information disponible.
- **Totaux de jour** en chiffres tabulaires.

---

## Ce qui reste volontairement de côté

| Point de la maquette | Pourquoi |
|---|---|
| Marchand en titre de ligne + chip source compacte | `TransactionUiItem` n'a pas de marchand : il faudrait extraire/stocker le libellé de la notification mobile money. C'est une évolution de données, pas du style. |
| « Part du mois · 19,65 % » dans le champ catégorie | demande un use case d'agrégation par catégorie sur la période, appelé depuis l'écran de création. |
| Phrase contextuelle de la carte budget vide (« … à la catégorie loyer ») | le libellé actuel est générique faute de savoir **quelle** catégorie est vide ; il faut d'abord décider d'où vient cette catégorie. |
| Devise réduite au centre du donut | le texte central est dessiné par MPAndroidChart (`StaticLayout`), pas par Compose : mélanger deux tailles demanderait un `SpannableString` non garanti selon les versions de la lib. |
| Montants hors des 5 écrans de la maquette (budgets « sur X », dettes, épargne) | ils interpolent le montant dans une phrase (« sur 50 000 FCFA ») : il faut d'abord les passer en composition (nombre + devise séparés). |

---

## Vérifications

- Aucun test unitaire ne figeait la couleur d'un montant à zéro (seul
  `AccountCreateViewModelTest` vérifie `totalAmountBackgroundColor`, sur des montants non nuls → vert/rouge conservés).
- Les signatures publiques utilisées ailleurs sont préservées : `AmountInfoWidget`/`…Compact`,
  `PieChartView`, `TransactionItem` (nouveau paramètre optionnel), `CategoryTransactionItem`
  (nouveau paramètre optionnel), `DecimalTextField`/`AutoSelectTextField` (paramètre `textStyle` optionnel).
- **Compilé et testé par la CI GitHub Actions**, run
  [`44054265265`](https://github.com/Draymondo/M3Finances/actions/runs/44054265265) :
  `assembleDebug` ✅ et `testDebugUnitTest` ✅ (les deux jobs verts).
  L'APK est téléchargeable depuis l'artefact `app-debug-apk` de ce run.
- Le sandbox de développement n'a ni JDK ni SDK Android et l'accès réseau y est restreint : la
  compilation passe donc obligatoirement par la CI.

## CI

`.github/workflows/build.yml` a été adapté pour rendre cette boucle exploitable :

- ajout de `workflow_dispatch` → n'importe quelle branche peut être compilée à la demande
  (`gh workflow run Build --ref <branche>`), sans devoir merger dans `main` ;
- le job `unit-test` reçoit `--no-configuration-cache --stacktrace`, comme le job de build :
  `org.gradle.configuration-cache=true` dans `gradle.properties` faisait échouer la tâche de test
  dès la phase de configuration (échec en ~30 s, sans erreur Kotlin) ;
- en cas d'échec, les lignes `e: …`, le bloc « What went wrong » et les `Caused by:` sont republiés
  en **annotations de check-run** — récupérables via l'API même quand les logs bruts d'Actions sont
  inaccessibles.

