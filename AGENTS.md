# AGENTS.md — M3Finances

## Contexte du projet
M3Finances est une app Android de gestion financière personnelle, fork du projet open-source **expensemanager** (naveenapps). Package racine : `com.naveenapps.expensemanager`.

**Stack** : Kotlin, Jetpack Compose (UI), Room (base locale), Koin (DI), architecture multi-modules Gradle (`feature/*` pour les écrans, `core/*` pour le code partagé).

**Conventions déjà en place dans le repo :**
- La logique métier vit dans des Use Cases dédiés (ex. `AddDebtRepaymentUseCase`), pas dans les ViewModels/Composables.
- Les enums persistés le sont **par ordinal** (voir `DebtDirection`, `AccountType`) : toute nouvelle valeur doit être ajoutée à la fin de l'enum, jamais insérée au milieu ni réordonnée, sous peine de corrompre les données déjà enregistrées. S'applique directement aux nouveaux enums demandés ci-dessous (`savingsStrategy`, `goalType`).
- Les écrans de création/édition utilisent des interrupteurs pour révéler des champs conditionnels — voir l'écran de création de dette (switch « Argent réellement avancé »). Réutiliser ce pattern.
- `SavingsGoal` et `Debt` ne stockent pas de "montant actuel" : chacun s'appuie sur un compte caché (`AccountType.SAVINGS_GOAL` / `AccountType.DEBT`) dont le solde, alimenté par des `TransactionType.TRANSFER`, EST la progression. Réutiliser ce mécanisme plutôt qu'un champ redondant.
- Persistance Room + synchronisation cloud automatique vers Firestore (une collection par table Room, via `InvalidationTracker`, debounce 10s).
- IA : tout passe par `GeminiRepository` / `GeminiRepositoryImpl` (`core/repository`, `core/data/repository`), modèle `gemini-3.1-flash-lite`, clé API BYOK dans les settings. Le chat (`feature/transaction/.../chat/ChatViewModel.kt`) route le langage naturel vers une grammaire de commandes (`TRANSACTION|`, `SAVINGS_GOAL|nom|montant`, `BUDGET|montant`, etc.) parsée par split de chaîne — il n'a **pas** accès en lecture aux données réelles. Les "conseils" IA (`generateMonthlyReport`) sont appelés depuis `WeeklySummaryWorker` et `AnalysisScreenViewModel`, qui construisent le texte de contexte à la main (actuellement juste totaux entrées/dépenses ± 20 dernières transactions).

---

## Tâche 1 — Stratégie d'épargne adaptative pour revenus variables

**Fichiers concernés (déjà vérifiés dans le repo) :**
- Modèle : `core/model/src/main/kotlin/.../SavingsGoal.kt` (champs actuels : `id, accountId, name, targetAmount, targetDate, notes, isAchieved, createdOn, updatedOn`)
- Use cases : `core/domain/.../usecase/savingsgoal/{AddSavingsGoalUseCase, AddSavingsGoalContributionUseCase, UpdateSavingsGoalUseCase, GetSavingsGoalsUseCase, FindSavingsGoalByIdUseCase}.kt`
- Repository : `core/repository/.../SavingsGoalRepository.kt`, impl dans `core/data/.../SavingsGoalRepositoryImpl.kt`
- UI : `feature/savings/.../savingsgoal/create/{SavingsGoalCreateScreen, SavingsGoalCreateState, SavingsGoalCreateViewModel, SavingsGoalCreateAction}.kt`, `feature/savings/.../savingsgoal/list/{SavingsGoalListScreen, SavingsGoalListState, SavingsGoalListViewModel}.kt`
- Chat : ajouter le paramètre optionnel à la commande `SAVINGS_GOAL|` dans `ChatViewModel.kt`

### 1. Modèle de données
Étendre `SavingsGoal` avec :
- `savingsStrategy: Enum` — `FIXED` (comportement actuel, ne pas y toucher) ou `PERCENTAGE_INCOME` (nouveau)
- `targetPercentage: Double?` — % du revenu épargné à chaque encaissement, utilisé seulement si `PERCENTAGE_INCOME`
- `estimatedCompletionDate: Date?` — recalculée dynamiquement (§3), distincte de `targetDate`

