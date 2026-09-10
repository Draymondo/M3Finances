package com.naveenapps.expensemanager.core.data.di

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.koin.dsl.module

val AppModule = module {
    single<Gson> { GsonBuilder().create() }

    // Used only for the automatic cloud backup identity/destination — see
    // GoogleAuthRepositoryImpl / CloudBackupRepositoryImpl. Not analytics, not crash reporting;
    // those were removed entirely (see FIREBASE removal notes) and are not coming back.
    single<FirebaseAuth> { Firebase.auth }
    single<FirebaseFirestore> { Firebase.firestore }
}

