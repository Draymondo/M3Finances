#!/data/data/com.termux/files/usr/bin/bash
# Ajoute 3 raccourcis d'application (long-appui sur l'icône du launcher) :
# Nouvelle dépense, Dettes, Liste de courses.
# A exécuter depuis la racine du repo (~/repo).
set -euo pipefail

if [ ! -f "settings.gradle.kts" ]; then
  echo "Erreur : lance ce script depuis la racine du repo (là où se trouve settings.gradle.kts)."
  exit 1
fi

echo "== Création des nouveaux fichiers =="

echo "  app/src/main/res/xml/shortcuts.xml"
mkdir -p "app/src/main/res/xml"
cat > "app/src/main/res/xml/shortcuts.xml" << 'CLAUDE_FIX_EOF'
<?xml version="1.0" encoding="utf-8"?>
<!--
    Static (long-press-on-launcher-icon) shortcuts. Each targets MainActivity with a
    "shortcut_destination" extra — see MainActivity.landingScreenFromShortcut — which picks the
    NavHost's start destination directly, skipping the normal Home screen. If the app is locked
    (see AppLockScreen), the destination still applies after a successful unlock, since
    MainActivity always computes it up front regardless of lock state.
-->
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">

    <shortcut
        android:shortcutId="new_transaction"
        android:enabled="true"
        android:icon="@drawable/ic_add"
        android:shortcutShortLabel="@string/shortcut_new_transaction_short"
        android:shortcutLongLabel="@string/shortcut_new_transaction_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.naveenapps.expensemanager"
            android:targetClass="com.naveenapps.expensemanager.MainActivity">
            <extra
                android:name="shortcut_destination"
                android:value="new_transaction" />
        </intent>
    </shortcut>

    <shortcut
        android:shortcutId="debt_list"
        android:enabled="true"
        android:icon="@drawable/payments"
        android:shortcutShortLabel="@string/shortcut_debt_list_short"
        android:shortcutLongLabel="@string/shortcut_debt_list_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.naveenapps.expensemanager"
            android:targetClass="com.naveenapps.expensemanager.MainActivity">
            <extra
                android:name="shortcut_destination"
                android:value="debt_list" />
        </intent>
    </shortcut>

    <shortcut
        android:shortcutId="shopping_list"
        android:enabled="true"
        android:icon="@drawable/shopping_cart"
        android:shortcutShortLabel="@string/shortcut_shopping_list_short"
        android:shortcutLongLabel="@string/shortcut_shopping_list_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="com.naveenapps.expensemanager"
            android:targetClass="com.naveenapps.expensemanager.MainActivity">
            <extra
                android:name="shortcut_destination"
                android:value="shopping_list" />
        </intent>
    </shortcut>

</shortcuts>
CLAUDE_FIX_EOF

echo
echo "== Application des correctifs sur les fichiers existants =="
python3 - << 'CLAUDE_PY_EOF'
import sys

