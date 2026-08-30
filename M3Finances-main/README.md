# M3Finances

M3Finances est une version personnalisée de **Expense Manager**, une application open-source de gestion financière pour Android. Ce document liste tout ce qui a été changé, ajouté ou retiré par rapport à la version d'origine.

---

## 🧾 Pour tout le monde : qu'est-ce qui a changé ?

### 🆕 Nouvelles fonctionnalités

- **Comptes Mobile Money** — En plus des comptes bancaires classiques, tu peux maintenant créer des comptes de type Wave, MTN Money ou Orange Money, très utilisés en Côte d'Ivoire.

- **Gestion des dettes** — Un nouvel écran permet d'enregistrer une dette (que tu doives de l'argent ou qu'on t'en doive), de suivre son remboursement petit à petit, et d'enregistrer des rappels à des dates que tu choisis toi-même (avec relance automatique tant qu'une dette en retard n'est pas réglée).

- **Transactions récurrentes / abonnements** — Tu peux enregistrer une dépense ou un revenu qui se répète automatiquement (loyer, abonnement, salaire...) sans avoir à la ressaisir chaque mois.

- **Recherche de transactions** — Une barre de recherche en texte libre permet de retrouver rapidement une transaction précise.

- **Objectifs d'épargne** — Tu peux te fixer un objectif d'épargne (montant à atteindre, date cible optionnelle), enregistrer des contributions ou des retraits, et suivre ta progression.

- **Transactions scindées (split)** — Une seule dépense peut maintenant être répartie sur plusieurs catégories (par exemple : un ticket de supermarché scindé entre "Alimentation" et "Hygiène").

- **Graphique d'évolution du patrimoine net** — Un nouveau graphique dans les analyses montre l'évolution de ton solde total (patrimoine) dans le temps.

- **Sauvegarde automatique dans le cloud** — Toutes tes données (comptes, catégories, transactions, budgets, dettes, objectifs d'épargne, transactions récurrentes...) sont sauvegardées automatiquement et en continu via un compte Google, pour ne rien perdre en cas de changement de téléphone.

### 🎨 Changements visuels

- Nouveau nom : **M3Finances**
- Nouvelle icône : monogramme "M3" blanc sur fond bleu nuit
- Toute l'application est désormais **traduite en français à 100 %** (auparavant certains textes restaient en anglais)

### 🗑️ Fonctionnalités retirées

- **Aucun suivi ni statistiques d'usage** envoyés à des serveurs externes (l'app n'"espionne" plus l'utilisation — Firebase Analytics et Crashlytics ont été retirés)
- Options **Sauvegarde/Restauration manuelle** et **"Noter l'application"** retirées du menu Réglages (elles n'avaient plus d'utilité pour un usage personnel)
- Écran **"À propos"** retiré

### 🐛 Corrections de bugs

Plusieurs bugs présents dans la version d'origine ont été corrigés : calculs de soldes incorrects dans certains cas, écrans qui pouvaient planter, formulaires qui acceptaient des données invalides, et un bug qui empêchait d'enregistrer un remboursement partiel de dette.

### 🔒 Sécurité

