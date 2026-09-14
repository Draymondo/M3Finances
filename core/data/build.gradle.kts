plugins {
    id("naveenapps.plugin.android.library")
    id("naveenapps.plugin.kotlin.basic")
    id("naveenapps.plugin.di")
    id("naveenapps.plugin.room")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.naveenapps.expensemanager.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:repository"))
    implementation(project(":core:datastore"))
    implementation(project(":core:database"))
    implementation(project(":core:settings"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.google.generativeai)

    implementation(libs.opencsv)
    implementation(libs.gson)

    // Convertisseur de devises — première intégration réseau du projet
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.dataStore.preference)
    implementation(libs.backup.restore)
    implementation(libs.google.android.play.review)

    // Sauvegarde cloud automatique (voir CloudBackupRepositoryImpl / GoogleAuthRepositoryImpl)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    androidTestImplementation(project(":core:testing"))
    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}