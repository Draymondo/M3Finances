# Audit — la maquette « Quick wins » vs le code

> **Note (mise à jour)** : cet audit décrit l'état **avant** travaux. Les quick wins listés
> ci-dessous ont depuis été implémentés — voir `docs/quick-wins-implementation.md` pour le
> détail de ce qui a changé et de ce qui reste hors scope.

**Maquette de référence :** `quick-wins-mockup.html` (5 écrans, état « après » proposé)
**Code audité :** branche `arena/42e82d05-m3finances`, base `bbb4de2`
**Date :** 17/11/2026

---

## Verdict en une ligne

**Le code ne correspond pas à la maquette.** La maquette est un *état cible* ; aucun des 6 quick wins
n'est implémenté. La maquette a réutilisé (à juste titre) la structure existante de l'app —
sections de l'accueil, barre d'actions, en-têtes de jour, carte budget vide, tap sur le donut —
donc la « silhouette » se ressemble, mais **le travail visuel proposé reste entièrement à faire**.

Score : **0 / 6 quick wins livrés**, 6 éléments préexistants conformes (non des quick wins).

---

## Tableau de conformité

| # | Quick win de la maquette | État | Preuve dans le code |
|---|---|---|---|
| 1 | Chiffres tabulaires (`tnum`) + devise en corps réduit + espace fine | ❌ Absent | `tnum` n'existe qu'à 2 endroits, hors des montants : `feature/currency/.../CurrencyCustomiseScreen.kt:251,344`. La devise est collée au nombre : `CurrencyRepositoryImpl.getCurrency()` → `"${currencyFormatted}${currency.symbol}"` (aucun espace, même taille de police) |
| 2 | Héros en dégradé émeraude (clair & sombre) | ❌ Absent — et régression | `AmountSummaryView.kt:107` : `.background(Color(0xFF15191D))` — un rectangle plat **codé en dur en sombre**, identique en thème clair |
| 3 | Chip `0,00 %` neutre (gris), pas verte | ❌ Absent | `AmountSummaryView.kt:133-134` : `if (trendText.startsWith("-")) rouge else vert` → `"0.00%"` tombe dans le `else` = **vert sur fond vert** (`#43A546` sur `#163A2B`). Le VM produit bien `"0.00%"` (`DashboardViewModel.kt:170`) |
| 4 | Donut sans étiquettes qui se chevauchent | ❌ Absent | `PieChartView.kt:140-152` : `setDrawValues(true)` + `xValuePosition/yValuePosition = OUTSIDE_SLICE` + lignes de rappel + `setExtraOffsets(24f,12f,24f,12f)` → callouts toujours actifs sur l'écran Catégories |
| 5 | Outils : recherche + 3 groupes + grille 2 colonnes + badge NOUVEAU | ❌ Absent (5/5) | `ToolsScreen.kt:62` : simple `LazyColumn` d'`ElevatedCard` pleine largeur. Aucun champ de recherche, aucun grouping, aucune grille, aucun badge. `ToolsViewModel.kt:101` trie même par usage décroissant (l'ordre bouge tout seul) |
| 6 | Bug du champ date rogné réparé (champs pleine largeur) | ❌ Absent | `TransactionCreateScreen.kt:405-427` : `Row` + 2 × `ClickableTextField(modifier = Modifier.weight(1f))` avec icône de tête + label flottant — exactement la cause du rognage décrite dans la maquette |

Verdict détaillé : **0 quick win sur 6**.

---

## Écran par écran

### 1 · Accueil
| Élément maquette | Code | Verdict |
|---|---|---|
| Héros dégradé émeraude | `AppCardView` + fond plat `#15191D` | ❌ |
| Total `11 944` + `FCFA` réduit | `11 944FCFA` (concaténation brute) | ❌ |
| Chip `0,00 %` neutre | Chip vert | ❌ |
| Comptes à 0 FCFA en gris muet | `getAmountTextColor()` → `if (this < 0) red else green` (`ColorExt.kt:5-9`) → **0 est vert** | ❌ |
| Comptes / Catégories / Budgets / Transactions + « TOUT AFFICHER » | `DashboardScreen.kt` : `DashboardWidgetTitle(onViewAllClick=…)` | ✅ préexistant |
| Barre du haut : cloche (badge), calendrier, chat, réglages | `DashboardScreen.kt:138-186` | ✅ préexistant |
| Légende du donut non tronquée | `CategoryTransactionSmallItem` : `maxLines = 1` + `Ellipsis` → « Alimentatio… » | ❌ |
| Carte budget vide actionnable + phrase contextuelle (« … à la catégorie loyer ») | Carte + bordure primaire + cercle `+` bien présents (`DashboardScreen.kt:~400`), mais texte **générique** : « Appuyez pour définir une limite de dépenses » (`values-fr/strings.xml:19`) | ⚠️ partiel |

### 2 · Transactions
| Élément maquette | Code | Verdict |
|---|---|---|
| Date supprimée des lignes | `TransactionItem.kt:280` : `Text(text = date)` sous le montant | ❌ |
| En-tête de jour + total du jour à droite | `TransactionHeaderItem` + `totalAmount` (`TransactionListScreen.kt:199-210, 319-365`) | ✅ préexistant |
| Marchand en titre, chip source compacte (WAVE), « quoi · où » | Titre = **catégorie**, sous-titre = compte + notes séparées ; pas de chip source | ❌ |
| Recherche dans la barre | `TransactionListAction.OpenSearch` + icône loupe | ✅ préexistant |
| Indicateur de rapprochement (montant barré rouge) | **Aucun `TextDecoration.LineThrough`** dans `feature/transaction` (seulement dette/épargne) — l'élément montré dans la capture n'est pas dans ce code | ❓ à clarifier |

