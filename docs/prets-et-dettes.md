# Outils — « Prêts & dettes »

## Pourquoi

L'outil Dettes gérait déjà les deux sens (`DebtDirection.LENT` / `BORROWED`), les remboursements
partiels (transactions de virement vers un compte caché `AccountType.DEBT`) et les rappels. Il
manquait la **lecture** : une liste plate mélangeait ce qu'on nous doit et ce que nous devons,
sans total, donc impossible de répondre à « combien on me doit aujourd'hui ? ».

Pas de nouvelle table, pas de migration : la base reste en **v22**. Tout se joue dans l'agrégation
et l'affichage.

## Ce qui a changé

### 1. Renommage de l'outil

| | Avant | Après |
|---|---|---|
| Titre (tuile Outils + écran) | Dettes | **Prêts & dettes** |
| Sous-titre | Argent prêté ou emprunté | **Ce qu'on me doit, ce que je dois** |

Les textes de notification (« Rappel de dette ») sont volontairement inchangés : ils distinguent
déjà les deux sens dans leur contenu (« X vous doit… » / « Vous devez… à X »).

Comme le titre ne contient plus le mot « dette », l'écran Outils accepte désormais des **alias de
recherche** (`searchAliasRes` sur `ToolItemUiModel`, `string-array tool_alias_debts`) : taper
« dette », « prêt », « emprunt », « crédit » ou « carnet » trouve toujours l'outil.

### 2. Deux sections, avec totaux

`DebtListState` expose maintenant `debtors` / `creditors` / `settled` et deux totaux formatés.

- **On me doit** — dettes `LENT`, en-tête avec le nombre de dettes et le total.
- **Je dois** — dettes `BORROWED`, même en-tête.
- Une personne qui existe des deux côtés apparaît **dans les deux sections** (deux lignes, comme
  demandé) : chaque dette garde son échéance et ses rappels, et une dette en retard n'est jamais
  masquée par une avance plus grosse.

### 3. Une carte par personne

Les dettes sont regroupées par **nom normalisé** (espaces retirés, minuscules) : « Ali » et « ali »
forment une seule carte. La carte affiche le nom, le nombre de dettes, l'état (en retard, prochaine
échéance, ou pas d'échéance) et le **total additionné**.

- **Une seule dette** → la carte ouvre directement le détail (un seul geste, pas de dépliage inutile).
- **Plusieurs dettes** → la carte se déplie (`ExpandMore`/`ExpandLess`) et chaque dette apparaît
  avec ses notes, son échéance et son montant, cliquable vers l'écran de modification.
- Les cartes sont triées : en retard d'abord, puis échéance la plus proche, puis nom.

### 4. Dettes réglées

Une dette est considérée close si `isSettled` est vrai **ou** si le reste dû est tombé à zéro.
Les dettes closes sortent des deux sections et des totaux, et se retrouvent dans un bloc
**« Réglées »** replié par défaut, en bas de l'écran (nom barré, montant grisé).

### 5. Auto-complétion des noms

Dans l'écran de création, dès qu'on tape au moins une lettre, des `AssistChip` proposent les noms
déjà utilisés dans d'autres dettes (préfixe, insensible à la casse, 4 maximum). Le but est d'éviter
les doublons à la source. Les noms viennent de `GetDebtsUseCase`, aucune nouvelle requête.

## Fichiers touchés

| Fichier | Rôle |
|---|---|
| `feature/debt/list/DebtListState.kt` | `PersonDebtGroup`, sections, totaux |
| `feature/debt/list/DebtListViewModel.kt` | regroupement par personne, tri, totaux formatés |
| `feature/debt/list/DebtListScreen.kt` | en-têtes de section, cartes dépliables, bloc Réglées |
| `feature/debt/create/DebtCreateState.kt` | `knownPersonNames` |
| `feature/debt/create/DebtCreateViewModel.kt` | collecte des noms connus |
| `feature/debt/create/DebtCreateScreen.kt` | chips d'auto-complétion |
| `feature/debt/di/DebtViewModelModule.kt` | injections ajoutées |
| `feature/debt/res/values{,-fr}/strings.xml` | titre, sections, `plurals/debt_count` |
| `feature/settings/.../tools/*` | renommage de la tuile + alias de recherche |
| `feature/settings/res/values{,-fr}/strings.xml` | tuile « Prêts & dettes », `tool_alias_debts` |

## Vérifications

- Compilation et tests unitaires passés par la CI GitHub Actions (voir le run associé au commit).
- Aucune migration de base : `version = 22` inchangée, donc installable par-dessus la version
  actuellement sur l'appareil sans perte de données.

## Hors scope (assumé)

- Les notes restent du texte libre par dette ; pas de fusion automatique des dettes d'une même
  personne (elles gardent chacune leur échéance et leurs rappels).
- Pas de rappel rapide depuis la liste : il se règle dans le détail de la dette.
- Les montants sont additionnés dans la devise de l'application (pas de conversion multi-devises).
