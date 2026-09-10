#!/usr/bin/env bash
# Usage : place ce script ET fix-prompt-periode-honnetete.patch à la racine du dépôt
# (là où se trouve settings.gradle.kts) dans Codespaces, puis lance :
#   bash apply-and-build.sh
set -euo pipefail

echo "=== 1. Localisation du dépôt ==="
ROOT_DIR="$(pwd)"
if [ ! -f "$ROOT_DIR/settings.gradle.kts" ]; then
    FOUND=$(find "$ROOT_DIR" -maxdepth 4 -name settings.gradle.kts 2>/dev/null | head -1)
    if [ -z "$FOUND" ]; then
        echo "Erreur : settings.gradle.kts introuvable. Lance ce script depuis la racine du dépôt (ou juste au-dessus)."
        exit 1
    fi
    ROOT_DIR="$(dirname "$FOUND")"
fi
cd "$ROOT_DIR"
echo "Racine du projet : $ROOT_DIR"

echo ""
echo "=== 2. Application du patch ==="
PATCH_FILE="fix-prompt-periode-honnetete.patch"
if [ ! -f "$PATCH_FILE" ]; then
    echo "Erreur : $PATCH_FILE introuvable à la racine ($ROOT_DIR)."
    echo "Ajoute le fichier patch à côté de ce script avant de relancer."
    exit 1
fi
git apply --check "$PATCH_FILE"
git apply "$PATCH_FILE"
echo "Patch appliqué avec succès."

echo ""
echo "=== 3. Commit ==="
git add -A
git commit -m "fix: bilan IA - periode neutre, honnetete chat, anti-hallucination categories"
echo "Commit effectué."

echo ""
echo "=== 4. Préparation du SDK Android ==="
# Codespaces ne fournit pas le SDK Android par défaut — on l'installe si absent.
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"

if [ ! -d "$ANDROID_SDK_ROOT/cmdline-tools/latest" ]; then
    echo "Téléchargement des cmdline-tools Android (une seule fois)..."
    TMP_DIR="$(mktemp -d)"
    curl -sSL -o "$TMP_DIR/cmdline-tools.zip" \
        "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    unzip -q "$TMP_DIR/cmdline-tools.zip" -d "$TMP_DIR/extracted"
    mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    mv "$TMP_DIR/extracted/cmdline-tools/"* "$ANDROID_SDK_ROOT/cmdline-tools/latest/"
    rm -rf "$TMP_DIR"
fi

export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

echo "Acceptation des licences SDK..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager --install "platform-tools" > /dev/null

echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
echo "SDK prêt (sdk.dir écrit dans local.properties)."
echo "Note : si le build échoue en réclamant une plateforme ou des build-tools manquants,"
echo "le Gradle Plugin Android essaiera normalement de les télécharger automatiquement à la volée."
echo "Si ça échoue quand même, lance manuellement :"
echo '  sdkmanager "platforms;android-36" "build-tools;36.0.0"'

echo ""
echo "=== 5. Compilation de l'APK debug ==="
chmod +x ./gradlew
./gradlew assembleDebug --no-configuration-cache

echo ""
APK_PATH="$(find . -path "*/outputs/apk/debug/*.apk" 2>/dev/null | head -1)"
if [ -z "$APK_PATH" ]; then
    echo "Le build a tourné mais aucun .apk n'a été trouvé — regarde les logs Gradle ci-dessus pour l'erreur exacte."
    exit 1
fi

echo "=== TERMINÉ ==="
echo "APK généré : $APK_PATH"
echo ""
echo "Pour le récupérer sur ton téléphone depuis Codespaces :"
echo "  1. Dans l'explorateur de fichiers à gauche de VS Code (web), navigue jusqu'à ce chemin."
echo "  2. Clic droit sur le fichier .apk -> Download."
echo "  3. Transfère-le sur ton téléphone (Drive, lien direct, etc.) et installe-le."