### 2. Suggestion à chaque encaissement (nouveau Use Case, ex. `SuggestContributionUseCase`)
- À chaque nouvelle transaction de type revenu de montant `M` (saisie manuelle **ou** issue de l'IA Notifications), si un objectif actif est en `PERCENTAGE_INCOME` : calculer `M × targetPercentage`, proposer le versement (bandeau/dialog, bouton « Épargner ce montant »).
- Déclenchement par transaction individuelle, jamais par semaine — un utilisateur payé 2 jours/semaine ou 6 jours/semaine suit exactement la même règle, la fréquence des paies compense déjà mécaniquement les semaines creuses. Pas de règle de sur-compensation à ajouter.
- Détection auto (optionnelle) : écart-type des revenus des 30-60 derniers jours pour suggérer `PERCENTAGE_INCOME` par défaut à la création — sans forcer le choix.

### 3. Recalcul dynamique de la date estimée (nouveau Use Case, ex. `RecalculateGoalEstimateUseCase`)
- `restant = targetAmount - currentAmount` (currentAmount = solde du compte caché lié)
- `rythmeHebdo` = moyenne des versements réels sur les 4 dernières semaines
- Si `rythmeHebdo > 0` : `semainesRestantes = restant / rythmeHebdo` → `estimatedCompletionDate`
- Si `rythmeHebdo == 0` : afficher « date indéterminée », jamais de division par zéro ni de date aberrante

### 4. UI
- Création/édition : sélecteur Fixe / Revenu variable (pattern interrupteur du §conventions). Si `PERCENTAGE_INCOME` : champ pourcentage + estimation dynamique affichée.
- Liste/détail : `estimatedCompletionDate` remplace ou complète `targetDate` quand la stratégie est `PERCENTAGE_INCOME`.

### 5. Tests
`RecalculateGoalEstimateUseCase` : semaine à 0 revenu, semaines partielles, semaines pleines, changement de stratégie sur un objectif existant.

---

## Tâche 2 — Objectif de revenu à atteindre (extension de Budget, pas un nouveau module)

**Constat vérifié dans le repo :** `Budget` (`core/model/.../Budget.kt`) n'a rien de spécifique aux dépenses — montant, période (`BudgetPeriod` : DAILY/WEEKLY/MONTHLY/YEARLY), comptes/catégories concernés, report de solde. Le filtre "dépenses uniquement" n'existe qu'à exactement 2 endroits : `it.type.isExpense()` dans `GetBudgetDetailUseCase.kt` et dans `GetBudgetEquivalentsUseCase.kt` (les deux dans `core/domain/.../usecase/budget/`).

### Changements
1. Ajouter `goalType: BudgetGoalType = EXPENSE` (`EXPENSE` ou `INCOME`) sur `Budget` — enum ordinal, ajouté en respectant la convention ci-dessus.
2. Dans `GetBudgetDetailUseCase` et `GetBudgetEquivalentsUseCase`, remplacer le filtre fixe par une condition sur `budget.goalType` (`isExpense()` si `EXPENSE`, `isIncome()` si `INCOME`).
3. `feature/budget/.../create/BudgetCreateScreen.kt` : ajouter le sélecteur Dépense/Revenu.
4. Adapter les libellés à l'affichage quand `goalType == INCOME` (« reste à gagner » au lieu de « reste à dépenser »), y compris dans `BudgetAlertWorker`/`BudgetNotificationHelper` (`core/notification/.../budget/`).
- Bénéfice : périodes, report de solde, alertes et écran liste/détail sont réutilisés tels quels, aucune nouvelle infrastructure.

---

## Tâche 3 — Revenu nécessaire (estimation inverse depuis les objectifs actifs)

Nouveau Use Case en lecture seule (rien de persisté, même esprit que `GetBudgetEquivalentsUseCase`), qui pour une période donnée additionne :
- Les budgets actifs en `goalType == EXPENSE`, convertis à la période choisie via la même logique `daysInPeriod` que `GetBudgetEquivalentsUseCase`.
- Les objectifs d'épargne actifs, convertis à la même période — pour un objectif `PERCENTAGE_INCOME`, utiliser la moyenne réelle versée ces dernières semaines plutôt que de résoudre l'équation circulaire (revenu nécessaire dépendant du revenu).
- **Hors scope pour l'instant** : `Debt` n'a pas d'échéancier de remboursement périodique dans le modèle actuel (juste une `dueDate` globale) — ne pas inventer un montant "par mois". À ajouter seulement si un remboursement récurrent est un jour introduit sur `Debt`.

Affichage : ligne « Revenu nécessaire ce mois : ~X » sur l'écran budget/dashboard ; peut aussi préremplir le montant suggéré à la création d'un objectif de revenu (Tâche 2).

---

## Ne pas faire
- Pas de nouvelle dépendance externe pour les calculs statistiques (écart-type maison suffit).
- Ne pas modifier le comportement de la stratégie `FIXED` de `SavingsGoal` ni du `goalType == EXPENSE` par défaut de `Budget` — ce sont des extensions, pas des remplacements.
- Ne pas committer/pousser sans validation explicite — proposer les changements, laisser l'utilisateur relire.
