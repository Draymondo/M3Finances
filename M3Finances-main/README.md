# M3Finances

M3Finances est une application Android de gestion financière personnelle, construite en Kotlin avec Jetpack Compose, Room, Koin et Google Gemini. Elle vise à centraliser les comptes, les transactions, les budgets, les objectifs, les dettes, les listes de courses et l’assistant IA pour gérer son budget au quotidien.

## Audit complet de l’application

### 1. Vue d’ensemble

L’application est une version personnalisée et fortement étendue d’un gestionnaire de dépenses Android. La base du projet repose sur une architecture multi-modules :

- `app` : application Android principale
- `core/*` : modèles, repository, data, datastore, design system, notifications, settings
- `feature/*` : écrans par domaine métier (dashboard, transaction, budget, category, account, savings, recurring, debt, shopping, settings, etc.)

La stack technique actuelle est cohérente pour une app de finance personnelle :

- Kotlin + Jetpack Compose
- Room pour la persistance locale
- Flow / StateFlow pour l’état UI
- Koin pour l’injection de dépendances
- Material 3 pour le design
- Firebase / Firestore pour la sauvegarde cloud
- Gemini via Google Generative AI SDK pour OCR, aide au classement, parsing de notifications et assistant chat

### 2. Fonctionnalités déjà présentes

#### Gestion courante
- Comptes bancaires et comptes de type Mobile Money
- Transactions de type revenu / dépense / transfert
- Catégories personnalisables
- Budgets mensuels
- Recherche de transactions
- Analyse du dashboard (solde total, revenus, dépenses, évolution)
- Transactions scindées (split)
- Paramétrage des filtres et de la période
- Gestion des comptes, catégories, export, thèmes et langues

#### Gestion avancée de finances personnelles
- Dettes avec direction, échéance, suivi et relance
- Transactions récurrentes et abonnements
- Objectifs d’épargne
- Listes de courses
- Comptes/transactions spécifiques à une logique de suivi d’argent personnel

#### IA et automatisation
- Assistant de chat financier en français
- Analyse de reçus par image (OCR + structuration des données)
- Parsing de notifications Mobile Money (Wave, Orange Money, MTN MoMo, Moov Money, Djamo) via un NotificationListenerService, avec detection automatique de la source et score de confiance sur chaque proposition avant validation
- Suggestion de catégorie automatique à partir d’une note de transaction
- Propositions éditables avant validation

### 3. Architecture technique

Le projet suit un découpage propre par couches :

- `core/model` : modèles de données du domaine
- `core/domain` : use cases métier
- `core/data` : implémentations repository + intégrations externes
- `core/database` : schéma Room + DAO
- `core/repository` : interfaces de repository
- `core/designsystem` : composants UI réutilisables
- `feature/*` : écrans et ViewModels

Cette organisation facilite la maintenance et permet d’ajouter des fonctionnalités sans casser le reste de l’application.

### 4. État du code et qualité

#### Points forts
- Architecture modulaire claire et extensible
- UI moderne en Compose
- Séparation des responsabilités
- Modules bien séparés par domaine
- Assistant IA intégré dans le flux de création de données
- Cohérence entre données, repository et écrans

#### Points à surveiller
- Le projet reste orienté “usage personnel” et n’est pas pensé pour une distribution publique générale
- Certaines zones sont très personnalisées à la logique du projet (comptes Mobile Money, règles métier spécifiques)
- Le chat IA dépend d’une clé Gemini valide et du comportement des prompts
- Des éléments de configuration restent spécifiques au projet local (keystore, Firebase, OAuth)

### 5. Sécurité et configuration

Le dépôt est configuré pour un usage personnel, avec des éléments propres à ce projet :

- `keys/debug.keystore` : utilisé pour signer les builds debug
- `google-services.json` : configuration Firebase locale
- `firestore.rules` : règles de sécurité versionnées dans le dépôt
- `credentials.properties` / Play Publishing si disponibles localement

