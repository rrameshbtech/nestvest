package com.nestvest

import android.os.Bundle
import android.os.Build
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import com.nestvest.profile.ui.ProfileRoute
import com.nestvest.profile.ui.ProfileViewModel
import com.nestvest.profile.ui.theme.NestVestTheme
import com.nestvest.expense.ui.ExpenseRoute
import com.nestvest.expense.ui.ExpenseViewModel
import com.nestvest.expense.ui.ExpenseScreen
import com.nestvest.expense.ui.ReminderViewModel
import com.nestvest.expense.reminder.ExpenseReminderNotifier

class MainActivity : ComponentActivity() {
    private var captureRequest by mutableIntStateOf(0)

    private fun receiveCaptureRequest(intent: Intent) {
        if (intent.getBooleanExtra(ExpenseReminderNotifier.OPEN_CAPTURE, false)) {
            captureRequest++
            intent.removeExtra(ExpenseReminderNotifier.OPEN_CAPTURE)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveCaptureRequest(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiveCaptureRequest(intent)
        enableEdgeToEdge()
        val app = application as NestVestApplication
        val repository = app.repository
        setContent {
            val vm: ProfileViewModel = viewModel(factory = viewModelFactory {
                initializer { ProfileViewModel(repository, createSavedStateHandle()) }
            })
            NestVestTheme {
                ProfileRoute(vm) { profile ->
                    val expenses: ExpenseViewModel = viewModel(factory = viewModelFactory {
                        initializer {
                            ExpenseViewModel(app.expenseRepository,
                                profile, createSavedStateHandle())
                        }
                    })
                    val reminders: ReminderViewModel = viewModel(factory = viewModelFactory {
                        initializer {
                            ReminderViewModel(app.reminderRepository, app.reminderScheduler,
                                app.reminderNotifier::available) { Build.VERSION.SDK_INT >= 33 }
                        }
                    })
                    LaunchedEffect(captureRequest) {
                        if (captureRequest > 0) expenses.navigate(ExpenseScreen.CAPTURE)
                    }
                    ExpenseRoute(expenses, app.expenseRepository, reminders)
                }
            }
        }
    }
}