### 3 · Catégories
| Élément maquette | Code | Verdict |
|---|---|---|
| Aucune étiquette autour du donut | Callouts `OUTSIDE_SLICE` toujours actifs quand `hideValues = false` (`CategoryTransactionListScreen.kt:202-216`) | ❌ |
| Centre du donut : total + FCFA réduit, tabulaire | `centerText = "Dépense\n164 895 FCFA"` en un seul style gras | ❌ |
| Barre de la catégorie max calée sur 100 %, autres en proportion | `progress = { percentage / 100f }` (`CategoryTransactionItem.kt:93`) → loyer 30,47 % = barre à 30 % | ❌ |
| Légende = liste cliquable unique (pas de duplication) | La liste existe ; la légende du dashboard est une 2e présentation (écran différent) | ✅ acceptable |
| Hint « Touche le graphique pour voir les revenus » | `tap_chart_to_switch_to` | ✅ préexistant |

### 4 · Outils
| Élément maquette | Code | Verdict |
|---|---|---|
| Recherche en haut | rien | ❌ |
| 3 groupes sémantiques | `LazyColumn` à plat, triée par usage | ❌ |
| Grille 2 colonnes, ~9 outils visibles | cartes pleine largeur (~5 visibles) | ❌ |
| Badge « NOUVEAU » sur Paiements échelonnés | **`ToolType` n'a pas de paiements échelonnés** (`core/model/ToolType.kt` : 9 entrées) | ❌ |
| Code couleur par groupe | `primaryContainer` unique pour toutes les icônes | ❌ |

> Note : la maquette parle de « 23 cartes » ; le code en affiche **9**. Le compteur de la maquette est
> probablement la somme des entrées de Réglages + Outils. À trancher avant de figer la grille.

### 5 · Nouvelle transaction
| Élément maquette | Code | Verdict |
|---|---|---|
| Date **et** heure en champs pleine largeur, valeur sur une ligne | 2 champs `weight(1f)` côte à côte + icônes (`:409`, `:419`) | ❌ |
| Montant dominant, grand, tabulaire | `DecimalTextField` standard (`:386`), taille de champ normale | ❌ |
| Catégorie avec contexte « part du mois · 19,65 % » | `CategoryItem` nu, **aucun pourcentage** dans `feature/transaction` | ❌ |
| CTA pleine largeur 52 dp, libellé « Enregistrer » | `ExtendedFloatingActionButton` flottant (`:219-231`) | ❌ |
| Segmented control conservé, rouge en sélection | `TransactionTypeSelectionView` conservé | ✅ préexistant |
| FAB « + » masqué sur cet écran | Le FAB **est** le CTA d'enregistrement ici ; il n'y a pas de FAB « + » de liste | ✅ (par construction) |

---

## Écarts « silencieux » repérés au passage

1. **Héros en thème clair cassé** : `Color(0xFF15191D)` et `Color(0xFFF4F6F8)` codés en dur dans
   `AmountInfoWidget` → carte sombre sur fond clair. Le dégradé émeraude demandé est l'occasion de
   corriger ça (`Brush` + `MaterialTheme.colorScheme`).
2. **Espace avant la devise** : changer `getCurrency()` impacte *tous* les montants de l'app
   (listes, budgets, dettes, widget). C'est un changement transversal, pas cosmétique.
3. **Le donut du dashboard est déjà sans étiquettes** (`hideValues = true`), seul l'écran Catégories
   souffre des callouts — la maquette mélange les deux écrans sur ce point.
4. **Aucune « AmountTypography » partagée** : chaque écran redéfinit ses `fontSize`/`letterSpacing`
   (ex. `15.sp` en dur dans `TransactionItem.kt`). Le quick win « chiffres tabulaires » suppose
   d'introduire ce composant/style commun d'abord.

---

## Ce qu'il faudrait toucher pour que le code = la maquette

| Chantier | Fichiers principaux | Effort |
|---|---|---|
| Style de montant partagé (tnum, devise réduite, espace fine) | `core/designsystem/.../components/` (nouveau), `CurrencyRepositoryImpl.kt` | M |
| Héros dégradé + chip neutre | `AmountSummaryView.kt` | S |
| Donut sans callouts + centre tabulaire + barres normalisées | `PieChartView.kt`, `CategoryTransactionItem.kt` | S |
| Lignes de transaction sans date + titre marchand + chip source | `TransactionItem.kt`, `TransactionListViewModel.kt` (mapping) | M |
| Comptes à 0 en gris | `ColorExt.kt` | XS |
| Outils : recherche + groupes + grille + badge | `ToolsScreen.kt`, `ToolsState.kt`, `ToolsViewModel.kt`, `ToolType.kt` | L |
| Champ date/heure réparé + montant dominant + CTA pleine largeur | `TransactionCreateScreen.kt`, `TextFieldWidgets.kt` | M |

---

**Conclusion** : rien dans le code n'a été aligné sur la maquette — le HTML était une proposition,
et la proposition n'a pas encore été exécutée. La bonne nouvelle : la hiérarchie des écrans est déjà
la bonne, donc l'essentiel du travail est du style et de la mise en page, pas de la restructuration.
