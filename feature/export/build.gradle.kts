plugins {
    id("naveenapps.plugin.android.feature")
    id("naveenapps.plugin.kotlin.basic")
    id("naveenapps.plugin.compose")
    id("naveenapps.plugin.di")
}

android {
    namespace = "com.naveenapps.expensemanager.feature.export"
}

dependencies {
    implementation(project(":feature:account"))
    implementation(project(":feature:filter"))

    // Tests du chemin d'export (BUG-001) : Intent SAF et sélecteur de fichier simulés
    // par Robolectric, comme dans :core:data.
    testImplementation(libs.robolectric)
}