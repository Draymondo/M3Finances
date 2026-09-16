import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("naveenapps.plugin.android.app")
    // id("com.google.android.gms.oss-licenses-plugin")
    id("naveenapps.plugin.kotlin.basic")
    id("naveenapps.plugin.compose")
    id("naveenapps.plugin.di")
    id("com.github.triplet.play")
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler)
}


val keysFolderPath: String =
    if (File("${rootDir.absolutePath}/keys/credentials.properties").exists()) {
        "${rootDir.absolutePath}/keys"
    } else {
        rootDir.absolutePath
    }

fun getCredentialsFile(): File {
    val credentialFilePath = "$keysFolderPath/credentials.properties"
    return File(credentialFilePath)
}

fun getKeystoreFile(): File {
    val keystoreFilePath = "$keysFolderPath/android_keystore.jks"
    return File(keystoreFilePath)
}

fun getPlayStorePublisherFile(): File {
    val playStorePublisherFile = "$keysFolderPath/play_publish.json"
    return File(playStorePublisherFile)
}

val credentials = getCredentialsFile()
val keystore = getKeystoreFile()
if (credentials.exists() && keystore.exists()) {
    println("----- Both Keystore & Credentials available -----")
    println("----- ${credentials.absolutePath} -----")
    val properties = Properties().apply {
        load(FileInputStream(credentials))
    }

    android {
        signingConfigs {
            create("release") {
                keyAlias = properties.getProperty("KEY_ALIAS")
                storePassword = properties.getProperty("KEY_STORE_PASSWORD")
                keyPassword = properties.getProperty("KEY_PASSWORD")
                storeFile = keystore
            }
        }
    }
} else {
    println("----- Credentials not available -----")
}

val playStorePublisher = getPlayStorePublisherFile()
if (playStorePublisher.exists()) {
    println("----- Play Store Publisher available -----")
    println("----- ${playStorePublisher.absolutePath} -----")
    val track = System.getenv()["PLAYSTORE_TRACK"]
    val status = System.getenv()["PLAYSTORE_RELEASE_STATUS"]?.uppercase()
    println("----- ENV: $track & $status -----")
    val playStoreTrack = track ?: "beta"
    val playStoreReleaseStatus =
        runCatching { ReleaseStatus.valueOf(status!!) }.getOrNull() ?: ReleaseStatus.DRAFT

    println("----- $playStoreTrack & $playStoreReleaseStatus-----")

    android {
        play {
            this.serviceAccountCredentials.set(playStorePublisher)
            this.track.set(playStoreTrack)
            this.releaseStatus.set(playStoreReleaseStatus)
            println(this.serviceAccountCredentials.get().asFile.absolutePath)
        }
    }
} else {
    println("----- Publisher not available -----")
}

android {

    namespace = "com.naveenapps.expensemanager"

    defaultConfig {
        applicationId = "com.naveenapps.expensemanager"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Usage perso uniquement sur appareil arm64 (Android moderne) : on ne construit pas
        // les binaires natifs pour armeabi-v7a / x86 / x86_64, ce qui réduit nettement la
        // taille de l'APK. À retirer si l'app doit un jour tourner sur un appareil 32-bit/x86.
        ndk {
            abiFilters += "arm64-v8a"
        }

        resConfigs("en", "fr")
    }

    signingConfigs {
        // Keystore debug standard (alias/mot de passe par défaut d'Android), committé dans le
        // repo à keys/debug.keystore. Le brancher explicitement ici garantit que CHAQUE build
        // debug (local ou CI GitHub Actions, qui tourne sur un runner neuf à chaque fois) est
        // signé avec la même clé — sinon AGP génère un keystore debug aléatoire par run, et le
        // SHA-1 ne matche jamais celui enregistré côté Firebase pour le Google Sign-In.
        getByName("debug") {
            storeFile = file("${rootDir.absolutePath}/keys/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            // applicationIdSuffix ".debug" retiré : le client OAuth Android enregistré dans
            // google-services.json ne couvre que com.naveenapps.expensemanager (sans suffixe).
            // Avec le suffixe, Google Play Services ne trouve aucun identifiant pour le
            // package installé et le Credential Manager renvoie NoCredentialException
            // ("No credentials available").
            enableUnitTestCoverage = true
        }
        create("macrobenchmark") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val keyStore = runCatching { signingConfigs.getByName("release") }.getOrNull()
                ?: signingConfigs.getByName("debug")

            signingConfig = keyStore
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    testOptions {
        managedDevices {
            devices {
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel2api30").apply {
                    // Use device profiles you typically see in Android Studio.
                    device = "Pixel 2"
                    // Use only API levels 27 and higher.
                    apiLevel = 30
                    // To include Google services, use "google".
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:navigation"))
    implementation(project(":core:notification"))
    implementation(project(":core:repository"))
    implementation(project(":core:settings"))

    implementation(project(":feature:account"))
    implementation(project(":feature:analysis"))
    implementation(project(":feature:budget"))
    implementation(project(":feature:category"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:transaction"))
    implementation(project(":feature:filter"))
    implementation(project(":feature:country"))
    implementation(project(":feature:currency"))

    implementation(project(":feature:settings"))
    implementation(project(":feature:theme"))
    implementation(project(":feature:language"))
    implementation(project(":feature:export"))
    implementation(project(":feature:reminder"))
    implementation(project(":feature:currency"))
    implementation(project(":feature:recurring"))
    implementation(project(":feature:debt"))
    implementation(project(":feature:savings"))
    implementation(project(":feature:envelope"))
    implementation(project(":feature:shopping"))
    implementation(project(":feature:worktime"))
    implementation(project(":feature:currencyconverter"))
    implementation(project(":feature:calendar"))

    implementation(libs.androidx.splash.screen)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.google.oss.licenses)

    implementation(libs.app.update.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    
    implementation(libs.androidx.work.ktx)
    implementation(libs.koin.android.work.manager)

    testImplementation(project(":core:testing"))
    androidTestImplementation(project(":core:testing"))
}
