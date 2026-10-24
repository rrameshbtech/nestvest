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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.nestvest.dashboard.domain.DashboardDestination
import com.nestvest.dashboard.ui.DashboardRoute
import com.nestvest.dashboard.ui.DashboardViewModel
import com.nestvest.profile.ui.ProfileRoute
import com.nestvest.profile.ui.ProfileViewModel
import com.nestvest.profile.ui.theme.NestVestTheme
import com.nestvest.expense.ui.ExpenseRoute
import com.nestvest.expense.ui.ExpenseViewModel
import com.nestvest.expense.ui.ExpenseScreen
import com.nestvest.expense.ui.ReminderViewModel
import com.nestvest.expense.reminder.ExpenseReminderNotifier
import com.nestvest.kakeibo.ui.KakeiboRoute
import com.nestvest.kakeibo.ui.KakeiboViewModel

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
                var destination by remember { mutableStateOf<DashboardDestination?>(null) }
                LaunchedEffect(captureRequest) {
                    if (captureRequest > 0) destination = DashboardDestination.EXPENSE_CAPTURE
                }
                ProfileRoute(vm) { profile ->
                    val dashboard: DashboardViewModel = viewModel(key = "dashboard", factory = viewModelFactory {
                        initializer {
                            DashboardViewModel(
                                app.dashboardPreferenceRepository,
                                app.expenseRepository,
                                profile,
                                kakeibo = app.kakeiboRepository,
                            )
                        }
                    })
                    if (destination == null) {
                        DashboardRoute(dashboard) { destination = it }
                    } else if (destination == DashboardDestination.KAKEIBO) {
                        val kakeibo: KakeiboViewModel = viewModel(
                            key = "kakeibo-route",
                            factory = viewModelFactory {
                                initializer { KakeiboViewModel(app.kakeiboRepository, profile) }
                            },
                        )
                        KakeiboRoute(kakeibo) { destination = null }
                    } else {
                        val selectedDestination = checkNotNull(destination)
                        val screen = when (selectedDestination) {
                            DashboardDestination.EXPENSE_CAPTURE -> ExpenseScreen.CAPTURE
                            DashboardDestination.EXPENSE_HISTORY -> ExpenseScreen.HISTORY
                            DashboardDestination.REMINDERS -> ExpenseScreen.REMINDERS
                            DashboardDestination.KAKEIBO -> error("Kakeibo has a dedicated route")
                        }
                        val expenses: ExpenseViewModel = viewModel(key = "expense-route", factory = viewModelFactory {
                            initializer {
                                ExpenseViewModel(app.expenseRepository, profile, createSavedStateHandle(),
                                    initialScreen = screen)
                            }
                        })
                        LaunchedEffect(destination) { expenses.navigate(screen) }
                        val reminders: ReminderViewModel = viewModel(key = "reminders", factory = viewModelFactory {
                            initializer {
                                ReminderViewModel(app.reminderRepository, app.reminderScheduler,
                                    app.reminderNotifier::available) { Build.VERSION.SDK_INT >= 33 }
                            }
                        })
                        ExpenseRoute(expenses, app.expenseRepository, reminders, screen) {
                            destination = null
                        }
                    }
                }
            }
        }
    }
}
