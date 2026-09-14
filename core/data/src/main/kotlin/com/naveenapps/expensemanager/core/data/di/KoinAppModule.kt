package com.naveenapps.expensemanager.core.data.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.naveenapps.expensemanager.core.data.network.exchangerate.ExchangeRateApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Devise gérée par exchangerate-api.com (v6) — voir le "Convertisseur de devises".
private const val EXCHANGE_RATE_API_BASE_URL = "https://v6.exchangerate-api.com/"

val AppModule = module {
    single<Gson> { GsonBuilder().create() }

    // Used only for the automatic cloud backup identity/destination — see
    // GoogleAuthRepositoryImpl / CloudBackupRepositoryImpl. Not analytics, not crash reporting;
    // those were removed entirely (see FIREBASE removal notes) and are not coming back.
    single<FirebaseAuth> { Firebase.auth }
    single<FirebaseFirestore> { Firebase.firestore }

    // Client réseau du "Convertisseur de devises" — première fonctionnalité de l'app à faire de
    // vrais appels réseau (voir ExchangeRateApiService).
    single<Json> { Json { ignoreUnknownKeys = true } }
    single<OkHttpClient> {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl(EXCHANGE_RATE_API_BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
    single<ExchangeRateApiService> { get<Retrofit>().create(ExchangeRateApiService::class.java) }
}