Cette configuration est fonctionnelle pour un usage personnel, mais elle n’est pas prête pour une publication publique standard sans revue et nettoyage des credentials.

### 6. IA / Gemini : choix de modèles

Le projet applique une stratégie de coût / performance selon le cas d’usage :

- `scanReceipt` : `gemini-3.6-flash` pour l’analyse multimodale de reçu (image + OCR + structuration)
- `parseMobileMoneyNotification` : `gemini-3.1-flash-lite` pour un texte de notification simple (la source Mobile Money est detectee localement via le package Android de la notification, pas par Gemini ; le score de confiance est calcule localement a partir des champs reellement extraits, pas auto-declare par le modele)
- `suggestCategory` : `gemini-3.1-flash-lite` pour une classification rapide de texte

Cette séparation est cohérente pour équilibrer précision, vitesse et coût de tokens.

### 7. Build et validation

Le projet est conçu pour fonctionner en debug sur appareil Android local, avec installation depuis Gradle. La validation récente confirme que le build de l’application est fonctionnel :

- `:app:installDebug`
- Résultat attendu : `BUILD SUCCESSFUL`
- Installation sur appareil Android détectée

### 8. Conclusion de l’audit

L’application est dans un état global solide pour un usage personnel avancé :

- architecture saine,
- UX assez complète,
- fonctionnalités de finance personnelle bien présentes,
- IA intégrée de façon utile,
- app déjà bien stabilisée sur Android.

Le principal point de vigilance reste le contexte “personnel / local” du projet, surtout côté credentials et settings Firebase.

---

## Structure du projet

```text
M3Finances-main/
+-- app/
+-- core/
+-- feature/
+-- gradle/
+-- keys/
+-- build.gradle.kts
+-- settings.gradle.kts
+-- README.md
+-- firestore.rules
+-- codemagic.yaml
+-- .github/
+-- .gitignore
```

Modules principaux :

- `core:data` : repository impls, cloud sync, AI parsing
- `core:database` : Room database
- `core:designsystem` : composants UI réutilisables
- `core:model` : modèles de données
- `feature:transaction` : transactions, scan, assistant IA
- `feature:dashboard` : écran d’accueil et synthèse
- `feature:budget` : budget
- `feature:debt` : dettes
- `feature:savings` : objectifs d’épargne
- `feature:shopping` : liste de courses
- `feature:settings` : réglages

---

## Démarrage rapide

### Prérequis
- Android Studio
- JDK 17+
- Android SDK 35 / compatible
- Git
- Clé Gemini configurée dans les paramètres de l’application si l’IA est utilisée

### Lancer le projet

```bash
./gradlew :app:installDebug
```

### Vérifier le build

```bash
./gradlew :app:assembleDebug
```

---

## Points de vigilance / recommandations

1. Nettoyer les fichiers de configuration spécifiques au projet avant une sortie publique.
2. Vérifier régulièrement la compatibilité des modèles Gemini avec la version du SDK.
3. Ajouter des tests de régression sur les flux IA et les parsing de texte/image.
4. Évaluer la sécurité et la stratégie de sauvegarde cloud avant un usage partagé.
5. Garder un esprit “personnel” si l’app reste destinée à un seul utilisateur.

---

## Statut du projet

État actuel :
- Application fonctionnelle en debug
- IA intégrée et utilisée dans plusieurs flux
- Dashboard et assistant finance déjà renforcés
- Build Android validée sur appareil
- Projet orienté usage personnel robuste et personnalisable

Ce dépôt est donc dans un état de développement avancé, fonctionnel et cohérent pour un usage personnel sérieux.


## Historique recent

- Ajout de `source` (Wave, Orange Money, MTN MoMo, Moov Money, Djamo) et `confidence` sur les transactions en attente issues des notifications Mobile Money, avec migration Room 13->14
- Service d'ecoute des notifications renomme `MobileMoneyNotificationListenerService` -> `MobileMoneyNotificationListenerService` pour refleter son perimetre reel (9 apps ecoutees, pas seulement Wave)