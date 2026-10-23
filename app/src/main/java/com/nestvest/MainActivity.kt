package com.nestvest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nestvest.profile.ui.ProfileRoute
import com.nestvest.profile.ui.ProfileViewModel
import com.nestvest.profile.ui.theme.NestVestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as NestVestApplication).repository
        setContent {
            val vm: ProfileViewModel = viewModel(factory = viewModelFactory {
                initializer { ProfileViewModel(repository, createSavedStateHandle()) }
            })
            NestVestTheme { ProfileRoute(vm) }
        }
    }
}
