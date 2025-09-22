// FirebaseModule.kt
package com.example.werun.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}

// Optional: Additional module for other dependencies
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // You can add other app-wide dependencies here if needed
    // For example, if you want to provide a custom dispatcher:
    /*
    @Provides
    @Singleton
    @Named("IODispatcher")
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
    */
}