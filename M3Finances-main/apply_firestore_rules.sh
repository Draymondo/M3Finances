#!/data/data/com.termux/files/usr/bin/bash
# Adds firestore.rules (documentation of the security rules, merged with what's live in the
# Firebase console as of 2026-08-20) to the repo. This does NOT deploy anything to Firebase
# by itself — it's a local doc file. Deployment still requires pasting into the console (or
# setting up the Firebase CLI) separately.
# Run this from the ROOT of your git clone (where settings.gradle.kts lives).
set -e
echo "Writing firestore.rules..."
cat > "firestore.rules" << 'CLAUDE_FIRESTORE_RULES_APPLY_EOF'
rules_version = '2';

// Règles de sécurité Firestore pour le projet m3notes-9712f.
// Ce projet Firebase est partagé entre deux apps :
//   - com.m3notes.app (attestation) — blocs /notes/{noteId} et /vaultMeta/{userId} ci-dessous,
//     copiés tels quels depuis la console au 20/08/2026, non modifiés
//   - M3Finances — bloc /users/{userId}/{document=**} ajouté ci-dessous
//
// Toutes les données de M3Finances vivent sous /users/{userId}/... (comptes, catégories,
// transactions, budgets, et tout ce qui sera ajouté plus tard — dettes, objectifs
// d'épargne, transactions récurrentes — puisque le wildcard {document=**} couvre
// n'importe quelle sous-collection présente ou future sans qu'il faille modifier cette
// règle). Une personne ne peut lire ou écrire que sous son propre userId, authentifié via
// Google Sign-In (Firebase Auth) — voir GoogleAuthRepositoryImpl.kt et
// CloudBackupRepositoryImpl.kt côté app.
//
// Tout ce qui n'est explicitement autorisé nulle part est refusé par défaut par Firestore
// — inutile d'ajouter un `deny` explicite en fin de fichier.

service cloud.firestore {
  match /databases/{database}/documents {

    // --- com.m3notes.app (attestation) — NE PAS MODIFIER ---
    match /notes/{noteId} {
      allow read, update, delete: if request.auth != null
        && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null
        && request.auth.uid == request.resource.data.userId;
    }

    match /vaultMeta/{userId} {
      allow read, write: if request.auth != null
        && request.auth.uid == userId;
    }

    // --- M3Finances ---
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

  }
}
CLAUDE_FIRESTORE_RULES_APPLY_EOF

echo "Done. Now run: git add -A && git commit -m \"Add versioned Firestore security rules\" && git push"
