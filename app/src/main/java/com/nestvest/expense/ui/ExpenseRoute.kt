package com.nestvest.expense.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nestvest.expense.data.RoomExpenseRepository
import com.nestvest.R
import kotlinx.coroutines.delay

@Composable
fun ExpenseRoute(viewModel: ExpenseViewModel, repository: RoomExpenseRepository,
    reminders: ReminderViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        reminders.permissionResult()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resume()
        reminders.refreshAvailability()
    }
    LaunchedEffect(viewModel) {
        while (true) { delay(30_000); viewModel.resume() }
    }
    LaunchedEffect(repository) {
        if (!repository.reconcileReceipts()) viewModel.cleanupPending()
    }
    BackHandler(state.screen == ExpenseScreen.HISTORY) { viewModel.navigate(ExpenseScreen.CAPTURE) }
    BackHandler(state.screen == ExpenseScreen.REMINDERS) { viewModel.navigate(ExpenseScreen.HISTORY) }
    Surface {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (state.screen) {
                ExpenseScreen.CAPTURE -> ExpenseCaptureScreen(state, viewModel.profile.preferredCurrency,
                    viewModel::amountChanged, viewModel::save, { viewModel.navigate(ExpenseScreen.HISTORY) })
                ExpenseScreen.HISTORY -> ExpenseHistoryScreen(state, viewModel::periodChanged,
                    viewModel::select, viewModel::refresh, { viewModel.navigate(ExpenseScreen.CAPTURE) },
                    currency = viewModel.profile.preferredCurrency,
                    settings = {
                        OutlinedIconButton(onClick = { viewModel.navigate(ExpenseScreen.REMINDERS) },
                            shape = CircleShape, border = BorderStroke(1.dp, ExpenseStyle.Line)) {
                            Icon(painterResource(R.drawable.ic_reminder_settings), tint = ExpenseStyle.Green,
                                contentDescription = stringResource(R.string.reminder_title), modifier = Modifier.size(18.dp))
                        }
                    })
                ExpenseScreen.DETAIL -> if (state.selected == null) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(if (state.loading) R.string.expense_loading
                            else if (state.readError) R.string.expense_read_error else R.string.expense_missing))
                        TextButton(onClick = { viewModel.navigate(ExpenseScreen.HISTORY) }) {
                            Text(stringResource(R.string.expense_back))
                        }
                    }
                } else state.selected?.let { expense ->
                    val detail: ExpenseDetailViewModel = viewModel(key = expense.expenseId.toString(),
                        factory = viewModelFactory { initializer { ExpenseDetailViewModel(repository, expense) } })
                    LaunchedEffect(expense) { detail.refresh(expense) }
                    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                        if (uri != null) detail.attach { repository.attach(expense.expenseId, uri) }
                    }
                    ExpenseDetailScreen(detail, { viewModel.navigate(ExpenseScreen.HISTORY) },
                        { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        { pending ->
                            viewModel.navigate(ExpenseScreen.HISTORY)
                            if (pending) viewModel.cleanupPending()
                        })
                }
                ExpenseScreen.REMINDERS -> ReminderSettingsScreen(reminders,
                    requestPermission = {
                        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else reminders.permissionResult()
                    },
                    openSettings = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    },
                    onBack = { viewModel.navigate(ExpenseScreen.HISTORY) })
            }
        }
    }
}
