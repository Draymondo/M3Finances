# Sauvegarde cloud automatique — étapes à faire toi-même (console Firebase)

## État au 13 août 2026

- ✅ **Projet Firebase réutilisé** : projet existant `m3notes-9712f` (déjà utilisé pour l'app
  d'attestation `com.m3notes.app`), pas de nouveau projet créé.
- ✅ **Authentification Google activée** (en plus de l'email/mot de passe déjà en place).
- ✅ **App M3Finances enregistrée** dans ce projet (`com.naveenapps.expensemanager`).
- ✅ **`google-services.json` reçu et placé** dans `app/google-services.json` — contient bien les
  deux apps (`com.m3notes.app` + `com.naveenapps.expensemanager`).
- ⏳ **SHA-1 de M3Finances pas encore ajouté** — à faire depuis Android Studio sur PC (voir
  étape 3 ci-dessous). Tant que ce n'est pas fait, le *build* fonctionnera, mais une tentative
  réelle de connexion Google sur un appareil échouera (erreur d'auth) — normal et attendu.
- ⏳ **Règles de sécurité Storage** — pas encore configurées pour M3Finances, à faire une fois
  qu'on aura vu les règles actuelles (déjà utilisées par l'app d'attestation) pour ne rien casser.

## 3. Récupérer et ajouter le SHA-1 de M3Finances (À FAIRE SUR PC)

1. Ouvre le projet M3Finances dans Android Studio
2. Panneau **Gradle** (à droite, icône éléphant) → `M3Finances → app → Tasks → android → signingReport`
3. Double-clique dessus, ça lance une tâche Gradle et affiche un résultat dans l'onglet "Run" en bas
4. Cherche la ligne `SHA1:` sous la variante `debug` (et aussi `release` si tu as déjà un keystore de release séparé) — copie la valeur
5. Firebase Console → Paramètres du projet → Général → dans la carte de l'app M3Finances
   (`com.naveenapps.expensemanager`) → **Ajouter une empreinte** → colle le SHA-1
6. Une fois fait, retélécharge `google-services.json` (il changera pour inclure le certificat) et
   remplace la copie dans `app/google-services.json`

## 5. Règles de sécurité Firestore — en attente

Toujours en attente que tu partages le contenu actuel des règles Firestore (Firebase Console →
Firestore Database → onglet Règles) pour qu'on ajoute la partie M3Finances sans toucher à ce qui
sert déjà à l'app d'attestation. Voir le commentaire en tête de `CloudBackupRepositoryImpl.kt`
pour la règle exacte à ajouter (scoping `/users/{userId}/...`).

**Changement important depuis la dernière version de ce document** : on est passés de Firebase
**Storage** à Firebase **Firestore**. Storage impose désormais le forfait payant Blaze même pour
un usage minime (changement de politique Google, février 2026), alors que Firestore reste
disponible sur le forfait gratuit Spark. Au passage, l'architecture est aussi devenue plus
robuste : au lieu d'un seul fichier de sauvegarde opaque, chaque compte/catégorie/budget/
transaction est maintenant son propre document Firestore, sans limite de taille à craindre à
long terme.

## Fonctionnalité Dettes (parquée) — impact sur la sauvegarde cloud

Fait : Dettes, rappels de dette, objectifs d'épargne et transactions récurrentes sont
maintenant synchronisés eux aussi (mêmes mappeurs dans `FirestoreEntityMappers.kt`, mêmes
listes dans `CloudBackupRepositoryImpl` et `DatabaseChangeCloudBackupTrigger`).

Les paramètres applicatifs non sensibles sont maintenant synchronisés dans
`/users/{userId}/settings/app` : thème, langue, devise, format des nombres, rappels,
filtres, catégories/comptes par défaut et options d'affichage. La clé Gemini et l'état du
verrouillage de l'application restent uniquement sur l'appareil et ne sont jamais envoyés
dans Firestore.

---

## Ce qui n'est PAS encore fait côté app (pas de code écrit pour ça)

Il n'y a **aucun bouton dans l'interface** pour déclencher la première connexion Google. Le code
actuel ne fait qu'une tentative *silencieuse* au démarrage (pour la restauration automatique sur
un nouveau téléphone) — mais la toute première connexion, sur ton tout premier téléphone, doit
être explicite quelque part (ex: un bouton "Connecter Google" dans Settings) pour que
`filterByAuthorizedAccounts` ait quelque chose à trouver la fois suivante. Ce bouton reste à
construire — pas fait dans cette session.

## Vérifications à faire une fois sur PC (non vérifiables hors-ligne)

- Les versions dans `gradle/libs.versions.toml` (`firebaseBom`, `androidxCredentials`,
  `googleIdentityGoogleid`) sont celles connues au moment de la rédaction — vérifier s'il y a
  plus récent.
- L'API Credential Manager / Google Identity utilisée dans `GoogleAuthRepositoryImpl` n'a pas pu
  être vérifiée contre la doc en ligne — à comparer avec
  https://developer.android.com/identity/sign-in/credential-manager-siwg avant de faire confiance
  au code tel quel.