Une revue de sécurité a été faite sur l'ensemble du code (recherche de secrets exposés, configuration réseau/permissions Android, règles d'accès aux données cloud). Rien de critique trouvé ; les règles de sécurité qui protègent les données cloud (accessibles uniquement par leur propriétaire, personne d'autre) sont désormais versionnées dans le dépôt (`firestore.rules`) au lieu de vivre uniquement dans la console Firebase.

---

## 🛠️ Pour les développeurs

### Contexte

M3Finances est un fork de [expensemanager (naveenapps)](https://github.com/naveenapps/expensemanager), une app Android multi-module basée sur **Jetpack Compose**, **Room** et **Koin**, en architecture Clean/MVI (`core/*` pour la logique partagée, `feature/*` pour chaque écran métier).

### Nouveaux modules Gradle

| Module | Contenu |
|---|---|
| `feature/debt` | Écrans de gestion des dettes |
| `feature/recurring` | Écrans des transactions récurrentes |
| `feature/savings` | Écrans des objectifs d'épargne |

Module retiré : `feature/about`.

### Base de données (Room)

- Migration jusqu'à `version = 11` (voir `core/database/schemas/`)
- Nouvelles entités : `DebtEntity`, `DebtReminderEntity`, `RecurringTransactionEntity`, `SavingsGoalEntity`, `TransactionSplitItemEntity`
- Nouveaux DAO correspondants : `DebtDao`, `DebtReminderDao`, `RecurringTransactionDao`, `SavingsGoalDao`
- La fonctionnalité Dettes et la fonctionnalité Objectifs d'épargne réutilisent toutes les deux le mécanisme de virement existant (`AccountType.DEBT` / `AccountType.SAVINGS_GOAL` = comptes cachés), pas de table dédiée à la logique de solde — le montant est toujours dérivé des transactions plutôt que stocké en double

### Couche domaine / data / repository

Nouveaux `UseCase`, `Repository` (interface `core/repository`) et `RepositoryImpl` (`core/data`) pour chacune des nouvelles fonctionnalités : `debt/`, `recurringtransaction/`, `savingsgoal/`, `networth/`, ainsi que `SearchTransactionsUseCase` pour la recherche et la logique de validation pour le split de transactions.

### Sauvegarde cloud (Firebase Firestore)

- Une collection Firestore par table Room (`GoogleAuthRepositoryImpl`, `CloudBackupRepository` / `Impl` dans `core/data/.../cloudbackup`) — couvre désormais comptes, catégories, transactions, transactions scindées, budgets, dettes, rappels de dette, objectifs d'épargne et transactions récurrentes
- Authentification via **Credential Manager + Firebase Auth** (Google Sign-In)
- Déclenchement automatique de la synchronisation via `InvalidationTracker` de Room (`DatabaseChangeCloudBackupTrigger`) — pas de bouton "sauvegarder" manuel
- Règles de sécurité Firestore scopées par utilisateur (`/users/{userId}/...`), versionnées dans `firestore.rules` à la racine du dépôt — ce fichier documente aussi le partage du projet Firebase avec une autre app personnelle (`com.m3notes.app`)

### Build & CI/CD

- **Firebase Analytics / Crashlytics / AppDistribution** entièrement retirés de `app/build.gradle.kts` (plus aucun tracking passif)
- **Restriction ABI** : `abiFilters += "arm64-v8a"` — build limité aux appareils arm64 modernes, réduit la taille de l'APK (usage personnel uniquement, à retirer si compatibilité 32-bit/x86 nécessaire)
- **Keystore debug committé** (`keys/debug.keystore`) et explicitement branché sur le build type `debug`, pour que chaque build (local ou CI) soit signé avec le même SHA-1 — indispensable pour que le Google Sign-In fonctionne de façon stable en CI
- `applicationIdSuffix ".debug"` retiré : le client OAuth Google n'est enregistré que pour `com.naveenapps.expensemanager` sans suffixe
- Pipeline **GitHub Actions** (`.github/workflows/build.yml`) pour la compilation et les tests à chaque push

### Identité visuelle

- `app_name` → `M3Finances`
- `ic_launcher_background` → `#07052A` (au lieu de `#FFFFFF`)
- Icônes `mipmap-*` (debug et release) régénérées
- Nouvelles icônes vectorielles ajoutées dans `core/designsystem/.../drawable` (catégories de dépenses : maison, transport, loisirs, etc., dont une icône `djamo.xml` spécifique à Mobile Money Côte d'Ivoire)

---

## ⚠️ Usage personnel uniquement

Ce dépôt est un fork personnel configuré avec des identifiants et une configuration Firebase propres à un usage individuel (clé OAuth Google, projet Firestore, keystore debug commité). Il n'est **pas destiné à être publié, redistribué ou réutilisé tel quel**.
