plugins {
    id("naveenapps.plugin.android.feature")
    id("naveenapps.plugin.kotlin.basic")
    id("naveenapps.plugin.compose")
    id("naveenapps.plugin.di")
}

android {
    namespace = "com.naveenapps.expensemanager.feature.calendar"
}

dependencies {
    implementation(project(":core:settings"))
    implementation(project(":feature:transaction"))
}