EDITS = [
  (
    "app/src/main/AndroidManifest.xml",
    '            android:name=".MainActivity"\n            android:exported="true">\n            <intent-filter>',
    '            android:name=".MainActivity"\n            android:exported="true">\n            <meta-data\n                android:name="android.app.shortcuts"\n                android:resource="@xml/shortcuts" />\n            <intent-filter>',
  ),
  (
    "app/src/main/res/values/strings.xml",
    '    <string name="app_lock_unlock">Unlock</string>\n</resources>',
    '    <string name="app_lock_unlock">Unlock</string>\n    <string name="shortcut_new_transaction_short">New expense</string>\n    <string name="shortcut_new_transaction_long">Add a new expense</string>\n    <string name="shortcut_debt_list_short">Debts</string>\n    <string name="shortcut_debt_list_long">View my debts</string>\n    <string name="shortcut_shopping_list_short">Shopping</string>\n    <string name="shortcut_shopping_list_long">View my shopping lists</string>\n</resources>',
  ),
  (
    "app/src/main/res/values-fr/strings.xml",
    '    <string name="app_lock_unlock">Déverrouiller</string>\n</resources>',
    '    <string name="app_lock_unlock">Déverrouiller</string>\n    <string name="shortcut_new_transaction_short">Nouvelle dépense</string>\n    <string name="shortcut_new_transaction_long">Ajouter une nouvelle dépense</string>\n    <string name="shortcut_debt_list_short">Dettes</string>\n    <string name="shortcut_debt_list_long">Voir mes dettes</string>\n    <string name="shortcut_shopping_list_short">Courses</string>\n    <string name="shortcut_shopping_list_long">Voir mes listes de courses</string>\n</resources>',
  ),
  (
    "app/src/main/kotlin/com/naveenapps/expensemanager/MainActivity.kt",
    "                    MainScreen(\n                        composeNavigator = appComposeNavigator,\n                        componentProvider = activityComponentProvider,\n                        isDarkTheme = isDarkTheme,\n                        landingScreen = if (onBoardingStatus == true) {\n                            ExpenseManagerScreens.Home\n                        } else {\n                            ExpenseManagerScreens.IntroScreen\n                        }\n                    )",
    "                    MainScreen(\n                        composeNavigator = appComposeNavigator,\n                        componentProvider = activityComponentProvider,\n                        isDarkTheme = isDarkTheme,\n                        landingScreen = if (onBoardingStatus == true) {\n                            landingScreenFromShortcut() ?: ExpenseManagerScreens.Home\n                        } else {\n                            ExpenseManagerScreens.IntroScreen\n                        }\n                    )",
  ),
  (
    "app/src/main/kotlin/com/naveenapps/expensemanager/MainActivity.kt",
    "    override fun onStart() {\n        super.onStart()\n        launchAppUpdateCheck()\n    }\n\n    private fun showBiometricPrompt() {",
    "    override fun onStart() {\n        super.onStart()\n        launchAppUpdateCheck()\n    }\n\n    /** Maps the `shortcut_destination` extra set by `res/xml/shortcuts.xml` to the NavHost's\n     * start destination, so a long-press shortcut lands directly on that screen instead of\n     * Home. `null` (no extra, or an unrecognised value) falls back to the normal Home landing. */\n    private fun landingScreenFromShortcut(): ExpenseManagerScreens? {\n        return when (intent?.getStringExtra(SHORTCUT_DESTINATION_EXTRA)) {\n            \"new_transaction\" -> ExpenseManagerScreens.TransactionCreate(id = null)\n            \"debt_list\" -> ExpenseManagerScreens.DebtList\n            \"shopping_list\" -> ExpenseManagerScreens.ShoppingListList\n            else -> null\n        }\n    }\n\n    private fun showBiometricPrompt() {",
  ),
  (
    "app/src/main/kotlin/com/naveenapps/expensemanager/MainActivity.kt",
    "    companion object {\n        private const val DAYS_FOR_FLEXIBLE_UPDATE: Int = 3\n    }",
    "    companion object {\n        private const val DAYS_FOR_FLEXIBLE_UPDATE: Int = 3\n        private const val SHORTCUT_DESTINATION_EXTRA = \"shortcut_destination\"\n    }",
  ),
]

def main():
    ok = True
    for path, old, new in EDITS:
        with open(path, "r", encoding="utf-8") as fh:
            content = fh.read()
        if new in content:
            print(f"  [déjà appliqué] {path}")
            continue
        count = content.count(old)
        if count == 0:
            print(f"  [ERREUR] motif introuvable dans {path} — vérifie ce fichier à la main.")
            print("           Motif recherché : " + old.splitlines()[0][:80])
            ok = False
            continue
        if count > 1:
            print(f"  [ERREUR] motif trouvé {count} fois dans {path} (attendu 1), abandon pour ce fichier.")
            ok = False
            continue
        content = content.replace(old, new, 1)
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(content)
        print(f"  [ok] {path}")
    if not ok:
        print("\nCertains correctifs n'ont pas pu être appliqués automatiquement.")
        sys.exit(1)

if __name__ == "__main__":
    main()
CLAUDE_PY_EOF

echo
echo "Correctif appliqué. Étapes suivantes :"
echo "  git add -A"
echo "  git commit -m \"Ajout : raccourcis d'application (nouvelle dépense, dettes, courses)\""
echo "  git pull --rebase origin main"
echo "  git push"
