plugins {
    id("naveenapps.plugin.android.library")
    id("naveenapps.plugin.kotlin.basic")
    id("naveenapps.plugin.di")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.naveenapps.expensemanager.core.datastore"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.dataStore.preference)
    implementation(libs.kotlinx.serialization.json)
}