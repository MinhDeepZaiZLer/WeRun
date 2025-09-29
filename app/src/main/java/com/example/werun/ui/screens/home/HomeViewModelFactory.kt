package com.example.werun.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.werun.data.repository.HomeRepository
import com.example.werun.data.repository.HomeRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModelFactory(
    private val repository: HomeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Helper function để tạo factory (sử dụng trong Composable)
fun provideHomeViewModelFactory(): HomeViewModelFactory {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val repository = HomeRepositoryImpl(firestore, auth)
    return HomeViewModelFactory(repository)
}